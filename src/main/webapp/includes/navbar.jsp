<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="com.teamwork.data.NotificationDB" %>
<%@ page import="com.teamwork.business.Notification" %>
<%@ page import="java.util.List" %>

<%
    // Tự động nạp danh sách thông báo và số lượng chưa đọc cho Người dùng đang đăng nhập
    if (session.getAttribute("currentUser") != null) {
        com.teamwork.business.User navUser = (com.teamwork.business.User) session.getAttribute("currentUser");
        int unreadNotifCount = NotificationDB.countUnread(navUser.getId());
        List<Notification> notifList = NotificationDB.selectByRecipientId(navUser.getId());
        request.setAttribute("unreadNotifCount", unreadNotifCount);
        request.setAttribute("notifList", notifList);
    }
%>

<!-- Top Navbar: Đậm nét, Sang trọng (Dark Navy Slate #0f172a) - Tương phản cao 100% -->
<nav class="navbar navbar-expand-lg navbar-dark bg-dark-navy sticky-top shadow py-2 no-print" data-bs-theme="dark">
    <div class="container-fluid px-3 px-lg-4">
        
        <!-- Logo & Brand Name -->
        <a class="navbar-brand d-flex align-items-center fw-bold text-white fs-5" href="${pageContext.request.contextPath}/">
            <span class="brand-icon me-2 d-flex align-items-center justify-content-center shadow-sm">
                <i class="bi bi-grid-1x2-fill text-white"></i>
            </span>
            <span class="text-white fw-extrabold tracking-tight">TeamWork</span><span class="text-info fw-bold ms-1">Hub</span>
        </a>

        <!-- Mobile Toggle Button -->
        <button class="navbar-toggler border-0 shadow-none" type="button" data-bs-toggle="collapse" data-bs-target="#navbarContent" aria-controls="navbarContent" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>

        <!-- Navbar Menu -->
        <div class="collapse navbar-collapse" id="navbarContent">
            <!-- Left Side Navigation (Khi đã đăng nhập) -->
            <ul class="navbar-nav me-auto mb-2 mb-lg-0 ms-lg-3 ${not empty sessionScope.currentUser ? '' : 'd-none'}">
                <li class="nav-item">
                    <a class="nav-link text-light px-3 py-1 rounded-pill fw-medium d-flex align-items-center gap-1 ${activeNav == 'dashboard' ? 'active bg-white bg-opacity-10 text-white fw-bold' : ''}" 
                       href="${pageContext.request.contextPath}/project?action=list" style="transition: all 0.2s;">
                        <i class="bi bi-kanban text-info"></i> Không gian làm việc
                    </a>
                </li>
            </ul>

            <!-- Right Side: Command Palette ⚡ + Language + Theme + Notifications + Profile -->
            <div class="d-flex align-items-center gap-2 ms-auto">
                <!-- Nút Mở Command Palette (Ctrl + K) -->
                <button type="button" class="navbar-cp-trigger ${not empty sessionScope.currentUser ? '' : 'd-none'}" 
                        onclick="openCommandPalette()" 
                        data-cp-trigger
                        title="Mở thanh tìm kiếm & điều hướng lệnh (Ctrl + K)" 
                        aria-label="Mở Command Palette">
                    <i class="bi bi-search fs-9"></i>
                    <span class="d-none d-lg-inline fs-9">Tìm kiếm hoặc lệnh...</span>
                    <kbd class="cp-kbd d-none d-sm-inline-flex">Ctrl K</kbd>
                </button>

                <!-- Language selector -->
                <div class="dropdown">
                    <button class="btn btn-sm btn-outline-light rounded-pill px-2 py-1 d-flex align-items-center gap-1"
                            type="button" id="languageDropdown" data-bs-toggle="dropdown" aria-expanded="false"
                            title="Chọn ngôn ngữ" aria-label="Chọn ngôn ngữ">
                        <i class="bi bi-translate"></i>
                        <span id="languageCurrent" class="d-none d-sm-inline">VI</span>
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end shadow-sm mt-2" aria-labelledby="languageDropdown">
                        <li><button type="button" class="dropdown-item language-option" data-language="vi">Tiếng Việt</button></li>
                        <li><button type="button" class="dropdown-item language-option" data-language="en">English</button></li>
                    </ul>
                </div>

                <!-- Nút chuyển đổi Giao diện Sáng / Tối toàn hệ thống (Global Theme Switcher) -->
                <button type="button" id="globalThemeToggleBtn" onclick="toggleGlobalTheme()"
                        class="btn btn-sm btn-outline-light rounded-pill px-2 py-1 d-flex align-items-center gap-1 shadow-none"
                        title="Chuyển đổi giao diện Sáng / Tối (Light / Dark Mode)"
                        aria-label="Chuyển đổi giao diện Sáng / Tối">
                    <i class="bi bi-moon-stars-fill" id="globalThemeIconMoon"></i>
                    <i class="bi bi-sun-fill text-warning d-none" id="globalThemeIconSun"></i>
                    <span id="globalThemeBtnText" class="d-none d-sm-inline fs-9 fw-semibold">Tối</span>
                </button>
                
                <!-- =========================================================
                     1. QUẢ CHUÔNG THÔNG BÁO THỜI GIAN THỰC (NOTIFICATION BELL)
                     ========================================================= -->
                <div class="dropdown ${not empty sessionScope.currentUser ? '' : 'd-none'}">
                    <button class="btn navbar-bell-btn position-relative shadow-sm" 
                            type="button" id="notificationDropdown" data-bs-toggle="dropdown" aria-expanded="false" 
                            title="Trung tâm thông báo">
                        <i class="bi bi-bell-fill fs-6 text-warning"></i>
                        <c:if test="${unreadNotifCount > 0}">
                            <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger border border-light fs-9 px-1 py-0 shadow" style="font-size: 0.65rem;">
                                ${unreadNotifCount > 9 ? '9+' : unreadNotifCount}
                            </span>
                        </c:if>
                    </button>
                    
                    <div class="dropdown-menu dropdown-menu-end shadow-lg border border-secondary mt-2 rounded-4 p-0 overflow-hidden" 
                         style="width: 360px; max-width: 90vw; background-color: #ffffff; color: #0f172a;" 
                         data-bs-theme="light"
                         aria-labelledby="notificationDropdown">
                        
                        <!-- Header của Dropdown Thông Báo -->
                        <div class="p-3 bg-dark-navy text-white d-flex align-items-center justify-content-between border-bottom">
                            <div class="d-flex align-items-center gap-2">
                                <i class="bi bi-bell-fill text-warning"></i>
                                <span class="fw-bold fs-7">Trung Tâm Thông Báo</span>
                                <c:if test="${unreadNotifCount > 0}">
                                    <span class="badge bg-danger rounded-pill fs-9">${unreadNotifCount} mới</span>
                                </c:if>
                            </div>
                            <c:if test="${unreadNotifCount > 0}">
                                <a href="${pageContext.request.contextPath}/notification?action=readAll" 
                                   class="text-white-50 text-decoration-none fs-9" title="Đánh dấu tất cả là đã đọc">
                                    <i class="bi bi-check2-all me-1"></i> Đã đọc tất cả
                                </a>
                            </c:if>
                        </div>

                        <!-- Danh sách các Thông Báo (Tự động cuộn) -->
                        <div class="overflow-auto" style="max-height: 380px;">
                            <c:choose>
                                <c:when test="${not empty notifList}">
                                    <c:forEach items="${notifList}" var="n">
                                        <a href="${pageContext.request.contextPath}/notification?action=read&id=${n.id}&redirect=${n.link}" 
                                           class="d-flex align-items-start gap-3 p-3 border-bottom text-decoration-none transition-all ${n.read ? 'bg-white text-muted opacity-75' : 'bg-light-subtle text-dark fw-medium'} hover-bg-light">
                                            <div class="p-2 rounded-circle bg-white shadow-sm border mt-1 flex-shrink-0 d-flex align-items-center justify-content-center" style="width: 32px; height: 32px;">
                                                <i class="bi ${n.iconClass} fs-6"></i>
                                            </div>
                                            <div class="flex-grow-1 overflow-hidden">
                                                <div class="d-flex align-items-center justify-content-between mb-1">
                                                    <span class="fs-8 fw-bold ${n.read ? 'text-secondary' : 'text-dark'}">${n.title}</span>
                                                    <span class="fs-9 text-muted">${n.createdAt}</span>
                                                </div>
                                                <p class="fs-8 mb-0 text-secondary" style="display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">
                                                    ${n.content}
                                                </p>
                                            </div>
                                            <c:if test="${!n.read}">
                                                <span class="p-1 bg-danger rounded-circle mt-2 flex-shrink-0" title="Chưa đọc" style="width: 8px; height: 8px;"></span>
                                            </c:if>
                                        </a>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <div class="p-4 text-center text-muted">
                                        <i class="bi bi-bell-slash fs-2 d-block mb-2 opacity-50"></i>
                                        <span class="fs-8">Hiện chưa có thông báo nào dành cho bạn.</span>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <!-- Footer của Dropdown -->
                        <div class="p-2 bg-light text-center border-top">
                            <span class="fs-9 text-muted">Teamwork Hub • Tương tác thời gian thực</span>
                        </div>
                    </div>
                </div>

                <!-- =========================================================
                     2. USER PROFILE DROPDOWN (Khi ĐÃ ĐĂNG NHẬP)
                     ========================================================= -->
                <div class="dropdown ${not empty sessionScope.currentUser ? '' : 'd-none'}">
                    <button class="btn navbar-user-pill d-flex align-items-center gap-2 px-3 py-1 dropdown-toggle shadow-sm" 
                            type="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <div class="avatar-sm rounded-circle text-white d-flex align-items-center justify-content-center fw-bold fs-7 shadow-sm" style="background: linear-gradient(135deg, #395886 0%, #638ECB 100%);">
                            <i class="bi bi-person-fill"></i>
                        </div>
                        <span class="d-none d-md-inline text-white fw-bold fs-7">
                            ${sessionScope.currentUser.fullName}
                        </span>
                        <span class="badge rounded-pill fs-9 px-2 py-1 text-white" style="background-color: #638ECB;">
                            ${sessionScope.currentUser.role}
                        </span>
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end shadow-lg border border-secondary mt-2 rounded-3" data-bs-theme="light" style="background-color: #ffffff; color: #0f172a;">
                        <li class="px-3 py-2 border-bottom">
                            <div class="fw-bold text-dark fs-7">${sessionScope.currentUser.fullName}</div>
                            <div class="text-muted fs-8">@${sessionScope.currentUser.username} &bull; ${sessionScope.currentUser.email}</div>
                        </li>
                        <li>
                            <a class="dropdown-item py-2 d-flex align-items-center gap-2 fw-medium text-dark" 
                               href="${pageContext.request.contextPath}/profile">
                                <i class="bi bi-person-badge text-primary"></i> Hồ sơ chuyên môn của tôi
                            </a>
                        </li>
                        <li><hr class="dropdown-divider my-1"></li>
                        <li>
                            <a class="dropdown-item py-2 text-danger d-flex align-items-center gap-2 fw-medium" 
                               href="${pageContext.request.contextPath}/auth?action=logout">
                                <i class="bi bi-box-arrow-right"></i> Đăng xuất
                            </a>
                        </li>
                    </ul>
                </div>

                <!-- Trường hợp: CHƯA ĐĂNG NHẬP (Hiển thị nút Đăng nhập) -->
                <div class="${empty sessionScope.currentUser ? '' : 'd-none'}">
                    <a href="${pageContext.request.contextPath}/auth?action=viewLogin" 
                       class="btn btn-sm btn-primary px-3 py-2 rounded-pill fw-semibold shadow-sm text-decoration-none text-white">
                        <i class="bi bi-box-arrow-in-right me-1"></i> Đăng nhập / Đăng ký
                    </a>
                </div>
            </div>
        </div>
    </div>
</nav>

<!-- Command Palette Modal (Ctrl + K) -->
<jsp:include page="/includes/command_palette.jsp" />
