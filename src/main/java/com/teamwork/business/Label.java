package com.teamwork.business;

import java.io.Serializable;
import java.util.Objects;

/**
 * JavaBean Model Ä‘áº¡i diá»‡n cho má»™t NhÃ£n phÃ¢n loáº¡i (Label / Tag) trong há»‡ thá»‘ng Kanban.
 * Má»—i Label thuá»™c vá» má»™t Project cá»¥ thá»ƒ vÃ  cÃ³ mÃ u sáº¯c, biá»ƒu tÆ°á»£ng (icon) tÃ¹y biáº¿n.
 * 
 * Ãp dá»¥ng nguyÃªn táº¯c Backend Code Mastery:
 * - Dá»¯ liá»‡u báº¥t biáº¿n Ä‘Æ°á»£c validate cháº·t cháº½ (name, colorKey, icon).
 * - Cung cáº¥p cÃ¡c helper methods an toÃ n cho táº§ng View (JSTL / JSP).
 */
public class Label implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===================== CÃC THUá»˜C TÃNH =====================

    private int id;             // KhÃ³a chÃ­nh Ä‘á»‹nh danh nhÃ£n
    private int projectId;      // Thuá»™c dá»± Ã¡n nÃ o (KhÃ³a ngoáº¡i trá» Ä‘áº¿n Project.id)
    private String name;        // TÃªn nhÃ£n hiá»ƒn thá»‹ (vÃ­ dá»¥: "Bug", "Hotfix", "UI/UX", "Security")
    private String colorKey;    // MÃ£ mÃ u: "red", "blue", "purple", "amber", "green", "pink", "cyan", "slate"
    private String icon;        // Biá»ƒu tÆ°á»£ng Bootstrap Icons (vÃ­ dá»¥: "bi-tag-fill", "bi-bug-fill")

    // ===================== CONSTRUCTOR Máº¶C Äá»ŠNH =====================

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

    // ===================== CÃC HELPER METHODS CHO VIEW (JSP/JSTL) =====================

    /**
     * Tráº£ vá» CSS class badge Bootstrap tÆ°Æ¡ng á»©ng vá»›i mÃ£ mÃ u Ä‘Ã£ chá»n
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
     * Tráº£ vá» mÃ£ mÃ u Hex Ä‘áº¡i diá»‡n Ä‘á»ƒ dÃ¹ng khi váº½ Color Picker hoáº·c cháº¥m mÃ u
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
     * TÃªn mÃ u sáº¯c hiá»ƒn thá»‹ báº±ng Tiáº¿ng Viá»‡t
     */
    public String getColorDisplayName() {
        if (colorKey == null) return "Xanh dÆ°Æ¡ng";
        switch (colorKey.toLowerCase()) {
            case "red":    return "Äá» Coral";
            case "blue":   return "Xanh DÆ°Æ¡ng";
            case "purple": return "TÃ­m Pastel";
            case "amber":  return "VÃ ng Há»• PhÃ¡ch";
            case "green":  return "Xanh LÃ¡";
            case "pink":   return "Há»“ng";
            case "cyan":   return "Xanh Ngá»c";
            case "slate":
            default:       return "XÃ¡m Slate";
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