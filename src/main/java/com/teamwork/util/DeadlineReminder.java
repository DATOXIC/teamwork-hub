package com.teamwork.util;

import com.teamwork.business.Project;
import com.teamwork.business.SubTask;
import com.teamwork.business.SubTaskStatus;
import com.teamwork.business.TaskStatus;
import com.teamwork.business.Task;
import com.teamwork.business.User;
import com.teamwork.data.NotificationDB;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.SubTaskDB;
import com.teamwork.data.TaskDB;
import com.teamwork.data.UserDB;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Nhắc hạn chót hằng ngày: công việc / việc con hết hạn VÀO NGÀY MAI (giờ Việt Nam) và chưa xong
 * → người phụ trách nhận thông báo 🔔 + email (nếu đã cấu hình mail.properties).
 *
 * <p>Chạy bởi {@code AppLifecycleListener} mỗi ngày lúc {@code TEAMWORK_REMINDER_HOUR} giờ (mặc định 8h).
 * Tắt bằng biến môi trường {@code TEAMWORK_REMINDERS=off}.</p>
 *
 * <p>Không gửi trùng: mỗi lời nhắc có một đường link riêng (đuôi {@code #nhac-han-task-5-2026-10-09});
 * nếu người nhận đã có thông báo với link đó thì bỏ qua — chạy lại nhiều lần trong ngày (khởi động lại server,
 * Render "ngủ" rồi thức dậy) cũng chỉ nhắc một lần.</p>
 */
public final class DeadlineReminder implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(DeadlineReminder.class.getName());
    public static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Một lời nhắc cần gửi. kind = "task" (người phụ trách công việc) hoặc "subtask" (người làm việc con). */
    public record Reminder(int recipientId, int projectId, String kind, int itemId, String title, String parentTitle, String dueIso) {
        /** Link mở bảng công việc; phần sau # vừa giúp người đọc hiểu, vừa là khóa chống gửi trùng. */
        public String link() {
            return "/task?action=list&projectId=" + projectId + "#nhac-han-" + kind + "-" + itemId + "-" + dueIso;
        }
    }

    // ===================== CẤU HÌNH =====================

    /** Bật mặc định; tắt bằng TEAMWORK_REMINDERS=off|false|0 (hoặc -Dteamwork.reminders=off). */
    public static boolean enabled() {
        String v = System.getProperty("teamwork.reminders", System.getenv("TEAMWORK_REMINDERS"));
        return v == null || !(v.equalsIgnoreCase("off") || v.equalsIgnoreCase("false") || v.equals("0"));
    }

    /** Giờ chạy trong ngày (0–23, giờ Việt Nam), mặc định 8. */
    public static int hour() {
        String v = System.getProperty("teamwork.reminderHour", System.getenv("TEAMWORK_REMINDER_HOUR"));
        try {
            int h = Integer.parseInt(v == null ? "8" : v.trim());
            return (h >= 0 && h <= 23) ? h : 8;
        } catch (NumberFormatException e) {
            return 8;
        }
    }

    /** Số mili-giây từ {@code now} tới lần chạy kế tiếp lúc {@code hour}:00 (hôm nay nếu chưa tới giờ, không thì ngày mai). */
    public static long initialDelayMillis(ZonedDateTime now, int hour) {
        ZonedDateTime next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0);
        if (!next.isAfter(now)) next = next.plusDays(1);
        return java.time.Duration.between(now, next).toMillis();
    }

    // ===================== LẬP DANH SÁCH (thuần, không đụng DB — có unit test) =====================

    /**
     * Danh sách lời nhắc cho hạn {@code dueIso} từ các task / việc con tới hạn.
     * Việc con chỉ nhắc khi công việc cha có trong {@code parents} (cần projectId và tên để ghi thông báo).
     */
    public static List<Reminder> plan(String dueIso, List<Task> tasks, List<SubTask> subTasks, Map<Integer, Task> parents) {
        List<Reminder> out = new ArrayList<>();
        if (tasks != null) {
            for (Task t : tasks) {
                if (t.getAssigneeId() <= 0 || TaskStatus.isDone(t.getStatus()) || !dueIso.equals(t.getDueDate())) continue;
                out.add(new Reminder(t.getAssigneeId(), t.getProjectId(), "task", t.getId(), t.getTitle(), null, dueIso));
            }
        }
        if (subTasks != null) {
            for (SubTask st : subTasks) {
                Task parent = parents == null ? null : parents.get(st.getTaskId());
                if (parent == null || st.getAssigneeId() <= 0 || SubTaskStatus.isFinished(st.getStatus()) || !dueIso.equals(st.getDueDate())) continue;
                out.add(new Reminder(st.getAssigneeId(), parent.getProjectId(), "subtask", st.getId(), st.getTitle(), parent.getTitle(), dueIso));
            }
        }
        return out;
    }

    static String message(Reminder r, String projectName) {
        String what = "task".equals(r.kind())
                ? "Công việc [" + r.title() + "]"
                : "Nhiệm vụ [" + r.title() + "] (thuộc công việc [" + r.parentTitle() + "])";
        String due = LocalDate.parse(r.dueIso()).format(VN_DATE);
        return what + " trong dự án [" + (projectName == null ? "#" + r.projectId() : projectName)
                + "] hết hạn vào NGÀY MAI (" + due + ") và chưa hoàn thành.";
    }

    // ===================== CHẠY (đọc DB, gửi thông báo + email) =====================

    @Override
    public void run() {
        try {
            int[] r = runFor(LocalDate.now(VN).plusDays(1));
            LOGGER.info("Nhắc hạn: " + r[0] + " lời nhắc mới, " + r[1] + " email đã gửi, " + r[2] + " đã nhắc từ trước.");
        } catch (Throwable e) {
            // Không ném lỗi ra ngoài: ScheduledExecutorService sẽ NGỪNG lịch chạy nếu một lần chạy bị lỗi
            LOGGER.log(Level.SEVERE, "Nhắc hạn: lỗi khi chạy", e);
        }
    }

    /** @return {số thông báo mới, số email gửi được, số lời nhắc bỏ qua vì đã gửi} */
    public static int[] runFor(LocalDate dueDate) {
        String dueIso = dueDate.toString();
        List<Task> tasks = TaskDB.selectOpenDueOn(dueIso);
        List<SubTask> subs = SubTaskDB.selectOpenDueOn(dueIso);

        Map<Integer, Task> parents = new HashMap<>();
        for (SubTask st : subs) {
            parents.computeIfAbsent(st.getTaskId(), TaskDB::selectById);
        }
        List<Reminder> reminders = plan(dueIso, tasks, subs, parents);

        Set<Integer> userIds = new HashSet<>();
        for (Reminder r : reminders) userIds.add(r.recipientId());
        Map<Integer, User> users = new HashMap<>();
        for (User u : UserDB.selectByIds(userIds)) users.put(u.getId(), u);
        Map<Integer, String> projectNames = new HashMap<>();
        boolean mail = MailUtil.isConfigured();

        int sent = 0, emailed = 0, skipped = 0;
        for (Reminder r : reminders) {
            if (NotificationDB.existsWithLink(r.recipientId(), r.link())) {
                skipped++;
                continue;
            }
            String projectName = projectNames.computeIfAbsent(r.projectId(), id -> {
                Project p = ProjectDB.selectById(id);
                return p == null ? null : p.getName();
            });
            String text = message(r, projectName);
            // Kiểu GENERAL: cột type là enum trong DB (INVITE, TASK_ASSIGNED, PROGRESS, COMMENT, GENERAL)
            NotificationDB.send(r.recipientId(), "⏰ Sắp tới hạn chót", text, r.link(), "GENERAL");
            sent++;

            User u = users.get(r.recipientId());
            if (mail && u != null && u.getEmail() != null && !u.getEmail().isBlank()) {
                String name = (u.getFullName() == null || u.getFullName().isBlank()) ? "bạn" : u.getFullName();
                boolean ok = MailUtil.send(u.getEmail(), "[TeamWork Hub] Nhắc hạn: " + r.title(),
                        "Xin chào " + name + ",\n\n" + text + "\n\n"
                        + "Mở TeamWork Hub để cập nhật tiến độ hoặc báo trưởng dự án nếu cần dời hạn.\n\n"
                        + "TeamWork Hub (email tự động — không cần trả lời)");
                if (ok) emailed++;
            }
        }
        return new int[] {sent, emailed, skipped};
    }
}
