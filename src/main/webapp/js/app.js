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

    function translate(value, language) {
        if (language === 'vi' || !value) return value;
        var result = value;
        Object.keys(translations).sort(function (a, b) { return b.length - a.length; }).forEach(function (key) {
            result = result.split(key).join(translations[key]);
        });
        return result;
    }

    function applyLanguage(language) {
        document.documentElement.lang = language;
        document.querySelectorAll('*').forEach(function (element) {
            element.childNodes.forEach(function (node) {
                if (node.nodeType !== Node.TEXT_NODE) return;
                if (!originalText.has(node)) originalText.set(node, node.nodeValue);
                node.nodeValue = translate(originalText.get(node), language);
            });
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
    }

    function initLanguage() {
        var language = localStorage.getItem('teamwork-language') || 'vi';
        document.querySelectorAll('.language-option').forEach(function (option) {
            option.addEventListener('click', function () { applyLanguage(option.dataset.language); });
        });
        applyLanguage(language);
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

})();


