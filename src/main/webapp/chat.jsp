<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!-- Nạp Chat CSS chuyên biệt với phong cách Apple Glassmorphism / Arc Space -->
<c:set var="extraCss" value="styles/chat.css" scope="request" />

<!-- 1. NẠP HEADER & NAVBAR CHUNG -->
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container-fluid px-3 px-lg-5 py-4 chat-shell">

    <!-- 2. THANH ĐIỀU HƯỚNG DỰ ÁN & CHUYỂN TAB (Kanban / Docs / Chat / Báo cáo) -->
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4 p-3 chat-subnav">
        
        <!-- Cụm bên trái: Nút quay lại + Tên dự án + Chuyển Tab -->
        <div class="d-flex flex-wrap align-items-center gap-3">
            <a href="${pageContext.request.contextPath}/project?action=list" 
               class="btn btn-sm rounded-pill px-3 d-inline-flex align-items-center gap-1 chat-subnav-btn-back shadow-none" 
               title="Quay về danh sách dự án"
               aria-label="Quay về danh sách dự án">
                <i class="bi bi-arrow-left" aria-hidden="true"></i>
                <span>Dashboard</span>
            </a>
            
            <div class="border-start ps-3 d-flex flex-wrap align-items-center gap-2 gap-md-3" style="border-color: var(--chat-glass-border-subtle) !important;">
                <div>
                    <!-- Thẻ H1 ngữ nghĩa cho SEO & A11y, style hiển thị tinh tế -->
                    <h1 class="h5 fw-bold mb-0 chat-subnav-title">${project.name}</h1>
                    <span class="fs-8 text-muted">Kênh Thảo luận & Trao đổi nhóm</span>
                </div>

                <!-- 4 Nút chuyển phân hệ nhanh: Cuộn ngang mượt mà trên mobile -->
                <nav aria-label="Phân hệ dự án" class="d-flex align-items-center gap-1 p-1 rounded-pill chat-subnav-tabs subnav-tabs-scroll">
                    <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}" 
                       class="chat-tab-link"
                       title="Mở bảng Kanban">
                        <i class="bi bi-kanban me-1" aria-hidden="true"></i> Kanban
                    </a>
                    <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}" 
                       class="chat-tab-link"
                       title="Mở tài liệu wiki">
                        <i class="bi bi-journal-text me-1" aria-hidden="true"></i> Tài liệu
                    </a>
                    <a href="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}" 
                       class="chat-tab-link active"
                       aria-current="page"
                       title="Kênh thảo luận trực tiếp">
                        <i class="bi bi-chat-dots me-1" aria-hidden="true"></i> Thảo luận
                    </a>
                    <a href="${pageContext.request.contextPath}/project?action=report&projectId=${project.id}" 
                       class="chat-tab-link"
                       title="Xem báo cáo tổng hợp tiến độ và đánh giá">
                        <i class="bi bi-file-earmark-bar-graph me-1" aria-hidden="true"></i> Báo cáo
                    </a>
                </nav>
            </div>
        </div>

        <!-- Cụm bên phải: Nút bật Sidebar Mobile + Thống kê số tin nhắn Live Pulse -->
        <div class="d-flex align-items-center gap-2">
            <!-- Nút bật Drawer thành viên trên di động -->
            <button class="btn btn-sm rounded-pill px-3 py-1 d-lg-none d-inline-flex align-items-center gap-1 chat-subnav-btn-back shadow-none" 
                    type="button" 
                    data-bs-toggle="offcanvas" 
                    data-bs-target="#chatSidebarOffcanvas" 
                    aria-controls="chatSidebarOffcanvas"
                    aria-label="Xem thành viên và ID tra cứu">
                <i class="bi bi-people-fill" aria-hidden="true"></i>
                <span class="fs-8 fw-semibold">Thành viên (${userList.size()})</span>
            </button>

            <div class="chat-pulse-badge rounded-pill px-3 py-1 fs-8 fw-semibold d-inline-flex align-items-center gap-2">
                <span class="pulse-indicator" aria-hidden="true"></span>
                <span>${messageList.size()} tin nhắn</span>
            </div>
        </div>
    </div>

    <!-- UI-04: Floating Toast -->
    <jsp:include page="/includes/toast.jsp" />

    <!-- 3. BỐ CỤC 2 CỘT (DESKTOP) HOẶC 1 CỘT (MOBILE) -->
    <div class="row g-4">

        <!-- ========================================================
             CỘT BÊN TRÁI: TEAM RADAR & RESOURCE SHELF (DESKTOP)
             ======================================================== -->
        <aside class="col-12 col-lg-4 col-xl-3 d-none d-lg-block" aria-label="Thông tin nhóm và phím tắt">
            <div class="d-flex flex-column gap-3">
                
                <!-- Card 1: Team Radar (Thành viên trong dự án) -->
                <div class="chat-sidebar-card p-3">
                    <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom" style="border-color: var(--chat-glass-border-subtle) !important;">
                        <h2 class="chat-sidebar-title mb-0 d-flex align-items-center gap-2">
                            <i class="bi bi-people-fill" style="color: var(--chat-accent);" aria-hidden="true"></i>
                            <span>Team Radar</span>
                        </h2>
                        <span class="badge rounded-pill px-2 py-1 fs-9" style="background: var(--chat-glass-surface-hover); color: var(--chat-text-secondary); border: 1px solid var(--chat-glass-border-subtle);">
                            ${userList.size()} online
                        </span>
                    </div>

                    <div class="d-flex flex-column gap-2 overflow-y-auto" style="max-height: 27vh;" role="list">
                        <c:choose>
                            <c:when test="${not empty userList}">
                                <c:forEach items="${userList}" var="u">
                                    <div class="chat-member-item d-flex align-items-center justify-content-between" role="listitem">
                                        <div class="d-flex align-items-center gap-2">
                                            <div class="chat-avatar-ring" aria-hidden="true">
                                                ${u.fullName.substring(0, 1).toUpperCase()}
                                                <span class="chat-avatar-status" title="Đang hoạt động"></span>
                                            </div>
                                            <div>
                                                <span class="chat-member-name d-block text-truncate" style="max-width: 130px;">${u.fullName}</span>
                                                <span class="chat-member-role">${u.role}</span>
                                            </div>
                                        </div>
                                        <button type="button" 
                                                class="btn btn-sm p-1 text-muted border-0 shadow-none chat-hover-btn" 
                                                onclick="insertShortcut('@${u.fullName}')" 
                                                title="Nhắc tên @${u.fullName}"
                                                aria-label="Nhắc tên ${u.fullName}">
                                            <i class="bi bi-at fs-7"></i>
                                        </button>
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

                <!-- Card 2: Resource Quick-Shelf (Segmented Control tra cứu Docs & Tasks) -->
                <div class="chat-sidebar-card p-3">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                        <h2 class="chat-sidebar-title mb-0 d-flex align-items-center gap-2">
                            <i class="bi bi-layers-half" style="color: var(--chat-accent-light);" aria-hidden="true"></i>
                            <span>Resource Shelf</span>
                        </h2>
                        <span class="fs-9 text-muted">1-Click Insert</span>
                    </div>

                    <!-- Segmented Control Button Tabs -->
                    <div class="chat-segmented-control mb-3">
                        <button type="button" 
                                class="chat-segment-btn active" 
                                id="tabBtnDocs" 
                                onclick="switchResourceTab('docs')"
                                aria-label="Xem danh sách tài liệu">
                            <i class="bi bi-journal-text" aria-hidden="true"></i> Docs (${docList.size()})
                        </button>
                        <button type="button" 
                                class="chat-segment-btn" 
                                id="tabBtnTasks" 
                                onclick="switchResourceTab('tasks')"
                                aria-label="Xem danh sách công việc">
                            <i class="bi bi-check2-circle" aria-hidden="true"></i> Tasks (${taskList.size()})
                        </button>
                    </div>

                    <!-- Tab Content 1: Docs -->
                    <div id="shelfDocs" class="d-flex flex-column gap-2 overflow-y-auto" style="max-height: 26vh;">
                        <c:choose>
                            <c:when test="${not empty docList}">
                                <c:forEach items="${docList}" var="d">
                                    <button type="button" 
                                            class="chat-resource-chip" 
                                            onclick="insertShortcut('#doc-${d.id}')" 
                                            title="Bấm để chèn #doc-${d.id} vào ô chat"
                                            aria-label="Chèn mã tài liệu số ${d.id}: ${d.title}">
                                        <span class="chat-chip-tag chat-chip-tag-doc">#doc-${d.id}</span>
                                        <span class="text-truncate">${d.title}</span>
                                    </button>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <div class="text-muted py-3 fs-9 fst-italic text-center">Chưa có tài liệu nào</div>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <!-- Tab Content 2: Tasks (Ẩn mặc định) -->
                    <div id="shelfTasks" class="d-flex flex-column gap-2 overflow-y-auto d-none" style="max-height: 26vh;">
                        <c:choose>
                            <c:when test="${not empty taskList}">
                                <c:forEach items="${taskList}" var="t">
                                    <button type="button" 
                                            class="chat-resource-chip" 
                                            onclick="insertShortcut('#task-${t.id}')" 
                                            title="Bấm để chèn #task-${t.id} vào ô chat"
                                            aria-label="Chèn mã công việc số ${t.id}: ${t.title}">
                                        <span class="chat-chip-tag chat-chip-tag-task">#task-${t.id}</span>
                                        <span class="text-truncate">${t.title}</span>
                                    </button>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <div class="text-muted py-3 fs-9 fst-italic text-center">Chưa có công việc nào</div>
                            </c:otherwise>
                        </c:choose>
                    </div>

                </div>

            </div>
        </aside>

        <!-- ========================================================
             CỘT BÊN PHẢI: DÒNG THỜI GIAN TIN NHẮN & FLOATING ISLAND
             ======================================================== -->
        <main class="col-12 col-lg-8 col-xl-9">
            <div class="chat-main-card">
                
                <!-- 1. Đầu khung Chat: Tiêu đề kênh kính mờ -->
                <div class="chat-channel-header">
                    <div class="d-flex align-items-center gap-3">
                        <div class="chat-channel-icon" aria-hidden="true">
                            <i class="bi bi-hash fs-5"></i>
                        </div>
                        <div>
                            <h2 class="chat-channel-title">Kênh Thảo luận chung</h2>
                            <span class="chat-channel-desc">Không gian trao đổi tiến độ và kỹ thuật của dự án</span>
                        </div>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <button type="button" 
                                class="btn btn-sm chat-hover-btn rounded-pill px-2 py-1 fs-8" 
                                onclick="scrollToBottom()" 
                                title="Cuộn xuống tin nhắn mới nhất">
                            <i class="bi bi-arrow-down-circle me-1"></i> Mới nhất
                        </button>
                    </div>
                </div>

                <!-- 2. Thân khung Chat: Stream Feed tin nhắn kính mờ -->
                <div class="chat-feed-container" 
                     id="chatMessageContainer"
                     role="log"
                     aria-live="polite"
                     aria-atomic="false"
                     aria-label="Dòng thời gian tin nhắn thảo luận">
                    
                    <c:forEach items="${messageList}" var="msg">
                        <c:choose>
                            <%-- Tin nhắn của chính mình (Canh phải, Kính Gradient) --%>
                            <c:when test="${msg.authorId == sessionScope.currentUser.id}">
                                <div class="chat-row-me" id="msg-${msg.id}">
                                    <!-- Micro Action Bar khi rê chuột -->
                                    <div class="chat-hover-bar" role="toolbar" aria-label="Thao tác tin nhắn">
                                        <button type="button" 
                                                class="chat-hover-btn" 
                                                onclick="copyMessageText('${msg.id}')" 
                                                title="Sao chép nội dung tin nhắn"
                                                aria-label="Sao chép nội dung">
                                            <i class="bi bi-clipboard" aria-hidden="true"></i>
                                        </button>
                                        <button type="button" 
                                                class="chat-hover-btn" 
                                                onclick="quoteMessage('${msg.authorName}', '${msg.id}')" 
                                                title="Trích dẫn tin nhắn"
                                                aria-label="Trích dẫn tin nhắn">
                                            <i class="bi bi-reply-fill" aria-hidden="true"></i>
                                        </button>
                                        <button type="button" 
                                                class="chat-hover-btn" 
                                                onclick="openEditModal('${msg.id}')" 
                                                title="Chỉnh sửa tin nhắn"
                                                aria-label="Chỉnh sửa tin nhắn">
                                            <i class="bi bi-pencil" aria-hidden="true"></i>
                                        </button>
                                        <c:if test="${msg.authorId == sessionScope.currentUser.id || sessionScope.currentUser.role == 'ADMIN' || project.ownerId == sessionScope.currentUser.id}">
                                            <button type="button" 
                                                    class="chat-hover-btn chat-hover-btn-danger" 
                                                    onclick="confirmDeleteMessage('${project.id}', '${msg.id}')" 
                                                    title="Xóa tin nhắn" 
                                                    aria-label="Xóa tin nhắn">
                                                <i class="bi bi-trash3" aria-hidden="true"></i>
                                            </button>
                                        </c:if>
                                    </div>

                                    <div class="chat-bubble-me">
                                        <div class="message-body fs-8 lh-base text-white" 
                                             id="msg-content-${msg.id}"
                                             data-raw-content="<c:out value='${msg.content}' />"><c:out value="${msg.content}" /></div>
                                        <div class="d-flex justify-content-end align-items-center gap-1 mt-1">
                                            <span class="chat-time-meta text-white-50"><i class="bi bi-clock me-1" aria-hidden="true"></i>${msg.sentAt}</span>
                                        </div>
                                    </div>
                                </div>
                            </c:when>

                            <%-- Tin nhắn của thành viên khác (Canh trái kèm Avatar, Kính sương mờ) --%>
                            <c:otherwise>
                                <div class="chat-row-other" id="msg-${msg.id}">
                                    <div class="chat-avatar-ring flex-shrink-0 mt-1" aria-hidden="true">
                                        ${msg.authorInitial}
                                    </div>
                                    <div class="chat-bubble-other">
                                        <!-- Micro Action Bar khi rê chuột -->
                                        <div class="chat-hover-bar" role="toolbar" aria-label="Thao tác tin nhắn">
                                            <button type="button" 
                                                    class="chat-hover-btn" 
                                                    onclick="copyMessageText('${msg.id}')" 
                                                    title="Sao chép nội dung tin nhắn"
                                                    aria-label="Sao chép nội dung">
                                                <i class="bi bi-clipboard" aria-hidden="true"></i>
                                            </button>
                                            <button type="button" 
                                                    class="chat-hover-btn" 
                                                    onclick="quoteMessage('${msg.authorName}', '${msg.id}')" 
                                                    title="Trích dẫn tin nhắn"
                                                    aria-label="Trích dẫn tin nhắn">
                                                <i class="bi bi-reply-fill" aria-hidden="true"></i>
                                            </button>
                                            <c:if test="${sessionScope.currentUser.role == 'ADMIN' || project.ownerId == sessionScope.currentUser.id}">
                                                <button type="button" 
                                                        class="chat-hover-btn" 
                                                        onclick="openEditModal('${msg.id}')" 
                                                        title="Chỉnh sửa tin nhắn"
                                                        aria-label="Chỉnh sửa tin nhắn">
                                                    <i class="bi bi-pencil" aria-hidden="true"></i>
                                                </button>
                                                <button type="button" 
                                                        class="chat-hover-btn chat-hover-btn-danger" 
                                                        onclick="confirmDeleteMessage('${project.id}', '${msg.id}')" 
                                                        title="Xóa tin nhắn" 
                                                        aria-label="Xóa tin nhắn">
                                                    <i class="bi bi-trash3" aria-hidden="true"></i>
                                                </button>
                                            </c:if>
                                        </div>

                                        <div class="d-flex align-items-center justify-content-between gap-3 mb-1">
                                            <span class="chat-author-name">${msg.authorName}</span>
                                        </div>
                                        <div class="message-body fs-8 lh-base" 
                                             id="msg-content-${msg.id}"
                                             data-raw-content="<c:out value='${msg.content}' />"><c:out value="${msg.content}" /></div>
                                        <div class="d-flex justify-content-end align-items-center gap-1 mt-1">
                                            <span class="chat-time-meta text-muted"><i class="bi bi-clock me-1" aria-hidden="true"></i>${msg.sentAt}</span>
                                        </div>
                                    </div>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>

                    <!-- Hiển thị khi chưa có tin nhắn nào (Empty State) -->
                    <c:if test="${empty messageList}">
                        <div class="chat-empty-state" id="emptyChatPlaceholder">
                            <div class="chat-empty-icon" aria-hidden="true">
                                <i class="bi bi-chat-square-quote"></i>
                            </div>
                            <h3 class="fw-bold mb-1 fs-6" style="color: var(--chat-text-primary);">Chưa có cuộc thảo luận nào</h3>
                            <p class="fs-8 text-muted mb-0" style="max-width: 320px;">Hãy bắt đầu trao đổi đầu tiên cùng các thành viên trong nhóm dự án!</p>
                        </div>
                    </c:if>

                </div>

                <!-- 3. Chân khung Chat: FLOATING COMMAND ISLAND -->
                <div class="chat-floating-island-wrapper">
                    <div class="chat-floating-island">
                        
                        <!-- Thanh phím tắt & Action Bar trên cùng của Island -->
                        <div class="chat-quick-toolbar">
                            <div class="d-flex align-items-center gap-1">
                                <button type="button" 
                                        class="chat-quick-chip" 
                                        onclick="insertShortcut('#doc-')" 
                                        title="Chèn tiền tố bài viết Wiki">
                                    <i class="bi bi-journal-text" style="color: var(--chat-accent);" aria-hidden="true"></i>
                                    <span>#doc-</span>
                                </button>
                                <button type="button" 
                                        class="chat-quick-chip" 
                                        onclick="insertShortcut('#task-')" 
                                        title="Chèn tiền tố công việc Kanban">
                                    <i class="bi bi-check2-circle text-success" aria-hidden="true"></i>
                                    <span>#task-</span>
                                </button>
                                <button type="button" 
                                        class="chat-quick-chip" 
                                        onclick="insertShortcut('@')" 
                                        title="Nhắc tên thành viên">
                                    <i class="bi bi-at" style="color: #f59e0b;" aria-hidden="true"></i>
                                    <span>@nhắc tên</span>
                                </button>
                            </div>
                            <div>
                                <button type="button" 
                                        class="chat-quick-chip" 
                                        id="btnToggleEmoji"
                                        onclick="toggleEmojiDrawer()" 
                                        title="Mở bảng Emoji nhanh">
                                    <span>😊</span>
                                    <span>Emoji</span>
                                </button>
                            </div>
                        </div>

                        <!-- Ngăn kéo chọn Emoji nhanh -->
                        <div class="chat-emoji-drawer" id="emojiDrawer" role="region" aria-label="Bảng chọn emoji nhanh">
                            <button type="button" class="chat-emoji-btn" onclick="insertEmoji('👍')" title="Thích">👍</button>
                            <button type="button" class="chat-emoji-btn" onclick="insertEmoji('🚀')" title="Tên lửa">🚀</button>
                            <button type="button" class="chat-emoji-btn" onclick="insertEmoji('❤️')" title="Trái tim">❤️</button>
                            <button type="button" class="chat-emoji-btn" onclick="insertEmoji('🔥')" title="Tuyệt vời">🔥</button>
                            <button type="button" class="chat-emoji-btn" onclick="insertEmoji('🎉')" title="Chúc mừng">🎉</button>
                            <button type="button" class="chat-emoji-btn" onclick="insertEmoji('👀')" title="Đang xem">👀</button>
                            <button type="button" class="chat-emoji-btn" onclick="insertEmoji('✅')" title="Hoàn tất">✅</button>
                            <button type="button" class="chat-emoji-btn" onclick="insertEmoji('💡')" title="Ý tưởng">💡</button>
                        </div>

                        <!-- Form nhập tin nhắn -->
                        <form method="post" action="${pageContext.request.contextPath}/chat" id="chatForm" class="d-flex align-items-center gap-2">
                            <input type="hidden" name="action" value="sendProjectMessage">
                            <input type="hidden" name="projectId" value="${project.id}">

                            <label for="chatInput" class="visually-hidden">Nội dung tin nhắn</label>
                            <input type="text" 
                                   class="form-control chat-input-field" 
                                   id="chatInput" 
                                   name="content" 
                                   placeholder="Nhập tin nhắn... (Gõ #doc-1, #task-1 hoặc @tên)" 
                                   aria-label="Nhập tin nhắn thảo luận"
                                   autocomplete="off" 
                                   required>

                            <button type="submit" class="chat-btn-send shadow-none flex-shrink-0" id="btnSend" aria-label="Gửi tin nhắn">
                                <span>Gửi</span>
                                <i class="bi bi-send-fill" aria-hidden="true"></i>
                            </button>
                        </form>
                    </div>
                </div>

            </div>
        </main>

    </div>
</div>

<!-- ========================================================
     OFFCANVAS CHO MOBILE: XEM THÀNH VIÊN VÀ TRA CỨU ID (GLASS)
     ======================================================== -->
<div class="offcanvas offcanvas-start chat-offcanvas rounded-end-4" tabindex="-1" id="chatSidebarOffcanvas" aria-labelledby="chatSidebarOffcanvasLabel">
    <div class="offcanvas-header py-3">
        <h5 class="offcanvas-title fw-bold fs-6 d-flex align-items-center gap-2" id="chatSidebarOffcanvasLabel">
            <i class="bi bi-layers-half" style="color: var(--chat-accent);" aria-hidden="true"></i>
            Thông tin nhóm & Tài nguyên
        </h5>
        <button type="button" class="btn-close shadow-none" data-bs-dismiss="offcanvas" aria-label="Đóng bảng thông tin"></button>
    </div>
    <div class="offcanvas-body p-3 d-flex flex-column gap-3">
        
        <!-- Mobile Card 1: Thành viên nhóm -->
        <div class="chat-sidebar-card p-3">
            <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom" style="border-color: var(--chat-glass-border-subtle) !important;">
                <span class="chat-sidebar-title mb-0 d-flex align-items-center gap-2">
                    <i class="bi bi-people-fill" style="color: var(--chat-accent);" aria-hidden="true"></i>
                    Team Radar (${userList.size()})
                </span>
            </div>
            <div class="d-flex flex-column gap-2 overflow-y-auto" style="max-height: 35vh;">
                <c:choose>
                    <c:when test="${not empty userList}">
                        <c:forEach items="${userList}" var="u">
                            <div class="chat-member-item d-flex align-items-center justify-content-between">
                                <div class="d-flex align-items-center gap-2">
                                    <div class="chat-avatar-ring" aria-hidden="true">
                                        ${u.fullName.substring(0, 1).toUpperCase()}
                                        <span class="chat-avatar-status"></span>
                                    </div>
                                    <div>
                                        <span class="chat-member-name d-block text-truncate" style="max-width: 140px;">${u.fullName}</span>
                                        <span class="chat-member-role">${u.role}</span>
                                    </div>
                                </div>
                                <button type="button" 
                                        class="btn btn-sm p-1 text-muted border-0 chat-hover-btn" 
                                        data-bs-dismiss="offcanvas"
                                        onclick="insertShortcut('@${u.fullName}')" 
                                        title="Nhắc tên @${u.fullName}">
                                    <i class="bi bi-at fs-7"></i>
                                </button>
                            </div>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <div class="text-center py-3 text-muted fs-8">Chưa có thành viên nào</div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Mobile Card 2: Tra cứu ID -->
        <div class="chat-sidebar-card p-3">
            <span class="chat-sidebar-title mb-2 d-block">
                <i class="bi bi-bookmark-star text-warning me-1" aria-hidden="true"></i> ID tra cứu nhanh
            </span>
            <div class="d-flex flex-column gap-2 overflow-y-auto" style="max-height: 40vh;">
                <span class="fs-9 fw-bold text-muted text-uppercase tracking-wider">📄 Tài liệu Wiki:</span>
                <c:forEach items="${docList}" var="d">
                    <button type="button" 
                            class="chat-resource-chip" 
                            data-bs-dismiss="offcanvas"
                            onclick="insertShortcut('#doc-${d.id}')"
                            aria-label="Chèn mã tài liệu ${d.title}">
                        <span class="chat-chip-tag chat-chip-tag-doc">#doc-${d.id}</span>
                        <span class="text-truncate">${d.title}</span>
                    </button>
                </c:forEach>

                <span class="fs-9 fw-bold text-muted text-uppercase tracking-wider mt-2">✅ Thẻ Công việc:</span>
                <c:forEach items="${taskList}" var="t">
                    <button type="button" 
                            class="chat-resource-chip" 
                            data-bs-dismiss="offcanvas"
                            onclick="insertShortcut('#task-${t.id}')"
                            aria-label="Chèn mã công việc ${t.title}">
                        <span class="chat-chip-tag chat-chip-tag-task">#task-${t.id}</span>
                        <span class="text-truncate">${t.title}</span>
                    </button>
                </c:forEach>
            </div>
        </div>

    </div>
</div>

<!-- ========================================================
     MODAL CHỈNH SỬA TIN NHẮN (GLASSMORPHISM DIALOG)
     ======================================================== -->
<div class="modal fade" id="editMessageModal" tabindex="-1" aria-labelledby="editMessageModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content chat-sidebar-card border-0 shadow-lg" style="background: var(--chat-island-bg) !important; backdrop-filter: blur(28px) !important; border: 1px solid var(--chat-glass-border) !important;">
            <div class="modal-header border-bottom py-3" style="border-color: var(--chat-glass-border-subtle) !important;">
                <h5 class="modal-title fs-6 fw-bold d-flex align-items-center gap-2" id="editMessageModalLabel" style="color: var(--chat-text-primary) !important;">
                    <i class="bi bi-pencil-square" style="color: var(--chat-accent);" aria-hidden="true"></i>
                    <span>Chỉnh sửa tin nhắn</span>
                </h5>
                <button type="button" class="btn-close shadow-none" data-bs-dismiss="modal" aria-label="Đóng"></button>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/chat" id="editMessageForm">
                <input type="hidden" name="action" value="editProjectMessage">
                <input type="hidden" name="projectId" value="${project.id}">
                <input type="hidden" name="messageId" id="editMessageId">
                
                <div class="modal-body p-3">
                    <label for="editMessageContent" class="form-label fs-8 fw-semibold" style="color: var(--chat-text-secondary);">Nội dung tin nhắn:</label>
                    <textarea class="form-control chat-input-field border rounded-3 p-2 fs-8" 
                              id="editMessageContent" 
                              name="content" 
                              rows="3" 
                              style="background: var(--chat-glass-surface-subtle) !important; border-color: var(--chat-glass-border) !important; color: var(--chat-text-primary) !important;"
                              required></textarea>
                </div>
                <div class="modal-footer border-top py-2 px-3 d-flex justify-content-end gap-2" style="border-color: var(--chat-glass-border-subtle) !important;">
                    <button type="button" class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-none" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="chat-btn-send shadow-none px-3 py-1 fs-8">
                        <i class="bi bi-check2" aria-hidden="true"></i>
                        <span>Lưu thay đổi</span>
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- ========================================================
     MODAL XÁC NHẬN XÓA TIN NHẮN (GLASSMORPHISM DIALOG)
     ======================================================== -->
<div class="modal fade" id="deleteMessageModal" tabindex="-1" aria-labelledby="deleteMessageModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-sm">
        <div class="modal-content chat-sidebar-card border-0 shadow-lg" style="background: var(--chat-island-bg) !important; backdrop-filter: blur(28px) !important; border: 1px solid var(--chat-glass-border) !important;">
            <div class="modal-header border-bottom py-3" style="border-color: var(--chat-glass-border-subtle) !important;">
                <h5 class="modal-title fs-6 fw-bold d-flex align-items-center gap-2 text-danger" id="deleteMessageModalLabel">
                    <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
                    <span>Xác nhận xóa</span>
                </h5>
                <button type="button" class="btn-close shadow-none" data-bs-dismiss="modal" aria-label="Đóng"></button>
            </div>
            <div class="modal-body p-3 text-center">
                <p class="fs-8 mb-0" style="color: var(--chat-text-primary);">Bạn có chắc chắn muốn xóa vĩnh viễn tin nhắn này không?</p>
            </div>
            <div class="modal-footer border-top py-2 px-3 d-flex justify-content-end gap-2" style="border-color: var(--chat-glass-border-subtle) !important;">
                <button type="button" class="btn btn-sm btn-outline-secondary rounded-pill px-3 shadow-none" data-bs-dismiss="modal">Hủy</button>
                <a id="btnConfirmDeleteMessage" href="#" class="btn btn-sm btn-danger rounded-pill px-3 shadow-none fs-8 d-inline-flex align-items-center gap-1">
                    <i class="bi bi-trash3" aria-hidden="true"></i>
                    <span>Xóa tin nhắn</span>
                </a>
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
<jsp:include page="/includes/footer.jsp" />