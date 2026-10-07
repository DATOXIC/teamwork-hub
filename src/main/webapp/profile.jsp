<%-- =========================================================================
     [MVC VIEW SKELETON: USER PROFILE & PRODUCTIVITY SHOWCASE]
     VI: Xem hồ sơ cá nhân, chỉ số cống hiến, dự án tham gia và quản lý tài khoản
     EN: Professional profile view, productivity metrics, workspaces & invitations
     - Controllers: 
         * com.teamwork.controllers.ProfileServlet (/profile)
         * com.teamwork.controllers.ProjectInviteServlet (/invite)
     - Models: com.teamwork.business.User, com.teamwork.business.Project
     - Session Attributes: currentUser (Thực thể người dùng đang đăng nhập)
     - Request Attributes:
         * profileUser (User): Hồ sơ người dùng đang được hiển thị
         * isOwner (Boolean): true nếu currentUser.id == profileUser.id (chính chủ)
         * userProjects (List<Project>): Danh sách các dự án thành viên tham gia
         * availableProjectsToInvite (List<Project>): Danh sách dự án PM có thể mời
         * leadTaskCount (int): Số công việc lớn đang chủ trì (Task Lead)
         * completedSubTasks (int) / totalSubTasks (int): Thống kê việc con hoàn thành
         * completionRate (int): Tỷ lệ % hoàn thành tổng thể
     ========================================================================= --%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<%-- ◀ SERVLET: ProfileServlet → setAttribute("profileUser") --%>
<c:set var="pageTitle" value="Hồ Sơ: ${fn:escapeXml(profileUser.fullName)} &bull; TeamWork Hub" scope="request" />
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container py-4 my-auto profile-page-shell">

    <!-- 1. Breadcrumb & Nút Quay lại / Tác vụ nhanh -->
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-4">
        <a href="${pageContext.request.contextPath}/project?action=list" 
           class="btn btn-outline-secondary btn-sm rounded-pill px-3 shadow-2xs d-inline-flex align-items-center gap-1.5 fw-medium">
            <i class="bi bi-arrow-left"></i> <span>Quay lại Không gian làm việc</span>
        </a>
        <div class="d-flex align-items-center gap-2">
            <button type="button" class="btn btn-sm btn-light border rounded-pill px-3 py-1 fs-8 text-secondary d-inline-flex align-items-center gap-1.5 shadow-2xs hover-bg-light"
                    onclick="copyProfileLink()" id="btnCopyProfile" title="Sao chép đường dẫn hồ sơ công khai vào bộ nhớ tạm">
                <i class="bi bi-share-fill text-primary"></i> <span id="btnCopyProfileText">Chia sẻ hồ sơ</span>
            </button>
            <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-3 py-1.5 fs-8 fw-semibold d-inline-flex align-items-center gap-1">
                <i class="bi bi-patch-check-fill"></i> Hồ sơ chuyên môn công khai
            </span>
        </div>
    </div>

    <!-- 2. Thông báo Flash — UI-04: Floating Toast -->
    <jsp:include page="/includes/toast.jsp" />

    <!-- =========================================================================
         [SECTION 1: IDENTITY & CV HEADER / THÔNG TIN CHUYÊN MÔN]
         VI: Hiển thị cover banner, avatar, họ tên, chuyên môn, bio, dải kỹ năng và liên kết
         EN: Displays cover banner, avatar, name, role, bio, tech stack badges, and social links
         ========================================================================= -->
    <div class="card profile-identity-card">
        <!-- Ảnh bìa công nghệ phát quang (Tech Aurora Cover) -->
        <div class="profile-cover-banner">
            <div class="profile-cover-glow profile-cover-glow--1"></div>
            <div class="profile-cover-glow profile-cover-glow--2"></div>
        </div>

        <div class="profile-identity-body">
            <div class="row align-items-start g-4">
                
<!-- Cột Trái: Avatar & Tên & Chuyên Môn & Bio -->
<div class="col-12 col-lg-8">
    <!-- Khối Avatar phủ đè viền ảnh bìa -->
    <div class="profile-avatar-wrapper">
        <div class="profile-avatar-banner" title="${fn:escapeXml(profileUser.fullName)}">
            <c:out value="${not empty profileUser.fullName ? profileUser.fullName.substring(0, 1).toUpperCase() : 'U'}" />
        </div>
        <span class="profile-online-dot" title="Tài khoản đang hoạt động"></span>
    </div>

    <div>
        <!-- Họ tên & Badge Chức danh -->
        <div class="d-flex flex-wrap align-items-center gap-2 mb-1">
            <h2 class="fw-extrabold text-dark mb-0 tracking-tight">${fn:escapeXml(profileUser.fullName)}</h2>
            <i class="bi bi-patch-check-fill text-primary fs-5" title="Thành viên đã xác thực danh tính"></i>
            <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-3 py-1 fs-8 fw-semibold ms-1">
                <i class="bi bi-briefcase-fill me-1"></i>${fn:escapeXml(profileUser.role)}
            </span>
        </div>

        <!-- Thông tin Username & Email -->
        <div class="d-flex flex-wrap align-items-center gap-3 text-muted fs-8 mb-3 mt-1">
            <span class="d-inline-flex align-items-center gap-1">
                <i class="bi bi-at text-primary"></i>${fn:escapeXml(profileUser.username)}
            </span>
            <span class="d-inline-flex align-items-center gap-1.5" title="Email liên lạc chính thức">
                <i class="bi bi-envelope text-secondary"></i>
                <c:choose>
                    <c:when test="${isOwner}">
                        <span class="text-dark fw-medium">${fn:escapeXml(profileUser.email)}</span>
                    </c:when>
                    <c:otherwise>
                        <span class="text-muted" title="Email liên lạc">${fn:escapeXml(profileUser.email)}</span>
                    </c:otherwise>
                </c:choose>
            </span>
            <c:if test="${isOwner}">
                <span class="badge bg-light text-secondary border rounded-pill fs-9 px-2 py-0.5">
                    <i class="bi bi-shield-lock me-1"></i>Chính chủ
                </span>
            </c:if>
        </div>
                        
        <!-- Bio giới thiệu bản thân -->
        <p class="profile-bio-text">
            <c:choose>
                <c:when test="${not empty profileUser.bio}">
                    <c:out value="${profileUser.bio}" />
                </c:when>
                <c:otherwise>
                    <span class="fst-italic text-muted">Chưa cập nhật lời giới thiệu bản thân.</span>
                </c:otherwise>
            </c:choose>
        </p>

        <!-- Dải Kỹ năng Chuyên Môn (Tech Stack Badges) -->
        <div class="d-flex flex-wrap align-items-center gap-2 mb-3">
            <c:choose>
                <c:when test="${not empty profileUser.skillList}">
                    <c:forEach items="${profileUser.skillList}" var="sk">
                        <span class="profile-skill-badge shadow-2xs">
                            <i class="bi bi-code-slash text-primary"></i> ${fn:escapeXml(sk)}
                        </span>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <span class="text-muted fs-8 fst-italic">Chưa thiết lập danh sách kỹ năng chuyên môn.</span>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- Liên kết Mạng xã hội công việc (GitHub / LinkedIn) -->
        <div class="d-flex flex-wrap align-items-center gap-2 pt-1">
            <c:if test="${not empty profileUser.githubUrl}">
                <a href="${fn:escapeXml(profileUser.githubUrl)}" target="_blank" rel="noopener noreferrer" 
                   class="profile-social-btn profile-social-btn--github shadow-2xs">
                    <i class="bi bi-github fs-7"></i>
                    <span>GitHub</span>
                    <i class="bi bi-box-arrow-up-right fs-9 opacity-75 ms-1"></i>
                </a>
            </c:if>
            <c:if test="${not empty profileUser.linkedinUrl}">
                <a href="${fn:escapeXml(profileUser.linkedinUrl)}" target="_blank" rel="noopener noreferrer" 
                   class="profile-social-btn profile-social-btn--linkedin shadow-2xs">
                    <i class="bi bi-linkedin fs-7"></i>
                    <span>LinkedIn</span>
                    <i class="bi bi-box-arrow-up-right fs-9 opacity-75 ms-1"></i>
                </a>
            </c:if>
        </div>

                    </div>
                </div>

                <!-- Cột Phải: Nút Hành Động Nhanh (Chỉnh sửa hoặc Mời vào dự án) -->
                <div class="col-12 col-lg-4 text-start text-lg-end pt-lg-2">
                    <c:choose>
                        <%-- KỊCH BẢN 1: HỒ SƠ CHÍNH MÌNH ➔ NÚT CHỈNH SỬA --%>
                        <c:when test="${isOwner}">
                            <button type="button" class="btn btn-primary-custom px-4 py-2 rounded-pill fw-semibold shadow-sm fs-7 d-inline-flex align-items-center gap-2" 
                                    data-bs-toggle="modal" data-bs-target="#editProfileModal">
                                <i class="bi bi-pencil-square"></i>
                                <span>Chỉnh sửa hồ sơ</span>
                            </button>
                        </c:when>

                        <%-- KỊCH BẢN 2: HỒ SƠ NGƯỜI KHÁC ➔ NÚT MỜI NHANH (NẾU LÀ PM) --%>
                        <c:otherwise>
                            <%-- ◀ SERVLET: ProfileServlet → setAttribute("availableProjectsToInvite") --%>
                            <c:if test="${not empty availableProjectsToInvite}">
                                <button type="button" class="btn btn-success px-4 py-2 rounded-pill fw-semibold shadow-sm fs-7 d-inline-flex align-items-center gap-2" 
                                        data-bs-toggle="modal" data-bs-target="#quickInviteModal">
                                    <i class="bi bi-person-plus-fill"></i>
                                    <span>+ Mời vào Dự án của tôi</span>
                                </button>
                            </c:if>
                        </c:otherwise>
                    </c:choose>
                </div>

            </div>
        </div>
    </div>

    <!-- =========================================================================
         [SECTION 2: PRODUCTIVITY METRICS / BẢNG CHỈ SỐ NĂNG SUẤT BENTO-GRID]
         VI: Tổng hợp số lượng dự án, task lead, tiến độ việc con từ cơ sở dữ liệu
         EN: Aggregated workspace count, task leads, and subtask completion rates from DB
         ========================================================================= -->
    <div class="mb-5">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <h5 class="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                <i class="bi bi-graph-up-arrow text-primary"></i>
                <span>Chỉ số Năng suất &amp; Cống hiến Real-time</span>
            </h5>
            <span class="fs-9 text-muted d-none d-sm-inline">Tự động tổng hợp dữ liệu thực tế từ hệ thống</span>
        </div>

        <div class="row g-3">
            <!-- Chỉ số 1: Dự án tham gia -->
            <div class="col-6 col-lg-3">
                <div class="stat-card-modern h-100 d-flex flex-column justify-content-between">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                        <span class="fs-8 text-secondary fw-semibold">Không gian dự án</span>
                        <div class="stat-icon-wrapper stat-icon-wrapper--primary shadow-2xs">
                            <i class="bi bi-folder2-open"></i>
                        </div>
                    </div>
                    <%-- ◀ SERVLET: ProfileServlet → setAttribute("userProjects") --%>
                    <div class="stat-number-display text-primary">${userProjects.size()}</div>
                    <span class="fs-9 text-muted mt-1">không gian đang tham gia</span>
                </div>
            </div>

            <!-- Chỉ số 2: Task Lead -->
            <div class="col-6 col-lg-3">
                <div class="stat-card-modern h-100 d-flex flex-column justify-content-between">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                        <span class="fs-8 text-secondary fw-semibold">Công việc chủ trì</span>
                        <div class="stat-icon-wrapper stat-icon-wrapper--warning shadow-2xs">
                            <i class="bi bi-person-workspace"></i>
                        </div>
                    </div>
                    <%-- ◀ SERVLET: ProfileServlet → setAttribute("leadTaskCount") --%>
                    <div class="stat-number-display text-warning">${leadTaskCount}</div>
                    <span class="fs-9 text-muted mt-1">công việc lớn phụ trách</span>
                </div>
            </div>

            <!-- Chỉ số 3: Việc con hoàn thành -->
            <div class="col-6 col-lg-3">
                <div class="stat-card-modern h-100 d-flex flex-column justify-content-between">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                        <span class="fs-8 text-secondary fw-semibold">Nhiệm vụ đóng lại</span>
                        <div class="stat-icon-wrapper stat-icon-wrapper--success shadow-2xs">
                            <i class="bi bi-check2-all"></i>
                        </div>
                    </div>
                    <%-- ◀ SERVLET: ProfileServlet → setAttribute("completedSubTasks") --%>
                    <%-- ◀ SERVLET: ProfileServlet → setAttribute("totalSubTasks") --%>
                    <div class="stat-number-display text-success">
                        ${completedSubTasks} <span class="fs-6 text-muted fw-normal">/ ${totalSubTasks}</span>
                    </div>
                    <span class="fs-9 text-muted mt-1">nhiệm vụ bàn giao xong</span>
                </div>
            </div>

            <!-- Chỉ số 4: Tỷ lệ hoàn thành -->
            <div class="col-6 col-lg-3">
                <div class="stat-card-modern h-100 d-flex flex-column justify-content-between">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                        <span class="fs-8 text-secondary fw-semibold">Tỷ lệ hoàn tất</span>
                        <c:choose>
                            <c:when test="${completionRate >= 80}">
                                <span class="stat-rate-badge bg-success-subtle text-success border border-success-subtle">Xuất sắc</span>
                            </c:when>
                            <c:when test="${completionRate >= 50}">
                                <span class="stat-rate-badge bg-primary-subtle text-primary border border-primary-subtle">Tích cực</span>
                            </c:when>
                            <c:otherwise>
                                <span class="stat-rate-badge bg-warning-subtle text-warning-emphasis border border-warning-subtle">Tiến độ tốt</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <%-- ◀ SERVLET: ProfileServlet → setAttribute("completionRate") --%>
                    <div class="stat-number-display text-dark">${completionRate}%</div>
                    <div class="profile-progress-track mt-2">
                        <div class="profile-progress-fill ${completionRate == 100 ? 'is-done' : ''}" style="width: ${completionRate}%;"></div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- =========================================================================
         [SECTION 3: WORKSPACE LIST / DANH SÁCH DỰ ÁN THAM GIA]
         VI: Liệt kê các dự án người dùng đang tham gia cùng tiến độ động
         EN: Lists active projects the user belongs to along with dynamic progress
         ========================================================================= -->
    <div class="mb-5">
        <div class="d-flex align-items-center justify-content-between mb-3">
            <h5 class="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                <i class="bi bi-kanban text-primary"></i>
                <span>Các Không Gian Dự Án Đang Tham Gia (${userProjects.size()})</span>
            </h5>
            <span class="fs-9 text-muted">Truy cập nhanh bảng Kanban và tiến độ</span>
        </div>

        <div class="row g-3">
            <c:forEach items="${userProjects}" var="p">
                <div class="col-12 col-md-6 col-lg-4">
                    <div class="profile-project-card h-100 d-flex flex-column justify-content-between">
                        <div>
<div class="d-flex justify-content-between align-items-center mb-2">
    <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2.5 py-1 fs-9 fw-bold">
        #${fn:escapeXml(p.projectCode)}
                                </span>
                                <span class="text-muted fs-9 d-flex align-items-center gap-1">
                                    <i class="bi bi-calendar3"></i> ${p.createdAt}
                                </span>
                            </div>
<h6 class="fw-bold text-dark mb-1 fs-7">
    <a href="${pageContext.request.contextPath}/task?projectId=${p.id}" class="text-dark text-decoration-none hover-primary">
        ${fn:escapeXml(p.name)}
    </a>
</h6>
<p class="text-secondary fs-8 mb-3" style="display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; min-height: 2.4em;">
    <c:out value="${not empty p.description ? p.description : 'Chưa có mô tả cho dự án này.'}" />
                            </p>
                        </div>
                        <div class="pt-2 border-top">
                            <div class="d-flex justify-content-between align-items-center fs-9 text-muted mb-1.5">
                                <span>Tiến độ dự án</span>
                                <span class="fw-bold text-dark">${p.progressPercentage}%</span>
                            </div>
                            <div class="profile-progress-track mb-3">
                                <div class="profile-progress-fill ${p.progressPercentage == 100 ? 'is-done' : ''}" style="width: ${p.progressPercentage}%;"></div>
                            </div>
                            <a href="${pageContext.request.contextPath}/task?projectId=${p.id}" 
                               class="btn btn-outline-primary btn-sm w-100 rounded-pill py-1.5 fs-8 fw-semibold d-inline-flex align-items-center justify-content-center gap-1.5 shadow-2xs">
                                <span>Vào không gian dự án</span>
                                <i class="bi bi-arrow-right"></i>
                            </a>
                        </div>
                    </div>
                </div>
            </c:forEach>

            <c:if test="${empty userProjects}">
                <div class="col-12 text-center py-5 bg-white rounded-4 border shadow-2xs">
                    <div class="avatar-sm rounded-circle bg-light text-muted mx-auto d-flex align-items-center justify-content-center mb-2" style="width: 48px; height: 48px;">
                        <i class="bi bi-folder-x fs-4 opacity-50"></i>
                    </div>
                    <h6 class="fw-bold text-dark mb-1 fs-7">Chưa tham gia dự án nào</h6>
                    <p class="text-muted fs-8 mb-0">Thành viên này hiện chưa được thêm vào không gian làm việc nào trong hệ thống.</p>
                </div>
            </c:if>
        </div>
    </div>

</div>

<script>
    function copyProfileLink() {
        var currentUrl = window.location.href;
        if (navigator.clipboard && window.isSecureContext) {
            navigator.clipboard.writeText(currentUrl).then(function() {
                showCopyFeedback();
            }).catch(function() {
                fallbackCopy(currentUrl);
            });
        } else {
            fallbackCopy(currentUrl);
        }
    }

    function fallbackCopy(text) {
        var textArea = document.createElement("textarea");
        textArea.value = text;
        textArea.style.position = "fixed";
        textArea.style.opacity = "0";
        document.body.appendChild(textArea);
        textArea.select();
        try {
            document.execCommand('copy');
            showCopyFeedback();
        } catch (err) {
            console.error('Không thể sao chép liên kết:', err);
        }
        document.body.removeChild(textArea);
    }

    function showCopyFeedback() {
        var btn = document.getElementById('btnCopyProfile');
        var text = document.getElementById('btnCopyProfileText');
        if (text) {
            var oldText = text.textContent;
            text.textContent = 'Đã sao chép!';
            if (btn) btn.classList.add('btn-primary', 'text-white');
            setTimeout(function() {
                text.textContent = oldText;
                if (btn) btn.classList.remove('btn-primary', 'text-white');
            }, 2000);
        }
    }
</script>

<!-- =========================================================================
     [MODAL 1: EDIT PROFILE FORM / CHỈNH SỬA HỒ SƠ CÁ NHÂN (CHÍNH CHỦ)]
     VI: Form cập nhật thông tin cá nhân gửi về ProfileServlet
     EN: Form to update user profile information sent to ProfileServlet
     - Action: ${pageContext.request.contextPath}/profile
     - Method: POST
     - Handled by: ProfileServlet.doPost() -> handleUpdateProfile()
     - Params: action=update, userId, fullName, role, bio, skills, githubUrl, linkedinUrl
     ========================================================================= -->
<c:if test="${isOwner}">
    <div class="modal fade" id="editProfileModal" tabindex="-1" aria-labelledby="editProfileModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                <div class="modal-header border-0 pb-0">
                    <h5 class="modal-title fw-bold text-dark" id="editProfileModalLabel">
                        <i class="bi bi-pencil-square text-primary me-2"></i>Chỉnh sửa hồ sơ cá nhân
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>

                <%-- ▶ SERVLET: /profile → ProfileServlet.doPost() → case "update" --%>
                <form action="${pageContext.request.contextPath}/profile" method="post">
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" name="userId" value="${profileUser.id}">

                    <div class="modal-body py-3">
                        <div class="row g-3 mb-3">
                            <div class="col-12 col-md-6">
<label for="inputFullName" class="form-label fw-semibold fs-7 text-dark mb-1">Họ và tên <span class="text-danger">*</span></label>
<div class="input-group">
    <span class="input-group-text bg-light border-end-0 text-muted"><i class="bi bi-person"></i></span>
    <input type="text" class="form-control border-start-0 fs-7" id="inputFullName" name="fullName" value="${fn:escapeXml(profileUser.fullName)}" required>
</div>
</div>
<div class="col-12 col-md-6">
<label for="inputRole" class="form-label fw-semibold fs-7 text-dark mb-1">Chuyên môn / Chức danh</label>
<div class="input-group">
    <span class="input-group-text bg-light border-end-0 text-muted"><i class="bi bi-briefcase"></i></span>
    <input type="text" class="form-control border-start-0 fs-7" id="inputRole" name="role" value="${fn:escapeXml(profileUser.role)}" placeholder="Ví dụ: Kỹ sư Phần mềm Fullstack">
</div>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label for="inputBio" class="form-label fw-semibold fs-7 text-dark mb-1 d-flex justify-content-between align-items-center">
                                <span>Lời giới thiệu bản thân (Bio ngắn)</span>
                                <span class="text-muted fs-9 fw-normal">Tối đa 250 ký tự</span>
                            </label>
                            <textarea class="form-control fs-7 rounded-3" id="inputBio" name="bio" rows="3" maxlength="250" 
                                      placeholder="Mô tả ngắn gọn đam mê, chuyên môn và phong cách làm việc của bạn...">${fn:escapeXml(profileUser.bio)}</textarea>
                        </div>

                        <div class="mb-3">
                            <label for="inputSkills" class="form-label fw-semibold fs-7 text-dark mb-1">
                                Kỹ năng chuyên môn
                            </label>
<div class="input-group mb-2">
    <span class="input-group-text bg-light border-end-0 text-muted"><i class="bi bi-code-slash"></i></span>
    <input type="text" class="form-control border-start-0 fs-7" id="inputSkills" name="skills" value="${fn:escapeXml(profileUser.skills)}" 
           placeholder="Cách nhau bằng dấu phẩy, ví dụ: Java, Jakarta EE, PostgreSQL, MySQL, Docker, RESTful API">
</div>
<div class="form-text fs-9 text-muted d-flex flex-wrap align-items-center gap-1">
    <span>Gợi ý kỹ năng:</span>
    <span role="button" tabindex="0" class="profile-skill-chip-interactive" onclick="addSkill('Java')">+ Java</span>
    <span role="button" tabindex="0" class="profile-skill-chip-interactive" onclick="addSkill('PostgreSQL')">+ PostgreSQL</span>
    <span role="button" tabindex="0" class="profile-skill-chip-interactive" onclick="addSkill('MySQL')">+ MySQL</span>
                                <span role="button" tabindex="0" class="profile-skill-chip-interactive" onclick="addSkill('Docker')">+ Docker</span>
                                <span role="button" tabindex="0" class="profile-skill-chip-interactive" onclick="addSkill('UI/UX')">+ UI/UX</span>
                                <span role="button" tabindex="0" class="profile-skill-chip-interactive" onclick="addSkill('Spring Boot')">+ Spring Boot</span>
                                <span role="button" tabindex="0" class="profile-skill-chip-interactive" onclick="addSkill('REST API')">+ REST API</span>
                            </div>
                        </div>

                        <div class="row g-3">
                            <div class="col-12 col-md-6">
<label for="inputGithub" class="form-label fw-semibold fs-7 text-dark mb-1">Liên kết GitHub</label>
<div class="input-group">
    <span class="input-group-text bg-light border-end-0 text-muted"><i class="bi bi-github"></i></span>
    <input type="url" class="form-control border-start-0 fs-7" id="inputGithub" name="githubUrl" value="${fn:escapeXml(profileUser.githubUrl)}" placeholder="https://github.com/username">
</div>
</div>
<div class="col-12 col-md-6">
<label for="inputLinkedin" class="form-label fw-semibold fs-7 text-dark mb-1">Liên kết LinkedIn</label>
<div class="input-group">
    <span class="input-group-text bg-light border-end-0 text-muted"><i class="bi bi-linkedin text-primary"></i></span>
    <input type="url" class="form-control border-start-0 fs-7" id="inputLinkedin" name="linkedinUrl" value="${fn:escapeXml(profileUser.linkedinUrl)}" placeholder="https://linkedin.com/in/username">
</div>
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer border-0 pt-0">
                        <button type="button" class="btn btn-light rounded-pill px-4 fs-7 fw-semibold" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary-custom rounded-pill px-4 fs-7 fw-semibold shadow-sm">
                            <i class="bi bi-check-lg me-1"></i> Lưu thay đổi
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script>
        function addSkill(skillName) {
            var input = document.getElementById('inputSkills');
            if (input.value.trim() === '') {
                input.value = skillName;
            } else {
                if (!input.value.includes(skillName)) {
                    input.value = input.value.trim() + ', ' + skillName;
                }
            }
        }
    </script>
</c:if>

<!-- =========================================================================
     [MODAL 2: QUICK PROJECT INVITE FORM / MỜI NHANH VÀO DỰ ÁN]
     VI: Form gửi lời mời tham gia dự án do người xem quản lý gửi về ProjectInviteServlet
     EN: Form for project managers to dispatch project invitations
     - Action: ${pageContext.request.contextPath}/invite
     - Method: POST
     - Handled by: ProjectInviteServlet.doPost() -> handleSendInvite()
     - Params: action=sendInvite, usernameOrEmail, projectId
     ========================================================================= -->
<c:if test="${not isOwner && not empty availableProjectsToInvite}">
    <div class="modal fade" id="quickInviteModal" tabindex="-1" aria-labelledby="quickInviteModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                <div class="modal-header border-0 pb-0">
                    <h5 class="modal-title fw-bold text-dark" id="quickInviteModalLabel">
                        <i class="bi bi-person-plus-fill text-success me-2"></i>Mời ${fn:escapeXml(profileUser.fullName)} vào Dự án
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>

                <%-- ▶ SERVLET: /invite → ProjectInviteServlet.doPost() → case "sendInvite" → handleSendInvite() --%>
                <form action="${pageContext.request.contextPath}/invite" method="post">
                    <input type="hidden" name="action" value="sendInvite">
                    <input type="hidden" name="usernameOrEmail" value="${fn:escapeXml(profileUser.username)}">

                    <div class="modal-body py-3">
                        <p class="text-muted fs-8 mb-3">
                            Chọn dự án bạn muốn mời <strong>${fn:escapeXml(profileUser.fullName)}</strong> tham gia. Lời mời sẽ có hiệu lực trong <strong>7 ngày</strong>.
                        </p>

                        <div class="mb-3">
                            <label for="selectProject" class="form-label fw-semibold fs-7 text-dark">
                                Chọn dự án của bạn <span class="text-danger">*</span>
                            </label>
                            <select class="form-select fs-7 rounded-3" id="selectProject" name="projectId" required>
                                <c:forEach items="${availableProjectsToInvite}" var="ap">
                                    <option value="${ap.id}">[${fn:escapeXml(ap.projectCode)}] ${fn:escapeXml(ap.name)}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>

                    <div class="modal-footer border-0 pt-0">
                        <button type="button" class="btn btn-light rounded-pill px-4 fs-7 fw-semibold" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-success rounded-pill px-4 fs-7 fw-semibold shadow-sm">
                            <i class="bi bi-send-fill me-1"></i> Gửi lời mời ngay
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<jsp:include page="/includes/footer.jsp" />
