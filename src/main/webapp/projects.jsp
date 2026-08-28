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
            <h2 class="fw-extrabold text-dark mb-1">
                <i class="bi bi-grid-1x2-fill text-primary me-2"></i>Dự án của bạn
            </h2>
            <p class="text-muted fs-7 mb-0">
                Chào mừng trở lại, <strong>${sessionScope.currentUser.fullName}</strong>! Hãy chọn một dự án để bắt đầu làm việc.
            </p>
        </div>
        <div class="d-flex align-items-center gap-2">
            <!-- Nút mở Modal Xin Gia Nhập Bằng Mã -->
            <button type="button" class="btn btn-outline-primary px-3 py-2 rounded-pill fw-semibold shadow-sm fs-7 d-flex align-items-center gap-2" 
                    data-bs-toggle="modal" data-bs-target="#joinByCodeModal">
                <i class="bi bi-key-fill"></i> Nhập Mã Xin Vào
            </button>
            
            <!-- Nút kích hoạt Modal Tạo Dự Án Mới -->
            <button type="button" class="btn btn-primary-custom px-4 py-2 rounded-pill fw-semibold shadow-sm fs-7 d-flex align-items-center gap-2" 
                    data-bs-toggle="modal" data-bs-target="#createProjectModal">
                <i class="bi bi-plus-circle-fill"></i> Tạo dự án mới
            </button>
        </div>
    </div>

    <!-- 3. Thông báo Flash (Toast Messages Thành công / Thất bại) -->
    <c:if test="${not empty toastSuccess}">
        <div class="alert alert-success alert-dismissible fade show fs-7 py-2 px-3 mb-4 rounded-3 border-0 shadow-sm d-flex align-items-center" role="alert">
            <i class="bi bi-check-circle-fill me-2 fs-6"></i>
            <div class="flex-grow-1">${toastSuccess}</div>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>
    <c:if test="${not empty toastError}">
        <div class="alert alert-danger alert-dismissible fade show fs-7 py-2 px-3 mb-4 rounded-3 border-0 shadow-sm d-flex align-items-center" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2 fs-6"></i>
            <div class="flex-grow-1">${toastError}</div>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger alert-dismissible fade show fs-7 py-2 px-3 mb-4 rounded-3 border-0 shadow-sm d-flex align-items-center" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2 fs-6"></i>
            <div class="flex-grow-1">${errorMessage}</div>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <!-- =========================================================================
         4. HỘP THƯ LỜI MỜI / YÊU CẦU XIN GIA NHẬP ĐANG CHỜ PHẢN HỒI (PENDING INVITES)
         ========================================================================= -->
    <c:if test="${not empty pendingInvites}">
        <div class="card border-0 bg-primary-subtle rounded-4 p-4 mb-4 shadow-sm">
            <div class="d-flex align-items-center justify-content-between mb-3">
                <div class="d-flex align-items-center gap-2">
                    <i class="bi bi-envelope-paper-heart-fill text-primary fs-5"></i>
                    <h5 class="fw-bold text-dark mb-0">Hộp Thư Yêu Cầu & Lời Mời Tham Gia Dự Án (${pendingInvites.size()})</h5>
                </div>
                <span class="badge bg-primary rounded-pill px-3 py-1 fs-9">Đang chờ bạn phản hồi</span>
            </div>

            <div class="row g-3">
                <c:forEach items="${pendingInvites}" var="inv">
                    <div class="col-12 col-lg-6">
                        <div class="bg-white rounded-3 p-3 border shadow-sm h-100 d-flex flex-column justify-content-between">
                            <div>
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <span class="badge bg-dark-navy text-white rounded-pill px-2 py-1 fs-9">
                                        <i class="bi bi-hash"></i> ${inv.projectCode}
                                    </span>
                                    <span class="badge ${inv.statusBadgeClass} rounded-pill px-2 py-1 fs-9">
                                        ${inv.statusLabel}
                                    </span>
                                </div>
                                <h6 class="fw-bold text-dark mb-1">${inv.projectName}</h6>
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
                                <div class="fs-9 text-muted mb-3">
                                    <i class="bi bi-clock-history me-1"></i> Hạn phản hồi: <strong class="text-danger">${inv.expiredAt}</strong> (Còn hiệu lực)
                                </div>
                            </div>

                            <!-- Nút bấm Duyệt / Từ chối -->
                            <div class="d-flex align-items-center gap-2 pt-2 border-top">
                                <form method="post" action="${pageContext.request.contextPath}/invite" class="m-0 flex-grow-1">
                                    <input type="hidden" name="action" value="accept">
                                    <input type="hidden" name="inviteId" value="${inv.id}">
                                    <button type="submit" class="btn btn-success btn-sm w-100 rounded-pill fw-semibold fs-8 py-1 shadow-sm">
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
    <h5 class="fw-bold text-dark mb-3 mt-2"><i class="bi bi-star-fill text-warning me-2"></i>Dự án của tôi</h5>
    <div class="row g-4 mb-5">
        <c:forEach items="${myProjects}" var="p">
            <div class="col-12 col-md-6 col-lg-4">
                <div class="card h-100 border-0 bg-white shadow-sm rounded-4 p-4 d-flex flex-column justify-content-between transition hover-shadow">
                    <div>
                        <div class="d-flex justify-content-between align-items-start mb-3">
                            <div class="d-flex align-items-center gap-2">
                                <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-3 py-1 fs-8">
                                    <i class="bi bi-folder2-open me-1"></i> ID: #${p.id}
                                </span>
                                <span class="badge bg-dark-navy text-white rounded-pill px-2 py-1 fs-9" title="Mã chia sẻ dự án">
                                    <i class="bi bi-hash"></i> ${p.projectCode}
                                </span>
                            </div>
                            <span class="badge bg-light text-secondary border rounded-pill px-2 py-1 fs-9" title="Số lượng thành viên hiện tại">
                                <i class="bi bi-people-fill text-primary me-1"></i> ${memberCountMap[p.id]}/10
                            </span>
                        </div>
                        <h5 class="fw-bold text-dark mb-2">${p.name}</h5>
                        <p class="text-secondary fs-7 mb-4 line-clamp-2">
                            ${not empty p.description ? p.description : 'Chưa có mô tả cho dự án này.'}
                        </p>
                    </div>
                    <div class="pt-3 border-top">
                        <div class="d-flex justify-content-between align-items-center fs-8 text-muted mb-1">
                            <span>Tiến độ hoàn thành</span>
                            <span class="fw-bold text-dark">${p.progressPercentage}%</span>
                        </div>
                        <div class="progress rounded-pill mb-3" style="height: 8px;">
                            <div class="progress-bar bg-primary" role="progressbar" 
                                 style="width: ${p.progressPercentage}%;" 
                                 aria-valuenow="${p.progressPercentage}" aria-valuemin="0" aria-valuemax="100"></div>
                        </div>
                        <a href="${pageContext.request.contextPath}/task?action=list&projectId=${p.id}" 
                           class="btn btn-outline-primary w-100 rounded-pill py-2 fs-7 fw-semibold d-flex align-items-center justify-content-center gap-2">
                            <span>Vào không gian dự án</span>
                            <i class="bi bi-arrow-right"></i>
                        </a>
                    </div>
                </div>
            </div>
        </c:forEach>
        <c:if test="${empty myProjects}">
            <div class="col-12 text-center py-4">
                <div class="text-muted fs-2 mb-2"><i class="bi bi-inbox"></i></div>
                <h6 class="text-dark fw-bold">Chưa tham gia dự án nào</h6>
                <p class="text-muted fs-8">Hãy bấm nút "Tạo dự án mới" hoặc "Nhập Mã Xin Vào" để bắt đầu làm việc nhóm!</p>
            </div>
        </c:if>
    </div>

    <!-- LƯỚI 2: CÁC DỰ ÁN KHÁC (CÓ THỂ XIN VÀO) -->
    <h5 class="fw-bold text-dark mb-3"><i class="bi bi-globe me-2"></i>Dự án khác trong hệ thống</h5>
    <div class="row g-4">
        <c:forEach items="${otherProjects}" var="p">
            <div class="col-12 col-md-6 col-lg-4">
                <div class="card h-100 border-0 bg-light shadow-sm rounded-4 p-4 d-flex flex-column justify-content-between">
                    <div>
                        <div class="d-flex justify-content-between align-items-start mb-3">
                            <span class="badge bg-secondary-subtle text-secondary rounded-pill px-3 py-1 fs-8">
                                <i class="bi bi-folder2-open me-1"></i> ID: #${p.id}
                            </span>
                            <span class="badge bg-light text-secondary border rounded-pill px-2 py-1 fs-9">
                                <i class="bi bi-people-fill me-1"></i> ${memberCountMap[p.id]}/10
                            </span>
                        </div>
                        <h5 class="fw-bold text-dark mb-2">${p.name}</h5>
                        <p class="text-secondary fs-7 mb-4 line-clamp-2">
                            ${not empty p.description ? p.description : 'Chưa có mô tả cho dự án này.'}
                        </p>
                    </div>
                    <div class="pt-3 border-top text-center">
                        <button type="button" class="btn btn-outline-secondary w-100 rounded-pill py-2 fs-7 fw-semibold"
                                onclick="document.getElementById('projectCodeInput').value='${p.projectCode}'; new bootstrap.Modal(document.getElementById('joinByCodeModal')).show();">
                            <i class="bi bi-box-arrow-in-right me-1"></i> Xin gia nhập
                        </button>
                    </div>
                </div>
            </div>
        </c:forEach>
        <c:if test="${empty otherProjects}">
            <div class="col-12 text-center py-4">
                <span class="text-muted fs-8">Không có dự án nào khác.</span>
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

<jsp:include page="/includes/footer.jsp" />
