<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <c:set var="pageTitle" value="Báo Cáo Tiến Độ Dự Án — ${project.name}" />
        <c:set var="extraCss" value="styles/report.css" />

        <jsp:include page="/includes/header.jsp" />
        <jsp:include page="/includes/navbar.jsp" />

        <div class="container-fluid px-lg-5 py-4">

            <!-- 1. THANH ĐIỀU HƯỚNG & NÚT THAO TÁC (ẨN KHI IN) -->
            <div
                class="no-print d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4 pb-3 border-bottom bg-white p-3 rounded-4 shadow-sm">
                <div class="d-flex align-items-center gap-3">
                    <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}"
                        class="btn btn-outline-secondary btn-sm rounded-pill px-3 shadow-none"
                        title="Quay về bảng Kanban">
                        <i class="bi bi-arrow-left me-1"></i> Bảng Kanban
                    </a>
                    <div class="border-start ps-3 d-flex align-items-center gap-2">
                        <span class="badge bg-dark-navy text-white rounded-pill px-2 py-1 fs-9">
                            <i class="bi bi-hash"></i> ${project.projectCode}
                        </span>
                        <span class="fw-bold text-dark fs-7">${project.name}</span>
                    </div>

                    <!-- Tab Chuyển Phân Hệ Nhanh -->
                    <div class="d-none d-md-flex align-items-center gap-2 bg-light p-1 rounded-pill border ms-2">
                        <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}"
                            class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8">
                            <i class="bi bi-kanban me-1"></i> Kanban
                        </a>
                        <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}"
                            class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8">
                            <i class="bi bi-journal-text me-1"></i> Tài liệu
                        </a>
                        <a href="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}"
                            class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8">
                            <i class="bi bi-chat-dots me-1"></i> Thảo luận
                        </a>
                        <a href="${pageContext.request.contextPath}/project?action=report&projectId=${project.id}"
                            class="btn btn-sm btn-white bg-white text-primary shadow-2xs rounded-pill px-3 py-1 fw-bold fs-8">
                            <i class="bi bi-file-earmark-bar-graph me-1"></i> Báo cáo
                        </a>
                    </div>
                </div>

                <!-- Cụm Nút Thao Tác Xuất / In -->
                <div class="d-flex align-items-center gap-2">
                    <button type="button" onclick="window.print()"
                        class="btn btn-primary-custom btn-sm rounded-pill px-3 py-2 fw-semibold shadow-sm fs-8 d-flex align-items-center gap-2 text-white">
                        <i class="bi bi-printer-fill"></i> In Báo Cáo / Lưu PDF
                    </button>
                    <button type="button" onclick="exportTasksToCSV()"
                        class="btn btn-outline-success btn-sm rounded-pill px-3 py-2 fw-semibold shadow-sm fs-8 d-flex align-items-center gap-2">
                        <i class="bi bi-file-earmark-spreadsheet-fill"></i> Xuất CSV
                    </button>
                    <a href="${pageContext.request.contextPath}/project?action=list"
                        class="btn btn-outline-secondary btn-sm rounded-pill px-3 py-2 fw-semibold fs-8"
                        title="Về danh sách dự án">
                        <i class="bi bi-grid-fill me-1"></i> Dashboard
                    </a>
                </div>
            </div>

            <!-- Thông báo Flash Toast (nếu có) -->
            <jsp:include page="/includes/toast.jsp" />

            <!-- =========================================================================
            2. REPORT HEADER BANNER: TIÊU ĐỀ BÁO CÁO & THÔNG TIN DỰ ÁN
            ========================================================================= -->
            <div class="report-header-badge mb-4">
                <div class="d-flex flex-wrap justify-content-between align-items-start gap-3">
                    <div>
                        <div class="d-flex align-items-center gap-2 mb-2">
                            <span class="badge bg-primary text-white rounded-pill px-3 py-1 fs-9">
                                <i class="bi bi-file-earmark-bar-graph-fill me-1"></i> BÁO CÁO TIẾN ĐỘ DỰ ÁN
                            </span>
                            <span
                                class="badge bg-secondary-subtle text-white border border-secondary rounded-pill px-2 py-1 fs-9">
                                Mã: #${project.projectCode}
                            </span>
                        </div>
                        <h2 class="fw-extrabold mb-1 text-white tracking-tight">${project.name}</h2>
                        <p class="text-white-50 fs-8 mb-0 report-project-desc">
                            ${not empty project.description ? project.description : 'Dự án chưa cập nhật mô tả chi
                            tiết.'}
                        </p>
                    </div>

                    <!-- Metadata Khung Phải -->
                    <div class="text-md-end text-white-50 fs-8">
                        <div><strong>Ngày khởi tạo:</strong> <span class="text-white">${project.createdAt}</span></div>
                        <div><strong>Thời điểm xuất báo cáo:</strong> <span class="text-white">${generatedAt}</span>
                        </div>
                        <div><strong>Người xuất báo cáo:</strong> <span
                                class="text-white">${sessionScope.currentUser.fullName}
                                (${sessionScope.currentUser.role})</span></div>
                    </div>
                </div>
            </div>

            <!-- =========================================================================
            3. EXECUTIVE SUMMARY CARDS: CÁC CHỈ SỐ TIẾN ĐỘ QUAN TRỌNG NHẤT
            ========================================================================= -->
            <div class="row g-3 mb-4 avoid-break">
                <!-- Card 1: Tổng số công việc -->
                <div class="col-6 col-md-4 col-xl">
                    <div class="report-stat-card h-100">
                        <div class="d-flex align-items-center justify-content-between mb-1">
                            <span class="report-card-title">Tổng Công Việc</span>
                            <i class="bi bi-list-task text-primary fs-5"></i>
                        </div>
                        <div class="report-stat-number text-dark">${totalTasks}</div>
                        <div class="fs-9 text-muted mt-1">
                            <i class="bi bi-people-fill me-1"></i> ${memberCount} thành viên
                        </div>
                    </div>
                </div>

                <!-- Card 2: Đã hoàn thành & Tỷ lệ -->
                <div class="col-6 col-md-4 col-xl">
                    <div class="report-stat-card h-100 border-success-subtle">
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
                    <div class="report-stat-card h-100 border-info-subtle">
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
                    <div class="report-stat-card h-100 border-warning-subtle">
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
                    <div
                        class="report-stat-card h-100 ${overdueCount > 0 ? 'border-danger bg-danger-subtle bg-opacity-10' : ''}">
                        <div class="d-flex align-items-center justify-content-between mb-1">
                            <span class="report-card-title text-danger">Bị Quá Hạn</span>
                            <i class="bi bi-exclamation-triangle-fill text-danger fs-5"></i>
                        </div>
                        <div class="report-stat-number text-danger">${overdueCount}</div>
                        <div class="fs-9 text-danger fw-bold mt-1">
                            <c:choose>
                                <c:when test="${overdueCount > 0}">Cần xử lý gấp!</c:when>
                                <c:otherwise><span class="text-success"><i class="bi bi-shield-check me-1"></i>Tiến độ
                                        an toàn</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </div>

            <!-- =========================================================================
            4. TIẾN ĐỘ TỔNG THỂ & PHÂN BỐ CÔNG VIỆC
            ========================================================================= -->
            <div class="row g-4 mb-4 avoid-break">
                <!-- Cột Trái: Thanh tiến độ tổng thể -->
                <div class="col-12 col-lg-7">
                    <div class="bg-white p-4 rounded-4 shadow-sm border h-100">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <h6 class="fw-bold text-dark fs-7 mb-0 d-flex align-items-center gap-2">
                                <i class="bi bi-speedometer2 text-primary"></i> Tiến Độ Dự Án Tổng Thể
                            </h6>
                            <span class="badge bg-primary text-white rounded-pill px-3 py-1 fs-8 fw-bold">
                                ${progressPercentage}% Hoàn Tất
                            </span>
                        </div>
                        <p class="text-muted fs-8 mb-3">Đã nghiệm thu đạt chuẩn ${doneCount} trên tổng số ${totalTasks}
                            công việc được giao.</p>

                        <!-- Progress bar -->
                        <div class="progress progress-bar-custom mb-3">
                            <div class="progress-bar bg-success progress-bar-striped" role="progressbar"
                                style="width: ${progressPercentage}%;" aria-valuenow="${progressPercentage}"
                                aria-valuemin="0" aria-valuemax="100"></div>
                        </div>

                        <!-- Chi tiết trạng thái -->
                        <div class="d-flex flex-wrap gap-2 pt-2 border-top">
                            <span class="badge bg-light text-dark border fs-9"><i
                                    class="bi bi-circle-fill text-secondary me-1"></i> Cần làm: ${todoCount}</span>
                            <span class="badge bg-light text-dark border fs-9"><i
                                    class="bi bi-circle-fill text-primary me-1"></i> Chờ duyệt KH:
                                ${planningCount}</span>
                            <span class="badge bg-light text-dark border fs-9"><i
                                    class="bi bi-circle-fill text-info me-1"></i> Đang làm: ${inProgressCount}</span>
                            <span class="badge bg-light text-dark border fs-9"><i
                                    class="bi bi-circle-fill text-warning me-1"></i> Chờ nghiệm thu:
                                ${submittedCount}</span>
                            <span class="badge bg-light text-dark border fs-9"><i
                                    class="bi bi-circle-fill text-primary me-1"></i> Cần cân chỉnh:
                                ${reviseCount}</span>
                            <span class="badge bg-light text-dark border fs-9"><i
                                    class="bi bi-circle-fill text-danger me-1"></i> Chưa đạt: ${rejectedCount}</span>
                            <span class="badge bg-light text-dark border fs-9"><i
                                    class="bi bi-circle-fill text-success me-1"></i> Đã nghiệm thu: ${doneCount}</span>
                        </div>
                    </div>
                </div>

                <!-- Cột Phải: Phân bổ mức độ ưu tiên -->
                <div class="col-12 col-lg-5">
                    <div class="bg-white p-4 rounded-4 shadow-sm border h-100">
                        <h6 class="fw-bold text-dark fs-7 mb-2 d-flex align-items-center gap-2">
                            <i class="bi bi-flag-fill text-warning"></i> Phân Bổ Mức Độ Ưu Tiên
                        </h6>
                        <p class="text-muted fs-8 mb-3">Tỷ lệ công việc theo mức độ quan trọng trong dự án.</p>

                        <div class="d-flex flex-column gap-2">
                            <!-- High Priority -->
                            <div>
                                <div class="d-flex justify-content-between align-items-center fs-8 mb-1">
                                    <span class="fw-semibold text-danger"><i
                                            class="bi bi-exclamation-circle-fill me-1"></i> Ưu tiên Cao (HIGH)</span>
                                    <span class="fw-bold">${highPriorityCount} việc</span>
                                </div>
                                <div class="progress progress-mini">
                                    <div class="progress-bar bg-danger"
                                        style="width: ${totalTasks > 0 ? (highPriorityCount * 100 / totalTasks) : 0}%;">
                                    </div>
                                </div>
                            </div>

                            <!-- Medium Priority -->
                            <div>
                                <div class="d-flex justify-content-between align-items-center fs-8 mb-1">
                                    <span class="fw-semibold text-warning-emphasis"><i
                                            class="bi bi-dash-circle-fill me-1"></i> Ưu tiên Trung bình (MEDIUM)</span>
                                    <span class="fw-bold">${mediumPriorityCount} việc</span>
                                </div>
                                <div class="progress progress-mini">
                                    <div class="progress-bar bg-warning"
                                        style="width: ${totalTasks > 0 ? (mediumPriorityCount * 100 / totalTasks) : 0}%;">
                                    </div>
                                </div>
                            </div>

                            <!-- Low Priority -->
                            <div>
                                <div class="d-flex justify-content-between align-items-center fs-8 mb-1">
                                    <span class="fw-semibold text-info-emphasis"><i
                                            class="bi bi-arrow-down-circle-fill me-1"></i> Ưu tiên Thấp (LOW)</span>
                                    <span class="fw-bold">${lowPriorityCount} việc</span>
                                </div>
                                <div class="progress progress-mini">
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
            5. BẢNG ĐÓNG GÓP & HIỆU SUẤT CỦA THÀNH VIÊN (MEMBER PERFORMANCE)
            ========================================================================= -->
            <div class="bg-white p-4 rounded-4 shadow-sm border mb-4 avoid-break">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <div>
                        <h5 class="fw-bold text-dark fs-6 mb-1 d-flex align-items-center gap-2">
                            <i class="bi bi-people-fill text-primary"></i> Đóng Góp & Năng Suất Thành Viên
                        </h5>
                        <p class="text-muted fs-8 mb-0">Thống kê khối lượng công việc, tỷ lệ hoàn thành và điểm chất
                            lượng bàn giao theo từng người.</p>
                    </div>
                    <span class="badge bg-light text-secondary border rounded-pill px-3 py-2 fs-8">
                        Tổng cộng: ${memberCount} thành viên
                    </span>
                </div>

                <div class="table-responsive">
                    <table class="table table-hover report-table mb-0 align-middle">
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
                                                ${stat.member.userName.substring(0, 1)}
                                            </div>
                                            <div>
                                                <div class="fw-bold text-dark">${stat.member.userName}</div>
                                                <div class="text-muted fs-9">${stat.member.userEmail}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <div><span
                                                class="badge bg-light text-dark border fs-9">${stat.member.userRole}</span>
                                        </div>
                                        <c:choose>
                                            <c:when test="${stat.member.projectRole == 'OWNER'}">
                                                <span
                                                    class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle rounded-pill fs-9 mt-1">
                                                    <i class="bi bi-star-fill me-1"></i> Trưởng dự án (PM)
                                                </span>
                                            </c:when>
                                            <c:otherwise>
                                                <span
                                                    class="badge bg-secondary-subtle text-secondary rounded-pill fs-9 mt-1">
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
                                                <span
                                                    class="badge bg-danger text-white rounded-pill px-2 fs-9">${stat.overdueCount}</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-muted fs-9">0</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-center">
                                        <div class="d-flex align-items-center gap-2">
                                            <div class="progress flex-grow-1 progress-mini">
                                                <div class="progress-bar bg-success"
                                                    style="width: ${stat.completionRate}%;"></div>
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
                                    <td colspan="9" class="text-center py-3 text-muted">Dự án chưa có thành viên nào.
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- =========================================================================
            6. DANH SÁCH CHI TIẾT TẤT CẢ CÔNG VIỆC (DETAILED TASKS INVENTORY)
            ========================================================================= -->
            <div class="bg-white p-4 rounded-4 shadow-sm border mb-4 avoid-break">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <div>
                        <h5 class="fw-bold text-dark fs-6 mb-1 d-flex align-items-center gap-2">
                            <i class="bi bi-card-checklist text-primary"></i> Danh Sách Chi Tiết Công Việc
                        </h5>
                        <p class="text-muted fs-8 mb-0">Bảng kê chi tiết trạng thái, người phụ trách, hạn chót và kết
                            quả nghiệm thu.</p>
                    </div>
                    <span class="badge bg-light text-secondary border rounded-pill px-3 py-2 fs-8">
                        ${tasks.size()} công việc
                    </span>
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
                                <tr>
                                    <td class="text-muted fw-bold">#${t.id}</td>
                                    <td>
                                        <div class="fw-bold text-dark">${t.title}</div>
                                        <c:if test="${not empty t.description}">
                                            <div class="text-muted fs-9 text-truncate report-desc-truncate"
                                                title="${t.description}">
                                                ${t.description}
                                            </div>
                                        </c:if>
                                    </td>
                                    <td>
                                        <div class="d-flex align-items-center gap-1">
                                            <i class="bi bi-person text-secondary"></i>
                                            <span class="fs-8 fw-medium">${not empty t.assigneeName ? t.assigneeName :
                                                'Chưa phân công'}</span>
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
                                                        <span
                                                            class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill fs-9 mt-1">
                                                            <i class="bi bi-clock-history me-1"></i> Trễ hạn
                                                        </span>
                                                    </c:when>
                                                    <c:when test="${t.status == 'DONE' || t.status == 'APPROVED'}">
                                                        <span
                                                            class="badge bg-success-subtle text-success border border-success-subtle rounded-pill fs-9 mt-1">
                                                            <i class="bi bi-check me-1"></i> Xong
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span
                                                            class="badge bg-light text-muted border rounded-pill fs-9 mt-1">Đúng
                                                            hạn</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-muted fs-9">Chưa đặt</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-center">
                                        <span class="badge ${t.statusBadgeClass} rounded-pill px-2 py-1 fs-9">
                                            ${t.statusLabel}
                                        </span>
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
                                            <div class="fs-9 text-dark"><strong>Nộp:</strong> ${t.finalDeliverableNote}
                                            </div>
                                        </c:if>
                                        <c:if test="${not empty t.deliverableFile}">
                                            <div class="fs-9 text-primary mt-1">
                                                <i class="bi bi-link-45deg"></i> <a href="${t.deliverableFile}"
                                                    target="_blank"
                                                    class="text-decoration-none">${t.deliverableFile}</a>
                                            </div>
                                        </c:if>
                                        <c:if test="${not empty t.pmFeedback}">
                                            <div class="fs-9 text-muted mt-1"><em>PM: "${t.pmFeedback}"</em></div>
                                        </c:if>
                                        <c:if
                                            test="${empty t.finalDeliverableNote && empty t.deliverableFile && empty t.pmFeedback}">
                                            <span class="text-muted fs-9">—</span>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty tasks}">
                                <tr>
                                    <td colspan="8" class="text-center py-4 text-muted">Dự án chưa có công việc nào.
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- =========================================================================
            7. TÓM TẮT TÀI LIỆU WIKI & HOẠT ĐỘNG TRAO ĐỔI (DOCS & DISCUSSIONS)
            ========================================================================= -->
            <div class="row g-4 mb-4 avoid-break">
                <!-- Wiki Docs -->
                <div class="col-12 col-lg-6">
                    <div class="bg-white p-4 rounded-4 shadow-sm border h-100">
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <h6 class="fw-bold text-dark fs-7 mb-0 d-flex align-items-center gap-2">
                                <i class="bi bi-journal-text text-primary"></i> Tài Liệu Kỹ Thuật & Wiki (${docCount})
                            </h6>
                            <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}"
                                class="no-print fs-9 text-decoration-none">Xem tất cả &rarr;</a>
                        </div>
                        <c:choose>
                            <c:when test="${not empty docs}">
                                <ul class="list-group list-group-flush fs-8">
                                    <c:forEach items="${docs}" var="d" end="4">
                                        <li
                                            class="list-group-item px-0 py-2 d-flex justify-content-between align-items-center">
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
                                <p class="text-muted fs-8 mb-0">Chưa có bài viết Wiki nào được tạo trong dự án.</p>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <!-- Discussions -->
                <div class="col-12 col-lg-6">
                    <div class="bg-white p-4 rounded-4 shadow-sm border h-100">
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <h6 class="fw-bold text-dark fs-7 mb-0 d-flex align-items-center gap-2">
                                <i class="bi bi-chat-dots-fill text-info"></i> Tương Tác & Thảo Luận Nhóm
                            </h6>
                            <a href="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}"
                                class="no-print fs-9 text-decoration-none">Vào phòng chat &rarr;</a>
                        </div>
                        <div class="p-3 bg-light rounded-3 mb-3">
                            <div class="d-flex align-items-center justify-content-between mb-2">
                                <span class="fs-8 text-muted">Tổng số tin nhắn & bình luận:</span>
                                <span class="fw-bold fs-7 text-dark">${messageCount} lượt trao đổi</span>
                            </div>
                            <div class="d-flex align-items-center justify-content-between">
                                <span class="fs-8 text-muted">Mức độ tương tác nhóm:</span>
                                <c:choose>
                                    <c:when test="${messageCount > 20}">
                                        <span
                                            class="badge bg-success-subtle text-success border border-success-subtle rounded-pill fs-9">Rất
                                            sôi nổi 🚀</span>
                                    </c:when>
                                    <c:when test="${messageCount > 5}">
                                        <span
                                            class="badge bg-info-subtle text-info-emphasis border border-info-subtle rounded-pill fs-9">Tích
                                            cực 👍</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge bg-light text-muted border rounded-pill fs-9">Bình
                                            thường</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                        <p class="text-muted fs-8 mb-0">
                            <i class="bi bi-info-circle me-1"></i> Hệ thống hỗ trợ liên kết thông minh: gắn thẻ
                            <code>#task-ID</code>, <code>#doc-ID</code> và nhắc tên <code>@thành_viên</code> trong luồng
                            thảo luận.
                        </p>
                    </div>
                </div>
            </div>

            <!-- =========================================================================
            8. KHUNG CHỮ KÝ XÁC NHẬN (PHỤC VỤ IN ẤN & NỘP BÁO CÁO)
            ========================================================================= -->
            <div class="row g-4 mt-4 pt-3 avoid-break">
                <div class="col-6">
                    <div class="signature-box">
                        <div class="fw-bold text-dark mb-1">TRƯỞNG DỰ ÁN (PROJECT MANAGER)</div>
                        <div class="text-muted fs-9 mb-5">(Ký và ghi rõ họ tên)</div>
                        <div class="fw-semibold text-dark">${project.ownerId == sessionScope.currentUser.id ?
                            sessionScope.currentUser.fullName : '...................................................'}
                        </div>
                    </div>
                </div>
                <div class="col-6">
                    <div class="signature-box">
                        <div class="fw-bold text-dark mb-1">GIẢNG VIÊN HƯỚNG DẪN / ĐÁNH GIÁ</div>
                        <div class="text-muted fs-9 mb-5">(Ký và ghi nhận xét đánh giá)</div>
                        <div class="fw-semibold text-dark">...................................................</div>
                    </div>
                </div>
            </div>

        </div>

        <!-- =========================================================================
        JAVASCRIPT TIỆN ÍCH: XUẤT CSV TIẾNG VIỆT CHUẨN UTF-8 (CÓ BOM)
        ========================================================================= -->
        <script>
            function exportTasksToCSV() {
                const table = document.getElementById("tasksInventoryTable");
                if (!table) return;

                let csvContent = "\uFEFF"; // UTF-8 Byte Order Mark (BOM) giúp Excel hiển thị tiếng Việt không bị lỗi font

                // Header CSV
                csvContent += "Mã Task,Tên Công Việc,Người Phụ Trách,Mức Ưu Tiên,Hạn Chót,Trạng Thái,Đánh Giá Sao,Ghi Chú Bàn Giao\n";

                // Đọc từng dòng dữ liệu trong tbody
                const rows = table.querySelectorAll("tbody tr");
                rows.forEach(row => {
                    const cols = row.querySelectorAll("td");
                    if (cols.length >= 8) {
                        const id = cols[0].innerText.replace('#', '').trim();
                        const title = '"' + cols[1].innerText.replace(/"/g, '""').replace(/\n/g, ' ').trim() + '"';
                        const assignee = '"' + cols[2].innerText.replace(/"/g, '""').trim() + '"';
                        const priority = '"' + cols[3].innerText.trim() + '"';
                        const dueDate = '"' + cols[4].innerText.replace(/\n/g, ' ').trim() + '"';
                        const status = '"' + cols[5].innerText.trim() + '"';
                        const rating = '"' + cols[6].innerText.trim() + '"';
                        const notes = '"' + cols[7].innerText.replace(/"/g, '""').replace(/\n/g, ' ').trim() + '"';

                        csvContent += [id, title, assignee, priority, dueDate, status, rating, notes].join(",") + "\n";
                    }
                });

                // Tạo blob và trigger download
                const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
                const link = document.createElement("a");
                const url = URL.createObjectURL(blob);
                const projectNameSafe = "${project.projectCode}".replace(/[^a-zA-Z0-9_-]/g, '_');
                link.setAttribute("href", url);
                link.setAttribute("download", "Bao-cao-tien-do-" + projectNameSafe + ".csv");
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
            }
        </script>

        <jsp:include page="/includes/footer.jsp" />