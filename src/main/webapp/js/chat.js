/**
 * File Javascript xử lý cho Giao diện Chat Thảo luận (chat.jsp):
 * Phong cách Apple Glassmorphism / Arc Space
 * 
 * 1. Tự động cuộn dòng thời gian tin nhắn xuống vị trí mới nhất (scrollToBottom).
 * 2. Bộ máy phân tích và chuyển đổi cú pháp #doc-X, #task-X, @username thành Smart Glass Badges.
 * 3. Chuyển tab nhanh Resource Shelf (Tài liệu Docs vs Công việc Tasks).
 * 4. Micro-Actions: Copy tin nhắn 1-click & Trích dẫn tin nhắn (Quote).
 * 5. Quick Emoji Drawer: Chọn emoji thông dụng nhanh chóng vào input.
 * 6. Chống double-submit (spam click) và hiển thị spinner trạng thái đang gửi.
 * 7. Tự động lưu nháp tin nhắn vào LocalStorage (Local Draft) phòng ngừa rớt mạng/reload trang.
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
// HÀM 2: CHUYỂN ĐỔI CÚ PHÁP #MENTION THÀNH SMART GLASS BADGES BẤM ĐƯỢC
// =========================================================================
function convertRawTextToMentionHtml(rawText, projectId, ctxPath) {
    if (rawText === null || rawText === undefined) {
        return "";
    }

    var resultHtml = rawText;

    // 1. Chuyển đổi cú pháp #doc-X thành Link mở bài viết Wiki dạng Glass Badge
    var docRegex = /#doc-(\d+)/g;
    resultHtml = resultHtml.replace(docRegex, function(match, docId) {
        var docUrl = ctxPath + "/doc?action=view&projectId=" + projectId + "&docId=" + docId;
        return '<a href="' + docUrl + '" class="chat-mention-doc" title="Mở bài viết Wiki #' + docId + '" aria-label="Mở tài liệu wiki số ' + docId + '">'
             + '<i class="bi bi-journal-text" aria-hidden="true"></i>'
             + '<span>#doc-' + docId + '</span>'
             + '</a>';
    });

    // 2. Chuyển đổi cú pháp #task-X thành Link mở Bảng Kanban dạng Glass Badge
    var taskRegex = /#task-(\d+)/g;
    resultHtml = resultHtml.replace(taskRegex, function(match, taskId) {
        var taskUrl = ctxPath + "/task?action=list&projectId=" + projectId;
        return '<a href="' + taskUrl + '" class="chat-mention-task" title="Mở thẻ Kanban Task #' + taskId + '" aria-label="Mở thẻ công việc số ' + taskId + '">'
             + '<i class="bi bi-check2-circle" aria-hidden="true"></i>'
             + '<span>#task-' + taskId + '</span>'
             + '</a>';
    });

    // 3. Chuyển đổi cú pháp @username thành thẻ Highlight nhắc tên thành viên
    var userRegex = /@([a-zA-Z0-9_\u00C0-\u024F\u1E00-\u1EFF]+)/g;
    resultHtml = resultHtml.replace(userRegex, function(match, username) {
        return '<span class="chat-mention-user" aria-label="Được nhắc tên: ' + username + '">'
             + '<i class="bi bi-at" aria-hidden="true"></i>'
             + '<span>' + username + '</span>'
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
// HÀM 5: CHUYỂN TAB RESOURCE SHELF (DOCS VS TASKS)
// =========================================================================
function switchResourceTab(tabName) {
    var btnDocs = document.getElementById("tabBtnDocs");
    var btnTasks = document.getElementById("tabBtnTasks");
    var shelfDocs = document.getElementById("shelfDocs");
    var shelfTasks = document.getElementById("shelfTasks");

    if (!btnDocs || !btnTasks || !shelfDocs || !shelfTasks) return;

    if (tabName === 'docs') {
        btnDocs.classList.add("active");
        btnTasks.classList.remove("active");
        shelfDocs.classList.remove("d-none");
        shelfTasks.classList.add("d-none");
    } else if (tabName === 'tasks') {
        btnTasks.classList.add("active");
        btnDocs.classList.remove("active");
        shelfTasks.classList.remove("d-none");
        shelfDocs.classList.add("d-none");
    }
}

// =========================================================================
// HÀM 6: MICRO-ACTIONS (COPY & QUOTE TIN NHẮN)
// =========================================================================
function copyMessageText(msgId) {
    var contentElem = document.getElementById("msg-content-" + msgId);
    if (!contentElem) return;

    var textToCopy = contentElem.getAttribute("data-raw-content") || contentElem.textContent;

    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(textToCopy).then(function() {
            showCopyFeedback(msgId);
        }).catch(function(err) {
            fallbackCopyText(textToCopy, msgId);
        });
    } else {
        fallbackCopyText(textToCopy, msgId);
    }
}

function fallbackCopyText(text, msgId) {
    var textArea = document.createElement("textarea");
    textArea.value = text;
    textArea.style.position = "fixed";
    textArea.style.left = "-9999px";
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    try {
        document.execCommand('copy');
        showCopyFeedback(msgId);
    } catch (err) {
        console.warn("Không thể sao chép tin nhắn:", err);
    }
    document.body.removeChild(textArea);
}

function showCopyFeedback(msgId) {
    var msgRow = document.getElementById("msg-" + msgId);
    if (!msgRow) return;
    
    var copyBtn = msgRow.querySelector(".bi-clipboard");
    if (copyBtn) {
        copyBtn.className = "bi bi-check2 text-success";
        setTimeout(function() {
            copyBtn.className = "bi bi-clipboard";
        }, 1500);
    }
}

function quoteMessage(authorName, msgId) {
    var contentElem = document.getElementById("msg-content-" + msgId);
    var chatInput = document.getElementById("chatInput");
    if (!contentElem || !chatInput) return;

    var raw = contentElem.getAttribute("data-raw-content") || contentElem.textContent;
    var quotePrefix = "> [" + authorName + "]: " + raw.trim() + "\n";

    chatInput.value = quotePrefix + chatInput.value;
    saveDraft(chatInput.value);
    chatInput.focus();
}

function openEditModal(msgId) {
    var contentElem = document.getElementById("msg-content-" + msgId);
    var modalInput = document.getElementById("editMessageContent");
    var modalIdField = document.getElementById("editMessageId");
    
    if (!contentElem || !modalInput || !modalIdField) return;

    var raw = contentElem.getAttribute("data-raw-content") || contentElem.textContent;
    modalIdField.value = msgId;
    modalInput.value = raw.trim();

    var modalElem = document.getElementById('editMessageModal');
    if (modalElem) {
        var modalInstance = bootstrap.Modal.getInstance(modalElem);
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(modalElem);
        }
        modalInstance.show();
        setTimeout(function() {
            modalInput.focus();
            modalInput.select();
        }, 350);
    }
}

function confirmDeleteMessage(projectId, messageId) {
    var modalElem = document.getElementById('deleteMessageModal');
    var confirmBtn = document.getElementById('btnConfirmDeleteMessage');
    if (modalElem && confirmBtn) {
        confirmBtn.href = contextPath + "/chat?action=delete&projectId=" + projectId + "&messageId=" + messageId;
        var modalInstance = bootstrap.Modal.getInstance(modalElem);
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(modalElem);
        }
        modalInstance.show();
    } else {
        if (confirm("Bạn có chắc chắn muốn xóa tin nhắn này không?")) {
            window.location.href = contextPath + "/chat?action=delete&projectId=" + projectId + "&messageId=" + messageId;
        }
    }
}

// =========================================================================
// HÀM 7: QUICK EMOJI DRAWER
// =========================================================================
function toggleEmojiDrawer() {
    var drawer = document.getElementById("emojiDrawer");
    if (drawer) {
        drawer.classList.toggle("active");
    }
}

function insertEmoji(emoji) {
    var chatInput = document.getElementById("chatInput");
    if (!chatInput) return;

    var val = chatInput.value;
    chatInput.value = (val.length > 0 && !val.endsWith(' ') ? val + ' ' : val) + emoji + ' ';
    saveDraft(chatInput.value);
    chatInput.focus();
}

// =========================================================================
// HÀM 8: QUẢN LÝ BẢN NHÁP LOCALSTORAGE (CHỐNG MẤT NỘI DUNG)
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
// HÀM 9: THIẾT LẬP BỘ LẮNG NGHE SỰ KIỆN FORM CHAT (ANTI-SPAM & LOADING)
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

    // Lắng nghe phím bấm: Escape để đóng emoji drawer hoặc blur
    chatInput.addEventListener("keydown", function(e) {
        if (e.key === "Escape") {
            var drawer = document.getElementById("emojiDrawer");
            if (drawer && drawer.classList.contains("active")) {
                drawer.classList.remove("active");
            } else {
                chatInput.blur();
            }
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
        btnSend.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span><span>Gửi...</span>';

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

    // 6. Cho phép bấm vào bóng tin nhắn để hiện Action Bar (hữu ích cho Mobile / Touchpad)
    document.addEventListener("click", function(e) {
        var bubble = e.target.closest(".chat-bubble-me, .chat-bubble-other");
        if (bubble) {
            var row = bubble.closest(".chat-row-me, .chat-row-other");
            if (row) {
                document.querySelectorAll(".chat-row-me.show-actions, .chat-row-other.show-actions").forEach(function(r) {
                    if (r !== row) r.classList.remove("show-actions");
                });
                row.classList.toggle("show-actions");
            }
        } else if (!e.target.closest(".chat-hover-bar")) {
            document.querySelectorAll(".chat-row-me.show-actions, .chat-row-other.show-actions").forEach(function(r) {
                r.classList.remove("show-actions");
            });
        }
    });
});
