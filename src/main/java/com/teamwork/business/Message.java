package com.teamwork.business;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JavaBean & JPA Entity đại diện cho một Tin nhắn Thảo luận (Message) hoặc Bình luận công việc (Comment).
 * - Khi taskId == null (hoặc 0): Là tin nhắn thảo luận chung trong kênh Chat của Dự án.
 * - Khi taskId > 0: Là bình luận chi tiết thuộc về một Công việc (Task) cụ thể.
 */
@Entity
@Table(name = "messages")
public class Message implements Serializable {

    // ===================== CÁC THUỘC TÍNH =====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;             // Khóa chính định danh tin nhắn

    @Column(name = "project_id", nullable = false)
    private int projectId;      // Thuộc dự án nào (Khóa ngoại trỏ đến Project.id)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @Column(name = "task_id")
    private Integer taskId;     // Thuộc task nào (null = Chat chung dự án, > 0 = Comment của task)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private Task task;

    @Column(name = "author_id")
    private Integer authorId;   // ID người gửi (Khóa ngoại trỏ đến User.id, null-safe)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", insertable = false, updatable = false)
    private User author;

    @Column(name = "author_name")
    private String authorName;  // Tên hiển thị người gửi (Lưu sẵn để JSP không phải join bảng)

    @Column(name = "content", nullable = false)
    private String content;     // Nội dung tin nhắn (Hỗ trợ #task-3, #doc-2, @username)

    @Column(name = "sent_at", insertable = false, updatable = false)
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
        return this.taskId != null ? this.taskId : 0;
    }
    public void setTaskId(int taskId) {
        this.taskId = taskId > 0 ? taskId : null;
    }
    public void setTaskId(Integer taskId) {
        this.taskId = (taskId != null && taskId > 0) ? taskId : null;
    }

    public Task getTask() {
        return this.task;
    }
    public void setTask(Task task) {
        this.task = task;
    }

    public Project getProject() {
        return this.project;
    }
    public void setProject(Project project) {
        this.project = project;
    }

    public int getAuthorId() {
        return this.authorId != null ? this.authorId : 0;
    }
    public void setAuthorId(int authorId) {
        this.authorId = authorId > 0 ? authorId : null;
    }
    public void setAuthorId(Integer authorId) {
        this.authorId = (authorId != null && authorId > 0) ? authorId : null;
    }

    public User getAuthor() {
        return this.author;
    }
    public void setAuthor(User author) {
        this.author = author;
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
