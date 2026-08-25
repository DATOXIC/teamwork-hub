package com.teamwork.controllers;

import com.teamwork.business.Doc;
import com.teamwork.business.Message;
import com.teamwork.business.Project;
import com.teamwork.business.Task;
import com.teamwork.business.TaskDoc;
import com.teamwork.business.User;
import com.teamwork.data.DocDB;
import com.teamwork.data.MessageDB;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.TaskDB;
import com.teamwork.data.TaskDocDB;
import com.teamwork.data.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller phụ trách Bảng công việc Kanban (Tasks Module):
 * - Hiển thị 3 cột công việc TODO, IN_PROGRESS, DONE và danh sách tài liệu đính kèm (GET /task?action=list)
 * - Thêm công việc mới kèm đính kèm nhiều tài liệu hướng dẫn (POST /task?action=add)
 * - Cập nhật trạng thái công việc khi kéo thả HTML5 (POST /task?action=updateStatus)
 * - Xóa công việc và tự động dọn dẹp liên kết tài liệu (GET /task?action=delete)
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

            default:
                response.sendRedirect(request.getContextPath() + "/project?action=list");
                break;
        }
    }

    /**
     * Nghiệp vụ 1: Lấy toàn bộ dữ liệu 3 cột Kanban, danh sách tài liệu dự án và map liên kết TaskDoc
     */
    private void handleShowKanban(HttpServletRequest request, HttpServletResponse response, int projectId)
            throws ServletException, IOException {

        // 1. Kiểm tra dự án tồn tại
        Project project = ProjectDB.selectById(projectId);
        if (project == null) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 2. Lấy danh sách Task phân theo 3 cột trạng thái
        List<Task> todoTasks = TaskDB.selectByProjectAndStatus(projectId, "TODO");
        List<Task> inProgressTasks = TaskDB.selectByProjectAndStatus(projectId, "IN_PROGRESS");
        List<Task> doneTasks = TaskDB.selectByProjectAndStatus(projectId, "DONE");

        // 3. Lấy danh sách thành viên để gán người phụ trách
        List<User> userList = UserDB.selectAll();

        // 4. Lấy danh sách toàn bộ tài liệu Wiki của dự án này (để đổ vào dropdown đính kèm)
        List<Doc> docList = DocDB.selectByProjectId(projectId);

        // 5. Gom toàn bộ danh sách tài liệu đính kèm cho từng Task vào một Map (taskId -> List<TaskDoc>)
        Map<Integer, List<TaskDoc>> taskDocsMap = new HashMap<>();

        // 6. Gom toàn bộ danh sách bình luận riêng cho từng Task vào một Map (taskId -> List<Message>)
        Map<Integer, List<Message>> taskCommentsMap = new HashMap<>();

        // Nạp liên kết tài liệu và bình luận cho cột TODO
        for (Task t : todoTasks) {
            List<TaskDoc> attachedDocs = TaskDocDB.selectByTaskId(t.getId());
            taskDocsMap.put(t.getId(), attachedDocs);

            List<Message> comments = MessageDB.selectByTaskId(t.getId());
            taskCommentsMap.put(t.getId(), comments);
        }

        // Nạp liên kết tài liệu và bình luận cho cột IN_PROGRESS
        for (Task t : inProgressTasks) {
            List<TaskDoc> attachedDocs = TaskDocDB.selectByTaskId(t.getId());
            taskDocsMap.put(t.getId(), attachedDocs);

            List<Message> comments = MessageDB.selectByTaskId(t.getId());
            taskCommentsMap.put(t.getId(), comments);
        }

        // Nạp liên kết tài liệu và bình luận cho cột DONE
        for (Task t : doneTasks) {
            List<TaskDoc> attachedDocs = TaskDocDB.selectByTaskId(t.getId());
            taskDocsMap.put(t.getId(), attachedDocs);

            List<Message> comments = MessageDB.selectByTaskId(t.getId());
            taskCommentsMap.put(t.getId(), comments);
        }

        // 7. Đóng gói dữ liệu gửi sang tasks.jsp
        request.setAttribute("project", project);
        request.setAttribute("todoTasks", todoTasks);
        request.setAttribute("inProgressTasks", inProgressTasks);
        request.setAttribute("doneTasks", doneTasks);
        request.setAttribute("userList", userList);
        request.setAttribute("docList", docList);
        request.setAttribute("taskDocsMap", taskDocsMap);
        request.setAttribute("taskCommentsMap", taskCommentsMap);
        request.setAttribute("activeNav", "projects");

        // 7. Forward sang giao diện tasks.jsp
        request.getRequestDispatcher("/tasks.jsp").forward(request, response);
    }

    /**
     * Nghiệp vụ 2: Xóa một task khỏi dự án theo taskId (kèm dọn dẹp sạch liên kết TaskDoc)
     */
    private void handleDeleteTask(HttpServletRequest request, HttpServletResponse response, int projectId)
            throws IOException {

        String taskIdParam = request.getParameter("taskId");
        if (taskIdParam != null && !taskIdParam.trim().isEmpty()) {
            try {
                int taskId = Integer.parseInt(taskIdParam.trim());

                // 1. Dọn dẹp các liên kết Task-Doc trên RAM trước
                TaskDocDB.deleteByTaskId(taskId);

                // 2. Xóa Task trong TaskDB
                TaskDB.delete(taskId);
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

        // 1. Bắt các tham số từ Form
        String projectIdParam = request.getParameter("projectId");
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String priority = request.getParameter("priority");
        String dueDate = request.getParameter("dueDate");
        String assigneeIdParam = request.getParameter("assigneeId");
        String[] selectedDocIds = request.getParameterValues("docIds"); // Mảng các ID tài liệu được chọn

        // Ép kiểu projectId an toàn
        int projectId = 0;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // Validation tiêu đề bắt buộc
        if (title == null || title.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        if (priority == null || priority.trim().isEmpty()) {
            priority = "MEDIUM";
        }

        // Tìm thông tin người phụ trách
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

        // 2. Tạo đối tượng Task mới
        Task newTask = new Task(
            0,
            projectId,
            title.trim(),
            (description != null ? description.trim() : ""),
            "TODO", // Mặc định cột TODO
            priority,
            (dueDate != null ? dueDate.trim() : ""),
            assigneeId,
            assigneeName
        );

        // Lưu vào TaskDB và nhận lại ID vừa được cấp
        int newTaskId = TaskDB.insert(newTask);

        // 3. Lưu các tài liệu hướng dẫn đính kèm vào TaskDocDB
        if (selectedDocIds != null && selectedDocIds.length > 0) {
            for (String docIdStr : selectedDocIds) {
                try {
                    int docId = Integer.parseInt(docIdStr.trim());
                    Doc doc = DocDB.selectById(docId);
                    if (doc != null) {
                        // Thêm liên kết vào TaskDocDB
                        TaskDocDB.insert(newTaskId, docId, doc.getTitle());
                    }
                } catch (NumberFormatException e) {
                    // Bỏ qua nếu docId không hợp lệ
                }
            }
        }

        // 4. Áp dụng PRG pattern: Redirect về lại bảng Kanban
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

        // Redirect về lại bảng Kanban
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }
}
