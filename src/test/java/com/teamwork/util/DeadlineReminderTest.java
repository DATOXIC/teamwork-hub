package com.teamwork.util;

import static org.junit.jupiter.api.Assertions.*;

import com.teamwork.business.SubTask;
import com.teamwork.business.Task;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class DeadlineReminderTest {

    private static final String TOMORROW = "2026-10-09";

    private static Task task(int id, int project, int lead, String status, String due) {
        Task t = new Task();
        t.setId(id);
        t.setProjectId(project);
        t.setAssigneeId(lead);
        t.setStatus(status);
        t.setDueDate(due);
        t.setTitle("Task " + id);
        return t;
    }

    private static SubTask sub(int id, int taskId, int assignee, String status, String due) {
        SubTask st = new SubTask();
        st.setId(id);
        st.setTaskId(taskId);
        st.setAssigneeId(assignee);
        st.setStatus(status);
        st.setDueDate(due);
        st.setTitle("Sub " + id);
        return st;
    }

    @Test
    void remindsOpenTasksAndSubTasksDueTomorrow() {
        Task parent = task(1, 10, 2, "IN_PROGRESS", "2026-10-20");
        List<DeadlineReminder.Reminder> plan = DeadlineReminder.plan(TOMORROW,
                List.of(task(5, 10, 3, "IN_PROGRESS", TOMORROW)),
                List.of(sub(50, 1, 4, "TODO", TOMORROW)),
                Map.of(1, parent));
        assertEquals(2, plan.size());
        assertEquals(3, plan.get(0).recipientId());
        assertEquals("task", plan.get(0).kind());
        assertEquals(4, plan.get(1).recipientId());
        assertEquals(10, plan.get(1).projectId(), "việc con lấy projectId từ công việc cha");
        assertEquals("Task 1", plan.get(1).parentTitle());
    }

    @Test
    void skipsDoneUnassignedOrOtherDates() {
        List<DeadlineReminder.Reminder> plan = DeadlineReminder.plan(TOMORROW,
                List.of(task(6, 10, 3, "DONE", TOMORROW),
                        task(7, 10, 3, "APPROVED", TOMORROW),
                        task(8, 10, 0, "TODO", TOMORROW),
                        task(9, 10, 3, "TODO", "2026-10-10")),
                List.of(sub(60, 1, 4, "APPROVED", TOMORROW),
                        sub(61, 1, 0, "TODO", TOMORROW),
                        sub(62, 99, 4, "TODO", TOMORROW)),   // không có công việc cha
                Map.of(1, task(1, 10, 2, "IN_PROGRESS", null)));
        assertTrue(plan.isEmpty(), plan.toString());
    }

    @Test
    void linkIsUniquePerItemAndDueDate() {
        DeadlineReminder.Reminder a = new DeadlineReminder.Reminder(3, 10, "task", 5, "A", null, TOMORROW);
        DeadlineReminder.Reminder b = new DeadlineReminder.Reminder(3, 10, "subtask", 5, "A", "P", TOMORROW);
        DeadlineReminder.Reminder c = new DeadlineReminder.Reminder(3, 10, "task", 5, "A", null, "2026-10-10");
        assertEquals("/task?action=list&projectId=10#nhac-han-task-5-2026-10-09", a.link());
        assertNotEquals(a.link(), b.link(), "task và việc con cùng id là 2 lời nhắc khác nhau");
        assertNotEquals(a.link(), c.link(), "dời hạn → được nhắc lại cho hạn mới");
    }

    @Test
    void messageMentionsProjectAndVietnameseDate() {
        DeadlineReminder.Reminder r = new DeadlineReminder.Reminder(3, 10, "subtask", 50, "Vẽ ERD", "Thiết kế CSDL", TOMORROW);
        String msg = DeadlineReminder.message(r, "TeamWork Hub");
        assertTrue(msg.contains("Nhiệm vụ [Vẽ ERD]"), msg);
        assertTrue(msg.contains("Thiết kế CSDL"), msg);
        assertTrue(msg.contains("09/10/2026"), msg);
        assertTrue(msg.contains("[TeamWork Hub]"), msg);
    }

    @Test
    void firstRunIsTodayIfBeforeTheHourOtherwiseTomorrow() {
        ZonedDateTime morning = ZonedDateTime.of(2026, 10, 8, 6, 30, 0, 0, DeadlineReminder.VN);
        assertEquals(TimeUnit.MINUTES.toMillis(90), DeadlineReminder.initialDelayMillis(morning, 8));
        ZonedDateTime afternoon = ZonedDateTime.of(2026, 10, 8, 15, 0, 0, 0, DeadlineReminder.VN);
        assertEquals(TimeUnit.HOURS.toMillis(17), DeadlineReminder.initialDelayMillis(afternoon, 8));
        ZonedDateTime exactly = ZonedDateTime.of(2026, 10, 8, 8, 0, 0, 0, DeadlineReminder.VN);
        assertEquals(TimeUnit.DAYS.toMillis(1), DeadlineReminder.initialDelayMillis(exactly, 8));
    }

    @Test
    void canBeTurnedOffAndHourIsValidated() {
        try {
            System.setProperty("teamwork.reminders", "off");
            assertFalse(DeadlineReminder.enabled());
            System.setProperty("teamwork.reminders", "on");
            assertTrue(DeadlineReminder.enabled());
            System.setProperty("teamwork.reminderHour", "25");
            assertEquals(8, DeadlineReminder.hour());
            System.setProperty("teamwork.reminderHour", "7");
            assertEquals(7, DeadlineReminder.hour());
        } finally {
            System.clearProperty("teamwork.reminders");
            System.clearProperty("teamwork.reminderHour");
        }
    }

    @Test
    void mentionSnippetIsShortened() {
        assertEquals("ngắn", MentionNotifier.snippet("  ngắn  "));
        String longText = "x".repeat(300);
        String s = MentionNotifier.snippet(longText);
        assertEquals(MentionNotifier.SNIPPET, s.length());
        assertTrue(s.endsWith("…"));
    }
}
