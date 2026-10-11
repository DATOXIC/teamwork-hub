package com.teamwork.business;

import com.teamwork.business.analytics.BurndownCalculator;
import com.teamwork.business.analytics.BurndownData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Động cơ Burndown (BurndownCalculator Pure POJO)")
class BurndownCalculatorTest {

    @Test
    @DisplayName("Đường xu hướng lý tưởng dốc tuyến tính đều đặn từ tổng số việc về 0")
    void testLinearIdealTrendline() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 11); // 11 ngày (chỉ số 0 đến 10)

        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Task t = new Task();
            t.setId(i);
            t.setStatus("TODO");
            tasks.add(t);
        }

        BurndownData data = BurndownCalculator.calculate(start, end, tasks, start);

        assertEquals(10, data.getTotalTasks());
        assertEquals(0, data.getCompletedTasks());
        assertEquals(10, data.getRemainingTasks());
        assertEquals(11, data.getLabels().size());
        assertEquals(11, data.getIdealRemaining().size());

        // Ngày đầu (i=0): 10 việc
        assertEquals(10.0, data.getIdealRemaining().get(0));
        // Giữa kỳ (i=5): 5 việc
        assertEquals(5.0, data.getIdealRemaining().get(5));
        // Ngày cuối (i=10): 0 việc
        assertEquals(0.0, data.getIdealRemaining().get(10));
    }

    @Test
    @DisplayName("Đường thực tế phản ánh chính xác số việc giảm dần theo ngày hoàn thành")
    void testActualBurndownWithCompletedTasks() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 10); // 10 ngày
        LocalDate asOfDate = LocalDate.of(2026, 10, 5); // Hôm nay là ngày 5

        List<Task> tasks = new ArrayList<>();

        // Task 1: hoàn thành ngày 2
        Task t1 = new Task();
        t1.setId(1);
        t1.setStatus("DONE");
        t1.setReviewedAt("02/10/2026 14:00");
        tasks.add(t1);

        // Task 2: hoàn thành ngày 4
        Task t2 = new Task();
        t2.setId(2);
        t2.setStatus("DONE");
        t2.setReviewedAt("04/10/2026 09:30");
        tasks.add(t2);

        // Task 3: chưa xong
        Task t3 = new Task();
        t3.setId(3);
        t3.setStatus("IN_PROGRESS");
        tasks.add(t3);

        // Task 4: chưa xong
        Task t4 = new Task();
        t4.setId(4);
        t4.setStatus("TODO");
        tasks.add(t4);

        BurndownData data = BurndownCalculator.calculate(start, end, tasks, asOfDate);

        assertEquals(4, data.getTotalTasks());
        assertEquals(2, data.getCompletedTasks());
        assertEquals(2, data.getRemainingTasks());
        assertEquals(50, data.getCompletionRate());

        // Ngày 1 (01/10): chưa có việc nào xong -> 4 việc còn lại
        assertEquals(4.0, data.getActualRemaining().get(0));
        // Ngày 2 (02/10): task 1 xong -> 3 việc còn lại
        assertEquals(3.0, data.getActualRemaining().get(1));
        // Ngày 3 (03/10): 3 việc còn lại
        assertEquals(3.0, data.getActualRemaining().get(2));
        // Ngày 4 (04/10): task 2 xong -> 2 việc còn lại
        assertEquals(2.0, data.getActualRemaining().get(3));
        // Ngày 5 (05/10 - asOfDate): 2 việc còn lại
        assertEquals(2.0, data.getActualRemaining().get(4));

        // Các ngày trong tương lai (sau ngày 5) phải là null để Chart.js ngắt đường thực tế
        assertNull(data.getActualRemaining().get(5)); // ngày 6
        assertNull(data.getActualRemaining().get(9)); // ngày 10
    }

    @Test
    @DisplayName("Dự án rỗng (0 công việc) không gây lỗi chia cho 0 và trả về kết quả rỗng chuẩn")
    void testEmptyTasks() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 10);

        BurndownData data = BurndownCalculator.calculate(start, end, Collections.emptyList(), start);

        assertEquals(0, data.getTotalTasks());
        assertEquals(0, data.getCompletedTasks());
        assertEquals(0, data.getRemainingTasks());
        assertEquals(0, data.getCompletionRate());
        assertEquals(10, data.getLabels().size());
        assertEquals(0.0, data.getIdealRemaining().get(0));
        assertEquals(0.0, data.getActualRemaining().get(0));
        assertTrue(data.isOnTrack());
    }

    @Test
    @DisplayName("Dự án chỉ diễn ra trong 1 ngày (start == end) tính toán an toàn không crash")
    void testSingleDayProjectDuration() {
        LocalDate sameDay = LocalDate.of(2026, 10, 15);

        List<Task> tasks = new ArrayList<>();
        Task t1 = new Task();
        t1.setId(1);
        t1.setStatus("DONE");
        t1.setReviewedAt("15/10/2026 12:00");
        tasks.add(t1);

        BurndownData data = BurndownCalculator.calculate(sameDay, sameDay, tasks, sameDay);

        assertEquals(1, data.getTotalTasks());
        assertEquals(1, data.getCompletedTasks());
        assertEquals(0, data.getRemainingTasks());
        assertEquals(100, data.getCompletionRate());
        assertEquals(1, data.getLabels().size());
        assertEquals(0.0, data.getIdealRemaining().get(0));
        assertEquals(0.0, data.getActualRemaining().get(0));
        assertTrue(data.isOnTrack());
    }

    @Test
    @DisplayName("Phân tích ngày tháng đa định dạng (parseDate) dẻo dai và chống crash")
    void testDateParsingResilience() {
        // dd/MM/yyyy HH:mm
        LocalDate d1 = BurndownCalculator.parseDate("15/10/2026 14:30");
        assertNotNull(d1);
        assertEquals(LocalDate.of(2026, 10, 15), d1);

        // dd/MM/yyyy
        LocalDate d2 = BurndownCalculator.parseDate("20/12/2026");
        assertNotNull(d2);
        assertEquals(LocalDate.of(2026, 12, 20), d2);

        // yyyy-MM-dd
        LocalDate d3 = BurndownCalculator.parseDate("2026-11-05");
        assertNotNull(d3);
        assertEquals(LocalDate.of(2026, 11, 5), d3);

        // Chuỗi rỗng / null
        assertNull(BurndownCalculator.parseDate(null));
        assertNull(BurndownCalculator.parseDate(""));
        assertNull(BurndownCalculator.parseDate("   "));
        assertNull(BurndownCalculator.parseDate("invalid-date-string"));
    }

    @Test
    @DisplayName("Dự án hoàn thành vượt tiến độ (All tasks done early)")
    void testAllTasksCompletedEarly() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 10);
        LocalDate asOfDate = LocalDate.of(2026, 10, 3); // Mới ngày 3 đã xong hết

        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Task t = new Task();
            t.setId(i);
            t.setStatus("DONE");
            t.setReviewedAt("02/10/2026 15:00");
            tasks.add(t);
        }

        BurndownData data = BurndownCalculator.calculate(start, end, tasks, asOfDate);

        assertEquals(5, data.getCompletedTasks());
        assertEquals(0, data.getRemainingTasks());
        assertEquals(100, data.getCompletionRate());
        assertTrue(data.isOnTrack());
        assertTrue(data.getVarianceTasks() < 0); // Âm nghĩa là vượt tiến độ
    }

    @Test
    @DisplayName("Dự án bị chậm tiến độ (Behind schedule)")
    void testBehindScheduleTasks() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 10);
        LocalDate asOfDate = LocalDate.of(2026, 10, 8); // Đã qua 8 ngày mà chỉ xong 1 việc

        List<Task> tasks = new ArrayList<>();
        Task t1 = new Task();
        t1.setId(1);
        t1.setStatus("DONE");
        t1.setReviewedAt("02/10/2026 15:00");
        tasks.add(t1);

        for (int i = 2; i <= 10; i++) {
            Task t = new Task();
            t.setId(i);
            t.setStatus("IN_PROGRESS");
            tasks.add(t);
        }

        BurndownData data = BurndownCalculator.calculate(start, end, tasks, asOfDate);

        assertEquals(1, data.getCompletedTasks());
        assertEquals(9, data.getRemainingTasks());
        assertFalse(data.isOnTrack());
        assertTrue(data.getVarianceTasks() > 0); // Chậm hơn lý tưởng
    }
}
