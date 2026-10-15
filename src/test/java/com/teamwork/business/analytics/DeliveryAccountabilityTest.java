package com.teamwork.business.analytics;

import com.teamwork.business.Task;
import com.teamwork.business.TaskDependency;
import com.teamwork.business.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DeliveryAccountabilityTest {

    @Test
    @DisplayName("Test DeliveryAccountability: OTDR, Say-Do ratio và Reliability Rating")
    void testDeliveryAccountabilityCalculation() {
        LocalDate today = LocalDate.of(2026, 10, 15);

        User u1 = new User();
        u1.setId(101);
        u1.setFullName("Nguyen Van A");
        u1.setRole("MEMBER");

        User u2 = new User();
        u2.setId(102);
        u2.setFullName("Tran Thi B");
        u2.setRole("MEMBER");

        List<User> users = List.of(u1, u2);
        List<Task> tasks = new ArrayList<>();

        // U1 Task 1: Hoàn thành đúng hạn (due 2026-10-10, reviewed/done 2026-10-09)
        Task t1 = new Task();
        t1.setId(1);
        t1.setAssigneeId(101);
        t1.setStatus("DONE");
        t1.setDueDate("2026-10-10");
        t1.setReviewedAt("2026-10-09");
        tasks.add(t1);

        // U1 Task 2: Hoàn thành trễ hạn (due 2026-10-05, reviewed/done 2026-10-08)
        Task t2 = new Task();
        t2.setId(2);
        t2.setAssigneeId(101);
        t2.setStatus("DONE");
        t2.setDueDate("2026-10-05");
        t2.setReviewedAt("2026-10-08");
        tasks.add(t2);

        // U2 Task 3: Đang làm nhưng đã quá hạn (due 2026-10-12, today 2026-10-15)
        Task t3 = new Task();
        t3.setId(3);
        t3.setAssigneeId(102);
        t3.setStatus("IN_PROGRESS");
        t3.setDueDate("2026-10-12");
        tasks.add(t3);

        // U2 Task 4: Đã hoàn thành đúng hạn (due 2026-10-14, done 2026-10-13)
        Task t4 = new Task();
        t4.setId(4);
        t4.setAssigneeId(102);
        t4.setStatus("DONE");
        t4.setDueDate("2026-10-14");
        t4.setReviewedAt("2026-10-13");
        tasks.add(t4);

        List<DeliveryAccountability> report = DeliveryAccountability.analyze(users, tasks, today);
        assertEquals(2, report.size());

        DeliveryAccountability da1 = report.stream().filter(r -> r.getUserId() == 101).findFirst().orElseThrow();
        assertEquals(2, da1.getTotalAssignedTasks());
        assertEquals(2, da1.getCompletedTasks());
        assertEquals(1, da1.getOnTimeCompletedTasks());
        assertEquals(1, da1.getLateCompletedTasks());
        assertEquals(50, da1.getOnTimeDeliveryRate(), "OTDR của U1 phải là 50% (1 on-time / 2 done)");
        assertEquals(100, da1.getSayDoRatio(), "Say-Do của U1 phải là 100% (2 done / 2 assigned)");
        assertEquals("NEEDS_IMPROVEMENT", da1.getReliabilityRating());

        DeliveryAccountability da2 = report.stream().filter(r -> r.getUserId() == 102).findFirst().orElseThrow();
        assertEquals(2, da2.getTotalAssignedTasks());
        assertEquals(1, da2.getCompletedTasks());
        assertEquals(1, da2.getOnTimeCompletedTasks());
        assertEquals(1, da2.getOverdueActiveTasks());
        assertEquals(100, da2.getOnTimeDeliveryRate(), "OTDR của U2 là 100% (1 on-time / 1 done)");
        assertEquals(50, da2.getSayDoRatio(), "Say-Do của U2 là 50% (1 done / 2 assigned)");
        assertEquals("GOOD", da2.getReliabilityRating(), "Có 1 việc quá hạn nên đạt mức GOOD");

        assertTrue(da1.toJson().contains("\"userId\":101"));
        assertTrue(da1.toJson().contains("\"onTimeDeliveryRate\":50"));
    }

    @Test
    @DisplayName("Test StaleBlockerDetector: Phát hiện task trơ và điểm nghẽn nghiêm trọng")
    void testStaleAndBlockerDetection() {
        LocalDate today = LocalDate.of(2026, 10, 15);

        List<Task> tasks = new ArrayList<>();

        // Task 10: Task tiên quyết, đang bị trễ hạn (due 2026-10-10) và chặn 2 task khác -> CRITICAL BLOCKER
        Task t10 = new Task();
        t10.setId(10);
        t10.setTitle("Thiết kế Database & Schema");
        t10.setStatus("IN_PROGRESS");
        t10.setDueDate("2026-10-10");
        t10.setUpdatedAt("2026-10-08"); // Bất động từ ngày 8 đến ngày 15 (>3 ngày)
        tasks.add(t10);

        // Task 11: Task chịu phụ thuộc vào Task 10
        Task t11 = new Task();
        t11.setId(11);
        t11.setTitle("Viết REST API");
        t11.setStatus("TODO");
        t11.setDueDate("2026-10-20");
        tasks.add(t11);

        // Task 12: Task thứ hai chịu phụ thuộc vào Task 10
        Task t12 = new Task();
        t12.setId(12);
        t12.setTitle("Viết Migration Script");
        t12.setStatus("TODO");
        t12.setDueDate("2026-10-18");
        tasks.add(t12);

        // Task 13: Task độc lập nhưng đã xong -> không bị stale hay block
        Task t13 = new Task();
        t13.setId(13);
        t13.setTitle("Khởi tạo dự án");
        t13.setStatus("DONE");
        t13.setDueDate("2026-10-05");
        tasks.add(t13);

        List<TaskDependency> deps = new ArrayList<>();
        deps.add(new TaskDependency(1, 11, 10)); // 11 phụ thuộc vào 10
        deps.add(new TaskDependency(1, 12, 10)); // 12 phụ thuộc vào 10

        StaleBlockerDetector.StaleBlockerReport report = StaleBlockerDetector.detect(tasks, deps, today);

        assertEquals(4, report.getTotalTasks());
        assertEquals(1, report.getTotalStaleCount(), "Task 10 phải bị xem là stale");
        assertEquals(1, report.getTotalBlockerCount(), "Task 10 phải là blocker");
        assertEquals(2, report.getTotalBlockedTasksCount(), "Có 2 task (11 và 12) bị cản trở");
        assertEquals(50, report.getBlockerImpactScore(), "2/4 task bị ảnh hưởng = 50%");

        StaleBlockerDetector.BlockerTaskInfo blocker = report.getCriticalBlockers().get(0);
        assertEquals(10, blocker.getTaskId());
        assertEquals("CRITICAL", blocker.getSeverity(), "Chặn 2 task và quá hạn -> CRITICAL");
        assertEquals(2, blocker.getBlockedTasksCount());

        StaleBlockerDetector.StaleTaskInfo stale = report.getStaleTasks().get(0);
        assertEquals(10, stale.getTaskId());
        assertEquals(7, stale.getStaleDays(), "Từ 2026-10-08 đến 2026-10-15 là 7 ngày");

        String json = report.toJson();
        assertTrue(json.contains("\"blockerImpactScore\":50"));
        assertTrue(json.contains("\"severity\":\"CRITICAL\""));
    }
}
