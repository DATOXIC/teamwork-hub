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
import static com.teamwork.controllers.task.TaskWorkflowHandler.*;

/**
 * Nhóm Việc con (Sub-task): thêm / tick / sửa / xóa và vòng duyệt cấp 1 của Task Lead (submit, approve, revise, reject).
 *
 * <p>Được gọi từ {@link com.teamwork.controllers.TaskServlet} (bộ điều phối duy nhất của URL /task).
 * Tên hàm {@code handleXxx} khớp với tham số {@code action} trong form / fetch của tasks.jsp.</p>
 */
public final class SubTaskHandler {

    private SubTaskHandler() {}

    /** Công việc còn ở giai đoạn lập kế hoạch (TODO hoặc đang chờ PM duyệt kế hoạch), chưa được khóa phạm vi. */
    static boolean isBeforePlanLock(Task task) {
        return task != null
                && ("TODO".equalsIgnoreCase(task.getStatus()) || "PLANNING".equalsIgnoreCase(task.getStatus()));
    }

    /**
     * Nghiệp vụ 5: Thêm Việc Con (Sub-task) mới và phân công cho thành viên (BẢO VỆ PHÂN QUYỀN TASK LEAD / PM)
     */
    public static void handleAddSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String title = request.getParameter("title");
        String dueDateParam = request.getParameter("dueDate");
        String subDueDate = (dueDateParam != null) ? dueDateParam.trim() : "";

        if (projectId <= 0 || taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task parentTask = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        
        if (parentTask == null || project == null || parentTask.getProjectId() != projectId) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        int assigneeId = safeParseInt(request.getParameter("assigneeId"), 0);
        String assigneeName = "Chưa phân công";
        if (project.isSoloProject()) {
            assigneeId = currentUser != null ? currentUser.getId() : 0;
            assigneeName = currentUser != null ? currentUser.getFullName() : "Chưa phân công";
        } else if (assigneeId > 0 && ProjectMemberDB.isMember(projectId, assigneeId)) {
            User u = UserDB.selectById(assigneeId);
            if (u != null) {
                assigneeName = u.getFullName();
            } else {
                assigneeId = 0;
            }
        } else {
            assigneeId = 0;
        }

        HttpSession session = request.getSession(false);

        // KIỂM SOÁT THẨM QUYỀN & KHÓA PHẠM VI (SCOPE LOCK):
        // 1. Chỉ Task Lead của chính Task này HOẶC Trưởng Dự Án mới được thêm việc con.
        // 2. Chỉ được thêm việc con khi Task đang ở trạng thái TODO (Giai đoạn Lập Kế Hoạch). Khi đã trình PM hoặc đã khóa thì không được thêm tự do.
        if (isTaskLead(currentUser, parentTask) || isProjectOwner(currentUser, project)) {
            // Cho phép thêm việc con khi Task đang ở TODO hoặc IN_PROGRESS (phục vụ phát sinh việc con)
            // Khóa lại khi Task đã nộp nghiệm thu (SUBMITTED, REVISE, REJECTED, DONE)
            if (!"TODO".equalsIgnoreCase(parentTask.getStatus()) && !"IN_PROGRESS".equalsIgnoreCase(parentTask.getStatus())) {
                if (session != null) {
                    // ▶ JSP: docs.jsp đọc bằng ${toastError}
                    session.setAttribute("toastError", 
                        "⚠️ Công việc này đã nộp hoặc hoàn tất nghiệm thu, không thể thêm nhiệm vụ mới!");
                }
                response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                return;
            }

            // RÀNG BUỘC BẤT BIẾN CÂY PHÂN CẤP (HIERARCHICAL DEADLINE INVARIANT):
            // Hạn chót của việc con không được vượt quá Hạn chót của Task cha
            if (!subDueDate.isEmpty() && parentTask.getDueDate() != null && !parentTask.getDueDate().trim().isEmpty()) {
                try {
                    LocalDate subDate = LocalDate.parse(subDueDate);
                    LocalDate parentDate = LocalDate.parse(parentTask.getDueDate().trim());
                    if (subDate.isAfter(parentDate)) {
                        if (session != null) {
                            session.setAttribute("toastError", 
                                "Nhiệm vụ phải được hoàn thành trước hạn chót của công việc lớn (" + parentTask.getDueDate().trim() + ")!");
                        }
                        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                        return;
                    }
                } catch (Exception ignored) {
                    // Nếu lỗi định dạng ngày, tiếp tục lưu dạng chuỗi an toàn
                }
            }

            if (title != null && !title.trim().isEmpty()) {
                SubTask newSubTask = new SubTask(
                    0, 
                    taskId, 
                    title.trim(), 
                    assigneeId, 
                    assigneeName, 
                    "TODO", 
                    subDueDate, 
                    "", 
                    "", 
                    "", 
                    ""
                );
                SubTaskDB.insert(newSubTask);
                if (session != null) session.setAttribute("toastSuccess", "Đã thêm nhiệm vụ vào kế hoạch phân rã thành công!");
            }
        } else {
            if (session != null) session.setAttribute("toastError", "Bạn không có quyền phân rã nhiệm vụ cho công việc này!");
        }

        // Áp dụng PRG: Redirect về lại bảng Kanban
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 6: Đổi trạng thái hoàn thành [☑] của Việc Con (SubTask) (BẢO VỆ PHÂN QUYỀN 3 BÊN)
     * Kiểm tra các sub task của một task lớn đã hoàn thành xong hết chưa, nếu xong hết rồi thì chuyển cái Task lớn sang trạng thái Done đúng không.
     */
    public static void handleToggleSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int subTaskId = safeParseInt(request.getParameter("subTaskId"), 0);
        boolean isCompleted = Boolean.parseBoolean(request.getParameter("completed"));
        boolean isAjax = isAjaxRequest(request);

        if (projectId <= 0 || subTaskId <= 0) {
            if (isAjax) {
                sendJsonResponse(response, false, "Dữ liệu yêu cầu không hợp lệ!", null);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }
        
        // Xác định User và SubTask
        User currentUser = getCurrentUser(request);
        SubTask st = SubTaskDB.selectById(subTaskId);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && "DONE".equalsIgnoreCase(parentTask.getStatus())) {
                String errMsg = "🔒 Công việc [" + parentTask.getTitle() + "] đã hoàn thành và được khóa, không thể thay đổi nhiệm vụ!";
                if (isAjax) {
                    sendJsonResponse(response, false, errMsg, null);
                    return;
                }
                HttpSession session = request.getSession(false);
                if (session != null) session.setAttribute("toastError", errMsg);
                response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                return;
            }

            if (parentTask != null && project != null && parentTask.getProjectId() == projectId && canManageSubTask(currentUser, st, parentTask, project)) {
                // Xác định chế độ TRƯỚC khi ghi — cờ này quyết định ô tick mang ý nghĩa gì.
                boolean isGateEnforced = (project.isTeamProject() && parentTask.isRequiresGate());

                // Cổng 1: công việc có duyệt chỉ được thực thi (tick / nộp nhiệm vụ) SAU khi PM đã khóa kế hoạch.
                // Trước đó (TODO / PLANNING) việc tick sẽ lén chuyển công việc sang Đang làm, bỏ qua bước duyệt kế hoạch.
                if (isGateEnforced && isCompleted && isBeforePlanLock(parentTask)) {
                    String errMsg = "🛡️ Kế hoạch của công việc [" + parentTask.getTitle()
                            + "] chưa được trưởng dự án duyệt. Hãy bấm 'Gửi duyệt kế hoạch' trong chi tiết công việc trước khi thực hiện nhiệm vụ.";
                    if (isAjax) {
                        sendJsonResponse(response, false, errMsg, null);
                        return;
                    }
                    HttpSession gateSession = request.getSession(false);
                    if (gateSession != null) gateSession.setAttribute("toastError", errMsg);
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                // 1. Ghi trạng thái theo đúng chế độ của dự án:
                //    - Quality Gate (nhóm) : tick = NỘP BÀI  -> "SUBMITTED", chờ Task Lead nghiệm thu.
                //    - Fast-track / Solo   : tick = XONG HẲN -> "DONE", không ai phải duyệt.
                //    Người làm KHÔNG bao giờ tự ghi được "APPROVED" — đó là đặc quyền của Task Lead.
                String targetStatus = isCompleted ? (isGateEnforced ? "SUBMITTED" : "DONE") : "TODO";
                SubTaskDB.updateStatus(subTaskId, targetStatus);

                int newProgress = SubTaskDB.calculateProgress(st.getTaskId());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                // 2. CƠ CHẾ TỰ ĐỘNG CHUYỂN CỘT KANBAN CHO TASK LỚN:
                if (!isGateEnforced) {
                    // Chế độ Fast-track (Tự do / Solo): Hoàn tất 100% subtask thì tự động hoàn thành Task
                    if (newProgress == 100 && !"DONE".equals(parentTask.getStatus())) 
                    {
                        TaskDB.updateStatus(parentTask.getId(), "DONE");
                        String celebrationText = "🏆 CHÚC MỪNG: Tất cả nhiệm vụ đã hoàn tất (100%)! Thẻ công việc [" + parentTask.getTitle() + "] đã tự động chuyển sang trạng thái ĐÃ XONG!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", celebrationText, now));
                    } 
                    else if (newProgress > 0 && newProgress < 100 && "TODO".equals(parentTask.getStatus())) 
                    {
                        TaskDB.updateStatus(parentTask.getId(), "IN_PROGRESS");
                        String progressText = "🚀 BẮT ĐẦU THỰC HIỆN: Đã hoàn thành " + newProgress + "% nhiệm vụ. Công việc [" + parentTask.getTitle() + "] đã tự động chuyển sang ĐANG LÀM!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", progressText, now));
                    } 
                    else if (newProgress < 100 && "DONE".equals(parentTask.getStatus())) 
                    {
                        TaskDB.updateStatus(parentTask.getId(), "IN_PROGRESS");
                        String reopenText = "⚠️ CẬP NHẬT: Còn nhiệm vụ chưa xong (" + newProgress + "%). Công việc [" + parentTask.getTitle() + "] đã được mở lại sang ĐANG LÀM!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", reopenText, now));
                    }
                } else {
                    // Chế độ Quality Gate (Nhóm): việc con vừa chuyển sang SUBMITTED nên CHƯA tính vào tiến độ.
                    // Tiến độ chỉ tăng khi Task Lead duyệt (APPROVED) — xem handleApproveSubTask.
                    // Trạng thái công việc cha KHÔNG đổi ở đây: chuyển sang Đang làm là việc của PM khi duyệt kế hoạch.
                }

                // 3. Ghi nhận lên Luồng Thảo Luận (nội dung khác nhau theo chế độ)
                if (isCompleted) {
                    String notificationText = isGateEnforced
                        ? "📤 " + st.getAssigneeName() + " vừa nộp nhiệm vụ: [" + st.getTitle() + "] — Đang chờ trưởng nhóm công việc nghiệm thu."
                        : "🎉 " + st.getAssigneeName() + " vừa hoàn thành nhiệm vụ: [" + st.getTitle() + "] — Đóng góp đưa tiến độ công việc lên " + newProgress + "%!";
                    Message systemMessage = new Message(0, projectId, st.getTaskId(), 0, "Hệ Thống", notificationText, now);
                    MessageDB.insert(systemMessage);

                    // 4. Bắn thông báo 🔔 cho Task Lead khi Assignee đánh dấu hoàn thành (chế độ Quality Gate)
                    // Để Task Lead biết cần vào duyệt nghiệm thu, tránh việc con "trôi" mà không ai kiểm duyệt.
                    if (isGateEnforced && parentTask.getAssigneeId() > 0 && parentTask.getAssigneeId() != currentUser.getId()) {
                        NotificationDB.send(
                            parentTask.getAssigneeId(),
                            "📋 Cần duyệt nhiệm vụ",
                            st.getAssigneeName() + " vừa nộp nhiệm vụ [" + st.getTitle() + "]. Hãy vào kiểm tra và duyệt nghiệm thu!",
                            "/task?action=list&projectId=" + projectId,
                            "bi-clipboard-check text-warning"
                        );
                    }
                }

                if (isAjax) {
                    String msg = isCompleted
                            ? (isGateEnforced ? "Đã nộp nhiệm vụ — chờ trưởng nhóm công việc duyệt nghiệm thu!" : "Đã đánh dấu hoàn thành nhiệm vụ!")
                            : "Đã chuyển nhiệm vụ về cần làm.";
                    sendJsonResponse(response, true, msg, "{\"subTaskId\":" + subTaskId + ",\"parentTaskId\":" + st.getTaskId() + ",\"isCompleted\":" + isCompleted + ",\"newProgress\":" + newProgress + ",\"parentStatus\":\"" + escapeJson(parentTask.getStatus()) + "\"}");
                    return;
                }
            } else if (isAjax) {
                sendJsonResponse(response, false, "Bạn không có quyền thao tác trên nhiệm vụ này!", null);
                return;
            }
        } else if (isAjax) {
            sendJsonResponse(response, false, "Không tìm thấy nhiệm vụ tương ứng!", null);
            return;
        }

        // Áp dụng PRG: Redirect về lại bảng Kanban
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 7: Xóa một việc con (BẢO VỆ PHÂN QUYỀN TASK LEAD / PM & KHÓA PHẠM VI)
     */
    public static void handleDeleteSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int subTaskId = safeParseInt(request.getParameter("subTaskId"), 0);

        if (projectId <= 0 || subTaskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        SubTask st = SubTaskDB.selectById(subTaskId);
        HttpSession session = request.getSession(false);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null && parentTask.getProjectId() == projectId) {
                // RÀNG BUỘC KHÓA PHẠM VI: Không được xóa khi đã trình PM hoặc đã khóa
                if (!"TODO".equalsIgnoreCase(parentTask.getStatus())) {
                    if (session != null) {
                        session.setAttribute("toastError", 
                            "⚠️ Kế hoạch phân rã đã được trình trưởng dự án hoặc đã khóa. Không thể xóa nhiệm vụ!");
                    }
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                // KIỂM SOÁT THẨM QUYỀN: Chỉ Task Lead của chính Task này HOẶC Trưởng Dự Án mới được xóa việc con
                if (isTaskLead(currentUser, parentTask) || isProjectOwner(currentUser, project)) {
                    SubTaskDB.delete(subTaskId);
                    if (session != null) session.setAttribute("toastSuccess", "Đã xóa nhiệm vụ khỏi kế hoạch phân rã!");
                } else {
                    if (session != null) session.setAttribute("toastError", "Bạn không có quyền xóa nhiệm vụ này!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 7.2: Chỉnh sửa thông tin việc con (Sub-task)
     * Thẩm quyền: Trưởng Dự Án (PM) hoặc Trưởng Nhóm Task (Task Lead)
     */
    public static void handleEditSubTask(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int subTaskId = safeParseInt(request.getParameter("subTaskId"), 0);
        String title = request.getParameter("title");
        String assigneeIdParam = request.getParameter("assigneeId");
        String dueDate = request.getParameter("dueDate");

        SubTask subTask = SubTaskDB.selectById(subTaskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (subTask == null || project == null) {
            if (session != null) session.setAttribute("toastError", "Không tìm thấy nhiệm vụ cần sửa!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        Task parentTask = TaskDB.selectById(subTask.getTaskId());
        if (parentTask == null || parentTask.getProjectId() != projectId) {
            if (session != null) session.setAttribute("toastError", "Không tìm thấy công việc cha thuộc dự án!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // RÀNG BUỘC KHÓA BẤT BIẾN: KHÔNG THỂ SỬA VIỆC CON KHI TASK CHA ĐÃ DONE
        if ("DONE".equalsIgnoreCase(parentTask.getStatus())) {
            if (session != null) session.setAttribute("toastError", "🔒 Công việc [" + parentTask.getTitle() + "] đã hoàn thành và được khóa, không thể sửa nhiệm vụ!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Kiểm tra thẩm quyền: PM hoặc Task Lead của parentTask
        if (!isProjectOwner(currentUser, project) && !isTaskLead(currentUser, parentTask)) {
            if (session != null) session.setAttribute("toastError", "Chỉ Trưởng Dự Án hoặc trưởng nhóm công việc mới có quyền sửa nhiệm vụ!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Validate tiêu đề
        if (title == null || title.trim().isEmpty()) {
            if (session != null) session.setAttribute("toastError", "Tiêu đề nhiệm vụ không được để trống!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        subTask.setTitle(title.trim());
        subTask.setDueDate(dueDate != null ? dueDate.trim() : "");

        // Cập nhật người phụ trách việc con
        if (assigneeIdParam != null && !assigneeIdParam.trim().isEmpty()) {
            int newAssigneeId = safeParseInt(assigneeIdParam, 0);
            if (newAssigneeId > 0 && ProjectMemberDB.isMember(projectId, newAssigneeId)) {
                User newAssignee = UserDB.selectById(newAssigneeId);
                if (newAssignee != null) {
                    subTask.setAssigneeId(newAssigneeId);
                    subTask.setAssigneeName(newAssignee.getFullName());
                }
            }
        }

        SubTaskDB.update(subTask);

        if (session != null) {
            // ▶ JSP: docs.jsp, tasks.jsp đọc bằng ${toastSuccess}
            session.setAttribute("toastSuccess", "Đã cập nhật thông tin nhiệm vụ [" + subTask.getTitle() + "] thành công!");
        }
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 8: Thành viên Nộp Báo Cáo / Kết Quả Việc Con (Chuyển sang 🟡 SUBMITTED)
     * KÈM BẮN THÔNG BÁO CHO TASK LEAD
     */
    public static void handleSubmitSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int subTaskId = safeParseInt(request.getParameter("subTaskId"), 0);
        String submissionNote = request.getParameter("submissionNote");

        if (projectId <= 0 || subTaskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        SubTask st = SubTaskDB.selectById(subTaskId);
        HttpSession session = request.getSession(false);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null && parentTask.getProjectId() == projectId) {
                if (project.isTeamProject() && parentTask.isRequiresGate() && isBeforePlanLock(parentTask)) {
                    if (session != null) {
                        session.setAttribute("toastError", "🛡️ Kế hoạch của công việc [" + parentTask.getTitle()
                                + "] chưa được trưởng dự án duyệt nên chưa thể nộp kết quả nhiệm vụ.");
                    }
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }
                // KIỂM SOÁT THẨM QUYỀN NGHIỆM THU TẦNG 1:
                // 1. Nếu việc con đã gán cho ai (assigneeId > 0): CHỈ chính thành viên đó mới được nộp kết quả.
                // 2. Nếu việc con chưa gán cho ai (assigneeId == 0): Task Lead hoặc PM có thể nộp.
                boolean isAssignedMember = isSubTaskAssignee(currentUser, st);
                boolean isUnassignedAndLeadOrOwner = (st.getAssigneeId() == 0 && (isTaskLead(currentUser, parentTask) || isProjectOwner(currentUser, project)));

                if (isAssignedMember || isUnassignedAndLeadOrOwner) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String now = LocalDateTime.now().format(formatter);

                    SubTaskDB.submitDeliverable(subTaskId, submissionNote, now);

                    // Bắn thông báo thời gian thực 🔔 cho Task Lead (nếu Task Lead không phải chính mình)
                    int leadId = parentTask.getAssigneeId();
                    if (leadId > 0 && leadId != currentUser.getId()) {
                        NotificationDB.send(
                            leadId,
                            "🟡 Báo cáo nộp nhiệm vụ",
                            currentUser.getFullName() + " vừa nộp kết quả nhiệm vụ [" + st.getTitle() + "], mời bạn nghiệm thu!",
                            "/task?action=list&projectId=" + projectId,
                            "bi-hourglass-split text-warning"
                        );
                    }

                    // Thông báo lên Luồng Thảo luận
                    String msgContent = "📤 " + currentUser.getFullName() + " vừa nộp kết quả nhiệm vụ: [" + st.getTitle() + "] — Ghi chú: \"" + (submissionNote != null && !submissionNote.trim().isEmpty() ? submissionNote : "Đã hoàn tất công việc") + "\"";
                    MessageDB.insert(new Message(0, projectId, st.getTaskId(), 0, "Hệ Thống", msgContent, now));

                    if (session != null) session.setAttribute("toastSuccess", "Đã nộp báo cáo kết quả nhiệm vụ thành công! Đang chờ trưởng nhóm công việc duyệt.");
                } else {
                    if (session != null) session.setAttribute("toastError", "Bạn không có quyền nộp bài cho nhiệm vụ của người khác!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 9: Task Lead Duyệt Nghiệm Thu ĐẠT (Chuyển sang 🟢 APPROVED)
     * KÈM CƠ CHẾ DOMINO TỰ ĐỘNG NÂNG % TIẾN ĐỘ VÀ BAY SANG CỘT DONE
     */
    public static void handleApproveSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int subTaskId = safeParseInt(request.getParameter("subTaskId"), 0);

        if (projectId <= 0 || subTaskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        SubTask st = SubTaskDB.selectById(subTaskId);
        HttpSession session = request.getSession(false);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null && parentTask.getProjectId() == projectId) {
                // KIỂM SOÁT BẢO MẬT PHÂN TẦNG NGHIỆM THU:
                // Thẩm quyền duyệt việc con (Tầng 1) thuộc về TRƯỞNG NHÓM TASK (Task Lead) của Task này.
                // Nếu Task lớn chưa phân công (assigneeId == 0), PM mới được tạm quyền duyệt.
                if (canReviewSubTask(currentUser, parentTask, project)) 
                {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String now = LocalDateTime.now().format(formatter);

                    SubTaskDB.approveDeliverable(subTaskId, now);

                    int newProgress = SubTaskDB.calculateProgress(st.getTaskId());

                    // CƠ CHẾ DOMINO TỰ ĐỘNG CHUYỂN CỘT KANBAN:
                    // Chỉ auto-complete khi TẤT CẢ subtask đều APPROVED (không chỉ DONE)
                    boolean allApproved = SubTaskDB.areAllSubtasksApproved(st.getTaskId());
                    boolean gateEnforced = project.isTeamProject() && parentTask.isRequiresGate();
                    if (gateEnforced) {
                        // Công việc có duyệt: Task Lead duyệt xong nhiệm vụ cuối KHÔNG được tự đóng công việc cha.
                        // Task Lead phải "Bàn giao" và Trưởng Dự Án nghiệm thu (chấm sao) mới sang DONE.
                        if (allApproved && "IN_PROGRESS".equals(parentTask.getStatus())) {
                            String readyText = "✅ Tất cả nhiệm vụ của công việc [" + parentTask.getTitle()
                                    + "] đã được duyệt (100%). Trưởng nhóm công việc có thể bấm 'Bàn Giao Cho Trưởng Dự Án' để nghiệm thu.";
                            MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", readyText, now));
                        }
                    }
                    else if (allApproved && !"DONE".equals(parentTask.getStatus())) {
                        TaskDB.updateStatus(parentTask.getId(), "DONE");
                        String celebrationText = "🏆 CHÚC MỪNG TOÀN ĐỘI: Tất cả nhiệm vụ đã được duyệt nghiệm thu ĐẠT (100%)! Thẻ công việc [" + parentTask.getTitle() + "] đã tự động chuyển sang trạng thái ĐÃ XONG!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", celebrationText, now));
                    } 
                    else if (newProgress > 0 && newProgress < 100 && "TODO".equals(parentTask.getStatus())) {
                        TaskDB.updateStatus(parentTask.getId(), "IN_PROGRESS");
                        String progressText = "🚀 BẮT ĐẦU THỰC HIỆN: Đã nghiệm thu " + newProgress + "% nhiệm vụ. Công việc [" + parentTask.getTitle() + "] chuyển sang ĐANG LÀM!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", progressText, now));
                    }

                    // Bắn thông báo thời gian thực 🔔 cho Người phụ trách việc con
                    if (st.getAssigneeId() > 0 && st.getAssigneeId() != currentUser.getId()) {
                        NotificationDB.send(
                            st.getAssigneeId(),
                            "🟢 Nghiệm thu ĐẠT",
                            "Nhiệm vụ [" + st.getTitle() + "] của bạn đã được Leader duyệt đạt 100%!",
                            "/task?action=list&projectId=" + projectId,
                            "bi-check-circle-fill text-success"
                        );
                    }

                    if (session != null) session.setAttribute("toastSuccess", "Đã duyệt nghiệm thu ĐẠT cho nhiệm vụ!");
                } else {
                    if (session != null) session.setAttribute("toastError", "Thẩm quyền thẩm định nhiệm vụ thuộc về Trưởng nhóm công việc của công việc này!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 10: Task Lead Yêu Cầu Cân Chỉnh Nhỏ (Chuyển sang 🔵 REVISE - Màu Xanh Dương)
     */
    public static void handleReviseSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int subTaskId = safeParseInt(request.getParameter("subTaskId"), 0);
        String feedbackNote = request.getParameter("feedbackNote");

        if (projectId <= 0 || subTaskId <= 0) 
        {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        SubTask st = SubTaskDB.selectById(subTaskId);
        HttpSession session = request.getSession(false);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null && parentTask.getProjectId() == projectId) {
                if (canReviewSubTask(currentUser, parentTask, project)) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String now = LocalDateTime.now().format(formatter);

                    SubTaskDB.reviseDeliverable(subTaskId, feedbackNote, now);

                    // Bắn thông báo thời gian thực 🔔 cho Thành viên phụ trách
                    if (st.getAssigneeId() > 0 && st.getAssigneeId() != currentUser.getId()) {
                        NotificationDB.send(
                            st.getAssigneeId(),
                            "🔵 Yêu cầu cân chỉnh nhiệm vụ",
                            "Leader dặn dò: \"" + (feedbackNote != null ? feedbackNote : "Cần cân chỉnh một số chi tiết") + "\" đối với nhiệm vụ [" + st.getTitle() + "]",
                            "/task?action=list&projectId=" + projectId,
                            "bi-pencil-square text-primary"
                        );
                    }

                    if (session != null) session.setAttribute("toastSuccess", "Đã gửi yêu cầu cân chỉnh nhỏ (🔵 Xanh Dương) tới thành viên!");
                } else {
                    if (session != null) session.setAttribute("toastError", "Thẩm quyền thẩm định nhiệm vụ thuộc về Trưởng nhóm công việc của công việc này!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 11: Task Lead Trả Về Do Chưa Đạt Yêu Cầu (Chuyển sang 🔴 REJECTED - Màu Đỏ)
     */
    public static void handleRejectSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int subTaskId = safeParseInt(request.getParameter("subTaskId"), 0);
        String feedbackNote = request.getParameter("feedbackNote");

        if (projectId <= 0 || subTaskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        SubTask st = SubTaskDB.selectById(subTaskId);
        HttpSession session = request.getSession(false);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null && parentTask.getProjectId() == projectId) {
                if (canReviewSubTask(currentUser, parentTask, project)) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String now = LocalDateTime.now().format(formatter);

                    SubTaskDB.rejectDeliverable(subTaskId, feedbackNote, now);

                    // Bắn thông báo thời gian thực 🔔 cho Thành viên phụ trách
                    if (st.getAssigneeId() > 0 && st.getAssigneeId() != currentUser.getId()) {
                        NotificationDB.send(
                            st.getAssigneeId(),
                            "🔴 Nhiệm vụ chưa đạt yêu cầu",
                            "Leader phản hồi lỗi: \"" + (feedbackNote != null ? feedbackNote : "Chưa đạt yêu cầu đề ra") + "\" đối với nhiệm vụ [" + st.getTitle() + "]",
                            "/task?action=list&projectId=" + projectId,
                            "bi-exclamation-triangle-fill text-danger"
                        );
                    }

                    if (session != null) session.setAttribute("toastSuccess", "Đã trả về nhiệm vụ và gửi phản hồi (🔴 Màu Đỏ) cho thành viên!");
                } else {
                    if (session != null) session.setAttribute("toastError", "Thẩm quyền thẩm định nhiệm vụ thuộc về Trưởng nhóm công việc của công việc này!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ AJAX: Tạo nhanh Việc con (Subtask) trực tiếp dưới Task cha (List View)
     */
    public static void handleQuickAddSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String title = request.getParameter("title");
        String dueDateParam = request.getParameter("dueDate");
        String subDueDate = (dueDateParam != null) ? dueDateParam.trim() : "";
        int assigneeId = safeParseInt(request.getParameter("assigneeId"), 0);

        if (projectId <= 0 || taskId <= 0 || title == null || title.trim().isEmpty()) {
            sendJsonResponse(response, false, "Tiêu đề nhiệm vụ không được để trống!", null);
            return;
        }

        User currentUser = getCurrentUser(request);
        if (currentUser == null || !ProjectMemberDB.isMember(projectId, currentUser.getId())) {
            sendJsonResponse(response, false, "Bạn không có quyền thao tác trong dự án này!", null);
            return;
        }

        Task parentTask = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        if (parentTask == null || parentTask.getProjectId() != projectId) {
            sendJsonResponse(response, false, "Không tìm thấy công việc cha!", null);
            return;
        }

        if (!isTaskLead(currentUser, parentTask) && !isProjectOwner(currentUser, project)) {
            sendJsonResponse(response, false, "Bạn không có quyền phân rã nhiệm vụ cho công việc này!", null);
            return;
        }

        if (!"TODO".equalsIgnoreCase(parentTask.getStatus()) && !"IN_PROGRESS".equalsIgnoreCase(parentTask.getStatus())) {
            sendJsonResponse(response, false, "Công việc này đã nộp hoặc hoàn tất nghiệm thu, không thể thêm nhiệm vụ mới!", null);
            return;
        }

        String assigneeName = "Chưa phân công";
        if (project.isSoloProject()) {
            assigneeId = currentUser.getId();
            assigneeName = currentUser.getFullName();
        } else if (assigneeId > 0 && ProjectMemberDB.isMember(projectId, assigneeId)) {
            User u = UserDB.selectById(assigneeId);
            if (u != null) {
                assigneeName = u.getFullName();
            } else {
                assigneeId = 0;
            }
        } else {
            assigneeId = 0;
        }

        SubTask newSubTask = new SubTask(
            0,
            taskId,
            title.trim(),
            assigneeId,
            assigneeName,
            "TODO",
            subDueDate,
            "",
            "",
            "",
            ""
        );
        int subId = SubTaskDB.insert(newSubTask);
        if (subId <= 0) {
            sendJsonResponse(response, false, "Lỗi khi lưu nhiệm vụ vào cơ sở dữ liệu!", null);
            return;
        }
        newSubTask.setId(subId);

        // Ghi nhận Activity Log
        ActivityLogDB.logAsync(projectId, currentUser.getId(), "SUBTASK_CREATE", "SUBTASK", subId, newSubTask.getTitle(), "Thêm nhiệm vụ: " + newSubTask.getTitle() + " cho công việc #" + taskId);

        String dataJson = String.format(
            "{\"id\":%d,\"taskId\":%d,\"title\":\"%s\",\"status\":\"%s\",\"dueDate\":\"%s\",\"assigneeId\":%d,\"assigneeName\":\"%s\"}",
            newSubTask.getId(),
            newSubTask.getTaskId(),
            escapeJson(newSubTask.getTitle()),
            escapeJson(newSubTask.getStatus()),
            escapeJson(newSubTask.getDueDate()),
            newSubTask.getAssigneeId(),
            escapeJson(newSubTask.getAssigneeName())
        );

        sendJsonResponse(response, true, "Đã thêm nhiệm vụ thành công!", dataJson);
    }
}
