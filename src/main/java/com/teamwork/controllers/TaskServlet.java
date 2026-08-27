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
                TaskDB.updateStatus(taskId, newStatus.trim());
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

        // KIỂM SOÁT THẨM QUYỀN: Chỉ Task Lead của chính Task này HOẶC Trưởng Dự Án mới được thêm việc con
        if (currentUser != null && parentTask != null && project != null) {
            if (currentUser.getId() == parentTask.getAssigneeId() || currentUser.getId() == project.getOwnerId()) {
                if (title != null && !title.trim().isEmpty() && taskId > 0) {
                    SubTask newSubTask = new SubTask(0, taskId, title.trim(), assigneeId, assigneeName, false);
                    SubTaskDB.insert(newSubTask);
                }
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
     * Nghiệp vụ 7: Xóa một việc con (BẢO VỆ PHÂN QUYỀN TASK LEAD / PM)
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
                // KIỂM SOÁT THẨM QUYỀN: Chỉ Task Lead của chính Task này HOẶC Trưởng Dự Án mới được xóa việc con
                if (currentUser.getId() == parentTask.getAssigneeId() || currentUser.getId() == project.getOwnerId()) {
                    SubTaskDB.delete(subTaskId);
                }
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
