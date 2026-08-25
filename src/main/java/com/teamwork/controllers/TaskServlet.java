package com.teamwork.controllers;

import java.io.IOException;
import java.util.List;

import com.teamwork.data.ProjectDB;
import com.teamwork.data.TaskDB;
import com.teamwork.data.UserDB;
import com.teamwork.business.Project;
import com.teamwork.business.User;
import com.teamwork.business.Task;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class TaskServlet extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        // Xác định ID của Project
        String projectIdParam = request.getParameter("projectId");
        int projectId = 0;

        if (projectIdParam == null || projectIdParam.trim().isEmpty())
        {
                response.sendRedirect(request.getContextPath()+"/project?action=list");
                return;
        }

        try
        {
                projectId = Integer.parseInt(projectIdParam.trim());
        }
        catch (NumberFormatException e)
        {
                response.sendRedirect(request.getContextPath()+"/project?action=list");
                return;
        }

        // Xử lý hành động của người dùng (LIST hoặc DELETE)
        String action = request.getParameter("action");

        if (action == null || action.trim().isEmpty()) action = "list";

        switch (action) 
        {
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
            throws ServletException, IOException
    {
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) 
        {
            action = "add"; 
        }

        switch (action) 
        {
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
     * Nghiệp vụ 1: Lấy toàn bộ dữ liệu 3 cột Kanban và chuyển sang giao diện tasks.jsp
     */
    private void handleShowKanban(HttpServletRequest request, HttpServletResponse response, int projectId)
            throws ServletException, IOException 
        {
                // Xác định ID project
                Project project = ProjectDB.selectById(projectId);
                
                if( project == null)
                {
                        response.sendRedirect(request.getContextPath() + "/project?action=list");
                        return;
                }

                // LẤY DANH SÁCH TASK PHÂN THEO 3 CỘT TRẠNG THÁI
                List<Task> todoTasks = TaskDB.selectByProjectAndStatus(projectId, "TODO");
                List<Task> inProgressTasks = TaskDB.selectByProjectAndStatus(projectId, "IN_PROGRESS");
                List<Task> doneTasks = TaskDB.selectByProjectAndStatus(projectId, "DONE");

                // DANH SÁCH THÀNH VIÊN
                List<User> userList = UserDB.selectAll();
                

                // Đóng gói và gửi cho JSP để hiển thị
                request.setAttribute("project", project);
                request.setAttribute("todoTasks", todoTasks);
                request.setAttribute("inProgressTasks", inProgressTasks);
                request.setAttribute("doneTasks", doneTasks);
                request.setAttribute("userList", userList);
                request.setAttribute("activeNav", "projects");

                // GỦI CHO JSP
                request.getRequestDispatcher("/tasks.jsp").forward(request, response);
        }

            /**
     * Nghiệp vụ 2: Xóa một task khỏi dự án theo taskId
     */
    private void handleDeleteTask(HttpServletRequest request, HttpServletResponse response, int projectId)
            throws IOException 
        {
        // Xác định ID Task
        String taskIdParam = request.getParameter("taskId");


        if (taskIdParam != null && !taskIdParam.trim().isEmpty()) 
        {
            try 
            {
                int taskId = Integer.parseInt(taskIdParam.trim());
                TaskDB.delete(taskId);
            } 
            catch (NumberFormatException e) 
            {

            }
        }

        // Xóa xong -> Dùng Redirect tải lại đúng trang Kanban của dự án này
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

        /**
     * Nghiệp vụ 3: Tiếp nhận form thêm task mới và lưu vào TaskDB
     */
    private void handleAddTask(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // BƯỚC 1: BẮT CÁC THAM SỐ TỪ FORM GỬI LÊN
        String projectIdParam = request.getParameter("projectId");
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String priority = request.getParameter("priority");
        String dueDate = request.getParameter("dueDate");
        String assigneeIdParam = request.getParameter("assigneeId");

        // Ép kiểu projectId an toàn
        int projectId = 0;
        try 
        {
            projectId = Integer.parseInt(projectIdParam.trim());
        } 
        catch (Exception e) 
        {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // KIỂM TRA TIÊU ĐỀ
        if (title == null || title.trim().isEmpty()) {
            // Nếu người dùng không nhập tiêu đề -> Tải lại trang Kanban
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Xử lý giá trị mặc định cho priority nếu bị rỗng
        if (priority == null || priority.trim().isEmpty()) {
            priority = "MEDIUM";
        }

        // ÉP KIỂU assigneeId VÀ TÌM TÊN NGƯỜI PHỤ TRÁCH
        int assigneeId = 0;
        String assigneeName = "Chưa phân công";

        if (assigneeIdParam != null && !assigneeIdParam.trim().isEmpty()) {
            try {
                assigneeId = Integer.parseInt(assigneeIdParam.trim());

                // Tra cứu trong UserDB để lấy tên người dùng
                User assignee = UserDB.selectById(assigneeId);
                if (assignee != null) {
                    assigneeName = assignee.getFullName();
                }

            } 
            catch (NumberFormatException e) {
                assigneeId = 0;
            }
        }

        // BƯỚC 4: TẠO ĐỐI TƯỢNG TASK MỚI (Mặc định status là "TODO")
        Task newTask = new Task(
            0,                      // id sẽ được TaskDB tự động tăng
            projectId,              // Thuộc dự án này
            title.trim(),           // Tiêu đề
            (description != null ? description.trim() : ""), // Mô tả
            "TODO",                 // Mặc định luôn ở cột "Cần làm"
            priority,               // Mức độ ưu tiên
            (dueDate != null ? dueDate.trim() : ""),         // Hạn chót
            assigneeId,             // ID người làm
            assigneeName            // Tên người làm
        );

        // Lưu vào kho dữ liệu RAM
        TaskDB.insert(newTask);

        // BƯỚC 5: ÁP DỤNG MÔ HÌNH PRG (Chuyển hướng về lại trang Bảng Kanban)
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

        /**
     * Nghiệp vụ 4: Cập nhật trạng thái của Task khi người dùng kéo thả chuột hoặc bấm chuyển cột
     */
    private void handleUpdateTaskStatus(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // BẮT CÁC THAM SỐ GỬI LÊN
        String projectIdParam = request.getParameter("projectId");
        String taskIdParam = request.getParameter("taskId");
        String newStatus = request.getParameter("newStatus");

        int projectId = 0;
        int taskId = 0;

        // ÉP KIỂU VÀ KIỂM TRA TÍNH HỢP LỆ
        try {
            if (projectIdParam != null) 
                {
                projectId = Integer.parseInt(projectIdParam.trim());
            }
            if (taskIdParam != null) {
                taskId = Integer.parseInt(taskIdParam.trim());
            }
        } 
        catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // KIỂM TRA TRẠNG THÁI MỚI VÀ CẬP NHẬT VÀO KHO DỮ LIỆU
        if (newStatus != null && !newStatus.trim().isEmpty() && taskId > 0) {
            // Chuẩn hóa IN HOA
            String cleanStatus = newStatus.trim().toUpperCase();

            // Đảm bảo trạng thái chỉ thuộc 1 trong 3 cột hợp lệ
            if (cleanStatus.equals("TODO") || cleanStatus.equals("IN_PROGRESS") || cleanStatus.equals("DONE")) {
                TaskDB.updateStatus(taskId, cleanStatus);
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }
}
