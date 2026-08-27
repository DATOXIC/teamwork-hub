package com.teamwork.business;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JavaBean Model: Đại diện cho Lời Mời / Yêu Cầu Gia Nhập Dự Án 2 Chiều (Project Invite & Join Request).
 * - Chiều 1: type = "INVITATION" (PM gửi lời mời cho Thành viên)
 * - Chiều 2: type = "JOIN_REQUEST" (Thành viên nhập Mã Dự Án xin gia nhập)
 * - Tự động tính toán hạn hết hạn 7 ngày (expiredAt)
 */
public class ProjectInvite implements Serializable {

    private int id;
    private int projectId;            // ID của Dự án
    private String projectName;       // Tên Dự án
    private String projectCode;       // Mã Dự Án (VD: TW-HUB-01)
    private String type;              // "INVITATION" (PM mời) hoặc "JOIN_REQUEST" (Xin gia nhập)
    private int senderId;             // ID người gửi
    private String senderName;        // Tên người gửi
    private int receiverId;           // ID người nhận có thẩm quyền duyệt
    private String receiverName;      // Tên người nhận
    private String status;            // "PENDING", "ACCEPTED", "REJECTED", "REVOKED", "EXPIRED"
    private String createdAt;         // Thời điểm tạo (dd/MM/yyyy HH:mm)
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
