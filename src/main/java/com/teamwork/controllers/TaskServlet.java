package com.teamwork.controllers;

import com.teamwork.business.Doc;
import com.teamwork.business.Message;
import com.teamwork.business.Project;
import com.teamwork.business.SubTask;
import com.teamwork.business.Task;
import com.teamwork.business.TaskDoc;
import com.teamwork.business.User;
import com.teamwork.data.DocDB;
import com.teamwork.data.MessageDB;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.SubTaskDB;
import com.teamwork.data.TaskDB;
import com.teamwork.data.TaskDocDB;
import com.teamwork.data.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.teamwork.business.ProjectInvite;
import com.teamwork.business.ProjectMember;
import com.teamwork.data.NotificationDB;
import com.teamwork.data.ProjectInviteDB;
import com.teamwork.data.ProjectMemberDB;
import jakarta.servlet.http.HttpSession;
import com.teamwork.business.UserWorkload;

/**
 * Controller phụ trách Bảng công việc Kanban (Tasks Module) & Cây Phân Cấp Việc Con (Sub-tasks):
 * - Hiển thị 3 cột công việc TODO, IN_PROGRESS, DONE kèm Tài liệu, Bình luận, Việc con & % Tiến độ (GET /task?action=list)
 * - Thêm công việc lớn (Task Cha) kèm đính kèm tài liệu (POST /task?action=add)
 * - Cập nhật trạng thái công việc khi kéo thả HTML5 (POST /task?action=updateStatus)
 * - Xóa công việc lớn có kiểm soát thẩm quyền Task Lead / PM (GET /task?action=delete)
 * - Thêm việc con có kiểm soát thẩm quyền Task Lead / PM (POST /task?action=addSubTask)
 * - Tick chọn hoàn thành việc con [☑] có kiểm soát thẩm quyền 3 bên (POST /task?action=toggleSubTask)
 * - Xóa việc con có kiểm soát thẩm quyền Task Lead / PM (POST /task?action=deleteSubTask)
 */
public class TaskServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Xác định ID của Project từ URL
        String projectIdParam = request.getParameter("projectId");
        int projectId = 0;

        if (projectIdParam == null || projectIdParam.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        try {
            projectId = Integer.parseInt(projectIdParam.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 2. Xử lý hành động của người dùng (list hoặc delete)
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        switch (action) {
            case "list":
                handleShowKanban(request, response, projectId);
                break;

            case "delete":
                handleDeleteTask(request, response, projectId);
                break;

            default:
                handleShowKanban(request, response, projectId);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "add";
        }

        switch (action) {
            case "add":
                handleAddTask(request, response);
                break;

            case "updateStatus":
                handleUpdateTaskStatus(request, response);
                break;

            case "addSubTask":
                handleAddSubTask(request, response);
                break;

            case "toggleSubTask":
                handleToggleSubTask(request, response);
                break;

            case "submitSubTask":
                handleSubmitSubTask(request, response);
                break;

            case "approveSubTask":
                handleApproveSubTask(request, response);
                break;

            case "reviseSubTask":
                handleReviseSubTask(request, response);
                break;

            case "rejectSubTask":
                handleRejectSubTask(request, response);
                break;

            case "submitParentTask":
                handleSubmitParentTask(request, response);
                break;

            case "submitPlanningRequest":
                handleSubmitPlanningRequest(request, response);
                break;

            case "pmApprovePlanning":
                handlePmApprovePlanning(request, response);
                break;

            case "pmRejectPlanning":
                handlePmRejectPlanning(request, response);
                break;

            case "pmApproveTask":
                handlePmApproveTask(request, response);
                break;

            case "pmReviseTask":
                handlePmReviseTask(request, response);
                break;

            case "pmRejectTask":
                handlePmRejectTask(request, response);
                break;

            case "deleteSubTask":
                handleDeleteSubTask(request, response);
                break;

            default:
                response.sendRedirect(request.getContextPath() + "/project?action=list");
                break;
        }
    }

    /**
     * Nghiệp vụ 1: Lấy toàn bộ dữ liệu 3 cột Kanban, danh sách tài liệu dự án,
     * map liên kết TaskDoc, map bình luận TaskComments, map Việc Con SubTasks và % Tiến độ
     */
    private void handleShowKanban(HttpServletRequest request, HttpServletResponse response, int projectId)
            throws ServletException, IOException {

        // 1. Kiểm tra dự án tồn tại
        Project project = ProjectDB.selectById(projectId);
        if (project == null) 
        {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 2. Lấy danh sách Task phân theo 3 cột trạng thái
        List<Task> todoTasks = TaskDB.selectByProjectAndStatus(projectId, "TODO");
        List<Task> inProgressTasks = TaskDB.selectByProjectAndStatus(projectId, "IN_PROGRESS");
        List<Task> doneTasks = TaskDB.selectByProjectAndStatus(projectId, "DONE");

        // 3. Lấy danh sách thành viên để gán người phụ trách
        List<User> userList = UserDB.selectAll();

        // 4. Lấy danh sách toàn bộ tài liệu Wiki của dự án này
        List<Doc> docList = DocDB.selectByProjectId(projectId);

        // 5. Gom toàn bộ danh sách tài liệu đính kèm cho từng Task vào một Map (taskId -> List<TaskDoc>)
        Map<Integer, List<TaskDoc>> taskDocsMap = new HashMap<>();

        // 6. Gom toàn bộ danh sách bình luận riêng cho từng Task vào một Map (taskId -> List<Message>)
        Map<Integer, List<Message>> taskCommentsMap = new HashMap<>();

        // 7. Gom toàn bộ danh sách việc con cho từng Task vào một Map (taskId -> List<SubTask>)
        Map<Integer, List<SubTask>> taskSubTasksMap = new HashMap<>();

        // 8. Gom % tiến độ tự động cho từng Task vào một Map (taskId -> Integer progress)
        Map<Integer, Integer> taskProgressMap = new HashMap<>();

        // Nạp dữ liệu đa tầng cho cột TODO
        for (Task t : todoTasks) {
            taskDocsMap.put(t.getId(), TaskDocDB.selectByTaskId(t.getId()));
            taskCommentsMap.put(t.getId(), MessageDB.selectByTaskId(t.getId()));
            taskSubTasksMap.put(t.getId(), SubTaskDB.selectByTaskId(t.getId()));
            taskProgressMap.put(t.getId(), SubTaskDB.calculateProgress(t.getId()));
        }

        // Nạp dữ liệu đa tầng cho cột IN_PROGRESS
        for (Task t : inProgressTasks) {
            taskDocsMap.put(t.getId(), TaskDocDB.selectByTaskId(t.getId()));
            taskCommentsMap.put(t.getId(), MessageDB.selectByTaskId(t.getId()));
            taskSubTasksMap.put(t.getId(), SubTaskDB.selectByTaskId(t.getId()));
            taskProgressMap.put(t.getId(), SubTaskDB.calculateProgress(t.getId()));
        }

        // Nạp dữ liệu đa tầng cho cột DONE
        for (Task t : doneTasks) {
            taskDocsMap.put(t.getId(), TaskDocDB.selectByTaskId(t.getId()));
            taskCommentsMap.put(t.getId(), MessageDB.selectByTaskId(t.getId()));
            taskSubTasksMap.put(t.getId(), SubTaskDB.selectByTaskId(t.getId()));
            taskProgressMap.put(t.getId(), SubTaskDB.calculateProgress(t.getId()));
        }

        // 8.5. Tính toán khối lượng công việc của từng thành viên (UserWorkload DTO) cho Dải Avatar B.3
        List<Task> allTasks = new ArrayList<>();
        allTasks.addAll(todoTasks);
        allTasks.addAll(inProgressTasks);
        allTasks.addAll(doneTasks);
        List<UserWorkload> userWorkloadList = computeUserWorkloads(userList, allTasks);

        // 8.6. Lấy danh sách thành viên và lời mời của dự án (Chặng C.3)
        List<ProjectMember> projectMemberList = ProjectMemberDB.selectByProjectId(projectId);
        List<ProjectInvite> projectInviteList = ProjectInviteDB.selectByProjectId(projectId);
        int memberCount = ProjectMemberDB.countMembers(projectId);

        // 8.7. Xử lý Flash Message (Toast)
        HttpSession session = request.getSession(false);
        if (session != null) {
            String toastSuccess = (String) session.getAttribute("toastSuccess");
            if (toastSuccess != null) {
                request.setAttribute("toastSuccess", toastSuccess);
                session.removeAttribute("toastSuccess");
            }
            String toastError = (String) session.getAttribute("toastError");
            if (toastError != null) {
                request.setAttribute("toastError", toastError);
                session.removeAttribute("toastError");
            }
        }

        // 9. Đóng gói dữ liệu gửi sang tasks.jsp
        request.setAttribute("project", project);
        request.setAttribute("todoTasks", todoTasks);
        request.setAttribute("inProgressTasks", inProgressTasks);
        request.setAttribute("doneTasks", doneTasks);
        request.setAttribute("userList", userList);
        request.setAttribute("docList", docList);
        request.setAttribute("taskDocsMap", taskDocsMap);
        request.setAttribute("taskCommentsMap", taskCommentsMap);
        request.setAttribute("taskSubTasksMap", taskSubTasksMap);
        request.setAttribute("taskProgressMap", taskProgressMap);
        request.setAttribute("userWorkloadList", userWorkloadList);
        request.setAttribute("projectMemberList", projectMemberList);
        request.setAttribute("projectInviteList", projectInviteList);
        request.setAttribute("memberCount", memberCount);
        request.setAttribute("activeNav", "projects");

        // 10. Forward sang giao diện tasks.jsp
        request.getRequestDispatcher("/tasks.jsp").forward(request, response);
    }

    /**
     * Nghiệp vụ 2: Xóa một task lớn khỏi dự án theo taskId (BẢO VỆ PHÂN QUYỀN TASK LEAD / PM)
     */
    private void handleDeleteTask(HttpServletRequest request, HttpServletResponse response, int projectId)
            throws IOException {

        String taskIdParam = request.getParameter("taskId");
        if (taskIdParam != null && !taskIdParam.trim().isEmpty()) {
            try {
                int taskId = Integer.parseInt(taskIdParam.trim());
                User currentUser = (User) request.getSession().getAttribute("currentUser");
                Task task = TaskDB.selectById(taskId);
                Project project = ProjectDB.selectById(projectId);

                if (currentUser != null && task != null && project != null) {
                    // Kiểm tra thẩm quyền: Chỉ Task Lead của chính Task này HOẶC Trưởng Dự Án mới được xóa
                    if (currentUser.getId() == task.getAssigneeId() || currentUser.getId() == project.getOwnerId()) {
                        // 1. Dọn dẹp các liên kết Task-Doc trên RAM
                        TaskDocDB.deleteByTaskId(taskId);

                        // 2. Dọn dẹp các việc con thuộc Task này trên RAM
                        SubTaskDB.deleteByTaskId(taskId);

                        // 3. Dọn dẹp các bình luận của Task này trên RAM
                        MessageDB.deleteByTaskId(taskId);

                        // 4. Xóa Task trong TaskDB
                        TaskDB.delete(taskId);
                    }
                }
            } catch (NumberFormatException e) {
                // Bỏ qua nếu taskId không hợp lệ
            }
        }

        // Xóa xong -> Redirect về lại bảng Kanban của dự án
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 3: Tiếp nhận form thêm task mới và lưu liên kết tài liệu đính kèm vào TaskDocDB
     */
    private void handleAddTask(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String projectIdParam = request.getParameter("projectId");
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String priority = request.getParameter("priority");
        String dueDate = request.getParameter("dueDate");
        String assigneeIdParam = request.getParameter("assigneeId");
        String[] selectedDocIds = request.getParameterValues("docIds");

        int projectId = 0;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        if (title == null || title.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        if (priority == null || priority.trim().isEmpty()) {
            priority = "MEDIUM";
        }

        int assigneeId = 0;
        String assigneeName = "Chưa phân công";
        if (assigneeIdParam != null && !assigneeIdParam.trim().isEmpty()) {
            try {
                assigneeId = Integer.parseInt(assigneeIdParam.trim());
                User assignee = UserDB.selectById(assigneeId);
                if (assignee != null) {
                    assigneeName = assignee.getFullName();
                }
            } catch (NumberFormatException e) {
                assigneeId = 0;
            }
        }

        Task newTask = new Task(
            0,
            projectId,
            title.trim(),
            (description != null ? description.trim() : ""),
            "TODO",
            priority,
            (dueDate != null ? dueDate.trim() : ""),
            assigneeId,
            assigneeName
        );

        int newTaskId = TaskDB.insert(newTask);

        if (selectedDocIds != null && selectedDocIds.length > 0) {
            for (String docIdStr : selectedDocIds) {
                try {
                    int docId = Integer.parseInt(docIdStr.trim());
                    Doc doc = DocDB.selectById(docId);
                    if (doc != null) {
                        TaskDocDB.insert(newTaskId, docId, doc.getTitle());
                    }
                } catch (NumberFormatException e) {
                    // Bỏ qua
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 4: Cập nhật trạng thái Task khi người dùng kéo thả
     */
    private void handleUpdateTaskStatus(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String taskIdParam = request.getParameter("taskId");
        String newStatus = request.getParameter("newStatus");

        int projectId = 0;
        int taskId = 0;

        try {
            if (projectIdParam != null) {
                projectId = Integer.parseInt(projectIdParam.trim());
            }
            if (taskIdParam != null) {
                taskId = Integer.parseInt(taskIdParam.trim());
            }

            if (taskId > 0 && newStatus != null && !newStatus.trim().isEmpty()) {
                String status = newStatus.trim();

                // RÀNG BUỘC CHẤT LƯỢNG NGHIỆM THU:
                // Nếu muốn chuyển sang DONE, bắt buộc toàn bộ danh sách việc con (Sub-tasks) phải hoàn thành 100%
                if ("DONE".equalsIgnoreCase(status)) {
                    List<SubTask> subTasks = SubTaskDB.selectByTaskId(taskId);
                    int progress = SubTaskDB.calculateProgress(taskId);
                    Task task = TaskDB.selectById(taskId);
                    String taskTitle = (task != null) ? task.getTitle() : "này";

                    if (subTasks != null && !subTasks.isEmpty() && progress < 100) {
                        request.getSession().setAttribute("toastError", 
                            "⚠️ Không thể đánh dấu hoàn thành Task [" + taskTitle + "]! Vẫn còn việc con chưa hoàn tất (Tiến độ: " + progress + "%). Hãy hoàn thành và nghiệm thu đủ 100% việc con trước.");
                        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                        return;
                    }
                }

                TaskDB.updateStatus(taskId, status);

                if ("DONE".equalsIgnoreCase(status)) {
                    request.getSession().setAttribute("toastSuccess", "🎉 Chúc mừng! Thẻ công việc đã được hoàn tất thành công.");
                }
            }
        } catch (Exception e) {
            // Xử lý lỗi an toàn
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 5: Thêm Việc Con (Sub-task) mới và phân công cho thành viên (BẢO VỆ PHÂN QUYỀN TASK LEAD / PM)
     */
    private void handleAddSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String taskIdParam = request.getParameter("taskId");
        String title = request.getParameter("title");
        String assigneeIdParam = request.getParameter("assigneeId");

        int projectId = 0;
        int taskId = 0;
        int assigneeId = 0;
        String assigneeName = "Chưa phân công";

        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            taskId = Integer.parseInt(taskIdParam.trim());
            if (assigneeIdParam != null && !assigneeIdParam.trim().isEmpty()) {
                assigneeId = Integer.parseInt(assigneeIdParam.trim());
                User u = UserDB.selectById(assigneeId);
                if (u != null) {
                    assigneeName = u.getFullName();
                }
            }
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = (User) request.getSession().getAttribute("currentUser");
        Task parentTask = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);

        // KIỂM SOÁT THẨM QUYỀN & KHÓA PHẠM VI (SCOPE LOCK):
        // 1. Chỉ Task Lead của chính Task này HOẶC Trưởng Dự Án mới được thêm việc con.
        // 2. Chỉ được thêm việc con khi Task đang ở trạng thái TODO (Giai đoạn Lập Kế Hoạch). Khi đã trình PM hoặc đã khóa thì không được thêm tự do.
        if (currentUser != null && parentTask != null && project != null) {
            if (!"TODO".equalsIgnoreCase(parentTask.getStatus())) {
                request.getSession().setAttribute("toastError", 
                    "⚠️ Kế hoạch phân rã đã được trình PM hoặc đã khóa (Scope Lock). Không thể thêm việc con mới!");
                response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                return;
            }

            if (currentUser.getId() == parentTask.getAssigneeId() || currentUser.getId() == project.getOwnerId()) {
                if (title != null && !title.trim().isEmpty() && taskId > 0) {
                    SubTask newSubTask = new SubTask(0, taskId, title.trim(), assigneeId, assigneeName, false);
                    SubTaskDB.insert(newSubTask);
                    request.getSession().setAttribute("toastSuccess", "Đã thêm việc con vào kế hoạch phân rã thành công!");
                }
            } else {
                request.getSession().setAttribute("toastError", "Bạn không có quyền phân rã việc con cho Task này!");
            }
        }

        // Áp dụng PRG: Redirect về lại bảng Kanban
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 6: Đổi trạng thái hoàn thành [☑] của Việc Con (SubTask) (BẢO VỆ PHÂN QUYỀN 3 BÊN)
     * KÈM CƠ CHẾ DOMINO TỰ ĐỘNG CHUYỂN CỘT KANBAN VÀ THÔNG BÁO VINH DANH
     */
    private void handleToggleSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String subTaskIdParam = request.getParameter("subTaskId");
        String completedParam = request.getParameter("completed");

        int projectId = 0;
        int subTaskId = 0;
        boolean isCompleted = false;

        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            subTaskId = Integer.parseInt(subTaskIdParam.trim());
            isCompleted = Boolean.parseBoolean(completedParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = (User) request.getSession().getAttribute("currentUser");
        SubTask st = SubTaskDB.selectById(subTaskId);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null) {
                // KIỂM SOÁT THẨM QUYỀN 3 BÊN:
                // 1. Phải là Người thực hiện việc con này
                // 2. HOẶC là Task Lead của Task cha này
                // 3. HOẶC là Trưởng Dự Án (PM)
                boolean isAssignee = (currentUser.getId() == st.getAssigneeId());
                boolean isTaskLead = (currentUser.getId() == parentTask.getAssigneeId());
                boolean isProjectOwner = (currentUser.getId() == project.getOwnerId());

                if (isAssignee || isTaskLead || isProjectOwner) {
                    // 1. Cập nhật trạng thái hoàn thành [☑] trong kho SubTaskDB
                    SubTaskDB.updateStatus(subTaskId, isCompleted);

                    int newProgress = SubTaskDB.calculateProgress(st.getTaskId());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String now = LocalDateTime.now().format(formatter);

                    // 2. CƠ CHẾ TỰ ĐỘNG CHUYỂN CỘT KANBAN CHO TASK LỚN:
                    if (newProgress == 100 && !"DONE".equals(parentTask.getStatus())) {
                        TaskDB.updateStatus(parentTask.getId(), "DONE");
                        String celebrationText = "🏆 CHÚC MỪNG TOÀN ĐỘI: Tất cả việc con đã hoàn tất (100%)! Thẻ công việc [" + parentTask.getTitle() + "] đã tự động chuyển sang trạng thái ĐÃ XONG!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", celebrationText, now));
                    } else if (newProgress > 0 && newProgress < 100 && "TODO".equals(parentTask.getStatus())) {
                        TaskDB.updateStatus(parentTask.getId(), "IN_PROGRESS");
                        String progressText = "🚀 BẮT ĐẦU THỰC HIỆN: Đã hoàn thành " + newProgress + "% việc con. Task [" + parentTask.getTitle() + "] đã tự động chuyển sang ĐANG LÀM!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", progressText, now));
                    } else if (newProgress < 100 && "DONE".equals(parentTask.getStatus())) {
                        TaskDB.updateStatus(parentTask.getId(), "IN_PROGRESS");
                        String reopenText = "⚠️ CẬP NHẬT: Còn việc con chưa xong (" + newProgress + "%). Task [" + parentTask.getTitle() + "] đã được mở lại sang ĐANG LÀM!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", reopenText, now));
                    }

                    // 3. Thông báo ghi nhận cá nhân vừa hoàn thành việc con
                    if (isCompleted) {
                        String notificationText = "🎉 " + st.getAssigneeName() + " vừa hoàn thành việc con: [" + st.getTitle() + "] — Đóng góp đưa tiến độ Task lên " + newProgress + "%!";
                        Message systemMessage = new Message(0, projectId, st.getTaskId(), 0, "Hệ Thống", notificationText, now);
                        MessageDB.insert(systemMessage);
                    }
                }
            }
        }

        // Áp dụng PRG: Redirect về lại bảng Kanban
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 7: Xóa một việc con (BẢO VỆ PHÂN QUYỀN TASK LEAD / PM & KHÓA PHẠM VI)
     */
    private void handleDeleteSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String subTaskIdParam = request.getParameter("subTaskId");

        int projectId = 0;
        int subTaskId = 0;

        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            subTaskId = Integer.parseInt(subTaskIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = (User) request.getSession().getAttribute("currentUser");
        SubTask st = SubTaskDB.selectById(subTaskId);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null) {
                // RÀNG BUỘC KHÓA PHẠM VI: Không được xóa khi đã trình PM hoặc đã khóa
                if (!"TODO".equalsIgnoreCase(parentTask.getStatus())) {
                    request.getSession().setAttribute("toastError", 
                        "⚠️ Kế hoạch phân rã đã được trình PM hoặc đã khóa (Scope Lock). Không thể xóa việc con!");
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                // KIỂM SOÁT THẨM QUYỀN: Chỉ Task Lead của chính Task này HOẶC Trưởng Dự Án mới được xóa việc con
                if (currentUser.getId() == parentTask.getAssigneeId() || currentUser.getId() == project.getOwnerId()) {
                    SubTaskDB.delete(subTaskId);
                    request.getSession().setAttribute("toastSuccess", "Đã xóa việc con khỏi kế hoạch phân rã!");
                } else {
                    request.getSession().setAttribute("toastError", "Bạn không có quyền xóa việc con này!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 8: Thành viên Nộp Báo Cáo / Kết Quả Việc Con (Chuyển sang 🟡 SUBMITTED)
     * KÈM BẮN THÔNG BÁO CHO TASK LEAD
     */
    private void handleSubmitSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String subTaskIdParam = request.getParameter("subTaskId");
        String submissionNote = request.getParameter("submissionNote");

        int projectId = 0;
        int subTaskId = 0;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            subTaskId = Integer.parseInt(subTaskIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");
        SubTask st = SubTaskDB.selectById(subTaskId);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null) {
                // KIỂM SOÁT THẨM QUYỀN NGHIỆM THU TẦNG 1:
                // 1. Nếu việc con đã gán cho ai (assigneeId > 0): CHỈ chính thành viên đó mới được nộp kết quả.
                // 2. Nếu việc con chưa gán cho ai (assigneeId == 0): Task Lead hoặc PM có thể nộp.
                boolean isAssignedMember = (st.getAssigneeId() > 0 && currentUser.getId() == st.getAssigneeId());
                boolean isUnassignedAndLeadOrOwner = (st.getAssigneeId() == 0 && (currentUser.getId() == parentTask.getAssigneeId() || currentUser.getId() == project.getOwnerId()));

                if (isAssignedMember || isUnassignedAndLeadOrOwner) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String now = LocalDateTime.now().format(formatter);

                    SubTaskDB.submitDeliverable(subTaskId, submissionNote, now);

                    // Bắn thông báo thời gian thực 🔔 cho Task Lead (nếu Task Lead không phải chính mình)
                    int leadId = parentTask.getAssigneeId();
                    if (leadId > 0 && leadId != currentUser.getId()) {
                        NotificationDB.send(
                            leadId,
                            "🟡 Báo cáo nộp việc con",
                            currentUser.getFullName() + " vừa nộp kết quả việc con [" + st.getTitle() + "], mời bạn nghiệm thu!",
                            "/task?action=list&projectId=" + projectId,
                            "bi-hourglass-split text-warning"
                        );
                    }

                    // Thông báo lên Luồng Thảo luận
                    String msgContent = "📤 " + currentUser.getFullName() + " vừa nộp kết quả việc con: [" + st.getTitle() + "] — Ghi chú: \"" + (submissionNote != null && !submissionNote.trim().isEmpty() ? submissionNote : "Đã hoàn tất công việc") + "\"";
                    MessageDB.insert(new Message(0, projectId, st.getTaskId(), 0, "Hệ Thống", msgContent, now));

                    session.setAttribute("toastSuccess", "Đã nộp báo cáo kết quả việc con thành công! Đang chờ Task Lead duyệt.");
                } else {
                    session.setAttribute("toastError", "Bạn không có quyền nộp bài cho việc con của người khác!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 9: Task Lead Duyệt Nghiệm Thu ĐẠT (Chuyển sang 🟢 APPROVED)
     * KÈM CƠ CHẾ DOMINO TỰ ĐỘNG NÂNG % TIẾN ĐỘ VÀ BAY SANG CỘT DONE
     */
    private void handleApproveSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String subTaskIdParam = request.getParameter("subTaskId");

        int projectId = 0;
        int subTaskId = 0;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            subTaskId = Integer.parseInt(subTaskIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");
        SubTask st = SubTaskDB.selectById(subTaskId);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null) {
                // KIỂM SOÁT BẢO MẬT PHÂN TẦNG NGHIỆM THU:
                // Thẩm quyền duyệt việc con (Tầng 1) thuộc về TRƯỞNG NHÓM TASK (Task Lead) của Task này.
                // Nếu Task lớn chưa phân công (assigneeId == 0), PM mới được tạm quyền duyệt.
                boolean isTaskLead = (parentTask.getAssigneeId() > 0 && currentUser.getId() == parentTask.getAssigneeId());
                boolean isUnassignedAndOwner = (parentTask.getAssigneeId() == 0 && currentUser.getId() == project.getOwnerId());

                if (isTaskLead || isUnassignedAndOwner) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String now = LocalDateTime.now().format(formatter);

                    SubTaskDB.approveDeliverable(subTaskId, now);

                    int newProgress = SubTaskDB.calculateProgress(st.getTaskId());

                    // CƠ CHẾ DOMINO TỰ ĐỘNG CHUYỂN CỘT KANBAN:
                    if (newProgress == 100 && !"DONE".equals(parentTask.getStatus())) {
                        TaskDB.updateStatus(parentTask.getId(), "DONE");
                        String celebrationText = "🏆 CHÚC MỪNG TOÀN ĐỘI: Tất cả việc con đã được duyệt nghiệm thu ĐẠT (100%)! Thẻ công việc [" + parentTask.getTitle() + "] đã tự động chuyển sang trạng thái ĐÃ XONG!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", celebrationText, now));
                    } else if (newProgress > 0 && newProgress < 100 && "TODO".equals(parentTask.getStatus())) {
                        TaskDB.updateStatus(parentTask.getId(), "IN_PROGRESS");
                        String progressText = "🚀 BẮT ĐẦU THỰC HIỆN: Đã nghiệm thu " + newProgress + "% việc con. Task [" + parentTask.getTitle() + "] chuyển sang ĐANG LÀM!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", progressText, now));
                    }

                    // Bắn thông báo thời gian thực 🔔 cho Người phụ trách việc con
                    if (st.getAssigneeId() > 0 && st.getAssigneeId() != currentUser.getId()) {
                        NotificationDB.send(
                            st.getAssigneeId(),
                            "🟢 Nghiệm thu ĐẠT",
                            "Việc con [" + st.getTitle() + "] của bạn đã được Leader duyệt đạt 100%!",
                            "/task?action=list&projectId=" + projectId,
                            "bi-check-circle-fill text-success"
                        );
                    }

                    session.setAttribute("toastSuccess", "Đã duyệt nghiệm thu ĐẠT cho việc con!");
                } else {
                    session.setAttribute("toastError", "Thẩm quyền thẩm định việc con thuộc về Trưởng Nhóm Task (Task Lead) của công việc này!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 10: Task Lead Yêu Cầu Cân Chỉnh Nhỏ (Chuyển sang 🔵 REVISE - Màu Xanh Dương)
     */
    private void handleReviseSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String subTaskIdParam = request.getParameter("subTaskId");
        String feedbackNote = request.getParameter("feedbackNote");

        int projectId = 0;
        int subTaskId = 0;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            subTaskId = Integer.parseInt(subTaskIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");
        SubTask st = SubTaskDB.selectById(subTaskId);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null) {
                boolean isTaskLead = (parentTask.getAssigneeId() > 0 && currentUser.getId() == parentTask.getAssigneeId());
                boolean isUnassignedAndOwner = (parentTask.getAssigneeId() == 0 && currentUser.getId() == project.getOwnerId());

                if (isTaskLead || isUnassignedAndOwner) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String now = LocalDateTime.now().format(formatter);

                    SubTaskDB.reviseDeliverable(subTaskId, feedbackNote, now);

                    // Bắn thông báo thời gian thực 🔔 cho Thành viên phụ trách
                    if (st.getAssigneeId() > 0 && st.getAssigneeId() != currentUser.getId()) {
                        NotificationDB.send(
                            st.getAssigneeId(),
                            "🔵 Yêu cầu cân chỉnh việc con",
                            "Leader dặn dò: \"" + (feedbackNote != null ? feedbackNote : "Cần cân chỉnh một số chi tiết") + "\" đối với việc con [" + st.getTitle() + "]",
                            "/task?action=list&projectId=" + projectId,
                            "bi-pencil-square text-primary"
                        );
                    }

                    session.setAttribute("toastSuccess", "Đã gửi yêu cầu cân chỉnh nhỏ (🔵 Xanh Dương) tới thành viên!");
                } else {
                    session.setAttribute("toastError", "Thẩm quyền thẩm định việc con thuộc về Trưởng Nhóm Task (Task Lead) của công việc này!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 11: Task Lead Trả Về Do Chưa Đạt Yêu Cầu (Chuyển sang 🔴 REJECTED - Màu Đỏ)
     */
    private void handleRejectSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String subTaskIdParam = request.getParameter("subTaskId");
        String feedbackNote = request.getParameter("feedbackNote");

        int projectId = 0;
        int subTaskId = 0;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            subTaskId = Integer.parseInt(subTaskIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");
        SubTask st = SubTaskDB.selectById(subTaskId);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null) {
                boolean isTaskLead = (parentTask.getAssigneeId() > 0 && currentUser.getId() == parentTask.getAssigneeId());
                boolean isUnassignedAndOwner = (parentTask.getAssigneeId() == 0 && currentUser.getId() == project.getOwnerId());

                if (isTaskLead || isUnassignedAndOwner) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String now = LocalDateTime.now().format(formatter);

                    SubTaskDB.rejectDeliverable(subTaskId, feedbackNote, now);

                    // Bắn thông báo thời gian thực 🔔 cho Thành viên phụ trách
                    if (st.getAssigneeId() > 0 && st.getAssigneeId() != currentUser.getId()) {
                        NotificationDB.send(
                            st.getAssigneeId(),
                            "🔴 Việc con chưa đạt yêu cầu",
                            "Leader phản hồi lỗi: \"" + (feedbackNote != null ? feedbackNote : "Chưa đạt yêu cầu đề ra") + "\" đối với việc con [" + st.getTitle() + "]",
                            "/task?action=list&projectId=" + projectId,
                            "bi-exclamation-triangle-fill text-danger"
                        );
                    }

                    session.setAttribute("toastSuccess", "Đã trả về việc con và gửi phản hồi (🔴 Màu Đỏ) cho thành viên!");
                } else {
                    session.setAttribute("toastError", "Thẩm quyền thẩm định việc con thuộc về Trưởng Nhóm Task (Task Lead) của công việc này!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 12: Task Lead Bàn Giao & Nộp Báo Cáo Task Lớn Lên Cho PM (Chuyển sang 🟡 SUBMITTED)
     */
    private void handleSubmitParentTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String taskIdParam = request.getParameter("taskId");
        
        // Thu thập 5 trường thông tin báo cáo bàn giao có cấu trúc + Tệp đính kèm
        String summary = request.getParameter("summary");
        String demoUrl = request.getParameter("demoUrl");
        String codeUrl = request.getParameter("codeUrl");
        String testResult = request.getParameter("testResult");
        String testingGuide = request.getParameter("testingGuide");
        String deliverableNote = request.getParameter("deliverableNote");
        String deliverableFile = request.getParameter("deliverableFile");

        int projectId = 0;
        int taskId = 0;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            taskId = Integer.parseInt(taskIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);

        if (task != null && project != null && currentUser != null) {
            // KIỂM SOÁT THẨM QUYỀN NGHIỆM THU TẦNG 2:
            // 1. Nếu Task lớn đã gán cho Task Lead cụ thể (task.assigneeId > 0): CHỈ chính Task Lead đó mới được nộp bàn giao.
            // 2. Nếu Task lớn chưa gán cho ai (task.assigneeId == 0): PM mới được nộp.
            boolean isTaskLead = (task.getAssigneeId() > 0 && currentUser.getId() == task.getAssigneeId());
            boolean isUnassignedAndOwner = (task.getAssigneeId() == 0 && currentUser.getId() == project.getOwnerId());

            if (isTaskLead || isUnassignedAndOwner) {
                // RÀNG BUỘC CHẤT LƯỢNG: Task Lead chỉ được nộp bàn giao khi toàn bộ việc con đã hoàn tất 100%
                List<SubTask> subTasks = SubTaskDB.selectByTaskId(taskId);
                int progress = SubTaskDB.calculateProgress(taskId);
                if (subTasks != null && !subTasks.isEmpty() && progress < 100) {
                    session.setAttribute("toastError", 
                        "⚠️ Không thể nộp bàn giao Task [" + task.getTitle() + "] cho PM khi danh sách việc con chưa đạt 100% (Tiến độ hiện tại: " + progress + "%). Hãy hoàn thành các việc con trước!");
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
                    sb.append("🧭 [Hướng dẫn PM nghiệm thu]: ").append(testingGuide.trim());
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

                // Bắn thông báo thời gian thực 🔔 cho Trưởng Dự Án (PM)
                if (project.getOwnerId() > 0 && project.getOwnerId() != currentUser.getId()) {
                    NotificationDB.send(
                        project.getOwnerId(),
                        "🟡 Bàn giao Task lớn",
                        currentUser.getFullName() + " vừa nộp báo cáo bàn giao Task [" + task.getTitle() + "] kèm tệp đính kèm, kính mời PM nghiệm thu!",
                        "/task?action=list&projectId=" + projectId,
                        "bi-box-seam-fill text-warning"
                    );
                }

                // Thông báo lên Luồng Thảo luận
                String msgContent = "📦 [BÀN GIAO TASK]: " + currentUser.getFullName() + " đã nộp hồ sơ bàn giao Task [" + task.getTitle() + "] kèm tệp [" + deliverableFile.trim() + "] lên PM!";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                session.setAttribute("toastSuccess", "Đã nộp báo cáo bàn giao Task lớn thành công! Đang chờ PM phê duyệt.");
            } else {
                session.setAttribute("toastError", "Bạn không phải là Task Lead của thẻ công việc này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 12.5: Task Lead Trình Kế Hoạch Phân Rã Việc Con Cho PM Thẩm Định (CỔNG 1 ➔ Chuyển sang 🟣 PLANNING)
     */
    private void handleSubmitPlanningRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String taskIdParam = request.getParameter("taskId");
        String planningNote = request.getParameter("planningNote");

        int projectId = 0;
        int taskId = 0;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            taskId = Integer.parseInt(taskIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);

        if (task != null && project != null && currentUser != null) {
            // KIỂM SOÁT THẨM QUYỀN: Task Lead của task hoặc PM
            boolean isTaskLead = (task.getAssigneeId() > 0 && currentUser.getId() == task.getAssigneeId());
            boolean isUnassignedAndOwner = (task.getAssigneeId() == 0 && currentUser.getId() == project.getOwnerId());

            if (isTaskLead || isUnassignedAndOwner) {
                // RÀNG BUỘC CHẤT LƯỢNG: Phải phân rã ít nhất 1 việc con mới được trình PM
                List<SubTask> subTasks = SubTaskDB.selectByTaskId(taskId);
                if (subTasks == null || subTasks.isEmpty()) {
                    session.setAttribute("toastError", 
                        "⚠️ Không thể trình kế hoạch rỗng! Vui lòng phân rã ít nhất 1 việc con (Sub-task) trước khi gửi PM duyệt.");
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
                        "🟣 Trình Kế Hoạch Phân Rã Việc Con",
                        currentUser.getFullName() + " vừa trình kế hoạch phân rã " + subTasks.size() + " việc con cho Task [" + task.getTitle() + "], kính mời PM xem xét và khóa kế hoạch!",
                        "/task?action=list&projectId=" + projectId,
                        "bi-diagram-3-fill text-primary"
                    );
                }

                // Thông báo lên Luồng Thảo luận
                String msgContent = "📋 [TRÌNH KẾ HOẠCH PHÂN RÃ]: " + currentUser.getFullName() + " đã phân rã xong " + subTasks.size() + " việc con cho Task [" + task.getTitle() + "] và trình lên Trưởng Dự Án (PM) phê duyệt khóa phạm vi!";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                session.setAttribute("toastSuccess", "Đã trình kế hoạch phân rã việc con lên Trưởng Dự Án (PM) thành công! Đang chờ PM phê duyệt khóa phạm vi.");
            } else {
                session.setAttribute("toastError", "Bạn không phải là Task Lead của thẻ công việc này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 12.6: Trưởng Dự Án (PM) Phê Duyệt Kế Hoạch & KHÓA PHÂN RÃ (SCOPE LOCK ➔ Chuyển sang 🚀 IN_PROGRESS)
     */
    private void handlePmApprovePlanning(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String taskIdParam = request.getParameter("taskId");
        String feedback = request.getParameter("feedback");

        int projectId = 0;
        int taskId = 0;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            taskId = Integer.parseInt(taskIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);

        if (task != null && project != null && currentUser != null) {
            // KIỂM SOÁT BẢO MẬT: Chỉ DUY NHẤT Trưởng Dự Án (PM) mới được duyệt kế hoạch Cổng 1
            if (currentUser.getId() == project.getOwnerId()) {
                List<SubTask> subTasks = SubTaskDB.selectByTaskId(taskId);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmApprovePlanning(taskId, feedback, now);

                // Bắn thông báo thời gian thực 🔔 cho Task Lead
                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "🔒 PM Đã Phê Duyệt & Khóa Kế Hoạch",
                        "Trưởng Dự Án đã duyệt ma trận phân rã " + (subTasks != null ? subTasks.size() : 0) + " việc con của Task [" + task.getTitle() + "]. Kế hoạch đã khóa (Scope Lock), đội ngũ bắt tay thực thi!",
                        "/task?action=list&projectId=" + projectId,
                        "bi-lock-fill text-success"
                    );
                }

                // Thông báo lên Luồng Thảo luận
                String msgContent = "🔒 [PM KHÓA KẾ HOẠCH PHÂN RÃ]: Trưởng Dự Án đã duyệt danh mục " + (subTasks != null ? subTasks.size() : 0) + " việc con của Task [" + task.getTitle() + "]! Phạm vi công việc chính thức được KHÓA (Scope Baseline Lock). Đội ngũ bắt đầu thực thi!";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                session.setAttribute("toastSuccess", "Trưởng Dự Án đã phê duyệt và khóa kế hoạch phân rã thành công! Task chuyển sang Đang Làm.");
            } else {
                session.setAttribute("toastError", "Chỉ Trưởng Dự Án (PM) mới có thẩm quyền duyệt và khóa kế hoạch phân rã!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 12.7: Trưởng Dự Án (PM) Yêu Cầu Task Lead Bổ Sung / Chỉnh Sửa Kế Hoạch (Trả về ⚪ TODO)
     */
    private void handlePmRejectPlanning(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String taskIdParam = request.getParameter("taskId");
        String feedback = request.getParameter("feedback");

        int projectId = 0;
        int taskId = 0;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            taskId = Integer.parseInt(taskIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);

        if (task != null && project != null && currentUser != null) {
            if (currentUser.getId() == project.getOwnerId()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmRejectPlanning(taskId, feedback, now);

                // Bắn thông báo thời gian thực 🔔 cho Task Lead
                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "↩️ PM Yêu Cầu Chỉnh Sửa Kế Hoạch",
                        "Trưởng Dự Án yêu cầu bổ sung kế hoạch Task [" + task.getTitle() + "]: \"" + (feedback != null ? feedback : "Cần bóc tách thêm việc con") + "\"",
                        "/task?action=list&projectId=" + projectId,
                        "bi-arrow-counterclockwise text-warning"
                    );
                }

                // Thông báo lên Luồng Thảo luận
                String msgContent = "↩️ [PM YÊU CẦU ĐIỀU CHỈNH KẾ HOẠCH]: Trưởng Dự Án yêu cầu Task Lead hoàn thiện lại danh mục việc con của Task [" + task.getTitle() + "]. Lý do: \"" + (feedback != null && !feedback.trim().isEmpty() ? feedback : "Cần phân rã chi tiết hơn") + "\"";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                session.setAttribute("toastSuccess", "Đã trả về kế hoạch phân rã để Task Lead tiếp tục hoàn thiện.");
            } else {
                session.setAttribute("toastError", "Chỉ Trưởng Dự Án mới có quyền đưa ra quyết định này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 13: Trưởng Dự Án (PM) Phê Duyệt Nghiệm Thu Task Lớn ĐẠT (Chuyển sang 🟢 DONE)
     */
    private void handlePmApproveTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String taskIdParam = request.getParameter("taskId");
        String feedback = request.getParameter("feedback");
        String ratingParam = request.getParameter("qualityRating");

        int projectId = 0;
        int taskId = 0;
        int qualityRating = 5;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            taskId = Integer.parseInt(taskIdParam.trim());
            if (ratingParam != null && !ratingParam.trim().isEmpty()) {
                qualityRating = Integer.parseInt(ratingParam.trim());
            }
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);

        if (task != null && project != null && currentUser != null) {
            // KIỂM SOÁT BẢO MẬT: Chỉ DUY NHẤT Trưởng Dự Án (PM) mới có quyền PHÊ DUYỆT TỐI CAO
            if (currentUser.getId() == project.getOwnerId()) {
                // RÀNG BUỘC CHẤT LƯỢNG NGHIỆM THU: PM chỉ duyệt đạt khi toàn bộ việc con đã đạt 100%
                List<SubTask> subTasks = SubTaskDB.selectByTaskId(taskId);
                int progress = SubTaskDB.calculateProgress(taskId);
                if (subTasks != null && !subTasks.isEmpty() && progress < 100) {
                    session.setAttribute("toastError", 
                        "⚠️ Không thể duyệt đạt Task [" + task.getTitle() + "]! Vẫn còn " + (100 - progress) + "% việc con chưa được hoàn tất nghiệm thu.");
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmApproveTask(taskId, feedback, qualityRating, now);

                // Bắn thông báo thời gian thực 🔔 cho Task Lead
                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "🏆 PM Phê Duyệt Nghiệm Thu (" + qualityRating + " ⭐)",
                        "Trưởng Dự Án đã chính thức ký duyệt nghiệm thu hoàn tất 100% và chấm " + qualityRating + " sao cho Task [" + task.getTitle() + "]!",
                        "/task?action=list&projectId=" + projectId,
                        "bi-trophy-fill text-warning"
                    );
                }

                // Thông báo cúp vàng lên Thảo luận
                String msgContent = "🏆 [PM KÝ DUYỆT ĐÓNG TASK]: Trưởng Dự Án đã nghiệm thu hoàn thành 100% (Đánh giá: " + qualityRating + " ⭐) cho Task [" + task.getTitle() + "]! Lời nhận xét: \"" + (feedback != null && !feedback.trim().isEmpty() ? feedback : "Đạt chất lượng xuất sắc!") + "\"";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                session.setAttribute("toastSuccess", "Trưởng Dự Án đã phê duyệt nghiệm thu thành công! Task đã hoàn tất 100%.");
            } else {
                session.setAttribute("toastError", "Chỉ Trưởng Dự Án (PM) mới có thẩm quyền phê duyệt nghiệm thu tối cao!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 14: Trưởng Dự Án (PM) Yêu Cầu Cân Chỉnh Nhỏ (Chuyển sang 🔵 REVISE - Màu Xanh Dương)
     */
    private void handlePmReviseTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String taskIdParam = request.getParameter("taskId");
        String feedback = request.getParameter("feedback");

        int projectId = 0;
        int taskId = 0;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            taskId = Integer.parseInt(taskIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);

        if (task != null && project != null && currentUser != null) {
            if (currentUser.getId() == project.getOwnerId()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmReviseTask(taskId, feedback, now);

                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "🔵 PM Yêu Cầu Cân Chỉnh",
                        "Trưởng Dự Án dặn dò: \"" + (feedback != null ? feedback : "Cần cân chỉnh thêm một số chi tiết") + "\" đối với Task [" + task.getTitle() + "]",
                        "/task?action=list&projectId=" + projectId,
                        "bi-pencil-square text-primary"
                    );
                }

                session.setAttribute("toastSuccess", "Đã gửi yêu cầu cân chỉnh nhỏ (🔵 Xanh Dương) tới Task Lead!");
            } else {
                session.setAttribute("toastError", "Chỉ Trưởng Dự Án mới có quyền đưa ra quyết định này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 15: Trưởng Dự Án (PM) Trả Về Do Chưa Đạt (Chuyển sang 🔴 REJECTED - Màu Đỏ)
     */
    private void handlePmRejectTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String projectIdParam = request.getParameter("projectId");
        String taskIdParam = request.getParameter("taskId");
        String feedback = request.getParameter("feedback");

        int projectId = 0;
        int taskId = 0;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            taskId = Integer.parseInt(taskIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);

        if (task != null && project != null && currentUser != null) {
            if (currentUser.getId() == project.getOwnerId()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmRejectTask(taskId, feedback, now);

                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "🔴 PM Chưa Đạt Yêu Cầu",
                        "Trưởng Dự Án phản hồi lỗi: \"" + (feedback != null ? feedback : "Chưa đạt chuẩn đề ra") + "\" đối với Task [" + task.getTitle() + "]",
                        "/task?action=list&projectId=" + projectId,
                        "bi-exclamation-triangle-fill text-danger"
                    );
                }

                session.setAttribute("toastSuccess", "Đã trả về Task lớn và gửi phản hồi (🔴 Màu Đỏ) cho Task Lead!");
            } else {
                session.setAttribute("toastError", "Chỉ Trưởng Dự Án mới có quyền đưa ra quyết định này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Thuật toán tổng hợp Khối Lượng Công Việc (Workload Engine) cho từng thành viên:
     * 1. Đếm số Task lớn làm Lead & gom danh sách chi tiết các Task đó (leadTasks)
     * 2. Đếm số Việc Con được giao & số Việc Con đã xong [☑]
     * 3. Thu thập mảng relatedTaskIds để JavaScript lọc Kanban trong 0.01 giây
     */
    private List<UserWorkload> computeUserWorkloads(List<User> userList, List<Task> allTasks) {
        List<UserWorkload> workloadList = new ArrayList<>();
        // 1. Duyệt qua từng thành viên trong hệ thống
        for (User u : userList) {
            int leadTaskCount = 0;
            int subTaskCount = 0;
            int completedSubTaskCount = 0;
            List<Integer> relatedTaskIds = new ArrayList<>();
            List<Task> leadTasks = new ArrayList<>();

            // 2. Quét qua tất cả các Task của dự án
            for (Task t : allTasks) {
                // a. Nếu người này là Task Lead của Task lớn
                if (t.getAssigneeId() == u.getId()) {
                    leadTaskCount++;
                    leadTasks.add(t);
                    if (!relatedTaskIds.contains(t.getId())) {
                        relatedTaskIds.add(t.getId());
                    }
                }
                // b. Quét các Việc Con bên trong Task lớn này
                List<SubTask> subTasks = SubTaskDB.selectByTaskId(t.getId());
                for (SubTask st : subTasks) {
                    if (st.getAssigneeId() == u.getId()) {
                        subTaskCount++;
                        if (st.isCompleted()) {
                            completedSubTaskCount++;
                        }
                        if (!relatedTaskIds.contains(t.getId())) {
                            relatedTaskIds.add(t.getId());
                        }
                    }
                }
            }
            // 3. Đóng gói vào đối tượng UserWorkload
            UserWorkload uw = new UserWorkload(u, leadTaskCount, subTaskCount, completedSubTaskCount, relatedTaskIds, leadTasks);
            workloadList.add(uw);
        }
        return workloadList;

}
}
