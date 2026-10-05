package com.teamwork.controllers.task;

import com.teamwork.business.Doc;
import com.teamwork.business.Label;
import com.teamwork.business.Message;
import com.teamwork.business.Notification;
import com.teamwork.business.Project;
import com.teamwork.business.SubTask;
import com.teamwork.business.Task;
import com.teamwork.business.TaskDoc;
import com.teamwork.business.User;
import com.teamwork.data.DocDB;
import com.teamwork.data.LabelDB;
import com.teamwork.data.MessageDB;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.SubTaskDB;
import com.teamwork.data.TaskDB;
import com.teamwork.data.TaskDocDB;
import com.teamwork.data.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.teamwork.business.ProjectInvite;
import com.teamwork.business.ProjectMember;
import com.teamwork.data.NotificationDB;
import com.teamwork.data.ProjectInviteDB;
import com.teamwork.data.ProjectMemberDB;
import jakarta.servlet.http.HttpSession;
import com.teamwork.business.UserWorkload;
import com.teamwork.business.ActivityLog;
import com.teamwork.data.ActivityLogDB;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import static com.teamwork.controllers.task.TaskAccess.*;
import static com.teamwork.controllers.task.TaskJson.*;
import static com.teamwork.controllers.task.TaskBoardHandler.*;
import static com.teamwork.controllers.task.TaskCrudHandler.*;
import static com.teamwork.controllers.task.SubTaskHandler.*;

/**
 * Nhóm quy trình nghiệm thu Task cha: nộp bàn giao, xin duyệt kế hoạch (Planning) và PM duyệt / cân chỉnh / trả về.
 *
 * <p>Được gọi từ {@link com.teamwork.controllers.TaskServlet} (bộ điều phối duy nhất của URL /task).
 * Tên hàm {@code handleXxx} khớp với tham số {@code action} trong form / fetch của tasks.jsp.</p>
 */
public final class TaskWorkflowHandler {

    private TaskWorkflowHandler() {}


    /**
     * Nghiệp vụ 12: Task Lead Bàn Giao & Nộp Báo Cáo Task Lớn Lên Cho PM (Chuyển sang 🟡 SUBMITTED)
     */
    public static void handleSubmitParentTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        
        // Thu thập 5 trường thông tin báo cáo bàn giao có cấu trúc + Tệp đính kèm
        String summary = request.getParameter("summary");
        String demoUrl = request.getParameter("demoUrl");
        String codeUrl = request.getParameter("codeUrl");
        String testResult = request.getParameter("testResult");
        String testingGuide = request.getParameter("testingGuide");
        String deliverableNote = request.getParameter("deliverableNote");
        String deliverableFile = request.getParameter("deliverableFile");

        if (projectId <= 0 || taskId <= 0) 
        {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task != null && project != null && task.getProjectId() == projectId && currentUser != null) {
            // KIỂM SOÁT THẨM QUYỀN NGHIỆM THU TẦNG 2:
            // 1. Nếu Task lớn đã gán cho Task Lead cụ thể: CHỈ chính Task Lead đó mới được nộp bàn giao.
            // 2. Nếu Task lớn chưa gán cho ai: PM mới được nộp.
            boolean gateTask = project.isTeamProject() && task.isRequiresGate();
            if (gateTask && ("TODO".equalsIgnoreCase(task.getStatus()) || "PLANNING".equalsIgnoreCase(task.getStatus())
                    || "DONE".equalsIgnoreCase(task.getStatus()))) {
                // Không được nộp bàn giao khi chưa qua Cổng 1 (duyệt kế hoạch) hoặc khi công việc đã đóng.
                if (session != null) session.setAttribute("toastError",
                        "Công việc [" + task.getTitle() + "] chưa ở giai đoạn thực hiện (cần được duyệt kế hoạch trước) nên chưa thể nộp bàn giao.");
            } else if (canReviewSubTask(currentUser, task, project)) {
                // RÀNG BUỘC CHẤT LƯỢNG: Task Lead chỉ được nộp bàn giao khi toàn bộ việc con đã hoàn tất 100%
                List<SubTask> subTasks = SubTaskDB.selectByTaskId(taskId);
                int progress = SubTaskDB.calculateProgress(taskId);
                if (subTasks != null && !subTasks.isEmpty() && progress < 100) {
                    if (session != null) {
                        // ▶ JSP: docs.jsp đọc bằng ${toastError}
                        session.setAttribute("toastError", 
                            "⚠️ Không thể nộp bàn giao công việc [" + task.getTitle() + "] cho trưởng dự án khi danh sách nhiệm vụ chưa đạt 100% (Tiến độ hiện tại: " + progress + "%). Hãy hoàn thành các nhiệm vụ trước!");
                    }
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                // Ghép 5 trường dữ liệu thành Bản Báo Cáo Bàn Giao Chuẩn Cấu Trúc
                StringBuilder sb = new StringBuilder();
                if (summary != null && !summary.trim().isEmpty()) {
                    sb.append("📌 [Tóm tắt kết quả]: ").append(summary.trim()).append("\n\n");
                }
                if (demoUrl != null && !demoUrl.trim().isEmpty()) {
                    sb.append("🌐 [Link Demo/Sản phẩm]: ").append(demoUrl.trim()).append("\n\n");
                }
                if (codeUrl != null && !codeUrl.trim().isEmpty()) {
                    sb.append("💻 [Link Mã nguồn/PR]: ").append(codeUrl.trim()).append("\n\n");
                }
                if (testResult != null && !testResult.trim().isEmpty()) {
                    sb.append("🧪 [Kết quả kiểm thử]: ").append(testResult.trim()).append("\n\n");
                }
                if (testingGuide != null && !testingGuide.trim().isEmpty()) {
                    sb.append("🧭 [Hướng dẫn trưởng dự án nghiệm thu]: ").append(testingGuide.trim());
                }

                String finalNote = sb.toString().trim();
                if (finalNote.isEmpty()) {
                    finalNote = (deliverableNote != null && !deliverableNote.trim().isEmpty()) 
                        ? deliverableNote.trim() 
                        : "Đã hoàn thành toàn bộ công việc theo yêu cầu.";
                }

                if (deliverableFile == null || deliverableFile.trim().isEmpty()) {
                    deliverableFile = "Bao_Cao_Nghiem_Thu_Task_" + task.getId() + ".pdf";
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.submitTaskDeliverable(taskId, finalNote, deliverableFile.trim(), now);

                // Ghi nhận Activity Log
                ActivityLogDB.logAsync(projectId, currentUser.getId(), "TASK_SUBMIT", "TASK", taskId, task.getTitle(), "Đã nộp hồ sơ bàn giao nghiệm thu kèm tệp [" + deliverableFile.trim() + "] lên trưởng dự án");

                // Bắn thông báo thời gian thực 🔔 cho Trưởng Dự Án (PM)
                if (project.getOwnerId() > 0 && project.getOwnerId() != currentUser.getId()) {
                    NotificationDB.send(
                        project.getOwnerId(),
                        "🟡 Bàn giao công việc lớn",
                        currentUser.getFullName() + " vừa nộp báo cáo bàn giao công việc [" + task.getTitle() + "] kèm tệp đính kèm, kính mời trưởng dự án nghiệm thu!",
                        "/task?action=list&projectId=" + projectId,
                        "bi-box-seam-fill text-warning"
                    );
                }

                // Thông báo lên Luồng Thảo luận
                String msgContent = "📦 [BÀN GIAO CÔNG VIỆC]: " + currentUser.getFullName() + " đã nộp hồ sơ bàn giao công việc [" + task.getTitle() + "] kèm tệp [" + deliverableFile.trim() + "] lên trưởng dự án!";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                if (session != null) session.setAttribute("toastSuccess", "Đã nộp báo cáo bàn giao công việc lớn thành công! Đang chờ trưởng dự án phê duyệt.");
            } else {
                if (session != null) session.setAttribute("toastError", "Bạn không phải là trưởng nhóm công việc của thẻ công việc này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 12.5: Task Lead Trình Kế Hoạch Phân Rã Việc Con Cho PM Thẩm Định (CỔNG 1 ➔ Chuyển sang 🟣 PLANNING)
     */
    public static void handleSubmitPlanningRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String planningNote = request.getParameter("planningNote");

        if (projectId <= 0 || taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task != null && project != null && task.getProjectId() == projectId && currentUser != null) {
            // KIỂM SOÁT THẨM QUYỀN: Task Lead của task hoặc PM
            if (!"TODO".equalsIgnoreCase(task.getStatus())) {
                if (session != null) session.setAttribute("toastError",
                        "Chỉ gửi duyệt kế hoạch được khi công việc đang ở trạng thái Cần làm (hiện tại: " + task.getStatus() + ").");
            } else if (canReviewSubTask(currentUser, task, project)) {
                // RÀNG BUỘC CHẤT LƯỢNG: Phải phân rã ít nhất 1 việc con mới được trình PM
                List<SubTask> subTasks = SubTaskDB.selectByTaskId(taskId);
                if (subTasks == null || subTasks.isEmpty()) {
                    if (session != null) {
                        session.setAttribute("toastError", 
                            "⚠️ Không thể trình kế hoạch rỗng! Vui lòng phân rã ít nhất 1 nhiệm vụ trước khi gửi trưởng dự án duyệt.");
                    }
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.submitPlanningRequest(taskId, planningNote, now);

                // Bắn thông báo thời gian thực 🔔 cho Trưởng Dự Án (PM)
                if (project.getOwnerId() > 0 && project.getOwnerId() != currentUser.getId()) {
                    NotificationDB.send(
                        project.getOwnerId(),
                        "🟣 Trình Kế Hoạch Phân Rã Nhiệm Vụ",
                        currentUser.getFullName() + " vừa trình kế hoạch phân rã " + subTasks.size() + " nhiệm vụ cho công việc [" + task.getTitle() + "], kính mời trưởng dự án xem xét và khóa kế hoạch!",
                        "/task?action=list&projectId=" + projectId,
                        "bi-diagram-3-fill text-primary"
                    );
                }

                // Thông báo lên Luồng Thảo luận
                String msgContent = "📋 [TRÌNH KẾ HOẠCH PHÂN RÃ]: " + currentUser.getFullName() + " đã phân rã xong " + subTasks.size() + " nhiệm vụ cho công việc [" + task.getTitle() + "] và trình lên Trưởng Dự Án phê duyệt khóa phạm vi!";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                if (session != null) session.setAttribute("toastSuccess", "Đã trình kế hoạch phân rã nhiệm vụ lên Trưởng Dự Án thành công! Đang chờ trưởng dự án phê duyệt khóa phạm vi.");
            } else {
                if (session != null) session.setAttribute("toastError", "Bạn không phải là trưởng nhóm công việc của thẻ công việc này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 12.6: Trưởng Dự Án (PM) Phê Duyệt Kế Hoạch & KHÓA PHÂN RÃ (SCOPE LOCK ➔ Chuyển sang 🚀 IN_PROGRESS)
     */
    public static void handlePmApprovePlanning(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String feedback = request.getParameter("feedback");

        if (projectId <= 0 || taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task != null && project != null && task.getProjectId() == projectId && currentUser != null) {
            // KIỂM SOÁT BẢO MẬT: Chỉ DUY NHẤT Trưởng Dự Án (PM) mới được duyệt kế hoạch Cổng 1
            if (!"PLANNING".equalsIgnoreCase(task.getStatus())) {
                if (session != null) session.setAttribute("toastError", "Công việc này không ở trạng thái chờ duyệt kế hoạch!");
            } else if (isProjectOwner(currentUser, project)) {
                List<SubTask> subTasks = SubTaskDB.selectByTaskId(taskId);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmApprovePlanning(taskId, feedback, now);

                // Bắn thông báo thời gian thực 🔔 cho Task Lead
                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "🔒 Trưởng Dự Án Đã Phê Duyệt & Khóa Kế Hoạch",
                        "Trưởng Dự Án đã duyệt ma trận phân rã " + (subTasks != null ? subTasks.size() : 0) + " nhiệm vụ của công việc [" + task.getTitle() + "]. Kế hoạch đã khóa, đội ngũ bắt tay thực thi!",
                        "/task?action=list&projectId=" + projectId,
                        "bi-lock-fill text-success"
                    );
                }

                // Thông báo lên Luồng Thảo luận
                String msgContent = "🔒 [TRƯỞNG DỰ ÁN KHÓA KẾ HOẠCH PHÂN RÃ]: Trưởng Dự Án đã duyệt danh mục " + (subTasks != null ? subTasks.size() : 0) + " nhiệm vụ của công việc [" + task.getTitle() + "]! Phạm vi công việc chính thức được KHÓA (Scope Baseline Lock). Đội ngũ bắt đầu thực thi!";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                if (session != null) session.setAttribute("toastSuccess", "Trưởng Dự Án đã phê duyệt và khóa kế hoạch phân rã thành công! Công việc chuyển sang Đang Làm.");
            } else {
                if (session != null) session.setAttribute("toastError", "Chỉ Trưởng Dự Án mới có thẩm quyền duyệt và khóa kế hoạch phân rã!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 12.7: Trưởng Dự Án (PM) Yêu Cầu Task Lead Bổ Sung / Chỉnh Sửa Kế Hoạch (Trả về ⚪ TODO)
     */
    public static void handlePmRejectPlanning(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String feedback = request.getParameter("feedback");

        if (projectId <= 0 || taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task != null && project != null && task.getProjectId() == projectId && currentUser != null) {
            if (!"PLANNING".equalsIgnoreCase(task.getStatus())) {
                if (session != null) session.setAttribute("toastError", "Công việc này không ở trạng thái chờ duyệt kế hoạch!");
            } else if (isProjectOwner(currentUser, project)) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmRejectPlanning(taskId, feedback, now);

                // Bắn thông báo thời gian thực 🔔 cho Task Lead
                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "↩️ Trưởng Dự Án Yêu Cầu Chỉnh Sửa Kế Hoạch",
                        "Trưởng Dự Án yêu cầu bổ sung kế hoạch công việc [" + task.getTitle() + "]: \"" + (feedback != null ? feedback : "Cần bóc tách thêm nhiệm vụ") + "\"",
                        "/task?action=list&projectId=" + projectId,
                        "bi-arrow-counterclockwise text-warning"
                    );
                }

                // Thông báo lên Luồng Thảo luận
                String msgContent = "↩️ [TRƯỞNG DỰ ÁN YÊU CẦU ĐIỀU CHỈNH KẾ HOẠCH]: Trưởng Dự Án yêu cầu trưởng nhóm công việc hoàn thiện lại danh mục nhiệm vụ của công việc [" + task.getTitle() + "]. Lý do: \"" + (feedback != null && !feedback.trim().isEmpty() ? feedback : "Cần phân rã chi tiết hơn") + "\"";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                if (session != null) session.setAttribute("toastSuccess", "Đã trả về kế hoạch phân rã để trưởng nhóm công việc tiếp tục hoàn thiện.");
            } else {
                if (session != null) session.setAttribute("toastError", "Chỉ Trưởng Dự Án mới có quyền đưa ra quyết định này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 13: Trưởng Dự Án (PM) Phê Duyệt Nghiệm Thu Task Lớn ĐẠT (Chuyển sang 🟢 DONE)
     */
    public static void handlePmApproveTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String feedback = request.getParameter("feedback");
        int qualityRating = Math.max(1, Math.min(5, safeParseInt(request.getParameter("qualityRating"), 5)));

        if (projectId <= 0 || taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task != null && project != null && task.getProjectId() == projectId && currentUser != null) {
            // KIỂM SOÁT BẢO MẬT: Chỉ DUY NHẤT Trưởng Dự Án (PM) mới có quyền PHÊ DUYỆT TỐI CAO
            if (isProjectOwner(currentUser, project)) {
                // RÀNG BUỘC CHẤT LƯỢNG NGHIỆM THU: PM chỉ duyệt đạt khi toàn bộ việc con đã đạt 100%
                List<SubTask> subTasks = SubTaskDB.selectByTaskId(taskId);
                int progress = SubTaskDB.calculateProgress(taskId);
                if (subTasks != null && !subTasks.isEmpty() && progress < 100) {
                    if (session != null) {
                        session.setAttribute("toastError", 
                            "⚠️ Không thể duyệt đạt công việc [" + task.getTitle() + "]! Vẫn còn " + (100 - progress) + "% nhiệm vụ chưa được hoàn tất nghiệm thu.");
                    }
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmApproveTask(taskId, feedback, qualityRating, now);

                // Ghi nhận Activity Log
                String approveDesc = "Trưởng dự án đã phê duyệt nghiệm thu (" + qualityRating + " ⭐): " + (feedback != null && !feedback.trim().isEmpty() ? feedback : "Đạt chất lượng xuất sắc!");
                ActivityLogDB.logAsync(projectId, currentUser.getId(), "PM_APPROVE", "TASK", taskId, task.getTitle(), approveDesc);

                // Bắn thông báo thời gian thực 🔔 cho Task Lead
                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "🏆 Trưởng Dự Án Phê Duyệt Nghiệm Thu (" + qualityRating + " ⭐)",
                        "Trưởng Dự Án đã chính thức ký duyệt nghiệm thu hoàn tất 100% và chấm " + qualityRating + " sao cho công việc [" + task.getTitle() + "]!",
                        "/task?action=list&projectId=" + projectId,
                        "bi-trophy-fill text-warning"
                    );
                }

                // Thông báo cúp vàng lên Thảo luận
                String msgContent = "🏆 [TRƯỞNG DỰ ÁN KÝ DUYỆT ĐÓNG CÔNG VIỆC]: Trưởng Dự Án đã nghiệm thu hoàn thành 100% (Đánh giá: " + qualityRating + " ⭐) cho công việc [" + task.getTitle() + "]! Lời nhận xét: \"" + (feedback != null && !feedback.trim().isEmpty() ? feedback : "Đạt chất lượng xuất sắc!") + "\"";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                if (session != null) session.setAttribute("toastSuccess", "Trưởng Dự Án đã phê duyệt nghiệm thu thành công! Công việc đã hoàn tất 100%.");
            } else {
                if (session != null) session.setAttribute("toastError", "Chỉ Trưởng Dự Án mới có thẩm quyền phê duyệt nghiệm thu tối cao!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 14: Trưởng Dự Án (PM) Yêu Cầu Cân Chỉnh Nhỏ (Chuyển sang 🔵 REVISE - Màu Xanh Dương)
     */
    public static void handlePmReviseTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String feedback = request.getParameter("feedback");

        if (projectId <= 0 || taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task != null && project != null && task.getProjectId() == projectId && currentUser != null) {
            if (isProjectOwner(currentUser, project)) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmReviseTask(taskId, feedback, now);

                // Ghi nhận Activity Log
                String reviseDesc = "Trưởng dự án yêu cầu cân chỉnh nhỏ: " + (feedback != null && !feedback.trim().isEmpty() ? feedback : "Cần hoàn thiện thêm chi tiết");
                ActivityLogDB.logAsync(projectId, currentUser.getId(), "PM_REVISE", "TASK", taskId, task.getTitle(), reviseDesc);

                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "🔵 Trưởng Dự Án Yêu Cầu Cân Chỉnh",
                        "Trưởng Dự Án dặn dò: \"" + (feedback != null ? feedback : "Cần cân chỉnh thêm một số chi tiết") + "\" đối với công việc [" + task.getTitle() + "]",
                        "/task?action=list&projectId=" + projectId,
                        "bi-pencil-square text-primary"
                    );
                }

                if (session != null) session.setAttribute("toastSuccess", "Đã gửi yêu cầu cân chỉnh nhỏ (🔵 Xanh Dương) tới trưởng nhóm công việc!");
            } else {
                if (session != null) session.setAttribute("toastError", "Chỉ Trưởng Dự Án mới có quyền đưa ra quyết định này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 15: Trưởng Dự Án (PM) Trả Về Do Chưa Đạt (Chuyển sang 🔴 REJECTED - Màu Đỏ)
     */
    public static void handlePmRejectTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String feedback = request.getParameter("feedback");

        if (projectId <= 0 || taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task != null && project != null && task.getProjectId() == projectId && currentUser != null) {
            if (isProjectOwner(currentUser, project)) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmRejectTask(taskId, feedback, now);

                // Ghi nhận Activity Log
                String rejectDesc = "Trưởng dự án từ chối nghiệm thu: " + (feedback != null && !feedback.trim().isEmpty() ? feedback : "Chưa đạt yêu cầu đề ra");
                ActivityLogDB.logAsync(projectId, currentUser.getId(), "PM_REJECT", "TASK", taskId, task.getTitle(), rejectDesc);

                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "🔴 Trưởng Dự Án Chưa Đạt Yêu Cầu",
                        "Trưởng Dự Án phản hồi lỗi: \"" + (feedback != null ? feedback : "Chưa đạt chuẩn đề ra") + "\" đối với công việc [" + task.getTitle() + "]",
                        "/task?action=list&projectId=" + projectId,
                        "bi-exclamation-triangle-fill text-danger"
                    );
                }

                if (session != null) session.setAttribute("toastSuccess", "Đã trả về công việc lớn và gửi phản hồi (🔴 Màu Đỏ) cho trưởng nhóm công việc!");
            } else {
                if (session != null) session.setAttribute("toastError", "Chỉ Trưởng Dự Án mới có quyền đưa ra quyết định này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }
}
