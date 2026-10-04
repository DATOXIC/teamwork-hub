<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- Nạp file CSS chuyên biệt cho trang chủ với các hiệu ứng Aurora & Glassmorphism --%>
<c:set var="extraCss" value="styles/home.css" scope="request" />
<c:set var="pageTitle" value="TeamWork Hub — Nền Tảng Làm Việc Nhóm Thế Hệ Mới" scope="request" />

<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="homepage-shell">

    <!-- ============================================================
         1. AURORA BREATHING GLOW (VẦNG HÀO QUANG CỰC QUANG DEEP TECH)
         ============================================================ -->
    <div class="aurora-bg-glow" aria-hidden="true">
        <div class="aurora-orb aurora-orb--1"></div>
        <div class="aurora-orb aurora-orb--2"></div>
        <div class="aurora-orb aurora-orb--3"></div>
    </div>

    <!-- ============================================================
         2. HERO SECTION (TIÊU ĐỀ, CTA & KHUNG MOCKUP STUDIO 3D)
         ============================================================ -->
    <header class="hero-epic">
        
        <!-- Badge Pill phát sáng công nghệ -->
        <div class="hero-badge-pill reveal-up">
            <span class="badge-dot-live" aria-hidden="true"></span>
            <span>Nền tảng làm việc nhóm &amp; quản trị dự án All-in-One</span>
        </div>

        <!-- Tiêu đề chính -->
        <h1 class="hero-title-main reveal-up" data-delay="1">
            Không gian làm việc nhóm <br>
            <span class="text-gradient-aurora">Tập trung &amp; Tinh gọn</span>
        </h1>

        <!-- Mô tả ngắn gọn -->
        <p class="hero-subtitle-lead reveal-up" data-delay="2">
            Một nền tảng duy nhất kết nối liền mạch giữa Kanban kéo thả, tài liệu Wiki thông minh, 
            thảo luận theo ngữ cảnh và báo cáo năng suất tự động — giải phóng đội ngũ khỏi những công cụ rời rạc.
        </p>

        <!-- Cụm nút CTA kép -->
        <div class="hero-cta-group reveal-up" data-delay="3">
            <c:choose>
                <c:when test="${empty sessionScope.currentUser}">
                    <a href="${pageContext.request.contextPath}/auth?action=viewLogin" class="btn-cta-primary shadow-lg">
                        <i class="bi bi-rocket-takeoff-fill" aria-hidden="true"></i>
                        <span>Bắt đầu ngay miễn phí</span>
                    </a>
                </c:when>
                <c:otherwise>
                    <a href="${pageContext.request.contextPath}/project?action=list" class="btn-cta-primary shadow-lg">
                        <i class="bi bi-grid-fill" aria-hidden="true"></i>
                        <span>Vào Không Gian Dự Án</span>
                    </a>
                </c:otherwise>
            </c:choose>

            <a href="#bento-features" class="btn-cta-secondary">
                <span>Khám phá tính năng</span>
                <i class="bi bi-arrow-down" aria-hidden="true"></i>
            </a>
        </div>

        <!-- ========================================================
             3. KHUNG STUDIO DISPLAY SHOWCASE VỚI TAB TƯƠNG TÁC LIVE
             ======================================================== -->
        <div class="showcase-container reveal-up" data-delay="4">

            <!-- Floating Widget 1: Task hoàn thành (Bên trái) -->
            <aside class="floating-widget floating-widget--left" aria-label="Thông báo task hoàn thành thời gian thực">
                <div class="widget-icon-box widget-icon-box--emerald" aria-hidden="true">
                    <i class="bi bi-check-circle-fill"></i>
                </div>
                <div>
                    <div class="widget-text-title">Công việc hoàn thành</div>
                    <div class="widget-text-desc">#task-12: Thiết kế DB JPA • Vừa xong</div>
                </div>
            </aside>

            <!-- Floating Widget 2: Tin nhắn mới (Bên phải) -->
            <aside class="floating-widget floating-widget--right" aria-label="Tin nhắn thảo luận thời gian thực">
                <div class="widget-icon-box widget-icon-box--indigo" aria-hidden="true">
                    <i class="bi bi-chat-quote-fill"></i>
                </div>
                <div>
                    <div class="widget-text-title">@David Đỗ</div>
                    <div class="widget-text-desc">"Bản Chat UI mới mượt mà quá! 🚀"</div>
                </div>
            </aside>

            <!-- Bộ chuyển đổi Tab tương tác ngay trên trang chủ -->
            <nav class="showcase-tabs-nav" aria-label="Chọn tính năng xem trước">
                <button type="button" class="showcase-tab-btn active" data-target="panel-kanban" id="tab-kanban">
                    <i class="bi bi-kanban-fill text-warning" aria-hidden="true"></i>
                    <span>Kanban</span>
                </button>
                <button type="button" class="showcase-tab-btn" data-target="panel-wiki" id="tab-wiki">
                    <i class="bi bi-journal-richtext text-primary" aria-hidden="true"></i>
                    <span>Tài liệu Wiki</span>
                </button>
                <button type="button" class="showcase-tab-btn" data-target="panel-chat" id="tab-chat">
                    <i class="bi bi-chat-dots-fill text-info" aria-hidden="true"></i>
                    <span>Thảo luận @</span>
                </button>
                <button type="button" class="showcase-tab-btn" data-target="panel-stats" id="tab-stats">
                    <i class="bi bi-graph-up-arrow text-success" aria-hidden="true"></i>
                    <span>Báo cáo tiến độ</span>
                </button>
            </nav>

            <!-- Khung Mac Studio Display -->
            <div class="studio-display-window">
                <!-- Thanh Topbar giả lập Mac OS -->
                <div class="window-topbar">
                    <div class="window-dots" aria-hidden="true">
                        <span class="window-dot window-dot--red"></span>
                        <span class="window-dot window-dot--yellow"></span>
                        <span class="window-dot window-dot--green"></span>
                    </div>
                    <div class="window-url-bar">
                        <i class="bi bi-lock-fill text-success" aria-hidden="true"></i>
                        <span>teamwork-hub.local/project?id=1</span>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <span class="badge rounded-pill bg-success-subtle text-success fs-9 px-2 py-0 border border-success-subtle">
                            Live Demo
                        </span>
                    </div>
                </div>

                <!-- Vùng hiển thị màn hình tương ứng với từng Tab -->
                <div class="window-viewport">
                    <!-- Panel 1: Kanban -->
                    <div class="viewport-panel active" id="panel-kanban" role="tabpanel" aria-labelledby="tab-kanban">
                        <img src="${pageContext.request.contextPath}/images/kanban.png" 
                             alt="Bảng Kanban Trực Quan TeamWork Hub"
                             loading="eager" />
                    </div>

                    <!-- Panel 2: Wiki Docs -->
                    <div class="viewport-panel" id="panel-wiki" role="tabpanel" aria-labelledby="tab-wiki">
                        <img src="${pageContext.request.contextPath}/images/wiki.png" 
                             alt="Tài Liệu Wiki Thông Minh"
                             loading="lazy" />
                    </div>

                    <!-- Panel 3: Chat Thảo luận -->
                    <div class="viewport-panel" id="panel-chat" role="tabpanel" aria-labelledby="tab-chat">
                        <img src="${pageContext.request.contextPath}/images/chat.png" 
                             alt="Phòng Thảo Luận Nhóm Tập Trung"
                             loading="lazy" />
                    </div>

                    <!-- Panel 4: Báo cáo Năng suất -->
                    <div class="viewport-panel" id="panel-stats" role="tabpanel" aria-labelledby="tab-stats">
                        <img src="${pageContext.request.contextPath}/images/activity.png" 
                             alt="Chỉ Số Năng Suất & Hoạt Động Nhóm"
                             loading="lazy" />
                    </div>
                </div>
            </div>

        </div>
    </header>

    <!-- ============================================================
         4. METRICS STRIP (DẢI SỐ LIỆU TỰ HÀO & HIỆU NĂNG)
         ============================================================ -->
    <section class="metrics-strip reveal-up" aria-label="Chỉ số hiệu năng nền tảng">
        <div class="row g-4 justify-content-center">
            <div class="col-6 col-md-3 metric-item">
                <div class="metric-number">100%</div>
                <div class="metric-label">Không gian làm việc tập trung</div>
            </div>
            <div class="col-6 col-md-3 metric-item">
                <div class="metric-number">0s</div>
                <div class="metric-label">Độ trễ chuyển đổi công cụ</div>
            </div>
            <div class="col-6 col-md-3 metric-item">
                <div class="metric-number">4-in-1</div>
                <div class="metric-label">Kanban, Docs, Chat &amp; Báo cáo</div>
            </div>
            <div class="col-6 col-md-3 metric-item">
                <div class="metric-number">Realtime</div>
                <div class="metric-label">Phản hồi &amp; cập nhật tức thì</div>
            </div>
        </div>
    </section>

    <!-- ============================================================
         5. BENTO GRID FEATURES SHOWCASE (BỐ CỤC BENTO HIỆN ĐẠI)
         ============================================================ -->
    <section class="section-bento" id="bento-features">
        
        <div class="section-header-centered reveal-up">
            <div class="section-badge">
                <i class="bi bi-stars" aria-hidden="true"></i>
                <span>Tính Năng Vượt Trội</span>
            </div>
            <h2 class="section-title">Mọi công cụ bạn cần. Trong một giao diện duy nhất.</h2>
            <p class="section-subtitle">
                Được thiết kế tinh xảo để loại bỏ hoàn toàn sự phân mảnh công việc. 
                Mọi tác vụ, ghi chú và cuộc trò chuyện đều liên kết mật thiết với nhau.
            </p>
        </div>

        <div class="bento-grid">
            
            <!-- Bento Card 1: Kanban Board (Thẻ lớn - 8 Cột) -->
            <article class="bento-card bento-card--large reveal-up" data-delay="1">
                <div class="bento-card-header">
                    <div class="bento-icon bento-icon--kanban" aria-hidden="true">
                        <i class="bi bi-kanban-fill"></i>
                    </div>
                    <h3 class="bento-card-title">Bảng Kanban Kéo Thả Siêu Tốc</h3>
                    <p class="bento-card-desc">
                        Theo dõi luồng công việc rõ ràng qua các cột trạng thái. 
                        Phân cấp độ ưu tiên màu sắc, gán thành viên phụ trách và cảnh báo hạn chót tức thì.
                    </p>
                </div>
                
                <!-- Preview tương tác mô phỏng Kanban -->
                <div class="bento-preview-box">
                    <div class="mini-kanban-cols">
                        <div class="mini-kanban-col">
                            <div class="mini-kanban-col-title">
                                <span>Cần làm</span>
                                <span class="badge rounded-pill bg-secondary-subtle text-secondary fs-9">2</span>
                            </div>
                            <div class="mini-kanban-card">
                                <div>Khảo sát yêu cầu UI/UX</div>
                                <span class="mini-tag-badge mini-tag--medium">Trung bình</span>
                            </div>
                            <div class="mini-kanban-card">
                                <div>Viết Unit Test DAO</div>
                                <span class="mini-tag-badge mini-tag--high">Khẩn cấp</span>
                            </div>
                        </div>

                        <div class="mini-kanban-col">
                            <div class="mini-kanban-col-title">
                                <span>Đang làm</span>
                                <span class="badge rounded-pill bg-warning-subtle text-warning fs-9">1</span>
                            </div>
                            <div class="mini-kanban-card border-warning-subtle shadow-sm">
                                <div>Tích hợp JPA 3.1 &amp; Hibernate</div>
                                <span class="mini-tag-badge mini-tag--high">Ưu tiên cao</span>
                            </div>
                        </div>

                        <div class="mini-kanban-col">
                            <div class="mini-kanban-col-title">
                                <span>Hoàn thành</span>
                                <span class="badge rounded-pill bg-success-subtle text-success fs-9">3</span>
                            </div>
                            <div class="mini-kanban-card opacity-75">
                                <div class="text-decoration-line-through text-muted">Thiết kế CSDL PostgreSQL</div>
                                <span class="mini-tag-badge mini-tag--done">Xong</span>
                            </div>
                        </div>
                    </div>
                </div>
            </article>

            <!-- Bento Card 2: Wiki Knowledge (Thẻ vừa - 4 Cột) -->
            <article class="bento-card bento-card--medium reveal-up" data-delay="2">
                <div class="bento-card-header">
                    <div class="bento-icon bento-icon--wiki" aria-hidden="true">
                        <i class="bi bi-journal-richtext"></i>
                    </div>
                    <h3 class="bento-card-title">Tài Liệu Wiki Nhóm</h3>
                    <p class="bento-card-desc">
                        Không gian tài liệu chuẩn Markdown sống ngay trong dự án. 
                        Đánh số định danh <code class="text-info fs-8">#doc-1</code> để trích dẫn tức thì sang kênh Chat.
                    </p>
                </div>

                <div class="bento-preview-box">
                    <div class="mini-wiki-tree">
                        <div class="mini-wiki-item">
                            <i class="bi bi-file-earmark-text text-primary" aria-hidden="true"></i>
                            <span class="text-truncate">#doc-1: Kiến Trúc Hệ Thống MVC</span>
                        </div>
                        <div class="mini-wiki-item">
                            <i class="bi bi-file-earmark-code text-info" aria-hidden="true"></i>
                            <span class="text-truncate">#doc-2: Quy Chuẩn Coding Guideline</span>
                        </div>
                        <div class="mini-wiki-item">
                            <i class="bi bi-shield-check text-success" aria-hidden="true"></i>
                            <span class="text-truncate">#doc-3: Tài Liệu Bảo Mật &amp; Auth</span>
                        </div>
                    </div>
                </div>
            </article>

            <!-- Bento Card 3: Thảo luận Ngữ Cảnh (Thẻ vừa - 4 Cột) -->
            <article class="bento-card bento-card--medium reveal-up" data-delay="3">
                <div class="bento-card-header">
                    <div class="bento-icon bento-icon--chat" aria-hidden="true">
                        <i class="bi bi-chat-dots-fill"></i>
                    </div>
                    <h3 class="bento-card-title">Thảo Luận Ngữ Cảnh</h3>
                    <p class="bento-card-desc">
                        Bong bóng chat phong cách Glassmorphism. 
                        @nhắc tên đồng đội và liên kết thẳng đến thẻ công việc bằng 1 click chuột.
                    </p>
                </div>

                <div class="bento-preview-box">
                    <div class="mini-chat-thread">
                        <div class="mini-chat-bubble mini-chat-bubble--other">
                            <span class="mini-mention-pill">@Khang</span> xem giúp mình tài liệu <span class="mini-mention-pill">#doc-1</span> nhé!
                        </div>
                        <div class="mini-chat-bubble mini-chat-bubble--me">
                            Mình vừa duyệt xong, đã gắn vào thẻ <span class="mini-mention-pill">#task-4</span> rồi! ✨
                        </div>
                    </div>
                </div>
            </article>

            <!-- Bento Card 4: Báo cáo Năng suất (Thẻ lớn - 8 Cột) -->
            <article class="bento-card bento-card--large reveal-up" data-delay="4">
                <div class="bento-card-header">
                    <div class="bento-icon bento-icon--stats" aria-hidden="true">
                        <i class="bi bi-graph-up-arrow"></i>
                    </div>
                    <h3 class="bento-card-title">Chỉ Số Năng Suất Tự Động</h3>
                    <p class="bento-card-desc">
                        Tự động tính toán tiến độ hoàn thành, đo lường mức độ đóng góp cá nhân 
                        và trực quan hóa bằng biểu đồ minh bạch theo thời gian thực.
                    </p>
                </div>

                <div class="bento-preview-box">
                    <div class="mini-progress-row">
                        <div class="mini-progress-header">
                            <span>Tiến độ Sprint Hiện Tại</span>
                            <span class="text-success fw-bold">92%</span>
                        </div>
                        <div class="mini-progress-track">
                            <div class="mini-progress-fill bg-success" style="width: 92%;"></div>
                        </div>
                    </div>

                    <div class="mini-progress-row">
                        <div class="mini-progress-header">
                            <span>Hoàn thành Công việc (Tasks)</span>
                            <span class="text-info fw-bold">18 / 20 hoàn tất</span>
                        </div>
                        <div class="mini-progress-track">
                            <div class="mini-progress-fill bg-info" style="width: 85%;"></div>
                        </div>
                    </div>

                    <div class="mini-progress-row mb-0">
                        <div class="mini-progress-header">
                            <span>Tương tác Thảo luận &amp; Đóng góp</span>
                            <span class="text-warning fw-bold">+34% so với tuần trước</span>
                        </div>
                        <div class="mini-progress-track">
                            <div class="mini-progress-fill bg-warning" style="width: 78%;"></div>
                        </div>
                    </div>
                </div>
            </article>

        </div>
    </section>

    <!-- ============================================================
         6. WORKFLOW SECTION (QUY TRÌNH 4 BƯỚC LIỀN MẠCH)
         ============================================================ -->
    <section class="section-workflow">
        <div class="section-header-centered reveal-up">
            <div class="section-badge">
                <i class="bi bi-arrow-repeat" aria-hidden="true"></i>
                <span>Quy Trình Chuẩn Mực</span>
            </div>
            <h2 class="section-title">Từ Ý Tưởng Đến Đích Đến Thành Công</h2>
            <p class="section-subtitle">
                Đơn giản hóa hành trình cộng tác dự án qua 4 bước khép kín và nhất quán.
            </p>
        </div>

        <div class="workflow-steps-grid">
            <div class="workflow-step-card reveal-up" data-delay="1">
                <div class="step-number-badge">1</div>
                <h4 class="step-card-title">Khởi Tạo &amp; Lập Kế Hoạch</h4>
                <p class="step-card-desc">Ghi chép mục tiêu, đặc tả kỹ thuật và tài liệu kiến trúc dự án trên Wiki.</p>
            </div>

            <div class="workflow-step-card reveal-up" data-delay="2">
                <div class="step-number-badge">2</div>
                <h4 class="step-card-title">Phân Rã Công Việc</h4>
                <p class="step-card-desc">Tạo thẻ Kanban, gán quyền phụ trách, thiết lập deadline và độ ưu tiên rõ ràng.</p>
            </div>

            <div class="workflow-step-card reveal-up" data-delay="3">
                <div class="step-number-badge">3</div>
                <h4 class="step-card-title">Trao Đổi Ngữ Cảnh</h4>
                <p class="step-card-desc">Thảo luận trực tiếp, nhắc tên đồng nghiệp và liên kết mã thẻ công việc tức thì.</p>
            </div>

            <div class="workflow-step-card reveal-up" data-delay="4">
                <div class="step-number-badge">4</div>
                <h4 class="step-card-title">Đo Lường &amp; Báo Cáo</h4>
                <p class="step-card-desc">Theo dõi tiến độ tự động, minh bạch đánh giá đóng góp của từng thành viên.</p>
            </div>
        </div>
    </section>

    <!-- ============================================================
         7. EPIC CALL TO ACTION (CTA BLOCK)
         ============================================================ -->
    <section class="section-cta-epic">
        <div class="cta-epic-card reveal-up">
            <h2 class="cta-title">Sẵn sàng nâng tầm cách làm việc của đội ngũ?</h2>
            <p class="cta-subtitle">
                Trải nghiệm không gian làm việc nhóm tinh gọn, tập trung và mạnh mẽ. 
                Hoàn toàn miễn phí cho dự án của bạn.
            </p>
            <div>
                <c:choose>
                    <c:when test="${empty sessionScope.currentUser}">
                        <a href="${pageContext.request.contextPath}/auth?action=viewLogin" class="btn-cta-primary shadow-lg">
                            <i class="bi bi-rocket-takeoff-fill me-2" aria-hidden="true"></i> Bắt đầu ngay miễn phí
                        </a>
                    </c:when>
                    <c:otherwise>
                        <a href="${pageContext.request.contextPath}/project?action=list" class="btn-cta-primary shadow-lg">
                            <i class="bi bi-grid-fill me-2" aria-hidden="true"></i> Vào Không Gian Dự Án
                        </a>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </section>

</div>

<!-- ============================================================
     8. JAVASCRIPT: TƯƠNG TÁC TAB SHOWCASE, SPOTLIGHT & SCROLL REVEAL
     ============================================================ -->
<script>
    document.addEventListener('DOMContentLoaded', function () {
        
        // 1. Tương tác chuyển đổi Tab Showcase trên Hero Studio Window
        var tabBtns = document.querySelectorAll('.showcase-tab-btn');
        var viewPanels = document.querySelectorAll('.viewport-panel');

        tabBtns.forEach(function (btn) {
            btn.addEventListener('click', function () {
                var targetId = this.getAttribute('data-target');

                // Bật active cho button
                tabBtns.forEach(function (b) { b.classList.remove('active'); });
                this.classList.add('active');

                // Bật active cho màn hình hiển thị tương ứng
                viewPanels.forEach(function (panel) {
                    if (panel.id === targetId) {
                        panel.classList.add('active');
                    } else {
                        panel.classList.remove('active');
                    }
                });
            });
        });

        // 2. Hiệu ứng viền sáng phát quang theo con trỏ chuột (Mouse-Tracking Spotlight)
        var bentoCards = document.querySelectorAll('.bento-card');
        bentoCards.forEach(function (card) {
            card.addEventListener('mousemove', function (e) {
                var rect = card.getBoundingClientRect();
                var x = e.clientX - rect.left;
                var y = e.clientY - rect.top;
                card.style.setProperty('--mouse-x', x + 'px');
                card.style.setProperty('--mouse-y', y + 'px');
            });
        });

        // 3. Hiệu ứng Scroll Reveal (Trồi lên so le khi cuộn chuột)
        var revealElements = document.querySelectorAll('.reveal-up');
        if ('IntersectionObserver' in window) {
            var revealObserver = new IntersectionObserver(function (entries) {
                entries.forEach(function (entry) {
                    if (entry.isIntersecting) {
                        entry.target.classList.add('is-visible');
                        revealObserver.unobserve(entry.target);
                    }
                });
            }, {
                threshold: 0.1,
                rootMargin: '0px 0px -40px 0px'
            });

            revealElements.forEach(function (el) {
                revealObserver.observe(el);
            });
        } else {
            // Fallback nếu trình duyệt không hỗ trợ IntersectionObserver
            revealElements.forEach(function (el) {
                el.classList.add('is-visible');
            });
        }

        // 4. Tự động chuyển tab nhẹ sau 6 giây nếu người dùng không tương tác (Carousel Preview)
        var autoTimer = null;
        var currentTabIndex = 0;
        function startAutoTabCycle() {
            autoTimer = setInterval(function () {
                currentTabIndex = (currentTabIndex + 1) % tabBtns.length;
                tabBtns[currentTabIndex].click();
            }, 6000);
        }
        function stopAutoTabCycle() {
            if (autoTimer) clearInterval(autoTimer);
        }

        startAutoTabCycle();
        var showcaseContainer = document.querySelector('.showcase-container');
        if (showcaseContainer) {
            showcaseContainer.addEventListener('mouseenter', stopAutoTabCycle);
            showcaseContainer.addEventListener('mouseleave', startAutoTabCycle);
        }
    });
</script>

<jsp:include page="/includes/footer.jsp" />