package com.teamwork.business;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
    private String deliverableFile;     // Tên tệp đính kèm báo cáo / biên bản nghiệm thu chính thức
    private int qualityRating;          // Đánh giá chất lượng của PM (1 - 5 sao ⭐)
    private String planningNote;        // Ghi chú kế hoạch phân rã Task Lead gửi PM thẩm định (Cổng 1)
    private String planningReviewedAt;  // Thời điểm PM phê duyệt & khóa kế hoạch phân rã

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
        this.deliverableFile = "";
        this.qualityRating = 5;
        this.planningNote = "";
        this.planningReviewedAt = "";
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================

    public Task(int id, int projectId, String title, String description,
                String status, String priority, String dueDate,
                int assigneeId, String assigneeName,
                String finalDeliverableNote, String pmFeedback,
                String submittedAt, String reviewedAt,
                String deliverableFile, int qualityRating,
                String planningNote, String planningReviewedAt) 
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
        this.deliverableFile = (deliverableFile != null) ? deliverableFile.trim() : "";
        this.qualityRating = qualityRating > 0 ? qualityRating : 5;
        this.planningNote = (planningNote != null) ? planningNote.trim() : "";
        this.planningReviewedAt = (planningReviewedAt != null) ? planningReviewedAt.trim() : "";
    }

    // Constructor rút gọn (Dùng khi Tạo Task mới từ Form)
    public Task(int id, int projectId, String title, String description,
                String status, String priority, String dueDate,
                int assigneeId, String assigneeName) 
    {
        this(id, projectId, title, description, status, priority, dueDate, assigneeId, assigneeName, "", "", "", "", "", 5, "", "");
    }

    // ===================== HÀM TIỆN ÍCH PHỤC VỤ GIAO DIỆN =====================

    /**
     * Trả về lớp màu CSS Bootstrap tương ứng với mức độ ưu tiên
     */
    public String getPriorityBadgeClass() 
    {
        if (this.priority != null && this.priority.equalsIgnoreCase("HIGH")) 
        {
            return "bg-danger text-white";
        } 
        else if (this.priority != null && this.priority.equalsIgnoreCase("MEDIUM")) 
        {
            return "bg-warning text-dark";
        } 
        else 
        {
            return "bg-info text-dark";
        }
    }

    /**
     * Trả về tên hiển thị tiếng Việt của mức độ ưu tiên
     */
    public String getPriorityLabel() 
    {
        if (this.priority != null && this.priority.equalsIgnoreCase("HIGH")) 
        {
            return "Cao";
        } 
        else if (this.priority != null && this.priority.equalsIgnoreCase("MEDIUM")) 
        {
            return "Trung bình";
        } 
        else 
        {
            return "Thấp";
        }
    }

    /**
     * Trả về lớp màu CSS Bootstrap tương ứng với các trạng thái của Task Lớn
     */
    public String getStatusBadgeClass() 
    {
        if ("PLANNING".equalsIgnoreCase(status)) return "bg-primary-subtle text-primary border border-primary-subtle"; // 🟣 Đang chờ PM duyệt kế hoạch
        if ("SUBMITTED".equalsIgnoreCase(status)) return "bg-warning text-dark"; // 🟡 Vàng Cam: Chờ PM duyệt nghiệm thu
        if ("REVISE".equalsIgnoreCase(status)) return "bg-primary text-white";   // 🔵 Xanh Dương: PM cần cân chỉnh
        if ("REJECTED".equalsIgnoreCase(status)) return "bg-danger text-white";   // 🔴 Màu Đỏ: Chưa đạt yêu cầu
        if ("DONE".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status)) return "bg-success text-white"; // 🟢 Xanh Lá: Đã nghiệm thu
        if ("IN_PROGRESS".equalsIgnoreCase(status)) return "bg-info-subtle text-info-emphasis border border-info-subtle"; // 🚀 Đang làm (Đã khóa kế hoạch)
        return "bg-light text-secondary border"; // ⚪ TODO: Cần làm (Đang lập kế hoạch)
    }

    /**
     * Trả về tên nhãn hiển thị tiếng Việt kèm icon cho Task Lớn
     */
    public String getStatusLabel() {
        if ("PLANNING".equalsIgnoreCase(status)) return "🟣 Chờ PM duyệt kế hoạch";
        if ("SUBMITTED".equalsIgnoreCase(status)) return "🟡 Chờ PM duyệt nghiệm thu";
        if ("REVISE".equalsIgnoreCase(status)) return "🔵 Cần cân chỉnh";
        if ("REJECTED".equalsIgnoreCase(status)) return "🔴 Chưa đạt yêu cầu";
        if ("DONE".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status)) return "🟢 Đã nghiệm thu";
        if ("IN_PROGRESS".equalsIgnoreCase(status)) return "🚀 Đang làm (Đã khóa)";
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
        this.title = (title != null) ? title.trim() : "";
    }

    public String getDescription() {
        return this.description;
    }
    public void setDescription(String description) {
        this.description = (description != null) ? description.trim() : "";
    }

    public String getStatus() {
        return this.status;
    }
    public void setStatus(String status) {
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "TODO";
    }

    public String getPriority() {
        return this.priority;
    }
    public void setPriority(String priority) {
        this.priority = (priority != null && !priority.trim().isEmpty()) ? priority.trim().toUpperCase() : "MEDIUM";
    }

    public String getDueDate() {
        return this.dueDate;
    }
    public void setDueDate(String dueDate) {
        this.dueDate = (dueDate != null) ? dueDate.trim() : "";
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
        this.assigneeName = (assigneeName != null && !assigneeName.trim().isEmpty()) ? assigneeName.trim() : "Chưa phân công";
    }

    public String getFinalDeliverableNote() {
        return this.finalDeliverableNote;
    }
    public void setFinalDeliverableNote(String finalDeliverableNote) {
        this.finalDeliverableNote = (finalDeliverableNote != null) ? finalDeliverableNote.trim() : "";
    }

    public String getPmFeedback() {
        return this.pmFeedback;
    }
    public void setPmFeedback(String pmFeedback) {
        this.pmFeedback = (pmFeedback != null) ? pmFeedback.trim() : "";
    }

    public String getSubmittedAt() {
        return this.submittedAt;
    }
    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = (submittedAt != null) ? submittedAt.trim() : "";
    }

    public String getReviewedAt() {
        return this.reviewedAt;
    }
    public void setReviewedAt(String reviewedAt) {
        this.reviewedAt = (reviewedAt != null) ? reviewedAt.trim() : "";
    }

    public String getDeliverableFile() {
        return this.deliverableFile;
    }
    public void setDeliverableFile(String deliverableFile) {
        this.deliverableFile = (deliverableFile != null) ? deliverableFile.trim() : "";
    }

    public int getQualityRating() {
        return this.qualityRating;
    }
    public void setQualityRating(int qualityRating) {
        this.qualityRating = Math.max(1, Math.min(5, qualityRating));
    }

    public String getPlanningNote() {
        return this.planningNote;
    }
    public void setPlanningNote(String planningNote) {
        this.planningNote = (planningNote != null) ? planningNote.trim() : "";
    }

    public String getPlanningReviewedAt() {
        return this.planningReviewedAt;
    }
    public void setPlanningReviewedAt(String planningReviewedAt) {
        this.planningReviewedAt = (planningReviewedAt != null) ? planningReviewedAt.trim() : "";
    }

    // ===================== CÁC HÀM TIỆN ÍCH TÍNH TOÁN HẠN CHÓT (DEADLINE UTILITIES) =====================

    /**
     * Tính toán số ngày còn lại đến hạn chót (so với ngày hiện tại).
     * - Nếu trả về số dương (> 0): Còn N ngày nữa mới đến hạn.
     * - Nếu trả về 0: Hạn chót chính là ngày hôm nay.
     * - Nếu trả về số âm (< 0): Đã quá hạn |N| ngày.
     * - Nếu không có hạn chót (rỗng/null) hoặc định dạng sai: Trả về Long.MAX_VALUE.
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
     * Kiểm tra xem Task đã bị Quá Hạn hay chưa.
     * Task được xem là quá hạn khi:
     * 1. Chưa hoàn thành (status khác "DONE")
     * 2. Có ngày hạn chót và ngày hạn chót đã trôi qua trước ngày hôm nay (daysRemaining < 0)
     */
    public boolean isOverdue() {
        if ("DONE".equalsIgnoreCase(this.status)) {
            return false;
        }
        long days = getDaysRemaining();
        return (days < 0 && days != Long.MAX_VALUE);
    }

    /**
     * Kiểm tra xem Task có đang Sắp Đến Hạn (Khẩn Cấp trong 0 đến 2 ngày) hay không.
     */
    public boolean isDueSoon() {
        if ("DONE".equalsIgnoreCase(this.status)) {
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
     * - "ON_TRACK": Đang trong hạn an toàn (🟢)
     * - "COMPLETED_ON_TIME": Đã hoàn thành đúng hạn (✅)
     * - "COMPLETED_LATE": Đã hoàn thành nhưng trễ hạn (⚠️)
     * - "NO_DEADLINE": Không thiết lập hạn chót
     */
    public String getDeadlineStatus() 
    {
        if (this.dueDate == null || this.dueDate.trim().isEmpty()) 
        {
            return "NO_DEADLINE";
        }

        if ("DONE".equalsIgnoreCase(this.status)) 
        {
            // Nếu đã xong, kiểm tra xem có nộp đúng hạn không
            if (this.submittedAt != null && !this.submittedAt.trim().isEmpty()) 
            {
                try 
                {
                    String subDateStr = this.submittedAt.trim().substring(0, 10);
                    LocalDate subDate;
                    if (subDateStr.contains("/")) 
                    {
                        subDate = LocalDate.parse(subDateStr, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    } 
                    else 
                    {
                        subDate = LocalDate.parse(subDateStr);
                    }
                    LocalDate due = LocalDate.parse(this.dueDate.trim());
                    if (subDate.isAfter(due)) 
                    {
                        return "COMPLETED_LATE";
                    }
                } 
                catch (Exception ignored) 
                {

                }
            }
            return "COMPLETED_ON_TIME";
        }

        long days = getDaysRemaining();
        if (days < 0) 
        {
            return "OVERDUE";
        } 
        else if (days == 0) 
        {
            return "DUE_TODAY";
        } 
        else if (days <= 2) 
        {
            return "DUE_SOON";
        } 
        else 
        {
            return "ON_TRACK";
        }
    }

    /**
     * Trả về lớp màu CSS Bootstrap tương ứng với trạng thái hạn chót để hiển thị Badge trực quan
     */
    public String getDeadlineBadgeClass() 
    {
        String deadlineStatus = getDeadlineStatus();
        switch (deadlineStatus) 
        {
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
     * Trả về nhãn chữ tiếng Việt định dạng đẹp kèm biểu tượng và số ngày đếm ngược
     */
    public String getDeadlineLabel() 
    {
        if (this.dueDate == null || this.dueDate.trim().isEmpty()) 
        {
            return "Chưa đặt hạn chót";
        }

        String deadlineStatus = getDeadlineStatus();
        long days = getDaysRemaining();

        switch (deadlineStatus) 
        {
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
}
