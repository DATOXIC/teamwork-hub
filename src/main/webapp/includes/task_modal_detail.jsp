<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!-- ========================================================
     MODAL CHI TIẾT TASK 2 CỘT (TASK MINI-HUB - TRỤ CỘT 1)
     Biến mỗi Task thành một không gian làm việc tích hợp:
     - Cột Trái: Yêu cầu công việc & Tài liệu hướng dẫn đính kèm
     - Cột Phải: Luồng hội thoại / Bình luận trao đổi riêng của Task
     ======================================================== -->
<div class="modal fade" id="taskDetailModal-${task.id}" tabindex="-1" aria-labelledby="taskDetailModalLabel-${task.id}" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-xl">
        <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
            
            <!-- 1. Đầu Modal: Tiêu đề task & Mức độ ưu tiên -->
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

            <!-- 2. Thân Modal 2 Cột -->
            <div class="modal-body p-0">
                <div class="row g-0">
                    
                    <!-- ==========================================
                         CỘT TRÁI (COL-MD-7): THÔNG TIN & TÀI LIỆU HƯỚNG DẪN
                         ========================================== -->
                    <div class="col-12 col-md-7 p-4 border-end">
                        
                        <!-- Khung tóm tắt: Trạng thái, Người làm, Hạn chót -->
                        <div class="row g-3 p-3 bg-light rounded-3 border mb-4">
                            <div class="col-6 col-sm-4">
                                <span class="text-muted fs-9 d-block mb-1">Trạng thái</span>
                                <c:choose>
                                    <c:when test="${task.status == 'TODO'}">
                                        <span class="badge bg-secondary rounded-pill px-2 py-1 fs-9">Cần làm</span>
                                    </c:when>
                                    <c:when test="${task.status == 'IN_PROGRESS'}">
                                        <span class="badge bg-primary rounded-pill px-2 py-1 fs-9">Đang làm</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge bg-success rounded-pill px-2 py-1 fs-9">Đã xong</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                            <div class="col-6 col-sm-4">
                                <span class="text-muted fs-9 d-block mb-1">Người phụ trách</span>
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

                        <!-- Mô tả chi tiết yêu cầu -->
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

                        <!-- Danh sách Tài liệu hướng dẫn đính kèm (TaskDoc) -->
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

                    <!-- ==========================================
                         CỘT PHẢI (COL-MD-5): HỘI THOẠI & BÌNH LUẬN CỦA TASK
                         ========================================== -->
                    <div class="col-12 col-md-5 p-4 d-flex flex-column bg-light-subtle" style="min-height: 480px;">
                        
                        <!-- Tiêu đề cột hội thoại -->
                        <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                            <div class="d-flex align-items-center gap-2">
                                <i class="bi bi-chat-square-dots-fill text-primary"></i>
                                <h6 class="fw-bold mb-0 text-dark fs-7">Hội thoại của Task</h6>
                            </div>
                            <span class="badge bg-secondary rounded-pill px-2 py-1 fs-9">
                                ${not empty taskCommentsMap[task.id] ? taskCommentsMap[task.id].size() : 0}
                            </span>
                        </div>

                        <!-- Danh sách các bình luận (Cuộn dọc) -->
                        <div class="flex-grow-1 overflow-y-auto d-flex flex-column gap-2 mb-3 pe-1" style="max-height: 320px;">
                            <c:forEach items="${taskCommentsMap[task.id]}" var="comment">
                                <div class="p-2 px-3 bg-white rounded-3 border shadow-2xs">
                                    <div class="d-flex align-items-center justify-content-between mb-1">
                                        <span class="fw-bold text-dark fs-8">${comment.authorName}</span>
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
