package com.teamwork.controllers.task;

import static org.junit.jupiter.api.Assertions.*;

import com.teamwork.business.Project;
import com.teamwork.business.SubTask;
import com.teamwork.business.Task;
import com.teamwork.business.User;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Ma trận phân quyền của /task (đặc tả trong TaskAccess): mỗi thao tác × mỗi vai trò.
 * Đây là câu trả lời cho "thành viên thường có làm được X không?" khi bảo vệ.
 */
class TaskPermissionMatrixTest {

    enum Role { PM, LEAD, SUB_ASSIGNEE, OTHER_MEMBER, ANONYMOUS }

    private static final int PM_ID = 1, LEAD_ID = 2, SUB_ID = 3, OTHER_ID = 4;

    private static User user(Role r) {
        int id;
        switch (r) {
            case PM: id = PM_ID; break;
            case LEAD: id = LEAD_ID; break;
            case SUB_ASSIGNEE: id = SUB_ID; break;
            case OTHER_MEMBER: id = OTHER_ID; break;
            default: return null;
        }
        User u = new User();
        u.setId(id);
        return u;
    }

    private static Project project() {
        Project p = new Project();
        p.setId(10);
        p.setOwnerId(PM_ID);
        return p;
    }

    private static Task task(int leadId) {
        Task t = new Task();
        t.setId(100);
        t.setProjectId(10);
        t.setAssigneeId(leadId);
        return t;
    }

    private static SubTask subTask(int assigneeId) {
        SubTask st = new SubTask();
        st.setId(1000);
        st.setTaskId(100);
        st.setAssigneeId(assigneeId);
        return st;
    }

    /** Một thao tác cần kiểm tra quyền, tính trên (user, task có Task Lead, việc con đã giao). */
    interface Action {
        boolean allowed(User u, Project p, Task t, SubTask st);
    }

    private static void assertMatrix(String name, Action action, Set<Role> allowed) {
        Project p = project();
        Task t = task(LEAD_ID);
        SubTask st = subTask(SUB_ID);
        for (Role r : Role.values()) {
            assertEquals(allowed.contains(r), action.allowed(user(r), p, t, st), name + " / " + r);
        }
    }

    // ===== Chỉ PM =====

    @Test
    void onlyPmCreatesAndAssignsTasks() {
        assertMatrix("tạo/giao task", (u, p, t, st) -> TaskAccess.canCreateTask(u, p), EnumSet.of(Role.PM));
    }

    @Test
    void onlyPmTogglesQualityGate() {
        assertMatrix("bật/tắt cổng duyệt", (u, p, t, st) -> TaskAccess.canToggleGate(u, p), EnumSet.of(Role.PM));
    }

    @Test
    void onlyPmReviewsPlansAndDeliverables() {
        assertMatrix("PM duyệt", (u, p, t, st) -> TaskAccess.canPmReview(u, p), EnumSet.of(Role.PM));
    }

    // ===== PM + Task Lead =====

    @Test
    void pmAndLeadManageTheTask() {
        assertMatrix("sửa/xóa/đổi trạng thái/hạn chót", (u, p, t, st) -> TaskAccess.canManageTask(u, t, p),
                EnumSet.of(Role.PM, Role.LEAD));
    }

    @Test
    void pmAndLeadPlanSubTasks() {
        assertMatrix("thêm/sửa/xóa việc con", (u, p, t, st) -> TaskAccess.canPlanSubTasks(u, t, p),
                EnumSet.of(Role.PM, Role.LEAD));
    }

    @Test
    void leadSubmitsTaskWhenTaskHasLead() {
        // Task đã có Task Lead: chỉ Lead nộp, PM không nộp thay (PM là người duyệt)
        assertMatrix("nộp bàn giao / gửi kế hoạch", (u, p, t, st) -> TaskAccess.canSubmitTask(u, t, p),
                EnumSet.of(Role.LEAD));
    }

    @Test
    void leadReviewsSubTasksWhenTaskHasLead() {
        assertMatrix("duyệt việc con", (u, p, t, st) -> TaskAccess.canReviewSubTask(u, t, p),
                EnumSet.of(Role.LEAD));
    }

    // ===== Việc con =====

    @Test
    void assigneeLeadAndPmTickSubTask() {
        assertMatrix("tick việc con", (u, p, t, st) -> TaskAccess.canManageSubTask(u, st, t, p),
                EnumSet.of(Role.PM, Role.LEAD, Role.SUB_ASSIGNEE));
    }

    @Test
    void onlyAssigneeSubmitsAssignedSubTask() {
        assertMatrix("nộp việc con đã giao", (u, p, t, st) -> TaskAccess.canSubmitSubTask(u, st, t, p),
                EnumSet.of(Role.SUB_ASSIGNEE));
    }

    // ===== Trường hợp đặc biệt: chưa có người phụ trách =====

    @Test
    void pmStepsInWhenTaskHasNoLead() {
        Project p = project();
        Task noLead = task(0);
        assertTrue(TaskAccess.canSubmitTask(user(Role.PM), noLead, p), "PM nộp thay khi task chưa có lead");
        assertTrue(TaskAccess.canReviewSubTask(user(Role.PM), noLead, p), "PM duyệt việc con khi task chưa có lead");
        assertFalse(TaskAccess.canSubmitTask(user(Role.OTHER_MEMBER), noLead, p));
        assertFalse(TaskAccess.canManageTask(user(Role.OTHER_MEMBER), noLead, p),
                "task chưa có lead KHÔNG có nghĩa là ai cũng sửa được");
    }

    @Test
    void unassignedSubTaskIsSubmittedByLeadOrPm() {
        Project p = project();
        Task t = task(LEAD_ID);
        SubTask unassigned = subTask(0);
        assertTrue(TaskAccess.canSubmitSubTask(user(Role.LEAD), unassigned, t, p));
        assertTrue(TaskAccess.canSubmitSubTask(user(Role.PM), unassigned, t, p));
        assertFalse(TaskAccess.canSubmitSubTask(user(Role.OTHER_MEMBER), unassigned, t, p));
        assertFalse(TaskAccess.canSubmitSubTask(user(Role.SUB_ASSIGNEE), unassigned, t, p));
    }

    @Test
    void pmOfAnotherProjectHasNoPower() {
        Project other = new Project();
        other.setId(99);
        other.setOwnerId(OTHER_ID); // người này là PM ở dự án KHÁC
        Task t = task(LEAD_ID);
        assertFalse(TaskAccess.canCreateTask(user(Role.OTHER_MEMBER), project()));
        assertFalse(TaskAccess.canManageTask(user(Role.OTHER_MEMBER), t, project()));
        assertTrue(TaskAccess.canCreateTask(user(Role.OTHER_MEMBER), other), "chỉ có quyền trong dự án của mình");
    }

    @Test
    void nullInputsAreAlwaysDenied() {
        Project p = project();
        Task t = task(LEAD_ID);
        User pm = user(Role.PM);
        assertFalse(TaskAccess.canCreateTask(null, p));
        assertFalse(TaskAccess.canCreateTask(pm, null));
        assertFalse(TaskAccess.canManageTask(user(Role.LEAD), null, null));
        assertFalse(TaskAccess.canSubmitSubTask(pm, null, t, p));
    }
}
