package com.teamwork.controllers;

import com.teamwork.business.User;
import com.teamwork.data.UserDB;
import com.teamwork.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Controller chịu trách nhiệm xử lý toàn bộ luồng Xác thực (Authentication):
 * - Đăng nhập (action = login)
 * - Đăng ký (action = register)
 * - Đăng xuất (action = logout)
 */
public class AuthServlet extends HttpServlet {
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{4,20}$";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException 
    {
        // Thiết lập bảng mã UTF-8 để không bị lỗi tiếng Việt
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        String action = request.getParameter("action");
        if (action == null) 
        {
            action = "viewLogin";
        }

        // Xử lý các yêu cầu GET
        switch (action) 
        {
            case "logout":
                processLogout(request, response);
                break;
            case "viewLogin":
            default:
                HttpSession session = request.getSession(false);
                if (session != null) 
                {
                    String successMsg = (String) session.getAttribute("successMessage");
                    String registeredUser = (String) session.getAttribute("registeredUsername");
                    
                    if (successMsg != null) 
                    {
                        request.setAttribute("successMessage", successMsg);
                        session.removeAttribute("successMessage"); // Xóa ngay sau khi dùng
                    }
                    if (registeredUser != null) 
                    {
                        request.setAttribute("username", registeredUser);
                        session.removeAttribute("registeredUsername"); // Xóa ngay sau khi dùng
                    }
                }
                // Chuyển tiếp về trang đăng nhập
                request.getRequestDispatcher("/login.jsp").forward(request, response);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        String action = request.getParameter("action");
        if (action == null) 
        {
            action = "login";
        }

        // Xử lý các yêu cầu POST
        switch (action) 
        {
            case "login":
                processLogin(request, response);
                break;
            case "register":
                processRegister(request, response);
                break;
            default:
                request.getRequestDispatcher("/login.jsp").forward(request, response);
                break;
        }
    }

    /**
     * Nghiệp vụ 1: Xử lý ĐĂNG NHẬP
     */
    private void processLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // 1. Kiểm tra rỗng (Validation)
        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) 
        {
            request.setAttribute("errorMessage", "Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu!");
            request.setAttribute("username", username); // Giữ lại username đã nhập để người dùng không phải gõ lại
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        // 2. Gọi Data Layer để xác thực
        User user = UserDB.selectByCredentials(username.trim(), password.trim());

        if (user != null) {
            // Đăng nhập THÀNH CÔNG:
            // Tạo hoặc lấy Session hiện tại, lưu đối tượng User vào Session
            HttpSession session = request.getSession();
            session.setAttribute("currentUser", user);

            // Điều hướng sang trang danh sách dự án (Project Dashboard)
            // Dùng redirect để trình duyệt đổi URL, tránh việc F5 submit lại form đăng nhập
            response.sendRedirect(request.getContextPath() + "/project?action=list");
        } 
        else 
        {
            // Đăng nhập THẤT BẠI:
            request.setAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác!");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }

    /**
     * Nghiệp vụ 2: Xử lý ĐĂNG KÝ
     */
    private void processRegister(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");

        // 1. Server-side Validation
        if (username == null || username.trim().length() < 4 || !username.trim().matches(USERNAME_PATTERN)) 
        {
            request.setAttribute("regError", "Tên đăng nhập từ 4-20 ký tự (chỉ gồm chữ, số và dấu _, không có khoảng trắng)!");
            forwardRegisterForm(request, response, username, fullName, email);
            return;
        }

        if (password == null || password.trim().length() < 6)
        {
            request.setAttribute("regError", "Mật khẩu phải có ít nhất 6 ký tự!");
            forwardRegisterForm(request, response, username, fullName, email);
            return;
        }

        if (!password.equals(confirmPassword)) 
        {
            request.setAttribute("regError", "Mật khẩu xác nhận không khớp!");
            forwardRegisterForm(request, response, username, fullName, email);
            return;
        }

        if (fullName == null || fullName.trim().isEmpty()) 
        {
            request.setAttribute("regError", "Vui lòng nhập họ và tên đầy đủ!");
            forwardRegisterForm(request, response, username, fullName, email);
            return;
        }

        // 2. Kiểm tra xem username đã tồn tại trong hệ thống chưa
        if (UserDB.selectByUsername(username.trim()) != null) 
        {
            request.setAttribute("regError", "Tên đăng nhập này đã được sử dụng. Vui lòng chọn tên khác!");
            forwardRegisterForm(request, response, username, fullName, email);
            return;
        }

        if(email == null || !email.trim().matches(EMAIL_PATTERN))
        {
            request.setAttribute("regError", "Email không hợp lệ!");
            forwardRegisterForm(request, response, username, fullName, email);
            return;
        }

        if (UserDB.selectByUsername(username.trim()) != null) 
        {
            request.setAttribute("regError", "Tên đăng nhập này đã được sử dụng!");
            forwardRegisterForm(request, response, username, fullName, email);
            return;
        }

        if (UserDB.selectByUsernameOrEmail(email.trim()) != null) 
        {
        request.setAttribute("regError", "Email này đã được đăng ký trong hệ thống!");
        forwardRegisterForm(request, response, username, fullName, email);
        return;
        }

        // 3. Tạo đối tượng User mới và thêm vào DB
        User newUser = new User
        (
            0, // ID sẽ được UserDB tự động cấp tăng dần
            username.trim(),
            PasswordUtil.hashPassword(password.trim()),
            fullName.trim(),
            (email != null ? email.trim() : ""),
            "MEMBER", // Mặc định tài khoản đăng ký mới là MEMBER
            "images/default_avatar.png"
        );

        UserDB.insert(newUser);

        // Đăng ký thành công -> Thông báo và mở sẵn tab Đăng nhập
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Đăng ký tài khoản thành công! Bạn có thể đăng nhập ngay.");
        session.setAttribute("registeredUsername", username.trim());
        response.sendRedirect(request.getContextPath() + "/auth?action=viewLogin");
    }

    /**
     * Nghiệp vụ 3: Xử lý ĐĂNG XUẤT
     */
    private void processLogout(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false); // Lấy session nếu có, không tự động tạo mới
        if (session != null) 
        {
            session.removeAttribute("currentUser");
            session.invalidate(); // Hủy toàn bộ session trên máy chủ
        }
        // Chuyển hướng người dùng về trang đăng nhập
        response.sendRedirect(request.getContextPath() + "/auth?action=viewLogin");
    }

    /**
     * Helper giữ lại các giá trị form khi đăng ký bị lỗi
     */
    private void forwardRegisterForm(HttpServletRequest request, HttpServletResponse response,
                                    String username, String fullName, String email)
            throws ServletException, IOException {
        request.setAttribute("regUsername", username);
        request.setAttribute("regFullName", fullName);
        request.setAttribute("regEmail", email);
        request.setAttribute("activeTab", "register"); // Để trang login.jsp biết mở tab Đăng ký
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }
}
