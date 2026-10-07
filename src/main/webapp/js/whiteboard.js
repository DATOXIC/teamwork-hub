/*
 * whiteboard.js — Khởi tạo bảng vẽ Excalidraw + tự động lưu + đồng bộ với đồng đội (whiteboard.jsp).
 * Cấu hình từ server lấy từ đối tượng toàn cục khai báo ngay trước thẻ <script src> trong whiteboard.jsp.
 *
 * Đồng bộ nhiều người: mỗi nét vẽ (element) có `id` và `version`. Trước khi lưu, trình duyệt tải bản mới nhất
 * trên server (GET ...&format=json) rồi GỘP theo từng nét (nét nào có version cao hơn thì thắng) thay vì ghi đè
 * cả bảng. Cứ vài giây lại tự gộp nét mới của đồng đội vào bảng đang vẽ. Nét bị xóa được giữ dạng
 * "dấu xóa" (isDeleted) một thời gian để việc xóa cũng lan sang người khác.
 */
(function () {
// Thư viện vẽ tải từ CDN (unpkg): mất mạng / CDN lỗi thì báo rõ thay vì để khung trắng
if (!window.React || !window.ReactDOM || !window.ExcalidrawLib) {
    var root = document.getElementById('wbRoot');
    if (root) {
        root.innerHTML = '<div class="empty-state h-100" role="alert">'
            + '<i class="bi bi-wifi-off empty-state-icon"></i>'
            + '<div class="empty-state-title">Không tải được bảng vẽ</div>'
            + '<p class="empty-state-hint">Thư viện vẽ (Excalidraw) chưa tải được — kiểm tra kết nối mạng rồi tải lại trang. Nét vẽ đã lưu không bị mất.</p>'
            + '<button type="button" class="btn btn-sm btn-primary-custom rounded-pill px-3 mt-3" onclick="location.reload()">Tải lại trang</button>'
            + '</div>';
    }
    return;
}
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

var PULL_INTERVAL_MS = 5000;               // Chu kỳ gộp nét vẽ mới của đồng đội
var TOMBSTONE_TTL_MS = 7 * 24 * 3600 * 1000; // Giữ "dấu xóa" 7 ngày rồi dọn

var api = null;       // excalidrawAPI (có sau khi Excalidraw khởi tạo xong)
var timer = null;
var busy = false;     // đang lưu hoặc đang gộp, tránh chạy chồng

function setStatus(html) { statusEl.innerHTML = html; }

// Chữ ký của bảng vẽ: id + version (+ trạng thái xóa) của mọi nét. Đổi khi có nét thêm / sửa / xóa.
function signature(elements) {
    return (elements || []).map(function (el) {
        return el.id + ':' + (el.version || 0) + (el.isDeleted ? 'd' : '');
    }).join(',');
}

var lastSig = signature(initialData.elements);

// Gộp nét vẽ từ server vào nét vẽ cục bộ: nét chưa có thì thêm, nét có version cao hơn thì thay.
function mergeElements(local, remote) {
    var index = {};
    var out = local.slice();
    out.forEach(function (el, i) { index[el.id] = i; });
    var changed = false;
    (remote || []).forEach(function (r) {
        var i = index[r.id];
        if (i === undefined) {
            index[r.id] = out.length;
            out.push(r);
            changed = true;
        } else if ((r.version || 0) > (out[i].version || 0)) {
            out[i] = r;
            changed = true;
        }
    });
    return { elements: out, changed: changed };
}

function fetchRemote() {
    var url = saveUrl + (saveUrl.indexOf('?') === -1 ? '?' : '&') + 'format=json&_=' + Date.now();
    return fetch(url, { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
        .then(function (r) { if (!r.ok) throw new Error(r.status); return r.json(); });
}

// Đưa nét / ảnh của đồng đội lên bảng đang mở. Trả về mảng nét sau khi gộp.
function applyRemote(remote) {
    var localAll = api.getSceneElementsIncludingDeleted();
    var merged = mergeElements(localAll, remote.elements);

    var remoteFiles = remote.files || {};
    var haveFiles = api.getFiles() || {};
    var missing = Object.keys(remoteFiles).filter(function (k) { return !haveFiles[k]; })
        .map(function (k) { return remoteFiles[k]; });
    if (missing.length) api.addFiles(missing);

    if (merged.changed) {
        api.updateScene({ elements: merged.elements });
    }
    return merged.elements;
}

function userIsDrawing() {
    var s = api.getAppState();
    return !!(s.draggingElement || s.editingElement || s.newElement || s.resizingElement || s.multiElement || s.selectedElementsAreBeingDragged);
}

// Định kỳ: lấy bản mới nhất từ server và gộp vào bảng (không làm gián đoạn khi đang vẽ).
function pull() {
    if (!api || busy || document.hidden || userIsDrawing()) return;
    busy = true;
    fetchRemote().then(function (remote) {
        if (!remote || !remote.elements) return;
        var before = signature(api.getSceneElementsIncludingDeleted());
        var elements = applyRemote(remote);
        var after = signature(elements);
        if (after !== before) {
            lastSig = after; // thay đổi này đến từ server, không cần lưu lại
            setStatus('<i class="bi bi-people me-1 text-primary"></i> Đã cập nhật từ đồng đội');
        }
    }).catch(function () { /* bỏ qua, lần sau thử lại */ })
      .then(function () { busy = false; });
}

function save() {
    if (!api) return;
    if (busy) { timer = setTimeout(save, 500); return; }
    busy = true;

    // 1) Tải bản mới nhất trên server và gộp với nét vẽ của mình (để không ghi đè đồng đội)
    fetchRemote().catch(function () { return null; }).then(function (remote) {
        var elements = (remote && remote.elements) ? applyRemote(remote) : api.getSceneElementsIncludingDeleted();

        // 2) Dọn "dấu xóa" quá cũ cho gọn dữ liệu
        var now = Date.now();
        var toSave = elements.filter(function (el) {
            return !el.isDeleted || (now - (el.updated || now)) < TOMBSTONE_TTL_MS;
        });

        var usedIds = {};
        toSave.forEach(function (el) { if (!el.isDeleted && el.fileId) usedIds[el.fileId] = true; });
        var allFiles = Object.assign({}, (remote && remote.files) || {}, api.getFiles() || {});
        var keptFiles = {};
        Object.keys(allFiles).forEach(function (k) { if (usedIds[k]) keptFiles[k] = allFiles[k]; });

        // ▶ SERVLET: POST WB_CONFIG.saveUrl (= /whiteboard?projectId=…, khai báo trong whiteboard.jsp) → WhiteboardServlet.doPost()
        //   body là JSON vẽ đã gộp; Servlet lưu DB rồi trả JSON. Lần mở trang đầu thì dữ liệu đi theo ${whiteboardJson} (doGet → forward).
        var sig = signature(toSave);
        return fetch(saveUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify({
                elements: toSave,
                appState: { viewBackgroundColor: api.getAppState().viewBackgroundColor },
                files: keptFiles
            })
        }).then(function (r) {
            if (!r.ok) throw new Error(r.status);
            lastSig = sig;
            setStatus('<i class="bi bi-cloud-check me-1 text-success"></i> Đã lưu');
        });
    }).catch(function () {
        setStatus('<i class="bi bi-exclamation-triangle me-1 text-danger"></i> Lưu thất bại, sẽ thử lại khi bạn vẽ tiếp');
    }).then(function () { busy = false; });
}

function scheduleSave(elements) {
    if (signature(elements) === lastSig) return;
    setStatus('<i class="bi bi-arrow-repeat me-1"></i> Đang lưu...');
    clearTimeout(timer);
    timer = setTimeout(save, 1500);
}

ReactDOM.createRoot(document.getElementById('wbRoot')).render(
    React.createElement(ExcalidrawLib.Excalidraw, {
        initialData: initialData,
        langCode: 'vi-VN',
        theme: currentTheme,
        excalidrawAPI: function (instance) { api = instance; },
        onChange: scheduleSave
    })
);

setInterval(pull, PULL_INTERVAL_MS);
document.addEventListener('visibilitychange', function () { if (!document.hidden) pull(); });
})();
