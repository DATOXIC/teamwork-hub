package com.teamwork.business;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA Entity đại diện cho Bảng vẽ (Whiteboard) của một Dự án.
 * Mỗi dự án có một bảng vẽ; nội dung lưu dạng JSON do Excalidraw sinh ra.
 */
@Entity
@Table(name = "whiteboards")
public class Whiteboard implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "project_id", nullable = false)
    private int projectId;

    @Column(name = "content", length = 10000000)
    private String content;         // JSON {elements, appState, files}

    @Column(name = "updated_by")
    private Integer updatedBy;      // ID người lưu gần nhất

    public Whiteboard() {
        this.content = "";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Integer updatedBy) { this.updatedBy = updatedBy; }
}
