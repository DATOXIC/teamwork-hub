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
        <div>
            <!-- Nút kích hoạt Modal Pop-up của Bootstrap -->
            <button type="button" class="btn btn-primary-custom px-4 py-2 rounded-pill fw-semibold shadow-sm d-flex align-items-center gap-2" 
                    data-bs-toggle="modal" data-bs-target="#createProjectModal">
                <i class="bi bi-plus-circle-fill"></i> Tạo dự án mới
            </button>
        </div>
    </div>

    <!-- 3. Thông báo lỗi (nếu người dùng để trống tên dự án khi tạo) -->
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger alert-dismissible fade show fs-7 py-2 mb-4" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2"></i>${errorMessage}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <!-- 4. Project Grid: Lưới hiển thị danh sách Card dự án (Phong cách Basecamp) -->
    <div class="row g-4">
        <%-- VÒNG LẶP JSTL DUYỆT QUA DANH SÁCH DỰ ÁN --%>
        <c:forEach items="${projects}" var="p">
            <div class="col-12 col-md-6 col-lg-4">
                <div class="card h-100 border-0 bg-white shadow-sm rounded-4 p-4 d-flex flex-column justify-content-between transition hover-shadow">
                    
                    <!-- Phần thân trên của Card -->
                    <div>
                        <div class="d-flex justify-content-between align-items-start mb-3">
                            <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-3 py-1 fs-8">
                                <i class="bi bi-folder2-open me-1"></i> ID: #${p.id}
                            </span>
                            <span class="text-muted fs-8">
                                <i class="bi bi-calendar3 me-1"></i>${p.createdAt}
                            </span>
                        </div>

                        <h5 class="fw-bold text-dark mb-2">${p.name}</h5>
                        <p class="text-secondary fs-7 mb-4 line-clamp-2">
                            ${not empty p.description ? p.description : 'Chưa có mô tả cho dự án này.'}
                        </p>
                    </div>

                    <!-- Phần đuôi của Card: Hiển thị Tiến độ % và Nút vào dự án -->
                    <div class="pt-3 border-top">
                        <div class="d-flex justify-content-between align-items-center fs-8 text-muted mb-1">
                            <span>Tiến độ hoàn thành</span>
                            <span class="fw-bold text-dark">${p.progressPercentage}%</span>
                        </div>

                        <!-- Thanh Progress Bar tự co giãn theo con số % -->
                        <div class="progress rounded-pill mb-3" style="height: 8px;">
                            <div class="progress-bar bg-primary" role="progressbar" 
                                 style="width: ${p.progressPercentage}%;" 
                                 aria-valuenow="${p.progressPercentage}" aria-valuemin="0" aria-valuemax="100"></div>
                        </div>

                        <!-- Nút bấm bước vào bảng Kanban của dự án -->
                        <a href="${pageContext.request.contextPath}/task?action=list&projectId=${p.id}" 
                           class="btn btn-outline-primary w-100 rounded-pill py-2 fs-7 fw-semibold d-flex align-items-center justify-content-center gap-2">
                            <span>Vào không gian dự án</span>
                            <i class="bi bi-arrow-right"></i>
                        </a>
                    </div>

                </div>
            </div>
        </c:forEach>

        <!-- Hiển thị khi danh sách dự án trống -->
        <c:if test="${empty projects}">
            <div class="col-12 text-center py-5">
                <div class="text-muted fs-1 mb-2"><i class="bi bi-inbox"></i></div>
                <h5 class="text-dark fw-bold">Chưa có dự án nào</h5>
                <p class="text-muted fs-7">Hãy bấm nút "Tạo dự án mới" ở trên để khởi tạo dự án đầu tiên của nhóm!</p>
            </div>
        </c:if>
    </div>
</div>

<!-- ================= 5. MODAL: CỬA SỔ POP-UP TẠO DỰ ÁN MỚI ================= -->
<div class="modal fade" id="createProjectModal" tabindex="-1" aria-labelledby="createProjectModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow rounded-4 p-2">
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
                        <input type="text" class="form-control" id="proj-name" name="name" 
                               placeholder="Ví dụ: Nâng cấp Website E-Commerce" required autofocus>
                    </div>

                    <div class="mb-3">
                        <label for="proj-desc" class="form-label fw-semibold fs-7 text-dark">
                            Mô tả mục tiêu dự án
                        </label>
                        <textarea class="form-control" id="proj-desc" name="description" rows="3" 
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

<%-- 6. Nhúng Footer --%>
<jsp:include page="/includes/footer.jsp" />
