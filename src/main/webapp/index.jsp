<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<!-- Hero Section (Phong cách Linear / Notion / Basecamp) -->
<section class="py-5 my-auto">
    <div class="container py-lg-4 text-center">
        <div class="row justify-content-center">
            <div class="col-lg-9">
                <!-- Badge giới thiệu -->
                <div class="d-inline-flex align-items-center gap-2 px-3 py-1-5 rounded-pill bg-white border shadow-sm mb-4">
                    <span class="badge bg-primary rounded-pill px-2 py-0-5 fs-9 fw-semibold">Jakarta EE 10</span>
                    <span class="fs-8 text-secondary fw-medium">Đồ án Web Programming chuẩn kiến trúc MVC Model 2</span>
                </div>

                <!-- Tiêu đề chính -->
                <h1 class="display-4 fw-extrabold text-dark tracking-tight mb-3">
                    Không gian làm việc nhóm <br>
                    <span class="text-gradient">Tập trung & Tinh gọn</span>
                </h1>

                <!-- Mô tả ngắn -->
                <p class="lead text-secondary mb-4 px-lg-5 fs-6">
                    Xóa bỏ sự phân mảnh công cụ. Kết hợp bảng Kanban kéo thả, tài liệu ghi chú Wiki và kênh thảo luận trực tiếp trên cùng một nền tảng Java Web hiện đại.
                </p>

                <!-- Nút Call to Action (CTA) -->
                <div class="d-flex flex-wrap justify-content-center gap-3 mb-5">
                    <div class="${empty sessionScope.currentUser ? '' : 'd-none'}">
                        <a href="${pageContext.request.contextPath}/auth?action=viewLogin" 
                           class="btn btn-primary-custom btn-lg px-4 py-2-5 rounded-pill fw-semibold shadow-sm fs-7">
                            <i class="bi bi-rocket-takeoff me-2"></i> Bắt đầu ngay miễn phí
                        </a>
                    </div>
                    <div class="${not empty sessionScope.currentUser ? '' : 'd-none'}">
                        <a href="${pageContext.request.contextPath}/project?action=list" 
                           class="btn btn-primary-custom btn-lg px-4 py-2-5 rounded-pill fw-semibold shadow-sm fs-7">
                            <i class="bi bi-grid me-2"></i> Vào Không Gian Dự Án
                        </a>
                    </div>
                </div>
            </div>
        </div>

        <!-- 4 Trụ cột tính năng (4 Feature Cards) -->
        <div class="row g-4 mt-2 text-start">
            <!-- Card 1: Kanban -->
            <div class="col-12 col-md-6 col-lg-3">
                <div class="feature-card-modern h-100 d-flex flex-column">
                    <div class="p-2-5 rounded-3 d-inline-flex align-items-center justify-content-center bg-primary-subtle text-primary mb-3" style="width: 44px; height: 44px;">
                        <i class="bi bi-kanban-fill fs-5"></i>
                    </div>
                    <h5 class="fw-bold text-dark fs-6 mb-2">Bảng Kanban Trực Quan</h5>
                    <p class="text-secondary fs-8 mb-0 lh-base">Kéo thả 3 cột trạng thái (To Do, In Progress, Done) với HTML5 Drag & Drop và bộ lọc tức thì 0.01s.</p>
                </div>
            </div>

            <!-- Card 2: Docs -->
            <div class="col-12 col-md-6 col-lg-3">
                <div class="feature-card-modern h-100 d-flex flex-column">
                    <div class="p-2-5 rounded-3 d-inline-flex align-items-center justify-content-center bg-success-subtle text-success mb-3" style="width: 44px; height: 44px;">
                        <i class="bi bi-journal-richtext fs-5"></i>
                    </div>
                    <h5 class="fw-bold text-dark fs-6 mb-2">Tài Liệu Wiki Notion</h5>
                    <p class="text-secondary fs-8 mb-0 lh-base">Bố cục 25/75 thoáng đãng, tìm kiếm tức thì, liên kết trực tiếp vào các thẻ công việc tham chiếu.</p>
                </div>
            </div>

            <!-- Card 3: Chat -->
            <div class="col-12 col-md-6 col-lg-3">
                <div class="feature-card-modern h-100 d-flex flex-column">
                    <div class="p-2-5 rounded-3 d-inline-flex align-items-center justify-content-center bg-info-subtle text-info mb-3" style="width: 44px; height: 44px;">
                        <i class="bi bi-chat-dots-fill fs-5"></i>
                    </div>
                    <h5 class="fw-bold text-dark fs-6 mb-2">Thảo Luận Nhóm Slack</h5>
                    <p class="text-secondary fs-8 mb-0 lh-base">Bong bóng tin nhắn phân cấp người nói, tự động biến #doc-1, #task-1 và @nhắc tên thành liên kết bấm được.</p>
                </div>
            </div>

            <!-- Card 4: Analytics -->
            <div class="col-12 col-md-6 col-lg-3">
                <div class="feature-card-modern h-100 d-flex flex-column">
                    <div class="p-2-5 rounded-3 d-inline-flex align-items-center justify-content-center bg-warning-subtle text-warning-emphasis mb-3" style="width: 44px; height: 44px;">
                        <i class="bi bi-graph-up-arrow fs-5"></i>
                    </div>
                    <h5 class="fw-bold text-dark fs-6 mb-2">Chỉ Số Năng Suất Tự Động</h5>
                    <p class="text-secondary fs-8 mb-0 lh-base">Tự động tính toán tỷ lệ % hoàn thành công việc và lưu vết cống hiến trên hồ sơ chuyên môn.</p>
                </div>
            </div>
        </div>
    </div>
</section>

<jsp:include page="/includes/footer.jsp" />