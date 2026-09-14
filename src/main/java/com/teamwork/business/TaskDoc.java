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
 * JavaBean & JPA Entity đại diện cho mối liên kết Nhiều - Nhiều giữa Công việc (Task) và Tài liệu hướng dẫn (Doc).
 * Một Task có thể đính kèm nhiều tài liệu hướng dẫn kỹ thuật.
 * Một Doc có thể được nhiều Task tham chiếu đến.
 */
@Entity
@Table(name = "task_docs")
@IdClass(TaskDocId.class)
public class TaskDoc implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===================== CÁC THUỘC TÍNH =====================

    @Id
    @Column(name = "task_id", nullable = false)
    private int taskId;       // ID công việc (Khóa ngoại trỏ đến Task.id)

    @Id
    @Column(name = "doc_id", nullable = false)
    private int docId;        // ID tài liệu hướng dẫn (Khóa ngoại trỏ đến Doc.id)

    @Transient
    private String docTitle;  // Tiêu đề của tài liệu (Lưu sẵn để JSP hiển thị nhanh không cần truy vấn lại)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", insertable = false, updatable = false)
    private Doc doc;

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================

    /**
     * Constructor không tham số: Bắt buộc theo chuẩn JavaBean và JPA.
     */
    public TaskDoc() {
        this.taskId = 0;
        this.docId = 0;
        this.docTitle = "";
    }

    // ===================== CONSTRUCTOR TIỆN ÍCH =====================

    public TaskDoc(int taskId, int docId) {
        this.taskId = taskId;
        this.docId = docId;
        this.docTitle = "";
    }

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
        if ((this.docTitle == null || this.docTitle.isEmpty()) && this.doc != null) {
            return this.doc.getTitle();
        }
        return this.docTitle;
    }
    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }

    public Task getTask() {
        return task;
    }
    public void setTask(Task task) {
        this.task = task;
    }

    public Doc getDoc() {
        return doc;
    }
    public void setDoc(Doc doc) {
        this.doc = doc;
    }
}
