<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!-- Inject Wiki CSS vào Header trước khi include -->
<c:set var="extraCss" value="styles/wiki.css" scope="request" />

<!-- 1. NẠP HEADER & NAVBAR CHUNG -->
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container-fluid px-lg-5 py-4 wiki-shell">

    <!-- 2. THANH ĐIỀU HƯỚNG DỰ ÁN & CHUYỂN TAB (Kanban / Docs / Chat) -->
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4 wiki-subnav">
        
        <!-- Cụm bên trái: Quay lại + Tên dự án + Chuyển Tab -->
        <div class="d-flex flex-wrap align-items-center gap-3">
            <a href="${pageContext.request.contextPath}/project?action=list" 
               class="wiki-btn-cta-outline wiki-btn-cta-sm rounded-pill" 
               title="Quay về danh sách dự án">
                <i class="bi bi-arrow-left me-1"></i> Dashboard
            </a>
            
            <div class="border-start ps-3 d-flex align-items-center gap-3" style="border-color: var(--wiki-border) !important;">
                <div>
                    <h4 class="fw-extrabold mb-0 tracking-tight">${project.name}</h4>
                    <span class="wiki-subtitle">Không gian Tài liệu & Ghi chú Wiki</span>
                </div>

                <!-- 4 Nút chuyển phân hệ nhanh: Kanban / Docs / Chat / Báo cáo -->
                <div class="d-none d-md-flex wiki-tab-group ms-2">
                    <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}" 
                       class="wiki-tab">
                        <i class="bi bi-kanban me-1"></i> Kanban
                    </a>
                    <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}" 
                       class="wiki-tab active">
                        <i class="bi bi-journal-text me-1"></i> Tài liệu
                    </a>
                    <a href="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}" 
                       class="wiki-tab">
                        <i class="bi bi-chat-dots me-1"></i> Thảo luận
                    </a>
                    <a href="${pageContext.request.contextPath}/project?action=report&projectId=${project.id}" 
                       class="wiki-tab"
                       title="Xem báo cáo tổng hợp tiến độ và đánh giá">
                        <i class="bi bi-file-earmark-bar-graph me-1"></i> Báo cáo
                    </a>
                </div>
            </div>
        </div>

        <!-- Cụm bên phải: Nút viết bài mới -->
        <div>
            <button type="button" class="wiki-btn-cta"
                    data-bs-toggle="modal" data-bs-target="#createDocModal">
                <i class="bi bi-pencil-square"></i> Viết bài mới
            </button>
        </div>
    </div>

    <!-- UI-04: Floating Toast -->
    <jsp:include page="/includes/toast.jsp" />

    <!-- 3. BỐ CỤC 2 CỘT NOTION WIKI (DANH MỤC TRÁI 25% + NỘI DUNG PHẢI 75%) -->
    <div class="row g-4">

        <!-- ==========================================
             CỘT BÊN TRÁI (COL-12 COL-LG-4 COL-XL-3): DANH MỤC BÀI VIẾT
             ========================================== -->
        <div class="col-12 col-lg-4 col-xl-3">
            <div class="wiki-sidebar">
                
                <!-- Tiêu đề danh mục & Tổng số bài -->
                <div class="wiki-sidebar-header">
                    <div class="d-flex align-items-center gap-2">
                        <i class="bi bi-folder2-open" style="color: var(--wiki-accent); font-size: 1rem;"></i>
                        <h6>Danh mục tài liệu</h6>
                    </div>
                    <span class="wiki-count-badge">
                        ${docs.size()} bài
                    </span>
                </div>

                <!-- Ô tìm kiếm tài liệu nhanh (Instant search) -->
                <div class="wiki-search">
                    <i class="bi bi-search wiki-search-icon"></i>
                    <input type="text" 
                           id="docSearchInput" 
                           placeholder="Tìm tài liệu..." 
                           oninput="filterDocList(this.value)">
                </div>

                <!-- Danh sách các bài viết cuộn dọc -->
                <div class="wiki-doc-list" id="docListContainer" style="max-height: 68vh;">
                    
                    <c:forEach items="${docs}" var="d">
                        <a href="${pageContext.request.contextPath}/doc?action=view&projectId=${project.id}&docId=${d.id}" 
                           class="wiki-doc-item ${selectedDoc.id == d.id ? 'active' : ''}"
                           data-doc-title="${d.title.toLowerCase()}">
                            
                            <div class="d-flex align-items-start gap-2 mb-1">
                                <i class="bi bi-file-earmark-text wiki-doc-icon mt-0-5"></i>
                                <span class="wiki-doc-title text-truncate d-block flex-grow-1">${d.title}</span>
                            </div>

                            <!-- Đoạn trích dẫn tóm tắt -->
                            <p class="wiki-doc-snippet ms-3">${d.snippet}</p>

                            <!-- Tác giả & Ngày cập nhật -->
                            <div class="wiki-doc-meta ms-3">
                                <span class="text-truncate" style="max-width: 55%;"><i class="bi bi-person me-1"></i>${d.authorName}</span>
                                <span><i class="bi bi-clock me-1"></i>${d.updatedAt}</span>
                            </div>
                        </a>
                    </c:forEach>

                    <!-- Hiển thị khi chưa có bài viết nào -->
                    <c:if test="${empty docs}">
                        <div class="wiki-empty-sidebar">
                            <i class="bi bi-journal-x"></i>
                            <p>Dự án này chưa có tài liệu nào.</p>
                            <button type="button" class="wiki-btn-cta wiki-btn-cta-sm"
                                    data-bs-toggle="modal" data-bs-target="#createDocModal">
                                Viết bài đầu tiên
                            </button>
                        </div>
                    </c:if>

                </div>
            </div>
        </div>

        <!-- ==========================================
             CỘT BÊN PHẢI (COL-12 COL-LG-8 COL-XL-9): KHUNG ĐỌC & SOẠN THẢO BÀI VIẾT
             ========================================== -->
        <div class="col-12 col-lg-8 col-xl-9">
            <div class="wiki-content">
                
                <c:choose>
                    <c:when test="${not empty selectedDoc}">
                        
                        <!-- Đầu bài viết: Tiêu đề + Nút Sửa/Xóa -->
                        <div class="wiki-article-header">
                            <div>
                                <h3 class="wiki-article-title">${selectedDoc.title}</h3>
                                <div class="wiki-article-meta">
                                    <span class="wiki-meta-chip">
                                        <i class="bi bi-person-circle" style="color: var(--wiki-accent);"></i>
                                        <span class="wiki-meta-author">${selectedDoc.authorName}</span>
                                    </span>
                                    <span class="wiki-meta-divider">&bull;</span>
                                    <span class="wiki-meta-chip">
                                        <i class="bi bi-calendar3"></i>
                                        Tạo: ${selectedDoc.createdAt}
                                    </span>
                                    <span class="wiki-meta-divider">&bull;</span>
                                    <span class="wiki-meta-chip">
                                        <i class="bi bi-arrow-repeat"></i>
                                        Cập nhật: ${selectedDoc.updatedAt}
                                    </span>
                                </div>
                            </div>

                            <!-- Cụm nút hành động Sửa & Xóa -->
                            <div class="wiki-article-actions">
                                <button type="button" class="wiki-btn-edit"
                                        data-bs-toggle="modal" data-bs-target="#editDocModal">
                                    <i class="bi bi-pencil"></i> Sửa bài
                                </button>
                                
                                <a href="${pageContext.request.contextPath}/doc?action=delete&docId=${selectedDoc.id}&projectId=${project.id}" 
                                   class="wiki-btn-delete"
                                   onclick="return confirm('Bạn có chắc chắn muốn xóa bài viết tài liệu này không?');">
                                    <i class="bi bi-trash3"></i> Xóa
                                </a>
                            </div>
                        </div>

                        <!-- Thân nội dung bài viết -->
                        <div class="wiki-body mb-5">
                            <c:out value="${selectedDoc.content}" />
                        </div>

                        <!-- Khung: Các công việc đang tham chiếu tài liệu này (TaskDoc) -->
                        <div class="wiki-related-section">
                            <div class="wiki-related-header">
                                <div class="d-flex align-items-center gap-2">
                                    <span class="wiki-related-icon">
                                        <i class="bi bi-pin-angle-fill"></i>
                                    </span>
                                    <h6>Các công việc đang áp dụng tài liệu này</h6>
                                </div>
                                <span class="wiki-count-badge">
                                    ${relatedTasks.size()} công việc
                                </span>
                            </div>

                            <c:if test="${not empty relatedTasks}">
                                <div class="d-flex flex-column gap-2">
                                    <c:forEach items="${relatedTasks}" var="rt">
                                        <div class="wiki-task-item">
                                            <div class="d-flex align-items-center gap-2 text-truncate">
                                                <span class="badge ${rt.priorityBadgeClass} rounded-pill px-2 py-0-5 fs-9">
                                                    ● ${rt.priorityLabel}
                                                </span>
                                                <span class="badge ${rt.statusBadgeClass} rounded-pill px-2 py-0-5 fs-9">
                                                    ${rt.statusLabel}
                                                </span>
                                                <span class="wiki-task-title text-truncate">${rt.title}</span>
                                            </div>
                                            <div class="d-flex align-items-center gap-3 flex-shrink-0">
                                                <span class="wiki-task-meta"><i class="bi bi-person-fill me-1" style="color: var(--wiki-accent);"></i>${rt.assigneeName}</span>
                                                <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}" 
                                                   class="wiki-task-link">
                                                    Xem trên Kanban <i class="bi bi-arrow-right ms-1"></i>
                                                </a>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:if>

                            <c:if test="${empty relatedTasks}">
                                <div class="wiki-no-tasks">
                                    Chưa có công việc nào gắn kèm tài liệu hướng dẫn này. Bạn có thể đính kèm khi tạo việc trên <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}">Bảng Kanban</a>.
                                </div>
                            </c:if>
                        </div>

                    </c:when>
                    <c:otherwise>
                        <div class="wiki-empty-state">
                            <div class="wiki-empty-icon">
                                <i class="bi bi-journal-richtext"></i>
                            </div>
                            <h5>Chọn một bài viết để đọc</h5>
                            <p>Hoặc tạo một tài liệu mới để chia sẻ ghi chú và kiến thức kỹ thuật cho nhóm.</p>
                            <button type="button" class="wiki-btn-cta"
                                    data-bs-toggle="modal" data-bs-target="#createDocModal">
                                <i class="bi bi-pencil-square"></i> Viết tài liệu mới
                            </button>
                        </div>
                    </c:otherwise>

                </c:choose>

            </div>
        </div>

    </div>
</div>

<!-- ==========================================
     4. MODAL 1: FORM "+ VIẾT TÀI LIỆU MỚI" (UC09)
     ========================================== -->
<div class="modal fade wiki-modal" id="createDocModal" tabindex="-1" aria-labelledby="createDocModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content">
            
            <div class="modal-header wiki-modal-header-create border-0">
                <h5 class="modal-title fw-bold text-white" id="createDocModalLabel">
                    <i class="bi bi-journal-plus me-2"></i> Soạn thảo tài liệu mới
                </h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Đóng"></button>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/doc">
                <input type="hidden" name="action" value="create">
                <input type="hidden" name="projectId" value="${project.id}">

                <div class="modal-body">
                    
                    <!-- Tiêu đề bài viết -->
                    <div class="mb-3">
                        <label for="createDocTitle" class="wiki-form-label">
                            Tiêu đề tài liệu <span class="text-danger">*</span>
                        </label>
                        <input type="text" 
                               class="form-control wiki-form-control py-2 px-3" 
                               id="createDocTitle" 
                               name="title" 
                               placeholder="Ví dụ: Quy chuẩn đặt tên bảng trong Cơ sở dữ liệu..." 
                               required>
                    </div>

                    <!-- Nội dung bài viết -->
                    <div class="mb-2">
                        <label for="createDocContent" class="wiki-form-label">Nội dung chi tiết</label>
                        <textarea class="form-control wiki-form-control p-3 font-monospace" 
                                  id="createDocContent" 
                                  name="content" 
                                  rows="12" 
                                  placeholder="Nhập nội dung tài liệu, ghi chú cuộc họp hoặc hướng dẫn kỹ thuật..."></textarea>
                    </div>

                </div>

                <div class="modal-footer border-0">
                    <button type="button" class="wiki-btn-cancel" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="wiki-btn-submit">
                        <i class="bi bi-send-check me-1"></i> Xuất bản tài liệu
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- ==========================================
     5. MODAL 2: FORM "CHỈNH SỬA TÀI LIỆU" (UC09)
     ========================================== -->
<c:if test="${not empty selectedDoc}">
    <div class="modal fade wiki-modal" id="editDocModal" tabindex="-1" aria-labelledby="editDocModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content">
                
                <div class="modal-header wiki-modal-header-edit border-0">
                    <h5 class="modal-title fw-bold" id="editDocModalLabel">
                        <i class="bi bi-pencil me-2"></i> Chỉnh sửa tài liệu
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>

                <form method="post" action="${pageContext.request.contextPath}/doc">
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" name="projectId" value="${project.id}">
                    <input type="hidden" name="docId" value="${selectedDoc.id}">

                    <div class="modal-body">
                        
                        <!-- Tiêu đề bài viết -->
                        <div class="mb-3">
                            <label for="editDocTitle" class="wiki-form-label">
                                Tiêu đề tài liệu <span class="text-danger">*</span>
                            </label>
                            <input type="text" 
                                   class="form-control wiki-form-control py-2 px-3" 
                                   id="editDocTitle" 
                                   name="title" 
                                   value="${selectedDoc.title}" 
                                   required>
                        </div>

                        <!-- Nội dung bài viết -->
                        <div class="mb-2">
                            <label for="editDocContent" class="wiki-form-label">Nội dung chi tiết</label>
                            <textarea class="form-control wiki-form-control p-3 font-monospace" 
                                      id="editDocContent" 
                                      name="content" 
                                      rows="12"><c:out value="${selectedDoc.content}" /></textarea>
                        </div>

                    </div>

                    <div class="modal-footer border-0">
                        <button type="button" class="wiki-btn-cancel" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="wiki-btn-submit wiki-btn-submit-dark">
                            <i class="bi bi-check2-circle me-1"></i> Lưu thay đổi
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<script>
function filterDocList(query) {
    var q = (query || '').trim().toLowerCase();
    var items = document.querySelectorAll('#docListContainer .wiki-doc-item');
    items.forEach(function(item) {
        var title = item.getAttribute('data-doc-title') || '';
        if (q === '' || title.indexOf(q) !== -1) {
            item.style.display = 'block';
        } else {
            item.style.display = 'none';
        }
    });
}
</script>

<!-- 6. NẠP FOOTER CHUNG -->
<jsp:include page="/includes/footer.jsp" />
