<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<%-- =========================================================================
     [ENTERPRISE FEATURE: PROJECT HEALTH, BURNDOWN & WORKLOAD MISSION CONTROL]
     VI: Trung tâm phân tích sức khỏe, dự báo cán đích, cân bằng tải & điểm nghẽn
     EN: Enterprise Project Health, Burndown Forecast, Capacity Heatmap & Blocker Radar
     - Controller: com.teamwork.controllers.ProjectAnalyticsServlet (/analytics, /project-analytics)
     - Calculators: BurndownCalculator, QualityVelocityCalculator, WorkloadAnalytics, 
                    DeliveryAccountability, StaleBlockerDetector
     ========================================================================= --%>

<c:set var="pageTitle" value="Phân Tích Động Lực & Sức Khỏe — ${fn:escapeXml(project.name)}" scope="request" />

<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container-fluid px-lg-5 py-4">
    <!-- 1. CỤM ĐIỀU HƯỚNG DỰ ÁN (8 Tab Capsule Navigation) -->
    <c:set var="activeSubNav" value="analytics" scope="request" />
    <c:set var="currentNav" value="analytics" scope="request" />
    <jsp:include page="/includes/project_subnav.jsp" />

    <!-- Action Bar & Tiêu đề Phân hệ -->
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4 no-print">
        <div class="d-flex align-items-center gap-2 flex-wrap">
            <span class="badge rounded-pill bg-primary-subtle text-primary border border-primary-subtle px-3 py-1-5 fs-8 fw-semibold">
                <i class="bi bi-graph-up-arrow me-1"></i> Trung Tâm Phân Tích Doanh Nghiệp
            </span>
            <span class="text-muted fs-8">
                Khảo sát chu kỳ Burn-down, tỷ lệ đạt lần đầu (FTPR), cân bằng tải tuần và phát hiện điểm nghẽn
            </span>
        </div>
        <div class="d-flex align-items-center gap-2">
            <!-- Nút Làm Mới Dữ Liệu AJAX -->
            <button type="button" id="btnRefreshAnalytics" onclick="refreshAnalyticsData()" 
                    class="btn btn-sm btn-outline-secondary rounded-pill px-3 py-1-5 fw-medium fs-8 d-flex align-items-center gap-1.5 shadow-none"
                    title="Cập nhật lại số liệu phân tích thời gian thực">
                <i class="bi bi-arrow-clockwise" id="refreshSpinnerIcon"></i>
                <span class="d-none d-sm-inline">Làm mới dữ liệu</span>
            </button>
            <button type="button" onclick="window.print()"
                    class="btn btn-sm btn-outline-secondary rounded-pill px-3 py-1-5 fw-medium fs-8 d-flex align-items-center gap-1.5 shadow-none"
                    title="In báo cáo phân tích hoặc lưu PDF">
                <i class="bi bi-printer"></i>
                <span class="d-none d-sm-inline">In / PDF</span>
            </button>
            <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}"
               class="btn btn-sm btn-light border rounded-pill px-3 py-1-5 fw-medium fs-8 d-flex align-items-center gap-1.5 shadow-none">
                <i class="bi bi-kanban"></i> Sang Bảng việc
            </a>
            <a href="${pageContext.request.contextPath}/project?action=report&projectId=${project.id}"
               class="btn btn-sm btn-primary-custom text-white rounded-pill px-3 py-1-5 fw-medium fs-8 d-flex align-items-center gap-1.5 shadow-sm">
                <i class="bi bi-file-earmark-bar-graph"></i> Báo cáo tổng thể
            </a>
        </div>
    </div>

    <!-- Thông báo Flash Toast (nếu có) -->
    <jsp:include page="/includes/toast.jsp" />

    <!-- =========================================================================
         2. UNIFIED EXECUTIVE MISSION CONTROL HERO BAR (DỰ BÁO CÁN ĐÍCH & SỨC KHỎE)
         ========================================================================= -->
    <div class="analytics-hero-card p-4 mb-4">
        <div class="row g-4 align-items-center">
            <!-- Cột Trái: Trạng thái Dự báo Cán Đích & Điểm Sức Khỏe -->
            <div class="col-12 col-lg-5 border-lg-end pe-lg-4">
                <div class="d-flex align-items-center justify-content-between mb-2">
                    <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted d-flex align-items-center gap-1.5">
                        <i class="bi bi-compass text-primary"></i> La Bàn Dự Báo Cán Đích
                    </span>
                    <c:choose>
                        <c:when test="${qualityMetrics.healthStatus == 'HEALTHY'}">
                            <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2.5 py-1 fs-9 d-flex align-items-center gap-1.5">
                                <span class="spinner-grow spinner-grow-sm text-success" style="width: 6px; height: 6px;" role="status"></span> Lành mạnh
                            </span>
                        </c:when>
                        <c:when test="${qualityMetrics.healthStatus == 'AT_RISK'}">
                            <span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill px-2.5 py-1 fs-9 d-flex align-items-center gap-1.5">
                                <span class="spinner-grow spinner-grow-sm text-warning" style="width: 6px; height: 6px;" role="status"></span> Cần chú ý
                            </span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-2.5 py-1 fs-9 d-flex align-items-center gap-1.5">
                                <span class="spinner-grow spinner-grow-sm text-danger" style="width: 6px; height: 6px;" role="status"></span> Báo động
                            </span>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="d-flex align-items-baseline gap-2 mb-1">
                    <span class="fs-2 fw-bold tracking-tight text-body">
                        <c:choose>
                            <c:when test="${not empty burndownData.forecastedCompletionDate}">
                                ${burndownData.forecastedCompletionDate}
                            </c:when>
                            <c:otherwise>
                                ${burndownData.projectEndDate}
                            </c:otherwise>
                        </c:choose>
                    </span>
                    <c:choose>
                        <c:when test="${burndownData.forecastVarianceDays < 0}">
                            <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2.5 py-1 fs-8 fw-semibold">
                                <i class="bi bi-lightning-charge-fill me-1"></i> Sớm ${-burndownData.forecastVarianceDays} ngày
                            </span>
                        </c:when>
                        <c:when test="${burndownData.forecastVarianceDays == 0}">
                            <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2.5 py-1 fs-8 fw-semibold">
                                <i class="bi bi-check-circle-fill me-1"></i> Đúng hạn
                            </span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-2.5 py-1 fs-8 fw-semibold">
                                <i class="bi bi-exclamation-triangle-fill me-1"></i> Trễ ${burndownData.forecastVarianceDays} ngày
                            </span>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="fs-8 text-muted mb-3">
                    <span>Hạn chót cam kết: <strong class="text-body">${burndownData.projectEndDate}</strong></span>
                    <span class="mx-1">&bull;</span>
                    <span>Bắt đầu: ${burndownData.projectStartDate}</span>
                </div>

                <!-- Thanh đo Sức Khỏe Dự Án -->
                <div class="p-2-5 rounded-3 bg-body border">
                    <div class="d-flex align-items-center justify-content-between mb-1.5 fs-8">
                        <span class="text-muted fw-medium">Chỉ số sức khỏe tổng thể</span>
                        <span class="fw-bold text-body">${qualityMetrics.healthScore} / 100</span>
                    </div>
                    <div class="progress mb-1.5" style="height: 6px;">
                        <c:set var="barColor" value="${qualityMetrics.healthScore >= 75 ? 'bg-success' : (qualityMetrics.healthScore >= 50 ? 'bg-warning' : 'bg-danger')}" />
                        <div class="progress-bar ${barColor} rounded-pill" role="progressbar" 
                             style="width: ${qualityMetrics.healthScore}%" aria-valuenow="${qualityMetrics.healthScore}" aria-valuemin="0" aria-valuemax="100"></div>
                    </div>
                    <span class="fs-9 text-muted text-truncate-2 d-block" title="${fn:escapeXml(qualityMetrics.healthDescription)}">
                        ${qualityMetrics.healthDescription}
                    </span>
                </div>
            </div>

            <!-- Cột Phải: Bento Grid 4 Trụ Cột Động Lực & Khối Lượng -->
            <div class="col-12 col-lg-7 ps-lg-3">
                <div class="row g-2">
                    <!-- Ô 1: Vận tốc đội ngũ -->
                    <div class="col-6 col-sm-6">
                        <div class="analytics-metric-box h-100">
                            <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted d-block mb-1">
                                <i class="bi bi-speedometer2 text-primary me-1"></i> Vận tốc thực tế
                            </span>
                            <div class="d-flex align-items-baseline gap-1.5">
                                <span class="fs-4 fw-bold text-primary">${burndownData.teamVelocityHoursPerDay}</span>
                                <span class="fs-9 text-muted">giờ / ngày</span>
                            </div>
                            <span class="fs-9 text-muted d-block mt-0.5">Tốc độ tiêu thụ giờ thực tế</span>
                        </div>
                    </div>

                    <!-- Ô 2: Tiến độ theo Giờ -->
                    <div class="col-6 col-sm-6">
                        <div class="analytics-metric-box h-100">
                            <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted d-block mb-1">
                                <i class="bi bi-clock-history text-success me-1"></i> Tiến độ theo giờ
                            </span>
                            <div class="d-flex align-items-baseline gap-1.5">
                                <span class="fs-4 fw-bold text-success">${burndownData.hourCompletionRate}%</span>
                                <span class="fs-9 text-muted">trọng số</span>
                            </div>
                            <span class="fs-9 text-muted d-block mt-0.5">Đã làm: ${burndownData.completedHours}h / ${burndownData.totalHours}h</span>
                        </div>
                    </div>

                    <!-- Ô 3: Phát sinh Scope Creep -->
                    <div class="col-6 col-sm-6">
                        <div class="analytics-metric-box h-100">
                            <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted d-block mb-1">
                                <i class="bi bi-graph-up text-warning me-1"></i> Scope Creep (Phát sinh)
                            </span>
                            <div class="d-flex align-items-baseline gap-1.5">
                                <span class="fs-4 fw-bold ${burndownData.scopeCreepHours > 0 ? 'text-danger' : 'text-body'}">+${burndownData.scopeCreepHours}h</span>
                                <span class="fs-9 text-muted">${burndownData.scopeCreepRate}% quy mô</span>
                            </div>
                            <span class="fs-9 text-muted d-block mt-0.5">Giờ việc bổ sung ngoài ban đầu</span>
                        </div>
                    </div>

                    <!-- Ô 4: Cột mốc then chốt -->
                    <div class="col-6 col-sm-6">
                        <div class="analytics-metric-box h-100">
                            <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted d-block mb-1">
                                <i class="bi bi-flag-fill text-info me-1"></i> Cột mốc (Milestones)
                            </span>
                            <div class="d-flex align-items-baseline gap-1.5">
                                <span class="fs-4 fw-bold text-body">${burndownData.completedMilestones}</span>
                                <span class="fs-7 text-muted fw-semibold">/ ${burndownData.milestoneCount}</span>
                            </div>
                            <span class="fs-9 text-muted d-block mt-0.5">Mục tiêu chiến lược đã chạm mốc</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- =========================================================================
         2.1. 4 THẺ PULSE KPI NHẸ NHÀNG (ĐỒNG BỘ DOM VỚI JAVASCRIPT REFRESH)
         ========================================================================= -->
    <div class="row g-3 mb-4">
        <!-- KPI 1: Điểm Sức Khỏe Dự Án -->
        <div class="col-6 col-xl-3">
            <div class="card h-100 border rounded-4 p-3 bg-body shadow-2xs">
                <div class="d-flex align-items-center justify-content-between mb-1.5">
                    <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted">Sức khỏe dự án</span>
                    <i class="bi bi-heart-pulse text-primary fs-7"></i>
                </div>
                <div class="d-flex align-items-baseline gap-1 mb-1">
                    <span class="fs-2 fw-bold tracking-tight text-body" id="kpiHealthScore">${qualityMetrics.healthScore}</span>
                    <span class="fs-8 text-muted">/ 100 điểm</span>
                </div>
                <span class="fs-8 text-muted text-truncate d-block">
                    ${qualityMetrics.healthStatus == 'HEALTHY' ? 'Lành mạnh & đúng tiến độ' : (qualityMetrics.healthStatus == 'AT_RISK' ? 'Cần theo dõi rủi ro' : 'Báo động chậm trễ')}
                </span>
            </div>
        </div>

        <!-- KPI 2: Tiến Độ Burndown -->
        <div class="col-6 col-xl-3">
            <div class="card h-100 border rounded-4 p-3 bg-body shadow-2xs">
                <div class="d-flex align-items-center justify-content-between mb-1.5">
                    <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted">Tiến độ Burndown</span>
                    <i class="bi bi-check2-circle text-success fs-7"></i>
                </div>
                <div class="d-flex align-items-baseline gap-1 mb-1">
                    <span class="fs-2 fw-bold tracking-tight text-body" id="kpiCompletionRate">${burndownData.completionRate}%</span>
                    <span class="fs-8 text-muted">công việc</span>
                </div>
                <span class="fs-8 text-muted text-truncate d-block">
                    Xong <strong class="text-body">${burndownData.completedTasks}</strong> / Còn lại <strong class="text-body">${burndownData.remainingTasks}</strong> việc
                </span>
            </div>
        </div>

        <!-- KPI 3: Tỷ Lệ Đạt Lần Đầu (First-Time Pass Rate) -->
        <div class="col-6 col-xl-3">
            <div class="card h-100 border rounded-4 p-3 bg-body shadow-2xs">
                <div class="d-flex align-items-center justify-content-between mb-1.5">
                    <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted">Đạt ngay lần đầu (FTPR)</span>
                    <i class="bi bi-shield-check text-info fs-7"></i>
                </div>
                <div class="d-flex align-items-baseline gap-1 mb-1">
                    <span class="fs-2 fw-bold tracking-tight text-body" id="kpiPassRate">${qualityMetrics.firstTimePassRate}%</span>
                    <span class="fs-8 text-muted">Revision 1</span>
                </div>
                <span class="fs-8 text-muted text-truncate d-block">
                    Đạt Rev 1: <strong class="text-success">${qualityMetrics.firstTimePassedCount}</strong> &bull; Sửa: <strong class="${qualityMetrics.revisionRequiredCount > 0 ? 'text-warning' : 'text-body'}">${qualityMetrics.revisionRequiredCount}</strong>
                </span>
            </div>
        </div>

        <!-- KPI 4: Vận Tốc Phản Hồi PM (PM Turnaround Velocity) -->
        <div class="col-6 col-xl-3">
            <div class="card h-100 border rounded-4 p-3 bg-body shadow-2xs">
                <div class="d-flex align-items-center justify-content-between mb-1.5">
                    <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted">Vận tốc duyệt của PM</span>
                    <i class="bi bi-clock-history text-secondary fs-7"></i>
                </div>
                <div class="d-flex align-items-baseline gap-1 mb-1">
                    <c:choose>
                        <c:when test="${qualityMetrics.avgResponseHours > 0}">
                            <span class="fs-2 fw-bold tracking-tight text-body" id="kpiAvgHours">${qualityMetrics.avgResponseHours}</span>
                            <span class="fs-8 text-muted">giờ / lượt</span>
                        </c:when>
                        <c:otherwise>
                            <span class="fs-2 fw-bold tracking-tight text-body" id="kpiAvgHours">--</span>
                            <span class="fs-8 text-muted">chưa có</span>
                        </c:otherwise>
                    </c:choose>
                </div>
                <span class="fs-8 text-muted text-truncate d-block">
                    ⭐ ${qualityMetrics.avgQualityRating} / 5 &bull; Chờ duyệt: <strong class="${qualityMetrics.pendingReviewCount > 0 ? 'text-primary' : 'text-body'}">${qualityMetrics.pendingReviewCount}</strong>
                </span>
            </div>
        </div>
    </div>

    <!-- =========================================================================
         2.2. SEGMENTED NAVIGATION CONTROLLER (CHUYỂN ĐỔI GÓC NHÌN 1 CHẠM)
         ========================================================================= -->
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4 no-print">
        <div class="analytics-segmented-nav" id="analyticsSegmentedNav">
            <button type="button" class="analytics-segmented-btn active" data-view="view-burndown">
                <i class="bi bi-activity"></i> 1. Tiến Độ & Dự Báo Burndown
            </button>
            <button type="button" class="analytics-segmented-btn" data-view="view-workload">
                <i class="bi bi-calendar-week"></i> 2. Năng Lực & Cân Bằng Tải
            </button>
            <button type="button" class="analytics-segmented-btn" data-view="view-accountability">
                <i class="bi bi-award"></i> 3. Trách Nhiệm & Điểm Nghẽn
                <c:if test="${staleBlockerReport.totalBlockerCount > 0}">
                    <span class="badge bg-danger rounded-pill px-1-5 py-0-5" style="font-size: 10px;">${staleBlockerReport.totalBlockerCount}</span>
                </c:if>
            </button>
            <button type="button" class="analytics-segmented-btn" data-view="view-all">
                <i class="bi bi-grid"></i> Xem Toàn Cảnh
            </button>
        </div>
        <div class="text-muted fs-9 d-none d-md-block">
            <i class="bi bi-info-circle me-1"></i> Nhấn từng phân hệ để tập trung theo dõi hoặc chọn "Xem Toàn Cảnh" để in báo cáo
        </div>
    </div>

    <!-- =========================================================================
         PHÂN HỆ 1: TIẾN ĐỘ BURNDOWN & CHẤT LƯỢNG NGHIỆM THU (VIEW-BURNDOWN)
         ========================================================================= -->
    <div id="view-burndown" class="analytics-view-section">
        <div class="row g-4 mb-4">
            <!-- Cột Trái (8 cols): Biểu Đồ Burndown Tương Tác -->
            <div class="col-12 col-xl-8">
                <div class="card h-100 border rounded-4 bg-body shadow-2xs overflow-hidden">
                    <div class="card-header bg-body border-bottom py-3 px-4 d-flex flex-wrap align-items-center justify-content-between gap-2">
                        <div>
                            <h5 class="card-title fs-6 fw-bold mb-0 text-body d-flex align-items-center gap-2">
                                <i class="bi bi-activity text-primary"></i> Biểu Đồ Burndown — Tiến Độ Hoàn Thành Nhiệm Vụ
                            </h5>
                            <span class="fs-8 text-muted">
                                Nét đứt: Kế hoạch lý tưởng &bull; Nét liền: Số việc còn lại thực tế qua từng ngày
                            </span>
                        </div>
                        <span class="badge bg-light text-secondary border rounded-pill px-3 py-1 fs-8">
                            <i class="bi bi-calendar3 me-1"></i> ${burndownData.projectStartDate} &rarr; ${burndownData.projectEndDate}
                        </span>
                    </div>
                    <div class="card-body p-4">
                        <c:choose>
                            <c:when test="${burndownData.totalTasks == 0}">
                                <div class="text-center py-5">
                                    <div class="rounded-circle bg-light d-inline-flex p-3 mb-3 text-muted">
                                        <i class="bi bi-kanban fs-1"></i>
                                    </div>
                                    <h6 class="fw-semibold text-body">Chưa có công việc nào trong dự án</h6>
                                    <p class="text-muted fs-8 mb-3">Tạo công việc đầu tiên trên Bảng Kanban để kích hoạt biểu đồ Burndown tự động.</p>
                                    <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}" class="btn btn-sm btn-primary-custom text-white rounded-pill px-3 py-1-5">
                                        <i class="bi bi-plus-lg me-1"></i> Tạo công việc mới
                                    </a>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="position-relative" style="min-height: 300px; max-height: 380px;">
                                    <canvas id="burndownChart"></canvas>
                                </div>
                                <div class="row g-2 mt-3 pt-3 border-top text-center fs-8 text-muted">
                                    <div class="col-6 col-md-3">
                                        <span class="d-block text-secondary">Tổng công việc</span>
                                        <strong class="fs-7 text-body">${burndownData.totalTasks}</strong>
                                    </div>
                                    <div class="col-6 col-md-3">
                                        <span class="d-block text-secondary">Đã hoàn thành</span>
                                        <strong class="fs-7 text-success">${burndownData.completedTasks}</strong>
                                    </div>
                                    <div class="col-6 col-md-3">
                                        <span class="d-block text-secondary">Còn lại hiện tại</span>
                                        <strong class="fs-7 text-primary">${burndownData.remainingTasks}</strong>
                                    </div>
                                    <div class="col-6 col-md-3">
                                        <span class="d-block text-secondary">Độ lệch tiến độ</span>
                                        <strong class="fs-7 ${burndownData.varianceTasks <= 0 ? 'text-success' : 'text-danger'}">
                                            ${burndownData.varianceTasks <= 0 ? '+' : ''}${-burndownData.varianceTasks} việc
                                        </strong>
                                    </div>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <!-- Cột Phải (4 cols): Phân Bố Chất Lượng & Top Task Sửa Đổi -->
            <div class="col-12 col-xl-4 d-flex flex-column gap-4">
                <!-- Phân Bố Nghiệm Thu Donut Chart -->
                <div class="card border rounded-4 bg-body shadow-2xs overflow-hidden">
                    <div class="card-header bg-body border-bottom py-2.5 px-3.5 d-flex align-items-center justify-content-between">
                        <span class="fw-bold fs-7 text-body d-flex align-items-center gap-1.5">
                            <i class="bi bi-pie-chart text-info"></i> Phân Bố Chất Lượng Nghiệm Thu
                        </span>
                        <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5 fs-9">FTPR</span>
                    </div>
                    <div class="card-body p-3">
                        <div class="position-relative mb-2" style="min-height: 180px; max-height: 200px;">
                            <canvas id="qualityDonutChart"></canvas>
                        </div>
                        <!-- Phân bố đánh giá sao 1-5 -->
                        <div class="border-top pt-2">
                            <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted d-block mb-1">Thang điểm hài lòng của PM</span>
                            <div class="d-flex flex-column gap-1">
                                <c:forEach begin="1" end="5" var="star">
                                    <c:set var="starIdx" value="${6 - star}" />
                                    <c:set var="starCount" value="${qualityMetrics.ratingDistribution[starIdx]}" />
                                    <c:set var="totalReviewed" value="${qualityMetrics.totalReviewedTasks > 0 ? qualityMetrics.totalReviewedTasks : 1}" />
                                    <c:set var="starPct" value="${(starCount * 100) / totalReviewed}" />
                                    <div class="d-flex align-items-center gap-2 fs-9">
                                        <span class="text-muted" style="width: 40px;">${starIdx} ⭐</span>
                                        <div class="progress flex-grow-1" style="height: 5px;">
                                            <div class="progress-bar bg-warning" style="width: ${starPct}%"></div>
                                        </div>
                                        <span class="text-body fw-semibold" style="width: 25px; text-align: right;">${starCount}</span>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Top Công Việc Cần Sửa Đổi Nhiều Nhất -->
                <div class="card border rounded-4 bg-body shadow-2xs overflow-hidden flex-grow-1">
                    <div class="card-header bg-body border-bottom py-2.5 px-3.5 d-flex align-items-center justify-content-between">
                        <span class="fw-bold fs-7 text-body d-flex align-items-center gap-1.5">
                            <i class="bi bi-repeat text-warning"></i> Công Việc Sửa Đổi Nhiều
                        </span>
                        <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5 fs-9">Top 5</span>
                    </div>
                    <div class="card-body p-0">
                        <c:choose>
                            <c:when test="${empty qualityMetrics.mostRevisedTasks}">
                                <div class="text-center py-4">
                                    <div class="rounded-circle bg-success-subtle text-success d-inline-flex p-2.5 mb-2">
                                        <i class="bi bi-shield-check fs-4"></i>
                                    </div>
                                    <h6 class="fw-semibold text-body fs-8 mb-1">Chất lượng xuất sắc!</h6>
                                    <p class="text-muted fs-9 mb-0">Chưa có công việc nào bị yêu cầu nộp lại.</p>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="list-group list-group-flush fs-9">
                                    <c:forEach var="item" items="${qualityMetrics.mostRevisedTasks}" end="4">
                                        <div class="list-group-item px-3 py-2 border-bottom">
                                            <div class="d-flex align-items-center justify-content-between mb-1">
                                                <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}#task-${item.taskId}" 
                                                   class="fw-semibold text-decoration-none text-body text-truncate d-inline-block" style="max-width: 180px;" 
                                                   title="${fn:escapeXml(item.taskTitle)}">
                                                    ${fn:escapeXml(item.taskTitle)}
                                                </a>
                                                <span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill px-1.5 py-0-5">
                                                    ${item.rejectionsCount} lần sửa
                                                </span>
                                            </div>
                                            <div class="d-flex align-items-center justify-content-between text-muted fs-9">
                                                <span><i class="bi bi-person me-1"></i>${fn:escapeXml(item.assigneeName)}</span>
                                                <c:choose>
                                                    <c:when test="${item.finalStatus == 'APPROVED' || item.finalStatus == 'DONE'}">
                                                        <span class="text-success fw-medium">Đã duyệt</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="text-info fw-medium">Chờ sửa/duyệt</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- =========================================================================
         PHÂN HỆ 2: NĂNG LỰC & CÂN BẰNG TẢI (VIEW-WORKLOAD)
         ========================================================================= -->
    <div id="view-workload" class="analytics-view-section">
        <div class="row g-4 mb-4">
            <!-- Cột Trái (7 cols): Ma Trận Phân Bổ Năng Lực Tuần (T2 - T6 Heatmap) -->
            <div class="col-12 col-xl-7">
                <div class="card h-100 border rounded-4 bg-body shadow-2xs overflow-hidden">
                    <div class="card-header bg-body border-bottom py-3 px-4 d-flex flex-wrap align-items-center justify-content-between gap-2">
                        <div>
                            <h5 class="card-title fs-6 fw-bold mb-0 text-body d-flex align-items-center gap-2">
                                <i class="bi bi-calendar-week text-primary"></i> Ma Trận Phân Bổ Năng Lực Tuần (T2 &rarr; T6)
                            </h5>
                            <span class="fs-8 text-muted">
                                Dàn đều giờ công cho 5 ngày làm việc &bull; Định mức chuẩn 40h/tuần (8h/ngày) &bull; Ô đỏ khi &gt;8h/ngày
                            </span>
                        </div>
                        <div class="d-flex align-items-center gap-1.5">
                            <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-0-5 fs-9">Tối ưu (50-100%)</span>
                            <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-2 py-0-5 fs-9">Quá tải (&gt;8h)</span>
                            <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle rounded-pill px-2 py-0-5 fs-9">Rảnh (&lt;50%)</span>
                        </div>
                    </div>
                    <div class="card-body p-0">
                        <c:choose>
                            <c:when test="${empty workloadAnalytics}">
                                <div class="text-center py-5 text-muted fs-8">Chưa có thành viên nào tham gia dự án.</div>
                            </c:when>
                            <c:otherwise>
                                <div class="table-responsive">
                                    <table class="table table-hover align-middle mb-0 fs-8">
                                        <thead class="table-light text-muted fs-9 text-uppercase tracking-wider">
                                            <tr>
                                                <th class="ps-4 py-2-5">Thành viên</th>
                                                <th class="text-center py-2-5">Định mức</th>
                                                <th class="text-center py-2-5">Tổng giờ</th>
                                                <th class="text-center py-2-5">T2</th>
                                                <th class="text-center py-2-5">T3</th>
                                                <th class="text-center py-2-5">T4</th>
                                                <th class="text-center py-2-5">T5</th>
                                                <th class="text-center py-2-5">T6</th>
                                                <th class="pe-4 py-2-5 text-end">Trạng thái</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="member" items="${workloadAnalytics}">
                                                <tr>
                                                    <td class="ps-4">
                                                        <div class="d-flex align-items-center gap-2">
                                                            <div class="rounded-circle bg-primary-subtle text-primary fw-bold d-flex align-items-center justify-content-center"
                                                                 style="width: 32px; height: 32px; font-size: 12px;">
                                                                <c:choose>
                                                                    <c:when test="${not empty member.fullName}">
                                                                        ${fn:substring(member.fullName, 0, 1)}
                                                                    </c:when>
                                                                    <c:otherwise>U</c:otherwise>
                                                                </c:choose>
                                                            </div>
                                                            <div>
                                                                <span class="fw-semibold text-body d-block text-truncate" style="max-width: 140px;">
                                                                    ${fn:escapeXml(member.fullName)}
                                                                </span>
                                                                <span class="fs-9 text-muted">${member.role}</span>
                                                            </div>
                                                        </div>
                                                    </td>
                                                    <td class="text-center text-muted">${member.weeklyCapacityHours}h</td>
                                                    <td class="text-center">
                                                        <span class="fw-bold ${member.weekAssignedHours > member.weeklyCapacityHours ? 'text-danger' : 'text-body'}">
                                                            ${member.weekAssignedHours}h
                                                        </span>
                                                        <div class="fs-9 text-muted">${member.capacityUtilizationRate}%</div>
                                                    </td>
                                                    <!-- Thứ 2 -->
                                                    <td class="text-center">
                                                        <c:choose>
                                                            <c:when test="${member.mondayHours > member.dailyCapacityHours}">
                                                                <span class="heatmap-cell-pill heatmap-cell-overload">${member.mondayHours}h</span>
                                                            </c:when>
                                                            <c:when test="${member.mondayHours > 0}">
                                                                <span class="heatmap-cell-pill heatmap-cell-normal">${member.mondayHours}h</span>
                                                            </c:when>
                                                            <c:otherwise><span class="heatmap-cell-pill heatmap-cell-empty">-</span></c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                    <!-- Thứ 3 -->
                                                    <td class="text-center">
                                                        <c:choose>
                                                            <c:when test="${member.tuesdayHours > member.dailyCapacityHours}">
                                                                <span class="heatmap-cell-pill heatmap-cell-overload">${member.tuesdayHours}h</span>
                                                            </c:when>
                                                            <c:when test="${member.tuesdayHours > 0}">
                                                                <span class="heatmap-cell-pill heatmap-cell-normal">${member.tuesdayHours}h</span>
                                                            </c:when>
                                                            <c:otherwise><span class="heatmap-cell-pill heatmap-cell-empty">-</span></c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                    <!-- Thứ 4 -->
                                                    <td class="text-center">
                                                        <c:choose>
                                                            <c:when test="${member.wednesdayHours > member.dailyCapacityHours}">
                                                                <span class="heatmap-cell-pill heatmap-cell-overload">${member.wednesdayHours}h</span>
                                                            </c:when>
                                                            <c:when test="${member.wednesdayHours > 0}">
                                                                <span class="heatmap-cell-pill heatmap-cell-normal">${member.wednesdayHours}h</span>
                                                            </c:when>
                                                            <c:otherwise><span class="heatmap-cell-pill heatmap-cell-empty">-</span></c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                    <!-- Thứ 5 -->
                                                    <td class="text-center">
                                                        <c:choose>
                                                            <c:when test="${member.thursdayHours > member.dailyCapacityHours}">
                                                                <span class="heatmap-cell-pill heatmap-cell-overload">${member.thursdayHours}h</span>
                                                            </c:when>
                                                            <c:when test="${member.thursdayHours > 0}">
                                                                <span class="heatmap-cell-pill heatmap-cell-normal">${member.thursdayHours}h</span>
                                                            </c:when>
                                                            <c:otherwise><span class="heatmap-cell-pill heatmap-cell-empty">-</span></c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                    <!-- Thứ 6 -->
                                                    <td class="text-center">
                                                        <c:choose>
                                                            <c:when test="${member.fridayHours > member.dailyCapacityHours}">
                                                                <span class="heatmap-cell-pill heatmap-cell-overload">${member.fridayHours}h</span>
                                                            </c:when>
                                                            <c:when test="${member.fridayHours > 0}">
                                                                <span class="heatmap-cell-pill heatmap-cell-normal">${member.fridayHours}h</span>
                                                            </c:when>
                                                            <c:otherwise><span class="heatmap-cell-pill heatmap-cell-empty">-</span></c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                    <!-- Trạng thái -->
                                                    <td class="pe-4 text-end">
                                                        <c:choose>
                                                            <c:when test="${member.capacityStatus == 'OVERLOADED'}">
                                                                <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-2 py-0-5 fs-9">
                                                                    <i class="bi bi-exclamation-octagon-fill me-1"></i> Quá tải
                                                                </span>
                                                            </c:when>
                                                            <c:when test="${member.capacityStatus == 'UNDERUTILIZED'}">
                                                                <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle rounded-pill px-2 py-0-5 fs-9">
                                                                    <i class="bi bi-clock me-1"></i> Đang rảnh
                                                                </span>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-0-5 fs-9">
                                                                    <i class="bi bi-check-circle me-1"></i> Tối ưu
                                                                </span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <!-- Cột Phải (5 cols): Trợ Lý Đề Xuất San Tải Thông Minh (Smart Rebalancing Advisor) -->
            <div class="col-12 col-xl-5">
                <div class="card h-100 border rounded-4 bg-body shadow-2xs overflow-hidden">
                    <div class="card-header bg-body border-bottom py-3 px-4 d-flex align-items-center justify-content-between">
                        <div>
                            <h5 class="card-title fs-6 fw-bold mb-0 text-body d-flex align-items-center gap-2">
                                <i class="bi bi-lightbulb-fill text-primary"></i> Trợ Lý San Tải Tự Động (AI Advisor)
                            </h5>
                            <span class="fs-8 text-muted">Gợi ý chuyển giao việc từ người quá tải sang người còn dư năng lực</span>
                        </div>
                        <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2.5 py-1 fs-9 fw-semibold">
                            Rebalancing Engine
                        </span>
                    </div>
                    <div class="card-body p-4">
                        <c:choose>
                            <c:when test="${empty rebalanceSuggestions}">
                                <div class="text-center py-5">
                                    <div class="rounded-circle bg-success-subtle text-success d-inline-flex p-3 mb-2">
                                        <i class="bi bi-check2-all fs-2"></i>
                                    </div>
                                    <h6 class="fw-semibold text-body fs-7">Tải công việc đang phân bổ tối ưu!</h6>
                                    <p class="text-muted fs-8 mb-0">Không có thành viên nào bị quá tải định mức tuần (&gt;40h) hoặc dồn toa ngày (&gt;8h).</p>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="d-flex flex-column gap-2.5">
                                    <c:forEach var="sug" items="${rebalanceSuggestions}">
                                        <div class="rebalance-card-item">
                                            <div class="d-flex align-items-center justify-content-between mb-1.5">
                                                <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2 py-0-5 fs-9 fw-semibold">
                                                    Đề xuất chuyển giao
                                                </span>
                                                <span class="badge bg-light text-body border rounded-pill px-2 py-0-5 fs-9 fw-bold">
                                                    ~${sug.estimatedHours}h
                                                </span>
                                            </div>
                                            <div class="fw-semibold text-body fs-8 mb-1">
                                                <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}#task-${sug.taskId}" 
                                                   class="text-decoration-none text-body">
                                                    #${sug.taskId} - ${fn:escapeXml(sug.taskTitle)}
                                                </a>
                                            </div>
                                            <div class="d-flex align-items-center gap-2 fs-8 mb-2">
                                                <span class="text-danger fw-semibold"><i class="bi bi-person-dash me-1"></i>${fn:escapeXml(sug.fromUserName)}</span>
                                                <i class="bi bi-arrow-right text-muted"></i>
                                                <span class="text-success fw-semibold"><i class="bi bi-person-plus me-1"></i>${fn:escapeXml(sug.toUserName)}</span>
                                            </div>
                                            <div class="mt-2 pt-2 border-top d-flex flex-wrap align-items-center justify-content-between gap-2">
                                                <span class="fs-9 text-muted"><i class="bi bi-info-circle me-1"></i>${fn:escapeXml(sug.reason)}</span>
                                                <button type="button" class="btn btn-xs btn-primary rounded-pill px-2.5 py-1 fs-9 d-flex align-items-center gap-1 shadow-2xs"
                                                        onclick="applyRebalance(${sug.taskId}, ${sug.toUserId}, '${fn:escapeXml(sug.taskTitle)}', '${fn:escapeXml(sug.toUserName)}')">
                                                    <i class="bi bi-lightning-charge-fill"></i> Chuyển cho ${fn:escapeXml(sug.toUserName)} ngay
                                                </button>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- =========================================================================
         PHÂN HỆ 3: TRÁCH NHIỆM BÀN GIAO & RADAR ĐIỂM NGHẼN (VIEW-ACCOUNTABILITY)
         ========================================================================= -->
    <div id="view-accountability" class="analytics-view-section">
        <!-- Hàng 1: Bảng Đo Lường Trách Nhiệm Giao Hàng (Delivery Accountability) -->
        <div class="card border rounded-4 mb-4 bg-body shadow-2xs overflow-hidden">
            <div class="card-header bg-body border-bottom py-3 px-4 d-flex flex-wrap align-items-center justify-content-between gap-2">
                <div>
                    <h5 class="card-title fs-6 fw-bold mb-0 text-body d-flex align-items-center gap-2">
                        <i class="bi bi-award text-success"></i> Đo Lường Tính Chịu Trách Nhiệm Bàn Giao & Độ Tin Cậy (Delivery Accountability)
                    </h5>
                    <span class="fs-8 text-muted">
                        Chuẩn On-Time Delivery Rate (OTDR), Say-Do Ratio (Cam kết vs Hoàn thành) và Xếp hạng độ tin cậy thực hiện
                    </span>
                </div>
                <div class="d-flex align-items-center gap-1.5">
                    <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-0-5 fs-9">Xuất sắc (&ge;90%)</span>
                    <span class="badge bg-info-subtle text-info border border-info-subtle rounded-pill px-2 py-0-5 fs-9">Tốt (75-89%)</span>
                    <span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill px-2 py-0-5 fs-9">Cần cải thiện (&lt;75%)</span>
                </div>
            </div>
            <div class="card-body p-0">
                <c:choose>
                    <c:when test="${empty accountabilityList}">
                        <div class="text-center py-4 text-muted fs-8">Chưa có dữ liệu thành viên để đo lường.</div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0 fs-8">
                                <thead class="table-light text-muted fs-9 text-uppercase tracking-wider">
                                    <tr>
                                        <th class="ps-4 py-2-5">Thành viên</th>
                                        <th class="text-center py-2-5">Được giao</th>
                                        <th class="text-center py-2-5">Đã xong</th>
                                        <th class="text-center py-2-5">Đúng hạn</th>
                                        <th class="text-center py-2-5">Trễ hạn</th>
                                        <th class="text-center py-2-5">Quá hạn dồn ứ</th>
                                        <th class="text-center py-2-5">Dời hạn</th>
                                        <th class="text-center py-2-5" style="min-width: 140px;">Đúng hạn (OTDR)</th>
                                        <th class="text-center py-2-5">Say-Do Ratio</th>
                                        <th class="pe-4 py-2-5 text-end">Độ tin cậy</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="acc" items="${accountabilityList}">
                                        <tr>
                                            <td class="ps-4">
                                                <div class="d-flex align-items-center gap-2">
                                                    <div class="rounded-circle bg-primary-subtle text-primary fw-bold d-flex align-items-center justify-content-center"
                                                         style="width: 32px; height: 32px; font-size: 12px;">
                                                        <c:choose>
                                                            <c:when test="${not empty acc.fullName}">
                                                                ${fn:substring(acc.fullName, 0, 1)}
                                                            </c:when>
                                                            <c:otherwise>U</c:otherwise>
                                                        </c:choose>
                                                    </div>
                                                    <div>
                                                        <span class="fw-semibold text-body d-block text-truncate" style="max-width: 160px;">
                                                            ${fn:escapeXml(acc.fullName)}
                                                        </span>
                                                        <span class="fs-9 text-muted">${acc.role}</span>
                                                    </div>
                                                </div>
                                            </td>
                                            <td class="text-center fw-semibold text-body">${acc.totalAssignedTasks}</td>
                                            <td class="text-center text-success fw-semibold">${acc.completedTasks}</td>
                                            <td class="text-center text-body">${acc.onTimeCompletedTasks}</td>
                                            <td class="text-center ${acc.lateCompletedTasks > 0 ? 'text-warning fw-semibold' : 'text-muted'}">
                                                ${acc.lateCompletedTasks}
                                            </td>
                                            <td class="text-center ${acc.overdueActiveTasks > 0 ? 'text-danger fw-bold' : 'text-muted'}">
                                                ${acc.overdueActiveTasks}
                                            </td>
                                            <td class="text-center ${acc.totalRescheduledTasks > 0 ? 'text-warning fw-semibold' : 'text-muted'}">
                                                <c:choose>
                                                    <c:when test="${acc.totalRescheduledTasks > 0}">
                                                        <span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill px-2 py-0-5 fs-9 fw-semibold">
                                                            ${acc.totalRescheduledTasks} lần
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>0</c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="text-center">
                                                <div class="d-flex align-items-center justify-content-center gap-2">
                                                    <div class="progress flex-grow-1" style="height: 6px; max-width: 80px;">
                                                        <div class="progress-bar ${acc.onTimeDeliveryRate >= 90 ? 'bg-success' : (acc.onTimeDeliveryRate >= 75 ? 'bg-info' : 'bg-warning')}" 
                                                             style="width: ${acc.onTimeDeliveryRate}%"></div>
                                                    </div>
                                                    <span class="fw-bold ${acc.onTimeDeliveryRate >= 90 ? 'text-success' : (acc.onTimeDeliveryRate >= 75 ? 'text-info' : 'text-warning')}">
                                                        ${acc.onTimeDeliveryRate}%
                                                    </span>
                                                </div>
                                            </td>
                                            <td class="text-center fw-bold text-body">${acc.sayDoRatio}%</td>
                                            <td class="pe-4 text-end">
                                                <c:choose>
                                                    <c:when test="${acc.reliabilityRating == 'EXCELLENT'}">
                                                        <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2.5 py-1">
                                                            <i class="bi bi-patch-check-fill me-1"></i> Xuất sắc
                                                        </span>
                                                    </c:when>
                                                    <c:when test="${acc.reliabilityRating == 'GOOD'}">
                                                        <span class="badge bg-info-subtle text-info border border-info-subtle rounded-pill px-2.5 py-1">
                                                            <i class="bi bi-check-circle-fill me-1"></i> Tốt
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill px-2.5 py-1">
                                                            <i class="bi bi-exclamation-circle-fill me-1"></i> Cần cải thiện
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Hàng 1.5: RADAR CẢNH BÁO SỚM NGUY CƠ CẬN KỀ TRƯỚC 24H (NEAR-MISS 24H RADAR) -->
        <div class="card border rounded-4 mb-4 bg-body shadow-2xs overflow-hidden">
            <div class="card-header bg-body border-bottom py-3 px-4 d-flex flex-wrap align-items-center justify-content-between gap-2">
                <div>
                    <h5 class="card-title fs-6 fw-bold mb-0 text-body d-flex align-items-center gap-2">
                        <i class="bi bi-shield-exclamation text-danger"></i> Radar Cảnh Báo Sớm Nguy Cơ Cận Kề Trước 24h (Near-Miss 24h Radar)
                    </h5>
                    <span class="fs-8 text-muted">
                        Chủ động phát hiện các việc sắp trượt hạn (còn &le; 24h mà chưa bắt đầu hoặc tiến độ &lt; 40%, việc Critical dậm chân, dời hạn nhiều lần)
                    </span>
                </div>
                <c:choose>
                    <c:when test="${staleBlockerReport.totalNearMissCount > 0}">
                        <span class="badge bg-danger text-white rounded-pill px-2.5 py-1 fs-9 fw-bold">
                            ⚠️ ${staleBlockerReport.totalNearMissCount} Cảnh báo khẩn cấp
                        </span>
                    </c:when>
                    <c:otherwise>
                        <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2.5 py-1 fs-9">
                            <i class="bi bi-shield-check me-1"></i> An toàn tuyệt đối (0 rủi ro cận kề)
                        </span>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="card-body p-0">
                <c:choose>
                    <c:when test="${empty staleBlockerReport.nearMissTasks}">
                        <div class="text-center py-4">
                            <div class="rounded-circle bg-success-subtle text-success d-inline-flex p-2.5 mb-2">
                                <i class="bi bi-check2-circle fs-3"></i>
                            </div>
                            <h6 class="fw-semibold text-body fs-8 mb-1">Toàn bộ công việc trong vòng 24h tới đều an toàn!</h6>
                            <p class="text-muted fs-9 mb-0">Không có công việc nào sắp đến hạn bị bỏ quên hoặc có tiến độ trễ bất thường.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0 fs-8">
                                <thead class="table-light text-muted fs-9 text-uppercase tracking-wider">
                                    <tr>
                                        <th class="ps-4 py-2-5">Công việc</th>
                                        <th class="py-2-5">Người phụ trách</th>
                                        <th class="text-center py-2-5">Hạn chót</th>
                                        <th class="text-center py-2-5">Tiến độ</th>
                                        <th class="py-2-5">Nguyên nhân cảnh báo rủi ro</th>
                                        <th class="text-center py-2-5">Mức độ</th>
                                        <th class="pe-4 py-2-5 text-end">Hành động</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="nm" items="${staleBlockerReport.nearMissTasks}">
                                        <tr>
                                            <td class="ps-4 fw-medium text-body">
                                                <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}#task-${nm.taskId}" 
                                                   class="text-decoration-none text-body fw-bold text-truncate d-inline-block" style="max-width: 220px;"
                                                   title="${fn:escapeXml(nm.taskTitle)}">
                                                    #${nm.taskId} - ${fn:escapeXml(nm.taskTitle)}
                                                </a>
                                            </td>
                                            <td class="text-muted">${fn:escapeXml(nm.assigneeName)}</td>
                                            <td class="text-center">
                                                <span class="badge bg-light text-danger border rounded-pill px-2 py-0-5 font-monospace">
                                                    ${nm.dueDate}
                                                </span>
                                            </td>
                                            <td class="text-center">
                                                <div class="d-flex align-items-center justify-content-center gap-1.5">
                                                    <div class="progress" style="width: 50px; height: 5px;">
                                                        <div class="progress-bar ${nm.progress < 40 ? 'bg-danger' : 'bg-warning'}" style="width: ${nm.progress}%;"></div>
                                                    </div>
                                                    <span class="fw-bold fs-9">${nm.progress}%</span>
                                                </div>
                                            </td>
                                            <td class="text-danger fs-9 fw-medium">
                                                <i class="bi bi-exclamation-triangle-fill me-1"></i>${fn:escapeXml(nm.riskReason)}
                                            </td>
                                            <td class="text-center">
                                                <c:choose>
                                                    <c:when test="${nm.riskLevel == 'CRITICAL'}">
                                                        <span class="badge bg-danger text-white rounded-pill px-2 py-0-5 fs-9 fw-bold">NGUY CẤP</span>
                                                    </c:when>
                                                    <c:when test="${nm.riskLevel == 'HIGH'}">
                                                        <span class="badge bg-warning text-dark rounded-pill px-2 py-0-5 fs-9 fw-bold">CAO</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-secondary rounded-pill px-2 py-0-5 fs-9">CẢNH BÁO</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="pe-4 text-end">
                                                <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}#task-${nm.taskId}" 
                                                   class="btn btn-xs btn-outline-danger rounded-pill px-2.5 py-1 fs-9">
                                                    <i class="bi bi-box-arrow-up-right me-1"></i> Xử lý ngay
                                                </a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Hàng 2: Radar Điểm Nghẽn & Việc Chết Lâm Sàng (2 Cột Cân Đối) -->
        <div class="row g-4 mb-4">
            <!-- Cột Trái: Critical Blockers (Điểm Nghẽn Tiến Độ) -->
            <div class="col-12 col-lg-6">
                <div class="card h-100 border rounded-4 bg-body shadow-2xs overflow-hidden">
                    <div class="card-header bg-body border-bottom py-3 px-4 d-flex align-items-center justify-content-between">
                        <div>
                            <h5 class="card-title fs-6 fw-bold mb-0 text-body d-flex align-items-center gap-2">
                                <i class="bi bi-node-minus-fill text-danger"></i> Điểm Nghẽn Tiến Độ (Critical Blockers)
                            </h5>
                            <span class="fs-8 text-muted">Các việc chưa xong đang cản trở tiến độ của công việc khác</span>
                        </div>
                        <c:choose>
                            <c:when test="${staleBlockerReport.totalBlockerCount > 0}">
                                <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-2.5 py-1 fs-9 fw-bold">
                                    ${staleBlockerReport.totalBlockerCount} Điểm nghẽn (${staleBlockerReport.blockerImpactScore}% bị cản trở)
                                </span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2.5 py-1 fs-9">
                                    <i class="bi bi-check2-all me-1"></i> Thông suốt
                                </span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <div class="card-body p-0">
                        <c:choose>
                            <c:when test="${empty staleBlockerReport.criticalBlockers}">
                                <div class="text-center py-5">
                                    <div class="rounded-circle bg-success-subtle text-success d-inline-flex p-3 mb-2">
                                        <i class="bi bi-check-circle-fill fs-2"></i>
                                    </div>
                                    <h6 class="fw-semibold text-body fs-7">Chuỗi phụ thuộc hoàn toàn thông suốt!</h6>
                                    <p class="text-muted fs-8 mb-0">Không có công việc nào đang cản trở dây chuyền tiến độ của đội ngũ.</p>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="list-group list-group-flush fs-8">
                                    <c:forEach var="blocker" items="${staleBlockerReport.criticalBlockers}">
                                        <div class="list-group-item p-3 border-bottom">
                                            <div class="d-flex align-items-center justify-content-between mb-1">
                                                <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}#task-${blocker.taskId}" 
                                                   class="fw-bold text-decoration-none text-body text-truncate d-inline-block" style="max-width: 320px;">
                                                    #${blocker.taskId} - ${fn:escapeXml(blocker.taskTitle)}
                                                </a>
                                                <c:choose>
                                                    <c:when test="${blocker.severity == 'CRITICAL'}">
                                                        <span class="badge bg-danger text-white rounded-pill px-2 py-0-5 fs-9 fw-bold">CRITICAL</span>
                                                    </c:when>
                                                    <c:when test="${blocker.severity == 'HIGH'}">
                                                        <span class="badge bg-warning text-dark rounded-pill px-2 py-0-5 fs-9 fw-bold">HIGH</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-secondary rounded-pill px-2 py-0-5 fs-9">MEDIUM</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                            <div class="d-flex align-items-center gap-3 text-muted fs-9 mb-1.5">
                                                <span><i class="bi bi-person me-1"></i>${fn:escapeXml(blocker.assigneeName)}</span>
                                                <c:if test="${not empty blocker.dueDate}">
                                                    <span><i class="bi bi-calendar-event me-1"></i>Hạn: ${blocker.dueDate}</span>
                                                </c:if>
                                                <span class="badge bg-light text-body border rounded-pill">${blocker.status}</span>
                                            </div>
                                            <div class="text-danger fs-9 fw-medium">
                                                <i class="bi bi-exclamation-triangle-fill me-1"></i>${fn:escapeXml(blocker.reason)}
                                            </div>
                                            <div class="mt-2 pt-2 border-top fs-9 text-muted">
                                                <span>Đang cản trở <strong>${blocker.blockedTasksCount}</strong> công việc:</span>
                                                <div class="d-flex flex-wrap gap-1 mt-1">
                                                    <c:forEach var="bTitle" items="${blocker.blockedTaskTitles}">
                                                        <span class="badge bg-light text-secondary border rounded-pill text-truncate" style="max-width: 200px;">
                                                            ${fn:escapeXml(bTitle)}
                                                        </span>
                                                    </c:forEach>
                                                </div>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <!-- Cột Phải: Stale Tasks (Công Việc Chết Lâm Sàng > 3 ngày) -->
            <div class="col-12 col-lg-6">
                <div class="card h-100 border rounded-4 bg-body shadow-2xs overflow-hidden">
                    <div class="card-header bg-body border-bottom py-3 px-4 d-flex align-items-center justify-content-between">
                        <div>
                            <h5 class="card-title fs-6 fw-bold mb-0 text-body d-flex align-items-center gap-2">
                                <i class="bi bi-hourglass-bottom text-warning"></i> Công Việc "Chết Lâm Sàng" (Stale Tasks)
                            </h5>
                            <span class="fs-8 text-muted">Các việc đang thực hiện nhưng không có hoạt động hoặc cập nhật &gt; 3 ngày</span>
                        </div>
                        <span class="badge bg-light text-secondary border rounded-pill px-2.5 py-1 fs-9">
                            ${staleBlockerReport.totalStaleCount} Việc trơ
                        </span>
                    </div>
                    <div class="card-body p-0">
                        <c:choose>
                            <c:when test="${empty staleBlockerReport.staleTasks}">
                                <div class="text-center py-5">
                                    <div class="rounded-circle bg-success-subtle text-success d-inline-flex p-3 mb-2">
                                        <i class="bi bi-lightning-charge-fill fs-2"></i>
                                    </div>
                                    <h6 class="fw-semibold text-body fs-7">Mọi công việc đều đang chuyển động!</h6>
                                    <p class="text-muted fs-8 mb-0">Không có công việc nào bị bỏ rơi hay dậm chân quá 3 ngày.</p>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="list-group list-group-flush fs-8">
                                    <c:forEach var="stale" items="${staleBlockerReport.staleTasks}">
                                        <div class="list-group-item p-3 border-bottom">
                                            <div class="d-flex align-items-center justify-content-between mb-1">
                                                <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}#task-${stale.taskId}" 
                                                   class="fw-bold text-decoration-none text-body text-truncate d-inline-block" style="max-width: 320px;">
                                                    #${stale.taskId} - ${fn:escapeXml(stale.taskTitle)}
                                                </a>
                                                <span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill px-2 py-0-5 fs-9 fw-bold">
                                                    ${stale.staleDays} ngày trơ
                                                </span>
                                            </div>
                                            <div class="d-flex align-items-center justify-content-between text-muted fs-9 mb-2">
                                                <span><i class="bi bi-person me-1"></i>${fn:escapeXml(stale.assigneeName)}</span>
                                                <span>Tiến độ: <strong>${stale.progressPercentage}%</strong></span>
                                            </div>
                                            <div class="progress mb-2" style="height: 5px;">
                                                <div class="progress-bar bg-warning rounded-pill" style="width: ${stale.progressPercentage}%"></div>
                                            </div>
                                            <div class="text-danger fs-9">
                                                <i class="bi bi-clock-history me-1"></i>${fn:escapeXml(stale.reason)}
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- =========================================================================
     SCRIPT CONFIGURATION & CHART.JS ENGINE
     ========================================================================= -->
<script>
    window.ANALYTICS_CONFIG = {
        projectId: ${project.id},
        contextPath: '${pageContext.request.contextPath}',
        burndownData: ${not empty burndownJson ? burndownJson : '{}'},
        qualityData: ${not empty qualityJson ? qualityJson : '{}'},
        workloadData: ${not empty workloadsJson ? workloadsJson : '[]'},
        accountabilityData: ${not empty accountabilityJson ? accountabilityJson : '[]'},
        staleBlockerData: ${not empty staleBlockerJson ? staleBlockerJson : '{}'},
        rebalanceData: ${not empty rebalanceJson ? rebalanceJson : '[]'}
    };
</script>
<!-- Nạp Chart.js 4.4.2 CDN chuẩn jsDelivr -->
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.2/dist/chart.umd.min.js"></script>
<!-- Script điều khiển Analytics Dashboard -->
<script src="${pageContext.request.contextPath}/js/project-analytics.js?v=<%= System.currentTimeMillis() %>"></script>

<!-- Script Điều Khiển Segmented Tab Controller -->
<script>
(function() {
    'use strict';
    document.addEventListener('DOMContentLoaded', function() {
        const nav = document.getElementById('analyticsSegmentedNav');
        if (!nav) return;

        const buttons = nav.querySelectorAll('.analytics-segmented-btn');
        const sections = {
            'view-burndown': document.getElementById('view-burndown'),
            'view-workload': document.getElementById('view-workload'),
            'view-accountability': document.getElementById('view-accountability')
        };

        function switchView(targetViewId, savePreference) {
            // Cập nhật trạng thái active của buttons
            buttons.forEach(function(btn) {
                if (btn.getAttribute('data-view') === targetViewId) {
                    btn.classList.add('active');
                } else {
                    btn.classList.remove('active');
                }
            });

            // Ẩn/Hiện các section
            if (targetViewId === 'view-all') {
                Object.values(sections).forEach(function(sec) {
                    if (sec) sec.style.display = 'block';
                });
            } else {
                Object.keys(sections).forEach(function(key) {
                    const sec = sections[key];
                    if (sec) {
                        sec.style.display = (key === targetViewId) ? 'block' : 'none';
                    }
                });
            }

            // Kích hoạt lại resize của Chart.js khi container hiển thị lại
            window.dispatchEvent(new Event('resize'));

            if (savePreference) {
                try {
                    sessionStorage.setItem('analytics_active_view_' + window.ANALYTICS_CONFIG.projectId, targetViewId);
                } catch(e) {}
            }
        }

        // Lắng nghe sự kiện click
        buttons.forEach(function(btn) {
            btn.addEventListener('click', function() {
                const view = this.getAttribute('data-view');
                if (view) {
                    switchView(view, true);
                }
            });
        });

        // Khôi phục view đã chọn từ sessionStorage (nếu có)
        try {
            const savedView = sessionStorage.getItem('analytics_active_view_' + window.ANALYTICS_CONFIG.projectId);
            if (savedView && (sections[savedView] || savedView === 'view-all')) {
                switchView(savedView, false);
            }
        } catch(e) {}
    });
})();
</script>

<jsp:include page="/includes/footer.jsp" />
