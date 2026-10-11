package com.teamwork.business.automation;

import com.teamwork.business.Project;
import com.teamwork.business.ProjectAutomation;
import com.teamwork.business.SubTask;
import com.teamwork.business.SubTaskStatus;
import com.teamwork.business.Task;
import com.teamwork.business.TaskStatus;
import com.teamwork.data.ActivityLogDB;
import com.teamwork.data.NotificationDB;
import com.teamwork.data.ProjectAutomationDB;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.SubTaskDB;
import com.teamwork.data.TaskDB;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Động cơ Tự động hóa Quy trình Không cần Code (No-Code Automation Engine - Milestone 3).
 *
 * <p>Cung cấp 3 công thức tự động hóa (Automation Recipes):
 * <ol>
 *   <li><b>AUTO_PROMPT_GATE2</b>: Tự động nhắc Task Lead nộp bàn giao Gate 2 khi 100% việc con hoàn thành.</li>
 *   <li><b>AUTO_CLOSE_SUBTASKS</b>: Tự động đóng/nghiệm thu tất cả việc con còn lại khi PM phê duyệt Task cha sang DONE.</li>
 *   <li><b>AUTO_ESCALATE_PRIORITY</b>: Tự động nâng độ ưu tiên lên HIGH và cảnh báo PM khi Task đến hạn chót mà tiến độ &lt; 50%.</li>
 * </ol>
 *
 * <p><b>Cơ chế bảo vệ chống lặp vô tận (Infinite Loop & Self-trigger Protection):</b>
 * Sử dụng {@code ThreadLocal<Boolean> IN_AUTOMATION} để phát hiện và ngăn chặn mọi kích hoạt đệ quy.
 * Mọi thao tác tự động đều được ghi log dưới danh tính "HỆ THỐNG TỰ ĐỘNG" (userId = 0).
 */
public class AutomationEngine {

    private static final Logger LOGGER = Logger.getLogger(AutomationEngine.class.getName());
    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Cờ bảo vệ chống kích hoạt lặp vô hạn (Infinite Loop / Re-entrancy Guard).
     */
    private static final ThreadLocal<Boolean> IN_AUTOMATION = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Kiểm tra luồng hiện tại có đang nằm trong quá trình thực thi tự động hóa hay không.
     */
    public static boolean isInAutomation() {
        return Boolean.TRUE.equals(IN_AUTOMATION.get());
    }

    /**
     * Cho phép cấu hình cưỡng bức cờ IN_AUTOMATION (dành riêng cho Unit Test kiểm chứng).
     */
    public static void setInAutomationForTesting(boolean inAutomation) {
        IN_AUTOMATION.set(inAutomation);
    }

    /**
     * Xóa cờ IN_AUTOMATION sau khi test xong.
     */
    public static void clearAutomationGuard() {
        IN_AUTOMATION.remove();
    }

    // =========================================================================
    // PURE RULE EVALUATION HELPERS (Cho phép Unit Test không phụ thuộc CSDL)
    // =========================================================================

    /**
     * Kiểm tra điều kiện kích hoạt Recipe 1: Auto-Prompt Gate 2.
     *
     * @param recipeEnabled Quy tắc có đang được bật cho dự án không
     * @param requiresGate Task có yêu cầu cổng chất lượng (Quality Gate) không
     * @param progress Tiến độ công việc phụ hiện tại (%)
     * @param taskStatus Trạng thái hiện tại của Task cha
     * @return true nếu thỏa mãn mọi điều kiện kích hoạt
     */
    public static boolean shouldPromptGate2(boolean recipeEnabled, boolean requiresGate, int progress, String taskStatus) {
        if (!recipeEnabled) return false;
        if (!requiresGate) return false;
        if (progress < 100) return false;
        if (taskStatus == null) return false;
        String statusUpper = taskStatus.toUpperCase();
        return !TaskStatus.isDone(statusUpper) && !TaskStatus.SUBMITTED.is(statusUpper);
    }

    /**
     * Kiểm tra điều kiện kích hoạt Recipe 3: Auto-Escalate Priority.
     *
     * @param recipeEnabled Quy tắc có đang được bật cho dự án không
     * @param dueDateIso Hạn chót dạng ISO (YYYY-MM-DD)
     * @param progress Tiến độ subtask hiện tại (%)
     * @param currentPriority Mức độ ưu tiên hiện tại (LOW, MEDIUM, HIGH)
     * @param referenceDate Ngày mốc so sánh (thường là hôm nay)
     * @param taskStatus Trạng thái hiện tại của Task
     * @return true nếu thỏa mãn điều kiện nâng ưu tiên lên HIGH
     */
    public static boolean shouldEscalatePriority(boolean recipeEnabled, String dueDateIso, int progress,
                                                String currentPriority, LocalDate referenceDate, String taskStatus) {
        if (!recipeEnabled) return false;
        if (taskStatus != null) {
            String statusUpper = taskStatus.toUpperCase();
            if (TaskStatus.isDone(statusUpper) || TaskStatus.SUBMITTED.is(statusUpper)) {
                return false;
            }
        }
        if (dueDateIso == null || dueDateIso.trim().isEmpty()) return false;
        if (referenceDate == null) return false;
        try {
            LocalDate due = LocalDate.parse(dueDateIso.trim());
            // Điều kiện: hạn chót <= ngày tham chiếu (hôm nay hoặc đã quá hạn)
            if (due.isAfter(referenceDate)) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        if (progress >= 50) return false;
        return currentPriority == null || !"HIGH".equalsIgnoreCase(currentPriority.trim());
    }

    // =========================================================================
    // RECIPE 1: TỰ ĐỘNG NHẮC NỘP BÀN GIAO GATE 2 (AUTO_PROMPT_GATE2)
    // =========================================================================

    /**
     * Kích hoạt khi tiến độ việc con của Task thay đổi (ví dụ: Task Lead duyệt nghiệm thu việc con hoặc thành viên tick hoàn thành).
     * Nếu toàn bộ việc con đạt 100% và công thức AUTO_PROMPT_GATE2 đang bật:
     * - Tự động gửi thông báo đến Task Lead nhắc nộp sản phẩm hoàn thiện Gate 2.
     * - Ghi ActivityLog dưới danh nghĩa "HỆ THỐNG TỰ ĐỘNG" (userId = 0).
     *
     * @param taskId ID của Task cha
     * @param projectId ID của dự án
     * @return true nếu đã kích hoạt thành công quy tắc, false nếu bỏ qua
     */
    public static boolean onSubtasksProgressUpdated(int taskId, int projectId) {
        if (taskId <= 0 || projectId <= 0) return false;
        if (isInAutomation()) {
            LOGGER.info("[AutomationEngine] Đã chặn kích hoạt đệ quy: Thread đang nằm trong Automation Guard.");
            return false;
        }

        try {
            IN_AUTOMATION.set(Boolean.TRUE);

            if (!ProjectAutomationDB.isRecipeEnabled(projectId, ProjectAutomation.AUTO_PROMPT_GATE2)) {
                return false;
            }

            Task task = TaskDB.selectById(taskId);
            if (task == null || task.getProjectId() != projectId) {
                return false;
            }

            int progress = SubTaskDB.calculateProgress(taskId);
            if (!shouldPromptGate2(true, task.isRequiresGate(), progress, task.getStatus())) {
                return false;
            }

            // Gửi thông báo đến Task Lead (assignee của Task cha)
            int leadId = task.getAssigneeId();
            if (leadId > 0) {
                String link = "/task?action=list&projectId=" + projectId + "#gate2-prompt-" + taskId;
                if (!NotificationDB.existsWithLink(leadId, link)) {
                    NotificationDB.send(
                        leadId,
                        "🚀 Nhắc nộp bàn giao Gate 2",
                        "Tất cả công việc phụ của [" + task.getTitle() + "] đã hoàn thành 100%. Vui lòng nộp hồ sơ sản phẩm để PM nghiệm thu!",
                        link,
                        "TASK_ASSIGNED"
                    );
                }
            }

            // Ghi nhận Activity Log dưới danh nghĩa HỆ THỐNG TỰ ĐỘNG (userId = 0)
            String logDesc = "HỆ THỐNG TỰ ĐỘNG: Đã nhắc Task Lead nộp hồ sơ bàn giao Gate 2 khi 100% công việc phụ hoàn thành.";
            ActivityLogDB.logAsync(projectId, 0, "AUTO_RULE_TRIGGER", "TASK", taskId, task.getTitle(), logDesc);

            // Ghi nhận lượt kích hoạt
            ProjectAutomationDB.recordTrigger(projectId, ProjectAutomation.AUTO_PROMPT_GATE2);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[AutomationEngine] Lỗi khi xử lý onSubtasksProgressUpdated cho Task " + taskId, e);
            return false;
        } finally {
            IN_AUTOMATION.set(Boolean.FALSE);
        }
    }

    // =========================================================================
    // RECIPE 2: TỰ ĐỘNG ĐÓNG VIỆC CON KHI PM PHÊ DUYỆT TASK (AUTO_CLOSE_SUBTASKS)
    // =========================================================================

    /**
     * Kích hoạt khi PM duyệt nghiệm thu Task cha sang DONE.
     * Nếu công thức AUTO_CLOSE_SUBTASKS đang bật:
     * - Tự động đồng bộ và đóng toàn bộ việc con chưa hoàn tất sang APPROVED (team) hoặc DONE (solo).
     * - Ghi ActivityLog dưới danh nghĩa "HỆ THỐNG TỰ ĐỘNG" (userId = 0).
     *
     * @param taskId ID của Task vừa được duyệt
     * @param projectId ID của dự án
     * @return Số lượng việc con đã được tự động đóng
     */
    public static int onTaskApproved(int taskId, int projectId) {
        if (taskId <= 0 || projectId <= 0) return 0;
        if (isInAutomation()) {
            LOGGER.info("[AutomationEngine] Đã chặn kích hoạt đệ quy: Thread đang nằm trong Automation Guard.");
            return 0;
        }

        try {
            IN_AUTOMATION.set(Boolean.TRUE);

            if (!ProjectAutomationDB.isRecipeEnabled(projectId, ProjectAutomation.AUTO_CLOSE_SUBTASKS)) {
                return 0;
            }

            Task task = TaskDB.selectById(taskId);
            Project project = ProjectDB.selectById(projectId);
            List<SubTask> subtasks = SubTaskDB.selectByTaskId(taskId);
            if (subtasks == null || subtasks.isEmpty()) {
                return 0;
            }

            String now = LocalDateTime.now().format(DATE_FORMATTER);
            int closedCount = 0;
            boolean isTeam = project != null && project.isTeamProject();

            for (SubTask st : subtasks) {
                if (!SubTaskStatus.isFinished(st.getStatus())) {
                    if (isTeam) {
                        SubTaskDB.approveDeliverable(st.getId(), now);
                    } else {
                        SubTaskDB.updateStatus(st.getId(), "DONE");
                    }
                    closedCount++;
                }
            }

            if (closedCount > 0) {
                String taskTitle = task != null ? task.getTitle() : "Task #" + taskId;
                String logDesc = "HỆ THỐNG TỰ ĐỘNG: Đã tự động đóng và nghiệm thu " + closedCount
                        + " công việc phụ khi PM phê duyệt công việc.";
                ActivityLogDB.logAsync(projectId, 0, "AUTO_RULE_TRIGGER", "TASK", taskId, taskTitle, logDesc);

                ProjectAutomationDB.recordTrigger(projectId, ProjectAutomation.AUTO_CLOSE_SUBTASKS);
            }

            return closedCount;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[AutomationEngine] Lỗi khi xử lý onTaskApproved cho Task " + taskId, e);
            return 0;
        } finally {
            IN_AUTOMATION.set(Boolean.FALSE);
        }
    }

    // =========================================================================
    // RECIPE 3: TỰ ĐỘNG NÂNG ĐỘ ƯU TIÊN TASK SẮP HẾT HẠN (AUTO_ESCALATE_PRIORITY)
    // =========================================================================

    /**
     * Quét và đánh giá toàn bộ Task của dự án:
     * Với các công việc có due_date &lt;= hôm nay và tiến độ &lt; 50%:
     * - Tự động nâng mức ưu tiên lên HIGH.
     * - Gửi cảnh báo khẩn cấp đến Trưởng Dự Án (PM).
     * - Ghi ActivityLog dưới danh nghĩa "HỆ THỐNG TỰ ĐỘNG" (userId = 0).
     *
     * @param projectId ID của dự án cần quét
     * @return Số lượng công việc đã được tự động nâng mức ưu tiên
     */
    public static int evaluateDueTasks(int projectId) {
        if (projectId <= 0) return 0;
        if (isInAutomation()) {
            LOGGER.info("[AutomationEngine] Đã chặn kích hoạt đệ quy: Thread đang nằm trong Automation Guard.");
            return 0;
        }

        try {
            IN_AUTOMATION.set(Boolean.TRUE);

            if (!ProjectAutomationDB.isRecipeEnabled(projectId, ProjectAutomation.AUTO_ESCALATE_PRIORITY)) {
                return 0;
            }

            Project project = ProjectDB.selectById(projectId);
            List<Task> tasks = TaskDB.selectByProjectId(projectId);
            if (tasks == null || tasks.isEmpty()) {
                return 0;
            }

            LocalDate today = LocalDate.now(VN_ZONE);
            int escalatedCount = 0;

            for (Task t : tasks) {
                int progress = SubTaskDB.calculateProgress(t.getId());
                if (shouldEscalatePriority(true, t.getDueDate(), progress, t.getPriority(), today, t.getStatus())) {
                    // Cập nhật mức độ ưu tiên lên HIGH
                    t.setPriority("HIGH");
                    TaskDB.update(t);

                    // Gửi thông báo cảnh báo đến PM nếu chưa gửi hôm nay
                    if (project != null && project.getOwnerId() > 0) {
                        String link = "/task?action=list&projectId=" + projectId + "#auto-escalate-" + t.getId() + "-" + today;
                        if (!NotificationDB.existsWithLink(project.getOwnerId(), link)) {
                            NotificationDB.send(
                                project.getOwnerId(),
                                "⚠️ Tự động nâng ưu tiên KHẨN CẤP",
                                "Công việc [" + t.getTitle() + "] đã đến hạn chót nhưng tiến độ mới đạt " + progress
                                        + "%. Hệ thống đã tự động nâng độ ưu tiên lên KHẨN CẤP (HIGH).",
                                link,
                                "GENERAL"
                            );
                        }
                    }

                    // Ghi nhận ActivityLog
                    String logDesc = "HỆ THỐNG TỰ ĐỘNG: Đã tự động nâng mức ưu tiên lên HIGH do công việc đến hạn chót ("
                            + t.getDueDate() + ") và tiến độ dưới 50% (" + progress + "%).";
                    ActivityLogDB.logAsync(projectId, 0, "AUTO_RULE_TRIGGER", "TASK", t.getId(), t.getTitle(), logDesc);

                    escalatedCount++;
                }
            }

            if (escalatedCount > 0) {
                ProjectAutomationDB.recordTrigger(projectId, ProjectAutomation.AUTO_ESCALATE_PRIORITY);
            }

            return escalatedCount;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[AutomationEngine] Lỗi khi đánh giá evaluateDueTasks cho projectId=" + projectId, e);
            return 0;
        } finally {
            IN_AUTOMATION.set(Boolean.FALSE);
        }
    }

    /**
     * Quét và thực thi Recipe 3 cho toàn bộ các dự án trong hệ thống (dành cho Cron/Scheduler định kỳ hằng ngày).
     */
    public static int evaluateDueTasksForAllProjects() {
        try {
            List<Project> projects = ProjectDB.selectAll();
            if (projects == null || projects.isEmpty()) return 0;

            int totalEscalated = 0;
            for (Project p : projects) {
                totalEscalated += evaluateDueTasks(p.getId());
            }
            return totalEscalated;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[AutomationEngine] Lỗi khi quét tự động hóa cho toàn bộ dự án", e);
            return 0;
        }
    }
}
