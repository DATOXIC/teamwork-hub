package com.teamwork.controllers.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.teamwork.business.SubTask;
import com.teamwork.business.Task;
import org.junit.jupiter.api.Test;

/**
 * Bước duyệt chỉ hợp lệ khi đối tượng đang ở trạng thái SUBMITTED (đã nộp).
 * Truyền session = null: hàm chỉ bỏ qua việc ghi toast, kết quả kiểm tra không đổi.
 */
class ReviewStatusGuardTest {

    private static Task taskWithStatus(String status) {
        Task t = new Task();
        t.setTitle("Thiết kế CSDL");
        t.setStatus(status);
        return t;
    }

    private static SubTask subTaskWithStatus(String status) {
        SubTask st = new SubTask();
        st.setTitle("Vẽ ERD");
        st.setStatus(status);
        return st;
    }

    @Test
    void pmCanReviewOnlySubmittedTask() {
        assertTrue(TaskWorkflowHandler.isAwaitingPmReview(taskWithStatus("SUBMITTED"), null));
        for (String s : new String[] {"TODO", "PLANNING", "IN_PROGRESS", "REVISE", "REJECTED", "DONE"}) {
            assertFalse(TaskWorkflowHandler.isAwaitingPmReview(taskWithStatus(s), null), s);
        }
    }

    @Test
    void leadCanReviewOnlySubmittedSubTask() {
        assertTrue(SubTaskHandler.isAwaitingLeadReview(subTaskWithStatus("SUBMITTED"), null));
        for (String s : new String[] {"TODO", "REVISE", "REJECTED", "DONE", "APPROVED"}) {
            assertFalse(SubTaskHandler.isAwaitingLeadReview(subTaskWithStatus(s), null), s);
        }
    }
}
