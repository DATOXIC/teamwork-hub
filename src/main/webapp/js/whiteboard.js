/*
 * whiteboard.js — Khởi tạo bảng vẽ Excalidraw + tự động lưu (whiteboard.jsp).
 * Cấu hình từ server lấy từ đối tượng toàn cục khai báo ngay trước thẻ <script src> trong whiteboard.jsp.
 */
(function () {
var saveUrl = WB_CONFIG.saveUrl;
var statusEl = document.getElementById('wbStatus');
var initial = null;
try {
    var raw = document.getElementById('wbData').textContent.trim();
    if (raw) initial = JSON.parse(raw);
} catch (err) { initial = null; }

var currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
var defaultBg = (currentTheme === 'dark') ? '#0E1322' : '#ffffff';

var initialData = { elements: [], appState: { viewBackgroundColor: defaultBg, theme: currentTheme }, scrollToContent: true };
if (initial) {
    initialData.elements = initial.elements || [];
    initialData.files = initial.files || {};
    initialData.appState = Object.assign({ viewBackgroundColor: defaultBg, theme: currentTheme }, initial.appState || {});
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
        // ▶ SERVLET: POST WB_CONFIG.saveUrl (= /whiteboard?projectId=…, khai báo trong whiteboard.jsp) → WhiteboardServlet.doPost()
        //   body là JSON vẽ; Servlet lưu DB rồi trả JSON. Lần mở trang đầu thì dữ liệu đi theo ${whiteboardJson} (doGet → forward).
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
        theme: currentTheme,
        onChange: scheduleSave
    })
);
})();
