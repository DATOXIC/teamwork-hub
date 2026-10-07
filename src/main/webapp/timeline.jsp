<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<%-- =========================================================================
     [MVC VIEW SKELETON: INTERACTIVE GANTT TIMELINE & PROJECT ROADMAP]
     VI: Sơ đồ Gantt tiến độ công việc, lộ trình dự án và phân rã thời gian
     EN: Interactive Gantt chart, project timeline, and schedule management
     - Controller: com.teamwork.controllers.TimelineServlet (/timeline)
     - Model: com.teamwork.business.Project, com.teamwork.business.Task,
              com.teamwork.business.ProjectMember, com.teamwork.business.SubTask
     - Request Attributes: project, members, allTasks, taskProgressMap,
                           subtaskCountMap, completedSubtaskMap, totalTasks,
                           scheduledCount, unscheduledCount, doneCount,
                           inProgressCount, todoCount, overdueCount,
                           overallProgress, todayDate, tasksJson, isOwner
     ========================================================================= --%>

<%-- ◀ SERVLET: TimelineServlet → setAttribute("project") --%>
<c:set var="pageTitle" value="Lộ Trình & Sơ Đồ Gantt &bull; ${fn:escapeXml(project.name)}" scope="request" />
<c:set var="extraCss" value="styles/timeline.css" scope="request" />

<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/styles/timeline.css?v=<%= System.currentTimeMillis() %>">

<div class="container-fluid px-3 px-lg-4 py-3 timeline-shell">

    <!-- =========================================================================
         1. SUB-NAV: ĐIỀU HƯỚNG DỰ ÁN & CHUYỂN PHÂN HỆ (7 Phân hệ chuẩn hóa)
         ========================================================================= -->
    <c:set var="activeSubNav" value="timeline" scope="request" />
    <jsp:include page="/includes/project_subnav.jsp" />

    <!-- Thông báo nổi Toast -->
    <jsp:include page="/includes/toast.jsp" />

    <!-- =========================================================================
         2. BENTO SUMMARY KPI BAR: CHỈ SỐ NHỊP ĐỘ DỰ ÁN
         ========================================================================= -->
    <div class="row g-3 mb-3">
        <!-- KPI 1: Tổng công việc -->
        <div class="col-6 col-md-3">
            <div class="timeline-kpi-card">
                <div class="timeline-kpi-icon bg-primary-subtle text-primary">
                    <i class="bi bi-list-task"></i>
                </div>
                <div>
                    <div class="fs-9 text-muted fw-semibold text-uppercase tracking-wider">Tổng công việc</div>
                    <%-- ◀ SERVLET: TimelineServlet → setAttribute("totalTasks") --%>
                    <h4 class="fw-bold mb-0 text-dark">${totalTasks}</h4>
                    <%-- ◀ SERVLET: TimelineServlet → setAttribute("scheduledCount") --%>
                    <%-- ◀ SERVLET: TimelineServlet → setAttribute("unscheduledCount") --%>
                    <span class="fs-9 text-secondary">${scheduledCount} có hạn chót &bull; ${unscheduledCount} chưa xếp</span>
                </div>
            </div>
        </div>

        <!-- KPI 2: Đang thực hiện -->
        <div class="col-6 col-md-3">
            <div class="timeline-kpi-card">
                <div class="timeline-kpi-icon bg-info-subtle text-info">
                    <i class="bi bi-play-circle-fill"></i>
                </div>
                <div>
                    <div class="fs-9 text-muted fw-semibold text-uppercase tracking-wider">Đang triển khai</div>
                    <%-- ◀ SERVLET: TimelineServlet → setAttribute("inProgressCount") --%>
                    <h4 class="fw-bold mb-0 text-dark">${inProgressCount}</h4>
                    <%-- ◀ SERVLET: TimelineServlet → setAttribute("todoCount") --%>
                    <span class="fs-9 text-secondary">${todoCount} việc đang chờ</span>
                </div>
            </div>
        </div>

        <!-- KPI 3: Cảnh báo trễ hạn -->
        <div class="col-6 col-md-3">
            <div class="timeline-kpi-card">
                <%-- ◀ SERVLET: TimelineServlet → setAttribute("overdueCount") --%>
                <div class="timeline-kpi-icon ${overdueCount > 0 ? 'bg-danger-subtle text-danger' : 'bg-success-subtle text-success'}">
                    <i class="bi ${overdueCount > 0 ? 'bi-exclamation-triangle-fill' : 'bi-shield-check'}"></i>
                </div>
                <div>
                    <div class="fs-9 text-muted fw-semibold text-uppercase tracking-wider">Rủi ro tiến độ</div>
                    <h4 class="fw-bold mb-0 ${overdueCount > 0 ? 'text-danger' : 'text-success'}">${overdueCount}</h4>
                    <span class="fs-9 text-secondary">${overdueCount > 0 ? 'Cần xử lý khẩn cấp!' : 'Đúng hạn 100%'}</span>
                </div>
            </div>
        </div>

        <!-- KPI 4: Tỷ lệ hoàn thành -->
        <div class="col-6 col-md-3">
            <div class="timeline-kpi-card">
                <div class="timeline-kpi-icon bg-success-subtle text-success">
                    <i class="bi bi-trophy-fill"></i>
                </div>
                <div class="flex-grow-1">
                    <div class="d-flex justify-content-between align-items-center">
                        <span class="fs-9 text-muted fw-semibold text-uppercase tracking-wider">Hoàn thành</span>
                        <%-- ◀ SERVLET: TimelineServlet → setAttribute("overallProgress") --%>
                        <span class="fw-bold fs-8 text-success">${overallProgress}%</span>
                    </div>
                    <%-- ◀ SERVLET: TimelineServlet → setAttribute("doneCount") --%>
                    <h4 class="fw-bold mb-1 text-dark">${doneCount} <span class="fs-9 fw-normal text-muted">/ ${totalTasks} việc</span></h4>
                    <div class="progress" style="height: 5px;">
                        <div class="progress-bar bg-success" style="width: ${overallProgress}%;"></div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- =========================================================================
         3. THANH CÔNG CỤ ĐIỀU KHIỂN & BỘ LỌC TỨC THÌ (TOOLBAR)
         ========================================================================= -->
    <div class="timeline-toolbar d-flex flex-wrap align-items-center justify-content-between gap-3">
        
        <!-- Cụm Trái: Chuyển Zoom & Cuộn Hôm Nay -->
        <div class="d-flex flex-wrap align-items-center gap-2">
            <!-- Nút Zoom: Ngày / Tuần / Tháng -->
            <div class="timeline-zoom-pill">
                <button type="button" class="timeline-zoom-btn active" data-zoom="day">
                    <i class="bi bi-view-list me-1"></i> Ngày
                </button>
                <button type="button" class="timeline-zoom-btn" data-zoom="week">
                    <i class="bi bi-calendar3-week me-1"></i> Tuần
                </button>
                <button type="button" class="timeline-zoom-btn" data-zoom="month">
                    <i class="bi bi-calendar3 me-1"></i> Tháng
                </button>
            </div>

            <!-- Nút cuộn về Hôm nay -->
            <button type="button" id="btnScrollToday" class="btn btn-sm btn-outline-danger rounded-pill px-3 shadow-2xs d-inline-flex align-items-center gap-1" title="Cuộn nhanh tới ngày hôm nay (Phím T)">
                <i class="bi bi-record-circle-fill"></i> Hôm nay
            </button>
        </div>

        <!-- Cụm Giữa: Ô tìm kiếm và Bộ lọc tức thì -->
        <div class="d-flex flex-wrap align-items-center gap-2 flex-grow-1 justify-content-md-end">
            <!-- Ô tìm kiếm -->
            <div class="position-relative" style="min-width: 180px; max-width: 240px;">
                <i class="bi bi-search position-absolute top-50 start-0 translate-middle-y ms-2-5 text-muted fs-9"></i>
                <input type="text" id="timelineSearchInput" class="form-control form-control-sm rounded-pill ps-4 shadow-none fs-8"
                       placeholder="Tìm công việc hoặc người...">
            </div>

            <!-- Lọc theo Thành viên -->
            <select id="timelineMemberFilter" class="form-select form-select-sm rounded-pill shadow-none fs-8" style="width: auto;">
                <option value="ALL">Tất cả thành viên</option>
                <%-- ◀ SERVLET: TimelineServlet → setAttribute("members") --%>
                <c:forEach items="${members}" var="m">
                    <option value="${m.userId}">${fn:escapeXml(m.userName)}</option>
                </c:forEach>
            </select>

            <!-- Lọc theo Mức ưu tiên -->
            <select id="timelinePriorityFilter" class="form-select form-select-sm rounded-pill shadow-none fs-8" style="width: auto;">
                <option value="ALL">Mọi ưu tiên</option>
                <option value="HIGH">🔴 Khẩn cấp (High)</option>
                <option value="MEDIUM">🟡 Trung bình (Medium)</option>
                <option value="LOW">🟢 Thấp (Low)</option>
            </select>

            <!-- Lọc theo Trạng thái -->
            <select id="timelineStatusFilter" class="form-select form-select-sm rounded-pill shadow-none fs-8" style="width: auto;">
                <option value="ALL">Mọi trạng thái</option>
                <option value="OVERDUE">🚨 Quá hạn</option>
                <option value="TODO">⚪ Cần làm</option>
                <option value="IN_PROGRESS">🔵 Đang làm</option>
                <option value="SUBMITTED">🟠 Chờ nghiệm thu</option>
                <option value="DONE">🟢 Đã xong</option>
            </select>

            <!-- Nút Thêm công việc nhanh vào Lộ trình -->
            <button type="button" class="btn btn-sm btn-primary-custom rounded-pill px-3 shadow-2xs d-inline-flex align-items-center gap-1 text-white"
                    data-bs-toggle="modal" data-bs-target="#quickAddTaskModal" title="Tạo công việc có gắn mốc thời gian">
                <i class="bi bi-plus-circle-fill"></i> Thêm việc
            </button>
        </div>
    </div>

    <!-- =========================================================================
         4. SƠ ĐỒ GANTT TỔNG THỂ (SPLIT-PANE CONTAINER)
         ========================================================================= -->
    <div class="gantt-master-container">
        <div class="gantt-split-wrapper">
            
            <!-- 4.1. KHUNG TRÁI: DANH SÁCH CÂY CÔNG VIỆC -->
            <div class="gantt-left-pane">
                <div class="gantt-left-header">
                    <span>Công việc &amp; Phụ trách</span>
                    <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5 fs-9" id="filterMatchCount">
                        ${totalTasks} / ${totalTasks}
                    </span>
                </div>
                <div class="gantt-left-body" id="ganttLeftBody">
                    <!-- JavaScript sẽ tự động render các hàng task ở đây -->
                </div>
            </div>

            <!-- 4.2. KHUNG PHẢI: TRỤC THỜI GIAN & LƯỚI GANTT CANVAS -->
            <div class="gantt-right-pane" id="ganttRightPane">
                <!-- Header thời gian (Tháng & Ngày) -->
                <div class="gantt-timeline-header" id="ganttTimelineHeader">
                    <!-- JavaScript render -->
                </div>

                <!-- Lưới nền & Các thanh tiến độ Gantt Bar -->
                <div class="gantt-grid-canvas" id="ganttGridCanvas">
                    <!-- JavaScript render -->
                </div>
            </div>

        </div>
    </div>

    <!-- =========================================================================
         5. KHAY CÔNG VIỆC CHƯA XẾP HẠN CHÓT (UNSCHEDULED TASKS TRAY)
         ========================================================================= -->
    <c:if test="${unscheduledCount > 0}">
        <div class="gantt-unscheduled-tray">
            <div class="d-flex align-items-center justify-content-between mb-2">
                <div class="d-flex align-items-center gap-2">
                    <span class="badge bg-warning-subtle text-dark border border-warning-subtle rounded-pill px-2.5 py-1 fs-9 fw-semibold">
                        <i class="bi bi-clock-history me-1"></i> Chưa xếp lịch (${unscheduledCount})
                    </span>
                    <span class="fs-8 text-muted">Bấm vào bất kỳ thẻ nào để đặt hạn chót và đưa lên sơ đồ Gantt:</span>
                </div>
            </div>
            
            <div class="d-flex flex-wrap gap-2">
                <%-- ◀ SERVLET: TimelineServlet → setAttribute("allTasks") --%>
                <c:forEach items="${allTasks}" var="ut">
                    <c:if test="${empty ut.dueDate}">
                        <%-- ◀ SERVLET: TimelineServlet → setAttribute("todayDate") --%>
                        <%-- Dữ liệu người dùng đi qua data-* (escapeXml) rồi JS đọc ra, không nhúng thẳng vào chuỗi JS trong onclick --%>
                        <div class="unscheduled-task-badge"
                             data-task-id="${ut.id}" data-title="${fn:escapeXml(ut.title)}"
                             data-assignee="${fn:escapeXml(ut.assigneeName)}" data-status="${fn:escapeXml(ut.status)}"
                             onclick="document.getElementById('modalTaskId').value=this.dataset.taskId; document.getElementById('modalTaskTitle').textContent=this.dataset.title; document.getElementById('modalTaskAssignee').textContent=this.dataset.assignee; document.getElementById('modalTaskStatus').textContent=this.dataset.status; document.getElementById('modalTaskDueDate').value='${todayDate}'; new bootstrap.Modal(document.getElementById('quickEditTimelineModal')).show();">
                            <span class="gantt-priority-dot ${ut.priority.toLowerCase()}"></span>
                            <span class="fw-medium text-dark text-truncate" style="max-width: 220px;" title="${fn:escapeXml(ut.title)}">${fn:escapeXml(ut.title)}</span>
                            <span class="badge bg-secondary-subtle text-secondary rounded-pill px-2 py-0 fs-10">${fn:escapeXml(ut.assigneeName)}</span>
                            <i class="bi bi-calendar-plus text-primary fs-8"></i>
                        </div>
                    </c:if>
                </c:forEach>
            </div>
        </div>
    </c:if>

</div>

<!-- =========================================================================
     6. MODAL 1: XEM & CHỈNH SỬA HẠN CHÓT NHANH TỨC THÌ
     ========================================================================= -->
<div class="modal fade" id="quickEditTimelineModal" tabindex="-1" aria-labelledby="quickEditModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-4 border-0 shadow-lg">
            <div class="modal-header border-bottom px-4 pt-3 pb-2">
                <h5 class="modal-title fw-bold fs-7 d-flex align-items-center gap-2" id="quickEditModalLabel">
                    <i class="bi bi-calendar-date-fill text-primary"></i>
                    <span>Chi Tiết &amp; Cập Nhật Hạn Chót</span>
                </h5>
                <button type="button" class="btn-close shadow-none" data-bs-dismiss="modal" aria-label="Đóng"></button>
            </div>
            <div class="modal-body px-4 py-3">
                <input type="hidden" id="modalTaskId" value="">

                <div class="mb-3">
                    <label class="form-label fs-9 fw-semibold text-muted text-uppercase mb-1">Tên công việc</label>
                    <h6 class="fw-bold text-dark mb-0" id="modalTaskTitle">---</h6>
                </div>

                <div class="row g-2 mb-3">
                    <div class="col-6">
                        <label class="form-label fs-9 fw-semibold text-muted text-uppercase mb-1">Người phụ trách</label>
                        <div class="fs-8 fw-semibold text-dark" id="modalTaskAssignee">---</div>
                    </div>
                    <div class="col-6">
                        <label class="form-label fs-9 fw-semibold text-muted text-uppercase mb-1">Trạng thái</label>
                        <div class="fs-8 fw-semibold text-primary" id="modalTaskStatus">---</div>
                    </div>
                </div>

                <div class="mb-3">
                    <label class="form-label fs-9 fw-semibold text-muted text-uppercase mb-1">Tiến độ việc con (Subtasks)</label>
                    <div class="progress" style="height: 8px;">
                        <div class="progress-bar bg-primary" id="modalTaskProgress" style="width: 0%;"></div>
                    </div>
                </div>

                <div class="mb-3">
                    <label for="modalTaskDueDate" class="form-label fs-9 fw-semibold text-muted text-uppercase mb-1">
                        Hạn chót hoàn thành (Due Date) <span class="text-danger">*</span>
                    </label>
                    <div class="input-group input-group-sm">
                        <span class="input-group-text bg-light"><i class="bi bi-calendar-event"></i></span>
                        <input type="date" id="modalTaskDueDate" class="form-control shadow-none fs-8" required>
                    </div>
                    <div class="form-text fs-10 text-muted">Thanh Gantt sẽ tự động co giãn và di chuyển theo ngày mới.</div>
                </div>
            </div>
            <div class="modal-footer border-top px-4 py-2-5 d-flex justify-content-between">
                <a href="javascript:void(0)" id="modalJumpKanbanBtn" class="btn btn-sm btn-link text-decoration-none fs-8 p-0 text-secondary">
                    <i class="bi bi-arrow-up-right-square me-1"></i> Xem trên Kanban
                </a>
                <div class="d-flex align-items-center gap-2">
                    <button type="button" class="btn btn-sm btn-light border rounded-pill px-3 fs-8" data-bs-dismiss="modal">Đóng</button>
                    <button type="button" class="btn btn-sm btn-primary-custom rounded-pill px-3-5 fs-8 text-white fw-semibold" onclick="submitQuickDueDateUpdate()">
                        <i class="bi bi-check2 me-1"></i> Lưu thay đổi
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- =========================================================================
     7. MODAL 2: THÊM NHANH CÔNG VIỆC MỚI VÀO LỘ TRÌNH
     ========================================================================= -->
<div class="modal fade" id="quickAddTaskModal" tabindex="-1" aria-labelledby="addTaskModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-4 border-0 shadow-lg">
            <%-- ▶ SERVLET: /timeline → TimelineServlet.doPost() → case "quickAddTask" → handleQuickAddTask() --%>
            <form action="${pageContext.request.contextPath}/timeline" method="POST">
                <input type="hidden" name="action" value="quickAddTask">
                <input type="hidden" name="projectId" value="${project.id}">

                <div class="modal-header border-bottom px-4 pt-3 pb-2">
                    <h5 class="modal-title fw-bold fs-7 d-flex align-items-center gap-2" id="addTaskModalLabel">
                        <i class="bi bi-plus-circle-fill text-primary"></i>
                        <span>Thêm Công Việc Vào Lộ Trình</span>
                    </h5>
                    <button type="button" class="btn-close shadow-none" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>
                <div class="modal-body px-4 py-3">
                    <div class="mb-3">
                        <label for="newTitle" class="form-label fs-8 fw-semibold mb-1">
                            Tiêu đề công việc <span class="text-danger">*</span>
                        </label>
                        <input type="text" id="newTitle" name="title" class="form-control form-control-sm shadow-none fs-8"
                               placeholder="Ví dụ: Thiết kế giao diện Dashboard" required autofocus>
                    </div>

                    <div class="row g-2 mb-3">
                        <div class="col-6">
                            <label for="newDueDate" class="form-label fs-8 fw-semibold mb-1">
                                Hạn chót hoàn thành <span class="text-danger">*</span>
                            </label>
                            <input type="date" id="newDueDate" name="dueDate" class="form-control form-control-sm shadow-none fs-8"
                                   value="${todayDate}" required>
                        </div>
                        <div class="col-6">
                            <label for="newPriority" class="form-label fs-8 fw-semibold mb-1">Mức ưu tiên</label>
                            <select id="newPriority" name="priority" class="form-select form-select-sm shadow-none fs-8">
                                <option value="HIGH">🔴 Khẩn cấp (High)</option>
                                <option value="MEDIUM" selected>🟡 Trung bình (Medium)</option>
                                <option value="LOW">🟢 Thấp (Low)</option>
                            </select>
                        </div>
                    </div>

                    <div class="mb-3">
                        <label for="newAssignee" class="form-label fs-8 fw-semibold mb-1">Người phụ trách (Task Lead)</label>
                        <select id="newAssignee" name="assigneeId" class="form-select form-select-sm shadow-none fs-8">
                            <option value="0">-- Chưa phân công --</option>
                            <c:forEach items="${members}" var="m">
                                <option value="${m.userId}">${fn:escapeXml(m.userName)} (${fn:escapeXml(m.userRole)})</option>
                            </c:forEach>
                        </select>
                    </div>

                    <div class="mb-2">
                        <label for="newDesc" class="form-label fs-8 fw-semibold mb-1">Mô tả ngắn</label>
                        <textarea id="newDesc" name="description" class="form-control form-control-sm shadow-none fs-8" rows="2"
                                  placeholder="Ghi chú thêm về yêu cầu công việc..."></textarea>
                    </div>
                </div>
                <div class="modal-footer border-top px-4 py-2-5">
                    <button type="button" class="btn btn-sm btn-light border rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-sm btn-primary-custom rounded-pill px-3-5 fs-8 text-white fw-semibold">
                        <i class="bi bi-plus-lg me-1"></i> Tạo công việc
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- =========================================================================
     8. NẠP DỮ LIỆU JSON & KÍCH HOẠT ENGINE GANTT
     ========================================================================= -->
<script>
    // Cấu hình ngữ cảnh toàn cục
    window.timelineConfig = {
        projectId: ${project.id},
        contextPath: '${pageContext.request.contextPath}',
        today: '${todayDate}'
    };

    // Nạp dữ liệu Tasks JSON an toàn từ Controller
    <%-- ◀ SERVLET: TimelineServlet → setAttribute("tasksJson") --%>
    window.timelineTasksData = ${tasksJson};
</script>

<script src="${pageContext.request.contextPath}/js/timeline.js?v=<%= System.currentTimeMillis() %>"></script>

<jsp:include page="/includes/footer.jsp" />
