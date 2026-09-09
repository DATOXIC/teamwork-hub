/**
 * File Javascript xử lý cho Giao diện Chat Thảo luận (chat.jsp):
 * 1. Tự động cuộn dòng thời gian tin nhắn xuống vị trí mới nhất khi tải trang (scrollToBottom).
 * 2. Bộ máy phân tích và chuyển đổi cú pháp #doc-X, #task-X, @username thành các liên kết HTML bấm được (renderMentions).
 * 3. Chống double-submit (spam click) và hiển thị spinner trạng thái đang gửi.
 * 4. Tự động lưu nháp tin nhắn vào LocalStorage (Local Draft) phòng ngừa rớt mạng/reload trang.
 * 5. Hỗ trợ phím tắt và cải thiện khả năng điều hướng bàn phím.
 */

// =========================================================================
// HÀM 1: TỰ ĐỘNG CUỘN XUỐNG DÒNG TIN NHẮN CUỐI CÙNG
// =========================================================================
function scrollToBottom() {
    var chatContainer = document.getElementById("chatMessageContainer");
    if (chatContainer !== null) {
        chatContainer.scrollTop = chatContainer.scrollHeight;
    }
}

// =========================================================================
// HÀM 2: CHUYỂN ĐỔI CÚ PHÁP #MENTION THÀNH THẺ HTML LINK BẤM ĐƯỢC
// =========================================================================
function convertRawTextToMentionHtml(rawText, projectId, ctxPath) {
    if (rawText === null || rawText === undefined) {
        return "";
    }

    var resultHtml = rawText;

    // 1. Chuyển đổi cú pháp #doc-X thành Link mở bài viết Wiki
    var docRegex = /#doc-(\d+)/g;
    resultHtml = resultHtml.replace(docRegex, function(match, docId) {
        var docUrl = ctxPath + "/doc?action=view&projectId=" + projectId + "&docId=" + docId;
        return '<a href="' + docUrl + '" class="badge bg-primary-subtle text-primary text-decoration-none border border-primary-subtle rounded-pill px-2 py-1 fs-9 fw-semibold d-inline-flex align-items-center gap-1" title="Mở bài viết Wiki #' + docId + '" aria-label="Mở tài liệu wiki số ' + docId + '">'
             + '<i class="bi bi-journal-text" aria-hidden="true"></i>'
             + '<span>#doc-' + docId + '</span>'
             + '</a>';
    });

    // 2. Chuyển đổi cú pháp #task-X thành Link mở Bảng Kanban
    var taskRegex = /#task-(\d+)/g;
    resultHtml = resultHtml.replace(taskRegex, function(match, taskId) {
        var taskUrl = ctxPath + "/task?action=list&projectId=" + projectId;
        return '<a href="' + taskUrl + '" class="badge bg-success-subtle text-success text-decoration-none border border-success-subtle rounded-pill px-2 py-1 fs-9 fw-semibold d-inline-flex align-items-center gap-1" title="Mở thẻ Kanban Task #' + taskId + '" aria-label="Mở thẻ công việc số ' + taskId + '">'
             + '<i class="bi bi-check2-circle" aria-hidden="true"></i>'
             + '<span>#task-' + taskId + '</span>'
             + '</a>';
    });

    // 3. Chuyển đổi cú pháp @username thành thẻ Highlight nhắc tên thành viên
    var userRegex = /@([a-zA-Z0-9_\u00C0-\u024F\u1E00-\u1EFF]+)/g;
    resultHtml = resultHtml.replace(userRegex, function(match, username) {
        return '<span class="badge bg-info-subtle text-info-emphasis border border-info-subtle rounded-pill px-2 py-1 fs-9 fw-bold d-inline-flex align-items-center gap-1" aria-label="Được nhắc tên: ' + username + '">'
             + '<i class="bi bi-person-fill" aria-hidden="true"></i>'
             + '<span>@' + username + '</span>'
             + '</span>';
    });

    return resultHtml;
}

// =========================================================================
// HÀM 3: QUÉT VÀ RENDER TOÀN BỘ CÁC THẺ TIN NHẮN TRÊN TRANG
// =========================================================================
function renderAllMessages() {
    var messageElements = document.querySelectorAll(".message-body");
    for (var elem of messageElements) {
        var rawContent = elem.getAttribute("data-raw-content");
        if (rawContent === null || rawContent === "") {
            rawContent = elem.textContent;
        }
        var convertedHtml = convertRawTextToMentionHtml(rawContent, currentProjectId, contextPath);
        elem.innerHTML = convertedHtml;
    }
}

// =========================================================================
// HÀM 4: CHÈN PHÍM TẮT VÀO Ô CHAT & ĐỒNG BỘ LOCAL DRAFT
// =========================================================================
function insertShortcut(text) {
    var input = document.getElementById('chatInput');
    if (input) {
        var val = input.value;
        input.value = (val.length > 0 && !val.endsWith(' ') ? val + ' ' : val) + text;
        saveDraft(input.value);
        input.focus();
    }
}

// =========================================================================
// HÀM 5: QUẢN LÝ BẢN NHÁP LOCALSTORAGE (CHỐNG MẤT NỘI DUNG KHI LỖI MẠNG)
// =========================================================================
function getDraftKey() {
    return "teamwork_hub_chat_draft_" + (typeof currentProjectId !== 'undefined' ? currentProjectId : 'default');
}

function saveDraft(content) {
    try {
        if (content && content.trim().length > 0) {
            localStorage.setItem(getDraftKey(), content);
        } else {
            localStorage.removeItem(getDraftKey());
        }
    } catch (e) {
        console.warn("Không thể lưu bản nháp vào LocalStorage:", e);
    }
}

function restoreDraft() {
    var chatInput = document.getElementById("chatInput");
    if (!chatInput) return;

    try {
        var savedDraft = localStorage.getItem(getDraftKey());
        if (savedDraft && savedDraft.trim().length > 0) {
            chatInput.value = savedDraft;
        }
    } catch (e) {
        console.warn("Không thể khôi phục bản nháp từ LocalStorage:", e);
    }
}

function clearDraft() {
    try {
        localStorage.removeItem(getDraftKey());
    } catch (e) {
        console.warn("Không thể xóa bản nháp:", e);
    }
}

// =========================================================================
// HÀM 6: THIẾT LẬP BỘ LẮNG NGHE SỰ KIỆN FORM CHAT (ANTI-SPAM & LOADING)
// =========================================================================
function initChatForm() {
    var chatForm = document.getElementById("chatForm");
    var chatInput = document.getElementById("chatInput");
    var btnSend = document.getElementById("btnSend");

    if (!chatForm || !chatInput || !btnSend) return;

    // Lắng nghe gõ phím để lưu nháp liên tục
    chatInput.addEventListener("input", function() {
        saveDraft(chatInput.value);
    });

    // Lắng nghe phím bấm: Escape để blur, Enter để submit an toàn
    chatInput.addEventListener("keydown", function(e) {
        if (e.key === "Escape") {
            chatInput.blur();
        }
    });

    // Xử lý gửi tin nhắn: Chống spam và hiện trạng thái Loading
    chatForm.addEventListener("submit", function(e) {
        var content = chatInput.value.trim();
        if (!content) {
            e.preventDefault();
            chatInput.focus();
            return false;
        }

        // Kích hoạt trạng thái disabled & loading spinner
        btnSend.disabled = true;
        btnSend.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span><span>Đang gửi...</span>';

        // Xóa bản nháp khi submit
        clearDraft();
        return true;
    });
}

// =========================================================================
// KHỞI CHẠY KHI TOÀN BỘ DOM HTML ĐÃ TẢI XONG
// =========================================================================
document.addEventListener("DOMContentLoaded", function() {
    // 1. Quét và chuyển đổi các cú pháp Mention thành Link
    renderAllMessages();

    // 2. Khôi phục bản nháp nếu có
    restoreDraft();

    // 3. Khởi tạo xử lý Form & Anti-Spam
    initChatForm();

    // 4. Tự động cuộn xuống dòng tin nhắn mới nhất
    scrollToBottom();

    // 5. Tự động đưa con trỏ chuột vào ô nhập tin nhắn
    var chatInput = document.getElementById("chatInput");
    if (chatInput !== null) {
        chatInput.focus();
    }
});
