<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <!DOCTYPE html>
        <html lang="vi">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Đăng nhập &bull; TeamWork Hub</title>

            <%-- Google Fonts --%>
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link
                    href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Inter:wght@400;500;600;700&display=swap"
                    rel="stylesheet">

                <%-- Bootstrap 5 CSS --%>
                    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
                        rel="stylesheet">

                    <%-- Bootstrap Icons --%>
                        <link rel="stylesheet"
                            href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">

                        <%-- CSS riêng của trang Login — đặt trong <head>, tải trước body --%>
                            <link rel="stylesheet"
                                href="${pageContext.request.contextPath}/styles/login.css?v=<%= System.currentTimeMillis() %>">
        </head>

        <body style="margin: 0; padding: 0; font-family: 'Plus Jakarta Sans', 'Inter', sans-serif;">

            <%-- Navbar nhỏ gọn: chỉ Brand + tên trường, không có menu đăng nhập --%>
                <nav class="navbar navbar-dark py-2 shadow-sm"
                    style="background-color: #0f172a; border-bottom: 1px solid #1e293b;">
                    <div class="container-fluid px-4">
                        <a class="navbar-brand d-flex align-items-center fw-bold"
                            href="${pageContext.request.contextPath}/">
                            <span class="me-2 d-flex align-items-center justify-content-center"
                                style="width: 30px; height: 30px; background: linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%); border-radius: 7px;">
                                <i class="bi bi-grid-1x2-fill text-white" style="font-size: 14px;"></i>
                            </span>
                            <span class="text-white fw-bold">TeamWork</span><span
                                class="text-info fw-bold ms-1">Hub</span>
                        </a>
                        <span class="small" style="color: #94a3b8; font-size: 13px;">
                            <i class="bi bi-mortarboard-fill text-warning me-1"></i> HCM-UTE Campus
                        </span>
                    </div>
                </nav>

                <%--============================================================LAYOUT 2 CỘT — Tái tạo từ
                    LoginWindow.xaml============================================================--%>
                    <div class="login-wrapper">

                        <%-- ═══════════ CỘT TRÁI: Branding Panel (Dark Navy & Indigo) ═══════════ --%>
                            <div class="login-branding">
                                <div class="branding-content">

                                    <%-- Header: Logo HCMUTE + Tên trường --%>
                                        <div class="branding-header">
                                            <img src="${pageContext.request.contextPath}/images/ute_logo.png"
                                                alt="Logo HCMUTE" />
                                            <div class="branding-header-text">
                                                <h4>TRƯỜNG ĐẠI HỌC CÔNG NGHỆ KỸ THUẬT TP.HCM</h4>
                                                <p>Khoa Đào tạo Tiên tiến &bull; Web Programming</p>
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
                                                        <i class="bi bi-mortarboard-fill text-warning me-1"></i> HCM-UTE
                                                        Campus
                                                    </div>
                                                </div>

                                                <h2 class="branding-title">TeamWork Hub</h2>
                                                <p class="branding-desc">Không gian cộng tác, quản lý tiến độ và chia sẻ
                                                    tài liệu tập trung cho nhóm sinh viên</p>
                                            </div>

                                            <%-- Footer: Slogan HCMUTE --%>
                                                <div class="branding-footer">
                                                    <div class="branding-slogan-pill">
                                                        <i class="bi bi-stars text-warning me-1"></i> Chất lượng &bull;
                                                        Sáng tạo &bull; Hội nhập
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
                                                <c:if test="${not empty successMessage}">
                                                    <div class="login-success-banner">
                                                        <p><i class="bi bi-check-circle-fill"></i> ${successMessage}</p>
                                                    </div>
                                                </c:if>

                                                <%-- Thông báo lỗi đăng nhập --%>
                                                    <c:if test="${not empty errorMessage}">
                                                        <div class="login-error-banner">
                                                            <p><i class="bi bi-exclamation-triangle-fill"></i>
                                                                ${errorMessage}</p>
                                                        </div>
                                                    </c:if>

                                                    <%-- Thông báo lỗi đăng ký --%>
                                                        <c:if test="${not empty regError}">
                                                            <div class="login-error-banner">
                                                                <p><i class="bi bi-exclamation-triangle-fill"></i>
                                                                    ${regError}</p>
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
                                                                    onclick="switchTab('register')"
                                                                    id="tab-btn-register">
                                                                    <i class="bi bi-person-plus"></i> Đăng ký
                                                                </button>
                                                            </div>

                                                            <%-- ═══════ TAB 1: FORM ĐĂNG NHẬP ═══════ --%>
                                                                <div class="login-tab-pane ${activeTab != 'register' ? 'active' : ''}"
                                                                    id="pane-login">
                                                                    <form
                                                                        action="${pageContext.request.contextPath}/auth"
                                                                        method="post" autocomplete="off">
                                                                        <input type="hidden" name="action"
                                                                            value="login">

                                                                        <%-- Tên đăng nhập --%>
                                                                            <label class="login-label"
                                                                                for="login-username">Tên đăng
                                                                                nhập</label>
                                                                            <div class="login-input-group">
                                                                                <i class="bi bi-person input-icon"></i>
                                                                                <input type="text" id="login-username"
                                                                                    name="username" value="${username}"
                                                                                    placeholder="Nhập tên đăng nhập..."
                                                                                    required autofocus>
                                                                            </div>

                                                                            <%-- Mật khẩu --%>
                                                                                <label class="login-label"
                                                                                    for="login-password">Mật
                                                                                    khẩu</label>
                                                                                <div class="login-input-group">
                                                                                    <i
                                                                                        class="bi bi-lock input-icon"></i>
                                                                                    <input type="password"
                                                                                        id="login-password"
                                                                                        name="password"
                                                                                        placeholder="Nhập mật khẩu"
                                                                                        required>
                                                                                    <button type="button"
                                                                                        class="toggle-password-btn"
                                                                                        onclick="togglePassword('login-password', this)"
                                                                                        tabindex="-1"
                                                                                        aria-label="Hiện/ẩn mật khẩu">
                                                                                        <i class="bi bi-eye"></i>
                                                                                    </button>
                                                                                </div>

                                                                                <%-- Ghi nhớ & Quên MK --%>
                                                                                    <div class="login-options-row">
                                                                                        <label class="login-remember">
                                                                                            <input type="checkbox"
                                                                                                name="remember">
                                                                                            <span>Ghi nhớ đăng
                                                                                                nhập</span>
                                                                                        </label>
                                                                                        <a href="#"
                                                                                            class="login-forgot-link">Quên
                                                                                            mật khẩu?</a>
                                                                                    </div>

                                                                                    <%-- Nút Đăng nhập --%>
                                                                                        <button type="submit"
                                                                                            class="btn-login-primary">
                                                                                            Đăng Nhập &nbsp;→
                                                                                        </button>
                                                                    </form>

                                                                    <%-- Nút Thoát --%>
                                                                        <a href="${pageContext.request.contextPath}/"
                                                                            style="text-decoration:none;">
                                                                            <button type="button"
                                                                                class="btn-login-exit">Thoát</button>
                                                                        </a>

                                                                        <%-- Link Đăng ký --%>
                                                                            <div class="login-signup-link mb-3">
                                                                                <span>Chưa có tài khoản?</span>
                                                                                <a href="javascript:void(0)"
                                                                                    onclick="switchTab('register')">Đăng
                                                                                    ký ngay</a>
                                                                            </div>

                                                                            <%-- Khu vực Đăng nhập Nhanh cho Demo --%>
                                                                                <div class="quick-login-section">
                                                                                    <div class="quick-login-title">
                                                                                        <i
                                                                                            class="bi bi-lightning-charge-fill text-warning me-1"></i>
                                                                                        Tài khoản mẫu thử nghiệm:
                                                                                    </div>
                                                                                    <div class="quick-login-pills">
                                                                                        <button type="button"
                                                                                            class="quick-login-btn"
                                                                                            onclick="fillLogin('admin', 'admin123')">
                                                                                            <i
                                                                                                class="bi bi-shield-lock-fill text-primary"></i>
                                                                                            <strong>admin</strong> (PM)
                                                                                        </button>
                                                                                        <button type="button"
                                                                                            class="quick-login-btn"
                                                                                            onclick="fillLogin('member1', 'pass123')">
                                                                                            <i
                                                                                                class="bi bi-code-slash text-success"></i>
                                                                                            <strong>member1</strong>
                                                                                            (Dev)
                                                                                        </button>
                                                                                        <button type="button"
                                                                                            class="quick-login-btn"
                                                                                            onclick="fillLogin('binh', 'pass123')">
                                                                                            <i
                                                                                                class="bi bi-palette-fill text-info"></i>
                                                                                            <strong>binh</strong>
                                                                                            (Design)
                                                                                        </button>
                                                                                        <button type="button"
                                                                                            class="quick-login-btn"
                                                                                            onclick="fillLogin('chi', 'pass123')">
                                                                                            <i
                                                                                                class="bi bi-bug-fill text-danger"></i>
                                                                                            <strong>chi</strong> (QA)
                                                                                        </button>
                                                                                    </div>
                                                                                </div>
                                                                </div>

                                                                <%-- ═══════ TAB 2: FORM ĐĂNG KÝ ═══════ --%>
                                                                    <div class="login-tab-pane ${activeTab == 'register' ? 'active' : ''}"
                                                                        id="pane-register">
                                                                        <form
                                                                            action="${pageContext.request.contextPath}/auth"
                                                                            onsubmit="return validateRegisterForm()"
                                                                            method="post" autocomplete="off">
                                                                            <input type="hidden" name="action"
                                                                                value="register">

                                                                            <%-- Họ và tên --%>
                                                                                <label class="login-label"
                                                                                    for="reg-fullname">Họ và tên</label>
                                                                                <div class="login-input-group">
                                                                                    <i
                                                                                        class="bi bi-person-badge input-icon"></i>
                                                                                    <input type="text" id="reg-fullname"
                                                                                        name="fullName"
                                                                                        value="${regFullName}"
                                                                                        placeholder="Ví dụ: Nguyễn Văn A"
                                                                                        required>
                                                                                </div>

                                                                                <%-- Tên đăng nhập --%>
                                                                                    <label class="login-label"
                                                                                        for="reg-username">Tên đăng
                                                                                        nhập</label>
                                                                                    <div class="login-input-group">
                                                                                        <i
                                                                                            class="bi bi-person input-icon"></i>
                                                                                        <input type="text"
                                                                                            id="reg-username"
                                                                                            name="username"
                                                                                            value="${regUsername}"
                                                                                            placeholder="Tối thiểu 4 ký tự"
                                                                                            required>
                                                                                    </div>

                                                                                    <%-- Email --%>
                                                                                        <label class="login-label"
                                                                                            for="reg-email">Email liên
                                                                                            hệ</label>
                                                                                        <div class="login-input-group">
                                                                                            <i
                                                                                                class="bi bi-envelope input-icon"></i>
                                                                                            <input type="email"
                                                                                                id="reg-email"
                                                                                                name="email"
                                                                                                value="${regEmail}"
                                                                                                placeholder="name@example.com"
                                                                                                required>
                                                                                        </div>

                                                                                        <%-- Mật khẩu + Xác nhận (2 cột)
                                                                                            --%>
                                                                                            <div
                                                                                                class="register-password-row">
                                                                                                <div>
                                                                                                    <label
                                                                                                        class="login-label"
                                                                                                        for="reg-pass">Mật
                                                                                                        khẩu</label>
                                                                                                    <div
                                                                                                        class="login-input-group">
                                                                                                        <i
                                                                                                            class="bi bi-lock input-icon"></i>
                                                                                                        <input
                                                                                                            type="password"
                                                                                                            id="reg-pass"
                                                                                                            name="password"
                                                                                                            placeholder="≥ 6 ký tự"
                                                                                                            required>
                                                                                                        <button
                                                                                                            type="button"
                                                                                                            class="toggle-password-btn"
                                                                                                            onclick="togglePassword('reg-pass', this)"
                                                                                                            tabindex="-1"
                                                                                                            aria-label="Hiện/ẩn mật khẩu">
                                                                                                            <i
                                                                                                                class="bi bi-eye"></i>
                                                                                                        </button>
                                                                                                    </div>
                                                                                                    <div class="field-error"
                                                                                                        id="reg-pass-error">
                                                                                                    </div>
                                                                                                </div>
                                                                                                <div>
                                                                                                    <label
                                                                                                        class="login-label"
                                                                                                        for="reg-confirmpass">Xác
                                                                                                        nhận</label>
                                                                                                    <div
                                                                                                        class="login-input-group">
                                                                                                        <i
                                                                                                            class="bi bi-lock-fill input-icon"></i>
                                                                                                        <input
                                                                                                            type="password"
                                                                                                            id="reg-confirmpass"
                                                                                                            name="confirmPassword"
                                                                                                            placeholder="Nhập lại mật khẩu"
                                                                                                            required>
                                                                                                        <button
                                                                                                            type="button"
                                                                                                            class="toggle-password-btn"
                                                                                                            onclick="togglePassword('reg-confirmpass', this)"
                                                                                                            tabindex="-1"
                                                                                                            aria-label="Hiện/ẩn mật khẩu">
                                                                                                            <i
                                                                                                                class="bi bi-eye"></i>
                                                                                                        </button>
                                                                                                    </div>
                                                                                                    <div class="field-error"
                                                                                                        id="reg-confirmpass-error">
                                                                                                    </div>
                                                                                                </div>
                                                                                            </div>

                                                                                            <%-- Nút Đăng ký --%>
                                                                                                <button type="submit"
                                                                                                    class="btn-register-primary">
                                                                                                    <i
                                                                                                        class="bi bi-person-check"></i>
                                                                                                    Tạo tài khoản mới
                                                                                                </button>
                                                                        </form>

                                                                        <%-- Link quay lại Đăng nhập --%>
                                                                            <div class="login-signup-link">
                                                                                <span>Đã có tài khoản?</span>
                                                                                <a href="javascript:void(0)"
                                                                                    onclick="switchTab('login')">Đăng
                                                                                    nhập ngay</a>
                                                                            </div>
                                                                    </div>

                                    </div>
                                </div>

                    </div>

                    <%-- Script chuyển tab --%>
                        <script>
                            function switchTab(tab) {
                                document.getElementById('tab-btn-login').classList.toggle('active', tab === 'login');
                                document.getElementById('tab-btn-register').classList.toggle('active', tab === 'register');
                                document.getElementById('pane-login').classList.toggle('active', tab === 'login');
                                document.getElementById('pane-register').classList.toggle('active', tab === 'register');
                                var header = document.querySelector('.login-card-header h3');
                                var subtitle = document.querySelector('.login-card-header p');
                                if (tab === 'register') {
                                    header.textContent = 'Đăng Ký';
                                    subtitle.textContent = 'Tạo tài khoản mới để bắt đầu';
                                } else {
                                    header.textContent = 'Đăng Nhập';
                                    subtitle.textContent = 'Nhập tài khoản để truy cập hệ thống';
                                }
                            }

                            function fillLogin(u, p) {
                                document.getElementById('login-username').value = u;
                                document.getElementById('login-password').value = p;
                                switchTab('login');
                            }

                            /* ── Mục 4: Toggle hiện/ẩn mật khẩu ── */
                            function togglePassword(inputId, btn) {
                                var input = document.getElementById(inputId);
                                var icon = btn.querySelector('i');
                                if (input.type === 'password') {
                                    input.type = 'text';
                                    icon.classList.remove('bi-eye');
                                    icon.classList.add('bi-eye-slash');
                                } else {
                                    input.type = 'password';
                                    icon.classList.remove('bi-eye-slash');
                                    icon.classList.add('bi-eye');
                                }
                            }

                            /* ── Mục 5: Client-side validation form đăng ký ── */
                            function validateRegisterForm() {
                                var pass = document.getElementById('reg-pass');
                                var confirm = document.getElementById('reg-confirmpass');
                                var passError = document.getElementById('reg-pass-error');
                                var confirmError = document.getElementById('reg-confirmpass-error');
                                var valid = true;

                                // Reset errors
                                passError.textContent = '';
                                confirmError.textContent = '';
                                pass.classList.remove('input-error');
                                confirm.classList.remove('input-error');

                                // Kiểm tra độ dài mật khẩu
                                if (pass.value.length < 6) {
                                    passError.textContent = 'Mật khẩu phải có ít nhất 6 ký tự';
                                    pass.classList.add('input-error');
                                    pass.focus();
                                    valid = false;
                                }

                                // Kiểm tra mật khẩu khớp
                                if (valid && pass.value !== confirm.value) {
                                    confirmError.textContent = 'Mật khẩu xác nhận không khớp';
                                    confirm.classList.add('input-error');
                                    confirm.focus();
                                    valid = false;
                                }

                                return valid;
                            }
                        </script>

                        <script
                            src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
        </body>

        </html>