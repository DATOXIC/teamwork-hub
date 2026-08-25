package com.teamwork.business;

import java.io.Serializable;

/**
 * JavaBean Model đại diện cho mối liên kết Nhiều - Nhiều giữa Công việc (Task) và Tài liệu hướng dẫn (Doc).
 * Một Task có thể đính kèm nhiều tài liệu hướng dẫn kỹ thuật.
 * Một Doc có thể được nhiều Task tham chiếu đến.
 */
public class TaskDoc implements Serializable {

    // ===================== CÁC THUỘC TÍNH =====================

    private int taskId;       // ID công việc (Khóa ngoại trỏ đến Task.id)
    private int docId;        // ID tài liệu hướng dẫn (Khóa ngoại trỏ đến Doc.id)
    private String docTitle;  // Tiêu đề của tài liệu (Lưu sẵn để JSP hiển thị nhanh không cần truy vấn lại)

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================

    /**
     * Constructor không tham số: Bắt buộc theo chuẩn JavaBean.
     */
    public TaskDoc() {
        this.taskId = 0;
        this.docId = 0;
        this.docTitle = "";
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================

    /**
     * Constructor đầy đủ tham số: Dùng khi tạo liên kết mới giữa Task và Doc.
     */
    public TaskDoc(int taskId, int docId, String docTitle) {
        this.taskId = taskId;
        this.docId = docId;
        this.docTitle = docTitle;
    }

    // ===================== GETTERS & SETTERS =====================

    public int getTaskId() {
        return this.taskId;
    }
    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getDocId() {
        return this.docId;
    }
    public void setDocId(int docId) {
        this.docId = docId;
    }

    public String getDocTitle() {
        return this.docTitle;
    }
    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }
}
