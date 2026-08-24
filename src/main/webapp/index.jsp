<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<!-- Hero Section (Phong cách Notion / Basecamp) -->
<section class="py-5 my-auto">
    <div class="container py-lg-4 text-center">
        <div class="row justify-content-center">
            <div class="col-lg-9">
                <!-- Badge giới thiệu -->
                <div class="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill bg-white border shadow-sm mb-4">
                    <span class="badge bg-primary rounded-pill">Mới</span>
                    <span class="fs-7 text-muted fw-medium">Đồ án Web Programming chuẩn kiến trúc MVC Model 2</span>
                </div>

                <!-- Tiêu đề chính -->
                <h1 class="display-4 fw-extrabold text-dark tracking-tight mb-3">
                    Không gian làm việc nhóm <br>
                    <span class="text-primary">Tập trung & Tinh gọn</span>
                </h1>

                <!-- Mô tả ngắn -->
                <p class="lead text-secondary mb-5 px-lg-5">
                    Xóa bỏ sự phân mảnh công cụ. Kết hợp bảng Kanban kéo thả, tài liệu ghi chú nhóm và kênh thảo luận trực tiếp trên cùng một nền tảng Java Web hiện đại.
                </p>

                <!-- Nút Call to Action (CTA) -->
                <div class="d-flex flex-wrap justify-content-center gap-3 mb-5">
                    <div class="${empty sessionScope.currentUser ? '' : 'd-none'}">
                        <a href="${pageContext.request.contextPath}/auth?action=viewLogin" 
                           class="btn btn-primary-custom btn-lg px-4 py-3 rounded-pill fw-semibold shadow">
                            <i class="bi bi-rocket-takeoff me-2"></i> Bắt đầu ngay miễn phí
                        </a>
                    </div>
                    <div class="${not empty sessionScope.currentUser ? '' : 'd-none'}">
                        <a href="${pageContext.request.contextPath}/project?action=list" 
                           class="btn btn-primary-custom btn-lg px-4 py-3 rounded-pill fw-semibold shadow">
                            <i class="bi bi-grid me-2"></i> Vào Không Gian Dự Án
                        </a>
                    </div>
                </div>
            </div>
        </div>

        <!-- 4 Trụ cột tính năng (4 Feature Cards) -->
        <div class="row g-4 mt-4 text-start">
            <!-- Card 1: Kanban -->
            <div class="col-12 col-md-6 col-lg-3">
                <div class="card h-100 border-0 bg-white shadow-sm p-4 rounded-4 hover-shadow transition">
                    <div class="brand-icon mb-3 d-flex align-items-center justify-content-center bg-primary text-white" style="width: 44px; height: 44px;">
                        <i class="bi bi-kanban fs-5"></i>
                    </div>
                    <h5 class="fw-bold text-dark">Bảng Kanban Trello</h5>
                    <p class="text-muted fs-7 mb-0">Kéo thả 3 cột trạng thái (To Do, In Progress, Done) với HTML5 Drag & Drop mượt mà.</p>
                </div>
            </div>

            <!-- Card 2: Docs -->
            <div class="col-12 col-md-6 col-lg-3">
                <div class="card h-100 border-0 bg-white shadow-sm p-4 rounded-4 hover-shadow transition">
                    <div class="brand-icon mb-3 d-flex align-items-center justify-content-center bg-success text-white" style="width: 44px; height: 44px;">
                        <i class="bi bi-journal-text fs-5"></i>
                    </div>
                    <h5 class="fw-bold text-dark">Tài liệu Wiki Notion</h5>
                    <p class="text-muted fs-7 mb-0">Soạn thảo văn bản ghi chú cuộc họp, tự động lưu nháp phía trình duyệt chống mất bài.</p>
                </div>
            </div>

            <!-- Card 3: Chat -->
            <div class="col-12 col-md-6 col-lg-3">
                <div class="card h-100 border-0 bg-white shadow-sm p-4 rounded-4 hover-shadow transition">
                    <div class="brand-icon mb-3 d-flex align-items-center justify-content-center bg-info text-white" style="width: 44px; height: 44px;">
                        <i class="bi bi-chat-dots fs-5"></i>
                    </div>
                    <h5 class="fw-bold text-dark">Thảo luận Basecamp</h5>
                    <p class="text-muted fs-7 mb-0">Trao đổi tin nhắn tức thì theo từng dự án với thông tin người gửi và mốc thời gian.</p>
                </div>
            </div>

            <!-- Card 4: Analytics -->
            <div class="col-12 col-md-6 col-lg-3">
                <div class="card h-100 border-0 bg-white shadow-sm p-4 rounded-4 hover-shadow transition">
                    <div class="brand-icon mb-3 d-flex align-items-center justify-content-center bg-warning text-dark" style="width: 44px; height: 44px;">
                        <i class="bi bi-pie-chart fs-5"></i>
                    </div>
                    <h5 class="fw-bold text-dark">Thống kê Tiến độ</h5>
                    <p class="text-muted fs-7 mb-0">Tự động tính toán tỷ lệ % hoàn thành công việc trên từng card dự án trực quan.</p>
                </div>
            </div>
        </div>
    </div>
</section>

<jsp:include page="/includes/footer.jsp" />