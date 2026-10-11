package com.teamwork.business;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Thực thể lưu trữ cấu hình quy tắc tự động hóa quy trình theo từng dự án (Project Automation Recipe).
 */
@Entity
@Table(name = "project_automations")
public class ProjectAutomation implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String AUTO_PROMPT_GATE2 = "AUTO_PROMPT_GATE2";
    public static final String AUTO_CLOSE_SUBTASKS = "AUTO_CLOSE_SUBTASKS";
    public static final String AUTO_ESCALATE_PRIORITY = "AUTO_ESCALATE_PRIORITY";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "project_id", nullable = false)
    private int projectId;

    @Column(name = "recipe_id", nullable = false, length = 50)
    private String recipeId; // 'AUTO_PROMPT_GATE2', 'AUTO_CLOSE_SUBTASKS', 'AUTO_ESCALATE_PRIORITY'

    @Column(name = "recipe_name", nullable = false, length = 200)
    private String recipeName;

    @Column(name = "description")
    private String description = "";

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = true;

    @Column(name = "config_json")
    private String configJson = "{}";

    @Column(name = "last_triggered_at")
    private String lastTriggeredAt;

    @Column(name = "trigger_count", nullable = false)
    private int triggerCount = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private String createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private String updatedAt;

    public ProjectAutomation() {
    }

    public ProjectAutomation(int projectId, String recipeId, String recipeName, String description, boolean isEnabled) {
        this.projectId = projectId;
        this.recipeId = recipeId;
        this.recipeName = recipeName;
        this.description = description != null ? description : "";
        this.isEnabled = isEnabled;
    }

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

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = (description != null) ? description : "";
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean getEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public String getConfigJson() {
        return configJson != null ? configJson : "{}";
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    public void setLastTriggeredAt(String lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }

    public int getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(int triggerCount) {
        this.triggerCount = triggerCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
