package com.teamwork.util;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;

/**
 * Chống Open Redirect: chỉ cho phép chuyển hướng tới đường dẫn NỘI BỘ của ứng dụng.
 *
 * <p>Các tham số như {@code redirectUrl}, {@code redirect} hay header {@code Referer} do phía trình duyệt
 * gửi lên nên kẻ xấu có thể đặt thành {@code https://trang-gia-mao.com} hoặc {@code //trang-gia-mao.com}
 * để dụ người dùng sang trang lừa đảo sau khi bấm một link có tên miền thật của ứng dụng.</p>
 */
public final class RedirectUtil {

    private RedirectUtil() {}

    /**
     * Chuyển {@code target} thành đường dẫn nội bộ an toàn (đã có context path), hoặc {@code null} nếu không hợp lệ.
     * Chấp nhận "/task?..." hoặc "/teamwork-hub/task?..."; từ chối URL tuyệt đối, "//host", "\", ký tự xuống dòng.
     */
    public static String toLocalPath(String contextPath, String target) {
        if (target == null) {
            return null;
        }
        String t = target.trim();
        if (t.isEmpty() || !t.startsWith("/") || t.startsWith("//") || t.contains("\\")
                || t.chars().anyMatch(ch -> ch < 0x20 || ch == 0x7f)) {
            return null;
        }
        String ctx = contextPath == null ? "" : contextPath;
        if (!ctx.isEmpty() && (t.equals(ctx) || t.startsWith(ctx + "/") || t.startsWith(ctx + "?"))) {
            return t;
        }
        return ctx + t;
    }

    /**
     * Đường dẫn nội bộ lấy từ header Referer nếu Referer cùng host, ngược lại {@code null}.
     */
    public static String refererToLocalPath(String serverName, String contextPath, String referer) {
        if (referer == null || referer.isEmpty() || serverName == null) {
            return null;
        }
        try {
            URI uri = URI.create(referer);
            if (!serverName.equalsIgnoreCase(uri.getHost()) || uri.getRawPath() == null) {
                return null;
            }
            String path = uri.getRawPath() + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
            String ctx = contextPath == null ? "" : contextPath;
            // Referer phải nằm trong ứng dụng này (đúng context path)
            if (!ctx.isEmpty() && !(path.equals(ctx) || path.startsWith(ctx + "/") || path.startsWith(ctx + "?"))) {
                return null;
            }
            return toLocalPath(ctx, path);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** {@code target} nếu là đường dẫn nội bộ hợp lệ, ngược lại {@code contextPath + fallback}. */
    public static String localOr(HttpServletRequest request, String target, String fallback) {
        String safe = toLocalPath(request.getContextPath(), target);
        return safe != null ? safe : request.getContextPath() + fallback;
    }

    /** Quay lại trang trước (Referer cùng host), ngược lại {@code contextPath + fallback}. */
    public static String backOr(HttpServletRequest request, String fallback) {
        String safe = refererToLocalPath(request.getServerName(), request.getContextPath(), request.getHeader("Referer"));
        return safe != null ? safe : request.getContextPath() + fallback;
    }
}
