<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- whiteboard.jsp — Bảng vẽ cộng tác (Excalidraw nhúng). Controller: WhiteboardServlet (/whiteboard)
     Model: ${project}, ${whiteboardJson} (JSON đã escape '<'). Tự động lưu qua POST /whiteboard. --%>

<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container-fluid px-lg-5 py-4">
    <c:set var="activeSubNav" value="whiteboard" scope="request" />
    <jsp:include page="/includes/project_subnav.jsp" />

    <div class="d-flex justify-content-end mb-2">
        <span id="wbStatus" class="badge rounded-pill bg-light text-muted border px-3 py-1 fs-8"><i class="bi bi-cloud-check me-1 text-success"></i> Tự động lưu</span>
    </div>

    <div id="wbRoot" class="wb-frame"></div>
</div>

<style>
    .wb-frame { height: calc(100vh - 230px); min-height: 480px; border: 1px solid var(--bs-border-color, #e5e7eb); border-radius: 16px; overflow: hidden; background: var(--bs-card-bg, #fff); }
    [data-theme="dark"] .wb-frame, [data-bs-theme="dark"] .wb-frame { background: #0E1322 !important; border-color: rgba(255, 255, 255, 0.12) !important; }
</style>

<%-- ◀ SERVLET: WhiteboardServlet → setAttribute("whiteboardJson") --%>
<script type="application/json" id="wbData">${whiteboardJson}</script>

<script src="https://unpkg.com/react@18.2.0/umd/react.production.min.js"></script>
<script src="https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js"></script>
<script>window.EXCALIDRAW_ASSET_PATH = "https://unpkg.com/@excalidraw/excalidraw@0.17.6/dist/";</script>
<script src="https://unpkg.com/@excalidraw/excalidraw@0.17.6/dist/excalidraw.production.min.js"></script>
<script>
    <%-- ◀ SERVLET: WhiteboardServlet → setAttribute("project") --%>
    window.WB_CONFIG = { saveUrl: '${pageContext.request.contextPath}/whiteboard?projectId=${project.id}' };
</script>
<script src="${pageContext.request.contextPath}/js/whiteboard.js"></script>

<jsp:include page="/includes/footer.jsp" />
