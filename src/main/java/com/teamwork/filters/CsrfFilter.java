package com.teamwork.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import com.teamwork.util.RedirectUtil;
import java.io.IOException;
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
                // Form có tệp quá lớn: Tomcat bỏ dở việc đọc form nên KHÔNG thấy _csrf → báo đúng lý do thay vì "hết phiên"
                if (isOversizedUpload(request)) {
                    reject(request, response, session,
                            "Tệp đính kèm vượt quá giới hạn 20 MB. Vui lòng chọn tệp nhỏ hơn.");
                    return;
                }
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

    /** Request multipart mà Tomcat từ chối đọc vì vượt giới hạn @MultipartConfig của servlet đích. */
    static boolean isOversizedUpload(HttpServletRequest request) {
        String type = request.getContentType();
        if (type == null || !type.toLowerCase().startsWith("multipart/")) return false;
        try {
            request.getParts();
            return false;
        } catch (IllegalStateException tooLarge) {
            return true;
        } catch (Exception other) {
            return false;
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, HttpSession session)
            throws IOException {
        reject(request, response, session,
                "Phiên làm việc đã hết hạn hoặc yêu cầu không hợp lệ. Vui lòng tải lại trang và thử lại!");
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, HttpSession session, String message)
            throws IOException {
        if (isAjax(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            // Trả cả "ok" và "success" vì các trang đang đọc một trong hai khóa này
            response.getWriter().write("{\"ok\":false,\"success\":false,\"message\":\"" + message + "\"}");
            return;
        }
        session.setAttribute("toastError", message);
        // Quay lại trang trước (Referer cùng host), ngược lại về trang mặc định — RedirectUtil chặn open redirect
        boolean loggedIn = session.getAttribute("currentUser") != null;
        response.sendRedirect(RedirectUtil.backOr(request, loggedIn ? "/project?action=list" : "/auth?action=viewLogin"));
    }

    private static boolean isAjax(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String contentType = request.getContentType();
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (accept != null && accept.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"));
    }
}
