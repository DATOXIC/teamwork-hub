package com.teamwork.business;

import com.teamwork.business.automation.AutomationEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử đơn vị thuần túy (Pure Unit Test) cho Động cơ Tự động hóa (AutomationEngine).
 * Kiểm tra tính đúng đắn logic của 3 công thức (Recipe 1, 2, 3), cơ chế chống lặp vô tận (Loop Guard),
 * tính lũy thừa (Idempotency) và các trường hợp biên (Boundary Cases).
 */
@DisplayName("Kiểm thử Động cơ Tự động hóa (AutomationEngine Pure POJO)")
class AutomationEngineTest {

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
    // 1. KIỂM THỬ CƠ CHẾ BẢO VỆ CHỐNG LẶP VÔ TẬN (LOOP GUARD & SELF-TRIGGER)
    // =========================================================================

    @Test
    @DisplayName("Cờ IN_AUTOMATION mặc định là false")
    void testDefaultNotInAutomation() {
        assertFalse(AutomationEngine.isInAutomation(), "Mặc định luồng không được nằm trong Automation Guard");
    }

    @Test
    @DisplayName("Bật cờ IN_AUTOMATION chặn kích hoạt đệ quy của Recipe 1")
    void testAutomationGuardBlocksRecipe1() {
        AutomationEngine.setInAutomationForTesting(true);
        assertTrue(AutomationEngine.isInAutomation());

        // Khi cờ bật, hàm lập tức trả về false mà không gọi CSDL hay gửi thông báo
        boolean triggered = AutomationEngine.onSubtasksProgressUpdated(100, 1);
        assertFalse(triggered, "Recipe 1 phải bị chặn hoàn toàn khi IN_AUTOMATION = true");
    }

    @Test
    @DisplayName("Bật cờ IN_AUTOMATION chặn kích hoạt đệ quy của Recipe 2")
    void testAutomationGuardBlocksRecipe2() {
        AutomationEngine.setInAutomationForTesting(true);
        int closed = AutomationEngine.onTaskApproved(100, 1);
        assertEquals(0, closed, "Recipe 2 phải trả về 0 subtask được đóng khi IN_AUTOMATION = true");
    }

    @Test
    @DisplayName("Bật cờ IN_AUTOMATION chặn kích hoạt đệ quy của Recipe 3")
    void testAutomationGuardBlocksRecipe3() {
        AutomationEngine.setInAutomationForTesting(true);
        int escalated = AutomationEngine.evaluateDueTasks(1);
        assertEquals(0, escalated, "Recipe 3 phải trả về 0 task được nâng cấp khi IN_AUTOMATION = true");
    }

    @Test
    @DisplayName("ThreadLocal bảo đảm tính cách ly độc lập giữa các luồng đồng thời")
    void testThreadLocalIsolation() throws InterruptedException {
        // Luồng chính bật guard
        AutomationEngine.setInAutomationForTesting(true);
        assertTrue(AutomationEngine.isInAutomation());

        AtomicBoolean otherThreadResult = new AtomicBoolean(true);
        Thread worker = new Thread(() -> {
            // Luồng phụ kiểm tra cờ của chính nó -> phải là false
            otherThreadResult.set(AutomationEngine.isInAutomation());
        });
        worker.start();
        worker.join();

        assertFalse(otherThreadResult.get(), "Luồng khác không được bị ảnh hưởng bởi cờ của luồng hiện tại");
        assertTrue(AutomationEngine.isInAutomation(), "Luồng hiện tại vẫn giữ nguyên trạng thái guard");
    }

    // =========================================================================
    // 2. KIỂM THỬ RECIPE 1: TỰ ĐỘNG NHẮC NỘP BÀN GIAO GATE 2 (AUTO_PROMPT_GATE2)
    // =========================================================================

    @Test
    @DisplayName("Recipe 1: Kích hoạt khi bật, có gate, đạt 100% và task đang làm (IN_PROGRESS)")
    void testRecipe1TriggerSuccess() {
        boolean shouldPrompt = AutomationEngine.shouldPromptGate2(true, true, 100, "IN_PROGRESS");
        assertTrue(shouldPrompt, "Phải kích hoạt khi đạt đủ mọi điều kiện");
    }

    @Test
    @DisplayName("Recipe 1: Kích hoạt khi task ở trạng thái TODO hoặc REVISE nhưng đã hoàn thành 100% subtask")
    void testRecipe1TriggerInOtherValidStatuses() {
        assertTrue(AutomationEngine.shouldPromptGate2(true, true, 100, "TODO"));
        assertTrue(AutomationEngine.shouldPromptGate2(true, true, 100, "REVISE"));
        assertTrue(AutomationEngine.shouldPromptGate2(true, true, 100, "REJECTED"));
    }

    @Test
    @DisplayName("Recipe 1: Bị chặn khi công thức bị tắt")
    void testRecipe1Disabled() {
        assertFalse(AutomationEngine.shouldPromptGate2(false, true, 100, "IN_PROGRESS"),
                "Không được kích hoạt khi recipe đã bị tắt");
    }

    @Test
    @DisplayName("Recipe 1: Bị chặn khi Task không yêu cầu Gate chất lượng")
    void testRecipe1NoGateRequired() {
        assertFalse(AutomationEngine.shouldPromptGate2(true, false, 100, "IN_PROGRESS"),
                "Không được kích hoạt nếu requiresGate = false");
    }

    @Test
    @DisplayName("Recipe 1: Bị chặn khi tiến độ việc con chưa đạt 100%")
    void testRecipe1ProgressIncomplete() {
        assertFalse(AutomationEngine.shouldPromptGate2(true, true, 0, "IN_PROGRESS"));
        assertFalse(AutomationEngine.shouldPromptGate2(true, true, 50, "IN_PROGRESS"));
        assertFalse(AutomationEngine.shouldPromptGate2(true, true, 99, "IN_PROGRESS"));
    }

    @Test
    @DisplayName("Recipe 1: Bị chặn khi Task cha đã ở trạng thái DONE hoặc SUBMITTED")
    void testRecipe1AlreadySubmittedOrDone() {
        assertFalse(AutomationEngine.shouldPromptGate2(true, true, 100, "DONE"),
                "Task đã xong không nhắc nộp lại");
        assertFalse(AutomationEngine.shouldPromptGate2(true, true, 100, "SUBMITTED"),
                "Task đã nộp chờ duyệt không nhắc nộp lại");
    }

    @Test
    @DisplayName("Recipe 1: Xử lý an toàn khi trạng thái null hoặc rỗng")
    void testRecipe1NullStatusHandling() {
        assertFalse(AutomationEngine.shouldPromptGate2(true, true, 100, null));
    }

    // =========================================================================
    // 3. KIỂM THỬ RECIPE 2: HẰNG SỐ VÀ LOGIC SUBTASK FINISHED (AUTO_CLOSE_SUBTASKS)
    // =========================================================================

    @Test
    @DisplayName("Recipe 2: Hằng số công thức khớp với đặc tả hệ thống")
    void testRecipeConstants() {
        assertEquals("AUTO_PROMPT_GATE2", ProjectAutomation.AUTO_PROMPT_GATE2);
        assertEquals("AUTO_CLOSE_SUBTASKS", ProjectAutomation.AUTO_CLOSE_SUBTASKS);
        assertEquals("AUTO_ESCALATE_PRIORITY", ProjectAutomation.AUTO_ESCALATE_PRIORITY);
    }

    @Test
    @DisplayName("Recipe 2: Xác thực các trạng thái việc con đã hoàn tất (Finished)")
    void testSubTaskStatusFinishedCheck() {
        assertTrue(SubTaskStatus.isFinished("APPROVED"), "APPROVED là trạng thái đã nghiệm thu xong");
        assertTrue(SubTaskStatus.isFinished("DONE"), "DONE là trạng thái đã xong");
        assertFalse(SubTaskStatus.isFinished("TODO"), "TODO là trạng thái chưa xong, cần tự động đóng");
        assertFalse(SubTaskStatus.isFinished("SUBMITTED"), "SUBMITTED là trạng thái đang chờ duyệt, cần tự động đóng khi PM duyệt");
        assertFalse(SubTaskStatus.isFinished(null));
    }

    // =========================================================================
    // 4. KIỂM THỬ RECIPE 3: TỰ ĐỘNG NÂNG ĐỘ ƯU TIÊN (AUTO_ESCALATE_PRIORITY)
    // =========================================================================

    @Test
    @DisplayName("Recipe 3: Nâng ưu tiên khi hạn chót là hôm nay và tiến độ < 50%")
    void testRecipe3DueTodayUnder50Escalates() {
        String dueIso = today.toString(); // 2026-10-15
        boolean shouldEscalate = AutomationEngine.shouldEscalatePriority(
                true, dueIso, 40, "MEDIUM", today, "IN_PROGRESS");
        assertTrue(shouldEscalate, "Hạn là hôm nay và tiến độ 40% < 50% -> Phải nâng ưu tiên lên HIGH");
    }

    @Test
    @DisplayName("Recipe 3: Nâng ưu tiên khi công việc đã quá hạn (hôm qua) và tiến độ < 50%")
    void testRecipe3OverdueUnder50Escalates() {
        String dueIso = today.minusDays(1).toString(); // 2026-10-14
        boolean shouldEscalate = AutomationEngine.shouldEscalatePriority(
                true, dueIso, 20, "LOW", today, "TODO");
        assertTrue(shouldEscalate, "Đã quá hạn và tiến độ 20% < 50% -> Phải nâng ưu tiên");
    }

    @Test
    @DisplayName("Recipe 3: Không nâng ưu tiên nếu tiến độ đạt từ 50% trở lên")
    void testRecipe3ProgressAtLeast50DoesNotEscalate() {
        String dueIso = today.toString();
        assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 50, "MEDIUM", today, "IN_PROGRESS"),
                "Tiến độ đúng 50% không bị nâng ưu tiên");
        assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 75, "MEDIUM", today, "IN_PROGRESS"),
                "Tiến độ 75% không bị nâng ưu tiên");
        assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 100, "MEDIUM", today, "IN_PROGRESS"),
                "Tiến độ 100% không bị nâng ưu tiên");
    }

    @Test
    @DisplayName("Recipe 3: Tính lũy thừa (Idempotency) - Nếu đã là HIGH thì không nâng nữa")
    void testRecipe3AlreadyHighDoesNotEscalate() {
        String dueIso = today.toString();
        assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 10, "HIGH", today, "IN_PROGRESS"),
                "Đã là HIGH thì không kích hoạt cảnh báo trùng lặp");
        assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 10, "high", today, "IN_PROGRESS"),
                "Không phân biệt hoa thường với priority HIGH");
    }

    @Test
    @DisplayName("Recipe 3: Không nâng ưu tiên nếu hạn chót ở tương lai (ngày mai hoặc tuần sau)")
    void testRecipe3FutureDueDateDoesNotEscalate() {
        String dueTomorrow = today.plusDays(1).toString();
        String dueNextWeek = today.plusDays(7).toString();
        assertFalse(AutomationEngine.shouldEscalatePriority(true, dueTomorrow, 0, "LOW", today, "TODO"),
                "Hạn chót ngày mai chưa phải xử lý khẩn cấp");
        assertFalse(AutomationEngine.shouldEscalatePriority(true, dueNextWeek, 0, "LOW", today, "TODO"),
                "Hạn chót tuần sau chưa phải xử lý khẩn cấp");
    }

    @Test
    @DisplayName("Recipe 3: Không nâng ưu tiên nếu công việc đã hoàn thành hoặc đã nộp")
    void testRecipe3DoneOrSubmittedDoesNotEscalate() {
        String dueIso = today.toString();
        assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 0, "MEDIUM", today, "DONE"));
        assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 0, "MEDIUM", today, "SUBMITTED"));
    }

    @Test
    @DisplayName("Recipe 3: Xử lý an toàn các dữ liệu biên null hoặc định dạng sai")
    void testRecipe3NullAndMalformedInputsSafe() {
        assertFalse(AutomationEngine.shouldEscalatePriority(true, null, 10, "MEDIUM", today, "TODO"));
        assertFalse(AutomationEngine.shouldEscalatePriority(true, "", 10, "MEDIUM", today, "TODO"));
        assertFalse(AutomationEngine.shouldEscalatePriority(true, "invalid-date", 10, "MEDIUM", today, "TODO"));
        assertFalse(AutomationEngine.shouldEscalatePriority(true, today.toString(), 10, "MEDIUM", null, "TODO"));
        assertFalse(AutomationEngine.shouldEscalatePriority(false, today.toString(), 10, "MEDIUM", today, "TODO"));
    }

    // =========================================================================
    // 5. KIỂM THỬ GIÁ TRỊ BIÊN (BOUNDARY VALUE ANALYSIS)
    // =========================================================================

    @Test
    @DisplayName("Điểm biên tiến độ: 49% bị nâng ưu tiên, 50% không bị nâng ưu tiên")
    void testProgressBoundary49Vs50() {
        String dueIso = today.toString();
        assertTrue(AutomationEngine.shouldEscalatePriority(true, dueIso, 49, "LOW", today, "IN_PROGRESS"),
                "49% < 50% -> Bị nâng ưu tiên");
        assertFalse(AutomationEngine.shouldEscalatePriority(true, dueIso, 50, "LOW", today, "IN_PROGRESS"),
                "50% = 50% -> Không bị nâng ưu tiên");
    }

    @Test
    @DisplayName("Điểm biên ngày hạn chót: hôm nay bị nâng, ngày mai an toàn")
    void testDateBoundaryTodayVsTomorrow() {
        assertTrue(AutomationEngine.shouldEscalatePriority(true, today.toString(), 20, "LOW", today, "IN_PROGRESS"),
                "Hôm nay -> Bị nâng");
        assertFalse(AutomationEngine.shouldEscalatePriority(true, today.plusDays(1).toString(), 20, "LOW", today, "IN_PROGRESS"),
                "Ngày mai -> An toàn");
    }
}
