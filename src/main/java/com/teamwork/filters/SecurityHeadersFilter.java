package com.teamwork.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Gắn các HTTP header bảo mật cho MỌI response (kể cả redirect, trang lỗi, file tĩnh).
 *
 * <ul>
 *   <li>{@code X-Frame-Options} + CSP {@code frame-ancestors}: cấm trang khác nhúng app vào iframe
 *       (chống clickjacking — lừa người dùng bấm nút "Xóa" nằm ẩn dưới một trang giả).</li>
 *   <li>{@code X-Content-Type-Options: nosniff}: trình duyệt không "đoán" kiểu file (chống chạy file tải lên như script).</li>
 *   <li>{@code Referrer-Policy}: không gửi đường dẫn đầy đủ (có projectId, taskId...) sang trang web khác.</li>
 *   <li>CSP cơ bản: cấm plugin ({@code object-src}), cấm đổi {@code <base>}, form chỉ được gửi về chính app.</li>
 * </ul>
 *
 * <p>CSP chưa giới hạn {@code script-src}: các JSP còn dùng nhiều {@code onclick="..."} và {@code <script>} nội tuyến,
 * chặn sẽ làm hỏng giao diện. Muốn siết thêm phải tách hết script nội tuyến ra file .js trước.</p>
 */
public class SecurityHeadersFilter implements Filter {

    static final String CONTENT_SECURITY_POLICY =
            "frame-ancestors 'none'; object-src 'none'; base-uri 'self'; form-action 'self'";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);

        // Chạy sau HTTPS proxy (Render): bắt trình duyệt luôn dùng HTTPS cho tên miền này
        if (request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000");
        }

        chain.doFilter(req, res);
    }
}
