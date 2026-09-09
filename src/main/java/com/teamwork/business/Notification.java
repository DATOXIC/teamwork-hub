package com.teamwork.business;

import java.io.Serializable;

/**
 * JavaBean Model: Đại diện cho một Bản Tin Thông Báo (Notification).
 * - Phục vụ hiển thị trên Quả Chuông 🔔 Header và Trung Tâm Thông Báo
 * - Hỗ trợ Deep-linking: Bấm vào thông báo là chuyển hướng thẳng tới Task/Dự án tương ứng
 */
public class Notification implements Serializable {

    private int id;
    private int recipientId;          // ID của người nhận thông báo
    private String title;             // Tiêu đề: "Giao việc mới", "Lời mời dự án", "Tiến độ 100%"
    private String content;           // Nội dung chi tiết thông báo
    private String link;              // Đường link hành động (URL) khi nhấp chuột vào
    private String type;              // Loại: "INVITE", "TASK_ASSIGNED", "PROGRESS", "COMMENT", "GENERAL"
    private boolean isRead;           // Trạng thái đã đọc (true) hay chưa đọc (false)
    private String createdAt;         // Thời điểm phát thông báo (dd/MM/yyyy HH:mm)

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================
    public Notification() {
        this.id = 0;
        this.recipientId = 0;
        this.title = "";
        this.content = "";
        this.link = "#";
        this.type = "GENERAL";
        this.isRead = false;
        this.createdAt = "";
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================
    public Notification(int id, int recipientId, String title, String content, String link, String type, boolean isRead, String createdAt) {
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
        if ("TASK_ASSIGNED".equalsIgnoreCase(type) || "TASK".equalsIgnoreCase(type)
                || "TASK_ASSIGN".equalsIgnoreCase(type)) return "bi-person-badge-fill text-warning";
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
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
