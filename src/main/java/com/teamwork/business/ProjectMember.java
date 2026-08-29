package com.teamwork.business;

import java.io.Serializable;

/**
 * JavaBean Model: Đại diện cho Thành Viên Thuộc Dự Án (Project Member).
 * - Đóng vai trò là bản ghi trung gian kết nối Nhiều - Nhiều giữa Project và User.
 * - Quản lý vai trò (OWNER / MEMBER) và thời điểm gia nhập của từng người.
 * - Class gốc chứa một List Class này
 */
public class ProjectMember implements Serializable 
{
    // 2 tham chiếu tạo thành mối quan hệ
    private int projectId;            // ID của Dự án
    private int userId;               // ID của Thành viên


    private String userName;          // Họ và tên thành viên
    private String userEmail;         // Email của thành viên
    private String userRole;          // Chuyên môn của thành viên (Developer, Designer, Tester...)
    private String projectRole;       // Vai trò trong dự án: "OWNER" (Trưởng dự án) hoặc "MEMBER" (Thành viên)
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
