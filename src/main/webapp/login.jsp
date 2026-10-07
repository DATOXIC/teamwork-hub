<%-- =========================================================================
     [MVC VIEW SKELETON: AUTHENTICATION & REGISTRATION DISPATCHER]
     VI: Luồng xác thực đăng nhập và đăng ký tài khoản người dùng
     EN: User authentication and registration dispatching view
     - Controller: com.teamwork.controllers.AuthServlet (/auth)
     - Model: com.teamwork.business.User, com.teamwork.data.UserDB
     - Session Attributes: currentUser (User entity set upon successful login)
     - Request Attributes: 
         * successMessage (String): Thông báo đăng ký thành công / Registration success banner
         * errorMessage (String): Thông báo lỗi đăng nhập / Login error banner
         * regError (String): Thông báo lỗi đăng ký / Registration error banner
         * activeTab (String): Tab đang kích hoạt ('login' hoặc 'register')
         * username (String): Lưu lại giá trị tên đăng nhập / Preserved username input
         * regFullName, regUsername, regEmail (String): Dữ liệu form đăng ký khi lỗi
         * rememberChecked (Boolean): Trạng thái ghi nhớ đăng nhập / Remember checkbox state
     - Cookie: teamwork_remember_user (token ký HMAC chứa username + hạn dùng 14 ngày, xem util/RememberMeToken)
     ========================================================================= --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <%-- CSRF token của session (CsrfFilter tạo); csrf.js tự gắn vào mọi form POST --%>
    <meta name="csrf-token" content="${sessionScope.csrfToken}">
    <script src="${pageContext.request.contextPath}/js/csrf.js"></script>

    <!-- Khởi tạo Theme tức thời để loại bỏ triệt để hiện tượng nhấp nháy giao diện (FOUC) -->
    <script>
        (function() {
            var t = null;
            try {
                t = localStorage.getItem('teamwork_theme') || localStorage.getItem('teamwork_report_theme');
            } catch (e) {}
            if (!t) {
                t = (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light';
            }
            document.documentElement.setAttribute('data-theme', t);
            document.documentElement.setAttribute('data-bs-theme', t);
        })();
    </script>
    <title>Đăng nhập &bull; TeamWork Hub</title>

    <%-- Favicon --%>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/images/favicon.png">

    <%-- Google Font: Inter (Chuẩn ClickUp & Linear) --%>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=Plus+Jakarta+Sans:wght@500;600;700&display=swap"
          rel="stylesheet">

    <%-- Bootstrap 5 CSS --%>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">

    <%-- Bootstrap Icons --%>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">

    <%-- Design token (màu, font, bo góc...) — PHẢI nạp trước mọi CSS khác --%>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/tokens.css?v=<%= System.currentTimeMillis() %>">

    <%-- CSS riêng của trang Login — đặt trong <head>, tải trước body --%>
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/styles/login.css?v=<%= System.currentTimeMillis() %>">

    <!-- Page Components Semantic CSS -->
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/styles/page-components.css?v=<%= System.currentTimeMillis() %>">
</head>

<body class="login-body">

    <%-- ============================================================
         SECTION 1: NAVBAR
         Navbar nhỏ gọn: chỉ Brand + tên trường, không có menu đăng nhập
         ============================================================ --%>
    <nav class="navbar navbar-dark py-2 shadow-sm login-navbar">
        <div class="container-fluid px-4">
            <a class="navbar-brand d-flex align-items-center fw-bold"
               href="${pageContext.request.contextPath}/">
                <span class="me-2 login-brand-logo-icon">
                    <i class="bi bi-grid-1x2-fill text-white"></i>
                </span>
                <span class="text-white fw-bold">TeamWork</span>
                <span class="text-info fw-bold ms-1">Hub</span>
            </a>
            <span class="small login-campus-caption">
                <i class="bi bi-mortarboard-fill text-warning me-1"></i> HCM-UTE Campus
            </span>
            <div class="d-flex align-items-center gap-2 ms-auto">
                <div class="dropdown">
                    <button class="btn btn-sm btn-outline-light rounded-pill px-2 py-1" type="button"
                            data-bs-toggle="dropdown" aria-expanded="false" aria-label="Chọn ngôn ngữ">
                        <i class="bi bi-translate"></i> <span id="languageCurrent">VI</span>
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end">
                        <li><button type="button" class="dropdown-item language-option" data-language="vi">Tiếng Việt</button></li>
                        <li><button type="button" class="dropdown-item language-option" data-language="en">English</button></li>
                    </ul>
                </div>
                <!-- Global Theme Switcher -->
                <button type="button" id="globalThemeToggleBtn" onclick="toggleGlobalTheme()"
                        class="btn btn-sm btn-outline-light rounded-pill px-2.5 py-1 d-inline-flex align-items-center gap-1 shadow-none"
                        title="Chuyển đổi giao diện Sáng / Tối" aria-label="Chuyển đổi giao diện Sáng / Tối">
                    <i class="bi bi-moon-stars-fill" id="globalThemeIconMoon"></i>
                    <i class="bi bi-sun-fill text-warning d-none" id="globalThemeIconSun"></i>
                    <span id="globalThemeBtnText" class="fs-9">Tối</span>
                </button>
            </div>
        </div>
    </nav>

    <%-- ============================================================
         SECTION 2: LAYOUT 2 CỘT — Tái tạo từ LoginWindow.xaml
         Cột Trái: Branding Panel (Dark Navy & Indigo)
         Cột Phải: Form Panel (Đăng nhập / Đăng ký)
         ============================================================ --%>
    <div class="login-wrapper">

        <%-- ═══════════ CỘT TRÁI: Branding Panel ═══════════ --%>
        <div class="login-branding">
            <div class="branding-content">

                <%-- Header: Logo HCMUTE + Tên trường --%>
                <div class="branding-header">
                    <img src="${pageContext.request.contextPath}/images/ute_logo.png" alt="Logo HCMUTE" />
                    <div class="branding-header-text">
                        <h4>TRƯỜNG ĐẠI HỌC CÔNG NGHỆ KỸ THUẬT TP.HCM</h4>
                        <p>Khoa Đào tạo Tiên tiến &bull; Lập trình Web</p>
                    </div>
                </div>

                <%-- Body: Showcase Cổng trường & Tên ứng dụng --%>
                <div class="branding-body">
                    <div class="branding-hero-showcase">
                        <div class="branding-hero-img">
                            <img src="${pageContext.request.contextPath}/images/HCMUTE_GATE.png"
                                 alt="Cổng trường HCMUTE" />
                        </div>
                        <div class="branding-hero-badge">
                            <i class="bi bi-mortarboard-fill text-warning me-1"></i> HCM-UTE Campus
                        </div>
                    </div>
                    <h2 class="branding-title">TeamWork Hub</h2>
                    <p class="branding-desc">Không gian cộng tác, quản lý tiến độ và chia sẻ
                        tài liệu tập trung cho nhóm sinh viên</p>
                </div>

                <%-- Footer: Slogan HCMUTE --%>
                <div class="branding-footer">
                    <div class="branding-slogan-pill">
                        <i class="bi bi-stars text-warning me-1"></i> Chất lượng &bull; Sáng tạo &bull; Hội nhập
                    </div>
                </div>

            </div>
        </div>

        <%-- ═══════════ CỘT PHẢI: Form Panel ═══════════ --%>
        <div class="login-form-panel">
            <div class="login-card">

                <%-- Card Header --%>
                <div class="login-card-header">
                    <h3>Đăng Nhập</h3>
                    <p>Nhập tài khoản để truy cập hệ thống</p>
                </div>

                <%-- Thông báo thành công (sau khi Đăng ký) --%>
                <%-- ◀ SERVLET: AuthServlet → setAttribute("successMessage") --%>
                <c:if test="${not empty successMessage}">
                    <div class="login-success-banner">
                        <p><i class="bi bi-check-circle-fill"></i> ${fn:escapeXml(successMessage)}</p>
                    </div>
                </c:if>

                <%-- Thông báo lỗi đăng nhập --%>
                <%-- ◀ SERVLET: AuthServlet → setAttribute("errorMessage") --%>
                <c:if test="${not empty errorMessage}">
                    <div class="login-error-banner">
                        <p><i class="bi bi-exclamation-triangle-fill"></i> ${fn:escapeXml(errorMessage)}</p>
                    </div>
                </c:if>

                <%-- Thông báo lỗi đăng ký --%>
                <%-- ◀ SERVLET: AuthServlet → setAttribute("regError") --%>
                <c:if test="${not empty regError}">
                    <div class="login-error-banner">
                        <p><i class="bi bi-exclamation-triangle-fill"></i> ${fn:escapeXml(regError)}</p>
                    </div>
                </c:if>

                <%-- Tab Pills: Đăng nhập | Đăng ký --%>
                <div class="login-tabs">
                    <button type="button"
                            class="login-tab-btn ${activeTab != 'register' ? 'active' : ''}"
                            onclick="switchTab('login')" id="tab-btn-login">
                        <i class="bi bi-box-arrow-in-right"></i> Đăng nhập
                    </button>
                    <button type="button"
                            class="login-tab-btn ${activeTab == 'register' ? 'active' : ''}"
                            onclick="switchTab('register')" id="tab-btn-register">
                        <i class="bi bi-person-plus"></i> Đăng ký
                    </button>
                </div>

                <%-- ═══════ TAB 1: FORM ĐĂNG NHẬP ═══════
                     [MVC BINDING: FORM 1 - USER AUTHENTICATION / ĐĂNG NHẬP]
                     VI: Gửi thông tin định danh (username, password, remember) về AuthServlet
                     EN: Submits user credentials to AuthServlet via POST
                     - Action: ${pageContext.request.contextPath}/auth
                     - Method: POST
                     - Handled by: AuthServlet.doPost() -> handleLogin()
                     ====================================================== --%>
                <%-- ◀ SERVLET: AuthServlet → setAttribute("activeTab") --%>
                <div class="login-tab-pane ${activeTab != 'register' ? 'active' : ''}" id="pane-login">
                    <%-- ▶ SERVLET: /auth → AuthServlet.doPost() → case "login" → processLogin() --%>
                    <form action="${pageContext.request.contextPath}/auth" method="post" autocomplete="off">
                        <input type="hidden" name="action" value="login">

                        <%-- Tên đăng nhập (Login Username) --%>
                        <label class="login-label" for="login-username">Tên đăng nhập</label>
                        <div class="login-input-group">
                            <i class="bi bi-person input-icon"></i>
                            <input type="text" id="login-username" name="username"
                                   value="<c:out value='${username}'/>"
                                   placeholder="Nhập tên đăng nhập..."
                                   required autofocus>
                        </div>

                        <%-- Mật khẩu (Password) --%>
                        <label class="login-label" for="login-password">Mật khẩu</label>
                        <div class="login-input-group">
                            <i class="bi bi-lock input-icon"></i>
                            <input type="password" id="login-password" name="password"
                                   placeholder="Nhập mật khẩu" required>
                            <button type="button" class="toggle-password-btn"
                                    onclick="togglePassword('login-password', this)"
                                    tabindex="-1" aria-label="Hiện/ẩn mật khẩu">
                                <i class="bi bi-eye"></i>
                            </button>
                        </div>

                        <%-- Ghi nhớ & Quên MK (Remember Me & Forgot Password) --%>
                        <div class="login-options-row">
                            <label class="login-remember">
                                <input type="checkbox" name="remember"
                                       ${rememberChecked ? 'checked' : ''}>
                                <span>Ghi nhớ đăng nhập</span>
                            </label>
                            <a href="${pageContext.request.contextPath}/auth?action=forgot" class="login-forgot-link">Quên mật khẩu?</a>
                        </div>

                        <%-- Nút Đăng nhập --%>
                        <button type="submit" class="btn-login-primary">Đăng Nhập &nbsp;→</button>
                    </form>

                    <%-- Nút Thoát (Exit Button) --%>
                    <a href="${pageContext.request.contextPath}/" class="login-exit-link">
                        <button type="button" class="btn-login-exit">Thoát</button>
                    </a>

                    <%-- Link Đăng ký (Switch to Register) --%>
                    <div class="login-signup-link mb-3">
                        <span>Chưa có tài khoản?</span>
                        <a href="javascript:void(0)" onclick="switchTab('register')">Đăng ký ngay</a>
                    </div>

                    <%-- Đăng nhập nhanh cho buổi demo: bấm một tài khoản → tự điền vào form (login.js).
                         Tài khoản lấy từ dữ liệu seed (database/schema_postgres_supabase.sql).
                         Ẩn bằng TEAMWORK_DEMO_ACCOUNTS=off (AuthServlet → ${showDemoAccounts});
                         mở thẳng /login.jsp không qua servlet thì vẫn hiện (giá trị rỗng ≠ false). --%>
                    <c:if test="${showDemoAccounts != false}">
                        <div class="quick-login-section" role="group" aria-labelledby="quickLoginTitle">
                            <div class="quick-login-title" id="quickLoginTitle">
                                <i class="bi bi-lightning-charge-fill text-warning me-1" aria-hidden="true"></i>
                                Tài khoản mẫu thử nghiệm
                            </div>
                            <div class="quick-login-pills">
                                <button type="button" class="quick-login-btn" data-demo-user="admin" data-demo-pass="admin123">
                                    <i class="bi bi-shield-lock-fill text-primary" aria-hidden="true"></i>
                                    <span><strong>admin</strong> · Trưởng dự án</span>
                                </button>
                                <button type="button" class="quick-login-btn" data-demo-user="alice" data-demo-pass="alice123">
                                    <i class="bi bi-brush-fill text-info" aria-hidden="true"></i>
                                    <span><strong>alice</strong> · Frontend</span>
                                </button>
                                <button type="button" class="quick-login-btn" data-demo-user="bob" data-demo-pass="bob123">
                                    <i class="bi bi-code-slash text-success" aria-hidden="true"></i>
                                    <span><strong>bob</strong> · Backend</span>
                                </button>
                                <button type="button" class="quick-login-btn" data-demo-user="david" data-demo-pass="david123">
                                    <i class="bi bi-bug-fill text-danger" aria-hidden="true"></i>
                                    <span><strong>david</strong> · Kiểm thử (QA)</span>
                                </button>
                            </div>
                            <p class="quick-login-hint">Bấm để tự điền, rồi chọn <strong>Đăng Nhập</strong>.</p>
                        </div>
                    </c:if>
                </div>

                <%-- ═══════ TAB 2: FORM ĐĂNG KÝ ═══════
                     [MVC BINDING: FORM 2 - USER REGISTRATION / ĐĂNG KÝ TÀI KHOẢN MỚI]
                     VI: Thu thập thông tin tài khoản mới, kiểm tra hợp lệ client trước khi POST
                     EN: Collects registration fields, validates client-side, dispatches via POST
                     - Action: ${pageContext.request.contextPath}/auth
                     - Method: POST
                     - Handled by: AuthServlet.doPost() -> handleRegister()
                     ====================================================== --%>
                <div class="login-tab-pane ${activeTab == 'register' ? 'active' : ''}" id="pane-register">
                    <%-- ▶ SERVLET: /auth → AuthServlet.doPost() → case "register" → processRegister() --%>
                    <form action="${pageContext.request.contextPath}/auth"
                          onsubmit="return validateRegisterForm()"
                          method="post" autocomplete="off">
                        <input type="hidden" name="action" value="register">

                        <%-- Họ và tên (Full Name) --%>
                        <label class="login-label" for="reg-fullname">Họ và tên</label>
                        <div class="login-input-group">
                            <i class="bi bi-person-badge input-icon"></i>
                            <input type="text" id="reg-fullname" name="fullName"
                                   value="${fn:escapeXml(regFullName)}" placeholder="Ví dụ: Nguyễn Văn A" required>
                        </div>

                        <%-- Tên đăng nhập (Username) --%>
                        <label class="login-label" for="reg-username">Tên đăng nhập</label>
                        <div class="login-input-group">
                            <i class="bi bi-person input-icon"></i>
                            <input type="text" id="reg-username" name="username"
                                   value="${fn:escapeXml(regUsername)}" placeholder="Tối thiểu 4 ký tự" required>
                        </div>

                        <%-- Email liên hệ (Contact Email) --%>
                        <label class="login-label" for="reg-email">Email liên hệ</label>
                        <div class="login-input-group">
                            <i class="bi bi-envelope input-icon"></i>
                            <input type="email" id="reg-email" name="email"
                                   value="${fn:escapeXml(regEmail)}" placeholder="ten@vidu.com" required>
                        </div>

                        <%-- Mật khẩu + Xác nhận (2 cột) — Password & Confirm --%>
                        <div class="register-password-row">
                            <div>
                                <label class="login-label" for="reg-pass">Mật khẩu</label>
                                <div class="login-input-group">
                                    <i class="bi bi-lock input-icon"></i>
                                    <input type="password" id="reg-pass" name="password"
                                           placeholder="≥ 6 ký tự" required>
                                    <button type="button" class="toggle-password-btn"
                                            onclick="togglePassword('reg-pass', this)"
                                            tabindex="-1" aria-label="Hiện/ẩn mật khẩu">
                                        <i class="bi bi-eye"></i>
                                    </button>
                                </div>
                                <div class="field-error" id="reg-pass-error"></div>
                            </div>
                            <div>
                                <label class="login-label" for="reg-confirmpass">Xác nhận</label>
                                <div class="login-input-group">
                                    <i class="bi bi-lock-fill input-icon"></i>
                                    <input type="password" id="reg-confirmpass" name="confirmPassword"
                                           placeholder="Nhập lại mật khẩu" required>
                                    <button type="button" class="toggle-password-btn"
                                            onclick="togglePassword('reg-confirmpass', this)"
                                            tabindex="-1" aria-label="Hiện/ẩn mật khẩu">
                                        <i class="bi bi-eye"></i>
                                    </button>
                                </div>
                                <div class="field-error" id="reg-confirmpass-error"></div>
                            </div>
                        </div>

                        <%-- Nút Đăng ký (Submit Register) --%>
                        <button type="submit" class="btn-register-primary">
                            <i class="bi bi-person-check"></i> Tạo tài khoản mới
                        </button>
                    </form>

                    <%-- Link quay lại Đăng nhập (Back to Login) --%>
                    <div class="login-signup-link">
                        <span>Đã có tài khoản?</span>
                        <a href="javascript:void(0)" onclick="switchTab('login')">Đăng nhập ngay</a>
                    </div>
                </div>

            </div><%-- end .login-card --%>
        </div><%-- end .login-form-panel --%>

    </div><%-- end .login-wrapper --%>

    <%-- ============================================================
         SECTION 3: SCRIPTS
         - switchTab()         : Chuyển qua lại giữa form Đăng nhập và Đăng ký
         - togglePassword()    : Hiện/ẩn mật khẩu khi bấm icon con mắt
         - validateRegisterForm(): Client-side validation trước khi POST
         ============================================================ --%>
    <script src="${pageContext.request.contextPath}/js/login.js"></script>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/app.js"></script>

</body>
</html>
