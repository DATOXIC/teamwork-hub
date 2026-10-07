package com.teamwork.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeliverableStorageTest {

    @TempDir
    Path tmp;

    @BeforeEach
    void useTempDir() {
        System.setProperty("teamwork.upload.dir", tmp.toString());
    }

    @AfterEach
    void reset() {
        System.clearProperty("teamwork.upload.dir");
    }

    private static ByteArrayInputStream bytes(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void storesOutsideWebappAndResolvesBack() throws Exception {
        String stored = DeliverableStorage.store(7, "Báo cáo nghiệm thu.pdf", 5, bytes("%PDF-"));
        assertTrue(DeliverableStorage.isStoredName(stored), stored);
        assertEquals("Báo cáo nghiệm thu.pdf", DeliverableStorage.displayName(stored));
        Path file = DeliverableStorage.resolve(7, stored);
        assertNotNull(file);
        assertTrue(file.startsWith(tmp.resolve("task-7")));
        assertEquals("%PDF-", Files.readString(file));
        assertEquals("application/pdf", DeliverableStorage.contentType(stored));
    }

    @Test
    void anotherTaskCannotReadTheFile() throws Exception {
        String stored = DeliverableStorage.store(7, "a.pdf", 1, bytes("x"));
        assertNull(DeliverableStorage.resolve(8, stored), "tệp của task 7 không lấy được qua task 8");
    }

    @Test
    void rejectsDisallowedExtensions() {
        for (String name : new String[] {"virus.exe", "shell.jsp", "page.html", "script.js", "noext", "x.pdf.jsp"}) {
            assertThrows(DeliverableStorage.InvalidFileException.class,
                    () -> DeliverableStorage.store(1, name, 3, bytes("abc")), name);
        }
    }

    @Test
    void rejectsEmptyAndOversizedFiles() {
        assertThrows(DeliverableStorage.InvalidFileException.class, () -> DeliverableStorage.store(1, "a.pdf", 0, bytes("")));
        assertThrows(DeliverableStorage.InvalidFileException.class,
                () -> DeliverableStorage.store(1, "a.pdf", DeliverableStorage.MAX_BYTES + 1, bytes("x")));
    }

    @Test
    void pathTraversalInNameIsStripped() throws Exception {
        String stored = DeliverableStorage.store(3, "..\\..\\..\\Windows\\evil.pdf", 1, bytes("x"));
        assertEquals("evil.pdf", DeliverableStorage.displayName(stored));
        String stored2 = DeliverableStorage.store(3, "../../etc/passwd.txt", 1, bytes("x"));
        assertEquals("passwd.txt", DeliverableStorage.displayName(stored2));
        assertTrue(DeliverableStorage.resolve(3, stored2).startsWith(tmp.resolve("task-3")));
    }

    @Test
    void forgedStoredNamesAreRefused() {
        assertNull(DeliverableStorage.resolve(1, "../../secret.pdf"));
        assertNull(DeliverableStorage.resolve(1, "0123456789abcdef0123456789abcdef__../x.pdf"));
        assertNull(DeliverableStorage.resolve(1, "https://example.com/a.pdf"));
        assertNull(DeliverableStorage.resolve(1, null));
        assertFalse(DeliverableStorage.isStoredName("bao-cao.pdf"), "tên gõ tay kiểu cũ không phải tệp đã tải lên");
    }

    @Test
    void sanitizeKeepsVietnameseAndLimitsLength() {
        assertEquals("Kế hoạch_ sprint_1_.docx", DeliverableStorage.sanitize("Kế hoạch: sprint<1>.docx"));
        String longName = "a".repeat(300) + ".zip";
        String clean = DeliverableStorage.sanitize(longName);
        assertTrue(clean.length() <= 150);
        assertTrue(clean.endsWith(".zip"));
    }

    @Test
    void deleteQuietlyRemovesOnlyThatTasksFile() throws Exception {
        String stored = DeliverableStorage.store(5, "a.pdf", 1, bytes("x"));
        DeliverableStorage.deleteQuietly(6, stored);
        assertNotNull(DeliverableStorage.resolve(5, stored), "xóa nhầm task khác thì không được xóa");
        DeliverableStorage.deleteQuietly(5, stored);
        assertNull(DeliverableStorage.resolve(5, stored));
    }
}
