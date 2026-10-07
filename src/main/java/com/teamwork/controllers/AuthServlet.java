package com.teamwork.controllers;

import com.teamwork.business.User;
import com.teamwork.data.UserDB;
import com.teamwork.util.LoginAttemptLimiter;
import com.teamwork.util.MailUtil;
import com.teamwork.util.OtpChallenge;
import com.teamwork.util.PasswordUtil;
import com.teamwork.util.RememberMeToken;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Logger;

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
 *   <li>Tích hợp OAuth2 / Google Sign-In</li>
 *   <li>Gửi email xác nhận sau khi đăng ký (Email Verification)</li>
 * </ul>
 */
@WebServlet("/auth")
public class AuthServlet extends BaseServlet {

    // =========================================================================
    // CONSTANTS — Hằng số dùng chung trong toàn bộ Servlet
    // =========================================================================

    /** Pattern hợp lệ cho Tên đăng nhập: 4-20 ký tự chữ/số/gạch dưới */
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{4,20}$";

    /** Pattern hợp lệ cho địa chỉ Email */
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    private static final Logger LOGGER = Logger.getLogger(AuthServlet.class.getName());

    /** Bộ giới hạn đăng nhập sai dùng chung cho toàn ứng dụng */
    private static final LoginAttemptLimiter LOGIN_LIMITER = new LoginAttemptLimiter();

    // Giới hạn gửi email OTP: 3 lần / 15 phút cho mỗi tài khoản (chống spam hộp thư nạn nhân),
    // 20 lần / 15 phút cho mỗi IP (cao hơn vì sau proxy như Render nhiều người có thể chung 1 IP)
    private static final long OTP_WINDOW_MS = 15 * 60 * 1000L;
    private static final LoginAttemptLimiter OTP_USER_LIMITER = new LoginAttemptLimiter(3, OTP_WINDOW_MS, OTP_WINDOW_MS);
    private static final LoginAttemptLimiter OTP_IP_LIMITER = new LoginAttemptLimiter(20, OTP_WINDOW_MS, OTP_WINDOW_MS);

    /** Khóa Session lưu trạng thái OTP quên mật khẩu */
    private static final String SESSION_OTP = "pwResetChallenge";
    private static final String SESSION_OTP_USER = "pwResetUsername";

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
            // "logout" chỉ nhận qua POST (doPost): link GET có thể bị trang khác kích hoạt để đăng xuất người dùng

            case "forgot":
                clearResetState(request.getSession());
                forwardForgot(request, response, 1);
                break;

            case "viewLogin":
            default:
                // Khung "Tài khoản mẫu thử nghiệm" (bản demo cho giảng viên): hiện mặc định,
                // ẩn bằng biến môi trường TEAMWORK_DEMO_ACCOUNTS=off khi dùng thật.
                // ▶ JSP: login.jsp đọc bằng ${showDemoAccounts}
                request.setAttribute("showDemoAccounts", !"off".equalsIgnoreCase(System.getenv("TEAMWORK_DEMO_ACCOUNTS")));

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
                        // ▶ JSP: login.jsp đọc bằng ${successMessage}
                        request.setAttribute("successMessage", successMsg);
                        session.removeAttribute("successMessage"); // Xóa ngay sau khi dùng (Flash Pattern)
                    }
                    if (registeredUser != null) {
                        // ▶ JSP: login.jsp đọc bằng ${username}
                        request.setAttribute("username", registeredUser);
                        session.removeAttribute("registeredUsername");
                    }
                    // Lỗi flash (vd. CsrfFilter chặn form đăng nhập để quá lâu) → hiện như lỗi đăng nhập
                    String toastError = (String) session.getAttribute("toastError");
                    if (toastError != null) {
                        request.setAttribute("errorMessage", toastError);
                        session.removeAttribute("toastError");
                    }
                }

                // Đọc Cookie "Ghi nhớ đăng nhập" để điền sẵn username vào form
                if (request.getAttribute("username") == null && request.getCookies() != null) {
                    for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                        String rememberedName = "teamwork_remember_user".equals(c.getName())
                                ? RememberMeToken.peekUsername(c.getValue()) : null;
                        if (rememberedName != null && !rememberedName.trim().isEmpty()) {
                            request.setAttribute("username", rememberedName.trim());
                            // ▶ JSP: login.jsp đọc bằng ${rememberChecked}
                            request.setAttribute("rememberChecked", true);
                            break;
                        }
                    }
                }

                // Chuyển tiếp sang trang Đăng nhập (login.jsp)
                // ▶ forward → login.jsp (các setAttribute ở trên chính là dữ liệu JSP hiển thị)
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
            case "logout":
                processLogout(request, response);
                break;
            case "register":
                processRegister(request, response);
                break;
            case "forgotRequest":
                processForgotRequest(request, response);
                break;
            case "forgotVerify":
                processForgotVerify(request, response);
                break;
            case "forgotReset":
                processForgotReset(request, response);
                break;
            default:
                // ▶ forward → login.jsp (các setAttribute ở trên chính là dữ liệu JSP hiển thị)
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
        // Giữ trạng thái ô "Ghi nhớ" khi phải hiển thị lại form đăng nhập do lỗi
        request.setAttribute("rememberChecked", isRemember);

        // 1. Kiểm tra rỗng — Empty Validation
        if (username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            // ▶ JSP: login.jsp đọc bằng ${errorMessage}
            request.setAttribute("errorMessage", "Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu!");
            request.setAttribute("username", username); // Giữ lại username để không phải gõ lại
            // ▶ forward → login.jsp (các setAttribute ở trên chính là dữ liệu JSP hiển thị)
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        // 2. Giới hạn đăng nhập sai: đang bị khóa thì từ chối, kể cả khi mật khẩu đúng
        String limiterKey = LoginAttemptLimiter.key(username, request.getRemoteAddr());
        long now = System.currentTimeMillis();
        long lockedSeconds = LOGIN_LIMITER.lockedSeconds(limiterKey, now);
        if (lockedSeconds > 0) {
            LOGGER.warning("Đăng nhập bị chặn do bị khóa tạm: user=" + logSafe(username)
                    + " ip=" + request.getRemoteAddr());
            request.setAttribute("errorMessage", "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau "
                    + ((lockedSeconds + 59) / 60) + " phút!");
            request.setAttribute("username", username);
            // ▶ forward → login.jsp (các setAttribute ở trên chính là dữ liệu JSP hiển thị)
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }

        // 3. Gọi Data Layer để xác thực (kiểm tra username + mật khẩu đã hash)
        User user = UserDB.selectByCredentials(username.trim(), password.trim());

        if (user != null) {
            LOGIN_LIMITER.reset(limiterKey);
            LOGGER.info("Đăng nhập thành công: user=" + logSafe(user.getUsername())
                    + " ip=" + request.getRemoteAddr());

            // Đăng nhập THÀNH CÔNG → Tạo Session, lưu đối tượng User
            HttpSession session = request.getSession();
            request.changeSessionId(); // Đổi mã session sau khi đăng nhập (chống session fixation)
            // ▶ JSP: chat.jsp đọc bằng ${currentUser}
            session.setAttribute("currentUser", user);

            // 3. Xử lý Cookie "Ghi nhớ đăng nhập" (Remember Me — hạn 14 ngày)
            // Cookie chứa token có chữ ký HMAC (không còn là username thô nên không thể tự đặt cookie để mạo danh)
            String cookiePath = request.getContextPath().isEmpty() ? "/" : request.getContextPath();
            // Chưa cấu hình TEAMWORK_REMEMBER_SECRET → issue() trả null → không phát cookie (xóa cookie cũ nếu có)
            String rememberToken = isRemember ? RememberMeToken.issue(user) : null;
            if (isRemember && rememberToken == null) {
                LOGGER.warning("Ghi nhớ đăng nhập đang TẮT: chưa đặt biến môi trường TEAMWORK_REMEMBER_SECRET (>= 32 ký tự)");
            }
            jakarta.servlet.http.Cookie rememberCookie =
                    new jakarta.servlet.http.Cookie(RememberMeToken.COOKIE_NAME, rememberToken != null ? rememberToken : "");
            rememberCookie.setPath(cookiePath);
            rememberCookie.setHttpOnly(true);  // Bảo mật: chống XSS đọc Cookie
            rememberCookie.setMaxAge(rememberToken != null ? RememberMeToken.MAX_AGE_SECONDS : 0); // 14 ngày hoặc xóa ngay
            response.addCookie(rememberCookie);

            // Điều hướng sang Dashboard danh sách dự án
            response.sendRedirect(request.getContextPath() + "/project?action=list");

        } else {
            // Đăng nhập THẤT BẠI → Ghi nhận lần sai, thông báo lỗi, giữ lại username
            boolean nowLocked = LOGIN_LIMITER.recordFailure(limiterKey, now);
            LOGGER.warning("Đăng nhập thất bại: user=" + logSafe(username)
                    + " ip=" + request.getRemoteAddr() + (nowLocked ? " (đã bị khóa tạm)" : ""));
            request.setAttribute("errorMessage", nowLocked
                    ? "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau "
                            + (LoginAttemptLimiter.LOCK_MS / 60000) + " phút!"
                    : "Tên đăng nhập hoặc mật khẩu không chính xác!");
            request.setAttribute("username", username);
            // ▶ forward → login.jsp (các setAttribute ở trên chính là dữ liệu JSP hiển thị)
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
            // ▶ JSP: login.jsp đọc bằng ${regError}
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
            "",                                         // Chức danh (không phải quyền): để trống → UserDB gán "Lập trình viên"
            "images/default_avatar.png"
        );
        UserDB.insert(newUser);

        // 4. Đăng ký thành công → Flash Message → Redirect về Login, mở sẵn tab Đăng nhập
        HttpSession session = request.getSession();
        // ▶ JSP: login.jsp đọc bằng ${successMessage}
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
    // NGHIỆP VỤ 4: QUÊN MẬT KHẨU (Forgot Password Flow — OTP qua email)
    // =========================================================================

    /**
     * Bước 1: nhận username, sinh OTP 6 số (hiệu lực 3 phút) và gửi tới email của tài khoản.
     * Dùng chung cho "Gửi lại mã" (giãn cách tối thiểu 60 giây).
     * Luôn trả cùng một thông báo dù username có tồn tại hay không để không lộ danh sách tài khoản.
     */
    private void processForgotRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        String username = request.getParameter("username");
        if (username == null || username.trim().isEmpty()) {
            // ▶ JSP: forgot-password.jsp đọc bằng ${forgotError}
            request.setAttribute("forgotError", "Vui lòng nhập tên đăng nhập!");
            forwardForgot(request, response, 1);
            return;
        }
        username = username.trim();

        HttpSession session = request.getSession();
        long now = System.currentTimeMillis();

        // Chống spam gửi lại: cùng username phải chờ đủ RESEND_MS
        OtpChallenge existing = (OtpChallenge) session.getAttribute(SESSION_OTP);
        String existingUser = (String) session.getAttribute(SESSION_OTP_USER);
        if (existing != null && username.equalsIgnoreCase(existingUser) && !existing.canResend(now)) {
            // ▶ JSP: forgot-password.jsp đọc bằng ${forgotUsername}
            request.setAttribute("forgotUsername", username);
            request.setAttribute("forgotError",
                    "Vui lòng đợi " + existing.resendWaitSeconds(now) + " giây trước khi gửi lại mã.");
            forwardForgot(request, response, 2);
            return;
        }

        // Chống spam email trên nhiều session: giới hạn theo tài khoản và theo IP (đếm cả khi tài khoản
        // không tồn tại để bộ đếm không làm lộ tài khoản nào có thật)
        String otpUserKey = "otp-user|" + username.toLowerCase();
        String otpIpKey = "otp-ip|" + request.getRemoteAddr();
        long otpLocked = Math.max(OTP_USER_LIMITER.lockedSeconds(otpUserKey, now),
                OTP_IP_LIMITER.lockedSeconds(otpIpKey, now));
        if (otpLocked > 0) {
            LOGGER.warning("Chặn gửi OTP do vượt giới hạn: user=" + logSafe(username) + " ip=" + request.getRemoteAddr());
            request.setAttribute("forgotUsername", username);
            request.setAttribute("forgotError", "Bạn đã yêu cầu mã quá nhiều lần. Vui lòng thử lại sau "
                    + ((otpLocked + 59) / 60) + " phút!");
            forwardForgot(request, response, 1);
            return;
        }
        OTP_USER_LIMITER.recordFailure(otpUserKey, now);
        OTP_IP_LIMITER.recordFailure(otpIpKey, now);

        User user = UserDB.selectByUsername(username);
        boolean deliverable = user != null && user.getEmail() != null && !user.getEmail().trim().isEmpty();
        String otp = OtpChallenge.generateOtp();

        OtpChallenge challenge;
        if (deliverable) {
            if (!MailUtil.sendOtp(user.getEmail().trim(), user.getFullName(), otp)) {
                request.setAttribute("forgotUsername", username);
                request.setAttribute("forgotError", "Không gửi được email lúc này. Vui lòng thử lại sau!");
                forwardForgot(request, response, 1);
                return;
            }
            challenge = new OtpChallenge(user.getId(), otp, now);
        } else {
            challenge = new OtpChallenge(0, otp, now); // OTP giả: không gửi, không bao giờ hợp lệ
        }

        session.setAttribute(SESSION_OTP, challenge);
        session.setAttribute(SESSION_OTP_USER, username);

        request.setAttribute("forgotUsername", username);
        // ▶ JSP: forgot-password.jsp đọc bằng ${forgotInfo}
        request.setAttribute("forgotInfo",
                "Nếu tài khoản tồn tại, mã OTP gồm 6 số đã được gửi tới email đăng ký. Mã có hiệu lực 3 phút.");
        forwardForgot(request, response, 2);
    }

    /**
     * Bước 2: kiểm tra OTP. Đúng → cho phép đặt mật khẩu mới.
     * Sai quá 3 lần hoặc hết hạn → hủy OTP, quay lại bước 1.
     */
    private void processForgotVerify(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        HttpSession session = request.getSession();
        OtpChallenge challenge = (OtpChallenge) session.getAttribute(SESSION_OTP);
        if (challenge == null) {
            request.setAttribute("forgotError", "Phiên đặt lại mật khẩu đã hết hạn. Vui lòng bắt đầu lại!");
            forwardForgot(request, response, 1);
            return;
        }

        String username = (String) session.getAttribute(SESSION_OTP_USER);
        request.setAttribute("forgotUsername", username);

        OtpChallenge.Result result = challenge.verify(request.getParameter("otp"), System.currentTimeMillis());
        switch (result) {
            case OK:
                if (challenge.getUserId() <= 0) { // OTP giả (tài khoản không tồn tại)
                    clearResetState(session);
                    request.setAttribute("forgotError", "Mã OTP không đúng. Vui lòng bắt đầu lại!");
                    forwardForgot(request, response, 1);
                    return;
                }
                forwardForgot(request, response, 3);
                return;
            case WRONG:
                request.setAttribute("forgotError",
                        "Mã OTP không đúng. Bạn còn " + challenge.remainingAttempts() + " lần thử.");
                forwardForgot(request, response, 2);
                return;
            case EXPIRED:
                clearResetState(session);
                request.setAttribute("forgotError", "Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới!");
                forwardForgot(request, response, 1);
                return;
            case LOCKED:
            default:
                clearResetState(session);
                request.setAttribute("forgotError", "Bạn đã nhập sai quá 3 lần. Vui lòng yêu cầu mã mới!");
                forwardForgot(request, response, 1);
                return;
        }
    }

    /**
     * Bước 3: đặt mật khẩu mới. Chỉ được phép khi OTP đã xác nhận đúng trong cùng Session.
     */
    private void processForgotReset(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        HttpSession session = request.getSession();
        OtpChallenge challenge = (OtpChallenge) session.getAttribute(SESSION_OTP);
        if (challenge == null || !challenge.isVerified() || challenge.getUserId() <= 0) {
            clearResetState(session);
            request.setAttribute("forgotError", "Bạn chưa xác nhận OTP. Vui lòng bắt đầu lại!");
            forwardForgot(request, response, 1);
            return;
        }

        String password = request.getParameter("password");
        String confirm  = request.getParameter("confirmPassword");

        if (password == null || password.trim().length() < 6) {
            request.setAttribute("forgotError", "Mật khẩu phải có ít nhất 6 ký tự!");
            forwardForgot(request, response, 3);
            return;
        }
        if (!password.equals(confirm)) {
            request.setAttribute("forgotError", "Mật khẩu xác nhận không khớp!");
            forwardForgot(request, response, 3);
            return;
        }

        if (!UserDB.updatePassword(challenge.getUserId(), password.trim())) {
            request.setAttribute("forgotError", "Không thể cập nhật mật khẩu lúc này. Vui lòng thử lại!");
            forwardForgot(request, response, 3);
            return;
        }

        String username = (String) session.getAttribute(SESSION_OTP_USER);
        clearResetState(session);
        session.setAttribute("successMessage", "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.");
        session.setAttribute("registeredUsername", username);
        response.sendRedirect(request.getContextPath() + "/auth?action=viewLogin");
    }

    // =========================================================================
    // HELPER — Phương thức tiện ích dùng nội bộ
    // =========================================================================

    /** Hiển thị forgot-password.jsp ở bước 1 (username), 2 (OTP) hoặc 3 (mật khẩu mới). */
    private void forwardForgot(HttpServletRequest request, HttpServletResponse response, int step)
            throws ServletException, IOException
    {
        // ▶ JSP: forgot-password.jsp đọc bằng ${forgotStep}
        request.setAttribute("forgotStep", step);
        // ▶ forward → forgot-password.jsp (các setAttribute ở trên chính là dữ liệu JSP hiển thị)
        request.getRequestDispatcher("/forgot-password.jsp").forward(request, response);
    }

    /** Loại bỏ ký tự xuống dòng khỏi dữ liệu người dùng trước khi ghi log (chống giả mạo dòng log). */
    private static String logSafe(String s) {
        return s == null ? "" : s.replaceAll("[\\r\\n\\t]", "_");
    }

    /** Xóa toàn bộ trạng thái OTP trong Session. */
    private void clearResetState(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_OTP);
            session.removeAttribute(SESSION_OTP_USER);
        }
    }

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
        // ▶ JSP: login.jsp đọc bằng ${regUsername}
        request.setAttribute("regUsername", username);
        // ▶ JSP: login.jsp đọc bằng ${regFullName}
        request.setAttribute("regFullName", fullName);
        // ▶ JSP: login.jsp đọc bằng ${regEmail}
        request.setAttribute("regEmail", email);
        // ▶ JSP: login.jsp đọc bằng ${activeTab}
        request.setAttribute("activeTab", "register"); // Mở sẵn tab Đăng ký trong login.jsp
        // ▶ forward → login.jsp (các setAttribute ở trên chính là dữ liệu JSP hiển thị)
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }
}
