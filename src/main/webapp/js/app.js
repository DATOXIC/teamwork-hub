/**
 * TeamWork Hub — app.js
 * Global utility scripts: Floating Toast System + misc helpers
 */

(function () {
    'use strict';

    // Shared client-side translations for the common UI and frequently used labels.
    var translations = {
        'Nền tảng làm việc nhóm & quản trị dự án All-in-One': 'All-in-one team workspace and project management',
        'Không gian làm việc nhóm': 'Team workspace', 'Tập trung & Tinh gọn': 'Focused & streamlined',
        'Bắt đầu ngay miễn phí': 'Get started for free', 'Vào Không Gian Dự Án': 'Open project workspace',
        'Bảng Kanban Trực Quan': 'Visual Kanban board', 'Tài Liệu Wiki Thông Minh': 'Smart Wiki documents',
        'Trung Tâm Thông Báo': 'Notification center', 'Đã đọc tất cả': 'Mark all as read',
        'Hiện chưa có thông báo nào dành cho bạn.': 'You have no notifications yet.',
        'Hồ sơ chuyên môn của tôi': 'My professional profile', 'Đăng xuất': 'Log out',
        'Đăng nhập / Đăng ký': 'Log in / Sign up', 'Không gian làm việc': 'Workspace',
        'Tài liệu': 'Documents', 'Thảo luận': 'Discussion', 'Báo cáo': 'Reports',
        'Thành viên': 'Members', 'Thành viên nhóm': 'Team members', 'Chưa có thành viên nào': 'No members yet',
        'ID tra cứu nhanh': 'Quick ID lookup', 'Công việc': 'Tasks', 'Chưa có công việc nào': 'No tasks yet',
        'Kênh Thảo luận chung': 'General discussion channel', 'Gửi': 'Send', 'Đang gửi...': 'Sending...',
        'Chưa có cuộc trò chuyện nào': 'No conversations yet', 'Danh mục tài liệu': 'Document categories',
        'Tìm tài liệu...': 'Search documents...', 'Dự án này chưa có tài liệu nào.': 'This project has no documents yet.',
        'Viết bài đầu tiên': 'Write the first document', 'Viết bài mới': 'New document',
        'Soạn thảo tài liệu mới': 'Create a new document', 'Chỉnh sửa tài liệu': 'Edit document',
        'Tiêu đề tài liệu': 'Document title', 'Nội dung chi tiết': 'Detailed content', 'Hủy': 'Cancel',
        'Xuất bản tài liệu': 'Publish document', 'Lưu thay đổi': 'Save changes', 'Hồ sơ cá nhân': 'Personal profile',
        'Đã Xảy Ra Lỗi Hệ Thống': 'A system error occurred', 'Không Tìm Thấy Trang Yêu Cầu': 'Page not found',
        'Về Danh Sách Dự Án': 'Back to projects', 'Trang Chủ': 'Home', 'Đăng Nhập': 'Log In',
        'Đăng ký': 'Sign up', 'Tên đăng nhập': 'Username', 'Mật khẩu': 'Password',
        'Ghi nhớ đăng nhập': 'Remember me', 'Quên mật khẩu?': 'Forgot password?', 'Họ và tên': 'Full name',
        'Chưa có tài khoản?': 'No account yet?', 'Đăng ký ngay': 'Sign up now', 'Đóng': 'Close',
        'Xóa': 'Delete', 'Sửa bài': 'Edit document', 'Đã đồng bộ Cloud': 'Cloud synced',
        'Ưu tiên': 'Priority', 'Nhãn': 'Label', 'Từ khóa': 'Keyword'
    };
    var originalText = new WeakMap();
    var originalAttrs = new WeakMap();
    var translationCache = {};
    var activeLanguage = 'vi';
    var translationQueue = [];
    var activeTranslationRequests = 0;
    var maxTranslationRequests = 6;
    var translationGeneration = 0;
    var suppressObserver = false;
    var observerTimer = null;

    function translate(value, language) {
        if (language === 'vi' || !value) return value;
        var result = value;
        Object.keys(translations).sort(function (a, b) { return b.length - a.length; }).forEach(function (key) {
            result = result.split(key).join(translations[key]);
        });
        return result;
    }

    function hasVietnamese(value) {
        return /[À-ỹĐđ]/.test(value || '');
    }

    function canTranslateRemotely(value) {
        return hasVietnamese(value) && value.length <= 500 && !/[${}<>]/.test(value);
    }

    // Translate strings not yet present in the local dictionary. The local map
    // remains the fallback so the UI still works when the network is unavailable.
    function runTranslationQueue() {
        while (activeTranslationRequests < maxTranslationRequests && translationQueue.length) {
            var job = translationQueue.shift();
            activeTranslationRequests++;
            job.run().then(job.resolve, job.reject).finally(function () {
                activeTranslationRequests--;
                runTranslationQueue();
            });
        }
    }

    function translateRemotely(value) {
        if (!canTranslateRemotely(value)) return Promise.resolve(value);
        if (translationCache[value]) return Promise.resolve(translationCache[value]);
        return new Promise(function (resolve, reject) {
            translationQueue.push({
                resolve: resolve,
                reject: reject,
                run: function () {
                    var url = 'https://translate.googleapis.com/translate_a/single?client=gtx&sl=vi&tl=en&dt=t&q=' + encodeURIComponent(value);
                    return fetch(url, { credentials: 'omit' }).then(function (response) {
                        if (!response.ok) throw new Error('Translation request failed');
                        return response.json();
                    }).then(function (data) {
                        var translated = (data[0] || []).map(function (part) { return part[0] || ''; }).join('');
                        if (translated) translationCache[value] = translated;
                        return translated || value;
                    }).catch(function () { return value; });
                }
            });
            runTranslationQueue();
        });
    }

    function getTextNodes() {
        var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
            acceptNode: function (node) {
                var parent = node.parentElement;
                if (!parent || /^(SCRIPT|STYLE|NOSCRIPT)$/.test(parent.tagName)) return NodeFilter.FILTER_REJECT;
                return NodeFilter.FILTER_ACCEPT;
            }
        });
        var nodes = [];
        while (walker.nextNode()) nodes.push(walker.currentNode);
        return nodes;
    }

    function applyLanguage(language) {
        var generation = ++translationGeneration;
        suppressObserver = true;
        activeLanguage = language;
        document.documentElement.lang = language;
        var textNodes = getTextNodes();
        textNodes.forEach(function (node) {
            if (!originalText.has(node)) originalText.set(node, node.nodeValue);
            node.nodeValue = translate(originalText.get(node), language);
        });
        document.querySelectorAll('*').forEach(function (element) {
            ['title', 'aria-label', 'placeholder'].forEach(function (attribute) {
                if (!element.hasAttribute(attribute)) return;
                if (!originalAttrs.has(element)) originalAttrs.set(element, {});
                var attrs = originalAttrs.get(element);
                if (!Object.prototype.hasOwnProperty.call(attrs, attribute)) attrs[attribute] = element.getAttribute(attribute);
                element.setAttribute(attribute, translate(attrs[attribute], language));
            });
        });
        var current = document.getElementById('languageCurrent');
        if (current) current.textContent = language.toUpperCase();
        localStorage.setItem('teamwork-language', language);
        if (language === 'vi') {
            if (document.title && !document.body.dataset.originalTitle) document.body.dataset.originalTitle = document.title;
            if (document.body.dataset.originalTitle) document.title = document.body.dataset.originalTitle;
            suppressObserver = false;
            return Promise.resolve();
        }

        // Translate every remaining Vietnamese text/attribute in parallel.
        var pending = [];
        textNodes.forEach(function (node) {
            var original = originalText.get(node);
            if (canTranslateRemotely(original)) {
                pending.push(translateRemotely(original).then(function (value) {
                    if (generation === translationGeneration) node.nodeValue = value;
                }));
            }
        });
        document.querySelectorAll('*').forEach(function (element) {
            ['title', 'aria-label', 'placeholder'].forEach(function (attribute) {
                var attrs = originalAttrs.get(element);
                var original = attrs && attrs[attribute];
                if (original && canTranslateRemotely(original)) {
                    pending.push(translateRemotely(original).then(function (value) {
                        if (generation === translationGeneration) element.setAttribute(attribute, value);
                    }));
                }
            });
        });
        if (document.title && !document.body.dataset.originalTitle) document.body.dataset.originalTitle = document.title;
        if (document.body.dataset.originalTitle) {
            pending.push(translateRemotely(document.body.dataset.originalTitle).then(function (value) {
                if (generation === translationGeneration) document.title = value;
            }));
        }
        return Promise.all(pending).finally(function () { suppressObserver = false; });
    }

    function initLanguage() {
        var language = localStorage.getItem('teamwork-language') || 'vi';
        document.addEventListener('click', function (event) {
            var option = event.target.closest('.language-option');
            if (option && option.dataset.language) {
                event.preventDefault();
                applyLanguage(option.dataset.language);
            }
        });
        applyLanguage(language);

        // Translate text injected later by JSP fragments or AJAX responses.
        if (!document.body.dataset.i18nObserver) {
            var observer = new MutationObserver(function (mutations) {
                if (suppressObserver || activeLanguage !== 'en') return;
                var hasAddedContent = mutations.some(function (mutation) { return mutation.addedNodes.length > 0; });
                if (!hasAddedContent) return;
                clearTimeout(observerTimer);
                observerTimer = setTimeout(function () { applyLanguage('en'); }, 80);
            });
            observer.observe(document.body, { childList: true, subtree: true });
            document.body.dataset.i18nObserver = 'true';
        }
    }

    function showToast(message, type) {
        if (!message || !message.trim()) return;

        var container = document.getElementById('toastContainerCustom');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toastContainerCustom';
            container.className = 'toast-container-custom';
            document.body.appendChild(container);
        }

        var isSuccess = (type === 'success');
        var iconHtml = isSuccess
            ? '<i class="bi bi-check-circle-fill toast-icon text-success"></i>'
            : '<i class="bi bi-exclamation-triangle-fill toast-icon text-danger"></i>';

        var toast = document.createElement('div');
        toast.className = 'toast-item ' + (isSuccess ? 'toast-success' : 'toast-error');
        
        var contentSpan = document.createElement('span');
        contentSpan.className = 'flex-grow-1';
        contentSpan.textContent = message;

        var closeBtn = document.createElement('button');
        closeBtn.className = 'toast-close btn-close btn-close-sm';
        closeBtn.setAttribute('aria-label', 'Đóng');

        toast.innerHTML = iconHtml;
        toast.appendChild(contentSpan);
        toast.appendChild(closeBtn);

        container.appendChild(toast);

        function dismiss() {
            toast.classList.add('toast-dismissing');
            toast.addEventListener('animationend', function() {
                toast.remove();
            }, { once: true });
        }

        closeBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            dismiss();
        });

        toast.addEventListener('click', dismiss);
        setTimeout(dismiss, 4000);
    }

    function initToastsFromDOM() {
        var toastData = document.getElementById('toastData');
        if (!toastData) return;

        var successMsg = toastData.getAttribute('data-success');
        var errorMsg   = toastData.getAttribute('data-error');

        if (successMsg && successMsg.trim().length > 0) {
            showToast(successMsg, 'success');
        }
        if (errorMsg && errorMsg.trim().length > 0) {
            showToast(errorMsg, 'error');
        }
    }

    function initProgressBars() {
        var progressBars = document.querySelectorAll('.project-progress-bar');
        if (!progressBars || progressBars.length === 0) return;

        setTimeout(function() {
            progressBars.forEach(function(bar) {
                var targetWidth = bar.getAttribute('data-progress');
                if (targetWidth) {
                    bar.style.width = targetWidth;
                }
            });
        }, 120);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            initToastsFromDOM();
            initProgressBars();
            initLanguage();
        });
    } else {
        initToastsFromDOM();
        initProgressBars();
        initLanguage();
    }

    window.showToast = showToast;
    window.setLanguage = applyLanguage;

})();


