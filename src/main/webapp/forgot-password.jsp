<%-- =========================================================================
     [MVC VIEW: FORGOT PASSWORD — 3 BƯỚC]
     - Controller: com.teamwork.controllers.AuthServlet (/auth), forward từ forwardForgot()
     - Request Attributes:
         * forgotStep (Integer): 1 = nhập username, 2 = nhập OTP, 3 = nhập mật khẩu mới
         * forgotUsername (String): username đang đặt lại (bước 2)
         * forgotError / forgotInfo (String): banner lỗi / thông tin
     ========================================================================= --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- ◀ SERVLET: AuthServlet → setAttribute("forgotStep") --%>
<c:set var="step" value="${empty forgotStep ? 1 : forgotStep}" />
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
    <title>Quên mật khẩu &bull; TeamWork Hub</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/images/favicon.png">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=Plus+Jakarta+Sans:wght@500;600;700&display=swap"
          rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/login.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles/page-components.css?v=<%= System.currentTimeMillis() %>">
</head>

<body class="login-body">

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

    <div class="login-wrapper">
        <div class="login-form-panel">
            <div class="login-card">

                <div class="login-card-header">
                    <h3>Quên Mật Khẩu</h3>
                    <p>
                        <c:choose>
                            <c:when test="${step == 1}">Nhập tên đăng nhập để nhận mã OTP qua email</c:when>
                            <c:when test="${step == 2}">Nhập mã OTP gồm 6 số đã gửi tới email của bạn</c:when>
                            <c:otherwise>Đặt mật khẩu mới cho tài khoản</c:otherwise>
                        </c:choose>
                    </p>
                </div>

                <%-- ◀ SERVLET: AuthServlet → setAttribute("forgotInfo") --%>
                <c:if test="${not empty forgotInfo}">
                    <div class="login-success-banner">
                        <p><i class="bi bi-check-circle-fill"></i> <c:out value="${forgotInfo}" /></p>
                    </div>
                </c:if>
                <%-- ◀ SERVLET: AuthServlet → setAttribute("forgotError") --%>
                <c:if test="${not empty forgotError}">
                    <div class="login-error-banner">
                        <p><i class="bi bi-exclamation-triangle-fill"></i> <c:out value="${forgotError}" /></p>
                    </div>
                </c:if>

                <%-- ═══════ BƯỚC 1: NHẬP USERNAME ═══════ --%>
                <c:if test="${step == 1}">
                    <%-- ▶ SERVLET: /auth → AuthServlet.doPost() → case "forgotRequest" → processForgotRequest() --%>
                    <form action="${pageContext.request.contextPath}/auth" method="post" autocomplete="off">
                        <input type="hidden" name="action" value="forgotRequest">

                        <label class="login-label" for="fp-username">Tên đăng nhập</label>
                        <div class="login-input-group">
                            <i class="bi bi-person input-icon"></i>
                            <input type="text" id="fp-username" name="username"
                                   value="<c:out value='${forgotUsername}'/>"
                                   placeholder="Nhập tên đăng nhập..." required autofocus>
                        </div>

                        <button type="submit" class="btn-login-primary">Gửi mã OTP &nbsp;→</button>
                    </form>
                </c:if>

                <%-- ═══════ BƯỚC 2: NHẬP OTP ═══════ --%>
                <c:if test="${step == 2}">
                    <%-- ▶ SERVLET: /auth → AuthServlet.doPost() → case "forgotVerify" → processForgotVerify() --%>
                    <form action="${pageContext.request.contextPath}/auth" method="post" autocomplete="off">
                        <input type="hidden" name="action" value="forgotVerify">

                        <label class="login-label" for="fp-otp">Mã OTP (6 số)</label>
                        <div class="login-input-group">
                            <i class="bi bi-shield-lock input-icon"></i>
                            <input type="text" id="fp-otp" name="otp" inputmode="numeric"
                                   pattern="[0-9]{6}" maxlength="6" placeholder="••••••"
                                   required autofocus>
                        </div>

                        <button type="submit" class="btn-login-primary">Xác nhận &nbsp;→</button>
                    </form>

                    <%-- Gửi lại mã (giãn cách tối thiểu 60 giây, kiểm tra ở server) --%>
                    <%-- ▶ SERVLET: /auth → AuthServlet.doPost() → case "forgotRequest" → processForgotRequest() --%>
                    <form action="${pageContext.request.contextPath}/auth" method="post" class="mt-3">
                        <input type="hidden" name="action" value="forgotRequest">
                        <%-- ◀ SERVLET: AuthServlet → setAttribute("forgotUsername") --%>
                        <input type="hidden" name="username" value="<c:out value='${forgotUsername}'/>">
                        <button type="submit" class="btn-login-exit">Gửi lại mã OTP</button>
                    </form>
                </c:if>

                <%-- ═══════ BƯỚC 3: MẬT KHẨU MỚI ═══════ --%>
                <c:if test="${step == 3}">
                    <%-- ▶ SERVLET: /auth → AuthServlet.doPost() → case "forgotReset" → processForgotReset() --%>
                    <form action="${pageContext.request.contextPath}/auth" method="post"
                          onsubmit="return validateResetForm()" autocomplete="off">
                        <input type="hidden" name="action" value="forgotReset">

                        <label class="login-label" for="fp-pass">Mật khẩu mới</label>
                        <div class="login-input-group">
                            <i class="bi bi-lock input-icon"></i>
                            <input type="password" id="fp-pass" name="password"
                                   placeholder="≥ 6 ký tự" required autofocus>
                            <button type="button" class="toggle-password-btn"
                                    onclick="togglePassword('fp-pass', this)"
                                    tabindex="-1" aria-label="Hiện/ẩn mật khẩu">
                                <i class="bi bi-eye"></i>
                            </button>
                        </div>
                        <div class="field-error" id="fp-pass-error"></div>

                        <label class="login-label" for="fp-confirm">Xác nhận mật khẩu</label>
                        <div class="login-input-group">
                            <i class="bi bi-lock-fill input-icon"></i>
                            <input type="password" id="fp-confirm" name="confirmPassword"
                                   placeholder="Nhập lại mật khẩu" required>
                            <button type="button" class="toggle-password-btn"
                                    onclick="togglePassword('fp-confirm', this)"
                                    tabindex="-1" aria-label="Hiện/ẩn mật khẩu">
                                <i class="bi bi-eye"></i>
                            </button>
                        </div>
                        <div class="field-error" id="fp-confirm-error"></div>

                        <button type="submit" class="btn-login-primary">Đặt lại mật khẩu</button>
                    </form>
                </c:if>

                <div class="login-signup-link mt-3">
                    <a href="${pageContext.request.contextPath}/auth?action=viewLogin">
                        <i class="bi bi-arrow-left"></i> Quay lại đăng nhập
                    </a>
                </div>

            </div>
        </div>
    </div>

    <script src="${pageContext.request.contextPath}/js/forgot-password.js"></script>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
