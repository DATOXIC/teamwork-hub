package com.teamwork.controllers;
import com.teamwork.business.Project;
import com.teamwork.business.User;
import com.teamwork.data.ProjectDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
/**
 * Controller phụ trách Quản lý Dự án:
 * - Xem danh sách dự án (GET /project?action=list)
 * - Tạo dự án mới (POST /project?action=create)
 * - Xem chi tiết dự án (GET /project?action=detail)
 */
public class ProjectServlet extends HttpServlet
{
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException 
    {
        // 1. Đọc action từ URL (AuthFilter đã đảm bảo người dùng đã đăng nhập)
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) 
        {
            action = "list";
        }

        // 2. Điều phối xử lý GET
        switch (action) 
        {
            case "list":
                showProjectList(request, response);
                break;
            case "detail":
                showProjectDetail(request, response);
                break;
            default:
                showProjectList(request, response);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Lấy thông tin User đang đăng nhập từ Session để gán làm chủ dự án (ownerId)
        HttpSession session = request.getSession(false);
        User currentUser = (User) session.getAttribute("currentUser");
        
        // 2. Đọc action từ form submit
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty())
        {
            action = "create";
        }

        // 3. Điều phối xử lý POST
        switch (action) 
        {
            case "create":
                createProject(request, response, currentUser);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/project?action=list");
                break;
        }
    }
        /**
     * Nghiệp vụ 1: Lấy danh sách dự án và chuyển sang View (projects.jsp)
     */
    private void showProjectList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException 
    {
        // DÒNG 1: LẤY DỮ LIỆU TỪ KHO
        List<Project> projects = ProjectDB.selectAll();

        // DÒNG 2: ĐÓNG GÓI DANH SÁCH VÀO HỘP
        request.setAttribute("projects", projects);

        // DÒNG 3: BẬT ĐÈN BÁO TRÊN THANH MENU
        request.setAttribute("activeNav", "dashboard"); 

        // DÒNG 4: CHUYỂN GIAO CHO GIAO DIỆN HIỂN THỊ
        request.getRequestDispatcher("/projects.jsp").forward(request, response);
    }

    /**
     * Nghiệp vụ 2: Tạo dự án mới từ dữ liệu form Modal
     */
    private void createProject(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws ServletException, IOException {
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        // Validation kiểm tra rỗng
        if (name == null || name.trim().isEmpty()) {
            request.setAttribute("errorMessage", "Tên dự án không được để trống!");
            showProjectList(request, response);
            return;
        }
        // Lấy thời gian realtime hiện tại (Định dạng: dd/MM/yyyy HH:mm)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String createdAt = LocalDateTime.now().format(formatter);
        // Tạo đối tượng Project mới (ownerId là ID của User đang đăng nhập)
        Project newProject = new Project(
            0,
            name.trim(),
            (description != null ? description.trim() : ""),
            currentUser.getId(),
            createdAt,
            0, // totalTasks ban đầu = 0
            0  // doneTasks ban đầu = 0
        );
        // Lưu vào kho dữ liệu
        ProjectDB.insert(newProject);
        // Chuyển hướng (Redirect) về danh sách dự án (Mô hình PRG tránh lặp form)
        response.sendRedirect(request.getContextPath() + "/project?action=list");
    }


    /**
     * Nghiệp vụ 3: Xem chi tiết dự án (Chuẩn bị cho Sprint 3: Bảng Kanban)
     */
    private void showProjectDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException 
    {
        String projectIdStr = request.getParameter("projectId");
        try 
        {
            int projectId = Integer.parseInt(projectIdStr);
            Project project = ProjectDB.selectById(projectId);
            if (project != null) 
            {
                // Điều hướng sang phân hệ Task / Kanban của dự án đó (Sprint 3)
                response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                return;
            }
        } 
        catch (NumberFormatException e) 
        {
            // ID không hợp lệ
        }
        // Nếu không tìm thấy dự án, quay về Dashboard
        response.sendRedirect(request.getContextPath() + "/project?action=list");
    }
}
