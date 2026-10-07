package com.teamwork.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LoginAttemptLimiterTest {

    private static final long T0 = 10_000_000L;
    private static final String KEY = LoginAttemptLimiter.key("Admin", "1.2.3.4");

    @Test
    void notLockedInitially() {
        assertEquals(0, new LoginAttemptLimiter().lockedSeconds(KEY, T0));
    }

    @Test
    void locksOnFifthConsecutiveFailure() {
        LoginAttemptLimiter l = new LoginAttemptLimiter();
        for (int i = 0; i < 4; i++) {
            assertFalse(l.recordFailure(KEY, T0 + i));
            assertEquals(0, l.lockedSeconds(KEY, T0 + i));
        }
        assertTrue(l.recordFailure(KEY, T0 + 5));
        assertEquals(300, l.lockedSeconds(KEY, T0 + 5));
    }

    @Test
    void unlocksAfterLockDuration() {
        LoginAttemptLimiter l = new LoginAttemptLimiter();
        for (int i = 0; i < 5; i++) l.recordFailure(KEY, T0);
        assertTrue(l.lockedSeconds(KEY, T0 + LoginAttemptLimiter.LOCK_MS - 1) > 0);
        assertEquals(0, l.lockedSeconds(KEY, T0 + LoginAttemptLimiter.LOCK_MS));
    }

    @Test
    void countRestartsAfterLockExpires() {
        LoginAttemptLimiter l = new LoginAttemptLimiter();
        for (int i = 0; i < 5; i++) l.recordFailure(KEY, T0);
        long later = T0 + LoginAttemptLimiter.LOCK_MS + 1;
        assertFalse(l.recordFailure(KEY, later)); // lần sai đầu tiên của chu kỳ mới
        assertEquals(0, l.lockedSeconds(KEY, later));
    }

    @Test
    void successResetsCounter() {
        LoginAttemptLimiter l = new LoginAttemptLimiter();
        for (int i = 0; i < 4; i++) l.recordFailure(KEY, T0);
        l.reset(KEY);
        assertFalse(l.recordFailure(KEY, T0 + 10)); // lại là lần sai thứ 1
        assertEquals(0, l.lockedSeconds(KEY, T0 + 10));
    }

    @Test
    void failuresOutsideWindowDoNotAccumulate() {
        LoginAttemptLimiter l = new LoginAttemptLimiter();
        for (int i = 0; i < 4; i++) l.recordFailure(KEY, T0);
        assertFalse(l.recordFailure(KEY, T0 + LoginAttemptLimiter.WINDOW_MS + 1));
    }

    @Test
    void keysAreIndependentAndCaseInsensitive() {
        LoginAttemptLimiter l = new LoginAttemptLimiter();
        for (int i = 0; i < 5; i++) l.recordFailure(LoginAttemptLimiter.key("admin", "1.2.3.4"), T0);
        assertTrue(l.lockedSeconds(LoginAttemptLimiter.key("ADMIN", "1.2.3.4"), T0) > 0);
        assertEquals(0, l.lockedSeconds(LoginAttemptLimiter.key("admin", "9.9.9.9"), T0));
        assertEquals(0, l.lockedSeconds(LoginAttemptLimiter.key("bob", "1.2.3.4"), T0));
    }

    @Test
    void customThresholdsForOtpSending() {
        // Giống cấu hình OTP trong AuthServlet: 3 lần / 15 phút, khóa 15 phút
        long fifteenMin = 15 * 60 * 1000L;
        LoginAttemptLimiter otp = new LoginAttemptLimiter(3, fifteenMin, fifteenMin);
        assertFalse(otp.recordFailure("otp-user|an", T0));
        assertFalse(otp.recordFailure("otp-user|an", T0 + 1));
        assertTrue(otp.recordFailure("otp-user|an", T0 + 2), "lần thứ 3 → khóa");
        assertEquals(15 * 60, otp.lockedSeconds("otp-user|an", T0 + 2));
        assertEquals(0, otp.lockedSeconds("otp-user|an", T0 + 2 + fifteenMin));
    }
}
