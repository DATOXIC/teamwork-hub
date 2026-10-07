<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<%-- =========================================================================
     [MVC VIEW SKELETON: WORKSPACE DASHBOARD & PROJECT MANAGEMENT]
     VI: Bảng điều khiển không gian làm việc, danh sách dự án và hộp thư lời mời
     EN: Workspace dashboard, project portfolio, and pending invitations management
     - Controller: com.teamwork.controllers.ProjectServlet (/project?action=list)
     - Partner Controller: com.teamwork.controllers.ProjectInviteServlet (/invite)
     - Model: com.teamwork.business.Project, com.teamwork.business.ProjectMember,
              com.teamwork.business.ProjectInvite, com.teamwork.business.User
     - Session Attributes: currentUser, toastSuccess, toastError
     - Request Attributes: myProjects, pendingInvites, kpiPmCount, kpiTotalTasks,
                           kpiDoneTasks, memberCountMap, pendingInvites
     - Form Actions: POST /invite (action=accept | reject | requestJoin),
                     POST /project (action=create)
     ========================================================================= --%>

<%-- 1. Đặt tiêu đề cho tab trình duyệt và nhúng Header, Navbar --%>
<c:set var="pageTitle" value="Không gian làm việc &bull; TeamWork Hub" />
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container py-4 my-auto">

    <!-- 2. Header Section: Tiêu đề trang & Nút Tạo dự án mới -->
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4 pb-3 workspace-header-bar">
        <div>
            <h3 class="fw-bold mb-1 d-flex align-items-center gap-2 workspace-title">
                <i class="bi bi-grid-1x2-fill workspace-title-icon"></i>
                <span>Không Gian Làm Việc</span>
            </h3>
            <p class="fs-8 mb-0 workspace-subtitle">
                Chào mừng trở lại, <strong class="workspace-user-highlight">${fn:escapeXml(sessionScope.currentUser.fullName)}</strong>! Bạn đang tham gia
                <strong class="workspace-count-highlight">${myProjects.size()} dự án</strong>.
            </p>
        </div>
        <div class="d-flex align-items-center gap-2">
            <!-- Nút mở Modal Xin Gia Nhập Bằng Mã -->
            <button type="button"
                class="btn btn-join-code-trigger px-3 py-1-5 rounded-pill fw-semibold shadow-2xs fs-8 d-flex align-items-center gap-2"
                data-bs-toggle="modal" data-bs-target="#joinByCodeModal">
                <i class="bi bi-key-fill text-accent-soft"></i> Nhập Mã Xin Vào
            </button>

            <!-- Nút kích hoạt Modal Tạo Dự Án Mới -->
            <button type="button"
                class="btn btn-primary-custom px-3-5 py-1-5 rounded-pill fw-semibold shadow-2xs fs-8 d-flex align-items-center gap-2 text-white"
                data-bs-toggle="modal" data-bs-target="#createProjectModal">
                <i class="bi bi-plus-circle-fill"></i> Tạo dự án mới
            </button>
        </div>
    </div>

                <!-- 3. Thông báo Flash — UI-04: Floating Toast (tự biến mất sau 4 giây) -->
                <jsp:include page="/includes/toast.jsp" />

                <!-- =========================================================================
                     4. BENTO KPI BAR: CHỈ SỐ HOẠT ĐỘNG TỔNG QUAN (QUICK METRICS)
                     VI: Thống kê nhanh các chỉ số dự án, tiến độ chung và hộp thư
                     EN: Quick summary metrics for owned/joined projects, task completion and invites
                     ========================================================================= -->
                <%-- Ba chỉ số kpiPmCount / kpiTotalTasks / kpiDoneTasks được ProjectServlet
                     tính sẵn và đặt vào request scope (xem showProjectList, mục 2) --%>
                <div class="row g-3 mb-4">
                    <!-- KPI 1: Tổng dự án tham gia -->
                    <div class="col-6 col-md-3">
                        <div class="workspace-kpi-card">
                            <div class="kpi-icon-box kpi-icon-primary">
                                <i class="bi bi-kanban-fill"></i>
                            </div>
                            <div>
                                <div class="fs-9 text-muted fw-semibold text-uppercase tracking-wider">Dự án của bạn</div>
                                <%-- ◀ SERVLET: ProjectServlet → setAttribute("myProjects") --%>
                                <h4 class="fw-bold mb-0 workspace-kpi-value">${myProjects.size()}</h4>
                                <%-- ◀ SERVLET: ProjectServlet → setAttribute("kpiPmCount") --%>
                                <span class="fs-9 workspace-kpi-desc">(${kpiPmCount} làm Trưởng nhóm)</span>
                            </div>
                        </div>
                    </div>

                    <!-- KPI 2: Khám phá dự án -->
                    <div class="col-6 col-md-3">
                        <div class="workspace-kpi-card">
                            <div class="kpi-icon-box kpi-icon-accent">
                                <i class="bi bi-envelope-paper"></i>
                            </div>
                            <div>
                                <div class="fs-9 text-muted fw-semibold text-uppercase tracking-wider">Lời mời</div>
                                <%-- ◀ SERVLET: ProjectServlet → setAttribute("pendingInvites") --%>
                                <h4 class="fw-bold mb-0 workspace-kpi-value">${empty pendingInvites ? 0 : pendingInvites.size()}</h4>
                                <span class="fs-9 workspace-kpi-desc">Đang chờ bạn phản hồi</span>
                            </div>
                        </div>
                    </div>

                    <!-- KPI 3: Tổng đầu việc (Tasks) -->
                    <div class="col-6 col-md-3">
                        <div class="workspace-kpi-card">
                            <div class="kpi-icon-box kpi-icon-success">
                                <i class="bi bi-check2-circle"></i>
                            </div>
                            <div>
                                <div class="fs-9 text-muted fw-semibold text-uppercase tracking-wider">Tiến độ chung</div>
                                <%-- ◀ SERVLET: ProjectServlet → setAttribute("kpiDoneTasks") --%>
                                <%-- ◀ SERVLET: ProjectServlet → setAttribute("kpiTotalTasks") --%>
                                <h4 class="fw-bold mb-0 workspace-kpi-value">${kpiDoneTasks}/${kpiTotalTasks}</h4>
                                <span class="fs-9 text-success fw-medium">Đã giải quyết</span>
                            </div>
                        </div>
                    </div>

                    <!-- KPI 4: Hộp thư chờ -->
                    <div class="col-6 col-md-3">
                        <div class="workspace-kpi-card">
                            <div class="kpi-icon-box kpi-icon-warning">
                                <i class="bi bi-envelope-paper-heart-fill"></i>
                            </div>
                            <div>
                                <div class="fs-9 text-muted fw-semibold text-uppercase tracking-wider">Hộp thư lời mời</div>
                                <%-- ◀ SERVLET: ProjectServlet → setAttribute("pendingInvites") --%>
                                <h4 class="fw-bold mb-0 workspace-kpi-value">${not empty pendingInvites ? pendingInvites.size() : 0}</h4>
                                <span class="fs-9 workspace-kpi-warning">Chờ bạn xử lý</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- =========================================================================
                     5. HỘP THƯ LỜI MỜI / YÊU CẦU XIN GIA NHẬP ĐANG CHỜ PHẢN HỒI (PENDING INVITES)
                     VI: Xử lý lời mời tham gia dự án hoặc đơn xin gia nhập từ thành viên
                     EN: Inbox for pending project invitations and membership join requests
                     ========================================================================= -->
                <c:if test="${not empty pendingInvites}">
                    <div class="mb-4">
                        <div class="d-flex align-items-center justify-content-between mb-3">
                            <div class="d-flex align-items-center gap-2">
                                <span class="p-1 rounded-2 lh-1 invite-alert-pill">
                                    <i class="bi bi-envelope-paper-heart-fill fs-7"></i>
                                </span>
                                <h6 class="fw-bold fs-7 mb-0 invite-section-title">Hộp Thư Yêu Cầu & Lời Mời (${pendingInvites.size()})</h6>
                            </div>
                            <span class="badge rounded-pill px-2-5 py-1 fs-9 fw-semibold invite-alert-pill">
                                Đang chờ bạn phản hồi
                            </span>
                        </div>

                        <div class="row g-3">
                            <c:forEach items="${pendingInvites}" var="inv">
                                <div class="col-12 col-lg-6">
                                    <div class="pending-invite-card h-100 d-flex flex-column justify-content-between">
                                        <div>
                                            <div class="d-flex align-items-center justify-content-between mb-2">
                                                <span role="button" tabindex="0" class="project-code-badge"
                                                    data-code="${fn:escapeXml(inv.projectCode)}" onclick="copyProjectCode(this.dataset.code)"
                                                    title="Bấm để sao chép mã">
                                                    <i class="bi bi-hash"></i><span>${fn:escapeXml(inv.projectCode)}</span>
                                                    <i class="bi bi-copy fs-9 ms-1 copy-code-icon"></i>
                                                </span>
                                                <span
                                                    class="badge ${inv.statusBadgeClass} rounded-pill px-2 py-0-5 fs-9">
                                                    ${inv.statusLabel}
                                                </span>
                                            </div>
                                            <h6 class="fw-bold mb-1 fs-7 invite-project-title">${fn:escapeXml(inv.projectName)}</h6>
                                            <p class="text-secondary fs-8 mb-2">
                                                <c:choose>
                                                    <c:when test="${inv.type == 'INVITATION'}">
                                                        <i class="bi bi-person-fill me-1 invite-sender-icon"></i> Trưởng nhóm
                                                        <strong>${fn:escapeXml(inv.senderName)}</strong> đã gửi lời mời bạn vào dự án này.
                                                    </c:when>
                                                    <c:otherwise>
                                                        <i class="bi bi-person-plus-fill text-warning me-1"></i> Thành
                                                        viên <strong>${fn:escapeXml(inv.senderName)}</strong> gửi đơn xin gia nhập dự án của bạn.
                                                    </c:otherwise>
                                                </c:choose>
                                            </p>
                                            <div class="fs-9 text-muted mb-2">
                                                <i class="bi bi-clock-history me-1"></i> Hạn phản hồi: <strong
                                                    class="text-danger">${inv.expiredAt}</strong> (Còn hiệu lực)
                                            </div>
                                        </div>

                                        <!-- Nút bấm Duyệt / Từ chối -->
                                        <div class="d-flex align-items-center gap-2 pt-2 border-top invite-card-actions">
                                            <%-- ▶ SERVLET: /invite → ProjectInviteServlet.doPost() → case "accept" → handleAccept() --%>
                                            <form method="post" action="${pageContext.request.contextPath}/invite"
                                                class="m-0 flex-grow-1">
                                                <input type="hidden" name="action" value="accept">
                                                <input type="hidden" name="inviteId" value="${inv.id}">
                                                <button type="submit"
                                                    class="btn btn-invite-accept btn-sm w-100 rounded-pill fw-semibold fs-8 py-1-5 shadow-2xs text-white">
                                                    <i class="bi bi-check-circle-fill me-1"></i> Đồng ý gia nhập
                                                </button>
                                            </form>
                                            <%-- ▶ SERVLET: /invite → ProjectInviteServlet.doPost() → case "reject" → handleReject() --%>
                                            <form method="post" action="${pageContext.request.contextPath}/invite"
                                                class="m-0 flex-grow-1"
                                                onsubmit="return confirm('Bạn có chắc chắn muốn từ chối yêu cầu này?');">
                                                <input type="hidden" name="action" value="reject">
                                                <input type="hidden" name="inviteId" value="${inv.id}">
                                                <button type="submit"
                                                    class="btn btn-invite-reject btn-outline-secondary btn-sm w-100 rounded-pill fw-semibold fs-8 py-1-5">
                                                    <i class="bi bi-x-circle me-1"></i> Từ chối
                                                </button>
                                            </form>
                                        </div>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                </c:if>

                <!-- =========================================================================
                     6. WORKSPACE TOOLBAR: TÌM KIẾM TỨC THÌ (LIVE SEARCH) & FILTER TABS
                     VI: Bộ lọc theo vai trò (Trưởng nhóm / Thành viên) và ô tìm kiếm tức thì
                     EN: Role-based filtering (Owner / Member) and real-time live search toolbar
                     ========================================================================= -->
                <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4 p-3 rounded-4 workspace-toolbar-card">
                    <!-- Filter Pills -->
                    <div class="d-flex align-items-center gap-2 flex-wrap">
                        <button type="button" class="filter-pill-btn active" onclick="filterProjects('all', this)">
                            <i class="bi bi-grid-fill me-1"></i> Tất cả (${myProjects.size()})
                        </button>
                        <button type="button" class="filter-pill-btn" onclick="filterProjects('owner', this)">
                            <i class="bi bi-star-fill text-warning me-1"></i> Tôi làm Trưởng nhóm (${kpiPmCount})
                        </button>
                        <button type="button" class="filter-pill-btn" onclick="filterProjects('member', this)">
                            <i class="bi bi-person-fill me-1"></i> Tôi là Thành viên (${myProjects.size() - kpiPmCount})
                        </button>
                    </div>

                    <!-- Ô Live Search Box -->
                    <div class="search-input-group workspace-search-group">
                        <i class="bi bi-search text-muted me-2"></i>
                        <input aria-label="Tìm tên hoặc mã dự án" type="text" id="projectSearchInput" placeholder="Tìm tên hoặc mã dự án..." onkeyup="searchProjectsLive(this.value)">
                        <button type="button" aria-label="Xóa ô tìm kiếm" class="btn btn-link p-0 text-muted d-none" id="clearSearchBtn" onclick="clearProjectSearch()">
                            <i class="bi bi-x-circle-fill"></i>
                        </button>
                    </div>
                </div>

                <!-- =========================================================================
                     7. PROJECT GRID: LƯỚI HIỂN THỊ DANH SÁCH CARD DỰ ÁN
                     VI: Lưới danh sách các dự án người dùng đang tham gia
                     EN: Project cards grid showcasing active workspaces user belongs to
                     ========================================================================= -->
                <!-- LƯỚI 1: DỰ ÁN CỦA TÔI -->
                <div class="d-flex align-items-center justify-content-between mb-3 mt-2">
                    <h6 class="fw-bold fs-7 mb-0 d-flex align-items-center gap-2 workspace-section-heading">
                        <span class="d-inline-flex align-items-center justify-content-center rounded-2 p-1 workspace-section-badge">
                            <i class="bi bi-folder-check"></i>
                        </span>
                        <span>Dự án của tôi (<span id="myProjectsCountBadge">${myProjects.size()}</span>)</span>
                    </h6>
                    <span class="badge rounded-pill px-2-5 py-1 fs-9 fw-semibold workspace-status-badge">
                        <i class="bi bi-activity me-1"></i>Không gian đang hoạt động
                    </span>
                </div>

                <div class="row g-3 mb-5" id="myProjectsGrid">
                    <c:forEach items="${myProjects}" var="p">
                        <div class="col-12 col-md-6 col-lg-4 project-item" data-name="${fn:escapeXml(p.name.toLowerCase())}" data-code="${fn:escapeXml(p.projectCode.toLowerCase())}" data-role="${p.ownerId == sessionScope.currentUser.id ? 'owner' : 'member'}">
                            <div class="project-card">
                                <div class="project-card-body">
                                    <!-- Header thẻ: Monogram Avatar + Mã dự án + Badge vai trò (Gọn gàng, không chật chội) -->
                                    <div class="d-flex justify-content-between align-items-center mb-3">
                                        <div class="d-flex align-items-center gap-2">
                                            <div class="project-avatar-monogram">
                                                ${fn:escapeXml(p.name.substring(0, 1).toUpperCase())}
                                            </div>
                                            <span role="button" tabindex="0" class="project-code-badge" data-code="${fn:escapeXml(p.projectCode)}" onclick="copyProjectCode(this.dataset.code)"
                                                title="Bấm để sao chép mã dự án">
                                                <i class="bi bi-hash"></i><span>${fn:escapeXml(p.projectCode)}</span>
                                                <i class="bi bi-copy fs-9 text-primary" id="copy-icon-${fn:escapeXml(p.projectCode)}"></i>
                                            </span>
                                        </div>
                                        <div class="flex-shrink-0">
                                            <!-- UI-05: Badge vai trò PM / Thành viên -->
                                            <c:choose>
                                                <c:when test="${p.ownerId == sessionScope.currentUser.id}">
                                                    <span class="badge badge-role-owner rounded-pill px-2-5 py-1 fs-9 fw-semibold text-white d-inline-flex align-items-center"
                                                        title="Bạn là Trưởng Dự Án">
                                                        <i class="bi bi-star-fill text-warning me-1"></i> Trưởng nhóm
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge badge-role-member rounded-pill px-2-5 py-1 fs-9 fw-semibold d-inline-flex align-items-center"
                                                        title="Bạn là thành viên">
                                                        <i class="bi bi-person-fill me-1"></i> Thành viên
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>

                                    <!-- Tên dự án -->
                                    <h5 class="fw-bold text-dark mb-1 fs-6 lh-sm text-truncate" title="${fn:escapeXml(p.name)}">${fn:escapeXml(p.name)}</h5>

                                    <!-- Mô tả dự án -->
                                    <p class="text-secondary fs-8 mb-3 project-desc-line-clamp">
                                        ${not empty p.description ? fn:escapeXml(p.description) : 'Chưa có mô tả cho dự án này.'}
                                    </p>

                                    <!-- Thông tin phụ: Thành viên & Loại hình dự án (được chuyển xuống đây để thoáng header) -->
                                    <div class="d-flex align-items-center justify-content-between text-muted fs-8 pt-1">
                                        <span class="d-flex align-items-center gap-1">
                                            <i class="bi bi-people-fill text-accent-soft"></i>
                                            <%-- ◀ SERVLET: ProjectServlet → setAttribute("memberCountMap") --%>
                                            <strong>${memberCountMap[p.id]}/10</strong> thành viên
                                        </span>
                                        <span class="badge ${p.projectTypeBadgeClass} rounded-pill px-2-5 py-1 fs-9 fw-medium"
                                            title="Mô hình quản lý: ${p.projectTypeLabel}">
                                            <i class="bi ${p.projectTypeIcon} me-1"></i>${p.projectTypeLabel}
                                        </span>
                                    </div>
                                </div>

                                <!-- Footer thẻ: Tiến độ hoàn thành + Nút Vào dự án -->
                                <div class="project-card-footer">
                                    <div class="d-flex justify-content-between align-items-center fs-8 text-muted mb-1-5">
                                        <span>Tiến độ: <strong class="text-dark">${p.doneTasks}/${p.totalTasks} việc</strong></span>
                                        <span class="fw-bold project-progress-pct">${p.progressPercentage}%</span>
                                    </div>
                                    <div class="project-progress-container mb-3">
                                        <div class="project-progress-bar progress-init-zero" data-progress="${p.progressPercentage}%"></div>
                                    </div>
                                    <div class="d-flex gap-2">
                                        <a href="${pageContext.request.contextPath}/task?action=list&projectId=${p.id}"
                                            class="btn btn-primary-custom flex-grow-1 rounded-pill py-1-5 fs-8 fw-semibold d-flex align-items-center justify-content-center gap-2 shadow-2xs text-white">
                                            <span>Vào không gian dự án</span>
                                            <i class="bi bi-arrow-right"></i>
                                        </a>
                                        <a href="${pageContext.request.contextPath}/project?action=report&projectId=${p.id}"
                                            class="btn btn-outline-secondary btn-project-report-icon rounded-pill px-3 py-1-5 fs-8 fw-semibold d-flex align-items-center justify-content-center shadow-2xs"
                                            title="Xem báo cáo tiến độ dự án">
                                            <i class="bi bi-file-earmark-bar-graph"></i>
                                        </a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                    <!-- Thông báo không tìm thấy kết quả khi lọc hoặc tìm kiếm -->
                    <div class="col-12 d-none text-center py-5" id="noSearchResultsAlert">
                        <div class="p-4 rounded-4 bg-white border d-inline-block search-no-results-card">
                            <i class="bi bi-search fs-2 mb-2 d-block search-empty-icon"></i>
                            <h6 class="fw-bold mb-1 search-empty-title">Không tìm thấy dự án phù hợp</h6>
                            <p class="text-muted fs-8 mb-3">Không có dự án nào khớp với bộ lọc hoặc từ khóa tìm kiếm của bạn.</p>
                            <button type="button" class="btn btn-reset-filters btn-sm rounded-pill px-3 fs-8 fw-semibold" onclick="clearProjectSearch(); filterProjects('all', document.querySelector('.filter-pill-btn'));">
                                <i class="bi bi-arrow-counterclockwise me-1"></i> Đặt lại bộ lọc
                            </button>
                        </div>
                    </div>
                    <c:if test="${empty myProjects}">
                        <div class="col-12">
                            <div class="empty-state bg-white rounded-4 border">
                                <i class="bi bi-grid empty-state-icon"></i>
                                <p class="empty-state-title">Chưa tham gia dự án nào</p>
                                <p class="empty-state-hint">Tạo dự án mới hoặc nhập mã để gia nhập nhóm của bạn bè!</p>
                                <div class="d-flex justify-content-center gap-2 mt-3">
                                    <button type="button" class="btn btn-outline-primary btn-sm rounded-pill px-3 fs-8"
                                        data-bs-toggle="modal" data-bs-target="#joinByCodeModal">
                                        <i class="bi bi-key-fill me-1"></i> Nhập Mã
                                    </button>
                                    <button type="button"
                                        class="btn btn-primary-custom btn-sm rounded-pill px-3 fs-8 text-white"
                                        data-bs-toggle="modal" data-bs-target="#createProjectModal">
                                        <i class="bi bi-plus-circle-fill me-1"></i> Tạo dự án
                                    </button>
                                </div>
                            </div>
                        </div>
                    </c:if>
                </div>

                <!-- =========================================================================
                     8. THAM GIA DỰ ÁN KHÁC BẰNG MÃ
                     Không liệt kê dự án của người khác (tránh lộ tên/mô tả dự án): muốn tham gia thì
                     nhập Mã dự án do trưởng dự án chia sẻ, hoặc chờ được mời.
                     ========================================================================= -->
                <div class="project-card-other d-flex flex-column flex-md-row align-items-md-center justify-content-between gap-3">
                    <div class="d-flex align-items-center gap-3">
                        <span class="d-inline-flex align-items-center justify-content-center rounded-2 p-2 workspace-section-badge-secondary">
                            <i class="bi bi-box-arrow-in-right"></i>
                        </span>
                        <div>
                            <h6 class="fw-bold fs-7 mb-1">Tham gia dự án của nhóm khác</h6>
                            <p class="text-secondary fs-8 mb-0">Nhập mã dự án do trưởng dự án chia sẻ để gửi yêu cầu gia nhập, hoặc chờ lời mời.</p>
                        </div>
                    </div>
                    <button type="button" class="btn btn-explore-join-request rounded-pill px-4 py-1-5 fs-8 fw-semibold shadow-2xs text-nowrap"
                            data-bs-toggle="modal" data-bs-target="#joinByCodeModal">
                        <i class="bi bi-hash me-1"></i> Nhập mã dự án
                    </button>
                </div>
            </div>

            <!-- =========================================================================
                 9. MODAL 1: CỬA SỔ POP-UP XIN GIA NHẬP BẰNG MÃ DỰ ÁN (PROJECT CODE)
                 VI: Gửi yêu cầu gia nhập dự án thông qua mã code (ProjectInviteServlet)
                 EN: Membership join request dialog via project code (ProjectInviteServlet)
                 ========================================================================= -->
            <div class="modal fade" id="joinByCodeModal" tabindex="-1" aria-labelledby="joinByCodeModalLabel"
                aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                        <div class="modal-header border-0 pb-0">
                            <h5 class="modal-title fw-bold text-dark" id="joinByCodeModalLabel">
                                <i class="bi bi-key-fill text-primary me-2"></i>Xin gia nhập dự án bằng Mã
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                        </div>

                        <%-- ▶ SERVLET: /invite → ProjectInviteServlet.doPost() → case "requestJoin" → handleRequestJoin() --%>
                        <form action="${pageContext.request.contextPath}/invite" method="post">
                            <input type="hidden" name="action" value="requestJoin">

                            <div class="modal-body py-3">
                                <p class="text-muted fs-8 mb-3">
                                    Nhập <strong>Mã Dự Án</strong> do Trưởng nhóm cung cấp (ví dụ:
                                    <code>TW-HUB-01</code>) để gửi yêu cầu xin gia nhập.
                                </p>

                                <div class="mb-3">
                                    <label for="inputProjectCode" class="form-label fw-semibold fs-7 text-dark">
                                        Mã dự án <span class="text-danger">*</span>
                                    </label>
                                    <div class="input-group">
                                        <span class="input-group-text bg-light border-end-0 fs-7 text-muted">
                                            <i class="bi bi-hash"></i>
                                        </span>
                                        <input type="text"
                                            class="form-control text-uppercase fw-bold fs-7 rounded-end-3"
                                            id="inputProjectCode" name="projectCode" placeholder="Ví dụ: TW-HUB-01"
                                            required autofocus>
                                    </div>
                                </div>
                            </div>

                            <div class="modal-footer border-0 pt-0">
                                <button type="button" class="btn btn-light rounded-pill px-4 fs-7 fw-semibold"
                                    data-bs-dismiss="modal">Hủy</button>
                                <button type="submit"
                                    class="btn btn-primary-custom rounded-pill px-4 fs-7 fw-semibold shadow-sm">
                                    <i class="bi bi-send-fill me-1"></i> Gửi yêu cầu xin vào
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>

            <!-- =========================================================================
                 10. MODAL 2: CỬA SỔ POP-UP TẠO DỰ ÁN MỚI
                 VI: Khởi tạo dự án mới với mô hình TEAM hoặc SOLO (ProjectServlet)
                 EN: Dialog for creating new workspace project with TEAM or SOLO workflow (ProjectServlet)
                 ========================================================================= -->
            <div class="modal fade" id="createProjectModal" tabindex="-1" aria-labelledby="createProjectModalLabel"
                aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                        <div class="modal-header border-0 pb-0">
                            <h5 class="modal-title fw-bold text-dark" id="createProjectModalLabel">
                                <i class="bi bi-plus-circle text-primary me-2"></i>Tạo dự án mới
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                        </div>

                        <!-- FORM SUBMIT POST VỀ PROJECTSERVLET -->
                        <%-- ▶ SERVLET: /project → ProjectServlet.doPost() → case "create" → createProject() --%>
                        <form action="${pageContext.request.contextPath}/project" method="post">
                            <input type="hidden" name="action" value="create">

                            <div class="modal-body py-3">
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
                                <div class="mb-3">
                                    <label class="form-label fw-semibold fs-7 text-dark mb-2 d-flex align-items-center justify-content-between">
                                        <span>Mô hình làm việc & Quy trình kiểm soát</span>
                                        <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2 py-0-5 fs-9">Tùy chỉnh linh hoạt</span>
                                    </label>
                                    <div class="row g-2">
                                        <div class="col-6">
                                            <label class="p-2.5 rounded-3 border border-primary bg-primary-subtle bg-opacity-10 d-block position-relative h-100 project-type-option-card" id="card-type-team">
                                                <input type="radio" name="projectType" value="TEAM" class="form-check-input position-absolute top-0 end-0 m-2" checked onchange="document.getElementById('card-type-team').classList.add('border-primary','bg-primary-subtle','bg-opacity-10'); document.getElementById('card-type-solo').classList.remove('border-primary','bg-primary-subtle','bg-opacity-10');">
                                                <div class="d-flex align-items-center gap-1-5 mb-1 pe-3">
                                                    <span class="badge bg-primary text-white rounded-circle p-1 lh-1"><i class="bi bi-people-fill fs-9"></i></span>
                                                    <strong class="fs-8 text-dark">Dự Án Nhóm</strong>
                                                </div>
                                                <p class="fs-9 text-secondary mb-0 lh-sm">
                                                    Áp dụng <strong>kiểm duyệt 2 bước</strong>: duyệt kế hoạch và nghiệm thu có chấm sao.
                                                </p>
                                            </label>
                                        </div>
                                        <div class="col-6">
                                            <label class="p-2.5 rounded-3 border d-block position-relative h-100 project-type-option-card" id="card-type-solo">
                                                <input type="radio" name="projectType" value="SOLO" class="form-check-input position-absolute top-0 end-0 m-2" onchange="document.getElementById('card-type-solo').classList.add('border-primary','bg-primary-subtle','bg-opacity-10'); document.getElementById('card-type-team').classList.remove('border-primary','bg-primary-subtle','bg-opacity-10');">
                                                <div class="d-flex align-items-center gap-1-5 mb-1 pe-3">
                                                    <span class="badge bg-info text-white rounded-circle p-1 lh-1"><i class="bi bi-person-fill fs-9"></i></span>
                                                    <strong class="fs-8 text-dark">Cá Nhân (Không cần duyệt)</strong>
                                                </div>
                                                <p class="fs-9 text-secondary mb-0 lh-sm">
                                                    Linh hoạt: tự do đổi trạng thái, nhiệm vụ là danh sách kiểm tra, không cần qua duyệt.
                                                </p>
                                            </label>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="modal-footer border-0 pt-0">
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

            <!-- Toast thông báo Sao chép mã thành công -->
            <div class="position-fixed bottom-0 end-0 p-3 toast-container-bottom-end">
                <div id="copyToast" class="toast align-items-center text-bg-dark border-0 rounded-3 shadow-lg"
                    role="alert" aria-live="assertive" aria-atomic="true">
                    <div class="d-flex">
                        <div class="toast-body fs-8 py-2 d-flex align-items-center gap-2">
                            <i class="bi bi-check2-circle text-success fs-6"></i>
                            <span>Đã sao chép mã dự án: <strong id="copiedCodeText" class="text-info"></strong></span>
                        </div>
                        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"
                            aria-label="Đóng"></button>
                    </div>
                </div>
            </div>

            <script src="${pageContext.request.contextPath}/js/projects.js"></script>

            <jsp:include page="/includes/footer.jsp" />