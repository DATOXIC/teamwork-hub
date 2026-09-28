package com.teamwork.controllers;

import com.teamwork.business.User;
import com.teamwork.data.UserDB;
import com.teamwork.util.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * AuthServlet — Controller xử lý toàn bộ luồng Xác thực (Authentication).
 *
 * <p><b>Các luồng được xử lý:</b></p>
 * <ul>
 *   <li>GET  /auth                  → Hiển thị trang Đăng nhập (viewLogin)</li>
 *   <li>GET  /auth?action=logout    → Đăng xuất, hủy Session và Cookie</li>
 *   <li>POST /auth (action=login)   → Xác thực tài khoản, tạo Session</li>
 *   <li>POST /auth (action=register)→ Tạo tài khoản mới, kiểm tra trùng lặp</li>
 * </ul>
 *
 * <p><b>Kiến trúc MVC:</b></p>
 * <pre>
 *   Browser → AuthServlet (Controller) → UserDB (Model / Data Layer) → login.jsp (View)
 * </pre>
 *
 * <p><b>TODO — Điểm mở rộng phổ biến (Extension Points):</b></p>
 * <ul>
 *   <li>Thêm giới hạn số lần đăng nhập sai (Login Attempt Limiter)</li>
 *   <li>Tích hợp OAuth2 / Google Sign-In</li>
 *   <li>Ghi ActivityLog mỗi lần đăng nhập/đăng xuất</li>
 *   <li>Gửi email xác nhận sau khi đăng ký (Email Verification)</li>
 * </ul>
 */
@WebServlet("/auth")
public class AuthServlet extends HttpServlet {

    // =========================================================================
    // CONSTANTS — Hằng số dùng chung trong toàn bộ Servlet
    // =========================================================================

    /** Pattern hợp lệ cho Tên đăng nhập: 4-20 ký tự chữ/số/gạch dưới */
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{4,20}$";

    /** Pattern hợp lệ cho địa chỉ Email */
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    // =========================================================================
    // doGet — XỬ LÝ CÁC YÊU CẦU HTTP GET
    // =========================================================================

    /**
     * Xử lý GET: hiển thị trang login hoặc thực hiện đăng xuất.
     *
     * <p>Các action GET được hỗ trợ:
     * <ul>
     *   <li>{@code logout}    → {@link #processLogout}</li>
     *   <li>{@code viewLogin} → Chuyển sang login.jsp (mặc định)</li>
     * </ul>
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        // Thiết lập bảng mã UTF-8 để không bị lỗi tiếng Việt
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        String action = request.getParameter("action");
        if (action == null) {
            action = "viewLogin";
        }

        switch (action) {
            case "logout":
                processLogout(request, response);
                break;

            case "viewLogin":
            default:
                // Nếu người dùng đã đăng nhập hợp lệ, chuyển thẳng vào Dashboard
                HttpSession session = request.getSession(false);
                if (session != null) {
                    if (session.getAttribute("currentUser") != null) {
                        response.sendRedirect(request.getContextPath() + "/project?action=list");
                        return;
                    }

                    // Truyền Flash Message (thành công đăng ký) sang View một lần duy nhất
                    String successMsg = (String) session.getAttribute("successMessage");
                    String registeredUser = (String) session.getAttribute("registeredUsername");

                    if (successMsg != null) {
                        request.setAttribute("successMessage", successMsg);
                        session.removeAttribute("successMessage"); // Xóa ngay sau khi dùng (Flash Pattern)
                    }
                    if (registeredUser != null) {
                        request.setAttribute("username", registeredUser);
                        session.removeAttribute("registeredUsername");
                    }
                }

                // Đọc Cookie "Ghi nhớ đăng nhập" để điền sẵn username vào form
                if (request.getAttribute("username") == null && request.getCookies() != null) {
                    for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                        if ("teamwork_remember_user".equals(c.getName())
                                && c.getValue() != null
                                && !c.getValue().trim().isEmpty()) {
                            request.setAttribute("username", c.getValue().trim());
                            request.setAttribute("rememberChecked", true);
                            break;
                        }
                    }
                }

                // Chuyển tiếp sang trang Đăng nhập (login.jsp)
                request.getRequestDispatcher("/login.jsp").forward(request, response);
                break;
        }
    }

    // =========================================================================
    // doPost — XỬ LÝ CÁC YÊU CẦU HTTP POST
    // =========================================================================

    /**
     * Xử lý POST: điều phối sang các handler Đăng nhập / Đăng ký.
     *
     * <p>Các action POST được hỗ trợ:
     * <ul>
     *   <li>{@code login}    → {@link #processLogin}</li>
     *   <li>{@code register} → {@link #processRegister}</li>
     * </ul>
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        String action = request.getParameter("action");
        if (action == null) {
            action = "login";
        }

        switch (action) {
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

    // =========================================================================
    // NGHIỆP VỤ 1: ĐĂNG NHẬP (Login Flow)
    // =========================================================================

    /**
     * Xử lý luồng Đăng nhập (Login Flow).
     *
     * <p><b>Quy trình:</b>
     * <ol>
     *   <li>Lấy username & password từ form</li>
     *   <li>Kiểm tra rỗng (Empty Validation)</li>
     *   <li>Truy vấn DB để xác thực thông tin</li>
     *   <li>Thành công → Tạo Session + xử lý Cookie "Ghi nhớ" → Redirect Dashboard</li>
     *   <li>Thất bại → Trả lỗi về View, giữ nguyên username đã nhập</li>
     * </ol>
     *
     * <p><b>TODO — Mở rộng:</b>
     * <pre>
     *   // Ví dụ: Ghi log hoạt động khi đăng nhập thành công
     *   ActivityLogDB.insert(new ActivityLog(user.getId(), "LOGIN", "Đăng nhập thành công"));
     *
     *   // Ví dụ: Giới hạn N lần thử sai — đọc đếm từ Session
     *   Integer failCount = (Integer) session.getAttribute("loginFailCount");
     *   if (failCount != null && failCount >= 5) { /* khóa tài khoản tạm thời *&#47; }
     * </pre>
     */
    private void processLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String remember = request.getParameter("remember");
        boolean isRemember = "on".equalsIgnoreCase(remember)
                || "true".equalsIgnoreCase(remember)
                || "1".equals(remember);

        // 1. Kiểm tra rỗng — Empty Validation
        if (username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            request.setAttribute("errorMessage", "Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu!");
            request.setAttribute("username", username); // Giữ lại username để không phải gõ lại
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        // 2. Gọi Data Layer để xác thực (kiểm tra username + mật khẩu đã hash)
        User user = UserDB.selectByCredentials(username.trim(), password.trim());

        if (user != null) {
            // Đăng nhập THÀNH CÔNG → Tạo Session, lưu đối tượng User
            HttpSession session = request.getSession();
            session.setAttribute("currentUser", user);

            // 3. Xử lý Cookie "Ghi nhớ đăng nhập" (Remember Me — hạn 14 ngày)
            String cookiePath = request.getContextPath().isEmpty() ? "/" : request.getContextPath();
            jakarta.servlet.http.Cookie rememberCookie =
                    new jakarta.servlet.http.Cookie("teamwork_remember_user", user.getUsername());
            rememberCookie.setPath(cookiePath);
            rememberCookie.setHttpOnly(true);  // Bảo mật: chống XSS đọc Cookie
            rememberCookie.setMaxAge(isRemember ? 14 * 24 * 60 * 60 : 0); // 14 ngày hoặc xóa ngay
            response.addCookie(rememberCookie);

            // Điều hướng sang Dashboard danh sách dự án
            response.sendRedirect(request.getContextPath() + "/project?action=list");

        } else {
            // Đăng nhập THẤT BẠI → Thông báo lỗi, giữ lại username
            request.setAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác!");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }

    // =========================================================================
    // NGHIỆP VỤ 2: ĐĂNG KÝ (Register Flow)
    // =========================================================================

    /**
     * Xử lý luồng Đăng ký tài khoản mới (Register Flow).
     *
     * <p><b>Quy trình:</b>
     * <ol>
     *   <li>Server-side Validation: username pattern, độ dài password, email format</li>
     *   <li>Kiểm tra trùng lặp username và email trong DB</li>
     *   <li>Hash mật khẩu trước khi lưu ({@link PasswordUtil#hashPassword})</li>
     *   <li>Thêm User mới vào DB, redirect về trang Login với thông báo thành công</li>
     * </ol>
     *
     * <p><b>TODO — Thêm rule validation mới theo mẫu sau:</b>
     * <pre>
     *   if (/* điều kiện vi phạm *&#47;) {
     *       request.setAttribute("regError", "Thông báo lỗi tương ứng");
     *       forwardRegisterForm(request, response, username, fullName, email);
     *       return;
     *   }
     * </pre>
     */
    private void processRegister(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        String username        = request.getParameter("username");
        String password        = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String fullName        = request.getParameter("fullName");
        String email           = request.getParameter("email");

        // 1. Server-side Validation — kiểm tra từng trường theo thứ tự
        if (username == null || username.trim().length() < 4 || !username.trim().matches(USERNAME_PATTERN)) {
            request.setAttribute("regError", "Tên đăng nhập từ 4-20 ký tự (chỉ gồm chữ, số và dấu _, không có khoảng trắng)!");
            forwardRegisterForm(request, response, username, fullName, email);
            return;
        }

        if (password == null || password.trim().length() < 6) {
            request.setAttribute("regError", "Mật khẩu phải có ít nhất 6 ký tự!");
            forwardRegisterForm(request, response, username, fullName, email);
            return;
        }

        if (!password.equals(confirmPassword)) {
            request.setAttribute("regError", "Mật khẩu xác nhận không khớp!");
            forwardRegisterForm(request, response, username, fullName, email);
            return;
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            request.setAttribute("regError", "Vui lòng nhập họ và tên đầy đủ!");
            forwardRegisterForm(request, response, username, fullName, email);
            return;
        }

        // 2. Kiểm tra trùng lặp trong DB
        if (UserDB.selectByUsername(username.trim()) != null) {
            request.setAttribute("regError", "Tên đăng nhập này đã được sử dụng. Vui lòng chọn tên khác!");
            forwardRegisterForm(request, response, username, fullName, email);
            return;
        }

        if (email == null || !email.trim().matches(EMAIL_PATTERN)) {
            request.setAttribute("regError", "Email không hợp lệ!");
            forwardRegisterForm(request, response, username, fullName, email);
            return;
        }

        if (UserDB.selectByUsernameOrEmail(email.trim()) != null) {
            request.setAttribute("regError", "Email này đã được đăng ký trong hệ thống!");
            forwardRegisterForm(request, response, username, fullName, email);
            return;
        }

        // 3. Tạo đối tượng User mới và thêm vào DB
        User newUser = new User(
            0,                                          // ID tự động cấp bởi DB
            username.trim(),
            PasswordUtil.hashPassword(password.trim()), // Hash mật khẩu trước khi lưu
            fullName.trim(),
            (email != null ? email.trim() : ""),
            "MEMBER",                                   // Mặc định vai trò mới là MEMBER
            "images/default_avatar.png"
        );
        UserDB.insert(newUser);

        // 4. Đăng ký thành công → Flash Message → Redirect về Login, mở sẵn tab Đăng nhập
        HttpSession session = request.getSession();
        session.setAttribute("successMessage", "Đăng ký tài khoản thành công! Bạn có thể đăng nhập ngay.");
        session.setAttribute("registeredUsername", username.trim());
        response.sendRedirect(request.getContextPath() + "/auth?action=viewLogin");
    }

    // =========================================================================
    // NGHIỆP VỤ 3: ĐĂNG XUẤT (Logout Flow)
    // =========================================================================

    /**
     * Xử lý luồng Đăng xuất (Logout Flow).
     *
     * <p><b>Quy trình:</b>
     * <ol>
     *   <li>Hủy toàn bộ Session trên server</li>
     *   <li>Xóa sạch Cookie xác thực và Cookie dự án cuối</li>
     *   <li>Redirect về trang Login</li>
     * </ol>
     *
     * <p><b>TODO — Mở rộng:</b>
     * <pre>
     *   // Ghi log đăng xuất
     *   ActivityLogDB.insert(new ActivityLog(userId, "LOGOUT", "Đăng xuất hệ thống"));
     * </pre>
     */
    private void processLogout(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        HttpSession session = request.getSession(false); // false = không tự tạo session mới
        if (session != null) {
            session.removeAttribute("currentUser");
            session.invalidate(); // Hủy toàn bộ session trên máy chủ
        }

        // Xóa sạch Cookie xác thực và Cookie dự án đã lưu
        String cookiePath = request.getContextPath().isEmpty() ? "/" : request.getContextPath();

        jakarta.servlet.http.Cookie rememberCookie =
                new jakarta.servlet.http.Cookie("teamwork_remember_user", "");
        rememberCookie.setMaxAge(0); // MaxAge = 0 → Xóa cookie ngay lập tức
        rememberCookie.setPath(cookiePath);
        rememberCookie.setHttpOnly(true);
        response.addCookie(rememberCookie);

        jakarta.servlet.http.Cookie lastProjectCookie =
                new jakarta.servlet.http.Cookie("last_project_id", "");
        lastProjectCookie.setMaxAge(0);
        lastProjectCookie.setPath(cookiePath);
        lastProjectCookie.setHttpOnly(true);
        response.addCookie(lastProjectCookie);

        // Chuyển hướng về trang Đăng nhập
        response.sendRedirect(request.getContextPath() + "/auth?action=viewLogin");
    }

    // =========================================================================
    // HELPER — Phương thức tiện ích dùng nội bộ
    // =========================================================================

    /**
     * Chuyển tiếp lại form Đăng ký, giữ nguyên giá trị người dùng đã nhập
     * khi có lỗi validation (tránh phải gõ lại từ đầu).
     *
     * @param username Tên đăng nhập đã nhập
     * @param fullName Họ và tên đã nhập
     * @param email    Email đã nhập
     */
    private void forwardRegisterForm(HttpServletRequest request, HttpServletResponse response,
                                     String username, String fullName, String email)
            throws ServletException, IOException
    {
        request.setAttribute("regUsername", username);
        request.setAttribute("regFullName", fullName);
        request.setAttribute("regEmail", email);
        request.setAttribute("activeTab", "register"); // Mở sẵn tab Đăng ký trong login.jsp
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }
}
