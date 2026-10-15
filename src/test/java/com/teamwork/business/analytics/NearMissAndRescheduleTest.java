package com.teamwork.business.analytics;

import com.teamwork.business.Task;
import com.teamwork.business.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Radar Cảnh Báo Sớm Nguy Cơ Cận Kề Trước 24h & Kiểm Soát Dời Deadline")
class NearMissAndRescheduleTest {

    @Test
    @DisplayName("Phát hiện chính xác các Task nguy cơ cận kề trong 24h (Near-Miss Radar)")
    void testDetectNearMissTasks() {
        LocalDate today = LocalDate.of(2026, 10, 15);

        List<Task> tasks = new ArrayList<>();

        // Task 1: Hạn chót hôm nay (2026-10-15) nhưng chưa bắt đầu (TODO, 0%) -> CRITICAL
        Task t1 = new Task();
        t1.setId(101);
        t1.setTitle("Cấu hình Production SSL");
        t1.setDueDate("2026-10-15");
        t1.setStatus("TODO");
        t1.setProgress(0);
        tasks.add(t1);

        // Task 2: Hạn chót ngày mai (2026-10-16) nhưng tiến độ mới 20% (<40%) -> HIGH
        Task t2 = new Task();
        t2.setId(102);
        t2.setTitle("Viết tài liệu API");
        t2.setDueDate("2026-10-16");
        t2.setStatus("IN_PROGRESS");
        t2.setProgress(20);
        tasks.add(t2);

        // Task 3: Dời hạn 3 lần liên tiếp -> WARNING
        Task t3 = new Task();
        t3.setId(103);
        t3.setTitle("Refactor CSDL Migration");
        t3.setDueDate("2026-10-25");
        t3.setStatus("IN_PROGRESS");
        t3.setProgress(50);
        t3.setRescheduleCount(3);
        tasks.add(t3);

        // Task 4: An toàn: hạn chót còn 10 ngày, tiến độ 60%, chưa dời hạn
        Task t4 = new Task();
        t4.setId(104);
        t4.setTitle("Thiết kế banner landing page");
        t4.setDueDate("2026-10-25");
        t4.setStatus("IN_PROGRESS");
        t4.setProgress(60);
        t4.setRescheduleCount(0);
        tasks.add(t4);

        StaleBlockerDetector.StaleBlockerReport report = StaleBlockerDetector.detect(tasks, Collections.emptyList(), today);

        assertNotNull(report);
        assertEquals(3, report.getTotalNearMissCount(), "Cần phát hiện đúng 3 task có nguy cơ cận kề");

        List<NearMissTask> nmList = report.getNearMissTasks();
        assertEquals(101, nmList.get(0).getTaskId());
        assertEquals("CRITICAL", nmList.get(0).getRiskLevel(), "Task 101 chưa bắt đầu trong 24h phải là CRITICAL");

        boolean hasHigh = nmList.stream().anyMatch(t -> t.getTaskId() == 102 && "HIGH".equals(t.getRiskLevel()));
        assertTrue(hasHigh, "Task 102 tiến độ thấp trong 24h phải là HIGH");

        boolean hasWarning = nmList.stream().anyMatch(t -> t.getTaskId() == 103 && "WARNING".equals(t.getRiskLevel()));
        assertTrue(hasWarning, "Task 103 dời hạn 3 lần phải là WARNING");
    }

    @Test
    @DisplayName("Thống kê số lần dời deadline và phản ánh vào độ tin cậy DeliveryAccountability")
    void testRescheduleImpactOnAccountability() {
        LocalDate today = LocalDate.of(2026, 10, 15);

        User u1 = new User();
        u1.setId(1);
        u1.setFullName("Lê Văn Thường Xuyên Trễ");
        u1.setRole("DEVELOPER");

        List<Task> tasks = new ArrayList<>();

        // 3 task hoàn thành nhưng dời deadline mỗi task 1 lần
        for (int i = 1; i <= 3; i++) {
            Task t = new Task();
            t.setId(i);
            t.setAssigneeId(1);
            t.setStatus("DONE");
            t.setDueDate("2026-10-15");
            t.setReviewedAt("2026-10-15 10:00:00");
            t.setRescheduleCount(1);
            tasks.add(t);
        }

        List<DeliveryAccountability> list = DeliveryAccountability.analyze(List.of(u1), tasks, today);

        assertNotNull(list);
        assertEquals(1, list.size());
        DeliveryAccountability da = list.get(0);

        assertEquals(3, da.getTotalAssignedTasks());
        assertEquals(3, da.getCompletedTasks());
        assertEquals(3, da.getTotalRescheduledTasks(), "Tổng số lần dời hạn của thành viên phải là 3");

        // Dù OTDR = 100% (do hoàn thành đúng hạn mới), nhưng vì đã dời hạn 3 lần -> Độ tin cậy không thể là EXCELLENT!
        assertNotEquals("EXCELLENT", da.getReliabilityRating(), 
            "Thành viên dời hạn >= 3 lần không được xếp loại EXCELLENT");
    }
}
