package com.teamwork.business;

import java.io.Serializable;

/**
 * JavaBean Model đại diện cho một Việc Con (Sub-task) trong Cây Phân Cấp Công Việc.
 * - Mỗi SubTask luôn trực thuộc một Task Cha (Task.id).
 * - Người đứng đầu Task Cha (Task Lead) có thể chia nhỏ và phân công SubTask cho từng thành viên.
 */
public class SubTask implements Serializable {

    // ===================== CÁC THUỘC TÍNH =====================

    private int id;              // Khóa chính định danh việc con
    private int taskId;          // Thuộc task cha nào (Khóa ngoại trỏ đến Task.id)
    private String title;        // Tiêu đề việc con (Ví dụ: "Viết cấu hình Dockerfile")
    private int assigneeId;      // ID thành viên được giao việc con (trỏ đến User.id)
    private String assigneeName; // Tên hiển thị người làm việc con
    private boolean completed;   // Trạng thái đã hoàn thành hay chưa (true: Đã xong, false: Chưa xong)

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================

    /**
     * Constructor không tham số: Bắt buộc theo chuẩn JavaBean.
     */
    public SubTask() {
        this.id = 0;
        this.taskId = 0;
        this.title = "";
        this.assigneeId = 0;
        this.assigneeName = "Chưa phân công";
        this.completed = false;
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================

    /**
     * Constructor đầy đủ tham số: Dùng trong SubTaskDB để khởi tạo nhanh dữ liệu mẫu
     * hoặc tạo việc con mới từ Controller.
     */
    public SubTask(int id, int taskId, String title, int assigneeId, String assigneeName, boolean completed) {
        this.id = id;
        this.taskId = taskId;
        this.title = title;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.completed = completed;
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

    public boolean isCompleted() {
        return this.completed;
    }
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
