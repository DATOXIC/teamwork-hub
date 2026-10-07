package com.teamwork.business;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProjectInviteTest {

    private static ProjectInvite inviteExpiringAt(String expiredAt) {
        ProjectInvite pi = new ProjectInvite();
        pi.setStatus("PENDING");
        pi.setExpiredAt(expiredAt);
        return pi;
    }

    @Test
    void postgresTimestamptzInThePastIsExpired() {
        assertTrue(inviteExpiringAt("2026-09-25 16:01:31.13931+00").isExpired());
    }

    @Test
    void postgresTimestamptzInTheFutureIsNotExpired() {
        assertFalse(inviteExpiringAt("2999-01-01 00:00:00+07").isExpired());
    }

    @Test
    void offsetWithMinutesIsParsed() {
        assertTrue(inviteExpiringAt("2000-01-01 00:00:00.5+05:30").isExpired());
    }

    @Test
    void timestampWithoutOffsetIsParsed() {
        assertTrue(inviteExpiringAt("2000-01-01 08:00:00").isExpired());
        assertFalse(inviteExpiringAt("2999-01-01 08:00:00.123").isExpired());
    }

    @Test
    void legacyVietnameseFormatStillWorks() {
        assertTrue(inviteExpiringAt("01/01/2000 08:00").isExpired());
        assertFalse(inviteExpiringAt("01/01/2999 08:00").isExpired());
    }

    @Test
    void expiredStatusOrMissingDate() {
        ProjectInvite pi = inviteExpiringAt(null);
        assertFalse(pi.isExpired());
        pi.setStatus("EXPIRED");
        assertTrue(pi.isExpired());
        assertFalse(inviteExpiringAt("không phải ngày").isExpired());
    }
}
