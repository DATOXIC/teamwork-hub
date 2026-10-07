<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<%-- 
    =============================================================================
    UNIFIED PROJECT SUB-NAVBAR COMPONENT (/includes/project_subnav.jsp)
    Chuẩn hóa 7 phân hệ dự án: Kanban | Lộ trình | Bảng vẽ | Họp video | Tài liệu | Thảo luận | Báo cáo
    Hỗ trợ cuộn ngang mượt mà trên Mobile & Tablet, tự thích ứng giao diện Sáng / Tối.
    Tham số:
      - activeSubNav: 'kanban' | 'timeline' | 'whiteboard' | 'meeting' | 'docs' | 'chat' | 'report'
      - project: Project entity (${project})
    =============================================================================
--%>
<c:set var="currentNav" value="${not empty activeSubNav ? activeSubNav : (not empty param.activeSubNav ? param.activeSubNav : requestScope.activeSubNav)}" />

<div class="project-subnav-shell mb-4 p-2 p-md-3 rounded-4 shadow-sm">
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-3">
        
        <!-- Cụm bên trái: Nút quay lại + Mã dự án + Tên dự án -->
        <div class="d-flex align-items-center gap-2 gap-md-3 flex-wrap">
            <a href="${pageContext.request.contextPath}/project?action=list" 
               class="btn btn-sm rounded-pill px-3 py-1-5 subnav-btn-back d-inline-flex align-items-center gap-1 shadow-none" 
               title="Quay về danh sách dự án"
               aria-label="Quay về danh sách dự án">
                <i class="bi bi-arrow-left" aria-hidden="true"></i>
                <span class="d-none d-sm-inline fw-medium">Dự án</span>
            </a>
            
            <div class="border-start ps-2 ps-md-3 d-flex align-items-center gap-2 subnav-divider">
                <%-- ◀ SERVLET: ChatServlet, DocServlet, MeetingServlet … → setAttribute("project") --%>
                <c:if test="${not empty project.projectCode}">
                    <span class="badge rounded-pill px-2-5 py-1 fs-9 fw-semibold subnav-code-badge">
                        #${fn:escapeXml(project.projectCode)}
                    </span>
                </c:if>
                <div>
                    <h1 class="h6 fw-bold mb-0 text-truncate subnav-project-title" style="max-width: 260px;" title="${fn:escapeXml(project.name)}">
                        ${fn:escapeXml(project.name)}
                    </h1>
                </div>
            </div>
        </div>

        <!-- Cụm giữa: 7 Tab Phân Hệ Dự Án (Scrollable Capsule Navigation) -->
        <nav aria-label="Phân hệ dự án" class="subnav-tabs-wrapper subnav-tabs-scroll flex-grow-1 flex-md-grow-0">
            <div class="d-flex align-items-center gap-1 p-1 rounded-pill subnav-tabs-capsule">
                <!-- 1. Kanban -->
                <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}" 
                   class="subnav-tab-item ${currentNav == 'kanban' ? 'active' : ''}"
                   title="Bảng công việc Kanban">
                    <i class="bi bi-kanban me-1" aria-hidden="true"></i>
                    <span>Kanban</span>
                </a>

                <!-- 2. Lộ trình (Gantt) -->
                <a href="${pageContext.request.contextPath}/timeline?projectId=${project.id}" 
                   class="subnav-tab-item ${currentNav == 'timeline' ? 'active' : ''}"
                   title="Lộ trình & Sơ đồ Gantt">
                    <i class="bi bi-calendar-range me-1" aria-hidden="true"></i>
                    <span>Lộ trình</span>
                </a>

                <!-- 3. Bảng vẽ (Whiteboard) -->
                <a href="${pageContext.request.contextPath}/whiteboard?projectId=${project.id}" 
                   class="subnav-tab-item ${currentNav == 'whiteboard' ? 'active' : ''}"
                   title="Bảng vẽ ý tưởng & Sơ đồ nhóm">
                    <i class="bi bi-easel me-1" aria-hidden="true"></i>
                    <span>Bảng vẽ</span>
                </a>

                <!-- 4. Họp video (Meeting) -->
                <a href="${pageContext.request.contextPath}/meeting?projectId=${project.id}" 
                   class="subnav-tab-item ${currentNav == 'meeting' ? 'active' : ''}"
                   title="Phòng họp video trực tuyến">
                    <i class="bi bi-camera-video me-1" aria-hidden="true"></i>
                    <span>Họp video</span>
                </a>

                <!-- 5. Tài liệu (Docs) -->
                <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}" 
                   class="subnav-tab-item ${currentNav == 'docs' ? 'active' : ''}"
                   title="Không gian tài liệu & Wiki nhóm">
                    <i class="bi bi-journal-text me-1" aria-hidden="true"></i>
                    <span>Tài liệu</span>
                </a>

                <!-- 6. Thảo luận (Chat) -->
                <a href="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}" 
                   class="subnav-tab-item ${currentNav == 'chat' ? 'active' : ''}"
                   title="Kênh thảo luận & Trao đổi trực tiếp">
                    <i class="bi bi-chat-dots me-1" aria-hidden="true"></i>
                    <span>Thảo luận</span>
                </a>

                <!-- 7. Báo cáo (Report) -->
                <a href="${pageContext.request.contextPath}/project?action=report&projectId=${project.id}" 
                   class="subnav-tab-item ${currentNav == 'report' ? 'active' : ''}"
                   title="Báo cáo tiến độ & Đo lường năng suất">
                    <i class="bi bi-file-earmark-bar-graph me-1" aria-hidden="true"></i>
                    <span>Báo cáo</span>
                </a>
            </div>
        </nav>

    </div>
</div>
