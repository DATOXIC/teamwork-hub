package com.teamwork.util;

import static org.junit.jupiter.api.Assertions.*;

import com.teamwork.business.User;
import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MentionParserTest {

    private static User user(int id, String fullName, String username) {
        User u = new User();
        u.setId(id);
        u.setFullName(fullName);
        u.setUsername(username);
        return u;
    }

    private static final List<User> TEAM = List.of(
            user(1, "Bob", "bob1"),
            user(2, "Bob Tran", "bobtran"),
            user(3, "Nguyễn Thị Ánh", "anhnguyen"),
            user(4, "Anh", "anhpm"));

    @Test
    void fullNameWithSpacesPrefersLongestMatch() {
        assertEquals(Set.of(2), MentionParser.findMentionedUserIds("Nhờ @Bob Tran xem giúp", TEAM));
        assertEquals(Set.of(1), MentionParser.findMentionedUserIds("@Bob ơi", TEAM));
    }

    @Test
    void usernameAlsoWorksAndCaseIsIgnored() {
        assertEquals(Set.of(2), MentionParser.findMentionedUserIds("cc @BOBTRAN", TEAM));
        assertEquals(Set.of(3), MentionParser.findMentionedUserIds("@nguyễn thị ánh duyệt nhé", TEAM));
    }

    @Test
    void vietnameseInDifferentUnicodeFormsStillMatches() {
        String decomposed = Normalizer.normalize("@Nguyễn Thị Ánh", Normalizer.Form.NFD);
        assertEquals(Set.of(3), MentionParser.findMentionedUserIds(decomposed, TEAM));
    }

    @Test
    void nameMustEndAtWordBoundary() {
        assertTrue(MentionParser.findMentionedUserIds("@Anhthu", TEAM).isEmpty(), "@Anhthu không phải @Anh");
        assertEquals(Set.of(4), MentionParser.findMentionedUserIds("@Anh, xong chưa?", TEAM));
    }

    @Test
    void emailsAndPlainTextAreNotMentions() {
        assertTrue(MentionParser.findMentionedUserIds("gửi về bob@gmail.com", TEAM).isEmpty());
        assertTrue(MentionParser.findMentionedUserIds("không nhắc ai", TEAM).isEmpty());
        assertTrue(MentionParser.findMentionedUserIds(null, TEAM).isEmpty());
        assertTrue(MentionParser.findMentionedUserIds("@ ", TEAM).isEmpty());
    }

    @Test
    void severalMentionsKeepOrderWithoutDuplicates() {
        assertEquals(List.of(2, 4), List.copyOf(MentionParser.findMentionedUserIds("@Bob Tran và @Anh, nhắc lại @bobtran", TEAM)));
    }

    @Test
    void ambiguousNameNotifiesEveryoneItMatches() {
        List<User> team = List.of(user(5, "Anh", "anh5"), user(6, "Minh Anh", "anh"));
        assertEquals(Set.of(5, 6), MentionParser.findMentionedUserIds("@Anh xem nhé", team));
    }

    @Test
    void onlyCandidatesCanBeMentioned() {
        assertTrue(MentionParser.findMentionedUserIds("@Người Ngoài", TEAM).isEmpty());
    }
}
