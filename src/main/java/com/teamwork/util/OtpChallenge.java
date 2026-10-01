package com.teamwork.util;

import java.io.Serializable;
import java.security.SecureRandom;

/**
 * Trạng thái một phiên OTP quên mật khẩu (lưu trong HttpSession).
 *
 * <ul>
 *   <li>OTP 6 số, hiệu lực {@link #TTL_MS} (3 phút)</li>
 *   <li>Tối đa {@link #MAX_ATTEMPTS} lần nhập sai, quá số lần → khóa, phải yêu cầu OTP mới</li>
 *   <li>Chỉ lưu bản băm của OTP, không lưu OTP gốc</li>
 * </ul>
 *
 * Mọi hàm nhận thời gian {@code now} (ms) làm tham số để dễ kiểm thử.
 */
public class OtpChallenge implements Serializable {

    public enum Result { OK, WRONG, EXPIRED, LOCKED }

    public static final long TTL_MS = 3 * 60 * 1000L;
    public static final long RESEND_MS = 60 * 1000L;
    public static final int MAX_ATTEMPTS = 3;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final int userId;          // 0 = tài khoản không tồn tại (OTP giả, không được gửi đi)
    private final String otpHash;
    private final long sentAt;
    private final long expiresAt;
    private int attempts;
    private boolean verified;

    public OtpChallenge(int userId, String otp, long now) {
        this.userId = userId;
        this.otpHash = PasswordUtil.hashPassword(otp);
        this.sentAt = now;
        this.expiresAt = now + TTL_MS;
    }

    /** Sinh OTP 6 chữ số (có thể bắt đầu bằng 0). */
    public static String generateOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    /** Kiểm tra OTP người dùng nhập. Mỗi lần sai làm tăng bộ đếm. */
    public Result verify(String input, long now) {
        if (verified) {
            return Result.OK;
        }
        if (attempts >= MAX_ATTEMPTS) {
            return Result.LOCKED;
        }
        if (now > expiresAt) {
            return Result.EXPIRED;
        }
        if (input != null && PasswordUtil.verifyPassword(input.trim(), otpHash)) {
            verified = true;
            return Result.OK;
        }
        attempts++;
        return attempts >= MAX_ATTEMPTS ? Result.LOCKED : Result.WRONG;
    }

    public boolean canResend(long now) {
        return now - sentAt >= RESEND_MS;
    }

    public long resendWaitSeconds(long now) {
        long remain = RESEND_MS - (now - sentAt);
        return Math.max(0, (remain + 999) / 1000);
    }

    public int remainingAttempts() {
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }

    public boolean isVerified() {
        return verified;
    }

    public int getUserId() {
        return userId;
    }
}
