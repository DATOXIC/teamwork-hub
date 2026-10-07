package com.teamwork.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RedirectUtilTest {

    private static final String CTX = "/teamwork-hub";

    @Test
    void internalPathsAreAccepted() {
        assertEquals(CTX + "/task?action=list&projectId=5", RedirectUtil.toLocalPath(CTX, "/task?action=list&projectId=5"));
        assertEquals(CTX + "/task?x=1", RedirectUtil.toLocalPath(CTX, CTX + "/task?x=1"));
        assertEquals("/task", RedirectUtil.toLocalPath("", "/task")); // deploy ROOT (Render)
    }

    @Test
    void externalOrTrickyTargetsAreRejected() {
        assertNull(RedirectUtil.toLocalPath(CTX, "https://evil.com"));
        assertNull(RedirectUtil.toLocalPath(CTX, "//evil.com/path"));
        assertNull(RedirectUtil.toLocalPath("", "//evil.com"));
        assertNull(RedirectUtil.toLocalPath(CTX, "/\\evil.com"));
        assertNull(RedirectUtil.toLocalPath(CTX, "javascript:alert(1)"));
        assertNull(RedirectUtil.toLocalPath(CTX, "evil.com"));
        assertNull(RedirectUtil.toLocalPath(CTX, "/task\r\nSet-Cookie: x=1"));
        assertNull(RedirectUtil.toLocalPath(CTX, ""));
        assertNull(RedirectUtil.toLocalPath(CTX, null));
    }

    @Test
    void refererMustBeSameHostAndSameApp() {
        assertEquals(CTX + "/doc?action=list&projectId=2",
                RedirectUtil.refererToLocalPath("localhost", CTX, "http://localhost:8080/teamwork-hub/doc?action=list&projectId=2"));
        assertNull(RedirectUtil.refererToLocalPath("localhost", CTX, "https://evil.com/teamwork-hub/doc"));
        assertNull(RedirectUtil.refererToLocalPath("localhost", CTX, "http://localhost:8080/other-app/page"));
        assertNull(RedirectUtil.refererToLocalPath("localhost", CTX, "khong phai url"));
        assertNull(RedirectUtil.refererToLocalPath("localhost", CTX, null));
    }
}
