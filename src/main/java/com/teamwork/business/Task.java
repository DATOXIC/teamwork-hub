package com.teamwork.business;

import java.io.Serializable;

/**
 * JavaBean Model đại diện cho một Thẻ công việc (Task) trong Bảng Kanban.
 * Mỗi Task thuộc về một Project cụ thể và có trạng thái, mức độ ưu tiên riêng.
 */
public class Task implements Serializable {

    // ===================== CÁC THUỘC TÍNH =====================

    private int id;             // Khóa chính định danh task
    private int projectId;      // Thuộc dự án nào (Khóa ngoại trỏ đến Project.id)
    private String title;       // Tiêu đề công việc (Ví dụ: "Thiết kế CSDL")
    private String description; // Mô tả chi tiết yêu cầu công việc
    private String status;      // Trạng thái: "TODO", "IN_PROGRESS", "DONE"
    private String priority;    // Mức độ ưu tiên: "HIGH", "MEDIUM", "LOW"
    private String dueDate;     // Hạn chót hoàn thành (định dạng: YYYY-MM-DD)
    private int assigneeId;     // ID người được giao việc (trỏ đến User.id)
    private String assigneeName;// Tên hiển thị người phụ trách (lưu sẵn để JSP không phải truy vấn lại)

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================

    /**
     * Constructor không tham số: Bắt buộc theo chuẩn JavaBean.
     * Khởi tạo giá trị mặc định hợp lý cho tất cả các thuộc tính.
     */
    public Task() {
        this.id = 0;
        this.projectId = 0;
        this.title = "";
        this.description = "";
        this.status = "TODO";           // Mặc định task mới tạo luôn nằm ở cột "Cần làm"
        this.priority = "MEDIUM";       // Mặc định mức độ ưu tiên vừa phải
        this.dueDate = "";
        this.assigneeId = 0;
        this.assigneeName = "Chưa phân công"; // Hiển thị khi chưa có người nhận việc
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================

    /**
     * Constructor đầy đủ tham số: Dùng trong TaskDB để tạo nhanh dữ liệu mẫu (Seed Data).
     */
    public Task(int id, int projectId, String title, String description,
                String status, String priority, String dueDate,
                int assigneeId, String assigneeName) 
    {
        this.id = id;
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
    }

    // ===================== HÀM TIỆN ÍCH PHỤC VỤ GIAO DIỆN =====================

    /**
     * Trả về class CSS Bootstrap tương ứng với mức độ ưu tiên.
     * 
     * MỤC ĐÍCH: Thay vì để file kanban.jsp phải tự viết điều kiện if/else phức tạp
     * để chọn màu, ta tập trung logic này vào trong Model luôn.
     * JSP chỉ cần gọi ${task.priorityBadgeClass} là lấy được đúng màu cần hiển thị.
     * 
     * HIGH   => Màu đỏ (bg-danger text-white)
     * MEDIUM => Màu vàng (bg-warning text-dark)  
     * LOW    => Màu xanh dương nhạt (bg-info text-dark)
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
     * Trả về tên hiển thị tiếng Việt của mức độ ưu tiên.
     * 
     * MỤC ĐÍCH: Thay vì hiển thị chữ "HIGH", "MEDIUM", "LOW" 
     * gốc tiếng Anh lên giao diện, ta trả về tiếng Việt cho thân thiện.
     */
    public String getPriorityLabel() {
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
        this.status = status;
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
}
