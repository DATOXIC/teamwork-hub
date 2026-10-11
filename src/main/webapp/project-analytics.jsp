<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<%-- =========================================================================
     [ENTERPRISE FEATURE: PROJECT HEALTH, BURNDOWN & QUALITY ANALYTICS]
     VI: Bảng điều khiển phân tích động lực dự án, biểu đồ Burndown & kiểm toán chất lượng
     EN: Enterprise Project Health, Burndown Chart, Quality & Velocity Analytics Dashboard
     - Controller: com.teamwork.controllers.ProjectAnalyticsServlet (/analytics, /project-analytics)
     - Calculators: BurndownCalculator, QualityVelocityCalculator, WorkloadAnalytics
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
                <i class="bi bi-graph-up-arrow me-1"></i> Trung tâm Phân tích Doanh nghiệp
            </span>
            <span class="text-muted fs-8">
                Khảo sát chu kỳ Burn-down, tỷ lệ đạt lần đầu (FTPR) và cân bằng tải công việc
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
         2. EXECUTIVE BENTO KPI CARDS (4 CHỈ SỐ LÃNH ĐẠO)
         ========================================================================= -->
    <div class="row g-3 mb-4">
        <!-- KPI 1: Điểm Sức Khỏe Dự Án -->
        <div class="col-12 col-sm-6 col-xl-3">
            <div class="card h-100 border rounded-4 p-3 bg-body shadow-2xs">
                <div class="d-flex align-items-center justify-content-between mb-2">
                    <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted">Sức khỏe dự án</span>
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
                    <span class="fs-1 fw-bold tracking-tight text-body" id="kpiHealthScore">${qualityMetrics.healthScore}</span>
                    <span class="fs-7 text-muted fw-semibold">/ 100</span>
                </div>
                <div class="progress mb-2" style="height: 6px;">
                    <c:set var="barColor" value="${qualityMetrics.healthScore >= 75 ? 'bg-success' : (qualityMetrics.healthScore >= 50 ? 'bg-warning' : 'bg-danger')}" />
                    <div class="progress-bar ${barColor} rounded-pill" role="progressbar" 
                         style="width: ${qualityMetrics.healthScore}%" aria-valuenow="${qualityMetrics.healthScore}" aria-valuemin="0" aria-valuemax="100"></div>
                </div>
                <p class="fs-8 text-muted mb-0 text-truncate-2" title="${fn:escapeXml(qualityMetrics.healthDescription)}">
                    ${qualityMetrics.healthDescription}
                </p>
            </div>
        </div>

        <!-- KPI 2: Tiến Độ Burndown -->
        <div class="col-12 col-sm-6 col-xl-3">
            <div class="card h-100 border rounded-4 p-3 bg-body shadow-2xs">
                <div class="d-flex align-items-center justify-content-between mb-2">
                    <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted">Tiến độ Burndown</span>
                    <c:choose>
                        <c:when test="${burndownData.onTrack}">
                            <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2.5 py-1 fs-9">
                                <i class="bi bi-check-circle-fill me-1"></i> Đúng hạn
                            </span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-2.5 py-1 fs-9">
                                <i class="bi bi-exclamation-triangle-fill me-1"></i> Chậm ${burndownData.varianceTasks} việc
                            </span>
                        </c:otherwise>
                    </c:choose>
                </div>
                <div class="d-flex align-items-baseline gap-2 mb-1">
                    <span class="fs-1 fw-bold tracking-tight text-body" id="kpiCompletionRate">${burndownData.completionRate}%</span>
                    <span class="fs-8 text-muted">hoàn thành</span>
                </div>
                <div class="progress mb-2" style="height: 6px;">
                    <div class="progress-bar bg-primary rounded-pill" role="progressbar" 
                         style="width: ${burndownData.completionRate}%" aria-valuenow="${burndownData.completionRate}" aria-valuemin="0" aria-valuemax="100"></div>
                </div>
                <div class="d-flex justify-content-between fs-8 text-muted">
                    <span>Đã xong: <strong class="text-body">${burndownData.completedTasks}</strong></span>
                    <span>Còn lại: <strong class="text-body">${burndownData.remainingTasks}</strong> / ${burndownData.totalTasks}</span>
                </div>
            </div>
        </div>

        <!-- KPI 3: Tỷ Lệ Đạt Lần Đầu (First-Time Pass Rate) -->
        <div class="col-12 col-sm-6 col-xl-3">
            <div class="card h-100 border rounded-4 p-3 bg-body shadow-2xs">
                <div class="d-flex align-items-center justify-content-between mb-2">
                    <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted">Đạt ngay lần đầu (FTPR)</span>
                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2.5 py-1 fs-9">
                        Chất lượng bàn giao
                    </span>
                </div>
                <div class="d-flex align-items-baseline gap-2 mb-1">
                    <span class="fs-1 fw-bold tracking-tight text-body" id="kpiPassRate">${qualityMetrics.firstTimePassRate}%</span>
                    <span class="fs-8 text-muted">Revision 1</span>
                </div>
                <div class="progress mb-2" style="height: 6px;">
                    <div class="progress-bar bg-info rounded-pill" role="progressbar" 
                         style="width: ${qualityMetrics.firstTimePassRate}%" aria-valuenow="${qualityMetrics.firstTimePassRate}" aria-valuemin="0" aria-valuemax="100"></div>
                </div>
                <div class="d-flex justify-content-between fs-8 text-muted">
                    <span>Đạt Rev 1: <strong class="text-success">${qualityMetrics.firstTimePassedCount}</strong></span>
                    <span>Cần chỉnh sửa: <strong class="${qualityMetrics.revisionRequiredCount > 0 ? 'text-warning' : 'text-body'}">${qualityMetrics.revisionRequiredCount}</strong></span>
                </div>
            </div>
        </div>

        <!-- KPI 4: Vận Tốc Phản Hồi PM (PM Turnaround Velocity) -->
        <div class="col-12 col-sm-6 col-xl-3">
            <div class="card h-100 border rounded-4 p-3 bg-body shadow-2xs">
                <div class="d-flex align-items-center justify-content-between mb-2">
                    <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted">Vận tốc duyệt của PM</span>
                    <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle rounded-pill px-2.5 py-1 fs-9">
                        <i class="bi bi-clock-history me-1"></i> Turnaround
                    </span>
                </div>
                <div class="d-flex align-items-baseline gap-2 mb-1">
                    <c:choose>
                        <c:when test="${qualityMetrics.avgResponseHours > 0}">
                            <span class="fs-1 fw-bold tracking-tight text-body" id="kpiAvgHours">${qualityMetrics.avgResponseHours}</span>
                            <span class="fs-8 text-muted">giờ / lượt</span>
                        </c:when>
                        <c:otherwise>
                            <span class="fs-1 fw-bold tracking-tight text-body">--</span>
                            <span class="fs-8 text-muted">chưa có dữ liệu</span>
                        </c:otherwise>
                    </c:choose>
                </div>
                <div class="d-flex align-items-center gap-1.5 mb-2">
                    <c:forEach begin="1" end="5" var="star">
                        <i class="bi ${star <= qualityMetrics.avgQualityRating ? 'bi-star-fill text-warning' : (star - qualityMetrics.avgQualityRating < 1 && star - qualityMetrics.avgQualityRating > 0 ? 'bi-star-half text-warning' : 'bi-star text-muted')} fs-8"></i>
                    </c:forEach>
                    <span class="fs-8 text-muted ms-1">(${qualityMetrics.avgQualityRating} ⭐)</span>
                </div>
                <div class="d-flex justify-content-between fs-8 text-muted">
                    <span>Tổng đợt nộp: <strong class="text-body">${qualityMetrics.totalSubmissions}</strong></span>
                    <span>Chờ duyệt: <strong class="${qualityMetrics.pendingReviewCount > 0 ? 'text-primary' : 'text-body'}">${qualityMetrics.pendingReviewCount}</strong></span>
                </div>
            </div>
        </div>
    </div>

    <!-- =========================================================================
         3. BIỂU ĐỒ BURNDOWN TƯƠNG TÁC (CHART.JS 4.4+)
         ========================================================================= -->
    <div class="card border rounded-4 mb-4 bg-body shadow-2xs overflow-hidden">
        <div class="card-header bg-body border-bottom py-3 px-4 d-flex flex-wrap align-items-center justify-content-between gap-2">
            <div>
                <h5 class="card-title fs-6 fw-bold mb-0 text-body d-flex align-items-center gap-2">
                    <i class="bi bi-activity text-primary"></i> Biểu Đồ Burndown — Tiến Độ Hoàn Thành Nhiệm Vụ
                </h5>
                <span class="fs-8 text-muted">
                    Đường nét đứt xám: Tốc độ lý tưởng &bull; Đường nét liền xanh: Số việc còn lại thực tế qua từng ngày
                </span>
            </div>
            <div class="d-flex align-items-center gap-2">
                <span class="badge bg-light text-secondary border rounded-pill px-3 py-1 fs-8">
                    <i class="bi bi-calendar3 me-1"></i> ${burndownData.projectStartDate} &rarr; ${burndownData.projectEndDate}
                </span>
            </div>
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
                    <div class="position-relative" style="min-height: 320px; max-height: 420px;">
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

    <!-- =========================================================================
         4. PHÂN TÍCH CHẤT LƯỢNG NGHIỆM THU & CÔNG VIỆC CẦN SỬA ĐỔI NHIỀU NHẤT
         ========================================================================= -->
    <div class="row g-4 mb-4">
        <!-- Cột trái: Phân bố Chất Lượng & Đánh Giá Sao -->
        <div class="col-12 col-lg-5">
            <div class="card h-100 border rounded-4 bg-body shadow-2xs overflow-hidden">
                <div class="card-header bg-body border-bottom py-3 px-4">
                    <h5 class="card-title fs-6 fw-bold mb-0 text-body d-flex align-items-center gap-2">
                        <i class="bi bi-pie-chart text-info"></i> Phân Bố Chất Lượng Nghiệm Thu
                    </h5>
                    <span class="fs-8 text-muted">Tỷ lệ duyệt đạt ngay lần đầu vs yêu cầu nộp lại</span>
                </div>
                <div class="card-body p-4 d-flex flex-column justify-content-between">
                    <div class="position-relative mb-3" style="min-height: 220px; max-height: 260px;">
                        <canvas id="qualityDonutChart"></canvas>
                    </div>
                    <!-- Phân bố đánh giá sao 1-5 -->
                    <div class="border-top pt-3">
                        <span class="fs-9 text-uppercase tracking-wider fw-bold text-muted d-block mb-2">Thang điểm hài lòng của PM</span>
                        <div class="d-flex flex-column gap-1.5">
                            <c:forEach begin="1" end="5" var="star">
                                <c:set var="starIdx" value="${6 - star}" />
                                <c:set var="starCount" value="${qualityMetrics.ratingDistribution[starIdx]}" />
                                <c:set var="totalReviewed" value="${qualityMetrics.totalReviewedTasks > 0 ? qualityMetrics.totalReviewedTasks : 1}" />
                                <c:set var="starPct" value="${(starCount * 100) / totalReviewed}" />
                                <div class="d-flex align-items-center gap-2 fs-8">
                                    <span class="text-muted" style="width: 45px;">${starIdx} ⭐</span>
                                    <div class="progress flex-grow-1" style="height: 6px;">
                                        <div class="progress-bar bg-warning" style="width: ${starPct}%"></div>
                                    </div>
                                    <span class="text-body fw-semibold" style="width: 30px; text-align: right;">${starCount}</span>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Cột phải: Bảng Các Công Việc Bị Yêu Cầu Chỉnh Sửa Nhiều Nhất -->
        <div class="col-12 col-lg-7">
            <div class="card h-100 border rounded-4 bg-body shadow-2xs overflow-hidden">
                <div class="card-header bg-body border-bottom py-3 px-4 d-flex align-items-center justify-content-between">
                    <div>
                        <h5 class="card-title fs-6 fw-bold mb-0 text-body d-flex align-items-center gap-2">
                            <i class="bi bi-repeat text-warning"></i> Công Việc Cần Sửa Đổi Nhiều Nhất
                        </h5>
                        <span class="fs-8 text-muted">Đầu việc có nhiều đợt nộp hoặc nhận phản hồi REVISE / REJECT</span>
                    </div>
                    <span class="badge bg-light text-secondary border rounded-pill px-2.5 py-1 fs-9">Top 10</span>
                </div>
                <div class="card-body p-0">
                    <c:choose>
                        <c:when test="${empty qualityMetrics.mostRevisedTasks}">
                            <div class="text-center py-5">
                                <div class="rounded-circle bg-success-subtle text-success d-inline-flex p-3 mb-2">
                                    <i class="bi bi-shield-check fs-2"></i>
                                </div>
                                <h6 class="fw-semibold text-body fs-7">Chất lượng công việc xuất sắc!</h6>
                                <p class="text-muted fs-8 mb-0">Chưa có công việc nào bị Trưởng Dự Án yêu cầu chỉnh sửa.</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="table-responsive">
                                <table class="table table-hover align-middle mb-0 fs-8">
                                    <thead class="table-light text-muted fs-9 text-uppercase tracking-wider">
                                        <tr>
                                            <th class="ps-4 py-2-5">Công việc</th>
                                            <th class="py-2-5">Phụ trách</th>
                                            <th class="text-center py-2-5">Số đợt nộp</th>
                                            <th class="text-center py-2-5">Yêu cầu sửa</th>
                                            <th class="text-center py-2-5">Đánh giá</th>
                                            <th class="pe-4 py-2-5">Trạng thái</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="item" items="${qualityMetrics.mostRevisedTasks}">
                                            <tr>
                                                <td class="ps-4 fw-medium text-body">
                                                    <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}#task-${item.taskId}" 
                                                       class="text-decoration-none text-body text-truncate d-inline-block" style="max-width: 220px;" 
                                                       title="${fn:escapeXml(item.taskTitle)}">
                                                        ${fn:escapeXml(item.taskTitle)}
                                                    </a>
                                                    <c:if test="${not empty item.latestFeedback}">
                                                        <div class="text-muted fs-9 text-truncate" style="max-width: 220px;" title="${fn:escapeXml(item.latestFeedback)}">
                                                            <i class="bi bi-chat-left-text me-1"></i>${fn:escapeXml(item.latestFeedback)}
                                                        </div>
                                                    </c:if>
                                                </td>
                                                <td class="text-muted">${fn:escapeXml(item.assigneeName)}</td>
                                                <td class="text-center">
                                                    <span class="badge bg-light text-body border rounded-pill px-2 py-0-5">
                                                        Đợt #${item.revisionCount}
                                                    </span>
                                                </td>
                                                <td class="text-center">
                                                    <c:choose>
                                                        <c:when test="${item.rejectionsCount > 0}">
                                                            <span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill px-2 py-0-5 fw-bold">
                                                                ${item.rejectionsCount} lần
                                                            </span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="text-muted">0</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td class="text-center">
                                                    <c:choose>
                                                        <c:when test="${item.latestRating > 0}">
                                                            <span class="text-warning fw-semibold">${item.latestRating} ⭐</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="text-muted">--</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td class="pe-4">
                                                    <c:choose>
                                                        <c:when test="${item.finalStatus == 'APPROVED' || item.finalStatus == 'DONE'}">
                                                            <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-0-5">Đã duyệt</span>
                                                        </c:when>
                                                        <c:when test="${item.finalStatus == 'REVISE'}">
                                                            <span class="badge bg-info-subtle text-info border border-info-subtle rounded-pill px-2 py-0-5">Cần chỉnh sửa</span>
                                                        </c:when>
                                                        <c:when test="${item.finalStatus == 'REJECTED'}">
                                                            <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-2 py-0-5">Từ chối</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5">Chờ duyệt</span>
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
    </div>

    <!-- =========================================================================
         5. WORKLOAD BALANCE HEATMAP & CẢNH BÁO QUÁ TẢI (OVERLOAD ALERTS)
         ========================================================================= -->
    <div class="card border rounded-4 mb-4 bg-body shadow-2xs overflow-hidden">
        <div class="card-header bg-body border-bottom py-3 px-4 d-flex flex-wrap align-items-center justify-content-between gap-2">
            <div>
                <h5 class="card-title fs-6 fw-bold mb-0 text-body d-flex align-items-center gap-2">
                    <i class="bi bi-people text-primary"></i> Phân Bổ Tải Công Việc & Cảnh Báo Quá Tải Nhóm
                </h5>
                <span class="fs-8 text-muted">
                    Ma trận đo lường số nhiệm vụ đồng thời per thành viên &bull; Ngưỡng báo động: Đang làm &ge; 5 việc hoặc có việc quá hạn
                </span>
            </div>
            <div class="d-flex align-items-center gap-2">
                <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2.5 py-1 fs-9">
                    <i class="bi bi-circle-fill me-1" style="font-size: 6px;"></i> Cân bằng (&lt;5 việc)
                </span>
                <span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill px-2.5 py-1 fs-9">
                    <i class="bi bi-circle-fill me-1" style="font-size: 6px;"></i> Quá tải (&ge;5 việc)
                </span>
                <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-2.5 py-1 fs-9">
                    <i class="bi bi-circle-fill me-1" style="font-size: 6px;"></i> Quá hạn (&gt;0 việc)
                </span>
            </div>
        </div>
        <div class="card-body p-4">
            <c:choose>
                <c:when test="${empty workloadAnalytics}">
                    <div class="text-center py-4 text-muted fs-8">Chưa có thành viên nào tham gia dự án.</div>
                </c:when>
                <c:otherwise>
                    <div class="row g-3">
                        <c:forEach var="member" items="${workloadAnalytics}">
                            <div class="col-12 col-md-6 col-xl-4">
                                <div class="border rounded-3 p-3 h-100 bg-body-tertiary transition-all ${member.overloaded ? 'border-danger-subtle shadow-xs' : ''}">
                                    <div class="d-flex align-items-center justify-content-between mb-2">
                                        <div class="d-flex align-items-center gap-2">
                                            <div class="rounded-circle bg-primary-subtle text-primary fw-bold d-flex align-items-center justify-content-center"
                                                 style="width: 34px; height: 34px; font-size: 13px;">
                                                <c:choose>
                                                    <c:when test="${not empty member.fullName}">
                                                        ${fn:substring(member.fullName, 0, 1)}
                                                    </c:when>
                                                    <c:otherwise>U</c:otherwise>
                                                </c:choose>
                                            </div>
                                            <div>
                                                <span class="fw-semibold text-body fs-8 d-block text-truncate" style="max-width: 150px;">
                                                    ${fn:escapeXml(member.fullName)}
                                                </span>
                                                <span class="fs-9 text-muted d-block">${member.role}</span>
                                            </div>
                                        </div>
                                        <c:choose>
                                            <c:when test="${member.loadLevel == 'CRITICAL'}">
                                                <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-2 py-0-5 fs-9">
                                                    <i class="bi bi-exclamation-octagon-fill me-1"></i> Báo động đỏ
                                                </span>
                                            </c:when>
                                            <c:when test="${member.loadLevel == 'HIGH'}">
                                                <span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill px-2 py-0-5 fs-9">
                                                    <i class="bi bi-exclamation-triangle-fill me-1"></i> Quá tải
                                                </span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-0-5 fs-9">
                                                    <i class="bi bi-check-circle me-1"></i> Cân bằng
                                                </span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>

                                    <!-- Thống kê phân rã -->
                                    <div class="row g-2 text-center fs-8 my-2 py-2 border-top border-bottom">
                                        <div class="col-4">
                                            <span class="text-muted fs-9 d-block">Đang làm</span>
                                            <strong class="${member.inProgressCount >= 5 ? 'text-warning' : 'text-body'} fs-7">
                                                ${member.inProgressCount}
                                            </strong>
                                        </div>
                                        <div class="col-4">
                                            <span class="text-muted fs-9 d-block">Đã xong</span>
                                            <strong class="text-success fs-7">${member.doneCount}</strong>
                                        </div>
                                        <div class="col-4">
                                            <span class="text-muted fs-9 d-block">Quá hạn</span>
                                            <strong class="${member.overdueCount > 0 ? 'text-danger' : 'text-muted'} fs-7">
                                                ${member.overdueCount}
                                            </strong>
                                        </div>
                                    </div>

                                    <!-- Thanh tiến độ hoàn thành -->
                                    <div class="d-flex align-items-center justify-content-between fs-9 text-muted mb-1">
                                        <span>Tỷ lệ hoàn thành</span>
                                        <strong>${member.completionRate}%</strong>
                                    </div>
                                    <div class="progress" style="height: 5px;">
                                        <div class="progress-bar ${member.completionRate >= 80 ? 'bg-success' : 'bg-primary'}" 
                                             style="width: ${member.completionRate}%"></div>
                                    </div>

                                    <c:if test="${member.overloaded}">
                                        <div class="mt-2 fs-9 text-danger d-flex align-items-center gap-1">
                                            <i class="bi bi-info-circle"></i>
                                            <span class="text-truncate" title="${fn:escapeXml(member.overloadReason)}">
                                                ${fn:escapeXml(member.overloadReason)}
                                            </span>
                                        </div>
                                    </c:if>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<!-- =========================================================================
     6. SCRIPT CONFIGURATION & CHART.JS ENGINE
     ========================================================================= -->
<script>
    window.ANALYTICS_CONFIG = {
        projectId: ${project.id},
        contextPath: '${pageContext.request.contextPath}',
        burndownData: ${not empty burndownJson ? burndownJson : '{}'},
        qualityData: ${not empty qualityJson ? qualityJson : '{}'},
        workloadData: ${not empty workloadsJson ? workloadsJson : '[]'}
    };
</script>
<!-- Nạp Chart.js 4.4.2 CDN chuẩn jsDelivr -->
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.2/dist/chart.umd.min.js"></script>
<!-- Script điều khiển Analytics Dashboard -->
<script src="${pageContext.request.contextPath}/js/project-analytics.js?v=<%= System.currentTimeMillis() %>"></script>

<jsp:include page="/includes/footer.jsp" />
