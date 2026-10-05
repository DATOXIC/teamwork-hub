package com.teamwork.util;

import com.teamwork.business.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Token cho cookie "Ghi nhớ đăng nhập".
 *
 * <p>Trước đây cookie chỉ chứa tên đăng nhập thô nên ai tự đặt cookie {@code teamwork_remember_user=admin}
 * đều được đăng nhập thành {@code admin}. Nay cookie có dạng:
 * <pre>base64url(username).hạn_dùng_ms.base64url(HMAC-SHA256(khóa, username|hạn_dùng|hash mật khẩu))</pre>
 * Chữ ký phụ thuộc vào hash mật khẩu hiện tại của tài khoản: đổi / đặt lại mật khẩu thì mọi token cũ tự vô hiệu.
 *
 * <p>Khóa bí mật lấy từ biến môi trường {@code TEAMWORK_REMEMBER_SECRET} (nên đặt khi triển khai thật).
 * Nếu không đặt, dùng khóa mặc định — vẫn không thể giả mạo token nếu không biết hash mật khẩu của người dùng.
 */
public final class RememberMeToken {

    public static final String COOKIE_NAME = "teamwork_remember_user";
    public static final int MAX_AGE_SECONDS = 14 * 24 * 60 * 60; // 14 ngày

    private static final String DEFAULT_SECRET = "TeamWorkHub_RememberMe_Default_Key_2026";

    private RememberMeToken() {}

    private static byte[] secret() {
        String env = System.getenv("TEAMWORK_REMEMBER_SECRET");
        String key = (env != null && !env.isEmpty()) ? env : DEFAULT_SECRET;
        return key.getBytes(StandardCharsets.UTF_8);
    }

    /** Tạo token cho người dùng vừa đăng nhập thành công. */
    public static String issue(User user) {
        long expiresAt = System.currentTimeMillis() + MAX_AGE_SECONDS * 1000L;
        return build(user.getUsername(), expiresAt, user.getPassword());
    }

    private static String build(String username, long expiresAt, String passwordHash) {
        String userPart = b64(username.getBytes(StandardCharsets.UTF_8));
        return userPart + "." + expiresAt + "." + sign(username, expiresAt, passwordHash);
    }

    /**
     * Chỉ đọc tên đăng nhập trong token để điền sẵn vào form (CHƯA xác thực chữ ký, KHÔNG dùng để đăng nhập).
     *
     * @return username hoặc null nếu cookie không đúng định dạng (vd. cookie dạng cũ chỉ chứa username)
     */
    public static String peekUsername(String token) {
        String[] parts = split(token);
        if (parts == null) return null;
        try {
            return new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Xác thực token với người dùng đọc từ DB: đúng định dạng, chưa hết hạn, đúng chữ ký.
     */
    public static boolean verify(String token, User user) {
        String[] parts = split(token);
        if (parts == null || user == null || user.getUsername() == null) return false;

        String username = peekUsername(token);
        if (username == null || !username.equals(user.getUsername())) return false;

        long expiresAt;
        try {
            expiresAt = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (System.currentTimeMillis() > expiresAt) return false;

        String expected = sign(username, expiresAt, user.getPassword());
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8));
    }

    private static String[] split(String token) {
        if (token == null) return null;
        String[] parts = token.trim().split("\\.");
        return parts.length == 3 ? parts : null;
    }

    private static String sign(String username, long expiresAt, String passwordHash) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret(), "HmacSHA256"));
            String payload = username + "|" + expiresAt + "|" + (passwordHash == null ? "" : passwordHash);
            return b64(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được chữ ký cho cookie ghi nhớ đăng nhập", e);
        }
    }

    private static String b64(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
