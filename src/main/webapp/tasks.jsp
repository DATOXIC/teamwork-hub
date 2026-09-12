<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
    <c:set var="subtaskMode" value="${cookie.preferred_subtask_mode != null && cookie.preferred_subtask_mode.value == 'expanded' ? 'expanded' : 'collapsed'}" />

        <!-- 1. NẠP HEADER & THANH ĐIỀU HƯỚNG CHUNG -->
        <!-- 1. NẠP HEADER CHUNG -->
        <jsp:include page="/includes/header.jsp" />

        <style>
            /* Khóa cứng Viewport SaaS: Triệt tiêu hoàn toàn thanh cuộn cấp độ trang web */
            html, body {
                overflow: hidden !important;
                height: 100vh !important;
                max-height: 100vh !important;
                width: 100vw !important;
                max-width: 100vw !important;
                margin: 0 !important;
                padding: 0 !important;
            }
        </style>

        <!-- =========================================================================
             CLICKUP 3.0 UNIFIED APP SHELL ARCHITECTURE (1 TRANG HỢP NHẤT)
             ========================================================================= -->
        <!-- =========================================================================
             UNIFIED MODERN SAAS WORKSPACE ARCHITECTURE
             ========================================================================= -->
        <div class="clickup-shell">

            <div class="clickup-islands-row">

                <!-- =========================================================================
                     1. CỘT DUY NHẤT BÊN TRÁI: UNIFIED WORKSPACE SIDEBAR (250PX)
                     ========================================================================= -->
                 <aside class="clickup-sidebar ${cookie.sidebar_collapsed.value == 'true' ? 'collapsed' : ''}" id="clickupSidebar">
                    <!-- Header: Logo Brand & Click-to-Collapse Sidebar -->
                    <div class="clickup-sidebar-header p-2">
                        <button type="button" class="sidebar-brand-btn w-100 d-flex align-items-center justify-content-between p-1.5 rounded-3 border-0 bg-transparent text-start" onclick="toggleClickUpSidebar()" title="Bấm vào Logo để thu gọn thanh bên (Ctrl+B)">
                            <div class="d-flex align-items-center gap-2 overflow-hidden">
                                <span class="brand-logo-icon d-inline-flex align-items-center justify-content-center text-white rounded-2 shadow-2xs flex-shrink-0" style="width: 28px; height: 28px; font-size: 0.9rem; background: linear-gradient(135deg, #395886 0%, #628ECB 100%);">
                                    <i class="bi bi-grid-1x2-fill"></i>
                                </span>
                                <div class="d-flex flex-column text-truncate" style="line-height: 1.2;">
                                    <span class="fw-bold text-dark text-truncate fs-8">TeamWork <span class="text-primary">Hub</span></span>
                                    <span class="fs-10 text-muted text-truncate fw-semibold">
                                        <c:choose>
                                            <c:when test="${sessionScope.currentUser.role == 'ADMIN'}">Quản trị viên</c:when>
                                            <c:when test="${sessionScope.currentUser.role == 'MANAGER'}">Trưởng nhóm</c:when>
                                            <c:otherwise>Thành viên</c:otherwise>
                                        </c:choose>
                                    </span>
                                </div>
                            </div>
                            <span class="sidebar-collapse-indicator text-secondary fs-8 p-1 rounded-2 d-inline-flex align-items-center justify-content-center" title="Thu gọn thanh bên">
                                <i class="bi bi-chevron-bar-left"></i>
                            </span>
                        </button>
                    </div>

                    <!-- Scrollable Body: Quick Nav, Favorites, Spaces List -->
                    <div class="clickup-sidebar-scroll flex-grow-1 overflow-y-auto">
                        <!-- Quick Search Box (Ctrl+K) -->
                        <div class="px-2.5 pt-2 pb-1">
                            <div class="clickup-search-box position-relative d-flex align-items-center">
                                <i class="bi bi-search text-muted fs-8 ms-2.5"></i>
                                <input type="text" id="clickupSearchInput" class="form-control form-control-sm border-0 bg-transparent fs-8 ps-2 pe-5 py-1-5 shadow-none" onkeyup="searchClickUpTasks(this.value)" placeholder="Tìm kiếm nhanh... (Ctrl+K)" autocomplete="off">
                                <span class="search-kbd-badge position-absolute end-0 me-2 text-muted">Ctrl+K</span>
                            </div>
                        </div>

                        <!-- Điều hướng toàn cục (Global Quick Nav) -->
                        <div class="clickup-sidebar-section pt-1">
                            <div class="d-flex flex-column gap-1">
                                <a href="javascript:void(0)" onclick="openInboxDrawer()" data-bs-toggle="offcanvas" data-bs-target="#inboxDrawer" class="clickup-nav-link" id="nav-inbox" title="Hộp thư thông báo">
                                    <div class="d-flex align-items-center gap-2">
                                        <span class="clickup-nav-icon bg-primary-subtle text-primary">
                                            <i class="bi bi-bell-fill"></i>
                                        </span>
                                        <span class="clickup-nav-label">Hộp thư thông báo</span>
                                    </div>
                                    <c:if test="${unreadNotifCount > 0}">
                                        <span class="badge bg-danger rounded-pill fs-9 px-2 py-0-5">${unreadNotifCount}</span>
                                    </c:if>
                                </a>
                                <a href="${pageContext.request.contextPath}/project?action=list" class="clickup-nav-link" id="nav-all-spaces" title="Xem tất cả không gian dự án">
                                    <div class="d-flex align-items-center gap-2">
                                        <span class="clickup-nav-icon bg-secondary-subtle text-secondary">
                                            <i class="bi bi-grid-fill"></i>
                                        </span>
                                        <span class="clickup-nav-label">Tất cả không gian</span>
                                    </div>
                                    <i class="bi bi-chevron-right fs-10 text-muted opacity-50 ms-auto"></i>
                                </a>
                            </div>
                        </div>

                        <!-- Section Mục yêu thích (Favorites) -->
                        <div class="clickup-sidebar-section" id="sidebarFavoritesSection">
                            <div class="clickup-section-title">
                                <span>Mục yêu thích</span>
                                <i class="bi bi-star-fill text-warning fs-9"></i>
                            </div>
                            <div id="favoritesList" class="d-flex flex-column gap-1">
                                <!-- Rendered tự động qua JS từ localStorage -->
                            </div>
                        </div>

                        <!-- Section Spaces (Danh sách Không Gian Dự Án) -->
                        <div class="clickup-sidebar-section">
                            <div class="clickup-section-title d-flex align-items-center justify-content-between">
                                <span>Không Gian Dự Án</span>
                                <span class="badge bg-light text-muted border rounded-pill fs-10 px-1.5 py-0">${not empty userProjects ? userProjects.size() : 0} dự án</span>
                            </div>
                            <div class="d-flex flex-column gap-1 mb-2">
                                <c:forEach items="${userProjects}" var="p">
                                    <a href="${pageContext.request.contextPath}/task?action=list&projectId=${p.id}" class="clickup-space-item ${p.id == project.id ? 'active' : ''}" title="${p.name}">
                                        <span class="clickup-space-icon ${p.id == project.id ? 'active-icon' : ''}">
                                            <i class="bi ${p.id == project.id ? 'bi-folder-check' : 'bi-folder2'}"></i>
                                        </span>
                                        <span class="text-truncate flex-grow-1 fs-8 fw-semibold space-name-text">${p.name}</span>
                                        <span class="badge ${p.soloProject ? 'badge-solo' : 'badge-team'} rounded-pill px-1-5 py-0 fs-10 fw-semibold">
                                            ${p.soloProject ? 'Cá nhân' : 'Nhóm'}
                                        </span>
                                    </a>
                                </c:forEach>
                            </div>

                            <!-- Cặp nút thao tác Dự án: Tạo mới & Nhập mã -->
                            <div class="d-flex align-items-center gap-1.5 mt-2.5">
                                <button type="button" class="btn btn-sm btn-create-space flex-grow-1 d-flex align-items-center justify-content-center gap-1.5 rounded-2 py-1-5 fs-8 text-white shadow-xs" data-bs-toggle="modal" data-bs-target="#createProjectModal" title="Tạo không gian dự án mới">
                                    <i class="bi bi-plus-lg"></i>
                                    <span>Tạo không gian</span>
                                </button>
                                <button type="button" class="btn btn-sm btn-join-code d-flex align-items-center justify-content-center gap-1 rounded-2 py-1-5 px-2.5 fs-8 text-secondary" data-bs-toggle="modal" data-bs-target="#joinByCodeModal" title="Nhập mã tham gia không gian">
                                    <i class="bi bi-key-fill text-warning"></i>
                                    <span>Nhập mã</span>
                                </button>
                            </div>
                        </div>
                    </div>

                    <!-- Footer Sidebar: User Profile Card & Phím tắt Help -->
                    <div class="clickup-sidebar-footer p-2.5 border-top d-flex align-items-center justify-content-between bg-white">
                        <div class="dropdown flex-grow-1 me-2">
                            <a href="#" class="sidebar-user-card d-flex align-items-center gap-2 text-decoration-none text-dark p-1 rounded-2" data-bs-toggle="dropdown" title="${sessionScope.currentUser.fullName}">
                                <div class="user-avatar-wrap position-relative flex-shrink-0">
                                    <div class="user-avatar-circle d-flex align-items-center justify-content-center text-white fw-bold shadow-2xs" style="width: 32px; height: 32px; font-size: 0.8rem; border-radius: 9px; background: linear-gradient(135deg, #1e293b 0%, #334155 100%);">
                                        ${sessionScope.currentUser.fullName.substring(0, 1).toUpperCase()}
                                    </div>
                                    <span class="user-status-dot position-absolute bottom-0 end-0 rounded-circle border border-white bg-success" style="width: 8px; height: 8px;" title="Đang trực tuyến"></span>
                                </div>
                                <div class="d-flex flex-column text-truncate" style="line-height: 1.25;">
                                    <span class="fw-bold text-dark fs-8 text-truncate">${sessionScope.currentUser.fullName}</span>
                                    <span class="fs-10 text-muted text-truncate">
                                        <c:choose>
                                            <c:when test="${sessionScope.currentUser.role == 'ADMIN'}">Quản trị viên</c:when>
                                            <c:when test="${sessionScope.currentUser.role == 'MANAGER'}">Trưởng nhóm</c:when>
                                            <c:otherwise>Thành viên</c:otherwise>
                                        </c:choose>
                                    </span>
                                </div>
                                <i class="bi bi-chevron-expand fs-9 text-muted ms-auto opacity-75"></i>
                            </a>
                            <ul class="dropdown-menu dropdown-menu-start shadow-lg border rounded-3 p-2 fs-8" style="min-width: 220px; z-index: 1070;">
                                <li class="px-2 py-1 mb-1 border-bottom">
                                    <span class="fw-bold text-dark d-block">${sessionScope.currentUser.fullName}</span>
                                    <span class="fs-9 text-muted">${sessionScope.currentUser.email}</span>
                                </li>
                                <li><a class="dropdown-item rounded-2 py-1-5" href="${pageContext.request.contextPath}/profile"><i class="bi bi-person me-2 text-primary"></i>Hồ sơ cá nhân</a></li>
                                <c:if test="${sessionScope.currentUser.role == 'ADMIN'}">
                                    <li><a class="dropdown-item rounded-2 py-1-5" href="${pageContext.request.contextPath}/admin?action=dashboard"><i class="bi bi-shield-lock me-2 text-warning"></i>Quản trị hệ thống</a></li>
                                </c:if>
                                <li><hr class="dropdown-divider my-1"></li>
                                <li><a class="dropdown-item rounded-2 py-1-5 text-danger" href="${pageContext.request.contextPath}/auth?action=logout"><i class="bi bi-box-arrow-right me-2"></i>Đăng xuất</a></li>
                            </ul>
                        </div>
                        <button type="button" class="btn btn-sm btn-light border rounded-circle p-0 d-inline-flex align-items-center justify-content-center shadow-2xs flex-shrink-0 sidebar-help-btn" data-bs-toggle="modal" data-bs-target="#shortcutsHelpModal" title="Phím tắt & Trợ giúp (?)" style="width: 28px; height: 28px;">
                            <i class="bi bi-question-circle text-secondary fs-8"></i>
                        </button>
                    </div>
                </aside>

                <!-- =========================================================================
                     2. MAIN WORKSPACE CANVAS
                     ========================================================================= -->
                <main class="clickup-main-panel">
                    <!-- Header Dự Án: Breadcrumb, Actions & Project Multi-View Tabs -->
                    <div class="clickup-main-header">
                        <!-- Hàng 1: Project Title, Badges & Action Buttons -->
                        <div class="clickup-project-title-row">
                            <!-- Left: Breadcrumb & Title -->
                            <div class="d-flex align-items-center gap-2">
                                <button type="button" class="btn btn-sm btn-light border rounded-2 p-1 btn-expand-sidebar ${cookie.sidebar_collapsed.value == 'true' ? '' : 'd-none'}" id="btnExpandSidebar" onclick="toggleClickUpSidebar()" title="Mở rộng thanh bên">
                                    <i class="bi bi-chevron-bar-right fs-8"></i>
                                </button>
                                <div class="d-flex align-items-center gap-1.5 text-muted fs-8">
                                    <span>Không gian</span>
                                    <i class="bi bi-chevron-right fs-9 text-muted"></i>
                                </div>
                                <h6 class="fw-bold text-dark mb-0 fs-7">${project.name}</h6>
                                <span class="badge ${project.projectTypeBadgeClass} rounded-pill px-2 py-0-5 fs-9 d-inline-flex align-items-center gap-1 shadow-2xs">
                                    <i class="bi ${project.projectTypeIcon}"></i> ${project.projectTypeLabel}
                                </span>
                                <c:if test="${project.ownerId == sessionScope.currentUser.id}">
                                    <button type="button" class="btn btn-sm btn-light border rounded-circle p-1 ms-1 d-inline-flex align-items-center justify-content-center shadow-2xs" data-bs-toggle="modal" data-bs-target="#projectSettingsModal" title="Cài đặt dự án & Quy trình" style="width: 22px; height: 22px;">
                                        <i class="bi bi-gear text-secondary fs-9"></i>
                                    </button>
                                </c:if>
                                <i class="bi bi-star text-muted fs-8 ms-1" id="btnStarProject" style="cursor: pointer;" title="Đánh dấu dự án yêu thích" onclick="toggleProjectFavorite(${project.id}, '<c:out value="${project.name}" />')"></i>
                            </div>

                            <!-- Right: Actions & Badges -->
                            <div class="d-flex align-items-center gap-2">
                                <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2.5 py-1 fs-9 d-none d-md-inline-flex align-items-center gap-1" id="cloudSyncBadge">
                                    <i class="bi bi-cloud-check-fill"></i>
                                    <span id="cloudSyncText">Đã đồng bộ Cloud</span>
                                </span>
                                <c:if test="${project.ownerId == sessionScope.currentUser.id && project.teamProject}">
                                    <button type="button" class="btn btn-sm btn-outline-secondary rounded-pill px-2-5 py-1 fs-9 d-flex align-items-center gap-1" data-bs-toggle="modal" data-bs-target="#inviteMemberModal">
                                        <i class="bi bi-person-plus"></i> Mời bạn
                                    </button>
                                </c:if>
                                <!-- Nút Xuất Excel (.csv UTF-8 BOM) -->
                                <a href="${pageContext.request.contextPath}/task?action=exportCsv&projectId=${project.id}" 
                                   class="btn btn-sm btn-light border text-success fw-semibold d-inline-flex align-items-center gap-1 px-2.5 py-1 rounded-pill shadow-2xs text-nowrap"
                                   title="Tải toàn bộ danh sách công việc của dự án về máy tính (.csv chuẩn UTF-8 BOM)">
                                    <i class="bi bi-file-earmark-excel-fill text-success fs-8"></i>
                                    <span class="fs-8">Xuất Excel</span>
                                </a>
                            </div>
                        </div>

                        <!-- Hàng 2: Multi-View Tabs (Phân hệ dự án chính - Độc quyền tại Header) -->
                        <div class="clickup-multiview-bar">
                            <ul class="clickup-tabs">
                                <li>
                                    <a href="javascript:void(0)" onclick="switchClickUpTab('tasks')" class="clickup-tab-link ${currentView == 'tasks' ? 'active' : ''}" id="tab-btn-tasks">
                                        <i class="bi bi-list-task"></i> Công việc
                                        <span class="badge bg-light text-muted border rounded-pill fs-9 ms-1">${not empty allProjectTasks ? allProjectTasks.size() : 0}</span>
                                    </a>
                                </li>
                                <li>
                                    <a href="javascript:void(0)" onclick="switchClickUpTab('chat')" class="clickup-tab-link ${currentView == 'chat' ? 'active' : ''}" id="tab-btn-chat">
                                        <i class="bi bi-chat-dots"></i> Thảo luận
                                    </a>
                                </li>
                                <li>
                                    <a href="javascript:void(0)" onclick="switchClickUpTab('docs')" class="clickup-tab-link ${currentView == 'docs' ? 'active' : ''}" id="tab-btn-docs">
                                        <i class="bi bi-journal-text"></i> Tài liệu
                                        <c:if test="${not empty docList}">
                                            <span class="badge bg-light text-muted border rounded-pill fs-9 ms-1">${docList.size()}</span>
                                        </c:if>
                                    </a>
                                </li>
                                <li>
                                    <a href="javascript:void(0)" onclick="switchClickUpTab('schedule')" class="clickup-tab-link ${currentView == 'schedule' ? 'active' : ''}" id="tab-btn-schedule">
                                        <i class="bi bi-calendar-event"></i> Lịch biểu
                                    </a>
                                </li>
                                <li>
                                    <a href="javascript:void(0)" onclick="switchClickUpTab('metrics')" class="clickup-tab-link ${currentView == 'metrics' ? 'active' : ''}" id="tab-btn-metrics">
                                        <i class="bi bi-pie-chart-fill text-info"></i> Phân bổ công việc
                                        <span class="badge bg-info-subtle text-info rounded-pill px-1.5 py-0 fs-10 fw-bold ms-1">
                                            ${userWorkloadList.size()}
                                        </span>
                                    </a>
                                </li>
                                <li>
                                    <a href="javascript:void(0)" onclick="switchClickUpTab('activity')" class="clickup-tab-link ${currentView == 'activity' ? 'active' : ''}" id="tab-btn-activity">
                                        <i class="bi bi-clock-history"></i> Hoạt động
                                        <c:if test="${not empty activityLogs}">
                                            <span class="badge bg-light text-secondary border rounded-pill fs-9 ms-1">${activityLogs.size()}</span>
                                        </c:if>
                                    </a>
                                </li>
                            </ul>
                        </div>
                    </div>

                <!-- ClickUp Control Toolbar (Views, Subtasks, Progress, and + Add Task) -->
                <c:set var="progTotal" value="${not empty allProjectTasks ? allProjectTasks.size() : 0}" />
                <c:set var="progDone" value="${not empty doneTasks ? doneTasks.size() : 0}" />
                <c:set var="progInProg" value="${not empty inProgressTasks ? inProgressTasks.size() : 0}" />
                <c:set var="progTodo" value="${not empty todoTasks ? todoTasks.size() : 0}" />
                <c:set var="pctDone" value="${progTotal > 0 ? ((progDone * 100 - (progDone * 100 % progTotal)) / progTotal) : 0}" />
                <c:set var="pctInProg" value="${progTotal > 0 ? ((progInProg * 100 - (progInProg * 100 % progTotal)) / progTotal) : 0}" />
                <c:set var="pctTodo" value="${progTotal > 0 ? (100 - pctDone - pctInProg) : 0}" />

                <div class="clickup-control-toolbar ${currentView == 'tasks' ? '' : 'd-none'} d-flex align-items-center justify-content-between flex-wrap gap-2 py-2 px-3 bg-white border-bottom shadow-2xs">
                    <div class="d-flex align-items-center gap-2 flex-wrap flex-grow-1">
                        <!-- View Switcher (List vs Board) -->
                        <div class="clickup-view-switcher" id="taskViewSwitcher">
                            <button type="button" class="clickup-view-btn ${taskView == 'list' ? 'active' : ''}" id="btn-view-list" onclick="switchTaskSubView('list')">
                                <i class="bi bi-list-ul"></i> List
                            </button>
                            <button type="button" class="clickup-view-btn ${taskView == 'board' ? 'active' : ''}" id="btn-view-board" onclick="switchTaskSubView('board')">
                                <i class="bi bi-kanban"></i> Board
                            </button>
                        </div>

                        <!-- Pill Group: Status -->
                        <span class="btn btn-sm btn-light border rounded-pill px-2.5 py-1 fs-8 text-secondary fw-medium shadow-2xs">
                            <i class="bi bi-layers text-primary me-1"></i> Group: Status
                        </span>

                        <!-- Nút Chuyển Đổi Subtasks -->
                        <button type="button" 
                                class="btn btn-sm btn-light border d-flex align-items-center gap-1.5 px-2.5 py-1 rounded-pill shadow-2xs fs-8 text-dark fw-medium" 
                                id="btnToggleSubtasks" 
                                onclick="toggleAllSubtasks()" 
                                title="Bấm 1 lần để chuyển đổi giữa Đóng và Mở rộng việc con (Phím tắt: S)">
                            <i class="bi ${subtaskMode == 'expanded' ? 'bi-chevron-down text-primary' : 'bi-chevron-right text-secondary'}" id="subtaskToggleIcon"></i>
                            <span id="subtaskToggleLabel">Subtasks: ${subtaskMode == 'expanded' ? 'Mở rộng' : 'Đóng'}</span>
                        </button>

                        <!-- Bộ lọc Khoảng thời gian (Timeframe Filter) -->
                        <div class="dropdown">
                            <button class="btn btn-sm btn-light border rounded-pill px-2.5 py-1 fs-8 text-dark fw-medium shadow-2xs dropdown-toggle d-flex align-items-center gap-1.5" type="button" id="timeframeDropdownBtn" data-bs-toggle="dropdown" aria-expanded="false" title="Lọc theo khoảng thời gian hạn chót">
                                <i class="bi bi-calendar3 text-primary"></i>
                                <span id="currentTimeframeLabel">Tất cả thời gian</span>
                            </button>
                            <ul class="dropdown-menu shadow-sm border-0 fs-8" aria-labelledby="timeframeDropdownBtn">
                                <li><a class="dropdown-item active" href="javascript:void(0)" onclick="filterTasksByTimeframe('ALL', 'Tất cả thời gian')"><i class="bi bi-calendar-range me-2 text-secondary"></i> Tất cả thời gian</a></li>
                                <li><a class="dropdown-item" href="javascript:void(0)" onclick="filterTasksByTimeframe('THIS_WEEK', 'Tuần này')"><i class="bi bi-calendar-week me-2 text-primary"></i> Tuần này</a></li>
                                <li><a class="dropdown-item" href="javascript:void(0)" onclick="filterTasksByTimeframe('THIS_MONTH', 'Tháng này')"><i class="bi bi-calendar-month me-2 text-success"></i> Tháng này</a></li>
                                <li><a class="dropdown-item" href="javascript:void(0)" onclick="filterTasksByTimeframe('OVERDUE', 'Quá hạn')"><i class="bi bi-exclamation-triangle me-2 text-danger"></i> Quá hạn</a></li>
                            </ul>
                        </div>

                        <c:if test="${project.teamProject}">
                            <div class="vr mx-1 opacity-25 d-none d-sm-block" style="height: 18px;"></div>

                            <!-- Cụm Lọc thành viên ClickUp 3.0 -->
                            <div class="d-flex align-items-center flex-wrap gap-1.5" id="globalAssigneeFilterGroup">
                                <button type="button" id="assignee-btn-all" class="assignee-filter-btn active" onclick="filterClickUpTasks('ALL')" title="Xem tất cả công việc & thành viên">
                                    Tất cả
                                </button>
                                <button type="button" id="assignee-btn-me" class="assignee-filter-btn" onclick="filterClickUpTasks('MY_TASKS')" title="Chế độ 'Me Mode' - Việc của tôi">
                                    <i class="bi bi-person-fill text-primary"></i> Việc tôi
                                </button>

                                <!-- Dải Avatar Thành viên dự án -->
                                <div class="d-flex align-items-center gap-1 flex-wrap ms-1">
                                    <c:forEach items="${userWorkloadList}" var="uw" varStatus="loop">
                                        <c:if test="${loop.index < 7}">
                                            <button type="button" class="assignee-avatar-btn position-relative" 
                                                    data-user-id="${uw.user.id}" 
                                                    onclick="handleToolbarAvatarClick('${uw.user.id}', '<c:out value="${uw.user.fullName}" />')"
                                                    title="${uw.user.fullName} (${uw.totalTasks} việc &bull; ${uw.doneTasks} xong &bull; ${uw.overdueTasks > 0 ? uw.overdueTasks : 0} quá hạn)">
                                                <span class="avatar-circle-sm bg-primary text-white rounded-circle d-flex align-items-center justify-content-center" style="width: 24px; height: 24px; font-size: 0.65rem;">
                                                    ${uw.user.fullName.substring(0, 1).toUpperCase()}
                                                </span>
                                                <c:if test="${uw.overdueTasks > 0}">
                                                    <span class="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-light rounded-circle" style="transform: translate(-30%, -20%) !important;" title="${uw.overdueTasks} việc quá hạn"></span>
                                                </c:if>
                                            </button>
                                        </c:if>
                                    </c:forEach>
                                    <c:if test="${userWorkloadList.size() > 7}">
                                        <span class="badge bg-light text-muted border rounded-circle p-1 fs-9 d-flex align-items-center justify-content-center" style="width: 24px; height: 24px;" title="Còn ${userWorkloadList.size() - 7} thành viên khác">
                                            +${userWorkloadList.size() - 7}
                                        </span>
                                    </c:if>
                                </div>

                                <c:if test="${submittedCount > 0}">
                                    <button type="button" id="assignee-btn-submitted" class="status-filter-btn status-btn-submitted ms-1" onclick="filterClickUpTasks('STATUS_SUBMITTED')" title="Lọc các nhiệm vụ đã nộp báo cáo chờ PM duyệt">
                                        <i class="bi bi-send-check"></i>
                                        <span>Chờ duyệt</span>
                                        <span class="status-count-pill">${submittedCount}</span>
                                    </button>
                                </c:if>
                            </div>
                        </c:if>
                    </div>

                    <!-- Right: Fixed Primary CTA + Add Task Button -->
                    <div class="d-flex align-items-center gap-2 ms-auto flex-shrink-0">
                        <!-- Mini Progress Strip -->
                        <div class="clickup-mini-progress d-none d-xl-inline-flex" title="Tiến độ: ${pctDone}% (${progDone}/${progTotal} việc hoàn thành)">
                            <i class="bi bi-pie-chart-fill text-primary"></i>
                            <span class="tabular-nums">${pctDone}%</span>
                            <div class="clickup-mini-progress-bar">
                                <div style="width: ${pctDone}%; background-color: #10b981;"></div>
                                <div style="width: ${pctInProg}%; background-color: #3b82f6;"></div>
                                <div style="width: ${pctTodo}%; background-color: #f59e0b;"></div>
                            </div>
                        </div>

                        <button type="button" class="clickup-add-task-btn-dark" data-bs-toggle="modal" data-bs-target="#addTaskModal" title="Thêm công việc mới">
                            <i class="bi bi-plus-lg"></i>
                            <span>Add Task</span>
                        </button>
                    </div>
                </div>


                <!-- Workspace Content Area (Scrollable) -->
                <div class="clickup-workspace-body">
                    <!-- Toast thông báo -->
                    <jsp:include page="/includes/toast.jsp" />

                    <!-- Dải Banner Phản Hồi Khi Đang Lọc Task -->
                    <div id="activeFilterBanner" class="active-filter-banner d-none">
                        <div class="d-flex align-items-center gap-2">
                            <span class="avatar-circle-sm bg-primary text-white rounded-circle d-flex align-items-center justify-content-center" style="width: 22px; height: 22px; font-size: 0.65rem;">
                                <i class="bi bi-funnel-fill"></i>
                            </span>
                            <span id="activeFilterText">Đang lọc: <strong>Công việc của tôi</strong></span>
                        </div>
                        <button type="button" class="btn btn-sm btn-link text-primary fw-bold text-decoration-none p-0 fs-8" onclick="filterClickUpTasks('ALL')">
                            <i class="bi bi-x-circle me-1"></i> Xóa bộ lọc (Xem tất cả)
                        </button>
                    </div>

                    <!-- =========================================================================
                         TAB 1: TASKS CONTAINER (GỒM LIST VIEW VÀ BOARD VIEW)
                         ========================================================================= -->
                    <div id="clickup-view-tasks" class="clickup-view-pane ${currentView == 'tasks' ? '' : 'd-none'}">



                        <!-- A. HIERARCHICAL LIST VIEW (CLICKUP 3.0 LIST VIEW PHÂN CẤP CHA - CON) -->
                        <div id="task-subview-list" class="${taskView == 'list' ? '' : 'd-none'}">
                            <div class="table-responsive">
                                <table class="clickup-list-table">
                                    <thead>
                                        <tr class="clickup-list-header-row">
                                            <th style="width: ${project.soloProject ? '58%' : '44%'};">Name</th>
                                            <c:if test="${project.teamProject}">
                                                <th style="width: 16%;">Assignee</th>
                                            </c:if>
                                            <th style="width: 14%;">Priority</th>
                                            <th style="width: 14%;">Team / Nhãn</th>
                                            <th style="width: ${project.soloProject ? '14%' : '12%'}; text-align: right;">Hạn chót</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <!-- NHÓM 1: DONE (ĐÃ HOÀN THÀNH) -->
                                        <tr class="clickup-group-header-row">
                                            <td colspan="${project.soloProject ? 4 : 5}">
                                                <div class="clickup-group-banner text-success d-flex align-items-center" onclick="toggleClickUpGroup('done')">
                                                    <i class="bi bi-chevron-down me-1" id="chevron-done"></i>
                                                    <span class="clickup-group-badge bg-success text-white">DONE</span>
                                                    <span class="text-secondary fs-8 ms-1" id="group-count-done">${doneTasks.size()}</span>
                                                </div>
                                            </td>
                                        </tr>
                                        <c:forEach items="${doneTasks}" var="task">
                                            <tr class="clickup-task-row group-done-row" data-task-id="${task.id}" data-assignee-id="${task.assigneeId}" data-task-status="${task.status}" data-task-title="<c:out value='${task.title}' />" onclick="openClickUpTask(${task.id})">
                                                <td>
                                                    <div class="d-flex align-items-center gap-2 ps-2">
                                                        <c:choose>
                                                            <c:when test="${not empty taskSubTasksMap[task.id]}">
                                                                <span class="subtask-caret ${subtaskMode == 'expanded' ? 'is-expanded' : ''}" id="caret-${task.id}" onclick="event.stopPropagation(); toggleSubtasks(${task.id}, event);" title="Thu gọn / Mở rộng việc con">
                                                                    <i class="bi bi-chevron-${subtaskMode == 'expanded' ? 'down' : 'right'}"></i>
                                                                </span>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <span style="width: 18px; display: inline-block;"></span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                        <span class="clickup-status-dot dot-done dot-locked" id="status-dot-${task.id}" onclick="event.stopPropagation();" title="Công việc đã hoàn thành (Đã khóa, không thể thay đổi)">
                                                            <i class="bi bi-check text-white"></i>
                                                        </span>
                                                        <span class="fw-semibold text-secondary text-decoration-line-through text-truncate" id="task-title-text-${task.id}" style="max-width: 300px;">${task.title}</span>
                                                        <c:if test="${not empty taskSubTasksMap[task.id]}">
                                                            <span class="badge bg-light text-secondary border rounded-pill fs-9" id="subtask-count-badge-${task.id}" title="${taskSubTasksMap[task.id].size()} việc con">
                                                                <i class="bi bi-link-45deg"></i> <span class="badge-num">${taskSubTasksMap[task.id].size()}</span>
                                                            </span>
                                                        </c:if>
                                                    </div>
                                                </td>
                                                <c:if test="${project.teamProject}">
                                                    <td>
                                                        <div class="d-flex align-items-center gap-1-5">
                                                            <span class="avatar-circle-sm bg-success text-white rounded-circle d-flex align-items-center justify-content-center" style="width: 22px; height: 22px; font-size: 0.65rem;" title="${task.assigneeName}">
                                                                ${task.assigneeName.substring(0, 1).toUpperCase()}
                                                            </span>
                                                            <span class="fs-9 text-secondary text-truncate" style="max-width: 90px;">${task.assigneeName}</span>
                                                        </div>
                                                    </td>
                                                </c:if>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${task.priority == 'HIGH'}">
                                                            <span class="clickup-priority-flag flag-urgent"><i class="bi bi-flag-fill"></i> Urgent</span>
                                                        </c:when>
                                                        <c:when test="${task.priority == 'MEDIUM'}">
                                                            <span class="clickup-priority-flag flag-normal"><i class="bi bi-flag-fill"></i> Normal</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="clickup-priority-flag flag-low"><i class="bi bi-flag-fill"></i> Low</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${not empty task.labelList}">
                                                            <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2 py-0-5 fs-9">
                                                                ${task.getLabelDisplayName(task.labelList[0])}
                                                            </span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5 fs-9">Design</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td style="text-align: right;">
                                                    <span class="fs-9 text-muted">${not empty task.dueDate ? task.dueDate : '—'}</span>
                                                </td>
                                            </tr>
                                            <!-- Subtasks -->
                                            <c:forEach items="${taskSubTasksMap[task.id]}" var="st">
                                                <c:set var="isStDone" value="${st.status == 'APPROVED' || st.status == 'DONE'}" />
                                                <tr class="clickup-subtask-row group-done-row ${subtaskMode == 'expanded' ? '' : 'd-none'}" data-parent-id="${task.id}" data-subtask-id="${st.id}" data-assignee-id="${st.assigneeId}" data-subtask-title="<c:out value='${st.title}' />" onclick="event.stopPropagation(); openClickUpTask(${task.id}, ${st.id});">
                                                    <td>
                                                        <div class="d-flex align-items-center gap-2" style="padding-left: 36px;">
                                                            <span class="clickup-status-dot dot-done dot-locked" id="subtask-status-dot-${st.id}" onclick="event.stopPropagation();" title="Việc con đã hoàn tất (Đã khóa)">
                                                                <i class="bi bi-check text-white"></i>
                                                            </span>
                                                            <span class="text-secondary text-truncate fs-8 ${isStDone ? 'text-decoration-line-through' : ''}" id="subtask-title-text-${st.id}" style="max-width: 290px;">${st.title}</span>
                                                        </div>
                                                    </td>
                                                    <c:if test="${project.teamProject}">
                                                        <td>
                                                            <span class="fs-9 text-muted text-truncate" style="max-width: 90px;">${st.assigneeName}</span>
                                                        </td>
                                                    </c:if>
                                                    <td>
                                                        <span class="fs-9 text-muted">Subtask</span>
                                                    </td>
                                                    <td>
                                                        <span class="badge ${isStDone ? 'bg-success-subtle text-success border border-success-subtle' : 'bg-light text-secondary border'} rounded-pill px-1-5 py-0 fs-9" id="subtask-badge-${st.id}">${st.status}</span>
                                                    </td>
                                                    <td style="text-align: right;">
                                                        <span class="fs-9 text-muted">${not empty st.dueDate ? st.dueDate : '—'}</span>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </c:forEach>

                                        <!-- NHÓM 2: IN PROGRESS (ĐANG LÀM) -->
                                        <tr class="clickup-group-header-row">
                                            <td colspan="${project.soloProject ? 4 : 5}">
                                                <div class="clickup-group-banner text-primary mt-3 d-flex align-items-center" onclick="toggleClickUpGroup('inprog')">
                                                    <i class="bi bi-chevron-down me-1" id="chevron-inprog"></i>
                                                    <span class="clickup-group-badge bg-primary text-white">IN PROGRESS</span>
                                                    <span class="text-secondary fs-8 ms-1" id="group-count-inprog">${inProgressTasks.size()}</span>
                                                </div>
                                            </td>
                                        </tr>
                                        <c:forEach items="${inProgressTasks}" var="task">
                                            <tr class="clickup-task-row group-inprog-row" data-task-id="${task.id}" data-assignee-id="${task.assigneeId}" data-task-status="${task.status}" data-task-title="<c:out value='${task.title}' />" onclick="openClickUpTask(${task.id})">
                                                <td>
                                                    <div class="d-flex align-items-center gap-2 ps-2">
                                                        <c:choose>
                                                            <c:when test="${not empty taskSubTasksMap[task.id]}">
                                                                <span class="subtask-caret ${subtaskMode == 'expanded' ? 'is-expanded' : ''}" id="caret-${task.id}" onclick="event.stopPropagation(); toggleSubtasks(${task.id}, event);" title="Thu gọn / Mở rộng việc con">
                                                                    <i class="bi bi-chevron-${subtaskMode == 'expanded' ? 'down' : 'right'}"></i>
                                                                </span>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <span style="width: 18px; display: inline-block;"></span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                        <span class="clickup-status-dot dot-inprog" id="status-dot-${task.id}" onclick="event.stopPropagation(); openStatusDropdown(event, ${task.id}, false, '${task.status}');" title="Trạng thái: ${task.status} (Bấm để đổi)"></span>
                                                        <span class="fw-semibold text-dark text-truncate" id="task-title-text-${task.id}" style="max-width: 300px;">${task.title}</span>
                                                        <c:if test="${task.status == 'SUBMITTED'}">
                                                            <span class="badge bg-purple text-white rounded-pill px-1-5 py-0 fs-9 ms-1" title="Nhiệm vụ đã nộp báo cáo kết quả, chờ PM duyệt">
                                                                <i class="bi bi-send-check me-0-5"></i>Chờ duyệt
                                                            </span>
                                                        </c:if>
                                                        <c:if test="${not empty taskSubTasksMap[task.id]}">
                                                            <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill fs-9" id="subtask-count-badge-${task.id}" title="${taskSubTasksMap[task.id].size()} việc con (${taskProgressMap[task.id]}%)">
                                                                <i class="bi bi-link-45deg"></i> <span class="badge-num">${taskSubTasksMap[task.id].size()}</span>
                                                            </span>
                                                        </c:if>
                                                        <button type="button" class="task-hover-add-subtask-btn" onclick="event.stopPropagation(); showInlineCreateSubtask(${task.id}, event);" title="Thêm việc con">
                                                            <i class="bi bi-plus"></i>
                                                        </button>
                                                    </div>
                                                </td>
                                                <c:if test="${project.teamProject}">
                                                    <td>
                                                        <div class="d-flex align-items-center gap-1-5">
                                                            <span class="avatar-circle-sm bg-primary text-white rounded-circle d-flex align-items-center justify-content-center" style="width: 22px; height: 22px; font-size: 0.65rem;" title="${task.assigneeName}">
                                                                ${task.assigneeName.substring(0, 1).toUpperCase()}
                                                            </span>
                                                            <span class="fs-9 text-dark fw-medium text-truncate" style="max-width: 90px;">${task.assigneeName}</span>
                                                        </div>
                                                    </td>
                                                </c:if>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${task.priority == 'HIGH'}">
                                                            <span class="clickup-priority-flag flag-urgent"><i class="bi bi-flag-fill"></i> Urgent</span>
                                                        </c:when>
                                                        <c:when test="${task.priority == 'MEDIUM'}">
                                                            <span class="clickup-priority-flag flag-normal"><i class="bi bi-flag-fill"></i> Normal</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="clickup-priority-flag flag-low"><i class="bi bi-flag-fill"></i> Low</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${not empty task.labelList}">
                                                            <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2 py-0-5 fs-9">
                                                                ${task.getLabelDisplayName(task.labelList[0])}
                                                            </span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="badge bg-info-subtle text-info-emphasis border border-info-subtle rounded-pill px-2 py-0-5 fs-9">PMM</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td style="text-align: right;">
                                                    <span class="fs-9 text-dark fw-medium">${not empty task.dueDate ? task.dueDate : '—'}</span>
                                                </td>
                                            </tr>
                                            <!-- Subtasks -->
                                            <c:forEach items="${taskSubTasksMap[task.id]}" var="st">
                                                <c:set var="isStDone" value="${st.status == 'APPROVED' || st.status == 'DONE'}" />
                                                <tr class="clickup-subtask-row group-inprog-row ${subtaskMode == 'expanded' ? '' : 'd-none'}" data-parent-id="${task.id}" data-subtask-id="${st.id}" data-assignee-id="${st.assigneeId}" data-subtask-title="<c:out value='${st.title}' />" onclick="event.stopPropagation(); openClickUpTask(${task.id}, ${st.id});">
                                                    <td>
                                                        <div class="d-flex align-items-center gap-2 ps-4" style="padding-left: 36px !important;">
                                                            <span class="clickup-status-dot dot-${isStDone ? 'done' : 'todo'}" id="subtask-status-dot-${st.id}" onclick="event.stopPropagation(); openStatusDropdown(event, ${st.id}, true, '${st.status}', ${task.id});" title="Việc con: ${st.status} (Bấm để đổi)">
                                                                <c:if test="${isStDone}">
                                                                    <i class="bi bi-check text-white"></i>
                                                                </c:if>
                                                            </span>
                                                            <span class="text-dark text-truncate fs-8 ${isStDone ? 'text-decoration-line-through text-muted' : ''}" id="subtask-title-text-${st.id}" style="max-width: 290px;">${st.title}</span>
                                                        </div>
                                                    </td>
                                                    <c:if test="${project.teamProject}">
                                                        <td>
                                                            <span class="fs-9 text-dark text-truncate" style="max-width: 90px;">${st.assigneeName}</span>
                                                        </td>
                                                    </c:if>
                                                    <td>
                                                        <span class="fs-9 text-muted">Subtask</span>
                                                    </td>
                                                    <td>
                                                        <span class="badge ${isStDone ? 'bg-success-subtle text-success border border-success-subtle' : 'bg-light text-secondary border'} rounded-pill px-1-5 py-0 fs-9" id="subtask-badge-${st.id}">${st.status}</span>
                                                    </td>
                                                    <td style="text-align: right;">
                                                        <span class="fs-9 text-muted">—</span>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </c:forEach>

                                        <!-- NHÓM 3: TO DO (CẦN LÀM) -->
                                        <tr class="clickup-group-header-row">
                                            <td colspan="${project.soloProject ? 4 : 5}">
                                                <div class="clickup-group-banner text-secondary mt-3 d-flex align-items-center" onclick="toggleClickUpGroup('todo')">
                                                    <i class="bi bi-chevron-down me-1" id="chevron-todo"></i>
                                                    <span class="clickup-group-badge bg-secondary text-white">TO DO</span>
                                                    <span class="text-secondary fs-8 ms-1" id="group-count-todo">${todoTasks.size()}</span>
                                                    <button type="button" class="clickup-group-add-btn ms-2" onclick="event.stopPropagation(); showInlineCreateTask('todo');" title="Thêm công việc vào TO DO">
                                                        <i class="bi bi-plus-lg"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                        <!-- Inline Quick Task Create for TO DO -->
                                        <tr id="inline-task-row-todo" class="clickup-inline-create-row d-none">
                                            <td>
                                                <div class="d-flex align-items-center gap-2 ps-2">
                                                    <span class="clickup-status-dot dot-todo"></span>
                                                    <input type="text" id="inline-task-title-todo" class="form-control form-control-sm clickup-inline-input fs-8" placeholder="Nhập tên việc cần làm mới... (Enter lưu, Esc hủy)" onkeydown="handleInlineTaskKey(event, 'todo')" />
                                                </div>
                                            </td>
                                            <c:choose>
                                                <c:when test="${project.teamProject}">
                                                    <td>
                                                        <select id="inline-task-assignee-todo" class="form-select form-select-sm py-0 fs-8" style="max-width: 130px;">
                                                            <option value="0">Chưa gán</option>
                                                            <c:forEach items="${userList}" var="u">
                                                                <option value="${u.id}"><c:out value="${u.fullName}" /></option>
                                                            </c:forEach>
                                                        </select>
                                                    </td>
                                                </c:when>
                                                <c:otherwise>
                                                    <input type="hidden" id="inline-task-assignee-todo" value="${sessionScope.currentUser.id}">
                                                </c:otherwise>
                                            </c:choose>
                                            <td>
                                                <select id="inline-task-priority-todo" class="form-select form-select-sm py-0 fs-8" style="max-width: 110px;">
                                                    <option value="MEDIUM">Normal</option>
                                                    <option value="HIGH">Urgent</option>
                                                    <option value="LOW">Low</option>
                                                </select>
                                            </td>
                                            <td>
                                                <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle rounded-pill px-2 py-0-5 fs-9">TO DO</span>
                                            </td>
                                            <td style="text-align: right;">
                                                <div class="d-inline-flex align-items-center gap-1">
                                                    <input type="date" id="inline-task-due-todo" class="form-control form-control-sm py-0 fs-9" style="max-width: 110px;" />
                                                    <button type="button" class="btn btn-sm btn-primary py-0 px-2 fs-8" onclick="submitInlineCreateTask('todo')" title="Lưu việc"><i class="bi bi-check-lg"></i></button>
                                                    <button type="button" class="btn btn-sm btn-light border py-0 px-2 fs-8" onclick="cancelInlineCreateTask('todo')" title="Hủy"><i class="bi bi-x-lg"></i></button>
                                                </div>
                                            </td>
                                        </tr>
                                        <c:forEach items="${todoTasks}" var="task">
                                            <tr class="clickup-task-row group-todo-row" data-task-id="${task.id}" data-assignee-id="${task.assigneeId}" data-task-status="${task.status}" data-task-title="<c:out value='${task.title}' />" onclick="openClickUpTask(${task.id})">
                                                <td>
                                                    <div class="d-flex align-items-center gap-2 ps-2">
                                                        <c:choose>
                                                            <c:when test="${not empty taskSubTasksMap[task.id]}">
                                                                <span class="subtask-caret ${subtaskMode == 'expanded' ? 'is-expanded' : ''}" id="caret-${task.id}" onclick="event.stopPropagation(); toggleSubtasks(${task.id}, event);" title="Thu gọn / Mở rộng việc con">
                                                                    <i class="bi bi-chevron-${subtaskMode == 'expanded' ? 'down' : 'right'}"></i>
                                                                </span>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <span style="width: 18px; display: inline-block;"></span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                        <span class="clickup-status-dot dot-todo" id="status-dot-${task.id}" onclick="event.stopPropagation(); openStatusDropdown(event, ${task.id}, false, '${task.status}');" title="Trạng thái: ${task.status} (Bấm để đổi)"></span>
                                                        <span class="fw-semibold text-dark text-truncate" id="task-title-text-${task.id}" style="max-width: 300px;">${task.title}</span>
                                                        <c:if test="${not empty taskSubTasksMap[task.id]}">
                                                            <span class="badge bg-light text-secondary border rounded-pill fs-9" id="subtask-count-badge-${task.id}" title="${taskSubTasksMap[task.id].size()} việc con">
                                                                <i class="bi bi-link-45deg"></i> <span class="badge-num">${taskSubTasksMap[task.id].size()}</span>
                                                            </span>
                                                        </c:if>
                                                        <button type="button" class="task-hover-add-subtask-btn" onclick="event.stopPropagation(); showInlineCreateSubtask(${task.id}, event);" title="Thêm việc con">
                                                            <i class="bi bi-plus"></i>
                                                        </button>
                                                    </div>
                                                </td>
                                                <c:if test="${project.teamProject}">
                                                    <td>
                                                        <div class="d-flex align-items-center gap-1-5">
                                                            <span class="avatar-circle-sm bg-secondary text-white rounded-circle d-flex align-items-center justify-content-center" style="width: 22px; height: 22px; font-size: 0.65rem;" title="${task.assigneeName}">
                                                                ${task.assigneeName.substring(0, 1).toUpperCase()}
                                                            </span>
                                                            <span class="fs-9 text-muted text-truncate" style="max-width: 90px;">${task.assigneeName}</span>
                                                        </div>
                                                    </td>
                                                </c:if>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${task.priority == 'HIGH'}">
                                                            <span class="clickup-priority-flag flag-urgent"><i class="bi bi-flag-fill"></i> Urgent</span>
                                                        </c:when>
                                                        <c:when test="${task.priority == 'MEDIUM'}">
                                                            <span class="clickup-priority-flag flag-normal"><i class="bi bi-flag-fill"></i> Normal</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="clickup-priority-flag flag-low"><i class="bi bi-flag-fill"></i> Low</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${not empty task.labelList}">
                                                            <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2 py-0-5 fs-9">
                                                                ${task.getLabelDisplayName(task.labelList[0])}
                                                            </span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5 fs-9">General</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td style="text-align: right;">
                                                    <span class="fs-9 text-muted">${not empty task.dueDate ? task.dueDate : '—'}</span>
                                                </td>
                                            </tr>
                                            <!-- Subtasks -->
                                            <c:forEach items="${taskSubTasksMap[task.id]}" var="st">
                                                <c:set var="isStDone" value="${st.status == 'APPROVED' || st.status == 'DONE'}" />
                                                <tr class="clickup-subtask-row group-todo-row ${subtaskMode == 'expanded' ? '' : 'd-none'}" data-parent-id="${task.id}" data-subtask-id="${st.id}" data-assignee-id="${st.assigneeId}" data-subtask-title="<c:out value='${st.title}' />" onclick="event.stopPropagation(); openClickUpTask(${task.id}, ${st.id});">
                                                    <td>
                                                        <div class="d-flex align-items-center gap-2 ps-4" style="padding-left: 36px !important;">
                                                            <span class="clickup-status-dot dot-${isStDone ? 'done' : 'todo'}" id="subtask-status-dot-${st.id}" onclick="event.stopPropagation(); openStatusDropdown(event, ${st.id}, true, '${st.status}', ${task.id});" title="Việc con: ${st.status} (Bấm để đổi)">
                                                                <c:if test="${isStDone}">
                                                                    <i class="bi bi-check text-white"></i>
                                                                </c:if>
                                                            </span>
                                                            <span class="text-dark text-truncate fs-8 ${isStDone ? 'text-decoration-line-through text-muted' : ''}" id="subtask-title-text-${st.id}" style="max-width: 290px;">${st.title}</span>
                                                        </div>
                                                    </td>
                                                    <c:if test="${project.teamProject}">
                                                        <td>
                                                            <span class="fs-9 text-muted text-truncate" style="max-width: 90px;">${st.assigneeName}</span>
                                                        </td>
                                                    </c:if>
                                                    <td>
                                                        <span class="fs-9 text-muted">Subtask</span>
                                                    </td>
                                                    <td>
                                                        <span class="badge ${isStDone ? 'bg-success-subtle text-success border border-success-subtle' : 'bg-light text-secondary border'} rounded-pill px-1-5 py-0 fs-9" id="subtask-badge-${st.id}">${st.status}</span>
                                                    </td>
                                                    <td style="text-align: right;">
                                                        <span class="fs-9 text-muted">—</span>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </c:forEach>
                                        <tr class="group-todo-row">
                                            <td colspan="${project.soloProject ? 4 : 5}" class="py-1">
                                                <a href="javascript:void(0)" onclick="showInlineCreateTask('todo')" class="d-inline-flex align-items-center gap-1 text-muted text-decoration-none fs-8 ps-3 py-1 hover-text-dark">
                                                    <i class="bi bi-plus-lg"></i> Thêm công việc
                                                </a>
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        <!-- ClickUp Floating Status Popover Menu -->
                        <div id="clickupStatusPopover" class="clickup-status-dropdown-menu shadow-lg" style="position: fixed; display: none; z-index: 999999;">
                            <div class="p-2">
                                <div class="px-2 py-1 text-muted fs-9 fw-bold text-uppercase border-bottom mb-2" id="clickupPopoverHeader">Đổi trạng thái</div>
                                <div id="clickupPopoverOptions"></div>
                            </div>
                        </div>

                        <!-- B. KANBAN BOARD VIEW -->
                        <div id="task-subview-board" class="${taskView == 'board' ? '' : 'd-none'}">
                            <div class="d-flex align-items-center justify-content-between mb-3 px-2 fs-9 text-muted">
                                <span>Hiển thị tất cả <strong>${todoTasks.size() + inProgressTasks.size() + doneTasks.size()}</strong> công việc</span>
                                <span><i class="bi bi-cursor me-1"></i> Bấm thẻ để xem chi tiết &bull; Kéo thả để đổi trạng thái</span>
                            </div>
                            <div class="kanban-wrapper">
                            <div class="row g-4 kanban-board">


                <!-- ==========================================
                     CỘT 1: CẦN LÀM (TO DO)
                     ========================================== -->
                <div class="col-12 col-md-6 col-lg-4">
                    <div class="kanban-column kanban-col-todo rounded-4 shadow-sm h-100 d-flex flex-column overflow-hidden">

                        <!-- Header Cột 1 phong cách Ảnh 1: Strip + Pill + Nút + -->
                        <div class="kanban-header-strip d-flex align-items-center justify-content-between">
                            <div class="kanban-header-pill pill-todo">
                                <i class="bi bi-circle fs-9"></i>
                                <span>Cần làm</span>
                                <span class="pill-count">${todoTasks.size()}</span>
                            </div>
                            <button type="button" class="btn-column-add" data-bs-toggle="modal" data-bs-target="#addTaskModal"
                                title="Thêm công việc vào Cần làm">
                                <i class="bi bi-plus-lg"></i>
                            </button>
                        </div>

                        <!-- Khu vực chứa các thẻ Task (Drop Zone) -->
                        <div class="kanban-task-area kanban-task-list d-flex flex-column gap-2-5 flex-grow-1"
                            id="column-TODO" data-status="TODO">

                            <c:forEach items="${todoTasks}" var="task">
                                <div class="card kanban-card p-3 ${task.isOverdue() ? 'border-danger border-2' : ''}"
                                    id="task-${task.id}" draggable="false" data-task-id="${task.id}"
                                    data-task-title="<c:out value='${task.title}' />"
                                    data-task-priority="${task.priority}"
                                    data-task-assignee="<c:out value='${task.assigneeName}' />"
                                    data-assignee-id="${task.assigneeId}"
                                    data-task-status="${task.status}"
                                    data-subtask-count="${not empty taskSubTasksMap[task.id] ? taskSubTasksMap[task.id].size() : 0}"
                                    data-progress="${not empty taskProgressMap[task.id] ? taskProgressMap[task.id] : 0}"
                                    data-task-labels="${task.labels}"
                                    data-requires-gate="${project.teamProject && task.requiresGate ? 'true' : 'false'}" onclick="openClickUpTask(${task.id})" style="cursor: pointer;">

                                    <!-- 1. Header thẻ: Dải nhãn tối giản + Priority Dot tinh tế (Ảnh 1 & 2) -->
                                    <div class="d-flex align-items-center justify-content-between gap-2 mb-2">
                                        <div class="d-flex flex-wrap align-items-center gap-1">
                                            <c:if test="${project.teamProject}">
                                                <c:choose>
                                                    <c:when test="${task.requiresGate}">
                                                        <span class="badge bg-purple-subtle text-purple border border-purple-subtle rounded-pill px-1.5 py-0.5 fs-10 fw-semibold" title="Bắt buộc qua Quality Gate">
                                                            <i class="bi bi-shield-check"></i> Gate
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-warning-subtle text-dark border border-warning-subtle rounded-pill px-1.5 py-0.5 fs-10 fw-semibold" title="Fast-track nhanh">
                                                            <i class="bi bi-lightning-charge-fill text-warning"></i> Fast
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </c:if>
                                            <c:forEach items="${task.labelList}" var="lbl">
                                                <span class="badge ${task.getLabelBadgeClass(lbl)} rounded-pill px-2 py-0-5 fs-9 fw-medium">
                                                    ${task.getLabelDisplayName(lbl)}
                                                </span>
                                            </c:forEach>
                                        </div>

                                        <div class="d-flex align-items-center gap-1-5">
                                            <c:choose>
                                                <c:when test="${task.priority == 'HIGH'}">
                                                    <span class="priority-dot priority-dot-high" title="Ưu tiên: Cao (High)"></span>
                                                </c:when>
                                                <c:when test="${task.priority == 'MEDIUM'}">
                                                    <span class="priority-dot priority-dot-medium" title="Ưu tiên: Trung bình (Medium)"></span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="priority-dot priority-dot-low" title="Ưu tiên: Thấp (Low)"></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>

                                    <!-- 2. Tiêu đề công việc to, đậm, rõ nét -->
                                    <h6 class="fw-bold text-dark mb-2 fs-7 lh-sm text-truncate-2">${task.title}</h6>

                                    <!-- 2.5. Thanh tiến độ mảnh mai (Linear style) -->
                                    <c:if test="${not empty taskSubTasksMap[task.id]}">
                                        <div class="task-progress-slim-container mb-2" title="Tiến độ việc con: ${taskProgressMap[task.id]}%">
                                            <div class="task-progress-slim ${taskProgressMap[task.id] == 100 ? 'is-complete' : ''}">
                                                <div class="task-progress-bar" style="width: ${taskProgressMap[task.id]}%;"></div>
                                            </div>
                                        </div>
                                    </c:if>

                                    <!-- 3. Footer phẳng phong cách Ảnh 2: Stacked Avatars + Flat Metadata icons -->
                                    <div class="d-flex align-items-center justify-content-between pt-2 border-top fs-9 text-secondary mt-1">
                                        <!-- Dải Avatar xếp lớp -->
                                        <c:if test="${project.teamProject}">
                                            <div class="avatar-group" title="Task Lead: ${task.assigneeName}">
                                                <div class="avatar-circle-sm bg-primary text-white rounded-circle d-flex align-items-center justify-content-center flex-shrink-0">
                                                    ${task.assigneeName.substring(0, 1).toUpperCase()}
                                                </div>
                                            </div>
                                        </c:if>

                                        <!-- Metadata phẳng không dùng badge -->
                                        <div class="d-flex align-items-center gap-2-5">
                                            <!-- Checklist việc con -->
                                            <c:if test="${not empty taskSubTasksMap[task.id]}">
                                                <span class="kanban-meta-item" title="${taskSubTasksMap[task.id].size()} việc con">
                                                    <i class="bi bi-check2-square text-secondary"></i>
                                                    <span>${taskSubTasksMap[task.id].size()}</span>
                                                </span>
                                            </c:if>

                                            <!-- Tài liệu đính kèm -->
                                            <c:if test="${not empty taskDocsMap[task.id]}">
                                                <span class="kanban-meta-item" title="${taskDocsMap[task.id].size()} tài liệu">
                                                    <i class="bi bi-paperclip text-secondary"></i>
                                                    <span>${taskDocsMap[task.id].size()}</span>
                                                </span>
                                            </c:if>

                                            <!-- Hạn chót -->
                                            <c:if test="${not empty task.dueDate}">
                                                <span class="kanban-meta-item ${task.isOverdue() ? 'text-danger fw-bold' : ''}" title="Hạn: ${task.dueDate}">
                                                    <i class="bi bi-clock ${task.isOverdue() ? 'text-danger' : 'text-secondary'}"></i>
                                                    <span>${task.dueDate}</span>
                                                </span>
                                            </c:if>
                                        </div>
                                    </div>

                                </div>
                            </c:forEach>

                            <c:if test="${empty todoTasks}">
                                <div class="empty-state">
                                    <i class="bi bi-list-task empty-state-icon"></i>
                                    <p class="empty-state-title">Chưa có công việc</p>
                                    <p class="empty-state-hint">Bấm "+ Thêm công việc" để bắt đầu lập kế hoạch</p>
                                </div>
                            </c:if>

                            <!-- Nút tạo task nhanh phong cách nét đứt (Ảnh 2) -->
                            <button type="button" class="add-task-dashed-card mt-1" data-bs-toggle="modal" data-bs-target="#addTaskModal">
                                <i class="bi bi-plus-lg"></i>
                                <span>Tạo công việc mới</span>
                            </button>

                        </div>
                    </div>
                </div>

                <!-- ==========================================
                     CỘT 2: ĐANG LÀM (IN PROGRESS)
                     ========================================== -->
                <div class="col-12 col-md-6 col-lg-4">
                    <div class="kanban-column kanban-col-in-progress rounded-4 shadow-sm h-100 d-flex flex-column overflow-hidden">

                        <!-- Header Cột 2 phong cách Ảnh 1: Strip + Pill + Nút + -->
                        <div class="kanban-header-strip d-flex align-items-center justify-content-between">
                            <div class="kanban-header-pill pill-in-progress">
                                <i class="bi bi-arrow-repeat fs-8"></i>
                                <span>Đang làm</span>
                                <span class="pill-count">${inProgressTasks.size()}</span>
                            </div>
                            <button type="button" class="btn-column-add" data-bs-toggle="modal" data-bs-target="#addTaskModal"
                                title="Thêm công việc vào Đang làm">
                                <i class="bi bi-plus-lg"></i>
                            </button>
                        </div>

                        <div class="kanban-task-area kanban-task-list d-flex flex-column gap-2-5 flex-grow-1"
                            id="column-IN_PROGRESS" data-status="IN_PROGRESS">

                            <c:forEach items="${inProgressTasks}" var="task">
                                <div class="card kanban-card kanban-card-inprogress p-3 ${task.isOverdue() ? 'border-danger border-2' : ''} ${task.status == 'SUBMITTED' ? 'kanban-card-submitted' : ''}"
                                    id="task-${task.id}" draggable="false" data-task-id="${task.id}"
                                    data-task-title="<c:out value='${task.title}' />"
                                    data-task-priority="${task.priority}"
                                    data-task-assignee="<c:out value='${task.assigneeName}' />"
                                    data-assignee-id="${task.assigneeId}"
                                    data-task-status="${task.status}"
                                    data-subtask-count="${not empty taskSubTasksMap[task.id] ? taskSubTasksMap[task.id].size() : 0}"
                                    data-progress="${not empty taskProgressMap[task.id] ? taskProgressMap[task.id] : 0}"
                                    data-task-labels="${task.labels}"
                                    data-requires-gate="${project.teamProject && task.requiresGate ? 'true' : 'false'}" onclick="openClickUpTask(${task.id})" style="cursor: pointer;">

                                    <!-- 1. Header thẻ: Dải nhãn tối giản + Priority Dot tinh tế (Ảnh 1 & 2) -->
                                    <div class="d-flex align-items-center justify-content-between gap-2 mb-2">
                                        <div class="d-flex flex-wrap align-items-center gap-1">
                                            <c:if test="${project.teamProject}">
                                                <c:choose>
                                                    <c:when test="${task.requiresGate}">
                                                        <span class="badge bg-purple-subtle text-purple border border-purple-subtle rounded-pill px-1.5 py-0.5 fs-10 fw-semibold" title="Bắt buộc qua Quality Gate">
                                                            <i class="bi bi-shield-check"></i> Gate
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-warning-subtle text-dark border border-warning-subtle rounded-pill px-1.5 py-0.5 fs-10 fw-semibold" title="Fast-track nhanh">
                                                            <i class="bi bi-lightning-charge-fill text-warning"></i> Fast
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </c:if>
                                            <c:forEach items="${task.labelList}" var="lbl">
                                                <span class="badge ${task.getLabelBadgeClass(lbl)} rounded-pill px-2 py-0-5 fs-9 fw-medium">
                                                    ${task.getLabelDisplayName(lbl)}
                                                </span>
                                            </c:forEach>
                                        </div>

                                        <div class="d-flex align-items-center gap-1-5">
                                            <c:choose>
                                                <c:when test="${task.priority == 'HIGH'}">
                                                    <span class="priority-dot priority-dot-high" title="Ưu tiên: Cao (High)"></span>
                                                </c:when>
                                                <c:when test="${task.priority == 'MEDIUM'}">
                                                    <span class="priority-dot priority-dot-medium" title="Ưu tiên: Trung bình (Medium)"></span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="priority-dot priority-dot-low" title="Ưu tiên: Thấp (Low)"></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>

                                    <!-- Huy hiệu Chờ PM Duyệt (SUBMITTED) -->
                                    <c:if test="${task.status == 'SUBMITTED'}">
                                        <div class="mb-2">
                                            <span class="badge bg-purple text-white rounded-pill px-2 py-1 fs-9 fw-semibold shadow-xs d-inline-flex align-items-center gap-1" title="Nhiệm vụ đã nộp báo cáo kết quả, chờ PM duyệt">
                                                <i class="bi bi-send-check"></i> Chờ PM Duyệt
                                            </span>
                                        </div>
                                    </c:if>

                                    <!-- 2. Tiêu đề công việc to, đậm, rõ nét -->
                                    <h6 class="fw-bold text-dark mb-2 fs-7 lh-sm text-truncate-2">${task.title}</h6>

                                    <!-- 2.5. Thanh tiến độ mảnh mai (Linear style) -->
                                    <c:if test="${not empty taskSubTasksMap[task.id]}">
                                        <div class="task-progress-slim-container mb-2" title="Tiến độ việc con: ${taskProgressMap[task.id]}%">
                                            <div class="task-progress-slim ${taskProgressMap[task.id] == 100 ? 'is-complete' : ''}">
                                                <div class="task-progress-bar" style="width: ${taskProgressMap[task.id]}%;"></div>
                                            </div>
                                        </div>
                                    </c:if>

                                    <!-- 3. Footer phẳng phong cách Ảnh 2: Stacked Avatars + Flat Metadata icons -->
                                    <div class="d-flex align-items-center justify-content-between pt-2 border-top fs-9 text-secondary mt-1">
                                        <!-- Dải Avatar xếp lớp -->
                                        <c:if test="${project.teamProject}">
                                            <div class="avatar-group" title="Task Lead: ${task.assigneeName}">
                                                <div class="avatar-circle-sm bg-primary text-white rounded-circle d-flex align-items-center justify-content-center flex-shrink-0">
                                                    ${task.assigneeName.substring(0, 1).toUpperCase()}
                                                </div>
                                            </div>
                                        </c:if>

                                        <!-- Metadata phẳng không dùng badge -->
                                        <div class="d-flex align-items-center gap-2-5">
                                            <!-- Checklist việc con có % tiến độ -->
                                            <c:if test="${not empty taskSubTasksMap[task.id]}">
                                                <span class="kanban-meta-item text-primary fw-semibold"
                                                    title="${taskSubTasksMap[task.id].size()} việc con (${taskProgressMap[task.id]}%)">
                                                    <i class="bi bi-check2-square text-primary"></i>
                                                    <span>${taskProgressMap[task.id]}%</span>
                                                </span>
                                            </c:if>

                                            <!-- Tài liệu đính kèm -->
                                            <c:if test="${not empty taskDocsMap[task.id]}">
                                                <span class="kanban-meta-item" title="${taskDocsMap[task.id].size()} tài liệu">
                                                    <i class="bi bi-paperclip text-secondary"></i>
                                                    <span>${taskDocsMap[task.id].size()}</span>
                                                </span>
                                            </c:if>

                                            <!-- Báo cáo kết quả đã nộp -->
                                            <c:if test="${task.status == 'SUBMITTED'}">
                                                <span class="kanban-meta-item text-purple fw-semibold" title="Đã nộp báo cáo kết quả">
                                                    <i class="bi bi-file-earmark-check text-purple"></i>
                                                    <span>Đã nộp</span>
                                                </span>
                                            </c:if>

                                            <!-- Hạn chót -->
                                            <c:if test="${not empty task.dueDate}">
                                                <span class="kanban-meta-item ${task.isOverdue() ? 'text-danger fw-bold' : ''}" title="Hạn: ${task.dueDate}">
                                                    <i class="bi bi-clock ${task.isOverdue() ? 'text-danger' : 'text-secondary'}"></i>
                                                    <span>${task.dueDate}</span>
                                                </span>
                                            </c:if>
                                        </div>
                                    </div>

                                </div>
                            </c:forEach>

                            <c:if test="${empty inProgressTasks}">
                                <div class="empty-state">
                                    <i class="bi bi-hourglass empty-state-icon"></i>
                                    <p class="empty-state-title">Chưa có việc đang làm</p>
                                    <p class="empty-state-hint">Kéo thẻ từ "Cần làm" sang đây để bắt đầu thực hiện</p>
                                </div>
                            </c:if>

                        </div>
                    </div>
                </div>

                <!-- ==========================================
                     CỘT 3: ĐÃ XONG (DONE)
                     ========================================== -->
                <div class="col-12 col-md-6 col-lg-4">
                    <div class="kanban-column kanban-col-done rounded-4 shadow-sm h-100 d-flex flex-column overflow-hidden">

                        <!-- Header Cột 3 phong cách Ảnh 1: Strip + Pill + Nút + -->
                        <div class="kanban-header-strip d-flex align-items-center justify-content-between">
                            <div class="kanban-header-pill pill-done">
                                <i class="bi bi-check-circle-fill fs-8"></i>
                                <span>Đã xong</span>
                                <span class="pill-count">${doneTasks.size()}</span>
                            </div>
                            <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-0-5 fs-9"><i class="bi bi-lock-fill"></i> Khóa</span>
                        </div>

                        <div class="kanban-task-area kanban-task-list d-flex flex-column gap-2-5 flex-grow-1"
                            id="column-DONE" data-status="DONE">

                            <c:forEach items="${doneTasks}" var="task">
                                <div class="card kanban-card kanban-card-done kanban-card-locked p-3" id="task-${task.id}"
                                    draggable="false" data-task-id="${task.id}"
                                    data-task-title="<c:out value='${task.title}' />"
                                    data-task-priority="${task.priority}"
                                    data-task-assignee="<c:out value='${task.assigneeName}' />"
                                    data-assignee-id="${task.assigneeId}"
                                    data-task-status="${task.status}"
                                    data-task-labels="${task.labels}"
                                    data-requires-gate="${project.teamProject && task.requiresGate ? 'true' : 'false'}" onclick="openClickUpTask(${task.id})" style="cursor: pointer;">

                                    <!-- 1. Header thẻ: Dải nhãn tối giản + Priority Dot tinh tế (Ảnh 1 & 2) -->
                                    <div class="d-flex align-items-center justify-content-between gap-2 mb-2">
                                        <div class="d-flex flex-wrap align-items-center gap-1">
                                            <c:if test="${project.teamProject}">
                                                <c:choose>
                                                    <c:when test="${task.requiresGate}">
                                                        <span class="badge bg-purple-subtle text-purple border border-purple-subtle rounded-pill px-1.5 py-0.5 fs-10 fw-semibold" title="Bắt buộc qua Quality Gate">
                                                            <i class="bi bi-shield-check"></i> Gate
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-warning-subtle text-dark border border-warning-subtle rounded-pill px-1.5 py-0.5 fs-10 fw-semibold" title="Fast-track nhanh">
                                                            <i class="bi bi-lightning-charge-fill text-warning"></i> Fast
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </c:if>
                                            <c:forEach items="${task.labelList}" var="lbl">
                                                <span class="badge ${task.getLabelBadgeClass(lbl)} rounded-pill px-2 py-0-5 fs-9 fw-medium">
                                                    ${task.getLabelDisplayName(lbl)}
                                                </span>
                                            </c:forEach>
                                        </div>

                                        <div class="d-flex align-items-center gap-1-5">
                                            <c:choose>
                                                <c:when test="${task.priority == 'HIGH'}">
                                                    <span class="priority-dot priority-dot-high" title="Ưu tiên: Cao (High)"></span>
                                                </c:when>
                                                <c:when test="${task.priority == 'MEDIUM'}">
                                                    <span class="priority-dot priority-dot-medium" title="Ưu tiên: Trung bình (Medium)"></span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="priority-dot priority-dot-low" title="Ưu tiên: Thấp (Low)"></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>

                                    <!-- 2. Tiêu đề công việc đã hoàn thành -->
                                    <div class="d-flex align-items-start gap-1-5 mb-2">
                                        <i class="bi bi-check-circle-fill text-success fs-7 mt-0-5 flex-shrink-0"></i>
                                        <h6 class="fw-semibold text-secondary mb-0 fs-7 lh-sm text-decoration-line-through text-truncate-2">
                                            ${task.title}</h6>
                                    </div>

                                    <!-- 2.5. Thanh tiến độ hoàn thành 100% mảnh mai (Linear style) -->
                                    <c:if test="${not empty taskSubTasksMap[task.id]}">
                                        <div class="task-progress-slim-container mb-2" title="Hoàn thành: 100%">
                                            <div class="task-progress-slim is-complete">
                                                <div class="task-progress-bar" style="width: 100%;"></div>
                                            </div>
                                        </div>
                                    </c:if>

                                    <!-- 3. Footer phẳng phong cách Ảnh 2: Stacked Avatars + Flat Metadata icons -->
                                    <div class="d-flex align-items-center justify-content-between pt-2 border-top fs-9 text-secondary mt-1">
                                        <!-- Dải Avatar Task Lead hoàn thành -->
                                        <c:if test="${project.teamProject}">
                                            <div class="avatar-group" title="Người hoàn thành: ${task.assigneeName}">
                                                <div class="avatar-circle-sm bg-success text-white rounded-circle d-flex align-items-center justify-content-center flex-shrink-0">
                                                    ${task.assigneeName.substring(0, 1).toUpperCase()}
                                                </div>
                                            </div>
                                        </c:if>

                                        <!-- Metadata phẳng -->
                                        <div class="d-flex align-items-center gap-2-5">
                                            <span class="kanban-meta-item text-success fw-semibold">
                                                <i class="bi bi-lock-fill text-success"></i>
                                                <span>Hoàn tất</span>
                                            </span>

                                            <!-- Checklist việc con -->
                                            <c:if test="${not empty taskSubTasksMap[task.id]}">
                                                <span class="kanban-meta-item text-success" title="Tất cả việc con đã hoàn tất">
                                                    <i class="bi bi-check2-square text-success"></i>
                                                    <span>${taskSubTasksMap[task.id].size()}</span>
                                                </span>
                                            </c:if>

                                            <!-- Tài liệu đính kèm -->
                                            <c:if test="${not empty taskDocsMap[task.id]}">
                                                <span class="kanban-meta-item" title="${taskDocsMap[task.id].size()} tài liệu">
                                                    <i class="bi bi-paperclip text-secondary"></i>
                                                    <span>${taskDocsMap[task.id].size()}</span>
                                                </span>
                                            </c:if>
                                        </div>
                                    </div>

                                </div>
                            </c:forEach>

                            <c:if test="${empty doneTasks}">
                                <div class="empty-state">
                                    <i class="bi bi-check2-circle empty-state-icon"></i>
                                    <p class="empty-state-title">Chưa có việc hoàn thành</p>
                                    <p class="empty-state-hint">Kéo thẻ vào đây hoặc nghiệm thu để hoàn thành task</p>
                                </div>
                            </c:if>

                        </div><!-- /kanban-task-area -->
                    </div><!-- /kanban-column -->
                </div><!-- /col-12 col-md-6 col-lg-4 (Cột 3) -->

                </div><!-- /row kanban-board -->
                </div><!-- /kanban-wrapper -->
                </div><!-- /task-subview-board -->

            </div><!-- /clickup-view-tasks -->

            <!-- =========================================================================
                 TAB 2: PROJECT CHAT VIEW (# CHAT)
                 ========================================================================= -->
            <div id="clickup-view-chat" class="clickup-view-pane ${currentView == 'chat' ? '' : 'd-none'}">
                <div class="card border-0 shadow-2xs rounded-3 overflow-hidden d-flex flex-column" style="height: calc(100vh - 180px);">
                    <!-- Chat Channel Header -->
                    <div class="px-3 py-2-5 border-bottom bg-white d-flex align-items-center justify-content-between">
                        <div class="d-flex align-items-center gap-2">
                            <span class="avatar-circle-sm bg-primary-subtle text-primary rounded-circle d-flex align-items-center justify-content-center" style="width: 28px; height: 28px; font-weight: 700;">
                                #
                            </span>
                            <div>
                                <div class="fw-bold text-dark fs-8">kênh-thảo-luận-chung</div>
                                <div class="fs-9 text-muted">Kênh trao đổi nội bộ cho dự án "${project.name}" &bull; ${projectChatMessages.size()} tin nhắn</div>
                            </div>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <a href="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}" class="btn btn-outline-secondary btn-xs rounded-pill px-2-5 py-1 fs-9">
                                <i class="bi bi-box-arrow-up-right me-1"></i> Mở trang Chat riêng
                            </a>
                        </div>
                    </div>

                    <!-- Chat Messages Body -->
                    <div class="flex-grow-1 p-3 overflow-y-auto d-flex flex-column gap-3 bg-light-subtle" id="clickupChatMessages" style="background-color: #f8f9fc;">
                        <c:if test="${empty projectChatMessages}">
                            <div class="text-center py-5 my-auto text-muted">
                                <i class="bi bi-chat-dots fs-1 d-block mb-2 text-secondary opacity-50"></i>
                                <div class="fw-semibold fs-7">Chưa có tin nhắn nào trong kênh này</div>
                                <div class="fs-9">Hãy bắt đầu cuộc trò chuyện với nhóm của bạn ngay bên dưới!</div>
                            </div>
                        </c:if>
                        <c:forEach items="${projectChatMessages}" var="msg">
                            <c:choose>
                                <c:when test="${msg.authorId == sessionScope.currentUser.id}">
                                    <div class="chat-row-me" id="shell-msg-${msg.id}">
                                        <c:if test="${msg.authorId == sessionScope.currentUser.id || sessionScope.currentUser.role == 'ADMIN' || project.ownerId == sessionScope.currentUser.id}">
                                            <a href="${pageContext.request.contextPath}/chat?action=delete&projectId=${project.id}&messageId=${msg.id}&source=taskShell" 
                                               class="text-muted text-hover-danger fs-9 text-decoration-none opacity-50 hover-opacity-100 me-1"
                                               onclick="return confirm('Bạn có chắc chắn muốn xóa tin nhắn này không?');"
                                               title="Xóa tin nhắn">
                                                <i class="bi bi-trash3"></i>
                                            </a>
                                        </c:if>
                                        <div class="chat-bubble-me">
                                            <div class="message-body fs-8 lh-base text-white" style="word-break: break-word; white-space: pre-line;"><c:out value="${msg.content}" /></div>
                                            <div class="d-flex justify-content-end align-items-center gap-1 mt-1">
                                                <span class="chat-time-me"><i class="bi bi-clock me-1"></i>${msg.sentAt}</span>
                                            </div>
                                        </div>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <div class="chat-row-other" id="shell-msg-${msg.id}">
                                        <div class="avatar-circle bg-dark text-white rounded-circle d-flex align-items-center justify-content-center fw-bold fs-8 flex-shrink-0 shadow-2xs"
                                             style="width: 32px; height: 32px;">
                                            ${msg.authorInitial}
                                        </div>
                                        <div class="chat-bubble-other">
                                            <div class="d-flex align-items-center justify-content-between gap-3 mb-1">
                                                <span class="fw-bold text-dark fs-8">${msg.authorName}</span>
                                                <span class="chat-time-other fs-9 text-muted"><i class="bi bi-clock me-1"></i>${msg.sentAt}</span>
                                            </div>
                                            <div class="message-body fs-8 lh-base text-secondary" style="word-break: break-word; white-space: pre-line;"><c:out value="${msg.content}" /></div>
                                        </div>
                                        <c:if test="${sessionScope.currentUser.role == 'ADMIN' || project.ownerId == sessionScope.currentUser.id}">
                                            <a href="${pageContext.request.contextPath}/chat?action=delete&projectId=${project.id}&messageId=${msg.id}&source=taskShell" 
                                               class="text-muted text-hover-danger fs-9 text-decoration-none opacity-50 hover-opacity-100 ms-1"
                                               onclick="return confirm('Xóa tin nhắn này của thành viên?');"
                                               title="Xóa tin nhắn">
                                                <i class="bi bi-trash3"></i>
                                            </a>
                                        </c:if>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </div>

                    <!-- Chat Input Form -->
                    <div class="p-2-5 bg-white border-top">
                        <form method="post" action="${pageContext.request.contextPath}/chat" class="d-flex align-items-center gap-2">
                            <input type="hidden" name="action" value="sendProjectMessage">
                            <input type="hidden" name="projectId" value="${project.id}">
                            <input type="hidden" name="source" value="taskShell">
                            <div class="input-group">
                                <input type="text" name="content" class="form-control fs-8 border-end-0 rounded-start-pill ps-3" placeholder="Nhập tin nhắn... (Gõ #task-id hoặc #doc-id để liên kết)" autocomplete="off" required>
                                <button type="submit" class="btn btn-primary-custom rounded-end-pill px-3 fs-8">
                                    <i class="bi bi-send-fill me-1"></i> Gửi
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>

            <!-- =========================================================================
                 TAB 3: PROJECT DOCS VIEW (DOCS)
                 ========================================================================= -->
            <div id="clickup-view-docs" class="clickup-view-pane ${currentView == 'docs' ? '' : 'd-none'}">
                <div class="d-flex align-items-center justify-content-between mb-3 px-1">
                    <div>
                        <h6 class="fw-bold text-dark mb-0 fs-7">Tài liệu & Wiki dự án</h6>
                        <span class="fs-9 text-muted">Tổng hợp tài liệu yêu cầu, phân tích và hướng dẫn kỹ thuật</span>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}" class="btn btn-sm btn-outline-secondary rounded-pill px-3 py-1 fs-9">
                            <i class="bi bi-journal-text me-1"></i> Mở không gian Wiki đầy đủ
                        </a>
                    </div>
                </div>

                <c:if test="${empty docList}">
                    <div class="card border-0 shadow-2xs rounded-3 p-5 text-center bg-white my-3">
                        <i class="bi bi-file-earmark-text text-muted fs-1 mb-2"></i>
                        <h6 class="fw-bold text-dark fs-7">Chưa có tài liệu nào trong dự án này</h6>
                        <p class="text-muted fs-9 mb-3">Tạo tài liệu mới để chia sẻ kiến thức và gắn kèm vào các công việc liên quan.</p>
                        <div>
                            <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}" class="btn btn-sm btn-primary-custom rounded-pill px-3 py-1 fs-8">
                                <i class="bi bi-pencil-square me-1"></i> Mở trang tài liệu để tạo
                            </a>
                        </div>
                    </div>
                </c:if>

                <c:if test="${not empty docList}">
                    <div class="row g-3">
                        <c:forEach items="${docList}" var="doc">
                            <div class="col-12 col-md-6 col-xl-4">
                                <div class="card border-0 shadow-2xs rounded-3 p-3 h-100 bg-white hover-shadow-sm transition-all position-relative">
                                    <div class="d-flex align-items-start justify-content-between gap-2 mb-2">
                                        <div class="d-flex align-items-center gap-2">
                                            <span class="avatar-circle-sm bg-info-subtle text-info rounded-2 d-flex align-items-center justify-content-center" style="width: 28px; height: 28px;">
                                                <i class="bi bi-file-earmark-richtext fs-7"></i>
                                            </span>
                                            <span class="badge bg-light text-muted border rounded-pill fs-9">#doc-${doc.id}</span>
                                        </div>
                                        <span class="fs-9 text-muted"><i class="bi bi-clock me-1"></i>${doc.updatedAt}</span>
                                    </div>
                                    <h6 class="fw-bold text-dark fs-7 mb-1 text-truncate-2">
                                        <a href="${pageContext.request.contextPath}/doc?action=view&projectId=${project.id}&docId=${doc.id}" class="text-dark text-decoration-none hover-primary">
                                            ${doc.title}
                                        </a>
                                    </h6>
                                    <p class="text-muted fs-8 text-truncate-3 mb-3 flex-grow-1" style="min-height: 48px;">
                                        <c:choose>
                                            <c:when test="${not empty doc.content}">
                                                ${doc.content}
                                            </c:when>
                                            <c:otherwise>
                                                <span class="fst-italic text-muted opacity-75">(Chưa có nội dung chi tiết)</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </p>
                                    <div class="d-flex align-items-center justify-content-between pt-2 border-top fs-9">
                                        <span class="text-muted"><i class="bi bi-person me-1"></i>${doc.authorName}</span>
                                        <a href="${pageContext.request.contextPath}/doc?action=view&projectId=${project.id}&docId=${doc.id}" class="text-primary fw-semibold text-decoration-none">
                                            Đọc bài <i class="bi bi-arrow-right"></i>
                                        </a>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:if>
            </div>

            <!-- =========================================================================
                 TAB 4: METRICS & WORKLOAD VIEW (THỐNG KÊ)
                 ========================================================================= -->
            <div id="clickup-view-metrics" class="clickup-view-pane ${currentView == 'metrics' ? '' : 'd-none'}">
                <!-- Dedicated Workload Top Header Bar (ClickUp 3.0 Clean Island Header) -->
                <div class="workload-header-bar d-flex align-items-center justify-content-between flex-wrap gap-2 py-2 px-3 mb-3">
                    <div class="d-flex align-items-center gap-2">
                        <button type="button" class="btn btn-sm btn-light border rounded-2 p-1 btn-expand-sidebar ${cookie.sidebar_collapsed.value == 'true' ? '' : 'd-none'}" onclick="toggleClickUpSidebar()" title="Mở rộng thanh bên">
                            <i class="bi bi-chevron-bar-right fs-8"></i>
                        </button>
                        <div class="d-flex align-items-center gap-1.5 text-muted fs-8">
                            <span>Team Space</span>
                            <i class="bi bi-chevron-right fs-9 text-muted"></i>
                        </div>
                        <h6 class="fw-bold text-dark mb-0 fs-7">${project.name}</h6>
                        <span class="badge ${project.projectTypeBadgeClass} rounded-pill px-2 py-0-5 fs-9 d-inline-flex align-items-center gap-1 shadow-2xs">
                            <i class="bi ${project.projectTypeIcon}"></i> ${project.projectTypeLabel}
                        </span>
                        <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2 py-0-5 fs-9 d-inline-flex align-items-center gap-1">
                            <i class="bi bi-pie-chart-fill"></i> Workload & Hiệu suất
                        </span>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <button type="button" class="btn btn-sm btn-light border text-dark rounded-pill px-2-5 py-1 fs-8 d-inline-flex align-items-center gap-1 shadow-2xs" onclick="switchClickUpTab('tasks')" title="Chuyển sang bảng công việc Tasks">
                            <i class="bi bi-arrow-left text-primary"></i> Quay lại Tasks
                        </button>
                        <a href="${pageContext.request.contextPath}/task?action=exportCsv&projectId=${project.id}" 
                           class="btn btn-sm btn-light border text-success fw-semibold d-inline-flex align-items-center gap-1 px-2.5 py-1 rounded-pill shadow-2xs text-nowrap"
                           title="Tải toàn bộ danh sách công việc của dự án về máy tính (.csv chuẩn UTF-8 BOM)">
                            <i class="bi bi-file-earmark-excel-fill text-success fs-8"></i>
                            <span class="fs-8">Xuất Excel</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/project?action=report&projectId=${project.id}" 
                           class="btn btn-sm btn-primary-custom rounded-pill px-3 py-1 fs-8 d-inline-flex align-items-center gap-1 shadow-2xs"
                           title="Xem trang báo cáo thống kê chuyên sâu & biểu đồ tiến độ">
                            <i class="bi bi-file-earmark-bar-graph"></i> Báo cáo Toàn diện
                        </a>
                    </div>
                </div>

                <!-- 4 Stat Summary Cards & Donut Chart Split View (UI/UX Pro Max) -->
                <c:set var="mTodoCount" value="${not empty todoTasks ? todoTasks.size() : 0}" />
                <c:set var="mInProgCount" value="${not empty inProgressTasks ? inProgressTasks.size() : 0}" />
                <c:set var="mDoneCount" value="${not empty doneTasks ? doneTasks.size() : 0}" />
                <c:set var="mTotalCount" value="${mTodoCount + mInProgCount + mDoneCount}" />

                <c:set var="mPctDone" value="${mTotalCount > 0 ? Math.round((mDoneCount * 100.0) / mTotalCount) : 0}" />
                <c:set var="mPctInProg" value="${mTotalCount > 0 ? Math.round((mInProgCount * 100.0) / mTotalCount) : 0}" />
                <c:set var="mPctTodo" value="${mTotalCount > 0 ? (100 - mPctDone - mPctInProg) : 0}" />
                <c:if test="${mPctTodo < 0}"><c:set var="mPctTodo" value="0" /></c:if>

                <c:set var="mOverdueCount" value="0" />
                <c:forEach items="${userWorkloadList}" var="uw">
                    <c:set var="mOverdueCount" value="${mOverdueCount + uw.overdueTasks}" />
                </c:forEach>

                <c:set var="cCirc" value="364.4" />
                <c:set var="cDashDone" value="${mTotalCount > 0 ? (mDoneCount * 364.4 / mTotalCount) : 0}" />
                <c:set var="cDashInProg" value="${mTotalCount > 0 ? (mInProgCount * 364.4 / mTotalCount) : 0}" />
                <c:set var="cDashTodo" value="${mTotalCount > 0 ? (mTodoCount * 364.4 / mTotalCount) : 0}" />
                <c:set var="cOffDone" value="0" />
                <c:set var="cOffInProg" value="${0 - cDashDone}" />
                <c:set var="cOffTodo" value="${0 - (cDashDone + cDashInProg)}" />

                <div class="row g-3 mb-3 align-items-stretch" id="metricsOverviewSplitView">
                    <!-- CỘT TRÁI (7/12): 4 Stat Cards dạng 2x2 Grid -->
                    <div class="col-12 col-xl-7 col-lg-7">
                        <div class="row g-3 h-100">
                            <!-- Card 1: Tổng công việc -->
                            <div class="col-12 col-sm-6">
                                <div class="metrics-stat-card card-total h-100 d-flex flex-column justify-content-between">
                                    <div>
                                        <div class="metrics-card-header">
                                            <span class="metrics-stat-title">Tổng công việc</span>
                                            <div class="metrics-icon-badge badge-total" title="Tổng số công việc dự án">
                                                <i class="bi bi-stack"></i>
                                            </div>
                                        </div>
                                        <div class="metrics-stat-body">
                                            <div class="metrics-stat-number text-dark" id="metric-total-count">${mTotalCount}</div>
                                            <span class="metrics-pct-pill pill-total" id="metric-total-pill">
                                                <i class="bi bi-pie-chart-fill me-1"></i>100%
                                            </span>
                                        </div>
                                    </div>
                                    <div>
                                        <div class="metrics-progress-track d-flex" id="metric-total-track" title="Tiến độ tổng thể: ${mDoneCount} Hoàn thành (${mPctDone}%), ${mInProgCount} Đang làm (${mPctInProg}%), ${mTodoCount} Cần làm (${mPctTodo}%)">
                                            <c:choose>
                                                <c:when test="${mTotalCount > 0}">
                                                    <div class="metrics-progress-segment bg-success" id="metric-total-seg-done" style="width: ${mPctDone}%;" title="Đã xong: ${mPctDone}%"></div>
                                                    <div class="metrics-progress-segment bg-primary" id="metric-total-seg-inprog" style="width: ${mPctInProg}%;" title="Đang làm: ${mPctInProg}%"></div>
                                                    <div class="metrics-progress-segment bg-amber" id="metric-total-seg-todo" style="width: ${mPctTodo}%;" title="Cần làm: ${mPctTodo}%"></div>
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="metrics-progress-fill bg-light" style="width: 100%;"></div>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                        <div class="metrics-stat-footer">
                                            <span class="text-muted" id="metric-total-footer-label"><i class="bi bi-people-fill me-1 text-secondary"></i>${memberCount} thành viên</span>
                                            <span class="metrics-stat-footer-sub text-dark" id="metric-total-footer-val">${mTotalCount} việc</span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Card 2: Cần làm (To Do) -->
                            <div class="col-12 col-sm-6">
                                <div class="metrics-stat-card card-todo h-100 d-flex flex-column justify-content-between">
                                    <div>
                                        <div class="metrics-card-header">
                                            <span class="metrics-stat-title text-amber">Cần làm</span>
                                            <div class="metrics-icon-badge badge-todo" title="Công việc chưa bắt đầu / đang chờ xử lý">
                                                <i class="bi bi-hourglass-split"></i>
                                            </div>
                                        </div>
                                        <div class="metrics-stat-body">
                                            <div class="metrics-stat-number text-amber" id="metric-todo-count">${mTodoCount}</div>
                                            <span class="metrics-pct-pill pill-todo" id="metric-todo-pill">
                                                <i class="bi bi-hourglass-split me-1"></i>${mPctTodo}%
                                            </span>
                                        </div>
                                    </div>
                                    <div>
                                        <div class="metrics-progress-track" title="${mTodoCount} / ${mTotalCount} công việc (${mPctTodo}%)">
                                            <div class="metrics-progress-fill bg-amber" id="metric-todo-bar" style="width: ${mPctTodo}%;"></div>
                                        </div>
                                        <div class="metrics-stat-footer">
                                            <span class="text-muted">Chưa bắt đầu</span>
                                            <span class="metrics-stat-footer-sub text-amber" id="metric-todo-footer-val">${mTodoCount}/${mTotalCount} việc</span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Card 3: Đang làm (In Progress) -->
                            <div class="col-12 col-sm-6">
                                <div class="metrics-stat-card card-inprog h-100 d-flex flex-column justify-content-between">
                                    <div>
                                        <div class="metrics-card-header">
                                            <span class="metrics-stat-title text-primary">Đang làm</span>
                                            <div class="metrics-icon-badge badge-inprog" title="Công việc đang xử lý tích cực">
                                                <i class="bi bi-lightning-charge-fill"></i>
                                            </div>
                                        </div>
                                        <div class="metrics-stat-body">
                                            <div class="metrics-stat-number text-primary" id="metric-inprog-count">${mInProgCount}</div>
                                            <span class="metrics-pct-pill pill-inprog" id="metric-inprog-pill">
                                                <i class="bi bi-lightning-charge-fill me-1"></i>${mPctInProg}%
                                            </span>
                                        </div>
                                    </div>
                                    <div>
                                        <div class="metrics-progress-track" title="${mInProgCount} / ${mTotalCount} công việc (${mPctInProg}%)">
                                            <div class="metrics-progress-fill bg-primary" id="metric-inprog-bar" style="width: ${mPctInProg}%;"></div>
                                        </div>
                                        <div class="metrics-stat-footer">
                                            <span class="text-muted">Đang xử lý</span>
                                            <span class="metrics-stat-footer-sub text-primary" id="metric-inprog-footer-val">${mInProgCount}/${mTotalCount} việc</span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Card 4: Hoàn thành (Done) -->
                            <div class="col-12 col-sm-6">
                                <div class="metrics-stat-card card-done h-100 d-flex flex-column justify-content-between">
                                    <div>
                                        <div class="metrics-card-header">
                                            <span class="metrics-stat-title text-success">Hoàn thành</span>
                                            <div class="metrics-icon-badge badge-done" title="Công việc đã hoàn tất thành công">
                                                <i class="bi bi-check-circle-fill"></i>
                                            </div>
                                        </div>
                                        <div class="metrics-stat-body">
                                            <div class="metrics-stat-number text-success" id="metric-done-count">${mDoneCount}</div>
                                            <span class="metrics-pct-pill pill-done" id="metric-done-pill">
                                                <i class="bi bi-check2 me-1"></i>${mPctDone}%
                                            </span>
                                        </div>
                                    </div>
                                    <div>
                                        <div class="metrics-progress-track" title="${mDoneCount} / ${mTotalCount} công việc (${mPctDone}%)">
                                            <div class="metrics-progress-fill bg-success" id="metric-done-bar" style="width: ${mPctDone}%;"></div>
                                        </div>
                                        <div class="metrics-stat-footer">
                                            <span class="text-muted">Đã về đích</span>
                                            <span class="metrics-stat-footer-sub text-success" id="metric-done-footer-val">${mDoneCount}/${mTotalCount} việc</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- CỘT PHẢI (5/12): Biểu đồ Donut Chart phân bổ trạng thái công việc -->
                    <div class="col-12 col-xl-5 col-lg-5">
                        <div class="card border-0 shadow-2xs rounded-3 bg-white h-100 p-3-5 d-flex flex-column justify-content-between">
                            <div class="d-flex align-items-center justify-content-between mb-2">
                                <div>
                                    <h6 class="fw-bold text-dark mb-0 fs-8 d-flex align-items-center gap-1.5">
                                        <i class="bi bi-pie-chart-fill text-primary"></i> Phân bổ trạng thái công việc
                                    </h6>
                                    <span class="fs-9 text-muted" id="donut-chart-subtitle">Toàn bộ dự án (${mTotalCount} việc)</span>
                                </div>
                                <span class="badge bg-light text-secondary border rounded-pill fs-9" id="donut-filter-badge">Tất cả</span>
                            </div>

                            <!-- Donut SVG & Central Metric -->
                            <div class="d-flex align-items-center justify-content-center py-2 position-relative">
                                <div class="donut-chart-container position-relative" style="width: 160px; height: 160px;">
                                    <svg viewBox="0 0 160 160" class="donut-svg" style="width: 100%; height: 100%; transform: rotate(-90deg);">
                                        <!-- Background Circle Track -->
                                        <circle cx="80" cy="80" r="58" fill="none" stroke="#f1f5f9" stroke-width="15" />
                                        
                                        <!-- Done Segment (Emerald Green) -->
                                        <circle id="donut-segment-done" cx="80" cy="80" r="58" fill="none" 
                                                stroke="#10b981" stroke-width="15"
                                                stroke-dasharray="${cDashDone} 364.4" stroke-dashoffset="${cOffDone}" class="donut-segment"
                                                title="Hoàn thành: ${mDoneCount} (${mPctDone}%)" />
                                        
                                        <!-- In-Progress Segment (Sky Blue) -->
                                        <circle id="donut-segment-inprog" cx="80" cy="80" r="58" fill="none" 
                                                stroke="#0284c7" stroke-width="15"
                                                stroke-dasharray="${cDashInProg} 364.4" stroke-dashoffset="${cOffInProg}" class="donut-segment"
                                                title="Đang làm: ${mInProgCount} (${mPctInProg}%)" />
                                        
                                        <!-- To-do Segment (Amber) -->
                                        <circle id="donut-segment-todo" cx="80" cy="80" r="58" fill="none" 
                                                stroke="#f59e0b" stroke-width="15"
                                                stroke-dasharray="${cDashTodo} 364.4" stroke-dashoffset="${cOffTodo}" class="donut-segment"
                                                title="Cần làm: ${mTodoCount} (${mPctTodo}%)" />
                                    </svg>

                                    <!-- Center Text -->
                                    <div class="position-absolute top-50 start-50 translate-middle text-center" style="pointer-events: none;">
                                        <div class="fw-bold text-dark fs-4 lh-1" id="donut-center-total">${mTotalCount}</div>
                                        <div class="fs-9 text-muted mt-1" id="donut-center-label">công việc</div>
                                        <div class="badge bg-success-subtle text-success rounded-pill px-2 py-0-5 fs-9 mt-1 fw-semibold" id="donut-center-pct">
                                            ${mPctDone}% Xong
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Legend Grid -->
                            <div class="row g-2 pt-2 border-top fs-8">
                                <div class="col-6">
                                    <div class="d-flex align-items-center justify-content-between p-1.5 rounded-2 bg-light-subtle donut-legend-item" id="legend-item-done" onclick="filterClickUpTasks('STATUS_DONE')" style="cursor: pointer;" title="Lọc công việc đã xong">
                                        <span class="d-flex align-items-center gap-1.5 text-muted">
                                            <span class="badge-dot bg-success"></span> Hoàn thành
                                        </span>
                                        <span class="fw-bold text-success fs-8" id="donut-legend-done">${mDoneCount} <span class="fs-9 text-muted fw-normal">(${mPctDone}%)</span></span>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="d-flex align-items-center justify-content-between p-1.5 rounded-2 bg-light-subtle donut-legend-item" id="legend-item-inprog" onclick="filterClickUpTasks('STATUS_IN_PROGRESS')" style="cursor: pointer;" title="Lọc công việc đang làm">
                                        <span class="d-flex align-items-center gap-1.5 text-muted">
                                            <span class="badge-dot bg-primary"></span> Đang làm
                                        </span>
                                        <span class="fw-bold text-primary fs-8" id="donut-legend-inprog">${mInProgCount} <span class="fs-9 text-muted fw-normal">(${mPctInProg}%)</span></span>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="d-flex align-items-center justify-content-between p-1.5 rounded-2 bg-light-subtle donut-legend-item" id="legend-item-todo" onclick="filterClickUpTasks('STATUS_TODO')" style="cursor: pointer;" title="Lọc công việc cần làm">
                                        <span class="d-flex align-items-center gap-1.5 text-muted">
                                            <span class="badge-dot bg-amber"></span> Cần làm
                                        </span>
                                        <span class="fw-bold text-amber fs-8" id="donut-legend-todo">${mTodoCount} <span class="fs-9 text-muted fw-normal">(${mPctTodo}%)</span></span>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="d-flex align-items-center justify-content-between p-1.5 rounded-2 bg-light-subtle donut-legend-item" id="legend-item-overdue" onclick="filterTasksByTimeframe('OVERDUE', 'Quá hạn')" style="cursor: pointer;" title="Lọc các việc quá hạn">
                                        <span class="d-flex align-items-center gap-1.5 text-muted">
                                            <span class="badge-dot bg-danger"></span> Quá hạn
                                        </span>
                                        <span class="fw-bold text-danger fs-8" id="donut-legend-overdue">${mOverdueCount} việc</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Workload Distribution Table (Expanded Modern Data Table) -->
                <div class="card border-0 shadow-2xs rounded-3 overflow-hidden bg-white mb-4" id="workloadTableCard">
                    <div class="px-3 py-2.5 border-bottom d-flex align-items-center justify-content-between flex-wrap gap-2">
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <h6 class="fw-bold text-dark mb-0 fs-8"><i class="bi bi-people-fill me-1.5 text-primary"></i> Phân bổ khối lượng công việc (Workload Capacity)</h6>
                            <span class="badge bg-light text-muted border rounded-pill fs-9" id="workload-member-count">${userWorkloadList.size()} thành viên</span>
                        </div>
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <!-- Quick Filter Tabs -->
                            <div class="d-flex align-items-center gap-1 flex-wrap" id="workloadFilterTabs">
                                <button type="button" class="workload-capacity-filter-btn active" onclick="filterWorkloadCapacity('ALL', this)">
                                    Tất cả
                                </button>
                                <button type="button" class="workload-capacity-filter-btn" onclick="filterWorkloadCapacity('OVERLOAD', this)">
                                    <span class="badge-dot bg-danger"></span> Quá tải / Có trễ
                                </button>
                                <button type="button" class="workload-capacity-filter-btn" onclick="filterWorkloadCapacity('BUSY', this)">
                                    <span class="badge-dot bg-warning"></span> Bận rộn
                                </button>
                                <button type="button" class="workload-capacity-filter-btn" onclick="filterWorkloadCapacity('OPTIMAL', this)">
                                    <span class="badge-dot bg-primary"></span> Cân bằng
                                </button>
                            </div>

                            <!-- Search Input -->
                            <div class="input-group input-group-sm" style="width: 200px;">
                                <span class="input-group-text bg-light border-end-0 py-0 text-muted fs-9"><i class="bi bi-search"></i></span>
                                <input type="text" class="form-control bg-light border-start-0 py-1 fs-9" placeholder="Tìm thành viên..." id="workloadSearchInput" oninput="filterWorkloadTable(this.value)">
                            </div>
                        </div>
                    </div>
                    <div class="table-responsive" style="overflow-x: auto; overflow-y: visible; max-height: none;">
                        <table class="table table-hover align-middle mb-0 fs-8" id="workloadTable">
                            <thead class="table-light fs-9 text-muted text-uppercase">
                                <tr>
                                    <th class="ps-3 py-2.5 sortable-th" onclick="sortWorkloadTable(0)" title="Nhấn để sắp xếp theo tên thành viên">
                                        <div class="d-flex align-items-center justify-content-between">
                                            <span>Thành viên</span>
                                            <i class="bi bi-arrow-down-up sort-icon" id="sort-icon-0"></i>
                                        </div>
                                    </th>
                                    <th class="py-2.5 text-center sortable-th" onclick="sortWorkloadTable(1)" title="Nhấn để sắp xếp theo công suất">
                                        <div class="d-flex align-items-center justify-content-center gap-1">
                                            <span>Công suất</span>
                                            <i class="bi bi-arrow-down-up sort-icon" id="sort-icon-1"></i>
                                        </div>
                                    </th>
                                    <th class="py-2.5 text-center sortable-th" onclick="sortWorkloadTable(2)" title="Nhấn để sắp xếp theo tổng việc">
                                        <div class="d-flex align-items-center justify-content-center gap-1">
                                            <span>Tổng việc</span>
                                            <i class="bi bi-arrow-down-up sort-icon" id="sort-icon-2"></i>
                                        </div>
                                    </th>
                                    <th class="py-2.5 text-center sortable-th" onclick="sortWorkloadTable(3)" title="Nhấn để sắp xếp theo việc cần làm">
                                        <div class="d-flex align-items-center justify-content-center gap-1">
                                            <span>Cần làm</span>
                                            <i class="bi bi-arrow-down-up sort-icon" id="sort-icon-3"></i>
                                        </div>
                                    </th>
                                    <th class="py-2.5 text-center sortable-th" onclick="sortWorkloadTable(4)" title="Nhấn để sắp xếp theo việc đang làm">
                                        <div class="d-flex align-items-center justify-content-center gap-1">
                                            <span>Đang làm</span>
                                            <i class="bi bi-arrow-down-up sort-icon" id="sort-icon-4"></i>
                                        </div>
                                    </th>
                                    <th class="py-2.5 text-center sortable-th" onclick="sortWorkloadTable(5)" title="Nhấn để sắp xếp theo việc hoàn thành">
                                        <div class="d-flex align-items-center justify-content-center gap-1">
                                            <span>Hoàn thành</span>
                                            <i class="bi bi-arrow-down-up sort-icon" id="sort-icon-5"></i>
                                        </div>
                                    </th>
                                    <th class="py-2.5 text-center sortable-th" onclick="sortWorkloadTable(6)" title="Nhấn để sắp xếp theo việc quá hạn">
                                        <div class="d-flex align-items-center justify-content-center gap-1">
                                            <span>Quá hạn</span>
                                            <i class="bi bi-arrow-down-up sort-icon" id="sort-icon-6"></i>
                                        </div>
                                    </th>
                                    <th class="py-2.5 sortable-th" style="min-width: 220px;" onclick="sortWorkloadTable(7)" title="Nhấn để sắp xếp theo tỷ lệ hoàn thành">
                                        <div class="d-flex align-items-center justify-content-between">
                                            <span>Tỷ lệ hoàn thành</span>
                                            <i class="bi bi-arrow-down-up sort-icon" id="sort-icon-7"></i>
                                        </div>
                                    </th>
                                    <th class="pe-3 py-2.5 text-end" style="width: 110px;">
                                        <span>Thao tác</span>
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach items="${userWorkloadList}" var="wl">
                                    <c:set var="wlPctInProg" value="${wl.totalTasks > 0 ? Math.round((wl.inProgressTasks * 100.0) / wl.totalTasks) : 0}" />
                                    <c:set var="wlTodoTasks" value="${wl.totalTasks - wl.inProgressTasks - wl.doneTasks}" />
                                    <c:if test="${wlTodoTasks < 0}"><c:set var="wlTodoTasks" value="0" /></c:if>

                                    <!-- Tính toán chỉ số công suất tải việc (Workload Capacity) -->
                                    <c:choose>
                                        <c:when test="${wl.overdueTasks > 0 || wl.inProgressTasks >= 5}">
                                            <c:set var="wlCapKey" value="overload" />
                                            <c:set var="wlCapLabel" value="Quá tải" />
                                            <c:set var="wlCapClass" value="bg-danger-subtle text-danger border border-danger-subtle" />
                                            <c:set var="wlCapIcon" value="bi-exclamation-octagon-fill" />
                                        </c:when>
                                        <c:when test="${wl.inProgressTasks >= 3}">
                                            <c:set var="wlCapKey" value="busy" />
                                            <c:set var="wlCapLabel" value="Bận rộn" />
                                            <c:set var="wlCapClass" value="bg-warning-subtle text-warning border border-warning-subtle" />
                                            <c:set var="wlCapIcon" value="bi-lightning-charge-fill" />
                                        </c:when>
                                        <c:when test="${wl.inProgressTasks >= 1 || wl.totalTasks > 0}">
                                            <c:set var="wlCapKey" value="optimal" />
                                            <c:set var="wlCapLabel" value="Cân bằng" />
                                            <c:set var="wlCapClass" value="bg-primary-subtle text-primary border border-primary-subtle" />
                                            <c:set var="wlCapIcon" value="bi-check2-circle" />
                                        </c:when>
                                        <c:otherwise>
                                            <c:set var="wlCapKey" value="ready" />
                                            <c:set var="wlCapLabel" value="Sẵn sàng" />
                                            <c:set var="wlCapClass" value="bg-success-subtle text-success border border-success-subtle" />
                                            <c:set var="wlCapIcon" value="bi-cup-hot-fill" />
                                        </c:otherwise>
                                    </c:choose>

                                    <tr class="workload-row" 
                                        data-user-id="${wl.user.id}" 
                                        data-name="<c:out value="${wl.user.fullName}" />"
                                        data-capacity="${wlCapKey}"
                                        data-total="${wl.totalTasks}"
                                        data-todo="${wlTodoTasks}"
                                        data-inprog="${wl.inProgressTasks}"
                                        data-done="${wl.doneTasks}"
                                        data-overdue="${wl.overdueTasks}"
                                        data-rate="${wl.completionRate}"
                                        onclick="handleWorkloadRowClick('${wl.user.id}', '<c:out value="${wl.user.fullName}" />')"
                                        title="Bấm để lọc thống kê của ${wl.user.fullName}">
                                        <td class="ps-3 py-3">
                                            <div class="d-flex align-items-center gap-2">
                                                <span class="avatar-circle-sm bg-primary text-white rounded-circle d-flex align-items-center justify-content-center shadow-2xs" style="width: 30px; height: 30px; font-size: 0.75rem; font-weight: 600;">
                                                    ${wl.user.fullName.substring(0, 1).toUpperCase()}
                                                </span>
                                                <div>
                                                    <div class="fw-semibold text-dark user-name-cell fs-8">${wl.user.fullName}</div>
                                                    <span class="badge bg-light text-secondary border rounded-pill px-1-5 py-0 fs-10">${wl.user.role}</span>
                                                </div>
                                            </div>
                                        </td>
                                        <td class="text-center">
                                            <span class="badge ${wlCapClass} rounded-pill px-2.5 py-1 fs-9 d-inline-flex align-items-center gap-1">
                                                <i class="bi ${wlCapIcon}"></i> ${wlCapLabel}
                                            </span>
                                        </td>
                                        <td class="text-center">
                                            <span class="badge bg-light text-dark border rounded-pill px-2.5 py-1 fs-8 fw-bold">${wl.totalTasks}</span>
                                        </td>
                                        <td class="text-center">
                                            <span class="fw-semibold text-amber fs-8">${wlTodoTasks}</span>
                                        </td>
                                        <td class="text-center">
                                            <span class="fw-semibold text-primary fs-8">${wl.inProgressTasks}</span>
                                        </td>
                                        <td class="text-center">
                                            <span class="fw-semibold text-success fs-8">${wl.doneTasks}</span>
                                        </td>
                                        <td class="text-center">
                                            <c:choose>
                                                <c:when test="${wl.overdueTasks > 0}">
                                                    <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-2 py-0-5 fs-9 fw-bold">
                                                        <i class="bi bi-exclamation-triangle-fill me-1"></i>${wl.overdueTasks} trễ
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-muted fs-9">-</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <div class="d-flex align-items-center gap-2">
                                                <div class="workload-two-color-bar flex-grow-1" style="height: 7px;" title="Hoàn thành: ${wl.completionRate}%, Đang làm: ${wlPctInProg}%">
                                                    <div class="bar-done" style="width: ${wl.completionRate}%;"></div>
                                                    <div class="bar-inprog" style="width: ${wlPctInProg}%;"></div>
                                                </div>
                                                <div class="workload-rate-label text-nowrap d-flex align-items-center gap-1" style="min-width: 75px;">
                                                    <span class="fw-bold text-success fs-8">${wl.completionRate}%</span>
                                                    <c:if test="${wlPctInProg > 0}">
                                                        <span class="fs-10 text-primary opacity-75" title="Đang làm ${wlPctInProg}%">(+${wlPctInProg}%)</span>
                                                    </c:if>
                                                </div>
                                            </div>
                                        </td>
                                        <td class="pe-3 text-end">
                                            <button type="button" class="btn btn-sm btn-light border rounded-pill px-2 py-1 fs-9 text-secondary d-inline-flex align-items-center gap-1 shadow-2xs hover-primary"
                                                    onclick="viewMemberTasks('${wl.user.id}', '<c:out value="${wl.user.fullName}" />', event)"
                                                    title="Xem danh sách công việc của thành viên này">
                                                <span>Xem việc</span>
                                                <i class="bi bi-arrow-right fs-10"></i>
                                            </button>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div><!-- /clickup-view-metrics -->

            <!-- =========================================================================
                 TAB 5: SCHEDULE & DEADLINE TIMELINE VIEW (LỊCH TRÌNH THEO HẠN CHÓT)
                 ========================================================================= -->
            <div id="clickup-view-schedule" class="clickup-view-pane ${currentView == 'schedule' ? '' : 'd-none'}">
                <div class="d-flex align-items-center justify-content-between mb-3 px-1">
                    <div>
                        <h6 class="fw-bold text-dark mb-0 fs-7"><i class="bi bi-calendar-check me-1 text-primary"></i> Lịch Trình Công Việc (Schedule Timeline)</h6>
                        <span class="fs-9 text-muted">Phân loại và giám sát thời hạn hoàn thành theo từng mốc thời gian</span>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <button type="button" class="btn btn-sm btn-outline-secondary rounded-pill px-3 py-1 fs-9" onclick="switchClickUpTab('tasks')">
                            <i class="bi bi-arrow-left me-1"></i> Trở về Bảng công việc
                        </button>
                    </div>
                </div>

                <div class="row g-3">
                    <!-- 1. QUÁ HẠN (OVERDUE) -->
                    <div class="col-12 col-md-6 col-xl-3">
                        <div class="schedule-lane">
                            <div class="schedule-lane-header">
                                <div class="d-flex align-items-center gap-2">
                                    <span class="badge bg-danger text-white rounded-pill px-2 py-0-5 fs-9">🔴 Quá hạn</span>
                                </div>
                                <span class="fs-9 text-danger fw-bold">Cần xử lý gấp</span>
                            </div>
                            <c:set var="hasOverdue" value="false" />
                            <c:forEach items="${allProjectTasks}" var="t">
                                <c:if test="${t.isOverdue()}">
                                    <c:set var="hasOverdue" value="true" />
                                    <div class="schedule-card schedule-card-overdue" onclick="openClickUpTask(${t.id})" style="cursor: pointer;">
                                        <div class="d-flex align-items-start justify-content-between gap-2 mb-1">
                                            <span class="badge bg-danger-subtle text-danger rounded-pill px-2 fs-9">Trễ ${t.daysRemaining < 0 ? -t.daysRemaining : 0} ngày</span>
                                            <span class="badge ${t.priorityBadgeClass} rounded-pill px-2 fs-9">● ${t.priorityLabel}</span>
                                        </div>
                                        <div class="fw-bold text-dark fs-8 mb-1 text-truncate-2">${t.title}</div>
                                        <div class="d-flex align-items-center justify-content-between fs-9 text-muted mt-2 pt-1 border-top">
                                            <span><i class="bi bi-person me-1"></i>${not empty t.assigneeName ? t.assigneeName : 'Chưa giao'}</span>
                                            <span class="text-danger fw-semibold"><i class="bi bi-calendar-x me-1"></i>${t.dueDate}</span>
                                        </div>
                                    </div>
                                </c:if>
                            </c:forEach>
                            <c:if test="${!hasOverdue}">
                                <div class="text-center py-4 text-muted fs-9 fst-italic">Không có công việc quá hạn 🎉</div>
                            </c:if>
                        </div>
                    </div>

                    <!-- 2. HÔM NAY (DUE TODAY) -->
                    <div class="col-12 col-md-6 col-xl-3">
                        <div class="schedule-lane">
                            <div class="schedule-lane-header">
                                <div class="d-flex align-items-center gap-2">
                                    <span class="badge bg-warning text-dark rounded-pill px-2 py-0-5 fs-9">🟠 Hôm nay</span>
                                </div>
                                <span class="fs-9 text-warning-emphasis fw-bold">Hạn trong ngày</span>
                            </div>
                            <c:set var="hasToday" value="false" />
                            <c:forEach items="${allProjectTasks}" var="t">
                                <c:if test="${!t.isOverdue() && t.getDeadlineStatus() == 'DUE_TODAY'}">
                                    <c:set var="hasToday" value="true" />
                                    <div class="schedule-card schedule-card-today" onclick="openClickUpTask(${t.id})" style="cursor: pointer;">
                                        <div class="d-flex align-items-start justify-content-between gap-2 mb-1">
                                            <span class="badge bg-warning-subtle text-warning-emphasis rounded-pill px-2 fs-9">Đến hạn hôm nay</span>
                                            <span class="badge ${t.priorityBadgeClass} rounded-pill px-2 fs-9">● ${t.priorityLabel}</span>
                                        </div>
                                        <div class="fw-bold text-dark fs-8 mb-1 text-truncate-2">${t.title}</div>
                                        <div class="d-flex align-items-center justify-content-between fs-9 text-muted mt-2 pt-1 border-top">
                                            <span><i class="bi bi-person me-1"></i>${not empty t.assigneeName ? t.assigneeName : 'Chưa giao'}</span>
                                            <span class="text-warning-emphasis fw-semibold"><i class="bi bi-clock-history me-1"></i>Hôm nay</span>
                                        </div>
                                    </div>
                                </c:if>
                            </c:forEach>
                            <c:if test="${!hasToday}">
                                <div class="text-center py-4 text-muted fs-9 fst-italic">Không có việc đến hạn hôm nay</div>
                            </c:if>
                        </div>
                    </div>

                    <!-- 3. TUẦN NÀY (THIS WEEK / DUE SOON) -->
                    <div class="col-12 col-md-6 col-xl-3">
                        <div class="schedule-lane">
                            <div class="schedule-lane-header">
                                <div class="d-flex align-items-center gap-2">
                                    <span class="badge bg-primary text-white rounded-pill px-2 py-0-5 fs-9">🔵 Tuần này</span>
                                </div>
                                <span class="fs-9 text-primary fw-bold">1 - 7 ngày tới</span>
                            </div>
                            <c:set var="hasThisWeek" value="false" />
                            <c:forEach items="${allProjectTasks}" var="t">
                                <c:if test="${!t.isOverdue() && t.getDeadlineStatus() != 'DUE_TODAY' && t.isDueSoon()}">
                                    <c:set var="hasThisWeek" value="true" />
                                    <div class="schedule-card schedule-card-thisweek" onclick="openClickUpTask(${t.id})" style="cursor: pointer;">
                                        <div class="d-flex align-items-start justify-content-between gap-2 mb-1">
                                            <span class="badge bg-primary-subtle text-primary rounded-pill px-2 fs-9">Còn ${t.daysRemaining} ngày</span>
                                            <span class="badge ${t.priorityBadgeClass} rounded-pill px-2 fs-9">● ${t.priorityLabel}</span>
                                        </div>
                                        <div class="fw-bold text-dark fs-8 mb-1 text-truncate-2">${t.title}</div>
                                        <div class="d-flex align-items-center justify-content-between fs-9 text-muted mt-2 pt-1 border-top">
                                            <span><i class="bi bi-person me-1"></i>${not empty t.assigneeName ? t.assigneeName : 'Chưa giao'}</span>
                                            <span class="text-primary fw-semibold"><i class="bi bi-calendar3 me-1"></i>${t.dueDate}</span>
                                        </div>
                                    </div>
                                </c:if>
                            </c:forEach>
                            <c:if test="${!hasThisWeek}">
                                <div class="text-center py-4 text-muted fs-9 fst-italic">Không có việc đến hạn tuần này</div>
                            </c:if>
                        </div>
                    </div>

                    <!-- 4. SẮP TỚI & DÀI HẠN (UPCOMING & LATER) -->
                    <div class="col-12 col-md-6 col-xl-3">
                        <div class="schedule-lane">
                            <div class="schedule-lane-header">
                                <div class="d-flex align-items-center gap-2">
                                    <span class="badge bg-success text-white rounded-pill px-2 py-0-5 fs-9">🟢 Sắp tới & Khác</span>
                                </div>
                                <span class="fs-9 text-muted">Dài hạn / Đã xong</span>
                            </div>
                            <c:set var="hasUpcoming" value="false" />
                            <c:forEach items="${allProjectTasks}" var="t">
                                <c:if test="${!t.isOverdue() && t.getDeadlineStatus() != 'DUE_TODAY' && !t.isDueSoon()}">
                                    <c:set var="hasUpcoming" value="true" />
                                    <div class="schedule-card schedule-card-upcoming" onclick="openClickUpTask(${t.id})" style="cursor: pointer;">
                                        <div class="d-flex align-items-start justify-content-between gap-2 mb-1">
                                            <span class="badge ${t.statusBadgeClass} rounded-pill px-2 fs-9">${t.statusLabel}</span>
                                            <span class="badge ${t.priorityBadgeClass} rounded-pill px-2 fs-9">● ${t.priorityLabel}</span>
                                        </div>
                                        <div class="fw-bold text-dark fs-8 mb-1 text-truncate-2">${t.title}</div>
                                        <div class="d-flex align-items-center justify-content-between fs-9 text-muted mt-2 pt-1 border-top">
                                            <span><i class="bi bi-person me-1"></i>${not empty t.assigneeName ? t.assigneeName : 'Chưa giao'}</span>
                                            <span class="text-muted"><i class="bi bi-calendar me-1"></i>${not empty t.dueDate ? t.dueDate : 'Chưa đặt'}</span>
                                        </div>
                                    </div>
                                </c:if>
                            </c:forEach>
                            <c:if test="${!hasUpcoming}">
                                <div class="text-center py-4 text-muted fs-9 fst-italic">Chưa có công việc nào</div>
                            </c:if>
                        </div>
                    </div>
                </div>
            </div><!-- /clickup-view-schedule -->

            <!-- =========================================================================
                 CLICKUP VIEW: ACTIVITY LOG & AUDIT TRAIL (DÒNG THỜI GIAN LỊCH SỬ HOẠT ĐỘNG)
                 ========================================================================= -->
            <div id="clickup-view-activity" class="clickup-view-pane ${currentView == 'activity' ? '' : 'd-none'}">
                <div class="d-flex align-items-center justify-content-between mb-3 px-1">
                    <div>
                        <h6 class="fw-bold text-dark mb-0 fs-7">
                            <i class="bi bi-clock-history me-1 text-primary"></i> Nhật Ký Hoạt Động (Project Activity Log & Audit Trail)
                        </h6>
                        <span class="fs-9 text-muted">Dòng thời gian ghi nhận tự động mọi biến động tạo task, chuyển trạng thái, nghiệm thu và tài liệu của dự án</span>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <a href="${pageContext.request.contextPath}/task?action=exportCsv&projectId=${project.id}" 
                           class="btn btn-sm btn-light border text-success rounded-pill px-3 py-1 fs-9 fw-semibold shadow-2xs d-flex align-items-center gap-1"
                           title="Xuất danh sách công việc ra file Excel (.csv chuẩn UTF-8)">
                            <i class="bi bi-file-earmark-spreadsheet-fill text-success"></i>
                            <span>Xuất Excel</span>
                        </a>
                        <button type="button" class="btn btn-sm btn-outline-secondary rounded-pill px-3 py-1 fs-9" onclick="switchClickUpTab('tasks')">
                            <i class="bi bi-arrow-left me-1"></i> Trở về Bảng công việc
                        </button>
                    </div>
                </div>

                <div class="card border-0 shadow-2xs rounded-3 overflow-hidden">
                    <div class="card-header bg-white border-bottom py-2.5 px-3 d-flex align-items-center justify-content-between">
                        <div class="d-flex align-items-center gap-2">
                            <span class="badge bg-primary-subtle text-primary rounded-pill px-2.5 py-1 fs-9 fw-semibold">
                                <i class="bi bi-activity me-1"></i> ${activityLogs.size()} hoạt động gần nhất
                            </span>
                            <span class="fs-9 text-muted">• Ghi vết tự động bảo toàn lịch sử</span>
                        </div>
                    </div>
                    <div class="card-body p-3 p-md-4">
                        <c:choose>
                            <c:when test="${empty activityLogs}">
                                <div class="text-center py-5 text-muted">
                                    <div class="avatar-circle-lg bg-light text-secondary rounded-circle mx-auto d-flex align-items-center justify-content-center mb-3" style="width: 56px; height: 56px;">
                                        <i class="bi bi-clock-history fs-2 opacity-50"></i>
                                    </div>
                                    <h6 class="fw-semibold text-dark fs-7 mb-1">Chưa có lịch sử hoạt động nào</h6>
                                    <p class="fs-9 text-muted mb-0">Các thao tác tạo việc, chuyển trạng thái, duyệt nghiệm thu hoặc tải tài liệu sẽ tự động xuất hiện tại đây.</p>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="activity-timeline position-relative ps-4 ps-md-4">
                                    <div class="activity-timeline-line position-absolute start-0 top-0 bottom-0 ms-3 border-start border-2 border-light-subtle"></div>
                                    <c:forEach items="${activityLogs}" var="act">
                                        <div class="activity-item position-relative mb-3 pb-2">
                                            <div class="activity-badge-dot position-absolute start-0 translate-middle-x rounded-circle d-flex align-items-center justify-content-center bg-white shadow-2xs border" style="left: -16px; top: 4px; width: 30px; height: 30px; z-index: 2;">
                                                <i class="bi ${act.iconClass} fs-8"></i>
                                            </div>
                                            <div class="activity-content-box bg-light-subtle rounded-3 p-2.5 border ms-3 shadow-2xs hover-shadow-xs transition">
                                                <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-1">
                                                    <div class="d-flex align-items-center gap-2">
                                                        <span class="avatar-circle-xs bg-primary text-white rounded-circle d-inline-flex align-items-center justify-content-center fw-bold fs-9" style="width: 22px; height: 22px;">
                                                            ${not empty act.userName ? act.userName.substring(0, 1).toUpperCase() : 'U'}
                                                        </span>
                                                        <span class="fw-semibold text-dark fs-8">${act.userName}</span>
                                                        <span class="badge ${act.badgeClass} rounded-pill px-2 py-0-5 fs-9 fw-semibold">
                                                            ${act.actionLabel}
                                                        </span>
                                                    </div>
                                                    <span class="fs-9 text-muted d-flex align-items-center gap-1" title="${act.createdAt}">
                                                        <i class="bi bi-clock"></i> ${act.timeAgo}
                                                    </span>
                                                </div>
                                                <div class="fs-8 text-dark mt-1">
                                                    <c:if test="${not empty act.targetTitle}">
                                                        <span class="fw-semibold text-primary">#${act.targetTitle}</span> — 
                                                    </c:if>
                                                    <span class="text-secondary">${act.description}</span>
                                                </div>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div><!-- /clickup-view-activity -->

        </div><!-- /clickup-workspace-body -->
    </main><!-- /clickup-main-panel -->
    </div><!-- /clickup-islands-row -->
</div><!-- /clickup-shell -->

<!-- =========================================================================
     INBOX SLIDE-OVER DRAWER (HỘP THƯ THÔNG BÁO THỰC TẾ)
     ========================================================================= -->
<div class="offcanvas offcanvas-end shadow-lg border-start-0" tabindex="-1" id="inboxDrawer" aria-labelledby="inboxDrawerLabel" style="width: 380px;">
    <div class="offcanvas-header border-bottom py-3 px-3 bg-light">
        <div class="d-flex align-items-center gap-2">
            <span class="avatar-circle-sm bg-primary text-white rounded-circle d-flex align-items-center justify-content-center" style="width: 28px; height: 28px;">
                <i class="bi bi-inbox-fill fs-8"></i>
            </span>
            <div>
                <h6 class="offcanvas-title fw-bold text-dark fs-7 mb-0" id="inboxDrawerLabel">Inbox &bull; Thông báo</h6>
                <span class="fs-9 text-muted">${userNotifications.size()} thông báo gần đây</span>
            </div>
        </div>
        <div class="d-flex align-items-center gap-2">
            <c:if test="${unreadNotifCount > 0}">
                <a href="${pageContext.request.contextPath}/notification?action=readAll" class="btn btn-xs btn-outline-primary rounded-pill px-2 py-1 fs-9" title="Đánh dấu tất cả là đã đọc">
                    <i class="bi bi-check2-all me-1"></i> Đọc hết
                </a>
            </c:if>
            <button type="button" class="btn-close fs-9" data-bs-dismiss="offcanvas" aria-label="Đóng"></button>
        </div>
    </div>
    <div class="offcanvas-body p-3 overflow-y-auto d-flex flex-column gap-2">
        <c:choose>
            <c:when test="${empty userNotifications}">
                <div class="text-center py-5 my-auto text-muted">
                    <i class="bi bi-bell-slash fs-1 d-block mb-2 text-secondary opacity-50"></i>
                    <div class="fw-semibold fs-7">Hộp thư trống</div>
                    <div class="fs-9">Bạn không có thông báo nào cần xử lý.</div>
                </div>
            </c:when>
            <c:otherwise>
                <c:forEach items="${userNotifications}" var="notif">
                    <a href="${pageContext.request.contextPath}/notification?action=read&id=${notif.id}&redirect=${notif.link}" 
                       class="notif-item ${notif.read ? '' : 'unread'}">
                        <div class="d-flex align-items-start justify-content-between gap-2 mb-1">
                            <div class="d-flex align-items-center gap-2">
                                <c:choose>
                                    <c:when test="${notif.type == 'INVITE'}">
                                        <span class="badge bg-success-subtle text-success rounded-pill px-2 py-0-5 fs-9"><i class="bi bi-envelope-open me-1"></i>Lời mời</span>
                                    </c:when>
                                    <c:when test="${notif.type == 'TASK_ASSIGN' || notif.type == 'TASK'}">
                                        <span class="badge bg-primary-subtle text-primary rounded-pill px-2 py-0-5 fs-9"><i class="bi bi-check2-square me-1"></i>Công việc</span>
                                    </c:when>
                                    <c:when test="${notif.type == 'COMMENT' || notif.type == 'MENTION'}">
                                        <span class="badge bg-info-subtle text-info rounded-pill px-2 py-0-5 fs-9"><i class="bi bi-chat-quote me-1"></i>Nhắc tên</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge bg-secondary-subtle text-secondary rounded-pill px-2 py-0-5 fs-9"><i class="bi bi-bell me-1"></i>Hệ thống</span>
                                    </c:otherwise>
                                </c:choose>
                                <span class="fs-9 text-muted"><i class="bi bi-clock me-1"></i>${notif.createdAtStr}</span>
                            </div>
                            <c:if test="${!notif.read}">
                                <span class="notif-unread-dot" title="Chưa đọc"></span>
                            </c:if>
                        </div>
                        <div class="fw-bold text-dark fs-8 mb-1"><c:out value="${notif.title}" /></div>
                        <div class="fs-9 text-secondary lh-sm text-truncate-2"><c:out value="${notif.content}" /></div>
                    </a>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<!-- ClickUp 3.0 Interactive Controller Scripts -->
<script>
    // 1. Sidebar Toggle & State persistence in localStorage and Cookie
    function toggleClickUpSidebar() {
        var sidebar = document.getElementById('clickupSidebar');
        var expandBtns = document.querySelectorAll('.btn-expand-sidebar');
        if (!sidebar) return;
        var isCollapsed = sidebar.classList.toggle('collapsed');
        localStorage.setItem('clickup_sidebar_collapsed', isCollapsed ? '1' : '0');
        var basePath = window.location.pathname.startsWith('/teamwork-hub') ? '/teamwork-hub' : '/';
        document.cookie = "sidebar_collapsed=" + (isCollapsed ? 'true' : 'false') + "; path=" + basePath + "; max-age=" + (30 * 24 * 60 * 60);
        expandBtns.forEach(function(btn) {
            if (isCollapsed) {
                btn.classList.remove('d-none');
            } else {
                btn.classList.add('d-none');
            }
        });
    }

    // Initialize sidebar state on page load
    document.addEventListener('DOMContentLoaded', function() {
        var isCollapsed = localStorage.getItem('clickup_sidebar_collapsed') === '1';
        var sidebar = document.getElementById('clickupSidebar');
        var expandBtns = document.querySelectorAll('.btn-expand-sidebar');
        if (isCollapsed && sidebar) {
            sidebar.classList.add('collapsed');
            expandBtns.forEach(function(btn) {
                btn.classList.remove('d-none');
            });
        }
    });

    // 2. Tab switching: chat, tasks, docs, metrics, schedule
    function switchClickUpTab(tab) {
        document.querySelectorAll('.clickup-tab-link').forEach(function(el) {
            el.classList.remove('active');
        });
        var activeBtn = document.getElementById('tab-btn-' + tab);
        if (activeBtn) activeBtn.classList.add('active');

        document.querySelectorAll('.clickup-view-pane').forEach(function(el) {
            el.classList.add('d-none');
        });
        var targetPane = document.getElementById('clickup-view-' + tab);
        if (targetPane) targetPane.classList.remove('d-none');

        // Header dự án luôn hiển thị để người dùng chuyển tab mượt mà; Toolbar lọc chỉ hiển thị ở tab tasks
        var mainHeader = document.querySelector('.clickup-main-header');
        var controlToolbar = document.querySelector('.clickup-control-toolbar');
        if (mainHeader) mainHeader.classList.remove('d-none');
        if (controlToolbar) {
            if (tab === 'tasks') {
                controlToolbar.classList.remove('d-none');
            } else {
                controlToolbar.classList.add('d-none');
            }
        }

        var switcher = document.getElementById('taskViewSwitcher');
        if (switcher) {
            if (tab === 'tasks') {
                switcher.classList.remove('d-none');
            } else {
                switcher.classList.add('d-none');
            }
        }

        try {
            var url = new URL(window.location.href);
            url.searchParams.set('view', tab);
            window.history.replaceState({}, '', url);
        } catch(e) {}

        if (tab === 'chat') {
            var chatBox = document.getElementById('clickupChatMessages');
            if (chatBox) chatBox.scrollTop = chatBox.scrollHeight;
        }
    }

    // 3. Task subview switching: list vs board
    function switchTaskSubView(view) {
        var btnList = document.getElementById('btn-view-list');
        var btnBoard = document.getElementById('btn-view-board');
        var viewList = document.getElementById('task-subview-list');
        var viewBoard = document.getElementById('task-subview-board');

        if (view === 'list') {
            if (btnList) btnList.classList.add('active');
            if (btnBoard) btnBoard.classList.remove('active');
            if (viewList) viewList.classList.remove('d-none');
            if (viewBoard) viewBoard.classList.add('d-none');
        } else {
            if (btnList) btnList.classList.remove('active');
            if (btnBoard) btnBoard.classList.add('active');
            if (viewList) viewList.classList.add('d-none');
            if (viewBoard) viewBoard.classList.remove('d-none');
        }

        // Lưu Cookie preferred_task_view (hạn 30 ngày)
        var basePath = window.location.pathname.startsWith('/teamwork-hub') ? '/teamwork-hub' : '/';
        document.cookie = "preferred_task_view=" + encodeURIComponent(view) + "; path=" + basePath + "; max-age=" + (30 * 24 * 60 * 60);

        try {
            var url = new URL(window.location.href);
            url.searchParams.set('taskView', view);
            window.history.replaceState({}, '', url);
        } catch(e) {}
    }

    // Project members data for dynamic selects
    window.projectMembersList = [
        <c:forEach items="${userList}" var="u" varStatus="loop">
        { id: ${u.id}, name: '<c:out value="${u.fullName}" />' }<c:if test="${!loop.last}">,</c:if>
        </c:forEach>
    ];

    // 4. Toggle collapsing of group in List View
    window.toggleClickUpGroup = function(groupId) {
        var rows = document.querySelectorAll('.group-' + groupId + '-row');
        var icon = document.getElementById('chevron-' + groupId);
        if (!rows || rows.length === 0) return;

        var isCurrentlyExpanded = icon ? icon.classList.contains('bi-chevron-down') : !rows[0].classList.contains('d-none');
        var willHide = isCurrentlyExpanded;

        rows.forEach(function(r) {
            // Never show inline create task or subtask rows when expanding group
            if (r.classList.contains('clickup-inline-create-row') || r.classList.contains('clickup-inline-subtask-row')) {
                r.classList.add('d-none');
                return;
            }
            if (willHide) {
                r.classList.add('d-none');
            } else {
                // If it's a subtask row and subtask preference is not expanded, keep it hidden
                var subtaskCookie = document.cookie.split('; ').find(function(c) { return c.startsWith('preferred_subtask_mode='); });
                var subMode = subtaskCookie ? decodeURIComponent(subtaskCookie.split('=')[1]) : '${subtaskMode}';
                if (r.classList.contains('clickup-subtask-row') && subMode !== 'expanded') {
                    return;
                }
                r.classList.remove('d-none');
            }
        });

        if (willHide && typeof cancelInlineCreateTask === 'function') {
            cancelInlineCreateTask(groupId);
        }

        if (icon) {
            icon.className = willHide ? 'bi bi-chevron-right me-1' : 'bi bi-chevron-down me-1';
        }

        try {
            localStorage.setItem('clickup_group_' + groupId + '_collapsed', willHide ? 'true' : 'false');
        } catch(e) {}
    };

    // 5. Toggle subtasks visibility for a parent task
    window.toggleSubtasks = function(taskId, event) {
        if (event) event.stopPropagation();
        var subRows = document.querySelectorAll('.clickup-subtask-row[data-parent-id="' + taskId + '"]');
        var caret = document.getElementById('caret-' + taskId);
        var caretIcon = caret ? caret.querySelector('i') : document.getElementById('subtask-caret-icon-' + taskId);
        var toggleBtn = document.getElementById('subtask-toggle-' + taskId);
        var isExpanding = false;

        subRows.forEach(function(row) {
            if (row.classList.contains('d-none')) {
                row.classList.remove('d-none');
                isExpanding = true;
            } else {
                row.classList.add('d-none');
                isExpanding = false;
            }
        });

        if (caretIcon) {
            caretIcon.className = isExpanding ? 'bi bi-chevron-down' : 'bi bi-chevron-right';
        }
        if (caret) {
            if (isExpanding) caret.classList.add('is-expanded');
            else caret.classList.remove('is-expanded');
        }
        if (toggleBtn) {
            var icon = toggleBtn.querySelector('i');
            if (icon) {
                icon.className = isExpanding ? 'bi bi-chevron-down' : 'bi bi-chevron-right';
            }
        }
    };

    // 5.1. Subtask Display 2-Mode Toggle (Đóng ⇄ Mở rộng)
    window.applySubtaskMode = function(mode) {
        var basePath = window.location.pathname.startsWith('/teamwork-hub') ? '/teamwork-hub' : '/';
        document.cookie = "preferred_subtask_mode=" + encodeURIComponent(mode) + "; path=" + basePath + "; max-age=" + (30 * 24 * 60 * 60);

        var label = document.getElementById('subtaskToggleLabel');
        var icon = document.getElementById('subtaskToggleIcon');
        var isExpanded = (mode === 'expanded');

        if (label) {
            label.textContent = isExpanded ? 'Subtasks: Mở rộng' : 'Subtasks: Đóng';
        }
        if (icon) {
            icon.className = isExpanded ? 'bi bi-chevron-down text-primary' : 'bi bi-chevron-right text-secondary';
        }

        var allSubRows = document.querySelectorAll('.clickup-subtask-row');
        var allCarets = document.querySelectorAll('.subtask-caret');

        if (isExpanded) {
            allSubRows.forEach(function(r) { r.classList.remove('d-none'); });
            allCarets.forEach(function(c) {
                c.classList.remove('d-none');
                c.classList.add('is-expanded');
                var i = c.querySelector('i');
                if (i) i.className = 'bi bi-chevron-down';
            });
        } else {
            allSubRows.forEach(function(r) { r.classList.add('d-none'); });
            allCarets.forEach(function(c) {
                c.classList.remove('d-none', 'is-expanded');
                var i = c.querySelector('i');
                if (i) i.className = 'bi bi-chevron-right';
            });
        }
    };

    window.toggleAllSubtasks = function() {
        var label = document.getElementById('subtaskToggleLabel');
        var isCurrentlyExpanded = label ? label.textContent.includes('Mở rộng') : false;
        var newMode = isCurrentlyExpanded ? 'collapsed' : 'expanded';
        window.applySubtaskMode(newMode);
        if (window.showToast) {
            window.showToast(newMode === 'expanded' ? 'Đã mở rộng tất cả việc con' : 'Đã đóng tất cả việc con', 'info');
        }
    };

    // Aliases
    window.setSubtaskMode = window.applySubtaskMode;

    // 5.2. ClickUp Floating Status Popover
    var currentStatusTarget = { id: 0, isSubtask: false, currentStatus: '', parentTaskId: 0 };

    window.openStatusDropdown = function(event, id, isSubtask, currentStatus, parentTaskId) {
        if (event) {
            if (typeof event.stopPropagation === 'function') event.stopPropagation();
            if (typeof event.preventDefault === 'function') event.preventDefault();
        }

        if (!isSubtask && currentStatus === 'DONE') {
            if (window.showToast) window.showToast('Công việc đã hoàn thành (DONE) đã bị khóa, không thể thay đổi trạng thái!', 'warning');
            return;
        }
        if (isSubtask && parentTaskId) {
            var parentRow = document.querySelector('.clickup-task-row[data-task-id="' + parentTaskId + '"]');
            if (parentRow && parentRow.classList.contains('group-done-row')) {
                if (window.showToast) window.showToast('Công việc cha đã hoàn thành (DONE). Toàn bộ việc con đã bị khóa!', 'warning');
                return;
            }
        }

        // Chế độ Solo: Subtask là 1-click checklist (tick là toggle DONE/TODO tức thì)
        if (isSubtask && ${project.soloProject}) {
            currentStatusTarget = {
                id: id,
                isSubtask: isSubtask,
                currentStatus: currentStatus,
                parentTaskId: parentTaskId || 0
            };
            var isDone = (currentStatus === 'APPROVED' || currentStatus === 'DONE');
            window.executeInlineStatusChange(isDone ? 'TODO' : 'DONE');
            return;
        }

        var popover = document.getElementById('clickupStatusPopover');
        if (!popover) return;

        // Nếu popover đang mở cho chính đối tượng này -> đóng lại (Toggle)
        if ((popover.classList.contains('show') || popover.style.display === 'block') &&
            currentStatusTarget.id === id && currentStatusTarget.isSubtask === isSubtask) {
            popover.classList.remove('show');
            popover.classList.add('d-none');
            popover.style.display = 'none';
            return;
        }

        currentStatusTarget = {
            id: id,
            isSubtask: isSubtask,
            currentStatus: currentStatus,
            parentTaskId: parentTaskId || 0
        };

        var header = document.getElementById('clickupPopoverHeader');
        var container = document.getElementById('clickupPopoverOptions');
        if (!container) return;

        if (isSubtask) {
            if (header) header.textContent = 'Trạng thái việc con';
            var isDone = (currentStatus === 'APPROVED' || currentStatus === 'DONE');
            container.innerHTML = 
                '<button type="button" class="btn btn-sm w-100 text-start d-flex align-items-center gap-2 py-2 px-2-5 rounded-2 hover-bg-light border-0 mb-1 ' + (!isDone ? 'bg-light fw-bold text-primary' : 'text-dark') + '" onclick="event.stopPropagation(); executeInlineStatusChange(\'TODO\')">' +
                    '<span class="clickup-status-dot dot-todo"></span>' +
                    '<span class="fs-8">Chưa xong (TO DO)</span>' +
                '</button>' +
                '<button type="button" class="btn btn-sm w-100 text-start d-flex align-items-center gap-2 py-2 px-2-5 rounded-2 hover-bg-light border-0 ' + (isDone ? 'bg-light fw-bold text-success' : 'text-dark') + '" onclick="event.stopPropagation(); executeInlineStatusChange(\'DONE\')">' +
                    '<span class="clickup-status-dot dot-done"><i class="bi bi-check text-white fs-9"></i></span>' +
                    '<span class="fs-8">Hoàn thành (DONE)</span>' +
                '</button>';
        } else {
            if (header) header.textContent = 'Trạng thái công việc';
            container.innerHTML = 
                '<button type="button" class="btn btn-sm w-100 text-start d-flex align-items-center gap-2 py-2 px-2-5 rounded-2 hover-bg-light border-0 mb-1 ' + (currentStatus === 'TODO' ? 'bg-light fw-bold text-primary' : 'text-dark') + '" onclick="event.stopPropagation(); executeInlineStatusChange(\'TODO\')">' +
                    '<span class="clickup-status-dot dot-todo"></span>' +
                    '<span class="fs-8">TO DO (Cần làm)</span>' +
                '</button>' +
                '<button type="button" class="btn btn-sm w-100 text-start d-flex align-items-center gap-2 py-2 px-2-5 rounded-2 hover-bg-light border-0 mb-1 ' + (currentStatus === 'IN_PROGRESS' ? 'bg-light fw-bold text-info' : 'text-dark') + '" onclick="event.stopPropagation(); executeInlineStatusChange(\'IN_PROGRESS\')">' +
                    '<span class="clickup-status-dot dot-inprog"></span>' +
                    '<span class="fs-8">IN PROGRESS (Đang làm)</span>' +
                '</button>' +
                '<button type="button" class="btn btn-sm w-100 text-start d-flex align-items-center gap-2 py-2 px-2-5 rounded-2 hover-bg-light border-0 ' + (currentStatus === 'DONE' ? 'bg-light fw-bold text-success' : 'text-dark') + '" onclick="event.stopPropagation(); executeInlineStatusChange(\'DONE\')">' +
                    '<span class="clickup-status-dot dot-done"><i class="bi bi-check text-white fs-9"></i></span>' +
                    '<span class="fs-8">DONE (Hoàn thành)</span>' +
                '</button>';
        }

        // Tìm phần tử kích hoạt (target element) an toàn tuyệt đối
        var targetEl = null;
        if (event) {
            if (event.currentTarget && event.currentTarget.classList && event.currentTarget.classList.contains('clickup-status-dot')) {
                targetEl = event.currentTarget;
            } else if (event.target) {
                targetEl = event.target.closest('.clickup-status-dot');
            }
        }
        if (!targetEl) {
            targetEl = document.getElementById((isSubtask ? 'subtask-status-dot-' : 'status-dot-') + id);
        }

        if (targetEl) {
            var rect = targetEl.getBoundingClientRect();
            var popoverWidth = 220;
            var leftPos = rect.left;
            var topPos = rect.bottom + 6;

            if (leftPos + popoverWidth > window.innerWidth - 12) {
                leftPos = Math.max(10, window.innerWidth - popoverWidth - 16);
            }
            if (topPos + 180 > window.innerHeight) {
                topPos = Math.max(10, rect.top - 170);
            }

            popover.style.position = 'fixed';
            popover.style.top = topPos + 'px';
            popover.style.left = leftPos + 'px';
            popover.style.zIndex = '999999';
        } else {
            popover.style.position = 'fixed';
            popover.style.top = '30%';
            popover.style.left = '40%';
            popover.style.zIndex = '999999';
        }

        popover.style.display = 'block';
        popover.classList.remove('d-none');
        popover.classList.add('show');
    };

    window.executeInlineStatusChange = function(newStatus) {
        var popover = document.getElementById('clickupStatusPopover');
        if (popover) {
            popover.classList.remove('show');
            popover.classList.add('d-none');
            popover.style.display = 'none';
        }

        var basePath = window.location.pathname.startsWith('/teamwork-hub') ? '/teamwork-hub' : '';
        var params = new URLSearchParams();

        if (currentStatusTarget.isSubtask) {
            var isCompleted = (newStatus === 'DONE');
            params.append('action', 'toggleSubTask');
            params.append('projectId', '${project.id}');
            params.append('subTaskId', currentStatusTarget.id);
            params.append('completed', isCompleted ? 'true' : 'false');
            params.append('ajax', 'true');

            fetch(basePath + '/task', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: params.toString()
            })
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data.success) {
                    if (window.showToast) window.showToast(data.message, 'success');
                    if (isCompleted && window.fireConfettiCelebration) {
                        window.fireConfettiCelebration();
                    }
                    var dot = document.getElementById('subtask-status-dot-' + currentStatusTarget.id);
                    var titleEl = document.getElementById('subtask-title-text-' + currentStatusTarget.id);
                    var badge = document.getElementById('subtask-badge-' + currentStatusTarget.id);
                    if (dot) {
                        dot.className = 'clickup-status-dot ' + (isCompleted ? 'dot-done' : 'dot-todo');
                        dot.innerHTML = isCompleted ? '<i class="bi bi-check text-white"></i>' : '';
                    }
                    if (titleEl) {
                        if (isCompleted) titleEl.classList.add('text-decoration-line-through', 'text-muted');
                        else titleEl.classList.remove('text-decoration-line-through', 'text-muted');
                    }
                    if (badge) {
                        badge.className = 'badge ' + (isCompleted ? 'bg-success-subtle text-success border border-success-subtle' : 'bg-light text-secondary border') + ' rounded-pill px-1-5 py-0 fs-9';
                        badge.textContent = isCompleted ? 'DONE' : 'TODO';
                    }
                    if (data.data && data.data.parentStatus && data.data.parentStatus === 'DONE') {
                        setTimeout(function() { window.location.reload(); }, 500);
                    }
                } else {
                    if (window.showToast) window.showToast(data.message, 'error');
                    else alert(data.message);
                }
            })
            .catch(function(err) {
                console.error(err);
                if (window.showToast) window.showToast('Lỗi khi cập nhật trạng thái việc con!', 'error');
            });
        } else {
            params.append('action', 'updateStatus');
            params.append('projectId', '${project.id}');
            params.append('taskId', currentStatusTarget.id);
            params.append('newStatus', newStatus);
            params.append('ajax', 'true');

            fetch(basePath + '/task', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: params.toString()
            })
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data.success) {
                    if (window.showToast) window.showToast(data.message, 'success');
                    if ((newStatus === 'DONE' || newStatus === 'IN_PROGRESS') && window.fireConfettiCelebration) {
                        window.fireConfettiCelebration();
                    }
                    setTimeout(function() { window.location.reload(); }, 400);
                } else {
                    if (window.showToast) window.showToast(data.message, 'error');
                    else alert(data.message);
                }
            })
            .catch(function(err) {
                console.error(err);
                if (window.showToast) window.showToast('Lỗi khi cập nhật trạng thái công việc!', 'error');
            });
        }
    };

    // 5.3. Quick Add Parent Task
    window.showInlineCreateTask = function(groupId) {
        var icon = document.getElementById('chevron-' + groupId);
        if (icon && icon.classList.contains('bi-chevron-right')) {
            window.toggleClickUpGroup(groupId);
        }
        var row = document.getElementById('inline-task-row-' + groupId);
        if (row) {
            row.classList.remove('d-none');
            var input = document.getElementById('inline-task-title-' + groupId);
            if (input) {
                input.focus();
                input.select();
            }
        }
    };

    window.cancelInlineCreateTask = function(groupId) {
        var row = document.getElementById('inline-task-row-' + groupId);
        if (row) {
            row.classList.add('d-none');
            var input = document.getElementById('inline-task-title-' + groupId);
            if (input) input.value = '';
        }
    };

    window.handleInlineTaskKey = function(event, groupId) {
        if (event.key === 'Enter') {
            event.preventDefault();
            window.submitInlineCreateTask(groupId);
        } else if (event.key === 'Escape') {
            event.preventDefault();
            window.cancelInlineCreateTask(groupId);
        }
    };

    window.submitInlineCreateTask = function(groupId) {
        if (groupId === 'done') {
            if (window.showToast) window.showToast('Không thể tạo trực tiếp công việc ở trạng thái DONE!', 'error');
            return;
        }
        var titleEl = document.getElementById('inline-task-title-' + groupId);
        var assigneeEl = document.getElementById('inline-task-assignee-' + groupId);
        var priorityEl = document.getElementById('inline-task-priority-' + groupId);
        var dueEl = document.getElementById('inline-task-due-' + groupId);

        var title = titleEl ? titleEl.value.trim() : '';
        if (!title) {
            if (window.showToast) window.showToast('Vui lòng nhập tiêu đề công việc!', 'error');
            else alert('Vui lòng nhập tiêu đề công việc!');
            if (titleEl) titleEl.focus();
            return;
        }

        var statusMap = { 'done': 'DONE', 'inprog': 'IN_PROGRESS', 'todo': 'TODO' };
        var status = statusMap[groupId] || 'TODO';
        var assigneeId = assigneeEl ? assigneeEl.value : '0';
        var priority = priorityEl ? priorityEl.value : 'MEDIUM';
        var dueDate = dueEl ? dueEl.value : '';

        var params = new URLSearchParams();
        params.append('action', 'quickAddParentTask');
        params.append('projectId', '${project.id}');
        params.append('status', status);
        params.append('title', title);
        params.append('priority', priority);
        params.append('assigneeId', assigneeId);
        params.append('dueDate', dueDate);
        params.append('ajax', 'true');

        var basePath = window.location.pathname.startsWith('/teamwork-hub') ? '/teamwork-hub' : '';
        fetch(basePath + '/task', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: params.toString()
        })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data.success) {
                if (window.showToast) window.showToast(data.message, 'success');
                window.cancelInlineCreateTask(groupId);
                setTimeout(function() { window.location.reload(); }, 400);
            } else {
                if (window.showToast) window.showToast(data.message || 'Lỗi khi tạo công việc', 'error');
                else alert(data.message);
            }
        })
        .catch(function(err) {
            console.error(err);
            if (window.showToast) window.showToast('Đã có lỗi xảy ra trong quá trình gửi yêu cầu!', 'error');
        });
    };

    // 5.4. Quick Add Subtask
    window.showInlineCreateSubtask = function(parentTaskId, event) {
        if (event) event.stopPropagation();

        var parentRow = document.querySelector('tr[data-task-id="' + parentTaskId + '"]');
        if (!parentRow) return;
        if (parentRow.classList.contains('group-done-row')) {
            if (window.showToast) window.showToast('Không thể thêm việc con vào công việc đã hoàn thành (DONE)!', 'warning');
            return;
        }

        var subRows = document.querySelectorAll('.clickup-subtask-row[data-parent-id="' + parentTaskId + '"]');
        subRows.forEach(function(r) { r.classList.remove('d-none'); });
        var caret = document.getElementById('caret-' + parentTaskId);
        var caretIcon = caret ? caret.querySelector('i') : document.getElementById('subtask-caret-icon-' + parentTaskId);
        if (caretIcon) caretIcon.className = 'bi bi-chevron-down';
        if (caret) caret.classList.add('is-expanded');

        var existingRow = document.getElementById('inline-subtask-row-' + parentTaskId);
        if (existingRow) {
            existingRow.classList.remove('d-none');
            var input = document.getElementById('inline-subtask-title-' + parentTaskId);
            if (input) { input.focus(); input.select(); }
            return;
        }

        var groupClass = '';
        parentRow.classList.forEach(function(cls) {
            if (cls.startsWith('group-') && cls.endsWith('-row')) {
                groupClass = cls;
            }
        });

        var lastTarget = parentRow;
        if (subRows && subRows.length > 0) {
            lastTarget = subRows[subRows.length - 1];
        }

        var memberOpts = '<option value="0">Chưa gán</option>';
        if (window.projectMembersList && window.projectMembersList.length > 0) {
            window.projectMembersList.forEach(function(m) {
                memberOpts += '<option value="' + m.id + '">' + m.name + '</option>';
            });
        }

        var assigneeTd = ${project.teamProject} ? (
            '<td>' +
                '<select id="inline-subtask-assignee-' + parentTaskId + '" class="form-select form-select-sm py-0 fs-8" style="max-width: 120px;">' +
                    memberOpts +
                '</select>' +
            '</td>'
        ) : '';

        var newRow = document.createElement('tr');
        newRow.id = 'inline-subtask-row-' + parentTaskId;
        newRow.className = 'clickup-inline-subtask-row clickup-inline-create-row ' + groupClass;
        newRow.innerHTML = 
            '<td>' +
                '<div class="d-flex align-items-center gap-2 ps-4" style="padding-left: 36px !important;">' +
                    '<span class="clickup-status-dot dot-todo"></span>' +
                    '<input type="text" id="inline-subtask-title-' + parentTaskId + '" class="form-control form-control-sm clickup-inline-input fs-8" placeholder="Tên việc con mới (Enter để lưu, Esc để hủy)..." onkeydown="handleInlineSubtaskKey(event, ' + parentTaskId + ')" />' +
                '</div>' +
            '</td>' +
            assigneeTd +
            '<td>' +
                '<span class="fs-9 text-muted">Subtask</span>' +
            '</td>' +
            '<td>' +
                '<span class="badge bg-light text-secondary border rounded-pill px-1-5 py-0 fs-9">TODO</span>' +
            '</td>' +
            '<td style="text-align: right;">' +
                '<button type="button" class="btn btn-sm btn-primary py-0 px-2 fs-8 me-1" onclick="submitInlineCreateSubtask(' + parentTaskId + ')"><i class="bi bi-check-lg"></i> Lưu</button>' +
                '<button type="button" class="btn btn-sm btn-light border py-0 px-2 fs-8" onclick="cancelInlineCreateSubtask(' + parentTaskId + ')"><i class="bi bi-x-lg"></i></button>' +
            '</td>';

        lastTarget.parentNode.insertBefore(newRow, lastTarget.nextSibling);

        var titleInput = document.getElementById('inline-subtask-title-' + parentTaskId);
        if (titleInput) {
            titleInput.focus();
        }
    };

    window.cancelInlineCreateSubtask = function(parentTaskId) {
        var row = document.getElementById('inline-subtask-row-' + parentTaskId);
        if (row) {
            row.remove();
        }
    };

    window.handleInlineSubtaskKey = function(event, parentTaskId) {
        if (event.key === 'Enter') {
            event.preventDefault();
            window.submitInlineCreateSubtask(parentTaskId);
        } else if (event.key === 'Escape') {
            event.preventDefault();
            window.cancelInlineCreateSubtask(parentTaskId);
        }
    };

    window.submitInlineCreateSubtask = function(parentTaskId) {
        var titleEl = document.getElementById('inline-subtask-title-' + parentTaskId);
        var assigneeEl = document.getElementById('inline-subtask-assignee-' + parentTaskId);

        var title = titleEl ? titleEl.value.trim() : '';
        if (!title) {
            if (window.showToast) window.showToast('Vui lòng nhập tiêu đề việc con!', 'error');
            else alert('Vui lòng nhập tiêu đề việc con!');
            if (titleEl) titleEl.focus();
            return;
        }

        var assigneeId = assigneeEl ? assigneeEl.value : (${project.soloProject} ? '${sessionScope.currentUser.id}' : '0');

        var params = new URLSearchParams();
        params.append('action', 'quickAddSubTask');
        params.append('projectId', '${project.id}');
        params.append('taskId', parentTaskId);
        params.append('title', title);
        params.append('assigneeId', assigneeId);
        params.append('dueDate', '');
        params.append('ajax', 'true');

        var basePath = window.location.pathname.startsWith('/teamwork-hub') ? '/teamwork-hub' : '';
        fetch(basePath + '/task', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: params.toString()
        })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data.success) {
                if (window.showToast) window.showToast(data.message, 'success');
                window.cancelInlineCreateSubtask(parentTaskId);
                setTimeout(function() { window.location.reload(); }, 400);
            } else {
                if (window.showToast) window.showToast(data.message || 'Lỗi khi tạo việc con', 'error');
                else alert(data.message);
            }
        })
        .catch(function(err) {
            console.error(err);
            if (window.showToast) window.showToast('Đã có lỗi xảy ra trong quá trình gửi yêu cầu!', 'error');
        });
    };

    // Close status popover when clicking anywhere outside or scrolling
    document.addEventListener('click', function(e) {
        var popover = document.getElementById('clickupStatusPopover');
        if (popover && (popover.classList.contains('show') || popover.style.display === 'block')) {
            if (!popover.contains(e.target) && !e.target.closest('.clickup-status-dot')) {
                popover.classList.remove('show');
                popover.classList.add('d-none');
                popover.style.display = 'none';
            }
        }
    });

    window.addEventListener('scroll', function() {
        var popover = document.getElementById('clickupStatusPopover');
        if (popover && (popover.classList.contains('show') || popover.style.display === 'block')) {
            popover.classList.remove('show');
            popover.classList.add('d-none');
            popover.style.display = 'none';
        }
    }, true);

    // Restore group collapse states from localStorage on DOM ready
    document.addEventListener('DOMContentLoaded', function() {
        ['done', 'inprog', 'todo'].forEach(function(gid) {
            try {
                var isCollapsed = localStorage.getItem('clickup_group_' + gid + '_collapsed');
                if (isCollapsed === 'true') {
                    var rows = document.querySelectorAll('.group-' + gid + '-row');
                    rows.forEach(function(r) { r.classList.add('d-none'); });
                    var icon = document.getElementById('chevron-' + gid);
                    if (icon) icon.className = 'bi bi-chevron-right me-1';
                }
            } catch(e) {}
        });
    });

    // 6. Search tasks across all views
    window.searchClickUpTasks = function(query) {
        var q = (query || '').toLowerCase().trim();

        // A. In List View
        var parentRows = document.querySelectorAll('.clickup-task-row');
        parentRows.forEach(function(pRow) {
            var title = (pRow.getAttribute('data-task-title') || pRow.innerText || '').toLowerCase();
            var taskId = pRow.getAttribute('data-task-id');
            var subRows = taskId ? document.querySelectorAll('.clickup-subtask-row[data-parent-id="' + taskId + '"]') : [];
            var matchParent = !q || title.indexOf(q) > -1;
            var matchAnySub = false;

            subRows.forEach(function(sRow) {
                var sTitle = (sRow.getAttribute('data-subtask-title') || sRow.innerText || '').toLowerCase();
                var sMatch = !q || sTitle.indexOf(q) > -1;
                if (sMatch) matchAnySub = true;
                sRow.style.display = (matchParent || sMatch) ? '' : 'none';
            });

            pRow.style.display = (matchParent || matchAnySub) ? '' : 'none';
        });

        // B. In Board View
        var kanbanCards = document.querySelectorAll('.kanban-card');
        kanbanCards.forEach(function(card) {
            var title = (card.getAttribute('data-task-title') || card.innerText || '').toLowerCase();
            card.style.display = (!q || title.indexOf(q) > -1) ? '' : 'none';
        });

        // C. In Schedule View
        var schedCards = document.querySelectorAll('.schedule-card');
        schedCards.forEach(function(card) {
            var text = (card.innerText || '').toLowerCase();
            card.style.display = (!q || text.indexOf(q) > -1) ? '' : 'none';
        });
    };

    // 7. Filter tasks by Member, Status, or Scope (With Active Feedback Banner & Metric Sync)
    var defaultProjectMetrics = {
        total: ${mTotalCount},
        todo: ${mTodoCount},
        inprog: ${mInProgCount},
        done: ${mDoneCount},
        pctTodo: ${mPctTodo},
        pctInProg: ${mPctInProg},
        pctDone: ${mPctDone},
        overdue: ${mOverdueCount},
        memberCount: ${memberCount}
    };

    window.filterClickUpTasks = function(mode, userId, userName) {
        document.querySelectorAll('.clickup-sidebar .clickup-nav-link').forEach(function(el) {
            el.classList.remove('active');
        });
        document.querySelectorAll('.assignee-avatar-btn, .assignee-filter-btn, .status-filter-btn').forEach(function(b) {
            b.classList.remove('active');
        });

        var currentUserId = "${sessionScope.currentUser.id}";
        var filterTargetUserId = null;
        var filterTargetStatus = null;
        var banner = document.getElementById('activeFilterBanner');
        var bannerText = document.getElementById('activeFilterText');

        if (mode === 'SCHEDULE') {
            window.switchClickUpTab('schedule');
            var navSched = document.getElementById('nav-schedule');
            if (navSched) navSched.classList.add('active');
            if (banner) { banner.classList.add('d-none'); banner.classList.remove('d-flex'); }
            return;
        }

        // Chuyển tab tasks chỉ khi không ở tab Thống kê (cho phép người dùng lọc ngay tại tab Thống kê)
        var activeTab = document.querySelector('.clickup-tab-link.active');
        var activeTabId = activeTab ? activeTab.id : '';
        if (activeTabId !== 'tab-btn-metrics' && activeTabId !== 'tab-btn-tasks') {
            window.switchClickUpTab('tasks');
        }

        if (mode === 'ALL') {
            var allBtn = document.getElementById('filter-all-btn');
            if (allBtn) allBtn.classList.add('active');
            var tbAll = document.getElementById('assignee-btn-all');
            if (tbAll) tbAll.classList.add('active');
            if (banner) { banner.classList.add('d-none'); banner.classList.remove('d-flex'); }
            filterTargetUserId = null;
            filterTargetStatus = null;
        } else if (mode === 'MY_TASKS') {
            var myBtn = document.getElementById('nav-mytasks');
            if (myBtn) myBtn.classList.add('active');
            var tbMe = document.getElementById('assignee-btn-me');
            if (tbMe) tbMe.classList.add('active');
            filterTargetUserId = currentUserId;
            if (banner && bannerText) {
                bannerText.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Đang lọc: <strong>Công việc của tôi</strong>';
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
        } else if (mode === 'USER') {
            var memberItem = document.querySelector('.member-filter-item[data-user-id="' + userId + '"]');
            if (memberItem) memberItem.classList.add('active');
            var tbAvatar = document.querySelector('.assignee-avatar-btn[data-user-id="' + userId + '"]');
            if (tbAvatar) tbAvatar.classList.add('active');
            filterTargetUserId = userId;
            if (banner && bannerText) {
                bannerText.innerHTML = '<i class="bi bi-person me-1"></i> Đang lọc theo thành viên: <strong>' + (userName || 'Thành viên') + '</strong>';
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
        } else if (mode === 'STATUS_DONE') {
            filterTargetStatus = 'DONE';
            if (banner && bannerText) {
                bannerText.innerHTML = '<i class="bi bi-check-circle-fill text-success me-1"></i> Đang lọc: <strong>Công việc đã nghiệm thu (DONE)</strong>';
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
        } else if (mode === 'STATUS_IN_PROGRESS') {
            filterTargetStatus = 'IN_PROGRESS';
            if (banner && bannerText) {
                bannerText.innerHTML = '<i class="bi bi-play-circle-fill text-primary me-1"></i> Đang lọc: <strong>Công việc đang thực hiện (IN PROGRESS)</strong>';
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
        } else if (mode === 'STATUS_TODO') {
            filterTargetStatus = 'TODO';
            if (banner && bannerText) {
                bannerText.innerHTML = '<i class="bi bi-circle text-secondary me-1"></i> Đang lọc: <strong>Công việc chờ thực hiện (TO DO)</strong>';
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
        } else if (mode === 'STATUS_SUBMITTED') {
            filterTargetStatus = 'SUBMITTED';
            var tbSub = document.getElementById('assignee-btn-submitted');
            if (tbSub) tbSub.classList.add('active');
            if (banner && bannerText) {
                bannerText.innerHTML = '<i class="bi bi-send-check text-purple me-1"></i> Đang lọc: <strong>Công việc chờ PM duyệt (SUBMITTED)</strong>';
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
        }

        // Đồng bộ KPI tổng quan, Donut Chart và Bảng Workload
        if (typeof updateMetricsAndWorkloadForUser === 'function') {
            if (mode === 'ALL') {
                updateMetricsAndWorkloadForUser(null, null);
            } else if (mode === 'MY_TASKS') {
                updateMetricsAndWorkloadForUser(currentUserId, 'Việc của tôi');
            } else if (mode === 'USER') {
                updateMetricsAndWorkloadForUser(userId, userName);
            }
        }

        // List View filtering
        var count = 0;
        var parentRows = document.querySelectorAll('.clickup-task-row');
        parentRows.forEach(function(pRow) {
            var assigneeId = pRow.getAttribute('data-assignee-id');
            var taskId = pRow.getAttribute('data-task-id');
            var taskStatus = pRow.getAttribute('data-task-status');
            var subRows = taskId ? document.querySelectorAll('.clickup-subtask-row[data-parent-id="' + taskId + '"]') : [];

            var show = false;
            if (filterTargetStatus) {
                if (filterTargetStatus === 'DONE' && pRow.classList.contains('group-done-row')) show = true;
                else if (filterTargetStatus === 'IN_PROGRESS' && pRow.classList.contains('group-inprog-row')) show = true;
                else if (filterTargetStatus === 'TODO' && pRow.classList.contains('group-todo-row')) show = true;
                else if (filterTargetStatus === 'SUBMITTED' && taskStatus === 'SUBMITTED') show = true;

                subRows.forEach(function(sRow) {
                    sRow.style.display = show ? '' : 'none';
                });
            } else if (filterTargetUserId) {
                var matchParent = assigneeId == filterTargetUserId;
                var matchSub = false;

                subRows.forEach(function(sRow) {
                    var sAssigneeId = sRow.getAttribute('data-assignee-id');
                    var sMatch = sAssigneeId == filterTargetUserId || matchParent;
                    if (sAssigneeId == filterTargetUserId) matchSub = true;
                    sRow.style.display = sMatch ? '' : 'none';
                });

                show = matchParent || matchSub;
            } else {
                show = true;
                subRows.forEach(function(sRow) {
                    sRow.style.display = '';
                });
            }

            if (show) count++;
            pRow.style.display = show ? '' : 'none';
        });

        // Board View filtering
        var kanbanCards = document.querySelectorAll('.kanban-card');
        kanbanCards.forEach(function(card) {
            var show = true;
            if (filterTargetUserId) {
                var assigneeId = card.getAttribute('data-assignee-id');
                show = assigneeId == filterTargetUserId;
            } else if (filterTargetStatus) {
                var col = card.closest('.kanban-col-todo, .kanban-col-in-progress, .kanban-col-done');
                var cardStatus = card.getAttribute('data-task-status');
                if (filterTargetStatus === 'DONE') show = col && col.classList.contains('kanban-col-done');
                else if (filterTargetStatus === 'IN_PROGRESS') show = col && col.classList.contains('kanban-col-in-progress');
                else if (filterTargetStatus === 'TODO') show = col && col.classList.contains('kanban-col-todo');
                else if (filterTargetStatus === 'SUBMITTED') show = cardStatus === 'SUBMITTED';
            }
            card.style.display = show ? '' : 'none';
        });

        if ((filterTargetUserId || filterTargetStatus) && bannerText) {
            bannerText.innerHTML += ' <span class="badge bg-primary text-white rounded-pill ms-1">' + count + ' việc</span>';
        }
    };

    // ClickUp 3.0 Avatar click handler with toggle behavior
    window.handleToolbarAvatarClick = function(userId, userName) {
        var btn = document.querySelector('.assignee-avatar-btn[data-user-id="' + userId + '"]');
        if (btn && btn.classList.contains('active')) {
            // Toggle off -> reset to ALL
            window.filterClickUpTasks('ALL');
        } else {
            window.filterClickUpTasks('USER', userId, userName);
        }
    };

    // Cập nhật số liệu KPI 4 Stat Cards, Donut Chart và làm mờ hàng Bảng Workload
    window.updateMetricsAndWorkloadForUser = function(userId, userName) {
        var totalEl = document.getElementById('metric-total-count');
        var todoEl = document.getElementById('metric-todo-count');
        var inprogEl = document.getElementById('metric-inprog-count');
        var doneEl = document.getElementById('metric-done-count');
        
        var totalPill = document.getElementById('metric-total-pill');
        var todoPill = document.getElementById('metric-todo-pill');
        var inprogPill = document.getElementById('metric-inprog-pill');
        var donePill = document.getElementById('metric-done-pill');

        var segDone = document.getElementById('metric-total-seg-done');
        var segInprog = document.getElementById('metric-total-seg-inprog');
        var segTodo = document.getElementById('metric-total-seg-todo');

        var barTodo = document.getElementById('metric-todo-bar');
        var barInprog = document.getElementById('metric-inprog-bar');
        var barDone = document.getElementById('metric-done-bar');

        var totalFooterLabel = document.getElementById('metric-total-footer-label');
        var totalFooterVal = document.getElementById('metric-total-footer-val');
        var todoFooterVal = document.getElementById('metric-todo-footer-val');
        var inprogFooterVal = document.getElementById('metric-inprog-footer-val');
        var doneFooterVal = document.getElementById('metric-done-footer-val');

        var donutSubtitle = document.getElementById('donut-chart-subtitle');
        var donutBadge = document.getElementById('donut-filter-badge');
        var donutCenterTotal = document.getElementById('donut-center-total');
        var donutCenterPct = document.getElementById('donut-center-pct');

        var segDonutDone = document.getElementById('donut-segment-done');
        var segDonutInprog = document.getElementById('donut-segment-inprog');
        var segDonutTodo = document.getElementById('donut-segment-todo');

        var legDone = document.getElementById('donut-legend-done');
        var legInprog = document.getElementById('donut-legend-inprog');
        var legTodo = document.getElementById('donut-legend-todo');
        var legOverdue = document.getElementById('donut-legend-overdue');

        var C = 364.4; // Chu vi vòng tròn bán kính 58

        if (!userId) {
            // Khôi phục toàn bộ dự án
            if (totalEl) totalEl.textContent = defaultProjectMetrics.total;
            if (todoEl) todoEl.textContent = defaultProjectMetrics.todo;
            if (inprogEl) inprogEl.textContent = defaultProjectMetrics.inprog;
            if (doneEl) doneEl.textContent = defaultProjectMetrics.done;

            if (totalPill) totalPill.innerHTML = '<i class="bi bi-pie-chart-fill me-1"></i>100%';
            if (todoPill) todoPill.innerHTML = '<i class="bi bi-hourglass-split me-1"></i>' + defaultProjectMetrics.pctTodo + '%';
            if (inprogPill) inprogPill.innerHTML = '<i class="bi bi-lightning-charge-fill me-1"></i>' + defaultProjectMetrics.pctInProg + '%';
            if (donePill) donePill.innerHTML = '<i class="bi bi-check-circle-fill me-1"></i>' + defaultProjectMetrics.pctDone + '%';

            if (segDone) segDone.style.width = defaultProjectMetrics.pctDone + '%';
            if (segInprog) segInprog.style.width = defaultProjectMetrics.pctInProg + '%';
            if (segTodo) segTodo.style.width = defaultProjectMetrics.pctTodo + '%';

            if (barTodo) barTodo.style.width = defaultProjectMetrics.pctTodo + '%';
            if (barInprog) barInprog.style.width = defaultProjectMetrics.pctInProg + '%';
            if (barDone) barDone.style.width = defaultProjectMetrics.pctDone + '%';

            if (totalFooterLabel) totalFooterLabel.innerHTML = '<i class="bi bi-people-fill me-1 text-secondary"></i>' + defaultProjectMetrics.memberCount + ' thành viên';
            if (totalFooterVal) totalFooterVal.textContent = defaultProjectMetrics.total + ' việc';
            if (todoFooterVal) todoFooterVal.textContent = defaultProjectMetrics.todo + '/' + defaultProjectMetrics.total + ' việc';
            if (inprogFooterVal) inprogFooterVal.textContent = defaultProjectMetrics.inprog + '/' + defaultProjectMetrics.total + ' việc';
            if (doneFooterVal) doneFooterVal.textContent = defaultProjectMetrics.done + '/' + defaultProjectMetrics.total + ' việc';

            // Donut Chart reset
            if (donutSubtitle) donutSubtitle.textContent = 'Toàn bộ dự án (' + defaultProjectMetrics.total + ' việc)';
            if (donutBadge) { donutBadge.textContent = 'Tất cả'; donutBadge.className = 'badge bg-light text-secondary border rounded-pill fs-9'; }
            if (donutCenterTotal) donutCenterTotal.textContent = defaultProjectMetrics.total;
            if (donutCenterPct) donutCenterPct.textContent = defaultProjectMetrics.pctDone + '% Xong';

            var dDone = defaultProjectMetrics.total > 0 ? (defaultProjectMetrics.done * C / defaultProjectMetrics.total) : 0;
            var dInprog = defaultProjectMetrics.total > 0 ? (defaultProjectMetrics.inprog * C / defaultProjectMetrics.total) : 0;
            var dTodo = defaultProjectMetrics.total > 0 ? (defaultProjectMetrics.todo * C / defaultProjectMetrics.total) : 0;

            if (segDonutDone) { segDonutDone.style.strokeDasharray = dDone + ' ' + C; segDonutDone.style.strokeDashoffset = '0'; }
            if (segDonutInprog) { segDonutInprog.style.strokeDasharray = dInprog + ' ' + C; segDonutInprog.style.strokeDashoffset = -dDone; }
            if (segDonutTodo) { segDonutTodo.style.strokeDasharray = dTodo + ' ' + C; segDonutTodo.style.strokeDashoffset = -(dDone + dInprog); }

            if (legDone) legDone.innerHTML = defaultProjectMetrics.done + ' <span class="fs-9 text-muted fw-normal">(' + defaultProjectMetrics.pctDone + '%)</span>';
            if (legInprog) legInprog.innerHTML = defaultProjectMetrics.inprog + ' <span class="fs-9 text-muted fw-normal">(' + defaultProjectMetrics.pctInProg + '%)</span>';
            if (legTodo) legTodo.innerHTML = defaultProjectMetrics.todo + ' <span class="fs-9 text-muted fw-normal">(' + defaultProjectMetrics.pctTodo + '%)</span>';
            if (legOverdue) legOverdue.textContent = defaultProjectMetrics.overdue + ' việc';

            // Bỏ làm mờ tất cả các hàng trong bảng Workload
            document.querySelectorAll('#workloadTable tbody tr').forEach(function(row) {
                row.classList.remove('workload-row-active', 'workload-row-dimmed');
            });
            return;
        }

        // Tìm dữ liệu của thành viên từ dòng bảng Workload
        var targetRow = document.querySelector('#workloadTable tbody tr[data-user-id="' + userId + '"]');
        var uTotal = 0, uDone = 0, uInprog = 0, uOverdue = 0, uRate = 0, uTodo = 0;
        var displayName = userName || 'Thành viên';

        if (targetRow) {
            uTotal = parseInt(targetRow.getAttribute('data-total')) || 0;
            uDone = parseInt(targetRow.getAttribute('data-done')) || 0;
            uInprog = parseInt(targetRow.getAttribute('data-inprog')) || 0;
            uOverdue = parseInt(targetRow.getAttribute('data-overdue')) || 0;
            uRate = parseInt(targetRow.getAttribute('data-rate')) || 0;
            uTodo = Math.max(0, uTotal - uDone - uInprog);
            displayName = targetRow.getAttribute('data-name') || displayName;
        }

        var uPctDone = uTotal > 0 ? Math.round((uDone * 100) / uTotal) : 0;
        var uPctInprog = uTotal > 0 ? Math.round((uInprog * 100) / uTotal) : 0;
        var uPctTodo = uTotal > 0 ? (100 - uPctDone - uPctInprog) : 0;
        if (uPctTodo < 0) uPctTodo = 0;

        // Cập nhật 4 Stat Cards
        if (totalEl) totalEl.textContent = uTotal;
        if (todoEl) todoEl.textContent = uTodo;
        if (inprogEl) inprogEl.textContent = uInprog;
        if (doneEl) doneEl.textContent = uDone;

        if (totalPill) totalPill.innerHTML = '<i class="bi bi-person-check-fill me-1"></i>100%';
        if (todoPill) todoPill.innerHTML = '<i class="bi bi-hourglass-split me-1"></i>' + uPctTodo + '%';
        if (inprogPill) inprogPill.innerHTML = '<i class="bi bi-lightning-charge-fill me-1"></i>' + uPctInprog + '%';
        if (donePill) donePill.innerHTML = '<i class="bi bi-check-circle-fill me-1"></i>' + uPctDone + '%';

        if (segDone) segDone.style.width = uPctDone + '%';
        if (segInprog) segInprog.style.width = uPctInprog + '%';
        if (segTodo) segTodo.style.width = uPctTodo + '%';

        if (barTodo) barTodo.style.width = uPctTodo + '%';
        if (barInprog) barInprog.style.width = uPctInprog + '%';
        if (barDone) barDone.style.width = uPctDone + '%';

        if (totalFooterLabel) totalFooterLabel.innerHTML = '<i class="bi bi-person-badge me-1 text-primary"></i>' + displayName;
        if (totalFooterVal) totalFooterVal.textContent = uTotal + ' việc';
        if (todoFooterVal) todoFooterVal.textContent = uTodo + '/' + uTotal + ' việc';
        if (inprogFooterVal) inprogFooterVal.textContent = uInprog + '/' + uTotal + ' việc';
        if (doneFooterVal) doneFooterVal.textContent = uDone + '/' + uTotal + ' việc';

        // Cập nhật Donut Chart
        if (donutSubtitle) donutSubtitle.textContent = 'Thành viên: ' + displayName;
        if (donutBadge) { donutBadge.textContent = displayName; donutBadge.className = 'badge bg-primary text-white rounded-pill fs-9'; }
        if (donutCenterTotal) donutCenterTotal.textContent = uTotal;
        if (donutCenterPct) donutCenterPct.textContent = uPctDone + '% Xong';

        var duDone = uTotal > 0 ? (uDone * C / uTotal) : 0;
        var duInprog = uTotal > 0 ? (uInprog * C / uTotal) : 0;
        var duTodo = uTotal > 0 ? (uTodo * C / uTotal) : 0;

        if (segDonutDone) { segDonutDone.style.strokeDasharray = duDone + ' ' + C; segDonutDone.style.strokeDashoffset = '0'; }
        if (segDonutInprog) { segDonutInprog.style.strokeDasharray = duInprog + ' ' + C; segDonutInprog.style.strokeDashoffset = -duDone; }
        if (segDonutTodo) { segDonutTodo.style.strokeDasharray = duTodo + ' ' + C; segDonutTodo.style.strokeDashoffset = -(duDone + duInprog); }

        if (legDone) legDone.innerHTML = uDone + ' <span class="fs-9 text-muted fw-normal">(' + uPctDone + '%)</span>';
        if (legInprog) legInprog.innerHTML = uInprog + ' <span class="fs-9 text-muted fw-normal">(' + uPctInprog + '%)</span>';
        if (legTodo) legTodo.innerHTML = uTodo + ' <span class="fs-9 text-muted fw-normal">(' + uPctTodo + '%)</span>';
        if (legOverdue) legOverdue.textContent = uOverdue + ' việc';

        // Bảng Workload: Làm nổi bật dòng được chọn, làm mờ các dòng còn lại
        document.querySelectorAll('#workloadTable tbody tr').forEach(function(row) {
            if (row.getAttribute('data-user-id') == userId) {
                row.classList.add('workload-row-active');
                row.classList.remove('workload-row-dimmed');
            } else {
                row.classList.remove('workload-row-active');
                row.classList.add('workload-row-dimmed');
            }
        });
    };

    // Xử lý khi click vào một hàng trong Bảng Workload
    window.handleWorkloadRowClick = function(userId, userName) {
        var activeAvatar = document.querySelector('.assignee-avatar-btn[data-user-id="' + userId + '"].active');
        if (activeAvatar) {
            window.filterClickUpTasks('ALL');
        } else {
            window.filterClickUpTasks('USER', userId, userName);
        }
    };

    // Sắp xếp nhanh các cột trong Bảng Workload
    var currentSortCol = -1;
    var currentSortAsc = true;

    window.sortWorkloadTable = function(colIdx) {
        var table = document.getElementById('workloadTable');
        if (!table) return;
        var tbody = table.querySelector('tbody');
        if (!tbody) return;
        var rows = Array.from(tbody.querySelectorAll('tr.workload-row'));

        if (currentSortCol === colIdx) {
            currentSortAsc = !currentSortAsc;
        } else {
            currentSortCol = colIdx;
            currentSortAsc = false; // Mặc định giảm dần với các chỉ số số học
            if (colIdx === 0) currentSortAsc = true; // A-Z với tên
        }

        // Cập nhật icon sort
        for (var i = 0; i <= 7; i++) {
            var icon = document.getElementById('sort-icon-' + i);
            if (icon) {
                if (i === colIdx) {
                    icon.className = currentSortAsc ? 'bi bi-sort-up sort-icon text-primary opacity-100' : 'bi bi-sort-down sort-icon text-primary opacity-100';
                } else {
                    icon.className = 'bi bi-arrow-down-up sort-icon';
                }
            }
        }

        var capRank = { 'overload': 3, 'busy': 2, 'optimal': 1, 'ready': 0 };

        rows.sort(function(a, b) {
            var valA, valB;
            if (colIdx === 0) {
                valA = (a.getAttribute('data-name') || '').toLowerCase();
                valB = (b.getAttribute('data-name') || '').toLowerCase();
                return currentSortAsc ? valA.localeCompare(valB, 'vi') : valB.localeCompare(valA, 'vi');
            } else if (colIdx === 1) {
                valA = capRank[a.getAttribute('data-capacity')] || 0;
                valB = capRank[b.getAttribute('data-capacity')] || 0;
            } else if (colIdx === 2) {
                valA = parseFloat(a.getAttribute('data-total')) || 0;
                valB = parseFloat(b.getAttribute('data-total')) || 0;
            } else if (colIdx === 3) {
                valA = parseFloat(a.getAttribute('data-todo')) || 0;
                valB = parseFloat(b.getAttribute('data-todo')) || 0;
            } else if (colIdx === 4) {
                valA = parseFloat(a.getAttribute('data-inprog')) || 0;
                valB = parseFloat(b.getAttribute('data-inprog')) || 0;
            } else if (colIdx === 5) {
                valA = parseFloat(a.getAttribute('data-done')) || 0;
                valB = parseFloat(b.getAttribute('data-done')) || 0;
            } else if (colIdx === 6) {
                valA = parseFloat(a.getAttribute('data-overdue')) || 0;
                valB = parseFloat(b.getAttribute('data-overdue')) || 0;
            } else if (colIdx === 7) {
                valA = parseFloat(a.getAttribute('data-rate')) || 0;
                valB = parseFloat(b.getAttribute('data-rate')) || 0;
            }

            return currentSortAsc ? (valA - valB) : (valB - valA);
        });

        rows.forEach(function(row) {
            tbody.appendChild(row);
        });
    };

    // Lọc nhanh theo tình trạng công suất tải việc (Workload Capacity Filters)
    window.filterWorkloadCapacity = function(type, tabEl) {
        var tabs = document.querySelectorAll('.workload-capacity-filter-btn');
        tabs.forEach(function(t) { t.classList.remove('active'); });
        if (tabEl) tabEl.classList.add('active');

        var rows = document.querySelectorAll('#workloadTable tbody tr.workload-row');
        var matched = 0;
        rows.forEach(function(r) {
            var cap = r.getAttribute('data-capacity') || '';
            var overdue = parseInt(r.getAttribute('data-overdue') || '0', 10);
            var inprog = parseInt(r.getAttribute('data-inprog') || '0', 10);

            var show = false;
            if (type === 'ALL') {
                show = true;
            } else if (type === 'OVERLOAD') {
                show = (cap === 'overload' || overdue > 0);
            } else if (type === 'BUSY') {
                show = (cap === 'busy' || inprog >= 3);
            } else if (type === 'OPTIMAL') {
                show = (cap === 'optimal' || cap === 'ready');
            }

            r.style.display = show ? '' : 'none';
            if (show) matched++;
        });

        var countBadge = document.getElementById('workload-member-count');
        if (countBadge) {
            countBadge.textContent = (type === 'ALL') ? (rows.length + ' thành viên') : (matched + '/' + rows.length + ' thành viên');
        }
    };

    // Chuyển nhanh sang tab Tasks và lọc trực tiếp theo thành viên
    window.viewMemberTasks = function(userId, userName, event) {
        if (event) event.stopPropagation();
        switchClickUpTab('tasks');
        if (typeof filterClickUpTasks === 'function') {
            filterClickUpTasks('USER', userId, userName);
        }
    };

    // Tìm kiếm nhanh thành viên trong Bảng Workload
    window.filterWorkloadTable = function(query) {
        var q = (query || '').trim().toLowerCase();
        var rows = document.querySelectorAll('#workloadTable tbody tr.workload-row');
        var matched = 0;
        rows.forEach(function(r) {
            var name = (r.getAttribute('data-name') || '').toLowerCase();
            var isMatch = !q || name.indexOf(q) > -1;
            r.style.display = isMatch ? '' : 'none';
            if (isMatch) matched++;
        });
        var countBadge = document.getElementById('workload-member-count');
        if (countBadge) {
            countBadge.textContent = q ? (matched + '/' + rows.length + ' người') : (rows.length + ' thành viên');
        }
    };

    // Lọc theo khoảng thời gian hạn chót
    window.filterTasksByTimeframe = function(timeframe, label) {
        var labelEl = document.getElementById('currentTimeframeLabel');
        if (labelEl) labelEl.textContent = label;

        // Cập nhật active trong dropdown
        var dropdownEl = document.getElementById('timeframeDropdownBtn');
        if (dropdownEl && dropdownEl.parentElement) {
            var menu = dropdownEl.parentElement.querySelector('.dropdown-menu');
            if (menu) {
                menu.querySelectorAll('.dropdown-item').forEach(function(item) {
                    if (item.textContent.indexOf(label) > -1) item.classList.add('active');
                    else item.classList.remove('active');
                });
            }
        }

        // Lọc trong danh sách List View & Board View
        var parentRows = document.querySelectorAll('.clickup-task-row');
        var now = new Date();
        var startOfWeek = new Date(now);
        startOfWeek.setDate(now.getDate() - now.getDay());
        var endOfWeek = new Date(now);
        endOfWeek.setDate(startOfWeek.getDate() + 6);

        var matchCount = 0;
        parentRows.forEach(function(pRow) {
            var taskId = pRow.getAttribute('data-task-id');
            var subRows = taskId ? document.querySelectorAll('.clickup-subtask-row[data-parent-id="' + taskId + '"]') : [];
            var lastTd = pRow.querySelector('td:last-child');
            var dueDateStr = lastTd ? lastTd.textContent.trim() : '';

            var show = true;
            if (timeframe === 'ALL') {
                show = true;
            } else if (timeframe === 'OVERDUE') {
                var isOverdue = pRow.querySelector('.badge.bg-danger') || (dueDateStr && dueDateStr !== '—' && new Date(dueDateStr) < now && !pRow.classList.contains('group-done-row'));
                show = !!isOverdue;
            } else if (timeframe === 'THIS_WEEK' || timeframe === 'THIS_MONTH') {
                if (!dueDateStr || dueDateStr === '—') {
                    show = false;
                } else {
                    var d = new Date(dueDateStr);
                    if (isNaN(d.getTime())) {
                        show = true;
                    } else if (timeframe === 'THIS_WEEK') {
                        show = (d >= startOfWeek && d <= endOfWeek);
                    } else if (timeframe === 'THIS_MONTH') {
                        show = (d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear());
                    }
                }
            }

            pRow.style.display = show ? '' : 'none';
            subRows.forEach(function(sRow) {
                sRow.style.display = show ? '' : 'none';
            });
            if (show) matchCount++;
        });

        // Banner phản hồi
        var banner = document.getElementById('activeFilterBanner');
        var bannerText = document.getElementById('activeFilterText');
        if (timeframe !== 'ALL') {
            if (banner && bannerText) {
                bannerText.innerHTML = '<i class="bi bi-calendar3 text-primary me-1"></i> Đang lọc theo thời gian: <strong>' + label + '</strong> <span class="badge bg-primary text-white rounded-pill ms-1">' + matchCount + ' việc</span>';
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
        } else {
            if (banner) { banner.classList.add('d-none'); banner.classList.remove('d-flex'); }
        }
    };

    // 8. Open Inbox Drawer safely
    window.openInboxDrawer = function() {
        var el = document.getElementById('inboxDrawer');
        if (el && window.bootstrap && window.bootstrap.Offcanvas) {
            var bsOffcanvas = bootstrap.Offcanvas.getInstance(el) || new bootstrap.Offcanvas(el);
            bsOffcanvas.show();
        }
    };

    // 9. ClickUp 3.0 Unified Side-Peek Task & Subtask Drawer Controller
    window.openClickUpTask = function(taskId, subtaskId) {
        if (!taskId) return;

        // A. Ẩn toàn bộ các Pane Task và Subtask đang mở
        var allPanes = document.querySelectorAll('.task-detail-pane, .subtask-detail-pane');
        allPanes.forEach(function(pane) {
            pane.classList.add('d-none');
        });

        // B. Kích hoạt Pane được chỉ định
        var targetPane = null;
        if (subtaskId) {
            targetPane = document.getElementById('subtaskPane-' + subtaskId);
        } else {
            targetPane = document.getElementById('taskPane-' + taskId);
        }

        if (targetPane) {
            targetPane.classList.remove('d-none');
            var drawerContent = document.getElementById('clickupDrawerContent');
            if (drawerContent) {
                drawerContent.scrollTop = 0;
            }
        }

        // C. Mở Offcanvas Drawer từ mép phải
        var drawerEl = document.getElementById('clickupTaskDrawer');
        if (drawerEl && window.bootstrap && window.bootstrap.Offcanvas) {
            var bsOffcanvas = bootstrap.Offcanvas.getInstance(drawerEl) || new bootstrap.Offcanvas(drawerEl);
            bsOffcanvas.show();
        }
    };

    // 10. ClickUp & Asana Style Confetti Celebration Animation
    window.fireConfettiCelebration = function() {
        if (typeof confetti === 'function') {
            var count = 200;
            var defaults = {
                origin: { y: 0.7 },
                zIndex: 99999
            };

            function fire(particleRatio, opts) {
                confetti(Object.assign({}, defaults, opts, {
                    particleCount: Math.floor(count * particleRatio)
                }));
            }

            fire(0.25, { spread: 26, startVelocity: 55, colors: ['#4f46e5', '#38bdf8', '#10b981', '#f59e0b', '#ec4899'] });
            fire(0.2, { spread: 60, colors: ['#6366f1', '#06b6d4', '#34d399', '#fbbf24'] });
            fire(0.35, { spread: 100, decay: 0.91, scalar: 0.8 });
            fire(0.1, { spread: 120, startVelocity: 25, decay: 0.92, colors: ['#a855f7', '#3b82f6', '#10b981'] });
            fire(0.1, { spread: 120, startVelocity: 45 });
        }
    };

    // 11. Quick Toggle Subtasks (Đóng ⇄ Mở rộng)
    window.quickToggleSubtaskMode = function() {
        if (typeof window.toggleAllSubtasks === 'function') {
            window.toggleAllSubtasks();
        }
    };

    // 12. ClickUp 3.0 Favorite Projects (Starred Spaces via localStorage)
    window.getFavoriteProjects = function() {
        try {
            var raw = localStorage.getItem('teamwork_fav_projects');
            return raw ? JSON.parse(raw) : [];
        } catch(e) {
            return [];
        }
    };

    window.saveFavoriteProjects = function(list) {
        try {
            localStorage.setItem('teamwork_fav_projects', JSON.stringify(list));
        } catch(e) {}
    };

    window.toggleProjectFavorite = function(projId, projName) {
        var list = window.getFavoriteProjects();
        var index = list.findIndex(function(item) { return item.id === projId; });
        var starBtn = document.getElementById('btnStarProject');

        if (index > -1) {
            list.splice(index, 1);
            if (starBtn) {
                starBtn.classList.remove('text-warning', 'bi-star-fill');
                starBtn.classList.add('text-muted', 'bi-star');
            }
            if (window.showToast) window.showToast('Đã bỏ dự án khỏi mục yêu thích', 'info');
        } else {
            list.push({ id: projId, name: projName });
            if (starBtn) {
                starBtn.classList.remove('text-muted', 'bi-star');
                starBtn.classList.add('text-warning', 'bi-star-fill');
            }
            if (window.showToast) window.showToast('Đã thêm dự án vào mục yêu thích ⭐', 'success');
            if (window.fireConfettiCelebration) window.fireConfettiCelebration();
        }
        window.saveFavoriteProjects(list);
        window.renderSidebarFavorites();
    };

    window.renderSidebarFavorites = function() {
        var container = document.getElementById('favoritesList');
        if (!container) return;
        var list = window.getFavoriteProjects();
        var currentProjectId = ${project.id};

        if (list.length === 0) {
            container.innerHTML = '<span class="fs-9 text-muted px-2 py-1 fst-italic">Chưa có dự án yêu thích</span>';
            return;
        }

        var html = '';
        list.forEach(function(item) {
            var isActive = item.id === currentProjectId;
            var url = '${pageContext.request.contextPath}/task?action=list&projectId=' + item.id;
            html += '<a href="' + url + '" class="clickup-space-item ' + (isActive ? 'active' : '') + '">';
            html += '<span class="clickup-space-icon text-warning"><i class="bi bi-star-fill"></i></span>';
            html += '<span class="text-truncate flex-grow-1 fs-8 fw-medium">' + item.name + '</span>';
            html += '</a>';
        });
        container.innerHTML = html;
    };

    // Khởi tạo Bootstrap tooltips, Favorites và Auto Confetti khi DOM sẵn sàng
    document.addEventListener('DOMContentLoaded', function() {
        if (window.bootstrap && bootstrap.Tooltip) {
            var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            tooltipTriggerList.map(function(el) {
                return new bootstrap.Tooltip(el);
            });
        }

        // Khởi tạo trạng thái Ngôi sao và Danh sách Yêu thích Sidebar
        var favs = window.getFavoriteProjects();
        var isFav = favs.some(function(item) { return item.id === ${project.id}; });
        var starBtn = document.getElementById('btnStarProject');
        if (starBtn && isFav) {
            starBtn.classList.remove('text-muted', 'bi-star');
            starBtn.classList.add('text-warning', 'bi-star-fill');
        }
        window.renderSidebarFavorites();

        // Tự động bắn pháo hoa khi hoàn thành bất kỳ cột mốc nào (Gate 1, Gate 2, Task In Progress/Done, Subtask)
        <c:if test="${not empty toastSuccess}">
            var toastMsg = "<c:out value='${toastSuccess}' />".toLowerCase();
            if (toastMsg.indexOf('duyệt') > -1 || 
                toastMsg.indexOf('nghiệm thu') > -1 || 
                toastMsg.indexOf('hoàn thành') > -1 || 
                toastMsg.indexOf('thành công') > -1 || 
                toastMsg.indexOf('tiến hành') > -1 || 
                toastMsg.indexOf('đang làm') > -1 || 
                toastMsg.indexOf('bắt đầu') > -1 || 
                toastMsg.indexOf('in progress') > -1 || 
                toastMsg.indexOf('khóa kế hoạch') > -1 || 
                toastMsg.indexOf('subtask') > -1) {
                setTimeout(function() {
                    if (window.fireConfettiCelebration) window.fireConfettiCelebration();
                }, 400);
            }
        </c:if>
    });
</script>
        <!-- =========================================================================
             4. CLICKUP 3.0 UNIFIED TASK & SUBTASK SIDE-PEEK DRAWER
             ========================================================================= -->
        <div class="offcanvas offcanvas-end clickup-task-drawer shadow-lg" tabindex="-1" id="clickupTaskDrawer" aria-labelledby="clickupTaskDrawerLabel">
            <div class="offcanvas-body drawer-body p-0" id="clickupDrawerContent">
                <c:forEach items="${allProjectTasks}" var="task">
                    <!-- =========================================================================
                         A. PANE TASK CHA #${task.id}
                         ========================================================================= -->
                    <div id="taskPane-${task.id}" class="task-detail-pane d-none" data-task-id="${task.id}">
                        <!-- DRAWER HEADER: BREADCRUMB & CONTROLS -->
                        <div class="drawer-header d-flex align-items-center justify-content-between">
                            <div class="d-flex align-items-center gap-2 flex-grow-1 me-3 text-truncate">
                                <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5 fs-9 fw-semibold">#${task.id}</span>
                                <c:forEach items="${task.labelList}" var="lbl">
                                    <span class="badge ${task.getLabelBadgeClass(lbl)} rounded-pill px-2 py-0-5 fs-9 fw-semibold">
                                        ${task.getLabelDisplayName(lbl)}
                                    </span>
                                </c:forEach>
                                <span class="badge ${task.priorityBadgeClass} rounded-pill px-2 py-0-5 fs-9 fw-semibold">
                                    ● ${task.priorityLabel}
                                </span>
                                <span class="badge ${task.statusBadgeClass} rounded-pill px-2 py-0-5 fs-9 fw-semibold">
                                    ${task.statusLabel}
                                </span>
                            </div>
                            <div class="d-flex align-items-center gap-2 flex-shrink-0">
                                <c:choose>
                                    <c:when test="${task.status == 'DONE'}">
                                        <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2-5 py-1 fs-9 fw-semibold">
                                            <i class="bi bi-lock-fill me-1"></i> Đã hoàn thành (Read-only)
                                        </span>
                                    </c:when>
                                    <c:when test="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}">
                                        <button type="button" class="btn btn-outline-secondary btn-xs rounded-pill px-2-5 py-1 fs-9 d-flex align-items-center gap-1"
                                            data-bs-toggle="collapse" data-bs-target="#editTaskFormCollapse-${task.id}">
                                            <i class="bi bi-pencil-square"></i> Chỉnh sửa
                                        </button>
                                    </c:when>
                                </c:choose>
                                <button type="button" class="btn-close" data-bs-dismiss="offcanvas" aria-label="Đóng"></button>
                            </div>
                        </div>

                        <!-- DRAWER BODY 2 CỘT -->
                        <div class="row g-0">
                            <!-- CỘT TRÁI (65%): NỘI DUNG, CHECKLIST VIỆC CON, TÀI LIỆU -->
                            <div class="col-12 col-lg-8 p-4 border-end">
                                <!-- FORM CHỈNH SỬA TASK CHA (COLLAPSIBLE) -->
                                <c:if test="${task.status != 'DONE' && (task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id)}">
                                    <div class="collapse mb-4" id="editTaskFormCollapse-${task.id}">
                                        <form method="post" action="${pageContext.request.contextPath}/task"
                                            class="p-3 bg-white rounded-3 border border-primary-subtle shadow-sm">
                                            <input type="hidden" name="action" value="editTask">
                                            <input type="hidden" name="projectId" value="${project.id}">
                                            <input type="hidden" name="taskId" value="${task.id}">
                                            <h6 class="fw-bold text-primary fs-8 mb-3"><i class="bi bi-pencil-fill me-1"></i> Chỉnh sửa thông tin công việc</h6>
                                            <div class="mb-2">
                                                <label class="form-label fs-9 fw-bold text-dark mb-1">Tiêu đề:</label>
                                                <input type="text" name="title" value="${task.title}" class="form-control form-control-sm" required>
                                            </div>
                                            <div class="mb-2">
                                                <label class="form-label fs-9 fw-bold text-dark mb-1">Mô tả chi tiết:</label>
                                                <textarea name="description" class="form-control form-control-sm" rows="3"><c:out value="${task.description}" /></textarea>
                                            </div>
                                            <div class="row g-2 mb-3">
                                                <div class="${project.soloProject ? 'col-6' : 'col-4'}">
                                                    <label class="form-label fs-9 fw-bold text-dark mb-1">Mức ưu tiên:</label>
                                                    <select name="priority" class="form-select form-select-sm">
                                                        <option value="LOW" ${task.priority=='LOW' ? 'selected' : ''}>Thấp (Low)</option>
                                                        <option value="MEDIUM" ${task.priority=='MEDIUM' ? 'selected' : ''}>Trung bình (Medium)</option>
                                                        <option value="HIGH" ${task.priority=='HIGH' ? 'selected' : ''}>Cao (High)</option>
                                                    </select>
                                                </div>
                                                <div class="${project.soloProject ? 'col-6' : 'col-4'}">
                                                    <label class="form-label fs-9 fw-bold text-dark mb-1">Hạn chót:</label>
                                                    <input type="date" name="dueDate" value="${task.dueDate}" class="form-control form-control-sm">
                                                </div>
                                                <c:choose>
                                                    <c:when test="${project.teamProject}">
                                                        <div class="col-4">
                                                            <label class="form-label fs-9 fw-bold text-dark mb-1">Người phụ trách:</label>
                                                            <c:choose>
                                                                <c:when test="${project.ownerId == sessionScope.currentUser.id}">
                                                                    <select name="assigneeId" class="form-select form-select-sm" required>
                                                                        <c:forEach items="${userList}" var="u">
                                                                            <option value="${u.id}" ${u.id==task.assigneeId ? 'selected' : ''}>${u.fullName}</option>
                                                                        </c:forEach>
                                                                    </select>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <input type="hidden" name="assigneeId" value="${task.assigneeId}">
                                                                    <input type="text" class="form-control form-control-sm bg-light" value="${task.assigneeName}" readonly>
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </div>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <input type="hidden" name="assigneeId" value="${sessionScope.currentUser.id}">
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                            <c:if test="${project.teamProject}">
                                                <input type="hidden" name="hasRequiresGateControl" value="true">
                                                <div class="p-2-5 bg-light rounded-2 border mb-3">
                                                    <div class="form-check form-switch mb-1">
                                                        <input class="form-check-input" type="checkbox" role="switch" id="editRequiresGate-${task.id}" name="requiresGate" value="true" ${task.requiresGate ? 'checked' : ''} ${project.ownerId == sessionScope.currentUser.id ? '' : 'disabled'}>
                                                        <label class="form-check-label fs-9 fw-bold text-dark" for="editRequiresGate-${task.id}">
                                                            <i class="bi bi-shield-check text-primary me-1"></i> Áp dụng Quality Gate (Cổng kiểm soát)
                                                        </label>
                                                    </div>
                                                    <div class="fs-10 text-muted">Bật để kích hoạt Gate 1 (Khóa kế hoạch WBS) & Gate 2 (Nghiệm thu PM). Tắt để chuyển sang Fast-track.<c:if test="${project.ownerId != sessionScope.currentUser.id}"> (Chỉ PM mới có quyền đổi)</c:if></div>
                                                </div>
                                            </c:if>
                                            <div class="d-flex justify-content-end gap-2">
                                                <button type="button" class="btn btn-light btn-sm fs-9" data-bs-toggle="collapse" data-bs-target="#editTaskFormCollapse-${task.id}">Hủy</button>
                                                <button type="submit" class="btn btn-primary btn-sm fs-9 fw-semibold">Lưu thay đổi</button>
                                            </div>
                                        </form>
                                    </div>
                                </c:if>

                                <!-- TIÊU ĐỀ & MÔ TẢ -->
                                <div class="mb-4">
                                    <h4 class="fw-bold text-dark mb-2 ${task.status == 'DONE' ? 'text-decoration-line-through text-muted' : ''}">${task.title}</h4>
                                    <div class="task-desc-card">
                                        <c:choose>
                                            <c:when test="${not empty task.description}">
                                                <div class="fs-8 text-secondary lh-base" style="white-space: pre-line;"><c:out value="${task.description}" /></div>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-muted fst-italic fs-8">Chưa có mô tả chi tiết cho công việc này. Nhấn "Chỉnh sửa" ở góc trên để bổ sung.</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>

                                <!-- DANH SÁCH VIỆC CON (SUBTASK CHECKLIST) - CLICKUP 3.0 STYLE -->
                                <div class="mb-4">
                                    <div class="d-flex align-items-center justify-content-between mb-2">
                                        <h6 class="fw-bold text-dark fs-7 mb-0 d-flex align-items-center gap-2">
                                            <i class="bi bi-list-check text-primary"></i>
                                            <span>Danh sách việc con (Checklist)</span>
                                        </h6>
                                        <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5 fs-9 fw-semibold">
                                            ${not empty taskSubTasksMap[task.id] ? taskSubTasksMap[task.id].size() : 0} việc
                                        </span>
                                    </div>

                                    <c:if test="${not empty taskSubTasksMap[task.id]}">
                                        <!-- Micro Progress Bar -->
                                        <div class="subtask-progress-card mb-2">
                                            <div class="d-flex align-items-center justify-content-between mb-1">
                                                <span class="fs-9 text-muted fw-semibold">Tiến độ hoàn thành việc con</span>
                                                <span class="fs-9 fw-bold text-success">${taskProgressMap[task.id]}%</span>
                                            </div>
                                            <div class="progress" style="height: 5px; background-color: #e2e8f0;">
                                                <div class="progress-bar bg-success rounded-pill" role="progressbar"
                                                    style="width: ${taskProgressMap[task.id]}%;"
                                                    aria-valuenow="${taskProgressMap[task.id]}" aria-valuemin="0" aria-valuemax="100"></div>
                                            </div>
                                        </div>

                                        <!-- Danh sách từng dòng Subtask -->
                                        <div class="d-flex flex-column gap-1 mb-2">
                                            <c:forEach items="${taskSubTasksMap[task.id]}" var="st">
                                                <div class="subtask-checklist-item d-flex align-items-center justify-content-between gap-2"
                                                    onclick="openClickUpTask(${task.id}, ${st.id})">
                                                    <div class="d-flex align-items-center gap-2 flex-grow-1 text-truncate">
                                                        <c:choose>
                                                            <c:when test="${st.status == 'APPROVED'}">
                                                                <i class="bi bi-check-circle-fill text-success fs-7" title="Đã duyệt hoàn thành"></i>
                                                            </c:when>
                                                            <c:when test="${st.status == 'SUBMITTED'}">
                                                                <i class="bi bi-hourglass-split text-warning fs-7" title="Đang chờ duyệt"></i>
                                                            </c:when>
                                                            <c:when test="${st.status == 'REVISE'}">
                                                                <i class="bi bi-arrow-repeat text-info fs-7" title="Cần cân chỉnh"></i>
                                                            </c:when>
                                                            <c:when test="${st.status == 'REJECTED'}">
                                                                <i class="bi bi-exclamation-circle-fill text-danger fs-7" title="Đã bị trả về"></i>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <i class="bi bi-circle text-muted fs-7" title="Chưa hoàn thành"></i>
                                                            </c:otherwise>
                                                        </c:choose>
                                                        <span class="fs-8 text-dark text-truncate ${st.status == 'APPROVED' ? 'text-decoration-line-through text-muted' : 'fw-medium'}">
                                                            ${st.title}
                                                        </span>
                                                    </div>
                                                    <div class="d-flex align-items-center gap-1-5 flex-shrink-0">
                                                        <span class="badge ${st.statusBadgeClass} rounded-pill px-2 py-0 fs-9">${st.statusLabel}</span>
                                                        <c:if test="${not empty st.dueDate}">
                                                            <span class="badge ${st.deadlineBadgeClass} rounded-pill px-2 py-0 fs-9" title="${st.dueDate}">
                                                                <i class="bi bi-calendar-event me-1"></i>${st.deadlineLabel}
                                                            </span>
                                                        </c:if>
                                                        <c:if test="${project.teamProject}">
                                                            <span class="badge bg-light text-secondary border rounded-pill px-2 py-0 fs-9">
                                                                <i class="bi bi-person-fill text-primary me-1"></i>${st.assigneeName}
                                                            </span>
                                                        </c:if>
                                                        <i class="bi bi-chevron-right fs-9 text-muted ms-1"></i>
                                                    </div>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </c:if>

                                    <c:if test="${empty taskSubTasksMap[task.id]}">
                                        <div class="p-3 bg-light-subtle rounded-3 text-muted fs-8 border text-center mb-2">
                                            Chưa có việc con nào được tạo.
                                        </div>
                                    </c:if>

                                    <!-- INLINE QUICK ADD SUBTASK BOX (CLICKUP STYLE) -->
                                    <c:if test="${task.status != 'DONE' && (task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id)}">
                                        <form method="post" action="${pageContext.request.contextPath}/task"
                                            class="quick-add-subtask-box d-flex align-items-center gap-2 mt-1"
                                            onsubmit="if (!this.title.value.trim()) return false;">
                                            <input type="hidden" name="action" value="addSubTask">
                                            <input type="hidden" name="projectId" value="${project.id}">
                                            <input type="hidden" name="taskId" value="${task.id}">
                                            <i class="bi bi-plus-circle text-primary fs-7"></i>
                                            <input type="text" name="title"
                                                class="form-control form-control-sm border-0 bg-transparent shadow-none fs-8 flex-grow-1 p-0"
                                                placeholder="+ Thêm việc con mới... (nhấn Enter để tạo)" autocomplete="off" required>
                                            <c:choose>
                                                <c:when test="${project.teamProject}">
                                                    <select name="assigneeId" class="form-select form-select-sm border-0 bg-transparent shadow-none fs-9 py-0 text-muted" style="width: auto; max-width: 140px;">
                                                        <option value="0">-- Phân công --</option>
                                                        <c:forEach items="${userList}" var="u">
                                                            <option value="${u.id}">${u.fullName}</option>
                                                        </c:forEach>
                                                    </select>
                                                </c:when>
                                                <c:otherwise>
                                                    <input type="hidden" name="assigneeId" value="${sessionScope.currentUser.id}">
                                                </c:otherwise>
                                            </c:choose>
                                            <input type="date" name="dueDate" class="form-control form-control-sm border-0 bg-transparent shadow-none fs-9 py-0 text-muted"
                                                style="width: auto; max-width: 125px;" max="${task.dueDate}" title="Hạn chót việc con (tối đa ${task.dueDate})">
                                            <button type="submit" class="btn btn-primary-custom btn-sm rounded-pill px-3 py-0-5 fs-9 fw-semibold text-nowrap">
                                                Tạo
                                            </button>
                                        </form>
                                    </c:if>
                                </div>

                                <!-- KHỐI TÀI LIỆU WIKI ĐÍNH KÈM -->
                                <div>
                                    <div class="d-flex align-items-center justify-content-between mb-2">
                                        <h6 class="fw-bold text-dark fs-7 mb-0 d-flex align-items-center gap-2">
                                            <i class="bi bi-journal-bookmark text-primary"></i>
                                            <span>Tài liệu hướng dẫn đính kèm</span>
                                        </h6>
                                        <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5 fs-9 fw-semibold">
                                            ${not empty taskDocsMap[task.id] ? taskDocsMap[task.id].size() : 0} tài liệu
                                        </span>
                                    </div>
                                    <c:if test="${not empty taskDocsMap[task.id]}">
                                        <div class="d-flex flex-column gap-2">
                                            <c:forEach items="${taskDocsMap[task.id]}" var="td">
                                                <div class="doc-attachment-item">
                                                    <div class="d-flex align-items-center gap-2 text-truncate">
                                                        <i class="bi bi-file-earmark-text text-primary fs-6"></i>
                                                        <span class="fw-semibold text-dark fs-8 text-truncate">${td.docTitle}</span>
                                                    </div>
                                                    <a href="${pageContext.request.contextPath}/doc?action=view&projectId=${project.id}&docId=${td.docId}"
                                                        class="btn btn-outline-secondary btn-xs rounded-pill px-3 py-1 fs-9 text-nowrap"
                                                        title="Đọc bài viết Wiki này">
                                                        Đọc bài <i class="bi bi-arrow-right ms-1"></i>
                                                    </a>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </c:if>
                                    <c:if test="${empty taskDocsMap[task.id]}">
                                        <div class="p-3 bg-light-subtle rounded-3 text-muted fs-8 border text-center">
                                            Chưa có tài liệu Wiki nào được đính kèm cho công việc này.
                                        </div>
                                    </c:if>
                                </div>
                            </div>

                            <!-- CỘT PHẢI (35%): THUỘC TÍNH, QUICK ACTIONS, GATES (CỔNG 1 & CỔNG 2), HỘI THOẠI -->
                            <div class="col-12 col-lg-4 task-modal-sidebar p-4 bg-light-subtle d-flex flex-column justify-content-between" style="min-height: 580px;">
                                <div>
                                    <!-- THUỘC TÍNH CÔNG VIỆC -->
                                    <div class="task-sidebar-section-title mb-2">
                                        <i class="bi bi-sliders me-1"></i> Thuộc tính công việc
                                    </div>
                                    <div class="task-property-list bg-white rounded-3 p-3 border shadow-2xs mb-3">
                                        <div class="task-property-row">
                                            <span class="task-property-label"><i class="bi bi-arrow-repeat"></i> Trạng thái</span>
                                            <span class="task-property-value">
                                                <span class="badge ${task.statusBadgeClass} rounded-pill px-2 py-0-5 fs-9">${task.statusLabel}</span>
                                            </span>
                                        </div>
                                        <c:if test="${project.teamProject}">
                                            <div class="task-property-row">
                                                <span class="task-property-label"><i class="bi bi-person"></i> Phụ trách</span>
                                                <span class="task-property-value d-flex align-items-center gap-1">
                                                    <span class="avatar-circle-sm bg-primary text-white rounded-circle d-inline-flex align-items-center justify-content-center"
                                                        style="width: 18px; height: 18px; font-size: 0.65rem;">
                                                        ${task.assigneeName.substring(0, 1).toUpperCase()}
                                                    </span>
                                                    <span>${task.assigneeName}</span>
                                                </span>
                                            </div>
                                        </c:if>
                                        <div class="task-property-row">
                                            <span class="task-property-label"><i class="bi bi-flag"></i> Mức ưu tiên</span>
                                            <span class="task-property-value">
                                                <span class="badge ${task.priorityBadgeClass} rounded-pill px-2 py-0-5 fs-9">● ${task.priorityLabel}</span>
                                            </span>
                                        </div>
                                        <div class="task-property-row">
                                            <span class="task-property-label"><i class="bi bi-calendar3"></i> Hạn chót</span>
                                            <span class="task-property-value">
                                                <c:choose>
                                                    <c:when test="${not empty task.dueDate}">
                                                        <span class="badge ${task.deadlineBadgeClass} rounded-pill px-2 py-0-5 fs-9">
                                                            <i class="bi bi-calendar-event me-1"></i>${task.dueDate}
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="text-muted fs-9">Chưa đặt hạn</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </span>
                                        </div>
                                        <div class="task-property-row">
                                            <span class="task-property-label"><i class="bi bi-folder2-open"></i> Dự án</span>
                                            <span class="task-property-value text-truncate" style="max-width: 130px;" title="${project.name}">#${project.projectCode}</span>
                                        </div>
                                        <div class="task-property-row">
                                            <span class="task-property-label"><i class="bi bi-check2-circle"></i> Tiến độ</span>
                                            <span class="task-property-value text-success fw-bold">${taskProgressMap[task.id]}%</span>
                                        </div>
                                    </div>

                                    <c:choose>
                                        <c:when test="${project.teamProject && task.requiresGate}">
                                            <!-- NÚT BẮT ĐẦU (NẾU ĐANG TODO) -->
                                            <c:if test="${task.status == 'TODO'}">
                                        <c:set var="hasAssignee" value="${task.assigneeId > 0}" />
                                        <c:set var="subTaskCount" value="${not empty taskSubTasksMap[task.id] ? taskSubTasksMap[task.id].size() : 0}" />
                                        <c:set var="canStartTask" value="${hasAssignee && subTaskCount > 0}" />
                                        <div class="mb-3">
                                            <div class="task-sidebar-section-title d-flex align-items-center justify-content-between mb-2">
                                                <span><i class="bi bi-lightning-charge me-1"></i> Thao tác nhanh</span>
                                                <c:choose>
                                                    <c:when test="${canStartTask}">
                                                        <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 fs-9">
                                                            <i class="bi bi-check-circle-fill me-1"></i> Đủ điều kiện
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill px-2 fs-9">
                                                            <i class="bi bi-lock-fill me-1"></i> Chưa đủ điều kiện
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                            <c:choose>
                                                <c:when test="${canStartTask}">
                                                    <form method="post" action="${pageContext.request.contextPath}/task" class="m-0">
                                                        <input type="hidden" name="action" value="updateStatus">
                                                        <input type="hidden" name="projectId" value="${project.id}">
                                                        <input type="hidden" name="taskId" value="${task.id}">
                                                        <input type="hidden" name="newStatus" value="IN_PROGRESS">
                                                        <button type="submit" class="btn btn-primary btn-sm w-100 rounded-3 fs-8 fw-bold d-flex align-items-center justify-content-center gap-1.5 shadow-sm py-2 text-white border-0"
                                                            style="background: linear-gradient(135deg, #2563eb, #1d4ed8);">
                                                            <i class="bi bi-play-circle-fill fs-7"></i> Bắt đầu làm việc (→ In Progress) ✨
                                                        </button>
                                                    </form>
                                                </c:when>
                                                <c:otherwise>
                                                    <button type="button" class="btn btn-light text-muted border border-secondary-subtle btn-sm w-100 rounded-3 fs-9 fw-semibold d-flex align-items-center justify-content-center gap-1 opacity-75" disabled>
                                                        <i class="bi bi-lock-fill text-secondary"></i> Bắt đầu làm việc (→ In Progress)
                                                    </button>
                                                    <div class="p-2.5 bg-light rounded-3 border fs-9 text-secondary mt-1">
                                                        <div class="fw-bold text-dark mb-1 pb-1 border-bottom fs-9"><i class="bi bi-shield-lock text-primary me-1"></i> Ràng buộc:</div>
                                                        <div class="d-flex align-items-center gap-1.5 ${hasAssignee ? 'text-success fw-semibold' : 'text-danger'}">
                                                            <i class="bi ${hasAssignee ? 'bi-check-circle-fill' : 'bi-x-circle-fill'}"></i>
                                                            <span>${hasAssignee ? 'Đã gán người phụ trách' : 'Chưa phân công (Bấm "Chỉnh sửa")'}</span>
                                                        </div>
                                                        <div class="d-flex align-items-center gap-1.5 ${subTaskCount > 0 ? 'text-success fw-semibold' : 'text-danger'} mt-1">
                                                            <i class="bi ${subTaskCount > 0 ? 'bi-check-circle-fill' : 'bi-x-circle-fill'}"></i>
                                                            <span>${subTaskCount > 0 ? 'Đã có việc con' : 'Chưa có việc con (Tạo ít nhất 1 việc con)'}</span>
                                                        </div>
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </c:if>

                                    <!-- CỔNG 1: THẨM ĐỊNH & KHÓA KẾ HOẠCH PHÂN RÃ (SCOPE LOCK) -->
                                    <c:if test="${task.status == 'PLANNING'}">
                                        <div class="gate-card gate-card-primary mb-3">
                                            <div class="d-flex align-items-center justify-content-between mb-2 pb-2 border-bottom">
                                                <div class="d-flex align-items-center gap-2">
                                                    <span class="p-1 bg-white text-primary rounded-2 shadow-2xs lh-1"><i class="bi bi-diagram-3-fill fs-6"></i></span>
                                                    <div>
                                                        <h6 class="fw-bold text-primary fs-8 mb-0">Cổng 1: Hồ Sơ Kế Hoạch</h6>
                                                        <span class="fs-9 text-muted">${not empty taskSubTasksMap[task.id] ? taskSubTasksMap[task.id].size() : 0} việc con &bull; ${task.submittedAt}</span>
                                                    </div>
                                                </div>
                                                <span class="badge bg-primary text-white rounded-pill px-2 py-0-5 fs-9 fw-semibold">Chờ PM duyệt</span>
                                            </div>

                                            <c:if test="${not empty task.planningNote}">
                                                <div class="p-2 bg-white rounded-2 border fs-9 text-dark mb-2 shadow-2xs">
                                                    <strong class="text-primary d-block mb-1 fs-9"><i class="bi bi-chat-left-quote-fill me-1"></i> Thuyết minh từ Task Lead:</strong>
                                                    <div class="text-secondary lh-base" style="white-space: pre-line;"><c:out value="${task.planningNote}" /></div>
                                                </div>
                                            </c:if>

                                            <!-- Nút Duyệt / Trả Về Dành Cho PM -->
                                            <c:choose>
                                                <c:when test="${project.ownerId == sessionScope.currentUser.id}">
                                                    <div class="d-flex align-items-center gap-1-5 pt-1 border-top border-primary-subtle">
                                                        <button type="button" class="btn btn-outline-warning text-dark btn-sm rounded-pill fs-9 py-1 px-2 fw-semibold flex-fill shadow-2xs"
                                                            data-bs-toggle="collapse" data-bs-target="#rejectPlanningPanel-${task.id}">
                                                            <i class="bi bi-arrow-counterclockwise me-1"></i> Yêu Cầu Bổ Sung
                                                        </button>
                                                        <button type="button" class="btn btn-primary btn-sm rounded-pill fs-9 py-1 px-2 fw-semibold flex-fill shadow-2xs text-white"
                                                            data-bs-toggle="collapse" data-bs-target="#approvePlanningPanel-${task.id}">
                                                            <i class="bi bi-lock-fill me-1"></i> Phê Duyệt & Khóa 🔒
                                                        </button>
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="alert alert-info py-1 px-2 rounded-2 fs-9 d-flex align-items-center gap-1 mb-0">
                                                        <i class="bi bi-hourglass-split text-primary fs-8"></i>
                                                        <span>Đã trình kế hoạch lên PM. Chờ PM phê duyệt!</span>
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>

                                            <!-- PANEL PM DUYỆT KẾ HOẠCH -->
                                            <div class="collapse mt-2" id="approvePlanningPanel-${task.id}">
                                                <div class="p-2.5 bg-white rounded-2 border border-primary shadow-sm">
                                                    <h6 class="fw-bold text-primary fs-8 mb-2"><i class="bi bi-lock-fill me-1"></i> PM Phê Duyệt & Khóa Kế Hoạch</h6>
                                                    <form action="${pageContext.request.contextPath}/task" method="post">
                                                        <input type="hidden" name="action" value="pmApprovePlanning">
                                                        <input type="hidden" name="projectId" value="${project.id}">
                                                        <input type="hidden" name="taskId" value="${task.id}">
                                                        <div class="mb-2">
                                                            <label for="feedback-${task.id}" class="form-label fw-semibold fs-9 text-dark mb-1">Dặn dò của PM:</label>
                                                            <textarea class="form-control fs-9 rounded-2" id="feedback-${task.id}" name="feedback" rows="2" placeholder="Ý kiến chỉ đạo của PM..."></textarea>
                                                        </div>
                                                        <div class="d-flex align-items-center justify-content-end gap-1">
                                                            <button type="button" class="btn btn-light rounded-pill px-2 py-0-5 fs-9" data-bs-toggle="collapse" data-bs-target="#approvePlanningPanel-${task.id}">Đóng</button>
                                                            <button type="submit" class="btn btn-primary rounded-pill px-3 py-0-5 fs-9 fw-semibold text-white">Khóa Kế Hoạch</button>
                                                        </div>
                                                    </form>
                                                </div>
                                            </div>

                                            <!-- PANEL PM TRẢ VỀ KẾ HOẠCH -->
                                            <div class="collapse mt-2" id="rejectPlanningPanel-${task.id}">
                                                <div class="p-2.5 bg-white rounded-2 border border-warning shadow-sm">
                                                    <h6 class="fw-bold text-dark fs-8 mb-2"><i class="bi bi-arrow-counterclockwise text-warning me-1"></i> Yêu Cầu Bổ Sung</h6>
                                                    <form action="${pageContext.request.contextPath}/task" method="post">
                                                        <input type="hidden" name="action" value="pmRejectPlanning">
                                                        <input type="hidden" name="projectId" value="${project.id}">
                                                        <input type="hidden" name="taskId" value="${task.id}">
                                                        <div class="mb-2">
                                                            <label for="rejectFeedback-${task.id}" class="form-label fw-semibold fs-9 text-dark mb-1">Các hạng mục cần bổ sung <span class="text-danger">*</span>:</label>
                                                            <textarea class="form-control fs-9 rounded-2" id="rejectFeedback-${task.id}" name="feedback" rows="2" placeholder="Ví dụ: Thiếu module kiểm thử..." required></textarea>
                                                        </div>
                                                        <div class="d-flex align-items-center justify-content-end gap-1">
                                                            <button type="button" class="btn btn-light rounded-pill px-2 py-0-5 fs-9" data-bs-toggle="collapse" data-bs-target="#rejectPlanningPanel-${task.id}">Đóng</button>
                                                            <button type="submit" class="btn btn-warning rounded-pill px-3 py-0-5 fs-9 fw-semibold text-dark">Gửi Yêu Cầu</button>
                                                        </div>
                                                    </form>
                                                </div>
                                            </div>
                                        </div>
                                    </c:if>

                                    <!-- CỔNG 2: BÀN GIAO & NGHIỆM THU TASK LỚN (TASK LEAD ➔ PM) -->
                                    <c:if test="${task.status == 'IN_PROGRESS' || task.status == 'SUBMITTED' || task.status == 'REVISE' || task.status == 'REJECTED' || task.status == 'DONE'}">
                                        <div class="gate-card gate-card-success mb-3">
                                            <div class="d-flex align-items-center justify-content-between mb-2 pb-1 border-bottom">
                                                <div class="d-flex align-items-center gap-1.5">
                                                    <span class="p-1 bg-primary-subtle text-primary rounded-2 lh-1"><i class="bi bi-box-seam-fill fs-8"></i></span>
                                                    <h6 class="fw-bold text-dark fs-8 mb-0">Hồ Sơ Nghiệm Thu (Cổng 2)</h6>
                                                </div>
                                                <c:if test="${task.status == 'DONE' && task.qualityRating > 0}">
                                                    <span class="badge bg-warning-subtle text-dark border border-warning-subtle rounded-pill px-2 py-0-5 fs-9 fw-bold">
                                                        <i class="bi bi-star-fill text-warning me-1"></i>${task.qualityRating}/5 ⭐
                                                    </span>
                                                </c:if>
                                            </div>

                                            <!-- Tóm tắt bàn giao nếu có -->
                                            <c:if test="${not empty task.finalDeliverableNote}">
                                                <div class="p-2 bg-white rounded-2 border fs-9 text-dark mb-2 shadow-2xs">
                                                    <span class="fw-bold text-warning fs-9 d-block mb-1"><i class="bi bi-file-earmark-check-fill me-1"></i> Báo Cáo Bàn Giao:</span>
                                                    <div class="text-secondary fs-9 lh-base" style="white-space: pre-line;"><c:out value="${task.finalDeliverableNote}" /></div>
                                                    <c:if test="${not empty task.deliverableFile}">
                                                        <div class="mt-2 p-1.5 bg-warning-subtle rounded border border-warning-subtle d-flex align-items-center justify-content-between gap-1">
                                                            <span class="fs-9 text-truncate fw-medium">${task.deliverableFile}</span>
                                                            <a href="${pageContext.request.contextPath}/uploads/deliverables/${task.deliverableFile}" class="btn btn-warning btn-xs rounded-pill px-2 py-0 fs-9 text-dark text-nowrap" download target="_blank">
                                                                <i class="bi bi-download"></i> Tải
                                                            </a>
                                                        </div>
                                                    </c:if>
                                                </div>
                                            </c:if>

                                            <!-- Đánh giá / Dặn dò của PM nếu có -->
                                            <c:if test="${not empty task.pmFeedback}">
                                                <div class="p-2 ${task.status == 'REVISE' ? 'bg-primary-subtle text-primary border-primary-subtle' : (task.status == 'REJECTED' ? 'bg-danger-subtle text-danger border-danger-subtle' : 'bg-success-subtle text-success border-success-subtle')} rounded-2 border fs-9 mb-2">
                                                    <div class="fw-bold fs-9 mb-1"><i class="bi bi-info-circle-fill me-1"></i> Đánh giá từ PM:</div>
                                                    <p class="mb-0 fs-9">"${task.pmFeedback}"</p>
                                                </div>
                                            </c:if>

                                            <!-- Nút Hành Động Nghiệm Thu -->
                                            <div class="d-flex flex-wrap align-items-center justify-content-between gap-1 pt-1 border-top">
                                                <c:set var="canSubmitParentTask" value="${(not empty task.assigneeId && task.assigneeId > 0 && task.assigneeId == sessionScope.currentUser.id) || ((empty task.assigneeId || task.assigneeId == 0) && project.ownerId == sessionScope.currentUser.id)}" />
                                                <c:if test="${canSubmitParentTask && task.status != 'DONE'}">
                                                    <button type="button" class="btn btn-outline-warning text-dark btn-sm rounded-pill fs-9 py-1 px-2.5 fw-semibold shadow-2xs"
                                                        data-bs-toggle="collapse" data-bs-target="#submitParentTaskPanel-${task.id}">
                                                        <i class="bi bi-box-seam me-1"></i> ${task.status == 'SUBMITTED' ? 'Cập Nhật Bàn Giao' : 'Bàn Giao Cho PM'}
                                                    </button>
                                                </c:if>

                                                <c:if test="${project.ownerId == sessionScope.currentUser.id && task.status == 'SUBMITTED'}">
                                                    <c:choose>
                                                        <c:when test="${not empty taskSubTasksMap[task.id] && taskProgressMap[task.id] < 100}">
                                                            <span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill px-2 py-1 fs-9">
                                                                Việc con chưa 100% (${taskProgressMap[task.id]}%)
                                                            </span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <div class="d-flex align-items-center gap-1 ms-auto">
                                                                <button type="button" class="btn btn-success btn-sm rounded-pill fs-9 py-1 px-2 fw-semibold shadow-2xs"
                                                                    data-bs-toggle="collapse" data-bs-target="#pmApprovePanel-${task.id}" title="Duyệt 100% Đạt">
                                                                    <i class="bi bi-check-circle-fill me-1"></i> Duyệt Đạt
                                                                </button>
                                                                <button type="button" class="btn btn-primary btn-sm rounded-pill fs-9 py-1 px-2 fw-semibold shadow-2xs"
                                                                    data-bs-toggle="collapse" data-bs-target="#pmRevisePanel-${task.id}" title="Cần Cân Chỉnh">
                                                                    <i class="bi bi-pencil-square"></i>
                                                                </button>
                                                                <button type="button" class="btn btn-danger btn-sm rounded-pill fs-9 py-1 px-2 fw-semibold shadow-2xs"
                                                                    data-bs-toggle="collapse" data-bs-target="#pmRejectPanel-${task.id}" title="Chưa Đạt">
                                                                    <i class="bi bi-x-circle-fill"></i>
                                                                </button>
                                                            </div>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </c:if>
                                            </div>

                                            <!-- PANEL 1: TASK LEAD NỘP BÀN GIAO -->
                                            <div class="collapse mt-2" id="submitParentTaskPanel-${task.id}">
                                                <div class="p-2.5 bg-white rounded-2 border border-warning shadow-sm">
                                                    <h6 class="fw-bold text-dark fs-8 mb-2"><i class="bi bi-box-seam-fill text-warning me-1"></i> Báo Cáo Bàn Giao Cho PM</h6>
                                                    <form action="${pageContext.request.contextPath}/task" method="post">
                                                        <input type="hidden" name="action" value="submitParentTask">
                                                        <input type="hidden" name="projectId" value="${project.id}">
                                                        <input type="hidden" name="taskId" value="${task.id}">
                                                        <div class="mb-2">
                                                            <label for="summary-${task.id}" class="form-label fw-semibold fs-9 text-dark mb-1">Tóm tắt kết quả tính năng <span class="text-danger">*</span>:</label>
                                                            <textarea class="form-control fs-9 rounded-2" id="summary-${task.id}" name="summary" rows="2" placeholder="Mô tả tóm tắt kết quả hoàn thành..." required></textarea>
                                                        </div>
                                                        <div class="row g-2 mb-2">
                                                            <div class="col-6">
                                                                <input type="text" class="form-control form-control-sm fs-9 rounded-2" id="demoUrl-${task.id}" name="demoUrl" placeholder="Link Demo/Figma...">
                                                            </div>
                                                            <div class="col-6">
                                                                <input type="text" class="form-control form-control-sm fs-9 rounded-2" id="codeUrl-${task.id}" name="codeUrl" placeholder="Link PR/Mã nguồn...">
                                                            </div>
                                                        </div>
                                                        <div class="mb-2">
                                                            <input type="text" class="form-control form-control-sm fs-9 rounded-2" id="deliverableFile-${task.id}" name="deliverableFile"
                                                                placeholder="Tên file báo cáo (PDF, ZIP...)" value="${not empty task.deliverableFile ? task.deliverableFile : ''}">
                                                        </div>
                                                        <div class="d-flex align-items-center justify-content-end gap-1">
                                                            <button type="button" class="btn btn-light rounded-pill px-2 py-0-5 fs-9" data-bs-toggle="collapse" data-bs-target="#submitParentTaskPanel-${task.id}">Đóng</button>
                                                            <button type="submit" class="btn btn-warning rounded-pill px-3 py-0-5 fs-9 fw-semibold text-dark ${not empty taskSubTasksMap[task.id] && taskProgressMap[task.id] < 100 ? 'disabled' : ''}">
                                                                Gửi Bàn Giao
                                                            </button>
                                                        </div>
                                                    </form>
                                                </div>
                                            </div>

                                            <!-- PANEL 2: PM DUYỆT ĐẠT & CHẤM SAO -->
                                            <div class="collapse mt-2" id="pmApprovePanel-${task.id}">
                                                <div class="p-2.5 bg-white rounded-2 border border-success shadow-sm">
                                                    <h6 class="fw-bold text-success fs-8 mb-2"><i class="bi bi-patch-check-fill me-1"></i> Nghiệm Thu & Chấm Điểm Sao</h6>
                                                    <form action="${pageContext.request.contextPath}/task" method="post">
                                                        <input type="hidden" name="action" value="pmApproveTask">
                                                        <input type="hidden" name="projectId" value="${project.id}">
                                                        <input type="hidden" name="taskId" value="${task.id}">
                                                        <div class="mb-2">
                                                            <label for="qualityRating-${task.id}" class="form-label fw-semibold fs-9 text-dark mb-1">Đánh giá chất lượng:</label>
                                                            <select class="form-select form-select-sm rounded-2 fs-9" id="qualityRating-${task.id}" name="qualityRating">
                                                                <option value="5" selected>⭐⭐⭐⭐⭐ 5 Sao - Xuất Sắc</option>
                                                                <option value="4">⭐⭐⭐⭐ 4 Sao - Tốt</option>
                                                                <option value="3">⭐⭐⭐ 3 Sao - Đạt Yêu Cầu</option>
                                                                <option value="2">⭐⭐ 2 Sao - Cần Cải Thiện</option>
                                                                <option value="1">⭐ 1 Sao - Yếu</option>
                                                            </select>
                                                        </div>
                                                        <div class="mb-2">
                                                            <textarea class="form-control fs-9 rounded-2" id="feedback-${task.id}" name="feedback" rows="2" placeholder="Lời nhận xét của PM..."></textarea>
                                                        </div>
                                                        <div class="d-flex align-items-center justify-content-end gap-1">
                                                            <button type="button" class="btn btn-light rounded-pill px-2 py-0-5 fs-9" data-bs-toggle="collapse" data-bs-target="#pmApprovePanel-${task.id}">Đóng</button>
                                                            <button type="submit" class="btn btn-success rounded-pill px-3 py-0-5 fs-9 fw-semibold">Xác Nhận Duyệt Đạt 🏆</button>
                                                        </div>
                                                    </form>
                                                </div>
                                            </div>

                                            <!-- PANEL 3: PM CÂN CHỈNH -->
                                            <div class="collapse mt-2" id="pmRevisePanel-${task.id}">
                                                <div class="p-2.5 bg-white rounded-2 border border-primary shadow-sm">
                                                    <h6 class="fw-bold text-primary fs-8 mb-2"><i class="bi bi-pencil-square me-1"></i> PM Dặn Dò Cân Chỉnh</h6>
                                                    <form action="${pageContext.request.contextPath}/task" method="post">
                                                        <input type="hidden" name="action" value="pmReviseTask">
                                                        <input type="hidden" name="projectId" value="${project.id}">
                                                        <input type="hidden" name="taskId" value="${task.id}">
                                                        <div class="mb-2">
                                                            <textarea class="form-control fs-9 rounded-2" id="pmReviseNote-${task.id}" name="feedback" rows="2" placeholder="Dặn dò tinh chỉnh cho Task Lead..." required></textarea>
                                                        </div>
                                                        <div class="d-flex align-items-center justify-content-end gap-1">
                                                            <button type="button" class="btn btn-light rounded-pill px-2 py-0-5 fs-9" data-bs-toggle="collapse" data-bs-target="#pmRevisePanel-${task.id}">Đóng</button>
                                                            <button type="submit" class="btn btn-primary rounded-pill px-3 py-0-5 fs-9 fw-semibold">Gửi Cân Chỉnh</button>
                                                        </div>
                                                    </form>
                                                </div>
                                            </div>

                                            <!-- PANEL 4: PM CHƯA ĐẠT -->
                                            <div class="collapse mt-2" id="pmRejectPanel-${task.id}">
                                                <div class="p-2.5 bg-white rounded-2 border border-danger shadow-sm">
                                                    <h6 class="fw-bold text-danger fs-8 mb-2"><i class="bi bi-exclamation-triangle-fill me-1"></i> Đánh Giá Chưa Đạt</h6>
                                                    <form action="${pageContext.request.contextPath}/task" method="post">
                                                        <input type="hidden" name="action" value="pmRejectTask">
                                                        <input type="hidden" name="projectId" value="${project.id}">
                                                        <input type="hidden" name="taskId" value="${task.id}">
                                                        <div class="mb-2">
                                                            <textarea class="form-control fs-9 rounded-2" id="pmRejectNote-${task.id}" name="feedback" rows="2" placeholder="Lý do chưa đạt / yêu cầu làm lại..." required></textarea>
                                                        </div>
                                                        <div class="d-flex align-items-center justify-content-end gap-1">
                                                            <button type="button" class="btn btn-light rounded-pill px-2 py-0-5 fs-9" data-bs-toggle="collapse" data-bs-target="#pmRejectPanel-${task.id}">Đóng</button>
                                                            <button type="submit" class="btn btn-danger rounded-pill px-3 py-0-5 fs-9 fw-semibold">Trả Về Làm Lại</button>
                                                        </div>
                                                    </form>
                                                </div>
                                            </div>
                                        </div>
                                    </c:if>
                                        </c:when>
                                        <c:otherwise>
                                            <!-- QUY TRÌNH NHANH (FAST-TRACK / SOLO DỰ ÁN CÁ NHÂN) -->
                                            <div class="card border-0 bg-light-subtle rounded-3 p-3 mb-3 border shadow-2xs">
                                                <div class="d-flex align-items-center justify-content-between mb-2">
                                                    <span class="fs-9 fw-bold text-dark"><i class="bi bi-lightning-charge-fill text-warning me-1"></i> Quy Trình Nhanh (Fast-track)</span>
                                                    <span class="badge bg-warning-subtle text-dark border border-warning-subtle rounded-pill px-2 py-0-5 fs-9">Không áp Gate</span>
                                                </div>
                                                <p class="fs-9 text-muted mb-2-5 lh-sm">Công việc này có thể tự do cập nhật trạng thái mà không cần qua các cổng phê duyệt WBS hay nghiệm thu.</p>
                                                
                                                <c:choose>
                                                    <c:when test="${task.status == 'TODO'}">
                                                        <form method="post" action="${pageContext.request.contextPath}/task" class="m-0">
                                                            <input type="hidden" name="action" value="updateStatus">
                                                            <input type="hidden" name="projectId" value="${project.id}">
                                                            <input type="hidden" name="taskId" value="${task.id}">
                                                            <input type="hidden" name="newStatus" value="IN_PROGRESS">
                                                            <button type="submit" class="btn btn-primary btn-sm w-100 rounded-3 fs-8 fw-bold py-2 text-white border-0 shadow-sm d-flex align-items-center justify-content-center gap-1.5"
                                                                style="background: linear-gradient(135deg, #2563eb, #1d4ed8);">
                                                                <i class="bi bi-play-circle-fill fs-7"></i> Bắt đầu làm việc (→ In Progress) 🚀
                                                            </button>
                                                        </form>
                                                    </c:when>
                                                    <c:when test="${task.status == 'IN_PROGRESS'}">
                                                        <div class="d-flex flex-column gap-2">
                                                            <form method="post" action="${pageContext.request.contextPath}/task" class="m-0">
                                                                <input type="hidden" name="action" value="updateStatus">
                                                                <input type="hidden" name="projectId" value="${project.id}">
                                                                <input type="hidden" name="taskId" value="${task.id}">
                                                                <input type="hidden" name="newStatus" value="DONE">
                                                                <button type="submit" class="btn btn-success btn-sm w-100 rounded-3 fs-8 fw-bold py-2 text-white border-0 shadow-sm d-flex align-items-center justify-content-center gap-1.5"
                                                                    style="background: linear-gradient(135deg, #10b981, #059669);">
                                                                    <i class="bi bi-check-circle-fill fs-7"></i> Hoàn thành công việc (→ Done) ✓
                                                                </button>
                                                            </form>
                                                            <form method="post" action="${pageContext.request.contextPath}/task" class="m-0">
                                                                <input type="hidden" name="action" value="updateStatus">
                                                                <input type="hidden" name="projectId" value="${project.id}">
                                                                <input type="hidden" name="taskId" value="${task.id}">
                                                                <input type="hidden" name="newStatus" value="TODO">
                                                                <button type="submit" class="btn btn-outline-secondary btn-sm w-100 rounded-3 fs-9 py-1.5 d-flex align-items-center justify-content-center gap-1">
                                                                    <i class="bi bi-arrow-counterclockwise"></i> Trả về Chưa làm (→ To Do)
                                                                </button>
                                                            </form>
                                                        </div>
                                                    </c:when>
                                                    <c:when test="${task.status == 'DONE'}">
                                                        <form method="post" action="${pageContext.request.contextPath}/task" class="m-0">
                                                            <input type="hidden" name="action" value="updateStatus">
                                                            <input type="hidden" name="projectId" value="${project.id}">
                                                            <input type="hidden" name="taskId" value="${task.id}">
                                                            <input type="hidden" name="newStatus" value="IN_PROGRESS">
                                                            <button type="submit" class="btn btn-outline-primary btn-sm w-100 rounded-3 fs-9 py-1.5 d-flex align-items-center justify-content-center gap-1.5">
                                                                <i class="bi bi-arrow-repeat"></i> Mở lại công việc (→ In Progress)
                                                            </button>
                                                        </form>
                                                    </c:when>
                                                </c:choose>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>

                                    <!-- HỘI THOẠI & BÌNH LUẬN -->
                                    <div class="d-flex align-items-center justify-content-between mb-2 pb-1 border-bottom">
                                        <div class="d-flex align-items-center gap-1">
                                            <i class="bi bi-chat-square-dots-fill text-primary fs-8"></i>
                                            <span class="fw-bold text-dark fs-8">Hội thoại (${not empty taskCommentsMap[task.id] ? taskCommentsMap[task.id].size() : 0})</span>
                                        </div>
                                    </div>
                                    <div class="flex-grow-1 overflow-y-auto d-flex flex-column gap-2 mb-3 pe-1" style="max-height: 220px;">
                                        <c:forEach items="${taskCommentsMap[task.id]}" var="comment">
                                            <div class="p-2 px-2.5 rounded-2 border shadow-2xs ${comment.authorName == 'Hệ Thống' ? 'bg-success-subtle border-success-subtle' : 'bg-white'}">
                                                <div class="d-flex align-items-center justify-content-between mb-1">
                                                    <span class="fw-bold fs-9 ${comment.authorName == 'Hệ Thống' ? 'text-success' : 'text-dark'}">
                                                        <c:choose>
                                                            <c:when test="${comment.authorName == 'Hệ Thống'}"><i class="bi bi-robot me-1"></i> Hệ Thống</c:when>
                                                            <c:otherwise>${comment.authorName}</c:otherwise>
                                                        </c:choose>
                                                    </span>
                                                    <span class="text-muted fs-9">${comment.sentAt}</span>
                                                </div>
                                                <div class="fs-9 text-dark lh-base" style="word-break: break-word; white-space: pre-line;">
                                                    <c:out value="${comment.content}" />
                                                </div>
                                            </div>
                                        </c:forEach>
                                        <c:if test="${empty taskCommentsMap[task.id]}">
                                            <div class="text-center text-muted my-auto py-3">
                                                <i class="bi bi-chat-left-dots fs-6 d-block mb-1 opacity-50"></i>
                                                <span class="fs-9">Chưa có bình luận nào.</span>
                                            </div>
                                        </c:if>
                                    </div>
                                    <div class="pt-2 border-top">
                                        <form method="post" action="${pageContext.request.contextPath}/chat" class="d-flex flex-column gap-2">
                                            <input type="hidden" name="action" value="sendTaskComment">
                                            <input type="hidden" name="projectId" value="${project.id}">
                                            <input type="hidden" name="taskId" value="${task.id}">
                                            <div class="input-group input-group-sm">
                                                <input type="text" class="form-control fs-9 rounded-start-pill ps-3 shadow-none border-secondary-subtle"
                                                    name="content" placeholder="Bình luận..." autocomplete="off" required>
                                                <button type="submit" class="btn btn-primary-custom rounded-end-pill px-3 fs-9 fw-semibold shadow-sm">
                                                    <i class="bi bi-send-fill"></i>
                                                </button>
                                            </div>
                                        </form>
                                    </div>
                                </div>

                                <!-- FOOTER: XÓA TASK NẾU CÓ QUYỀN -->
                                <c:if test="${task.status != 'DONE' && (task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id)}">
                                    <div class="pt-2 mt-2 border-top text-end">
                                        <a href="${pageContext.request.contextPath}/task?action=delete&taskId=${task.id}&projectId=${project.id}"
                                            class="text-danger fs-9 text-decoration-none d-inline-flex align-items-center gap-1 opacity-75 hover-opacity-100"
                                            onclick="return confirm('Bạn có chắc chắn muốn xóa vĩnh viễn công việc này không?');">
                                            <i class="bi bi-trash3"></i> Xóa thẻ này
                                        </a>
                                    </div>
                                </c:if>
                            </div>
                        </div>
                    </div>

                    <!-- =========================================================================
                         B. PANE SUBTASKS CỦA TASK CHA #${task.id}
                         ========================================================================= -->
                    <c:forEach items="${taskSubTasksMap[task.id]}" var="st">
                        <div id="subtaskPane-${st.id}" class="subtask-detail-pane d-none" data-parent-id="${task.id}" data-subtask-id="${st.id}">
                            <!-- SUBTASK HEADER: BREADCRUMB QUAY LẠI TASK CHA & CONTROLS -->
                            <div class="drawer-header d-flex align-items-center justify-content-between">
                                <div class="d-flex align-items-center gap-2 flex-grow-1 me-3 text-truncate">
                                    <button type="button" class="breadcrumb-back-btn border-0" onclick="openClickUpTask(${task.id})">
                                        <i class="bi bi-arrow-left"></i> Quay lại: <span class="text-truncate d-inline-block align-bottom" style="max-width: 220px;">${task.title}</span>
                                    </button>
                                    <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5 fs-9 fw-semibold">#SUB-${st.id}</span>
                                    <span class="badge ${st.statusBadgeClass} rounded-pill px-2 py-0-5 fs-9 fw-semibold">${st.statusLabel}</span>
                                </div>
                                <div class="d-flex align-items-center gap-2 flex-shrink-0">
                                    <c:if test="${task.status != 'DONE' && (task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id)}">
                                        <button type="button" class="btn btn-outline-secondary btn-xs rounded-pill px-2-5 py-1 fs-9 d-flex align-items-center gap-1"
                                            data-bs-toggle="collapse" data-bs-target="#editSubTaskCollapse-${st.id}">
                                            <i class="bi bi-pencil-square"></i> Sửa việc con
                                        </button>
                                    </c:if>
                                    <button type="button" class="btn-close" data-bs-dismiss="offcanvas" aria-label="Đóng"></button>
                                </div>
                            </div>

                            <!-- SUBTASK BODY 2 CỘT -->
                            <div class="row g-0">
                                <!-- CỘT TRÁI (65%): THÔNG TIN VIỆC CON, TIẾN ĐỘ 5 BƯỚC, BÀN GIAO, NGHIỆM THU -->
                                <div class="col-12 col-lg-8 p-4 border-end">
                                    <!-- FORM SỬA SUBTASK (COLLAPSIBLE) -->
                                    <c:if test="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}">
                                        <div class="collapse mb-3" id="editSubTaskCollapse-${st.id}">
                                            <form method="post" action="${pageContext.request.contextPath}/task"
                                                class="p-3 bg-white rounded-3 border border-primary-subtle shadow-2xs">
                                                <input type="hidden" name="action" value="editSubTask">
                                                <input type="hidden" name="projectId" value="${project.id}">
                                                <input type="hidden" name="subTaskId" value="${st.id}">
                                                <div class="mb-2">
                                                    <label class="form-label fs-9 fw-bold text-dark mb-1">Tiêu đề việc con:</label>
                                                    <input type="text" name="title" value="${st.title}" class="form-control form-control-sm" required>
                                                </div>
                                                <div class="row g-2 mb-2">
                                                    <div class="col-6">
                                                        <label class="form-label fs-9 fw-bold text-dark mb-1">Người làm:</label>
                                                        <select name="assigneeId" class="form-select form-select-sm" required>
                                                            <c:forEach items="${userList}" var="u">
                                                                <option value="${u.id}" ${u.id==st.assigneeId ? 'selected' : ''}>${u.fullName}</option>
                                                            </c:forEach>
                                                        </select>
                                                    </div>
                                                    <div class="col-6">
                                                        <label class="form-label fs-9 fw-bold text-dark mb-1">Hạn chót:</label>
                                                        <input type="date" name="dueDate" value="${st.dueDate}" class="form-control form-control-sm"
                                                            <c:if test="${not empty task.dueDate}">max="${task.dueDate}"</c:if>>
                                                    </div>
                                                </div>
                                                <div class="d-flex justify-content-end gap-2">
                                                    <button type="button" class="btn btn-light btn-sm fs-9" data-bs-toggle="collapse" data-bs-target="#editSubTaskCollapse-${st.id}">Hủy</button>
                                                    <button type="submit" class="btn btn-primary btn-sm fs-9 fw-semibold">Lưu thay đổi</button>
                                                </div>
                                            </form>
                                        </div>
                                    </c:if>

                                    <!-- TIÊU ĐỀ SUBTASK -->
                                    <div class="mb-3">
                                        <h4 class="fw-bold text-dark mb-1 ${st.status == 'APPROVED' ? 'text-decoration-line-through text-muted' : ''}">${st.title}</h4>
                                        <div class="fs-8 text-muted">
                                            <i class="bi bi-diagram-2 me-1"></i> Thuộc Task Cha:
                                            <a href="javascript:void(0)" onclick="openClickUpTask(${task.id})" class="fw-semibold text-primary text-decoration-none">${task.title}</a>
                                        </div>
                                    </div>

                                    <!-- THANH TIẾN ĐỘ 5 TRẠNG THÁI SUBTASK (STEPPER) -->
                                    <div class="p-3 bg-light rounded-3 border mb-4">
                                        <div class="d-flex align-items-center justify-content-between mb-2">
                                            <span class="fs-9 fw-bold text-secondary text-uppercase tracking-wider">
                                                <i class="bi bi-diagram-3 text-primary me-1"></i> Quy trình nghiệm thu việc con:
                                            </span>
                                            <span class="badge ${st.statusBadgeClass} rounded-pill px-2 py-0-5 fs-9">${st.statusLabel}</span>
                                        </div>
                                        <div class="d-flex align-items-center justify-content-between gap-1 text-center">
                                            <!-- Step 1: TODO -->
                                            <div class="flex-fill p-1.5 rounded-2 ${st.status == 'TODO' ? 'bg-secondary text-white fw-bold shadow-2xs' : 'bg-white border text-muted'} fs-9">
                                                1. Cần làm
                                            </div>
                                            <i class="bi bi-arrow-right text-muted fs-9"></i>
                                            <!-- Step 2: SUBMITTED -->
                                            <div class="flex-fill p-1.5 rounded-2 ${st.status == 'SUBMITTED' ? 'bg-warning text-dark fw-bold shadow-2xs' : 'bg-white border text-muted'} fs-9">
                                                2. Chờ duyệt
                                            </div>
                                            <i class="bi bi-arrow-right text-muted fs-9"></i>
                                            <!-- Step 3: REVISE or REJECTED -->
                                            <div class="flex-fill p-1.5 rounded-2 ${st.status == 'REVISE' ? 'bg-info text-dark fw-bold shadow-2xs' : (st.status == 'REJECTED' ? 'bg-danger text-white fw-bold shadow-2xs' : 'bg-white border text-muted')} fs-9">
                                                <c:choose>
                                                    <c:when test="${st.status == 'REVISE'}">3. Cân chỉnh</c:when>
                                                    <c:when test="${st.status == 'REJECTED'}">3. Chưa đạt</c:when>
                                                    <c:otherwise>3. Cân chỉnh / Trả về</c:otherwise>
                                                </c:choose>
                                            </div>
                                            <i class="bi bi-arrow-right text-muted fs-9"></i>
                                            <!-- Step 4: APPROVED -->
                                            <div class="flex-fill p-1.5 rounded-2 ${st.status == 'APPROVED' ? 'bg-success text-white fw-bold shadow-2xs' : 'bg-white border text-muted'} fs-9">
                                                4. Đã duyệt 🟢
                                            </div>
                                        </div>
                                    </div>

                                    <!-- KẾT QUẢ NỘP BÀI CỦA THÀNH VIÊN (NẾU CÓ) -->
                                    <c:if test="${st.status == 'SUBMITTED' || not empty st.submissionNote}">
                                        <div class="p-3 bg-white rounded-3 border fs-8 text-dark mb-3 shadow-2xs">
                                            <div class="d-flex align-items-center justify-content-between mb-1 pb-1 border-bottom">
                                                <span class="fw-bold text-warning fs-9">
                                                    <i class="bi bi-file-earmark-check-fill me-1"></i> Báo cáo kết quả nộp bài của thành viên:
                                                </span>
                                                <span class="fs-9 text-muted">${st.submittedAt}</span>
                                            </div>
                                            <p class="mb-0 text-secondary fs-8" style="white-space: pre-line;">
                                                ${not empty st.submissionNote ? st.submissionNote : 'Đã hoàn thành công việc, mời Task Lead kiểm tra và nghiệm thu.'}
                                            </p>
                                        </div>
                                    </c:if>

                                    <!-- Ý KIẾN ĐÁNH GIÁ TỪ TASK LEAD -->
                                    <c:if test="${st.status == 'REVISE'}">
                                        <div class="p-3 bg-primary-subtle text-primary border border-primary-subtle rounded-3 fs-8 mb-3">
                                            <div class="d-flex align-items-center justify-content-between mb-1">
                                                <span class="fw-bold fs-9"><i class="bi bi-info-circle-fill me-1"></i> Dặn dò từ Task Lead:</span>
                                                <span class="fs-9 opacity-75">${st.reviewedAt}</span>
                                            </div>
                                            <p class="mb-0 fs-8">"${st.feedbackNote}"</p>
                                        </div>
                                    </c:if>
                                    <c:if test="${st.status == 'REJECTED'}">
                                        <div class="p-3 bg-danger-subtle text-danger border border-danger-subtle rounded-3 fs-8 mb-3">
                                            <div class="d-flex align-items-center justify-content-between mb-1">
                                                <span class="fw-bold fs-9"><i class="bi bi-exclamation-triangle-fill me-1"></i> Lý do chưa đạt từ Task Lead:</span>
                                                <span class="fs-9 opacity-75">${st.reviewedAt}</span>
                                            </div>
                                            <p class="mb-0 fs-8">"${st.feedbackNote}"</p>
                                        </div>
                                    </c:if>
                                    <c:if test="${st.status == 'APPROVED'}">
                                        <div class="p-3 bg-success-subtle text-success border border-success-subtle rounded-3 fs-8 mb-3 d-flex align-items-center gap-2">
                                            <i class="bi bi-check-circle-fill fs-6"></i>
                                            <span>Đã nghiệm thu hoàn thành 100% &bull; ${st.reviewedAt}</span>
                                        </div>
                                    </c:if>

                                    <!-- NÚT NỘP BÀI CHO THÀNH VIÊN ĐƯỢC GIAO -->
                                    <c:set var="canSubmitSubTask" value="${(not empty st.assigneeId && st.assigneeId > 0 && st.assigneeId == sessionScope.currentUser.id) || ((empty st.assigneeId || st.assigneeId == 0) && (task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id))}" />
                                    <c:if test="${task.status != 'DONE' && canSubmitSubTask && st.status != 'APPROVED' && st.status != 'SUBMITTED'}">
                                        <div class="mb-3">
                                            <button type="button" class="btn btn-primary btn-sm rounded-pill fs-8 py-1-5 px-3 fw-semibold shadow-2xs"
                                                data-bs-toggle="collapse" data-bs-target="#submitSubTaskPanel-${st.id}">
                                                <i class="bi bi-upload me-1"></i> ${st.status == 'TODO' ? 'Nộp Báo Cáo Kết Quả' : 'Nộp Lại Kết Quả Mới'}
                                            </button>
                                        </div>
                                        <!-- Collapse Form nộp bài -->
                                        <div class="collapse mb-3" id="submitSubTaskPanel-${st.id}">
                                            <div class="p-3 bg-white rounded-3 border border-primary shadow-sm">
                                                <h6 class="fw-bold text-dark fs-8 mb-2"><i class="bi bi-upload text-primary me-1"></i> Nộp Báo Cáo Kết Quả: [${st.title}]</h6>
                                                <form action="${pageContext.request.contextPath}/task" method="post">
                                                    <input type="hidden" name="action" value="submitSubTask">
                                                    <input type="hidden" name="projectId" value="${project.id}">
                                                    <input type="hidden" name="subTaskId" value="${st.id}">
                                                    <div class="mb-2">
                                                        <label for="note-${st.id}" class="form-label fw-semibold fs-9 text-dark mb-1">
                                                            Ghi chú hoàn thành / Link sản phẩm bàn giao <span class="text-danger">*</span>:
                                                        </label>
                                                        <textarea class="form-control fs-8 rounded-3" id="note-${st.id}" name="submissionNote" rows="2"
                                                            placeholder="Mô tả công việc đã làm, link pull request, link tài liệu cho Leader..." required></textarea>
                                                    </div>
                                                    <div class="d-flex align-items-center justify-content-end gap-2">
                                                        <button type="button" class="btn btn-light rounded-pill px-3 fs-9" data-bs-toggle="collapse" data-bs-target="#submitSubTaskPanel-${st.id}">Đóng</button>
                                                        <button type="submit" class="btn btn-primary-custom rounded-pill px-4 fs-9 fw-semibold shadow-sm">
                                                            <i class="bi bi-send-fill me-1"></i> Gửi Báo Cáo Cho Leader
                                                        </button>
                                                    </div>
                                                </form>
                                            </div>
                                        </div>
                                    </c:if>

                                    <!-- NÚT DUYỆT 3 LỰA CHỌN CHO TASK LEAD (KHI SUBMITTED) -->
                                    <c:set var="isReviewer" value="${(task.assigneeId > 0 && task.assigneeId == sessionScope.currentUser.id) || (task.assigneeId == 0 && project.ownerId == sessionScope.currentUser.id)}" />
                                    <c:if test="${task.status != 'DONE' && isReviewer && st.status == 'SUBMITTED'}">
                                        <div class="p-3 bg-light rounded-3 border mb-3">
                                            <span class="fs-9 fw-bold text-dark d-block mb-2"><i class="bi bi-shield-check text-primary me-1"></i> Quyền Thẩm Định Của Task Lead:</span>
                                            <div class="d-flex flex-wrap align-items-center gap-2">
                                                <!-- Duyệt Đạt -->
                                                <form method="post" action="${pageContext.request.contextPath}/task" class="m-0">
                                                    <input type="hidden" name="action" value="approveSubTask">
                                                    <input type="hidden" name="projectId" value="${project.id}">
                                                    <input type="hidden" name="subTaskId" value="${st.id}">
                                                    <button type="submit" class="btn btn-success btn-sm rounded-pill fs-9 py-1 px-3 fw-semibold shadow-2xs">
                                                        <i class="bi bi-check-lg me-1"></i> Duyệt Đạt (🟢)
                                                    </button>
                                                </form>
                                                <!-- Cân Chỉnh -->
                                                <button type="button" class="btn btn-primary btn-sm rounded-pill fs-9 py-1 px-3 fw-semibold shadow-2xs"
                                                    data-bs-toggle="collapse" data-bs-target="#reviseSubTaskPanel-${st.id}">
                                                    <i class="bi bi-pencil me-1"></i> Cân Chỉnh (🔵)
                                                </button>
                                                <!-- Chưa Đạt -->
                                                <button type="button" class="btn btn-danger btn-sm rounded-pill fs-9 py-1 px-3 fw-semibold shadow-2xs"
                                                    data-bs-toggle="collapse" data-bs-target="#rejectSubTaskPanel-${st.id}">
                                                    <i class="bi bi-x-lg me-1"></i> Chưa Đạt (🔴)
                                                </button>
                                            </div>

                                            <!-- Form Cân Chỉnh -->
                                            <div class="collapse mt-2" id="reviseSubTaskPanel-${st.id}">
                                                <div class="p-2.5 bg-white rounded-2 border border-primary shadow-sm">
                                                    <h6 class="fw-bold text-primary fs-9 mb-1"><i class="bi bi-pencil-square me-1"></i> Lời dặn dò cân chỉnh nhỏ:</h6>
                                                    <form action="${pageContext.request.contextPath}/task" method="post">
                                                        <input type="hidden" name="action" value="reviseSubTask">
                                                        <input type="hidden" name="projectId" value="${project.id}">
                                                        <input type="hidden" name="subTaskId" value="${st.id}">
                                                        <textarea class="form-control fs-9 rounded-2 mb-2" name="feedbackNote" rows="2" placeholder="Ví dụ: Format lại code, kiểm tra css..." required></textarea>
                                                        <div class="d-flex align-items-center justify-content-end gap-1">
                                                            <button type="button" class="btn btn-light rounded-pill px-2 py-0-5 fs-9" data-bs-toggle="collapse" data-bs-target="#reviseSubTaskPanel-${st.id}">Đóng</button>
                                                            <button type="submit" class="btn btn-primary rounded-pill px-3 py-0-5 fs-9 fw-semibold">Gửi Cân Chỉnh</button>
                                                        </div>
                                                    </form>
                                                </div>
                                            </div>

                                            <!-- Form Chưa Đạt -->
                                            <div class="collapse mt-2" id="rejectSubTaskPanel-${st.id}">
                                                <div class="p-2.5 bg-white rounded-2 border border-danger shadow-sm">
                                                    <h6 class="fw-bold text-danger fs-9 mb-1"><i class="bi bi-exclamation-triangle-fill me-1"></i> Lý do chưa đạt yêu cầu:</h6>
                                                    <form action="${pageContext.request.contextPath}/task" method="post">
                                                        <input type="hidden" name="action" value="rejectSubTask">
                                                        <input type="hidden" name="projectId" value="${project.id}">
                                                        <input type="hidden" name="subTaskId" value="${st.id}">
                                                        <textarea class="form-control fs-9 rounded-2 mb-2" name="feedbackNote" rows="2" placeholder="Nêu rõ lỗi hoặc yêu cầu làm lại..." required></textarea>
                                                        <div class="d-flex align-items-center justify-content-end gap-1">
                                                            <button type="button" class="btn btn-light rounded-pill px-2 py-0-5 fs-9" data-bs-toggle="collapse" data-bs-target="#rejectSubTaskPanel-${st.id}">Đóng</button>
                                                            <button type="submit" class="btn btn-danger rounded-pill px-3 py-0-5 fs-9 fw-semibold">Trả Về Làm Lại</button>
                                                        </div>
                                                    </form>
                                                </div>
                                            </div>
                                        </div>
                                    </c:if>
                                </div>

                                <!-- CỘT PHẢI (35%): THUỘC TÍNH VIỆC CON & ĐIỀU HƯỚNG -->
                                <div class="col-12 col-lg-4 p-4 bg-light-subtle d-flex flex-column justify-content-between" style="min-height: 480px;">
                                    <div>
                                        <div class="task-sidebar-section-title mb-2">
                                            <i class="bi bi-sliders me-1"></i> Thuộc tính việc con
                                        </div>
                                        <div class="task-property-list bg-white rounded-3 p-3 border shadow-2xs mb-3">
                                            <div class="task-property-row">
                                                <span class="task-property-label"><i class="bi bi-person"></i> Người làm</span>
                                                <span class="task-property-value d-flex align-items-center gap-1">
                                                    <span class="avatar-circle-sm bg-primary text-white rounded-circle d-inline-flex align-items-center justify-content-center"
                                                        style="width: 18px; height: 18px; font-size: 0.65rem;">
                                                        ${st.assigneeName.substring(0, 1).toUpperCase()}
                                                    </span>
                                                    <span>${st.assigneeName}</span>
                                                </span>
                                            </div>
                                            <div class="task-property-row">
                                                <span class="task-property-label"><i class="bi bi-arrow-repeat"></i> Trạng thái</span>
                                                <span class="task-property-value">
                                                    <span class="badge ${st.statusBadgeClass} rounded-pill px-2 py-0-5 fs-9">${st.statusLabel}</span>
                                                </span>
                                            </div>
                                            <div class="task-property-row">
                                                <span class="task-property-label"><i class="bi bi-calendar3"></i> Hạn chót</span>
                                                <span class="task-property-value">
                                                    <c:choose>
                                                        <c:when test="${not empty st.dueDate}">
                                                            <span class="badge ${st.deadlineBadgeClass} rounded-pill px-2 py-0-5 fs-9">
                                                                <i class="bi bi-calendar-event me-1"></i>${st.dueDate}
                                                            </span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="text-muted fs-9">Chưa đặt</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </span>
                                            </div>
                                            <div class="task-property-row">
                                                <span class="task-property-label"><i class="bi bi-diagram-2"></i> Task Cha</span>
                                                <span class="task-property-value text-truncate" style="max-width: 130px;" title="${task.title}">
                                                    #${task.id}
                                                </span>
                                            </div>
                                        </div>

                                        <!-- Nút quay lại Task Cha -->
                                        <button type="button" class="btn btn-outline-primary btn-sm w-100 rounded-pill fs-8 fw-semibold py-2 shadow-2xs mb-2"
                                            onclick="openClickUpTask(${task.id})">
                                            <i class="bi bi-arrow-left me-1"></i> Xem Chi Tiết Task Cha
                                        </button>
                                    </div>

                                    <!-- Footer: Xóa việc con nếu có quyền -->
                                    <c:if test="${task.status != 'DONE' && (task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id)}">
                                        <div class="pt-2 mt-2 border-top text-end">
                                            <form method="post" action="${pageContext.request.contextPath}/task" class="m-0 d-inline"
                                                onsubmit="return confirm('Bạn có chắc chắn muốn xóa việc con này?');">
                                                <input type="hidden" name="action" value="deleteSubTask">
                                                <input type="hidden" name="projectId" value="${project.id}">
                                                <input type="hidden" name="subTaskId" value="${st.id}">
                                                <button type="submit" class="btn btn-link text-danger p-0 border-0 fs-9 text-decoration-none opacity-75 hover-opacity-100">
                                                    <i class="bi bi-trash3 me-1"></i> Xóa việc con này
                                                </button>
                                            </form>
                                        </div>
                                    </c:if>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </c:forEach>
            </div>
        </div>

        <!-- =========================================================================
     5. MODAL THẺ HỒ SƠ ĐỒNG ĐỘI (SOCIAL PROFILE CARD MODAL - PHẦN B.3.3)
     ========================================================================= -->
        <c:forEach items="${userWorkloadList}" var="uw">
            <div class="modal fade" id="memberProfileModal-${uw.user.id}" tabindex="-1"
                aria-labelledby="memberProfileModalLabel-${uw.user.id}" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered modal-md">
                    <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">

                        <!-- Đầu Card: Cover & Avatar lớn -->
                        <div class="bg-primary-subtle p-4 text-center border-bottom position-relative">
                            <div class="avatar-circle mx-auto bg-primary text-white d-flex align-items-center justify-content-center rounded-circle shadow-sm mb-2"
                                style="width: 72px; height: 72px; font-size: 2rem;">
                                <i class="bi bi-person-fill"></i>
                            </div>
                            <h5 class="fw-bold text-dark mb-1" id="memberProfileModalLabel-${uw.user.id}">
                                ${uw.user.fullName}
                            </h5>
                            <div class="d-flex align-items-center justify-content-center gap-2">
                                <span
                                    class="badge ${uw.user.id == project.ownerId ? 'bg-warning text-dark' : 'bg-primary'} rounded-pill px-3 py-1 fs-8">
                                    ${uw.user.id == project.ownerId ? '👑 Trưởng Dự Án (PM)' : uw.user.role}
                                </span>
                                <span class="text-muted fs-8">
                                    <i class="bi bi-envelope me-1"></i> ${uw.user.email}
                                </span>
                            </div>
                            <button type="button" class="btn-close position-absolute top-0 end-0 m-3"
                                data-bs-dismiss="modal" aria-label="Đóng"></button>
                        </div>

                        <!-- Thân Card: Bảng Thống Kê Khối Lượng Công Việc -->
                        <div class="modal-body p-4">
                            <h6 class="fw-bold text-dark fs-7 mb-3 text-uppercase tracking-wider">
                                <i class="bi bi-graph-up-arrow text-primary me-1"></i> Khối lượng & Năng suất trong dự
                                án này:
                            </h6>

                            <div class="row g-3 text-center mb-4">
                                <!-- Chỉ số 1: Số Task Lead -->
                                <div class="col-4">
                                    <div class="p-3 bg-light rounded-3 border">
                                        <span class="fs-9 text-muted d-block mb-1">Task Lead</span>
                                        <h4 class="fw-extrabold text-primary mb-0">${uw.leadTaskCount}</h4>
                                        <span class="fs-9 text-muted">chủ trì</span>
                                    </div>
                                </div>

                                <!-- Chỉ số 2: Số Việc con -->
                                <div class="col-4">
                                    <div class="p-3 bg-light rounded-3 border">
                                        <span class="fs-9 text-muted d-block mb-1">Việc con</span>
                                        <h4 class="fw-extrabold text-dark mb-0">${uw.subTaskCount}</h4>
                                        <span class="fs-9 text-muted">được giao</span>
                                    </div>
                                </div>

                                <!-- Chỉ số 3: Đã hoàn thành -->
                                <div class="col-4">
                                    <div class="p-3 bg-success-subtle rounded-3 border border-success-subtle">
                                        <span class="fs-9 text-success d-block mb-1">Đã xong</span>
                                        <h4 class="fw-extrabold text-success mb-0">${uw.completedSubTaskCount}</h4>
                                        <span class="fs-9 text-success">hoàn tất</span>
                                    </div>
                                </div>
                            </div>

                            <!-- Tiến độ hoàn thành việc con -->
                            <div class="p-3 bg-light rounded-3 border">
                                <div class="d-flex align-items-center justify-content-between mb-1">
                                    <span class="fs-8 fw-semibold text-dark">Tỷ lệ hoàn thành việc con</span>
                                    <span class="fs-8 fw-bold text-success">
                                        <c:choose>
                                            <c:when test="${uw.subTaskCount > 0}">
                                                ${Math.round((uw.completedSubTaskCount * 100.0) / uw.subTaskCount)}%
                                            </c:when>
                                            <c:otherwise>
                                                100%
                                            </c:otherwise>
                                        </c:choose>
                                    </span>
                                </div>
                                <div class="progress" style="height: 6px;">
                                    <div class="progress-bar bg-success rounded-pill" role="progressbar"
                                        style="width: ${uw.subTaskCount > 0 ? Math.round((uw.completedSubTaskCount * 100.0) / uw.subTaskCount) : 100}%;"
                                        aria-valuenow="${uw.subTaskCount > 0 ? Math.round((uw.completedSubTaskCount * 100.0) / uw.subTaskCount) : 100}"
                                        aria-valuemin="0" aria-valuemax="100"></div>
                                </div>
                            </div>

                            <!-- KHỐI 2: DANH SÁCH TASK LỚN ĐANG CHỦ TRÌ (TASK LEAD) -->
                            <div class="mt-4 pt-3 border-top">
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <h6 class="fw-bold text-dark fs-7 mb-0 text-uppercase tracking-wider">
                                        <i class="bi bi-award-fill text-warning me-1"></i> Các Task lớn đang chủ trì
                                        (${uw.leadTasks.size()}):
                                    </h6>
                                    <span class="badge bg-primary-subtle text-primary rounded-pill px-2 py-0 fs-9">
                                        ${uw.leadTasks.size()} Task
                                    </span>
                                </div>

                                <c:choose>
                                    <c:when test="${not empty uw.leadTasks}">
                                        <div class="d-flex flex-column gap-2">
                                            <c:forEach items="${uw.leadTasks}" var="leadTask">
                                                <div
                                                    class="p-2 px-3 bg-light rounded-3 border d-flex align-items-center justify-content-between">
                                                    <div class="d-flex align-items-center gap-2 text-truncate me-2">
                                                        <span
                                                            class="badge ${leadTask.priorityBadgeClass} rounded-pill px-2 py-0 fs-9">${leadTask.priorityLabel}</span>
                                                        <span class="fs-8 fw-semibold text-dark text-truncate"
                                                            title="${leadTask.title}">
                                                            ${leadTask.title}
                                                        </span>
                                                    </div>
                                                    <div class="d-flex align-items-center gap-1 text-nowrap">
                                                        <span
                                                            class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-0 fs-9 fw-bold"
                                                            title="Tiến độ Task">
                                                            ${taskProgressMap[leadTask.id]}%
                                                        </span>
                                                        <span
                                                            class="badge ${leadTask.status == 'DONE' ? 'bg-success' : (leadTask.status == 'IN_PROGRESS' ? 'bg-primary' : 'bg-secondary')} rounded-pill px-2 py-0 fs-9">
                                                            ${leadTask.status == 'DONE' ? 'Đã xong' : (leadTask.status == 'IN_PROGRESS' ? 'Đang làm' : 'Cần làm')}
                                                        </span>
                                                    </div>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="p-3 bg-light-subtle rounded-3 text-muted fs-8 border text-center">
                                            <i class="bi bi-inbox d-block fs-5 mb-1 opacity-50"></i>
                                            Chưa chủ trì Task lớn nào trong dự án này.
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>

                        <!-- Chân Card: Nút đóng -->
                        <div class="modal-footer px-4 py-3 bg-light border-0">
                            <button type="button"
                                class="btn btn-primary-custom w-100 rounded-pill fs-8 fw-semibold shadow-sm"
                                data-bs-dismiss="modal">
                                <i class="bi bi-check-lg me-1"></i> Đóng thẻ hồ sơ
                            </button>
                        </div>

                    </div>
                </div>
            </div>
        </c:forEach>

        <!-- ==========================================
     6. MODAL POPUP: FORM "+ THÊM CÔNG VIỆC MỚI" (UC05)
     ========================================== -->
        <div class="modal fade" id="addTaskModal" tabindex="-1" aria-labelledby="addTaskModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-lg">
                <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">

                    <div class="modal-header bg-white px-4 py-3 border-bottom d-flex align-items-center justify-content-between">
                        <div class="d-flex align-items-center gap-2">
                            <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2-5 py-1 fs-9 fw-bold">
                                <i class="bi bi-plus-circle-fill me-1"></i> TASK MỚI
                            </span>
                            <span class="fs-8 text-muted">trong <strong>${project.name}</strong></span>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <span class="fs-9 text-muted d-none d-sm-inline"><kbd>Ctrl</kbd> + <kbd>Enter</kbd> để tạo nhanh</span>
                            <button type="button" class="btn-close fs-9" data-bs-dismiss="modal" aria-label="Đóng"></button>
                        </div>
                    </div>

                    <form method="post" action="${pageContext.request.contextPath}/task">

                        <input type="hidden" name="action" value="add">
                        <input type="hidden" name="projectId" value="${project.id}">

                        <div class="modal-body px-4 py-4">

                            <div class="mb-3">
                                <input type="text" class="form-control clickup-modal-title-input" id="taskTitle"
                                    name="title" placeholder="Tiêu đề công việc..." required autocomplete="off">
                            </div>

                            <div class="mb-3">
                                <label for="taskDescription" class="form-label fw-semibold text-dark fs-7">Mô tả yêu
                                    cầu</label>
                                <textarea class="form-control rounded-3 p-3 fs-7" id="taskDescription"
                                    name="description" rows="3"
                                    placeholder="Mô tả mục tiêu của Module này..."></textarea>
                            </div>

                            <!-- Chọn Nhãn phân loại (Labels / Tags) -->
                            <div class="mb-3">
                                <div class="d-flex align-items-center gap-2 mb-2">
                                    <label class="form-label fw-semibold text-dark fs-7 mb-0">
                                        <i class="bi bi-tags me-1 text-primary"></i> Nhãn phân loại (Labels)
                                    </label>

                                    <!-- NÚT BẬT FORM CON SỔ RA TRỰC TIẾP TRONG FORM THÊM TASK -->
                                    <button type="button"
                                        class="btn btn-sm btn-light border border-secondary-subtle text-primary rounded-pill px-2-5 py-0-5 fs-8 fw-semibold d-inline-flex align-items-center gap-1 shadow-2xs"
                                        data-bs-toggle="collapse" data-bs-target="#inlineCreateLabelBox"
                                        aria-expanded="false" title="Mở bảng tạo nhãn con bên trong">
                                        <i class="bi bi-plus-circle-dotted"></i> Thêm nhãn
                                    </button>
                                </div>

                                <!-- Danh sách các nút nhãn hiện có -->
                                <div class="d-flex flex-wrap gap-2 mb-2" id="labelButtonGroup">
                                    <button type="button"
                                        class="btn btn-sm btn-outline-danger rounded-pill px-3 py-1 fs-8 fw-semibold label-toggle-btn"
                                        data-label="BUG" onclick="toggleTaskLabel(this, 'BUG')">🔴 Bug</button>
                                    <button type="button"
                                        class="btn btn-sm btn-outline-primary rounded-pill px-3 py-1 fs-8 fw-semibold label-toggle-btn"
                                        data-label="FEATURE" onclick="toggleTaskLabel(this, 'FEATURE')">✨
                                        Feature</button>
                                    <button type="button"
                                        class="btn btn-sm btn-outline-purple rounded-pill px-3 py-1 fs-8 fw-semibold label-toggle-btn"
                                        data-label="UI" onclick="toggleTaskLabel(this, 'UI')">🎨 UI/UX</button>
                                    <button type="button"
                                        class="btn btn-sm btn-outline-warning rounded-pill px-3 py-1 fs-8 fw-semibold label-toggle-btn text-dark"
                                        data-label="BACKEND" onclick="toggleTaskLabel(this, 'BACKEND')">⚙️
                                        Backend</button>
                                    <button type="button"
                                        class="btn btn-sm btn-outline-success rounded-pill px-3 py-1 fs-8 fw-semibold label-toggle-btn"
                                        data-label="DOCS" onclick="toggleTaskLabel(this, 'DOCS')">📚 Docs</button>
                                </div>
                                <input type="hidden" name="labels" id="taskSelectedLabels" value="">

                                <!-- ==========================================================
                             BẢNG CON TẠO NHÃN: NẰM GỌN TRỰC TIẾP TRONG FORM THÊM TASK
                             ========================================================== -->
                                <div class="collapse mt-2" id="inlineCreateLabelBox">
                                    <div class="p-3 bg-light border border-primary-subtle rounded-3 shadow-2xs">
                                        <div
                                            class="d-flex align-items-center justify-content-between mb-2 pb-1 border-bottom">
                                            <span class="fs-8 fw-bold text-primary">
                                                <i class="bi bi-tag-fill me-1"></i> Tạo nhãn tùy biến mới:
                                            </span>
                                            <button type="button" class="btn-close fs-9" data-bs-toggle="collapse"
                                                data-bs-target="#inlineCreateLabelBox" aria-label="Đóng"></button>
                                        </div>

                                        <div class="row g-2 align-items-end">
                                            <div class="col-12 col-md-5">
                                                <label class="form-label fs-9 fw-semibold text-dark mb-1">Tên
                                                    nhãn:</label>
                                                <input type="text" id="inlineLabelName"
                                                    class="form-control form-control-sm rounded-2 fs-8"
                                                    placeholder="Ví dụ: Security, API..." maxlength="30"
                                                    autocomplete="off">
                                            </div>

                                            <div class="col-12 col-md-4">
                                                <label class="form-label fs-9 fw-semibold text-dark mb-1">Màu
                                                    sắc:</label>
                                                <select class="form-select form-select-sm rounded-2 fs-8"
                                                    id="inlineLabelColor">
                                                    <option value="red">🔴 Đỏ Coral</option>
                                                    <option value="blue" selected>🔵 Xanh Dương</option>
                                                    <option value="purple">🟣 Tím Pastel</option>
                                                    <option value="amber">🟡 Vàng Hổ Phách</option>
                                                    <option value="green">🟢 Xanh Lá</option>
                                                    <option value="pink">🌸 Hồng</option>
                                                    <option value="cyan">💎 Xanh Ngọc</option>
                                                    <option value="slate">🔘 Xám Slate</option>
                                                </select>
                                            </div>

                                            <div class="col-12 col-md-3">
                                                <button type="button"
                                                    class="btn btn-sm btn-primary-custom w-100 rounded-2 fs-8 fw-semibold"
                                                    onclick="handleQuickCreateLabel()">
                                                    <i class="bi bi-plus-lg me-1"></i> Tạo & Chọn
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="row g-3 mb-3">
                                <div class="col-12 col-md-6">
                                    <label for="taskPriority" class="form-label fw-semibold text-dark fs-7">Mức độ ưu
                                        tiên</label>
                                    <select class="form-select rounded-3 py-2 px-3 fs-7" id="taskPriority"
                                        name="priority">
                                        <option value="HIGH">🔴 Cao (High)</option>
                                        <option value="MEDIUM" selected>🟡 Trung bình (Medium)</option>
                                        <option value="LOW">🔵 Thấp (Low)</option>
                                    </select>
                                </div>

                                <div class="col-12 col-md-6">
                                    <label for="taskDueDate" class="form-label fw-semibold text-dark fs-7">Hạn hoàn
                                        thành</label>
                                    <input type="date" class="form-control rounded-3 py-2 px-3 fs-7" id="taskDueDate"
                                        name="dueDate">
                                </div>
                            </div>

                            <div class="row g-3 mb-2">
                                <c:choose>
                                    <c:when test="${project.teamProject}">
                                        <div class="col-12 col-md-6">
                                            <label for="taskAssignee" class="form-label fw-semibold text-dark fs-7">Chỉ định
                                                Trưởng nhóm Task (Lead)</label>
                                            <select class="form-select rounded-3 py-2 px-3 fs-7" id="taskAssignee"
                                                name="assigneeId">
                                                <option value="0">-- Chưa chỉ định --</option>
                                                <c:forEach items="${userList}" var="u">
                                                    <option value="${u.id}">${u.fullName} (${u.role})</option>
                                                </c:forEach>
                                            </select>
                                        </div>
                                        <div class="col-12 col-md-6">
                                    </c:when>
                                    <c:otherwise>
                                        <input type="hidden" name="assigneeId" value="${sessionScope.currentUser.id}">
                                        <div class="col-12">
                                    </c:otherwise>
                                </c:choose>
                                    <label for="taskDocSelect" class="form-label fw-semibold text-dark fs-7">
                                        <i class="bi bi-paperclip me-1 text-primary"></i> Đính kèm tài liệu Wiki
                                    </label>
                                    <c:choose>
                                        <c:when test="${not empty docList}">
                                            <select class="form-select rounded-3 py-1 px-3 fs-8" id="taskDocSelect"
                                                name="docIds" multiple size="3"
                                                title="Giữ Ctrl hoặc Cmd để chọn nhiều tài liệu">
                                                <c:forEach items="${docList}" var="docItem">
                                                    <option value="${docItem.id}">📄 ${docItem.title}</option>
                                                </c:forEach>
                                            </select>
                                            <div class="form-text fs-9 text-muted mt-1">Giữ phím <kbd>Ctrl</kbd> để chọn
                                                nhiều tài liệu cùng lúc.</div>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="p-2 bg-light rounded-3 text-muted fs-8 border">
                                                Chưa có tài liệu nào trong dự án.
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>

                            <c:if test="${project.teamProject}">
                                <div class="p-3 bg-light rounded-3 border mb-2">
                                    <div class="form-check form-switch mb-1">
                                        <input class="form-check-input" type="checkbox" role="switch" id="taskRequiresGate" name="requiresGate" value="true" checked>
                                        <label class="form-check-label fw-bold text-dark fs-8" for="taskRequiresGate">
                                            <i class="bi bi-shield-check text-primary me-1"></i> Áp dụng Cổng Chất Lượng (Quality Gate)
                                        </label>
                                    </div>
                                    <p class="fs-9 text-muted mb-0">
                                        Khi bật, công việc bắt buộc phân rã việc con, được PM duyệt kế hoạch (Gate 1) và nghiệm thu chấm điểm (Gate 2). Tắt để làm nhanh không kiểm duyệt (Fast-track).
                                    </p>
                                </div>
                            </c:if>

                        </div>

                        <div class="modal-footer px-4 py-3 bg-light border-0 d-flex align-items-center justify-content-between">
                            <div class="text-muted fs-9">
                                <i class="bi bi-lightning-charge-fill text-warning me-1"></i>Phím tắt: <kbd>Ctrl</kbd> + <kbd>Enter</kbd>
                            </div>
                            <div class="d-flex align-items-center gap-2">
                                <button type="button" class="btn btn-light rounded-pill px-3 fs-7 fw-medium"
                                    data-bs-dismiss="modal">Hủy</button>
                                <button type="submit"
                                    class="btn btn-primary-custom rounded-pill px-4 py-2 fs-7 fw-semibold shadow-sm">
                                    <i class="bi bi-check-lg me-1"></i> Lưu công việc
                                </button>
                            </div>
                        </div>

                    </form>
                </div>
            </div>
        </div>

        <!-- =========================================================================
     7. MODAL: XEM & QUẢN LÝ ĐỘI NGŨ DỰ ÁN (X/10 THÀNH VIÊN)
     ========================================================================= -->
        <div class="modal fade" id="projectTeamModal" tabindex="-1" aria-labelledby="projectTeamModalLabel"
            aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-lg">
                <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">

                    <div class="modal-header bg-dark-navy text-white px-4 py-3 border-0">
                        <div class="d-flex align-items-center gap-2">
                            <i class="bi bi-people-fill text-warning fs-5"></i>
                            <div>
                                <h5 class="modal-title fw-bold mb-0" id="projectTeamModalLabel">Đội Ngũ Dự Án & Lời Mời
                                </h5>
                                <span class="fs-9 text-white-50">Hạn ngạch: <strong>${memberCount}/10</strong> thành
                                    viên</span>
                            </div>
                        </div>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"
                            aria-label="Đóng"></button>
                    </div>

                    <div class="modal-body p-4">
                        <!-- Navigation Tabs phong cách ClickUp 3.0 -->
                        <ul class="nav nav-pills nav-fill mb-4 bg-light p-1 rounded-pill border" id="projectTeamTabs" role="tablist">
                            <li class="nav-item" role="presentation">
                                <button class="nav-link active rounded-pill fs-8 fw-semibold py-1-5" id="tab-workload-btn" data-bs-toggle="pill" data-bs-target="#tab-workload-pane" type="button" role="tab" aria-controls="tab-workload-pane" aria-selected="true">
                                    <i class="bi bi-bar-chart-line-fill me-1 text-primary"></i> Khối lượng & Tiến độ
                                </button>
                            </li>
                            <li class="nav-item" role="presentation">
                                <button class="nav-link rounded-pill fs-8 fw-semibold py-1-5" id="tab-members-btn" data-bs-toggle="pill" data-bs-target="#tab-members-pane" type="button" role="tab" aria-controls="tab-members-pane" aria-selected="false">
                                    <i class="bi bi-people-fill me-1 text-success"></i> Quản lý Thành viên & Lời mời (${projectMemberList.size()})
                                </button>
                            </li>
                        </ul>

                        <div class="tab-content" id="projectTeamTabsContent">
                            <!-- TAB 1: KHỐI LƯỢNG & TIẾN ĐỘ THÀNH VIÊN (CLICKUP 3.0 WORKLOAD) -->
                            <div class="tab-pane fade show active" id="tab-workload-pane" role="tabpanel" aria-labelledby="tab-workload-btn">
                                <div class="d-flex align-items-center justify-content-between mb-3">
                                    <h6 class="fw-bold text-dark fs-7 mb-0 text-uppercase tracking-wider">
                                        <i class="bi bi-pie-chart-fill text-primary me-1"></i> Phân công & Khối lượng công việc:
                                    </h6>
                                    <span class="fs-9 text-muted"><strong>${userWorkloadList.size()}</strong> thành viên có nhiệm vụ</span>
                                </div>

                                <div class="d-flex flex-column gap-2-5">
                                    <c:forEach items="${userWorkloadList}" var="uw">
                                        <div class="p-3 bg-white rounded-3 border shadow-2xs">
                                            <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-2">
                                                <!-- Avatar & Tên thành viên -->
                                                <div class="d-flex align-items-center gap-2-5">
                                                    <div class="avatar-circle-sm bg-primary text-white rounded-circle d-flex align-items-center justify-content-center fw-bold fs-7" style="width: 34px; height: 34px;">
                                                        ${uw.user.fullName.substring(0, 1).toUpperCase()}
                                                    </div>
                                                    <div>
                                                        <div class="d-flex align-items-center gap-1-5">
                                                            <span class="fw-bold text-dark fs-7">${uw.user.fullName}</span>
                                                            <c:if test="${uw.user.id == project.ownerId}">
                                                                <span class="badge bg-warning-subtle text-dark rounded-pill px-1-5 py-0 fs-10 fw-semibold">PM</span>
                                                            </c:if>
                                                        </div>
                                                        <span class="fs-9 text-muted">${uw.user.email}</span>
                                                    </div>
                                                </div>

                                                <!-- Nút xem nhanh các task của thành viên này -->
                                                <div class="d-flex align-items-center gap-2">
                                                    <c:if test="${uw.overdueCount > 0}">
                                                        <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-2 py-0-5 fs-9 fw-bold" title="${uw.overdueCount} công việc đã quá hạn!">
                                                            <i class="bi bi-exclamation-triangle-fill me-1"></i>${uw.overdueCount} Quá hạn
                                                        </span>
                                                    </c:if>
                                                    <button type="button" class="btn btn-xs btn-outline-primary rounded-pill px-2-5 py-1 fs-9 fw-semibold"
                                                            onclick="bootstrap.Modal.getInstance(document.getElementById('projectTeamModal')).hide(); filterClickUpTasks('USER', '${uw.user.id}', '<c:out value="${uw.user.fullName}" />');"
                                                            title="Xem tất cả việc của ${uw.user.fullName}">
                                                        <i class="bi bi-funnel me-1"></i>Xem việc (${uw.totalTasks})
                                                    </button>
                                                </div>
                                            </div>

                                            <!-- Thanh tiến độ phân đoạn (Segmented Progress Bar) -->
                                            <div class="d-flex align-items-center gap-2 mb-2">
                                                <div class="progress flex-grow-1" style="height: 7px; background-color: #f1f5f9; border-radius: 9999px; overflow: hidden;">
                                                    <c:set var="pctDone" value="${uw.totalTasks > 0 ? (uw.doneCount * 100 / uw.totalTasks) : 0}" />
                                                    <c:set var="pctSubmitted" value="${uw.totalTasks > 0 ? (uw.submittedCount * 100 / uw.totalTasks) : 0}" />
                                                    <c:set var="pctInProg" value="${uw.totalTasks > 0 ? ((uw.inProgressCount - uw.submittedCount) * 100 / uw.totalTasks) : 0}" />
                                                    <div class="progress-bar bg-success" style="width: ${pctDone}%;" title="Đã xong: ${uw.doneCount}"></div>
                                                    <div class="progress-bar" style="width: ${pctSubmitted}%; background-color: #8b5cf6;" title="Chờ PM duyệt: ${uw.submittedCount}"></div>
                                                    <div class="progress-bar bg-primary" style="width: ${pctInProg}%;" title="Đang làm: ${uw.inProgressCount - uw.submittedCount}"></div>
                                                </div>
                                                <span class="fs-9 fw-bold text-dark tabular-nums">${uw.memberProgressPercentage}%</span>
                                            </div>

                                            <!-- Chi tiết trạng thái (Pills) -->
                                            <div class="d-flex align-items-center flex-wrap gap-1-5 fs-9">
                                                <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5" title="Chờ làm">
                                                    <i class="bi bi-circle me-1 text-secondary"></i>${uw.todoCount} Cần làm
                                                </span>
                                                <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2 py-0-5" title="Đang làm">
                                                    <i class="bi bi-play-circle me-1"></i>${uw.inProgressCount} Đang làm
                                                </span>
                                                <c:if test="${uw.submittedCount > 0}">
                                                    <span class="badge bg-purple text-white rounded-pill px-2 py-0-5" title="Đã nộp báo cáo chờ duyệt">
                                                        <i class="bi bi-send-check me-1"></i>${uw.submittedCount} Chờ duyệt
                                                    </span>
                                                </c:if>
                                                <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-0-5" title="Đã xong">
                                                    <i class="bi bi-check-circle-fill me-1"></i>${uw.doneCount} Đã xong
                                                </span>
                                            </div>
                                        </div>
                                    </c:forEach>

                                    <c:if test="${empty userWorkloadList}">
                                        <div class="text-center py-4 text-muted">
                                            <i class="bi bi-inbox fs-2 d-block mb-1 text-secondary opacity-50"></i>
                                            <span class="fs-8">Chưa có dữ liệu phân công công việc trong dự án</span>
                                        </div>
                                    </c:if>
                                </div>
                            </div>

                            <!-- TAB 2: QUẢN LÝ THÀNH VIÊN & LỜI MỜI (EXISTING) -->
                            <div class="tab-pane fade" id="tab-members-pane" role="tabpanel" aria-labelledby="tab-members-btn">
                                <!-- PHẦN 1: DANH SÁCH THÀNH VIÊN ĐANG THAM GIA -->
                                <div class="mb-4">
                                    <div class="d-flex align-items-center justify-content-between mb-2">
                                        <h6 class="fw-bold text-dark fs-7 mb-0 text-uppercase tracking-wider">
                                            <i class="bi bi-person-check-fill text-success me-1"></i> Thành viên chính thức
                                            (${projectMemberList.size()}):
                                        </h6>
                                        <span class="badge bg-success-subtle text-success rounded-pill px-2 py-0 fs-9">Đang hoạt
                                            động</span>
                                    </div>

                                    <div class="d-flex flex-column gap-2">
                                        <c:forEach items="${projectMemberList}" var="pm">
                                            <div
                                                class="p-3 bg-light rounded-3 border d-flex align-items-center justify-content-between">
                                                <div class="d-flex align-items-center gap-3">
                                                    <div class="avatar-sm rounded-circle ${pm.projectRole == 'OWNER' ? 'bg-warning text-dark' : 'bg-primary text-white'} d-flex align-items-center justify-content-center fw-bold fs-7"
                                                        style="width: 36px; height: 36px;">
                                                        <c:choose>
                                                            <c:when test="${pm.projectRole == 'OWNER'}"><i
                                                                    class="bi bi-star-fill"></i></c:when>
                                                            <c:otherwise><i class="bi bi-person-fill"></i></c:otherwise>
                                                        </c:choose>
                                                    </div>
                                                    <div>
                                                        <div class="d-flex align-items-center gap-2">
                                                            <span class="fw-bold text-dark fs-7">${pm.userName}</span>
                                                            <span
                                                                class="badge ${pm.projectRole == 'OWNER' ? 'bg-warning text-dark' : 'bg-secondary'} rounded-pill px-2 py-0 fs-9">
                                                                ${pm.projectRole == 'OWNER' ? 'Trưởng Dự Án (PM)' : 'Thành viên'}
                                                            </span>
                                                        </div>
                                                        <div class="text-muted fs-8">
                                                            ${pm.userEmail} &bull; <span
                                                                class="text-primary">${pm.userRole}</span>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div class="d-flex align-items-center gap-3">
                                                    <div class="text-end">
                                                        <span class="fs-9 text-muted d-block">Gia nhập:</span>
                                                        <span class="fs-9 fw-semibold text-secondary">${pm.joinedAt}</span>
                                                    </div>
                                                    <c:if
                                                        test="${project.ownerId == sessionScope.currentUser.id && pm.userId != project.ownerId}">
                                                        <form method="post" action="${pageContext.request.contextPath}/invite"
                                                            class="m-0"
                                                            onsubmit="return confirm('Bạn có chắc chắn muốn mời thành viên [${pm.userName}] rời khỏi dự án?');">
                                                            <input type="hidden" name="action" value="kick">
                                                            <input type="hidden" name="projectId" value="${project.id}">
                                                            <input type="hidden" name="userId" value="${pm.userId}">
                                                            <button type="submit"
                                                                class="btn btn-outline-danger btn-sm py-1 px-2 rounded-pill fs-9"
                                                                title="Mời rời dự án">
                                                                <i class="bi bi-person-x-fill me-1"></i> Mời rời
                                                            </button>
                                                        </form>
                                                    </c:if>
                                                </div>
                                            </div>
                                        </c:forEach>
                                    </div>

                                    <c:if test="${project.ownerId != sessionScope.currentUser.id}">
                                        <div class="mt-3 pt-2 text-end">
                                            <form method="post" action="${pageContext.request.contextPath}/invite"
                                                class="m-0 d-inline"
                                                onsubmit="return confirm('Bạn có chắc chắn muốn rời khỏi dự án [${project.name}]? Bạn sẽ không thể truy cập lại trừ khi được mời lại.');">
                                                <input type="hidden" name="action" value="leave">
                                                <input type="hidden" name="projectId" value="${project.id}">
                                                <button type="submit" class="btn btn-outline-danger btn-sm rounded-pill fs-8">
                                                    <i class="bi bi-box-arrow-left me-1"></i> Rời khỏi dự án này
                                                </button>
                                            </form>
                                        </div>
                                    </c:if>
                                </div>

                                <!-- PHẦN 2: LỜI MỜI / YÊU CẦU ĐANG CHỜ PHẢN HỒI (PENDING) -->
                                <c:if test="${not empty projectInviteList}">
                                    <div class="mt-4 pt-3 border-top">
                                        <div class="d-flex align-items-center justify-content-between mb-2">
                                            <h6 class="fw-bold text-dark fs-7 mb-0 text-uppercase tracking-wider">
                                                <i class="bi bi-hourglass-split text-warning me-1"></i> Lời mời & Yêu cầu đang
                                                chờ (${projectInviteList.size()}):
                                            </h6>
                                            <span
                                                class="badge bg-warning-subtle text-dark rounded-pill px-2 py-0 fs-9">PENDING</span>
                                        </div>

                                        <div class="d-flex flex-column gap-2">
                                            <c:forEach items="${projectInviteList}" var="inv">
                                                <div
                                                    class="p-2 px-3 bg-white rounded-3 border d-flex align-items-center justify-content-between shadow-2xs">
                                                    <div>
                                                        <div class="d-flex align-items-center gap-2">
                                                            <span
                                                                class="badge ${inv.statusBadgeClass} rounded-pill px-2 py-0 fs-9">${inv.statusLabel}</span>
                                                            <span class="fs-8 fw-bold text-dark">${inv.type == 'INVITATION' ? inv.receiverName : inv.senderName}</span>
                                                            <span class="fs-9 text-muted">(${inv.type == 'INVITATION' ? 'Được PM mời' : 'Gửi đơn xin vào'})</span>
                                                        </div>
                                                        <span class="fs-9 text-danger d-block mt-1">
                                                            <i class="bi bi-clock me-1"></i> Hạn: ${inv.expiredAt}
                                                        </span>
                                                    </div>

                                                    <!-- Nút PM Thu hồi lời mời nếu còn PENDING -->
                                                    <c:if
                                                        test="${project.ownerId == sessionScope.currentUser.id && inv.status == 'PENDING'}">
                                                        <form method="post" action="${pageContext.request.contextPath}/invite"
                                                            class="m-0"
                                                            onsubmit="return confirm('Bạn có chắc chắn muốn thu hồi lời mời này?');">
                                                            <input type="hidden" name="action" value="revoke">
                                                            <input type="hidden" name="inviteId" value="${inv.id}">
                                                            <button type="submit"
                                                                class="btn btn-outline-danger btn-sm rounded-pill fs-9 py-1 px-3"
                                                                title="Thu hồi lời mời">
                                                                <i class="bi bi-x-circle me-1"></i> Thu hồi
                                                            </button>
                                                        </form>
                                                    </c:if>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </div>
                                </c:if>
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer px-4 py-3 bg-light border-0">
                        <button type="button" class="btn btn-secondary rounded-pill px-4 fs-8 fw-semibold"
                            data-bs-dismiss="modal">Đóng</button>
                    </div>

                </div>
            </div>
        </div>

        <!-- =========================================================================
     8. MODAL: FORM MỜI THÀNH VIÊN VÀO DỰ ÁN (DÀNH RIÊNG CHO PM)
     ========================================================================= -->
        <c:if test="${project.ownerId == sessionScope.currentUser.id}">
            <div class="modal fade" id="inviteMemberModal" tabindex="-1" aria-labelledby="inviteMemberModalLabel"
                aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                        <div class="modal-header border-0 pb-0">
                            <h5 class="modal-title fw-bold text-dark" id="inviteMemberModalLabel">
                                <i class="bi bi-person-plus-fill text-success me-2"></i>Mời thành viên mới
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                        </div>

                        <form action="${pageContext.request.contextPath}/invite" method="post">
                            <input type="hidden" name="action" value="sendInvite">
                            <input type="hidden" name="projectId" value="${project.id}">

                            <div class="modal-body py-3">
                                <p class="text-muted fs-8 mb-3">
                                    Nhập <strong>Username hoặc Email</strong> của tài khoản bạn muốn mời vào dự án
                                    <strong>[${project.name}]</strong>. Lời mời sẽ có hiệu lực trong vòng <strong>7
                                        ngày</strong>.
                                </p>

                                <div class="mb-3">
                                    <label for="inputUsernameOrEmail" class="form-label fw-semibold fs-7 text-dark">
                                        Chọn tài khoản hoặc nhập Username / Email <span class="text-danger">*</span>
                                    </label>
                                    <c:if test="${not empty inviteCandidates}">
                                        <div class="mb-2">
                                            <select class="form-select fs-7 rounded-3"
                                                onchange="if(this.value) document.getElementById('inputUsernameOrEmail').value = this.value;">
                                                <option value="">-- Chọn nhanh tài khoản trong hệ thống --</option>
                                                <c:forEach items="${inviteCandidates}" var="cand">
                                                    <option value="${cand.username}">${cand.fullName} (@${cand.username}
                                                        - ${cand.role})</option>
                                                </c:forEach>
                                            </select>
                                        </div>
                                    </c:if>
                                    <div class="input-group">
                                        <span class="input-group-text bg-light border-end-0 fs-7 text-muted">
                                            <i class="bi bi-person-badge"></i>
                                        </span>
                                        <input type="text" class="form-control fs-7 rounded-end-3"
                                            id="inputUsernameOrEmail" name="usernameOrEmail"
                                            placeholder="Hoặc tự gõ: chi hoặc chi@teamwork.com" required autofocus>
                                    </div>
                                </div>

                                <div class="p-3 bg-light rounded-3 border">
                                    <div class="d-flex align-items-center justify-content-between fs-9 text-muted mb-1">
                                        <span>Hạn ngạch thành viên:</span>
                                        <strong class="text-dark">${memberCount}/10 người</strong>
                                    </div>
                                    <div class="progress" style="height: 5px;">
                                        <div class="progress-bar bg-success" role="progressbar"
                                            style="width: ${memberCount * 10}%;"></div>
                                    </div>
                                </div>
                            </div>

                            <div class="modal-footer border-0 pt-0">
                                <button type="button" class="btn btn-light rounded-pill px-4 fs-7 fw-semibold"
                                    data-bs-dismiss="modal">Hủy</button>
                                <button type="submit"
                                    class="btn btn-success rounded-pill px-4 fs-7 fw-semibold shadow-sm">
                                    <i class="bi bi-send-fill me-1"></i> Gửi lời mời (7 ngày)
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </c:if>

        <!-- MODAL: CÀI ĐẶT DỰ ÁN & QUY TRÌNH (SOLO VS TEAM) -->
        <c:if test="${project.ownerId == sessionScope.currentUser.id}">
            <div class="modal fade" id="projectSettingsModal" tabindex="-1" aria-labelledby="projectSettingsModalLabel" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
                        <div class="modal-header bg-dark-navy text-white px-4 py-3 border-0">
                            <div class="d-flex align-items-center gap-2">
                                <i class="bi bi-gear-fill text-warning fs-5"></i>
                                <div>
                                    <h5 class="modal-title fw-bold mb-0" id="projectSettingsModalLabel">Cài Đặt & Quy Trình Dự Án</h5>
                                    <span class="fs-9 text-white-50">Tùy biến chế độ kiểm soát chất lượng</span>
                                </div>
                            </div>
                            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Đóng"></button>
                        </div>
                        <form method="post" action="${pageContext.request.contextPath}/project">
                            <input type="hidden" name="action" value="update">
                            <input type="hidden" name="projectId" value="${project.id}">
                            <div class="modal-body p-4">
                                <div class="mb-3">
                                    <label class="form-label fw-semibold fs-7 text-dark">Tên dự án <span class="text-danger">*</span></label>
                                    <input type="text" class="form-control rounded-3 fs-7" name="name" value="${project.name}" required>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-semibold fs-7 text-dark">Mô tả dự án</label>
                                    <textarea class="form-control rounded-3 fs-7" name="description" rows="2"><c:out value="${project.description}" /></textarea>
                                </div>
                                
                                <div class="mb-2">
                                    <label class="form-label fw-semibold fs-7 text-dark mb-2">Chế độ vận hành (Workflow Mode):</label>
                                    <div class="row g-2">
                                        <div class="col-6">
                                            <label class="d-block p-3 rounded-3 border h-100 position-relative ${project.teamProject ? 'border-primary bg-primary-subtle' : 'bg-light'}" style="cursor: pointer;">
                                                <input type="radio" name="projectType" value="TEAM" class="position-absolute top-2 end-2" ${project.teamProject ? 'checked' : ''}>
                                                <div class="fw-bold text-dark fs-8 mb-1"><i class="bi bi-shield-check text-primary me-1"></i> Dự Án Nhóm</div>
                                                <div class="fs-9 text-muted lh-sm">Kích hoạt Quality Gate 2 tầng (Gate 1 duyệt WBS + Gate 2 PM nghiệm thu), thanh lọc nhân sự và cột Phụ trách.</div>
                                            </label>
                                        </div>
                                        <c:choose>
                                            <c:when test="${project.teamProject && memberCount > 1}">
                                                <div class="col-6">
                                                    <div class="d-block p-3 rounded-3 border h-100 position-relative bg-light opacity-75 border-secondary-subtle">
                                                        <input type="radio" name="projectType" value="SOLO" class="position-absolute top-2 end-2" disabled>
                                                        <div class="fw-bold text-secondary fs-8 mb-1"><i class="bi bi-lock-fill me-1"></i> Cá Nhân (Bị khóa)</div>
                                                        <div class="fs-9 text-danger lh-sm">
                                                            Đang có <strong>${memberCount} thành viên</strong>. Cần xóa hết thành viên khác trước khi chuyển sang chế độ Cá Nhân.
                                                        </div>
                                                    </div>
                                                </div>
                                            </c:when>
                                            <c:otherwise>
                                                <div class="col-6">
                                                    <label class="d-block p-3 rounded-3 border h-100 position-relative ${project.soloProject ? 'border-warning bg-warning-subtle' : 'bg-light'}" style="cursor: pointer;">
                                                        <input type="radio" name="projectType" value="SOLO" class="position-absolute top-2 end-2" ${project.soloProject ? 'checked' : ''}>
                                                        <div class="fw-bold text-dark fs-8 mb-1"><i class="bi bi-lightning-charge-fill text-warning me-1"></i> Cá Nhân (Fast-track)</div>
                                                        <div class="fs-9 text-muted lh-sm">Giao diện tối giản tuyệt đối, tự do kéo thả, ẩn toàn bộ thông tin phụ trách/đội ngũ.</div>
                                                    </label>
                                                </div>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                    <c:if test="${project.teamProject && memberCount == 1}">
                                        <div class="mt-2 fs-9 text-muted bg-light p-2 rounded-2 border">
                                            <i class="bi bi-info-circle text-primary me-1"></i> Khi chuyển sang <strong>Cá Nhân</strong>, toàn bộ task sẽ tự động mở khóa (bỏ duyệt cổng) và hủy các lời mời đang chờ.
                                        </div>
                                    </c:if>
                                </div>
                            </div>
                            <div class="modal-footer border-0 pt-0 px-4 pb-4">
                                <button type="button" class="btn btn-light rounded-pill px-4 fs-7 fw-semibold" data-bs-dismiss="modal">Hủy</button>
                                <button type="submit" class="btn btn-primary rounded-pill px-4 fs-7 fw-semibold shadow-sm">
                                    <i class="bi bi-check-lg me-1"></i> Lưu Cấu Hình
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </c:if>

        <!-- MODAL: TẠO DỰ ÁN MỚI (SPACES) TRỰC TIẾP TỪ SIDEBAR -->
        <div class="modal fade" id="createProjectModal" tabindex="-1" aria-labelledby="createProjectModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
                    <div class="modal-header bg-dark-navy text-white px-4 py-3 border-0">
                        <div class="d-flex align-items-center gap-2">
                            <i class="bi bi-folder-plus text-primary fs-5"></i>
                            <div>
                                <h5 class="modal-title fw-bold mb-0" id="createProjectModalLabel">Khởi Tạo Không Gian Mới</h5>
                                <span class="fs-9 text-white-50">Tạo dự án làm việc nhóm hoặc cá nhân</span>
                            </div>
                        </div>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Đóng"></button>
                    </div>

                    <!-- FORM SUBMIT POST VỀ PROJECTSERVLET -->
                    <form action="${pageContext.request.contextPath}/project" method="post">
                        <input type="hidden" name="action" value="create">

                        <div class="modal-body p-4">
                            <div class="mb-3">
                                <label for="proj-name" class="form-label fw-semibold fs-7 text-dark">
                                    Tên dự án <span class="text-danger">*</span>
                                </label>
                                <input type="text" class="form-control rounded-3 fs-7" id="proj-name" name="name"
                                    placeholder="Ví dụ: Nâng cấp Website E-Commerce" required autofocus>
                            </div>

                            <div class="mb-3">
                                <label for="proj-code"
                                    class="form-label fw-semibold fs-7 text-dark d-flex align-items-center justify-content-between">
                                    <span>Mã dự án (Tùy chọn)</span>
                                    <span class="text-muted fs-9 fw-normal">Tự động tạo nếu để trống</span>
                                </label>
                                <input type="text" class="form-control text-uppercase rounded-3 fs-7" id="proj-code"
                                    name="projectCode" placeholder="Ví dụ: TW-HUB-01">
                            </div>

                            <div class="mb-3">
                                <label for="proj-desc" class="form-label fw-semibold fs-7 text-dark">
                                    Mô tả mục tiêu dự án
                                </label>
                                <textarea class="form-control rounded-3 fs-7" id="proj-desc" name="description"
                                    rows="2"
                                    placeholder="Mô tả ngắn gọn phạm vi và mục tiêu của dự án..."></textarea>
                            </div>

                            <!-- BỘ CHỌN MÔ HÌNH DỰ ÁN (CLICKUP 3.0 WORKFLOW SELECTOR) -->
                            <div class="mb-2">
                                <label class="form-label fw-semibold fs-7 text-dark mb-2 d-flex align-items-center justify-content-between">
                                    <span>Mô hình làm việc & Quy trình kiểm soát</span>
                                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2 py-0-5 fs-9">Tùy chỉnh linh hoạt</span>
                                </label>
                                <div class="row g-2">
                                    <div class="col-6">
                                        <label class="p-2.5 rounded-3 border border-primary bg-primary-subtle bg-opacity-10 d-block position-relative h-100" id="card-type-team" style="cursor: pointer;">
                                            <input type="radio" name="projectType" value="TEAM" class="form-check-input position-absolute top-0 end-0 m-2" checked onchange="document.getElementById('card-type-team').classList.add('border-primary','bg-primary-subtle','bg-opacity-10'); document.getElementById('card-type-solo').classList.remove('border-primary','bg-primary-subtle','bg-opacity-10');">
                                            <div class="d-flex align-items-center gap-1-5 mb-1 pe-3">
                                                <span class="badge bg-primary text-white rounded-circle p-1 lh-1"><i class="bi bi-people-fill fs-9"></i></span>
                                                <strong class="fs-8 text-dark">Dự Án Nhóm</strong>
                                            </div>
                                            <p class="fs-9 text-secondary mb-0 lh-sm">
                                                Áp dụng <strong>Quality Gate 2 tầng</strong>: duyệt kế hoạch WBS và nghiệm thu bàn giao.
                                            </p>
                                        </label>
                                    </div>
                                    <div class="col-6">
                                        <label class="p-2.5 rounded-3 border d-block position-relative h-100" id="card-type-solo" style="cursor: pointer;">
                                            <input type="radio" name="projectType" value="SOLO" class="form-check-input position-absolute top-0 end-0 m-2" onchange="document.getElementById('card-type-solo').classList.add('border-primary','bg-primary-subtle','bg-opacity-10'); document.getElementById('card-type-team').classList.remove('border-primary','bg-primary-subtle','bg-opacity-10');">
                                            <div class="d-flex align-items-center gap-1-5 mb-1 pe-3">
                                                <span class="badge bg-info text-white rounded-circle p-1 lh-1"><i class="bi bi-person-fill fs-9"></i></span>
                                                <strong class="fs-8 text-dark">Cá Nhân (Fast-track)</strong>
                                            </div>
                                            <p class="fs-9 text-secondary mb-0 lh-sm">
                                                Tối giản như <strong>ClickUp Solo</strong>: tự do kéo thả, việc con là checklist 1 chạm.
                                            </p>
                                        </label>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="modal-footer border-0 pt-0 px-4 pb-4">
                            <button type="button" class="btn btn-light rounded-pill px-4 fs-7 fw-semibold"
                                data-bs-dismiss="modal">Hủy</button>
                            <button type="submit"
                                class="btn btn-primary-custom rounded-pill px-4 fs-7 fw-semibold shadow-sm">
                                <i class="bi bi-check-circle-fill me-1"></i> Khởi tạo dự án
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>

        <!-- MODAL: GIA NHẬP DỰ ÁN BẰNG MÃ (JOIN BY CODE) -->
        <div class="modal fade" id="joinByCodeModal" tabindex="-1" aria-labelledby="joinByCodeModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
                    <div class="modal-header border-0 bg-light px-4 pt-4 pb-2">
                        <div class="d-flex align-items-center gap-2">
                            <span class="badge bg-warning text-dark rounded-circle p-2">
                                <i class="bi bi-key-fill fs-6"></i>
                            </span>
                            <div>
                                <h5 class="modal-title fw-bold text-dark fs-6" id="joinByCodeModalLabel">Gia Nhập Dự Án Bằng Mã</h5>
                                <p class="text-muted fs-8 mb-0">Nhập mã định danh dự án (Project Code) do PM cung cấp</p>
                            </div>
                        </div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <form action="${pageContext.request.contextPath}/invite" method="POST">
                        <input type="hidden" name="action" value="requestJoin">
                        <div class="modal-body px-4 py-3">
                            <div class="mb-3">
                                <label for="inputProjectCode" class="form-label fw-semibold fs-7 text-dark">Mã dự án (Project Code) <span class="text-danger">*</span></label>
                                <div class="input-group">
                                    <span class="input-group-text bg-light"><i class="bi bi-hash"></i></span>
                                    <input type="text" class="form-control text-uppercase rounded-end-3 fs-7 fw-bold" id="inputProjectCode" name="projectCode" placeholder="Ví dụ: WEB-2026, PRJ-01" required autocomplete="off">
                                </div>
                                <div class="form-text fs-9 text-muted mt-1">Hệ thống sẽ gửi yêu cầu xin tham gia đến Trưởng dự án để phê duyệt.</div>
                            </div>
                        </div>
                        <div class="modal-footer border-0 pt-0 px-4 pb-4">
                            <button type="button" class="btn btn-light rounded-pill px-4 fs-7 fw-semibold" data-bs-dismiss="modal">Hủy</button>
                            <button type="submit" class="btn btn-primary-custom rounded-pill px-4 fs-7 fw-semibold shadow-sm">
                                <i class="bi bi-send-fill me-1"></i> Gửi yêu cầu gia nhập
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>

        <!-- MODAL: PHÍM TẮT HỆ THỐNG (KEYBOARD SHORTCUTS HELP) -->
        <div class="modal fade" id="shortcutsHelpModal" tabindex="-1" aria-labelledby="shortcutsHelpModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-lg">
                <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
                    <div class="modal-header border-0 bg-light px-4 pt-4 pb-2">
                        <div class="d-flex align-items-center gap-2">
                            <span class="badge bg-primary text-white rounded-circle p-2">
                                <i class="bi bi-keyboard-fill fs-6"></i>
                            </span>
                            <div>
                                <h5 class="modal-title fw-bold text-dark fs-6" id="shortcutsHelpModalLabel">Phím Tắt Hệ Thống (Keyboard Shortcuts)</h5>
                                <p class="text-muted fs-8 mb-0">Tăng tốc độ thao tác như một Power-User theo chuẩn ClickUp & Linear</p>
                            </div>
                        </div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body px-4 py-3">
                        <div class="row g-3">
                            <div class="col-md-6">
                                <div class="p-3 bg-light rounded-3 h-100">
                                    <h6 class="fw-bold text-primary fs-8 text-uppercase mb-3" style="letter-spacing: 0.05em;"><i class="bi bi-search me-1"></i> Điều hướng & Tìm kiếm</h6>
                                    <div class="d-flex align-items-center justify-content-between py-2 border-bottom">
                                        <span class="fs-8 text-dark">Tìm kiếm công việc nhanh</span>
                                        <kbd class="bg-white text-dark border shadow-2xs px-2 py-1 rounded fs-8 fw-semibold">Ctrl + K</kbd>
                                    </div>
                                    <div class="d-flex align-items-center justify-content-between py-2 border-bottom">
                                        <span class="fs-8 text-dark">Mở bảng trợ giúp phím tắt</span>
                                        <kbd class="bg-white text-dark border shadow-2xs px-2 py-1 rounded fs-8 fw-semibold">?</kbd>
                                    </div>
                                    <div class="d-flex align-items-center justify-content-between py-2">
                                        <span class="fs-8 text-dark">Đóng Modal / Side-Peek Drawer</span>
                                        <kbd class="bg-white text-dark border shadow-2xs px-2 py-1 rounded fs-8 fw-semibold">Esc</kbd>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="p-3 bg-light rounded-3 h-100">
                                    <h6 class="fw-bold text-primary fs-8 text-uppercase mb-3" style="letter-spacing: 0.05em;"><i class="bi bi-layers me-1"></i> Chế độ xem & Thao tác</h6>
                                    <div class="d-flex align-items-center justify-content-between py-2 border-bottom">
                                        <span class="fs-8 text-dark">Chuyển sang Chế độ Danh sách (List)</span>
                                        <kbd class="bg-white text-dark border shadow-2xs px-2 py-1 rounded fs-8 fw-semibold">L</kbd>
                                    </div>
                                    <div class="d-flex align-items-center justify-content-between py-2 border-bottom">
                                        <span class="fs-8 text-dark">Chuyển sang Bảng Kanban (Board)</span>
                                        <kbd class="bg-white text-dark border shadow-2xs px-2 py-1 rounded fs-8 fw-semibold">B</kbd>
                                    </div>
                                    <div class="d-flex align-items-center justify-content-between py-2">
                                        <span class="fs-8 text-dark">Chuyển nhanh Mở/Ẩn việc con (Subtasks)</span>
                                        <kbd class="bg-white text-dark border shadow-2xs px-2 py-1 rounded fs-8 fw-semibold">S</kbd>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer border-0 pt-0 px-4 pb-4">
                        <button type="button" class="btn btn-primary-custom rounded-pill px-4 fs-7 fw-semibold" data-bs-dismiss="modal">Đã hiểu</button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 7. NẠP FILE JAVASCRIPT KÉO THẢ & LỌC TỨC THÌ (0.01 GIÂY) -->
        <script src="${pageContext.request.contextPath}/js/tasks.js"></script>

        <!-- Bootstrap 5.3.3 JS Bundle CDN -->
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz" crossorigin="anonymous"></script>
        <!-- UI-04: Global App JS (Floating Toast System + Utilities) -->
        <script src="${pageContext.request.contextPath}/js/app.js"></script>
    </body>
</html>
