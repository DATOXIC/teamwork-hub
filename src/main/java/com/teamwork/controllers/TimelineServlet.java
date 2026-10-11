package com.teamwork.controllers;

import com.teamwork.controllers.task.TaskAccess;
import com.teamwork.business.SubTaskStatus;
import com.teamwork.business.TaskStatus;
import com.teamwork.business.ActivityLog;
import com.teamwork.business.Project;
import com.teamwork.business.ProjectMember;
import com.teamwork.business.SubTask;
import com.teamwork.business.Task;
import com.teamwork.business.User;
import com.teamwork.data.ActivityLogDB;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.ProjectMemberDB;
import com.teamwork.data.SubTaskDB;
import com.teamwork.data.TaskDB;
import com.teamwork.data.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import com.teamwork.business.TaskDependency;
import com.teamwork.data.TaskDependencyDB;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TimelineServlet — Controller phụ trách Phân hệ Lộ Trình & Sơ Đồ Gantt Tương Tác (/timeline).
 * 
 * <p>Áp dụng nguyên tắc Backend Code Mastery & Database API Design:</p>
 * <ul>
 *   <li>Xác thực phiên đăng nhập và phân quyền thành viên dự án chặt chẽ.</li>
 *   <li>Hỗ trợ cả Server-Side Rendering (JSP EL) và Client-Side Dynamic Interaction (JSON API).</li>
 *   <li>Tự động tính toán khung thời gian (Time Horizon), đường mốc Hôm nay (Today line),
 *       tỷ lệ tiến độ việc con (Subtasks progress %) và cảnh báo trễ hạn (Overdue warnings).</li>
 * </ul>
 */
@WebServlet(name = "TimelineServlet", urlPatterns = {"/timeline"})
public class TimelineServlet extends BaseServlet {

    private static final Logger LOGGER = Logger.getLogger(TimelineServlet.class.getName());
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Kiểm tra xác thực người dùng (Auth Boundary)
        HttpSession session = request.getSession(false);
        User currentUser = currentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=viewLogin");
            return;
        }

        // 2. Xác định Project ID từ request hoặc Cookie ghi nhớ gần nhất
        int projectId = parseProjectId(request, currentUser);
        if (!requireMember(request, response, currentUser, projectId, "Bạn không có quyền truy cập vào lộ trình của dự án này!")) {
            return;
        }

        String action = request.getParameter("action");
        if ("ganttData".equalsIgnoreCase(action)) {
            handleGanttData(request, response, currentUser, projectId);
            return;
        }

        // 4. Lấy thông tin dự án
        Project project = ProjectDB.selectById(projectId);
        if (project == null) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // Ghi nhớ Cookie dự án gần nhất
        rememberProjectCookie(request, response, projectId);

        // 5. Nạp danh sách thành viên dự án
        List<ProjectMember> members = ProjectMemberDB.selectByProjectId(projectId);

        // 6. Nạp danh sách toàn bộ Task & Dependency của dự án
        List<Task> allTasks = TaskDB.selectByProjectId(projectId);
        List<TaskDependency> dependencies = TaskDependencyDB.selectByProjectId(projectId);

        // 7. Tính toán tiến độ subtask và gom dữ liệu cho từng Task
        Map<Integer, Integer> taskProgressMap = new HashMap<>();
        Map<Integer, Integer> subtaskCountMap = new HashMap<>();
        Map<Integer, Integer> completedSubtaskMap = new HashMap<>();
        Map<Integer, List<SubTask>> subtasksByTask = new HashMap<>();

        int doneCount = 0;
        int inProgressCount = 0;
        int todoCount = 0;
        int overdueCount = 0;
        int scheduledCount = 0;
        int unscheduledCount = 0;

        LocalDate today = LocalDate.now();
        LocalDate minHorizon = today.minusDays(7);
        LocalDate maxHorizon = today.plusDays(21);

        for (Task t : allTasks) {
            int taskId = t.getId();
            List<SubTask> stList = SubTaskDB.selectByTaskId(taskId);
            subtasksByTask.put(taskId, stList);
            int totalSt = stList.size();
            subtaskCountMap.put(taskId, totalSt);

            int compSt = 0;
            for (SubTask st : stList) {
                if (st.isCompleted() || SubTaskStatus.isFinished(st.getStatus())) {
                    compSt++;
                }
            }
            completedSubtaskMap.put(taskId, compSt);

            int progressPct = 0;
            if (totalSt > 0) {
                progressPct = (compSt * 100) / totalSt;
            } else {
                progressPct = TaskStatus.DONE.is(t.getStatus()) ? 100 : 0;
            }
            taskProgressMap.put(taskId, progressPct);

            // Thống kê trạng thái
            String st = t.getStatus() != null ? t.getStatus().toUpperCase() : "TODO";
            if ("DONE".equals(st)) {
                doneCount++;
            } else if ("IN_PROGRESS".equals(st) || "SUBMITTED".equals(st) || "PLANNING".equals(st) || "REVISE".equals(st)) {
                inProgressCount++;
            } else {
                todoCount++;
            }

            // Kiểm tra hạn chót và giãn khung thời gian
            String due = t.getDueDate();
            if (due != null && !due.trim().isEmpty()) {
                scheduledCount++;
                try {
                    LocalDate d = LocalDate.parse(due.trim());
                    if (d.isBefore(minHorizon)) {
                        minHorizon = d.minusDays(3);
                    }
                    if (d.isAfter(maxHorizon)) {
                        maxHorizon = d.plusDays(7);
                    }
                } catch (DateTimeParseException ignored) {}

                if (t.isOverdue()) {
                    overdueCount++;
                }
            } else {
                unscheduledCount++;
            }
        }

        int totalTasks = allTasks.size();
        int overallProgress = totalTasks > 0 ? (doneCount * 100) / totalTasks : 0;

        // 8. Tạo chuỗi JSON an toàn cho Client-side Gantt Engine
        String tasksJson = buildTasksJson(allTasks, taskProgressMap, subtaskCountMap, completedSubtaskMap);

        // 9. Đặt Model Attributes cho JSP View
        // ▶ JSP: tasks.jsp, command_palette.jsp, project_report.jsp đọc bằng ${project}
        request.setAttribute("project", project);
        // ▶ JSP: timeline.jsp, project_report.jsp đọc bằng ${members}
        request.setAttribute("members", members);
        // ▶ JSP: timeline.jsp đọc bằng ${allTasks}
        request.setAttribute("allTasks", allTasks);
        request.setAttribute("subtasksByTask", subtasksByTask);
        // ▶ JSP: tasks.jsp đọc bằng ${taskProgressMap}
        request.setAttribute("taskProgressMap", taskProgressMap);
        request.setAttribute("subtaskCountMap", subtaskCountMap);
        request.setAttribute("completedSubtaskMap", completedSubtaskMap);

        // ▶ JSP: project_report.jsp, timeline.jsp đọc bằng ${totalTasks}
        request.setAttribute("totalTasks", totalTasks);
        // ▶ JSP: timeline.jsp đọc bằng ${scheduledCount}
        request.setAttribute("scheduledCount", scheduledCount);
        // ▶ JSP: timeline.jsp đọc bằng ${unscheduledCount}
        request.setAttribute("unscheduledCount", unscheduledCount);
        // ▶ JSP: project_report.jsp, timeline.jsp đọc bằng ${doneCount}
        request.setAttribute("doneCount", doneCount);
        // ▶ JSP: project_report.jsp, timeline.jsp đọc bằng ${inProgressCount}
        request.setAttribute("inProgressCount", inProgressCount);
        // ▶ JSP: project_report.jsp, timeline.jsp đọc bằng ${todoCount}
        request.setAttribute("todoCount", todoCount);
        // ▶ JSP: project_report.jsp, timeline.jsp đọc bằng ${overdueCount}
        request.setAttribute("overdueCount", overdueCount);
        // ▶ JSP: timeline.jsp đọc bằng ${overallProgress}
        request.setAttribute("overallProgress", overallProgress);

        // ▶ JSP: timeline.jsp đọc bằng ${todayDate}
        request.setAttribute("todayDate", today.format(ISO_DATE));
        request.setAttribute("horizonStartDate", minHorizon.format(ISO_DATE));
        request.setAttribute("horizonEndDate", maxHorizon.format(ISO_DATE));
        // ▶ JSP: timeline.jsp đọc bằng ${tasksJson}
        request.setAttribute("tasksJson", tasksJson);
        request.setAttribute("dependencies", dependencies);
        request.setAttribute("dependenciesJson", buildDependenciesJson(dependencies));

        // Quyền chỉ dựa vào ID chủ dự án do server lưu. KHÔNG dùng User.role: đó là chức danh người dùng tự nhập
        // trong hồ sơ (ai cũng gõ được "ADMIN").
        boolean isOwner = ProjectAccess.isOwner(currentUser, project);
        // ▶ JSP: profile.jsp, tasks.jsp đọc bằng ${isOwner}
        request.setAttribute("isOwner", isOwner);
        // ▶ JSP: navbar.jsp đọc bằng ${activeNav}
        request.setAttribute("activeNav", "timeline");

        // Forward sang View timeline.jsp
        // ▶ forward → timeline.jsp (các setAttribute ở trên chính là dữ liệu JSP hiển thị)
        request.getRequestDispatcher("/timeline.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User currentUser = currentUser(request);
        if (currentUser == null) {
            respondJsonOrRedirect(request, response, false, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại!", null);
            return;
        }

        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "updateDueDate";
        }

        int projectId = intParam(request, "projectId", 0);
        if (!ProjectAccess.isMember(currentUser, projectId)) {
            respondJsonOrRedirect(request, response, false, "Bạn không có quyền thao tác trong dự án này!", null);
            return;
        }

        Project project = ProjectDB.selectById(projectId);
        if (project == null) {
            respondJsonOrRedirect(request, response, false, "Dự án không tồn tại!", null);
            return;
        }

        switch (action) {
            case "updateDueDate":
                handleUpdateDueDate(request, response, currentUser, project);
                break;
            case "updateDates":
                handleUpdateDates(request, response, currentUser, project);
                break;
            case "addDependency":
                handleAddDependency(request, response, currentUser, project);
                break;
            case "deleteDependency":
                handleDeleteDependency(request, response, currentUser, project);
                break;
            case "quickAddTask":
                handleQuickAddTask(request, response, currentUser, project);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/timeline?projectId=" + projectId);
                break;
        }
    }

    /**
     * Cập nhật nhanh ngày hết hạn (Due Date) của Task khi kéo chỉnh thanh Gantt hoặc chọn lịch
     */
    private void handleUpdateDueDate(HttpServletRequest request, HttpServletResponse response,
                                     User currentUser, Project project) throws IOException {
        int projectId = project.getId();
        int taskId = intParam(request, "taskId", 0);
        String newDueDate = request.getParameter("newDueDate");

        if (taskId <= 0) {
            respondJsonOrRedirect(request, response, false, "Mã công việc không hợp lệ!", projectId);
            return;
        }

        Task task = TaskDB.selectById(taskId);
        if (task == null || task.getProjectId() != projectId) {
            respondJsonOrRedirect(request, response, false, "Công việc không tồn tại trong dự án!", projectId);
            return;
        }

        // Chỉ Trưởng dự án hoặc người phụ trách (Task Lead) được đổi hạn chót
        if (!TaskAccess.canManageTask(currentUser, task, project)) {
            respondJsonOrRedirect(request, response, false,
                    "Chỉ Trưởng dự án hoặc người phụ trách công việc mới được đổi hạn chót!", projectId);
            return;
        }
        if (TaskStatus.isDone(task.getStatus())) {
            respondJsonOrRedirect(request, response, false,
                    "Công việc đã hoàn thành và được khóa, không thể đổi hạn chót!", projectId);
            return;
        }

        String cleanedDate = "";
        if (newDueDate != null && !newDueDate.trim().isEmpty()) {
            try {
                LocalDate parsed = LocalDate.parse(newDueDate.trim());
                cleanedDate = parsed.format(ISO_DATE);
            } catch (DateTimeParseException e) {
                respondJsonOrRedirect(request, response, false, "Định dạng ngày không hợp lệ (YYYY-MM-DD)!", projectId);
                return;
            }
        }

        String oldDate = task.getDueDate();
        task.setDueDate(cleanedDate);
        boolean ok = TaskDB.update(task);

        if (ok) {
            String desc = "Đã đổi hạn chót từ [" + (oldDate != null && !oldDate.isEmpty() ? oldDate : "Chưa có") + "] sang [" + (cleanedDate.isEmpty() ? "Bỏ hạn chót" : cleanedDate) + "]";
            ActivityLogDB.log(projectId, currentUser.getId(), "UPDATE_DEADLINE", "TASK", taskId, task.getTitle(), desc);

            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            data.put("newDueDate", cleanedDate);
            data.put("isOverdue", task.isOverdue());
            data.put("deadlineStatus", task.getDeadlineStatus());
            respondJsonOrRedirect(request, response, true, "Đã cập nhật hạn chót thành công!", projectId, data);
        } else {
            respondJsonOrRedirect(request, response, false, "Không thể lưu cập nhật vào cơ sở dữ liệu!", projectId);
        }
    }

    /**
     * Cập nhật thời gian bắt đầu và kết thúc (kèm dời lịch tự động Cascade Scheduling)
     */
    private void handleUpdateDates(HttpServletRequest request, HttpServletResponse response,
                                   User currentUser, Project project) throws IOException {
        int projectId = project.getId();
        int taskId = intParam(request, "taskId", 0);
        String startDateStr = request.getParameter("startDate");
        String dueDateStr = request.getParameter("dueDate");

        if (taskId <= 0) {
            respondJsonOrRedirect(request, response, false, "Mã công việc không hợp lệ!", projectId);
            return;
        }

        Task task = TaskDB.selectById(taskId);
        if (task == null || task.getProjectId() != projectId) {
            respondJsonOrRedirect(request, response, false, "Công việc không tồn tại trong dự án!", projectId);
            return;
        }

        if (!TaskAccess.canManageTask(currentUser, task, project)) {
            respondJsonOrRedirect(request, response, false,
                    "Chỉ Trưởng dự án hoặc người phụ trách mới được cập nhật lịch trình!", projectId);
            return;
        }
        if (TaskStatus.isDone(task.getStatus())) {
            respondJsonOrRedirect(request, response, false,
                    "Công việc đã hoàn thành và được khóa, không thể đổi ngày!", projectId);
            return;
        }

        LocalDate parsedStart = null;
        LocalDate parsedDue = null;
        try {
            if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                parsedStart = LocalDate.parse(startDateStr.trim());
            }
            if (dueDateStr != null && !dueDateStr.trim().isEmpty()) {
                parsedDue = LocalDate.parse(dueDateStr.trim());
            }
        } catch (DateTimeParseException e) {
            respondJsonOrRedirect(request, response, false, "Định dạng ngày không hợp lệ (YYYY-MM-DD)!", projectId);
            return;
        }

        if (parsedStart != null && parsedDue != null && parsedDue.isBefore(parsedStart)) {
            respondJsonOrRedirect(request, response, false, "Hạn chót không thể sớm hơn ngày bắt đầu!", projectId);
            return;
        }

        List<Task> shifted = TaskDependencyDB.cascadeShiftDates(taskId, parsedStart, parsedDue);
        if (shifted == null || shifted.isEmpty()) {
            task.setStartDate(parsedStart != null ? parsedStart.toString() : task.getStartDate());
            task.setDueDate(parsedDue != null ? parsedDue.toString() : task.getDueDate());
            TaskDB.update(task);
            shifted = List.of(task);
        }

        String desc = "Cập nhật tiến độ Task [" + task.getTitle() + "]: "
                + (parsedStart != null ? "Bắt đầu " + parsedStart : "")
                + (parsedDue != null ? " Hạn chót " + parsedDue : "");
        if (shifted.size() > 1) {
            desc += " (Tự động dời lịch " + (shifted.size() - 1) + " công việc phụ thuộc)";
        }
        ActivityLogDB.log(projectId, currentUser.getId(), "UPDATE_SCHEDULE", "TASK", taskId, task.getTitle(), desc);

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("shiftedCount", shifted.size());
        respondJsonOrRedirect(request, response, true, "Đã cập nhật lịch trình thành công!", projectId, data);
    }

    /**
     * Thêm mới liên kết phụ thuộc công việc (Task Dependency)
     */
    private void handleAddDependency(HttpServletRequest request, HttpServletResponse response,
                                      User currentUser, Project project) throws IOException {
        int projectId = project.getId();
        int taskId = intParam(request, "taskId", 0);
        int dependsOnTaskId = intParam(request, "dependsOnTaskId", 0);
        int lagDays = intParam(request, "lagDays", 0);
        String dependencyType = request.getParameter("dependencyType");

        if (taskId <= 0 || dependsOnTaskId <= 0) {
            respondJsonOrRedirect(request, response, false, "Mã công việc không hợp lệ!", projectId);
            return;
        }

        if (taskId == dependsOnTaskId) {
            respondJsonOrRedirect(request, response, false, "Một công việc không thể tự phụ thuộc chính nó!", projectId);
            return;
        }

        Task taskA = TaskDB.selectById(dependsOnTaskId);
        Task taskB = TaskDB.selectById(taskId);
        if (taskA == null || taskB == null || taskA.getProjectId() != projectId || taskB.getProjectId() != projectId) {
            respondJsonOrRedirect(request, response, false, "Công việc không thuộc dự án này!", projectId);
            return;
        }

        if (!TaskAccess.canCreateTask(currentUser, project) && !TaskAccess.canManageTask(currentUser, taskB, project)) {
            respondJsonOrRedirect(request, response, false, "Bạn không có quyền thiết lập quan hệ phụ thuộc cho công việc này!", projectId);
            return;
        }

        if (TaskDependencyDB.wouldCreateCycle(projectId, taskId, dependsOnTaskId)) {
            respondJsonOrRedirect(request, response, false, "Không thể thiết lập phụ thuộc: Phát hiện chu trình vòng lặp (Circular Dependency)!", projectId);
            return;
        }

        TaskDependency dep = new TaskDependency(projectId, taskId, dependsOnTaskId, dependencyType, lagDays);
        int newId = TaskDependencyDB.insert(dep);
        if (newId > 0) {
            ActivityLogDB.log(projectId, currentUser.getId(), "ADD_DEPENDENCY", "TASK", taskId, taskB.getTitle(),
                    "Thiết lập phụ thuộc: [" + taskB.getTitle() + "] phụ thuộc vào [" + taskA.getTitle() + "]");
            Map<String, Object> data = new HashMap<>();
            data.put("dependencyId", newId);
            respondJsonOrRedirect(request, response, true, "Đã thiết lập liên kết phụ thuộc thành công!", projectId, data);
        } else {
            respondJsonOrRedirect(request, response, false, "Không thể lưu liên kết phụ thuộc vào cơ sở dữ liệu!", projectId);
        }
    }

    /**
     * Xóa liên kết phụ thuộc công việc
     */
    private void handleDeleteDependency(HttpServletRequest request, HttpServletResponse response,
                                         User currentUser, Project project) throws IOException {
        int projectId = project.getId();
        int dependencyId = intParam(request, "dependencyId", 0);
        int taskId = intParam(request, "taskId", 0);
        int dependsOnTaskId = intParam(request, "dependsOnTaskId", 0);

        boolean ok = false;
        if (dependencyId > 0) {
            ok = TaskDependencyDB.delete(dependencyId);
        } else if (taskId > 0 && dependsOnTaskId > 0) {
            ok = TaskDependencyDB.deleteByPair(taskId, dependsOnTaskId);
        }

        if (ok) {
            ActivityLogDB.log(projectId, currentUser.getId(), "DELETE_DEPENDENCY", "PROJECT", projectId, project.getName(),
                    "Đã hủy bỏ liên kết phụ thuộc công việc");
            respondJsonOrRedirect(request, response, true, "Đã xóa liên kết phụ thuộc thành công!", projectId);
        } else {
            respondJsonOrRedirect(request, response, false, "Không tìm thấy hoặc không thể xóa liên kết phụ thuộc!", projectId);
        }
    }

    /**
     * Xuất dữ liệu biểu đồ Frappe Gantt JSON (hỗ trợ AJAX reload)
     */
    private void handleGanttData(HttpServletRequest request, HttpServletResponse response,
                                User currentUser, int projectId) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        List<Task> tasks = TaskDB.selectByProjectId(projectId);
        List<TaskDependency> dependencies = TaskDependencyDB.selectByProjectId(projectId);

        Map<Integer, List<Integer>> predMap = new HashMap<>();
        for (TaskDependency d : dependencies) {
            predMap.computeIfAbsent(d.getTaskId(), k -> new ArrayList<>()).add(d.getDependsOnTaskId());
        }

        Map<Integer, Integer> progressMap = new HashMap<>();
        for (Task t : tasks) {
            int tid = t.getId();
            List<SubTask> subs = SubTaskDB.selectByTaskId(tid);
            if (subs != null && !subs.isEmpty()) {
                long doneSubs = subs.stream().filter(s -> SubTaskStatus.DONE.is(s.getStatus()) || SubTaskStatus.APPROVED.is(s.getStatus())).count();
                progressMap.put(tid, (int) Math.round((doneSubs * 100.0) / subs.size()));
            } else {
                progressMap.put(tid, TaskStatus.isDone(t.getStatus()) ? 100 : 0);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"ok\":true,\"tasks\":[");
        boolean first = true;
        for (Task t : tasks) {
            if (!first) sb.append(",");
            first = false;

            int tid = t.getId();
            String start = t.getEffectiveStartDate();
            String due = (t.getDueDate() != null && !t.getDueDate().trim().isEmpty())
                    ? t.getDueDate().trim() : start;

            try {
                if (LocalDate.parse(due).isBefore(LocalDate.parse(start))) {
                    due = start;
                }
            } catch (Exception ignored) {}

            int progress = progressMap.getOrDefault(tid, 0);
            List<Integer> preds = predMap.getOrDefault(tid, Collections.emptyList());
            String depStr = preds.stream().map(String::valueOf).collect(Collectors.joining(", "));

            boolean hasUnfinishedPrereqs = TaskDependencyDB.hasUnfinishedPrerequisites(tid);

            String customClass = "gantt-bar-" + (t.getStatus() != null ? t.getStatus().toLowerCase() : "todo");
            if (t.isOverdue()) customClass += " gantt-bar-overdue";
            if (hasUnfinishedPrereqs) customClass += " gantt-bar-locked";

            sb.append("{");
            sb.append("\"id\":\"").append(tid).append("\",");
            sb.append("\"name\":\"").append(escapeJson(t.getTitle())).append("\",");
            sb.append("\"start\":\"").append(start).append("\",");
            sb.append("\"end\":\"").append(due).append("\",");
            sb.append("\"progress\":").append(progress).append(",");
            sb.append("\"dependencies\":\"").append(depStr).append("\",");
            sb.append("\"custom_class\":\"").append(customClass).append("\",");
            sb.append("\"status\":\"").append(escapeJson(t.getStatus())).append("\",");
            sb.append("\"priority\":\"").append(escapeJson(t.getPriority())).append("\",");
            sb.append("\"assigneeName\":\"").append(escapeJson(t.getAssigneeName())).append("\",");
            sb.append("\"isLocked\":").append(hasUnfinishedPrereqs);
            sb.append("}");
        }
        sb.append("],\"dependencies\":");
        sb.append(buildDependenciesJson(dependencies));
        sb.append("}");

        out.print(sb.toString());
        out.flush();
    }

    /**
     * Thêm nhanh một công việc mới có gắn sẵn hạn chót trực tiếp trên giao diện Timeline
     */
    private void handleQuickAddTask(HttpServletRequest request, HttpServletResponse response,
                                    User currentUser, Project project) throws IOException {
        int projectId = project.getId();
        String title = request.getParameter("title");
        String dueDate = request.getParameter("dueDate");
        String priority = request.getParameter("priority");
        int assigneeId = intParam(request, "assigneeId", 0);
        String description = request.getParameter("description");

        // Quy tắc dự án: chỉ Trưởng dự án (PM) được tạo và giao công việc
        if (!TaskAccess.canCreateTask(currentUser, project)) {
            respondJsonOrRedirect(request, response, false, "Chỉ Trưởng dự án mới được tạo và giao công việc!", projectId);
            return;
        }

        if (title == null || title.trim().isEmpty()) {
            respondJsonOrRedirect(request, response, false, "Tiêu đề công việc không được để trống!", projectId);
            return;
        }

        String cleanPriority = priority != null ? priority.trim().toUpperCase() : "";
        if (!"HIGH".equals(cleanPriority) && !"LOW".equals(cleanPriority)) {
            cleanPriority = "MEDIUM";
        }

        String cleanDueDate = "";
        if (dueDate != null && !dueDate.trim().isEmpty()) {
            try {
                cleanDueDate = LocalDate.parse(dueDate.trim()).format(ISO_DATE);
            } catch (DateTimeParseException e) {
                respondJsonOrRedirect(request, response, false, "Định dạng ngày không hợp lệ (YYYY-MM-DD)!", projectId);
                return;
            }
        }

        String startDate = request.getParameter("startDate");
        String cleanStartDate = "";
        if (startDate != null && !startDate.trim().isEmpty()) {
            try {
                cleanStartDate = LocalDate.parse(startDate.trim()).format(ISO_DATE);
            } catch (DateTimeParseException ignored) {}
        }

        Task newTask = new Task();
        newTask.setProjectId(projectId);
        newTask.setTitle(title.trim());
        newTask.setDescription(description != null ? description.trim() : "");
        newTask.setStatus("TODO");
        newTask.setPriority(cleanPriority);
        newTask.setStartDate(cleanStartDate);
        newTask.setDueDate(cleanDueDate);

        if (project.isSoloProject()) {
            // Dự án cá nhân: tự giao cho chính mình, không có cổng duyệt
            newTask.setAssigneeId(currentUser.getId());
            newTask.setAssigneeName(currentUser.getFullName());
            newTask.setRequiresGate(false);
        } else {
            // Chỉ giao được cho thành viên của dự án
            User assignee = (assigneeId > 0 && ProjectMemberDB.isMember(projectId, assigneeId)) ? UserDB.selectById(assigneeId) : null;
            newTask.setAssigneeId(assignee != null ? assignee.getId() : 0);
            newTask.setAssigneeName(assignee != null ? assignee.getFullName() : "Chưa phân công");
            newTask.setRequiresGate(true);
        }

        int newId = TaskDB.insert(newTask);
        if (newId > 0) {
            ActivityLogDB.log(projectId, currentUser.getId(), "CREATE", "TASK", newId, title.trim(), "Tạo công việc từ Lộ trình Timeline");
            if (isAjaxRequest(request)) {
                Map<String, Object> data = new HashMap<>();
                data.put("taskId", newId);
                respondJsonOrRedirect(request, response, true, "Đã tạo công việc mới trên Lộ trình!", projectId, data);
            } else {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    // ▶ JSP: docs.jsp, tasks.jsp đọc bằng ${toastSuccess}
                    session.setAttribute("toastSuccess", "Đã tạo công việc \"" + title.trim() + "\" thành công!");
                }
                response.sendRedirect(request.getContextPath() + "/timeline?projectId=" + projectId);
            }
        } else {
            respondJsonOrRedirect(request, response, false, "Không thể tạo công việc mới!", projectId);
        }
    }

    /**
     * Trả về kết quả JSON (khi gọi qua AJAX) hoặc Chuyển hướng Redirect (khi Submit form thường)
     */
    private void respondJsonOrRedirect(HttpServletRequest request, HttpServletResponse response,
                                       boolean success, String message, Integer projectId) throws IOException {
        respondJsonOrRedirect(request, response, success, message, projectId, null);
    }

    private void respondJsonOrRedirect(HttpServletRequest request, HttpServletResponse response,
                                       boolean success, String message, Integer projectId,
                                       Map<String, Object> extraData) throws IOException {
        if (isAjaxRequest(request)) {
            // ▶ JS: trả JSON cho fetch() trong js/timeline.js (updateDueDate; không forward JSP)
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            StringBuilder sb = new StringBuilder();
            sb.append("{\"ok\":").append(success);
            sb.append(",\"message\":\"").append(escapeJson(message)).append("\"");
            if (extraData != null) {
                for (Map.Entry<String, Object> entry : extraData.entrySet()) {
                    sb.append(",\"").append(entry.getKey()).append("\":");
                    Object val = entry.getValue();
                    if (val instanceof Number || val instanceof Boolean) {
                        sb.append(val);
                    } else {
                        sb.append("\"").append(escapeJson(String.valueOf(val))).append("\"");
                    }
                }
            }
            sb.append("}");
            out.print(sb.toString());
            out.flush();
        } else {
            HttpSession session = request.getSession(false);
            if (session != null) {
                if (success) session.setAttribute("toastSuccess", message);
                else session.setAttribute("toastError", message);
            }
            if (projectId != null && projectId > 0) {
                response.sendRedirect(request.getContextPath() + "/timeline?projectId=" + projectId);
            } else {
                response.sendRedirect(request.getContextPath() + "/project?action=list");
            }
        }
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String xrw = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        String param = request.getParameter("isAjax");
        return "XMLHttpRequest".equalsIgnoreCase(xrw)
                || (accept != null && accept.contains("application/json"))
                || "true".equalsIgnoreCase(param);
    }

    /**
     * Xây dựng chuỗi JSON an toàn cho Client-Side Timeline Engine
     */
    private String buildTasksJson(List<Task> tasks, Map<Integer, Integer> progressMap,
                                  Map<Integer, Integer> subtaskCountMap, Map<Integer, Integer> completedMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Task t : tasks) {
            if (!first) sb.append(",");
            first = false;

            int id = t.getId();
            int progress = progressMap.getOrDefault(id, 0);
            int subCount = subtaskCountMap.getOrDefault(id, 0);
            int completed = completedMap.getOrDefault(id, 0);

            sb.append("{");
            sb.append("\"id\":").append(id).append(",");
            sb.append("\"title\":\"").append(escapeJson(t.getTitle())).append("\",");
            sb.append("\"status\":\"").append(escapeJson(t.getStatus())).append("\",");
            sb.append("\"priority\":\"").append(escapeJson(t.getPriority())).append("\",");
            sb.append("\"dueDate\":\"").append(escapeJson(t.getDueDate())).append("\",");
            sb.append("\"startDate\":\"").append(escapeJson(t.getStartDate())).append("\",");
            sb.append("\"effectiveStartDate\":\"").append(escapeJson(t.getEffectiveStartDate())).append("\",");
            sb.append("\"assigneeId\":").append(t.getAssigneeId()).append(",");
            sb.append("\"assigneeName\":\"").append(escapeJson(t.getAssigneeName())).append("\",");
            sb.append("\"progress\":").append(progress).append(",");
            sb.append("\"subtaskCount\":").append(subCount).append(",");
            sb.append("\"completedSubtasks\":").append(completed).append(",");
            sb.append("\"isOverdue\":").append(t.isOverdue()).append(",");
            sb.append("\"deadlineStatus\":\"").append(escapeJson(t.getDeadlineStatus())).append("\",");
            sb.append("\"deadlineBadgeClass\":\"").append(escapeJson(t.getDeadlineBadgeClass())).append("\"");
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String buildDependenciesJson(List<TaskDependency> deps) {
        if (deps == null || deps.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (TaskDependency d : deps) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{");
            sb.append("\"id\":").append(d.getId()).append(",");
            sb.append("\"taskId\":").append(d.getTaskId()).append(",");
            sb.append("\"dependsOnTaskId\":").append(d.getDependsOnTaskId()).append(",");
            sb.append("\"taskTitle\":\"").append(escapeJson(d.getTaskTitle())).append("\",");
            sb.append("\"dependsOnTaskTitle\":\"").append(escapeJson(d.getDependsOnTaskTitle())).append("\",");
            sb.append("\"dependencyType\":\"").append(escapeJson(d.getDependencyType())).append("\",");
            sb.append("\"lagDays\":").append(d.getLagDays());
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                // JSON này được nhúng trong <script> của timeline.jsp: chặn "</script>" và thẻ HTML
                .replace("<", "\\u003c")
                .replace(">", "\\u003e")
                .replace("&", "\\u0026");
    }

    private int parseProjectId(HttpServletRequest request, User currentUser) {
        String p = request.getParameter("projectId");
        if (p != null && !p.trim().isEmpty()) {
            try {
                return Integer.parseInt(p.trim());
            } catch (NumberFormatException ignored) {}
        }
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("last_project_id".equals(c.getName()) && c.getValue() != null) {
                    try {
                        int cachedId = Integer.parseInt(c.getValue().trim());
                        if (ProjectAccess.isMember(currentUser, cachedId)) {
                            return cachedId;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return 0;
    }

    private void rememberProjectCookie(HttpServletRequest request, HttpServletResponse response, int projectId) {
        Cookie c = new Cookie("last_project_id", String.valueOf(projectId));
        c.setMaxAge(30 * 24 * 60 * 60);
        c.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        c.setHttpOnly(true);
        response.addCookie(c);
    }

}
