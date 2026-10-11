package com.teamwork.business.analytics;

import com.teamwork.business.User;
import com.teamwork.business.UserWorkload;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Transfer Object (DTO) & Engine phụ trợ biểu diễn phân bổ tải công việc (Workload Balance Heatmap).
 * Phát hiện thành viên bị quá tải (Overload Alert) theo tiêu chí doanh nghiệp:
 * - Số việc đang triển khai (in-progress) >= 5
 * - Hoặc có việc bị quá hạn (overdue) > 0.
 */
public class WorkloadAnalytics implements Serializable {

    private static final long serialVersionUID = 1L;

    private int userId;
    private String fullName = "";
    private String email = "";
    private String avatarUrl = "";
    private String role = "";

    private int leadTaskCount = 0;
    private int subTaskCount = 0;
    private int totalWorkCount = 0;
    private int inProgressCount = 0;
    private int doneCount = 0;
    private int overdueCount = 0;
    private int completionRate = 0;

    private boolean overloaded = false;
    private String overloadReason = "Bình thường";
    private String loadLevel = "NORMAL"; // "NORMAL", "HIGH", "CRITICAL"

    public WorkloadAnalytics() {}

    public static List<WorkloadAnalytics> fromWorkloads(List<UserWorkload> workloads) {
        if (workloads == null) return Collections.emptyList();

        List<WorkloadAnalytics> list = new ArrayList<>();
        for (UserWorkload uw : workloads) {
            if (uw == null) continue;
            WorkloadAnalytics wa = new WorkloadAnalytics();
            User u = uw.getUser();
            if (u != null) {
                wa.userId = u.getId();
                wa.fullName = (u.getFullName() != null && !u.getFullName().trim().isEmpty()) 
                        ? u.getFullName().trim() : ("Thành viên #" + u.getId());
                wa.email = u.getEmail() != null ? u.getEmail() : "";
                wa.avatarUrl = u.getAvatar() != null ? u.getAvatar() : "";
                wa.role = u.getRole() != null ? u.getRole() : "MEMBER";
            } else {
                wa.userId = 0;
                wa.fullName = "Ẩn danh";
            }

            wa.leadTaskCount = uw.getLeadTaskCount();
            wa.subTaskCount = uw.getSubTaskCount();
            wa.totalWorkCount = uw.getTotalWorkCount();
            wa.inProgressCount = Math.max(uw.getInProgressCount(), uw.getInProgressTasks());
            wa.doneCount = Math.max(uw.getDoneCount(), uw.getDoneTasks());
            wa.overdueCount = Math.max(uw.getOverdueCount(), uw.getOverdueTasks());
            wa.completionRate = uw.getCompletionRate();

            // QUY TẮC PHÁT HIỆN QUÁ TẢI (OVERLOAD THRESHOLD):
            // 1. Nếu có việc quá hạn > 0 -> CRITICAL
            // 2. Nếu in-progress >= 5 -> HIGH (hoặc CRITICAL nếu in-progress >= 8)
            if (wa.overdueCount > 0 && wa.inProgressCount >= 5) {
                wa.overloaded = true;
                wa.loadLevel = "CRITICAL";
                wa.overloadReason = "Báo động đỏ: Có " + wa.overdueCount + " việc quá hạn & " + wa.inProgressCount + " việc đang làm";
            } else if (wa.overdueCount > 0) {
                wa.overloaded = true;
                wa.loadLevel = "CRITICAL";
                wa.overloadReason = "Có " + wa.overdueCount + " việc bị quá hạn";
            } else if (wa.inProgressCount >= 5) {
                wa.overloaded = true;
                wa.loadLevel = "HIGH";
                wa.overloadReason = "Đang gánh " + wa.inProgressCount + " việc cùng lúc (vượt ngưỡng an toàn 5 việc)";
            } else {
                wa.overloaded = false;
                wa.loadLevel = "NORMAL";
                wa.overloadReason = "Khối lượng công việc cân bằng";
            }

            list.add(wa);
        }
        return list;
    }

    // ================= GETTERS & SETTERS =================

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getLeadTaskCount() {
        return leadTaskCount;
    }

    public void setLeadTaskCount(int leadTaskCount) {
        this.leadTaskCount = leadTaskCount;
    }

    public int getSubTaskCount() {
        return subTaskCount;
    }

    public void setSubTaskCount(int subTaskCount) {
        this.subTaskCount = subTaskCount;
    }

    public int getTotalWorkCount() {
        return totalWorkCount;
    }

    public void setTotalWorkCount(int totalWorkCount) {
        this.totalWorkCount = totalWorkCount;
    }

    public int getInProgressCount() {
        return inProgressCount;
    }

    public void setInProgressCount(int inProgressCount) {
        this.inProgressCount = inProgressCount;
    }

    public int getDoneCount() {
        return doneCount;
    }

    public void setDoneCount(int doneCount) {
        this.doneCount = doneCount;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(int overdueCount) {
        this.overdueCount = overdueCount;
    }

    public int getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(int completionRate) {
        this.completionRate = completionRate;
    }

    public boolean isOverloaded() {
        return overloaded;
    }

    public void setOverloaded(boolean overloaded) {
        this.overloaded = overloaded;
    }

    public String getOverloadReason() {
        return overloadReason;
    }

    public void setOverloadReason(String overloadReason) {
        this.overloadReason = overloadReason;
    }

    public String getLoadLevel() {
        return loadLevel;
    }

    public void setLoadLevel(String loadLevel) {
        this.loadLevel = loadLevel;
    }

    public String toJson() {
        return "{" +
                "\"userId\":" + userId + "," +
                "\"fullName\":\"" + escapeJson(fullName) + "\"," +
                "\"email\":\"" + escapeJson(email) + "\"," +
                "\"avatarUrl\":\"" + escapeJson(avatarUrl) + "\"," +
                "\"role\":\"" + escapeJson(role) + "\"," +
                "\"leadTaskCount\":" + leadTaskCount + "," +
                "\"subTaskCount\":" + subTaskCount + "," +
                "\"totalWorkCount\":" + totalWorkCount + "," +
                "\"inProgressCount\":" + inProgressCount + "," +
                "\"doneCount\":" + doneCount + "," +
                "\"overdueCount\":" + overdueCount + "," +
                "\"completionRate\":" + completionRate + "," +
                "\"overloaded\":" + overloaded + "," +
                "\"overloadReason\":\"" + escapeJson(overloadReason) + "\"," +
                "\"loadLevel\":\"" + escapeJson(loadLevel) + "\"" +
                "}";
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
