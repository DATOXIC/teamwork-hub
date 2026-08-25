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
                                
                                <!-- Link xóa task (Dùng stopPropagation để không kích hoạt Modal) -->
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

                            <!-- Hàng 3.5: Huy hiệu Tài liệu đính kèm & Bình luận -->
                            <div class="d-flex flex-wrap align-items-center gap-1 mb-2">
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
                                <div class="d-flex align-items-center gap-1" title="Người thực hiện">
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

                            <!-- Huy hiệu Tài liệu đính kèm & Bình luận -->
                            <div class="d-flex flex-wrap align-items-center gap-1 mb-2">
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
                                <div class="d-flex align-items-center gap-1" title="Người thực hiện">
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

                            <!-- Huy hiệu Tài liệu đính kèm & Bình luận -->
                            <div class="d-flex flex-wrap align-items-center gap-1 mb-2">
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
                                <div class="d-flex align-items-center gap-1" title="Người thực hiện">
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
     4. MODAL CHI TIẾT TASK 2 CỘT (TASK MINI-HUB - TRỤ CỘT 1)
     Được render trước cho tất cả các task trong 3 cột
     ======================================================== -->
<c:forEach items="${todoTasks}" var="task">
    <jsp:include page="/includes/task_modal_detail.jsp" />
</c:forEach>
<c:forEach items="${inProgressTasks}" var="task">
    <jsp:include page="/includes/task_modal_detail.jsp" />
</c:forEach>
<c:forEach items="${doneTasks}" var="task">
    <jsp:include page="/includes/task_modal_detail.jsp" />
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
                    <i class="bi bi-plus-circle-dotted me-2"></i> Thêm thẻ công việc mới
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
                               placeholder="Ví dụ: Thiết kế giao diện đăng nhập..." 
                               required>
                    </div>

                    <!-- Ô 2: Mô tả chi tiết -->
                    <div class="mb-3">
                        <label for="taskDescription" class="form-label fw-semibold text-dark fs-7">Mô tả yêu cầu</label>
                        <textarea class="form-control rounded-3 p-3 fs-7" 
                                  id="taskDescription" 
                                  name="description" 
                                  rows="3" 
                                  placeholder="Mô tả cụ thể những việc cần làm..."></textarea>
                    </div>

                    <!-- Hàng đôi: Mức độ ưu tiên & Hạn chót -->
                    <div class="row g-3 mb-3">
                        <!-- Mức độ ưu tiên -->
                        <div class="col-12 col-md-6">
                            <label for="taskPriority" class="form-label fw-semibold text-dark fs-7">Mức độ ưu tiên</label>
                            <select class="form-select rounded-3 py-2 px-3 fs-7" id="taskPriority" name="priority">
                                <option value="HIGH">🔴 Cao (High)</option>
                                <option value="MEDIUM" selected>🟡 Trung bình (Medium)</option>
                                <option value="LOW">🔵 Thấp (Low)</option>
                            </select>
                        </div>

                        <!-- Hạn chót -->
                        <div class="col-12 col-md-6">
                            <label for="taskDueDate" class="form-label fw-semibold text-dark fs-7">Hạn hoàn thành</label>
                            <input type="date" 
                                   class="form-control rounded-3 py-2 px-3 fs-7" 
                                   id="taskDueDate" 
                                   name="dueDate">
                        </div>
                    </div>

                    <!-- Hàng đôi: Người thực hiện & Đính kèm tài liệu -->
                    <div class="row g-3 mb-2">
                        
                        <!-- Ô 3: Người thực hiện (Đổ danh sách từ userList) -->
                        <div class="col-12 col-md-6">
                            <label for="taskAssignee" class="form-label fw-semibold text-dark fs-7">Giao cho thành viên</label>
                            <select class="form-select rounded-3 py-2 px-3 fs-7" id="taskAssignee" name="assigneeId">
                                <option value="0">-- Chưa phân công --</option>
                                <c:forEach items="${userList}" var="u">
                                    <option value="${u.id}">${u.fullName} (${u.role})</option>
                                </c:forEach>
                            </select>
                        </div>

                        <!-- Ô 4: ĐÍNH KÈM TÀI LIỆU HƯỚNG DẪN (TaskDoc - Multi Select) -->
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
