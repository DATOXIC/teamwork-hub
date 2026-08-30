/**
 * TeamWork Hub — app.js
 * Global utility scripts: Floating Toast System + misc helpers
 */

(function () {
    'use strict';

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
        });
    } else {
        initToastsFromDOM();
        initProgressBars();
    }

    window.showToast = showToast;

})();


