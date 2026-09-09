package com.teamwork.business;

import java.io.Serializable;

/**
 * JavaBean Model: Đại diện cho một Dự Án (Project) trong hệ thống.
 * - Chứa Mã Dự Án (projectCode) phục vụ luồng Thành viên Xin Gia Nhập (Chiều 2)
 * - Chứa các thông tin thống kê tổng quan tiến độ % công việc
 */
public class Project implements Serializable {

    private int id;
    private String projectCode;       // Mã định danh ngắn gọn duy nhất (VD: TW-HUB-01, ECOMMERCE-99)
    private String name;              // Tên dự án
    private String description;       // Mô tả mục tiêu dự án
    private String projectType;       // 'SOLO' (Cá nhân / Fast-track) hoặc 'TEAM' (Nhóm / Quality Gate)
    private int ownerId;              // ID của Trưởng Dự Án (Project Manager / Owner)
    private String createdAt;         // Ngày tạo
    private int totalTasks;           // Tổng số Task lớn
    private int doneTasks;            // Số Task lớn đã hoàn thành (DONE)

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================
    public Project() {
        this.id = 0;
        this.projectCode = "";
        this.name = "";
        this.description = "";
        this.projectType = "TEAM";
        this.ownerId = 0;
        this.createdAt = "";
        this.totalTasks = 0;
        this.doneTasks = 0;
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================
    public Project(int id, String projectCode, String name, String description, String projectType, int ownerId, String createdAt, int totalTasks, int doneTasks) {
        this.id = id;
        this.projectCode = (projectCode != null && !projectCode.trim().isEmpty()) ? projectCode.trim().toUpperCase() : "";
        this.name = name;
        this.description = description;
        this.projectType = (projectType != null && !projectType.trim().isEmpty()) ? projectType.trim().toUpperCase() : "TEAM";
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.totalTasks = totalTasks;
        this.doneTasks = doneTasks;
    }

    // Constructor tương thích ngược có projectCode
    public Project(int id, String projectCode, String name, String description, int ownerId, String createdAt, int totalTasks, int doneTasks) {
        this(id, projectCode, name, description, "TEAM", ownerId, createdAt, totalTasks, doneTasks);
    }

    // Constructor tương thích ngược (Tự động sinh mã nếu không truyền)
    public Project(int id, String name, String description, int ownerId, String createdAt, int totalTasks, int doneTasks) {
        this(id, "PRJ-" + id, name, description, "TEAM", ownerId, createdAt, totalTasks, doneTasks);
    }

    // ===================== HÀM TÍNH TOÁN TIỆN ÍCH =====================
    /**
     * Tính toán phần trăm tiến độ tổng thể của Dự Án
     */
    public int getProgressPercentage() {
        if (totalTasks == 0) {
            return 0;
        }
        return (int) Math.round(((double) doneTasks / totalTasks) * 100);
    }

    // ===================== GETTERS & SETTERS =====================
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getProjectCode() {
        return projectCode;
    }
    public void setProjectCode(String projectCode) {
        this.projectCode = (projectCode != null) ? projectCode.trim().toUpperCase() : "";
    }

    public String getCode() {
        return getProjectCode();
    }
    public void setCode(String code) {
        setProjectCode(code);
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public int getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getTotalTasks() {
        return totalTasks;
    }
    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getDoneTasks() {
        return doneTasks;
    }
    public void setDoneTasks(int doneTasks) {
        this.doneTasks = doneTasks;
    }

    public String getProjectType() {
        return projectType != null ? projectType : "TEAM";
    }
    public void setProjectType(String projectType) {
        this.projectType = (projectType != null && !projectType.trim().isEmpty()) ? projectType.trim().toUpperCase() : "TEAM";
    }

    public boolean isSoloProject() {
        return "SOLO".equalsIgnoreCase(this.projectType);
    }

    public boolean isTeamProject() {
        return !"SOLO".equalsIgnoreCase(this.projectType);
    }

    public String getProjectTypeLabel() {
        return isSoloProject() ? "Cá nhân (Linh hoạt)" : "Nhóm (Quality Gate)";
    }

    public String getProjectTypeBadgeClass() {
        return isSoloProject() ? "bg-info-subtle text-info border border-info-subtle" : "bg-primary-subtle text-primary border border-primary-subtle";
    }

    public String getProjectTypeIcon() {
        return isSoloProject() ? "bi-person-fill" : "bi-people-fill";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return id == project.id;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}

