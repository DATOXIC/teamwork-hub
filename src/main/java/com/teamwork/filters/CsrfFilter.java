package com.teamwork.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Filter chống CSRF (Cross-Site Request Forgery) theo mô hình "Synchronizer Token".
 *
 * <p>Mỗi session có 1 token ngẫu nhiên lưu ở {@code sessionScope.csrfToken}. Mọi request POST
 * (form hoặc fetch) phải gửi kèm đúng token đó qua tham số {@code _csrf} hoặc header
 * {@code X-CSRF-Token}. Trang web khác không đọc được token nên không thể giả mạo request
 * dưới tên người dùng đang đăng nhập.</p>
 *
 * <p>Phía trình duyệt: {@code js/csrf.js} (nạp trong header.jsp) tự gắn token vào mọi form POST
 * và mọi fetch cùng origin, nên không cần sửa tay từng form.</p>
 */
public class CsrfFilter implements Filter {

    public static final String SESSION_KEY = "csrfToken";
    public static final String PARAM_NAME = "_csrf";
    public static final String HEADER_NAME = "X-CSRF-Token";

    private static final Logger LOGGER = Logger.getLogger(CsrfFilter.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (isStaticResource(path)) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(true);
        String expected = ensureToken(session);

        if (isStateChanging(request.getMethod())) {
            String provided = request.getHeader(HEADER_NAME);
            if (provided == null || provided.isEmpty()) {
                provided = request.getParameter(PARAM_NAME);
            }
            if (!tokensMatch(expected, provided)) {
                LOGGER.warning("CSRF: chặn " + request.getMethod() + " " + path + " ip=" + request.getRemoteAddr());
                reject(request, response, session);
                return;
            }
        }

        chain.doFilter(req, res);
    }

    /** Lấy token của session, tạo mới nếu chưa có. */
    static String ensureToken(HttpSession session) {
        Object existing = session.getAttribute(SESSION_KEY);
        if (existing instanceof String && !((String) existing).isEmpty()) {
            return (String) existing;
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_KEY, token);
        return token;
    }

    /** So sánh thời gian không đổi (chống đoán token qua thời gian phản hồi). */
    static boolean tokensMatch(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }

    static boolean isStateChanging(String method) {
        return !"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)
                && !"OPTIONS".equalsIgnoreCase(method);
    }

    private static boolean isStaticResource(String path) {
        return path.startsWith("/styles/") || path.startsWith("/js/") || path.startsWith("/images/");
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException {
        String message = "Phiên làm việc đã hết hạn hoặc yêu cầu không hợp lệ. Vui lòng tải lại trang và thử lại!";
        if (isAjax(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            // Trả cả "ok" và "success" vì các trang đang đọc một trong hai khóa này
            response.getWriter().write("{\"ok\":false,\"success\":false,\"message\":\"" + message + "\"}");
            return;
        }
        session.setAttribute("toastError", message);
        response.sendRedirect(safeBackUrl(request, session.getAttribute("currentUser") != null));
    }

    private static boolean isAjax(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String contentType = request.getContentType();
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (accept != null && accept.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"));
    }

    /** Quay lại trang trước nếu Referer cùng origin, ngược lại về trang mặc định (tránh open redirect). */
    private static String safeBackUrl(HttpServletRequest request, boolean loggedIn) {
        String fallback = request.getContextPath() + (loggedIn ? "/project?action=list" : "/auth?action=viewLogin");
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isEmpty()) {
            return fallback;
        }
        try {
            URI uri = URI.create(referer);
            boolean sameHost = request.getServerName().equalsIgnoreCase(uri.getHost());
            String refPath = uri.getRawPath();
            if (sameHost && refPath != null && refPath.startsWith(request.getContextPath() + "/")
                    && !refPath.startsWith("//")) {
                return refPath + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
            }
        } catch (IllegalArgumentException ignored) {
            // Referer sai định dạng → dùng fallback
        }
        return fallback;
    }
}
