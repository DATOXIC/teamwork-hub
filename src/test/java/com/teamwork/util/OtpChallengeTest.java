package com.teamwork.util;

import static org.junit.jupiter.api.Assertions.*;

import com.teamwork.util.OtpChallenge.Result;
import org.junit.jupiter.api.Test;

class OtpChallengeTest {

    private static final long T0 = 1_000_000L;

    @Test
    void generatedOtpIsSixDigits() {
        for (int i = 0; i < 200; i++) {
            assertTrue(OtpChallenge.generateOtp().matches("\\d{6}"));
        }
    }

    @Test
    void correctOtpVerifies() {
        OtpChallenge c = new OtpChallenge(1, "123456", T0);
        assertEquals(Result.OK, c.verify("123456", T0 + 1000));
        assertTrue(c.isVerified());
    }

    @Test
    void wrongOtpCountsAttemptsAndLocksOnThird() {
        OtpChallenge c = new OtpChallenge(1, "123456", T0);
        assertEquals(Result.WRONG, c.verify("000000", T0));
        assertEquals(Result.WRONG, c.verify("000001", T0));
        assertEquals(Result.LOCKED, c.verify("000002", T0));
        // đã khóa: ngay cả OTP đúng cũng bị từ chối
        assertEquals(Result.LOCKED, c.verify("123456", T0));
        assertFalse(c.isVerified());
    }

    @Test
    void expiredAfterThreeMinutes() {
        OtpChallenge c = new OtpChallenge(1, "123456", T0);
        assertEquals(Result.EXPIRED, c.verify("123456", T0 + OtpChallenge.TTL_MS + 1));
    }

    @Test
    void validAtExactExpiryBoundary() {
        OtpChallenge c = new OtpChallenge(1, "123456", T0);
        assertEquals(Result.OK, c.verify("123456", T0 + OtpChallenge.TTL_MS));
    }

    @Test
    void resendThrottle() {
        OtpChallenge c = new OtpChallenge(1, "123456", T0);
        assertFalse(c.canResend(T0 + 30_000));
        assertEquals(30, c.resendWaitSeconds(T0 + 30_000));
        assertTrue(c.canResend(T0 + 60_000));
    }

    @Test
    void nullInputIsWrong() {
        OtpChallenge c = new OtpChallenge(1, "123456", T0);
        assertEquals(Result.WRONG, c.verify(null, T0));
    }
}
