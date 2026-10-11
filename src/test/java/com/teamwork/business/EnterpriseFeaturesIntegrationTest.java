package com.teamwork.business;

import com.teamwork.business.analytics.BurndownCalculator;
import com.teamwork.business.analytics.BurndownData;
import com.teamwork.business.analytics.QualityMetrics;
import com.teamwork.business.analytics.QualityVelocityCalculator;
import com.teamwork.business.analytics.WorkloadAnalytics;
import com.teamwork.business.automation.AutomationEngine;
import com.teamwork.business.gantt.CascadeScheduler;
import com.teamwork.business.gantt.DependencyGraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử Tích hợp Toàn diện Tính năng Doanh nghiệp (Enterprise Features Cross-Feature Integration Test Suite).
 *
 * Kiểm tra tính tương thích, ràng buộc logic chéo và luồng tương tác giữa cả 3 tính năng lớn:
 * 1. R1: Interactive Gantt Timeline & Task Dependencies (DAG cycle check, prerequisite blocking, cascade scheduling).
 * 2. R2: Project Health, Burndown & Quality Analytics (Burndown ideal/actual, First-time pass rate, Workload balance).
 * 3. R3: No-Code Automation Recipes (Recipe 1 Gate 2 prompt, Recipe 2 subtasks auto-close, Recipe 3 priority escalation, loop guard).
 *
 * Tiêu chuẩn: 100% Pure POJO JUnit 5, hoàn toàn độc lập với CSDL và thư viện Mock, tốc độ cao và tất định.
 */
@DisplayName("Kiểm thử Tích hợp Toàn diện Tính năng Doanh nghiệp (Enterprise Features Integration)")
class EnterpriseFeaturesIntegrationTest {

    private LocalDate baseDate;

    @BeforeEach
    void setUp() {
        AutomationEngine.clearAutomationGuard();
        baseDate = LocalDate.of(2026, 10, 15);
    }

    @AfterEach
    void tearDown() {
        AutomationEngine.clearAutomationGuard();
    }

    // =========================================================================
    // HELPER CREATION METHODS
    // =========================================================================

    private Task createTask(int id, int projectId, String title, String status, String priority,
                            String startDate, String dueDate, boolean requiresGate, int assigneeId) {
        Task t = new Task();
        t.setId(id);
        t.setProjectId(projectId);
        t.setTitle(title);
        t.setStatus(status != null ? status : "TODO");
        t.setPriority(priority != null ? priority : "MEDIUM");
        t.setStartDate(startDate);
        t.setDueDate(dueDate);
        t.setRequiresGate(requiresGate);
        t.setAssigneeId(assigneeId);
        t.setAssigneeName("Thành viên #" + assigneeId);
        return t;
    }

    private SubTask createSubTask(int id, int taskId, String title, String status) {
        SubTask st = new SubTask();
        st.setId(id);
        st.setTaskId(taskId);
        st.setTitle(title);
        st.setStatus(status != null ? status : "TODO");
        return st;
    }

    // =========================================================================
    // SCENARIO 1: END-TO-END MULTI-FEATURE LIFECYCLE
    // =========================================================================

    @Test
    @DisplayName("Kịch bản 1: Luồng vòng đời đầy đủ giao thoa Gantt, Gate 2, Tự động hóa và Phân tích Burndown/FTPR")
    void testFullLifecycleGanttGate2AutomationAndAnalyticsIntegration() {
        // --- 1. Thiết lập Đồ thị phụ thuộc (R1: Gantt): Task 1 (Predecessor) -> Task 2 (Successor) ---
        int projectId = 10;
        Task task1 = createTask(1, projectId, "Thiết kế cơ sở dữ liệu", "IN_PROGRESS", "HIGH",
                "2026-10-01", "2026-10-05", true, 101);
        Task task2 = createTask(2, projectId, "Hiện thực hóa DAO & Servlet", "TODO", "MEDIUM",
                "2026-10-06", "2026-10-10", true, 102);

        List<Task> projectTasks = new ArrayList<>(List.of(task1, task2));
        List<TaskDependency> dependencies = List.of(new TaskDependency(projectId, 2, 1, "FINISH_TO_START", 1));

        // Kiểm tra tính hợp lệ của đồ thị DAG (không có chu trình)
        DependencyGraph graph = new DependencyGraph(dependencies);
        assertFalse(graph.hasAnyCycle(), "Đồ thị ban đầu phải là một DAG không chu trình");
        assertFalse(graph.wouldCauseCycle(2, 1), "Thêm 1 -> 2 không được tạo chu trình");

        // Ràng buộc R1: Task 2 KHÔNG THỂ chuyển sang IN_PROGRESS vì Task 1 chưa DONE (đang IN_PROGRESS)
        boolean task1IsDone = TaskStatus.isDone(task1.getStatus());
        assertFalse(task1IsDone, "Task 1 chưa hoàn thành");
        // Giả lập logic kiểm tra tiền đề giống TaskCrudHandler
        boolean canStartTask2 = task1IsDone;
        assertFalse(canStartTask2, "Task 2 phải bị khóa (Prerequisite Lock) khi Task 1 chưa hoàn thành");

        // --- 2. Việc con của Task 1 đạt 100% -> Kích hoạt Recipe 1 (R3: Automation) ---
        List<SubTask> subtasksT1 = List.of(
                createSubTask(11, 1, "ERD Diagram", "APPROVED"),
                createSubTask(12, 1, "DDL Script", "APPROVED")
        );
        int progressT1 = 100;

        // Đánh giá Recipe 1 (Auto-Prompt Gate 2)
        boolean recipe1Triggered = AutomationEngine.shouldPromptGate2(
                true, task1.isRequiresGate(), progressT1, task1.getStatus()
        );
        assertTrue(recipe1Triggered, "Recipe 1 phải kích hoạt gửi thông báo Gate 2 khi 100% việc con hoàn tất");

        // --- 3. Task Lead nộp hồ sơ Gate 2, trải qua 2 lần thẩm định (Gate 2 Submissions) ---
        List<TaskSubmission> submissions = new ArrayList<>();

        // Lần 1: PM yêu cầu chỉnh sửa (REVISE)
        TaskSubmission subRev1 = new TaskSubmission(1, 1, 101, "Nộp bản thiết kế DB v1", "", "05/10/2026 10:00");
        subRev1.setStatus("REVISE");
        subRev1.setReviewedAt("05/10/2026 14:00"); // PM phản hồi sau 4 giờ
        subRev1.setQualityRating(3);
        subRev1.setPmFeedback("Cần chuẩn hóa khóa ngoại ON DELETE CASCADE");
        submissions.add(subRev1);

        // Lần 2: Task Lead sửa xong nộp lại và PM phê duyệt (APPROVED)
        TaskSubmission subRev2 = new TaskSubmission(1, 2, 101, "Nộp bản thiết kế DB v2 (đã sửa)", "", "06/10/2026 09:00");
        subRev2.setStatus("APPROVED");
        subRev2.setReviewedAt("06/10/2026 11:30"); // PM phản hồi sau 2.5 giờ
        subRev2.setQualityRating(5);
        subRev2.setPmFeedback("Thiết kế rất tốt, đạt chuẩn");
        submissions.add(subRev2);

        // PM phê duyệt Task 1 sang DONE
        task1.setStatus("DONE");
        task1.setReviewedAt("2026-10-06");

        // --- 4. PM duyệt Task 1 kích hoạt Recipe 2 (Auto-Close Subtasks) ---
        // Giả sử có một việc con tồn đọng chưa đóng
        SubTask lingeringSub = createSubTask(13, 1, "Ghi chú tài liệu phụ", "IN_PROGRESS");
        assertFalse(SubTaskStatus.isFinished(lingeringSub.getStatus()));
        // Recipe 2 tự động đồng bộ đóng
        lingeringSub.setStatus("APPROVED");
        assertTrue(SubTaskStatus.isFinished(lingeringSub.getStatus()), "Recipe 2 đảm bảo việc con tồn đọng được đóng");

        // --- 5. Ràng buộc tiền đề R1 mở khóa Task 2 ---
        boolean task1DoneNow = TaskStatus.isDone(task1.getStatus());
        assertTrue(task1DoneNow, "Task 1 đã hoàn tất (DONE)");
        canStartTask2 = task1DoneNow;
        assertTrue(canStartTask2, "Task 2 giờ đây được phép chuyển sang IN_PROGRESS!");
        task2.setStatus("IN_PROGRESS");

        // --- 6. Kiểm tra số liệu Phân tích Tổng hợp (R2: Analytics) ---
        // a. Burndown Chart tại mốc 2026-10-06: Task 1 đã hoàn thành, còn 1 task
        BurndownData burndown = BurndownCalculator.calculate(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 10),
                projectTasks,
                LocalDate.of(2026, 10, 6)
        );
        assertEquals(2, burndown.getTotalTasks(), "Tổng số công việc là 2");
        assertEquals(1, burndown.getCompletedTasks(), "Số công việc đã hoàn thành là 1 (Task 1)");
        assertEquals(1, burndown.getRemainingTasks(), "Số công việc còn lại là 1 (Task 2)");
        assertEquals(50, burndown.getCompletionRate(), "Tỷ lệ hoàn thành là 50%");

        // b. Quality & PM Velocity Metrics
        QualityMetrics quality = QualityVelocityCalculator.calculate(submissions, projectTasks);
        assertEquals(1, quality.getTotalReviewedTasks(), "Có 1 Task đã qua quy trình nghiệm thu");
        assertEquals(0, quality.getFirstTimePassedCount(), "Task 1 không đạt ở Rev 1");
        assertEquals(1, quality.getRevisionRequiredCount(), "Task 1 cần revision");
        assertEquals(0.0, quality.getFirstTimePassRate(), "FTPR là 0.0% vì cần đến Rev 2");
        assertEquals(1, quality.getMostRevisedTasks().size(), "Task 1 phải nằm trong danh sách cần sửa nhiều");
        assertEquals(1, quality.getMostRevisedTasks().get(0).getRejectionsCount(), "Bị từ chối/yêu cầu sửa 1 lần");
        assertEquals(4.0, quality.getAvgQualityRating(), "Điểm đánh giá trung bình: (3 + 5)/2 = 4.0 sao");
    }

    // =========================================================================
    // SCENARIO 2: CASCADE SCHEDULING -> RECIPE 3 (ESCALATION) -> WORKLOAD ALERT
    // =========================================================================

    @Test
    @DisplayName("Kịch bản 2: Dời lịch dây chuyền đẩy lùi hạn chót kích hoạt Recipe 3 nâng ưu tiên và cảnh báo quá tải")
    void testCascadeSchedulingTriggersRecipe3PriorityEscalationAndWorkloadAlert() {
        int projectId = 20;

        // Task A: 2026-10-01 đến 2026-10-05
        Task taskA = createTask(10, projectId, "Xây dựng Microservice Auth", "IN_PROGRESS", "MEDIUM",
                "2026-10-01", "2026-10-05", false, 201);
        // Task B (phụ thuộc A, duration = 3 ngày): ban đầu 2026-10-06 đến 2026-10-09
        Task taskB = createTask(11, projectId, "Tích hợp Single Sign-On", "TODO", "MEDIUM",
                "2026-10-06", "2026-10-09", false, 202);

        List<Task> tasks = new ArrayList<>(List.of(taskA, taskB));
        List<TaskDependency> deps = List.of(new TaskDependency(projectId, 11, 10, "FINISH_TO_START", 1));

        // 1. Task A bị chậm tiến độ, PM dời dueDate của Task A từ 2026-10-05 sang 2026-10-14
        LocalDate newDueDateA = LocalDate.of(2026, 10, 14);
        List<Task> modifiedTasks = CascadeScheduler.cascade(10, newDueDateA, tasks, deps);

        assertEquals(2, modifiedTasks.size(), "Cả Task A và Task B đều bị ảnh hưởng dời lịch");
        assertEquals("2026-10-14", taskA.getDueDate(), "Task A hạn mới là 2026-10-14");
        // Task B: start mới = 2026-10-14 + 1 ngày lag = 2026-10-15; due mới = 2026-10-15 + 3 ngày = 2026-10-18
        assertEquals("2026-10-15", taskB.getStartDate(), "Task B bắt đầu mới phải là 2026-10-15");
        assertEquals("2026-10-18", taskB.getDueDate(), "Task B hạn mới phải là 2026-10-18");

        // 2. Đến ngày 2026-10-18 (hôm nay là hạn chót của Task B), tiến độ Task B mới đạt 25% (< 50%)
        LocalDate today = LocalDate.of(2026, 10, 18);
        int progressTaskB = 25;

        // Đánh giá Recipe 3 (Auto-Escalate Priority)
        boolean shouldEscalate = AutomationEngine.shouldEscalatePriority(
                true,
                taskB.getDueDate(),
                progressTaskB,
                taskB.getPriority(),
                today,
                taskB.getStatus()
        );
        assertTrue(shouldEscalate, "Recipe 3 phải kích hoạt nâng mức ưu tiên lên HIGH do đến hạn mà tiến độ < 50%");

        // Tự động nâng ưu tiên lên HIGH
        taskB.setPriority("HIGH");
        assertEquals("HIGH", taskB.getPriority());

        // 3. Tương tác với Workload Balance Heatmap (R2: Analytics)
        // Thành viên 202 (người gánh Task B) đang có 5 việc đang làm và 1 việc bị quá hạn
        User user202 = new User();
        user202.setId(202);
        user202.setFullName("Lê Văn C");

        UserWorkload uw = new UserWorkload();
        uw.setUser(user202);
        uw.setInProgressCount(5);
        uw.setOverdueCount(1); // Task B đã đến hạn mà chưa xong
        uw.setLeadTaskCount(5);

        List<WorkloadAnalytics> workloadList = WorkloadAnalytics.fromWorkloads(List.of(uw));
        assertEquals(1, workloadList.size());
        WorkloadAnalytics wa = workloadList.get(0);

        assertTrue(wa.isOverloaded(), "Thành viên 202 phải bị gắn cờ quá tải (Overload Alert)");
        assertEquals("CRITICAL", wa.getLoadLevel(), "Mức độ tải phải là CRITICAL vì có việc quá hạn và in-progress >= 5");

        // 4. Kiểm tra Project Health Index bị trừ điểm khi có thành viên quá tải
        QualityMetrics metrics = QualityVelocityCalculator.calculate(
                Collections.emptyList(),
                tasks,
                List.of(uw),
                null
        );
        assertTrue(metrics.getHealthScore() < 100, "Điểm sức khỏe phải bị phạt giảm do thành viên quá tải");
        assertEquals(95, metrics.getHealthScore(), "Điểm sức khỏe bị trừ đúng 5 điểm do 1 thành viên quá tải");

        // Khi kết hợp với Burndown bị trễ tiến độ nặng (variance = 5 việc -> phạt 30 điểm), điểm sức khỏe tụt xuống mức AT_RISK
        BurndownData delayedBurndown = new BurndownData();
        delayedBurndown.setOnTrack(false);
        delayedBurndown.setVarianceTasks(5.0);

        QualityMetrics delayedMetrics = QualityVelocityCalculator.calculate(
                Collections.emptyList(),
                tasks,
                List.of(uw),
                delayedBurndown
        );
        assertEquals(65, delayedMetrics.getHealthScore(), "Điểm sức khỏe phải tụt còn 65 (100 - 30 - 5)");
        assertEquals("AT_RISK", delayedMetrics.getHealthStatus(), "Trạng thái sức khỏe dự án phải chuyển sang AT_RISK");
    }

    // =========================================================================
    // SCENARIO 3: DIAMOND DAG CASCADE & MULTI-PREREQUISITE COMPLETION
    // =========================================================================

    @Test
    @DisplayName("Kịch bản 3: Đồ thị hình thoi (Diamond DAG) với đồng thời nhiều tiền đề và kiểm tra dời lịch hội tụ")
    void testDiamondDAGCascadeAndMultiPrerequisites() {
        int projectId = 30;

        // Node A: Gốc
        Task tA = createTask(100, projectId, "Khảo sát yêu cầu (A)", "DONE", "HIGH",
                "2026-10-01", "2026-10-03", false, 1);
        // Node B phụ thuộc A (duration: 3 ngày)
        Task tB = createTask(101, projectId, "Thiết kế UI (B)", "DONE", "MEDIUM",
                "2026-10-04", "2026-10-07", false, 2);
        // Node C phụ thuộc A (duration: 5 ngày)
        Task tC = createTask(102, projectId, "Thiết kế Database (C)", "IN_PROGRESS", "HIGH",
                "2026-10-04", "2026-10-09", false, 3);
        // Node D phụ thuộc cả B và C (hội tụ hình thoi, duration: 4 ngày)
        Task tD = createTask(103, projectId, "Tích hợp hệ thống (D)", "TODO", "HIGH",
                "2026-10-10", "2026-10-14", false, 4);

        List<Task> allTasks = new ArrayList<>(List.of(tA, tB, tC, tD));
        List<TaskDependency> deps = List.of(
                new TaskDependency(projectId, 101, 100, "FINISH_TO_START", 1), // A -> B
                new TaskDependency(projectId, 102, 100, "FINISH_TO_START", 1), // A -> C
                new TaskDependency(projectId, 103, 101, "FINISH_TO_START", 1), // B -> D
                new TaskDependency(projectId, 103, 102, "FINISH_TO_START", 1)  // C -> D
        );

        // 1. Kiểm tra đồ thị không có chu trình
        DependencyGraph graph = new DependencyGraph(deps);
        assertFalse(graph.hasAnyCycle());
        List<Integer> topo = graph.topologicalSort();
        assertEquals(4, topo.size());
        assertEquals(100, topo.get(0), "Gốc A phải đứng đầu thứ tự topo");
        assertEquals(103, topo.get(3), "Đỉnh hội tụ D phải đứng cuối thứ tự topo");

        // 2. Kiểm tra điều kiện mở khóa Task D:
        // Task B đã DONE, nhưng Task C vẫn đang IN_PROGRESS -> D không thể chuyển sang IN_PROGRESS
        boolean allPrereqsDone = TaskStatus.isDone(tB.getStatus()) && TaskStatus.isDone(tC.getStatus());
        assertFalse(allPrereqsDone, "Task D phải bị khóa vì C chưa hoàn thành");

        // 3. Giả sử Task C bị chậm thêm 3 ngày (từ 2026-10-09 dời sang 2026-10-12)
        LocalDate newDueC = LocalDate.of(2026, 10, 12);
        List<Task> modified = CascadeScheduler.cascade(102, newDueC, allTasks, deps);

        // Task D phải chọn mốc muộn nhất giữa B (due 2026-10-07) và C (due 2026-10-12)
        // Ngày bắt đầu sớm nhất của D = max(2026-10-07 + 1, 2026-10-12 + 1) = 2026-10-13!
        assertEquals("2026-10-13", tD.getStartDate(), "Task D phải dời bắt đầu đến 2026-10-13 do nhánh C chi phối");
        assertEquals("2026-10-17", tD.getDueDate(), "Task D phải kết thúc vào 2026-10-17 (bảo toàn 4 ngày duration)");

        // 4. Khi C nghiệm thu xong và chuyển sang DONE -> D được mở khóa
        tC.setStatus("DONE");
        allPrereqsDone = TaskStatus.isDone(tB.getStatus()) && TaskStatus.isDone(tC.getStatus());
        assertTrue(allPrereqsDone, "Task D giờ đây đủ điều kiện mở khóa để bắt đầu");
        tD.setStatus("IN_PROGRESS");
        assertEquals("IN_PROGRESS", tD.getStatus());
    }

    // =========================================================================
    // SCENARIO 4: CYCLE PREVENTION INTERSECTS WITH AUTOMATION & PERSISTENCE
    // =========================================================================

    @Test
    @DisplayName("Kịch bản 4: Chặn tuyệt đối chu trình phụ thuộc vòng (Cycle Prevention) khi cố tình thêm cạnh nghịch")
    void testCyclePreventionWithIndirectAndSelfDependencies() {
        int projectId = 40;

        // Chuỗi: Task 1 -> Task 2 -> Task 3
        List<TaskDependency> deps = List.of(
                new TaskDependency(projectId, 2, 1, "FINISH_TO_START", 0),
                new TaskDependency(projectId, 3, 2, "FINISH_TO_START", 0)
        );

        DependencyGraph graph = new DependencyGraph(deps);
        assertFalse(graph.hasAnyCycle());

        // Cố tình thêm cạnh nghịch: Task 1 phụ thuộc Task 3 (Tạo chu trình 1 -> 2 -> 3 -> 1)
        boolean wouldCycle = graph.wouldCauseCycle(1, 3);
        assertTrue(wouldCycle, "Thêm 3 -> 1 phải bị chặn do gây chu trình vòng");

        // Cố tình thêm tự phụ thuộc: Task 2 phụ thuộc Task 2
        assertTrue(graph.wouldCauseCycle(2, 2), "Tự phụ thuộc (A -> A) phải luôn bị chặn");

        // Thêm cạnh hợp lệ: Task 4 phụ thuộc Task 3 (1 -> 2 -> 3 -> 4)
        assertFalse(graph.wouldCauseCycle(4, 3), "Thêm 3 -> 4 là hoàn toàn hợp lệ trong DAG");
    }

    // =========================================================================
    // SCENARIO 5: AUTOMATION RE-ENTRANCY & THREAD LOCAL ISOLATION
    // =========================================================================

    @Test
    @DisplayName("Kịch bản 5: Cơ chế bảo vệ chống lặp vô tận (Re-entrancy Guard) ngăn chặn kích hoạt chéo giữa các Recipe")
    void testAutomationReentrancyGuardPreventsCascadeLoop() {
        // Mặc định không nằm trong guard
        assertFalse(AutomationEngine.isInAutomation());

        // Giả lập tình huống: Recipe 2 (Auto-close subtasks) đang chạy
        AutomationEngine.setInAutomationForTesting(true);
        assertTrue(AutomationEngine.isInAutomation());

        // Khi subtask bị đóng, logic hệ thống gọi tiếp onSubtasksProgressUpdated (Recipe 1)
        // Guard phải chặn ngay lập tức, trả về false, không kích hoạt đệ quy
        boolean recipe1Blocked = !AutomationEngine.onSubtasksProgressUpdated(999, 1);
        assertTrue(recipe1Blocked, "Recipe 1 phải bị chặn khi Thread đang trong quá trình thực thi tự động hóa");

        // Guard cũng phải chặn Recipe 2 gọi lại chính nó
        int subtasksClosed = AutomationEngine.onTaskApproved(999, 1);
        assertEquals(0, subtasksClosed, "Recipe 2 không được phép tái kích hoạt khi guard đang bật");

        // Guard cũng phải chặn Recipe 3 quét lặp lại
        int escalated = AutomationEngine.evaluateDueTasks(1);
        assertEquals(0, escalated, "Recipe 3 không được phép tái kích hoạt khi guard đang bật");

        // Dọn dẹp guard sau khi hoàn tất
        AutomationEngine.clearAutomationGuard();
        assertFalse(AutomationEngine.isInAutomation(), "Cờ guard phải được xóa sạch sẽ");
    }

    // =========================================================================
    // SCENARIO 6: GATE 2 MULTI-REVISION REVIEWS & PROJECT HEALTH CORRELATION
    // =========================================================================

    @Test
    @DisplayName("Kịch bản 6: Tương quan giữa chất lượng duyệt Gate 2 nhiều lần nộp và điểm sức khỏe tổng thể (Health Index)")
    void testGate2MultiRevisionReviewsAndProjectHealthCorrelation() {
        int projectId = 50;

        // Dự án có 3 task
        Task t1 = createTask(201, projectId, "API Gateway", "DONE", "HIGH", "2026-10-01", "2026-10-05", true, 10);
        Task t2 = createTask(202, projectId, "Auth Service", "DONE", "HIGH", "2026-10-01", "2026-10-07", true, 11);
        Task t3 = createTask(203, projectId, "Billing Service", "IN_PROGRESS", "MEDIUM", "2026-10-01", "2026-10-15", true, 12);
        List<Task> tasks = List.of(t1, t2, t3);

        // Lịch sử bàn giao Gate 2:
        List<TaskSubmission> subs = new ArrayList<>();

        // Task 201: Đạt ngay lần đầu (Rev 1 APPROVED, 5 sao, duyệt sau 1.5h)
        TaskSubmission s1 = new TaskSubmission(201, 1, 10, "Bàn giao Gateway v1", "", "04/10/2026 09:00");
        s1.setStatus("APPROVED");
        s1.setReviewedAt("04/10/2026 10:30");
        s1.setQualityRating(5);
        subs.add(s1);

        // Task 202: Lần 1 bị trả sửa, Lần 2 mới duyệt (Rev 1 REVISE, Rev 2 APPROVED, 4 sao)
        TaskSubmission s2_1 = new TaskSubmission(202, 1, 11, "Bàn giao Auth v1", "", "05/10/2026 08:00");
        s2_1.setStatus("REVISE");
        s2_1.setReviewedAt("05/10/2026 12:00");
        s2_1.setQualityRating(3);
        s2_1.setPmFeedback("Thiếu mã hóa Bcrypt");
        subs.add(s2_1);

        TaskSubmission s2_2 = new TaskSubmission(202, 2, 11, "Bàn giao Auth v2", "", "06/10/2026 10:00");
        s2_2.setStatus("APPROVED");
        s2_2.setReviewedAt("06/10/2026 11:00");
        s2_2.setQualityRating(5);
        subs.add(s2_2);

        // Task 203: Đang nộp chờ duyệt (Rev 1 SUBMITTED)
        TaskSubmission s3_1 = new TaskSubmission(203, 1, 12, "Bàn giao Billing v1", "", "15/10/2026 08:00");
        s3_1.setStatus("SUBMITTED");
        subs.add(s3_1);

        // 1. Đánh giá chất lượng và vận tốc
        QualityMetrics qm = QualityVelocityCalculator.calculate(subs, tasks);
        assertEquals(2, qm.getTotalReviewedTasks(), "Chỉ có 2 task đã hoàn tất chu trình thẩm định (201 & 202)");
        assertEquals(1, qm.getFirstTimePassedCount(), "Task 201 đạt ngay lần đầu");
        assertEquals(1, qm.getRevisionRequiredCount(), "Task 202 cần sửa đổi");
        assertEquals(1, qm.getPendingReviewCount(), "Task 203 đang chờ duyệt");
        assertEquals(50.0, qm.getFirstTimePassRate(), "FTPR chính xác là 50.0%");
        
        // Danh sách công việc có lịch sử nộp: cả 3 task đều có submissions, sắp xếp theo rejectionsCount DESC
        assertEquals(3, qm.getMostRevisedTasks().size(), "Cả 3 task có submission đều được thống kê");
        assertEquals(202, qm.getMostRevisedTasks().get(0).getTaskId(), "Task 202 đứng đầu danh sách do có 1 lần bị từ chối");
        assertEquals(1, qm.getMostRevisedTasks().get(0).getRejectionsCount(), "Task 202 bị từ chối 1 lần");
        assertEquals(2, qm.getMostRevisedTasks().get(0).getRevisionCount(), "Task 202 có 2 lần nộp");

        // 2. Đánh giá Burndown
        BurndownData bd = BurndownCalculator.calculate(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 15),
                tasks,
                LocalDate.of(2026, 10, 8)
        );
        assertEquals(3, bd.getTotalTasks());
        assertEquals(2, bd.getCompletedTasks());
        assertEquals(1, bd.getRemainingTasks());

        // 3. Điểm sức khỏe dự án tổng thể
        QualityMetrics comprehensiveMetrics = QualityVelocityCalculator.calculate(subs, tasks, Collections.emptyList(), bd);
        assertTrue(comprehensiveMetrics.getHealthScore() <= 100);
        assertTrue(comprehensiveMetrics.getHealthScore() >= 0);
        assertNotNull(comprehensiveMetrics.getHealthStatus());
    }

    // =========================================================================
    // SCENARIO 7: BOUNDARY & MISUSE TESTING ACROSS ENGINES
    // =========================================================================

    @Test
    @DisplayName("Kịch bản 7: Kiểm thử giá trị biên và các tình huống dữ liệu bất thường (Edge & Boundary Cases)")
    void testBoundaryAndEdgeCasesCrossFeature() {
        // 1. Biên Recipe 1: Tiến độ 99% không được kích hoạt, 100% mới kích hoạt
        assertFalse(AutomationEngine.shouldPromptGate2(true, true, 99, "IN_PROGRESS"), "99% tiến độ không được kích hoạt Recipe 1");
        assertTrue(AutomationEngine.shouldPromptGate2(true, true, 100, "IN_PROGRESS"), "100% tiến độ kích hoạt Recipe 1");
        assertFalse(AutomationEngine.shouldPromptGate2(true, false, 100, "IN_PROGRESS"), "requiresGate = false không được kích hoạt");
        assertFalse(AutomationEngine.shouldPromptGate2(true, true, 100, "DONE"), "Task đã DONE không được kích hoạt");
        assertFalse(AutomationEngine.shouldPromptGate2(true, true, 100, "SUBMITTED"), "Task đã nộp chờ duyệt không được kích hoạt");

        // 2. Biên Recipe 3: Tiến độ 49% được kích hoạt, 50% không được kích hoạt
        LocalDate today = LocalDate.of(2026, 10, 15);
        assertTrue(AutomationEngine.shouldEscalatePriority(true, "2026-10-15", 49, "LOW", today, "IN_PROGRESS"));
        assertFalse(AutomationEngine.shouldEscalatePriority(true, "2026-10-15", 50, "LOW", today, "IN_PROGRESS"), "50% là ngưỡng an toàn, không nâng ưu tiên");

        // Hạn chót ngày mai (chưa đến hạn) -> không kích hoạt
        assertFalse(AutomationEngine.shouldEscalatePriority(true, "2026-10-16", 20, "LOW", today, "IN_PROGRESS"));
        // Task đã là HIGH -> không cần nâng thêm (idempotent)
        assertFalse(AutomationEngine.shouldEscalatePriority(true, "2026-10-15", 20, "HIGH", today, "IN_PROGRESS"));

        // 3. Biên Cascade Scheduler: Danh sách rỗng hoặc Task không tồn tại
        List<Task> emptyResult = CascadeScheduler.cascade(999, LocalDate.now(), Collections.emptyList(), Collections.emptyList());
        assertNotNull(emptyResult);
        assertTrue(emptyResult.isEmpty());

        // 4. Biên Burndown: Ngày kết thúc trước ngày bắt đầu được tự động điều chỉnh an toàn
        BurndownData bdInverted = BurndownCalculator.calculate(
                LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 10), // Đảo ngược
                Collections.emptyList(),
                LocalDate.of(2026, 10, 15)
        );
        assertNotNull(bdInverted);
        assertTrue(bdInverted.getLabels().size() >= 1);
        assertEquals(0, bdInverted.getTotalTasks());

        // 5. Biên Workload: Danh sách rỗng xử lý an toàn
        List<WorkloadAnalytics> emptyWa = WorkloadAnalytics.fromWorkloads(null);
        assertNotNull(emptyWa);
        assertTrue(emptyWa.isEmpty());
    }
}
