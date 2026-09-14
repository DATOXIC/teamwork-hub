package com.teamwork.business;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * JavaBean & JPA Entity: Đại diện cho Thành Viên Thuộc Dự Án (Project Member).
 * - Đóng vai trò là bản ghi trung gian kết nối Nhiều - Nhiều giữa Project và User.
 * - Quản lý vai trò (OWNER / MEMBER) và thời điểm gia nhập của từng người.
 */
@Entity
@Table(name = "project_members")
@IdClass(ProjectMemberId.class)
public class ProjectMember implements Serializable 
{
    @Id
    @Column(name = "project_id", nullable = false)
    private int projectId;            // ID của Dự án

    @Id
    @Column(name = "user_id", nullable = false)
    private int userId;               // ID của Thành viên

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Transient
    private String userName;          // Họ và tên thành viên

    @Transient
    private String userEmail;         // Email của thành viên

    @Transient
    private String userRole;          // Chuyên môn của thành viên (Developer, Designer, Tester...)

    @Column(name = "project_role", nullable = false)
    private String projectRole;       // Vai trò trong dự án: "OWNER" (Trưởng dự án) hoặc "MEMBER" (Thành viên)

    @Column(name = "joined_at", insertable = false, updatable = false)
    private String joinedAt;          // Ngày giờ chính thức gia nhập dự án

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================
    public ProjectMember() {
        this.projectId = 0;
        this.userId = 0;
        this.userName = "";
        this.userEmail = "";
        this.userRole = "";
        this.projectRole = "MEMBER";
        this.joinedAt = "";
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================
    public ProjectMember(int projectId, int userId, String userName, String userEmail, String userRole, String projectRole, String joinedAt) {
        this.projectId = projectId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.userRole = userRole;
        this.projectRole = projectRole;
        this.joinedAt = joinedAt;
    }

    // ===================== GETTERS & SETTERS =====================
    public int getProjectId() {
        return projectId;
    }
    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Project getProject() {
        return project;
    }
    public void setProject(Project project) {
        this.project = project;
    }

    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }

    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserRole() {
        return userRole;
    }
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getProjectRole() {
        return projectRole;
    }
    public void setProjectRole(String projectRole) {
        this.projectRole = projectRole;
    }

    public String getJoinedAt() {
        return joinedAt;
    }
    public void setJoinedAt(String joinedAt) {
        this.joinedAt = joinedAt;
    }
}
