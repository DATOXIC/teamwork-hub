package com.teamwork.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.teamwork.business.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Chạy với secret đặt qua System property (giả định máy chạy test KHÔNG đặt biến môi trường
 * TEAMWORK_REMEMBER_SECRET — biến môi trường được ưu tiên hơn).
 */
class RememberMeTokenTest {

    private static final String PROP = "teamwork.remember.secret";

    @AfterEach
    void clearSecret() {
        System.clearProperty(PROP);
    }

    private static User user(String username, String passwordHash) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordHash);
        return u;
    }

    @Test
    void disabledWithoutSecret() {
        System.clearProperty(PROP);
        if (System.getenv("TEAMWORK_REMEMBER_SECRET") != null) return; // máy có đặt env → bỏ qua
        assertFalse(RememberMeToken.isEnabled());
        assertNull(RememberMeToken.issue(user("an", "hash")));
        assertFalse(RememberMeToken.verify("YW4.9999999999999.abc", user("an", "hash")));
    }

    @Test
    void shortSecretIsRejected() {
        if (System.getenv("TEAMWORK_REMEMBER_SECRET") != null) return;
        System.setProperty(PROP, "ngan-qua");
        assertFalse(RememberMeToken.isEnabled());
    }

    @Test
    void validTokenVerifiesAndTamperingFails() {
        System.setProperty(PROP, "0123456789abcdef0123456789abcdef-test-secret");
        User an = user("an", "pbkdf2$600000$c2FsdA$aGFzaA");
        String token = RememberMeToken.issue(an);
        assertTrue(RememberMeToken.verify(token, an));
        assertEquals("an", RememberMeToken.peekUsername(token));

        // Đổi mật khẩu (hash khác) → token cũ hết hiệu lực
        assertFalse(RememberMeToken.verify(token, user("an", "hash-moi")));
        // Sửa chữ ký → không hợp lệ
        assertFalse(RememberMeToken.verify(token.substring(0, token.length() - 2) + "xx", an));
        // Dùng token của "an" cho tài khoản khác → không hợp lệ
        assertFalse(RememberMeToken.verify(token, user("binh", an.getPassword())));
    }
}
