<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <!DOCTYPE html>
        <html lang="vi">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>${not empty pageTitle ? pageTitle : 'TeamWork Hub — Nền Tảng Làm Việc Nhóm'}</title>

            <!-- Favicon -->
            <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/images/favicon.png">

            <!-- Anti-FOUC Theme Script (Khởi tạo Theme tức thời tránh chớp nháy) -->
            <script>
                (function() {
                    try {
                        var theme = localStorage.getItem('teamwork_theme') || localStorage.getItem('teamwork_report_theme');
                        if (!theme) {
                            theme = (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light';
                        }
                        document.documentElement.setAttribute('data-theme', theme);
                        document.documentElement.setAttribute('data-bs-theme', theme);
                    } catch (e) {}
                })();
            </script>

            <!-- Google Font: Inter (Chuẩn ClickUp, Linear & Figma) -->
            <link rel="preconnect" href="https://fonts.googleapis.com">
            <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
            <link
                href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=Plus+Jakarta+Sans:wght@500;600;700&display=swap"
                rel="stylesheet">

            <!-- Bootstrap 5.3.3 CSS CDN -->
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">

            <!-- Bootstrap Icons CDN -->
            <link rel="stylesheet"
                href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">

            <!-- Canvas Confetti CDN (ClickUp & Asana Celebration Animation) -->
            <script src="https://cdn.jsdelivr.net/npm/canvas-confetti@1.9.3/dist/confetti.browser.min.js"></script>

            <!-- Custom CSS (Phong cách Basecamp / Notion) -->
            <link rel="stylesheet"
                href="${pageContext.request.contextPath}/styles/main.css?v=<%= System.currentTimeMillis() %>">

            <!-- CSS Bổ sung riêng cho từng trang trong HEAD với Cache-Busting -->
            <c:set var="resolvedCss" value="${not empty extraCss ? extraCss : (not empty requestScope.extraCss ? requestScope.extraCss : param.extraCss)}" />
            <c:if test="${not empty resolvedCss}">
                <link rel="stylesheet"
                    href="${pageContext.request.contextPath}/${resolvedCss}?v=<%= System.currentTimeMillis() %>">
            </c:if>
        </head>

        <body class="d-flex flex-column min-vh-100 bg-light">