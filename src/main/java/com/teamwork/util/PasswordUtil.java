package com.teamwork.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Lớp Tiện ích Bảo mật Mật khẩu (Password Security Utility):
 * - Băm mật khẩu bằng thuật toán SHA-256 kết hợp Chuỗi muối tĩnh (Salt)
 * - So sánh mật khẩu an toàn theo thời gian không đổi (Constant-Time Compare)
 *   chống lại các cuộc tấn công khai thác độ trễ thời gian (Timing Attacks).
 */
public class PasswordUtil {

    // Chuỗi Salt bảo vệ chống lại tấn công Rainbow Table
    private static final String SYSTEM_SALT = "TeamWorkHub_Secure_Salt_2026!@#";

    /**
     * Băm mật khẩu dạng Plain Text sang chuỗi mã hóa SHA-256 Hex (64 ký tự)
     *
     * @param plainPassword Mật khẩu gốc người dùng nhập
     * @return Chuỗi băm Hex 64 ký tự, hoặc chuỗi rỗng nếu đầu vào không hợp lệ
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            return "";
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String saltedPassword = SYSTEM_SALT + plainPassword;
            byte[] hashBytes = md.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));

            // Chuyển đổi mảng byte sang chuỗi Hex định dạng 64 ký tự
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Thuật toán SHA-256 không được hỗ trợ trên JVM này!", e);
        }
    }

    /**
     * Xác thực mật khẩu người dùng nhập vào so với mật khẩu đã băm trong cơ sở dữ liệu.
     * Sử dụng MessageDigest.isEqual để chống Timing Attack.
     *
     * @param plainPassword Mật khẩu người dùng nhập vào form đăng nhập
     * @param storedHash Mật khẩu băm đã lưu trong UserDB
     * @return true nếu mật khẩu trùng khớp, ngược lại false
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        String calculatedHash = hashPassword(plainPassword);

        // So sánh an toàn theo thời gian không đổi (Constant-Time Compare)
        byte[] a = calculatedHash.getBytes(StandardCharsets.UTF_8);
        byte[] b = storedHash.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(a, b);
    }
}
