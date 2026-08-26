<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!-- 1. NẠP HEADER & THANH ĐIỀU HƯỚNG CHUNG -->
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container-fluid px-lg-5 py-4">

    <!-- 2. THANH TIÊU ĐỀ DỰ ÁN & CÁC NÚT ĐIỀU HƯỚNG TRÊN CÙNG -->
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4 pb-3 border-bottom bg-white p-3 rounded-4 shadow-sm">
        
        <!-- Cụm bên trái: Nút quay lại + Tên dự án + Chuyển Tab -->
        <div class="d-flex align-items-center gap-3">
            <a href="${pageContext.request.contextPath}/project?action=list" 
               class="btn btn-outline-secondary btn-sm rounded-pill px-3 shadow-none" 
               title="Quay về danh sách dự án">
                <i class="bi bi-arrow-left me-1"></i> Dashboard
            </a>
            
            <div class="border-start ps-3 d-flex align-items-center gap-3">
                <div>
                    <div class="d-flex align-items-center gap-2">
                        <h4 class="fw-extrabold text-dark mb-0 tracking-tight">${project.name}</h4>
                        <span class="badge bg-light text-secondary border rounded-pill px-3 py-1 fs-8">
                            <i class="bi bi-clock-history me-1"></i> ${project.createdAt}
                        </span>
                    </div>
                    <c:if test="${not empty project.description}">
                        <p class="text-muted fs-8 mb-0 mt-1">${project.description}</p>
                    </c:if>
                </div>

                <!-- 3 Nút chuyển phân hệ nhanh: Kanban / Docs / Chat -->
                <div class="d-none d-md-flex align-items-center gap-2 bg-light p-1 rounded-pill border ms-2">
                    <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}" 
                       class="btn btn-sm btn-white bg-white text-primary shadow-2xs rounded-pill px-3 py-1 fw-bold fs-8">
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
                </div>
            </div>
        </div>

        <!-- Cụm bên phải: Nút bật Modal thêm công việc mới -->
        <div>
            <button type="button" class="btn btn-primary-custom px-4 py-2 rounded-pill fw-semibold shadow-sm"
                    data-bs-toggle="modal" data-bs-target="#addTaskModal">
                <i class="bi bi-plus-circle me-1"></i> Thêm công việc
            </button>
        </div>
    </div>

    <!-- 3. KHÔNG GIAN BẢNG KANBAN 3 CỘT (BOOTSTRAP GRID) -->
    <div class="row g-4 kanban-board">

        <!-- ==========================================
             CỘT 1: CẦN LÀM (TO DO)
             ========================================== -->
        <div class="col-12 col-md-6 col-lg-4">
            <div class="kanban-column bg-light-subtle p-3 rounded-4 border shadow-2xs h-100 d-flex flex-column">
                
                <!-- Tiêu đề Cột 1 -->
                <div class="d-flex align-items-center justify-content-between mb-3 px-1">
                    <div class="d-flex align-items-center gap-2">
                        <span class="p-2 bg-secondary-subtle text-secondary rounded-3">
                            <i class="bi bi-list-task"></i>
                        </span>
                        <h6 class="fw-bold mb-0 text-dark">Cần làm (To Do)</h6>
                    </div>
                    <span class="badge bg-secondary rounded-pill px-2 py-1 fs-8">${todoTasks.size()}</span>
                </div>

                <!-- Khu vực chứa các thẻ Task (Drop Zone) -->
                <div class="kanban-task-list d-flex flex-column gap-3 flex-grow-1" 
                     id="column-TODO" 
                     data-status="TODO">
                    
                    <!-- Lặp qua từng task trong danh sách todoTasks -->
                    <c:forEach items="${todoTasks}" var="task">
                        <div class="card kanban-card border-0 bg-white shadow-sm p-3 rounded-3 cursor-grab"
                             id="task-${task.id}"
                             draggable="true" 
                             data-task-id="${task.id}"
                             data-bs-toggle="modal" 
                             data-bs-target="#taskDetailModal-${task.id}"
                             style="cursor: pointer;">
                            
                            <!-- Hàng 1: Badge mức độ ưu tiên & Nút xóa -->
                            <div class="d-flex align-items-center justify-content-between mb-2">
                                <span class="badge ${task.priorityBadgeClass} rounded-pill px-2 py-1 fs-8 fw-semibold">
                                    ${task.priorityLabel}
                                </span>
                                
                                <!-- Link xóa task -->
                                <a href="${pageContext.request.contextPath}/task?action=delete&taskId=${task.id}&projectId=${project.id}" 
                                   class="text-muted text-hover-danger text-decoration-none p-1"
                                   onclick="event.stopPropagation(); return confirm('Bạn có chắc chắn muốn xóa thẻ công việc này không?');"
                                   title="Xóa công việc">
                                    <i class="bi bi-trash3"></i>
                                </a>
                            </div>

                            <!-- Hàng 2: Tiêu đề công việc -->
                            <h6 class="fw-bold text-dark mb-1 fs-6">${task.title}</h6>

                            <!-- Hàng 3: Mô tả chi tiết (nếu có) -->
                            <c:if test="${not empty task.description}">
                                <p class="text-muted fs-7 mb-2 text-truncate-2">${task.description}</p>
                            </c:if>

                            <!-- Hàng 3.5: Huy hiệu Tiến độ Việc con, Tài liệu đính kèm & Bình luận -->
                            <div class="d-flex flex-wrap align-items-center gap-1 mb-2">
                                <c:if test="${not empty taskSubTasksMap[task.id]}">
                                    <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-1 fs-9 d-inline-flex align-items-center gap-1" title="Tiến độ việc con: ${taskProgressMap[task.id]}%">
                                        <i class="bi bi-check2-square"></i> ${taskProgressMap[task.id]}% (${taskSubTasksMap[task.id].size()} việc)
                                    </span>
                                </c:if>
                                <c:if test="${not empty taskDocsMap[task.id]}">
                                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2 py-1 fs-9 d-inline-flex align-items-center gap-1" title="${taskDocsMap[task.id].size()} tài liệu đính kèm">
                                        <i class="bi bi-journal-text"></i> ${taskDocsMap[task.id].size()} doc
                                    </span>
                                </c:if>
                                <c:if test="${not empty taskCommentsMap[task.id]}">
                                    <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle rounded-pill px-2 py-1 fs-9 d-inline-flex align-items-center gap-1" title="${taskCommentsMap[task.id].size()} thảo luận">
                                        <i class="bi bi-chat-dots"></i> ${taskCommentsMap[task.id].size()}
                                    </span>
                                </c:if>
                            </div>

                            <!-- Hàng 4: Người phụ trách & Hạn chót -->
                            <div class="d-flex align-items-center justify-content-between pt-2 mt-2 border-top fs-8 text-secondary">
                                <div class="d-flex align-items-center gap-1" title="Trưởng nhóm Task (Lead)">
                                    <i class="bi bi-person-circle text-primary"></i>
                                    <span class="fw-medium text-dark">${task.assigneeName}</span>
                                </div>
                                <c:if test="${not empty task.dueDate}">
                                    <div class="d-flex align-items-center gap-1" title="Hạn hoàn thành">
                                        <i class="bi bi-calendar-event"></i>
                                        <span>${task.dueDate}</span>
                                    </div>
                                </c:if>
                            </div>

                            <!-- Hàng 5: Nút chuyển cột nhanh sang Đang làm -->
                            <div class="mt-2 pt-1 text-end" onclick="event.stopPropagation();">
                                <form method="post" action="${pageContext.request.contextPath}/task" class="d-inline">
                                    <input type="hidden" name="action" value="updateStatus">
                                    <input type="hidden" name="projectId" value="${project.id}">
                                    <input type="hidden" name="taskId" value="${task.id}">
                                    <input type="hidden" name="newStatus" value="IN_PROGRESS">
                                    <button type="submit" class="btn btn-outline-primary btn-xs py-1 px-2 rounded-2 fs-8" title="Chuyển sang Đang làm">
                                        Đang làm <i class="bi bi-arrow-right ms-1"></i>
                                    </button>
                                </form>
                            </div>

                        </div>
                    </c:forEach>

                    <!-- Thông báo khi cột rỗng -->
                    <c:if test="${empty todoTasks}">
                        <div class="empty-column-placeholder text-center text-muted py-4 border border-dashed rounded-3">
                            <i class="bi bi-inbox fs-4 d-block mb-1 opacity-50"></i>
                            <span class="fs-8">Không có việc cần làm</span>
                        </div>
                    </c:if>

                </div>
            </div>
        </div>

        <!-- ==========================================
             CỘT 2: ĐANG LÀM (IN PROGRESS)
             ========================================== -->
        <div class="col-12 col-md-6 col-lg-4">
            <div class="kanban-column bg-light-subtle p-3 rounded-4 border shadow-2xs h-100 d-flex flex-column">
                
                <!-- Tiêu đề Cột 2 -->
                <div class="d-flex align-items-center justify-content-between mb-3 px-1">
                    <div class="d-flex align-items-center gap-2">
                        <span class="p-2 bg-primary-subtle text-primary rounded-3">
                            <i class="bi bi-arrow-repeat"></i>
                        </span>
                        <h6 class="fw-bold mb-0 text-dark">Đang làm (In Progress)</h6>
                    </div>
                    <span class="badge bg-primary rounded-pill px-2 py-1 fs-8">${inProgressTasks.size()}</span>
                </div>

                <!-- Khu vực chứa các thẻ Task (Drop Zone) -->
                <div class="kanban-task-list d-flex flex-column gap-3 flex-grow-1" 
                     id="column-IN_PROGRESS" 
                     data-status="IN_PROGRESS">
                    
                    <!-- Lặp qua từng task trong danh sách inProgressTasks -->
                    <c:forEach items="${inProgressTasks}" var="task">
                        <div class="card kanban-card border-0 bg-white shadow-sm p-3 rounded-3 cursor-grab"
                             id="task-${task.id}"
                             draggable="true" 
                             data-task-id="${task.id}"
                             data-bs-toggle="modal" 
                             data-bs-target="#taskDetailModal-${task.id}"
                             style="cursor: pointer;">
                            
                            <div class="d-flex align-items-center justify-content-between mb-2">
                                <span class="badge ${task.priorityBadgeClass} rounded-pill px-2 py-1 fs-8 fw-semibold">
                                    ${task.priorityLabel}
                                </span>
                                
                                <a href="${pageContext.request.contextPath}/task?action=delete&taskId=${task.id}&projectId=${project.id}" 
                                   class="text-muted text-hover-danger text-decoration-none p-1"
                                   onclick="event.stopPropagation(); return confirm('Bạn có chắc chắn muốn xóa thẻ công việc này không?');"
                                   title="Xóa công việc">
                                    <i class="bi bi-trash3"></i>
                                </a>
                            </div>

                            <h6 class="fw-bold text-dark mb-1 fs-6">${task.title}</h6>

                            <c:if test="${not empty task.description}">
                                <p class="text-muted fs-7 mb-2 text-truncate-2">${task.description}</p>
                            </c:if>

                            <!-- Huy hiệu Tiến độ Việc con, Tài liệu đính kèm & Bình luận -->
                            <div class="d-flex flex-wrap align-items-center gap-1 mb-2">
                                <c:if test="${not empty taskSubTasksMap[task.id]}">
                                    <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-1 fs-9 d-inline-flex align-items-center gap-1" title="Tiến độ việc con: ${taskProgressMap[task.id]}%">
                                        <i class="bi bi-check2-square"></i> ${taskProgressMap[task.id]}% (${taskSubTasksMap[task.id].size()} việc)
                                    </span>
                                </c:if>
                                <c:if test="${not empty taskDocsMap[task.id]}">
                                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2 py-1 fs-9 d-inline-flex align-items-center gap-1" title="${taskDocsMap[task.id].size()} tài liệu đính kèm">
                                        <i class="bi bi-journal-text"></i> ${taskDocsMap[task.id].size()} doc
                                    </span>
                                </c:if>
                                <c:if test="${not empty taskCommentsMap[task.id]}">
                                    <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle rounded-pill px-2 py-1 fs-9 d-inline-flex align-items-center gap-1" title="${taskCommentsMap[task.id].size()} thảo luận">
                                        <i class="bi bi-chat-dots"></i> ${taskCommentsMap[task.id].size()}
                                    </span>
                                </c:if>
                            </div>

                            <div class="d-flex align-items-center justify-content-between pt-2 mt-2 border-top fs-8 text-secondary">
                                <div class="d-flex align-items-center gap-1" title="Trưởng nhóm Task (Lead)">
                                    <i class="bi bi-person-circle text-primary"></i>
                                    <span class="fw-medium text-dark">${task.assigneeName}</span>
                                </div>
                                <c:if test="${not empty task.dueDate}">
                                    <div class="d-flex align-items-center gap-1" title="Hạn hoàn thành">
                                        <i class="bi bi-calendar-event"></i>
                                        <span>${task.dueDate}</span>
                                    </div>
                                </c:if>
                            </div>

                            <!-- 2 nút chuyển cột nhanh: Sang Cần làm hoặc Sang Đã xong -->
                            <div class="d-flex align-items-center justify-content-between mt-2 pt-1" onclick="event.stopPropagation();">
                                <form method="post" action="${pageContext.request.contextPath}/task" class="d-inline">
                                    <input type="hidden" name="action" value="updateStatus">
                                    <input type="hidden" name="projectId" value="${project.id}">
                                    <input type="hidden" name="taskId" value="${task.id}">
                                    <input type="hidden" name="newStatus" value="TODO">
                                    <button type="submit" class="btn btn-outline-secondary btn-xs py-1 px-2 rounded-2 fs-8" title="Chuyển về Cần làm">
                                        <i class="bi bi-arrow-left me-1"></i> Cần làm
                                    </button>
                                </form>

                                <form method="post" action="${pageContext.request.contextPath}/task" class="d-inline">
                                    <input type="hidden" name="action" value="updateStatus">
                                    <input type="hidden" name="projectId" value="${project.id}">
                                    <input type="hidden" name="taskId" value="${task.id}">
                                    <input type="hidden" name="newStatus" value="DONE">
                                    <button type="submit" class="btn btn-outline-success btn-xs py-1 px-2 rounded-2 fs-8" title="Chuyển sang Đã xong">
                                        Xong <i class="bi bi-check2 ms-1"></i>
                                    </button>
                                </form>
                            </div>

                        </div>
                    </c:forEach>

                    <c:if test="${empty inProgressTasks}">
                        <div class="empty-column-placeholder text-center text-muted py-4 border border-dashed rounded-3">
                            <i class="bi bi-hourglass fs-4 d-block mb-1 opacity-50"></i>
                            <span class="fs-8">Không có việc đang làm</span>
                        </div>
                    </c:if>

                </div>
            </div>
        </div>

        <!-- ==========================================
             CỘT 3: ĐÃ XONG (DONE)
             ========================================== -->
        <div class="col-12 col-md-6 col-lg-4">
            <div class="kanban-column bg-light-subtle p-3 rounded-4 border shadow-2xs h-100 d-flex flex-column">
                
                <!-- Tiêu đề Cột 3 -->
                <div class="d-flex align-items-center justify-content-between mb-3 px-1">
                    <div class="d-flex align-items-center gap-2">
                        <span class="p-2 bg-success-subtle text-success rounded-3">
                            <i class="bi bi-check-circle"></i>
                        </span>
                        <h6 class="fw-bold mb-0 text-dark">Đã xong (Done)</h6>
                    </div>
                    <span class="badge bg-success rounded-pill px-2 py-1 fs-8">${doneTasks.size()}</span>
                </div>

                <!-- Khu vực chứa các thẻ Task (Drop Zone) -->
                <div class="kanban-task-list d-flex flex-column gap-3 flex-grow-1" 
                     id="column-DONE" 
                     data-status="DONE">
                    
                    <!-- Lặp qua từng task trong danh sách doneTasks -->
                    <c:forEach items="${doneTasks}" var="task">
                        <div class="card kanban-card border-0 bg-white shadow-sm p-3 rounded-3 cursor-grab opacity-75"
                             id="task-${task.id}"
                             draggable="true" 
                             data-task-id="${task.id}"
                             data-bs-toggle="modal" 
                             data-bs-target="#taskDetailModal-${task.id}"
                             style="cursor: pointer;">
                            
                            <div class="d-flex align-items-center justify-content-between mb-2">
                                <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-1 fs-8 fw-semibold">
                                    <i class="bi bi-check2"></i> Đã hoàn thành
                                </span>
                                
                                <a href="${pageContext.request.contextPath}/task?action=delete&taskId=${task.id}&projectId=${project.id}" 
                                   class="text-muted text-hover-danger text-decoration-none p-1"
                                   onclick="event.stopPropagation(); return confirm('Bạn có chắc chắn muốn xóa thẻ công việc này không?');"
                                   title="Xóa công việc">
                                    <i class="bi bi-trash3"></i>
                                </a>
                            </div>

                            <h6 class="fw-bold text-dark mb-1 fs-6 text-decoration-line-through">${task.title}</h6>

                            <c:if test="${not empty task.description}">
                                <p class="text-muted fs-7 mb-2 text-truncate-2">${task.description}</p>
                            </c:if>

                            <!-- Huy hiệu Tiến độ Việc con, Tài liệu đính kèm & Bình luận -->
                            <div class="d-flex flex-wrap align-items-center gap-1 mb-2">
                                <c:if test="${not empty taskSubTasksMap[task.id]}">
                                    <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-1 fs-9 d-inline-flex align-items-center gap-1" title="Tiến độ việc con: ${taskProgressMap[task.id]}%">
                                        <i class="bi bi-check2-square"></i> ${taskProgressMap[task.id]}% (${taskSubTasksMap[task.id].size()} việc)
                                    </span>
                                </c:if>
                                <c:if test="${not empty taskDocsMap[task.id]}">
                                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-2 py-1 fs-9 d-inline-flex align-items-center gap-1" title="${taskDocsMap[task.id].size()} tài liệu đính kèm">
                                        <i class="bi bi-journal-text"></i> ${taskDocsMap[task.id].size()} doc
                                    </span>
                                </c:if>
                                <c:if test="${not empty taskCommentsMap[task.id]}">
                                    <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle rounded-pill px-2 py-1 fs-9 d-inline-flex align-items-center gap-1" title="${taskCommentsMap[task.id].size()} thảo luận">
                                        <i class="bi bi-chat-dots"></i> ${taskCommentsMap[task.id].size()}
                                    </span>
                                </c:if>
                            </div>

                            <div class="d-flex align-items-center justify-content-between pt-2 mt-2 border-top fs-8 text-secondary">
                                <div class="d-flex align-items-center gap-1" title="Trưởng nhóm Task (Lead)">
                                    <i class="bi bi-person-circle text-primary"></i>
                                    <span class="fw-medium text-dark">${task.assigneeName}</span>
                                </div>
                                <c:if test="${not empty task.dueDate}">
                                    <div class="d-flex align-items-center gap-1" title="Hạn hoàn thành">
                                        <i class="bi bi-calendar-event"></i>
                                        <span>${task.dueDate}</span>
                                    </div>
                                </c:if>
                            </div>

                            <!-- Nút mở lại công việc sang Đang làm -->
                            <div class="mt-2 pt-1 text-start" onclick="event.stopPropagation();">
                                <form method="post" action="${pageContext.request.contextPath}/task" class="d-inline">
                                    <input type="hidden" name="action" value="updateStatus">
                                    <input type="hidden" name="projectId" value="${project.id}">
                                    <input type="hidden" name="taskId" value="${task.id}">
                                    <input type="hidden" name="newStatus" value="IN_PROGRESS">
                                    <button type="submit" class="btn btn-outline-secondary btn-xs py-1 px-2 rounded-2 fs-8" title="Mở lại công việc sang Đang làm">
                                        <i class="bi bi-arrow-left me-1"></i> Làm lại
                                    </button>
                                </form>
                            </div>

                        </div>
                    </c:forEach>

                    <c:if test="${empty doneTasks}">
                        <div class="empty-column-placeholder text-center text-muted py-4 border border-dashed rounded-3">
                            <i class="bi bi-check2-all fs-4 d-block mb-1 opacity-50"></i>
                            <span class="fs-8">Chưa có việc nào hoàn thành</span>
                        </div>
                    </c:if>

                </div>
            </div>
        </div>

    </div>
</div>

<!-- ========================================================
     4. MODAL CHI TIẾT TASK 2 CỘT (TASK MINI-HUB & SUB-TASKS)
     Được nhúng trực tiếp để quản lý Cây việc con và Hội thoại
     ======================================================== -->

<!-- HÀM TEMPLATE RENDER MODAL DÙNG CHUNG CHO TỪNG TASK -->
<c:set var="allTasksToRender" value="${todoTasks}" />
<c:forEach items="${allTasksToRender}" var="task">
    <div class="modal fade" id="taskDetailModal-${task.id}" tabindex="-1" aria-labelledby="taskDetailModalLabel-${task.id}" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-xl">
            <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
                <div class="modal-header bg-light px-4 py-3 border-bottom">
                    <div class="d-flex align-items-center gap-2">
                        <span class="badge ${task.priorityBadgeClass} rounded-pill px-3 py-1 fs-8 fw-semibold">
                            ${task.priorityLabel}
                        </span>
                        <h5 class="modal-title fw-bold text-dark mb-0" id="taskDetailModalLabel-${task.id}">
                            ${task.title}
                        </h5>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>
                <div class="modal-body p-0">
                    <div class="row g-0">
                        
                        <!-- CỘT TRÁI: THÔNG TIN, TIẾN ĐỘ, VIỆC CON & TÀI LIỆU -->
                        <div class="col-12 col-md-7 p-4 border-end">
                            
                            <!-- Khung tóm tắt: Trạng thái, Task Lead, Hạn chót -->
                            <div class="row g-3 p-3 bg-light rounded-3 border mb-4">
                                <div class="col-6 col-sm-4">
                                    <span class="text-muted fs-9 d-block mb-1">Trạng thái</span>
                                    <span class="badge bg-secondary rounded-pill px-2 py-1 fs-9">Cần làm</span>
                                </div>
                                <div class="col-6 col-sm-4">
                                    <span class="text-muted fs-9 d-block mb-1">Trưởng nhóm Task (Lead)</span>
                                    <span class="fw-semibold text-dark fs-8 d-inline-flex align-items-center gap-1">
                                        <i class="bi bi-person-circle text-primary"></i> ${task.assigneeName}
                                    </span>
                                </div>
                                <div class="col-12 col-sm-4">
                                    <span class="text-muted fs-9 d-block mb-1">Hạn hoàn thành</span>
                                    <span class="fw-semibold text-dark fs-8 d-inline-flex align-items-center gap-1">
                                        <i class="bi bi-calendar-event"></i> ${not empty task.dueDate ? task.dueDate : 'Chưa đặt hạn'}
                                    </span>
                                </div>
                            </div>

                            <!-- KHỐI 1: TIẾN ĐỘ TỰ ĐỘNG CỘNG DỒN TỪ VIỆC CON -->
                            <div class="mb-4 p-3 bg-light rounded-3 border">
                                <div class="d-flex align-items-center justify-content-between mb-1">
                                    <span class="fw-bold text-dark fs-8">
                                        <i class="bi bi-graph-up-arrow text-success me-1"></i> Tiến độ hoàn thành Task
                                    </span>
                                    <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-1 fs-9 fw-bold">
                                        ${taskProgressMap[task.id]}%
                                    </span>
                                </div>
                                <div class="progress" style="height: 6px;">
                                    <div class="progress-bar bg-success rounded-pill" role="progressbar" 
                                         style="width: ${taskProgressMap[task.id]}%;" 
                                         aria-valuenow="${taskProgressMap[task.id]}" aria-valuemin="0" aria-valuemax="100"></div>
                                </div>
                                <div class="d-flex justify-content-between mt-1 fs-9 text-muted">
                                    <span>Cộng dồn từ ${not empty taskSubTasksMap[task.id] ? taskSubTasksMap[task.id].size() : 0} việc con</span>
                                    <c:if test="${taskProgressMap[task.id] == 100}">
                                        <span class="text-success fw-bold"><i class="bi bi-check-circle-fill"></i> Hoàn thành 100%</span>
                                    </c:if>
                                </div>
                            </div>

                            <!-- KHỐI 2: MÔ TẢ YÊU CẦU -->
                            <div class="mb-4">
                                <h6 class="fw-bold text-dark fs-7 mb-2">
                                    <i class="bi bi-text-left text-primary me-1"></i> Mô tả yêu cầu công việc
                                </h6>
                                <div class="p-3 bg-white rounded-3 border text-dark fs-7 lh-base" style="white-space: pre-line;">
                                    <c:choose>
                                        <c:when test="${not empty task.description}">
                                            <c:out value="${task.description}" />
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted fst-italic">Không có mô tả chi tiết cho công việc này.</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>

                            <!-- KHỐI 3: DANH SÁCH VIỆC CON (SUB-TASKS CỦA TASK NÀY) -->
                            <div class="mb-4">
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <h6 class="fw-bold text-dark fs-7 mb-0">
                                        <i class="bi bi-list-check text-primary me-1"></i> Danh sách việc con (Sub-tasks)
                                    </h6>
                                    <span class="badge bg-secondary-subtle text-secondary rounded-pill px-2 py-1 fs-9">
                                        ${not empty taskSubTasksMap[task.id] ? taskSubTasksMap[task.id].size() : 0} việc
                                    </span>
                                </div>

                                <c:if test="${not empty taskSubTasksMap[task.id]}">
                                    <div class="d-flex flex-column gap-2 mb-3">
                                        <c:forEach items="${taskSubTasksMap[task.id]}" var="st">
                                            <div class="d-flex align-items-center justify-content-between p-2 px-3 bg-light rounded-3 border ${st.completed ? 'opacity-75' : ''}">
                                                <div class="d-flex align-items-center gap-2 flex-grow-1">
                                                    
                                                    <!-- Checkbox toggle hoàn thành việc con -->
                                                    <form method="post" action="${pageContext.request.contextPath}/task" class="m-0 d-flex align-items-center">
                                                        <input type="hidden" name="action" value="toggleSubTask">
                                                        <input type="hidden" name="projectId" value="${project.id}">
                                                        <input type="hidden" name="subTaskId" value="${st.id}">
                                                        <input type="hidden" name="completed" value="${!st.completed}">
                                                        <input class="form-check-input mt-0 cursor-pointer" 
                                                               type="checkbox" 
                                                               ${st.completed ? 'checked' : ''} 
                                                               onchange="this.form.submit()" 
                                                               title="${st.completed ? 'Bấm để đánh dấu chưa xong' : 'Bấm để hoàn thành và đóng góp tiến độ'}">
                                                    </form>

                                                    <!-- Tên việc con -->
                                                    <span class="fs-8 text-dark ${st.completed ? 'text-decoration-line-through text-muted' : 'fw-medium'}">
                                                        ${st.title}
                                                    </span>
                                                </div>

                                                <div class="d-flex align-items-center gap-2">
                                                    <!-- Người thực hiện việc con -->
                                                    <span class="badge bg-white text-secondary border rounded-pill px-2 py-1 fs-9" title="Người phụ trách việc con">
                                                        <i class="bi bi-person-fill text-primary"></i> ${st.assigneeName}
                                                    </span>

                                                    <!-- Nút xóa việc con (Chỉ Task Lead hoặc Project Owner thấy) -->
                                                    <c:if test="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}">
                                                        <form method="post" action="${pageContext.request.contextPath}/task" class="m-0 d-inline" onsubmit="return confirm('Bạn có chắc chắn muốn xóa việc con này?');">
                                                            <input type="hidden" name="action" value="deleteSubTask">
                                                            <input type="hidden" name="projectId" value="${project.id}">
                                                            <input type="hidden" name="subTaskId" value="${st.id}">
                                                            <button type="submit" class="btn btn-link text-muted text-hover-danger p-0 border-0 fs-8" title="Xóa việc con">
                                                                <i class="bi bi-x-circle"></i>
                                                            </button>
                                                        </form>
                                                    </c:if>
                                                </div>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:if>

                                <c:if test="${empty taskSubTasksMap[task.id]}">
                                    <div class="p-3 bg-light-subtle rounded-3 text-muted fs-8 border text-center mb-3">
                                        Chưa có việc con nào được tạo.
                                    </div>
                                </c:if>

                                <!-- Form "+ Thêm việc con và phân công" (Chỉ Task Lead hoặc Project Owner thấy) -->
                                <c:if test="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}">
                                    <div class="p-3 bg-white rounded-3 border border-primary-subtle">
                                        <span class="fs-9 fw-bold text-primary d-block mb-2">
                                            <i class="bi bi-plus-circle-fill me-1"></i> Giao thêm việc con (Dành cho Task Lead)
                                        </span>
                                        <form method="post" action="${pageContext.request.contextPath}/task" class="d-flex flex-column gap-2">
                                            <input type="hidden" name="action" value="addSubTask">
                                            <input type="hidden" name="projectId" value="${project.id}">
                                            <input type="hidden" name="taskId" value="${task.id}">

                                            <div class="row g-2">
                                                <div class="col-12 col-md-7">
                                                    <input type="text" 
                                                           name="title" 
                                                           class="form-control form-control-sm fs-8 rounded-3" 
                                                           placeholder="Nhập tên việc con cần giao..." 
                                                           required>
                                                </div>
                                                <div class="col-8 col-md-3">
                                                    <select name="assigneeId" class="form-select form-select-sm fs-8 rounded-3">
                                                        <option value="0">-- Chọn người làm --</option>
                                                        <c:forEach items="${userList}" var="u">
                                                            <option value="${u.id}">${u.fullName}</option>
                                                        </c:forEach>
                                                    </select>
                                                </div>
                                                <div class="col-4 col-md-2">
                                                    <button type="submit" class="btn btn-primary-custom btn-sm w-100 rounded-3 fs-8 fw-semibold">
                                                        + Giao
                                                    </button>
                                                </div>
                                            </div>
                                        </form>
                                    </div>
                                </c:if>
                            </div>

                            <!-- KHỐI 4: TÀI LIỆU HƯỚNG DẪN ĐÍNH KÈM -->
                            <div>
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <h6 class="fw-bold text-dark fs-7 mb-0">
                                        <i class="bi bi-journal-bookmark text-primary me-1"></i> Tài liệu hướng dẫn đính kèm
                                    </h6>
                                    <span class="badge bg-primary-subtle text-primary rounded-pill px-2 py-1 fs-9">
                                        ${not empty taskDocsMap[task.id] ? taskDocsMap[task.id].size() : 0} tài liệu
                                    </span>
                                </div>
                                <c:if test="${not empty taskDocsMap[task.id]}">
                                    <div class="d-flex flex-column gap-2">
                                        <c:forEach items="${taskDocsMap[task.id]}" var="td">
                                            <div class="d-flex align-items-center justify-content-between p-2 px-3 bg-light rounded-3 border">
                                                <div class="d-flex align-items-center gap-2 text-truncate">
                                                    <i class="bi bi-file-earmark-text text-primary fs-6"></i>
                                                    <span class="fw-semibold text-dark fs-8 text-truncate">${td.docTitle}</span>
                                                </div>
                                                <a href="${pageContext.request.contextPath}/doc?action=view&projectId=${project.id}&docId=${td.docId}" 
                                                   class="btn btn-outline-primary btn-xs rounded-pill px-3 py-1 fs-9 text-nowrap"
                                                   title="Đọc bài viết Wiki này">
                                                    Đọc bài <i class="bi bi-arrow-right ms-1"></i>
                                                </a>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:if>
                                <c:if test="${empty taskDocsMap[task.id]}">
                                    <div class="p-3 bg-light-subtle rounded-3 text-muted fs-8 border text-center">
                                        Chưa có tài liệu Wiki nào được đính kèm cho công việc này.
                                    </div>
                                </c:if>
                            </div>

                        </div>

                        <!-- CỘT PHẢI: HỘI THOẠI CỦA TASK & DÒNG THÔNG BÁO TỰ ĐỘNG -->
                        <div class="col-12 col-md-5 p-4 d-flex flex-column bg-light-subtle" style="min-height: 520px;">
                            <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                                <div class="d-flex align-items-center gap-2">
                                    <i class="bi bi-chat-square-dots-fill text-primary"></i>
                                    <h6 class="fw-bold mb-0 text-dark fs-7">Hội thoại của Task</h6>
                                </div>
                                <span class="badge bg-secondary rounded-pill px-2 py-1 fs-9">
                                    ${not empty taskCommentsMap[task.id] ? taskCommentsMap[task.id].size() : 0}
                                </span>
                            </div>
                            
                            <!-- Danh sách bình luận & thông báo tự động (Cuộn dọc) -->
                            <div class="flex-grow-1 overflow-y-auto d-flex flex-column gap-2 mb-3 pe-1" style="max-height: 380px;">
                                <c:forEach items="${taskCommentsMap[task.id]}" var="comment">
                                    <div class="p-2 px-3 rounded-3 border shadow-2xs ${comment.authorName == 'Hệ Thống' ? 'bg-success-subtle border-success-subtle' : 'bg-white'}">
                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                            <span class="fw-bold fs-8 ${comment.authorName == 'Hệ Thống' ? 'text-success' : 'text-dark'}">
                                                <c:choose>
                                                    <c:when test="${comment.authorName == 'Hệ Thống'}">
                                                        <i class="bi bi-robot me-1"></i> Hệ Thống
                                                    </c:when>
                                                    <c:otherwise>
                                                        ${comment.authorName}
                                                    </c:otherwise>
                                                </c:choose>
                                            </span>
                                            <span class="text-muted fs-9">${comment.sentAt}</span>
                                        </div>
                                        <div class="fs-8 text-dark lh-base" style="word-break: break-word; white-space: pre-line;">
                                            <c:out value="${comment.content}" />
                                        </div>
                                    </div>
                                </c:forEach>
                                <c:if test="${empty taskCommentsMap[task.id]}">
                                    <div class="text-center text-muted my-auto py-4">
                                        <i class="bi bi-chat-left-dots fs-3 d-block mb-1 opacity-50"></i>
                                        <span class="fs-8">Chưa có bình luận nào. Hãy gửi phản hồi đầu tiên!</span>
                                    </div>
                                </c:if>
                            </div>

                            <!-- Form gửi bình luận trực tiếp cho Task này -->
                            <div class="pt-2 border-top mt-auto">
                                <form method="post" action="${pageContext.request.contextPath}/chat" class="d-flex flex-column gap-2">
                                    <input type="hidden" name="action" value="sendTaskComment">
                                    <input type="hidden" name="projectId" value="${project.id}">
                                    <input type="hidden" name="taskId" value="${task.id}">
                                    <div class="input-group">
                                        <input type="text" 
                                               class="form-control fs-8 py-2 rounded-start-pill ps-3 shadow-none border-secondary-subtle" 
                                               name="content" 
                                               placeholder="Viết bình luận cho task này..." 
                                               autocomplete="off" 
                                               required>
                                        <button type="submit" class="btn btn-primary-custom rounded-end-pill px-3 fs-8 fw-semibold shadow-sm">
                                            <i class="bi bi-send-fill me-1"></i> Gửi
                                        </button>
                                    </div>
                                </form>
                            </div>

                        </div>

                    </div>
                </div>
            </div>
        </div>
    </div>
</c:forEach>

<!-- MODAL CHO CỘT ĐANG LÀM (IN_PROGRESS) -->
<c:set var="allTasksToRender" value="${inProgressTasks}" />
<c:forEach items="${allTasksToRender}" var="task">
    <div class="modal fade" id="taskDetailModal-${task.id}" tabindex="-1" aria-labelledby="taskDetailModalLabel-${task.id}" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-xl">
            <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
                <div class="modal-header bg-light px-4 py-3 border-bottom">
                    <div class="d-flex align-items-center gap-2">
                        <span class="badge ${task.priorityBadgeClass} rounded-pill px-3 py-1 fs-8 fw-semibold">
                            ${task.priorityLabel}
                        </span>
                        <h5 class="modal-title fw-bold text-dark mb-0" id="taskDetailModalLabel-${task.id}">
                            ${task.title}
                        </h5>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>
                <div class="modal-body p-0">
                    <div class="row g-0">
                        <div class="col-12 col-md-7 p-4 border-end">
                            <div class="row g-3 p-3 bg-light rounded-3 border mb-4">
                                <div class="col-6 col-sm-4">
                                    <span class="text-muted fs-9 d-block mb-1">Trạng thái</span>
                                    <span class="badge bg-primary rounded-pill px-2 py-1 fs-9">Đang làm</span>
                                </div>
                                <div class="col-6 col-sm-4">
                                    <span class="text-muted fs-9 d-block mb-1">Trưởng nhóm Task (Lead)</span>
                                    <span class="fw-semibold text-dark fs-8 d-inline-flex align-items-center gap-1">
                                        <i class="bi bi-person-circle text-primary"></i> ${task.assigneeName}
                                    </span>
                                </div>
                                <div class="col-12 col-sm-4">
                                    <span class="text-muted fs-9 d-block mb-1">Hạn hoàn thành</span>
                                    <span class="fw-semibold text-dark fs-8 d-inline-flex align-items-center gap-1">
                                        <i class="bi bi-calendar-event"></i> ${not empty task.dueDate ? task.dueDate : 'Chưa đặt hạn'}
                                    </span>
                                </div>
                            </div>

                            <div class="mb-4 p-3 bg-light rounded-3 border">
                                <div class="d-flex align-items-center justify-content-between mb-1">
                                    <span class="fw-bold text-dark fs-8">
                                        <i class="bi bi-graph-up-arrow text-success me-1"></i> Tiến độ hoàn thành Task
                                    </span>
                                    <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-1 fs-9 fw-bold">
                                        ${taskProgressMap[task.id]}%
                                    </span>
                                </div>
                                <div class="progress" style="height: 6px;">
                                    <div class="progress-bar bg-success rounded-pill" role="progressbar" 
                                         style="width: ${taskProgressMap[task.id]}%;" 
                                         aria-valuenow="${taskProgressMap[task.id]}" aria-valuemin="0" aria-valuemax="100"></div>
                                </div>
                                <div class="d-flex justify-content-between mt-1 fs-9 text-muted">
                                    <span>Cộng dồn từ ${not empty taskSubTasksMap[task.id] ? taskSubTasksMap[task.id].size() : 0} việc con</span>
                                    <c:if test="${taskProgressMap[task.id] == 100}">
                                        <span class="text-success fw-bold"><i class="bi bi-check-circle-fill"></i> Hoàn thành 100%</span>
                                    </c:if>
                                </div>
                            </div>

                            <div class="mb-4">
                                <h6 class="fw-bold text-dark fs-7 mb-2">
                                    <i class="bi bi-text-left text-primary me-1"></i> Mô tả yêu cầu công việc
                                </h6>
                                <div class="p-3 bg-white rounded-3 border text-dark fs-7 lh-base" style="white-space: pre-line;">
                                    <c:choose>
                                        <c:when test="${not empty task.description}">
                                            <c:out value="${task.description}" />
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted fst-italic">Không có mô tả chi tiết cho công việc này.</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>

                            <div class="mb-4">
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <h6 class="fw-bold text-dark fs-7 mb-0">
                                        <i class="bi bi-list-check text-primary me-1"></i> Danh sách việc con (Sub-tasks)
                                    </h6>
                                    <span class="badge bg-secondary-subtle text-secondary rounded-pill px-2 py-1 fs-9">
                                        ${not empty taskSubTasksMap[task.id] ? taskSubTasksMap[task.id].size() : 0} việc
                                    </span>
                                </div>

                                <c:if test="${not empty taskSubTasksMap[task.id]}">
                                    <div class="d-flex flex-column gap-2 mb-3">
                                        <c:forEach items="${taskSubTasksMap[task.id]}" var="st">
                                            <div class="d-flex align-items-center justify-content-between p-2 px-3 bg-light rounded-3 border ${st.completed ? 'opacity-75' : ''}">
                                                <div class="d-flex align-items-center gap-2 flex-grow-1">
                                                    <form method="post" action="${pageContext.request.contextPath}/task" class="m-0 d-flex align-items-center">
                                                        <input type="hidden" name="action" value="toggleSubTask">
                                                        <input type="hidden" name="projectId" value="${project.id}">
                                                        <input type="hidden" name="subTaskId" value="${st.id}">
                                                        <input type="hidden" name="completed" value="${!st.completed}">
                                                        <input class="form-check-input mt-0 cursor-pointer" 
                                                               type="checkbox" 
                                                               ${st.completed ? 'checked' : ''} 
                                                               onchange="this.form.submit()" 
                                                               title="${st.completed ? 'Bấm để đánh dấu chưa xong' : 'Bấm để hoàn thành và đóng góp tiến độ'}">
                                                    </form>
                                                    <span class="fs-8 text-dark ${st.completed ? 'text-decoration-line-through text-muted' : 'fw-medium'}">
                                                        ${st.title}
                                                    </span>
                                                </div>

                                                <div class="d-flex align-items-center gap-2">
                                                    <span class="badge bg-white text-secondary border rounded-pill px-2 py-1 fs-9" title="Người phụ trách việc con">
                                                        <i class="bi bi-person-fill text-primary"></i> ${st.assigneeName}
                                                    </span>
                                                    <c:if test="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}">
                                                        <form method="post" action="${pageContext.request.contextPath}/task" class="m-0 d-inline" onsubmit="return confirm('Bạn có chắc chắn muốn xóa việc con này?');">
                                                            <input type="hidden" name="action" value="deleteSubTask">
                                                            <input type="hidden" name="projectId" value="${project.id}">
                                                            <input type="hidden" name="subTaskId" value="${st.id}">
                                                            <button type="submit" class="btn btn-link text-muted text-hover-danger p-0 border-0 fs-8" title="Xóa việc con">
                                                                <i class="bi bi-x-circle"></i>
                                                            </button>
                                                        </form>
                                                    </c:if>
                                                </div>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:if>

                                <c:if test="${empty taskSubTasksMap[task.id]}">
                                    <div class="p-3 bg-light-subtle rounded-3 text-muted fs-8 border text-center mb-3">
                                        Chưa có việc con nào được tạo.
                                    </div>
                                </c:if>

                                <c:if test="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}">
                                    <div class="p-3 bg-white rounded-3 border border-primary-subtle">
                                        <span class="fs-9 fw-bold text-primary d-block mb-2">
                                            <i class="bi bi-plus-circle-fill me-1"></i> Giao thêm việc con (Dành cho Task Lead)
                                        </span>
                                        <form method="post" action="${pageContext.request.contextPath}/task" class="d-flex flex-column gap-2">
                                            <input type="hidden" name="action" value="addSubTask">
                                            <input type="hidden" name="projectId" value="${project.id}">
                                            <input type="hidden" name="taskId" value="${task.id}">

                                            <div class="row g-2">
                                                <div class="col-12 col-md-7">
                                                    <input type="text" 
                                                           name="title" 
                                                           class="form-control form-control-sm fs-8 rounded-3" 
                                                           placeholder="Nhập tên việc con cần giao..." 
                                                           required>
                                                </div>
                                                <div class="col-8 col-md-3">
                                                    <select name="assigneeId" class="form-select form-select-sm fs-8 rounded-3">
                                                        <option value="0">-- Chọn người làm --</option>
                                                        <c:forEach items="${userList}" var="u">
                                                            <option value="${u.id}">${u.fullName}</option>
                                                        </c:forEach>
                                                    </select>
                                                </div>
                                                <div class="col-4 col-md-2">
                                                    <button type="submit" class="btn btn-primary-custom btn-sm w-100 rounded-3 fs-8 fw-semibold">
                                                        + Giao
                                                    </button>
                                                </div>
                                            </div>
                                        </form>
                                    </div>
                                </c:if>
                            </div>

                            <div>
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <h6 class="fw-bold text-dark fs-7 mb-0">
                                        <i class="bi bi-journal-bookmark text-primary me-1"></i> Tài liệu hướng dẫn đính kèm
                                    </h6>
                                    <span class="badge bg-primary-subtle text-primary rounded-pill px-2 py-1 fs-9">
                                        ${not empty taskDocsMap[task.id] ? taskDocsMap[task.id].size() : 0} tài liệu
                                    </span>
                                </div>
                                <c:if test="${not empty taskDocsMap[task.id]}">
                                    <div class="d-flex flex-column gap-2">
                                        <c:forEach items="${taskDocsMap[task.id]}" var="td">
                                            <div class="d-flex align-items-center justify-content-between p-2 px-3 bg-light rounded-3 border">
                                                <div class="d-flex align-items-center gap-2 text-truncate">
                                                    <i class="bi bi-file-earmark-text text-primary fs-6"></i>
                                                    <span class="fw-semibold text-dark fs-8 text-truncate">${td.docTitle}</span>
                                                </div>
                                                <a href="${pageContext.request.contextPath}/doc?action=view&projectId=${project.id}&docId=${td.docId}" 
                                                   class="btn btn-outline-primary btn-xs rounded-pill px-3 py-1 fs-9 text-nowrap"
                                                   title="Đọc bài viết Wiki này">
                                                    Đọc bài <i class="bi bi-arrow-right ms-1"></i>
                                                </a>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:if>
                                <c:if test="${empty taskDocsMap[task.id]}">
                                    <div class="p-3 bg-light-subtle rounded-3 text-muted fs-8 border text-center">
                                        Chưa có tài liệu Wiki nào được đính kèm cho công việc này.
                                    </div>
                                </c:if>
                            </div>
                        </div>

                        <div class="col-12 col-md-5 p-4 d-flex flex-column bg-light-subtle" style="min-height: 520px;">
                            <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                                <div class="d-flex align-items-center gap-2">
                                    <i class="bi bi-chat-square-dots-fill text-primary"></i>
                                    <h6 class="fw-bold mb-0 text-dark fs-7">Hội thoại của Task</h6>
                                </div>
                                <span class="badge bg-secondary rounded-pill px-2 py-1 fs-9">
                                    ${not empty taskCommentsMap[task.id] ? taskCommentsMap[task.id].size() : 0}
                                </span>
                            </div>
                            <div class="flex-grow-1 overflow-y-auto d-flex flex-column gap-2 mb-3 pe-1" style="max-height: 380px;">
                                <c:forEach items="${taskCommentsMap[task.id]}" var="comment">
                                    <div class="p-2 px-3 rounded-3 border shadow-2xs ${comment.authorName == 'Hệ Thống' ? 'bg-success-subtle border-success-subtle' : 'bg-white'}">
                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                            <span class="fw-bold fs-8 ${comment.authorName == 'Hệ Thống' ? 'text-success' : 'text-dark'}">
                                                <c:choose>
                                                    <c:when test="${comment.authorName == 'Hệ Thống'}">
                                                        <i class="bi bi-robot me-1"></i> Hệ Thống
                                                    </c:when>
                                                    <c:otherwise>
                                                        ${comment.authorName}
                                                    </c:otherwise>
                                                </c:choose>
                                            </span>
                                            <span class="text-muted fs-9">${comment.sentAt}</span>
                                        </div>
                                        <div class="fs-8 text-dark lh-base" style="word-break: break-word; white-space: pre-line;">
                                            <c:out value="${comment.content}" />
                                        </div>
                                    </div>
                                </c:forEach>
                                <c:if test="${empty taskCommentsMap[task.id]}">
                                    <div class="text-center text-muted my-auto py-4">
                                        <i class="bi bi-chat-left-dots fs-3 d-block mb-1 opacity-50"></i>
                                        <span class="fs-8">Chưa có bình luận nào. Hãy gửi phản hồi đầu tiên!</span>
                                    </div>
                                </c:if>
                            </div>
                            <div class="pt-2 border-top mt-auto">
                                <form method="post" action="${pageContext.request.contextPath}/chat" class="d-flex flex-column gap-2">
                                    <input type="hidden" name="action" value="sendTaskComment">
                                    <input type="hidden" name="projectId" value="${project.id}">
                                    <input type="hidden" name="taskId" value="${task.id}">
                                    <div class="input-group">
                                        <input type="text" 
                                               class="form-control fs-8 py-2 rounded-start-pill ps-3 shadow-none border-secondary-subtle" 
                                               name="content" 
                                               placeholder="Viết bình luận cho task này..." 
                                               autocomplete="off" 
                                               required>
                                        <button type="submit" class="btn btn-primary-custom rounded-end-pill px-3 fs-8 fw-semibold shadow-sm">
                                            <i class="bi bi-send-fill me-1"></i> Gửi
                                        </button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</c:forEach>

<!-- MODAL CHO CỘT ĐÃ XONG (DONE) -->
<c:set var="allTasksToRender" value="${doneTasks}" />
<c:forEach items="${allTasksToRender}" var="task">
    <div class="modal fade" id="taskDetailModal-${task.id}" tabindex="-1" aria-labelledby="taskDetailModalLabel-${task.id}" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-xl">
            <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
                <div class="modal-header bg-light px-4 py-3 border-bottom">
                    <div class="d-flex align-items-center gap-2">
                        <span class="badge ${task.priorityBadgeClass} rounded-pill px-3 py-1 fs-8 fw-semibold">
                            ${task.priorityLabel}
                        </span>
                        <h5 class="modal-title fw-bold text-dark mb-0 text-decoration-line-through" id="taskDetailModalLabel-${task.id}">
                            ${task.title}
                        </h5>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>
                <div class="modal-body p-0">
                    <div class="row g-0">
                        <div class="col-12 col-md-7 p-4 border-end">
                            <div class="row g-3 p-3 bg-light rounded-3 border mb-4">
                                <div class="col-6 col-sm-4">
                                    <span class="text-muted fs-9 d-block mb-1">Trạng thái</span>
                                    <span class="badge bg-success rounded-pill px-2 py-1 fs-9">Đã xong</span>
                                </div>
                                <div class="col-6 col-sm-4">
                                    <span class="text-muted fs-9 d-block mb-1">Trưởng nhóm Task (Lead)</span>
                                    <span class="fw-semibold text-dark fs-8 d-inline-flex align-items-center gap-1">
                                        <i class="bi bi-person-circle text-primary"></i> ${task.assigneeName}
                                    </span>
                                </div>
                                <div class="col-12 col-sm-4">
                                    <span class="text-muted fs-9 d-block mb-1">Hạn hoàn thành</span>
                                    <span class="fw-semibold text-dark fs-8 d-inline-flex align-items-center gap-1">
                                        <i class="bi bi-calendar-event"></i> ${not empty task.dueDate ? task.dueDate : 'Chưa đặt hạn'}
                                    </span>
                                </div>
                            </div>

                            <div class="mb-4 p-3 bg-light rounded-3 border">
                                <div class="d-flex align-items-center justify-content-between mb-1">
                                    <span class="fw-bold text-dark fs-8">
                                        <i class="bi bi-graph-up-arrow text-success me-1"></i> Tiến độ hoàn thành Task
                                    </span>
                                    <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-1 fs-9 fw-bold">
                                        ${taskProgressMap[task.id]}%
                                    </span>
                                </div>
                                <div class="progress" style="height: 6px;">
                                    <div class="progress-bar bg-success rounded-pill" role="progressbar" 
                                         style="width: ${taskProgressMap[task.id]}%;" 
                                         aria-valuenow="${taskProgressMap[task.id]}" aria-valuemin="0" aria-valuemax="100"></div>
                                </div>
                                <div class="d-flex justify-content-between mt-1 fs-9 text-muted">
                                    <span>Cộng dồn từ ${not empty taskSubTasksMap[task.id] ? taskSubTasksMap[task.id].size() : 0} việc con</span>
                                    <c:if test="${taskProgressMap[task.id] == 100}">
                                        <span class="text-success fw-bold"><i class="bi bi-check-circle-fill"></i> Hoàn thành 100%</span>
                                    </c:if>
                                </div>
                            </div>

                            <div class="mb-4">
                                <h6 class="fw-bold text-dark fs-7 mb-2">
                                    <i class="bi bi-text-left text-primary me-1"></i> Mô tả yêu cầu công việc
                                </h6>
                                <div class="p-3 bg-white rounded-3 border text-dark fs-7 lh-base" style="white-space: pre-line;">
                                    <c:choose>
                                        <c:when test="${not empty task.description}">
                                            <c:out value="${task.description}" />
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted fst-italic">Không có mô tả chi tiết cho công việc này.</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>

                            <div class="mb-4">
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <h6 class="fw-bold text-dark fs-7 mb-0">
                                        <i class="bi bi-list-check text-primary me-1"></i> Danh sách việc con (Sub-tasks)
                                    </h6>
                                    <span class="badge bg-secondary-subtle text-secondary rounded-pill px-2 py-1 fs-9">
                                        ${not empty taskSubTasksMap[task.id] ? taskSubTasksMap[task.id].size() : 0} việc
                                    </span>
                                </div>

                                <c:if test="${not empty taskSubTasksMap[task.id]}">
                                    <div class="d-flex flex-column gap-2 mb-3">
                                        <c:forEach items="${taskSubTasksMap[task.id]}" var="st">
                                            <div class="d-flex align-items-center justify-content-between p-2 px-3 bg-light rounded-3 border ${st.completed ? 'opacity-75' : ''}">
                                                <div class="d-flex align-items-center gap-2 flex-grow-1">
                                                    <form method="post" action="${pageContext.request.contextPath}/task" class="m-0 d-flex align-items-center">
                                                        <input type="hidden" name="action" value="toggleSubTask">
                                                        <input type="hidden" name="projectId" value="${project.id}">
                                                        <input type="hidden" name="subTaskId" value="${st.id}">
                                                        <input type="hidden" name="completed" value="${!st.completed}">
                                                        <input class="form-check-input mt-0 cursor-pointer" 
                                                               type="checkbox" 
                                                               ${st.completed ? 'checked' : ''} 
                                                               onchange="this.form.submit()" 
                                                               title="${st.completed ? 'Bấm để đánh dấu chưa xong' : 'Bấm để hoàn thành và đóng góp tiến độ'}">
                                                    </form>
                                                    <span class="fs-8 text-dark ${st.completed ? 'text-decoration-line-through text-muted' : 'fw-medium'}">
                                                        ${st.title}
                                                    </span>
                                                </div>

                                                <div class="d-flex align-items-center gap-2">
                                                    <span class="badge bg-white text-secondary border rounded-pill px-2 py-1 fs-9" title="Người phụ trách việc con">
                                                        <i class="bi bi-person-fill text-primary"></i> ${st.assigneeName}
                                                    </span>
                                                    <c:if test="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}">
                                                        <form method="post" action="${pageContext.request.contextPath}/task" class="m-0 d-inline" onsubmit="return confirm('Bạn có chắc chắn muốn xóa việc con này?');">
                                                            <input type="hidden" name="action" value="deleteSubTask">
                                                            <input type="hidden" name="projectId" value="${project.id}">
                                                            <input type="hidden" name="subTaskId" value="${st.id}">
                                                            <button type="submit" class="btn btn-link text-muted text-hover-danger p-0 border-0 fs-8" title="Xóa việc con">
                                                                <i class="bi bi-x-circle"></i>
                                                            </button>
                                                        </form>
                                                    </c:if>
                                                </div>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:if>

                                <c:if test="${empty taskSubTasksMap[task.id]}">
                                    <div class="p-3 bg-light-subtle rounded-3 text-muted fs-8 border text-center mb-3">
                                        Chưa có việc con nào được tạo.
                                    </div>
                                </c:if>

                                <c:if test="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}">
                                    <div class="p-3 bg-white rounded-3 border border-primary-subtle">
                                        <span class="fs-9 fw-bold text-primary d-block mb-2">
                                            <i class="bi bi-plus-circle-fill me-1"></i> Giao thêm việc con (Dành cho Task Lead)
                                        </span>
                                        <form method="post" action="${pageContext.request.contextPath}/task" class="d-flex flex-column gap-2">
                                            <input type="hidden" name="action" value="addSubTask">
                                            <input type="hidden" name="projectId" value="${project.id}">
                                            <input type="hidden" name="taskId" value="${task.id}">

                                            <div class="row g-2">
                                                <div class="col-12 col-md-7">
                                                    <input type="text" 
                                                           name="title" 
                                                           class="form-control form-control-sm fs-8 rounded-3" 
                                                           placeholder="Nhập tên việc con cần giao..." 
                                                           required>
                                                </div>
                                                <div class="col-8 col-md-3">
                                                    <select name="assigneeId" class="form-select form-select-sm fs-8 rounded-3">
                                                        <option value="0">-- Chọn người làm --</option>
                                                        <c:forEach items="${userList}" var="u">
                                                            <option value="${u.id}">${u.fullName}</option>
                                                        </c:forEach>
                                                    </select>
                                                </div>
                                                <div class="col-4 col-md-2">
                                                    <button type="submit" class="btn btn-primary-custom btn-sm w-100 rounded-3 fs-8 fw-semibold">
                                                        + Giao
                                                    </button>
                                                </div>
                                            </div>
                                        </form>
                                    </div>
                                </c:if>
                            </div>

                            <div>
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <h6 class="fw-bold text-dark fs-7 mb-0">
                                        <i class="bi bi-journal-bookmark text-primary me-1"></i> Tài liệu hướng dẫn đính kèm
                                    </h6>
                                    <span class="badge bg-primary-subtle text-primary rounded-pill px-2 py-1 fs-9">
                                        ${not empty taskDocsMap[task.id] ? taskDocsMap[task.id].size() : 0} tài liệu
                                    </span>
                                </div>
                                <c:if test="${not empty taskDocsMap[task.id]}">
                                    <div class="d-flex flex-column gap-2">
                                        <c:forEach items="${taskDocsMap[task.id]}" var="td">
                                            <div class="d-flex align-items-center justify-content-between p-2 px-3 bg-light rounded-3 border">
                                                <div class="d-flex align-items-center gap-2 text-truncate">
                                                    <i class="bi bi-file-earmark-text text-primary fs-6"></i>
                                                    <span class="fw-semibold text-dark fs-8 text-truncate">${td.docTitle}</span>
                                                </div>
                                                <a href="${pageContext.request.contextPath}/doc?action=view&projectId=${project.id}&docId=${td.docId}" 
                                                   class="btn btn-outline-primary btn-xs rounded-pill px-3 py-1 fs-9 text-nowrap"
                                                   title="Đọc bài viết Wiki này">
                                                    Đọc bài <i class="bi bi-arrow-right ms-1"></i>
                                                </a>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:if>
                                <c:if test="${empty taskDocsMap[task.id]}">
                                    <div class="p-3 bg-light-subtle rounded-3 text-muted fs-8 border text-center">
                                        Chưa có tài liệu Wiki nào được đính kèm cho công việc này.
                                    </div>
                                </c:if>
                            </div>
                        </div>

                        <div class="col-12 col-md-5 p-4 d-flex flex-column bg-light-subtle" style="min-height: 520px;">
                            <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                                <div class="d-flex align-items-center gap-2">
                                    <i class="bi bi-chat-square-dots-fill text-primary"></i>
                                    <h6 class="fw-bold mb-0 text-dark fs-7">Hội thoại của Task</h6>
                                </div>
                                <span class="badge bg-secondary rounded-pill px-2 py-1 fs-9">
                                    ${not empty taskCommentsMap[task.id] ? taskCommentsMap[task.id].size() : 0}
                                </span>
                            </div>
                            <div class="flex-grow-1 overflow-y-auto d-flex flex-column gap-2 mb-3 pe-1" style="max-height: 380px;">
                                <c:forEach items="${taskCommentsMap[task.id]}" var="comment">
                                    <div class="p-2 px-3 rounded-3 border shadow-2xs ${comment.authorName == 'Hệ Thống' ? 'bg-success-subtle border-success-subtle' : 'bg-white'}">
                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                            <span class="fw-bold fs-8 ${comment.authorName == 'Hệ Thống' ? 'text-success' : 'text-dark'}">
                                                <c:choose>
                                                    <c:when test="${comment.authorName == 'Hệ Thống'}">
                                                        <i class="bi bi-robot me-1"></i> Hệ Thống
                                                    </c:when>
                                                    <c:otherwise>
                                                        ${comment.authorName}
                                                    </c:otherwise>
                                                </c:choose>
                                            </span>
                                            <span class="text-muted fs-9">${comment.sentAt}</span>
                                        </div>
                                        <div class="fs-8 text-dark lh-base" style="word-break: break-word; white-space: pre-line;">
                                            <c:out value="${comment.content}" />
                                        </div>
                                    </div>
                                </c:forEach>
                                <c:if test="${empty taskCommentsMap[task.id]}">
                                    <div class="text-center text-muted my-auto py-4">
                                        <i class="bi bi-chat-left-dots fs-3 d-block mb-1 opacity-50"></i>
                                        <span class="fs-8">Chưa có bình luận nào. Hãy gửi phản hồi đầu tiên!</span>
                                    </div>
                                </c:if>
                            </div>
                            <div class="pt-2 border-top mt-auto">
                                <form method="post" action="${pageContext.request.contextPath}/chat" class="d-flex flex-column gap-2">
                                    <input type="hidden" name="action" value="sendTaskComment">
                                    <input type="hidden" name="projectId" value="${project.id}">
                                    <input type="hidden" name="taskId" value="${task.id}">
                                    <div class="input-group">
                                        <input type="text" 
                                               class="form-control fs-8 py-2 rounded-start-pill ps-3 shadow-none border-secondary-subtle" 
                                               name="content" 
                                               placeholder="Viết bình luận cho task này..." 
                                               autocomplete="off" 
                                               required>
                                        <button type="submit" class="btn btn-primary-custom rounded-end-pill px-3 fs-8 fw-semibold shadow-sm">
                                            <i class="bi bi-send-fill me-1"></i> Gửi
                                        </button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</c:forEach>

<!-- ==========================================
     5. MODAL POPUP: FORM "+ THÊM CÔNG VIỆC MỚI" (UC05)
     ========================================== -->
<div class="modal fade" id="addTaskModal" tabindex="-1" aria-labelledby="addTaskModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
            
            <!-- Đầu Modal -->
            <div class="modal-header bg-primary text-white px-4 py-3 border-0">
                <h5 class="modal-title fw-bold" id="addTaskModalLabel">
                    <i class="bi bi-plus-circle-dotted me-2"></i> Thêm thẻ công việc lớn (Task Cha)
                </h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Đóng"></button>
            </div>

            <!-- Form gửi dữ liệu lên TaskServlet (doPost) -->
            <form method="post" action="${pageContext.request.contextPath}/task">
                
                <input type="hidden" name="action" value="add">
                <input type="hidden" name="projectId" value="${project.id}">

                <div class="modal-body px-4 py-4">
                    
                    <!-- Ô 1: Tiêu đề công việc -->
                    <div class="mb-3">
                        <label for="taskTitle" class="form-label fw-semibold text-dark fs-7">
                            Tiêu đề công việc <span class="text-danger">*</span>
                        </label>
                        <input type="text" 
                               class="form-control rounded-3 py-2 px-3 fs-7" 
                               id="taskTitle" 
                               name="title" 
                               placeholder="Ví dụ: Xây dựng Module Thanh toán VNPAY..." 
                               required>
                    </div>

                    <!-- Ô 2: Mô tả chi tiết -->
                    <div class="mb-3">
                        <label for="taskDescription" class="form-label fw-semibold text-dark fs-7">Mô tả yêu cầu</label>
                        <textarea class="form-control rounded-3 p-3 fs-7" 
                                  id="taskDescription" 
                                  name="description" 
                                  rows="3" 
                                  placeholder="Mô tả mục tiêu của Module này..."></textarea>
                    </div>

                    <!-- Hàng đôi: Mức độ ưu tiên & Hạn chót -->
                    <div class="row g-3 mb-3">
                        <div class="col-12 col-md-6">
                            <label for="taskPriority" class="form-label fw-semibold text-dark fs-7">Mức độ ưu tiên</label>
                            <select class="form-select rounded-3 py-2 px-3 fs-7" id="taskPriority" name="priority">
                                <option value="HIGH">🔴 Cao (High)</option>
                                <option value="MEDIUM" selected>🟡 Trung bình (Medium)</option>
                                <option value="LOW">🔵 Thấp (Low)</option>
                            </select>
                        </div>

                        <div class="col-12 col-md-6">
                            <label for="taskDueDate" class="form-label fw-semibold text-dark fs-7">Hạn hoàn thành</label>
                            <input type="date" 
                                   class="form-control rounded-3 py-2 px-3 fs-7" 
                                   id="taskDueDate" 
                                   name="dueDate">
                        </div>
                    </div>

                    <!-- Hàng đôi: Người đứng đầu Task (Task Lead) & Đính kèm tài liệu -->
                    <div class="row g-3 mb-2">
                        <div class="col-12 col-md-6">
                            <label for="taskAssignee" class="form-label fw-semibold text-dark fs-7">Chỉ định Trưởng nhóm Task (Lead)</label>
                            <select class="form-select rounded-3 py-2 px-3 fs-7" id="taskAssignee" name="assigneeId">
                                <option value="0">-- Chưa chỉ định --</option>
                                <c:forEach items="${userList}" var="u">
                                    <option value="${u.id}">${u.fullName} (${u.role})</option>
                                </c:forEach>
                            </select>
                        </div>

                        <div class="col-12 col-md-6">
                            <label for="taskDocSelect" class="form-label fw-semibold text-dark fs-7">
                                <i class="bi bi-paperclip me-1 text-primary"></i> Đính kèm tài liệu Wiki
                            </label>
                            <c:choose>
                                <c:when test="${not empty docList}">
                                    <select class="form-select rounded-3 py-1 px-3 fs-8" 
                                            id="taskDocSelect" 
                                            name="docIds" 
                                            multiple 
                                            size="3" 
                                            title="Giữ Ctrl hoặc Cmd để chọn nhiều tài liệu">
                                        <c:forEach items="${docList}" var="docItem">
                                            <option value="${docItem.id}">📄 ${docItem.title}</option>
                                        </c:forEach>
                                    </select>
                                    <div class="form-text fs-9 text-muted mt-1">Giữ phím <kbd>Ctrl</kbd> để chọn nhiều tài liệu cùng lúc.</div>
                                </c:when>
                                <c:otherwise>
                                    <div class="p-2 bg-light rounded-3 text-muted fs-8 border">
                                        Chưa có tài liệu nào trong dự án.
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                </div>

                <!-- Chân Modal: Nút Hủy và Nút Lưu -->
                <div class="modal-footer px-4 py-3 bg-light border-0">
                    <button type="button" class="btn btn-light rounded-pill px-3 fs-7 fw-medium" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary-custom rounded-pill px-4 py-2 fs-7 fw-semibold shadow-sm">
                        <i class="bi bi-check-lg me-1"></i> Lưu công việc
                    </button>
                </div>

            </form>
        </div>
    </div>
</div>

<!-- 6. NẠP FILE JAVASCRIPT KÉO THẢ CHUỘT (HTML5 DRAG & DROP) -->
<script src="${pageContext.request.contextPath}/js/tasks.js"></script>

<!-- 7. NẠP FOOTER CHUNG -->
<jsp:include page="/includes/footer.jsp" />
