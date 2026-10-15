package com.teamwork.business;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit Test cho Chặng 1: Kiểm thử Mô hình Định lượng & Tiến độ có Trọng số (Weighted Progress)
 * Thuần POJO, độc lập với DB, hỗ trợ JUnit 5.
 */
public class TaskProgressModelTest {

    @Test
    public void testTaskDefaultValues() {
        Task task = new Task();
        assertEquals(0.0, task.getEstimatedHours(), 0.001);
        assertEquals(0.0, task.getActualHours(), 0.001);
        assertFalse(task.isMilestone());
        assertEquals("TODO", task.getStatus());
        assertEquals(0, task.getProgressPercentage());
    }

    @Test
    public void testTaskStatusProgressWithoutSubtasks() {
        Task task = new Task();
        
        task.setStatus("TODO");
        assertEquals(0, task.getProgressPercentage());

        task.setStatus("IN_PROGRESS");
        assertEquals(40, task.getProgressPercentage());

        task.setStatus("REVISE");
        assertEquals(60, task.getProgressPercentage());

        task.setStatus("SUBMITTED");
        assertEquals(90, task.getProgressPercentage());

        task.setStatus("DONE");
        assertEquals(100, task.getProgressPercentage());
    }

    @Test
    public void testWeightedProgressWithSubtasksHours() {
        Task task = new Task();
        task.setId(1);
        task.setStatus("IN_PROGRESS");

        List<SubTask> subtasks = new ArrayList<>();

        // Subtask 1: 10 giờ, đã xong (DONE)
        SubTask st1 = new SubTask();
        st1.setId(101);
        st1.setEstimatedHours(10.0);
        st1.setStatus("DONE");
        subtasks.add(st1);

        // Subtask 2: 30 giờ, đang làm (TODO)
        SubTask st2 = new SubTask();
        st2.setId(102);
        st2.setEstimatedHours(30.0);
        st2.setStatus("TODO");
        subtasks.add(st2);

        // Subtask 3: 10 giờ, đang làm (TODO)
        SubTask st3 = new SubTask();
        st3.setId(103);
        st3.setEstimatedHours(10.0);
        st3.setStatus("TODO");
        subtasks.add(st3);

        task.setSubtasks(subtasks);

        // Tổng: 50h, Đã xong: 10h -> 10/50 = 20%
        assertEquals(20, task.getProgressPercentage());

        // Nghiệm thu tiếp Subtask 2 (30h) -> Đã xong: 40h/50h = 80%
        st2.setStatus("APPROVED");
        assertEquals(80, task.getProgressPercentage());

        // Nghiệm thu nốt Subtask 3 (10h) -> Đã xong: 50h/50h = 100%
        st3.setStatus("DONE");
        assertEquals(100, task.getProgressPercentage());
    }

    @Test
    public void testSubtaskFallbackProgressWhenZeroHours() {
        Task task = new Task();
        task.setStatus("IN_PROGRESS");

        List<SubTask> subtasks = new ArrayList<>();
        // 4 subtask không nhập số giờ (estimatedHours == 0)
        for (int i = 1; i <= 4; i++) {
            SubTask st = new SubTask();
            st.setId(i);
            st.setStatus(i == 1 ? "DONE" : "TODO");
            subtasks.add(st);
        }
        task.setSubtasks(subtasks);

        // 1/4 subtask xong -> Phải fallback sang đếm số lượng = 25%
        assertEquals(25, task.getProgressPercentage());
    }

    @Test
    public void testProjectMemberDefaultCapacity() {
        ProjectMember pm = new ProjectMember();
        assertEquals(40, pm.getWeeklyCapacityHours());

        pm.setWeeklyCapacityHours(30);
        assertEquals(30, pm.getWeeklyCapacityHours());

        // Giờ âm hoặc 0 phải tự động fallback sang 40 an toàn
        pm.setWeeklyCapacityHours(0);
        assertEquals(40, pm.getWeeklyCapacityHours());
    }
}
