package com.teamwork.business;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JavaBean & JPA Entity đại diện cho một Nhãn phân loại (Label / Tag) trong hệ thống Kanban.
 * Mỗi Label thuộc về một Project cụ thể và có màu sắc, biểu tượng (icon) tùy biến.
 */
@Entity
@Table(name = "labels")
public class Label implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===================== CÁC THUỘC TÍNH =====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;             // Khóa chính định danh nhãn

    @Column(name = "project_id", nullable = false)
    private int projectId;      // Thuộc dự án nào (Khóa ngoại trỏ đến Project.id)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @Column(name = "name", nullable = false)
    private String name;        // Tên nhãn hiển thị (ví dụ: "Bug", "Hotfix", "UI/UX", "Security")

    @Column(name = "color_key")
    private String colorKey;    // Mã màu: "red", "blue", "purple", "amber", "green", "pink", "cyan", "slate"

    @Column(name = "icon")
    private String icon;        // Biểu tượng Bootstrap Icons (ví dụ: "bi-tag-fill", "bi-bug-fill")

    // ===================== CONSTRUCTOR Máº¶C Ä á»ŠNH =====================

    public Label() {
        this.id = 0;
        this.projectId = 0;
        this.name = "";
        this.colorKey = "blue";
        this.icon = "bi-tag-fill";
    }

    // ===================== CONSTRUCTOR Äáº¦Y Äá»¦ THAM Sá» =====================

    public Label(int id, int projectId, String name, String colorKey, String icon) {
        this.id = id;
        this.projectId = projectId;
        this.name = (name != null) ? name.trim() : "";
        this.colorKey = (colorKey != null && !colorKey.trim().isEmpty()) ? colorKey.trim().toLowerCase() : "blue";
        this.icon = (icon != null && !icon.trim().isEmpty()) ? icon.trim() : "bi-tag-fill";
    }

    // ===================== GETTER VÃ€ SETTER =====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = (name != null) ? name.trim() : "";
    }

    public String getColorKey() {
        return colorKey;
    }

    public void setColorKey(String colorKey) {
        this.colorKey = (colorKey != null && !colorKey.trim().isEmpty()) ? colorKey.trim().toLowerCase() : "blue";
    }

    public String getIcon() {
        return (icon != null && !icon.trim().isEmpty()) ? icon : "bi-tag-fill";
    }

    public void setIcon(String icon) {
        this.icon = (icon != null && !icon.trim().isEmpty()) ? icon.trim() : "bi-tag-fill";
    }

    // ===================== CAC HELPER METHODS CHO VIEW (JSP/JSTL) =====================

    /**
     * Tra ve CSS class badge Bootstrap tuong ung voi ma mau da chon
     */
    public String getBadgeClass() {
        if (colorKey == null) return "bg-primary-subtle text-primary border border-primary-subtle";
        switch (colorKey.toLowerCase()) {
            case "red":
                return "bg-danger-subtle text-danger border border-danger-subtle";
            case "blue":
                return "bg-primary-subtle text-primary border border-primary-subtle";
            case "purple":
                return "bg-purple-subtle text-purple border border-purple-subtle";
            case "amber":
                return "bg-warning-subtle text-warning-emphasis border border-warning-subtle";
            case "green":
                return "bg-success-subtle text-success border border-success-subtle";
            case "pink":
                return "bg-danger-subtle text-danger-emphasis border border-danger-subtle";
            case "cyan":
                return "bg-info-subtle text-info-emphasis border border-info-subtle";
            case "slate":
            default:
                return "bg-secondary-subtle text-secondary border border-secondary-subtle";
        }
    }

    /**
     * Tra ve ma mau Hex dai dien de dung khi ve Color Picker hoac cham mau
     */
    public String getColorHex() {
        if (colorKey == null) return "#3b82f6";
        switch (colorKey.toLowerCase()) {
            case "red":    return "#ef4444";
            case "blue":   return "#3b82f6";
            case "purple": return "#8b5cf6";
            case "amber":  return "#f59e0b";
            case "green":  return "#10b981";
            case "pink":   return "#ec4899";
            case "cyan":   return "#06b6d4";
            case "slate":
            default:       return "#64748b";
        }
    }

    /**
     * Tên màu sắc hiển thị bằng Tiếng Việt
     */
    public String getColorDisplayName() {
        if (colorKey == null) return "Xanh d\u01B0\u01A1ng";
        switch (colorKey.toLowerCase()) {
            case "red":    return "\u0110\u1ECF Coral";
            case "blue":   return "Xanh D\u01B0\u01A1ng";
            case "purple": return "T\u00EDm Pastel";
            case "amber":  return "V\u00E0ng H\u1ED5 Ph\u00E1ch";
            case "green":  return "Xanh L\u00E1";
            case "pink":   return "H\u1ED3ng";
            case "cyan":   return "Xanh Ng\u1ECDc";
            case "slate":
            default:       return "X\u00E1m Slate";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Label)) return false;
        Label label = (Label) o;
        return id == label.id && projectId == label.projectId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, projectId);
    }

    @Override
    public String toString() {
        return "Label{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", name='" + name + '\'' +
                ", colorKey='" + colorKey + '\'' +
                ", icon='" + icon + '\'' +
                '}';
    }
}