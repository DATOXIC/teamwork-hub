package com.teamwork.business.analytics;

import com.teamwork.business.ProjectMember;
import com.teamwork.business.Task;
import com.teamwork.business.User;
import com.teamwork.business.UserWorkload;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object (DTO) & Engine: Quản trị Năng lực & Cân bằng Tải theo Lịch (Resource Capacity & Workload Balancing).
 * - Dàn đều giờ công việc theo ngày làm việc (Daily Workload Spread: Thứ 2 đến Thứ 6).
 * - Đo lường công suất khả dụng theo định mức tuần (weeklyCapacityHours, mặc định 40h/tuần = 8h/ngày).
 * - Tự động phát hiện ai đang bị "cháy máy" (> 8h/ngày hoặc > 40h/tuần) và ai đang rảnh (< 50% công suất).
 * - Cung cấp thuật toán Gợi ý Điều phối Thông minh (Smart Rebalancing Recommendations).
 */
public class WorkloadAnalytics implements Serializable {

    private static final long serialVersionUID = 1L;

    private int userId;
    private String fullName = "";
    private String email = "";
    private String avatarUrl = "";
    private String role = "";

    // Thống kê số lượng đầu việc (Task Counts)
    private int leadTaskCount = 0;
    private int subTaskCount = 0;
    private int totalWorkCount = 0;
    private int inProgressCount = 0;
    private int doneCount = 0;
    private int overdueCount = 0;
    private int completionRate = 0;

    // Trạng thái quá tải theo số lượng
    private boolean overloaded = false;
    private String overloadReason = "Bình thường";
    private String loadLevel = "NORMAL"; // "NORMAL", "HIGH", "CRITICAL"

    // Thống kê công suất và định mức giờ (Resource Capacity)
    private double weeklyCapacityHours = 40.0;
    private double dailyCapacityHours = 8.0;
    private double assignedRemainingHours = 0.0; // Tổng giờ việc chưa xong
    private double weekAssignedHours = 0.0;        // Số giờ phải làm trong tuần đang xét

    // Phân bổ tải 5 ngày làm việc trong tuần (Daily Spread)
    private double mondayHours = 0.0;
    private double tuesdayHours = 0.0;
    private double wednesdayHours = 0.0;
    private double thursdayHours = 0.0;
    private double fridayHours = 0.0;

    private int capacityUtilizationRate = 0;       // % công suất tuần (0 - 200%)
    private String capacityStatus = "OPTIMAL";     // "UNDERUTILIZED", "OPTIMAL", "OVERLOADED"

    public WorkloadAnalytics() {}

    // ==========================================
    // DTO: GỢI Ý ĐIỀU PHỐI THÔNG MINH
    // ==========================================
    public static class RebalanceSuggestion implements Serializable {
        private static final long serialVersionUID = 1L;

        private int fromUserId;
        private String fromUserName = "";
        private int toUserId;
        private String toUserName = "";
        private int taskId;
        private String taskTitle = "";
        private double estimatedHours = 0.0;
        private String reason = "";

        public int getFromUserId() { return fromUserId; }
        public void setFromUserId(int fromUserId) { this.fromUserId = fromUserId; }

        public String getFromUserName() { return fromUserName; }
        public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

        public int getToUserId() { return toUserId; }
        public void setToUserId(int toUserId) { this.toUserId = toUserId; }

        public String getToUserName() { return toUserName; }
        public void setToUserName(String toUserName) { this.toUserName = toUserName; }

        public int getTaskId() { return taskId; }
        public void setTaskId(int taskId) { this.taskId = taskId; }

        public String getTaskTitle() { return taskTitle; }
        public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

        public double getEstimatedHours() { return estimatedHours; }
        public void setEstimatedHours(double estimatedHours) { this.estimatedHours = estimatedHours; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"fromUserId\":").append(fromUserId).append(",");
            sb.append("\"fromUserName\":\"").append(escapeJson(fromUserName)).append("\",");
            sb.append("\"toUserId\":").append(toUserId).append(",");
            sb.append("\"toUserName\":\"").append(escapeJson(toUserName)).append("\",");
            sb.append("\"taskId\":").append(taskId).append(",");
            sb.append("\"taskTitle\":\"").append(escapeJson(taskTitle)).append("\",");
            sb.append("\"estimatedHours\":").append(estimatedHours).append(",");
            sb.append("\"reason\":\"").append(escapeJson(reason)).append("\"");
            sb.append("}");
            return sb.toString();
        }
    }

    // ==========================================
    // CÁC HÀM XÂY DỰNG ANALYTICS
    // ==========================================

    /**
     * Tương thích ngược: Phân tích cơ bản từ UserWorkload.
     */
    public static List<WorkloadAnalytics> fromWorkloads(List<UserWorkload> workloads) {
        return fromWorkloads(workloads, Collections.emptyList(), Collections.emptyList(), LocalDate.now());
    }

    /**
     * Phân tích nâng cao: Kết hợp UserWorkload, ProjectMember (định mức capacity), Task (giờ ước tính và hạn chót).
     */
    public static List<WorkloadAnalytics> fromWorkloads(
            List<UserWorkload> workloads,
            List<ProjectMember> members,
            List<Task> allTasks,
            LocalDate asOfDate) {

        if (workloads == null) return Collections.emptyList();
        if (members == null) members = Collections.emptyList();
        if (allTasks == null) allTasks = Collections.emptyList();
        if (asOfDate == null) asOfDate = LocalDate.now();

        // 1. Ánh xạ định mức capacity theo userId
        Map<Integer, Integer> capacityMap = new HashMap<>();
        for (ProjectMember pm : members) {
            if (pm != null && pm.getUserId() > 0) {
                capacityMap.put(pm.getUserId(), pm.getWeeklyCapacityHours());
            }
        }

        // 2. Tính toán ngày Thứ 2 đến Thứ 6 của tuần đang xét
        LocalDate monday = asOfDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = monday.plusDays(4);

        // 3. Nhóm các task đang làm / chưa xong theo userId
        Map<Integer, List<Task>> userActiveTasks = new HashMap<>();
        for (Task t : allTasks) {
            if (t == null) continue;
            boolean isDone = "DONE".equalsIgnoreCase(t.getStatus());
            if (!isDone && t.getAssigneeId() > 0) {
                userActiveTasks.computeIfAbsent(t.getAssigneeId(), k -> new ArrayList<>()).add(t);
            }
        }

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

            // Định mức giờ làm việc
            int configuredWeeklyCapacity = capacityMap.getOrDefault(wa.userId, 40);
            wa.weeklyCapacityHours = configuredWeeklyCapacity > 0 ? configuredWeeklyCapacity : 40.0;
            wa.dailyCapacityHours = Math.round((wa.weeklyCapacityHours / 5.0) * 10.0) / 10.0;

            // Tính toán dàn trải tải (Daily Spread) cho các task chưa xong
            List<Task> activeTasks = userActiveTasks.getOrDefault(wa.userId, Collections.emptyList());
            double totalRemainingHours = 0.0;
            double[] weekDayHours = new double[5]; // 0: Mon, 1: Tue, 2: Wed, 3: Thu, 4: Fri

            for (Task t : activeTasks) {
                double hours = t.getEstimatedHours() > 0 ? t.getEstimatedHours() : 6.0; // Mặc định 6h nếu chưa ước tính
                totalRemainingHours += hours;

                // Xác định khoảng thời gian thực hiện task
                LocalDate start = BurndownCalculator.parseDate(t.getStartDate());
                LocalDate due = BurndownCalculator.parseDate(t.getDueDate());

                if (start == null) start = asOfDate;
                if (due == null) due = start.plusDays(2);
                if (due.isBefore(start)) due = start;

                // Đếm số ngày làm việc (T2 - T6) của task
                List<LocalDate> workingDays = new ArrayList<>();
                LocalDate curr = start;
                while (!curr.isAfter(due)) {
                    if (curr.getDayOfWeek() != DayOfWeek.SATURDAY && curr.getDayOfWeek() != DayOfWeek.SUNDAY) {
                        workingDays.add(curr);
                    }
                    curr = curr.plusDays(1);
                }

                if (workingDays.isEmpty()) {
                    workingDays.add(start);
                }

                double hoursPerDay = hours / workingDays.size();

                // Phân bổ giờ vào tuần hiện tại (từ monday đến friday)
                for (LocalDate wd : workingDays) {
                    if (!wd.isBefore(monday) && !wd.isAfter(friday)) {
                        int dayIndex = wd.getDayOfWeek().getValue() - 1; // 1 (Mon) -> 0, 5 (Fri) -> 4
                        if (dayIndex >= 0 && dayIndex < 5) {
                            weekDayHours[dayIndex] += hoursPerDay;
                        }
                    }
                }
            }

            wa.assignedRemainingHours = Math.round(totalRemainingHours * 10.0) / 10.0;
            wa.mondayHours = Math.round(weekDayHours[0] * 10.0) / 10.0;
            wa.tuesdayHours = Math.round(weekDayHours[1] * 10.0) / 10.0;
            wa.wednesdayHours = Math.round(weekDayHours[2] * 10.0) / 10.0;
            wa.thursdayHours = Math.round(weekDayHours[3] * 10.0) / 10.0;
            wa.fridayHours = Math.round(weekDayHours[4] * 10.0) / 10.0;

            double unroundedWeekSum = weekDayHours[0] + weekDayHours[1] + weekDayHours[2] + weekDayHours[3] + weekDayHours[4];
            wa.weekAssignedHours = Math.round(unroundedWeekSum * 10.0) / 10.0;

            if (wa.weeklyCapacityHours > 0) {
                wa.capacityUtilizationRate = (int) Math.round((wa.weekAssignedHours / wa.weeklyCapacityHours) * 100.0);
            } else {
                wa.capacityUtilizationRate = 0;
            }

            // Đánh giá trạng thái quá tải theo giờ và công suất
            boolean hasOverloadedDay = wa.mondayHours > wa.dailyCapacityHours
                    || wa.tuesdayHours > wa.dailyCapacityHours
                    || wa.wednesdayHours > wa.dailyCapacityHours
                    || wa.thursdayHours > wa.dailyCapacityHours
                    || wa.fridayHours > wa.dailyCapacityHours;

            if (wa.weekAssignedHours > wa.weeklyCapacityHours || hasOverloadedDay || (wa.overdueCount > 0 && wa.inProgressCount >= 5)) {
                wa.overloaded = true;
                wa.capacityStatus = "OVERLOADED";
                wa.loadLevel = "CRITICAL";

                if (wa.weekAssignedHours > wa.weeklyCapacityHours) {
                    wa.overloadReason = "Vượt định mức tuần: " + wa.weekAssignedHours + "h / " + (int)wa.weeklyCapacityHours + "h (" + wa.capacityUtilizationRate + "%)";
                } else if (hasOverloadedDay) {
                    wa.overloadReason = "Có ngày làm việc vượt quá định mức 8h/ngày";
                } else {
                    wa.overloadReason = "Có " + wa.overdueCount + " việc quá hạn & " + wa.inProgressCount + " việc dồn ứ";
                }
            } else if (wa.capacityUtilizationRate < 40 && wa.inProgressCount <= 1) {
                wa.overloaded = false;
                wa.capacityStatus = "UNDERUTILIZED";
                wa.loadLevel = "NORMAL";
                wa.overloadReason = "Đang rảnh (" + wa.capacityUtilizationRate + "% công suất)";
            } else {
                wa.overloaded = false;
                wa.capacityStatus = "OPTIMAL";
                wa.loadLevel = "NORMAL";
                wa.overloadReason = "Khối lượng công việc cân bằng (" + wa.capacityUtilizationRate + "% công suất)";
            }

            list.add(wa);
        }

        return list;
    }

    /**
     * Thuật toán Gợi ý Điều phối Thông minh:
     * Quét các thành viên OVERLOADED và ghép nối với các thành viên UNDERUTILIZED hoặc OPTIMAL để đề xuất san tải.
     */
    public static List<RebalanceSuggestion> generateRebalanceSuggestions(List<WorkloadAnalytics> analyticsList, List<Task> allTasks) {
        if (analyticsList == null || allTasks == null) return Collections.emptyList();

        List<WorkloadAnalytics> overloadedUsers = new ArrayList<>();
        List<WorkloadAnalytics> freeUsers = new ArrayList<>();

        for (WorkloadAnalytics wa : analyticsList) {
            if ("OVERLOADED".equals(wa.getCapacityStatus()) || wa.isOverloaded()) {
                overloadedUsers.add(wa);
            } else if ("UNDERUTILIZED".equals(wa.getCapacityStatus()) || wa.getCapacityUtilizationRate() < 60) {
                freeUsers.add(wa);
            }
        }

        if (overloadedUsers.isEmpty() || freeUsers.isEmpty()) {
            return Collections.emptyList();
        }

        // Nhóm task chưa xong theo assignee
        Map<Integer, List<Task>> tasksByAssignee = new HashMap<>();
        for (Task t : allTasks) {
            if (t == null) continue;
            if (!"DONE".equalsIgnoreCase(t.getStatus()) && t.getAssigneeId() > 0) {
                tasksByAssignee.computeIfAbsent(t.getAssigneeId(), k -> new ArrayList<>()).add(t);
            }
        }

        List<RebalanceSuggestion> suggestions = new ArrayList<>();
        int freeIndex = 0;

        for (WorkloadAnalytics oUser : overloadedUsers) {
            List<Task> candidates = tasksByAssignee.getOrDefault(oUser.getUserId(), Collections.emptyList());
            if (candidates.isEmpty()) continue;

            // Ưu tiên chọn task có trạng thái TODO, hoặc task không phải milestone
            Task chosen = null;
            for (Task t : candidates) {
                if ("TODO".equalsIgnoreCase(t.getStatus()) && !t.isMilestone()) {
                    chosen = t;
                    break;
                }
            }
            if (chosen == null) {
                for (Task t : candidates) {
                    if (!t.isMilestone()) {
                        chosen = t;
                        break;
                    }
                }
            }
            if (chosen == null) chosen = candidates.get(0);

            WorkloadAnalytics targetFreeUser = freeUsers.get(freeIndex % freeUsers.size());
            freeIndex++;

            RebalanceSuggestion sug = new RebalanceSuggestion();
            sug.fromUserId = oUser.getUserId();
            sug.fromUserName = oUser.getFullName();
            sug.toUserId = targetFreeUser.getUserId();
            sug.toUserName = targetFreeUser.getFullName();
            sug.taskId = chosen.getId();
            sug.taskTitle = chosen.getTitle();
            sug.estimatedHours = chosen.getEstimatedHours() > 0 ? chosen.getEstimatedHours() : 6.0;
            sug.reason = oUser.getFullName() + " đang quá tải (" + oUser.getCapacityUtilizationRate() + "% công suất). Đề xuất chuyển công việc cho "
                    + targetFreeUser.getFullName() + " (đang còn trống, sử dụng " + targetFreeUser.getCapacityUtilizationRate() + "% công suất).";

            suggestions.add(sug);
        }

        return suggestions;
    }

    // ================= GETTERS & SETTERS =================

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getLeadTaskCount() { return leadTaskCount; }
    public void setLeadTaskCount(int leadTaskCount) { this.leadTaskCount = leadTaskCount; }

    public int getSubTaskCount() { return subTaskCount; }
    public void setSubTaskCount(int subTaskCount) { this.subTaskCount = subTaskCount; }

    public int getTotalWorkCount() { return totalWorkCount; }
    public void setTotalWorkCount(int totalWorkCount) { this.totalWorkCount = totalWorkCount; }

    public int getInProgressCount() { return inProgressCount; }
    public void setInProgressCount(int inProgressCount) { this.inProgressCount = inProgressCount; }

    public int getDoneCount() { return doneCount; }
    public void setDoneCount(int doneCount) { this.doneCount = doneCount; }

    public int getOverdueCount() { return overdueCount; }
    public void setOverdueCount(int overdueCount) { this.overdueCount = overdueCount; }

    public int getCompletionRate() { return completionRate; }
    public void setCompletionRate(int completionRate) { this.completionRate = completionRate; }

    public boolean isOverloaded() { return overloaded; }
    public void setOverloaded(boolean overloaded) { this.overloaded = overloaded; }

    public String getOverloadReason() { return overloadReason; }
    public void setOverloadReason(String overloadReason) { this.overloadReason = overloadReason; }

    public String getLoadLevel() { return loadLevel; }
    public void setLoadLevel(String loadLevel) { this.loadLevel = loadLevel; }

    public double getWeeklyCapacityHours() { return weeklyCapacityHours; }
    public void setWeeklyCapacityHours(double weeklyCapacityHours) { this.weeklyCapacityHours = weeklyCapacityHours; }

    public double getDailyCapacityHours() { return dailyCapacityHours; }
    public void setDailyCapacityHours(double dailyCapacityHours) { this.dailyCapacityHours = dailyCapacityHours; }

    public double getAssignedRemainingHours() { return assignedRemainingHours; }
    public void setAssignedRemainingHours(double assignedRemainingHours) { this.assignedRemainingHours = assignedRemainingHours; }

    public double getWeekAssignedHours() { return weekAssignedHours; }
    public void setWeekAssignedHours(double weekAssignedHours) { this.weekAssignedHours = weekAssignedHours; }

    public double getMondayHours() { return mondayHours; }
    public double getTuesdayHours() { return tuesdayHours; }
    public double getWednesdayHours() { return wednesdayHours; }
    public double getThursdayHours() { return thursdayHours; }
    public double getFridayHours() { return fridayHours; }

    public int getCapacityUtilizationRate() { return capacityUtilizationRate; }
    public void setCapacityUtilizationRate(int capacityUtilizationRate) { this.capacityUtilizationRate = capacityUtilizationRate; }

    public String getCapacityStatus() { return capacityStatus; }
    public void setCapacityStatus(String capacityStatus) { this.capacityStatus = capacityStatus; }

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
                "\"loadLevel\":\"" + escapeJson(loadLevel) + "\"," +
                "\"weeklyCapacityHours\":" + weeklyCapacityHours + "," +
                "\"dailyCapacityHours\":" + dailyCapacityHours + "," +
                "\"assignedRemainingHours\":" + assignedRemainingHours + "," +
                "\"weekAssignedHours\":" + weekAssignedHours + "," +
                "\"mondayHours\":" + mondayHours + "," +
                "\"tuesdayHours\":" + tuesdayHours + "," +
                "\"wednesdayHours\":" + wednesdayHours + "," +
                "\"thursdayHours\":" + thursdayHours + "," +
                "\"fridayHours\":" + fridayHours + "," +
                "\"capacityUtilizationRate\":" + capacityUtilizationRate + "," +
                "\"capacityStatus\":\"" + escapeJson(capacityStatus) + "\"" +
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
