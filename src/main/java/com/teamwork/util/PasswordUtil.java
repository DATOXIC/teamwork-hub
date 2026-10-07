package com.teamwork.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Lớp Tiện ích Bảo mật Mật khẩu (Password Security Utility).
 *
 * <p><b>Thuật toán:</b> PBKDF2-HMAC-SHA256 (có sẵn trong JDK, không cần thư viện ngoài), muối (salt)
 * ngẫu nhiên 16 byte RIÊNG cho từng mật khẩu, lặp {@value #ITERATIONS} vòng (khuyến nghị OWASP).
 * Lặp nhiều vòng khiến việc dò mật khẩu khi lộ CSDL chậm đi hàng trăm nghìn lần so với SHA-256 một vòng.</p>
 *
 * <p><b>Định dạng lưu:</b> {@code pbkdf2$<số vòng>$<salt Base64>$<hash Base64>} (~80 ký tự, cột password là VARCHAR(255)).</p>
 *
 * <p><b>Tương thích dữ liệu cũ:</b> hash cũ (SHA-256 + salt cố định, 64 ký tự hex) vẫn đăng nhập được;
 * {@link #needsRehash(String)} báo cần nâng cấp để UserDB băm lại bằng PBKDF2 ngay khi đăng nhập thành công.</p>
 */
public class PasswordUtil {

    /** Số vòng lặp PBKDF2-HMAC-SHA256 (OWASP Password Storage Cheat Sheet). */
    public static final int ITERATIONS = 600_000;

    private static final String PREFIX = "pbkdf2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Salt cố định của định dạng CŨ — chỉ dùng để kiểm tra hash cũ, không dùng để tạo hash mới. */
    private static final String LEGACY_SYSTEM_SALT = "TeamWorkHub_Secure_Salt_2026!@#";

    /**
     * Băm mật khẩu gốc sang định dạng PBKDF2 (mỗi lần gọi cho kết quả khác nhau vì salt ngẫu nhiên).
     *
     * @param plainPassword Mật khẩu gốc người dùng nhập
     * @return Chuỗi {@code pbkdf2$...}, hoặc chuỗi rỗng nếu đầu vào không hợp lệ
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            return "";
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(plainPassword, salt, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        return PREFIX + "$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(hash);
    }

    /** @return true nếu chuỗi là bản băm (PBKDF2 mới hoặc SHA-256 cũ), không phải mật khẩu thô. */
    public static boolean isHashed(String value) {
        return value != null && (value.startsWith(PREFIX + "$") || isLegacyHash(value));
    }

    /** @return true nếu hash cần băm lại theo cấu hình hiện tại (hash cũ, hoặc PBKDF2 ít vòng hơn). */
    public static boolean needsRehash(String storedHash) {
        String[] parts = splitPbkdf2(storedHash);
        if (parts == null) {
            return true;
        }
        try {
            return Integer.parseInt(parts[1]) < ITERATIONS;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * Xác thực mật khẩu người dùng nhập so với hash đã lưu. So sánh theo thời gian không đổi (chống Timing Attack).
     *
     * @param plainPassword Mật khẩu người dùng nhập vào form đăng nhập
     * @param storedHash Hash đã lưu trong CSDL (định dạng PBKDF2 mới hoặc SHA-256 cũ)
     * @return true nếu mật khẩu trùng khớp, ngược lại false
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || plainPassword.isEmpty() || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        String[] parts = splitPbkdf2(storedHash);
        if (parts != null) {
            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);
                if (iterations <= 0 || salt.length == 0 || expected.length == 0) {
                    return false;
                }
                return MessageDigest.isEqual(expected, pbkdf2(plainPassword, salt, iterations));
            } catch (IllegalArgumentException e) { // số vòng / Base64 hỏng
                return false;
            }
        }

        if (isLegacyHash(storedHash)) {
            return MessageDigest.isEqual(legacySha256(plainPassword).getBytes(StandardCharsets.UTF_8),
                    storedHash.getBytes(StandardCharsets.UTF_8));
        }
        return false;
    }

    private static String[] splitPbkdf2(String storedHash) {
        if (storedHash == null || !storedHash.startsWith(PREFIX + "$")) {
            return null;
        }
        String[] parts = storedHash.split("\\$");
        return parts.length == 4 ? parts : null;
    }

    private static boolean isLegacyHash(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("JVM không hỗ trợ " + ALGORITHM, e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Định dạng cũ: SHA-256(salt cố định + mật khẩu), 64 ký tự hex. */
    private static String legacySha256(String plainPassword) {
        try {
            byte[] hashBytes = MessageDigest.getInstance("SHA-256")
                    .digest((LEGACY_SYSTEM_SALT + plainPassword).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM không hỗ trợ SHA-256", e);
        }
    }
}
