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
 * Thực thể biểu diễn mối quan hệ phụ thuộc giữa 2 công việc (Task Dependency).
 * Ví dụ: taskId (công việc sau) phụ thuộc vào dependsOnTaskId (công việc trước/tiên quyết).
 * Chuẩn quan hệ mặc định: FINISH_TO_START (Task trước hoàn thành thì Task sau mới được bắt đầu).
 */
@Entity
@Table(name = "task_dependencies")
public class TaskDependency implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "project_id", nullable = false)
    private int projectId;

    @Column(name = "task_id", nullable = false)
    private int taskId; // Dependent Task (Successor / Công việc chịu phụ thuộc)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private Task task;

    @Column(name = "depends_on_task_id", nullable = false)
    private int dependsOnTaskId; // Prerequisite Task (Predecessor / Công việc tiên quyết)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depends_on_task_id", insertable = false, updatable = false)
    private Task dependsOnTask;

    @Column(name = "dependency_type", nullable = false, length = 30)
    private String dependencyType = "FINISH_TO_START";

    @Column(name = "lag_days", nullable = false)
    private int lagDays = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private String createdAt;

    @Transient
    private String taskTitle;

    @Transient
    private String dependsOnTaskTitle;

    @Transient
    private String dependsOnTaskStatus;

    public TaskDependency() {
    }

    public TaskDependency(int projectId, int taskId, int dependsOnTaskId) {
        this.projectId = projectId;
        this.taskId = taskId;
        this.dependsOnTaskId = dependsOnTaskId;
        this.dependencyType = "FINISH_TO_START";
        this.lagDays = 0;
    }

    public TaskDependency(int projectId, int taskId, int dependsOnTaskId, String dependencyType, int lagDays) {
        this.projectId = projectId;
        this.taskId = taskId;
        this.dependsOnTaskId = dependsOnTaskId;
        this.dependencyType = dependencyType != null ? dependencyType : "FINISH_TO_START";
        this.lagDays = lagDays;
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

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
        if (task != null) {
            this.taskTitle = task.getTitle();
        }
    }

    public int getDependsOnTaskId() {
        return dependsOnTaskId;
    }

    public void setDependsOnTaskId(int dependsOnTaskId) {
        this.dependsOnTaskId = dependsOnTaskId;
    }

    public Task getDependsOnTask() {
        return dependsOnTask;
    }

    public void setDependsOnTask(Task dependsOnTask) {
        this.dependsOnTask = dependsOnTask;
        if (dependsOnTask != null) {
            this.dependsOnTaskTitle = dependsOnTask.getTitle();
            this.dependsOnTaskStatus = dependsOnTask.getStatus();
        }
    }

    public String getDependencyType() {
        return dependencyType != null ? dependencyType : "FINISH_TO_START";
    }

    public void setDependencyType(String dependencyType) {
        this.dependencyType = (dependencyType != null && !dependencyType.trim().isEmpty())
                ? dependencyType.trim().toUpperCase() : "FINISH_TO_START";
    }

    public int getLagDays() {
        return lagDays;
    }

    public void setLagDays(int lagDays) {
        this.lagDays = lagDays;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getTaskTitle() {
        return taskTitle != null ? taskTitle : "";
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getDependsOnTaskTitle() {
        return dependsOnTaskTitle != null ? dependsOnTaskTitle : "";
    }

    public void setDependsOnTaskTitle(String dependsOnTaskTitle) {
        this.dependsOnTaskTitle = dependsOnTaskTitle;
    }

    public String getDependsOnTaskStatus() {
        return dependsOnTaskStatus != null ? dependsOnTaskStatus : "";
    }

    public void setDependsOnTaskStatus(String dependsOnTaskStatus) {
        this.dependsOnTaskStatus = dependsOnTaskStatus;
    }
}
