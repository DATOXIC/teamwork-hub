<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- 1. Đặt tiêu đề cho tab trình duyệt và nhúng Header, Navbar --%>
<c:set var="pageTitle" value="Không gian làm việc &bull; TeamWork Hub" />
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container py-4 my-auto">

    <!-- 2. Header Section: Tiêu đề trang & Nút Tạo dự án mới -->
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4 pb-3 border-bottom">
        <div>
            <h3 class="fw-bold text-dark mb-1 d-flex align-items-center gap-2">
                <i class="bi bi-grid-1x2-fill text-primary"></i>
                <span>Không Gian Làm Việc</span>
            </h3>
            <p class="text-muted fs-8 mb-0">
                Chào mừng trở lại, <strong>${sessionScope.currentUser.fullName}</strong>! Bạn đang tham gia <strong>${myProjects.size()} dự án</strong>.
            </p>
        </div>
        <div class="d-flex align-items-center gap-2">
            <!-- Nút mở Modal Xin Gia Nhập Bằng Mã -->
            <button type="button" class="btn btn-outline-secondary px-3 py-1-5 rounded-pill fw-semibold shadow-2xs fs-8 d-flex align-items-center gap-2" 
                    data-bs-toggle="modal" data-bs-target="#joinByCodeModal">
                <i class="bi bi-key-fill text-primary"></i> Nhập Mã Xin Vào
            </button>
            
            <!-- Nút kích hoạt Modal Tạo Dự Án Mới -->
            <button type="button" class="btn btn-primary-custom px-3-5 py-1-5 rounded-pill fw-semibold shadow-2xs fs-8 d-flex align-items-center gap-2 text-white" 
                    data-bs-toggle="modal" data-bs-target="#createProjectModal">
                <i class="bi bi-plus-circle-fill"></i> Tạo dự án mới
            </button>
        </div>
    </div>

    <!-- 3. Thông báo Flash — UI-04: Floating Toast (tự biến mất sau 4 giây) -->
    <jsp:include page="/includes/toast.jsp" />


    <!-- =========================================================================
         4. HỘP THƯ LỜI MỜI / YÊU CẦU XIN GIA NHẬP ĐANG CHỜ PHẢN HỒI (PENDING INVITES)
         ========================================================================= -->
    <c:if test="${not empty pendingInvites}">
        <div class="mb-4">
            <div class="d-flex align-items-center justify-content-between mb-3">
                <div class="d-flex align-items-center gap-2">
                    <span class="p-1 bg-primary-subtle text-primary rounded-2 lh-1">
                        <i class="bi bi-envelope-paper-heart-fill fs-7"></i>
                    </span>
                    <h6 class="fw-bold text-dark fs-7 mb-0">Hộp Thư Yêu Cầu & Lời Mời (${pendingInvites.size()})</h6>
                </div>
                <span class="badge bg-warning-subtle text-dark border border-warning-subtle rounded-pill px-2 py-0-5 fs-9 fw-semibold">
                    Đang chờ bạn phản hồi
                </span>
            </div>

            <div class="row g-3">
                <c:forEach items="${pendingInvites}" var="inv">
                    <div class="col-12 col-lg-6">
                        <div class="pending-invite-card h-100 d-flex flex-column justify-content-between">
                            <div>
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <span class="project-code-badge" onclick="copyProjectCode('${inv.projectCode}')" title="Bấm để sao chép mã">
                                        <i class="bi bi-hash"></i>${inv.projectCode}
                                        <i class="bi bi-copy text-primary fs-9 ms-1"></i>
                                    </span>
                                    <span class="badge ${inv.statusBadgeClass} rounded-pill px-2 py-0-5 fs-9">
                                        ${inv.statusLabel}
                                    </span>
                                </div>
                                <h6 class="fw-bold text-dark mb-1 fs-7">${inv.projectName}</h6>
                                <p class="text-secondary fs-8 mb-2">
                                    <c:choose>
                                        <c:when test="${inv.type == 'INVITATION'}">
                                            <i class="bi bi-person-fill text-primary me-1"></i> Trưởng nhóm <strong>${inv.senderName}</strong> đã gửi lời mời bạn vào dự án này.
                                        </c:when>
                                        <c:otherwise>
                                            <i class="bi bi-person-plus-fill text-warning me-1"></i> Thành viên <strong>${inv.senderName}</strong> gửi đơn xin gia nhập dự án của bạn.
                                        </c:otherwise>
                                    </c:choose>
                                </p>
                                <div class="fs-9 text-muted mb-2">
                                    <i class="bi bi-clock-history me-1"></i> Hạn phản hồi: <strong class="text-danger">${inv.expiredAt}</strong> (Còn hiệu lực)
                                </div>
                            </div>

                            <!-- Nút bấm Duyệt / Từ chối -->
                            <div class="d-flex align-items-center gap-2 pt-2 border-top">
                                <form method="post" action="${pageContext.request.contextPath}/invite" class="m-0 flex-grow-1">
                                    <input type="hidden" name="action" value="accept">
                                    <input type="hidden" name="inviteId" value="${inv.id}">
                                    <button type="submit" class="btn btn-success btn-sm w-100 rounded-pill fw-semibold fs-8 py-1 shadow-2xs">
                                        <i class="bi bi-check-circle-fill me-1"></i> Đồng ý gia nhập
                                    </button>
                                </form>
                                <form method="post" action="${pageContext.request.contextPath}/invite" class="m-0 flex-grow-1" onsubmit="return confirm('Bạn có chắc chắn muốn từ chối yêu cầu này?');">
                                    <input type="hidden" name="action" value="reject">
                                    <input type="hidden" name="inviteId" value="${inv.id}">
                                    <button type="submit" class="btn btn-outline-secondary btn-sm w-100 rounded-pill fw-semibold fs-8 py-1">
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
         5. PROJECT GRID: LƯỚI HIỂN THỊ DANH SÁCH CARD DỰ ÁN
         ========================================================================= -->
    <!-- LƯỚI 1: DỰ ÁN CỦA TÔI -->
    <div class="d-flex align-items-center justify-content-between mb-3 mt-2">
        <h6 class="fw-bold text-dark fs-7 mb-0 d-flex align-items-center gap-2">
            <i class="bi bi-star-fill text-warning"></i>
            <span>Dự án của tôi (${myProjects.size()})</span>
        </h6>
        <span class="fs-9 text-muted">Không gian đang hoạt động</span>
    </div>

    <div class="row g-3 mb-5">
        <c:forEach items="${myProjects}" var="p">
            <div class="col-12 col-md-6 col-lg-4">
                <div class="project-card">
                    <div>
                        <!-- Header thẻ: Mã dự án 1-click copy + Vai trò + Số lượng thành viên -->
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <span class="project-code-badge" onclick="copyProjectCode('${p.projectCode}')" title="Bấm để sao chép mã dự án">
                                <i class="bi bi-hash"></i>${p.projectCode}
                                <i class="bi bi-copy text-primary fs-9 ms-1" id="copy-icon-${p.projectCode}"></i>
                            </span>
                            <div class="d-flex align-items-center gap-1">
                                <!-- UI-05: Badge vai trò PM / Thành viên -->
                                <c:choose>
                                    <c:when test="${p.ownerId == sessionScope.currentUser.id}">
                                        <span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill px-2 fs-9" title="Bạn là Trưởng Dự Án">
                                            <i class="bi bi-star-fill"></i> PM
                                        </span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle rounded-pill px-2 fs-9" title="Bạn là thành viên">
                                            <i class="bi bi-person-fill"></i> TV
                                        </span>
                                    </c:otherwise>
                                </c:choose>
                                <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5 fs-9" title="Số lượng thành viên hiện tại">
                                    <i class="bi bi-people-fill text-primary me-1"></i> ${memberCountMap[p.id]}/10
                                </span>
                            </div>
                        </div>

                        <!-- Tên dự án -->
                        <h5 class="fw-bold text-dark mb-1 fs-6 lh-sm">${p.name}</h5>

                        <!-- Mô tả dự án -->
                        <p class="text-secondary fs-8 mb-3" style="display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; min-height: 2.4rem;">
                            ${not empty p.description ? p.description : 'Chưa có mô tả cho dự án này.'}
                        </p>
                    </div>

                    <!-- Footer thẻ: Tiến độ hoàn thành + Nút Vào dự án -->
                    <div class="pt-3 border-top">
                        <div class="d-flex justify-content-between align-items-center fs-8 text-muted mb-1">
                            <span>Tiến độ: <strong class="text-dark">${p.doneTasks}/${p.totalTasks} việc</strong></span>
                            <span class="fw-bold text-primary">${p.progressPercentage}%</span>
                        </div>
                        <div class="project-progress-container mb-3">
                            <div class="project-progress-bar" data-progress="${p.progressPercentage}%" style="width: 0%;"></div>
                        </div>
                        <a href="${pageContext.request.contextPath}/task?action=list&projectId=${p.id}" 
                           class="btn btn-primary-custom w-100 rounded-pill py-1-5 fs-8 fw-semibold d-flex align-items-center justify-content-center gap-2 shadow-2xs text-white">
                            <span>Vào không gian dự án</span>
                            <i class="bi bi-arrow-right"></i>
                        </a>
                    </div>
                </div>
            </div>
        </c:forEach>
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
                        <button type="button" class="btn btn-primary-custom btn-sm rounded-pill px-3 fs-8 text-white"
                                data-bs-toggle="modal" data-bs-target="#createProjectModal">
                            <i class="bi bi-plus-circle-fill me-1"></i> Tạo dự án
                        </button>
                    </div>
                </div>
            </div>
        </c:if>
    </div>

    <!-- LƯỚI 2: CÁC DỰ ÁN KHÁC (CÓ THỂ XIN VÀO) -->
    <div class="d-flex align-items-center justify-content-between mb-3">
        <h6 class="fw-bold text-dark fs-7 mb-0 d-flex align-items-center gap-2">
            <i class="bi bi-globe text-secondary"></i>
            <span>Dự án khác trong hệ thống (${otherProjects.size()})</span>
        </h6>
        <span class="fs-9 text-muted">Có thể gửi yêu cầu xin gia nhập</span>
    </div>

    <div class="row g-3">
        <c:forEach items="${otherProjects}" var="p">
            <div class="col-12 col-md-6 col-lg-4">
                <div class="project-card-other">
                    <div>
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <span class="project-code-badge" onclick="copyProjectCode('${p.projectCode}')" title="Bấm để sao chép mã dự án">
                                <i class="bi bi-hash"></i>${p.projectCode}
                                <i class="bi bi-copy text-secondary fs-9 ms-1"></i>
                            </span>
                            <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5 fs-9">
                                <i class="bi bi-people-fill me-1"></i> ${memberCountMap[p.id]}/10
                            </span>
                        </div>
                        <h5 class="fw-bold text-dark mb-1 fs-6 lh-sm">${p.name}</h5>
                        <p class="text-secondary fs-8 mb-3" style="display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; min-height: 2.4rem;">
                            ${not empty p.description ? p.description : 'Chưa có mô tả cho dự án này.'}
                        </p>
                    </div>
                    <div class="pt-3 border-top text-center">
                        <button type="button" class="btn btn-outline-secondary w-100 rounded-pill py-1-5 fs-8 fw-semibold"
                                onclick="document.getElementById('inputProjectCode').value='${p.projectCode}'; new bootstrap.Modal(document.getElementById('joinByCodeModal')).show();">
                            <i class="bi bi-box-arrow-in-right me-1 text-primary"></i> Xin gia nhập
                        </button>
                    </div>
                </div>
            </div>
        </c:forEach>
        <c:if test="${empty otherProjects}">
            <div class="col-12 text-center py-4">
                <span class="text-muted fs-8">Không có dự án nào khác trên hệ thống.</span>
            </div>
        </c:if>
    </div>
</div>

<!-- =========================================================================
     6. MODAL 1: CỬA SỔ POP-UP XIN GIA NHẬP BẰNG MÃ DỰ ÁN (PROJECT CODE)
     ========================================================================= -->
<div class="modal fade" id="joinByCodeModal" tabindex="-1" aria-labelledby="joinByCodeModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg rounded-4 p-2">
            <div class="modal-header border-0 pb-0">
                <h5 class="modal-title fw-bold text-dark" id="joinByCodeModalLabel">
                    <i class="bi bi-key-fill text-primary me-2"></i>Xin gia nhập dự án bằng Mã
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            
            <form action="${pageContext.request.contextPath}/invite" method="post">
                <input type="hidden" name="action" value="requestJoin">

                <div class="modal-body py-3">
                    <p class="text-muted fs-8 mb-3">
                        Nhập <strong>Mã Dự Án (Project Code)</strong> do Trưởng nhóm cung cấp (ví dụ: <code>TW-HUB-01</code>) để gửi yêu cầu xin gia nhập.
                    </p>

                    <div class="mb-3">
                        <label for="inputProjectCode" class="form-label fw-semibold fs-7 text-dark">
                            Mã dự án <span class="text-danger">*</span>
                        </label>
                        <div class="input-group">
                            <span class="input-group-text bg-light border-end-0 fs-7 text-muted">
                                <i class="bi bi-hash"></i>
                            </span>
                            <input type="text" class="form-control text-uppercase fw-bold fs-7 rounded-end-3" 
                                   id="inputProjectCode" name="projectCode" 
                                   placeholder="Ví dụ: TW-HUB-01" required autofocus>
                        </div>
                    </div>
                </div>

                <div class="modal-footer border-0 pt-0">
                    <button type="button" class="btn btn-light rounded-pill px-4 fs-7 fw-semibold" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary-custom rounded-pill px-4 fs-7 fw-semibold shadow-sm">
                        <i class="bi bi-send-fill me-1"></i> Gửi yêu cầu xin vào
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- =========================================================================
     7. MODAL 2: CỬA SỔ POP-UP TẠO DỰ ÁN MỚI (CÓ THÊM Ô MÃ DỰ ÁN TÙY CHỌN)
     ========================================================================= -->
<div class="modal fade" id="createProjectModal" tabindex="-1" aria-labelledby="createProjectModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg rounded-4 p-2">
            <div class="modal-header border-0 pb-0">
                <h5 class="modal-title fw-bold text-dark" id="createProjectModalLabel">
                    <i class="bi bi-plus-circle text-primary me-2"></i>Tạo dự án mới
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            
            <!-- FORM SUBMIT POST VỀ PROJECTSERVLET -->
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
                        <label for="proj-code" class="form-label fw-semibold fs-7 text-dark d-flex align-items-center justify-content-between">
                            <span>Mã dự án (Tùy chọn)</span>
                            <span class="text-muted fs-9 fw-normal">Tự động tạo nếu để trống</span>
                        </label>
                        <input type="text" class="form-control text-uppercase rounded-3 fs-7" id="proj-code" name="projectCode" 
                               placeholder="Ví dụ: TW-HUB-01">
                    </div>

                    <div class="mb-3">
                        <label for="proj-desc" class="form-label fw-semibold fs-7 text-dark">
                            Mô tả mục tiêu dự án
                        </label>
                        <textarea class="form-control rounded-3 fs-7" id="proj-desc" name="description" rows="3" 
                                  placeholder="Mô tả ngắn gọn phạm vi và mục tiêu của dự án..."></textarea>
                    </div>
                </div>

                <div class="modal-footer border-0 pt-0">
                    <button type="button" class="btn btn-light rounded-pill px-4 fs-7 fw-semibold" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary-custom rounded-pill px-4 fs-7 fw-semibold shadow-sm">
                        <i class="bi bi-check-circle-fill me-1"></i> Khởi tạo dự án
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Toast thông báo Sao chép mã thành công -->
<div class="position-fixed bottom-0 end-0 p-3" style="z-index: 1100;">
    <div id="copyToast" class="toast align-items-center text-bg-dark border-0 rounded-3 shadow-lg" role="alert" aria-live="assertive" aria-atomic="true">
        <div class="d-flex">
            <div class="toast-body fs-8 py-2 d-flex align-items-center gap-2">
                <i class="bi bi-check2-circle text-success fs-6"></i>
                <span>Đã sao chép mã dự án: <strong id="copiedCodeText" class="text-info"></strong></span>
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
    </div>
</div>

<script>
function copyProjectCode(code) {
    if (!code) return;
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(code).then(showToast).catch(fallback);
    } else {
        fallback();
    }

    function fallback() {
        var temp = document.createElement('textarea');
        temp.value = code;
        document.body.appendChild(temp);
        temp.select();
        try {
            document.execCommand('copy');
            showToast();
        } catch (err) {
            console.error('Không thể copy', err);
        }
        document.body.removeChild(temp);
    }

    function showToast() {
        var textEl = document.getElementById('copiedCodeText');
        if (textEl) textEl.textContent = code;
        var toastEl = document.getElementById('copyToast');
        if (toastEl) {
            var toast = new bootstrap.Toast(toastEl, { delay: 2500 });
            toast.show();
        }
    }
}
</script>

<jsp:include page="/includes/footer.jsp" />
