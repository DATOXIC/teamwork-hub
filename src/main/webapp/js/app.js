/**
 * TeamWork Hub — app.js
 * Global utility scripts: Floating Toast System + misc helpers
 */

(function () {
    'use strict';

    function showToast(message, type) {
        if (!message || !message.trim()) return;

        let container = document.getElementById('toastContainerCustom');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toastContainerCustom';
            container.className = 'toast-container-custom';
            document.body.appendChild(container);
        }

        const icon = type === 'success'
            ? '<i class="bi bi-check-circle-fill toast-icon"></i>'
            : '<i class="bi bi-exclamation-triangle-fill toast-icon"></i>';

        const toast = document.createElement('div');
        toast.className = 	oast-item toast-;
        toast.innerHTML = ${icon}<span class="flex-grow-1"></span><button class="toast-close" aria-label="Dong">&#x2715;</button>;

        container.appendChild(toast);

        function dismiss() {
            toast.classList.add('toast-dismissing');
            toast.addEventListener('animationend', () => toast.remove(), { once: true });
        }

        toast.addEventListener('click', dismiss);
        setTimeout(dismiss, 4000);
    }

    function initToastsFromDOM() {
        const toastData = document.getElementById('toastData');
        if (!toastData) return;

        const successMsg = toastData.dataset.success;
        const errorMsg   = toastData.dataset.error;

        if (successMsg) showToast(successMsg, 'success');
        if (errorMsg)   showToast(errorMsg, 'error');
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initToastsFromDOM);
    } else {
        initToastsFromDOM();
    }

    window.showToast = showToast;

})();
