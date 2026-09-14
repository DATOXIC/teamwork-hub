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
import jakarta.persistence.Transient;

/**
 * JavaBean & JPA Entity: Đại diện cho Lời Mời / Yêu Cầu Gia Nhập Dự Án 2 Chiều (Project Invite & Join Request).
 * - Chiều 1: type = "INVITATION" (PM gửi lời mời cho Thành viên)
 * - Chiều 2: type = "JOIN_REQUEST" (Thành viên nhập Mã Dự Án xin gia nhập)
 * - Tự động tính toán hạn hết hạn 7 ngày (expiredAt)
 */
@Entity
@Table(name = "project_invites")
public class ProjectInvite implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "project_id", nullable = false)
    private int projectId;            // ID của Dự án

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @Transient
    private String projectName;       // Tên Dự án

    @Transient
    private String projectCode;       // Mã Dự Án (VD: TW-HUB-01)

    @Column(name = "type", nullable = false)
    private String type;              // "INVITATION" (PM mời) hoặc "JOIN_REQUEST" (Xin gia nhập)

    @Column(name = "sender_id", nullable = false)
    private int senderId;             // ID người gửi

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", insertable = false, updatable = false)
    private User sender;

    @Transient
    private String senderName;        // Tên người gửi

    @Column(name = "receiver_id", nullable = false)
    private int receiverId;           // ID người nhận có thẩm quyền duyệt

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", insertable = false, updatable = false)
    private User receiver;

    @Transient
    private String receiverName;      // Tên người nhận

    @Column(name = "status", nullable = false)
    private String status;            // "PENDING", "ACCEPTED", "REJECTED", "REVOKED", "EXPIRED"

    @Column(name = "created_at", insertable = false, updatable = false)
    private String createdAt;         // Thời điểm tạo (dd/MM/yyyy HH:mm)

    @Column(name = "expired_at", insertable = false, updatable = false)
    private String expiredAt;         // Thời điểm hết hạn (dd/MM/yyyy HH:mm - Mặc định +7 ngày)

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================
    public ProjectInvite() {
        this.id = 0;
        this.projectId = 0;
        this.projectName = "";
        this.projectCode = "";
        this.type = "INVITATION";
        this.senderId = 0;
        this.senderName = "";
        this.receiverId = 0;
        this.receiverName = "";
        this.status = "PENDING";
        this.createdAt = "";
        this.expiredAt = "";
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================
    public ProjectInvite(int id, int projectId, String projectName, String projectCode, String type, 
                         int senderId, String senderName, int receiverId, String receiverName, 
                         String status, String createdAt, String expiredAt) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.projectCode = projectCode;
        this.type = type;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.status = status;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
    }

    // ===================== CÁC HÀM TIỆN ÍCH THỜI GIAN & TRẠNG THÁI =====================

    /**
     * Kiểm tra xem Lời mời / Yêu cầu đã quá hạn 7 ngày chưa
     */
    public boolean isExpired() {
        if ("EXPIRED".equalsIgnoreCase(this.status)) {
            return true;
        }
        if (this.expiredAt == null || this.expiredAt.trim().isEmpty()) {
            return false;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime expireTime = LocalDateTime.parse(this.expiredAt.trim(), formatter);
            return LocalDateTime.now().isAfter(expireTime);
        } catch (Exception e) {
            return false;
        }
    }

    public String getStatusBadgeClass() {
        if ("ACCEPTED".equalsIgnoreCase(status)) return "bg-success text-white";
        if ("REJECTED".equalsIgnoreCase(status)) return "bg-danger text-white";
        if ("REVOKED".equalsIgnoreCase(status)) return "bg-secondary text-white";
        if ("EXPIRED".equalsIgnoreCase(status) || isExpired()) return "bg-dark text-white";
        return "bg-warning text-dark"; // PENDING
    }

    public String getStatusLabel() {
        if ("ACCEPTED".equalsIgnoreCase(status)) return "Đã đồng ý";
        if ("REJECTED".equalsIgnoreCase(status)) return "Đã từ chối";
        if ("REVOKED".equalsIgnoreCase(status)) return "Đã thu hồi";
        if ("EXPIRED".equalsIgnoreCase(status) || isExpired()) return "Đã hết hạn";
        return "Đang chờ duyệt"; // PENDING
    }

    // ===================== GETTERS & SETTERS =====================
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return projectId;
    }
    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public Project getProject() {
        return project;
    }
    public void setProject(Project project) {
        this.project = project;
    }

    public User getSender() {
        return sender;
    }
    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }
    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getProjectName() {
        return projectName;
    }
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectCode() {
        return projectCode;
    }
    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public int getSenderId() {
        return senderId;
    }
    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public int getReceiverId() {
        return receiverId;
    }
    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getExpiredAt() {
        return expiredAt;
    }
    public void setExpiredAt(String expiredAt) {
        this.expiredAt = expiredAt;
    }
}
