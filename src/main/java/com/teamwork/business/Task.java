package com.teamwork.business;

import java.io.Serializable;

/**
 * JavaBean Model đại diện cho một Thẻ công việc lớn (Task Cha) trong Bảng Kanban & Quy Trình Nghiệm Thu 2 Tầng.
 * Mỗi Task thuộc về một Project cụ thể và có trạng thái, mức độ ưu tiên, cùng biên bản bàn giao cho Trưởng Dự Án (PM).
 */
public class Task implements Serializable {

    // ===================== CÁC THUỘC TÍNH =====================

    private int id;                     // Khóa chính định danh task
    private int projectId;              // Thuộc dự án nào (Khóa ngoại trỏ đến Project.id)
    private String title;               // Tiêu đề công việc (Ví dụ: "Thiết kế CSDL")
    private String description;         // Mô tả chi tiết yêu cầu công việc
    private String status;              // Trạng thái: "TODO", "IN_PROGRESS", "SUBMITTED", "REVISE", "REJECTED", "DONE"
    private String priority;            // Mức độ ưu tiên: "HIGH", "MEDIUM", "LOW"
    private String dueDate;             // Hạn chót hoàn thành (định dạng: YYYY-MM-DD)
    private int assigneeId;             // ID Task Lead được giao việc (trỏ đến User.id)
    private String assigneeName;        // Tên hiển thị người phụ trách
    private String finalDeliverableNote;// Báo cáo tổng kết bàn giao của Task Lead cho PM
    private String pmFeedback;          // Nhận xét đánh giá / dặn dò chỉnh sửa của Trưởng Dự Án (PM)
    private String submittedAt;         // Thời điểm Task Lead nộp bàn giao
    private String reviewedAt;          // Thời điểm PM phê duyệt hoặc phản hồi

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================

    public Task() {
        this.id = 0;
        this.projectId = 0;
        this.title = "";
        this.description = "";
        this.status = "TODO";
        this.priority = "MEDIUM";
        this.dueDate = "";
        this.assigneeId = 0;
        this.assigneeName = "Chưa phân công";
        this.finalDeliverableNote = "";
        this.pmFeedback = "";
        this.submittedAt = "";
        this.reviewedAt = "";
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================

    public Task(int id, int projectId, String title, String description,
                String status, String priority, String dueDate,
                int assigneeId, String assigneeName,
                String finalDeliverableNote, String pmFeedback,
                String submittedAt, String reviewedAt) 
    {
        this.id = id;
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "TODO";
        this.priority = priority;
        this.dueDate = dueDate;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.finalDeliverableNote = (finalDeliverableNote != null) ? finalDeliverableNote.trim() : "";
        this.pmFeedback = (pmFeedback != null) ? pmFeedback.trim() : "";
        this.submittedAt = (submittedAt != null) ? submittedAt.trim() : "";
        this.reviewedAt = (reviewedAt != null) ? reviewedAt.trim() : "";
    }

    // Constructor tương thích ngược
    public Task(int id, int projectId, String title, String description,
                String status, String priority, String dueDate,
                int assigneeId, String assigneeName) 
    {
        this(id, projectId, title, description, status, priority, dueDate, assigneeId, assigneeName, "", "", "", "");
    }

    // ===================== HÀM TIỆN ÍCH PHỤC VỤ GIAO DIỆN =====================

    /**
     * Trả về lớp màu CSS Bootstrap tương ứng với mức độ ưu tiên
     */
    public String getPriorityBadgeClass() {
        if (this.priority != null && this.priority.equalsIgnoreCase("HIGH")) {
            return "bg-danger text-white";
        } else if (this.priority != null && this.priority.equalsIgnoreCase("MEDIUM")) {
            return "bg-warning text-dark";
        } else {
            return "bg-info text-dark";
        }
    }

    /**
     * Trả về tên hiển thị tiếng Việt của mức độ ưu tiên
     */
    public String getPriorityLabel() {
        if (this.priority != null && this.priority.equalsIgnoreCase("HIGH")) {
            return "Cao";
        } else if (this.priority != null && this.priority.equalsIgnoreCase("MEDIUM")) {
            return "Trung bình";
        } else {
            return "Thấp";
        }
    }

    /**
     * Trả về lớp màu CSS Bootstrap tương ứng với 5 trạng thái nghiệm thu của Task Lớn
     */
    public String getStatusBadgeClass() {
        if ("SUBMITTED".equalsIgnoreCase(status)) return "bg-warning text-dark"; // 🟡 Vàng Cam: Chờ PM duyệt
        if ("REVISE".equalsIgnoreCase(status)) return "bg-primary text-white";   // 🔵 Xanh Dương: PM cần cân chỉnh
        if ("REJECTED".equalsIgnoreCase(status)) return "bg-danger text-white";   // 🔴 Màu Đỏ: Chưa đạt yêu cầu
        if ("DONE".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status)) return "bg-success text-white"; // 🟢 Xanh Lá: Đã nghiệm thu
        if ("IN_PROGRESS".equalsIgnoreCase(status)) return "bg-info-subtle text-info-emphasis border border-info-subtle"; // 🚀 Đang làm
        return "bg-light text-secondary border"; // ⚪ TODO: Cần làm
    }

    /**
     * Trả về tên nhãn hiển thị tiếng Việt kèm icon cho Task Lớn
     */
    public String getStatusLabel() {
        if ("SUBMITTED".equalsIgnoreCase(status)) return "🟡 Chờ PM duyệt";
        if ("REVISE".equalsIgnoreCase(status)) return "🔵 Cần cân chỉnh";
        if ("REJECTED".equalsIgnoreCase(status)) return "🔴 Chưa đạt yêu cầu";
        if ("DONE".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status)) return "🟢 Đã nghiệm thu";
        if ("IN_PROGRESS".equalsIgnoreCase(status)) return "🚀 Đang làm";
        return "⚪ Cần làm";
    }

    // ===================== GETTERS & SETTERS =====================

    public int getId() {
        return this.id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return this.projectId;
    }
    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return this.title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return this.status;
    }
    public void setStatus(String status) {
        this.status = (status != null) ? status.trim().toUpperCase() : "TODO";
    }

    public String getPriority() {
        return this.priority;
    }
    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDueDate() {
        return this.dueDate;
    }
    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
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

    public String getFinalDeliverableNote() {
        return this.finalDeliverableNote;
    }
    public void setFinalDeliverableNote(String finalDeliverableNote) {
        this.finalDeliverableNote = finalDeliverableNote;
    }

    public String getPmFeedback() {
        return this.pmFeedback;
    }
    public void setPmFeedback(String pmFeedback) {
        this.pmFeedback = pmFeedback;
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
