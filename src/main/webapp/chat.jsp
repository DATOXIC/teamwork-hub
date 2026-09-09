<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!-- 1. NẠP HEADER & NAVBAR CHUNG -->
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="clickup-legacy-app-layout">
    <aside class="clickup-legacy-rail" aria-label="Điều hướng nhanh">
        <a href="${pageContext.request.contextPath}/project?action=list" class="clickup-legacy-rail-item" title="Workspace"><i class="bi bi-grid-1x2-fill"></i></a>
        <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}" class="clickup-legacy-rail-item" title="Tasks"><i class="bi bi-check2-square"></i></a>
        <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}" class="clickup-legacy-rail-item" title="Docs"><i class="bi bi-file-earmark-text"></i></a>
        <a href="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}" class="clickup-legacy-rail-item active" title="Chat"><i class="bi bi-chat-dots"></i></a>
        <span class="clickup-legacy-rail-spacer"></span>
        <a href="${pageContext.request.contextPath}/profile" class="clickup-legacy-rail-avatar" title="Hồ sơ">${sessionScope.currentUser.fullName.substring(0, 1).toUpperCase()}</a>
    </aside>
    <aside class="clickup-legacy-sidebar">
        <div class="clickup-legacy-sidebar-brand"><span> T </span><strong>TeamWork Hub</strong></div>
        <a href="${pageContext.request.contextPath}/project?action=list" class="clickup-legacy-sidebar-link"><i class="bi bi-grid"></i> Tất cả Spaces</a>
        <div class="clickup-legacy-sidebar-label">Không gian hiện tại</div>
        <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}" class="clickup-legacy-sidebar-link"><i class="bi bi-kanban"></i> ${project.name}</a>
        <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}" class="clickup-legacy-sidebar-link"><i class="bi bi-file-earmark-text"></i> Tài liệu</a>
        <a href="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}" class="clickup-legacy-sidebar-link active"><i class="bi bi-chat-dots"></i> Thảo luận</a>
    </aside>
    <main class="clickup-legacy-app-main">
<div class="container-fluid px-3 px-lg-5 py-4 clickup-legacy-page">

    <!-- 2. THANH ĐIỀU HƯỚNG DỰ ÁN & CHUYỂN TAB (Kanban / Docs / Chat / Báo cáo) -->
    <div class="clickup-legacy-header mb-3">
        
        <!-- Cụm bên trái: Nút quay lại + Tên dự án + Chuyển Tab -->
        <div class="d-flex flex-wrap align-items-center gap-3">
            <a href="${pageContext.request.contextPath}/project?action=list" 
               class="btn btn-outline-secondary btn-sm rounded-pill px-3 shadow-none d-inline-flex align-items-center gap-1" 
               title="Quay về danh sách dự án"
               aria-label="Quay về danh sách dự án">
                <i class="bi bi-arrow-left" aria-hidden="true"></i>
                <span>Dashboard</span>
            </a>
            
            <div class="border-start ps-3 d-flex flex-wrap align-items-center gap-2 gap-md-3">
                <div>
                    <!-- Thẻ H1 ngữ nghĩa cho SEO & A11y, style hiển thị tinh tế -->
                    <h1 class="h4 fw-extrabold text-dark mb-0 tracking-tight">${project.name}</h1>
                    <span class="fs-8 text-muted">Kênh Thảo luận & Trao đổi nhóm</span>
                </div>

                <!-- 4 Nút chuyển phân hệ nhanh: Cuộn ngang mượt mà trên mobile -->
                <nav aria-label="Phân hệ dự án" class="d-flex align-items-center gap-1 gap-md-2 bg-light p-1 rounded-pill border subnav-tabs-scroll">
                    <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}" 
                       class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8"
                       title="Mở bảng Kanban">
                        <i class="bi bi-kanban me-1" aria-hidden="true"></i> Kanban
                    </a>
                    <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}" 
                       class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8"
                       title="Mở tài liệu wiki">
                        <i class="bi bi-journal-text me-1" aria-hidden="true"></i> Tài liệu
                    </a>
                    <a href="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}" 
                       class="btn btn-sm btn-white bg-white text-primary shadow-2xs rounded-pill px-3 py-1 fw-bold fs-8"
                       aria-current="page"
                       title="Kênh thảo luận trực tiếp">
                        <i class="bi bi-chat-dots me-1" aria-hidden="true"></i> Thảo luận
                    </a>
                    <a href="${pageContext.request.contextPath}/project?action=report&projectId=${project.id}" 
                       class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8"
                       title="Xem báo cáo tổng hợp tiến độ và đánh giá">
                        <i class="bi bi-file-earmark-bar-graph me-1" aria-hidden="true"></i> Báo cáo
                    </a>
                </nav>
            </div>
        </div>

        <!-- Cụm bên phải: Nút bật Sidebar Mobile + Thống kê số tin nhắn -->
        <div class="d-flex align-items-center gap-2">
            <!-- Nút bật Drawer thành viên trên di động -->
            <button class="btn btn-outline-primary btn-sm rounded-pill px-3 py-1 d-lg-none d-inline-flex align-items-center gap-1 shadow-none" 
                    type="button" 
                    data-bs-toggle="offcanvas" 
                    data-bs-target="#chatSidebarOffcanvas" 
                    aria-controls="chatSidebarOffcanvas"
                    aria-label="Xem thành viên và ID tra cứu">
                <i class="bi bi-people-fill" aria-hidden="true"></i>
                <span class="fs-8 fw-semibold">Thành viên (${userList.size()})</span>
            </button>

            <span class="badge bg-primary-subtle text-primary rounded-pill px-3 py-2 fs-8 fw-semibold border border-primary-subtle d-inline-flex align-items-center gap-1">
                <i class="bi bi-chat-left-text" aria-hidden="true"></i>
                <span>${messageList.size()} tin nhắn</span>
            </span>
        </div>
    </div>

    <!-- UI-04: Floating Toast -->
    <jsp:include page="/includes/toast.jsp" />

    <!-- 3. BỐ CỤC 2 CỘT (DESKTOP) HOẶC 1 CỘT (MOBILE) -->
    <div class="row g-4">

        <!-- ========================================================
             CỘT BÊN TRÁI: THÀNH VIÊN & CÚ PHÁP (DESKTOP VIEW)
             ======================================================== -->
        <aside class="col-12 col-lg-4 col-xl-3 d-none d-lg-block" aria-label="Thông tin nhóm và phím tắt">
            <div class="d-flex flex-column gap-3">
                
                <!-- Card 1: Thành viên trong dự án -->
                <div class="card border-0 bg-white shadow-sm rounded-4 p-3">
                    <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                        <h2 class="fw-bold mb-0 text-dark fs-7">
                            <i class="bi bi-people text-primary me-2" aria-hidden="true"></i>Thành viên nhóm
                        </h2>
                        <span class="badge bg-secondary-subtle text-secondary rounded-pill fs-9">${userList.size()}</span>
                    </div>

                    <div class="d-flex flex-column gap-2 overflow-y-auto" style="max-height: 28vh;" role="list">
                        <c:choose>
                            <c:when test="${not empty userList}">
                                <c:forEach items="${userList}" var="u">
                                    <div class="d-flex align-items-center justify-content-between p-2 rounded-3 bg-light-subtle" role="listitem">
                                        <div class="d-flex align-items-center gap-2">
                                            <div class="avatar-circle bg-primary text-white rounded-circle d-flex align-items-center justify-content-center fw-bold fs-8 shadow-2xs" 
                                                 style="width: 28px; height: 28px;"
                                                 aria-hidden="true">
                                                ${u.fullName.substring(0, 1).toUpperCase()}
                                            </div>
                                            <div>
                                                <span class="fw-semibold text-dark fs-8 d-block">${u.fullName}</span>
                                                <span class="fs-9 text-muted">${u.role}</span>
                                            </div>
                                        </div>
                                        <span class="badge bg-secondary-subtle text-secondary rounded-pill px-2 fs-9">Thành viên</span>
                                    </div>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <div class="text-center py-3 text-muted fs-8">
                                    <i class="bi bi-person-x d-block mb-1 fs-5 text-secondary opacity-50" aria-hidden="true"></i>
                                    Chưa có thành viên nào
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <!-- Card 2: Tra cứu nhanh ID để gõ #mention -->
                <div class="card border-0 bg-white shadow-sm rounded-4 p-3">
                    <h2 class="fw-bold mb-2 text-dark fs-7">
                        <i class="bi bi-bookmark-star text-warning me-1" aria-hidden="true"></i> ID tra cứu nhanh
                    </h2>
                    <div class="accordion accordion-flush" id="quickRefAccordion">
                        
                        <!-- Danh sách Doc ID -->
                        <div class="accordion-item border-0">
                            <h3 class="accordion-header">
                                <button class="accordion-button collapsed px-0 py-2 fs-8 fw-semibold text-dark shadow-none" type="button" data-bs-toggle="collapse" data-bs-target="#collapseDocs" aria-expanded="false" aria-controls="collapseDocs">
                                    📄 Tài liệu (${docList.size()})
                                </button>
                            </h3>
                            <div id="collapseDocs" class="accordion-collapse collapse" data-bs-parent="#quickRefAccordion">
                                <div class="accordion-body px-0 py-1 fs-9">
                                    <c:choose>
                                        <c:when test="${not empty docList}">
                                            <c:forEach items="${docList}" var="d">
                                                <button type="button" 
                                                        class="btn btn-link text-start text-truncate py-1 px-1 text-secondary w-100 text-decoration-none fs-9 btn-shortcut-item" 
                                                        onclick="insertShortcut('#doc-${d.id}')" 
                                                        title="Bấm để chèn vào ô chat"
                                                        aria-label="Chèn liên kết tài liệu số ${d.id}: ${d.title}">
                                                    <code class="text-primary fw-bold">#doc-${d.id}</code>: ${d.title}
                                                </button>
                                            </c:forEach>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="text-muted py-2 fs-9 fst-italic text-center">Chưa có tài liệu nào</div>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>

                        <!-- Danh sách Task ID -->
                        <div class="accordion-item border-0">
                            <h3 class="accordion-header">
                                <button class="accordion-button collapsed px-0 py-2 fs-8 fw-semibold text-dark shadow-none" type="button" data-bs-toggle="collapse" data-bs-target="#collapseTasks" aria-expanded="false" aria-controls="collapseTasks">
                                    ✅ Công việc (${taskList.size()})
                                </button>
                            </h3>
                            <div id="collapseTasks" class="accordion-collapse collapse" data-bs-parent="#quickRefAccordion">
                                <div class="accordion-body px-0 py-1 fs-9">
                                    <c:choose>
                                        <c:when test="${not empty taskList}">
                                            <c:forEach items="${taskList}" var="t">
                                                <button type="button" 
                                                        class="btn btn-link text-start text-truncate py-1 px-1 text-secondary w-100 text-decoration-none fs-9 btn-shortcut-item" 
                                                        onclick="insertShortcut('#task-${t.id}')" 
                                                        title="Bấm để chèn vào ô chat"
                                                        aria-label="Chèn liên kết công việc số ${t.id}: ${t.title}">
                                                    <code class="text-success fw-bold">#task-${t.id}</code>: ${t.title}
                                                </button>
                                            </c:forEach>
                                        </c:when>
                                        <c:otherwise>
                                            <div class="text-muted py-2 fs-9 fst-italic text-center">Chưa có công việc nào</div>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>

                    </div>
                </div>

            </div>
        </aside>

        <!-- ========================================================
             CỘT BÊN PHẢI: DÒNG THỜI GIAN TIN NHẮN & Ô NHẬP (TRUNG TÂM)
             ======================================================== -->
        <main class="col-12 col-lg-8 col-xl-9">
            <div class="card border-0 bg-white shadow-sm rounded-4 overflow-hidden d-flex flex-column chat-card-container">
                
                <!-- 1. Đầu khung Chat: Tiêu đề kênh -->
                <div class="px-4 py-3 border-bottom bg-light d-flex align-items-center justify-content-between">
                    <div class="d-flex align-items-center gap-2">
                        <span class="badge bg-primary text-white rounded-pill p-2" aria-hidden="true">
                            <i class="bi bi-hash fs-6"></i>
                        </span>
                        <div>
                            <h2 class="fw-bold mb-0 text-dark fs-6">Kênh Thảo luận chung</h2>
                            <span class="fs-8 text-muted">Trao đổi tiến độ và thắc mắc kỹ thuật của dự án</span>
                        </div>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <span id="chatSyncStatus" class="chat-sync-status" aria-live="polite"><i class="bi bi-circle-fill"></i> Đã đồng bộ</span>
                        <button type="button" id="chatRefreshButton" class="btn btn-sm btn-light border rounded-circle p-0 d-inline-flex align-items-center justify-content-center" style="width: 30px;height:30px" title="Làm mới tin nhắn" aria-label="Làm mới tin nhắn">
                            <i class="bi bi-arrow-clockwise"></i>
                        </button>
                    </div>
                </div>

                <!-- 2. Thân khung Chat: Dòng thời gian tin nhắn (Cuộn dọc & A11y live region) -->
                <div class="flex-grow-1 p-3 p-md-4 overflow-y-auto d-flex flex-column gap-3 bg-light-subtle" 
                     id="chatMessageContainer"
                     role="log"
                     aria-live="polite"
                     aria-atomic="false"
                     aria-label="Dòng thời gian tin nhắn thảo luận">
                    
                    <c:forEach items="${messageList}" var="msg">
                        <c:choose>
                            <%-- Tin nhắn của chính mình (Canh phải, Nền Tím Indigo chữ trắng) --%>
                            <c:when test="${msg.authorId == sessionScope.currentUser.id}">
                                <div class="chat-row-me" id="msg-${msg.id}">
                                    <c:if test="${msg.authorId == sessionScope.currentUser.id || sessionScope.currentUser.role == 'ADMIN'}">
                                        <a href="${pageContext.request.contextPath}/chat?action=delete&projectId=${project.id}&messageId=${msg.id}" 
                                           class="text-muted text-hover-danger fs-9 text-decoration-none opacity-50 hover-opacity-100 me-1"
                                           onclick="return confirm('Bạn có chắc chắn muốn xóa tin nhắn này không?');"
                                           title="Xóa tin nhắn"
                                           aria-label="Xóa tin nhắn gửi lúc ${msg.sentAt}">
                                            <i class="bi bi-trash3" aria-hidden="true"></i>
                                        </a>
                                    </c:if>
                                    <div class="chat-bubble-me">
                                        <div class="message-body fs-8 lh-base text-white" style="word-break: break-word; white-space: pre-line;" data-raw-content="<c:out value='${msg.content}' />"><c:out value="${msg.content}" /></div>
                                        <div class="d-flex justify-content-end align-items-center gap-1 mt-1">
                                            <span class="chat-time-me"><i class="bi bi-clock me-1" aria-hidden="true"></i>${msg.sentAt}</span>
                                        </div>
                                    </div>
                                </div>
                            </c:when>

                            <%-- Tin nhắn của thành viên khác (Canh trái kèm Avatar, Nền trắng viền xám) --%>
                            <c:otherwise>
                                <div class="chat-row-other" id="msg-${msg.id}">
                                    <div class="avatar-circle bg-dark text-white rounded-circle d-flex align-items-center justify-content-center fw-bold fs-8 flex-shrink-0 shadow-2xs"
                                         style="width: 34px; height: 34px;"
                                         aria-hidden="true">
                                        ${msg.authorInitial}
                                    </div>
                                    <div class="chat-bubble-other">
                                        <div class="d-flex align-items-center justify-content-between gap-3 mb-1">
                                            <span class="fw-bold text-dark fs-8">${msg.authorName}</span>
                                            <c:if test="${sessionScope.currentUser.role == 'ADMIN'}">
                                                <a href="${pageContext.request.contextPath}/chat?action=delete&projectId=${project.id}&messageId=${msg.id}" 
                                                   class="text-muted text-hover-danger fs-9 text-decoration-none opacity-50 hover-opacity-100"
                                                   onclick="return confirm('Bạn có chắc chắn muốn xóa tin nhắn này không?');"
                                                   title="Xóa tin nhắn"
                                                   aria-label="Xóa tin nhắn của ${msg.authorName} gửi lúc ${msg.sentAt}">
                                                    <i class="bi bi-trash3" aria-hidden="true"></i>
                                                </a>
                                            </c:if>
                                        </div>
                                        <div class="message-body fs-8 text-dark lh-base" style="word-break: break-word; white-space: pre-line;" data-raw-content="<c:out value='${msg.content}' />"><c:out value="${msg.content}" /></div>
                                        <div class="d-flex justify-content-end align-items-center gap-1 mt-1">
                                            <span class="chat-time-other"><i class="bi bi-clock me-1" aria-hidden="true"></i>${msg.sentAt}</span>
                                        </div>
                                    </div>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>

                    <!-- Hiển thị khi chưa có tin nhắn nào (Empty State) -->
                    <c:if test="${empty messageList}">
                        <div class="text-center text-muted py-5 my-auto" id="emptyChatPlaceholder">
                            <i class="bi bi-chat-heart display-3 d-block mb-3 text-primary opacity-50" aria-hidden="true"></i>
                            <h3 class="fw-bold text-dark fs-5">Chưa có cuộc trò chuyện nào</h3>
                            <p class="fs-8 text-secondary">Hãy gửi tin nhắn đầu tiên để bắt đầu thảo luận cùng các thành viên trong nhóm!</p>
                        </div>
                    </c:if>

                </div>

                <!-- 3. Chân khung Chat: Form nhập tin nhắn & Gợi ý chèn nhanh -->
                <div class="p-3 bg-white border-top">
                    <!-- Thanh phím tắt chèn cú pháp nhanh -->
                    <div class="d-flex flex-wrap align-items-center gap-2 mb-2">
                        <span class="fs-9 text-muted fw-semibold d-inline-flex align-items-center gap-1">
                            <i class="bi bi-lightning-charge text-warning" aria-hidden="true"></i> Chèn nhanh:
                        </span>
                        <button type="button" class="btn btn-outline-secondary btn-xs rounded-pill px-2 py-0 fs-9 shadow-none" onclick="insertShortcut('#doc-')" aria-label="Chèn tiền tố liên kết bài viết wiki #doc-">
                            📄 #doc-
                        </button>
                        <button type="button" class="btn btn-outline-secondary btn-xs rounded-pill px-2 py-0 fs-9 shadow-none" onclick="insertShortcut('#task-')" aria-label="Chèn tiền tố liên kết công việc #task-">
                            ✅ #task-
                        </button>
                        <button type="button" class="btn btn-outline-secondary btn-xs rounded-pill px-2 py-0 fs-9 shadow-none" onclick="insertShortcut('@')" aria-label="Chèn ký tự nhắc tên @thành viên">
                            👤 @nhắc tên
                        </button>
                    </div>

                    <form method="post" action="${pageContext.request.contextPath}/chat" id="chatForm" class="d-flex align-items-center gap-2">
                        <input type="hidden" name="action" value="sendProjectMessage">
                        <input type="hidden" name="projectId" value="${project.id}">

                        <!-- Ô nhập nội dung tin nhắn có A11y Label & Focus Ring -->
                        <div class="input-group chat-input-group">
                            <span class="input-group-text bg-light border-end-0 rounded-start-pill ps-3 text-muted">
                                <i class="bi bi-chat-left-dots" aria-hidden="true"></i>
                            </span>
                            <label for="chatInput" class="visually-hidden">Nội dung tin nhắn</label>
                            <input type="text" 
                                   class="form-control bg-light border-start-0 border-end-0 py-2 fs-8 shadow-none" 
                                   id="chatInput" 
                                   name="content" 
                                   placeholder="Nhập tin nhắn thảo luận... (Gõ #doc-1, #task-1 hoặc @tên để liên kết)" 
                                   aria-label="Nhập tin nhắn thảo luận"
                                   autocomplete="off" 
                                   required>
                            <button type="submit" class="btn btn-primary-custom rounded-end-pill px-4 fw-semibold shadow-2xs text-white d-inline-flex align-items-center gap-1" id="btnSend" aria-label="Gửi tin nhắn">
                                <span>Gửi</span>
                                <i class="bi bi-send-fill" aria-hidden="true"></i>
                            </button>
                        </div>
                    </form>
                </div>

            </div>
        </main>

    </div>
</div>

<!-- ========================================================
     OFFCANVAS CHO MOBILE: XEM THÀNH VIÊN VÀ TRA CỨU ID
     ======================================================== -->
<div class="offcanvas offcanvas-start rounded-end-4" tabindex="-1" id="chatSidebarOffcanvas" aria-labelledby="chatSidebarOffcanvasLabel">
    <div class="offcanvas-header border-bottom py-3">
        <h5 class="offcanvas-title fw-bold text-dark fs-6 d-flex align-items-center gap-2" id="chatSidebarOffcanvasLabel">
            <i class="bi bi-info-circle text-primary" aria-hidden="true"></i>
            Thông tin nhóm & Tra cứu ID
        </h5>
        <button type="button" class="btn-close shadow-none" data-bs-dismiss="offcanvas" aria-label="Đóng bảng thông tin"></button>
    </div>
    <div class="offcanvas-body p-3 d-flex flex-column gap-3">
        
        <!-- Card Mobile 1: Thành viên nhóm -->
        <div class="card border-0 bg-light p-3 rounded-4">
            <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                <span class="fw-bold text-dark fs-7">
                    <i class="bi bi-people text-primary me-2" aria-hidden="true"></i>Thành viên nhóm
                </span>
                <span class="badge bg-secondary-subtle text-secondary rounded-pill fs-9">${userList.size()}</span>
            </div>
            <div class="d-flex flex-column gap-2 overflow-y-auto" style="max-height: 35vh;">
                <c:choose>
                    <c:when test="${not empty userList}">
                        <c:forEach items="${userList}" var="u">
                            <div class="d-flex align-items-center justify-content-between p-2 rounded-3 bg-white">
                                <div class="d-flex align-items-center gap-2">
                                    <div class="avatar-circle bg-primary text-white rounded-circle d-flex align-items-center justify-content-center fw-bold fs-8" 
                                         style="width: 28px; height: 28px;"
                                         aria-hidden="true">
                                        ${u.fullName.substring(0, 1).toUpperCase()}
                                    </div>
                                    <div>
                                        <span class="fw-semibold text-dark fs-8 d-block">${u.fullName}</span>
                                        <span class="fs-9 text-muted">${u.role}</span>
                                    </div>
                                </div>
                                <span class="badge bg-secondary-subtle text-secondary rounded-pill px-2 fs-9">Thành viên</span>
                            </div>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <div class="text-center py-3 text-muted fs-8">Chưa có thành viên nào</div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Card Mobile 2: Tra cứu ID -->
        <div class="card border-0 bg-light p-3 rounded-4">
            <span class="fw-bold text-dark fs-7 mb-2 d-block">
                <i class="bi bi-bookmark-star text-warning me-1" aria-hidden="true"></i> ID tra cứu nhanh
            </span>
            <div class="accordion accordion-flush" id="mobileQuickRefAccordion">
                <div class="accordion-item border-0 bg-transparent">
                    <h3 class="accordion-header">
                        <button class="accordion-button collapsed px-0 py-2 fs-8 fw-semibold text-dark bg-transparent shadow-none" type="button" data-bs-toggle="collapse" data-bs-target="#mobileCollapseDocs" aria-expanded="false" aria-controls="mobileCollapseDocs">
                            📄 Tài liệu (${docList.size()})
                        </button>
                    </h3>
                    <div id="mobileCollapseDocs" class="accordion-collapse collapse" data-bs-parent="#mobileQuickRefAccordion">
                        <div class="accordion-body px-0 py-1 fs-9">
                            <c:forEach items="${docList}" var="d">
                                <button type="button" 
                                        class="btn btn-link text-start text-truncate py-1 px-1 text-secondary w-100 text-decoration-none fs-9 btn-shortcut-item" 
                                        data-bs-dismiss="offcanvas"
                                        onclick="insertShortcut('#doc-${d.id}')"
                                        aria-label="Chèn mã tài liệu ${d.title}">
                                    <code class="text-primary fw-bold">#doc-${d.id}</code>: ${d.title}
                                </button>
                            </c:forEach>
                        </div>
                    </div>
                </div>

                <div class="accordion-item border-0 bg-transparent">
                    <h3 class="accordion-header">
                        <button class="accordion-button collapsed px-0 py-2 fs-8 fw-semibold text-dark bg-transparent shadow-none" type="button" data-bs-toggle="collapse" data-bs-target="#mobileCollapseTasks" aria-expanded="false" aria-controls="mobileCollapseTasks">
                            ✅ Công việc (${taskList.size()})
                        </button>
                    </h3>
                    <div id="mobileCollapseTasks" class="accordion-collapse collapse" data-bs-parent="#mobileQuickRefAccordion">
                        <div class="accordion-body px-0 py-1 fs-9">
                            <c:forEach items="${taskList}" var="t">
                                <button type="button" 
                                        class="btn btn-link text-start text-truncate py-1 px-1 text-secondary w-100 text-decoration-none fs-9 btn-shortcut-item" 
                                        data-bs-dismiss="offcanvas"
                                        onclick="insertShortcut('#task-${t.id}')"
                                        aria-label="Chèn mã công việc ${t.title}">
                                    <code class="text-success fw-bold">#task-${t.id}</code>: ${t.title}
                                </button>
                            </c:forEach>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </div>
</div>

<!-- ========================================================
     4. TRUYỀN BIẾN TOÀN CỤC CHO FILE JAVASCRIPT (chat.js)
     ======================================================== -->
<script>
    var contextPath = "${pageContext.request.contextPath}";
    var currentProjectId = "${project.id}";
</script>

<!-- 5. NẠP BỘ MÁY XỬ LÝ CHAT & RENDER MENTION (chat.js) VỚI CACHE-BUSTING -->
<script src="${pageContext.request.contextPath}/js/chat.js?v=<%= System.currentTimeMillis() %>"></script>

<!-- 6. NẠP FOOTER CHUNG -->
</div>
    </main>
</div>
<jsp:include page="/includes/footer.jsp" />
