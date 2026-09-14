package com.teamwork.business;

import java.io.Serializable;
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
 * JavaBean & JPA Entity đại diện cho một Bài viết Tài liệu / Ghi chú Wiki (Doc) trong Dự án.
 * Cho phép nhóm lưu trữ tài liệu kỹ thuật, biên bản cuộc họp và hướng dẫn dự án.
 */
@Entity
@Table(name = "docs")
public class Doc implements Serializable {

    // ===================== CÁC THUỘC TÍNH =====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;             // Khóa chính định danh tài liệu

    @Column(name = "project_id", nullable = false)
    private int projectId;      // Thuộc dự án nào (Khóa ngoại trỏ đến Project.id)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @Column(name = "title", nullable = false)
    private String title;       // Tiêu đề tài liệu (Ví dụ: "Hướng dẫn cài đặt môi trường")

    @Column(name = "content")
    private String content;     // Nội dung chi tiết bài viết (Hỗ trợ nhiều dòng văn bản)

    @Column(name = "author_id")
    private Integer authorId;   // ID người viết (trỏ đến User.id, null-safe)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", insertable = false, updatable = false)
    private User author;

    @Transient
    private String authorName;  // Tên tác giả hiển thị (Ví dụ: "Trưởng Nhóm Admin")

    @Column(name = "created_at", insertable = false, updatable = false)
    private String createdAt;   // Ngày giờ tạo bài (Định dạng: dd/MM/yyyy HH:mm)

    @Column(name = "updated_at", insertable = false, updatable = false)
    private String updatedAt;   // Ngày giờ chỉnh sửa lần cuối (Định dạng: dd/MM/yyyy HH:mm)

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================

    /**
     * Constructor không tham số: Bắt buộc theo chuẩn JavaBean.
     */
    public Doc() {
        this.id = 0;
        this.projectId = 0;
        this.title = "";
        this.content = "";
        this.authorId = 0;
        this.authorName = "Ẩn danh";
        this.createdAt = "";
        this.updatedAt = "";
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================

    /**
     * Constructor đầy đủ tham số: Dùng trong DocDB để tạo nhanh bài viết mẫu (Seed Data).
     */
    public Doc(int id, int projectId, String title, String content,
               int authorId, String authorName, String createdAt, String updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ===================== HÀM TIỆN ÍCH PHỤC VỤ GIAO DIỆN =====================

    /**
     * Trích đoạn ngắn nội dung (Snippet) để hiển thị xem trước ở danh mục bên trái.
     * Nếu nội dung dài hơn 80 ký tự thì cắt bớt và gắn thêm dấu "...".
     */
    public String getSnippet() 
    {
        if (this.content == null || this.content.trim().isEmpty()) {
            return "Chưa có nội dung...";
        }
        String cleanContent = this.content.trim().replace("\n", " ");
        if (cleanContent.length() <= 80) {
            return cleanContent;
        } else {
            return cleanContent.substring(0, 80) + "...";
        }
    }

    // ===================== GETTERS & SETTERS =====================

    public int getId() {
        return this.id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return this.projectId;
    }
    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return this.title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return this.content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public int getAuthorId() {
        return this.authorId != null ? this.authorId : 0;
    }
    public void setAuthorId(int authorId) {
        this.authorId = authorId > 0 ? authorId : null;
    }
    public void setAuthorId(Integer authorId) {
        this.authorId = (authorId != null && authorId > 0) ? authorId : null;
    }

    public Project getProject() {
        return this.project;
    }
    public void setProject(Project project) {
        this.project = project;
    }

    public User getAuthor() {
        return this.author;
    }
    public void setAuthor(User author) {
        this.author = author;
    }

    public String getAuthorName() {
        return this.authorName;
    }
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return this.updatedAt;
    }
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
