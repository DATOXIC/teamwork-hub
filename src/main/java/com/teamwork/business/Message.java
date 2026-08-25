package com.teamwork.business;

import java.io.Serializable;

/**
 * JavaBean Model đại diện cho một Tin nhắn Thảo luận (Message) hoặc Bình luận công việc (Comment).
 * - Khi taskId == 0: Là tin nhắn thảo luận chung trong kênh Chat của Dự án.
 * - Khi taskId > 0: Là bình luận chi tiết thuộc về một Công việc (Task) cụ thể.
 */
public class Message implements Serializable {

    // ===================== CÁC THUỘC TÍNH =====================

    private int id;             // Khóa chính định danh tin nhắn
    private int projectId;      // Thuộc dự án nào (Khóa ngoại trỏ đến Project.id)
    private int taskId;         // Thuộc task nào (0 = Chat chung dự án, > 0 = Comment của task)
    private int authorId;       // ID người gửi (Khóa ngoại trỏ đến User.id)
    private String authorName;  // Tên hiển thị người gửi (Lưu sẵn để JSP không phải join bảng)
    private String content;     // Nội dung tin nhắn (Hỗ trợ #task-3, #doc-2, @username)
    private String sentAt;      // Thời điểm gửi tin nhắn (Định dạng: dd/MM/yyyy HH:mm)

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================

    /**
     * Constructor không tham số: Bắt buộc theo chuẩn JavaBean.
     */
    public Message() {
        this.id = 0;
        this.projectId = 0;
        this.taskId = 0;
        this.authorId = 0;
        this.authorName = "Ẩn danh";
        this.content = "";
        this.sentAt = "";
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================

    /**
     * Constructor đầy đủ tham số: Dùng trong MessageDB để tạo nhanh tin nhắn mẫu (Seed Data)
     * hoặc tạo tin nhắn mới từ Controller.
     */
    public Message(int id, int projectId, int taskId, int authorId,
                   String authorName, String content, String sentAt) {
        this.id = id;
        this.projectId = projectId;
        this.taskId = taskId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.content = content;
        this.sentAt = sentAt;
    }

    // ===================== CÁC HÀM TIỆN ÍCH CHO GIAO DIỆN =====================

    /**
     * Kiểm tra xem tin nhắn này có phải là bình luận của một Task cụ thể hay không.
     */
    public boolean isTaskComment() {
        return this.taskId > 0;
    }

    /**
     * Lấy ký tự chữ cái đầu tiên của tên tác giả để vẽ Avatar tròn nếu chưa có ảnh.
     * Ví dụ: "Nguyễn Văn An" -> "N", "Trưởng Nhóm Admin" -> "T".
     */
    public String getAuthorInitial() {
        if (this.authorName == null || this.authorName.trim().isEmpty()) {
            return "U";
        }
        return this.authorName.trim().substring(0, 1).toUpperCase();
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

    public int getTaskId() {
        return this.taskId;
    }
    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getAuthorId() {
        return this.authorId;
    }
    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return this.authorName;
    }
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getContent() {
        return this.content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getSentAt() {
        return this.sentAt;
    }
    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }
}
