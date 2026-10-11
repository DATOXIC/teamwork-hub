package com.teamwork.business;

import com.teamwork.business.analytics.BurndownData;
import com.teamwork.business.analytics.QualityMetrics;
import com.teamwork.business.analytics.QualityVelocityCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Động cơ Chất Lượng & Vận Tốc PM (QualityVelocityCalculator Pure POJO)")
class QualityVelocityCalculatorTest {

    @Test
    @DisplayName("Tỷ lệ duyệt đạt ngay lần đầu (FTPR) đạt 100% khi toàn bộ việc qua ở Revision 1")
    void testFirstTimePassRate100Percent() {
        List<TaskSubmission> subs = new ArrayList<>();

        TaskSubmission s1 = new TaskSubmission(101, 1, 1, "Bàn giao 1", "", "10/10/2026 08:00");
        s1.setStatus("APPROVED");
        s1.setReviewedAt("10/10/2026 10:00");
        s1.setQualityRating(5);
        subs.add(s1);

        TaskSubmission s2 = new TaskSubmission(102, 1, 2, "Bàn giao 2", "", "10/10/2026 09:00");
        s2.setStatus("APPROVED");
        s2.setReviewedAt("10/10/2026 11:30");
        s2.setQualityRating(4);
        subs.add(s2);

        QualityMetrics metrics = QualityVelocityCalculator.calculate(subs, Collections.emptyList());

        assertEquals(2, metrics.getTotalReviewedTasks());
        assertEquals(2, metrics.getFirstTimePassedCount());
        assertEquals(0, metrics.getRevisionRequiredCount());
        assertEquals(100.0, metrics.getFirstTimePassRate());
        assertEquals(4.5, metrics.getAvgQualityRating());
    }

    @Test
    @DisplayName("FTPR phản ánh chính xác khi có các đầu việc bị yêu cầu nộp lại (Revision >= 2)")
    void testFirstTimePassRateWithRevisions() {
        List<TaskSubmission> subs = new ArrayList<>();

        // Task 101: Đạt ngay lần đầu (Rev 1 APPROVED)
        TaskSubmission s1_1 = new TaskSubmission(101, 1, 1, "Nộp lần 1", "", "10/10/2026 08:00");
        s1_1.setStatus("APPROVED");
        s1_1.setReviewedAt("10/10/2026 09:00");
        subs.add(s1_1);

        // Task 102: Lần 1 bị yêu cầu sửa (Rev 1 REVISE, Rev 2 APPROVED)
        TaskSubmission s2_1 = new TaskSubmission(102, 1, 2, "Nộp lần 1", "", "10/10/2026 08:00");
        s2_1.setStatus("REVISE");
        s2_1.setReviewedAt("10/10/2026 10:00");
        subs.add(s2_1);

        TaskSubmission s2_2 = new TaskSubmission(102, 2, 2, "Nộp sửa lần 2", "", "11/10/2026 08:00");
        s2_2.setStatus("APPROVED");
        s2_2.setReviewedAt("11/10/2026 10:00");
        subs.add(s2_2);

        // Task 103: Lần 1 bị từ chối (Rev 1 REJECTED)
        TaskSubmission s3_1 = new TaskSubmission(103, 1, 3, "Nộp lần 1", "", "10/10/2026 08:00");
        s3_1.setStatus("REJECTED");
        s3_1.setReviewedAt("10/10/2026 11:00");
        subs.add(s3_1);

        // Task 104: Đang chờ duyệt (SUBMITTED) -> chưa tính vào mẫu số đã duyệt
        TaskSubmission s4_1 = new TaskSubmission(104, 1, 4, "Nộp lần 1", "", "12/10/2026 08:00");
        s4_1.setStatus("SUBMITTED");
        subs.add(s4_1);

        QualityMetrics metrics = QualityVelocityCalculator.calculate(subs, Collections.emptyList());

        assertEquals(3, metrics.getTotalReviewedTasks()); // 101, 102, 103
        assertEquals(1, metrics.getFirstTimePassedCount()); // chỉ có 101
        assertEquals(2, metrics.getRevisionRequiredCount()); // 102 và 103
        assertEquals(1, metrics.getPendingReviewCount()); // 104
        assertEquals(33.3, metrics.getFirstTimePassRate()); // 1 / 3 = 33.3%
    }

    @Test
    @DisplayName("Vận tốc phản hồi PM (Turnaround Hours/Days) tính toán chính xác giữa submittedAt và reviewedAt")
    void testPmTurnaroundVelocity() {
        List<TaskSubmission> subs = new ArrayList<>();

        // Sub 1: 08:00 -> 12:00 = 4.0 giờ
        TaskSubmission s1 = new TaskSubmission(101, 1, 1, "Nộp", "", "10/10/2026 08:00");
        s1.setReviewedAt("10/10/2026 12:00");
        s1.setStatus("APPROVED");
        subs.add(s1);

        // Sub 2: 09:00 -> 15:00 = 6.0 giờ
        TaskSubmission s2 = new TaskSubmission(102, 1, 2, "Nộp", "", "11/10/2026 09:00");
        s2.setReviewedAt("11/10/2026 15:00");
        s2.setStatus("REVISE");
        subs.add(s2);

        QualityMetrics metrics = QualityVelocityCalculator.calculate(subs, Collections.emptyList());

        assertEquals(5.0, metrics.getAvgResponseHours()); // (4.0 + 6.0) / 2 = 5.0 giờ
        assertEquals(0.2, metrics.getAvgResponseDays()); // 5.0 / 24.0 = 0.2 ngày
        assertEquals(4.0, metrics.getFastestResponseHours());
        assertEquals(6.0, metrics.getSlowestResponseHours());
    }

    @Test
    @DisplayName("Xếp hạng chính xác danh sách các công việc bị yêu cầu sửa đổi nhiều nhất (Most Revised Tasks)")
    void testMostRevisedTasksRanking() {
        List<TaskSubmission> subs = new ArrayList<>();
        List<Task> tasks = new ArrayList<>();

        Task t1 = new Task(); t1.setId(1); t1.setTitle("Task 1 - Bình thường"); tasks.add(t1);
        Task t2 = new Task(); t2.setId(2); t2.setTitle("Task 2 - Sửa 2 lần"); tasks.add(t2);
        Task t3 = new Task(); t3.setId(3); t3.setTitle("Task 3 - Sửa 1 lần"); tasks.add(t3);

        // Task 1: 1 sub, 0 reject
        TaskSubmission s1 = new TaskSubmission(1, 1, 1, "Nộp 1", "", "10/10/2026 08:00");
        s1.setStatus("APPROVED");
        subs.add(s1);

        // Task 2: 3 subs, 2 rejects (REVISE + REJECTED)
        TaskSubmission s2_1 = new TaskSubmission(2, 1, 1, "Nộp 1", "", "10/10/2026 08:00");
        s2_1.setStatus("REVISE");
        subs.add(s2_1);
        TaskSubmission s2_2 = new TaskSubmission(2, 2, 1, "Nộp 2", "", "11/10/2026 08:00");
        s2_2.setStatus("REJECTED");
        subs.add(s2_2);
        TaskSubmission s2_3 = new TaskSubmission(2, 3, 1, "Nộp 3", "", "12/10/2026 08:00");
        s2_3.setStatus("APPROVED");
        subs.add(s2_3);

        // Task 3: 2 subs, 1 reject
        TaskSubmission s3_1 = new TaskSubmission(3, 1, 1, "Nộp 1", "", "10/10/2026 08:00");
        s3_1.setStatus("REVISE");
        subs.add(s3_1);
        TaskSubmission s3_2 = new TaskSubmission(3, 2, 1, "Nộp 2", "", "11/10/2026 08:00");
        s3_2.setStatus("APPROVED");
        subs.add(s3_2);

        QualityMetrics metrics = QualityVelocityCalculator.calculate(subs, tasks);
        List<QualityMetrics.RevisedTaskMetric> list = metrics.getMostRevisedTasks();

        assertFalse(list.isEmpty());
        // Đầu danh sách phải là Task 2 (2 lần reject, 3 revisions)
        assertEquals(2, list.get(0).getTaskId());
        assertEquals(2, list.get(0).getRejectionsCount());
        assertEquals(3, list.get(0).getRevisionCount());

        // Thứ hai là Task 3 (1 lần reject, 2 revisions)
        assertEquals(3, list.get(1).getTaskId());
        assertEquals(1, list.get(1).getRejectionsCount());
    }

    @Test
    @DisplayName("Đánh giá Điểm Sức Khỏe Dự Án phân cấp chuẩn xác: HEALTHY, AT_RISK, CRITICAL")
    void testHealthScoreClassification() {
        // Trường hợp 1: Dự án chuẩn, không có lỗi -> HEALTHY
        BurndownData healthyBurndown = new BurndownData();
        healthyBurndown.setOnTrack(true);
        healthyBurndown.setVarianceTasks(0);

        List<TaskSubmission> subsGood = new ArrayList<>();
        TaskSubmission sg = new TaskSubmission(1, 1, 1, "Good", "", "10/10/2026 08:00");
        sg.setStatus("APPROVED");
        subsGood.add(sg);

        QualityMetrics mHealthy = QualityVelocityCalculator.calculate(subsGood, Collections.emptyList(), Collections.emptyList(), healthyBurndown);
        assertEquals("HEALTHY", mHealthy.getHealthStatus());
        assertTrue(mHealthy.getHealthScore() >= 75);

        // Trường hợp 2: Dự án chậm tiến độ nặng, nhiều task quá hạn -> CRITICAL
        BurndownData badBurndown = new BurndownData();
        badBurndown.setOnTrack(false);
        badBurndown.setVarianceTasks(8.0); // Chậm 8 việc

        List<Task> overdueTasks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Task t = new Task();
            t.setId(i);
            t.setStatus("IN_PROGRESS");
            t.setDueDate("2020-01-01"); // Quá hạn nhiều năm
            overdueTasks.add(t);
        }

        QualityMetrics mCritical = QualityVelocityCalculator.calculate(Collections.emptyList(), overdueTasks, Collections.emptyList(), badBurndown);
        assertEquals("CRITICAL", mCritical.getHealthStatus());
        assertTrue(mCritical.getHealthScore() < 50);
    }

    @Test
    @DisplayName("Dữ liệu rỗng không ném NullPointerException và giữ giá trị mặc định an toàn")
    void testEmptySubmissionsAndTasks() {
        QualityMetrics metrics = QualityVelocityCalculator.calculate(null, null);

        assertNotNull(metrics);
        assertEquals(0, metrics.getTotalSubmissions());
        assertEquals(0, metrics.getTotalReviewedTasks());
        assertEquals(100.0, metrics.getFirstTimePassRate());
        assertEquals(0.0, metrics.getAvgResponseHours());
        assertEquals(100, metrics.getHealthScore());
        assertEquals("HEALTHY", metrics.getHealthStatus());
    }
}
