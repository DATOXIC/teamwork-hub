package com.teamwork.business;

import java.io.Serializable;

/**
 * JavaBean Model đại diện cho một Việc Con (Sub-task) trong Cây Phân Cấp & Quy Trình Nghiệm Thu 5 Cấp Độ.
 * - Quản lý 5 trạng thái: TODO (⚪), SUBMITTED (🟡), REVISE (🔵), REJECTED (🔴), APPROVED (🟢)
 * - Lưu vết ghi chú kết quả nộp bài của Cấp dưới (submissionNote) và nhận xét của Task Lead (feedbackNote)
 */
public class SubTask implements Serializable {

    // ===================== CÁC THUỘC TÍNH =====================

    private int id;                  // Khóa chính định danh việc con
    private int taskId;              // Thuộc task cha nào (Khóa ngoại trỏ đến Task.id)
    private String title;            // Tiêu đề việc con (Ví dụ: "Viết cấu hình Dockerfile")
    private int assigneeId;          // ID thành viên được giao việc con (trỏ đến User.id)
    private String assigneeName;     // Tên hiển thị người làm việc con
    private boolean completed;       // Trạng thái cờ hoàn thành (true khi status = "APPROVED")
    private String status;           // 5 Trạng thái: "TODO", "SUBMITTED", "REVISE", "REJECTED", "APPROVED"
    private String submissionNote;   // Lời nhắn nộp bài / link kết quả bàn giao của cấp dưới
    private String feedbackNote;     // Ý kiến nhận xét / lý do trả về của Task Lead
    private String submittedAt;      // Thời điểm nộp bài
    private String reviewedAt;       // Thời điểm duyệt / phản hồi

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================

    public SubTask() {
        this.id = 0;
        this.taskId = 0;
        this.title = "";
        this.assigneeId = 0;
        this.assigneeName = "Chưa phân công";
        this.completed = false;
        this.status = "TODO";
        this.submissionNote = "";
        this.feedbackNote = "";
        this.submittedAt = "";
        this.reviewedAt = "";
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================

    public SubTask(int id, int taskId, String title, int assigneeId, String assigneeName, 
                   String status, String submissionNote, String feedbackNote, 
                   String submittedAt, String reviewedAt) {
        this.id = id;
        this.taskId = taskId;
        this.title = title;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "TODO";
        this.completed = "APPROVED".equalsIgnoreCase(this.status);
        this.submissionNote = (submissionNote != null) ? submissionNote.trim() : "";
        this.feedbackNote = (feedbackNote != null) ? feedbackNote.trim() : "";
        this.submittedAt = (submittedAt != null) ? submittedAt.trim() : "";
        this.reviewedAt = (reviewedAt != null) ? reviewedAt.trim() : "";
    }

    // Constructor tương thích ngược
    public SubTask(int id, int taskId, String title, int assigneeId, String assigneeName, boolean completed) {
        this(id, taskId, title, assigneeId, assigneeName, completed ? "APPROVED" : "TODO", "", "", "", "");
    }

    // ===================== CÁC HÀM TIỆN ÍCH TRẠNG THÁI & MÀU SẮC =====================

    /**
     * Kiểm tra xem việc con đã được nghiệm thu ĐẠT hay chưa
     */
    public boolean isCompleted() {
        return "APPROVED".equalsIgnoreCase(this.status) || this.completed;
    }

    /**
     * Trả về lớp màu CSS Bootstrap tương ứng với 5 trạng thái
     */
    public String getStatusBadgeClass() {
        if ("SUBMITTED".equalsIgnoreCase(status)) return "bg-warning text-dark";
        if ("REVISE".equalsIgnoreCase(status)) return "bg-primary text-white"; // 🔵 Màu Xanh Dương: Cần cân chỉnh nhỏ
        if ("REJECTED".equalsIgnoreCase(status)) return "bg-danger text-white"; // 🔴 Màu Đỏ: Chưa đạt yêu cầu
        if ("APPROVED".equalsIgnoreCase(status) || isCompleted()) return "bg-success text-white"; // 🟢 Màu Xanh Lá: Đã nghiệm thu
        return "bg-light text-secondary border"; // ⚪ TODO: Đang làm
    }

    /**
     * Trả về tên nhãn hiển thị trực quan tiếng Việt kèm icon
     */
    public String getStatusLabel() {
        if ("SUBMITTED".equalsIgnoreCase(status)) return "🟡 Chờ duyệt";
        if ("REVISE".equalsIgnoreCase(status)) return "🔵 Cần cân chỉnh";
        if ("REJECTED".equalsIgnoreCase(status)) return "🔴 Chưa đạt yêu cầu";
        if ("APPROVED".equalsIgnoreCase(status) || isCompleted()) return "🟢 Đã nghiệm thu";
        return "⚪ Đang làm";
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
        return this.assigneeId;
    }
    public void setAssigneeId(int assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getAssigneeName() {
        return this.assigneeName;
    }
    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed) {
            this.status = "APPROVED";
        }
    }

    public String getStatus() {
        return this.status;
    }
    public void setStatus(String status) {
        this.status = (status != null) ? status.trim().toUpperCase() : "TODO";
        this.completed = "APPROVED".equalsIgnoreCase(this.status);
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
