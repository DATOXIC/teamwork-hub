package com.teamwork.business;

import static com.teamwork.business.TaskStatus.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Kiểm tra bảng chuyển trạng thái của Task và Sub-task: phủ MỌI cặp (from, to). */
class StatusTransitionTest {

    // ===== Task =====

    /** Bảng kỳ vọng viết tay, độc lập với TaskStatus.NEXT — lệch nhau là test đỏ. */
    private static Set<TaskStatus> expectedTaskNext(TaskStatus from) {
        switch (from) {
            case TODO:        return EnumSet.of(PLANNING, IN_PROGRESS, SUBMITTED, DONE);
            case PLANNING:    return EnumSet.of(TODO, IN_PROGRESS);
            case IN_PROGRESS: return EnumSet.of(TODO, SUBMITTED, DONE);
            case SUBMITTED:   return EnumSet.of(DONE, REVISE, REJECTED);
            case REVISE:
            case REJECTED:    return EnumSet.of(TODO, IN_PROGRESS, SUBMITTED, DONE);
            case DONE:        return EnumSet.noneOf(TaskStatus.class);
            default: throw new AssertionError(from);
        }
    }

    @Test
    void taskTableCoversEveryPair() {
        for (TaskStatus from : TaskStatus.values()) {
            for (TaskStatus to : TaskStatus.values()) {
                boolean expected = from == to || expectedTaskNext(from).contains(to);
                assertEquals(expected, from.canMoveTo(to), from + " -> " + to);
                assertEquals(expected, TaskStatus.canMove(from.name(), to.name()), "chuỗi: " + from + " -> " + to);
            }
        }
    }

    @Test
    void doneTaskIsLocked() {
        for (TaskStatus to : TaskStatus.values()) {
            if (to != DONE) assertFalse(DONE.canMoveTo(to), "DONE -> " + to);
        }
    }

    @Test
    void submittedTaskCannotBeDraggedAwayFromReview() {
        assertFalse(TaskStatus.canMove("SUBMITTED", "TODO"));
        assertFalse(TaskStatus.canMove("SUBMITTED", "IN_PROGRESS"));
    }

    @Test
    void legacyApprovedTaskCountsAsDone() {
        assertEquals(DONE, TaskStatus.of("approved"));
        assertTrue(TaskStatus.isDone("APPROVED"));
        assertTrue(TaskStatus.isDone(" done "));
        assertFalse(TaskStatus.canMove("APPROVED", "IN_PROGRESS"), "APPROVED cũ cũng bị khóa như DONE");
    }

    @Test
    void unknownOrMissingValues() {
        assertNull(TaskStatus.of(null));
        assertNull(TaskStatus.of("HACKED"));
        assertFalse(TaskStatus.canMove("TODO", "HACKED"));
        assertFalse(TaskStatus.canMove("HACKED", "TODO"));
        assertTrue(TaskStatus.canMove(null, "IN_PROGRESS"), "task chưa có trạng thái coi như TODO");
    }

    @Test
    void taskPhaseHelpers() {
        assertTrue(TaskStatus.isBeforePlanLock("TODO"));
        assertTrue(TaskStatus.isBeforePlanLock("PLANNING"));
        assertFalse(TaskStatus.isBeforePlanLock("IN_PROGRESS"));
        assertTrue(TaskStatus.allowsNewSubTasks("IN_PROGRESS"));
        assertFalse(TaskStatus.allowsNewSubTasks("SUBMITTED"));
        assertFalse(TaskStatus.allowsNewSubTasks("DONE"));
    }

    // ===== Sub-task =====

    private static Set<SubTaskStatus> expectedSubNext(SubTaskStatus from) {
        switch (from) {
            case TODO:      return EnumSet.of(SubTaskStatus.SUBMITTED, SubTaskStatus.DONE);
            case SUBMITTED: return EnumSet.of(SubTaskStatus.APPROVED, SubTaskStatus.REVISE, SubTaskStatus.REJECTED, SubTaskStatus.TODO);
            case REVISE:
            case REJECTED:  return EnumSet.of(SubTaskStatus.SUBMITTED, SubTaskStatus.DONE, SubTaskStatus.TODO);
            case DONE:      return EnumSet.of(SubTaskStatus.TODO, SubTaskStatus.SUBMITTED);
            case APPROVED:  return EnumSet.noneOf(SubTaskStatus.class);
            default: throw new AssertionError(from);
        }
    }

    @Test
    void subTaskTableCoversEveryPair() {
        for (SubTaskStatus from : SubTaskStatus.values()) {
            for (SubTaskStatus to : SubTaskStatus.values()) {
                boolean expected = from == to || expectedSubNext(from).contains(to);
                assertEquals(expected, from.canMoveTo(to), from + " -> " + to);
            }
        }
    }

    @Test
    void approvedSubTaskCannotBeUnticked() {
        assertFalse(SubTaskStatus.canMove("APPROVED", "TODO"), "bỏ tick không được xóa kết quả nghiệm thu");
        assertTrue(SubTaskStatus.canMove("DONE", "TODO"), "fast-track: bỏ tick bình thường");
    }

    @Test
    void onlyTaskLeadPathReachesApproved() {
        // APPROVED chỉ đến được từ SUBMITTED (Task Lead duyệt bài đã nộp)
        for (SubTaskStatus from : SubTaskStatus.values()) {
            if (from != SubTaskStatus.SUBMITTED && from != SubTaskStatus.APPROVED) {
                assertFalse(from.canMoveTo(SubTaskStatus.APPROVED), from + " -> APPROVED");
            }
        }
    }

    @Test
    void finishedMeansDoneOrApproved() {
        assertTrue(SubTaskStatus.isFinished("DONE"));
        assertTrue(SubTaskStatus.isFinished("approved"));
        assertFalse(SubTaskStatus.isFinished("SUBMITTED"));
        assertFalse(SubTaskStatus.isFinished(null));
    }
}
