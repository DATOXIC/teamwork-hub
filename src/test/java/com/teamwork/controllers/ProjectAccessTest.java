package com.teamwork.controllers;

import static org.junit.jupiter.api.Assertions.*;

import com.teamwork.business.Project;
import com.teamwork.business.User;
import com.teamwork.util.RequestUtil;
import org.junit.jupiter.api.Test;

class ProjectAccessTest {

    private static User user(int id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private static Project projectOwnedBy(int ownerId) {
        Project p = new Project();
        p.setOwnerId(ownerId);
        return p;
    }

    @Test
    void ownerIsRecognized() {
        Project p = projectOwnedBy(7);
        assertTrue(ProjectAccess.isOwner(user(7), p));
        assertFalse(ProjectAccess.isOwner(user(8), p));
    }

    @Test
    void nullsAreNeverOwner() {
        assertFalse(ProjectAccess.isOwner(null, projectOwnedBy(7)));
        assertFalse(ProjectAccess.isOwner(user(7), null));
        assertFalse(ProjectAccess.isOwnerId(0, projectOwnedBy(0)), "id 0 không phải user thật");
    }

    @Test
    void authorOrOwnerMayEditContent() {
        Project p = projectOwnedBy(1);
        assertTrue(ProjectAccess.isAuthorOrOwner(user(5), 5, p), "tác giả");
        assertTrue(ProjectAccess.isAuthorOrOwner(user(1), 5, p), "PM");
        assertFalse(ProjectAccess.isAuthorOrOwner(user(9), 5, p), "thành viên khác");
        assertFalse(ProjectAccess.isAuthorOrOwner(null, 5, p));
    }

    @Test
    void memberCheckRejectsMissingUserOrProjectWithoutQueryingDb() {
        // Hai trường hợp này phải trả false trước khi chạm tới DB
        assertFalse(ProjectAccess.isMember(null, 3));
        assertFalse(ProjectAccess.isMember(user(5), 0));
        assertFalse(ProjectAccess.isMember(user(5), -1));
    }

    @Test
    void parseIntFallsBackToDefault() {
        assertEquals(42, RequestUtil.parseInt(" 42 ", 0));
        assertEquals(-1, RequestUtil.parseInt(null, -1));
        assertEquals(-1, RequestUtil.parseInt("", -1));
        assertEquals(-1, RequestUtil.parseInt("12abc", -1));
        assertEquals(-1, RequestUtil.parseInt("99999999999", -1), "tràn int");
    }
}
