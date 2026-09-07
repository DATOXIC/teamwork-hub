<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <jsp:include page="/includes/header.jsp" />
    <jsp:include page="/includes/navbar.jsp" />

    <!-- ============================================================
     SCROLLTELLING HOMEPAGE — TeamWork Hub Landing Page
     ============================================================ -->

    <!-- ==================== BLOCK 1: HERO (Dark, Full Viewport) ==================== -->
    <section class="hero-scrolltelling">
        <!-- Tiêu đề chính -->
        <h1>
            Không gian làm việc nhóm <br>
            <span class="text-gradient">Tập trung & Tinh gọn</span>
        </h1>

        <!-- Mô tả ngắn -->
        <p class="hero-subtitle">
            Một nền tảng duy nhất cho Kanban, tài liệu Wiki và thảo luận nhóm — không cần chuyển đổi giữa hàng chục công
            cụ rời rạc.
        </p>

        <!-- Nút CTA -->
        <div class="d-flex flex-wrap justify-content-center gap-3 mb-2">
            <div class="${empty sessionScope.currentUser ? '' : 'd-none'}">
                <a href="${pageContext.request.contextPath}/auth?action=viewLogin" class="btn btn-hero-cta">
                    <i class="bi bi-rocket-takeoff me-2"></i> Bắt đầu ngay miễn phí
                </a>
            </div>
            <div class="${not empty sessionScope.currentUser ? '' : 'd-none'}">
                <a href="${pageContext.request.contextPath}/project?action=list" class="btn btn-hero-cta">
                    <i class="bi bi-grid me-2"></i> Vào Không Gian Dự Án
                </a>
            </div>
        </div>

        <!-- Ảnh Dashboard Peek (Screenshot tự nhiên, không viền) -->
        <div class="hero-dashboard-peek">
            <img src="${pageContext.request.contextPath}/images/dashboard-hero.png" 
                 alt="Dashboard Teamwork Hub"
                 class="hero-screenshot-natural">
        </div>
    </section>

    <!-- ==================== BLOCK 2: FEATURE — Bảng Kanban ==================== -->
    <section class="feature-block">
        <div class="container text-center">
            <div class="scroll-reveal">
                <div class="feature-icon-wrapper bg-primary-subtle text-primary">
                    <i class="bi bi-kanban-fill"></i>
                </div>
            </div>
            <h2 class="feature-title scroll-reveal" data-delay="1">Bảng Kanban Trực Quan</h2>
            <p class="feature-desc mx-auto scroll-reveal" data-delay="1">
                Không còn task thất lạc trong chat — mọi việc đều có vị trí rõ ràng trên bảng.
                Kéo thả 3 cột trạng thái, bộ lọc tức thì, nắm bắt tiến độ chỉ bằng một cái nhìn.
            </p>
            <div class="scroll-reveal" data-delay="2">
                <div class="feature-screenshot">
                    <i class="bi bi-kanban"></i>
                    <span>Screenshot: Cận cảnh bảng Kanban — 3 cột kéo thả</span>
                    <span class="fs-9 mt-1">(Thay bằng ảnh thật hoặc GIF sau)</span>
                </div>
            </div>
        </div>
    </section>

    <!-- ==================== BLOCK 3: FEATURE — Tài Liệu Wiki ==================== -->
    <section class="feature-block feature-block--alt">
        <div class="container text-center">
            <div class="scroll-reveal">
                <div class="feature-icon-wrapper bg-success-subtle text-success">
                    <i class="bi bi-journal-richtext"></i>
                </div>
            </div>
            <h2 class="feature-title scroll-reveal" data-delay="1">Tài Liệu Wiki Thông Minh</h2>
            <p class="feature-desc mx-auto scroll-reveal" data-delay="1">
                Ngừng tìm kiếm file trong hàng chục tin nhắn — mọi tài liệu đều sống trong cùng dự án.
                Bố cục thoáng đãng, liên kết trực tiếp đến từng thẻ công việc.
            </p>
            <div class="scroll-reveal" data-delay="2">
                <div class="feature-screenshot">
                    <i class="bi bi-journal-text"></i>
                    <span>Screenshot: Giao diện viết tài liệu Wiki — sidebar + editor</span>
                    <span class="fs-9 mt-1">(Thay bằng ảnh thật sau)</span>
                </div>
            </div>
        </div>
    </section>

    <!-- ==================== BLOCK 4: FEATURE — Thảo Luận Nhóm ==================== -->
    <section class="feature-block">
        <div class="container text-center">
            <div class="scroll-reveal">
                <div class="feature-icon-wrapper bg-info-subtle text-info">
                    <i class="bi bi-chat-dots-fill"></i>
                </div>
            </div>
            <h2 class="feature-title scroll-reveal" data-delay="1">Thảo Luận Nhóm Tập Trung</h2>
            <p class="feature-desc mx-auto scroll-reveal" data-delay="1">
                Trao đổi ngay trong dự án, không cần rời app. @nhắc tên đồng đội,
                #liên kết thẳng đến task — mọi cuộc trò chuyện đều có ngữ cảnh rõ ràng.
            </p>
            <div class="scroll-reveal" data-delay="2">
                <div class="feature-screenshot">
                    <i class="bi bi-chat-dots"></i>
                    <span>Screenshot: Khung chat nhóm — @mention + #task liên kết</span>
                    <span class="fs-9 mt-1">(Thay bằng ảnh thật sau)</span>
                </div>
            </div>
        </div>
    </section>

    <!-- ==================== BLOCK 5: FEATURE — Chỉ Số Năng Suất ==================== -->
    <section class="feature-block feature-block--alt">
        <div class="container text-center">
            <div class="scroll-reveal">
                <div class="feature-icon-wrapper bg-warning-subtle text-warning-emphasis">
                    <i class="bi bi-graph-up-arrow"></i>
                </div>
            </div>
            <h2 class="feature-title scroll-reveal" data-delay="1">Chỉ Số Năng Suất Tự Động</h2>
            <p class="feature-desc mx-auto scroll-reveal" data-delay="1">
                Tự động đo lường tỷ lệ hoàn thành và cống hiến cá nhân — không cần báo cáo thủ công.
                Biểu đồ trực quan, minh bạch từng thành viên.
            </p>
            <div class="scroll-reveal" data-delay="2">
                <div class="feature-screenshot">
                    <i class="bi bi-bar-chart-line"></i>
                    <span>Screenshot: Dashboard thống kê — biểu đồ tiến độ & đóng góp</span>
                    <span class="fs-9 mt-1">(Thay bằng ảnh thật sau)</span>
                </div>
            </div>
        </div>
    </section>

    <!-- ==================== BLOCK 6: CTA — Kêu Gọi Hành Động ==================== -->
    <section class="cta-block">
        <div class="container">
            <h2 class="scroll-reveal">Sẵn sàng nâng cấp cách làm việc nhóm?</h2>
            <p class="scroll-reveal" data-delay="1">
                Tập trung vào công việc thật sự quan trọng — để TeamWork Hub lo phần còn lại.
            </p>
            <div class="scroll-reveal" data-delay="2">
                <div class="${empty sessionScope.currentUser ? '' : 'd-none'}">
                    <a href="${pageContext.request.contextPath}/auth?action=viewLogin" class="btn btn-hero-cta">
                        <i class="bi bi-rocket-takeoff me-2"></i> Bắt đầu ngay miễn phí
                    </a>
                </div>
                <div class="${not empty sessionScope.currentUser ? '' : 'd-none'}">
                    <a href="${pageContext.request.contextPath}/project?action=list" class="btn btn-hero-cta">
                        <i class="bi bi-grid me-2"></i> Vào Không Gian Dự Án
                    </a>
                </div>
            </div>
        </div>
    </section>

    <!-- Scroll Reveal: Intersection Observer (CSS thuần, không dependency) -->
    <script>
        document.addEventListener('DOMContentLoaded', function () {
            var observer = new IntersectionObserver(function (entries) {
                entries.forEach(function (entry) {
                    if (entry.isIntersecting) {
                        entry.target.classList.add('is-visible');
                        observer.unobserve(entry.target);
                    }
                });
            }, { threshold: 0.15 });

            document.querySelectorAll('.scroll-reveal').forEach(function (el) {
                observer.observe(el);
            });
        });
    </script>

    <jsp:include page="/includes/footer.jsp" />