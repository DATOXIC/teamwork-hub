package com.teamwork.business.analytics;

import com.teamwork.business.Task;
import com.teamwork.business.TaskSubmission;
import com.teamwork.business.UserWorkload;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thuật toán tính toán Chất Lượng Bàn Giao & Vận Tốc Phản Hồi của Quản Lý (PM Velocity):
 * - Tỷ lệ duyệt đạt ngay lần đầu (First-time Pass Rate - FTPR).
 * - Xác định các đầu việc bị sửa đổi / từ chối nhiều nhất (Most Revised Tasks).
 * - Thời gian phản hồi trung bình (turnaround velocity) giữa submittedAt và reviewedAt.
 * - Điểm sức khỏe dự án tổng hợp (Project Health Index).
 * 
 * Thuần POJO, độc lập tuyệt đối với CSDL, hỗ trợ unit test 100% offline.
 */
public final class QualityVelocityCalculator {

    private static final DateTimeFormatter DMY_HM_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter YMD_HM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private QualityVelocityCalculator() {}

    /**
     * Tính toán đầy đủ kèm Burndown và Workload để xác định điểm sức khỏe toàn diện.
     */
    public static QualityMetrics calculate(List<TaskSubmission> submissions, List<Task> tasks,
                                           List<UserWorkload> workloads, BurndownData burndown) {
        QualityMetrics metrics = new QualityMetrics();

        List<TaskSubmission> safeSubs = (submissions != null) ? submissions : Collections.emptyList();
        List<Task> safeTasks = (tasks != null) ? tasks : Collections.emptyList();

        Map<Integer, Task> taskMap = new HashMap<>();
        for (Task t : safeTasks) {
            if (t != null) {
                taskMap.put(t.getId(), t);
            }
        }

        metrics.setTotalSubmissions(safeSubs.size());

        // 1. Nhóm các đợt nộp theo Task ID
        Map<Integer, List<TaskSubmission>> subsByTaskId = new HashMap<>();
        for (TaskSubmission sub : safeSubs) {
            if (sub != null && sub.getTaskId() > 0) {
                subsByTaskId.computeIfAbsent(sub.getTaskId(), k -> new ArrayList<>()).add(sub);
            }
        }

        // 2. Tính toán First-time Pass Rate (FTPR) & Thống kê đánh giá sao (Rating Distribution)
        int firstTimePassed = 0;
        int revisionRequired = 0;
        int pendingReview = 0;
        int totalRatingSum = 0;
        int totalRatedCount = 0;

        Map<Integer, Integer> ratingDist = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDist.put(i, 0);
        }

        List<QualityMetrics.RevisedTaskMetric> revisedTasksList = new ArrayList<>();

        for (Map.Entry<Integer, List<TaskSubmission>> entry : subsByTaskId.entrySet()) {
            int taskId = entry.getKey();
            List<TaskSubmission> taskSubs = entry.getValue();

            // Sắp xếp các đợt nộp theo số revision tăng dần
            taskSubs.sort(Comparator.comparingInt(TaskSubmission::getRevisionNumber));

            Task taskObj = taskMap.get(taskId);
            String taskTitle = (taskObj != null && taskObj.getTitle() != null) ? taskObj.getTitle() : ("Công việc #" + taskId);
            String assigneeName = (taskObj != null && taskObj.getAssigneeName() != null) ? taskObj.getAssigneeName() : "Chưa phân công";

            // Tìm Revision 1
            TaskSubmission rev1 = null;
            for (TaskSubmission s : taskSubs) {
                if (s.getRevisionNumber() == 1) {
                    rev1 = s;
                    break;
                }
            }
            if (rev1 == null && !taskSubs.isEmpty()) {
                rev1 = taskSubs.get(0);
            }

            int rejectionsCount = 0;
            for (TaskSubmission s : taskSubs) {
                String st = (s.getStatus() != null) ? s.getStatus().toUpperCase() : "";
                if ("REVISE".equals(st) || "REJECTED".equals(st)) {
                    rejectionsCount++;
                }
                // Thống kê sao đánh giá (1 - 5 ⭐)
                if (s.getQualityRating() >= 1 && s.getQualityRating() <= 5) {
                    ratingDist.put(s.getQualityRating(), ratingDist.getOrDefault(s.getQualityRating(), 0) + 1);
                    totalRatingSum += s.getQualityRating();
                    totalRatedCount++;
                }
            }

            // Đánh giá đạt ngay lần đầu (FTPR):
            if (rev1 != null) {
                String st1 = (rev1.getStatus() != null) ? rev1.getStatus().toUpperCase() : "SUBMITTED";
                if ("APPROVED".equals(st1)) {
                    firstTimePassed++;
                } else if ("REVISE".equals(st1) || "REJECTED".equals(st1) || taskSubs.size() > 1) {
                    revisionRequired++;
                } else if ("SUBMITTED".equals(st1)) {
                    pendingReview++;
                }
            }

            // Thu thập metric cho danh sách công việc cần chỉnh sửa nhiều
            TaskSubmission latestSub = taskSubs.get(taskSubs.size() - 1);
            QualityMetrics.RevisedTaskMetric rtm = new QualityMetrics.RevisedTaskMetric(
                    taskId,
                    taskTitle,
                    assigneeName,
                    taskSubs.size(),
                    rejectionsCount,
                    latestSub.getStatus(),
                    latestSub.getQualityRating(),
                    latestSub.getPmFeedback()
            );
            revisedTasksList.add(rtm);
        }

        int totalReviewedTasks = firstTimePassed + revisionRequired;
        double ftpr = 100.0;
        if (totalReviewedTasks > 0) {
            ftpr = Math.round(((double) firstTimePassed / totalReviewedTasks) * 1000.0) / 10.0;
        }

        metrics.setFirstTimePassedCount(firstTimePassed);
        metrics.setRevisionRequiredCount(revisionRequired);
        metrics.setPendingReviewCount(pendingReview);
        metrics.setTotalReviewedTasks(totalReviewedTasks);
        metrics.setFirstTimePassRate(ftpr);
        metrics.setRatingDistribution(ratingDist);
        if (totalRatedCount > 0) {
            metrics.setAvgQualityRating(Math.round(((double) totalRatingSum / totalRatedCount) * 10.0) / 10.0);
        }

        // Sắp xếp danh sách công việc bị chỉnh sửa nhiều nhất (ưu tiên số lần từ chối DESC, sau đó đến số revision DESC)
        revisedTasksList.sort((a, b) -> {
            int cmpRej = Integer.compare(b.getRejectionsCount(), a.getRejectionsCount());
            if (cmpRej != 0) return cmpRej;
            return Integer.compare(b.getRevisionCount(), a.getRevisionCount());
        });
        if (revisedTasksList.size() > 10) {
            revisedTasksList = new ArrayList<>(revisedTasksList.subList(0, 10));
        }
        metrics.setMostRevisedTasks(revisedTasksList);

        // 3. Tính toán Vận tốc phản hồi của Trưởng Dự Án (PM Turnaround Velocity)
        double totalTurnaroundHours = 0.0;
        int reviewedSubmissionsCount = 0;
        double fastestHours = Double.MAX_VALUE;
        double slowestHours = 0.0;

        for (TaskSubmission sub : safeSubs) {
            if (sub == null) continue;
            String subAt = sub.getSubmittedAt();
            String revAt = sub.getReviewedAt();
            if (subAt != null && !subAt.trim().isEmpty() && revAt != null && !revAt.trim().isEmpty()) {
                LocalDateTime startDt = parseDateTime(subAt);
                LocalDateTime endDt = parseDateTime(revAt);
                if (startDt != null && endDt != null && !endDt.isBefore(startDt)) {
                    Duration duration = Duration.between(startDt, endDt);
                    double hours = duration.toMinutes() / 60.0;
                    totalTurnaroundHours += hours;
                    reviewedSubmissionsCount++;
                    if (hours < fastestHours) fastestHours = hours;
                    if (hours > slowestHours) slowestHours = hours;
                }
            }
        }

        if (reviewedSubmissionsCount > 0) {
            double avgHours = Math.round((totalTurnaroundHours / reviewedSubmissionsCount) * 10.0) / 10.0;
            metrics.setAvgResponseHours(avgHours);
            metrics.setAvgResponseDays(Math.round((avgHours / 24.0) * 10.0) / 10.0);
            metrics.setFastestResponseHours(Math.round(fastestHours * 10.0) / 10.0);
            metrics.setSlowestResponseHours(Math.round(slowestHours * 10.0) / 10.0);
        } else {
            metrics.setAvgResponseHours(0.0);
            metrics.setAvgResponseDays(0.0);
            metrics.setFastestResponseHours(0.0);
            metrics.setSlowestResponseHours(0.0);
        }

        // 4. Tính toán Điểm Sức Khỏe Dự Án Tổng Hợp (0 - 100)
        computeHealthScore(metrics, safeTasks, workloads, burndown);

        return metrics;
    }

    /**
     * Overload tiện lợi khi chỉ truyền submissions và tasks.
     */
    public static QualityMetrics calculate(List<TaskSubmission> submissions, List<Task> tasks) {
        return calculate(submissions, tasks, null, null);
    }

    /**
     * Thuật toán tính Điểm Sức Khỏe Dự Án (Project Health Index):
     * Tổng hợp đa chiều: Tiến độ Burndown, Tỷ lệ duyệt đạt lần đầu, Việc quá hạn, Khối lượng quá tải.
     */
    private static void computeHealthScore(QualityMetrics metrics, List<Task> tasks,
                                           List<UserWorkload> workloads, BurndownData burndown) {
        int score = 100;

        // Yếu tố 1: Tiến độ so với đường lý tưởng Burndown (-0 đến -30 điểm)
        if (burndown != null && !burndown.isOnTrack()) {
            double var = burndown.getVarianceTasks();
            int schedulePenalty = (int) Math.round(var * 6.0);
            score -= Math.min(30, Math.max(5, schedulePenalty));
        }

        // Yếu tố 2: Số công việc quá hạn (-8 điểm mỗi việc, tối đa -30 điểm)
        int overdueCount = 0;
        for (Task t : tasks) {
            if (t != null && t.isOverdue()) {
                overdueCount++;
            }
        }
        if (overdueCount > 0) {
            int overduePenalty = Math.min(30, overdueCount * 8);
            score -= overduePenalty;
        }

        // Yếu tố 3: Tỷ lệ duyệt đạt lần đầu (FTPR) kém (< 80% trừ điểm)
        if (metrics.getTotalReviewedTasks() > 0 && metrics.getFirstTimePassRate() < 80.0) {
            double gap = 80.0 - metrics.getFirstTimePassRate();
            int qualityPenalty = (int) Math.round(gap * 0.4);
            score -= Math.min(25, qualityPenalty);
        }

        // Yếu tố 4: Số lượng thành viên bị quá tải (-5 điểm mỗi người, tối đa -15 điểm)
        if (workloads != null) {
            int overloadedCount = 0;
            for (UserWorkload uw : workloads) {
                if (uw != null && (uw.getInProgressTasks() >= 5 || uw.getOverdueTasks() > 0)) {
                    overloadedCount++;
                }
            }
            if (overloadedCount > 0) {
                score -= Math.min(15, overloadedCount * 5);
            }
        }

        // Đảm bảo điểm số trong khoảng [0, 100]
        if (score < 0) score = 0;
        if (score > 100) score = 100;
        metrics.setHealthScore(score);

        if (score >= 75) {
            metrics.setHealthStatus("HEALTHY");
            metrics.setHealthDescription("Dự án hoạt động lành mạnh, nhịp độ hoàn thành tiến độ ổn định và chất lượng nghiệm thu tốt.");
        } else if (score >= 50) {
            metrics.setHealthStatus("AT_RISK");
            metrics.setHealthDescription("Dự án tiềm ẩn rủi ro: Cần chú ý điều phối các việc trễ hạn và cân đối tải làm việc của nhóm.");
        } else {
            metrics.setHealthStatus("CRITICAL");
            metrics.setHealthDescription("Báo động khẩn cấp: Dự án chậm tiến độ nặng hoặc có nhiều công việc bị quá hạn và yêu cầu nộp lại.");
        }
    }

    /**
     * Phân tích chuỗi ngày giờ đa định dạng sang LocalDateTime.
     */
    public static LocalDateTime parseDateTime(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return null;

        try {
            if (trimmed.contains("/") && trimmed.contains(":")) {
                // "12/10/2026 14:30" hoặc "12/10/2026 14:30:00"
                if (trimmed.length() >= 16) {
                    return LocalDateTime.parse(trimmed.substring(0, 16), DMY_HM_FORMATTER);
                }
            } else if (trimmed.contains("-") && trimmed.contains(":")) {
                if (trimmed.contains("T")) {
                    return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } else if (trimmed.length() >= 16) {
                    return LocalDateTime.parse(trimmed.substring(0, 16), YMD_HM_FORMATTER);
                }
            }
        } catch (Exception ignored) {
            // Fallback: nếu chuỗi chỉ có ngày mà không có giờ
            try {
                if (trimmed.length() >= 10) {
                    String dPart = trimmed.substring(0, 10);
                    if (dPart.contains("/")) {
                        return java.time.LocalDate.parse(dPart, DateTimeFormatter.ofPattern("dd/MM/yyyy")).atStartOfDay();
                    } else if (dPart.contains("-")) {
                        return java.time.LocalDate.parse(dPart, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
                    }
                }
            } catch (Exception ignored2) {
                return null;
            }
        }
        return null;
    }
}
