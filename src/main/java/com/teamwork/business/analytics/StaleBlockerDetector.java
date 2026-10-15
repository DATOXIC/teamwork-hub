package com.teamwork.business.analytics;

import com.teamwork.business.Task;
import com.teamwork.business.TaskDependency;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Engine & DTO: Phát hiện các điểm nghẽn dự án (Critical Blockers) và các công việc "chết lâm sàng" (Stale Tasks).
 * Giúp Trưởng Dự Án (PM) can thiệp kịp thời trước khi tiến độ bị vỡ dây chuyền.
 */
public class StaleBlockerDetector implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_STALE_THRESHOLD_DAYS = 3;

    // ==========================================
    // DTO: THÔNG TIN TASK CHẾT LÂM SÀNG (STALE TASK)
    // ==========================================
    public static class StaleTaskInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private int taskId;
        private String taskTitle = "";
        private int assigneeId;
        private String assigneeName = "";
        private String status = "";
        private String priority = "";
        private String dueDate = "";
        private long staleDays = 0;
        private int progressPercentage = 0;
        private double estimatedHours = 0.0;
        private String reason = "";

        public int getTaskId() { return taskId; }
        public void setTaskId(int taskId) { this.taskId = taskId; }

        public String getTaskTitle() { return taskTitle; }
        public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

        public int getAssigneeId() { return assigneeId; }
        public void setAssigneeId(int assigneeId) { this.assigneeId = assigneeId; }

        public String getAssigneeName() { return assigneeName; }
        public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }

        public long getStaleDays() { return staleDays; }
        public void setStaleDays(long staleDays) { this.staleDays = staleDays; }

        public int getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }

        public double getEstimatedHours() { return estimatedHours; }
        public void setEstimatedHours(double estimatedHours) { this.estimatedHours = estimatedHours; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"taskId\":").append(taskId).append(",");
            sb.append("\"taskTitle\":\"").append(escapeJson(taskTitle)).append("\",");
            sb.append("\"assigneeId\":").append(assigneeId).append(",");
            sb.append("\"assigneeName\":\"").append(escapeJson(assigneeName)).append("\",");
            sb.append("\"status\":\"").append(escapeJson(status)).append("\",");
            sb.append("\"priority\":\"").append(escapeJson(priority)).append("\",");
            sb.append("\"dueDate\":\"").append(escapeJson(dueDate)).append("\",");
            sb.append("\"staleDays\":").append(staleDays).append(",");
            sb.append("\"progressPercentage\":").append(progressPercentage).append(",");
            sb.append("\"estimatedHours\":").append(estimatedHours).append(",");
            sb.append("\"reason\":\"").append(escapeJson(reason)).append("\"");
            sb.append("}");
            return sb.toString();
        }
    }

    // ==========================================
    // DTO: THÔNG TIN TASK ĐIỂM NGHẼN (CRITICAL BLOCKER)
    // ==========================================
    public static class BlockerTaskInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private int taskId;
        private String taskTitle = "";
        private int assigneeId;
        private String assigneeName = "";
        private String status = "";
        private String priority = "";
        private String dueDate = "";
        private int blockedTasksCount = 0;
        private List<Integer> blockedTaskIds = new ArrayList<>();
        private List<String> blockedTaskTitles = new ArrayList<>();
        private String severity = "MEDIUM"; // CRITICAL, HIGH, MEDIUM
        private String reason = "";

        public int getTaskId() { return taskId; }
        public void setTaskId(int taskId) { this.taskId = taskId; }

        public String getTaskTitle() { return taskTitle; }
        public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

        public int getAssigneeId() { return assigneeId; }
        public void setAssigneeId(int assigneeId) { this.assigneeId = assigneeId; }

        public String getAssigneeName() { return assigneeName; }
        public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }

        public int getBlockedTasksCount() { return blockedTasksCount; }
        public void setBlockedTasksCount(int blockedTasksCount) { this.blockedTasksCount = blockedTasksCount; }

        public List<Integer> getBlockedTaskIds() { return blockedTaskIds; }
        public void setBlockedTaskIds(List<Integer> blockedTaskIds) { this.blockedTaskIds = blockedTaskIds; }

        public List<String> getBlockedTaskTitles() { return blockedTaskTitles; }
        public void setBlockedTaskTitles(List<String> blockedTaskTitles) { this.blockedTaskTitles = blockedTaskTitles; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"taskId\":").append(taskId).append(",");
            sb.append("\"taskTitle\":\"").append(escapeJson(taskTitle)).append("\",");
            sb.append("\"assigneeId\":").append(assigneeId).append(",");
            sb.append("\"assigneeName\":\"").append(escapeJson(assigneeName)).append("\",");
            sb.append("\"status\":\"").append(escapeJson(status)).append("\",");
            sb.append("\"priority\":\"").append(escapeJson(priority)).append("\",");
            sb.append("\"dueDate\":\"").append(escapeJson(dueDate)).append("\",");
            sb.append("\"blockedTasksCount\":").append(blockedTasksCount).append(",");
            sb.append("\"severity\":\"").append(escapeJson(severity)).append("\",");
            sb.append("\"reason\":\"").append(escapeJson(reason)).append("\",");
            sb.append("\"blockedTaskTitles\":[");
            for (int i = 0; i < blockedTaskTitles.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(blockedTaskTitles.get(i))).append("\"");
            }
            sb.append("]");
            sb.append("}");
            return sb.toString();
        }
    }

    // ==========================================
    // DTO: BÁO CÁO TỔNG THỂ STALE & BLOCKER
    // ==========================================
    public static class StaleBlockerReport implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<StaleTaskInfo> staleTasks = new ArrayList<>();
        private List<BlockerTaskInfo> criticalBlockers = new ArrayList<>();
        private List<NearMissTask> nearMissTasks = new ArrayList<>();
        private int totalTasks = 0;
        private int totalStaleCount = 0;
        private int totalBlockerCount = 0;
        private int totalBlockedTasksCount = 0;
        private int totalNearMissCount = 0;
        private int blockerImpactScore = 0; // % tổng số task bị cản trở (0 - 100%)

        public List<StaleTaskInfo> getStaleTasks() { return staleTasks; }
        public List<BlockerTaskInfo> getCriticalBlockers() { return criticalBlockers; }
        public List<NearMissTask> getNearMissTasks() { return nearMissTasks; }
        public int getTotalTasks() { return totalTasks; }
        public int getTotalStaleCount() { return totalStaleCount; }
        public int getTotalBlockerCount() { return totalBlockerCount; }
        public int getTotalBlockedTasksCount() { return totalBlockedTasksCount; }
        public int getTotalNearMissCount() { return totalNearMissCount; }
        public int getBlockerImpactScore() { return blockerImpactScore; }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"totalTasks\":").append(totalTasks).append(",");
            sb.append("\"totalStaleCount\":").append(totalStaleCount).append(",");
            sb.append("\"totalBlockerCount\":").append(totalBlockerCount).append(",");
            sb.append("\"totalBlockedTasksCount\":").append(totalBlockedTasksCount).append(",");
            sb.append("\"totalNearMissCount\":").append(totalNearMissCount).append(",");
            sb.append("\"blockerImpactScore\":").append(blockerImpactScore).append(",");

            sb.append("\"staleTasks\":[");
            for (int i = 0; i < staleTasks.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(staleTasks.get(i).toJson());
            }
            sb.append("],");

            sb.append("\"criticalBlockers\":[");
            for (int i = 0; i < criticalBlockers.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(criticalBlockers.get(i).toJson());
            }
            sb.append("],");

            sb.append("\"nearMissTasks\":[");
            for (int i = 0; i < nearMissTasks.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(nearMissTasks.get(i).toJson());
            }
            sb.append("]");

            sb.append("}");
            return sb.toString();
        }
    }

    // ==========================================
    // ENGINE DETECTION LOGIC
    // ==========================================

    public static StaleBlockerReport detect(List<Task> tasks, List<TaskDependency> dependencies, LocalDate asOfDate) {
        return detect(tasks, dependencies, asOfDate, DEFAULT_STALE_THRESHOLD_DAYS);
    }

    public static StaleBlockerReport detect(List<Task> tasks, List<TaskDependency> dependencies, LocalDate asOfDate, int staleThresholdDays) {
        StaleBlockerReport report = new StaleBlockerReport();
        if (tasks == null) tasks = Collections.emptyList();
        if (dependencies == null) dependencies = Collections.emptyList();
        if (asOfDate == null) asOfDate = LocalDate.now();
        if (staleThresholdDays <= 0) staleThresholdDays = DEFAULT_STALE_THRESHOLD_DAYS;

        report.totalTasks = tasks.size();

        Map<Integer, Task> taskMap = new HashMap<>();
        for (Task t : tasks) {
            if (t != null && t.getId() > 0) {
                taskMap.put(t.getId(), t);
            }
        }

        // 1. Phát hiện Stale Tasks
        for (Task t : tasks) {
            if (t == null) continue;
            boolean isDone = "DONE".equalsIgnoreCase(t.getStatus());
            if (isDone) continue;

            // Chỉ xét các task đang trong luồng thực thi: IN_PROGRESS, REVISE, SUBMITTED hoặc TODO đã quá ngày bắt đầu
            LocalDate lastActiveDate = resolveLastActiveDate(t);
            long daysInactive = 0;
            if (lastActiveDate != null) {
                daysInactive = ChronoUnit.DAYS.between(lastActiveDate, asOfDate);
            }

            boolean isStale = false;
            String staleReason = "";

            if ("IN_PROGRESS".equalsIgnoreCase(t.getStatus()) || "REVISE".equalsIgnoreCase(t.getStatus())) {
                if (daysInactive >= staleThresholdDays) {
                    isStale = true;
                    staleReason = "Đang thực hiện nhưng không có cập nhật trong " + daysInactive + " ngày qua";
                }
            } else if ("SUBMITTED".equalsIgnoreCase(t.getStatus())) {
                if (daysInactive >= staleThresholdDays) {
                    isStale = true;
                    staleReason = "Đã nộp bàn giao nhưng chờ PM duyệt hơn " + daysInactive + " ngày";
                }
            } else if ("TODO".equalsIgnoreCase(t.getStatus())) {
                LocalDate startDate = BurndownCalculator.parseDate(t.getStartDate());
                if (startDate != null && asOfDate.isAfter(startDate.plusDays(staleThresholdDays))) {
                    isStale = true;
                    long overdueStartDays = ChronoUnit.DAYS.between(startDate, asOfDate);
                    staleReason = "Chưa bắt đầu sau " + overdueStartDays + " ngày kể từ ngày bắt đầu dự kiến";
                }
            }

            // Kiểm tra thêm trường hợp quá hạn mà tiến độ chưa xong
            LocalDate dueDate = BurndownCalculator.parseDate(t.getDueDate());
            if (!isStale && dueDate != null && asOfDate.isAfter(dueDate)) {
                long overdueDays = ChronoUnit.DAYS.between(dueDate, asOfDate);
                if (overdueDays >= staleThresholdDays) {
                    isStale = true;
                    staleReason = "Đã quá hạn chót " + overdueDays + " ngày và chưa hoàn thành";
                }
            }

            if (isStale) {
                StaleTaskInfo sti = new StaleTaskInfo();
                sti.taskId = t.getId();
                sti.taskTitle = t.getTitle();
                sti.assigneeId = t.getAssigneeId();
                sti.assigneeName = t.getAssigneeName() != null ? t.getAssigneeName() : "Chưa phân công";
                sti.status = t.getStatus();
                sti.priority = t.getPriority();
                sti.dueDate = t.getDueDate() != null ? t.getDueDate() : "";
                sti.staleDays = Math.max(0, daysInactive);
                sti.progressPercentage = t.getProgressPercentage();
                sti.estimatedHours = t.getEstimatedHours();
                sti.reason = staleReason;

                report.staleTasks.add(sti);
            }
        }

        // 2. Phát hiện Critical Blockers từ TaskDependency
        // dependency: taskId (successor / đang đợi) phụ thuộc vào dependsOnTaskId (predecessor / người cản trở)
        Map<Integer, List<Integer>> blockerMap = new HashMap<>(); // predecessorId -> list of blocked taskId
        for (TaskDependency dep : dependencies) {
            if (dep == null) continue;
            int predId = dep.getDependsOnTaskId();
            int succId = dep.getTaskId();

            Task predTask = taskMap.get(predId);
            Task succTask = taskMap.get(succId);

            // Chỉ coi là đang chặn nếu task tiên quyết CHƯA HOÀN THÀNH và task chịu phụ thuộc CHƯA HOÀN THÀNH
            if (predTask != null && succTask != null) {
                boolean predDone = "DONE".equalsIgnoreCase(predTask.getStatus());
                boolean succDone = "DONE".equalsIgnoreCase(succTask.getStatus());
                if (!predDone && !succDone) {
                    blockerMap.computeIfAbsent(predId, k -> new ArrayList<>()).add(succId);
                }
            }
        }

        Set<Integer> allUniqueBlockedTaskIds = new HashSet<>();

        for (Map.Entry<Integer, List<Integer>> entry : blockerMap.entrySet()) {
            int predId = entry.getKey();
            List<Integer> succIds = entry.getValue();
            Task predTask = taskMap.get(predId);
            if (predTask == null) continue;

            BlockerTaskInfo bti = new BlockerTaskInfo();
            bti.taskId = predTask.getId();
            bti.taskTitle = predTask.getTitle();
            bti.assigneeId = predTask.getAssigneeId();
            bti.assigneeName = predTask.getAssigneeName() != null ? predTask.getAssigneeName() : "Chưa phân công";
            bti.status = predTask.getStatus();
            bti.priority = predTask.getPriority();
            bti.dueDate = predTask.getDueDate() != null ? predTask.getDueDate() : "";
            bti.blockedTasksCount = succIds.size();
            bti.blockedTaskIds = succIds;

            for (Integer sid : succIds) {
                allUniqueBlockedTaskIds.add(sid);
                Task st = taskMap.get(sid);
                if (st != null) {
                    bti.blockedTaskTitles.add(st.getTitle());
                } else {
                    bti.blockedTaskTitles.add("Task #" + sid);
                }
            }

            // Đánh giá mức độ nghiêm trọng của Blocker
            LocalDate dueDate = BurndownCalculator.parseDate(predTask.getDueDate());
            boolean isOverdue = dueDate != null && asOfDate.isAfter(dueDate);
            boolean isHighPri = "HIGH".equalsIgnoreCase(predTask.getPriority());

            if (isOverdue || succIds.size() >= 2) {
                bti.severity = "CRITICAL";
                bti.reason = isOverdue
                        ? ("Đã quá hạn chót và đang trực tiếp chặn " + succIds.size() + " công việc tiếp theo")
                        : ("Đang chặn đồng thời " + succIds.size() + " công việc tiếp theo");
            } else if (isHighPri || "REVISE".equalsIgnoreCase(predTask.getStatus())) {
                bti.severity = "HIGH";
                bti.reason = "Đang bị yêu cầu chỉnh sửa hoặc ưu tiên CAO, cản trở công việc tiếp theo";
            } else {
                bti.severity = "MEDIUM";
                bti.reason = "Công việc tiên quyết chưa hoàn thành (" + predTask.getStatus() + ")";
            }

            report.criticalBlockers.add(bti);
        }

        // Sắp xếp blockers: CRITICAL trước, sau đó HIGH, sau đó MEDIUM
        report.criticalBlockers.sort((a, b) -> {
            int scoreA = "CRITICAL".equals(a.severity) ? 3 : ("HIGH".equals(a.severity) ? 2 : 1);
            int scoreB = "CRITICAL".equals(b.severity) ? 3 : ("HIGH".equals(b.severity) ? 2 : 1);
            if (scoreA != scoreB) return Integer.compare(scoreB, scoreA);
            return Integer.compare(b.blockedTasksCount, a.blockedTasksCount);
        });

        // Sắp xếp stale tasks: số ngày trơ nhiều nhất lên đầu
        report.staleTasks.sort((a, b) -> Long.compare(b.staleDays, a.staleDays));

        report.totalStaleCount = report.staleTasks.size();
        report.totalBlockerCount = report.criticalBlockers.size();
        report.totalBlockedTasksCount = allUniqueBlockedTaskIds.size();

        // 3. Phát hiện Nguy Cơ Cận Kề Trước 24h (Near-Miss Proactive Detection)
        for (Task t : tasks) {
            if (t == null) continue;
            boolean isDone = "DONE".equalsIgnoreCase(t.getStatus());
            if (isDone) continue;

            LocalDate dueDate = BurndownCalculator.parseDate(t.getDueDate());
            String riskLevel = null;
            String riskReason = null;

            if (dueDate != null) {
                long daysToDue = ChronoUnit.DAYS.between(asOfDate, dueDate);
                if (daysToDue >= 0 && daysToDue <= 1) {
                    if ("TODO".equalsIgnoreCase(t.getStatus()) || "PLANNING".equalsIgnoreCase(t.getStatus())) {
                        riskLevel = "CRITICAL";
                        riskReason = "Hạn chót trong 24h (" + (daysToDue == 0 ? "Hôm nay" : "Ngày mai") + ") nhưng chưa bắt đầu thực hiện!";
                    } else if (t.getProgressPercentage() < 40) {
                        riskLevel = "HIGH";
                        riskReason = "Hạn chót trong 24h (" + (daysToDue == 0 ? "Hôm nay" : "Ngày mai") + ") nhưng tiến độ mới đạt " + t.getProgressPercentage() + "%";
                    }
                }
            }

            if (riskLevel == null && t.getRescheduleCount() >= 2) {
                riskLevel = "WARNING";
                riskReason = "Đã dời hạn chót " + t.getRescheduleCount() + " lần liên tiếp (Nguy cơ vỡ kế hoạch)";
            }

            if (riskLevel == null && ("CRITICAL".equalsIgnoreCase(t.getPriority()) || "HIGH".equalsIgnoreCase(t.getPriority()))
                    && "REVISE".equalsIgnoreCase(t.getStatus())) {
                riskLevel = "HIGH";
                riskReason = "Công việc ưu tiên CAO bị yêu cầu nộp lại, cần trưởng nhóm hỗ trợ giải quyết";
            }

            if (riskLevel != null) {
                NearMissTask nmt = new NearMissTask(
                    t.getId(),
                    t.getTitle(),
                    t.getAssigneeId(),
                    t.getAssigneeName(),
                    t.getDueDate(),
                    t.getStatus(),
                    t.getProgressPercentage(),
                    t.getPriority(),
                    riskLevel,
                    riskReason,
                    t.getEstimatedHours(),
                    t.getRescheduleCount()
                );
                report.nearMissTasks.add(nmt);
            }
        }

        // Sắp xếp nearMissTasks: CRITICAL trước, sau đó HIGH, sau đó WARNING
        report.nearMissTasks.sort((a, b) -> {
            int scoreA = "CRITICAL".equals(a.getRiskLevel()) ? 3 : ("HIGH".equals(a.getRiskLevel()) ? 2 : 1);
            int scoreB = "CRITICAL".equals(b.getRiskLevel()) ? 3 : ("HIGH".equals(b.getRiskLevel()) ? 2 : 1);
            return Integer.compare(scoreB, scoreA);
        });

        report.totalNearMissCount = report.nearMissTasks.size();

        if (report.totalTasks > 0) {
            report.blockerImpactScore = (int) Math.round(((double) report.totalBlockedTasksCount / report.totalTasks) * 100.0);
        } else {
            report.blockerImpactScore = 0;
        }

        return report;
    }

    private static LocalDate resolveLastActiveDate(Task t) {
        if (t == null) return null;

        // Ưu tiên 1: updatedAt
        LocalDate d = parseAnyDate(t.getUpdatedAt());
        if (d != null) return d;

        // Ưu tiên 2: submittedAt hoặc reviewedAt hoặc planningReviewedAt
        d = parseAnyDate(t.getSubmittedAt());
        if (d != null) return d;

        d = parseAnyDate(t.getReviewedAt());
        if (d != null) return d;

        d = parseAnyDate(t.getPlanningReviewedAt());
        if (d != null) return d;

        // Ưu tiên 3: startDate
        d = BurndownCalculator.parseDate(t.getStartDate());
        if (d != null) return d;

        // Ưu tiên 4: createdAt
        d = parseAnyDate(t.getCreatedAt());
        if (d != null) return d;

        return null;
    }

    private static LocalDate parseAnyDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        String val = s.trim();

        // Thử cắt lấy 10 ký tự đầu nếu là YYYY-MM-DD
        if (val.length() >= 10 && val.charAt(4) == '-' && val.charAt(7) == '-') {
            try {
                return LocalDate.parse(val.substring(0, 10));
            } catch (Exception ignored) {}
        }

        // Thử format dd/MM/yyyy
        if (val.length() >= 10 && val.charAt(2) == '/' && val.charAt(5) == '/') {
            try {
                return LocalDate.parse(val.substring(0, 10), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception ignored) {}
        }

        return null;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
