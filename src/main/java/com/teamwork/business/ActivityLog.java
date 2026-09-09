package com.teamwork.business;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JavaBean Model Ä‘áº¡i diá»‡n cho má»™t báº£n ghi Nháº­t kÃ½ Hoáº¡t Ä‘á»™ng (Activity Log / Audit Trail).
 * LÆ°u váº¿t tá»± Ä‘á»™ng cÃ¡c sá»± kiá»‡n: táº¡o task, chuyá»ƒn tráº¡ng thÃ¡i, PM phÃª duyá»‡t/yÃªu cáº§u sá»­a, thÃªm tÃ i liá»‡u...
 */
public class ActivityLog implements Serializable {

    private int id;
    private int projectId;
    private int userId;
    private String userName;
    private String userAvatar;
    private String actionType;      // TASK_CREATE, STATUS_CHANGE, TASK_SUBMIT, PM_APPROVE, PM_REVISE, PM_REJECT, DOC_CREATE
    private String targetType;      // TASK, DOC, MEMBER, PROJECT
    private int targetId;
    private String targetTitle;
    private String description;
    private String createdAt;       // Chuá»—i Ä‘á»‹nh dáº¡ng ngÃ y giá» dd/MM/yyyy HH:mm
    private Timestamp rawCreatedAt; // Timestamp gá»‘c tá»« database Ä‘á»ƒ tÃ­nh toÃ¡n thá»i gian tÆ°Æ¡ng Ä‘á»‘i

    public ActivityLog() {
        this.id = 0;
        this.projectId = 0;
        this.userId = 0;
        this.userName = "Há»‡ thá»‘ng";
        this.userAvatar = "";
        this.actionType = "";
        this.targetType = "TASK";
        this.targetId = 0;
        this.targetTitle = "";
        this.description = "";
        this.createdAt = "";
    }

    public ActivityLog(int id, int projectId, int userId, String userName, String userAvatar,
                       String actionType, String targetType, int targetId, String targetTitle,
                       String description, String createdAt, Timestamp rawCreatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.userName = userName != null && !userName.trim().isEmpty() ? userName : "ThÃ nh viÃªn";
        this.userAvatar = userAvatar != null ? userAvatar : "";
        this.actionType = actionType != null ? actionType : "";
        this.targetType = targetType != null ? targetType : "TASK";
        this.targetId = targetId;
        this.targetTitle = targetTitle != null ? targetTitle : "";
        this.description = description != null ? description : "";
        this.createdAt = createdAt != null ? createdAt : "";
        this.rawCreatedAt = rawCreatedAt;
    }

    // ===================== CÃC HÃ€M TIá»†N ÃCH UI =====================

    /**
     * TÃ­nh thá»i gian tÆ°Æ¡ng Ä‘á»‘i thÃ¢n thiá»‡n (Relative Time) phong cÃ¡ch ClickUp:
     * "Vá»«a xong", "5 phÃºt trÆ°á»›c", "2 giá» trÆ°á»›c", "HÃ´m qua lÃºc HH:mm", "dd/MM/yyyy HH:mm".
     */
    public String getTimeAgo() {
        if (rawCreatedAt == null) {
            return createdAt != null ? createdAt : "Vá»«a xong";
        }
        try {
            LocalDateTime createdTime = rawCreatedAt.toLocalDateTime();
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(createdTime, now);

            long seconds = duration.getSeconds();
            if (seconds < 60) {
                return "Vá»«a xong";
            }
            long minutes = duration.toMinutes();
            if (minutes < 60) {
                return minutes + " phÃºt trÆ°á»›c";
            }
            long hours = duration.toHours();
            if (hours < 24) {
                return hours + " giá» trÆ°á»›c";
            }
            long days = duration.toDays();
            if (days == 1) {
                DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
                return "HÃ´m qua lÃºc " + createdTime.format(timeFmt);
            }
            if (days < 7) {
                return days + " ngÃ y trÆ°á»›c";
            }
            DateTimeFormatter fullFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return createdTime.format(fullFmt);
        } catch (Exception e) {
            return createdAt;
        }
    }

    /**
     * Tráº£ vá» class Bootstrap badge phÃ¢n loáº¡i theo hÃ nh Ä‘á»™ng
     */
    public String getBadgeClass() {
        if (actionType == null) return "bg-secondary text-white";
        switch (actionType.toUpperCase()) {
            case "TASK_CREATE":
                return "bg-primary-subtle text-primary border border-primary-subtle";
            case "STATUS_CHANGE":
                return "bg-info-subtle text-info border border-info-subtle";
            case "TASK_SUBMIT":
                return "bg-warning-subtle text-warning border border-warning-subtle";
            case "PM_APPROVE":
                return "bg-success-subtle text-success border border-success-subtle";
            case "PM_REVISE":
                return "bg-warning-subtle text-warning border border-warning-subtle";
            case "PM_REJECT":
                return "bg-danger-subtle text-danger border border-danger-subtle";
            case "DOC_CREATE":
                return "bg-purple-subtle text-primary border border-primary-subtle";
            default:
                return "bg-secondary-subtle text-secondary border border-secondary-subtle";
        }
    }

    /**
     * Tráº£ vá» Bootstrap Icon tÆ°Æ¡ng á»©ng vá»›i loáº¡i sá»± kiá»‡n
     */
    public String getIconClass() {
        if (actionType == null) return "bi-activity text-secondary";
        switch (actionType.toUpperCase()) {
            case "TASK_CREATE":
                return "bi-plus-circle-fill text-primary";
            case "STATUS_CHANGE":
                return "bi-arrow-left-right text-info";
            case "TASK_SUBMIT":
                return "bi-send-fill text-warning";
            case "PM_APPROVE":
                return "bi-check-circle-fill text-success";
            case "PM_REVISE":
                return "bi-arrow-clockwise text-warning";
            case "PM_REJECT":
                return "bi-x-circle-fill text-danger";
            case "DOC_CREATE":
                return "bi-file-earmark-text-fill text-primary";
            default:
                return "bi-clock-history text-secondary";
        }
    }

    /**
     * TÃªn nhÃ£n tiáº¿ng Viá»‡t cá»§a hÃ nh Ä‘á»™ng
     */
    public String getActionLabel() {
        if (actionType == null) return "Hoáº¡t Ä‘á»™ng";
        switch (actionType.toUpperCase()) {
            case "TASK_CREATE":
                return "Táº¡o cÃ´ng viá»‡c";
            case "STATUS_CHANGE":
                return "Äá»•i tráº¡ng thÃ¡i";
            case "TASK_SUBMIT":
                return "Ná»™p nghiá»‡m thu";
            case "PM_APPROVE":
                return "Duyá»‡t nghiá»‡m thu";
            case "PM_REVISE":
                return "YÃªu cáº§u sá»­a Ä‘á»•i";
            case "PM_REJECT":
                return "Tá»« chá»‘i nghiá»‡m thu";
            case "DOC_CREATE":
                return "Táº£i tÃ i liá»‡u má»›i";
            default:
                return "Cáº­p nháº­t";
        }
    }

    // ===================== GETTERS VÃ€ SETTERS =====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public int getTargetId() { return targetId; }
    public void setTargetId(int targetId) { this.targetId = targetId; }

    public String getTargetTitle() { return targetTitle; }
    public void setTargetTitle(String targetTitle) { this.targetTitle = targetTitle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Timestamp getRawCreatedAt() { return rawCreatedAt; }
    public void setRawCreatedAt(Timestamp rawCreatedAt) { this.rawCreatedAt = rawCreatedAt; }
}