package com.teamwork.business.analytics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Transfer Object (DTO) chứa kết quả tính toán biểu đồ Burndown (Burndown Chart).
 * Bao gồm đường xu hướng lý tưởng (Ideal Trendline), đường thực tế (Actual Remaining),
 * nhãn ngày và các chỉ số đo lường tiến độ tổng hợp.
 * 
 * Tuân thủ chuẩn POJO thuần túy, tương thích 100% với môi trường offline unit testing và Chart.js 4.4+.
 */
public class BurndownData implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> labels = new ArrayList<>();
    private List<Double> idealRemaining = new ArrayList<>();
    private List<Double> actualRemaining = new ArrayList<>();

    private int totalTasks;
    private int completedTasks;
    private int remainingTasks;
    private int completionRate; // Phần trăm hoàn thành 0 - 100%

    private String projectStartDate = "";
    private String projectEndDate = "";
    private String asOfDate = "";

    private boolean onTrack = true;     // Tiến độ thực tế có đang bằng hoặc vượt mức lý tưởng không
    private double varianceTasks = 0.0; // Số task chênh lệch so với đường lý tưởng (âm là vượt tiến độ, dương là chậm)

    public BurndownData() {
    }

    public BurndownData(List<String> labels, List<Double> idealRemaining, List<Double> actualRemaining,
                        int totalTasks, int completedTasks, int remainingTasks, int completionRate,
                        String projectStartDate, String projectEndDate, String asOfDate,
                        boolean onTrack, double varianceTasks) {
        this.labels = labels != null ? new ArrayList<>(labels) : new ArrayList<>();
        this.idealRemaining = idealRemaining != null ? new ArrayList<>(idealRemaining) : new ArrayList<>();
        this.actualRemaining = actualRemaining != null ? new ArrayList<>(actualRemaining) : new ArrayList<>();
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.remainingTasks = remainingTasks;
        this.completionRate = completionRate;
        this.projectStartDate = projectStartDate != null ? projectStartDate : "";
        this.projectEndDate = projectEndDate != null ? projectEndDate : "";
        this.asOfDate = asOfDate != null ? asOfDate : "";
        this.onTrack = onTrack;
        this.varianceTasks = varianceTasks;
    }

    // ================= GETTERS & SETTERS =================

    public List<String> getLabels() {
        return Collections.unmodifiableList(labels);
    }

    public void setLabels(List<String> labels) {
        this.labels = labels != null ? new ArrayList<>(labels) : new ArrayList<>();
    }

    public List<Double> getIdealRemaining() {
        return Collections.unmodifiableList(idealRemaining);
    }

    public void setIdealRemaining(List<Double> idealRemaining) {
        this.idealRemaining = idealRemaining != null ? new ArrayList<>(idealRemaining) : new ArrayList<>();
    }

    public List<Double> getActualRemaining() {
        return Collections.unmodifiableList(actualRemaining);
    }

    public void setActualRemaining(List<Double> actualRemaining) {
        this.actualRemaining = actualRemaining != null ? new ArrayList<>(actualRemaining) : new ArrayList<>();
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getRemainingTasks() {
        return remainingTasks;
    }

    public void setRemainingTasks(int remainingTasks) {
        this.remainingTasks = remainingTasks;
    }

    public int getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(int completionRate) {
        this.completionRate = completionRate;
    }

    public String getProjectStartDate() {
        return projectStartDate;
    }

    public void setProjectStartDate(String projectStartDate) {
        this.projectStartDate = projectStartDate != null ? projectStartDate : "";
    }

    public String getProjectEndDate() {
        return projectEndDate;
    }

    public void setProjectEndDate(String projectEndDate) {
        this.projectEndDate = projectEndDate != null ? projectEndDate : "";
    }

    public String getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(String asOfDate) {
        this.asOfDate = asOfDate != null ? asOfDate : "";
    }

    public boolean isOnTrack() {
        return onTrack;
    }

    public void setOnTrack(boolean onTrack) {
        this.onTrack = onTrack;
    }

    public double getVarianceTasks() {
        return varianceTasks;
    }

    public void setVarianceTasks(double varianceTasks) {
        this.varianceTasks = varianceTasks;
    }

    /**
     * Chuyển đổi DTO sang chuỗi JSON thuần an toàn XSS để render trực tiếp vào Chart.js hoặc gửi qua AJAX.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        sb.append("\"totalTasks\":").append(totalTasks).append(",");
        sb.append("\"completedTasks\":").append(completedTasks).append(",");
        sb.append("\"remainingTasks\":").append(remainingTasks).append(",");
        sb.append("\"completionRate\":").append(completionRate).append(",");
        sb.append("\"projectStartDate\":\"").append(escapeJson(projectStartDate)).append("\",");
        sb.append("\"projectEndDate\":\"").append(escapeJson(projectEndDate)).append("\",");
        sb.append("\"asOfDate\":\"").append(escapeJson(asOfDate)).append("\",");
        sb.append("\"onTrack\":").append(onTrack).append(",");
        sb.append("\"varianceTasks\":").append(varianceTasks).append(",");

        // labels
        sb.append("\"labels\":[");
        for (int i = 0; i < labels.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(labels.get(i))).append("\"");
        }
        sb.append("],");

        // idealRemaining
        sb.append("\"idealRemaining\":[");
        for (int i = 0; i < idealRemaining.size(); i++) {
            if (i > 0) sb.append(",");
            Double v = idealRemaining.get(i);
            sb.append(v != null ? String.valueOf(v) : "null");
        }
        sb.append("],");

        // actualRemaining
        sb.append("\"actualRemaining\":[");
        for (int i = 0; i < actualRemaining.size(); i++) {
            if (i > 0) sb.append(",");
            Double v = actualRemaining.get(i);
            sb.append(v != null ? String.valueOf(v) : "null");
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("<", "\\u003c")
                .replace(">", "\\u003e")
                .replace("&", "\\u0026");
    }
}
