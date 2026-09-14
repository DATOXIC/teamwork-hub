package com.teamwork.business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JavaBean & JPA Entity: Đại diện cho Người Dùng (User) trong hệ thống.
 * - Quản lý thông tin xác thực (Username, Password)
 * - Quản lý thông tin Hồ Sơ Cá Nhân (Bio, Skills, GitHub/LinkedIn URLs)
 */
@Entity
@Table(name = "users")
public class User implements Serializable {

    // ===================== CÁC THUỘC TÍNH =====================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "role")
    private String role;              // Chuyên môn / Chức danh (Project Manager, Developer, Designer, Tester...)

    @Column(name = "avatar")
    private String avatar;            // Đường dẫn ảnh đại diện

    @Column(name = "bio")
    private String bio;               // Lời giới thiệu ngắn bản thân (Tối đa 250 ký tự)

    @Column(name = "skills")
    private String skills;            // Danh sách Kỹ năng cách nhau bằng dấu phẩy (Java, MySQL, Docker...)

    @Column(name = "github_url")
    private String githubUrl;         // Link trang GitHub cá nhân

    @Column(name = "linkedin_url")
    private String linkedinUrl;       // Link trang LinkedIn cá nhân

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================
    public User() {
        this.id = 0;
        this.username = "";
        this.password = "";
        this.fullName = "";
        this.email = "";
        this.role = "Developer";
        this.avatar = "images/default_avatar.png";
        this.bio = "";
        this.skills = "";
        this.githubUrl = "";
        this.linkedinUrl = "";
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================
    public User(int id, String username, String password, String fullName, String email, String role, String avatar, 
                String bio, String skills, String githubUrl, String linkedinUrl) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.avatar = (avatar != null && !avatar.trim().isEmpty()) ? avatar : "images/default_avatar.png";
        this.bio = (bio != null) ? bio.trim() : "";
        this.skills = (skills != null) ? skills.trim() : "";
        this.githubUrl = (githubUrl != null) ? githubUrl.trim() : "";
        this.linkedinUrl = (linkedinUrl != null) ? linkedinUrl.trim() : "";
    }

    // Constructor tương thích ngược
    public User(int id, String username, String password, String fullName, String email, String role, String avatar) {
        this(id, username, password, fullName, email, role, avatar, "", "", "", "");
    }

    // ===================== HÀM TIỆN ÍCH CHO GIAO DIỆN =====================
    /**
     * Tách chuỗi skills ("Java, MySQL, Docker") thành Danh sách List<String> để JSP hiển thị từng thẻ pill
     */
    public List<String> getSkillList() {
        List<String> list = new ArrayList<>();
        if (this.skills != null && !this.skills.trim().isEmpty()) {
            String[] parts = this.skills.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }
        return list;
    }

    // ===================== GETTERS & SETTERS =====================
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    public String getAvatar() {
        return avatar;
    }
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getBio() {
        return bio;
    }
    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getSkills() {
        return skills;
    }
    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getGithubUrl() {
        return githubUrl;
    }
    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }
    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }
}
