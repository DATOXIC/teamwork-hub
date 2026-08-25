package com.teamwork.business;

import java.io.Serializable;

/**
 * JavaBean Model đại diện cho một Bài viết Tài liệu / Ghi chú Wiki (Doc) trong Dự án.
 * Cho phép nhóm lưu trữ tài liệu kỹ thuật, biên bản cuộc họp và hướng dẫn dự án.
 */
public class Doc implements Serializable {

    // ===================== CÁC THUỘC TÍNH =====================

    private int id;             // Khóa chính định danh tài liệu
    private int projectId;      // Thuộc dự án nào (Khóa ngoại trỏ đến Project.id)
    private String title;       // Tiêu đề tài liệu (Ví dụ: "Hướng dẫn cài đặt môi trường")
    private String content;     // Nội dung chi tiết bài viết (Hỗ trợ nhiều dòng văn bản)
    private int authorId;       // ID người viết (trỏ đến User.id)
    private String authorName;  // Tên tác giả hiển thị (Ví dụ: "Trưởng Nhóm Admin")
    private String createdAt;   // Ngày giờ tạo bài (Định dạng: dd/MM/yyyy HH:mm)
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
        return this.authorId;
    }
    public void setAuthorId(int authorId) {
        this.authorId = authorId;
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
