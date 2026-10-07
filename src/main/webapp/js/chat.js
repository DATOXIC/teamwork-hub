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

// Escape ký tự HTML đặc biệt: nội dung tin nhắn là văn bản người dùng nhập, không bao giờ được coi là HTML
function escapeHtml(text) {
    return String(text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

// =========================================================================
// HÀM 2: CHUYỂN ĐỔI CÚ PHÁP #MENTION THÀNH SMART GLASS BADGES BẤM ĐƯỢC
// =========================================================================
function convertRawTextToMentionHtml(rawText, projectId, ctxPath) {
    if (rawText === null || rawText === undefined) {
        return "";
    }

    // Escape TRƯỚC, sau đó mới chèn thẻ mention (các regex bên dưới chỉ bắt số / chữ nên không tạo lại HTML từ dữ liệu người dùng)
    var resultHtml = escapeHtml(rawText);

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
        return '<a href="' + taskUrl + '" class="chat-mention-task" title="Mở thẻ Kanban công việc #' + taskId + '" aria-label="Mở thẻ công việc số ' + taskId + '">'
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
        confirmBtn.href = contextPath + "/chat?action=delete&projectId=" + encodeURIComponent(projectId) + "&messageId=" + encodeURIComponent(messageId);
        confirmBtn.setAttribute("data-method", "post"); // app.js gửi bằng POST thay vì GET
        var modalInstance = bootstrap.Modal.getInstance(modalElem);
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(modalElem);
        }
        modalInstance.show();
    } else {
        if (confirm("Bạn có chắc chắn muốn xóa tin nhắn này không?")) {
            window.postTo(contextPath + "/chat?action=delete&projectId=" + encodeURIComponent(projectId) + "&messageId=" + encodeURIComponent(messageId));
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
// HÀM 9: THIẾT LẬP BỘ LẮNG NGHE SỰ KIỆN FORM CHAT (CHỐNG F5 & AJAX SUBMIT)
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

    // Xử lý gửi tin nhắn qua AJAX: KHÔNG RELOAD TRANG (CHỐNG F5), cuộn mượt
    chatForm.addEventListener("submit", function(e) {
        e.preventDefault(); // Tuyệt đối ngăn form submit kiểu truyền thống gây reload/F5 trang

        var content = chatInput.value.trim();
        if (!content) {
            chatInput.focus();
            return false;
        }

        // Kích hoạt trạng thái disabled & loading spinner trên nút gửi
        btnSend.disabled = true;
        var originalBtnHtml = btnSend.innerHTML;
        btnSend.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span><span>Gửi...</span>';

        // Xóa bản nháp khi bắt đầu gửi
        clearDraft();

        var params = new URLSearchParams();
        params.append("action", "sendProjectMessage");
        params.append("projectId", currentProjectId);
        params.append("content", content);

        fetch(contextPath + "/chat", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                "X-Requested-With": "XMLHttpRequest"
            },
            body: params.toString()
        })
        .then(function(res) {
            if (!res.ok) throw new Error("HTTP " + res.status);
            return res.json().catch(function() { return { success: true }; });
        })
        .then(function() {
            // Xóa trắng ô input và đưa lại con trỏ chuột
            chatInput.value = "";
            chatInput.focus();
            // Lập tức làm mới khung chat và cuộn mượt xuống dưới đáy
            return refreshChatFeed();
        })
        .catch(function(err) {
            console.error("Lỗi khi gửi tin nhắn qua AJAX, fallback submit:", err);
            chatForm.submit();
        })
        .finally(function() {
            btnSend.disabled = false;
            btnSend.innerHTML = originalBtnHtml;
        });

        return false;
    });
}

function initEditMessageForm() {
    var editForm = document.getElementById("editMessageForm");
    if (!editForm) return;

    editForm.addEventListener("submit", function(e) {
        e.preventDefault();
        var msgId = document.getElementById("editMessageId").value;
        var content = document.getElementById("editMessageContent").value.trim();
        if (!content) return;

        var params = new URLSearchParams();
        params.append("action", "editProjectMessage");
        params.append("projectId", currentProjectId);
        params.append("messageId", msgId);
        params.append("content", content);

        fetch(contextPath + "/chat", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                "X-Requested-With": "XMLHttpRequest"
            },
            body: params.toString()
        })
        .then(function() {
            var modalElem = document.getElementById('editMessageModal');
            if (modalElem) {
                var modalInstance = bootstrap.Modal.getInstance(modalElem);
                if (modalInstance) modalInstance.hide();
            }
            refreshChatFeed();
        })
        .catch(function(err) {
            console.error("Lỗi khi sửa tin nhắn:", err);
            editForm.submit();
        });
    });
}

function initDeleteMessageButton() {
    var confirmBtn = document.getElementById('btnConfirmDeleteMessage');
    if (!confirmBtn) return;

    confirmBtn.addEventListener("click", function(e) {
        e.preventDefault();
        var targetUrl = confirmBtn.getAttribute("href");
        if (!targetUrl || targetUrl === "#") return;

        fetch(targetUrl, {
            method: "GET",
            headers: { "X-Requested-With": "XMLHttpRequest" }
        })
        .then(function() {
            var modalElem = document.getElementById('deleteMessageModal');
            if (modalElem) {
                var modalInstance = bootstrap.Modal.getInstance(modalElem);
                if (modalInstance) modalInstance.hide();
            }
            refreshChatFeed();
        })
        .catch(function(err) {
            console.error("Lỗi khi xóa tin nhắn:", err);
            window.location.href = targetUrl;
        });
    });
}

// =========================================================================
// HÀM 10: RÚT GỌN HIỂN THỊ THỜI GIAN TIN NHẮN (CHUẨN GIỜ VIỆT NAM UTC+7)
// =========================================================================
function parseToDate(rawTime) {
    if (!rawTime) return null;
    rawTime = rawTime.trim();

    try {
        // Pattern 1: ISO/Postgres: yyyy-MM-dd[ T]HH:mm[:ss]...(+00, Z, etc.)
        var isoMatch = rawTime.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})(?::(\d{2}))?(?:\.\d+)?([+-]\d{2}(?::?\d{2})?|Z)?/);
        if (isoMatch) {
            var y = parseInt(isoMatch[1], 10);
            var m = parseInt(isoMatch[2], 10) - 1;
            var d = parseInt(isoMatch[3], 10);
            var hr = parseInt(isoMatch[4], 10);
            var min = parseInt(isoMatch[5], 10);
            var sec = isoMatch[6] ? parseInt(isoMatch[6], 10) : 0;
            var tz = isoMatch[7];

            if (tz) {
                var offsetMinutes = 0;
                if (tz !== 'Z' && tz !== 'z') {
                    var tzSign = tz.charAt(0) === '-' ? -1 : 1;
                    var tzNumbers = tz.replace(/[+-]/, '');
                    var tzHour = 0, tzMin = 0;
                    if (tzNumbers.indexOf(':') !== -1) {
                        var parts = tzNumbers.split(':');
                        tzHour = parseInt(parts[0], 10);
                        tzMin = parseInt(parts[1], 10);
                    } else if (tzNumbers.length <= 2) {
                        tzHour = parseInt(tzNumbers, 10);
                    } else {
                        tzHour = parseInt(tzNumbers.substring(0, 2), 10);
                        tzMin = parseInt(tzNumbers.substring(2, 4), 10);
                    }
                    offsetMinutes = tzSign * (tzHour * 60 + tzMin);
                }
                var utcMs = Date.UTC(y, m, d, hr, min, sec) - (offsetMinutes * 60000);
                return new Date(utcMs);
            } else {
                return new Date(y, m, d, hr, min, sec);
            }
        }

        // Pattern 2: dd/MM/yyyy HH:mm[:ss]
        var dmyMatch = rawTime.match(/^(\d{2})\/(\d{2})\/(\d{4})\s+(\d{2}):(\d{2})(?::(\d{2}))?/);
        if (dmyMatch) {
            var day = parseInt(dmyMatch[1], 10);
            var month = parseInt(dmyMatch[2], 10) - 1;
            var year = parseInt(dmyMatch[3], 10);
            var hour = parseInt(dmyMatch[4], 10);
            var min = parseInt(dmyMatch[5], 10);
            var sec = dmyMatch[6] ? parseInt(dmyMatch[6], 10) : 0;
            return new Date(year, month, day, hour, min, sec);
        }

        var fallback = new Date(rawTime);
        return isNaN(fallback.getTime()) ? null : fallback;
    } catch (e) {
        return null;
    }
}

function formatSmartTimestamp(rawTime) {
    if (!rawTime) return "";
    rawTime = rawTime.trim();

    try {
        var dateObj = parseToDate(rawTime);
        if (dateObj && !isNaN(dateObj.getTime())) {
            var now = new Date();
            var isToday = (dateObj.getDate() === now.getDate() &&
                           dateObj.getMonth() === now.getMonth() &&
                           dateObj.getFullYear() === now.getFullYear());

            var yesterday = new Date(now);
            yesterday.setDate(now.getDate() - 1);
            var isYesterday = (dateObj.getDate() === yesterday.getDate() &&
                               dateObj.getMonth() === yesterday.getMonth() &&
                               dateObj.getFullYear() === yesterday.getFullYear());

            var pad = function(n) { return n < 10 ? '0' + n : n; };
            var timeStr = pad(dateObj.getHours()) + ":" + pad(dateObj.getMinutes());

            if (isToday) {
                return timeStr;
            } else if (isYesterday) {
                return "Hôm qua " + timeStr;
            } else {
                return pad(dateObj.getDate()) + "/" + pad(dateObj.getMonth() + 1) + " " + timeStr;
            }
        }
    } catch (e) {
        console.warn("Lỗi format thời gian:", e);
    }

    if (rawTime.length >= 16 && rawTime.indexOf("/") !== -1) {
        return rawTime.substring(11, 16);
    }
    return rawTime;
}

function formatFullTimestamp(rawTime) {
    if (!rawTime) return "";
    var dateObj = parseToDate(rawTime);
    if (dateObj && !isNaN(dateObj.getTime())) {
        var pad = function(n) { return n < 10 ? '0' + n : n; };
        return pad(dateObj.getHours()) + ":" + pad(dateObj.getMinutes()) + " - " +
               pad(dateObj.getDate()) + "/" + pad(dateObj.getMonth() + 1) + "/" + dateObj.getFullYear() + " (GMT+7)";
    }
    return rawTime;
}

function renderAllTimestamps() {
    var metaEls = document.querySelectorAll(".chat-time-meta");
    for (var el of metaEls) {
        var raw = el.getAttribute("data-raw-time");
        if (!raw) {
            raw = el.getAttribute("title") || el.textContent.trim();
            el.setAttribute("data-raw-time", raw);
        }
        var shortTime = formatSmartTimestamp(raw);
        var fullTime = formatFullTimestamp(raw);
        el.classList.add("text-nowrap");
        var timeSpan = el.querySelector(".time-text");
        if (timeSpan) {
            timeSpan.textContent = shortTime;
            timeSpan.classList.add("text-nowrap");
        } else {
            var icon = el.querySelector("i");
            el.innerHTML = "";
            if (icon) {
                el.appendChild(icon);
            }
            var span = document.createElement("span");
            span.className = "time-text ms-1 text-nowrap";
            span.textContent = shortTime;
            el.appendChild(span);
        }
        el.setAttribute("title", fullTime);
    }
}

// =========================================================================
// HÀM 11: TỰ ĐỘNG LÀM MỚI KHUNG CHAT (POLLING NHẸ, KHÔNG CẦN F5)
// Mỗi vài giây hỏi server chữ ký danh sách tin nhắn (action=poll). Chỉ khi chữ ký đổi
// (có tin mới / tin bị sửa / bị xóa) mới tải lại khung tin nhắn, giữ nguyên ô đang soạn.
// =========================================================================
var CHAT_POLL_INTERVAL_MS = 4000;
var chatPollBusy = false;

function refreshChatFeed() {
    var url = contextPath + "/chat?action=view&projectId=" + encodeURIComponent(currentProjectId);
    return fetch(url, { credentials: "same-origin", headers: { "X-Requested-With": "XMLHttpRequest" } })
        .then(function(res) { return res.text(); })
        .then(function(html) {
            var doc = new DOMParser().parseFromString(html, "text/html");
            var freshFeed = doc.getElementById("chatMessageContainer");
            var feed = document.getElementById("chatMessageContainer");
            if (!freshFeed || !feed) return;

            // Chỉ tự cuộn xuống đáy nếu người dùng đang đọc ở gần cuối (không giật khi đang xem tin cũ)
            var nearBottom = (feed.scrollHeight - feed.scrollTop - feed.clientHeight) < 120;
            feed.innerHTML = freshFeed.innerHTML;
            renderAllMessages();
            renderAllTimestamps();
            if (nearBottom) scrollToBottom();

            // Cập nhật chữ ký mới nhất và số tin nhắn trên đầu trang
            var sigMatch = html.match(/var chatSig = "([^"]*)"/);
            if (sigMatch) chatSig = sigMatch[1];
            var freshBadge = doc.querySelector(".chat-pulse-badge span:last-child");
            var badge = document.querySelector(".chat-pulse-badge span:last-child");
            if (freshBadge && badge) badge.textContent = freshBadge.textContent;
        });
}

function pollChat() {
    if (chatPollBusy || document.hidden) return;
    chatPollBusy = true;
    fetch(contextPath + "/chat?action=poll&projectId=" + encodeURIComponent(currentProjectId),
          { credentials: "same-origin", headers: { "Accept": "application/json" } })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data && data.sig && data.sig !== chatSig) {
                return refreshChatFeed();
            }
        })
        .catch(function() { /* mất mạng / hết phiên: bỏ qua, lần sau thử lại */ })
        .then(function() { chatPollBusy = false; });
}

function initChatPolling() {
    if (typeof chatSig === "undefined" || !document.getElementById("chatMessageContainer")) return;
    setInterval(pollChat, CHAT_POLL_INTERVAL_MS);
    // Quay lại tab sau một lúc thì kiểm tra ngay
    document.addEventListener("visibilitychange", function() {
        if (!document.hidden) pollChat();
    });
}

// =========================================================================
// KHỞI CHẠY KHI TOÀN BỘ DOM HTML ĐÃ TẢI XONG
// =========================================================================
document.addEventListener("DOMContentLoaded", function() {
    // 1. Quét và chuyển đổi các cú pháp Mention thành Link
    renderAllMessages();

    // 1b. Tự động rút gọn thời gian tin nhắn thông minh
    renderAllTimestamps();

    // 2. Khôi phục bản nháp nếu có
    restoreDraft();

    // 3. Khởi tạo xử lý Form & Anti-Spam (AJAX gửi tin không F5)
    initChatForm();
    initEditMessageForm();
    initDeleteMessageButton();

    // 4. Tự động cuộn xuống dòng tin nhắn mới nhất
    scrollToBottom();

    // 4b. Tự động nhận tin nhắn mới của đồng đội mà không cần F5
    initChatPolling();

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
