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
