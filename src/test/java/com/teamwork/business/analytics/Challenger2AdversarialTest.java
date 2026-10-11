package com.teamwork.business.analytics;

import com.teamwork.business.Task;
import com.teamwork.business.TaskSubmission;
import com.teamwork.business.User;
import com.teamwork.business.UserWorkload;
import com.teamwork.business.automation.AutomationEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adversarial Challenger 2 Test Suite for Enterprise Features:
 * - R2: Burndown Engine & Quality Velocity & Workload Overload
 * - R3: Automation Recipe Trigger Boundaries & Idempotency
 *
 * Implements rigorous boundary value analysis (BVA), equivalence partitioning,
 * stress harnesses, and negative test cases.
 */
@DisplayName("Challenger 2: Adversarial Stress Test Suite (R2 Analytics & R3 Automations)")
class Challenger2AdversarialTest {

    private LocalDate today;

    @BeforeEach
    void setUp() {
        AutomationEngine.clearAutomationGuard();
        today = LocalDate.of(2026, 10, 15);
    }

    @AfterEach
    void tearDown() {
        AutomationEngine.clearAutomationGuard();
    }

    // =========================================================================
    // 1. BURNDOWN EDGE CASES & ADVERSARIAL BOUNDARIES
    // =========================================================================
    @Nested
    @DisplayName("1. Burndown Engine Adversarial Edge Cases")
    class BurndownEdgeCases {

        @Test
        @DisplayName("1.1 Dự án 0 công việc (Empty tasks list / null)")
        void testBurndownZeroTasks() {
            LocalDate start = LocalDate.of(2026, 10, 1);
            LocalDate end = LocalDate.of(2026, 10, 10);

            // Thử nghiệm với empty list
            BurndownData dataEmpty = BurndownCalculator.calculate(start, end, Collections.emptyList(), start);
            assertNotNull(dataEmpty);
            assertEquals(0, dataEmpty.getTotalTasks());
            assertEquals(0, dataEmpty.getCompletedTasks());
            assertEquals(0, dataEmpty.getRemainingTasks());
            assertEquals(0, dataEmpty.getCompletionRate());
            assertEquals(10, dataEmpty.getLabels().size());
            assertEquals(0.0, dataEmpty.getIdealRemaining().get(0));
            assertEquals(0.0, dataEmpty.getActualRemaining().get(0));
            assertTrue(dataEmpty.isOnTrack(), "0 task phải luôn được coi là On-Track");
            assertEquals(0.0, dataEmpty.getVarianceTasks());

            // Thử nghiệm với null list
            BurndownData dataNull = BurndownCalculator.calculate(start, end, null, start);
            assertNotNull(dataNull);
            assertEquals(0, dataNull.getTotalTasks());
            assertEquals(0, dataNull.getRemainingTasks());
            assertTrue(dataNull.isOnTrack());
        }

        @Test
        @DisplayName("1.2 Dự án 100% hoàn thành ngay ngày đầu tiên (Day 1 All Done)")
        void testBurndownAllDoneDayOne() {
            LocalDate start = LocalDate.of(2026, 10, 1);
            LocalDate end = LocalDate.of(2026, 10, 10);
            LocalDate asOfDate = LocalDate.of(2026, 10, 5); // Đang ở ngày 5

            List<Task> tasks = new ArrayList<>();
            for (int i = 1; i <= 8; i++) {
                Task t = new Task();
                t.setId(i);
                t.setStatus("DONE");
                t.setReviewedAt("01/10/2026 10:00"); // Nghiệm thu ngay ngày 1
                tasks.add(t);
            }

            BurndownData data = BurndownCalculator.calculate(start, end, tasks, asOfDate);
            assertEquals(8, data.getTotalTasks());
            assertEquals(8, data.getCompletedTasks());
            assertEquals(0, data.getRemainingTasks());
            assertEquals(100, data.getCompletionRate());
            assertTrue(data.isOnTrack(), "Hoàn thành sớm phải On-Track");
            assertTrue(data.getVarianceTasks() < 0, "Variance phải âm (vượt tiến độ)");

            // Ngày 0 (01/10): Số việc thực tế còn lại phải là 0
            assertEquals(0.0, data.getActualRemaining().get(0));
            // Ngày 4 (05/10 - asOfDate): Số việc thực tế còn lại phải là 0
            assertEquals(0.0, data.getActualRemaining().get(4));
            // Các ngày trong tương lai (sau 05/10) phải là null để ngắt đường vẽ
            assertNull(data.getActualRemaining().get(5));
            assertNull(data.getActualRemaining().get(9));
        }

        @Test
        @DisplayName("1.3 Dự án toàn bộ công việc đều bị quá hạn (All Overdue Tasks)")
        void testBurndownAllOverdueTasks() {
            LocalDate start = LocalDate.of(2026, 9, 1);
            LocalDate end = LocalDate.of(2026, 9, 20); // Đã kết thúc tháng 9
            LocalDate asOfDate = LocalDate.of(2026, 10, 15); // Hiện tại đã qua hạn kết thúc dự án

            List<Task> tasks = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                Task t = new Task();
                t.setId(i);
                t.setStatus("IN_PROGRESS");
                t.setDueDate("2026-09-15");
                tasks.add(t);
            }

            BurndownData data = BurndownCalculator.calculate(start, end, tasks, asOfDate);
            assertEquals(6, data.getTotalTasks());
            assertEquals(0, data.getCompletedTasks());
            assertEquals(6, data.getRemainingTasks());
            assertEquals(0, data.getCompletionRate());
            assertFalse(data.isOnTrack(), "Tất cả công việc quá hạn thì không thể On-Track");
            assertEquals(6.0, data.getVarianceTasks()); // Chậm 6 việc so với lý tưởng 0

            // Do asOfDate sau projectEnd, tất cả 20 ngày của dự án đều phải có số liệu thực tế = 6.0 (không có ngày nào bị null)
            assertEquals(20, data.getActualRemaining().size());
            for (Double act : data.getActualRemaining()) {
                assertNotNull(act, "Không được null vì toàn bộ các ngày đều nằm trong quá khứ");
                assertEquals(6.0, act);
            }
        }

        @Test
        @DisplayName("1.4a Dự án kéo dài đúng 1 ngày (start == end)")
        void testBurndownSingleDayProject() {
            LocalDate day = LocalDate.of(2026, 10, 20);

            List<Task> tasks = new ArrayList<>();
            Task t1 = new Task();
            t1.setId(1);
            t1.setStatus("DONE");
            t1.setReviewedAt("20/10/2026 11:00");
            tasks.add(t1);

            Task t2 = new Task();
            t2.setId(2);
            t2.setStatus("TODO");
            tasks.add(t2);

            BurndownData data = BurndownCalculator.calculate(day, day, tasks, day);
            assertEquals(2, data.getTotalTasks());
            assertEquals(1, data.getCompletedTasks());
            assertEquals(1, data.getRemainingTasks());
            assertEquals(1, data.getLabels().size());
            assertEquals(0.0, data.getIdealRemaining().get(0)); // Lý tưởng ngày duy nhất là về 0
            assertEquals(1.0, data.getActualRemaining().get(0));
            assertFalse(data.isOnTrack(), "Còn 1 việc ở ngày duy nhất -> trễ hạn");
        }

        @Test
        @DisplayName("1.4b Dự án kéo dài 1 năm (365 ngày) - Kích hoạt ngưỡng chặn an toàn 180 ngày")
        void testBurndownOneYearSpanCap180Days() {
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 12, 31); // 365 ngày
            LocalDate asOfDate = LocalDate.of(2026, 3, 1);

            List<Task> tasks = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                Task t = new Task();
                t.setId(i);
                t.setStatus("IN_PROGRESS");
                tasks.add(t);
            }

            BurndownData data = BurndownCalculator.calculate(start, end, tasks, asOfDate);
            assertNotNull(data);
            // Kiểm tra ngưỡng an toàn: không vượt quá 180 nhãn
            assertEquals(180, data.getLabels().size(), "Tổng số ngày biểu đồ phải bị chặn tối đa 180 ngày");
            assertEquals(180, data.getIdealRemaining().size());
            assertEquals(180, data.getActualRemaining().size());
            assertEquals(start.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE), data.getProjectStartDate());
            assertEquals(start.plusDays(179).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE), data.getProjectEndDate());
        }

        @Test
        @DisplayName("1.5 Ngày kết thúc trước ngày bắt đầu (Inverted Dates: end < start)")
        void testBurndownInvertedDatesAutoCorrection() {
            LocalDate start = LocalDate.of(2026, 10, 20);
            LocalDate end = LocalDate.of(2026, 10, 10); // end trước start 10 ngày

            List<Task> tasks = new ArrayList<>();
            Task t = new Task();
            t.setId(1);
            t.setStatus("TODO");
            tasks.add(t);

            BurndownData data = BurndownCalculator.calculate(start, end, tasks, start);
            assertNotNull(data);
            // Hệ thống phải tự động điều chỉnh projectEnd = start + 7 ngày
            assertEquals(8, data.getLabels().size(), "Khoảng thời gian nghịch đảo phải tự phục hồi thành start + 7 ngày (8 nhãn)");
            assertEquals(start.plusDays(7).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE), data.getProjectEndDate());
            assertFalse(data.getLabels().isEmpty());
        }
    }

    // =========================================================================
    // 2. FIRST-TIME PASS RATE (FTPR) EDGE CASES
    // =========================================================================
    @Nested
    @DisplayName("2. First-Time Pass Rate (FTPR) Adversarial Edge Cases")
    class FirstTimePassRateEdgeCases {

        @Test
        @DisplayName("2.1 Không có bài nộp nào (0 Submissions)")
        void testFtprZeroSubmissions() {
            QualityMetrics metrics = QualityVelocityCalculator.calculate(Collections.emptyList(), Collections.emptyList());
            assertNotNull(metrics);
            assertEquals(0, metrics.getTotalSubmissions());
            assertEquals(0, metrics.getTotalReviewedTasks());
            assertEquals(0, metrics.getFirstTimePassedCount());
            assertEquals(0, metrics.getRevisionRequiredCount());
            assertEquals(0, metrics.getPendingReviewCount());
            assertEquals(100.0, metrics.getFirstTimePassRate(), "Khi chưa có bài nộp, FTPR mặc định là 100.0% an toàn");
            assertEquals(0.0, metrics.getAvgResponseHours());
            assertEquals(100, metrics.getHealthScore());
            assertEquals("HEALTHY", metrics.getHealthStatus());
        }

        @Test
        @DisplayName("2.2 100% duyệt đạt ngay lần đầu (All First-Time Approvals)")
        void testFtpr100PercentFirstTimeApprovals() {
            List<TaskSubmission> subs = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                TaskSubmission s = new TaskSubmission(i, 1, 10, "Bàn giao Task " + i, "", "10/10/2026 08:00");
                s.setStatus("APPROVED");
                s.setReviewedAt("10/10/2026 09:30");
                s.setQualityRating(5);
                subs.add(s);
            }

            QualityMetrics metrics = QualityVelocityCalculator.calculate(subs, Collections.emptyList());
            assertEquals(5, metrics.getTotalReviewedTasks());
            assertEquals(5, metrics.getFirstTimePassedCount());
            assertEquals(0, metrics.getRevisionRequiredCount());
            assertEquals(0, metrics.getPendingReviewCount());
            assertEquals(100.0, metrics.getFirstTimePassRate());
            assertEquals(5.0, metrics.getAvgQualityRating());
        }

        @Test
        @DisplayName("2.3 100% yêu cầu nộp lại / từ chối (0% First-Time Pass Rate)")
        void testFtprZeroPercentAllRevisions() {
            List<TaskSubmission> subs = new ArrayList<>();

            // Task 1: Rev 1 REVISE, Rev 2 APPROVED
            TaskSubmission s1_1 = new TaskSubmission(1, 1, 10, "Nộp 1", "", "10/10/2026 08:00");
            s1_1.setStatus("REVISE");
            s1_1.setReviewedAt("10/10/2026 09:00");
            subs.add(s1_1);
            TaskSubmission s1_2 = new TaskSubmission(1, 2, 10, "Nộp 2", "", "11/10/2026 08:00");
            s1_2.setStatus("APPROVED");
            s1_2.setReviewedAt("11/10/2026 09:00");
            subs.add(s1_2);

            // Task 2: Rev 1 REJECTED, Rev 2 APPROVED
            TaskSubmission s2_1 = new TaskSubmission(2, 1, 10, "Nộp 1", "", "10/10/2026 08:00");
            s2_1.setStatus("REJECTED");
            s2_1.setReviewedAt("10/10/2026 09:00");
            subs.add(s2_1);
            TaskSubmission s2_2 = new TaskSubmission(2, 2, 10, "Nộp 2", "", "11/10/2026 08:00");
            s2_2.setStatus("APPROVED");
            s2_2.setReviewedAt("11/10/2026 09:00");
            subs.add(s2_2);

            QualityMetrics metrics = QualityVelocityCalculator.calculate(subs, Collections.emptyList());
            assertEquals(2, metrics.getTotalReviewedTasks());
            assertEquals(0, metrics.getFirstTimePassedCount(), "Không có công việc nào qua ở Revision 1");
            assertEquals(2, metrics.getRevisionRequiredCount());
            assertEquals(0.0, metrics.getFirstTimePassRate(), "Tỷ lệ phải là 0.0%");
        }

        @Test
        @DisplayName("2.4 Đan xen nhiều trường hợp (Mixed Revisions: 2 đạt, 2 sửa/từ chối = 50.0%)")
        void testFtprMixedRevisions() {
            List<TaskSubmission> subs = new ArrayList<>();

            // Task 1: APPROVED ở Rev 1
            TaskSubmission s1 = new TaskSubmission(1, 1, 10, "T1", "", "10/10/2026 08:00");
            s1.setStatus("APPROVED");
            subs.add(s1);

            // Task 2: REVISE ở Rev 1, sau đó Rev 2 APPROVED
            TaskSubmission s2_1 = new TaskSubmission(2, 1, 10, "T2 r1", "", "10/10/2026 08:00");
            s2_1.setStatus("REVISE");
            subs.add(s2_1);
            TaskSubmission s2_2 = new TaskSubmission(2, 2, 10, "T2 r2", "", "11/10/2026 08:00");
            s2_2.setStatus("APPROVED");
            subs.add(s2_2);

            // Task 3: REJECTED ở Rev 1 (chưa nộp lại)
            TaskSubmission s3 = new TaskSubmission(3, 1, 10, "T3", "", "10/10/2026 08:00");
            s3.setStatus("REJECTED");
            subs.add(s3);

            // Task 4: APPROVED ở Rev 1
            TaskSubmission s4 = new TaskSubmission(4, 1, 10, "T4", "", "10/10/2026 08:00");
            s4.setStatus("APPROVED");
            subs.add(s4);

            QualityMetrics metrics = QualityVelocityCalculator.calculate(subs, Collections.emptyList());
            assertEquals(4, metrics.getTotalReviewedTasks());
            assertEquals(2, metrics.getFirstTimePassedCount());
            assertEquals(2, metrics.getRevisionRequiredCount());
            assertEquals(50.0, metrics.getFirstTimePassRate());
        }

        @Test
        @DisplayName("2.5 Bài nộp đang chờ duyệt (Pending Submissions) không làm loãng mẫu số FTPR")
        void testFtprPendingSubmissionsExcludedFromDenominator() {
            List<TaskSubmission> subs = new ArrayList<>();

            // Task 1: Đã duyệt đạt
            TaskSubmission s1 = new TaskSubmission(1, 1, 10, "T1", "", "10/10/2026 08:00");
            s1.setStatus("APPROVED");
            subs.add(s1);

            // Task 2: Đang chờ duyệt (SUBMITTED)
            TaskSubmission s2 = new TaskSubmission(2, 1, 10, "T2", "", "12/10/2026 08:00");
            s2.setStatus("SUBMITTED");
            subs.add(s2);

            // Task 3: Đang chờ duyệt (SUBMITTED)
            TaskSubmission s3 = new TaskSubmission(3, 1, 10, "T3", "", "12/10/2026 08:00");
            s3.setStatus("SUBMITTED");
            subs.add(s3);

            QualityMetrics metrics = QualityVelocityCalculator.calculate(subs, Collections.emptyList());
            assertEquals(1, metrics.getTotalReviewedTasks(), "Mẫu số chỉ tính việc đã được PM thẩm định");
            assertEquals(1, metrics.getFirstTimePassedCount());
            assertEquals(2, metrics.getPendingReviewCount());
            assertEquals(100.0, metrics.getFirstTimePassRate(), "Tỷ lệ không được bị giảm bởi các bài chờ duyệt");
        }

        @Test
        @DisplayName("2.6 Đợt nộp bị đảo lộn thứ tự (Out of order revision numbers)")
        void testFtprOutOfOrderRevisionNumbers() {
            List<TaskSubmission> subs = new ArrayList<>();

            // Nộp Rev 2 trước Rev 1 trong danh sách đưa vào
            TaskSubmission s1_2 = new TaskSubmission(1, 2, 10, "Rev 2", "", "11/10/2026 08:00");
            s1_2.setStatus("APPROVED");
            subs.add(s1_2);

            TaskSubmission s1_1 = new TaskSubmission(1, 1, 10, "Rev 1", "", "10/10/2026 08:00");
            s1_1.setStatus("REVISE");
            subs.add(s1_1);

            QualityMetrics metrics = QualityVelocityCalculator.calculate(subs, Collections.emptyList());
            // Thuật toán phải tự sort theo revisionNumber để bắt đúng Rev 1 bị REVISE
            assertEquals(1, metrics.getTotalReviewedTasks());
            assertEquals(0, metrics.getFirstTimePassedCount());
            assertEquals(1, metrics.getRevisionRequiredCount());
            assertEquals(0.0, metrics.getFirstTimePassRate());
        }
    }

    // =========================================================================
    // 3. RECIPE BOUNDARY CONDITIONS & IDEMPOTENCY
    // =========================================================================
    @Nested
    @DisplayName("3. Automation Recipe Boundaries & Idempotency")
    class AutomationRecipeBoundaries {

        @Test
        @DisplayName("3.1 Recipe 1: Điểm biên tiến độ 99% vs 100% vs >100%")
        void testRecipe1ProgressBoundaries() {
            // Dưới 100% -> không kích hoạt
            assertFalse(AutomationEngine.shouldPromptGate2(true, true, 99, "IN_PROGRESS"), "99% không được kích hoạt");
            assertFalse(AutomationEngine.shouldPromptGate2(true, true, 0, "IN_PROGRESS"), "0% không được kích hoạt");
            assertFalse(AutomationEngine.shouldPromptGate2(true, true, -10, "IN_PROGRESS"), "Tiến độ âm không được kích hoạt");

            // Đạt 100% -> kích hoạt
            assertTrue(AutomationEngine.shouldPromptGate2(true, true, 100, "IN_PROGRESS"), "100% phải kích hoạt");
            assertTrue(AutomationEngine.shouldPromptGate2(true, true, 105, "IN_PROGRESS"), "Tiến độ >= 100% phải kích hoạt");

            // Các điều kiện loại trừ khác
            assertFalse(AutomationEngine.shouldPromptGate2(false, true, 100, "IN_PROGRESS"), "Bị tắt không kích hoạt");
            assertFalse(AutomationEngine.shouldPromptGate2(true, false, 100, "IN_PROGRESS"), "Không có Gate không kích hoạt");
            assertFalse(AutomationEngine.shouldPromptGate2(true, true, 100, "DONE"), "Đã DONE không kích hoạt");
            assertFalse(AutomationEngine.shouldPromptGate2(true, true, 100, "SUBMITTED"), "Đã SUBMITTED không kích hoạt");
            assertFalse(AutomationEngine.shouldPromptGate2(true, true, 100, null), "Status null không kích hoạt");
        }

        @Test
        @DisplayName("3.2 Recipe 3: Điểm biên tiến độ 49% vs 50% vs 51%")
        void testRecipe3ProgressBoundaries49Vs50() {
            String dueIso = today.toString();

            // 49% < 50% -> Kích hoạt nâng ưu tiên
            assertTrue(AutomationEngine.shouldEscalatePriority(true, dueIso, 49, "LOW", today, "IN_PROGRESS"));
            assertTrue(AutomationEngine.shouldEscalatePriority(true, dueIso, 0, "LOW", today, "TODO"));

            // 50% và 51% -> Không kích hoạt
            assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 50, "LOW", today, "IN_PROGRESS"),
                    "Đúng 50% tiến độ không được nâng ưu tiên");
            assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 51, "LOW", today, "IN_PROGRESS"),
                    "51% tiến độ không được nâng ưu tiên");
        }

        @Test
        @DisplayName("3.3 Recipe 3: Điểm biên ngày hạn chót hôm nay vs ngày mai vs hôm qua")
        void testRecipe3DueDateBoundariesTodayVsTomorrow() {
            // Hạn chót hôm nay (<= today) -> Kích hoạt
            assertTrue(AutomationEngine.shouldEscalatePriority(true, today.toString(), 20, "MEDIUM", today, "IN_PROGRESS"),
                    "Hạn chót đúng hôm nay phải kích hoạt");

            // Hạn chót hôm qua (quá hạn, < today) -> Kích hoạt
            assertTrue(AutomationEngine.shouldEscalatePriority(true, today.minusDays(1).toString(), 20, "MEDIUM", today, "IN_PROGRESS"),
                    "Quá hạn hôm qua phải kích hoạt");

            // Hạn chót ngày mai (> today) -> Không kích hoạt
            assertFalse(AutomationEngine.shouldEscalatePriority(true, today.plusDays(1).toString(), 20, "MEDIUM", today, "IN_PROGRESS"),
                    "Hạn chót ngày mai chưa kích hoạt");

            // Hạn chót 1 tuần sau -> Không kích hoạt
            assertFalse(AutomationEngine.shouldEscalatePriority(true, today.plusDays(7).toString(), 20, "MEDIUM", today, "IN_PROGRESS"),
                    "Hạn chót tuần sau chưa kích hoạt");
        }

        @Test
        @DisplayName("3.4 Recipe 3: Tính lũy thừa (Idempotency) - Task đã là HIGH tuyệt đối không leo thang lại")
        void testRecipe3PriorityEscalationIdempotency() {
            String dueIso = today.toString();

            // Đã là HIGH (chữ hoa chuẩn)
            assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 10, "HIGH", today, "IN_PROGRESS"),
                    "HIGH không được leo thang tiếp");

            // Chữ thường hoặc khoảng trắng
            assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 10, "high", today, "IN_PROGRESS"),
                    "high chữ thường không được leo thang tiếp");
            assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 10, "  HIGH  ", today, "IN_PROGRESS"),
                    "HIGH có khoảng trắng không được leo thang tiếp");

            // Các mức ưu tiên khác chưa phải HIGH -> Được leo thang
            assertTrue(AutomationEngine.shouldEscalatePriority(true, dueIso, 10, "MEDIUM", today, "IN_PROGRESS"));
            assertTrue(AutomationEngine.shouldEscalatePriority(true, dueIso, 10, "LOW", today, "IN_PROGRESS"));
            assertTrue(AutomationEngine.shouldEscalatePriority(true, dueIso, 10, null, today, "IN_PROGRESS"),
                    "Priority null được leo thang lên HIGH");
            assertTrue(AutomationEngine.shouldEscalatePriority(true, dueIso, 10, "", today, "IN_PROGRESS"),
                    "Priority rỗng được leo thang lên HIGH");
        }

        @Test
        @DisplayName("3.5 Recipe 3: Bỏ qua tuyệt đối đối với các Task đã DONE hoặc SUBMITTED")
        void testRecipe3IgnoresDoneOrSubmittedTasks() {
            String dueIso = today.toString();
            assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 0, "LOW", today, "DONE"));
            assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 0, "LOW", today, "SUBMITTED"));
            assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 0, "LOW", today, "done"));
            assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 0, "LOW", today, "submitted"));
        }
    }

    // =========================================================================
    // 4. WORKLOAD BALANCE OVERLOAD THRESHOLDS
    // =========================================================================
    @Nested
    @DisplayName("4. Workload Balance Overload Thresholds")
    class WorkloadOverloadThresholds {

        @Test
        @DisplayName("4.1 Ngưỡng số việc đang làm: 4 việc (NORMAL) vs 5 việc (HIGH Overload)")
        void testWorkloadInProgressThreshold4Vs5() {
            User u1 = new User();
            u1.setId(1);
            u1.setFullName("Thành viên A");

            // Thành viên 1: 4 việc in-progress, 0 overdue -> Cân bằng (NORMAL)
            UserWorkload uw1 = new UserWorkload();
            uw1.setUser(u1);
            uw1.setInProgressCount(4);
            uw1.setOverdueCount(0);

            List<WorkloadAnalytics> res1 = WorkloadAnalytics.fromWorkloads(List.of(uw1));
            assertEquals(1, res1.size());
            assertFalse(res1.get(0).isOverloaded(), "4 việc in-progress chưa bị coi là quá tải");
            assertEquals("NORMAL", res1.get(0).getLoadLevel());
            assertEquals("Khối lượng công việc cân bằng", res1.get(0).getOverloadReason());

            // Thành viên 2: 5 việc in-progress, 0 overdue -> Quá tải (HIGH)
            User u2 = new User();
            u2.setId(2);
            u2.setFullName("Thành viên B");

            UserWorkload uw2 = new UserWorkload();
            uw2.setUser(u2);
            uw2.setInProgressCount(5);
            uw2.setOverdueCount(0);

            List<WorkloadAnalytics> res2 = WorkloadAnalytics.fromWorkloads(List.of(uw2));
            assertEquals(1, res2.size());
            assertTrue(res2.get(0).isOverloaded(), "5 việc in-progress phải kích hoạt quá tải");
            assertEquals("HIGH", res2.get(0).getLoadLevel());
            assertTrue(res2.get(0).getOverloadReason().contains("5 việc"), "Lý do phải ghi rõ 5 việc");
        }

        @Test
        @DisplayName("4.2 Ngưỡng việc quá hạn: Chỉ cần có 1 việc quá hạn là báo động đỏ (CRITICAL Overload)")
        void testWorkloadOverdueTaskTriggersCritical() {
            User u = new User();
            u.setId(3);
            u.setFullName("Thành viên C");

            // Chỉ có 1 việc in-progress nhưng lại bị quá hạn
            UserWorkload uw = new UserWorkload();
            uw.setUser(u);
            uw.setInProgressCount(1);
            uw.setOverdueCount(1);

            List<WorkloadAnalytics> res = WorkloadAnalytics.fromWorkloads(List.of(uw));
            assertEquals(1, res.size());
            WorkloadAnalytics wa = res.get(0);
            assertTrue(wa.isOverloaded());
            assertEquals("CRITICAL", wa.getLoadLevel(), "Có việc quá hạn phải gắn mức CRITICAL");
            assertTrue(wa.getOverloadReason().contains("quá hạn"));
        }

        @Test
        @DisplayName("4.3 Quá tải kép: Vừa >= 5 việc đang làm vừa có việc quá hạn -> Báo động đỏ kép")
        void testWorkloadDoubleOverload() {
            User u = new User();
            u.setId(4);
            u.setFullName("Thành viên D");

            UserWorkload uw = new UserWorkload();
            uw.setUser(u);
            uw.setInProgressCount(7);
            uw.setOverdueCount(2);

            List<WorkloadAnalytics> res = WorkloadAnalytics.fromWorkloads(List.of(uw));
            assertEquals(1, res.size());
            WorkloadAnalytics wa = res.get(0);
            assertTrue(wa.isOverloaded());
            assertEquals("CRITICAL", wa.getLoadLevel());
            assertTrue(wa.getOverloadReason().contains("Báo động đỏ"));
        }

        @Test
        @DisplayName("4.4 Điểm sức khỏe dự án trừ điểm thành viên quá tải (-5 điểm/người, tối đa -15)")
        void testHealthScoreOverloadPenalties() {
            User u = new User();
            u.setId(5);

            // 1 thành viên quá tải
            UserWorkload uw1 = new UserWorkload();
            uw1.setUser(u);
            uw1.setInProgressCount(5); // quá tải

            QualityMetrics m1 = QualityVelocityCalculator.calculate(Collections.emptyList(), Collections.emptyList(), List.of(uw1), null);
            assertEquals(95, m1.getHealthScore(), "1 thành viên quá tải bị trừ 5 điểm: 100 - 5 = 95");

            // 2 thành viên quá tải
            UserWorkload uw2 = new UserWorkload();
            uw2.setUser(u);
            uw2.setInProgressCount(6);

            QualityMetrics m2 = QualityVelocityCalculator.calculate(Collections.emptyList(), Collections.emptyList(), List.of(uw1, uw2), null);
            assertEquals(90, m2.getHealthScore(), "2 thành viên quá tải bị trừ 10 điểm: 100 - 10 = 90");

            // 4 thành viên quá tải (vượt trần -15)
            UserWorkload uw3 = new UserWorkload();
            uw3.setUser(u);
            uw3.setInProgressCount(7);
            UserWorkload uw4 = new UserWorkload();
            uw4.setUser(u);
            uw4.setInProgressCount(8);

            QualityMetrics m4 = QualityVelocityCalculator.calculate(Collections.emptyList(), Collections.emptyList(), List.of(uw1, uw2, uw3, uw4), null);
            assertEquals(85, m4.getHealthScore(), "Tối đa bị trừ 15 điểm quá tải: 100 - 15 = 85");
        }
    }
}
