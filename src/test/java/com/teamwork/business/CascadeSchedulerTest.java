package com.teamwork.business;

import com.teamwork.business.gantt.CascadeScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử tính toán tự động dời lịch dây chuyền (Cascade Auto-Scheduling Engine)")
public class CascadeSchedulerTest {

    private Task createTask(int id, String title, String startDate, String dueDate) {
        Task t = new Task();
        t.setId(id);
        t.setProjectId(1);
        t.setTitle(title);
        t.setStartDate(startDate);
        t.setDueDate(dueDate);
        t.setStatus("TODO");
        return t;
    }

    @Test
    @DisplayName("Dời lịch 1 công việc kế thừa (Single Successor Shift) và bảo toàn thời lượng (Duration)")
    void testSingleSuccessorCascadeShift() {
        Task t1 = createTask(1, "Thiết kế kiến trúc", "2026-10-01", "2026-10-05");
        Task t2 = createTask(2, "Lập trình backend", "2026-10-06", "2026-10-10"); // duration: 4 ngày

        List<Task> tasks = new ArrayList<>();
        tasks.add(t1);
        tasks.add(t2);

        List<TaskDependency> deps = new ArrayList<>();
        // Task 2 phụ thuộc Task 1 (lagDays = 1)
        deps.add(new TaskDependency(1, 2, 1, "FS", 1));

        // Task 1 bị dời ngày kết thúc từ 2026-10-05 sang 2026-10-08
        List<Task> modified = CascadeScheduler.cascade(1, LocalDate.parse("2026-10-08"), tasks, deps);

        assertEquals(2, modified.size(), "Cả Task 1 và Task 2 đều phải bị thay đổi ngày");

        // Task 1: kết thúc ngày 2026-10-08
        assertEquals("2026-10-08", t1.getDueDate());

        // Task 2: ngày bắt đầu mới = 2026-10-08 + 1 ngày lag = 2026-10-09
        // ngày kết thúc mới = 2026-10-09 + 4 ngày duration = 2026-10-13
        assertEquals("2026-10-09", t2.getStartDate());
        assertEquals("2026-10-13", t2.getDueDate());
    }

    @Test
    @DisplayName("Dời lịch chuỗi dây chuyền nhiều nấc (A -> B -> C)")
    void testMultiNodeChainCascadeShift() {
        Task t1 = createTask(1, "Task A", "2026-10-01", "2026-10-05");
        Task t2 = createTask(2, "Task B", "2026-10-06", "2026-10-08"); // duration: 2 ngày
        Task t3 = createTask(3, "Task C", "2026-10-09", "2026-10-12"); // duration: 3 ngày

        List<Task> tasks = List.of(t1, t2, t3);
        List<TaskDependency> deps = List.of(
                new TaskDependency(1, 2, 1, "FS", 1),
                new TaskDependency(1, 3, 2, "FS", 1)
        );

        // Kéo dài Task A tới ngày 2026-10-10
        List<Task> modified = CascadeScheduler.cascade(1, LocalDate.parse("2026-10-10"), tasks, deps);

        assertEquals(3, modified.size(), "Toàn bộ chuỗi 3 task phải được cập nhật ngày");

        // t1 due: 2026-10-10
        assertEquals("2026-10-10", t1.getDueDate());

        // t2: bắt đầu 2026-10-11, hạn chót 2026-10-13
        assertEquals("2026-10-11", t2.getStartDate());
        assertEquals("2026-10-13", t2.getDueDate());

        // t3: bắt đầu 2026-10-14, hạn chót 2026-10-17
        assertEquals("2026-10-14", t3.getStartDate());
        assertEquals("2026-10-17", t3.getDueDate());
    }

    @Test
    @DisplayName("Nhiều công việc tiên quyết (Multiple Predecessors): Chọn mốc hoàn thành muộn nhất")
    void testMultiplePredecessorsTakesLatestDate() {
        // Task 1 kết thúc 2026-10-05
        Task t1 = createTask(1, "Thiết kế DB", "2026-10-01", "2026-10-05");
        // Task 2 kết thúc 2026-10-12
        Task t2 = createTask(2, "Thiết kế UI", "2026-10-01", "2026-10-12");
        // Task 3 phụ thuộc cả 1 và 2
        Task t3 = createTask(3, "Tích hợp hệ thống", "2026-10-06", "2026-10-08");

        List<Task> tasks = List.of(t1, t2, t3);
        List<TaskDependency> deps = List.of(
                new TaskDependency(1, 3, 1, "FS", 1),
                new TaskDependency(1, 3, 2, "FS", 1)
        );

        // Khi Task 1 được dời tới 2026-10-08, nhưng Task 2 kết thúc ngày 2026-10-12 (muộn hơn)
        // thì Task 3 phải bắt đầu sau Task 2 (tức ngày 2026-10-13)
        List<Task> modified = CascadeScheduler.cascade(1, LocalDate.parse("2026-10-08"), tasks, deps);

        assertEquals(2, modified.size());
        assertEquals("2026-10-08", t1.getDueDate());
        assertEquals("2026-10-13", t3.getStartDate());
    }

    @Test
    @DisplayName("Công việc phía sau đã có lịch bắt đầu xa hơn (Không vi phạm) thì không bị dời")
    void testUnaffectedTasksDoNotShift() {
        Task t1 = createTask(1, "Khởi tạo repo", "2026-10-01", "2026-10-02");
        // Task 2 đã được lên lịch tận cuối tháng
        Task t2 = createTask(2, "Deploy staging", "2026-10-25", "2026-10-30");

        List<Task> tasks = List.of(t1, t2);
        List<TaskDependency> deps = List.of(new TaskDependency(1, 2, 1, "FS", 1));

        // Dời Task 1 tới 2026-10-05. Ngày bắt đầu sớm nhất yêu cầu của Task 2 là 2026-10-06,
        // trong khi ngày thực tế của Task 2 là 2026-10-25 (không vi phạm).
        List<Task> modified = CascadeScheduler.cascade(1, LocalDate.parse("2026-10-05"), tasks, deps);

        assertEquals(1, modified.size(), "Chỉ có Task 1 bị thay đổi ngày, Task 2 giữ nguyên");
        assertEquals("2026-10-05", t1.getDueDate());
        assertEquals("2026-10-25", t2.getStartDate());
    }

    @Test
    @DisplayName("Khoảng thời gian trễ tùy chỉnh (Custom Lag Days)")
    void testCustomLagDaysShift() {
        Task t1 = createTask(1, "Kiểm thử đợt 1", "2026-10-01", "2026-10-05");
        Task t2 = createTask(2, "Kiểm thử đợt 2", "2026-10-06", "2026-10-08");

        List<Task> tasks = List.of(t1, t2);
        // lagDays = 3 (chờ 3 ngày làm mát / chuẩn bị)
        List<TaskDependency> deps = List.of(new TaskDependency(1, 2, 1, "FS", 3));

        List<Task> modified = CascadeScheduler.cascade(1, LocalDate.parse("2026-10-08"), tasks, deps);

        assertEquals(2, modified.size());
        // 2026-10-08 + 3 = 2026-10-11
        assertEquals("2026-10-11", t2.getStartDate());
    }

    @Test
    @DisplayName("Xử lý an toàn khi tham số đầu vào rỗng hoặc không hợp lệ")
    void testInvalidOrEmptyInputsGracefullyHandled() {
        assertTrue(CascadeScheduler.cascade(-1, LocalDate.now(), List.of(), List.of()).isEmpty());
        assertTrue(CascadeScheduler.cascade(1, LocalDate.now(), null, null).isEmpty());

        Task t1 = createTask(1, "Task test", "2026-10-01", "2026-10-05");
        assertTrue(CascadeScheduler.cascade(999, LocalDate.now(), List.of(t1), List.of()).isEmpty(),
                "Task ID không tồn tại phải trả về danh sách rỗng");
    }
}
