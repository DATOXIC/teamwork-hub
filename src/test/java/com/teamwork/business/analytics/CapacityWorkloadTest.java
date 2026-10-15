package com.teamwork.business.analytics;

import com.teamwork.business.ProjectMember;
import com.teamwork.business.Task;
import com.teamwork.business.User;
import com.teamwork.business.UserWorkload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CapacityWorkloadTest {

    @Test
    @DisplayName("Test Daily Workload Spread & Capacity Utilization (Chặng 4)")
    void testDailySpreadAndCapacity() {
        // Thứ 2: 2026-10-12 đến Thứ 6: 2026-10-16
        LocalDate monday = LocalDate.of(2026, 10, 12);

        User u1 = new User();
        u1.setId(101);
        u1.setFullName("Le Van Tuan");
        u1.setRole("LEAD");

        User u2 = new User();
        u2.setId(102);
        u2.setFullName("Pham Thi Mai");
        u2.setRole("MEMBER");

        UserWorkload uw1 = new UserWorkload();
        uw1.setUser(u1);
        uw1.setLeadTaskCount(2);

        UserWorkload uw2 = new UserWorkload();
        uw2.setUser(u2);
        uw2.setLeadTaskCount(1);

        ProjectMember pm1 = new ProjectMember();
        pm1.setUserId(101);
        pm1.setWeeklyCapacityHours(40); // 40h/tuần = 8h/ngày

        ProjectMember pm2 = new ProjectMember();
        pm2.setUserId(102);
        pm2.setWeeklyCapacityHours(20); // Part-time: 20h/tuần = 4h/ngày

        List<Task> tasks = new ArrayList<>();

        // U1 Task 1: 20 giờ từ Thứ 2 (2026-10-12) đến Thứ 6 (2026-10-16) -> 5 ngày làm việc = 4h/ngày
        Task t1 = new Task();
        t1.setId(1);
        t1.setAssigneeId(101);
        t1.setStatus("IN_PROGRESS");
        t1.setStartDate("2026-10-12");
        t1.setDueDate("2026-10-16");
        t1.setEstimatedHours(20.0);
        tasks.add(t1);

        // U1 Task 2: 25 giờ từ Thứ 2 (2026-10-12) đến Thứ 4 (2026-10-14) -> 3 ngày làm việc = 8.33h/ngày
        // Tổng Thứ 2 của U1 = 4h + 8.33h = 12.33h > 8h -> Quá tải!
        Task t2 = new Task();
        t2.setId(2);
        t2.setAssigneeId(101);
        t2.setStatus("TODO");
        t2.setStartDate("2026-10-12");
        t2.setDueDate("2026-10-14");
        t2.setEstimatedHours(25.0);
        tasks.add(t2);

        // U2 Task 3: 5 giờ từ Thứ 5 đến Thứ 6 -> 2 ngày làm việc = 2.5h/ngày (so với capacity 20h là rảnh)
        Task t3 = new Task();
        t3.setId(3);
        t3.setAssigneeId(102);
        t3.setStatus("TODO");
        t3.setStartDate("2026-10-15");
        t3.setDueDate("2026-10-16");
        t3.setEstimatedHours(5.0);
        tasks.add(t3);

        List<WorkloadAnalytics> analytics = WorkloadAnalytics.fromWorkloads(
                List.of(uw1, uw2),
                List.of(pm1, pm2),
                tasks,
                monday
        );

        assertEquals(2, analytics.size());

        WorkloadAnalytics wa1 = analytics.stream().filter(a -> a.getUserId() == 101).findFirst().orElseThrow();
        assertEquals(40.0, wa1.getWeeklyCapacityHours());
        assertEquals(8.0, wa1.getDailyCapacityHours());
        assertEquals(45.0, wa1.getAssignedRemainingHours());
        assertEquals(45.0, wa1.getWeekAssignedHours());
        assertTrue(wa1.getMondayHours() > 8.0, "Thứ 2 của U1 phải > 8h");
        assertEquals("OVERLOADED", wa1.getCapacityStatus());
        assertTrue(wa1.isOverloaded());
        assertTrue(wa1.getCapacityUtilizationRate() > 100);

        WorkloadAnalytics wa2 = analytics.stream().filter(a -> a.getUserId() == 102).findFirst().orElseThrow();
        assertEquals(20.0, wa2.getWeeklyCapacityHours());
        assertEquals(4.0, wa2.getDailyCapacityHours());
        assertEquals(5.0, wa2.getAssignedRemainingHours());
        assertEquals("UNDERUTILIZED", wa2.getCapacityStatus());
        assertFalse(wa2.isOverloaded());

        // Kiểm tra Rebalance Suggestion
        List<WorkloadAnalytics.RebalanceSuggestion> suggestions =
                WorkloadAnalytics.generateRebalanceSuggestions(analytics, tasks);

        assertFalse(suggestions.isEmpty(), "Phải có ít nhất 1 gợi ý điều phối từ U1 sang U2");
        WorkloadAnalytics.RebalanceSuggestion sug = suggestions.get(0);
        assertEquals(101, sug.getFromUserId());
        assertEquals(102, sug.getToUserId());
        assertEquals(2, sug.getTaskId(), "Ưu tiên chọn task TODO (#2) để san tải");
        assertTrue(sug.getReason().contains("quá tải"));

        String json = wa1.toJson();
        assertTrue(json.contains("\"capacityStatus\":\"OVERLOADED\""));
        assertTrue(json.contains("\"mondayHours\":"));
    }
}
