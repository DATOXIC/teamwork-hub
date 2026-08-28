<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- Thiết lập tiêu đề trang cho header.jsp --%>
<c:set var="pageTitle" value="Đăng nhập &bull; TeamWork Hub" />
<jsp:include page="/includes/header.jsp" />

<%-- CSS riêng cho trang Login --%>
<link rel="stylesheet" href="${pageContext.request.contextPath}/styles/login.css">

<jsp:include page="/includes/navbar.jsp" />

<%-- ============================================================
     LAYOUT 2 CỘT — Tái tạo từ LoginWindow.xaml
     ============================================================ --%>
<div class="login-wrapper">

    <%-- ═══════════ CỘT TRÁI: Branding Panel ═══════════ --%>
    <div class="login-branding">
        <%-- Glassmorphism bubble thứ 3 --%>
        <div class="bubble-extra"></div>

        <div class="branding-content">
            <%-- Header: Logo + Tên trường --%>
            <div class="branding-header">
                <img src="${pageContext.request.contextPath}/images/ute_logo.png" alt="Logo HCMUTE" />
                <div class="branding-header-text">
                    <h4>HCMUTE</h4>
                    <p>ĐẠI HỌC SƯ PHẠM KỸ THUẬT TP.HCM</p>
                </div>
            </div>

            <%-- Body: Hero image + Tiêu đề --%>
            <div class="branding-body">
                <div class="branding-hero-img">
                    <img src="${pageContext.request.contextPath}/images/HCMUTE_GATE.png" alt="Cổng trường HCMUTE" />
                </div>
                <h2>TeamWork Hub</h2>
                <div class="branding-divider">
                    <p>Nền tảng làm việc nhóm tập trung và hiệu quả</p>
                </div>
            </div>

            <%-- Footer: Slogan --%>
            <div class="branding-footer">
                <p>Chất lượng - Sáng tạo - Hội nhập</p>
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
                    <p><i class="bi bi-exclamation-triangle-fill"></i> ${errorMessage}</p>
                </div>
            </c:if>

            <%-- Thông báo lỗi đăng ký --%>
            <c:if test="${not empty regError}">
                <div class="login-error-banner">
                    <p><i class="bi bi-exclamation-triangle-fill"></i> ${regError}</p>
                </div>
            </c:if>

            <%-- Tab Pills: Đăng nhập | Đăng ký --%>
            <div class="login-tabs">
                <button type="button" class="login-tab-btn ${activeTab != 'register' ? 'active' : ''}"
                        onclick="switchTab('login')" id="tab-btn-login">
                    <i class="bi bi-box-arrow-in-right"></i> Đăng nhập
                </button>
                <button type="button" class="login-tab-btn ${activeTab == 'register' ? 'active' : ''}"
                        onclick="switchTab('register')" id="tab-btn-register">
                    <i class="bi bi-person-plus"></i> Đăng ký
                </button>
            </div>

            <%-- ═══════ TAB 1: FORM ĐĂNG NHẬP ═══════ --%>
            <div class="login-tab-pane ${activeTab != 'register' ? 'active' : ''}" id="pane-login">
                <form action="${pageContext.request.contextPath}/auth" method="post" autocomplete="off">
                    <input type="hidden" name="action" value="login">

                    <%-- Tên đăng nhập --%>
                    <label class="login-label" for="login-username">Tên đăng nhập</label>
                    <div class="login-input-group">
                        <i class="bi bi-person input-icon"></i>
                        <input type="text" id="login-username" name="username"
                               value="${username}" placeholder="Nhập tên đăng nhập hoặc MSSV..." required autofocus>
                    </div>

                    <%-- Mật khẩu --%>
                    <label class="login-label" for="login-password">Mật khẩu</label>
                    <div class="login-input-group">
                        <i class="bi bi-lock input-icon"></i>
                        <input type="password" id="login-password" name="password"
                               placeholder="Nhập mật khẩu" required>
                    </div>

                    <%-- Ghi nhớ & Quên MK --%>
                    <div class="login-options-row">
                        <label class="login-remember">
                            <input type="checkbox" name="remember">
                            <span>Ghi nhớ đăng nhập</span>
                        </label>
                        <a href="#" class="login-forgot-link">Quên mật khẩu?</a>
                    </div>

                    <%-- Nút Đăng nhập --%>
                    <button type="submit" class="btn-login-primary">
                        Đăng Nhập &nbsp;→
                    </button>
                </form>

                <%-- Nút Thoát --%>
                <a href="${pageContext.request.contextPath}/" style="text-decoration:none;">
                    <button type="button" class="btn-login-exit">Thoát</button>
                </a>

                <%-- Link Đăng ký --%>
                <div class="login-signup-link">
                    <span>Chưa có tài khoản?</span>
                    <a href="javascript:void(0)" onclick="switchTab('register')">Đăng ký ngay</a>
                </div>
            </div>

            <%-- ═══════ TAB 2: FORM ĐĂNG KÝ ═══════ --%>
            <div class="login-tab-pane ${activeTab == 'register' ? 'active' : ''}" id="pane-register">
                <form action="${pageContext.request.contextPath}/auth" method="post" autocomplete="off">
                    <input type="hidden" name="action" value="register">

                    <%-- Họ và tên --%>
                    <label class="login-label" for="reg-fullname">Họ và tên</label>
                    <div class="login-input-group">
                        <i class="bi bi-person-badge input-icon"></i>
                        <input type="text" id="reg-fullname" name="fullName"
                               value="${regFullName}" placeholder="Ví dụ: Nguyễn Văn A" required>
                    </div>

                    <%-- Tên đăng nhập --%>
                    <label class="login-label" for="reg-username">Tên đăng nhập</label>
                    <div class="login-input-group">
                        <i class="bi bi-person input-icon"></i>
                        <input type="text" id="reg-username" name="username"
                               value="${regUsername}" placeholder="Tối thiểu 4 ký tự" required>
                    </div>

                    <%-- Email --%>
                    <label class="login-label" for="reg-email">Email liên hệ</label>
                    <div class="login-input-group">
                        <i class="bi bi-envelope input-icon"></i>
                        <input type="email" id="reg-email" name="email"
                               value="${regEmail}" placeholder="name@example.com">
                    </div>

                    <%-- Mật khẩu + Xác nhận (2 cột) --%>
                    <div class="register-password-row">
                        <div>
                            <label class="login-label" for="reg-pass">Mật khẩu</label>
                            <div class="login-input-group">
                                <i class="bi bi-lock input-icon"></i>
                                <input type="password" id="reg-pass" name="password"
                                       placeholder="≥ 6 ký tự" required>
                            </div>
                        </div>
                        <div>
                            <label class="login-label" for="reg-confirmpass">Xác nhận</label>
                            <div class="login-input-group">
                                <i class="bi bi-lock-fill input-icon"></i>
                                <input type="password" id="reg-confirmpass" name="confirmPassword"
                                       placeholder="Nhập lại mật khẩu" required>
                            </div>
                        </div>
                    </div>

                    <%-- Nút Đăng ký --%>
                    <button type="submit" class="btn-register-primary">
                        <i class="bi bi-person-check"></i> Tạo tài khoản mới
                    </button>
                </form>

                <%-- Link quay lại Đăng nhập --%>
                <div class="login-signup-link">
                    <span>Đã có tài khoản?</span>
                    <a href="javascript:void(0)" onclick="switchTab('login')">Đăng nhập ngay</a>
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
</script>

<jsp:include page="/includes/footer.jsp" />