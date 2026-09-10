<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Báo Cáo Tiến Độ Dự Án — ${project.name}" scope="request" />
<c:set var="extraCss" value="styles/report.css" scope="request" />

<!-- Khởi tạo Theme tức thời để tránh nhấp nháy FOUC -->
<script>
    (function() {
        var t = localStorage.getItem('teamwork_report_theme');
        if (!t) {
            t = (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light';
        }
        document.documentElement.setAttribute('data-theme', t);
        document.documentElement.setAttribute('data-bs-theme', t);
    })();
</script>

<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/styles/report.css?v=<%= System.currentTimeMillis() %>">

<!-- =========================================================================
1. STICKY GLASSMORPHIC SUB-NAV (THANH ĐIỀU HƯỚNG NỔI HÍT TRẦN)
========================================================================= -->
<div class="report-sticky-nav no-print">
    <div class="container-fluid px-lg-5 py-2-5 d-flex flex-wrap align-items-center justify-content-between gap-3">
        <!-- Điều hướng phân hệ (Navigation Context) -->
        <div class="d-flex align-items-center gap-2 flex-wrap">
            <a href="${pageContext.request.contextPath}/project?action=list"
                class="btn btn-outline-secondary btn-sm rounded-pill px-3 py-1-5 shadow-none d-flex align-items-center gap-1"
                title="Quay về danh sách dự án (Dashboard)">
                <i class="bi bi-arrow-left"></i> <span class="d-none d-sm-inline">Dự án</span>
            </a>
            <div class="border-start ps-2 d-flex align-items-center gap-2">
                <span class="badge bg-dark-navy text-white rounded-pill px-2-5 py-1 fs-9">
                    #${project.projectCode}
                </span>
                <span class="fw-bold text-dark fs-7 text-truncate" style="max-width: 240px;" title="${project.name}">${project.name}</span>
            </div>

            <!-- Tab Chuyển Phân Hệ Nhanh (Segmented Capsule) -->
            <div class="d-none d-md-flex align-items-center gap-1 report-nav-pill-group ms-2">
                <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}"
                    class="report-nav-pill">
                    <i class="bi bi-kanban me-1"></i> Kanban
                </a>
                <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}"
                    class="report-nav-pill">
                    <i class="bi bi-journal-text me-1"></i> Tài liệu
                </a>
                <a href="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}"
                    class="report-nav-pill">
                    <i class="bi bi-chat-dots me-1"></i> Thảo luận
                </a>
                <span class="report-nav-pill active">
                    <i class="bi bi-file-earmark-bar-graph me-1"></i> Báo cáo
                </span>
            </div>
        </div>

        <!-- Cụm Nút Thao Tác Xuất / In + Nút Đổi Theme (Actions) -->
        <div class="d-flex align-items-center gap-2">
            <!-- Nút Bật / Tắt Giao Diện Sáng - Tối (Light / Dark Mode) -->
            <button type="button" id="themeToggleBtn" onclick="toggleGlobalTheme()"
                class="btn btn-sm rounded-pill px-3 py-1-5 fw-semibold d-flex align-items-center gap-2 theme-toggle-btn"
                title="Chuyển đổi giao diện Sáng / Tối (Light / Dark Mode)"
                aria-label="Chuyển đổi giao diện Sáng / Tối">
                <i class="bi bi-moon-stars-fill theme-icon-moon"></i>
                <i class="bi bi-sun-fill theme-icon-sun"></i>
                <span class="theme-text d-none d-sm-inline" id="themeBtnText">Chế độ tối</span>
            </button>

            <button type="button" onclick="window.print()"
                class="btn btn-primary-custom btn-sm rounded-pill px-3 py-1-5 fw-semibold shadow-sm fs-8 d-flex align-items-center gap-2 text-white"
                aria-label="In báo cáo tiến độ hoặc lưu file PDF">
                <i class="bi bi-printer-fill"></i> In Báo Cáo / PDF
            </button>
            <a href="${pageContext.request.contextPath}/task?action=exportCsv&projectId=${project.id}"
                class="btn btn-outline-success btn-sm rounded-pill px-3 py-1-5 fw-semibold shadow-sm fs-8 d-flex align-items-center gap-2"
                title="Tải toàn bộ danh sách công việc của dự án ra file Excel (.csv chuẩn UTF-8 BOM)"
                aria-label="Xuất danh sách công việc ra file Excel CSV">
                <i class="bi bi-file-earmark-spreadsheet-fill"></i> Xuất CSV
            </a>
        </div>
    </div>
</div>

<div class="container-fluid px-lg-5 py-4 report-app-container">

    <!-- Thông báo Flash Toast (nếu có) -->
    <jsp:include page="/includes/toast.jsp" />

    <!-- =========================================================================
    2. REPORT HEADER BANNER: TIÊU ĐỀ BÁO CÁO & THÔNG TIN DỰ ÁN
    ========================================================================= -->
    <div class="report-header-badge mb-4">
        <div class="d-flex flex-wrap justify-content-between align-items-start gap-3 position-relative" style="z-index: 1;">
            <div>
                <div class="d-flex align-items-center gap-2 mb-2 flex-wrap">
                    <span class="badge ${healthBadgeClass} rounded-pill px-3 py-1-5 fs-9 shadow-xs d-flex align-items-center gap-2">
                        <c:choose>
                            <c:when test="${projectHealth == 'HEALTHY'}">
                                <span class="pulse-dot pulse-dot-healthy"></span>
                            </c:when>
                            <c:when test="${projectHealth == 'AT_RISK'}">
                                <span class="pulse-dot pulse-dot-warning"></span>
                            </c:when>
                            <c:otherwise>
                                <span class="pulse-dot pulse-dot-critical"></span>
                            </c:otherwise>
                        </c:choose>
                        <span class="fw-semibold">${healthLabel}</span>
                    </span>
                    <span class="badge bg-secondary-subtle text-white border border-secondary rounded-pill px-2-5 py-1 fs-9">
                        #${project.projectCode}
                    </span>
                    <span class="badge bg-white bg-opacity-10 text-white rounded-pill px-2-5 py-1 fs-9">
                        <i class="bi bi-calendar3 me-1"></i> Khởi tạo: ${project.createdAt}
                    </span>
                </div>
                <h2 class="fw-extrabold mb-1 text-white tracking-tight">${project.name}</h2>
                <p class="fs-8 mb-2 report-project-desc">
                    ${not empty project.description ? project.description : 'Dự án chưa cập nhật mô tả chi tiết.'}
                </p>
                <div class="d-flex align-items-center gap-2 text-white-50 fs-9">
                    <i class="bi bi-info-circle text-info"></i>
                    <span>${healthDescription}</span>
                </div>
            </div>

            <!-- Metadata Khung Phải -->
            <div class="text-md-end text-white-50 fs-8">
                <div><strong>Thời điểm xuất báo cáo:</strong> <span class="text-white">${generatedAt}</span></div>
                <div class="mt-1"><strong>Người xuất báo cáo:</strong> <span class="text-white">${sessionScope.currentUser.fullName} (${sessionScope.currentUser.role})</span></div>
            </div>
        </div>
    </div>

    <!-- =========================================================================
    3. EXECUTIVE SPOTLIGHT: ĐIỂM NGHẼN & RỦI RO CẦN QUYẾT ĐỊNH (BLOCKER SPOTLIGHT)
    ========================================================================= -->
    <div class="mb-4 avoid-break">
        <c:choose>
            <c:when test="${blockerCount > 0}">
                <div class="blocker-spotlight-card shadow-sm">
                    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                        <div class="d-flex align-items-center gap-2">
                            <span class="badge bg-danger text-white rounded-pill px-2-5 py-1 fs-9 fw-bold">
                                <i class="bi bi-exclamation-octagon-fill me-1"></i> ${blockerCount} ĐIỂM NGHẼN
                            </span>
                            <h6 class="fw-bold text-dark fs-7 mb-0">Công Việc Có Nguy Cơ / Cần Can Thiệp Khẩn Cấp</h6>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <span class="fs-9 text-muted d-none d-md-inline">Ưu tiên xử lý để đảm bảo tiến độ nghiệm thu</span>
                            <a href="javascript:void(0)" onclick="handleKpiCardClick('OVERDUE')"
                                class="no-print text-decoration-none fw-semibold fs-9 text-danger d-inline-flex align-items-center gap-1">
                                Xem toàn bộ ${blockerCount} điểm nghẽn &darr;
                            </a>
                        </div>
                    </div>
                    <div class="row g-2">
                        <c:forEach items="${criticalBlockers}" var="b" end="3">
                            <div class="col-12 col-md-6 col-lg-3">
                                <div class="blocker-item h-100 d-flex flex-column justify-content-between">
                                    <div>
                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                            <span class="badge bg-dark-navy text-white fs-9 rounded-pill">#${b.id}</span>
                                            <span class="badge ${b.priorityBadgeClass} fs-9">${b.priority}</span>
                                        </div>
                                        <div class="fw-bold text-dark fs-8 text-truncate mb-1" title="${b.title}">${b.title}</div>
                                        <div class="fs-9 text-muted mb-2">
                                            <i class="bi bi-person text-secondary"></i> ${b.assigneeName}
                                        </div>
                                    </div>
                                    <div class="d-flex align-items-center justify-content-between pt-2 border-top fs-9">
                                        <span class="text-danger fw-semibold">
                                            <c:choose>
                                                <c:when test="${b.isOverdue()}">
                                                    <i class="bi bi-clock-history me-1"></i> Quá hạn
                                                </c:when>
                                                <c:otherwise>
                                                    <i class="bi bi-arrow-repeat me-1"></i> ${b.statusLabel}
                                                </c:otherwise>
                                            </c:choose>
                                        </span>
                                        <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}"
                                            class="text-decoration-none fw-bold fs-9 text-primary no-print"
                                            aria-label="Xem chi tiết công việc #${b.id}: ${b.title}">
                                            Xem task &rarr;
                                        </a>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="no-blocker-card d-flex align-items-center justify-content-between shadow-xs">
                    <div class="d-flex align-items-center gap-2">
                        <i class="bi bi-shield-check-fill fs-5 text-success"></i>
                        <div>
                            <div class="fw-bold fs-8">Dự án không có điểm nghẽn (No Active Blockers)</div>
                            <div class="fs-9 text-muted">Toàn bộ công việc đang chạy đúng kế hoạch, không có công việc nào bị quá hạn hoặc bị từ chối duyệt.</div>
                        </div>
                    </div>
                    <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-3 py-1 fs-9 fw-semibold d-flex align-items-center gap-1">
                        <i class="bi bi-shield-check-fill"></i> Tiến độ tối ưu
                    </span>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- =========================================================================
    4. EXECUTIVE SUMMARY CARDS: CÁC CHỈ SỐ TIẾN ĐỘ QUAN TRỌNG NHẤT (CLICK ĐỂ LỌC)
    ========================================================================= -->
    <div class="row g-3 mb-4 avoid-break">
        <!-- Card 1: Tổng số công việc -->
        <div class="col-6 col-md-4 col-xl">
            <div class="report-stat-card h-100" id="kpiCardAll" role="button" tabindex="0"
                onclick="handleKpiCardClick('ALL')" onkeydown="if(event.key==='Enter'||event.key===' ')handleKpiCardClick('ALL')"
                title="Click để xem toàn bộ danh sách công việc">
                <div class="d-flex align-items-center justify-content-between mb-1">
                    <span class="report-card-title">Tổng Công Việc</span>
                    <i class="bi bi-list-task text-primary fs-5"></i>
                </div>
                <div class="report-stat-number">${totalTasks}</div>
                <div class="fs-9 text-muted mt-1">
                    <i class="bi bi-people-fill me-1"></i> ${memberCount} thành viên
                </div>
            </div>
        </div>

        <!-- Card 2: Đã hoàn thành & Tỷ lệ -->
        <div class="col-6 col-md-4 col-xl">
            <div class="report-stat-card h-100 border-success-subtle" id="kpiCardDone" role="button" tabindex="0"
                onclick="handleKpiCardClick('DONE')" onkeydown="if(event.key==='Enter'||event.key===' ')handleKpiCardClick('DONE')"
                title="Click để lọc các công việc đã hoàn tất nghiệm thu">
                <div class="d-flex align-items-center justify-content-between mb-1">
                    <span class="report-card-title text-success">Đã Hoàn Thành</span>
                    <i class="bi bi-check-circle-fill text-success fs-5"></i>
                </div>
                <div class="report-stat-number text-success">${doneCount}</div>
                <div class="fs-9 text-success fw-bold mt-1">
                    Tỷ lệ hoàn thành: ${progressPercentage}%
                </div>
            </div>
        </div>

        <!-- Card 3: Đang thực hiện -->
        <div class="col-6 col-md-4 col-xl">
            <div class="report-stat-card h-100 border-info-subtle" id="kpiCardInProgress" role="button" tabindex="0"
                onclick="handleKpiCardClick('IN_PROGRESS')" onkeydown="if(event.key==='Enter'||event.key===' ')handleKpiCardClick('IN_PROGRESS')"
                title="Click để lọc các công việc đang triển khai">
                <div class="d-flex align-items-center justify-content-between mb-1">
                    <span class="report-card-title text-info-emphasis">Đang Làm</span>
                    <i class="bi bi-arrow-repeat text-info fs-5"></i>
                </div>
                <div class="report-stat-number text-info-emphasis">${inProgressCount}</div>
                <div class="fs-9 text-muted mt-1">
                    Kế hoạch đã khóa
                </div>
            </div>
        </div>

        <!-- Card 4: Chờ thẩm định / Nghiệm thu -->
        <div class="col-6 col-md-6 col-xl">
            <div class="report-stat-card h-100 border-warning-subtle" id="kpiCardPending" role="button" tabindex="0"
                onclick="handleKpiCardClick('PENDING_REVIEW')" onkeydown="if(event.key==='Enter'||event.key===' ')handleKpiCardClick('PENDING_REVIEW')"
                title="Click để lọc các công việc đang chờ duyệt kế hoạch hoặc nghiệm thu">
                <div class="d-flex align-items-center justify-content-between mb-1">
                    <span class="report-card-title text-warning-emphasis">Chờ PM Duyệt</span>
                    <i class="bi bi-hourglass-split text-warning fs-5"></i>
                </div>
                <div class="report-stat-number text-warning-emphasis">${submittedCount + planningCount}</div>
                <div class="fs-9 text-muted mt-1">
                    ${planningCount} duyệt kế hoạch &bull; ${submittedCount} nghiệm thu
                </div>
            </div>
        </div>

        <!-- Card 5: Quá hạn -->
        <div class="col-12 col-md-6 col-xl">
            <div class="report-stat-card h-100 ${overdueCount > 0 ? 'border-danger' : ''}"
                id="kpiCardOverdue" role="button" tabindex="0"
                onclick="handleKpiCardClick('OVERDUE')" onkeydown="if(event.key==='Enter'||event.key===' ')handleKpiCardClick('OVERDUE')"
                title="Click để lọc các công việc bị trễ hạn">
                <div class="d-flex align-items-center justify-content-between mb-1">
                    <span class="report-card-title text-danger">Bị Quá Hạn</span>
                    <i class="bi bi-exclamation-triangle-fill text-danger fs-5"></i>
                </div>
                <div class="report-stat-number text-danger">${overdueCount}</div>
                <div class="fs-9 text-danger fw-bold mt-1">
                    <c:choose>
                        <c:when test="${overdueCount > 0}">Cần xử lý gấp!</c:when>
                        <c:otherwise><span class="text-success"><i class="bi bi-shield-check me-1"></i>Tiến độ an toàn</span></c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>

    <!-- =========================================================================
    5. TIẾN ĐỘ TỔNG THỂ & PHÂN BỐ CÔNG VIỆC
    ========================================================================= -->
    <div class="row g-4 mb-4 avoid-break">
        <!-- Cột Trái: Thanh tiến độ tổng thể -->
        <div class="col-12 col-lg-7">
            <div class="bg-white p-4 rounded-3 shadow-sm border h-100">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <h6 class="fw-bold text-dark fs-7 mb-0 d-flex align-items-center gap-2">
                        <i class="bi bi-speedometer2 text-primary"></i> Tiến Độ Dự Án Tổng Thể
                    </h6>
                    <span class="badge bg-primary text-white rounded-pill px-3 py-1 fs-8 fw-bold">
                        ${progressPercentage}% Hoàn Tất
                    </span>
                </div>
                <p class="text-muted fs-8 mb-3">Đã nghiệm thu đạt chuẩn ${doneCount} trên tổng số ${totalTasks} công việc được giao.</p>

                <!-- Progress bar -->
                <div class="progress progress-bar-custom mb-3">
                    <div class="progress-bar bg-success progress-bar-striped" role="progressbar"
                        style="width: ${progressPercentage}%;" aria-valuenow="${progressPercentage}"
                        aria-valuemin="0" aria-valuemax="100"
                        aria-label="Tiến độ dự án tổng thể: ${progressPercentage}%"></div>
                </div>

                <!-- Chi tiết trạng thái -->
                <div class="d-flex flex-wrap gap-2 pt-2 border-top">
                    <span class="badge bg-light text-dark border fs-9"><i class="bi bi-circle-fill text-secondary me-1"></i> Cần làm: ${todoCount}</span>
                    <span class="badge bg-light text-dark border fs-9"><i class="bi bi-circle-fill text-primary me-1"></i> Chờ duyệt KH: ${planningCount}</span>
                    <span class="badge bg-light text-dark border fs-9"><i class="bi bi-circle-fill text-info me-1"></i> Đang làm: ${inProgressCount}</span>
                    <span class="badge bg-light text-dark border fs-9"><i class="bi bi-circle-fill text-warning me-1"></i> Chờ nghiệm thu: ${submittedCount}</span>
                    <span class="badge bg-light text-dark border fs-9"><i class="bi bi-circle-fill text-primary me-1"></i> Cần cân chỉnh: ${reviseCount}</span>
                    <span class="badge bg-light text-dark border fs-9"><i class="bi bi-circle-fill text-danger me-1"></i> Chưa đạt: ${rejectedCount}</span>
                    <span class="badge bg-light text-dark border fs-9"><i class="bi bi-circle-fill text-success me-1"></i> Đã nghiệm thu: ${doneCount}</span>
                </div>
            </div>
        </div>

        <!-- Cột Phải: Phân bổ mức độ ưu tiên -->
        <div class="col-12 col-lg-5">
            <div class="bg-white p-4 rounded-3 shadow-sm border h-100">
                <h6 class="fw-bold text-dark fs-7 mb-2 d-flex align-items-center gap-2">
                    <i class="bi bi-flag-fill text-warning"></i> Phân Bổ Mức Độ Ưu Tiên
                </h6>
                <p class="text-muted fs-8 mb-3">Tỷ lệ công việc theo mức độ quan trọng trong dự án.</p>

                <div class="d-flex flex-column gap-3">
                    <!-- High Priority -->
                    <div>
                        <div class="d-flex justify-content-between align-items-center fs-8 mb-1">
                            <span class="fw-semibold text-danger"><i class="bi bi-exclamation-circle-fill me-1"></i> Ưu tiên Cao (HIGH)</span>
                            <span class="fw-bold">${highPriorityCount} việc</span>
                        </div>
                        <div class="progress progress-mini" role="progressbar"
                            aria-valuenow="${totalTasks > 0 ? (highPriorityCount * 100 / totalTasks) : 0}"
                            aria-valuemin="0" aria-valuemax="100"
                            aria-label="Công việc ưu tiên cao: ${highPriorityCount} việc">
                            <div class="progress-bar bg-danger"
                                style="width: ${totalTasks > 0 ? (highPriorityCount * 100 / totalTasks) : 0}%;">
                            </div>
                        </div>
                    </div>

                    <!-- Medium Priority -->
                    <div>
                        <div class="d-flex justify-content-between align-items-center fs-8 mb-1">
                            <span class="fw-semibold text-warning-emphasis"><i class="bi bi-dash-circle-fill me-1"></i> Ưu tiên Trung bình (MEDIUM)</span>
                            <span class="fw-bold">${mediumPriorityCount} việc</span>
                        </div>
                        <div class="progress progress-mini" role="progressbar"
                            aria-valuenow="${totalTasks > 0 ? (mediumPriorityCount * 100 / totalTasks) : 0}"
                            aria-valuemin="0" aria-valuemax="100"
                            aria-label="Công việc ưu tiên trung bình: ${mediumPriorityCount} việc">
                            <div class="progress-bar bg-warning"
                                style="width: ${totalTasks > 0 ? (mediumPriorityCount * 100 / totalTasks) : 0}%;">
                            </div>
                        </div>
                    </div>

                    <!-- Low Priority -->
                    <div>
                        <div class="d-flex justify-content-between align-items-center fs-8 mb-1">
                            <span class="fw-semibold text-info-emphasis"><i class="bi bi-arrow-down-circle-fill me-1"></i> Ưu tiên Thấp (LOW)</span>
                            <span class="fw-bold">${lowPriorityCount} việc</span>
                        </div>
                        <div class="progress progress-mini" role="progressbar"
                            aria-valuenow="${totalTasks > 0 ? (lowPriorityCount * 100 / totalTasks) : 0}"
                            aria-valuemin="0" aria-valuemax="100"
                            aria-label="Công việc ưu tiên thấp: ${lowPriorityCount} việc">
                            <div class="progress-bar bg-info"
                                style="width: ${totalTasks > 0 ? (lowPriorityCount * 100 / totalTasks) : 0}%;">
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- =========================================================================
    6. BẢNG ĐÓNG GÓP & HIỆU SUẤT CỦA THÀNH VIÊN (MEMBER PERFORMANCE)
    ========================================================================= -->
    <div class="bg-white p-4 rounded-3 shadow-sm border mb-4 avoid-break">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <div>
                <h5 class="fw-bold text-dark fs-6 mb-1 d-flex align-items-center gap-2">
                    <i class="bi bi-people-fill text-primary"></i> Đóng Góp & Năng Suất Thành Viên
                </h5>
                <p class="text-muted fs-8 mb-0">Thống kê khối lượng công việc, tỷ lệ hoàn thành và điểm chất lượng bàn giao theo từng người.</p>
            </div>
            <span class="badge bg-light text-secondary border rounded-pill px-3 py-2 fs-8">
                Tổng cộng: ${memberCount} thành viên
            </span>
        </div>

        <div class="table-responsive">
            <table class="table table-hover report-table mb-0 align-middle" id="membersPerformanceTable">
                <thead>
                    <tr>
                        <th class="col-w-id">STT</th>
                        <th>Thành viên</th>
                        <th>Chuyên môn / Vai trò</th>
                        <th class="text-center">Được giao</th>
                        <th class="text-center">Hoàn thành</th>
                        <th class="text-center">Đang làm</th>
                        <th class="text-center">Quá hạn</th>
                        <th class="text-center col-w-progress">Tiến độ cá nhân</th>
                        <th class="text-center">Đánh giá TB</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${memberStats}" var="stat" varStatus="loop">
                        <tr>
                            <td class="text-muted text-center">${loop.index + 1}</td>
                            <td>
                                <div class="d-flex align-items-center gap-2">
                                    <div
                                        class="avatar-circle bg-primary-subtle text-primary fw-bold d-flex align-items-center justify-content-center rounded-circle report-avatar">
                                        ${not empty stat.member.userName ? stat.member.userName.substring(0, 1).toUpperCase() : 'U'}
                                    </div>
                                    <div>
                                        <div class="fw-bold text-dark">${stat.member.userName}</div>
                                        <div class="text-muted fs-9">${stat.member.userEmail}</div>
                                    </div>
                                </div>
                            </td>
                            <td>
                                <div><span class="badge bg-light text-dark border fs-9">${stat.member.userRole}</span></div>
                                <c:choose>
                                    <c:when test="${stat.member.projectRole == 'OWNER'}">
                                        <span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle rounded-pill fs-9 mt-1">
                                            <i class="bi bi-star-fill me-1"></i> Trưởng dự án (PM)
                                        </span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge bg-secondary-subtle text-secondary rounded-pill fs-9 mt-1">
                                            Thành viên
                                        </span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-center fw-bold">${stat.assignedCount}</td>
                            <td class="text-center text-success fw-bold">${stat.doneCount}</td>
                            <td class="text-center text-info-emphasis">${stat.pendingCount}</td>
                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${stat.overdueCount > 0}">
                                        <span class="badge bg-danger text-white rounded-pill px-2 fs-9">${stat.overdueCount}</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="text-muted fs-9">0</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-center">
                                <div class="d-flex align-items-center gap-2">
                                    <div class="progress flex-grow-1 progress-mini" role="progressbar"
                                        aria-valuenow="${stat.completionRate}" aria-valuemin="0" aria-valuemax="100"
                                        aria-label="Tiến độ của ${stat.member.userName}: ${stat.completionRate}%">
                                        <div class="progress-bar bg-success" style="width: ${stat.completionRate}%;"></div>
                                    </div>
                                    <span class="fw-bold fs-9 min-w-rate">${stat.completionRate}%</span>
                                </div>
                            </td>
                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${stat.ratedTasksCount > 0}">
                                        <span class="text-warning fw-bold fs-8">
                                            <i class="bi bi-star-fill"></i> ${stat.avgRating}
                                        </span>
                                        <span class="text-muted fs-9">(${stat.ratedTasksCount})</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="text-muted fs-9">Chưa đánh giá</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty memberStats}">
                        <tr>
                            <td colspan="9" class="text-center py-5">
                                <div class="report-empty-state">
                                    <i class="bi bi-people empty-icon d-block"></i>
                                    <h6 class="fw-bold text-dark mb-1">Chưa có thành viên nào trong dự án</h6>
                                    <p class="text-muted fs-8 mb-3">Dự án này chưa được phân công thành viên tham gia thực hiện nhiệm vụ.</p>
                                    <a href="${pageContext.request.contextPath}/project?action=members&projectId=${project.id}" class="btn btn-outline-primary btn-sm rounded-pill px-3 no-print">
                                        <i class="bi bi-person-plus me-1"></i> Phân công thành viên
                                    </a>
                                </div>
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>

    <!-- =========================================================================
    7. DANH SÁCH CHI TIẾT TẤT CẢ CÔNG VIỆC (DETAILED TASKS INVENTORY)
    ========================================================================= -->
    <div class="bg-white p-4 rounded-3 shadow-sm border mb-4 avoid-break" id="tasksInventorySection">
        <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
            <div>
                <h5 class="fw-bold text-dark fs-6 mb-1 d-flex align-items-center gap-2">
                    <i class="bi bi-card-checklist text-primary"></i> Bảng Kê Công Việc & Nghiệm Thu
                </h5>
                <p class="text-muted fs-8 mb-0">Quản lý chi tiết tiến độ, đầu ra nghiệm thu và đánh giá chất lượng từng nhiệm vụ.</p>
            </div>
            <div class="d-flex align-items-center gap-2">
                <span class="badge bg-light text-secondary border rounded-pill px-3 py-2 fs-8" id="tasksVisibleCount">
                    Hiển thị: ${tasks.size()} / ${tasks.size()} công việc
                </span>
            </div>
        </div>

        <!-- Interactive Filter & Search Controls (Segmented Capsule) -->
        <div class="no-print report-filter-bar d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
            <!-- Filter Capsule Track -->
            <div class="segmented-capsule-track" id="taskFilterTabs">
                <button type="button" class="filter-tab-btn active" onclick="filterTasks('ALL', this)">
                    Tất cả <span class="badge">${totalTasks}</span>
                </button>
                <button type="button" class="filter-tab-btn" onclick="filterTasks('TODO', this)">
                    Cần làm <span class="badge">${todoCount}</span>
                </button>
                <button type="button" class="filter-tab-btn" onclick="filterTasks('IN_PROGRESS', this)">
                    Đang làm <span class="badge">${inProgressCount}</span>
                </button>
                <button type="button" class="filter-tab-btn" onclick="filterTasks('PENDING_REVIEW', this)">
                    Chờ duyệt <span class="badge">${submittedCount + planningCount}</span>
                </button>
                <c:if test="${reviseCount + rejectedCount > 0}">
                    <button type="button" class="filter-tab-btn" onclick="filterTasks('REVISE_OR_REJECT', this)">
                        Chỉnh sửa <span class="badge">${reviseCount + rejectedCount}</span>
                    </button>
                </c:if>
                <button type="button" class="filter-tab-btn" onclick="filterTasks('DONE', this)">
                    Đã xong <span class="badge">${doneCount}</span>
                </button>
                <c:if test="${overdueCount > 0}">
                    <button type="button" class="filter-tab-btn text-danger" onclick="filterTasks('OVERDUE', this)">
                        <i class="bi bi-exclamation-circle-fill"></i> Quá hạn <span class="badge bg-danger text-white">${overdueCount}</span>
                    </button>
                </c:if>
            </div>

            <!-- Search Input -->
            <div class="report-search-wrap">
                <i class="bi bi-search report-search-icon"></i>
                <input type="text" class="form-control form-control-sm report-search-input" id="taskSearchInput"
                    placeholder="Tìm kiếm công việc..." oninput="handleTaskSearch()"
                    aria-label="Tìm kiếm công việc theo tên, người phụ trách hoặc mã số">
            </div>
        </div>

        <div class="table-responsive">
            <table class="table table-hover report-table mb-0 align-middle" id="tasksInventoryTable">
                <thead>
                    <tr>
                        <th class="col-w-id">Mã</th>
                        <th>Tên Công Việc</th>
                        <th>Người Phụ Trách</th>
                        <th class="text-center">Mức Ưu Tiên</th>
                        <th class="text-center">Hạn Chót</th>
                        <th class="text-center">Trạng Thái</th>
                        <th class="text-center">Chất Lượng</th>
                        <th>Ghi Chú Bàn Giao / Đánh Giá</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${tasks}" var="t">
                        <tr class="task-inventory-row" data-status="${t.status}" data-overdue="${t.isOverdue()}"
                            data-search="#${t.id} ${t.title.toLowerCase()} ${not empty t.assigneeName ? t.assigneeName.toLowerCase() : ''} ${t.priority.toLowerCase()}">
                            <td class="text-muted fw-bold">#${t.id}</td>
                            <td>
                                <div class="fw-bold text-dark">${t.title}</div>
                                <c:if test="${not empty t.description}">
                                    <div class="text-muted fs-9 text-truncate report-desc-truncate" title="${t.description}">
                                        ${t.description}
                                    </div>
                                </c:if>
                            </td>
                            <td>
                                <div class="d-flex align-items-center gap-1">
                                    <i class="bi bi-person text-secondary"></i>
                                    <span class="fs-8 fw-medium">${not empty t.assigneeName ? t.assigneeName : 'Chưa phân công'}</span>
                                </div>
                            </td>
                            <td class="text-center">
                                <span class="badge ${t.priorityBadgeClass} rounded-pill px-2 py-1 fs-9">
                                    ${t.priorityLabel}
                                </span>
                            </td>
                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${not empty t.dueDate}">
                                        <div class="fs-9 fw-semibold">${t.dueDate}</div>
                                        <c:choose>
                                            <c:when test="${t.isOverdue()}">
                                                <span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill fs-9 mt-1">
                                                    <i class="bi bi-clock-history me-1"></i> Trễ hạn
                                                </span>
                                            </c:when>
                                            <c:when test="${t.status == 'DONE' || t.status == 'APPROVED'}">
                                                <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill fs-9 mt-1">
                                                    <i class="bi bi-check me-1"></i> Xong
                                                </span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge bg-light text-muted border rounded-pill fs-9 mt-1">Đúng hạn</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="text-muted fs-9">Chưa đặt</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-center">
                                <!-- Linear-Style Status Dot Indicator -->
                                <c:choose>
                                    <c:when test="${t.status == 'DONE' || t.status == 'APPROVED'}">
                                        <span class="status-dot-indicator status-dot-done">
                                            <span class="status-dot"></span>
                                            <span>${t.statusLabel}</span>
                                        </span>
                                    </c:when>
                                    <c:when test="${t.status == 'IN_PROGRESS'}">
                                        <span class="status-dot-indicator status-dot-in-progress">
                                            <span class="status-dot"></span>
                                            <span>${t.statusLabel}</span>
                                        </span>
                                    </c:when>
                                    <c:when test="${t.status == 'SUBMITTED' || t.status == 'PLANNING'}">
                                        <span class="status-dot-indicator status-dot-pending">
                                            <span class="status-dot"></span>
                                            <span>${t.statusLabel}</span>
                                        </span>
                                    </c:when>
                                    <c:when test="${t.status == 'REVISE' || t.status == 'REJECTED'}">
                                        <span class="status-dot-indicator status-dot-danger">
                                            <span class="status-dot"></span>
                                            <span>${t.statusLabel}</span>
                                        </span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="status-dot-indicator status-dot-todo">
                                            <span class="status-dot"></span>
                                            <span>${t.statusLabel}</span>
                                        </span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-center">
                                <c:choose>
                                    <c:when test="${t.status == 'DONE' || t.status == 'APPROVED'}">
                                        <span class="text-warning fw-bold fs-8">
                                            <i class="bi bi-star-fill"></i> ${t.qualityRating}/5
                                        </span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="text-muted fs-9">—</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:if test="${not empty t.finalDeliverableNote}">
                                    <div class="fs-9 text-dark"><strong>Nộp:</strong> ${t.finalDeliverableNote}</div>
                                </c:if>
                                <c:if test="${not empty t.deliverableFile}">
                                    <div class="fs-9 text-primary mt-1">
                                        <i class="bi bi-link-45deg"></i> <a href="${t.deliverableFile}" target="_blank" class="text-decoration-none">${t.deliverableFile}</a>
                                    </div>
                                </c:if>
                                <c:if test="${not empty t.pmFeedback}">
                                    <div class="fs-9 text-muted mt-1"><em>PM: "${t.pmFeedback}"</em></div>
                                </c:if>
                                <c:if test="${empty t.finalDeliverableNote && empty t.deliverableFile && empty t.pmFeedback}">
                                    <span class="text-muted fs-9">—</span>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    <tr id="noMatchingTasksRow" class="d-none">
                        <td colspan="8" class="text-center py-5 text-muted">
                            <i class="bi bi-search fs-3 d-block mb-2 text-secondary"></i>
                            <div class="fw-semibold">Không tìm thấy công việc nào khớp với bộ lọc hoặc từ khóa tìm kiếm.</div>
                            <button type="button" onclick="filterTasks('ALL'); document.getElementById('taskSearchInput').value=''; handleTaskSearch();"
                                class="btn btn-outline-secondary btn-sm rounded-pill px-3 mt-2">
                                Đặt lại bộ lọc
                            </button>
                        </td>
                    </tr>
                    <c:if test="${empty tasks}">
                        <tr>
                            <td colspan="8" class="text-center py-5">
                                <div class="report-empty-state">
                                    <i class="bi bi-clipboard-check empty-icon d-block"></i>
                                    <h6 class="fw-bold text-dark mb-1">Dự án chưa có công việc nào</h6>
                                    <p class="text-muted fs-8 mb-3">Hãy tạo các nhiệm vụ trên bảng Kanban để bắt đầu theo dõi tiến độ dự án.</p>
                                    <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}" class="btn btn-primary-custom text-white btn-sm rounded-pill px-3 no-print">
                                        <i class="bi bi-plus-lg me-1"></i> Tạo công việc mới
                                    </a>
                                </div>
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>

    <!-- =========================================================================
    8. TÓM TẮT TÀI LIỆU WIKI & HOẠT ĐỘNG TRAO ĐỔI (DOCS & DISCUSSIONS)
    ========================================================================= -->
    <div class="row g-4 mb-4 avoid-break">
        <!-- Wiki Docs -->
        <div class="col-12 col-lg-6">
            <div class="bg-white p-4 rounded-3 shadow-sm border h-100">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h6 class="fw-bold text-dark fs-7 mb-0 d-flex align-items-center gap-2">
                        <i class="bi bi-journal-text text-primary"></i> Tài Liệu Kỹ Thuật & Wiki (${docCount})
                    </h6>
                    <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}" class="no-print fs-9 text-decoration-none">Xem tất cả &rarr;</a>
                </div>
                <c:choose>
                    <c:when test="${not empty docs}">
                        <ul class="list-group list-group-flush fs-8">
                            <c:forEach items="${docs}" var="d" end="4">
                                <li class="list-group-item px-0 py-2 d-flex justify-content-between align-items-center">
                                    <div class="d-flex align-items-center gap-2">
                                        <i class="bi bi-file-earmark-text text-muted"></i>
                                        <span class="fw-semibold text-dark">${d.title}</span>
                                    </div>
                                    <span class="fs-9 text-muted">${d.authorName} &bull; ${d.updatedAt}</span>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:when>
                    <c:otherwise>
                        <div class="py-4 text-center text-muted">
                            <i class="bi bi-journal-plus fs-3 text-secondary mb-1 d-block"></i>
                            <div class="fs-8">Chưa có bài viết Wiki nào được tạo trong dự án.</div>
                            <a href="${pageContext.request.contextPath}/doc?action=create&projectId=${project.id}" class="btn btn-outline-secondary btn-sm rounded-pill px-3 mt-2 no-print fs-9">
                                <i class="bi bi-plus-circle me-1"></i> Soạn thảo tài liệu
                            </a>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Discussions -->
        <div class="col-12 col-lg-6">
            <div class="bg-white p-4 rounded-3 shadow-sm border h-100">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h6 class="fw-bold text-dark fs-7 mb-0 d-flex align-items-center gap-2">
                        <i class="bi bi-chat-dots-fill text-info"></i> Tương Tác & Thảo Luận Nhóm
                    </h6>
                    <a href="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}" class="no-print fs-9 text-decoration-none">Vào phòng chat &rarr;</a>
                </div>
                <div class="p-3 bg-light rounded-3 mb-3">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                        <span class="fs-8 text-muted">Tổng số tin nhắn trao đổi:</span>
                        <span class="fw-bold fs-7 text-dark">${messageCount} lượt</span>
                    </div>
                    <div class="d-flex align-items-center justify-content-between">
                        <span class="fs-8 text-muted">Mức độ tương tác nhóm:</span>
                        <c:choose>
                            <c:when test="${messageCount > 20}">
                                <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill fs-9 d-flex align-items-center gap-1">
                                    <i class="bi bi-activity"></i> Rất sôi nổi
                                </span>
                            </c:when>
                            <c:when test="${messageCount > 5}">
                                <span class="badge bg-info-subtle text-info-emphasis border border-info-subtle rounded-pill fs-9 d-flex align-items-center gap-1">
                                    <i class="bi bi-chat-heart"></i> Tích cực
                                </span>
                            </c:when>
                            <c:otherwise>
                                <span class="badge bg-light text-muted border rounded-pill fs-9">Bình thường</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
                <p class="text-muted fs-8 mb-0">
                    Thành viên có thể trực tiếp thảo luận, chia sẻ tài liệu và phản hồi công việc trong luồng trao đổi của dự án.
                </p>
            </div>
        </div>
    </div>

    <!-- =========================================================================
    9. XÁC NHẬN CỦA TRƯỞNG DỰ ÁN (PHỤC VỤ IN ẤN & LƯU TRỮ)
    ========================================================================= -->
    <div class="d-flex justify-content-end mt-4 pt-3 avoid-break">
        <div class="col-12 col-sm-6 col-md-4">
            <div class="signature-box">
                <div class="fw-bold text-dark fs-8 text-uppercase tracking-wider">TRƯỞNG DỰ ÁN (PROJECT MANAGER)</div>
                <div class="text-muted fs-9 mb-1">(Ký và ghi rõ họ tên)</div>
                <div class="signature-line"></div>
                <c:set var="projectOwnerName" value="" />
                <c:forEach items="${members}" var="m">
                    <c:if test="${m.projectRole == 'OWNER'}">
                        <c:set var="projectOwnerName" value="${m.userName}" />
                    </c:if>
                </c:forEach>
                <div class="fw-bold text-dark fs-7">
                    <c:choose>
                        <c:when test="${not empty projectOwnerName}">
                            ${projectOwnerName}
                        </c:when>
                        <c:when test="${project.ownerId == sessionScope.currentUser.id}">
                            ${sessionScope.currentUser.fullName}
                        </c:when>
                        <c:otherwise>
                            ${sessionScope.currentUser.fullName}
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>

</div>

<!-- =========================================================================
JAVASCRIPT TIỆN ÍCH: BỘ LỌC TƯƠNG TÁC + TÌM KIẾM + ĐỒNG BỘ KPI + THEME TOGGLE
========================================================================= -->
<script>
    // 1. Quản lý trạng thái bộ lọc Task (Interactive Task Filter & Search)
    let currentStatusFilter = 'ALL';
    let currentSearchQuery = '';

    function filterTasks(filterType, btnElement) {
        currentStatusFilter = filterType;

        // Cập nhật trạng thái active của tab buttons
        const buttons = document.querySelectorAll('#taskFilterTabs .filter-tab-btn');
        buttons.forEach(b => b.classList.remove('active'));
        if (btnElement) {
            btnElement.classList.add('active');
        } else {
            // Tự tìm button theo filterType nếu được gọi từ KPI card
            buttons.forEach(b => {
                const onclickAttr = b.getAttribute('onclick') || '';
                if (onclickAttr.includes("'" + filterType + "'")) {
                    b.classList.add('active');
                }
            });
        }

        syncKpiCardState(filterType);
        applyTaskFilters();
    }

    function handleKpiCardClick(filterType) {
        filterTasks(filterType);
        scrollToTasks();
    }

    function syncKpiCardState(filterType) {
        const kpiCards = document.querySelectorAll('.report-stat-card');
        kpiCards.forEach(c => c.classList.remove('active'));

        const map = {
            'ALL': 'kpiCardAll',
            'DONE': 'kpiCardDone',
            'IN_PROGRESS': 'kpiCardInProgress',
            'PENDING_REVIEW': 'kpiCardPending',
            'OVERDUE': 'kpiCardOverdue'
        };

        if (map[filterType]) {
            const card = document.getElementById(map[filterType]);
            if (card) card.classList.add('active');
        }
    }

    function scrollToTasks() {
        const tableSection = document.getElementById('tasksInventorySection');
        if (tableSection) {
            tableSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }

    function handleTaskSearch() {
        const input = document.getElementById('taskSearchInput');
        currentSearchQuery = input ? input.value.trim().toLowerCase() : '';
        applyTaskFilters();
    }

    function applyTaskFilters() {
        const rows = document.querySelectorAll('.task-inventory-row');
        let visibleCount = 0;

        rows.forEach(row => {
            const status = row.getAttribute('data-status') || '';
            const isOverdue = row.getAttribute('data-overdue') === 'true';
            const searchTarget = (row.getAttribute('data-search') || '').toLowerCase();

            // Khớp trạng thái (Status match)
            let matchesStatus = false;
            if (currentStatusFilter === 'ALL') {
                matchesStatus = true;
            } else if (currentStatusFilter === 'TODO') {
                matchesStatus = (status === 'TODO' || status === 'PLANNING_REJECTED' || status === 'CREATED' || status === '');
            } else if (currentStatusFilter === 'IN_PROGRESS') {
                matchesStatus = (status === 'IN_PROGRESS');
            } else if (currentStatusFilter === 'PENDING_REVIEW') {
                matchesStatus = (status === 'SUBMITTED' || status === 'PLANNING');
            } else if (currentStatusFilter === 'REVISE_OR_REJECT') {
                matchesStatus = (status === 'REVISE' || status === 'REJECTED');
            } else if (currentStatusFilter === 'DONE') {
                matchesStatus = (status === 'DONE' || status === 'APPROVED');
            } else if (currentStatusFilter === 'OVERDUE') {
                matchesStatus = isOverdue;
            }

            // Khớp từ khóa tìm kiếm (Search match)
            let matchesSearch = true;
            if (currentSearchQuery.length > 0) {
                matchesSearch = searchTarget.includes(currentSearchQuery);
            }

            if (matchesStatus && matchesSearch) {
                row.classList.remove('d-none');
                visibleCount++;
            } else {
                row.classList.add('d-none');
            }
        });

        // Cập nhật số lượng công việc hiển thị
        const countBadge = document.getElementById('tasksVisibleCount');
        if (countBadge) {
            countBadge.innerText = 'Hiển thị: ' + visibleCount + ' / ' + rows.length + ' công việc';
        }

        // Hiển thị dòng empty state nếu không tìm thấy kết quả
        const noMatchRow = document.getElementById('noMatchingTasksRow');
        if (noMatchRow) {
            if (visibleCount === 0 && rows.length > 0) {
                noMatchRow.classList.remove('d-none');
            } else {
                noMatchRow.classList.add('d-none');
            }
        }
    }

    // 2. Theme Management (Light / Dark Mode Switcher)
    function toggleReportTheme() {
        if (typeof toggleGlobalTheme === 'function') {
            toggleGlobalTheme();
        } else {
            var current = document.documentElement.getAttribute('data-theme') || 'light';
            var next = (current === 'dark') ? 'light' : 'dark';
            document.documentElement.setAttribute('data-theme', next);
            document.documentElement.setAttribute('data-bs-theme', next);
            try {
                localStorage.setItem('teamwork_theme', next);
                localStorage.setItem('teamwork_report_theme', next);
            } catch (e) {}
            var btnText = document.getElementById('themeBtnText');
            if (btnText) {
                btnText.innerText = (next === 'dark') ? 'Chế độ sáng' : 'Chế độ tối';
            }
        }
    }
</script>

<jsp:include page="/includes/footer.jsp" />