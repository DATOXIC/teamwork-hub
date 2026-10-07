/*
 * csrf.js — Gắn CSRF token vào mọi request thay đổi dữ liệu (xem filters/CsrfFilter.java).
 * Nạp trong <head> (header.jsp, login.jsp, forgot-password.jsp) để chạy TRƯỚC các script khác.
 *
 *  - fetch(): tự thêm header X-CSRF-Token cho request POST/PUT/DELETE cùng origin.
 *  - <form method="post">: tự chèn <input type="hidden" name="_csrf"> khi trang tải xong và ngay lúc submit.
 *  - Form tạo bằng JS rồi gọi form.submit() (không phát sự kiện submit): gọi window.addCsrfInput(form) trước.
 */
(function () {
    'use strict';

    var meta = document.querySelector('meta[name="csrf-token"]');
    var token = meta ? meta.getAttribute('content') : '';
    window.CSRF_TOKEN = token;

    window.addCsrfInput = function (form) {
        if (!token || !form || (form.method || '').toLowerCase() !== 'post') return;
        if (form.querySelector('input[name="_csrf"]')) return;
        var input = document.createElement('input');
        input.type = 'hidden';
        input.name = '_csrf';
        input.value = token;
        form.appendChild(input);
    };

    // 1. fetch(): thêm header cho request thay đổi dữ liệu gửi về chính server này
    if (window.fetch && token) {
        var originalFetch = window.fetch;
        window.fetch = function (input, init) {
            init = init || {};
            var method = (init.method || (input && input.method) || 'GET').toUpperCase();
            var url = new URL(typeof input === 'string' ? input : input.url, window.location.href);
            if (method !== 'GET' && method !== 'HEAD' && url.origin === window.location.origin) {
                var headers = new Headers(init.headers || (input && input.headers) || {});
                if (!headers.has('X-CSRF-Token')) headers.set('X-CSRF-Token', token);
                init.headers = headers;
            }
            return originalFetch.call(this, input, init);
        };
    }

    // 2. Form có sẵn trên trang
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('form').forEach(window.addCsrfInput);
    });

    // 3. Form thêm vào sau (modal, render động): chèn ngay lúc submit (capture chạy trước handler khác)
    document.addEventListener('submit', function (e) {
        window.addCsrfInput(e.target);
    }, true);
})();
