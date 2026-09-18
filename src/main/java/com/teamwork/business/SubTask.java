package com.teamwork.business;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * JavaBean & JPA Entity đại diện cho một Việc Con (Sub-task) trong Cây Phân Cấp & Quy Trình Nghiệm Thu 5 Cấp Độ.
 * - Quản lý 5 trạng thái: TODO (⚪), SUBMITTED (🟡), REVISE (🔵), REJECTED (🔴), APPROVED (🟢)
 * - Quản lý Hạn chót riêng (dueDate - YYYY-MM-DD) ràng buộc không được vượt quá Task cha
 * - Lưu vết ghi chú kết quả nộp bài của Cấp dưới (submissionNote) và nhận xét của Task Lead (feedbackNote)
 */
@Entity
@Table(name = "subtasks")
public class SubTask implements Serializable {

    // ===================== CÁC THUỘC TÍNH =====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;                  // Khóa chính định danh việc con

    @Column(name = "task_id", nullable = false)
    private int taskId;              // Thuộc task cha nào (Khóa ngoại trỏ đến Task.id)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private Task task;

    @Column(name = "title", nullable = false)
    private String title;            // Tiêu đề việc con (Ví dụ: "Viết cấu hình Dockerfile")

    @Column(name = "assignee_id")
    private Integer assigneeId;      // ID thành viên được giao việc con (trỏ đến User.id, null-safe)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", insertable = false, updatable = false)
    private User assignee;

    @Transient
    private String assigneeName;     // Tên hiển thị người làm việc con

    @Transient
    private boolean completed;       // Trạng thái cờ hoàn thành (true khi status = "DONE" hoặc "APPROVED")

    @Column(name = "status")
    private String status;           // 6 Trạng thái: "TODO", "SUBMITTED", "REVISE", "REJECTED", "DONE", "APPROVED"

    @Column(name = "due_date")
    private String dueDate;          // Hạn chót hoàn thành việc con (định dạng: YYYY-MM-DD)

    @Column(name = "submission_note")
    private String submissionNote;   // Lời nhắn nộp bài / link kết quả bàn giao của cấp dưới

    @Column(name = "feedback_note")
    private String feedbackNote;     // Ý kiến nhận xét / lý do trả về của Task Lead

    @Column(name = "submitted_at", insertable = false, updatable = false)
    private String submittedAt;      // Thời điểm nộp bài (định dạng: YYYY-MM-DD HH:mm)

    @Column(name = "reviewed_at", insertable = false, updatable = false)
    private String reviewedAt;       // Thời điểm duyệt / phản hồi (định dạng: YYYY-MM-DD HH:mm)

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================

    public SubTask() {
        this.id = 0;
        this.taskId = 0;
        this.title = "";
        this.assigneeId = 0;
        this.assigneeName = "Chưa phân công";
        this.completed = false;
        this.status = "TODO";
        this.dueDate = "";
        this.submissionNote = "";
        this.feedbackNote = "";
        this.submittedAt = "";
        this.reviewedAt = "";
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================

    public SubTask(int id, int taskId, String title, int assigneeId, String assigneeName, 
                   String status, String dueDate, String submissionNote, String feedbackNote, 
                   String submittedAt, String reviewedAt) {
        this.id = id;
        this.taskId = taskId;
        this.title = title;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "TODO";
        this.completed = "APPROVED".equalsIgnoreCase(this.status) || "DONE".equalsIgnoreCase(this.status);
        this.dueDate = (dueDate != null) ? dueDate.trim() : "";
        this.submissionNote = (submissionNote != null) ? submissionNote.trim() : "";
        this.feedbackNote = (feedbackNote != null) ? feedbackNote.trim() : "";
        this.submittedAt = (submittedAt != null) ? submittedAt.trim() : "";
        this.reviewedAt = (reviewedAt != null) ? reviewedAt.trim() : "";
    }

    // Constructor tương thích ngược 1: không có dueDate
    public SubTask(int id, int taskId, String title, int assigneeId, String assigneeName, 
                   String status, String submissionNote, String feedbackNote, 
                   String submittedAt, String reviewedAt) {
        this(id, taskId, title, assigneeId, assigneeName, status, "", submissionNote, feedbackNote, submittedAt, reviewedAt);
    }

    // Constructor tương thích ngược 2: dạng đơn giản
    public SubTask(int id, int taskId, String title, int assigneeId, String assigneeName, boolean completed) {
        this(id, taskId, title, assigneeId, assigneeName, completed ? "DONE" : "TODO", "", "", "", "", "");
    }

    // ===================== CÁC HÀM TIỆN ÍCH TRẠNG THÁI & MÀU SẮC =====================

    /**
     * Kiểm tra xem việc con đã hoàn thành (DONE hoặc APPROVED) hay chưa
     */
    public boolean isCompleted() {
        return "APPROVED".equalsIgnoreCase(this.status) || "DONE".equalsIgnoreCase(this.status) || this.completed;
    }

    /**
     * Trả về lớp màu CSS Bootstrap tương ứng với các trạng thái
     */
    public String getStatusBadgeClass() {
        if ("SUBMITTED".equalsIgnoreCase(this.status)) return "bg-warning text-dark";
        if ("REVISE".equalsIgnoreCase(this.status)) return "bg-primary text-white"; // 🔵 Màu Xanh Dương: Cần cân chỉnh nhỏ
        if ("REJECTED".equalsIgnoreCase(this.status)) return "bg-danger text-white"; // 🔴 Màu Đỏ: Chưa đạt yêu cầu
        if ("APPROVED".equalsIgnoreCase(this.status)) return "bg-success text-white"; // 🟢 Màu Xanh Lá: Đã nghiệm thu
        if ("DONE".equalsIgnoreCase(this.status) || isCompleted()) return "bg-success-subtle text-success border border-success-subtle"; // 🟢 Màu Xanh Nhạt: Đã xong
        return "bg-light text-secondary border"; // ⚪ TODO: Đang làm
    }

    /**
     * Trả về tên nhãn hiển thị trực quan tiếng Việt kèm icon
     */
    public String getStatusLabel() {
        if ("SUBMITTED".equalsIgnoreCase(this.status)) return "🟡 Chờ duyệt";
        if ("REVISE".equalsIgnoreCase(this.status)) return "🔵 Cần cân chỉnh";
        if ("REJECTED".equalsIgnoreCase(this.status)) return "🔴 Chưa đạt yêu cầu";
        if ("APPROVED".equalsIgnoreCase(this.status)) return "🟢 Đã nghiệm thu";
        if ("DONE".equalsIgnoreCase(this.status)) return "✅ Đã xong";
        return "⚪ Đang làm";
    }

    // ===================== CÁC HÀM TIỆN ÍCH TÍNH TOÁN HẠN CHÓT (DEADLINE UTILITIES) =====================

    /**
     * Tính toán số ngày còn lại đến hạn chót của việc con (so với ngày hiện tại).
     * - Số dương (> 0): Còn N ngày.
     * - Số 0: Hạn chót chính là hôm nay.
     * - Số âm (< 0): Đã quá hạn |N| ngày.
     * - Không có hạn chót: Trả về Long.MAX_VALUE.
     */
    public long getDaysRemaining() {
        if (this.dueDate == null || this.dueDate.trim().isEmpty()) {
            return Long.MAX_VALUE;
        }

        try {
            LocalDate today = LocalDate.now();
            LocalDate targetDate = LocalDate.parse(this.dueDate.trim());
            return ChronoUnit.DAYS.between(today, targetDate);
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * Kiểm tra xem việc con có bị Quá Hạn hay không.
     * Quá hạn khi: Chưa được duyệt ("APPROVED") và ngày hiện tại đã vượt qua dueDate (daysRemaining < 0).
     */
    public boolean isOverdue() {
        if (isCompleted()) {
            return false;
        }
        long days = getDaysRemaining();
        return (days < 0 && days != Long.MAX_VALUE);
    }

    /**
     * Kiểm tra xem việc con có Sắp Đến Hạn (khẩn cấp trong 0 đến 2 ngày) hay không.
     */
    public boolean isDueSoon() {
        if (isCompleted()) {
            return false;
        }
        long days = getDaysRemaining();
        return (days >= 0 && days <= 2);
    }

    /**
     * Trả về mã chuỗi trạng thái hạn chót:
     * - "OVERDUE": Quá hạn (🔴)
     * - "DUE_TODAY": Hôm nay đến hạn (🚨)
     * - "DUE_SOON": Sắp đến hạn trong 1-2 ngày (🟠)
     * - "ON_TRACK": Đúng tiến độ (🟢)
     * - "COMPLETED_ON_TIME": Hoàn thành đúng hạn (✅)
     * - "COMPLETED_LATE": Hoàn thành trễ hạn (⚠️)
     * - "NO_DEADLINE": Chưa đặt hạn chót
     */
    public String getDeadlineStatus() {
        if (this.dueDate == null || this.dueDate.trim().isEmpty()) {
            return "NO_DEADLINE";
        }

        if (isCompleted()) {
            if (this.submittedAt != null && !this.submittedAt.trim().isEmpty()) {
                try {
                    String subDateStr = this.submittedAt.trim().substring(0, 10);
                    LocalDate subDate = LocalDate.parse(subDateStr);
                    LocalDate due = LocalDate.parse(this.dueDate.trim());
                    if (subDate.isAfter(due)) {
                        return "COMPLETED_LATE";
                    }
                } catch (Exception ignored) {
                }
            }
            return "COMPLETED_ON_TIME";
        }

        long days = getDaysRemaining();
        if (days < 0) {
            return "OVERDUE";
        } else if (days == 0) {
            return "DUE_TODAY";
        } else if (days <= 2) {
            return "DUE_SOON";
        } else {
            return "ON_TRACK";
        }
    }

    /**
     * Trả về class Bootstrap màu sắc tương ứng với trạng thái hạn chót
     */
    public String getDeadlineBadgeClass() {
        String deadlineStatus = getDeadlineStatus();
        switch (deadlineStatus) {
            case "OVERDUE":
                return "bg-danger text-white border-danger shadow-2xs";
            case "DUE_TODAY":
                return "bg-danger-subtle text-danger border-danger fw-bold shadow-2xs";
            case "DUE_SOON":
                return "bg-warning-subtle text-dark border-warning fw-semibold shadow-2xs";
            case "ON_TRACK":
                return "bg-light text-secondary border shadow-2xs";
            case "COMPLETED_ON_TIME":
                return "bg-success-subtle text-success border-success-subtle shadow-2xs";
            case "COMPLETED_LATE":
                return "bg-secondary-subtle text-secondary border shadow-2xs";
            default:
                return "bg-light text-muted border";
        }
    }

    /**
     * Trả về nhãn tiếng Việt định dạng đẹp kèm số ngày đếm ngược
     */
    public String getDeadlineLabel() {
        if (this.dueDate == null || this.dueDate.trim().isEmpty()) {
            return "Chưa đặt hạn chót";
        }

        String deadlineStatus = getDeadlineStatus();
        long days = getDaysRemaining();

        switch (deadlineStatus) {
            case "OVERDUE":
                return "🔴 Quá hạn " + Math.abs(days) + " ngày (" + this.dueDate + ")";
            case "DUE_TODAY":
                return "🚨 Hạn chót hôm nay (" + this.dueDate + ")";
            case "DUE_SOON":
                if (days == 1) {
                    return "🟠 Hạn chót ngày mai (" + this.dueDate + ")";
                }
                return "🟠 Còn " + days + " ngày (" + this.dueDate + ")";
            case "ON_TRACK":
                return "🟢 Còn " + days + " ngày (" + this.dueDate + ")";
            case "COMPLETED_ON_TIME":
                return "✅ Hoàn thành đúng hạn (" + this.dueDate + ")";
            case "COMPLETED_LATE":
                return "⚠️ Hoàn thành trễ hạn (" + this.dueDate + ")";
            default:
                return this.dueDate;
        }
    }

    // ===================== GETTERS & SETTERS =====================

    public int getId() {
        return this.id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getTaskId() {
        return this.taskId;
    }
    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return this.title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public int getAssigneeId() {
        return this.assigneeId != null ? this.assigneeId : 0;
    }
    public void setAssigneeId(int assigneeId) {
        this.assigneeId = (assigneeId > 0) ? assigneeId : null;
    }
    public void setAssigneeId(Integer assigneeId) {
        this.assigneeId = (assigneeId != null && assigneeId > 0) ? assigneeId : null;
    }

    public Task getTask() {
        return this.task;
    }
    public void setTask(Task task) {
        this.task = task;
    }

    public User getAssignee() {
        return this.assignee;
    }
    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public String getAssigneeName() {
        return this.assigneeName;
    }
    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed && !"APPROVED".equalsIgnoreCase(this.status)) {
            this.status = "DONE";
        } else if (!completed) {
            this.status = "TODO";
        }
    }

    public String getStatus() {
        return this.status;
    }
    public void setStatus(String status) {
        this.status = (status != null) ? status.trim().toUpperCase() : "TODO";
        this.completed = "APPROVED".equalsIgnoreCase(this.status) || "DONE".equalsIgnoreCase(this.status);
    }

    public String getDueDate() {
        return this.dueDate;
    }
    public void setDueDate(String dueDate) {
        this.dueDate = (dueDate != null) ? dueDate.trim() : "";
    }

    public String getSubmissionNote() {
        return this.submissionNote;
    }
    public void setSubmissionNote(String submissionNote) {
        this.submissionNote = submissionNote;
    }

    public String getFeedbackNote() {
        return this.feedbackNote;
    }
    public void setFeedbackNote(String feedbackNote) {
        this.feedbackNote = feedbackNote;
    }

    public String getSubmittedAt() {
        return this.submittedAt;
    }
    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getReviewedAt() {
        return this.reviewedAt;
    }
    public void setReviewedAt(String reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}

