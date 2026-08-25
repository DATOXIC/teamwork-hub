<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!-- 1. NẠP HEADER & NAVBAR CHUNG -->
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container-fluid px-lg-5 py-4">

    <!-- 2. THANH ĐIỀU HƯỚNG DỰ ÁN & CHUYỂN TAB (Kanban / Docs / Chat) -->
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4 pb-3 border-bottom bg-white p-3 rounded-4 shadow-sm">
        
        <!-- Cụm bên trái: Nút quay lại + Tên dự án + Chuyển Tab -->
        <div class="d-flex flex-wrap align-items-center gap-3">
            <a href="${pageContext.request.contextPath}/project?action=list" 
               class="btn btn-outline-secondary btn-sm rounded-pill px-3 shadow-none" 
               title="Quay về danh sách dự án">
                <i class="bi bi-arrow-left me-1"></i> Dashboard
            </a>
            
            <div class="border-start ps-3 d-flex align-items-center gap-3">
                <div>
                    <h4 class="fw-extrabold text-dark mb-0 tracking-tight">${project.name}</h4>
                    <span class="fs-8 text-muted">Kênh Thảo luận & Trao đổi nhóm</span>
                </div>

                <!-- 3 Nút chuyển phân hệ nhanh: Kanban / Docs / Chat -->
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
                       class="btn btn-sm btn-white bg-white text-primary shadow-2xs rounded-pill px-3 py-1 fw-bold fs-8">
                        <i class="bi bi-chat-dots me-1"></i> Thảo luận
                    </a>
                </div>
            </div>
        </div>

        <!-- Cụm bên phải: Thống kê số tin nhắn -->
        <div>
            <span class="badge bg-primary-subtle text-primary rounded-pill px-3 py-2 fs-8 fw-semibold border border-primary-subtle">
                <i class="bi bi-chat-left-text me-1"></i> ${messageList.size()} tin nhắn
            </span>
        </div>
    </div>

    <!-- 3. BỐ CỤC 2 CỘT (CỘT TRÁI: DANH SÁCH & HƯỚNG DẪN + CỘT PHẢI: KHUNG CHAT) -->
    <div class="row g-4">

        <!-- ========================================================
             CỘT BÊN TRÁI (COL-12 COL-LG-3): THÀNH VIÊN & HƯỚNG DẪN #MENTION
             ======================================================== -->
        <div class="col-12 col-lg-3">
            <div class="d-flex flex-column gap-3">
                
                <!-- Card 1: Thành viên trong dự án -->
                <div class="card border-0 bg-white shadow-sm rounded-4 p-3">
                    <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                        <h6 class="fw-bold mb-0 text-dark fs-7">
                            <i class="bi bi-people text-primary me-2"></i>Thành viên nhóm
                        </h6>
                        <span class="badge bg-secondary-subtle text-secondary rounded-pill fs-9">${userList.size()}</span>
                    </div>

                    <div class="d-flex flex-column gap-2">
                        <c:forEach items="${userList}" var="u">
                            <div class="d-flex align-items-center justify-content-between p-2 rounded-3 bg-light-subtle">
                                <div class="d-flex align-items-center gap-2">
                                    <div class="avatar-circle bg-primary text-white rounded-circle d-flex align-items-center justify-content-center fw-bold fs-8" 
                                         style="width: 28px; height: 28px;">
                                        ${u.fullName.substring(0, 1).toUpperCase()}
                                    </div>
                                    <div>
                                        <span class="fw-semibold text-dark fs-8 d-block">${u.fullName}</span>
                                        <span class="fs-9 text-muted">${u.role}</span>
                                    </div>
                                </div>
                                <span class="badge bg-success-subtle text-success rounded-pill px-2 fs-9">Online</span>
                            </div>
                        </c:forEach>
                    </div>
                </div>

                <!-- Card 2: Hướng dẫn cú pháp #mention thông minh -->
                <div class="card border-0 bg-white shadow-sm rounded-4 p-3 border-start border-4 border-primary">
                    <h6 class="fw-bold mb-2 text-dark fs-7">
                        <i class="bi bi-magic text-primary me-1"></i> Cú pháp liên kết nhanh
                    </h6>
                    <p class="text-muted fs-8 mb-3">Gõ các ký hiệu sau trong tin nhắn để tự động tạo link bấm được:</p>

                    <div class="d-flex flex-column gap-2 fs-8">
                        <div class="p-2 bg-light rounded-3 border">
                            <code class="text-primary fw-bold">#doc-1</code>
                            <span class="text-secondary d-block fs-9 mt-1">→ Link đến bài viết tài liệu ID 1</span>
                        </div>
                        <div class="p-2 bg-light rounded-3 border">
                            <code class="text-success fw-bold">#task-3</code>
                            <span class="text-secondary d-block fs-9 mt-1">→ Link đến công việc Kanban ID 3</span>
                        </div>
                        <div class="p-2 bg-light rounded-3 border">
                            <code class="text-info fw-bold">@NguyenVanAn</code>
                            <span class="text-secondary d-block fs-9 mt-1">→ Tô màu nhắc tên thành viên</span>
                        </div>
                    </div>
                </div>

                <!-- Card 3: Tra cứu nhanh ID để gõ #mention -->
                <div class="card border-0 bg-white shadow-sm rounded-4 p-3">
                    <h6 class="fw-bold mb-2 text-dark fs-7">
                        <i class="bi bi-bookmark-star text-warning me-1"></i> ID tra cứu nhanh
                    </h6>
                    <div class="accordion accordion-flush" id="quickRefAccordion">
                        
                        <!-- Danh sách Doc ID -->
                        <div class="accordion-item border-0">
                            <h2 class="accordion-header">
                                <button class="accordion-button collapsed px-0 py-2 fs-8 fw-semibold text-dark shadow-none" type="button" data-bs-toggle="collapse" data-bs-target="#collapseDocs">
                                    📄 Tài liệu (${docList.size()})
                                </button>
                            </h2>
                            <div id="collapseDocs" class="accordion-collapse collapse" data-bs-parent="#quickRefAccordion">
                                <div class="accordion-body px-0 py-1 fs-9">
                                    <c:forEach items="${docList}" var="d">
                                        <div class="text-truncate py-1 text-secondary">
                                            <code class="text-primary">#doc-${d.id}</code>: ${d.title}
                                        </div>
                                    </c:forEach>
                                </div>
                            </div>
                        </div>

                        <!-- Danh sách Task ID -->
                        <div class="accordion-item border-0">
                            <h2 class="accordion-header">
                                <button class="accordion-button collapsed px-0 py-2 fs-8 fw-semibold text-dark shadow-none" type="button" data-bs-toggle="collapse" data-bs-target="#collapseTasks">
                                    ✅ Công việc (${taskList.size()})
                                </button>
                            </h2>
                            <div id="collapseTasks" class="accordion-collapse collapse" data-bs-parent="#quickRefAccordion">
                                <div class="accordion-body px-0 py-1 fs-9">
                                    <c:forEach items="${taskList}" var="t">
                                        <div class="text-truncate py-1 text-secondary">
                                            <code class="text-success">#task-${t.id}</code>: ${t.title}
                                        </div>
                                    </c:forEach>
                                </div>
                            </div>
                        </div>

                    </div>
                </div>

            </div>
        </div>

        <!-- ========================================================
             CỘT BÊN PHẢI (COL-12 COL-LG-9): DÒNG THỜI GIAN TIN NHẮN & Ô NHẬP
             ======================================================== -->
        <div class="col-12 col-lg-9">
            <div class="card border-0 bg-white shadow-sm rounded-4 overflow-hidden d-flex flex-column" style="height: 75vh;">
                
                <!-- 1. Đầu khung Chat: Tiêu đề kênh -->
                <div class="px-4 py-3 border-bottom bg-light d-flex align-items-center justify-content-between">
                    <div class="d-flex align-items-center gap-2">
                        <span class="badge bg-primary text-white rounded-pill p-2">
                            <i class="bi bi-hash fs-6"></i>
                        </span>
                        <div>
                            <h6 class="fw-bold mb-0 text-dark">Kênh Thảo luận chung</h6>
                            <span class="fs-8 text-muted">Trao đổi tiến độ và thắc mắc kỹ thuật của dự án</span>
                        </div>
                    </div>
                </div>

                <!-- 2. Thân khung Chat: Dòng thời gian tin nhắn (Cuộn dọc) -->
                <div class="flex-grow-1 p-4 overflow-y-auto d-flex flex-column gap-3" id="chatMessageContainer">
                    
                    <c:forEach items="${messageList}" var="msg">
                        <!-- Thẻ tin nhắn: Phân biệt tin nhắn của mình vs người khác -->
                        <div class="message-card d-flex align-items-start gap-3 p-3 rounded-3 border ${msg.authorId == sessionScope.currentUser.id ? 'bg-primary-subtle border-primary-subtle' : 'bg-light-subtle border-light'}" 
                             id="msg-${msg.id}">
                            
                            <!-- Avatar tròn ký tự đầu -->
                            <div class="avatar-circle ${msg.authorId == sessionScope.currentUser.id ? 'bg-primary' : 'bg-dark'} text-white rounded-circle d-flex align-items-center justify-content-center fw-bold fs-7 flex-shrink-0"
                                 style="width: 38px; height: 38px;">
                                ${msg.authorInitial}
                            </div>

                            <!-- Thân tin nhắn -->
                            <div class="flex-grow-1">
                                <div class="d-flex align-items-center justify-content-between mb-1">
                                    <div class="d-flex align-items-center gap-2">
                                        <span class="fw-bold text-dark fs-7">${msg.authorName}</span>
                                        <c:if test="${msg.authorId == sessionScope.currentUser.id}">
                                            <span class="badge bg-primary text-white rounded-pill px-2 py-0 fs-9">Bạn</span>
                                        </c:if>
                                        <span class="text-muted fs-9"><i class="bi bi-clock me-1"></i>${msg.sentAt}</span>
                                    </div>

                                    <!-- Nút xóa tin nhắn (Chỉ xóa tin của chính mình hoặc Admin) -->
                                    <c:if test="${msg.authorId == sessionScope.currentUser.id || sessionScope.currentUser.role == 'ADMIN'}">
                                        <a href="${pageContext.request.contextPath}/chat?action=delete&projectId=${project.id}&messageId=${msg.id}" 
                                           class="text-muted text-hover-danger fs-8 text-decoration-none"
                                           onclick="return confirm('Bạn có chắc chắn muốn xóa tin nhắn này không?');"
                                           title="Xóa tin nhắn">
                                            <i class="bi bi-trash3"></i>
                                        </a>
                                    </c:if>
                                </div>

                                <!-- Nội dung tin nhắn (Có gắn class để chat.js quét và render mention) -->
                                <div class="message-body fs-7 text-dark lh-base" style="word-break: break-word; white-space: pre-line;" data-raw-content="<c:out value='${msg.content}' />"><c:out value="${msg.content}" /></div>
                            </div>

                        </div>
                    </c:forEach>

                    <!-- Hiển thị khi chưa có tin nhắn nào -->
                    <c:if test="${empty messageList}">
                        <div class="text-center text-muted py-5 my-auto" id="emptyChatPlaceholder">
                            <i class="bi bi-chat-heart display-3 d-block mb-3 text-primary opacity-50"></i>
                            <h5 class="fw-bold text-dark">Chưa có cuộc trò chuyện nào</h5>
                            <p class="fs-7 text-secondary">Hãy gửi tin nhắn đầu tiên để bắt đầu thảo luận cùng các thành viên trong nhóm!</p>
                        </div>
                    </c:if>

                </div>

                <!-- 3. Chân khung Chat: Form nhập tin nhắn -->
                <div class="p-3 bg-light border-top">
                    <form method="post" action="${pageContext.request.contextPath}/chat" id="chatForm" class="d-flex align-items-center gap-2">
                        <input type="hidden" name="action" value="sendProjectMessage">
                        <input type="hidden" name="projectId" value="${project.id}">

                        <!-- Ô nhập nội dung tin nhắn -->
                        <div class="input-group">
                            <span class="input-group-text bg-white border-end-0 rounded-start-pill ps-3 text-muted">
                                <i class="bi bi-chat-left-dots"></i>
                            </span>
                            <input type="text" 
                                   class="form-control border-start-0 border-end-0 py-2 fs-7 shadow-none" 
                                   id="chatInput" 
                                   name="content" 
                                   placeholder="Nhập tin nhắn... (Gõ #doc-1, #task-1 hoặc @tên để liên kết)" 
                                   autocomplete="off" 
                                   required>
                            <button type="submit" class="btn btn-primary-custom rounded-end-pill px-4 fw-semibold shadow-sm" id="btnSend">
                                <span>Gửi</span>
                                <i class="bi bi-send-fill ms-1"></i>
                            </button>
                        </div>
                    </form>
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

<!-- 5. NẠP BỘ MÁY XỬ LÝ CHAT & RENDER MENTION (chat.js) -->
<script src="${pageContext.request.contextPath}/js/chat.js"></script>

<!-- 6. NẠP FOOTER CHUNG -->
<jsp:include page="/includes/footer.jsp" />
