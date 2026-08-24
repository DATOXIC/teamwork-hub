<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!-- Top Navbar (Phong cách Basecamp / Trello) -->
<nav class="navbar navbar-expand-lg navbar-dark bg-dark-navy sticky-top shadow-sm py-2">
    <div class="container-fluid px-3 px-lg-4">
        <!-- Logo & Brand -->
        <a class="navbar-brand d-flex align-items-center fw-bold text-white fs-5" href="${pageContext.request.contextPath}/">
            <span class="brand-icon me-2 d-flex align-items-center justify-content-center">
                <i class="bi bi-grid-1x2-fill"></i>
            </span>
            TeamWork<span class="text-primary-accent ms-1">Hub</span>
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
                    <a class="nav-link text-white-50 px-2 py-1 rounded hover-light ${activeNav == 'dashboard' ? 'active text-white fw-semibold' : ''}" 
                       href="${pageContext.request.contextPath}/project?action=list">
                        <i class="bi bi-kanban me-1"></i> Không gian làm việc
                    </a>
                </li>
            </ul>

            <!-- Right Side: User Profile hoặc Login/Register Buttons -->
            <div class="d-flex align-items-center gap-2 ms-auto">
                <!-- Trường hợp: ĐÃ ĐĂNG NHẬP (Hiển thị Avatar & Tên) -->
                <div class="dropdown ${not empty sessionScope.currentUser ? '' : 'd-none'}">
                    <button class="btn btn-outline-light d-flex align-items-center gap-2 border-0 bg-navy-subtle px-3 py-1 rounded-pill dropdown-toggle shadow-none" 
                            type="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <div class="avatar-sm rounded-circle bg-primary text-white d-flex align-items-center justify-content-center fw-bold fs-7">
                            <i class="bi bi-person-fill"></i>
                        </div>
                        <span class="d-none d-md-inline text-white fw-medium fs-7">
                            ${sessionScope.currentUser.fullName}
                        </span>
                        <span class="badge bg-primary-subtle text-primary-accent border border-primary-subtle rounded-pill fs-8">
                            ${sessionScope.currentUser.role}
                        </span>
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end shadow border-0 mt-2 rounded-3">
                        <li class="px-3 py-2 border-bottom">
                            <div class="fw-semibold text-dark">${sessionScope.currentUser.fullName}</div>
                            <div class="text-muted fs-8">@${sessionScope.currentUser.username} &bull; ${sessionScope.currentUser.email}</div>
                        </li>
                        <li>
                            <a class="dropdown-item py-2 text-danger d-flex align-items-center gap-2" 
                               href="${pageContext.request.contextPath}/auth?action=logout">
                                <i class="bi bi-box-arrow-right"></i> Đăng xuất
                            </a>
                        </li>
                    </ul>
                </div>

                <!-- Trường hợp: CHƯA ĐĂNG NHẬP (Hiển thị nút Đăng nhập) -->
                <div class="${empty sessionScope.currentUser ? '' : 'd-none'}">
                    <a href="${pageContext.request.contextPath}/auth?action=viewLogin" 
                       class="btn btn-sm btn-primary-custom px-3 py-2 rounded-pill fw-semibold shadow-sm text-decoration-none">
                        <i class="bi bi-box-arrow-in-right me-1"></i> Đăng nhập / Đăng ký
                    </a>
                </div>
            </div>
        </div>
    </div>
</nav>
