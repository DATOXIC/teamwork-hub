package com.teamwork.business.analytics;

import com.teamwork.business.Task;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit Test cho Chặng 2: Kiểm thử Thuật toán Burndown có trọng số (Hours),
 * Vận tốc trung bình (Velocity), Dự báo ngày về đích (Forecast Date) và Milestones.
 */
public class WeightedBurndownForecastTest {

    @Test
    public void testWeightedBurndownAndForecastCalculation() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 10); // 10 ngày
        LocalDate asOfDate = LocalDate.of(2026, 10, 5); // Ngày thứ 5

        List<Task> tasks = new ArrayList<>();

        // Task 1: 20 giờ, Milestone, hoàn thành ngày 2026-10-03
        Task t1 = new Task();
        t1.setId(1);
        t1.setTitle("Thiết kế CSDL Core");
        t1.setEstimatedHours(20.0);
        t1.setMilestone(true);
        t1.setStatus("DONE");
        t1.setReviewedAt("03/10/2026 10:00");
        tasks.add(t1);

        // Task 2: 10 giờ, hoàn thành ngày 2026-10-04
        Task t2 = new Task();
        t2.setId(2);
        t2.setTitle("Viết Migration & Seeder");
        t2.setEstimatedHours(10.0);
        t2.setStatus("DONE");
        t2.setReviewedAt("04/10/2026 15:30");
        tasks.add(t2);

        // Task 3: 30 giờ, đang làm dở (chưa xong)
        Task t3 = new Task();
        t3.setId(3);
        t3.setTitle("Xây dựng Analytics API");
        t3.setEstimatedHours(30.0);
        t3.setStatus("IN_PROGRESS");
        tasks.add(t3);

        // Task 4: 20 giờ, chưa làm (TODO), Milestone
        Task t4 = new Task();
        t4.setId(4);
        t4.setTitle("Nghiệm thu Đợt 1");
        t4.setEstimatedHours(20.0);
        t4.setMilestone(true);
        t4.setStatus("TODO");
        tasks.add(t4);

        // Tổng: 20 + 10 + 30 + 20 = 80 giờ
        // Đã hoàn thành: 20 + 10 = 30 giờ
        // Còn lại: 50 giờ
        BurndownData data = BurndownCalculator.calculate(start, end, tasks, asOfDate);

        assertNotNull(data);
        assertEquals(80.0, data.getTotalHours(), 0.01);
        assertEquals(30.0, data.getCompletedHours(), 0.01);
        assertEquals(50.0, data.getRemainingHours(), 0.01);
        // % giờ hoàn thành: 30 / 80 = 37.5% -> round = 38%
        assertEquals(38, data.getHourCompletionRate());

        // Milestones: Có 2 milestone (t1, t4), đã xong 1 milestone (t1)
        assertEquals(2, data.getMilestoneCount());
        assertEquals(1, data.getCompletedMilestones());

        // Vận tốc (Team Velocity): Từ 01/10 đến 05/10 là 5 ngày, xong 30h -> 30/5 = 6.0 h/ngày
        assertEquals(6.0, data.getTeamVelocityHoursPerDay(), 0.1);

        // Dự báo ngày về đích: Cần thêm 50h / 6.0h/ngày = ceil(8.33) = 9 ngày nữa từ 05/10 -> 14/10/2026
        // Deadline là 10/10 -> Dự báo trễ 4 ngày (+4)
        assertEquals("2026-10-14", data.getForecastedCompletionDate());
        assertEquals(4, data.getForecastVarianceDays());

        // Kiểm tra chuỗi giờ lý tưởng
        assertFalse(data.getIdealHoursRemaining().isEmpty());
        // Ngày đầu tiên (01/10) lý tưởng phải là 80h
        assertEquals(80.0, data.getIdealHoursRemaining().get(0), 0.1);
        // Ngày cuối cùng (10/10) lý tưởng phải là 0h
        assertEquals(0.0, data.getIdealHoursRemaining().get(data.getIdealHoursRemaining().size() - 1), 0.1);

        // Kiểm tra chuỗi giờ thực tế: ngày sau asOfDate (ngày 6..10) phải là null
        int asOfIndex = 4; // 01, 02, 03, 04, 05 -> index 4
        assertNotNull(data.getActualHoursRemaining().get(asOfIndex));
        assertNull(data.getActualHoursRemaining().get(asOfIndex + 1));

        // Kiểm tra chuỗi JSON an toàn
        String json = data.toJson();
        assertTrue(json.contains("\"totalHours\":80.0"));
        assertTrue(json.contains("\"remainingHours\":50.0"));
        assertTrue(json.contains("\"forecastedCompletionDate\":\"2026-10-14\""));
    }

    @Test
    public void testAllTasksCompletedForecast() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 10);
        LocalDate asOfDate = LocalDate.of(2026, 10, 5);

        List<Task> tasks = new ArrayList<>();
        Task t1 = new Task();
        t1.setId(1);
        t1.setEstimatedHours(16.0);
        t1.setStatus("DONE");
        t1.setReviewedAt("03/10/2026 12:00");
        tasks.add(t1);

        BurndownData data = BurndownCalculator.calculate(start, end, tasks, asOfDate);

        assertEquals(0.0, data.getRemainingHours(), 0.01);
        assertEquals(100, data.getHourCompletionRate());
        assertEquals("2026-10-05", data.getForecastedCompletionDate());
        // Hoàn thành vào ngày 5, deadline là ngày 10 -> Sớm 5 ngày (-5)
        assertEquals(-5, data.getForecastVarianceDays());
    }
}
