<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- whiteboard.jsp — Bảng vẽ cộng tác (Excalidraw nhúng). Controller: WhiteboardServlet (/whiteboard)
     Model: ${project}, ${whiteboardJson} (JSON đã escape '<'). Tự động lưu qua POST /whiteboard. --%>

<jsp:include page="/includes/header.jsp" />
<jsp:include page="/includes/navbar.jsp" />

<div class="container-fluid px-lg-5 py-4">
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-3 bg-white p-3 rounded-4 shadow-sm">
        <div class="d-flex flex-wrap align-items-center gap-3">
            <a href="${pageContext.request.contextPath}/project?action=list"
               class="btn btn-outline-secondary btn-sm rounded-pill px-3 shadow-none">
                <i class="bi bi-arrow-left me-1"></i> Danh sách dự án
            </a>
            <div class="border-start ps-3">
                <h4 class="fw-extrabold text-dark mb-0 tracking-tight"><c:out value="${project.name}" /></h4>
                <span class="fs-8 text-muted">Bảng vẽ ý tưởng &amp; sơ đồ nhóm</span>
            </div>
            <div class="d-none d-md-flex align-items-center gap-2 bg-light p-1 rounded-pill border ms-2">
                <a href="${pageContext.request.contextPath}/task?action=list&projectId=${project.id}"
                   class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8"><i class="bi bi-kanban me-1"></i> Kanban</a>
                <a href="${pageContext.request.contextPath}/timeline?projectId=${project.id}"
                   class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8"><i class="bi bi-calendar-range me-1"></i> Lộ trình</a>
                <a href="${pageContext.request.contextPath}/whiteboard?projectId=${project.id}"
                   class="btn btn-sm btn-white bg-white text-primary shadow-2xs rounded-pill px-3 py-1 fw-bold fs-8"><i class="bi bi-easel me-1"></i> Bảng vẽ</a>
                <a href="${pageContext.request.contextPath}/meeting?projectId=${project.id}"
                   class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8"><i class="bi bi-camera-video me-1"></i> Họp video</a>
                <a href="${pageContext.request.contextPath}/doc?action=list&projectId=${project.id}"
                   class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8"><i class="bi bi-journal-text me-1"></i> Tài liệu</a>
                <a href="${pageContext.request.contextPath}/chat?action=view&projectId=${project.id}"
                   class="btn btn-sm text-secondary rounded-pill px-3 py-1 fw-medium fs-8"><i class="bi bi-chat-dots me-1"></i> Thảo luận</a>
            </div>
        </div>
        <span id="wbStatus" class="fs-8 text-muted"><i class="bi bi-cloud-check me-1"></i> Tự động lưu</span>
    </div>

    <div id="wbRoot" class="wb-frame"></div>
</div>

<style>
    .wb-frame { height: calc(100vh - 230px); min-height: 480px; border: 1px solid #e5e7eb; border-radius: 16px; overflow: hidden; background: #fff; }
</style>

<script type="application/json" id="wbData">${whiteboardJson}</script>

<script src="https://unpkg.com/react@18.2.0/umd/react.production.min.js"></script>
<script src="https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js"></script>
<script>window.EXCALIDRAW_ASSET_PATH = "https://unpkg.com/@excalidraw/excalidraw@0.17.6/dist/";</script>
<script src="https://unpkg.com/@excalidraw/excalidraw@0.17.6/dist/excalidraw.production.min.js"></script>
<script>
(function () {
    var saveUrl = '${pageContext.request.contextPath}/whiteboard?projectId=${project.id}';
    var statusEl = document.getElementById('wbStatus');
    var initial = null;
    try {
        var raw = document.getElementById('wbData').textContent.trim();
        if (raw) initial = JSON.parse(raw);
    } catch (err) { initial = null; }

    var initialData = { elements: [], appState: { viewBackgroundColor: '#ffffff' }, scrollToContent: true };
    if (initial) {
        initialData.elements = initial.elements || [];
        initialData.files = initial.files || {};
        initialData.appState = Object.assign({ viewBackgroundColor: '#ffffff' }, initial.appState || {});
    }

    function setStatus(html) { statusEl.innerHTML = html; }

    var timer = null;
    var lastSaved = JSON.stringify(initial ? (initial.elements || []) : []);

    function scheduleSave(elements, appState, files) {
        var live = elements.filter(function (el) { return !el.isDeleted; });
        var sig = JSON.stringify(live);
        if (sig === lastSaved) return;
        setStatus('<i class="bi bi-arrow-repeat me-1"></i> Đang lưu...');
        clearTimeout(timer);
        timer = setTimeout(function () {
            var usedIds = {};
            live.forEach(function (el) { if (el.fileId) usedIds[el.fileId] = true; });
            var keptFiles = {};
            Object.keys(files || {}).forEach(function (k) { if (usedIds[k]) keptFiles[k] = files[k]; });
            fetch(saveUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify({
                    elements: live,
                    appState: { viewBackgroundColor: appState.viewBackgroundColor },
                    files: keptFiles
                })
            }).then(function (r) {
                if (!r.ok) throw new Error(r.status);
                lastSaved = sig;
                setStatus('<i class="bi bi-cloud-check me-1 text-success"></i> Đã lưu');
            }).catch(function () {
                setStatus('<i class="bi bi-exclamation-triangle me-1 text-danger"></i> Lưu thất bại, sẽ thử lại khi bạn vẽ tiếp');
            });
        }, 1500);
    }

    ReactDOM.createRoot(document.getElementById('wbRoot')).render(
        React.createElement(ExcalidrawLib.Excalidraw, {
            initialData: initialData,
            langCode: 'vi-VN',
            onChange: scheduleSave
        })
    );
})();
</script>

<jsp:include page="/includes/footer.jsp" />
