package com.teamwork.business.analytics;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) đại diện cho một Công Việc Có Nguy Cơ Cận Kề Trước 24h (Near-Miss Risk).
 * 
 * Phát hiện chủ động các tình huống:
 * 1. Hạn chót trong 24h (hôm nay hoặc ngày mai) nhưng chưa bắt đầu (0%) hoặc tiến độ < 40%.
 * 2. Công việc độ ưu tiên CRITICAL / HIGH bị trả về chỉnh sửa (REVISE) hoặc dậm chân.
 * 3. Công việc đã bị dời deadline liên tục (rescheduleCount >= 2) có nguy cơ trượt hạn tiếp.
 */
public class NearMissTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private int taskId;
    private String taskTitle = "";
    private int assigneeId;
    private String assigneeName = "";
    private String dueDate = "";
    private String status = "";
    private int progress = 0;
    private String priority = "MEDIUM";
    private String riskLevel = "WARNING"; // CRITICAL, HIGH, WARNING
    private String riskReason = "";
    private double estimatedHours = 0.0;
    private int rescheduleCount = 0;

    public NearMissTask() {
    }

    public NearMissTask(int taskId, String taskTitle, int assigneeId, String assigneeName,
                        String dueDate, String status, int progress, String priority,
                        String riskLevel, String riskReason, double estimatedHours, int rescheduleCount) {
        this.taskId = taskId;
        this.taskTitle = taskTitle != null ? taskTitle : "";
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName != null ? assigneeName : "Chưa phân công";
        this.dueDate = dueDate != null ? dueDate : "";
        this.status = status != null ? status : "TODO";
        this.progress = progress;
        this.priority = priority != null ? priority : "MEDIUM";
        this.riskLevel = riskLevel != null ? riskLevel : "WARNING";
        this.riskReason = riskReason != null ? riskReason : "";
        this.estimatedHours = estimatedHours;
        this.rescheduleCount = rescheduleCount;
    }

    // ================= GETTERS & SETTERS =================

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public int getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(int assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRiskReason() {
        return riskReason;
    }

    public void setRiskReason(String riskReason) {
        this.riskReason = riskReason;
    }

    public double getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public int getRescheduleCount() {
        return rescheduleCount;
    }

    public void setRescheduleCount(int rescheduleCount) {
        this.rescheduleCount = rescheduleCount;
    }

    public String toJson() {
        return "{\"taskId\":" + taskId
                + ",\"taskTitle\":\"" + escapeJson(taskTitle) + "\""
                + ",\"assigneeId\":" + assigneeId
                + ",\"assigneeName\":\"" + escapeJson(assigneeName) + "\""
                + ",\"dueDate\":\"" + escapeJson(dueDate) + "\""
                + ",\"status\":\"" + escapeJson(status) + "\""
                + ",\"progress\":" + progress
                + ",\"priority\":\"" + escapeJson(priority) + "\""
                + ",\"riskLevel\":\"" + escapeJson(riskLevel) + "\""
                + ",\"riskReason\":\"" + escapeJson(riskReason) + "\""
                + ",\"estimatedHours\":" + estimatedHours
                + ",\"rescheduleCount\":" + rescheduleCount
                + "}";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
