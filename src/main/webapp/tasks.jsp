<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!-- 1. NẠP HEADER & THANH ĐIỀU HƯỚNG CHUNG -->
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container-fluid px-lg-5 py-4">

    <!-- 2. THANH TIÊU ĐỀ DỰ ÁN & CÁC NÚT ĐIỀU HƯỚNG TRÊN CÙNG -->
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-3 pb-3 border-bottom bg-white p-3 rounded-4 shadow-sm">
        
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
                        <span class="badge bg-dark-navy text-white rounded-pill px-2 py-1 fs-9" title="Mã chia sẻ dự án">
                            <i class="bi bi-hash"></i> ${project.projectCode}
                        </span>
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

        <!-- Cụm bên phải: Nút Đội ngũ + Mời thành viên + Thêm công việc -->
        <div class="d-flex align-items-center gap-2">
            <!-- Nút Xem Đội Ngũ Dự Án (Quota X/10) -->
            <button type="button" class="btn btn-outline-primary btn-sm rounded-pill px-3 py-2 fw-semibold shadow-sm fs-8 d-flex align-items-center gap-1"
                    data-bs-toggle="modal" data-bs-target="#projectTeamModal" title="Xem danh sách đội ngũ và lời mời">
                <i class="bi bi-people-fill"></i> Đội ngũ (${memberCount}/10)
            </button>

            <!-- Nút Mời Thành Viên (Dành riêng cho PM) -->
            <c:if test="${project.ownerId == sessionScope.currentUser.id}">
                <button type="button" class="btn btn-success btn-sm rounded-pill px-3 py-2 fw-semibold shadow-sm fs-8 d-flex align-items-center gap-1"
                        data-bs-toggle="modal" data-bs-target="#inviteMemberModal" title="Mời thành viên mới vào dự án">
                    <i class="bi bi-person-plus-fill"></i> + Mời Đồng Đội
                </button>
            </c:if>

            <!-- Nút Thêm công việc lớn -->
            <button type="button" class="btn btn-primary-custom btn-sm px-3 py-2 rounded-pill fw-semibold shadow-sm fs-8 d-flex align-items-center gap-1"
                    data-bs-toggle="modal" data-bs-target="#addTaskModal">
                <i class="bi bi-plus-circle"></i> Thêm công việc
            </button>
        </div>
    </div>

    <!-- Thông báo Flash (Toast Messages) -->
    <c:if test="${not empty toastSuccess}">
        <div class="alert alert-success alert-dismissible fade show fs-7 py-2 px-3 mb-4 rounded-3 border-0 shadow-sm d-flex align-items-center" role="alert">
            <i class="bi bi-check-circle-fill me-2 fs-6"></i>
            <div class="flex-grow-1">${toastSuccess}</div>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>
    <c:if test="${not empty toastError}">
        <div class="alert alert-danger alert-dismissible fade show fs-7 py-2 px-3 mb-4 rounded-3 border-0 shadow-sm d-flex align-items-center" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2 fs-6"></i>
            <div class="flex-grow-1">${toastError}</div>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <!-- =========================================================================
         2.5. THANH DẢI AVATAR LỌC VIỆC & THẺ HỒ SƠ ĐỒNG ĐỘI (PHẦN B.3.3)
         ========================================================================= -->
    <div class="bg-white p-3 rounded-4 shadow-2xs border mb-4">
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2">
            <span class="fs-8 fw-bold text-dark text-uppercase tracking-wider">
                <i class="bi bi-funnel-fill text-primary me-1"></i> Lọc công việc theo thành viên:
            </span>
            <span class="fs-9 text-muted" id="filterResultCount">
                Hiển thị tất cả <strong>${todoTasks.size() + inProgressTasks.size() + doneTasks.size()}</strong> công việc
            </span>
        </div>

        <!-- Dải các nút bấm lọc (Pill Filter Bar) -->
        <div class="d-flex flex-wrap align-items-center gap-2" id="memberFilterBar">
            
            <!-- Nút 1: Tất cả công việc (Mặc định active) -->
            <button type="button" 
                    class="btn btn-sm btn-primary-custom text-white rounded-pill px-3 py-1 fs-8 fw-semibold member-filter-btn active" 
                    data-filter-mode="ALL" 
                    data-related-tasks="ALL"
                    title="Hiển thị toàn bộ công việc trong dự án">
                <i class="bi bi-people-fill me-1"></i> Tất cả (${todoTasks.size() + inProgressTasks.size() + doneTasks.size()})
            </button>

            <!-- Nút 2: Việc của tôi (Chỉ hiện việc có liên quan đến user đang đăng nhập) -->
            <c:forEach items="${userWorkloadList}" var="uw">
                <c:if test="${uw.user.id == sessionScope.currentUser.id}">
                    <button type="button" 
                            class="btn btn-sm btn-outline-primary rounded-pill px-3 py-1 fs-8 fw-semibold member-filter-btn" 
                            data-filter-mode="MY_TASKS" 
                            data-related-tasks="${uw.relatedTaskIdsJoined}"
                            title="Chỉ hiển thị các Task do bạn làm Lead hoặc có việc con của bạn">
                        <i class="bi bi-lightning-charge-fill text-warning me-1"></i> Việc của tôi (${uw.totalWorkCount})
                    </button>
                </c:if>
            </c:forEach>

            <div class="vr mx-1 d-none d-md-block text-secondary opacity-25"></div>

            <!-- Nút cho từng thành viên trong hệ thống -->
            <c:forEach items="${userWorkloadList}" var="uw">
                <div class="btn-group" role="group">
                    
                    <!-- Nút lọc việc của thành viên này -->
                    <button type="button" 
                            class="btn btn-sm btn-outline-secondary rounded-start-pill ps-3 pe-2 py-1 fs-8 member-filter-btn d-inline-flex align-items-center gap-2" 
                            data-filter-mode="USER" 
                            data-user-id="${uw.user.id}"
                            data-user-name="${uw.user.fullName}"
                            data-related-tasks="${uw.relatedTaskIdsJoined}"
                            title="Bấm để lọc công việc của ${uw.user.fullName}">
                        
                        <i class="bi bi-person-circle ${uw.user.id == project.ownerId ? 'text-warning' : 'text-primary'}"></i>
                        <span class="fw-medium">${uw.user.fullName}</span>
                        
                        <!-- Huy hiệu Khối lượng công việc -->
                        <div class="d-flex align-items-center gap-1">
                            <c:if test="${uw.leadTaskCount > 0}">
                                <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-1-5 py-0 fs-9" title="Làm Task Lead ${uw.leadTaskCount} Task lớn">
                                    👑 ${uw.leadTaskCount}
                                </span>
                            </c:if>
                            <c:if test="${uw.subTaskCount > 0}">
                                <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle rounded-pill px-1-5 py-0 fs-9" title="Được giao ${uw.subTaskCount} việc con (${uw.completedSubTaskCount} đã xong)">
                                    📋 ${uw.subTaskCount}
                                </span>
                            </c:if>
                        </div>
                    </button>

                    <!-- Nút (i) mở Thẻ Hồ Sơ Đồng Đội (Profile Card Modal) -->
                    <button type="button" 
                            class="btn btn-sm btn-outline-secondary rounded-end-pill px-2 py-1 fs-8" 
                            data-bs-toggle="modal" 
                            data-bs-target="#memberProfileModal-${uw.user.id}" 
                            title="Xem Thẻ Hồ Sơ & Năng suất của ${uw.user.fullName}">
                        <i class="bi bi-info-circle"></i>
                    </button>
                </div>
            </c:forEach>

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
                    
                    <c:forEach items="${todoTasks}" var="task">
                        <div class="card kanban-card border-0 bg-white shadow-sm p-3 rounded-3 cursor-grab"
                             id="task-${task.id}"
                             draggable="true" 
                             data-task-id="${task.id}"
                             data-bs-toggle="modal" 
                             data-bs-target="#taskDetailModal-${task.id}"
                             style="cursor: pointer;">
                            
                            <div class="d-flex align-items-center justify-content-between mb-2">
                                <div class="d-flex align-items-center gap-1">
                                    <span class="badge ${task.priorityBadgeClass} rounded-pill px-2 py-1 fs-9 fw-semibold">
                                        ${task.priorityLabel}
                                    </span>
                                    <span class="badge ${task.statusBadgeClass} rounded-pill px-2 py-1 fs-9">
                                        ${task.statusLabel}
                                    </span>
                                </div>
                                
                                <c:if test="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}">
                                    <a href="${pageContext.request.contextPath}/task?action=delete&taskId=${task.id}&projectId=${project.id}" 
                                       class="text-muted text-hover-danger text-decoration-none p-1"
                                       onclick="event.stopPropagation(); return confirm('Bạn có chắc chắn muốn xóa thẻ công việc này không?');"
                                       title="Xóa công việc">
                                        <i class="bi bi-trash3"></i>
                                    </a>
                                </c:if>
                            </div>

                            <h6 class="fw-bold text-dark mb-1 fs-6">${task.title}</h6>

                            <c:if test="${not empty task.description}">
                                <p class="text-muted fs-7 mb-2 text-truncate-2">${task.description}</p>
                            </c:if>

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

                            <c:if test="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}">
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
                            </c:if>

                        </div>
                    </c:forEach>

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
                
                <div class="d-flex align-items-center justify-content-between mb-3 px-1">
                    <div class="d-flex align-items-center gap-2">
                        <span class="p-2 bg-primary-subtle text-primary rounded-3">
                            <i class="bi bi-arrow-repeat"></i>
                        </span>
                        <h6 class="fw-bold mb-0 text-dark">Đang làm (In Progress)</h6>
                    </div>
                    <span class="badge bg-primary rounded-pill px-2 py-1 fs-8">${inProgressTasks.size()}</span>
                </div>

                <div class="kanban-task-list d-flex flex-column gap-3 flex-grow-1" 
                     id="column-IN_PROGRESS" 
                     data-status="IN_PROGRESS">
                    
                    <c:forEach items="${inProgressTasks}" var="task">
                        <div class="card kanban-card border-0 bg-white shadow-sm p-3 rounded-3 cursor-grab"
                             id="task-${task.id}"
                             draggable="true" 
                             data-task-id="${task.id}"
                             data-bs-toggle="modal" 
                             data-bs-target="#taskDetailModal-${task.id}"
                             style="cursor: pointer;">
                            
                            <div class="d-flex align-items-center justify-content-between mb-2">
                                <div class="d-flex align-items-center gap-1">
                                    <span class="badge ${task.priorityBadgeClass} rounded-pill px-2 py-1 fs-9 fw-semibold">
                                        ${task.priorityLabel}
                                    </span>
                                    <span class="badge ${task.statusBadgeClass} rounded-pill px-2 py-1 fs-9">
                                        ${task.statusLabel}
                                    </span>
                                </div>
                                
                                <c:if test="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}">
                                    <a href="${pageContext.request.contextPath}/task?action=delete&taskId=${task.id}&projectId=${project.id}" 
                                       class="text-muted text-hover-danger text-decoration-none p-1"
                                       onclick="event.stopPropagation(); return confirm('Bạn có chắc chắn muốn xóa thẻ công việc này không?');"
                                       title="Xóa công việc">
                                        <i class="bi bi-trash3"></i>
                                    </a>
                                </c:if>
                            </div>

                            <h6 class="fw-bold text-dark mb-1 fs-6">${task.title}</h6>

                            <c:if test="${not empty task.description}">
                                <p class="text-muted fs-7 mb-2 text-truncate-2">${task.description}</p>
                            </c:if>

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

                            <c:if test="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}">
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
                            </c:if>

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
                
                <div class="d-flex align-items-center justify-content-between mb-3 px-1">
                    <div class="d-flex align-items-center gap-2">
                        <span class="p-2 bg-success-subtle text-success rounded-3">
                            <i class="bi bi-check-circle"></i>
                        </span>
                        <h6 class="fw-bold mb-0 text-dark">Đã xong (Done)</h6>
                    </div>
                    <span class="badge bg-success rounded-pill px-2 py-1 fs-8">${doneTasks.size()}</span>
                </div>

                <div class="kanban-task-list d-flex flex-column gap-3 flex-grow-1" 
                     id="column-DONE" 
                     data-status="DONE">
                    
                    <c:forEach items="${doneTasks}" var="task">
                        <div class="card kanban-card border-0 bg-white shadow-sm p-3 rounded-3 cursor-grab opacity-75"
                             id="task-${task.id}"
                             draggable="true" 
                             data-task-id="${task.id}"
                             data-bs-toggle="modal" 
                             data-bs-target="#taskDetailModal-${task.id}"
                             style="cursor: pointer;">
                            
                            <div class="d-flex align-items-center justify-content-between mb-2">
                                <div class="d-flex align-items-center gap-1">
                                    <span class="badge ${task.priorityBadgeClass} rounded-pill px-2 py-1 fs-9 fw-semibold">
                                        ${task.priorityLabel}
                                    </span>
                                    <span class="badge ${task.statusBadgeClass} rounded-pill px-2 py-1 fs-9">
                                        ${task.statusLabel}
                                    </span>
                                </div>
                                
                                <c:if test="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}">
                                    <a href="${pageContext.request.contextPath}/task?action=delete&taskId=${task.id}&projectId=${project.id}" 
                                       class="text-muted text-hover-danger text-decoration-none p-1"
                                       onclick="event.stopPropagation(); return confirm('Bạn có chắc chắn muốn xóa thẻ công việc này không?');"
                                       title="Xóa công việc">
                                        <i class="bi bi-trash3"></i>
                                    </a>
                                </c:if>
                            </div>

                            <h6 class="fw-bold text-dark mb-1 fs-6 text-decoration-line-through">${task.title}</h6>

                            <c:if test="${not empty task.description}">
                                <p class="text-muted fs-7 mb-2 text-truncate-2">${task.description}</p>
                            </c:if>

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

                            <c:if test="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}">
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
                            </c:if>

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

<!-- =========================================================================
     4. MODAL CHI TIẾT TASK 2 CỘT (TASK MINI-HUB & SUB-TASKS)
     ========================================================================= -->

<!-- MODAL CHO CỘT CẦN LÀM (TODO) -->
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
                        <div class="col-12 col-md-7 p-4 border-end">
                            <div class="row g-3 p-3 bg-light rounded-3 border mb-4">
                                <div class="col-6 col-sm-4">
                                    <span class="text-muted fs-9 d-block mb-1">Trạng thái</span>
                                    <span class="badge ${task.statusBadgeClass} rounded-pill px-2 py-1 fs-9">${task.statusLabel}</span>
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
                                    <i class="bi bi-text-left text-primary me-1"></i> Mô tả chi tiết
                                </h6>
                                <div class="p-3 bg-light rounded-3 text-secondary fs-8 border lh-base" style="white-space: pre-line;">
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
                                    <div class="d-flex flex-column gap-3 mb-3">
                                        <c:forEach items="${taskSubTasksMap[task.id]}" var="st">
                                            <div class="p-3 bg-light rounded-3 border ${st.status == 'APPROVED' ? 'opacity-75' : ''}">
                                                
                                                <!-- DÒNG 1: TRẠNG THÁI + TIÊU ĐỀ + NGƯỜI PHỤ TRÁCH + NÚT XÓA -->
                                                <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-1">
                                                    <div class="d-flex align-items-center gap-2">
                                                        <!-- Huy hiệu 5 màu -->
                                                        <span class="badge ${st.statusBadgeClass} rounded-pill px-2 py-1 fs-9">
                                                            ${st.statusLabel}
                                                        </span>
                                                        <!-- Tiêu đề việc con -->
                                                        <span class="fs-8 text-dark ${st.status == 'APPROVED' ? 'text-decoration-line-through text-muted' : 'fw-bold'}">
                                                            ${st.title}
                                                        </span>
                                                    </div>

                                                    <div class="d-flex align-items-center gap-2">
                                                        <span class="badge bg-white text-secondary border rounded-pill px-2 py-1 fs-9" title="Người phụ trách">
                                                            <i class="bi bi-person-fill text-primary"></i> ${st.assigneeName}
                                                        </span>
                                                        <!-- Nút Xóa (Dành riêng cho Task Lead & PM) -->
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

                                                <!-- DÒNG 2: THÔNG TIN CHI TIẾT BÀN GIAO / GÓP Ý DỰA THEO TRẠNG THÁI -->
                                                
                                                <!-- TRƯỜNG HỢP A: SUBMITTED (Đang chờ duyệt) - Hiển thị kết quả nộp bài -->
                                                <c:if test="${st.status == 'SUBMITTED'}">
                                                    <div class="p-2 bg-white rounded-2 border fs-8 text-dark mt-2 shadow-2xs">
                                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                                            <span class="fw-bold text-warning fs-9">
                                                                <i class="bi bi-file-earmark-check-fill me-1"></i> Kết quả nộp bài:
                                                            </span>
                                                            <span class="fs-9 text-muted">${st.submittedAt}</span>
                                                        </div>
                                                        <p class="mb-0 text-secondary fs-8">
                                                            ${not empty st.submissionNote ? st.submissionNote : 'Đã hoàn thành công việc, mời Task Lead kiểm tra và nghiệm thu.'}
                                                        </p>
                                                    </div>
                                                </c:if>

                                                <!-- TRƯỜNG HỢP B: REVISE (🔵 Màu Xanh Dương - Cần cân chỉnh nhỏ) -->
                                                <c:if test="${st.status == 'REVISE'}">
                                                    <div class="p-2 bg-primary-subtle text-primary border border-primary-subtle rounded-2 fs-8 mt-2">
                                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                                            <span class="fw-bold fs-9">
                                                                <i class="bi bi-info-circle-fill me-1"></i> Dặn dò từ Task Lead:
                                                            </span>
                                                            <span class="fs-9 opacity-75">${st.reviewedAt}</span>
                                                        </div>
                                                        <p class="mb-0 fs-8">"${st.feedbackNote}"</p>
                                                    </div>
                                                </c:if>

                                                <!-- TRƯỜNG HỢP C: REJECTED (🔴 Màu Đỏ - Chưa đạt yêu cầu) -->
                                                <c:if test="${st.status == 'REJECTED'}">
                                                    <div class="p-2 bg-danger-subtle text-danger border border-danger-subtle rounded-2 fs-8 mt-2">
                                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                                            <span class="fw-bold fs-9">
                                                                <i class="bi bi-exclamation-triangle-fill me-1"></i> Lý do chưa đạt từ Task Lead:
                                                            </span>
                                                            <span class="fs-9 opacity-75">${st.reviewedAt}</span>
                                                        </div>
                                                        <p class="mb-0 fs-8">"${st.feedbackNote}"</p>
                                                    </div>
                                                </c:if>

                                                <!-- TRƯỜNG HỢP D: APPROVED (🟢 Màu Xanh Lá - Đã nghiệm thu Đạt) -->
                                                <c:if test="${st.status == 'APPROVED'}">
                                                    <div class="fs-9 text-success mt-1 d-flex align-items-center gap-1">
                                                        <i class="bi bi-check-circle-fill"></i>
                                                        <span>Đã nghiệm thu hoàn thành 100% &bull; ${st.reviewedAt}</span>
                                                    </div>
                                                </c:if>

                                                <!-- DÒNG 3: CÁC NÚT HÀNH ĐỘNG TƯƠNG TÁC (NỘP BÀI / THẨM ĐỊNH) -->
                                                <div class="mt-2 pt-2 border-top d-flex flex-wrap align-items-center justify-content-between gap-2">
                                                    
                                                    <!-- 1. NÚT DÀNH CHO THÀNH VIÊN ĐƯỢC GIAO VIỆC: Nộp Báo Cáo / Bàn giao kết quả -->
                                                    <c:set var="isAssignee" value="${st.assigneeId == sessionScope.currentUser.id || task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}" />
                                                    <div>
                                                        <c:if test="${isAssignee && st.status != 'APPROVED' && st.status != 'SUBMITTED'}">
                                                            <button type="button" class="btn btn-outline-primary btn-sm rounded-pill fs-9 py-1 px-3 fw-semibold shadow-2xs"
                                                                    data-bs-toggle="modal" data-bs-target="#submitSubTaskModal-${st.id}">
                                                                <i class="bi bi-upload me-1"></i> ${st.status == 'TODO' ? 'Nộp Báo Cáo Kết Quả' : 'Nộp Lại Kết Quả Mới'}
                                                            </button>
                                                        </c:if>
                                                    </div>

                                                    <!-- 2. NÚT DÀNH CHO TASK LEAD & PM KHI CÓ BÀI NỘP (🟡 SUBMITTED): 3 LỰA CHỌN THẨM ĐỊNH -->
                                                    <c:set var="isReviewer" value="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}" />
                                                    <c:if test="${isReviewer && st.status == 'SUBMITTED'}">
                                                        <div class="d-flex align-items-center gap-1 ms-auto">
                                                            <!-- Nút 1: DUYỆT ĐẠT 🟢 -->
                                                            <form method="post" action="${pageContext.request.contextPath}/task" class="m-0">
                                                                <input type="hidden" name="action" value="approveSubTask">
                                                                <input type="hidden" name="projectId" value="${project.id}">
                                                                <input type="hidden" name="subTaskId" value="${st.id}">
                                                                <button type="submit" class="btn btn-success btn-sm rounded-pill fs-9 py-1 px-2 fw-semibold shadow-2xs" title="Nghiệm thu đạt 100%">
                                                                    <i class="bi bi-check-lg me-1"></i> Duyệt Đạt
                                                                </button>
                                                            </form>

                                                            <!-- Nút 2: CẦN CÂN CHỈNH 🔵 (Xanh Dương) -->
                                                            <button type="button" class="btn btn-primary btn-sm rounded-pill fs-9 py-1 px-2 fw-semibold shadow-2xs" 
                                                                    data-bs-toggle="modal" data-bs-target="#reviseSubTaskModal-${st.id}" title="Yêu cầu tinh chỉnh nhỏ">
                                                                <i class="bi bi-pencil me-1"></i> Cân chỉnh
                                                            </button>

                                                            <!-- Nút 3: CHƯA ĐẠT 🔴 (Màu Đỏ) -->
                                                            <button type="button" class="btn btn-danger btn-sm rounded-pill fs-9 py-1 px-2 fw-semibold shadow-2xs" 
                                                                    data-bs-toggle="modal" data-bs-target="#rejectSubTaskModal-${st.id}" title="Trả về làm lại do chưa đạt">
                                                                <i class="bi bi-x-lg me-1"></i> Chưa đạt
                                                            </button>
                                                        </div>
                                                    </c:if>

                                                </div>

                                            </div>

                                            <!-- MODAL 1: NỘP BÁO CÁO KẾT QUẢ CHO SUBTASK NÀY -->
                                            <div class="modal fade" id="submitSubTaskModal-${st.id}" tabindex="-1" aria-labelledby="submitSubTaskModalLabel-${st.id}" aria-hidden="true">
                                                <div class="modal-dialog modal-dialog-centered">
                                                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                                                        <div class="modal-header border-0 pb-0">
                                                            <h6 class="modal-title fw-bold text-dark" id="submitSubTaskModalLabel-${st.id}">
                                                                <i class="bi bi-upload text-primary me-2"></i>Nộp Kết Quả: [${st.title}]
                                                            </h6>
                                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                        </div>
                                                        <form action="${pageContext.request.contextPath}/task" method="post">
                                                            <input type="hidden" name="action" value="submitSubTask">
                                                            <input type="hidden" name="projectId" value="${project.id}">
                                                            <input type="hidden" name="subTaskId" value="${st.id}">
                                                            <div class="modal-body py-3">
                                                                <label for="note-${st.id}" class="form-label fw-semibold fs-8 text-dark">
                                                                    Ghi chú hoàn thành / Link bàn giao kết quả <span class="text-danger">*</span>
                                                                </label>
                                                                <textarea class="form-control fs-8 rounded-3" id="note-${st.id}" name="submissionNote" rows="3" 
                                                                          placeholder="Mô tả những gì bạn đã làm, dán link pull request, link tài liệu hoặc ghi chú cho Leader..." required></textarea>
                                                            </div>
                                                            <div class="modal-footer border-0 pt-0">
                                                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                                                <button type="submit" class="btn btn-primary-custom rounded-pill px-4 fs-8 fw-semibold shadow-sm">
                                                                    <i class="bi bi-send-fill me-1"></i> Gửi Báo Cáo Cho Leader
                                                                </button>
                                                            </div>
                                                        </form>
                                                    </div>
                                                </div>
                                            </div>

                                            <!-- MODAL 2: YÊU CẦU CÂN CHỈNH NHỎ 🔵 (MÀU XANH DƯƠNG) -->
                                            <div class="modal fade" id="reviseSubTaskModal-${st.id}" tabindex="-1" aria-labelledby="reviseSubTaskModalLabel-${st.id}" aria-hidden="true">
                                                <div class="modal-dialog modal-dialog-centered">
                                                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                                                        <div class="modal-header border-0 pb-0">
                                                            <h6 class="modal-title fw-bold text-primary" id="reviseSubTaskModalLabel-${st.id}">
                                                                <i class="bi bi-pencil-square me-2"></i>Dặn Dò Cân Chỉnh Nhỏ (Cơ bản đã ổn)
                                                            </h6>
                                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                        </div>
                                                        <form action="${pageContext.request.contextPath}/task" method="post">
                                                            <input type="hidden" name="action" value="reviseSubTask">
                                                            <input type="hidden" name="projectId" value="${project.id}">
                                                            <input type="hidden" name="subTaskId" value="${st.id}">
                                                            <div class="modal-body py-3">
                                                                <label for="reviseNote-${st.id}" class="form-label fw-semibold fs-8 text-dark">
                                                                    Lời dặn dò tinh chỉnh cho thành viên:
                                                                </label>
                                                                <textarea class="form-control fs-8 rounded-3" id="reviseNote-${st.id}" name="feedbackNote" rows="3" 
                                                                          placeholder="Ví dụ: Đã làm rất tốt, em đổi lại màu nút thành màu xanh dương và format code nhé..." required></textarea>
                                                            </div>
                                                            <div class="modal-footer border-0 pt-0">
                                                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                                                <button type="submit" class="btn btn-primary rounded-pill px-4 fs-8 fw-semibold shadow-sm">
                                                                    <i class="bi bi-send-fill me-1"></i> Gửi Yêu Cầu Cân Chỉnh (🔵)
                                                                </button>
                                                            </div>
                                                        </form>
                                                    </div>
                                                </div>
                                            </div>

                                            <!-- MODAL 3: TRẢ VỀ DO CHƯA ĐẠT YÊU CẦU 🔴 (MÀU ĐỎ) -->
                                            <div class="modal fade" id="rejectSubTaskModal-${st.id}" tabindex="-1" aria-labelledby="rejectSubTaskModalLabel-${st.id}" aria-hidden="true">
                                                <div class="modal-dialog modal-dialog-centered">
                                                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                                                        <div class="modal-header border-0 pb-0">
                                                            <h6 class="modal-title fw-bold text-danger" id="rejectSubTaskModalLabel-${st.id}">
                                                                <i class="bi bi-exclamation-triangle-fill me-2"></i>Đánh Giá Chưa Đạt Yêu Cầu
                                                            </h6>
                                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                        </div>
                                                        <form action="${pageContext.request.contextPath}/task" method="post">
                                                            <input type="hidden" name="action" value="rejectSubTask">
                                                            <input type="hidden" name="projectId" value="${project.id}">
                                                            <input type="hidden" name="subTaskId" value="${st.id}">
                                                            <div class="modal-body py-3">
                                                                <label for="rejectNote-${st.id}" class="form-label fw-semibold fs-8 text-dark">
                                                                    Nêu rõ lý do sai sót / lỗi yêu cầu:
                                                                </label>
                                                                <textarea class="form-control fs-8 rounded-3" id="rejectNote-${st.id}" name="feedbackNote" rows="3" 
                                                                          placeholder="Ví dụ: Sai kiến trúc CSDL, thiếu toàn bộ khóa ngoại bảng Users, yêu cầu làm lại..." required></textarea>
                                                            </div>
                                                            <div class="modal-footer border-0 pt-0">
                                                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                                                <button type="submit" class="btn btn-danger rounded-pill px-4 fs-8 fw-semibold shadow-sm">
                                                                    <i class="bi bi-x-circle-fill me-1"></i> Trả Về Yêu Cầu Làm Lại (🔴)
                                                                </button>
                                                            </div>
                                                        </form>
                                                    </div>
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
                                    <span class="badge ${task.statusBadgeClass} rounded-pill px-2 py-1 fs-9">${task.statusLabel}</span>
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
                                    <i class="bi bi-text-left text-primary me-1"></i> Mô tả chi tiết
                                </h6>
                                <div class="p-3 bg-light rounded-3 text-secondary fs-8 border lh-base" style="white-space: pre-line;">
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

                            <!-- =========================================================================
                                 KHỐI BÀN GIAO & PHÊ DUYỆT NGHIỆM THU TASK LỚN (TASK LEAD ➔ PM)
                                 ========================================================================= -->
                            <div class="mb-4 p-3 rounded-3 border bg-light-subtle">
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <h6 class="fw-bold text-dark fs-7 mb-0">
                                        <i class="bi bi-box-seam-fill text-primary me-1"></i> Bàn Giao & Nghiệm Thu Dự Án (Task Lead ➔ PM)
                                    </h6>
                                    <span class="badge ${task.statusBadgeClass} rounded-pill px-2 py-1 fs-9">
                                        ${task.statusLabel}
                                    </span>
                                </div>

                                <!-- NỘI DUNG 1: HIỂN THỊ BÁO CÁO BÀN GIAO CỦA TASK LEAD (NẾU CÓ) -->
                                <c:if test="${not empty task.finalDeliverableNote}">
                                    <div class="p-2 bg-white rounded-2 border fs-8 text-dark mb-2 shadow-2xs">
                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                            <span class="fw-bold text-warning fs-9">
                                                <i class="bi bi-file-earmark-text-fill me-1"></i> Báo cáo tổng kết bàn giao của Task Lead:
                                            </span>
                                            <span class="fs-9 text-muted">${task.submittedAt}</span>
                                        </div>
                                        <p class="mb-0 text-secondary fs-8">${task.finalDeliverableNote}</p>
                                    </div>
                                </c:if>

                                <!-- NỘI DUNG 2: HIỂN THỊ PHẢN HỒI / ĐÁNH GIÁ CỦA PM (NẾU CÓ) -->
                                <c:if test="${not empty task.pmFeedback}">
                                    <c:choose>
                                        <c:when test="${task.status == 'REVISE'}">
                                            <div class="p-2 bg-primary-subtle text-primary border border-primary-subtle rounded-2 fs-8 mb-2">
                                                <div class="d-flex align-items-center justify-content-between mb-1">
                                                    <span class="fw-bold fs-9">
                                                        <i class="bi bi-info-circle-fill me-1"></i> Dặn dò cân chỉnh từ Trưởng Dự Án (PM):
                                                    </span>
                                                    <span class="fs-9 opacity-75">${task.reviewedAt}</span>
                                                </div>
                                                <p class="mb-0 fs-8">"${task.pmFeedback}"</p>
                                            </div>
                                        </c:when>
                                        <c:when test="${task.status == 'REJECTED'}">
                                            <div class="p-2 bg-danger-subtle text-danger border border-danger-subtle rounded-2 fs-8 mb-2">
                                                <div class="d-flex align-items-center justify-content-between mb-1">
                                                    <span class="fw-bold fs-9">
                                                        <i class="bi bi-exclamation-triangle-fill me-1"></i> Lý do chưa đạt từ Trưởng Dự Án (PM):
                                                    </span>
                                                    <span class="fs-9 opacity-75">${task.reviewedAt}</span>
                                                </div>
                                                <p class="mb-0 fs-8">"${task.pmFeedback}"</p>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="p-2 bg-success-subtle text-success border border-success-subtle rounded-2 fs-8 mb-2">
                                                <div class="d-flex align-items-center justify-content-between mb-1">
                                                    <span class="fw-bold fs-9">
                                                        <i class="bi bi-patch-check-fill me-1"></i> Đánh giá nghiệm thu từ Trưởng Dự Án (PM):
                                                    </span>
                                                    <span class="fs-9 opacity-75">${task.reviewedAt}</span>
                                                </div>
                                                <p class="mb-0 fs-8">"${task.pmFeedback}"</p>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </c:if>

                                <!-- NỘI DUNG 3: CÁC NÚT TƯƠNG TÁC (TASK LEAD NỘP BÀN GIAO / PM PHÊ DUYỆT 3 LỰA CHỌN) -->
                                <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 pt-2 border-top">
                                    
                                    <!-- A. NÚT DÀNH CHO TASK LEAD / PM: Nộp bàn giao Task lớn -->
                                    <c:set var="isTaskLead" value="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}" />
                                    <c:if test="${isTaskLead && task.status != 'DONE'}">
                                        <button type="button" class="btn btn-outline-warning text-dark btn-sm rounded-pill fs-8 py-1 px-3 fw-semibold shadow-2xs"
                                                data-bs-toggle="modal" data-bs-target="#submitParentTaskModal-${task.id}">
                                            <i class="bi bi-box-seam me-1"></i> ${task.status == 'SUBMITTED' ? 'Cập Nhật Báo Cáo Bàn Giao' : 'Bàn Giao & Nộp Cho PM'}
                                        </button>
                                    </c:if>

                                    <!-- B. NÚT DÀNH RIÊNG CHO TRƯỞNG DỰ ÁN (PM) KHI CÓ BÀN GIAO (SUBMITTED) -->
                                    <c:if test="${project.ownerId == sessionScope.currentUser.id && task.status == 'SUBMITTED'}">
                                        <div class="d-flex align-items-center gap-2 ms-auto">
                                            <!-- 1. Duyệt Đạt 🟢 -->
                                            <form method="post" action="${pageContext.request.contextPath}/task" class="m-0">
                                                <input type="hidden" name="action" value="pmApproveTask">
                                                <input type="hidden" name="projectId" value="${project.id}">
                                                <input type="hidden" name="taskId" value="${task.id}">
                                                <button type="submit" class="btn btn-success btn-sm rounded-pill fs-8 py-1 px-3 fw-semibold shadow-2xs" title="Nghiệm thu hoàn tất 100%">
                                                    <i class="bi bi-check-circle-fill me-1"></i> PM Duyệt Đạt (🟢)
                                                </button>
                                            </form>

                                            <!-- 2. Cần Cân Chỉnh 🔵 (Xanh Dương) -->
                                            <button type="button" class="btn btn-primary btn-sm rounded-pill fs-8 py-1 px-3 fw-semibold shadow-2xs"
                                                    data-bs-toggle="modal" data-bs-target="#pmReviseModal-${task.id}">
                                                <i class="bi bi-pencil-square me-1"></i> Cân Chỉnh (🔵)
                                            </button>

                                            <!-- 3. Chưa Đạt 🔴 (Màu Đỏ) -->
                                            <button type="button" class="btn btn-danger btn-sm rounded-pill fs-8 py-1 px-3 fw-semibold shadow-2xs"
                                                    data-bs-toggle="modal" data-bs-target="#pmRejectModal-${task.id}">
                                                <i class="bi bi-x-circle-fill me-1"></i> Chưa Đạt (🔴)
                                            </button>
                                        </div>
                                    </c:if>

                                </div>
                            </div>

                            <!-- MODAL 1: TASK LEAD NỘP BÀO CÁO BÀN GIAO CHO PM -->
                            <div class="modal fade" id="submitParentTaskModal-${task.id}" tabindex="-1" aria-labelledby="submitParentTaskModalLabel-${task.id}" aria-hidden="true">
                                <div class="modal-dialog modal-dialog-centered">
                                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                                        <div class="modal-header border-0 pb-0">
                                            <h6 class="modal-title fw-bold text-dark" id="submitParentTaskModalLabel-${task.id}">
                                                <i class="bi bi-box-seam-fill text-warning me-2"></i>Bàn Giao Task: [${task.title}]
                                            </h6>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <form action="${pageContext.request.contextPath}/task" method="post">
                                            <input type="hidden" name="action" value="submitParentTask">
                                            <input type="hidden" name="projectId" value="${project.id}">
                                            <input type="hidden" name="taskId" value="${task.id}">
                                            <div class="modal-body py-3">
                                                <label for="deliverable-${task.id}" class="form-label fw-semibold fs-8 text-dark">
                                                    Báo cáo tổng kết bàn giao & Link sản phẩm cho Trưởng Dự Án (PM) <span class="text-danger">*</span>
                                                </label>
                                                <textarea class="form-control fs-8 rounded-3" id="deliverable-${task.id}" name="deliverableNote" rows="4" 
                                                          placeholder="Mô tả tóm tắt kết quả tính năng, đính kèm link demo, link pull request, lưu ý kỹ thuật cho PM..." required>${task.finalDeliverableNote}</textarea>
                                            </div>
                                            <div class="modal-footer border-0 pt-0">
                                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                                <button type="submit" class="btn btn-warning rounded-pill px-4 fs-8 fw-semibold shadow-sm text-dark">
                                                    <i class="bi bi-send-fill me-1"></i> Gửi Báo Cáo Bàn Giao Cho PM
                                                </button>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>

                            <!-- MODAL 2: PM YÊU CẦU CÂN CHỈNH NHỎ 🔵 (MÀU XANH DƯƠNG) -->
                            <div class="modal fade" id="pmReviseModal-${task.id}" tabindex="-1" aria-labelledby="pmReviseModalLabel-${task.id}" aria-hidden="true">
                                <div class="modal-dialog modal-dialog-centered">
                                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                                        <div class="modal-header border-0 pb-0">
                                            <h6 class="modal-title fw-bold text-primary" id="pmReviseModalLabel-${task.id}">
                                                <i class="bi bi-pencil-square me-2"></i>PM Dặn Dò Cân Chỉnh Nhỏ (Cơ bản đã tốt)
                                            </h6>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <form action="${pageContext.request.contextPath}/task" method="post">
                                            <input type="hidden" name="action" value="pmReviseTask">
                                            <input type="hidden" name="projectId" value="${project.id}">
                                            <input type="hidden" name="taskId" value="${task.id}">
                                            <div class="modal-body py-3">
                                                <label for="pmReviseNote-${task.id}" class="form-label fw-semibold fs-8 text-dark">
                                                    Ý kiến dặn dò tinh chỉnh cho Task Lead:
                                                </label>
                                                <textarea class="form-control fs-8 rounded-3" id="pmReviseNote-${task.id}" name="feedback" rows="3" 
                                                          placeholder="Ví dụ: Tính năng chạy rất mượt, em cân chỉnh lại màu sắc nút bấm và chuẩn hóa thông báo lỗi nhé..." required></textarea>
                                            </div>
                                            <div class="modal-footer border-0 pt-0">
                                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                                <button type="submit" class="btn btn-primary rounded-pill px-4 fs-8 fw-semibold shadow-sm">
                                                    <i class="bi bi-send-fill me-1"></i> Gửi Yêu Cầu Cân Chỉnh (🔵)
                                                </button>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>

                            <!-- MODAL 3: PM TRẢ VỀ DO CHƯA ĐẠT 🔴 (MÀU ĐỎ) -->
                            <div class="modal fade" id="pmRejectModal-${task.id}" tabindex="-1" aria-labelledby="pmRejectModalLabel-${task.id}" aria-hidden="true">
                                <div class="modal-dialog modal-dialog-centered">
                                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                                        <div class="modal-header border-0 pb-0">
                                            <h6 class="modal-title fw-bold text-danger" id="pmRejectModalLabel-${task.id}">
                                                <i class="bi bi-exclamation-triangle-fill me-2"></i>PM Đánh Giá Chưa Đạt Yêu Cầu
                                            </h6>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <form action="${pageContext.request.contextPath}/task" method="post">
                                            <input type="hidden" name="action" value="pmRejectTask">
                                            <input type="hidden" name="projectId" value="${project.id}">
                                            <input type="hidden" name="taskId" value="${task.id}">
                                            <div class="modal-body py-3">
                                                <label for="pmRejectNote-${task.id}" class="form-label fw-semibold fs-8 text-dark">
                                                    Nêu rõ lý do sai sót / yêu cầu làm lại:
                                                </label>
                                                <textarea class="form-control fs-8 rounded-3" id="pmRejectNote-${task.id}" name="feedback" rows="3" 
                                                          placeholder="Ví dụ: Tính năng chưa đúng spec yêu cầu, thiếu bảo mật phân quyền Servlet, yêu cầu làm lại..." required></textarea>
                                            </div>
                                            <div class="modal-footer border-0 pt-0">
                                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                                <button type="submit" class="btn btn-danger rounded-pill px-4 fs-8 fw-semibold shadow-sm">
                                                    <i class="bi bi-x-circle-fill me-1"></i> Trả Về Làm Lại (🔴)
                                                </button>
                                            </div>
                                        </form>
                                    </div>
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
                                    <div class="d-flex flex-column gap-3 mb-3">
                                        <c:forEach items="${taskSubTasksMap[task.id]}" var="st">
                                            <div class="p-3 bg-light rounded-3 border ${st.status == 'APPROVED' ? 'opacity-75' : ''}">
                                                
                                                <!-- DÒNG 1: TRẠNG THÁI + TIÊU ĐỀ + NGƯỜI PHỤ TRÁCH + NÚT XÓA -->
                                                <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-1">
                                                    <div class="d-flex align-items-center gap-2">
                                                        <!-- Huy hiệu 5 màu -->
                                                        <span class="badge ${st.statusBadgeClass} rounded-pill px-2 py-1 fs-9">
                                                            ${st.statusLabel}
                                                        </span>
                                                        <!-- Tiêu đề việc con -->
                                                        <span class="fs-8 text-dark ${st.status == 'APPROVED' ? 'text-decoration-line-through text-muted' : 'fw-bold'}">
                                                            ${st.title}
                                                        </span>
                                                    </div>

                                                    <div class="d-flex align-items-center gap-2">
                                                        <span class="badge bg-white text-secondary border rounded-pill px-2 py-1 fs-9" title="Người phụ trách">
                                                            <i class="bi bi-person-fill text-primary"></i> ${st.assigneeName}
                                                        </span>
                                                        <!-- Nút Xóa (Dành riêng cho Task Lead & PM) -->
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

                                                <!-- DÒNG 2: THÔNG TIN CHI TIẾT BÀN GIAO / GÓP Ý DỰA THEO TRẠNG THÁI -->
                                                
                                                <!-- TRƯỜNG HỢP A: SUBMITTED (Đang chờ duyệt) - Hiển thị kết quả nộp bài -->
                                                <c:if test="${st.status == 'SUBMITTED'}">
                                                    <div class="p-2 bg-white rounded-2 border fs-8 text-dark mt-2 shadow-2xs">
                                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                                            <span class="fw-bold text-warning fs-9">
                                                                <i class="bi bi-file-earmark-check-fill me-1"></i> Kết quả nộp bài:
                                                            </span>
                                                            <span class="fs-9 text-muted">${st.submittedAt}</span>
                                                        </div>
                                                        <p class="mb-0 text-secondary fs-8">
                                                            ${not empty st.submissionNote ? st.submissionNote : 'Đã hoàn thành công việc, mời Task Lead kiểm tra và nghiệm thu.'}
                                                        </p>
                                                    </div>
                                                </c:if>

                                                <!-- TRƯỜNG HỢP B: REVISE (🔵 Màu Xanh Dương - Cần cân chỉnh nhỏ) -->
                                                <c:if test="${st.status == 'REVISE'}">
                                                    <div class="p-2 bg-primary-subtle text-primary border border-primary-subtle rounded-2 fs-8 mt-2">
                                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                                            <span class="fw-bold fs-9">
                                                                <i class="bi bi-info-circle-fill me-1"></i> Dặn dò từ Task Lead:
                                                            </span>
                                                            <span class="fs-9 opacity-75">${st.reviewedAt}</span>
                                                        </div>
                                                        <p class="mb-0 fs-8">"${st.feedbackNote}"</p>
                                                    </div>
                                                </c:if>

                                                <!-- TRƯỜNG HỢP C: REJECTED (🔴 Màu Đỏ - Chưa đạt yêu cầu) -->
                                                <c:if test="${st.status == 'REJECTED'}">
                                                    <div class="p-2 bg-danger-subtle text-danger border border-danger-subtle rounded-2 fs-8 mt-2">
                                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                                            <span class="fw-bold fs-9">
                                                                <i class="bi bi-exclamation-triangle-fill me-1"></i> Lý do chưa đạt từ Task Lead:
                                                            </span>
                                                            <span class="fs-9 opacity-75">${st.reviewedAt}</span>
                                                        </div>
                                                        <p class="mb-0 fs-8">"${st.feedbackNote}"</p>
                                                    </div>
                                                </c:if>

                                                <!-- TRƯỜNG HỢP D: APPROVED (🟢 Màu Xanh Lá - Đã nghiệm thu Đạt) -->
                                                <c:if test="${st.status == 'APPROVED'}">
                                                    <div class="fs-9 text-success mt-1 d-flex align-items-center gap-1">
                                                        <i class="bi bi-check-circle-fill"></i>
                                                        <span>Đã nghiệm thu hoàn thành 100% &bull; ${st.reviewedAt}</span>
                                                    </div>
                                                </c:if>

                                                <!-- DÒNG 3: CÁC NÚT HÀNH ĐỘNG TƯƠNG TÁC (NỘP BÀI / THẨM ĐỊNH) -->
                                                <div class="mt-2 pt-2 border-top d-flex flex-wrap align-items-center justify-content-between gap-2">
                                                    
                                                    <!-- 1. NÚT DÀNH CHO THÀNH VIÊN ĐƯỢC GIAO VIỆC: Nộp Báo Cáo / Bàn giao kết quả -->
                                                    <c:set var="isAssignee" value="${st.assigneeId == sessionScope.currentUser.id || task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}" />
                                                    <div>
                                                        <c:if test="${isAssignee && st.status != 'APPROVED' && st.status != 'SUBMITTED'}">
                                                            <button type="button" class="btn btn-outline-primary btn-sm rounded-pill fs-9 py-1 px-3 fw-semibold shadow-2xs"
                                                                    data-bs-toggle="modal" data-bs-target="#submitSubTaskModal-${st.id}">
                                                                <i class="bi bi-upload me-1"></i> ${st.status == 'TODO' ? 'Nộp Báo Cáo Kết Quả' : 'Nộp Lại Kết Quả Mới'}
                                                            </button>
                                                        </c:if>
                                                    </div>

                                                    <!-- 2. NÚT DÀNH CHO TASK LEAD & PM KHI CÓ BÀI NỘP (🟡 SUBMITTED): 3 LỰA CHỌN THẨM ĐỊNH -->
                                                    <c:set var="isReviewer" value="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}" />
                                                    <c:if test="${isReviewer && st.status == 'SUBMITTED'}">
                                                        <div class="d-flex align-items-center gap-1 ms-auto">
                                                            <!-- Nút 1: DUYỆT ĐẠT 🟢 -->
                                                            <form method="post" action="${pageContext.request.contextPath}/task" class="m-0">
                                                                <input type="hidden" name="action" value="approveSubTask">
                                                                <input type="hidden" name="projectId" value="${project.id}">
                                                                <input type="hidden" name="subTaskId" value="${st.id}">
                                                                <button type="submit" class="btn btn-success btn-sm rounded-pill fs-9 py-1 px-2 fw-semibold shadow-2xs" title="Nghiệm thu đạt 100%">
                                                                    <i class="bi bi-check-lg me-1"></i> Duyệt Đạt
                                                                </button>
                                                            </form>

                                                            <!-- Nút 2: CẦN CÂN CHỈNH 🔵 (Xanh Dương) -->
                                                            <button type="button" class="btn btn-primary btn-sm rounded-pill fs-9 py-1 px-2 fw-semibold shadow-2xs" 
                                                                    data-bs-toggle="modal" data-bs-target="#reviseSubTaskModal-${st.id}" title="Yêu cầu tinh chỉnh nhỏ">
                                                                <i class="bi bi-pencil me-1"></i> Cân chỉnh
                                                            </button>

                                                            <!-- Nút 3: CHƯA ĐẠT 🔴 (Màu Đỏ) -->
                                                            <button type="button" class="btn btn-danger btn-sm rounded-pill fs-9 py-1 px-2 fw-semibold shadow-2xs" 
                                                                    data-bs-toggle="modal" data-bs-target="#rejectSubTaskModal-${st.id}" title="Trả về làm lại do chưa đạt">
                                                                <i class="bi bi-x-lg me-1"></i> Chưa đạt
                                                            </button>
                                                        </div>
                                                    </c:if>

                                                </div>

                                            </div>

                                            <!-- MODAL 1: NỘP BÁO CÁO KẾT QUẢ CHO SUBTASK NÀY -->
                                            <div class="modal fade" id="submitSubTaskModal-${st.id}" tabindex="-1" aria-labelledby="submitSubTaskModalLabel-${st.id}" aria-hidden="true">
                                                <div class="modal-dialog modal-dialog-centered">
                                                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                                                        <div class="modal-header border-0 pb-0">
                                                            <h6 class="modal-title fw-bold text-dark" id="submitSubTaskModalLabel-${st.id}">
                                                                <i class="bi bi-upload text-primary me-2"></i>Nộp Kết Quả: [${st.title}]
                                                            </h6>
                                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                        </div>
                                                        <form action="${pageContext.request.contextPath}/task" method="post">
                                                            <input type="hidden" name="action" value="submitSubTask">
                                                            <input type="hidden" name="projectId" value="${project.id}">
                                                            <input type="hidden" name="subTaskId" value="${st.id}">
                                                            <div class="modal-body py-3">
                                                                <label for="note-${st.id}" class="form-label fw-semibold fs-8 text-dark">
                                                                    Ghi chú hoàn thành / Link bàn giao kết quả <span class="text-danger">*</span>
                                                                </label>
                                                                <textarea class="form-control fs-8 rounded-3" id="note-${st.id}" name="submissionNote" rows="3" 
                                                                          placeholder="Mô tả những gì bạn đã làm, dán link pull request, link tài liệu hoặc ghi chú cho Leader..." required></textarea>
                                                            </div>
                                                            <div class="modal-footer border-0 pt-0">
                                                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                                                <button type="submit" class="btn btn-primary-custom rounded-pill px-4 fs-8 fw-semibold shadow-sm">
                                                                    <i class="bi bi-send-fill me-1"></i> Gửi Báo Cáo Cho Leader
                                                                </button>
                                                            </div>
                                                        </form>
                                                    </div>
                                                </div>
                                            </div>

                                            <!-- MODAL 2: YÊU CẦU CÂN CHỈNH NHỎ 🔵 (MÀU XANH DƯƠNG) -->
                                            <div class="modal fade" id="reviseSubTaskModal-${st.id}" tabindex="-1" aria-labelledby="reviseSubTaskModalLabel-${st.id}" aria-hidden="true">
                                                <div class="modal-dialog modal-dialog-centered">
                                                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                                                        <div class="modal-header border-0 pb-0">
                                                            <h6 class="modal-title fw-bold text-primary" id="reviseSubTaskModalLabel-${st.id}">
                                                                <i class="bi bi-pencil-square me-2"></i>Dặn Dò Cân Chỉnh Nhỏ (Cơ bản đã ổn)
                            </h6>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <form action="${pageContext.request.contextPath}/task" method="post">
                            <input type="hidden" name="action" value="reviseSubTask">
                            <input type="hidden" name="projectId" value="${project.id}">
                            <input type="hidden" name="subTaskId" value="${st.id}">
                            <div class="modal-body py-3">
                                <label for="reviseNote-${st.id}" class="form-label fw-semibold fs-8 text-dark">
                                    Lời dặn dò tinh chỉnh cho thành viên:
                                </label>
                                <textarea class="form-control fs-8 rounded-3" id="reviseNote-${st.id}" name="feedbackNote" rows="3" 
                                          placeholder="Ví dụ: Đã làm rất tốt, em đổi lại màu nút thành màu xanh dương và format code nhé..." required></textarea>
                            </div>
                            <div class="modal-footer border-0 pt-0">
                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                <button type="submit" class="btn btn-primary rounded-pill px-4 fs-8 fw-semibold shadow-sm">
                                    <i class="bi bi-send-fill me-1"></i> Gửi Yêu Cầu Cân Chỉnh (🔵)
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>

            <!-- MODAL 3: TRẢ VỀ DO CHƯA ĐẠT YÊU CẦU 🔴 (MÀU ĐỎ) -->
            <div class="modal fade" id="rejectSubTaskModal-${st.id}" tabindex="-1" aria-labelledby="rejectSubTaskModalLabel-${st.id}" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                        <div class="modal-header border-0 pb-0">
                            <h6 class="modal-title fw-bold text-danger" id="rejectSubTaskModalLabel-${st.id}">
                                <i class="bi bi-exclamation-triangle-fill me-2"></i>Đánh Giá Chưa Đạt Yêu Cầu
                            </h6>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <form action="${pageContext.request.contextPath}/task" method="post">
                            <input type="hidden" name="action" value="rejectSubTask">
                            <input type="hidden" name="projectId" value="${project.id}">
                            <input type="hidden" name="subTaskId" value="${st.id}">
                            <div class="modal-body py-3">
                                <label for="rejectNote-${st.id}" class="form-label fw-semibold fs-8 text-dark">
                                    Nêu rõ lý do sai sót / lỗi yêu cầu:
                                </label>
                                <textarea class="form-control fs-8 rounded-3" id="rejectNote-${st.id}" name="feedbackNote" rows="3" 
                                          placeholder="Ví dụ: Sai kiến trúc CSDL, thiếu toàn bộ khóa ngoại bảng Users, yêu cầu làm lại..." required></textarea>
                            </div>
                            <div class="modal-footer border-0 pt-0">
                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                <button type="submit" class="btn btn-danger rounded-pill px-4 fs-8 fw-semibold shadow-sm">
                                    <i class="bi bi-x-circle-fill me-1"></i> Trả Về Yêu Cầu Làm Lại (🔴)
                                </button>
                            </div>
                        </form>
                    </div>
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
                                    <span class="badge ${task.statusBadgeClass} rounded-pill px-2 py-1 fs-9">${task.statusLabel}</span>
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
                                    <i class="bi bi-text-left text-primary me-1"></i> Mô tả chi tiết
                                </h6>
                                <div class="p-3 bg-light rounded-3 text-secondary fs-8 border lh-base" style="white-space: pre-line;">
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

                            <!-- =========================================================================
                                 KHỐI BÀN GIAO & PHÊ DUYỆT NGHIỆM THU TASK LỚN (TASK LEAD ➔ PM)
                                 ========================================================================= -->
                            <div class="mb-4 p-3 rounded-3 border bg-light-subtle">
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <h6 class="fw-bold text-dark fs-7 mb-0">
                                        <i class="bi bi-box-seam-fill text-primary me-1"></i> Bàn Giao & Nghiệm Thu Dự Án (Task Lead ➔ PM)
                                    </h6>
                                    <span class="badge ${task.statusBadgeClass} rounded-pill px-2 py-1 fs-9">
                                        ${task.statusLabel}
                                    </span>
                                </div>

                                <!-- NỘI DUNG 1: HIỂN THỊ BÁO CÁO BÀN GIAO CỦA TASK LEAD (NẾU CÓ) -->
                                <c:if test="${not empty task.finalDeliverableNote}">
                                    <div class="p-2 bg-white rounded-2 border fs-8 text-dark mb-2 shadow-2xs">
                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                            <span class="fw-bold text-warning fs-9">
                                                <i class="bi bi-file-earmark-text-fill me-1"></i> Báo cáo tổng kết bàn giao của Task Lead:
                                            </span>
                                            <span class="fs-9 text-muted">${task.submittedAt}</span>
                                        </div>
                                        <p class="mb-0 text-secondary fs-8">${task.finalDeliverableNote}</p>
                                    </div>
                                </c:if>

                                <!-- NỘI DUNG 2: HIỂN THỊ PHẢN HỒI / ĐÁNH GIÁ CỦA PM (NẾU CÓ) -->
                                <c:if test="${not empty task.pmFeedback}">
                                    <c:choose>
                                        <c:when test="${task.status == 'REVISE'}">
                                            <div class="p-2 bg-primary-subtle text-primary border border-primary-subtle rounded-2 fs-8 mb-2">
                                                <div class="d-flex align-items-center justify-content-between mb-1">
                                                    <span class="fw-bold fs-9">
                                                        <i class="bi bi-info-circle-fill me-1"></i> Dặn dò cân chỉnh từ Trưởng Dự Án (PM):
                                                    </span>
                                                    <span class="fs-9 opacity-75">${task.reviewedAt}</span>
                                                </div>
                                                <p class="mb-0 fs-8">"${task.pmFeedback}"</p>
                                            </div>
                                        </c:when>
                                        <c:when test="${task.status == 'REJECTED'}">
                                            <div class="p-2 bg-danger-subtle text-danger border border-danger-subtle rounded-2 fs-8 mb-2">
                                                <div class="d-flex align-items-center justify-content-between mb-1">
                                                    <span class="fw-bold fs-9">
                                                        <i class="bi bi-exclamation-triangle-fill me-1"></i> Lý do chưa đạt từ Trưởng Dự Án (PM):
                                                    </span>
                                                    <span class="fs-9 opacity-75">${task.reviewedAt}</span>
                                                </div>
                                                <p class="mb-0 fs-8">"${task.pmFeedback}"</p>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="p-2 bg-success-subtle text-success border border-success-subtle rounded-2 fs-8 mb-2">
                                                <div class="d-flex align-items-center justify-content-between mb-1">
                                                    <span class="fw-bold fs-9">
                                                        <i class="bi bi-patch-check-fill me-1"></i> Đánh giá nghiệm thu từ Trưởng Dự Án (PM):
                                                    </span>
                                                    <span class="fs-9 opacity-75">${task.reviewedAt}</span>
                                                </div>
                                                <p class="mb-0 fs-8">"${task.pmFeedback}"</p>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </c:if>

                                <!-- NỘI DUNG 3: CÁC NÚT TƯƠNG TÁC (TASK LEAD NỘP BÀN GIAO / PM PHÊ DUYỆT 3 LỰA CHỌN) -->
                                <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 pt-2 border-top">
                                    
                                    <!-- A. NÚT DÀNH CHO TASK LEAD / PM: Nộp bàn giao Task lớn -->
                                    <c:set var="isTaskLead" value="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}" />
                                    <c:if test="${isTaskLead && task.status != 'DONE'}">
                                        <button type="button" class="btn btn-outline-warning text-dark btn-sm rounded-pill fs-8 py-1 px-3 fw-semibold shadow-2xs"
                                                data-bs-toggle="modal" data-bs-target="#submitParentTaskModal-${task.id}">
                                            <i class="bi bi-box-seam me-1"></i> ${task.status == 'SUBMITTED' ? 'Cập Nhật Báo Cáo Bàn Giao' : 'Bàn Giao & Nộp Cho PM'}
                                        </button>
                                    </c:if>

                                    <!-- B. NÚT DÀNH RIÊNG CHO TRƯỞNG DỰ ÁN (PM) KHI CÓ BÀN GIAO (SUBMITTED) -->
                                    <c:if test="${project.ownerId == sessionScope.currentUser.id && task.status == 'SUBMITTED'}">
                                        <div class="d-flex align-items-center gap-2 ms-auto">
                                            <!-- 1. Duyệt Đạt 🟢 -->
                                            <form method="post" action="${pageContext.request.contextPath}/task" class="m-0">
                                                <input type="hidden" name="action" value="pmApproveTask">
                                                <input type="hidden" name="projectId" value="${project.id}">
                                                <input type="hidden" name="taskId" value="${task.id}">
                                                <button type="submit" class="btn btn-success btn-sm rounded-pill fs-8 py-1 px-3 fw-semibold shadow-2xs" title="Nghiệm thu hoàn tất 100%">
                                                    <i class="bi bi-check-circle-fill me-1"></i> PM Duyệt Đạt (🟢)
                                                </button>
                                            </form>

                                            <!-- 2. Cần Cân Chỉnh 🔵 (Xanh Dương) -->
                                            <button type="button" class="btn btn-primary btn-sm rounded-pill fs-8 py-1 px-3 fw-semibold shadow-2xs"
                                                    data-bs-toggle="modal" data-bs-target="#pmReviseModal-${task.id}">
                                                <i class="bi bi-pencil-square me-1"></i> Cân Chỉnh (🔵)
                                            </button>

                                            <!-- 3. Chưa Đạt 🔴 (Màu Đỏ) -->
                                            <button type="button" class="btn btn-danger btn-sm rounded-pill fs-8 py-1 px-3 fw-semibold shadow-2xs"
                                                    data-bs-toggle="modal" data-bs-target="#pmRejectModal-${task.id}">
                                                <i class="bi bi-x-circle-fill me-1"></i> Chưa Đạt (🔴)
                                            </button>
                                        </div>
                                    </c:if>

                                </div>
                            </div>

                            <!-- MODAL 1: TASK LEAD NỘP BÀO CÁO BÀN GIAO CHO PM -->
                            <div class="modal fade" id="submitParentTaskModal-${task.id}" tabindex="-1" aria-labelledby="submitParentTaskModalLabel-${task.id}" aria-hidden="true">
                                <div class="modal-dialog modal-dialog-centered">
                                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                                        <div class="modal-header border-0 pb-0">
                                            <h6 class="modal-title fw-bold text-dark" id="submitParentTaskModalLabel-${task.id}">
                                                <i class="bi bi-box-seam-fill text-warning me-2"></i>Bàn Giao Task: [${task.title}]
                                            </h6>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <form action="${pageContext.request.contextPath}/task" method="post">
                                            <input type="hidden" name="action" value="submitParentTask">
                                            <input type="hidden" name="projectId" value="${project.id}">
                                            <input type="hidden" name="taskId" value="${task.id}">
                                            <div class="modal-body py-3">
                                                <label for="deliverable-${task.id}" class="form-label fw-semibold fs-8 text-dark">
                                                    Báo cáo tổng kết bàn giao & Link sản phẩm cho Trưởng Dự Án (PM) <span class="text-danger">*</span>
                                                </label>
                                                <textarea class="form-control fs-8 rounded-3" id="deliverable-${task.id}" name="deliverableNote" rows="4" 
                                                          placeholder="Mô tả tóm tắt kết quả tính năng, đính kèm link demo, link pull request, lưu ý kỹ thuật cho PM..." required>${task.finalDeliverableNote}</textarea>
                                            </div>
                                            <div class="modal-footer border-0 pt-0">
                                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                                <button type="submit" class="btn btn-warning rounded-pill px-4 fs-8 fw-semibold shadow-sm text-dark">
                                                    <i class="bi bi-send-fill me-1"></i> Gửi Báo Cáo Bàn Giao Cho PM
                                                </button>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>

                            <!-- MODAL 2: PM YÊU CẦU CÂN CHỈNH NHỎ 🔵 (MÀU XANH DƯƠNG) -->
                            <div class="modal fade" id="pmReviseModal-${task.id}" tabindex="-1" aria-labelledby="pmReviseModalLabel-${task.id}" aria-hidden="true">
                                <div class="modal-dialog modal-dialog-centered">
                                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                                        <div class="modal-header border-0 pb-0">
                                            <h6 class="modal-title fw-bold text-primary" id="pmReviseModalLabel-${task.id}">
                                                <i class="bi bi-pencil-square me-2"></i>PM Dặn Dò Cân Chỉnh Nhỏ (Cơ bản đã tốt)
                                            </h6>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <form action="${pageContext.request.contextPath}/task" method="post">
                                            <input type="hidden" name="action" value="pmReviseTask">
                                            <input type="hidden" name="projectId" value="${project.id}">
                                            <input type="hidden" name="taskId" value="${task.id}">
                                            <div class="modal-body py-3">
                                                <label for="pmReviseNote-${task.id}" class="form-label fw-semibold fs-8 text-dark">
                                                    Ý kiến dặn dò tinh chỉnh cho Task Lead:
                                                </label>
                                                <textarea class="form-control fs-8 rounded-3" id="pmReviseNote-${task.id}" name="feedback" rows="3" 
                                                          placeholder="Ví dụ: Tính năng chạy rất mượt, em cân chỉnh lại màu sắc nút bấm và chuẩn hóa thông báo lỗi nhé..." required></textarea>
                                            </div>
                                            <div class="modal-footer border-0 pt-0">
                                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                                <button type="submit" class="btn btn-primary rounded-pill px-4 fs-8 fw-semibold shadow-sm">
                                                    <i class="bi bi-send-fill me-1"></i> Gửi Yêu Cầu Cân Chỉnh (🔵)
                                                </button>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            </div>

                            <!-- MODAL 3: PM TRẢ VỀ DO CHƯA ĐẠT 🔴 (MÀU ĐỎ) -->
                            <div class="modal fade" id="pmRejectModal-${task.id}" tabindex="-1" aria-labelledby="pmRejectModalLabel-${task.id}" aria-hidden="true">
                                <div class="modal-dialog modal-dialog-centered">
                                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                                        <div class="modal-header border-0 pb-0">
                                            <h6 class="modal-title fw-bold text-danger" id="pmRejectModalLabel-${task.id}">
                                                <i class="bi bi-exclamation-triangle-fill me-2"></i>PM Đánh Giá Chưa Đạt Yêu Cầu
                                            </h6>
                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                        </div>
                                        <form action="${pageContext.request.contextPath}/task" method="post">
                                            <input type="hidden" name="action" value="pmRejectTask">
                                            <input type="hidden" name="projectId" value="${project.id}">
                                            <input type="hidden" name="taskId" value="${task.id}">
                                            <div class="modal-body py-3">
                                                <label for="pmRejectNote-${task.id}" class="form-label fw-semibold fs-8 text-dark">
                                                    Nêu rõ lý do sai sót / yêu cầu làm lại:
                                                </label>
                                                <textarea class="form-control fs-8 rounded-3" id="pmRejectNote-${task.id}" name="feedback" rows="3" 
                                                          placeholder="Ví dụ: Tính năng chưa đúng spec yêu cầu, thiếu bảo mật phân quyền Servlet, yêu cầu làm lại..." required></textarea>
                                            </div>
                                            <div class="modal-footer border-0 pt-0">
                                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                                <button type="submit" class="btn btn-danger rounded-pill px-4 fs-8 fw-semibold shadow-sm">
                                                    <i class="bi bi-x-circle-fill me-1"></i> Trả Về Làm Lại (🔴)
                                                </button>
                                            </div>
                                        </form>
                                    </div>
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
                                    <div class="d-flex flex-column gap-3 mb-3">
                                        <c:forEach items="${taskSubTasksMap[task.id]}" var="st">
                                            <div class="p-3 bg-light rounded-3 border ${st.status == 'APPROVED' ? 'opacity-75' : ''}">
                                                
                                                <!-- DÒNG 1: TRẠNG THÁI + TIÊU ĐỀ + NGƯỜI PHỤ TRÁCH + NÚT XÓA -->
                                                <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-1">
                                                    <div class="d-flex align-items-center gap-2">
                                                        <!-- Huy hiệu 5 màu -->
                                                        <span class="badge ${st.statusBadgeClass} rounded-pill px-2 py-1 fs-9">
                                                            ${st.statusLabel}
                                                        </span>
                                                        <!-- Tiêu đề việc con -->
                                                        <span class="fs-8 text-dark ${st.status == 'APPROVED' ? 'text-decoration-line-through text-muted' : 'fw-bold'}">
                                                            ${st.title}
                                                        </span>
                                                    </div>

                                                    <div class="d-flex align-items-center gap-2">
                                                        <span class="badge bg-white text-secondary border rounded-pill px-2 py-1 fs-9" title="Người phụ trách">
                                                            <i class="bi bi-person-fill text-primary"></i> ${st.assigneeName}
                                                        </span>
                                                        <!-- Nút Xóa (Dành riêng cho Task Lead & PM) -->
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

                                                <!-- DÒNG 2: THÔNG TIN CHI TIẾT BÀN GIAO / GÓP Ý DỰA THEO TRẠNG THÁI -->
                                                
                                                <!-- TRƯỜNG HỢP A: SUBMITTED (Đang chờ duyệt) - Hiển thị kết quả nộp bài -->
                                                <c:if test="${st.status == 'SUBMITTED'}">
                                                    <div class="p-2 bg-white rounded-2 border fs-8 text-dark mt-2 shadow-2xs">
                                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                                            <span class="fw-bold text-warning fs-9">
                                                                <i class="bi bi-file-earmark-check-fill me-1"></i> Kết quả nộp bài:
                                                            </span>
                                                            <span class="fs-9 text-muted">${st.submittedAt}</span>
                                                        </div>
                                                        <p class="mb-0 text-secondary fs-8">
                                                            ${not empty st.submissionNote ? st.submissionNote : 'Đã hoàn thành công việc, mời Task Lead kiểm tra và nghiệm thu.'}
                                                        </p>
                                                    </div>
                                                </c:if>

                                                <!-- TRƯỜNG HỢP B: REVISE (🔵 Màu Xanh Dương - Cần cân chỉnh nhỏ) -->
                                                <c:if test="${st.status == 'REVISE'}">
                                                    <div class="p-2 bg-primary-subtle text-primary border border-primary-subtle rounded-2 fs-8 mt-2">
                                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                                            <span class="fw-bold fs-9">
                                                                <i class="bi bi-info-circle-fill me-1"></i> Dặn dò từ Task Lead:
                                                            </span>
                                                            <span class="fs-9 opacity-75">${st.reviewedAt}</span>
                                                        </div>
                                                        <p class="mb-0 fs-8">"${st.feedbackNote}"</p>
                                                    </div>
                                                </c:if>

                                                <!-- TRƯỜNG HỢP C: REJECTED (🔴 Màu Đỏ - Chưa đạt yêu cầu) -->
                                                <c:if test="${st.status == 'REJECTED'}">
                                                    <div class="p-2 bg-danger-subtle text-danger border border-danger-subtle rounded-2 fs-8 mt-2">
                                                        <div class="d-flex align-items-center justify-content-between mb-1">
                                                            <span class="fw-bold fs-9">
                                                                <i class="bi bi-exclamation-triangle-fill me-1"></i> Lý do chưa đạt từ Task Lead:
                                                            </span>
                                                            <span class="fs-9 opacity-75">${st.reviewedAt}</span>
                                                        </div>
                                                        <p class="mb-0 fs-8">"${st.feedbackNote}"</p>
                                                    </div>
                                                </c:if>

                                                <!-- TRƯỜNG HỢP D: APPROVED (🟢 Màu Xanh Lá - Đã nghiệm thu Đạt) -->
                                                <c:if test="${st.status == 'APPROVED'}">
                                                    <div class="fs-9 text-success mt-1 d-flex align-items-center gap-1">
                                                        <i class="bi bi-check-circle-fill"></i>
                                                        <span>Đã nghiệm thu hoàn thành 100% &bull; ${st.reviewedAt}</span>
                                                    </div>
                                                </c:if>

                                                <!-- DÒNG 3: CÁC NÚT HÀNH ĐỘNG TƯƠNG TÁC (NỘP BÀI / THẨM ĐỊNH) -->
                                                <div class="mt-2 pt-2 border-top d-flex flex-wrap align-items-center justify-content-between gap-2">
                                                    
                                                    <!-- 1. NÚT DÀNH CHO THÀNH VIÊN ĐƯỢC GIAO VIỆC: Nộp Báo Cáo / Bàn giao kết quả -->
                                                    <c:set var="isAssignee" value="${st.assigneeId == sessionScope.currentUser.id || task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}" />
                                                    <div>
                                                        <c:if test="${isAssignee && st.status != 'APPROVED' && st.status != 'SUBMITTED'}">
                                                            <button type="button" class="btn btn-outline-primary btn-sm rounded-pill fs-9 py-1 px-3 fw-semibold shadow-2xs"
                                                                    data-bs-toggle="modal" data-bs-target="#submitSubTaskModal-${st.id}">
                                                                <i class="bi bi-upload me-1"></i> ${st.status == 'TODO' ? 'Nộp Báo Cáo Kết Quả' : 'Nộp Lại Kết Quả Mới'}
                                                            </button>
                                                        </c:if>
                                                    </div>

                                                    <!-- 2. NÚT DÀNH CHO TASK LEAD & PM KHI CÓ BÀI NỘP (🟡 SUBMITTED): 3 LỰA CHỌN THẨM ĐỊNH -->
                                                    <c:set var="isReviewer" value="${task.assigneeId == sessionScope.currentUser.id || project.ownerId == sessionScope.currentUser.id}" />
                                                    <c:if test="${isReviewer && st.status == 'SUBMITTED'}">
                                                        <div class="d-flex align-items-center gap-1 ms-auto">
                                                            <!-- Nút 1: DUYỆT ĐẠT 🟢 -->
                                                            <form method="post" action="${pageContext.request.contextPath}/task" class="m-0">
                                                                <input type="hidden" name="action" value="approveSubTask">
                                                                <input type="hidden" name="projectId" value="${project.id}">
                                                                <input type="hidden" name="subTaskId" value="${st.id}">
                                                                <button type="submit" class="btn btn-success btn-sm rounded-pill fs-9 py-1 px-2 fw-semibold shadow-2xs" title="Nghiệm thu đạt 100%">
                                                                    <i class="bi bi-check-lg me-1"></i> Duyệt Đạt
                                                                </button>
                                                            </form>

                                                            <!-- Nút 2: CẦN CÂN CHỈNH 🔵 (Xanh Dương) -->
                                                            <button type="button" class="btn btn-primary btn-sm rounded-pill fs-9 py-1 px-2 fw-semibold shadow-2xs" 
                                                                    data-bs-toggle="modal" data-bs-target="#reviseSubTaskModal-${st.id}" title="Yêu cầu tinh chỉnh nhỏ">
                                                                <i class="bi bi-pencil me-1"></i> Cân chỉnh
                                                            </button>

                                                            <!-- Nút 3: CHƯA ĐẠT 🔴 (Màu Đỏ) -->
                                                            <button type="button" class="btn btn-danger btn-sm rounded-pill fs-9 py-1 px-2 fw-semibold shadow-2xs" 
                                                                    data-bs-toggle="modal" data-bs-target="#rejectSubTaskModal-${st.id}" title="Trả về làm lại do chưa đạt">
                                                                <i class="bi bi-x-lg me-1"></i> Chưa đạt
                                                            </button>
                                                        </div>
                                                    </c:if>

                                                </div>

                                            </div>

                                            <!-- MODAL 1: NỘP BÁO CÁO KẾT QUẢ CHO SUBTASK NÀY -->
                                            <div class="modal fade" id="submitSubTaskModal-${st.id}" tabindex="-1" aria-labelledby="submitSubTaskModalLabel-${st.id}" aria-hidden="true">
                                                <div class="modal-dialog modal-dialog-centered">
                                                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                                                        <div class="modal-header border-0 pb-0">
                                                            <h6 class="modal-title fw-bold text-dark" id="submitSubTaskModalLabel-${st.id}">
                                                                <i class="bi bi-upload text-primary me-2"></i>Nộp Kết Quả: [${st.title}]
                                                            </h6>
                                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                        </div>
                                                        <form action="${pageContext.request.contextPath}/task" method="post">
                                                            <input type="hidden" name="action" value="submitSubTask">
                                                            <input type="hidden" name="projectId" value="${project.id}">
                                                            <input type="hidden" name="subTaskId" value="${st.id}">
                                                            <div class="modal-body py-3">
                                                                <label for="note-${st.id}" class="form-label fw-semibold fs-8 text-dark">
                                                                    Ghi chú hoàn thành / Link bàn giao kết quả <span class="text-danger">*</span>
                                                                </label>
                                                                <textarea class="form-control fs-8 rounded-3" id="note-${st.id}" name="submissionNote" rows="3" 
                                                                          placeholder="Mô tả những gì bạn đã làm, dán link pull request, link tài liệu hoặc ghi chú cho Leader..." required></textarea>
                                                            </div>
                                                            <div class="modal-footer border-0 pt-0">
                                                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                                                <button type="submit" class="btn btn-primary-custom rounded-pill px-4 fs-8 fw-semibold shadow-sm">
                                                                    <i class="bi bi-send-fill me-1"></i> Gửi Báo Cáo Cho Leader
                                                                </button>
                                                            </div>
                                                        </form>
                                                    </div>
                                                </div>
                                            </div>

                                            <!-- MODAL 2: YÊU CẦU CÂN CHỈNH NHỎ 🔵 (MÀU XANH DƯƠNG) -->
                                            <div class="modal fade" id="reviseSubTaskModal-${st.id}" tabindex="-1" aria-labelledby="reviseSubTaskModalLabel-${st.id}" aria-hidden="true">
                                                <div class="modal-dialog modal-dialog-centered">
                                                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                                                        <div class="modal-header border-0 pb-0">
                                                            <h6 class="modal-title fw-bold text-primary" id="reviseSubTaskModalLabel-${st.id}">
                                                                <i class="bi bi-pencil-square me-2"></i>Dặn Dò Cân Chỉnh Nhỏ (Cơ bản đã ổn)
                            </h6>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <form action="${pageContext.request.contextPath}/task" method="post">
                            <input type="hidden" name="action" value="reviseSubTask">
                            <input type="hidden" name="projectId" value="${project.id}">
                            <input type="hidden" name="subTaskId" value="${st.id}">
                            <div class="modal-body py-3">
                                <label for="reviseNote-${st.id}" class="form-label fw-semibold fs-8 text-dark">
                                    Lời dặn dò tinh chỉnh cho thành viên:
                                </label>
                                <textarea class="form-control fs-8 rounded-3" id="reviseNote-${st.id}" name="feedbackNote" rows="3" 
                                          placeholder="Ví dụ: Đã làm rất tốt, em đổi lại màu nút thành màu xanh dương và format code nhé..." required></textarea>
                            </div>
                            <div class="modal-footer border-0 pt-0">
                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                <button type="submit" class="btn btn-primary rounded-pill px-4 fs-8 fw-semibold shadow-sm">
                                    <i class="bi bi-send-fill me-1"></i> Gửi Yêu Cầu Cân Chỉnh (🔵)
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>

            <!-- MODAL 3: TRẢ VỀ DO CHƯA ĐẠT YÊU CẦU 🔴 (MÀU ĐỎ) -->
            <div class="modal fade" id="rejectSubTaskModal-${st.id}" tabindex="-1" aria-labelledby="rejectSubTaskModalLabel-${st.id}" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                        <div class="modal-header border-0 pb-0">
                            <h6 class="modal-title fw-bold text-danger" id="rejectSubTaskModalLabel-${st.id}">
                                <i class="bi bi-exclamation-triangle-fill me-2"></i>Đánh Giá Chưa Đạt Yêu Cầu
                            </h6>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <form action="${pageContext.request.contextPath}/task" method="post">
                            <input type="hidden" name="action" value="rejectSubTask">
                            <input type="hidden" name="projectId" value="${project.id}">
                            <input type="hidden" name="subTaskId" value="${st.id}">
                            <div class="modal-body py-3">
                                <label for="rejectNote-${st.id}" class="form-label fw-semibold fs-8 text-dark">
                                    Nêu rõ lý do sai sót / lỗi yêu cầu:
                                </label>
                                <textarea class="form-control fs-8 rounded-3" id="rejectNote-${st.id}" name="feedbackNote" rows="3" 
                                          placeholder="Ví dụ: Sai kiến trúc CSDL, thiếu toàn bộ khóa ngoại bảng Users, yêu cầu làm lại..." required></textarea>
                            </div>
                            <div class="modal-footer border-0 pt-0">
                                <button type="button" class="btn btn-light rounded-pill px-3 fs-8" data-bs-dismiss="modal">Hủy</button>
                                <button type="submit" class="btn btn-danger rounded-pill px-4 fs-8 fw-semibold shadow-sm">
                                    <i class="bi bi-x-circle-fill me-1"></i> Trả Về Yêu Cầu Làm Lại (🔴)
                                </button>
                            </div>
                        </form>
                    </div>
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

<!-- =========================================================================
     5. MODAL THẺ HỒ SƠ ĐỒNG ĐỘI (SOCIAL PROFILE CARD MODAL - PHẦN B.3.3)
     ========================================================================= -->
<c:forEach items="${userWorkloadList}" var="uw">
    <div class="modal fade" id="memberProfileModal-${uw.user.id}" tabindex="-1" aria-labelledby="memberProfileModalLabel-${uw.user.id}" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-md">
            <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
                
                <!-- Đầu Card: Cover & Avatar lớn -->
                <div class="bg-primary-subtle p-4 text-center border-bottom position-relative">
                    <div class="avatar-circle mx-auto bg-primary text-white d-flex align-items-center justify-content-center rounded-circle shadow-sm mb-2" 
                         style="width: 72px; height: 72px; font-size: 2rem;">
                        <i class="bi bi-person-fill"></i>
                    </div>
                    <h5 class="fw-bold text-dark mb-1" id="memberProfileModalLabel-${uw.user.id}">
                        ${uw.user.fullName}
                    </h5>
                    <div class="d-flex align-items-center justify-content-center gap-2">
                        <span class="badge ${uw.user.id == project.ownerId ? 'bg-warning text-dark' : 'bg-primary'} rounded-pill px-3 py-1 fs-8">
                            ${uw.user.id == project.ownerId ? '👑 Trưởng Dự Án (PM)' : uw.user.role}
                        </span>
                        <span class="text-muted fs-8">
                            <i class="bi bi-envelope me-1"></i> ${uw.user.email}
                        </span>
                    </div>
                    <button type="button" class="btn-close position-absolute top-0 end-0 m-3" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>

                <!-- Thân Card: Bảng Thống Kê Khối Lượng Công Việc -->
                <div class="modal-body p-4">
                    <h6 class="fw-bold text-dark fs-7 mb-3 text-uppercase tracking-wider">
                        <i class="bi bi-graph-up-arrow text-primary me-1"></i> Khối lượng & Năng suất trong dự án này:
                    </h6>

                    <div class="row g-3 text-center mb-4">
                        <!-- Chỉ số 1: Số Task Lead -->
                        <div class="col-4">
                            <div class="p-3 bg-light rounded-3 border">
                                <span class="fs-9 text-muted d-block mb-1">Task Lead</span>
                                <h4 class="fw-extrabold text-primary mb-0">${uw.leadTaskCount}</h4>
                                <span class="fs-9 text-muted">chủ trì</span>
                            </div>
                        </div>

                        <!-- Chỉ số 2: Số Việc con -->
                        <div class="col-4">
                            <div class="p-3 bg-light rounded-3 border">
                                <span class="fs-9 text-muted d-block mb-1">Việc con</span>
                                <h4 class="fw-extrabold text-dark mb-0">${uw.subTaskCount}</h4>
                                <span class="fs-9 text-muted">được giao</span>
                            </div>
                        </div>

                        <!-- Chỉ số 3: Đã hoàn thành -->
                        <div class="col-4">
                            <div class="p-3 bg-success-subtle rounded-3 border border-success-subtle">
                                <span class="fs-9 text-success d-block mb-1">Đã xong</span>
                                <h4 class="fw-extrabold text-success mb-0">${uw.completedSubTaskCount}</h4>
                                <span class="fs-9 text-success">hoàn tất</span>
                            </div>
                        </div>
                    </div>

                    <!-- Tiến độ hoàn thành việc con -->
                    <div class="p-3 bg-light rounded-3 border">
                        <div class="d-flex align-items-center justify-content-between mb-1">
                            <span class="fs-8 fw-semibold text-dark">Tỷ lệ hoàn thành việc con</span>
                            <span class="fs-8 fw-bold text-success">
                                <c:choose>
                                    <c:when test="${uw.subTaskCount > 0}">
                                        ${Math.round((uw.completedSubTaskCount * 100.0) / uw.subTaskCount)}%
                                    </c:when>
                                    <c:otherwise>
                                        100%
                                    </c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                        <div class="progress" style="height: 6px;">
                            <div class="progress-bar bg-success rounded-pill" role="progressbar" 
                                 style="width: ${uw.subTaskCount > 0 ? Math.round((uw.completedSubTaskCount * 100.0) / uw.subTaskCount) : 100}%;" 
                                 aria-valuenow="${uw.subTaskCount > 0 ? Math.round((uw.completedSubTaskCount * 100.0) / uw.subTaskCount) : 100}" 
                                 aria-valuemin="0" aria-valuemax="100"></div>
                        </div>
                    </div>

                    <!-- KHỐI 2: DANH SÁCH TASK LỚN ĐANG CHỦ TRÌ (TASK LEAD) -->
                    <div class="mt-4 pt-3 border-top">
                        <div class="d-flex align-items-center justify-content-between mb-2">
                            <h6 class="fw-bold text-dark fs-7 mb-0 text-uppercase tracking-wider">
                                <i class="bi bi-award-fill text-warning me-1"></i> Các Task lớn đang chủ trì (${uw.leadTasks.size()}):
                            </h6>
                            <span class="badge bg-primary-subtle text-primary rounded-pill px-2 py-0 fs-9">
                                ${uw.leadTasks.size()} Task
                            </span>
                        </div>

                        <c:choose>
                            <c:when test="${not empty uw.leadTasks}">
                                <div class="d-flex flex-column gap-2">
                                    <c:forEach items="${uw.leadTasks}" var="leadTask">
                                        <div class="p-2 px-3 bg-light rounded-3 border d-flex align-items-center justify-content-between">
                                            <div class="d-flex align-items-center gap-2 text-truncate me-2">
                                                <span class="badge ${leadTask.priorityBadgeClass} rounded-pill px-2 py-0 fs-9">${leadTask.priorityLabel}</span>
                                                <span class="fs-8 fw-semibold text-dark text-truncate" title="${leadTask.title}">
                                                    ${leadTask.title}
                                                </span>
                                            </div>
                                            <div class="d-flex align-items-center gap-1 text-nowrap">
                                                <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-0 fs-9 fw-bold" title="Tiến độ Task">
                                                    ${taskProgressMap[leadTask.id]}%
                                                </span>
                                                <span class="badge ${leadTask.status == 'DONE' ? 'bg-success' : (leadTask.status == 'IN_PROGRESS' ? 'bg-primary' : 'bg-secondary')} rounded-pill px-2 py-0 fs-9">
                                                    ${leadTask.status == 'DONE' ? 'Đã xong' : (leadTask.status == 'IN_PROGRESS' ? 'Đang làm' : 'Cần làm')}
                                                </span>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="p-3 bg-light-subtle rounded-3 text-muted fs-8 border text-center">
                                    <i class="bi bi-inbox d-block fs-5 mb-1 opacity-50"></i>
                                    Chưa chủ trì Task lớn nào trong dự án này.
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <!-- Chân Card: Nút đóng -->
                <div class="modal-footer px-4 py-3 bg-light border-0">
                    <button type="button" class="btn btn-primary-custom w-100 rounded-pill fs-8 fw-semibold shadow-sm" data-bs-dismiss="modal">
                        <i class="bi bi-check-lg me-1"></i> Đóng thẻ hồ sơ
                    </button>
                </div>

            </div>
        </div>
    </div>
</c:forEach>

<!-- ==========================================
     6. MODAL POPUP: FORM "+ THÊM CÔNG VIỆC MỚI" (UC05)
     ========================================== -->
<div class="modal fade" id="addTaskModal" tabindex="-1" aria-labelledby="addTaskModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
            
            <div class="modal-header bg-primary text-white px-4 py-3 border-0">
                <h5 class="modal-title fw-bold" id="addTaskModalLabel">
                    <i class="bi bi-plus-circle-dotted me-2"></i> Thêm thẻ công việc lớn (Task Cha)
                </h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Đóng"></button>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/task">
                
                <input type="hidden" name="action" value="add">
                <input type="hidden" name="projectId" value="${project.id}">

                <div class="modal-body px-4 py-4">
                    
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

                    <div class="mb-3">
                        <label for="taskDescription" class="form-label fw-semibold text-dark fs-7">Mô tả yêu cầu</label>
                        <textarea class="form-control rounded-3 p-3 fs-7" 
                                  id="taskDescription" 
                                  name="description" 
                                  rows="3" 
                                  placeholder="Mô tả mục tiêu của Module này..."></textarea>
                    </div>

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

<!-- =========================================================================
     7. MODAL: XEM & QUẢN LÝ ĐỘI NGŨ DỰ ÁN (X/10 THÀNH VIÊN)
     ========================================================================= -->
<div class="modal fade" id="projectTeamModal" tabindex="-1" aria-labelledby="projectTeamModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
            
            <div class="modal-header bg-dark-navy text-white px-4 py-3 border-0">
                <div class="d-flex align-items-center gap-2">
                    <i class="bi bi-people-fill text-warning fs-5"></i>
                    <div>
                        <h5 class="modal-title fw-bold mb-0" id="projectTeamModalLabel">Đội Ngũ Dự Án & Lời Mời</h5>
                        <span class="fs-9 text-white-50">Hạn ngạch: <strong>${memberCount}/10</strong> thành viên</span>
                    </div>
                </div>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Đóng"></button>
            </div>

            <div class="modal-body p-4">
                
                <!-- PHẦN 1: DANH SÁCH THÀNH VIÊN ĐANG THAM GIA -->
                <div class="mb-4">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                        <h6 class="fw-bold text-dark fs-7 mb-0 text-uppercase tracking-wider">
                            <i class="bi bi-person-check-fill text-success me-1"></i> Thành viên chính thức (${projectMemberList.size()}):
                        </h6>
                        <span class="badge bg-success-subtle text-success rounded-pill px-2 py-0 fs-9">Đang hoạt động</span>
                    </div>

                    <div class="d-flex flex-column gap-2">
                        <c:forEach items="${projectMemberList}" var="pm">
                            <div class="p-3 bg-light rounded-3 border d-flex align-items-center justify-content-between">
                                <div class="d-flex align-items-center gap-3">
                                    <div class="avatar-sm rounded-circle ${pm.projectRole == 'OWNER' ? 'bg-warning text-dark' : 'bg-primary text-white'} d-flex align-items-center justify-content-center fw-bold fs-7" style="width: 36px; height: 36px;">
                                        <c:choose>
                                            <c:when test="${pm.projectRole == 'OWNER'}"><i class="bi bi-star-fill"></i></c:when>
                                            <c:otherwise><i class="bi bi-person-fill"></i></c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div>
                                        <div class="d-flex align-items-center gap-2">
                                            <span class="fw-bold text-dark fs-7">${pm.userName}</span>
                                            <span class="badge ${pm.projectRole == 'OWNER' ? 'bg-warning text-dark' : 'bg-secondary'} rounded-pill px-2 py-0 fs-9">
                                                ${pm.projectRole == 'OWNER' ? 'Trưởng Dự Án (PM)' : 'Thành viên'}
                                            </span>
                                        </div>
                                        <div class="text-muted fs-8">
                                            ${pm.userEmail} &bull; <span class="text-primary">${pm.userRole}</span>
                                        </div>
                                    </div>
                                </div>
                                <div class="text-end">
                                    <span class="fs-9 text-muted d-block">Gia nhập:</span>
                                    <span class="fs-9 fw-semibold text-secondary">${pm.joinedAt}</span>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>

                <!-- PHẦN 2: LỜI MỜI / YÊU CẦU ĐANG CHỜ PHẢN HỒI (PENDING) -->
                <c:if test="${not empty projectInviteList}">
                    <div class="mt-4 pt-3 border-top">
                        <div class="d-flex align-items-center justify-content-between mb-2">
                            <h6 class="fw-bold text-dark fs-7 mb-0 text-uppercase tracking-wider">
                                <i class="bi bi-hourglass-split text-warning me-1"></i> Lời mời & Yêu cầu đang chờ (${projectInviteList.size()}):
                            </h6>
                            <span class="badge bg-warning-subtle text-dark rounded-pill px-2 py-0 fs-9">PENDING</span>
                        </div>

                        <div class="d-flex flex-column gap-2">
                            <c:forEach items="${projectInviteList}" var="inv">
                                <div class="p-2 px-3 bg-white rounded-3 border d-flex align-items-center justify-content-between shadow-2xs">
                                    <div>
                                        <div class="d-flex align-items-center gap-2">
                                            <span class="badge ${inv.statusBadgeClass} rounded-pill px-2 py-0 fs-9">${inv.statusLabel}</span>
                                            <span class="fs-8 fw-bold text-dark">${inv.type == 'INVITATION' ? inv.receiverName : inv.senderName}</span>
                                            <span class="fs-9 text-muted">(${inv.type == 'INVITATION' ? 'Được PM mời' : 'Gửi đơn xin vào'})</span>
                                        </div>
                                        <span class="fs-9 text-danger d-block mt-1">
                                            <i class="bi bi-clock me-1"></i> Hạn: ${inv.expiredAt}
                                        </span>
                                    </div>

                                    <!-- Nút PM Thu hồi lời mời nếu còn PENDING -->
                                    <c:if test="${project.ownerId == sessionScope.currentUser.id && inv.status == 'PENDING'}">
                                        <form method="post" action="${pageContext.request.contextPath}/invite" class="m-0" onsubmit="return confirm('Bạn có chắc chắn muốn thu hồi lời mời này?');">
                                            <input type="hidden" name="action" value="revoke">
                                            <input type="hidden" name="inviteId" value="${inv.id}">
                                            <button type="submit" class="btn btn-outline-danger btn-sm rounded-pill fs-9 py-1 px-3" title="Thu hồi lời mời">
                                                <i class="bi bi-x-circle me-1"></i> Thu hồi
                                            </button>
                                        </form>
                                    </c:if>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                </c:if>

            </div>

            <div class="modal-footer px-4 py-3 bg-light border-0">
                <button type="button" class="btn btn-secondary rounded-pill px-4 fs-8 fw-semibold" data-bs-dismiss="modal">Đóng</button>
            </div>

        </div>
    </div>
</div>

<!-- =========================================================================
     8. MODAL: FORM MỜI THÀNH VIÊN VÀO DỰ ÁN (DÀNH RIÊNG CHO PM)
     ========================================================================= -->
<c:if test="${project.ownerId == sessionScope.currentUser.id}">
    <div class="modal fade" id="inviteMemberModal" tabindex="-1" aria-labelledby="inviteMemberModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg rounded-4 p-2">
                <div class="modal-header border-0 pb-0">
                    <h5 class="modal-title fw-bold text-dark" id="inviteMemberModalLabel">
                        <i class="bi bi-person-plus-fill text-success me-2"></i>Mời thành viên mới
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>

                <form action="${pageContext.request.contextPath}/invite" method="post">
                    <input type="hidden" name="action" value="sendInvite">
                    <input type="hidden" name="projectId" value="${project.id}">

                    <div class="modal-body py-3">
                        <p class="text-muted fs-8 mb-3">
                            Nhập <strong>Username hoặc Email</strong> của tài khoản bạn muốn mời vào dự án <strong>[${project.name}]</strong>. Lời mời sẽ có hiệu lực trong vòng <strong>7 ngày</strong>.
                        </p>

                        <div class="mb-3">
                            <label for="inputUsernameOrEmail" class="form-label fw-semibold fs-7 text-dark">
                                Username hoặc Email <span class="text-danger">*</span>
                            </label>
                            <div class="input-group">
                                <span class="input-group-text bg-light border-end-0 fs-7 text-muted">
                                    <i class="bi bi-person-badge"></i>
                                </span>
                                <input type="text" class="form-control fs-7 rounded-end-3" 
                                       id="inputUsernameOrEmail" name="usernameOrEmail" 
                                       placeholder="Ví dụ: chi hoặc chi@teamwork.com" required autofocus>
                            </div>
                        </div>

                        <div class="p-3 bg-light rounded-3 border">
                            <div class="d-flex align-items-center justify-content-between fs-9 text-muted mb-1">
                                <span>Hạn ngạch thành viên:</span>
                                <strong class="text-dark">${memberCount}/10 người</strong>
                            </div>
                            <div class="progress" style="height: 5px;">
                                <div class="progress-bar bg-success" role="progressbar" style="width: ${memberCount * 10}%;"></div>
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer border-0 pt-0">
                        <button type="button" class="btn btn-light rounded-pill px-4 fs-7 fw-semibold" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-success rounded-pill px-4 fs-7 fw-semibold shadow-sm">
                            <i class="bi bi-send-fill me-1"></i> Gửi lời mời (7 ngày)
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<!-- 7. NẠP FILE JAVASCRIPT KÉO THẢ & LỌC TỨC THÌ (0.01 GIÂY) -->
<script src="${pageContext.request.contextPath}/js/tasks.js"></script>

<!-- 8. NẠP FOOTER CHUNG -->
<jsp:include page="/includes/footer.jsp" />
