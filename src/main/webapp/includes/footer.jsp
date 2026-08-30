<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!-- Footer -->
<footer class="footer mt-auto py-3 bg-white border-top">
    <div class="container text-center text-muted fs-8">
        <div class="d-flex flex-wrap justify-content-between align-items-center">
            <p class="col-md-4 mb-0 text-start">
                &copy; 2026 <strong>TeamWork Hub</strong> — Đồ án Lập trình Web MVC Model 2.
            </p>
            <div class="col-md-4 d-flex align-items-center justify-content-center mb-3 mb-md-0">
                <span class="badge bg-light text-secondary border px-3 py-2 rounded-pill">
                    <i class="bi bi-cpu me-1"></i> Java 21 &bull; Tomcat 10.1 &bull; Jakarta EE 10
                </span>
            </div>
            <ul class="nav col-md-4 justify-content-end list-unstyled d-flex gap-3 mb-0">
                <li><a href="${pageContext.request.contextPath}/project?action=list" class="text-muted text-decoration-none fs-8"><i class="bi bi-grid me-1"></i>Dashboard</a></li>
                <li><a href="${pageContext.request.contextPath}/auth?action=logout" class="text-muted text-decoration-none fs-8"><i class="bi bi-box-arrow-right me-1"></i>Đăng xuất</a></li>
            </ul>
        </div>
    </div>
</footer>

<!-- Bootstrap 5.3.3 JS Bundle CDN -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz" crossorigin="anonymous"></script>
<!-- UI-04: Global App JS (Floating Toast System + Utilities) -->
<script src="${pageContext.request.contextPath}/js/app.js"></script>
</body>
</html>
