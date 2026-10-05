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
import static com.teamwork.controllers.task.SubTaskHandler.*;
import static com.teamwork.controllers.task.TaskWorkflowHandler.*;

/**
 * Nhóm Task cha: thêm (add), đổi trạng thái kéo-thả (updateStatus), sửa (editTask), quick-add, tạo nhãn (createLabel).
 *
 * <p>Được gọi từ {@link com.teamwork.controllers.TaskServlet} (bộ điều phối duy nhất của URL /task).
 * Tên hàm {@code handleXxx} khớp với tham số {@code action} trong form / fetch của tasks.jsp.</p>
 */
public final class TaskCrudHandler {

    private TaskCrudHandler() {}


    /**
     * Ba trạng thái DUY NHẤT mà thao tác kéo-thả / đổi trạng thái trên bảng Kanban được phép đặt.
     * Các trạng thái thuộc quy trình nghiệm thu (PLANNING, SUBMITTED, REVISE, REJECTED, APPROVED)
     * chỉ được đặt bởi handler chuyên trách của chúng, không nhận từ tham số request.
     */
    public static final Set<String> BOARD_STATUS = Set.of("TODO", "IN_PROGRESS", "DONE");

    /**
     * Nghiệp vụ 3: Tiếp nhận form thêm task mới và lưu liên kết tài liệu đính kèm vào TaskDocDB
     */
    public static void handleAddTask(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        if (projectId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String priority = request.getParameter("priority");
        String dueDate = request.getParameter("dueDate");
        String[] selectedDocIds = request.getParameterValues("docIds");

        HttpSession session = request.getSession(false);

        if (title == null || title.trim().isEmpty()) {
            if (session != null) {
                // ▶ JSP: docs.jsp đọc bằng ${toastError}
                session.setAttribute("toastError", "Tiêu đề công việc không được để trống!");
            }
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        if (!"LOW".equalsIgnoreCase(priority) && !"HIGH".equalsIgnoreCase(priority)) 
        {
            priority = "MEDIUM";
        } 
        else 
        {
            priority = priority.toUpperCase();
        }

        int assigneeId = safeParseInt(request.getParameter("assigneeId"), 0);
        String assigneeName = "";
        boolean assigneeProvided = assigneeId > 0; // Ghi nhớ người dùng có chọn ai chưa

        if (assigneeId > 0 && ProjectMemberDB.isMember(projectId, assigneeId)) 
        {
            User assignee = UserDB.selectById(assigneeId);
            if (assignee != null) 
            {
                assigneeName = assignee.getFullName();
            } 
            else 
            {
                assigneeId = 0;
            }
        } else {
            assigneeId = 0;
        }

        // TASK-02: Phân biệt rõ hai trường hợp lỗi để thông báo chính xác hơn
        if (assigneeId <= 0) {
            if (session != null) {
                if (assigneeProvided) {
                    // Người dùng đã chọn nhưng người đó không còn là thành viên dự án
                    session.setAttribute("toastError", "Người phụ trách được chọn không còn là thành viên của dự án này! Vui lòng chọn lại.");
                } else {
                    // Người dùng chưa chọn ai
                    session.setAttribute("toastError", "Vui lòng chọn người phụ trách trong danh sách thành viên dự án!");
                }
            }
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Mọi công việc mới tạo bắt buộc bắt đầu từ TO DO theo chuẩn quy trình ClickUp/Agile
        String status = "TODO";

        Task newTask = new Task(
            0,
            projectId,
            title.trim(),
            (description != null ? description.trim() : ""),
            status,
            priority,
            (dueDate != null ? dueDate.trim() : ""),
            assigneeId,
            assigneeName
        );

        String labels = request.getParameter("labels");
        newTask.setLabels(labels != null ? labels.trim() : "");

        Project curPrj = ProjectDB.selectById(projectId);
        if (curPrj != null && curPrj.isSoloProject()) {
            User curUser = getCurrentUser(request);
            if (curUser != null) {
                newTask.setAssigneeId(curUser.getId());
                newTask.setAssigneeName(curUser.getFullName());
            }
            newTask.setRequiresGate(false);
        } else {
            boolean defaultRequiresGate = (curPrj != null && curPrj.isTeamProject());
            String reqGateParam = request.getParameter("requiresGate");
            if (reqGateParam != null) {
                newTask.setRequiresGate("true".equalsIgnoreCase(reqGateParam.trim()) || "on".equalsIgnoreCase(reqGateParam.trim()) || "1".equals(reqGateParam.trim()));
            } else {
                // Nếu form có cờ hasRequiresGateControl nhưng checkbox không được tick
                String hasControl = request.getParameter("hasRequiresGateControl");
                if (hasControl != null && !hasControl.trim().isEmpty()) {
                    newTask.setRequiresGate(false);
                } else {
                    newTask.setRequiresGate(defaultRequiresGate);
                }
            }
        }

        int newTaskId = TaskDB.insert(newTask);

        // Ghi nhận Activity Log
        User curUser = getCurrentUser(request);
        int curUserId = curUser != null ? curUser.getId() : 0;
        ActivityLogDB.logAsync(projectId, curUserId, "TASK_CREATE", "TASK", newTaskId, newTask.getTitle(), "Tạo công việc mới: " + newTask.getTitle());

        if (selectedDocIds != null && selectedDocIds.length > 0) {
            for (String docIdStr : selectedDocIds) {
                int docId = safeParseInt(docIdStr, 0);
                if (docId > 0) {
                    Doc doc = DocDB.selectById(docId);
                    if (doc != null) {
                        TaskDocDB.insert(newTaskId, docId, doc.getTitle());
                    }
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 4: Cập nhật trạng thái Task khi người dùng kéo thả
     */
    public static void handleUpdateTaskStatus(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String newStatus = request.getParameter("newStatus");
        if (newStatus == null || newStatus.trim().isEmpty()) {
            newStatus = request.getParameter("status");
        }
        boolean isAjax = isAjaxRequest(request);

        if (taskId > 0 && projectId > 0 && newStatus != null && !newStatus.trim().isEmpty()) {
            Task task = TaskDB.selectById(taskId);
            if (task != null && task.getProjectId() == projectId) {
                String status = newStatus.trim().toUpperCase();
                HttpSession session = request.getSession(false);

                // CHỐT 0 — LỌC TRẠNG THÁI ĐẦU VÀO:
                // Thiếu chốt này, request gửi "newStatus=APPROVED" sẽ đi xuyên qua Cổng 2 bên dưới
                // (vì Cổng 2 chỉ so sánh với chuỗi "DONE"), trong khi giao diện lại xếp APPROVED
                // vào cột Đã xong với nhãn "Đã nghiệm thu" — tức hoàn tất task mà không qua PM.
                if (!BOARD_STATUS.contains(status)) {
                    String errMsg = "Trạng thái [" + status + "] không hợp lệ cho thao tác trên bảng công việc!";
                    if (isAjax) {
                        sendJsonResponse(response, false, errMsg, null);
                        return;
                    }
                    if (session != null) session.setAttribute("toastError", errMsg);
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                Project currentPrj = ProjectDB.selectById(projectId);

                // CHỐT 1 — THẨM QUYỀN TRÊN TỪNG CÔNG VIỆC:
                // Trước đây hàm này KHÔNG kiểm tra quyền (handler duy nhất trong TaskServlet bị thiếu),
                // nên mọi thành viên dự án đều kéo-thả / đổi được trạng thái task của người khác.
                // doPost() chỉ chặn tới mức "có phải thành viên dự án không", chưa xét quyền trên task.
                // Quy ước lấy theo handleEditTask và handleDeleteTask: PM hoặc Task Lead của chính task đó.
                User currentUser = getCurrentUser(request);
                boolean isPm = isProjectOwner(currentUser, currentPrj);
                boolean isLead = isTaskLead(currentUser, task);

                if (!isPm && !isLead) {
                    String errMsg = "Bạn không có quyền đổi trạng thái công việc [" + task.getTitle()
                            + "]! Chỉ Người phụ trách hoặc Trưởng Dự Án mới được phép.";
                    if (isAjax) {
                        sendJsonResponse(response, false, errMsg, null);
                        return;
                    }
                    if (session != null) session.setAttribute("toastError", errMsg);
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                // RÀNG BUỘC KHÓA BẤT BIẾN: TASK ĐÃ HOÀN TẤT THÌ KHÔNG ĐỔI TRẠNG THÁI ĐƯỢC NỮA.
                // Xét cả "APPROVED" vì handleShowKanban coi nó tương đương DONE, và dữ liệu cũ
                // có thể đã mang giá trị này từ trước khi có CHỐT 0.
                if ("DONE".equalsIgnoreCase(task.getStatus()) || "APPROVED".equalsIgnoreCase(task.getStatus())) {
                    String errMsg = "🔒 Công việc [" + task.getTitle() + "] đã hoàn thành và được khóa vĩnh viễn, không thể thay đổi trạng thái!";
                    if (isAjax) {
                        sendJsonResponse(response, false, errMsg, null);
                        return;
                    }
                    if (session != null) session.setAttribute("toastError", errMsg);
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                boolean isGateEnforced = (currentPrj != null && currentPrj.isTeamProject() && task.isRequiresGate());

                // =========================================================================
                // RÀNG BUỘC GIAI ĐOẠN 1: CHUYỂN TỪ TODO SANG IN_PROGRESS (ĐANG LÀM)
                // =========================================================================
                if ("IN_PROGRESS".equalsIgnoreCase(status)
                        && ("TODO".equalsIgnoreCase(task.getStatus()) || "PLANNING".equalsIgnoreCase(task.getStatus()))) {
                    String taskTitle = task.getTitle();

                    // Ràng buộc Quality Gate: Chặn kéo thả trực tiếp từ TODO / PLANNING sang IN_PROGRESS.
                    // Bắt buộc phải nộp Kế hoạch WBS và được PM phê duyệt (Gate 1): chỉ PM mới chuyển sang Đang làm
                    // thông qua nút 'Phê Duyệt & Khóa' (handlePmApprovePlanning).
                    if (isGateEnforced) {
                        String errMsg = "PLANNING".equalsIgnoreCase(task.getStatus())
                                ? "🛡️ [Đang chờ duyệt kế hoạch] Kế hoạch của công việc này đã gửi và đang chờ trưởng dự án phê duyệt. Công việc sẽ tự chuyển sang Đang làm khi được duyệt."
                                : "🛡️ [Cần duyệt kế hoạch] Công việc này bắt buộc kiểm duyệt! Vui lòng mở chi tiết công việc, phân rã nhiệm vụ và bấm 'Gửi duyệt kế hoạch' để trưởng dự án phê duyệt trước khi bắt đầu.";
                        if (isAjax) {
                            sendJsonResponse(response, false, errMsg, null);
                            return;
                        }
                        if (session != null) session.setAttribute("toastError", errMsg);
                        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                        return;
                    }

                    // Đối với Fast-track: Bắt buộc phải có Người phụ trách (Task Lead)
                    if (task.getAssigneeId() <= 0) {
                        User curUser = getCurrentUser(request);
                        if (curUser != null && currentPrj != null && currentPrj.isSoloProject()) {
                            // Solo mode: Tự động gán cho chính mình
                            task.setAssigneeId(curUser.getId());
                            task.setAssigneeName(curUser.getFullName());
                            TaskDB.update(task);
                        } else {
                            String errMsg = "⚠️ Không thể bắt đầu công việc [" + taskTitle + "]! Công việc chưa được phân công. Vui lòng bấm 'Chỉnh sửa' để chọn Người phụ trách trước.";
                            if (isAjax) {
                                sendJsonResponse(response, false, errMsg, null);
                                return;
                            }
                            if (session != null) session.setAttribute("toastError", errMsg);
                            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                            return;
                        }
                    }
                }

                // =========================================================================
                // RÀNG BUỘC GIAI ĐOẠN 2: CHUYỂN SANG DONE (HOÀN THÀNH)
                // =========================================================================
                if ("DONE".equalsIgnoreCase(status)) {
                    if (isGateEnforced) {
                        // CỔNG 2 — Task bật Quality Gate CHỈ hoàn tất được qua đúng một đường:
                        // Task Lead nộp bàn giao (handleSubmitParentTask) -> PM phê duyệt (handlePmApproveTask).
                        //
                        // KHÔNG xét việc con nữa. Điều kiện cũ là:
                        //     (subTasks != null && !subTasks.isEmpty() && progress < 100)
                        // nên MỌI task chưa có việc con đều đi lọt thẳng sang DONE — bỏ qua cả hai cổng.
                        // Câu hỏi đúng ở đây là "PM đã phê duyệt chưa?" (thẩm quyền),
                        // không phải "việc con xong chưa?" (tiến độ) — tiến độ vô nghĩa khi chưa có việc con.
                        String errMsg = "🛡️ [Cần nghiệm thu] Công việc [" + task.getTitle() + "] bắt buộc qua nghiệm thu! "
                                + "Trưởng nhóm công việc hãy mở chi tiết công việc và bấm 'Nộp bàn giao' để trưởng dự án phê duyệt.";
                        if (isAjax) {
                            sendJsonResponse(response, false, errMsg, null);
                            return;
                        }
                        if (session != null) session.setAttribute("toastError", errMsg);
                        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                        return;
                    }
                }

                TaskDB.updateStatus(taskId, status);

                // Ghi nhận Activity Log
                User curUser = getCurrentUser(request);
                int curUserId = curUser != null ? curUser.getId() : 0;
                String actionDesc = "Chuyển trạng thái sang: " + status;
                if ("DONE".equalsIgnoreCase(status)) actionDesc = "Đã hoàn tất toàn bộ công việc";
                else if ("IN_PROGRESS".equalsIgnoreCase(status)) actionDesc = "Bắt đầu thực hiện công việc";
                else if ("TODO".equalsIgnoreCase(status)) actionDesc = "Chuyển về danh mục Cần làm";
                ActivityLogDB.logAsync(projectId, curUserId, "STATUS_CHANGE", "TASK", taskId, task.getTitle(), actionDesc);

                String toastSuccessMsg = "DONE".equalsIgnoreCase(status) ? "🎉 Chúc mừng! Thẻ công việc đã được hoàn tất thành công."
                        : ("IN_PROGRESS".equalsIgnoreCase(status) ? "🚀 Bắt đầu thực hiện công việc [" + task.getTitle() + "] thành công!"
                        : "Đã chuyển công việc về danh mục Cần làm.");

                if (isAjax) {
                    sendJsonResponse(response, true, toastSuccessMsg, "{\"taskId\":" + taskId + ",\"newStatus\":\"" + escapeJson(status) + "\"}");
                    return;
                }

                if (session != null) {
                    // ▶ JSP: docs.jsp, tasks.jsp đọc bằng ${toastSuccess}
                    session.setAttribute("toastSuccess", toastSuccessMsg);
                }
            } else if (isAjax) {
                sendJsonResponse(response, false, "Không tìm thấy công việc tương ứng trong dự án!", null);
                return;
            }
        } else if (isAjax) {
            sendJsonResponse(response, false, "Dữ liệu yêu cầu không hợp lệ!", null);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 7.1: Chỉnh sửa thông tin công việc lớn (Task cha)
     * Thẩm quyền: Trưởng Dự Án (PM) hoặc Trưởng Nhóm Task (Task Lead)
     */
    public static void handleEditTask(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String priority = request.getParameter("priority");
        String dueDate = request.getParameter("dueDate");
        String assigneeIdParam = request.getParameter("assigneeId");

        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task == null || project == null || task.getProjectId() != projectId) {
            if (session != null) session.setAttribute("toastError", "Không tìm thấy thông tin công việc!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // RÀNG BUỘC KHÓA BẤT BIẾN: KHÔNG THỂ CHỈNH SỬA THÔNG TIN TASK ĐÃ DONE
        if ("DONE".equalsIgnoreCase(task.getStatus())) {
            if (session != null) session.setAttribute("toastError", "🔒 Công việc [" + task.getTitle() + "] đã hoàn thành và được khóa vĩnh viễn, không thể chỉnh sửa!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Kiểm tra thẩm quyền: PM hoặc Task Lead của task này
        boolean isPm = isProjectOwner(currentUser, project);
        boolean isLead = isTaskLead(currentUser, task);

        if (!isPm && !isLead) {
            if (session != null) session.setAttribute("toastError", "Bạn không có quyền chỉnh sửa thông tin công việc này!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Validate tiêu đề
        if (title == null || title.trim().isEmpty()) {
            if (session != null) session.setAttribute("toastError", "Tiêu đề công việc không được để trống!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Cập nhật các trường cơ bản
        task.setTitle(title.trim());
        task.setDescription(description != null ? description.trim() : "");
        if (priority != null && ("LOW".equalsIgnoreCase(priority) || "MEDIUM".equalsIgnoreCase(priority) || "HIGH".equalsIgnoreCase(priority))) {
            task.setPriority(priority.toUpperCase());
        }
        task.setDueDate(dueDate != null ? dueDate.trim() : "");

        if (project.isSoloProject()) {
            task.setAssigneeId(currentUser.getId());
            task.setAssigneeName(currentUser.getFullName());
            task.setRequiresGate(false);
        } else {
            // Chỉ PM mới được đổi người phụ trách (assigneeId)
            if (isPm && assigneeIdParam != null && !assigneeIdParam.trim().isEmpty()) {
                int newAssigneeId = safeParseInt(assigneeIdParam, 0);
                if (newAssigneeId > 0 && newAssigneeId != task.getAssigneeId() && ProjectMemberDB.isMember(projectId, newAssigneeId)) {
                    User newAssignee = UserDB.selectById(newAssigneeId);
                    if (newAssignee != null) {
                        task.setAssigneeId(newAssigneeId);
                        task.setAssigneeName(newAssignee.getFullName());

                        // Gửi thông báo cho người mới được giao task
                        NotificationDB.send(
                            newAssigneeId,
                            "Phân Công Công Việc Mới",
                            "Bạn vừa được Trưởng Dự Án phân công làm trưởng nhóm công việc cho công việc [" + task.getTitle() + "].",
                            "/task?action=list&projectId=" + projectId,
                            "TASK"
                        );
                    }
                }
            }

            // Cập nhật chế độ Quality Gate nếu form có gửi cờ điều khiển
            String hasControl = request.getParameter("hasRequiresGateControl");
            if (hasControl != null && !hasControl.trim().isEmpty()) {
                String reqGateParam = request.getParameter("requiresGate");
                task.setRequiresGate("true".equalsIgnoreCase(reqGateParam) || "on".equalsIgnoreCase(reqGateParam) || "1".equals(reqGateParam));
            }
        }

        TaskDB.update(task);

        if (session != null) {
            session.setAttribute("toastSuccess", "Đã cập nhật thông tin công việc [" + task.getTitle() + "] thành công!");
        }
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    // ==================== TẠO NHÃN TÙY BIẾN (AJAX, TRẢ VỀ JSON) ====================

    public static final java.util.regex.Pattern LABEL_NAME_PATTERN =
            java.util.regex.Pattern.compile("^[\\p{L}\\p{N}][\\p{L}\\p{N} /_.+-]{0,29}$");
    public static final Set<String> LABEL_COLORS =
            Set.of("red", "blue", "purple", "amber", "green", "pink", "cyan", "slate");

    /**
     * Tạo nhãn mới cho dự án (POST /task action=createLabel). Tên nhãn không được chứa dấu phẩy,
     * dấu nháy hay ký tự HTML vì tên được lưu trong chuỗi tasks.labels và in thẳng ra giao diện.
     */
    public static void handleCreateLabel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User currentUser = getCurrentUser(request);
        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        String name = request.getParameter("name") != null ? request.getParameter("name").trim() : "";
        String color = request.getParameter("color") != null ? request.getParameter("color").trim().toLowerCase() : "blue";

        if (currentUser == null) {
            writeLabelJson(response, 401, null, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại!");
            return;
        }
        if (projectId <= 0 || !ProjectMemberDB.isMember(projectId, currentUser.getId())) {
            writeLabelJson(response, 403, null, "Bạn không có quyền thao tác trong dự án này!");
            return;
        }
        if (!LABEL_NAME_PATTERN.matcher(name).matches()) {
            writeLabelJson(response, 400, null, "Tên nhãn chỉ gồm chữ, số, khoảng trắng và / _ . + - (tối đa 30 ký tự)!");
            return;
        }
        if (!LABEL_COLORS.contains(color)) {
            color = "blue";
        }
        if (LabelDB.existsByName(projectId, name, 0)) {
            writeLabelJson(response, 409, null, "Nhãn \"" + name + "\" đã tồn tại trong dự án!");
            return;
        }

        Label label = new Label(0, projectId, name, color, "bi-tag-fill");
        if (LabelDB.insert(label) <= 0) {
            writeLabelJson(response, 500, null, "Không thể lưu nhãn, vui lòng thử lại!");
            return;
        }
        writeLabelJson(response, 200, label, null);
    }

    /**
     * Nghiệp vụ AJAX: Tạo nhanh Task lớn trực tiếp từ dòng inline của nhóm (List View)
     */
    public static void handleQuickAddParentTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        String title = request.getParameter("title");
        String status = request.getParameter("status");
        String priority = request.getParameter("priority");
        String dueDate = request.getParameter("dueDate");
        int assigneeId = safeParseInt(request.getParameter("assigneeId"), 0);

        if (projectId <= 0 || title == null || title.trim().isEmpty()) {
            sendJsonResponse(response, false, "Tiêu đề công việc không được để trống!", null);
            return;
        }

        User currentUser = getCurrentUser(request);
        if (currentUser == null || !ProjectMemberDB.isMember(projectId, currentUser.getId())) {
            sendJsonResponse(response, false, "Bạn không có quyền thao tác trong dự án này!", null);
            return;
        }

        // Mọi công việc mới tạo bắt buộc bắt đầu từ TO DO theo chuẩn quy trình ClickUp/Agile
        status = "TODO";

        if (priority == null || priority.trim().isEmpty()) {
            priority = "MEDIUM";
        }
        priority = priority.trim().toUpperCase();
        if (!"HIGH".equals(priority) && !"MEDIUM".equals(priority) && !"LOW".equals(priority)) {
            priority = "MEDIUM";
        }

        String assigneeName = "Chưa phân công";
        if (assigneeId > 0 && ProjectMemberDB.isMember(projectId, assigneeId)) {
            User u = UserDB.selectById(assigneeId);
            if (u != null) {
                assigneeName = u.getFullName();
            } else {
                assigneeId = 0;
            }
        } else {
            assigneeId = 0;
        }

        String cleanDueDate = (dueDate != null && !dueDate.trim().isEmpty()) ? dueDate.trim() : "";

        Task newTask = new Task(
            0,
            projectId,
            title.trim(),
            "",
            status,
            priority,
            cleanDueDate,
            assigneeId,
            assigneeName,
            "",
            "",
            "",
            "",
            "",
            5,
            "",
            ""
        );

        Project currentPrj = ProjectDB.selectById(projectId);
        if (currentPrj != null && currentPrj.isSoloProject()) {
            newTask.setAssigneeId(currentUser.getId());
            newTask.setAssigneeName(currentUser.getFullName());
            newTask.setRequiresGate(false);
        } else {
            boolean defaultRequiresGate = (currentPrj != null && currentPrj.isTeamProject());
            String reqGateParam = request.getParameter("requiresGate");
            if (reqGateParam != null) {
                newTask.setRequiresGate("true".equalsIgnoreCase(reqGateParam.trim()) || "on".equalsIgnoreCase(reqGateParam.trim()) || "1".equals(reqGateParam.trim()));
            } else {
                newTask.setRequiresGate(defaultRequiresGate);
            }
        }

        int newTaskId = TaskDB.insert(newTask);
        if (newTaskId <= 0) {
            sendJsonResponse(response, false, "Lỗi khi lưu công việc vào cơ sở dữ liệu!", null);
            return;
        }
        newTask.setId(newTaskId);

        // Ghi nhận Activity Log
        ActivityLogDB.logAsync(projectId, currentUser.getId(), "TASK_CREATE", "TASK", newTaskId, newTask.getTitle(), "Tạo nhanh công việc: " + newTask.getTitle());

        String dataJson = String.format(
            "{\"id\":%d,\"projectId\":%d,\"title\":\"%s\",\"status\":\"%s\",\"priority\":\"%s\",\"dueDate\":\"%s\",\"assigneeId\":%d,\"assigneeName\":\"%s\"}",
            newTask.getId(),
            projectId,
            escapeJson(newTask.getTitle()),
            escapeJson(newTask.getStatus()),
            escapeJson(newTask.getPriority()),
            escapeJson(newTask.getDueDate()),
            newTask.getAssigneeId(),
            escapeJson(newTask.getAssigneeName())
        );

        sendJsonResponse(response, true, "Đã tạo nhanh công việc thành công!", dataJson);
    }
}
