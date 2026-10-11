package com.teamwork.business.analytics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object (DTO) chứa các chỉ số đo lường Chất Lượng Nghiệm Thu & Tốc Độ Thẩm Định (PM Velocity).
 * - Tỷ lệ duyệt đạt ngay lần đầu (First-time Pass Rate - FTPR)
 * - Tốc độ phản hồi trung bình của Trưởng Dự Án (PM Review Turnaround Velocity)
 * - Danh sách công việc bị yêu cầu chỉnh sửa / từ chối nhiều nhất (Most Revised Tasks)
 * - Điểm số sức khỏe dự án tổng hợp (Project Health Score 0 - 100)
 */
public class QualityMetrics implements Serializable {

    private static final long serialVersionUID = 1L;

    private double firstTimePassRate = 100.0; // Tỷ lệ đạt lần đầu (%)
    private int firstTimePassedCount = 0;     // Số task đạt ngay Revision 1
    private int revisionRequiredCount = 0;    // Số task cần nộp lại (Revision >= 2 hoặc có REVISE/REJECT)
    private int totalReviewedTasks = 0;       // Tổng số task đã được PM đánh giá
    private int pendingReviewCount = 0;       // Số task đang chờ PM thẩm định (SUBMITTED)
    private int totalSubmissions = 0;         // Tổng số lượt nộp báo cáo nghiệm thu

    private double avgResponseHours = 0.0;    // Thời gian phản hồi trung bình (giờ)
    private double avgResponseDays = 0.0;     // Thời gian phản hồi trung bình (ngày)
    private double fastestResponseHours = 0.0;// Lượt phản hồi nhanh nhất (giờ)
    private double slowestResponseHours = 0.0;// Lượt phản hồi chậm nhất (giờ)
    private double avgQualityRating = 0.0;    // Điểm đánh giá sao trung bình (1 - 5 ⭐)

    private int healthScore = 100;            // Điểm sức khỏe dự án (0 - 100)
    private String healthStatus = "HEALTHY";  // "HEALTHY", "AT_RISK", "CRITICAL"
    private String healthDescription = "";    // Diễn giải trực quan trạng thái

    private List<RevisedTaskMetric> mostRevisedTasks = new ArrayList<>();
    private Map<Integer, Integer> ratingDistribution = new HashMap<>(); // 1 -> count, 2 -> count, ...

    public QualityMetrics() {
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0);
        }
    }

    // ================= GETTERS & SETTERS =================

    public double getFirstTimePassRate() {
        return firstTimePassRate;
    }

    public void setFirstTimePassRate(double firstTimePassRate) {
        this.firstTimePassRate = firstTimePassRate;
    }

    public int getFirstTimePassedCount() {
        return firstTimePassedCount;
    }

    public void setFirstTimePassedCount(int firstTimePassedCount) {
        this.firstTimePassedCount = firstTimePassedCount;
    }

    public int getRevisionRequiredCount() {
        return revisionRequiredCount;
    }

    public void setRevisionRequiredCount(int revisionRequiredCount) {
        this.revisionRequiredCount = revisionRequiredCount;
    }

    public int getTotalReviewedTasks() {
        return totalReviewedTasks;
    }

    public void setTotalReviewedTasks(int totalReviewedTasks) {
        this.totalReviewedTasks = totalReviewedTasks;
    }

    public int getPendingReviewCount() {
        return pendingReviewCount;
    }

    public void setPendingReviewCount(int pendingReviewCount) {
        this.pendingReviewCount = pendingReviewCount;
    }

    public int getTotalSubmissions() {
        return totalSubmissions;
    }

    public void setTotalSubmissions(int totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }

    public double getAvgResponseHours() {
        return avgResponseHours;
    }

    public void setAvgResponseHours(double avgResponseHours) {
        this.avgResponseHours = avgResponseHours;
    }

    public double getAvgResponseDays() {
        return avgResponseDays;
    }

    public void setAvgResponseDays(double avgResponseDays) {
        this.avgResponseDays = avgResponseDays;
    }

    public double getFastestResponseHours() {
        return fastestResponseHours;
    }

    public void setFastestResponseHours(double fastestResponseHours) {
        this.fastestResponseHours = fastestResponseHours;
    }

    public double getSlowestResponseHours() {
        return slowestResponseHours;
    }

    public void setSlowestResponseHours(double slowestResponseHours) {
        this.slowestResponseHours = slowestResponseHours;
    }

    public double getAvgQualityRating() {
        return avgQualityRating;
    }

    public void setAvgQualityRating(double avgQualityRating) {
        this.avgQualityRating = avgQualityRating;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(int healthScore) {
        this.healthScore = healthScore;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus != null ? healthStatus : "HEALTHY";
    }

    public String getHealthDescription() {
        return healthDescription;
    }

    public void setHealthDescription(String healthDescription) {
        this.healthDescription = healthDescription != null ? healthDescription : "";
    }

    public List<RevisedTaskMetric> getMostRevisedTasks() {
        return Collections.unmodifiableList(mostRevisedTasks);
    }

    public void setMostRevisedTasks(List<RevisedTaskMetric> mostRevisedTasks) {
        this.mostRevisedTasks = mostRevisedTasks != null ? new ArrayList<>(mostRevisedTasks) : new ArrayList<>();
    }

    public Map<Integer, Integer> getRatingDistribution() {
        return Collections.unmodifiableMap(ratingDistribution);
    }

    public void setRatingDistribution(Map<Integer, Integer> ratingDistribution) {
        this.ratingDistribution = ratingDistribution != null ? new HashMap<>(ratingDistribution) : new HashMap<>();
    }

    /**
     * Chuyển đổi sang JSON thuần an toàn cho frontend.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"firstTimePassRate\":").append(firstTimePassRate).append(",");
        sb.append("\"firstTimePassedCount\":").append(firstTimePassedCount).append(",");
        sb.append("\"revisionRequiredCount\":").append(revisionRequiredCount).append(",");
        sb.append("\"totalReviewedTasks\":").append(totalReviewedTasks).append(",");
        sb.append("\"pendingReviewCount\":").append(pendingReviewCount).append(",");
        sb.append("\"totalSubmissions\":").append(totalSubmissions).append(",");
        sb.append("\"avgResponseHours\":").append(avgResponseHours).append(",");
        sb.append("\"avgResponseDays\":").append(avgResponseDays).append(",");
        sb.append("\"fastestResponseHours\":").append(fastestResponseHours).append(",");
        sb.append("\"slowestResponseHours\":").append(slowestResponseHours).append(",");
        sb.append("\"avgQualityRating\":").append(avgQualityRating).append(",");
        sb.append("\"healthScore\":").append(healthScore).append(",");
        sb.append("\"healthStatus\":\"").append(escapeJson(healthStatus)).append("\",");
        sb.append("\"healthDescription\":\"").append(escapeJson(healthDescription)).append("\",");

        // ratingDistribution
        sb.append("\"ratingDistribution\":{");
        boolean firstRating = true;
        for (int star = 1; star <= 5; star++) {
            if (!firstRating) sb.append(",");
            firstRating = false;
            sb.append("\"").append(star).append("\":").append(ratingDistribution.getOrDefault(star, 0));
        }
        sb.append("},");

        // mostRevisedTasks
        sb.append("\"mostRevisedTasks\":[");
        for (int i = 0; i < mostRevisedTasks.size(); i++) {
            if (i > 0) sb.append(",");
            RevisedTaskMetric m = mostRevisedTasks.get(i);
            sb.append(m.toJson());
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    // ================= INNER METRIC CLASS =================

    /**
     * DTO chi tiết công việc có số lần yêu cầu chỉnh sửa / nộp lại nhiều nhất.
     */
    public static class RevisedTaskMetric implements Serializable {
        private static final long serialVersionUID = 1L;

        private int taskId;
        private String taskTitle = "";
        private String assigneeName = "";
        private int revisionCount = 1;     // Tổng số lượt nộp
        private int rejectionsCount = 0;   // Số lần bị bấm REVISE hoặc REJECT
        private String finalStatus = "";   // Trạng thái hiện tại
        private int latestRating = 0;      // Đánh giá sao gần nhất (nếu có)
        private String latestFeedback = "";// Nhận xét gần nhất của PM

        public RevisedTaskMetric() {}

        public RevisedTaskMetric(int taskId, String taskTitle, String assigneeName, int revisionCount,
                                 int rejectionsCount, String finalStatus, int latestRating, String latestFeedback) {
            this.taskId = taskId;
            this.taskTitle = taskTitle != null ? taskTitle : "";
            this.assigneeName = assigneeName != null ? assigneeName : "";
            this.revisionCount = revisionCount;
            this.rejectionsCount = rejectionsCount;
            this.finalStatus = finalStatus != null ? finalStatus : "";
            this.latestRating = latestRating;
            this.latestFeedback = latestFeedback != null ? latestFeedback : "";
        }

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
            this.taskTitle = taskTitle != null ? taskTitle : "";
        }

        public String getAssigneeName() {
            return assigneeName;
        }

        public void setAssigneeName(String assigneeName) {
            this.assigneeName = assigneeName != null ? assigneeName : "";
        }

        public int getRevisionCount() {
            return revisionCount;
        }

        public void setRevisionCount(int revisionCount) {
            this.revisionCount = revisionCount;
        }

        public int getRejectionsCount() {
            return rejectionsCount;
        }

        public void setRejectionsCount(int rejectionsCount) {
            this.rejectionsCount = rejectionsCount;
        }

        public String getFinalStatus() {
            return finalStatus;
        }

        public void setFinalStatus(String finalStatus) {
            this.finalStatus = finalStatus != null ? finalStatus : "";
        }

        public int getLatestRating() {
            return latestRating;
        }

        public void setLatestRating(int latestRating) {
            this.latestRating = latestRating;
        }

        public String getLatestFeedback() {
            return latestFeedback;
        }

        public void setLatestFeedback(String latestFeedback) {
            this.latestFeedback = latestFeedback != null ? latestFeedback : "";
        }

        public String toJson() {
            return "{" +
                    "\"taskId\":" + taskId + "," +
                    "\"taskTitle\":\"" + escapeJson(taskTitle) + "\"," +
                    "\"assigneeName\":\"" + escapeJson(assigneeName) + "\"," +
                    "\"revisionCount\":" + revisionCount + "," +
                    "\"rejectionsCount\":" + rejectionsCount + "," +
                    "\"finalStatus\":\"" + escapeJson(finalStatus) + "\"," +
                    "\"latestRating\":" + latestRating + "," +
                    "\"latestFeedback\":\"" + escapeJson(latestFeedback) + "\"" +
                    "}";
        }
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
