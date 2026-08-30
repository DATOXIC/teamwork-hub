package com.teamwork.controllers;

import com.teamwork.business.Project;
import com.teamwork.business.ProjectInvite;
import com.teamwork.business.ProjectMember;
import com.teamwork.business.User;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.ProjectInviteDB;
import com.teamwork.data.ProjectMemberDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller phụ trách Quản lý Dự án:
 * - Xem danh sách dự án (GET /project?action=list)
 * - Tạo dự án mới (POST /project?action=create)
 * - Nạp danh sách Lời Mời đang chờ (pendingInvites) cho Dashboard
 */
public class ProjectServlet extends HttpServlet {
    private static final String PROJECT_CODE_PATTERN = "^[a-zA-Z0-9_-]{3,15}$";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Kiểm tra xác thực: người chưa đăng nhập phải được redirect về trang login
        HttpSession sessionCheck = request.getSession(false);
        User currentUserCheck = (sessionCheck != null) ? (User) sessionCheck.getAttribute("currentUser") : null;
        if (currentUserCheck == null) 
        {
            response.sendRedirect(request.getContextPath() + "/auth?action=viewLogin");
            return;
        }

        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) 
        {
            action = "list";
        }

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

        HttpSession session = request.getSession(false);

        // Phòng trường hợp: 1. Hết hạn Session 2. Fake Post
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) 
        {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) 
        {
            action = "create";
        }

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
     * Nghiệp vụ 1: Lấy danh sách dự án, lời mời chờ duyệt và chuyển sang View (projects.jsp)
     */
    private void showProjectList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // 1. LẤY DỮ LIỆU DỰ ÁN TỪ KHO VÀ PHÂN LOẠI
        List<Project> allProjects = ProjectDB.selectAll();
        List<Project> myProjects = new ArrayList<>();
        List<Project> otherProjects = new ArrayList<>();

        if (currentUser != null) 
        {
            List<Project> userProjects = ProjectMemberDB.selectProjectsByUserId(currentUser.getId());

            // Phân loại Project có trong Database
            for (Project p : allProjects) 
            {
                if (userProjects.contains(p)) 
                {
                    myProjects.add(p);
                } 
                else 
                {
                    otherProjects.add(p);
                }
            }
        } 
        else 
        {
            // Trống NULL --> Safe Code --> Giúp JSP không bị lỗi
            otherProjects.addAll(allProjects);
        }

        request.setAttribute("myProjects", myProjects);
        request.setAttribute("otherProjects", otherProjects);
        request.setAttribute("projects", allProjects); 

        // 2. TÍNH TOÁN SỐ LƯỢNG THÀNH VIÊN CHO TỪNG DỰ ÁN
        Map<Integer, Integer> memberCountMap = new HashMap<>();
        for (Project p : allProjects) 
        {
            memberCountMap.put(p.getId(), ProjectMemberDB.countMembers(p.getId()));
        }
        request.setAttribute("memberCountMap", memberCountMap);

        // 3. LẤY DANH SÁCH LỜI MỜI / YÊU CẦU ĐANG CHỜ NGƯỜI DÙNG DUYỆT (Hộp thư Dashboard)
        if (currentUser != null) 
        {
            List<ProjectInvite> pendingInvites = ProjectInviteDB.selectPendingByReceiverId(currentUser.getId());
            request.setAttribute("pendingInvites", pendingInvites);
        }

        // 4. XỬ LÝ THÔNG BÁO FLASH (Toast Messages)
        if (session != null) 
        {
            String toastSuccess = (String) session.getAttribute("toastSuccess");
            if (toastSuccess != null) 
            {
                request.setAttribute("toastSuccess", toastSuccess);
                session.removeAttribute("toastSuccess");
            }
            String toastError = (String) session.getAttribute("toastError");
            if (toastError != null) 
            {
                request.setAttribute("toastError", toastError);
                session.removeAttribute("toastError");
            }
        }

        request.setAttribute("activeNav", "dashboard");
        request.getRequestDispatcher("/projects.jsp").forward(request, response);
    }

    /**
     * Nghiệp vụ 2: Tạo dự án mới và tự động đăng ký Người tạo làm OWNER
     */
    private void createProject(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws ServletException, IOException {

        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String projectCode = (request.getParameter("projectCode") != null) 
                             ? request.getParameter("projectCode").trim().toUpperCase() 
                             : "";

        if (name == null || name.trim().isEmpty()) 
        {
            request.setAttribute("errorMessage", "Tên dự án không được để trống!");
            showProjectList(request, response);
            return;
        }

        // Nếu người dùng tự nhập
        if (!projectCode.isEmpty()) 
        {
            if (!projectCode.matches(PROJECT_CODE_PATTERN)) 
            {
                request.setAttribute("errorMessage", "Mã dự án từ 3-15 ký tự (chỉ gồm chữ cái, số, dấu '-' hoặc '_', không có khoảng trắng)!");
                showProjectList(request, response);
                return;
            }
            if (ProjectDB.selectByCode(projectCode) != null) 
            {
                request.setAttribute("errorMessage", "Mã dự án [" + projectCode + "] đã tồn tại trên hệ thống. Vui lòng chọn mã khác!");
                showProjectList(request, response);
                return;
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String createdAt = LocalDateTime.now().format(formatter);

        Project newProject = new Project(
            0,
            projectCode,
            name.trim(),
            (description != null ? description.trim() : ""),
            currentUser.getId(),
            createdAt,
            0,
            0
        );

        int newProjectId = ProjectDB.insert(newProject);

        // TỰ ĐỘNG ĐĂNG KÝ NGƯỜI TẠO LÀM OWNER TRONG PROJECTMEMBERDB
        ProjectMember ownerMember = new ProjectMember(
            newProjectId,
            currentUser.getId(),
            currentUser.getFullName(),
            currentUser.getEmail(),
            currentUser.getRole(),
            "OWNER",
            createdAt
        );
        ProjectMemberDB.insert(ownerMember);

        HttpSession session = request.getSession();
        session.setAttribute("toastSuccess", "Đã khởi tạo dự án [" + newProject.getName() + " (" + newProject.getProjectCode() + ")] thành công!");
        response.sendRedirect(request.getContextPath() + "/project?action=list");
    }

    /**
     * Nghiệp vụ 3: Xem chi tiết dự án (chuyển sang Bảng Kanban)
     * 
     */
    private void showProjectDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String projectIdStr = request.getParameter("projectId");
        try 
        {
            int projectId = Integer.parseInt(projectIdStr);
            Project project = ProjectDB.selectById(projectId);
            if (project != null) 
            {
                response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                return;
            }
        } 
        catch (NumberFormatException e) 
        {
            // Không làm gì, để rơi xuống redirect
        }
        response.sendRedirect(request.getContextPath() + "/project?action=list");
    }
}
