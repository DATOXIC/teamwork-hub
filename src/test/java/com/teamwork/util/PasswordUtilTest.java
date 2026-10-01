package com.teamwork.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordUtilTest {

    @Test
    void hashedOutputIsRecognisedAsHashed() {
        assertTrue(PasswordUtil.isHashed(PasswordUtil.hashPassword("admin123")));
    }

    @Test
    void plaintextIsNotRecognisedAsHashed() {
        assertFalse(PasswordUtil.isHashed("admin123"));
        assertFalse(PasswordUtil.isHashed(""));
        assertFalse(PasswordUtil.isHashed(null));
    }

    @Test
    void storedHashCannotBeUsedAsPassword() {
        // Pass-the-hash: dùng chính chuỗi băm làm mật khẩu thì không được xác thực
        String stored = PasswordUtil.hashPassword("secret1");
        assertTrue(PasswordUtil.verifyPassword("secret1", stored));
        assertFalse(PasswordUtil.verifyPassword(stored, stored));
    }
}
