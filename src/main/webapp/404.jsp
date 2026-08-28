<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container py-5 my-auto text-center" style="min-height: 60vh;">
    <div class="row justify-content-center align-items-center h-100">
        <div class="col-12 col-md-8 col-lg-6">
            <div class="card border-0 shadow-sm rounded-4 p-5 bg-white">
                <div class="display-1 fw-extrabold text-primary mb-2">404</div>
                <div class="text-muted fs-1 mb-3">
                    <i class="bi bi-compass"></i>
                </div>
                <h4 class="fw-bold text-dark mb-2">Không Tìm Thấy Trang Yêu Cầu</h4>
                <p class="text-secondary fs-7 mb-4">
                    Đường dẫn bạn đang truy cập không tồn tại hoặc đã được di chuyển sang địa chỉ khác trong hệ thống TeamWork Hub.
                </p>
                <div class="d-flex justify-content-center gap-3">
                    <a href="${pageContext.request.contextPath}/project?action=list" class="btn btn-primary-custom rounded-pill px-4 py-2 fs-7 fw-semibold shadow-sm">
                        <i class="bi bi-folder2-open me-2"></i> Về Danh Sách Dự Án
                    </a>
                    <a href="${pageContext.request.contextPath}/index.jsp" class="btn btn-outline-secondary rounded-pill px-4 py-2 fs-7 fw-semibold">
                        <i class="bi bi-house-door me-2"></i> Trang Chủ
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/includes/footer.jsp" />
