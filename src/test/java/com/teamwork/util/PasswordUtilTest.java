package com.teamwork.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordUtilTest {

    /** Hash định dạng CŨ của "admin123" (SHA-256 + salt cố định) — dữ liệu đang có trong CSDL. */
    private static final String LEGACY_ADMIN123 = legacyHashOf("admin123");

    @Test
    void hashedOutputIsRecognisedAsHashed() {
        assertTrue(PasswordUtil.isHashed(PasswordUtil.hashPassword("admin123")));
        assertTrue(PasswordUtil.isHashed(LEGACY_ADMIN123));
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

    @Test
    void newHashesUsePbkdf2WithRandomSalt() {
        String a = PasswordUtil.hashPassword("secret1");
        String b = PasswordUtil.hashPassword("secret1");
        assertTrue(a.startsWith("pbkdf2$" + PasswordUtil.ITERATIONS + "$"));
        assertNotEquals(a, b, "salt ngẫu nhiên → cùng mật khẩu nhưng hash khác nhau");
        assertTrue(PasswordUtil.verifyPassword("secret1", a));
        assertTrue(PasswordUtil.verifyPassword("secret1", b));
        assertFalse(PasswordUtil.verifyPassword("secret2", a));
        assertTrue(a.length() < 255, "vừa cột users.password VARCHAR(255)");
    }

    @Test
    void legacyHashesStillLogInButNeedRehash() {
        assertTrue(PasswordUtil.verifyPassword("admin123", LEGACY_ADMIN123));
        assertFalse(PasswordUtil.verifyPassword("admin124", LEGACY_ADMIN123));
        assertTrue(PasswordUtil.needsRehash(LEGACY_ADMIN123));
        assertFalse(PasswordUtil.needsRehash(PasswordUtil.hashPassword("admin123")));
        assertTrue(PasswordUtil.needsRehash("pbkdf2$1000$AAAA$AAAA"), "ít vòng hơn cấu hình hiện tại");
    }

    @Test
    void malformedHashesNeverVerify() {
        assertFalse(PasswordUtil.verifyPassword("x", "pbkdf2$abc$AAAA$AAAA"));
        assertFalse(PasswordUtil.verifyPassword("x", "pbkdf2$600000$!!!$###"));
        assertFalse(PasswordUtil.verifyPassword("x", "pbkdf2$600000$AAAA"));
        assertFalse(PasswordUtil.verifyPassword("", PasswordUtil.hashPassword("x")));
        assertFalse(PasswordUtil.verifyPassword(null, LEGACY_ADMIN123));
    }

    private static String legacyHashOf(String plain) {
        try {
            byte[] h = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(("TeamWorkHub_Secure_Salt_2026!@#" + plain).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : h) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
