package com.teamwork.business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaBean DTO: Đại diện cho Gói Dữ Liệu Khối Lượng Công Việc của một Thành Viên trong Dự Án.
 * - Dùng để hiển thị Dải Avatar Lọc Nhanh trên Bảng Kanban
 * - Dùng để hiển thị Thẻ Hồ Sơ Đồng Đội (Social Profile Card) kèm Danh Sách Task Lớn Đang Chủ Trì
 * - Cung cấp danh sách relatedTaskIds để JavaScript lọc thẻ công việc trong 0.01s.
 */
public class UserWorkload implements Serializable {

    // ===================== CÁC THUỘC TÍNH =====================

    private User user;                       // Thông tin cá nhân của thành viên (Tên, Email, Role)
    private int leadTaskCount;               // Số Task lớn đang giữ vai trò Task Lead (Chủ trì)
    private int subTaskCount;                // Tổng số Việc Con được phân công thực hiện
    private int completedSubTaskCount;       // Số Việc Con đã hoàn thành [☑]
    private List<Integer> relatedTaskIds;    // Danh sách ID các Task mà người này có tham gia
    private List<Task> leadTasks;            // Danh sách chi tiết các Task lớn mà người này làm Lead

    // Thống kê phân rã trạng thái chuẩn ClickUp Workload
    private int todoCount;                   // Số Task lớn đang ở trạng thái TODO
    private int inProgressCount;             // Số Task lớn đang ở trạng thái IN_PROGRESS
    private int submittedCount;              // Số Task lớn đã nộp bàn giao SUBMITTED (Chờ PM duyệt)
    private int doneCount;                   // Số Task lớn đã hoàn thành DONE
    private int overdueCount;                // Số Task lớn bị quá hạn

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================

    public UserWorkload() {
        this.user = new User();
        this.leadTaskCount = 0;
        this.subTaskCount = 0;
        this.completedSubTaskCount = 0;
        this.relatedTaskIds = new ArrayList<>();
        this.leadTasks = new ArrayList<>();
        this.todoCount = 0;
        this.inProgressCount = 0;
        this.submittedCount = 0;
        this.doneCount = 0;
        this.overdueCount = 0;
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================

    public UserWorkload(User user, int leadTaskCount, int subTaskCount, int completedSubTaskCount, List<Integer> relatedTaskIds, List<Task> leadTasks) {
        this.user = user;
        this.leadTaskCount = leadTaskCount;
        this.subTaskCount = subTaskCount;
        this.completedSubTaskCount = completedSubTaskCount;
        this.relatedTaskIds = (relatedTaskIds != null) ? relatedTaskIds : new ArrayList<>();
        this.leadTasks = (leadTasks != null) ? leadTasks : new ArrayList<>();
        this.todoCount = 0;
        this.inProgressCount = 0;
        this.submittedCount = 0;
        this.doneCount = 0;
        this.overdueCount = 0;
    }

    // ===================== CÁC HÀM TIỆN ÍCH TÍNH TOÁN =====================

    public int getTotalWorkCount() {
        return this.leadTaskCount + this.subTaskCount;
    }

    public int getTotalTasks() {
        return this.leadTaskCount + this.subTaskCount;
    }

    public int getInProgressTasks() {
        return Math.max(0, (this.leadTaskCount + this.subTaskCount) - this.completedSubTaskCount);
    }

    public int getDoneTasks() {
        return this.completedSubTaskCount;
    }

    public int getOverdueTasks() {
        int count = 0;
        if (this.leadTasks != null) {
            for (Task t : this.leadTasks) {
                if (t != null && t.isOverdue()) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getCompletionRate() {
        int total = getTotalTasks();
        if (total <= 0) return 0;
        int rate = (this.completedSubTaskCount * 100) / total;
        return Math.min(100, Math.max(0, rate));
    }

    public String getRelatedTaskIdsJoined() {
        if (this.relatedTaskIds == null || this.relatedTaskIds.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.relatedTaskIds.size(); i++) {
            sb.append(this.relatedTaskIds.get(i));
            if (i < this.relatedTaskIds.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    // ===================== GETTERS & SETTERS =====================

    public User getUser() {
        return this.user;
    }
    public void setUser(User user) {
        this.user = user;
    }

    public int getLeadTaskCount() {
        return this.leadTaskCount;
    }
    public void setLeadTaskCount(int leadTaskCount) {
        this.leadTaskCount = leadTaskCount;
    }

    public int getSubTaskCount() {
        return this.subTaskCount;
    }
    public void setSubTaskCount(int subTaskCount) {
        this.subTaskCount = subTaskCount;
    }

    public int getCompletedSubTaskCount() {
        return this.completedSubTaskCount;
    }
    public void setCompletedSubTaskCount(int completedSubTaskCount) {
        this.completedSubTaskCount = completedSubTaskCount;
    }

    public List<Integer> getRelatedTaskIds() {
        return this.relatedTaskIds;
    }
    public void setRelatedTaskIds(List<Integer> relatedTaskIds) {
        this.relatedTaskIds = relatedTaskIds;
    }

    public List<Task> getLeadTasks() {
        return this.leadTasks;
    }
    public void setLeadTasks(List<Task> leadTasks) {
        this.leadTasks = leadTasks;
    }

    public int getTodoCount() {
        return this.todoCount;
    }
    public void setTodoCount(int todoCount) {
        this.todoCount = todoCount;
    }

    public int getInProgressCount() {
        return this.inProgressCount;
    }
    public void setInProgressCount(int inProgressCount) {
        this.inProgressCount = inProgressCount;
    }

    public int getSubmittedCount() {
        return this.submittedCount;
    }
    public void setSubmittedCount(int submittedCount) {
        this.submittedCount = submittedCount;
    }

    public int getDoneCount() {
        return this.doneCount;
    }
    public void setDoneCount(int doneCount) {
        this.doneCount = doneCount;
    }

    public int getOverdueCount() {
        return this.overdueCount;
    }
    public void setOverdueCount(int overdueCount) {
        this.overdueCount = overdueCount;
    }

    /**
     * Tỷ lệ hoàn thành % thực tế của thành viên (Ưu tiên theo số task hoàn thành)
     */
    public int getMemberProgressPercentage() {
        if (this.leadTaskCount > 0) {
            return (this.doneCount * 100) / this.leadTaskCount;
        }
        if (this.subTaskCount > 0) {
            return (this.completedSubTaskCount * 100) / this.subTaskCount;
        }
        return 0;
    }
}
