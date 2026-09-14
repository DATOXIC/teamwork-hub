package com.teamwork.business;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Duration;
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
 * JavaBean & JPA Entity đại diện cho một bản ghi Nhật ký Hoạt động (Activity Log / Audit Trail).
 * Lưu vết tự động các sự kiện: tạo task, chuyển trạng thái, PM phê duyệt/yêu cầu sửa, thêm tài liệu...
 */
@Entity
@Table(name = "activity_logs")
public class ActivityLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "project_id", nullable = false)
    private int projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @Column(name = "user_id")
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Transient
    private String userName;

    @Transient
    private String userAvatar;

    @Column(name = "action_type", nullable = false)
    private String actionType;      // TASK_CREATE, STATUS_CHANGE, TASK_SUBMIT, PM_APPROVE, PM_REVISE, PM_REJECT, DOC_CREATE, SUBTASK_CREATE

    @Column(name = "target_type", nullable = false)
    private String targetType;      // TASK, DOC, MEMBER, PROJECT, SUBTASK

    @Column(name = "target_id")
    private int targetId;

    @Column(name = "target_title")
    private String targetTitle;

    @Column(name = "description")
    private String description;

    @Transient
    private String createdAt;       // Chuỗi định dạng ngày giờ dd/MM/yyyy HH:mm

    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp rawCreatedAt; // Timestamp gốc từ database để tính toán thời gian tương đối

    public ActivityLog() {
        this.id = 0;
        this.projectId = 0;
        this.userId = 0;
        this.userName = "H\u1EC7 th\u1ED1ng";
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
        this.userName = (userName != null && !userName.trim().isEmpty()) ? userName.trim() : "Th\u00E0nh vi\u00EAn";
        this.userAvatar = userAvatar != null ? userAvatar : "";
        this.actionType = actionType != null ? actionType : "";
        this.targetType = targetType != null ? targetType : "TASK";
        this.targetId = targetId;
        this.targetTitle = targetTitle != null ? targetTitle : "";
        this.description = description != null ? description : "";
        this.createdAt = createdAt != null ? createdAt : "";
        this.rawCreatedAt = rawCreatedAt;
    }

    // ===================== CAC HAM TIEN ICH UI =====================

    /**
     * Tinh thoi gian tuong doi than thien (Relative Time) phong cach ClickUp:
     * "Vua xong", "5 phut truoc", "2 gio truoc", "Hom qua luc HH:mm", "dd/MM/yyyy HH:mm".
     */
    public String getTimeAgo() {
        if (rawCreatedAt == null) {
            return (createdAt != null && !createdAt.trim().isEmpty()) ? createdAt : "V\u1EEBa xong";
        }
        try {
            LocalDateTime createdTime = rawCreatedAt.toLocalDateTime();
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(createdTime, now);

            long seconds = duration.getSeconds();
            if (seconds < 60) {
                return "V\u1EEBa xong";
            }
            long minutes = duration.toMinutes();
            if (minutes < 60) {
                return minutes + " ph\u00FAt tr\u01B0\u1EDBc";
            }
            long hours = duration.toHours();
            if (hours < 24) {
                return hours + " gi\u1EDD tr\u01B0\u1EDBc";
            }
            long days = duration.toDays();
            if (days == 1) {
                DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
                return "H\u00F4m qua l\u00FAc " + createdTime.format(timeFmt);
            }
            if (days < 7) {
                return days + " ng\u00E0y tr\u01B0\u1EDBc";
            }
            DateTimeFormatter fullFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return createdTime.format(fullFmt);
        } catch (Exception e) {
            return (createdAt != null) ? createdAt : "V\u1EEBa xong";
        }
    }

    /**
     * Tra ve class Bootstrap badge phan loai theo hanh dong
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
            case "SUBTASK_CREATE":
                return "bg-info-subtle text-info border border-info-subtle";
            default:
                return "bg-secondary-subtle text-secondary border border-secondary-subtle";
        }
    }

    /**
     * Tra ve Bootstrap Icon tuong ung voi loai su kien
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
            case "SUBTASK_CREATE":
                return "bi-list-check text-info";
            default:
                return "bi-clock-history text-secondary";
        }
    }

    /**
     * Ten nhan tieng Viet cua hanh dong
     */
    public String getActionLabel() {
        if (actionType == null) return "Ho\u1EA1t \u0111\u1ED9ng";
        switch (actionType.toUpperCase()) {
            case "TASK_CREATE":
                return "T\u1EA1o c\u00F4ng vi\u1EC7c";
            case "STATUS_CHANGE":
                return "\u0110\u1ED5i tr\u1EA1ng th\u00E1i";
            case "TASK_SUBMIT":
                return "N\u1ED9p nghi\u1EC7m thu";
            case "PM_APPROVE":
                return "Duy\u1EC7t nghi\u1EC7m thu";
            case "PM_REVISE":
                return "Y\u00EAu c\u1EA7u s\u1EEDa \u0111\u1ED5i";
            case "PM_REJECT":
                return "T\u1EEB ch\u1ED1i nghi\u1EC7m thu";
            case "DOC_CREATE":
                return "T\u1EA3i t\u00E0i li\u1EC7u m\u1EDBi";
            case "SUBTASK_CREATE":
                return "Th\u00EAm vi\u1EC7c con";
            default:
                return "C\u1EADp nh\u1EADt";
        }
    }

    // ===================== GETTERS VA SETTERS =====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public int getUserId() { return userId != null ? userId : 0; }
    public void setUserId(int userId) { this.userId = userId > 0 ? userId : null; }
    public void setUserId(Integer userId) { this.userId = (userId != null && userId > 0) ? userId : null; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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