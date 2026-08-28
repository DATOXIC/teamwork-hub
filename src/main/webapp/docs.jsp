<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!-- 1. NẠP HEADER & NAVBAR CHUNG -->
<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container-fluid px-lg-5 py-4">

    <!-- 2. THANH ĐIỀU HƯỚNG DỰ ÁN & CHUYỂN TAB (Kanban / Docs / Chat) -->
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4 pb-3 border-bottom bg-white p-3 rounded-4 shadow-sm">
        
        <!-- Cụm bên trái: Quay lại + Tên dự án + Chuyển Tab -->
        <div class="d-flex flex-wrap align-items-center gap-3">
            <a href="${pageContext.request.contextPath}/project?action=list" 
               class="btn btn-outline-secondary btn-sm rounded-pill px-3 shadow-none" 
               title="Quay về danh sách dự án">
                <i class="bi bi-arrow-left me-1"></i> Dashboard
            </a>
            
            <div class="border-start ps-3 d-flex align-items-center gap-3">
                <div>
                    <h4 class="fw-extrabold text-dark mb-0 tracking-tight">${project.name}</h4>
                    <span class="fs-8 text-muted">Không gian Tài liệu & Ghi chú Wiki</span>
                </div>

                <!-- 3 Nút chuyển phân hệ nhanh: Kanban / Docs / Chat -->
                <div class="d-none d-md-flex align-items-center gap-2 bg-light p-1 rounded-pill border ms-2">
                    <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}" 
                       class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8">
                        <i class="bi bi-kanban me-1"></i> Kanban
                    </a>
                    <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}" 
                       class="btn btn-sm btn-white bg-white text-primary shadow-2xs rounded-pill px-3 py-1 fw-bold fs-8">
                        <i class="bi bi-journal-text me-1"></i> Tài liệu
                    </a>
                    <a href="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}" 
                       class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8">
                        <i class="bi bi-chat-dots me-1"></i> Thảo luận
                    </a>
                </div>
            </div>
        </div>

        <!-- Cụm bên phải: Nút viết bài mới -->
        <div>
            <button type="button" class="btn btn-primary-custom px-4 py-2 rounded-pill fw-semibold shadow-sm"
                    data-bs-toggle="modal" data-bs-target="#createDocModal">
                <i class="bi bi-pencil-square me-1"></i> Viết     <!-- 3. BỐ CỤC 2 CỘT NOTION WIKI (DANH MỤC TRÁI 25% + NỘI DUNG PHẢI 75%) -->
    <div class="row g-4">

        <!-- ==========================================
             CỘT BÊN TRÁI (COL-12 COL-LG-4 COL-XL-3): DANH MỤC BÀI VIẾT
             ========================================== -->
        <div class="col-12 col-lg-4 col-xl-3">
            <div class="card border-0 bg-white shadow-sm rounded-4 p-3 h-100">
                
                <!-- Tiêu đề danh mục & Tổng số bài -->
                <div class="d-flex align-items-center justify-content-between mb-2 px-1 pb-2 border-bottom">
                    <div class="d-flex align-items-center gap-2">
                        <i class="bi bi-folder2-open text-primary fs-6"></i>
                        <h6 class="fw-bold mb-0 text-dark fs-7">Danh mục tài liệu</h6>
                    </div>
                    <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5 fs-9 fw-semibold">
                        ${docs.size()} bài
                    </span>
                </div>

                <!-- Ô tìm kiếm tài liệu nhanh (Instant search) -->
                <div class="mb-2">
                    <div class="input-group input-group-sm">
                        <span class="input-group-text bg-light border-end-0 text-muted"><i class="bi bi-search"></i></span>
                        <input type="text" class="form-control bg-light border-start-0 fs-9 rounded-end-pill" 
                               id="docSearchInput" placeholder="Tìm tài liệu..." 
                               oninput="filterDocList(this.value)">
                    </div>
                </div>

                <!-- Danh sách các bài viết cuộn dọc -->
                <div class="doc-list d-flex flex-column gap-2 overflow-y-auto" id="docListContainer" style="max-height: 68vh;">
                    
                    <c:forEach items="${docs}" var="d">
                        <a href="${pageContext.request.contextPath}/doc?action=view&projectId=${project.id}&docId=${d.id}" 
                           class="wiki-doc-item ${selectedDoc.id == d.id ? 'active' : ''}"
                           data-doc-title="${d.title.toLowerCase()}">
                            
                            <div class="d-flex align-items-start gap-2 mb-1">
                                <i class="bi bi-file-earmark-text ${selectedDoc.id == d.id ? 'text-primary' : 'text-secondary'} mt-0-5 fs-7"></i>
                                <span class="fw-bold fs-8 wiki-doc-title ${selectedDoc.id == d.id ? 'text-primary' : 'text-dark'} text-truncate d-block flex-grow-1">${d.title}</span>
                            </div>

                            <!-- Đoạn trích dẫn tóm tắt -->
                            <p class="text-muted fs-9 mb-2 ms-3 text-truncate" style="max-width: 90%;">${d.snippet}</p>

                            <!-- Tác giả & Ngày cập nhật -->
                            <div class="d-flex align-items-center justify-content-between ms-3 fs-9 text-secondary border-top pt-1-5 mt-1">
                                <span class="text-truncate" style="max-width: 55%;"><i class="bi bi-person me-1"></i>${d.authorName}</span>
                                <span><i class="bi bi-clock me-1"></i>${d.updatedAt}</span>
                            </div>
                        </a>
                    </c:forEach>

                    <!-- Hiển thị khi chưa có bài viết nào -->
                    <c:if test="${empty docs}">
                        <div class="text-center text-muted py-5">
                            <i class="bi bi-journal-x fs-2 d-block mb-2 opacity-50"></i>
                            <p class="fs-8 mb-2">Dự án này chưa có tài liệu nào.</p>
                            <button type="button" class="btn btn-outline-primary btn-sm rounded-pill fs-8"
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
            <div class="card border-0 bg-white shadow-sm rounded-4 p-4 p-lg-5 h-100">
                
                <c:choose>
                    <c:when test="${not empty selectedDoc}">
                        
                        <!-- Đầu bài viết: Tiêu đề + Nút Sửa/Xóa -->
                        <div class="d-flex flex-wrap align-items-start justify-content-between gap-3 pb-3 mb-4 border-bottom">
                            <div>
                                <h3 class="fw-bold text-dark tracking-tight mb-2">${selectedDoc.title}</h3>
                                <div class="d-flex flex-wrap align-items-center gap-3 text-secondary fs-8">
                                    <div class="d-flex align-items-center gap-1">
                                        <i class="bi bi-person-circle text-primary"></i>
                                        <span class="fw-semibold text-dark">${selectedDoc.authorName}</span>
                                    </div>
                                    <span>&bull;</span>
                                    <div>
                                        <i class="bi bi-calendar3 me-1"></i> Tạo: ${selectedDoc.createdAt}
                                    </div>
                                    <span>&bull;</span>
                                    <div>
                                        <i class="bi bi-arrow-repeat me-1"></i> Cập nhật: ${selectedDoc.updatedAt}
                                    </div>
                                </div>
                            </div>

                            <!-- Cụm nút hành động Sửa & Xóa -->
                            <div class="d-flex align-items-center gap-2">
                                <button type="button" class="btn btn-outline-secondary btn-sm rounded-pill px-3 fs-8"
                                        data-bs-toggle="modal" data-bs-target="#editDocModal">
                                    <i class="bi bi-pencil me-1"></i> Sửa bài
                                </button>
                                
                                <a href="${pageContext.request.contextPath}/doc?action=delete&docId=${selectedDoc.id}&projectId=${project.id}" 
                                   class="btn btn-outline-danger btn-sm rounded-pill px-3 fs-8"
                                   onclick="return confirm('Bạn có chắc chắn muốn xóa bài viết tài liệu này không?');">
                                    <i class="bi bi-trash3 me-1"></i> Xóa
                                </a>
                            </div>
                        </div>

                        <!-- Thân nội dung bài viết -->
                        <div class="wiki-body mb-5">
                            <c:out value="${selectedDoc.content}" />
                        </div>

                        <!-- Khung: Các công việc đang tham chiếu tài liệu này (TaskDoc) -->
                        <div class="mt-auto pt-4 border-top">
                            <div class="d-flex align-items-center justify-content-between mb-3">
                                <div class="d-flex align-items-center gap-2">
                                    <span class="p-1 bg-primary-subtle text-primary rounded-2 lh-1">
                                        <i class="bi bi-pin-angle-fill fs-7"></i>
                                    </span>
                                    <h6 class="fw-bold mb-0 text-dark fs-7">Các công việc đang áp dụng tài liệu này</h6>
                                </div>
                                <span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5 fs-9 fw-semibold">
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
                                                <span class="fw-semibold text-dark fs-8 text-truncate">${rt.title}</span>
                                            </div>
                                            <div class="d-flex align-items-center gap-3 fs-8 text-secondary flex-shrink-0">
                                                <span><i class="bi bi-person-fill text-primary me-1"></i>${rt.assigneeName}</span>
                                                <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}" 
                                                   class="btn btn-outline-primary btn-xs rounded-pill px-3 py-1 fs-9">
                                                    Xem trên Kanban <i class="bi bi-arrow-right ms-1"></i>
                                                </a>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:if>

                            <c:if test="${empty relatedTasks}">
                                <div class="p-3 bg-light-subtle rounded-3 text-muted fs-8 border text-center">
                                    Chưa có công việc nào gắn kèm tài liệu hướng dẫn này. Bạn có thể đính kèm khi tạo việc trên <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}" class="text-primary fw-medium text-decoration-none">Bảng Kanban</a>.
                                </div>
                            </c:if>
                        </div>

                    </c:when>
                    <c:otherwise>
                        <div class="text-center text-muted py-5 my-auto">
                            <i class="bi bi-journal-richtext display-3 d-block mb-3 text-primary opacity-50"></i>
                            <h5 class="fw-bold text-dark">Chọn một bài viết để đọc</h5>
                            <p class="fs-8 text-secondary mb-4">Hoặc tạo một tài liệu mới để chia sẻ ghi chú và kiến thức kỹ thuật cho nhóm.</p>
                            <button type="button" class="btn btn-primary-custom px-4 py-2 rounded-pill fw-semibold fs-8 shadow-2xs text-white"
                                    data-bs-toggle="modal" data-bs-target="#createDocModal">
                                <i class="bi bi-pencil-square me-1"></i> Viết tài liệu mới
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
<div class="modal fade" id="createDocModal" tabindex="-1" aria-labelledby="createDocModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
            
            <div class="modal-header bg-primary text-white px-4 py-3 border-0">
                <h5 class="modal-title fw-bold" id="createDocModalLabel">
                    <i class="bi bi-journal-plus me-2"></i> Soạn thảo tài liệu mới
                </h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Đóng"></button>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/doc">
                <input type="hidden" name="action" value="create">
                <input type="hidden" name="projectId" value="${project.id}">

                <div class="modal-body px-4 py-4">
                    
                    <!-- Tiêu đề bài viết -->
                    <div class="mb-3">
                        <label for="createDocTitle" class="form-label fw-semibold text-dark fs-7">
                            Tiêu đề tài liệu <span class="text-danger">*</span>
                        </label>
                        <input type="text" 
                               class="form-control rounded-3 py-2 px-3 fs-7" 
                               id="createDocTitle" 
                               name="title" 
                               placeholder="Ví dụ: Quy chuẩn đặt tên bảng trong Cơ sở dữ liệu..." 
                               required>
                    </div>

                    <!-- Nội dung bài viết -->
                    <div class="mb-2">
                        <label for="createDocContent" class="form-label fw-semibold text-dark fs-7">Nội dung chi tiết</label>
                        <textarea class="form-control rounded-3 p-3 fs-7 font-monospace" 
                                  id="createDocContent" 
                                  name="content" 
                                  rows="12" 
                                  placeholder="Nhập nội dung tài liệu, ghi chú cuộc họp hoặc hướng dẫn kỹ thuật..."></textarea>
                    </div>

                </div>

                <div class="modal-footer px-4 py-3 bg-light border-0">
                    <button type="button" class="btn btn-light rounded-pill px-3 fs-7 fw-medium" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary-custom rounded-pill px-4 py-2 fs-7 fw-semibold shadow-sm">
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
    <div class="modal fade" id="editDocModal" tabindex="-1" aria-labelledby="editDocModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
                
                <div class="modal-header bg-dark text-white px-4 py-3 border-0">
                    <h5 class="modal-title fw-bold" id="editDocModalLabel">
                        <i class="bi bi-pencil me-2"></i> Chỉnh sửa tài liệu
                    </h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>

                <form method="post" action="${pageContext.request.contextPath}/doc">
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" name="projectId" value="${project.id}">
                    <input type="hidden" name="docId" value="${selectedDoc.id}">

                    <div class="modal-body px-4 py-4">
                        
                        <!-- Tiêu đề bài viết -->
                        <div class="mb-3">
                            <label for="editDocTitle" class="form-label fw-semibold text-dark fs-7">
                                Tiêu đề tài liệu <span class="text-danger">*</span>
                            </label>
                            <input type="text" 
                                   class="form-control rounded-3 py-2 px-3 fs-7" 
                                   id="editDocTitle" 
                                   name="title" 
                                   value="${selectedDoc.title}" 
                                   required>
                        </div>

                        <!-- Nội dung bài viết -->
                        <div class="mb-2">
                            <label for="editDocContent" class="form-label fw-semibold text-dark fs-7">Nội dung chi tiết</label>
                            <textarea class="form-control rounded-3 p-3 fs-7 font-monospace" 
                                      id="editDocContent" 
                                      name="content" 
                                      rows="12"><c:out value="${selectedDoc.content}" /></textarea>
                        </div>

                    </div>

                    <div class="modal-footer px-4 py-3 bg-light border-0">
                        <button type="button" class="btn btn-light rounded-pill px-3 fs-7 fw-medium" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-dark rounded-pill px-4 py-2 fs-7 fw-semibold shadow-sm">
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
