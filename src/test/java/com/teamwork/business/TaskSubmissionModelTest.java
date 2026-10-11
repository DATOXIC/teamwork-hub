package com.teamwork.business;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Kiểm thử đơn vị (Unit Test) cho mô hình TaskSubmission:
 * - Trạng thái duyệt (isApproved, isRevise, isRejected, isPending)
 * - Tên tệp hiển thị rút gọn (DeliverableDisplayName)
 * - Kiểm tra tính hợp lệ của tệp tải lên (isDeliverableUploaded)
 */
class TaskSubmissionModelTest {

    @Test
    void defaultValuesAndState() {
        TaskSubmission sub = new TaskSubmission(10, 1, 5, "Báo cáo bàn giao ban đầu", "abc123__report.pdf", "11/10/2026 10:00");

        assertEquals(10, sub.getTaskId());
        assertEquals(1, sub.getRevisionNumber());
        assertEquals(5, sub.getSubmittedBy());
        assertEquals("Báo cáo bàn giao ban đầu", sub.getSubmissionNote());
        assertEquals("abc123__report.pdf", sub.getFilePath());
        assertEquals("11/10/2026 10:00", sub.getSubmittedAt());
        assertEquals("SUBMITTED", sub.getStatus());
        assertTrue(sub.isPending());
        assertFalse(sub.isApproved());
        assertFalse(sub.isRevise());
        assertFalse(sub.isRejected());
    }

    @Test
    void statusTransitionsRecognized() {
        TaskSubmission sub = new TaskSubmission();

        sub.setStatus("SUBMITTED");
        assertTrue(sub.isPending());

        sub.setStatus("REVISE");
        assertTrue(sub.isRevise());
        assertFalse(sub.isPending());

        sub.setStatus("REJECTED");
        assertTrue(sub.isRejected());
        assertFalse(sub.isApproved());

        sub.setStatus("APPROVED");
        assertTrue(sub.isApproved());
        assertFalse(sub.isRevise());

        sub.setStatus("DONE");
        assertTrue(sub.isApproved());
    }

    @Test
    void deliverableFileHelpers() {
        TaskSubmission sub = new TaskSubmission();
        sub.setFilePath("");
        assertEquals("", sub.getDeliverableDisplayName());
        assertFalse(sub.isDeliverableUploaded());

        // File format: 32 hex chars + "__" + original name
        String validFile = "0123456789abcdef0123456789abcdef__Bao_cao_kiem_thu_v2.pdf";
        sub.setFilePath(validFile);
        assertEquals("Bao_cao_kiem_thu_v2.pdf", sub.getDeliverableDisplayName());
        assertTrue(sub.isDeliverableUploaded());

        // Legacy / plain filename
        sub.setFilePath("plain_document.docx");
        assertEquals("plain_document.docx", sub.getDeliverableDisplayName());
        assertFalse(sub.isDeliverableUploaded());
    }

    @Test
    void fallbackUserNames() {
        TaskSubmission sub = new TaskSubmission();
        assertEquals("Thành viên", sub.getSubmitterName());
        assertEquals("Trưởng Dự Án", sub.getReviewerName());

        sub.setSubmitterName("Nguyễn Văn A");
        sub.setReviewerName("Trần Trưởng Nhóm");
        assertEquals("Nguyễn Văn A", sub.getSubmitterName());
        assertEquals("Trần Trưởng Nhóm", sub.getReviewerName());
    }
}
