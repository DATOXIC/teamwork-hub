/**
 * File Javascript xử lý cho Giao diện Chat Thảo luận (chat.jsp):
 * 1. Tự động cuộn dòng thời gian tin nhắn xuống vị trí mới nhất khi tải trang (scrollToBottom).
 * 2. Bộ máy phân tích và chuyển đổi cú pháp #doc-X, #task-X, @username thành các liên kết HTML bấm được (renderMentions).
 */

// =========================================================================
// HÀM 1: TỰ ĐỘNG CUỘN XUỐNG DÒNG TIN NHẮN CUỐI CÙNG
// =========================================================================
function scrollToBottom() {
    // 1. Tìm khung chứa danh sách tin nhắn
    var chatContainer = document.getElementById("chatMessageContainer");

    // 2. Nếu tìm thấy khung, thiết lập thanh cuộn xuống vị trí tối đa
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
    // Ví dụ: #doc-1 -> <a href="/doc?action=view&projectId=1&docId=1">...</a>
    var docRegex = /#doc-(\d+)/g;
    resultHtml = resultHtml.replace(docRegex, function(match, docId) {
        var docUrl = ctxPath + "/doc?action=view&projectId=" + projectId + "&docId=" + docId;
        return '<a href="' + docUrl + '" class="badge bg-primary-subtle text-primary text-decoration-none border border-primary-subtle rounded-pill px-2 py-1 fs-9 fw-semibold d-inline-flex align-items-center gap-1" title="Mở bài viết Wiki #' + docId + '">'
             + '<i class="bi bi-journal-text"></i>'
             + '<span>#doc-' + docId + '</span>'
             + '</a>';
    });

    // 2. Chuyển đổi cú pháp #task-X thành Link mở Bảng Kanban
    // Ví dụ: #task-3 -> <a href="/task?action=list&projectId=1">...</a>
    var taskRegex = /#task-(\d+)/g;
    resultHtml = resultHtml.replace(taskRegex, function(match, taskId) {
        var taskUrl = ctxPath + "/task?action=list&projectId=" + projectId;
        return '<a href="' + taskUrl + '" class="badge bg-success-subtle text-success text-decoration-none border border-success-subtle rounded-pill px-2 py-1 fs-9 fw-semibold d-inline-flex align-items-center gap-1" title="Mở thẻ Kanban Task #' + taskId + '">'
             + '<i class="bi bi-check2-circle"></i>'
             + '<span>#task-' + taskId + '</span>'
             + '</a>';
    });

    // 3. Chuyển đổi cú pháp @username thành thẻ Highlight nhắc tên thành viên
    // Ví dụ: @NguyenVanAn -> <span class="badge bg-info-subtle...">@NguyenVanAn</span>
    var userRegex = /@([a-zA-Z0-9_\u00C0-\u024F\u1E00-\u1EFF]+)/g;
    resultHtml = resultHtml.replace(userRegex, function(match, username) {
        return '<span class="badge bg-info-subtle text-info-emphasis border border-info-subtle rounded-pill px-2 py-1 fs-9 fw-bold d-inline-flex align-items-center gap-1">'
             + '<i class="bi bi-person-fill"></i>'
             + '<span>@' + username + '</span>'
             + '</span>';
    });

    return resultHtml;
}

// =========================================================================
// HÀM 3: QUÉT VÀ RENDER TOÀN BỘ CÁC THẺ TIN NHẮN TRÊN TRANG
// =========================================================================
function renderAllMessages() {
    // 1. Lấy tất cả các phần tử chứa nội dung tin nhắn
    var messageElements = document.querySelectorAll(".message-body");

    // 2. Duyệt qua từng phần tử bằng vòng lặp for...of tường minh
    for (var elem of messageElements) {
        // Lấy nội dung văn bản thô từ thuộc tính data-raw-content
        var rawContent = elem.getAttribute("data-raw-content");

        if (rawContent === null || rawContent === "") {
            rawContent = elem.textContent;
        }

        // Chuyển đổi sang HTML có liên kết
        var convertedHtml = convertRawTextToMentionHtml(rawContent, currentProjectId, contextPath);

        // Gán lại nội dung HTML mới cho phần tử
        elem.innerHTML = convertedHtml;
    }
}

// =========================================================================
// KHỞI CHẠY KHI TOÀN BỘ DOM HTML ĐÃ TẢI XONG
// =========================================================================
document.addEventListener("DOMContentLoaded", function() {
    // 1. Quét và chuyển đổi các cú pháp Mention thành Link
    renderAllMessages();

    // 2. Tự động cuộn xuống dòng tin nhắn mới nhất
    scrollToBottom();

    // 3. Tự động đưa con trỏ chuột vào ô nhập tin nhắn
    var chatInput = document.getElementById("chatInput");
    if (chatInput !== null) {
        chatInput.focus();
    }
});
