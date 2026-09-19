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

        // Mỗi biến thể có màu nền, màu chữ và biểu tượng riêng (xem .toast-* trong main.css).
        // 'gate' dùng biểu tượng khiên trùng với badge "Gate" trên thẻ công việc.
        var variants = {
            success: { cls: 'toast-success', icon: 'bi-check-circle-fill' },
            error:   { cls: 'toast-error',   icon: 'bi-exclamation-triangle-fill' },
            warning: { cls: 'toast-warning', icon: 'bi-exclamation-circle-fill' },
            info:    { cls: 'toast-gate',    icon: 'bi-info-circle-fill' },
            gate:    { cls: 'toast-gate',    icon: 'bi-shield-check' }
        };
        var variant = variants[type] || variants.error;

        // Biểu tượng thừa hưởng màu chữ của toast nên mỗi biến thể tự hoà sắc.
        var iconHtml = '<i class="bi ' + variant.icon + ' toast-icon"></i>';

        var toast = document.createElement('div');
        toast.className = 'toast-item ' + variant.cls;
        
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

        // Thông điệp hướng dẫn dài cần nhiều thời gian đọc hơn thông báo ngắn.
        var readingTime = Math.min(9000, Math.max(4000, message.trim().length * 70));
        setTimeout(dismiss, readingTime);
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

    // =========================================================================
    // GLOBAL THEME SWITCHER (LIGHT / DARK MODE)
    // =========================================================================
    function applyGlobalTheme(theme) {
        if (!theme || (theme !== 'dark' && theme !== 'light')) {
            theme = 'light';
        }

        document.documentElement.setAttribute('data-theme', theme);
        document.documentElement.setAttribute('data-bs-theme', theme);

        try {
            localStorage.setItem('teamwork_theme', theme);
            localStorage.setItem('teamwork_report_theme', theme);
        } catch (e) {
            console.warn('localStorage error:', e);
        }

        // Cập nhật nút Theme trên thanh Top Navbar (nếu có)
        var navText = document.getElementById('globalThemeBtnText');
        var navMoon = document.getElementById('globalThemeIconMoon');
        var navSun  = document.getElementById('globalThemeIconSun');
        if (navText) {
            navText.textContent = (theme === 'dark') ? 'Sáng' : 'Tối';
        }
        if (navMoon && navSun) {
            if (theme === 'dark') {
                navMoon.classList.add('d-none');
                navSun.classList.remove('d-none');
            } else {
                navSun.classList.add('d-none');
                navMoon.classList.remove('d-none');
            }
        }

        // Cập nhật nút Theme tại thanh Subnav của trang Báo cáo (nếu có)
        var repText = document.getElementById('themeBtnText');
        if (repText) {
            repText.textContent = (theme === 'dark') ? 'Chế độ sáng' : 'Chế độ tối';
        }
    }

    function toggleGlobalTheme() {
        var current = document.documentElement.getAttribute('data-theme') || 'light';
        var next = (current === 'dark') ? 'light' : 'dark';
        applyGlobalTheme(next);
    }

    function initGlobalTheme() {
        var savedTheme = null;
        try {
            savedTheme = localStorage.getItem('teamwork_theme') || localStorage.getItem('teamwork_report_theme');
        } catch (e) {}

        if (!savedTheme) {
            savedTheme = (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light';
        }
        applyGlobalTheme(savedTheme);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            initGlobalTheme();
            initToastsFromDOM();
            initProgressBars();
            initLanguage();
        });
    } else {
        initGlobalTheme();
        initToastsFromDOM();
        initProgressBars();
        initLanguage();
    }

    /**
     * Hộp thoại xác nhận dựng trong giao diện, thay cho window.confirm() của trình duyệt
     * (hộp thoại gốc hiển thị tên miền "localhost:8080 cho biết..." và không theo thiết kế chung).
     *
     * options = { title, message, confirmLabel, cancelLabel, variant, onConfirm, onCancel }
     *   variant: 'warning' (mặc định) | 'danger' | 'primary'
     * Nếu Bootstrap chưa nạp thì rơi về confirm() gốc để không mất chức năng.
     */
    function confirmAction(options) {
        var opts = options || {};
        var title = opts.title || 'Xác nhận';
        var message = opts.message || '';
        var confirmLabel = opts.confirmLabel || 'Đồng ý';
        var cancelLabel = opts.cancelLabel || 'Quay lại';
        var variant = opts.variant || 'warning';
        var onConfirm = typeof opts.onConfirm === 'function' ? opts.onConfirm : function () {};
        var onCancel = typeof opts.onCancel === 'function' ? opts.onCancel : function () {};

        if (!window.bootstrap || !window.bootstrap.Modal) {
            if (window.confirm(title + ' — ' + message)) { onConfirm(); } else { onCancel(); }
            return;
        }

        var previous = document.getElementById('appConfirmModal');
        if (previous) previous.remove();

        var skins = {
            warning: { badge: 'bg-warning-subtle text-dark border-warning-subtle', icon: 'bi-exclamation-triangle-fill', btn: 'btn-warning' },
            danger:  { badge: 'bg-danger-subtle text-danger border-danger-subtle', icon: 'bi-trash3-fill',              btn: 'btn-danger'  },
            primary: { badge: 'bg-primary-subtle text-primary border-primary-subtle', icon: 'bi-info-circle-fill',     btn: 'btn-primary' }
        };
        var skin = skins[variant] || skins.warning;

        var wrapper = document.createElement('div');
        wrapper.className = 'modal fade';
        wrapper.id = 'appConfirmModal';
        wrapper.tabIndex = -1;
        wrapper.setAttribute('aria-hidden', 'true');
        wrapper.innerHTML =
            '<div class="modal-dialog modal-dialog-centered">' +
              '<div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">' +
                '<div class="modal-header bg-white px-4 py-3 border-bottom d-flex align-items-center">' +
                  '<span class="badge border rounded-pill px-2-5 py-1 fs-9 fw-bold ' + skin.badge + '">' +
                    '<i class="bi ' + skin.icon + ' me-1"></i><span data-role="title"></span>' +
                  '</span>' +
                  '<button type="button" class="btn-close fs-9 ms-auto" data-bs-dismiss="modal" aria-label="Đóng"></button>' +
                '</div>' +
                '<div class="modal-body px-4 py-3 fs-8 lh-base" data-role="message"></div>' +
                '<div class="modal-footer bg-light px-4 py-3 border-top d-flex justify-content-end gap-2">' +
                  '<button type="button" class="btn btn-light border btn-sm rounded-3 fs-8 px-3" data-bs-dismiss="modal" data-role="cancel"></button>' +
                  '<button type="button" class="btn btn-sm rounded-3 fs-8 px-3 ' + skin.btn + '" data-role="ok"></button>' +
                '</div>' +
              '</div>' +
            '</div>';

        // Dùng textContent cho phần nội dung vì có thể chứa dữ liệu người dùng nhập (tên công việc).
        wrapper.querySelector('[data-role="title"]').textContent = title;
        wrapper.querySelector('[data-role="message"]').textContent = message;
        wrapper.querySelector('[data-role="cancel"]').textContent = cancelLabel;
        wrapper.querySelector('[data-role="ok"]').textContent = confirmLabel;

        document.body.appendChild(wrapper);
        var modal = new window.bootstrap.Modal(wrapper);
        var accepted = false;

        wrapper.querySelector('[data-role="ok"]').addEventListener('click', function () {
            accepted = true;
            modal.hide();
        });
        wrapper.addEventListener('hidden.bs.modal', function () {
            wrapper.remove();
            if (accepted) { onConfirm(); } else { onCancel(); }
        });

        modal.show();
    }

    // Xuất ra phạm vi toàn cục (Global Window Scope) để các nút bấm JSP gọi được trực tiếp
    window.showToast = showToast;
    window.confirmAction = confirmAction;
    window.applyGlobalTheme = applyGlobalTheme;
    window.toggleGlobalTheme = toggleGlobalTheme;
    window.toggleReportTheme = toggleGlobalTheme; // Alias tương thích 100% cho trang Báo cáo

})();


