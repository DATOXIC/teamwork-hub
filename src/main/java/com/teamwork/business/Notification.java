package com.teamwork.business;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
 * JavaBean & JPA Entity: Đại diện cho một Bản Tin Thông Báo (Notification).
 */
@Entity
@Table(name = "notifications")
public class Notification implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "recipient_id", nullable = false)
    private int recipientId;          // ID của người nhận thông báo

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", insertable = false, updatable = false)
    private User recipient;

    @Column(name = "title", nullable = false)
    private String title;             // Tiêu đề: "Giao việc mới", "Lời mời dự án", "Tiến độ 100%"

    @Column(name = "content")
    private String content;           // Nội dung chi tiết thông báo

    @Column(name = "link")
    private String link;              // Đường link hành động (URL) khi nhấp chuột vào

    @Column(name = "type")
    private String type;              // Loại: "INVITE", "TASK_ASSIGNED", "PROGRESS", "COMMENT", "GENERAL"

    @Column(name = "is_read")
    private boolean isRead;           // Trạng thái đã đọc (true) hay chưa đọc (false)

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;  // Thời điểm phát thông báo

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================
    public Notification() {
        this.id = 0;
        this.recipientId = 0;
        this.title = "";
        this.content = "";
        this.link = "#";
        this.type = "GENERAL";
        this.isRead = false;
        this.createdAt = null;
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================
    public Notification(int id, int recipientId, String title, String content, String link, String type, boolean isRead, LocalDateTime createdAt) {
        this.id = id;
        this.recipientId = recipientId;
        this.title = title;
        this.content = content;
        this.link = link;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    // ===================== CÁC HÀM TIỆN ÍCH HIỂN THỊ =====================

    /**
     * Trả về icon Bootstrap tương ứng với từng loại thông báo
     */
    public String getIconClass() {
        if ("INVITE".equalsIgnoreCase(type)) return "bi-envelope-paper-heart-fill text-primary";
        if ("TASK_ASSIGNED".equalsIgnoreCase(type)) return "bi-person-badge-fill text-warning";
        if ("PROGRESS".equalsIgnoreCase(type)) return "bi-trophy-fill text-success";
        if ("COMMENT".equalsIgnoreCase(type)) return "bi-chat-square-dots-fill text-info";
        return "bi-bell-fill text-secondary";
    }

    // ===================== GETTERS & SETTERS =====================
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getRecipientId() {
        return recipientId;
    }
    public void setRecipientId(int recipientId) {
        this.recipientId = recipientId;
    }

    public User getRecipient() {
        return recipient;
    }
    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getLink() {
        return link;
    }
    public void setLink(String link) {
        this.link = link;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }
    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    public String getCreatedAt() {
        return createdAt != null ? createdAt.format(DISPLAY_FMT) : "";
    }
    public LocalDateTime getRawCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
