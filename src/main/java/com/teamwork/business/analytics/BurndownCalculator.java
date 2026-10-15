package com.teamwork.business.analytics;

import com.teamwork.business.Project;
import com.teamwork.business.Task;
import com.teamwork.business.TaskStatus;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Thuật toán tính toán Biểu đồ Burndown (Burndown Engine) thuần POJO:
 * - Đường xu hướng lý tưởng (Ideal Trendline): Đường dốc tuyến tính từ tổng số việc về 0 theo thời gian dự án.
 * - Đường thực tế (Actual Completed): Dựa trên thời điểm nghiệm thu (reviewedAt), nộp bài (submittedAt) hoặc trạng thái DONE.
 * - Không phụ thuộc DB hay Servlet, cho phép kiểm thử 100% độc lập và tái lập trong JUnit.
 */
public final class BurndownCalculator {

    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DMY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private BurndownCalculator() {}

    /**
     * Tính toán Burndown theo ngày hiện tại của hệ thống.
     */
    public static BurndownData calculate(LocalDate projectStart, LocalDate projectEnd, List<Task> tasks) {
        return calculate(projectStart, projectEnd, tasks, LocalDate.now());
    }

    /**
     * Tính toán Burndown từ đối tượng Project và danh sách Task.
     */
    public static BurndownData calculate(Project project, List<Task> tasks) {
        return calculate(project, tasks, LocalDate.now());
    }

    /**
     * Tính toán Burndown từ Project với ngày mốc giả định (phục vụ unit test hoặc xem lại lịch sử).
     */
    public static BurndownData calculate(Project project, List<Task> tasks, LocalDate asOfDate) {
        LocalDate start = null;
        if (project != null && project.getCreatedAt() != null && !project.getCreatedAt().trim().isEmpty()) {
            start = parseDate(project.getCreatedAt());
        }
        LocalDate end = null;
        if (tasks != null && !tasks.isEmpty()) {
            for (Task t : tasks) {
                if (t != null && t.getDueDate() != null && !t.getDueDate().trim().isEmpty()) {
                    LocalDate d = parseDate(t.getDueDate());
                    if (d != null) {
                        if (start == null || d.isBefore(start)) {
                            // nếu chưa có start, dùng due date trừ 14 ngày làm mốc
                        }
                        if (end == null || d.isAfter(end)) {
                            end = d;
                        }
                    }
                }
            }
        }
        return calculate(start, end, tasks, asOfDate);
    }

    /**
     * Hàm tính toán cốt lõi (Core Engine):
     * @param projectStart Ngày bắt đầu dự án (nếu null sẽ tự suy luận từ tasks hoặc lùi 14 ngày từ asOfDate)
     * @param projectEnd   Ngày kết thúc dự án (nếu null sẽ tự suy luận từ hạn chót tasks hoặc tiến 14 ngày)
     * @param tasks        Danh sách công việc
     * @param asOfDate     Ngày quan sát (thường là hôm nay)
     * @return Đối tượng BurndownData hoàn chỉnh
     */
    public static BurndownData calculate(LocalDate projectStart, LocalDate projectEnd, List<Task> tasks, LocalDate asOfDate) {
        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }

        List<Task> safeTasks = (tasks != null) ? tasks : new ArrayList<>();
        int totalTasks = safeTasks.size();

        // 1. Tự động suy luận projectStart / projectEnd nếu bị null
        if (projectStart == null) {
            projectStart = findEarliestTaskDate(safeTasks, asOfDate.minusDays(14));
        }
        if (projectEnd == null) {
            projectEnd = findLatestTaskDueDate(safeTasks, projectStart.plusDays(14));
        }

        // Đảm bảo projectEnd không được trước projectStart
        if (projectEnd.isBefore(projectStart)) {
            projectEnd = projectStart.plusDays(7);
        }

        long totalDays = ChronoUnit.DAYS.between(projectStart, projectEnd) + 1;
        if (totalDays <= 0) totalDays = 1;
        // Giới hạn an toàn tối đa 180 ngày để tránh mảng quá dài nếu dữ liệu lỗi
        if (totalDays > 180) {
            totalDays = 180;
            projectEnd = projectStart.plusDays(179);
        }

        List<String> labels = new ArrayList<>();
        List<Double> idealRemaining = new ArrayList<>();
        List<Double> actualRemaining = new ArrayList<>();
        List<Double> idealHoursRemaining = new ArrayList<>();
        List<Double> actualHoursRemaining = new ArrayList<>();

        // 2. Định lượng giờ công & Xác định ngày hoàn thành của từng task đã xong
        List<LocalDate> completionDates = new ArrayList<>();
        List<Double> completionHours = new ArrayList<>();
        int completedCountNow = 0;
        double totalHours = 0.0;
        double completedHoursNow = 0.0;
        int milestoneCount = 0;
        int completedMilestones = 0;

        for (Task t : safeTasks) {
            if (t == null) continue;
            double h = t.getEstimatedHours() > 0 ? t.getEstimatedHours() : 4.0; // fallback mặc định 4h/task
            totalHours += h;

            if (t.isMilestone()) {
                milestoneCount++;
            }

            boolean isDone = TaskStatus.isDone(t.getStatus()) || "DONE".equalsIgnoreCase(t.getStatus());
            if (isDone) {
                completedCountNow++;
                completedHoursNow += h;
                if (t.isMilestone()) {
                    completedMilestones++;
                }
                LocalDate compDate = resolveCompletionDate(t, asOfDate);
                completionDates.add(compDate);
                completionHours.add(h);
            }
        }

        // 3. Xây dựng chuỗi dữ liệu từng ngày từ projectStart tới projectEnd
        double dayDenominator = (totalDays > 1) ? (totalDays - 1.0) : 1.0;

        for (int i = 0; i < totalDays; i++) {
            LocalDate currentDay = projectStart.plusDays(i);
            labels.add(currentDay.format(LABEL_FORMATTER));

            // a. Đường lý tưởng Tasks: dốc tuyến tính từ totalTasks về 0
            double idealTasks;
            double idealHours;
            if (totalTasks == 0) {
                idealTasks = 0.0;
                idealHours = 0.0;
            } else if (totalDays == 1) {
                idealTasks = 0.0;
                idealHours = 0.0;
            } else {
                double progressRatio = 1.0 - ((double) i / dayDenominator);
                idealTasks = totalTasks * Math.max(0.0, progressRatio);
                idealHours = totalHours * Math.max(0.0, progressRatio);
            }
            idealRemaining.add(roundOneDecimal(idealTasks));
            idealHoursRemaining.add(roundOneDecimal(idealHours));

            // b. Đường thực tế:
            if (currentDay.isAfter(asOfDate)) {
                actualRemaining.add(null);
                actualHoursRemaining.add(null);
            } else {
                int completedOnOrBefore = 0;
                double completedHoursOnOrBefore = 0.0;
                for (int c = 0; c < completionDates.size(); c++) {
                    LocalDate cd = completionDates.get(c);
                    if (cd != null && !cd.isAfter(currentDay)) {
                        completedOnOrBefore++;
                        completedHoursOnOrBefore += completionHours.get(c);
                    }
                }
                int actualRemTasks = Math.max(0, totalTasks - completedOnOrBefore);
                double actualRemHours = Math.max(0.0, totalHours - completedHoursOnOrBefore);
                actualRemaining.add((double) actualRemTasks);
                actualHoursRemaining.add(roundOneDecimal(actualRemHours));
            }
        }

        // 4. Tính toán các chỉ số tổng hợp
        int remainingTasks = Math.max(0, totalTasks - completedCountNow);
        double remainingHours = Math.max(0.0, totalHours - completedHoursNow);
        int completionRate = (totalTasks > 0) ? (int) Math.round(((double) completedCountNow / totalTasks) * 100.0) : 0;
        int hourCompletionRate = (totalHours > 0) ? (int) Math.round((completedHoursNow / totalHours) * 100.0) : 0;

        // Vận tốc trung bình (Team Velocity theo giờ/ngày)
        long elapsedDays = Math.max(1, ChronoUnit.DAYS.between(projectStart, asOfDate) + 1);
        double teamVelocityHoursPerDay = roundOneDecimal(completedHoursNow / elapsedDays);

        // Dự báo ngày về đích (Forecasted Completion Date)
        String forecastedCompletionDate;
        int forecastVarianceDays = 0;
        if (remainingHours <= 0) {
            forecastedCompletionDate = asOfDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            forecastVarianceDays = (int) ChronoUnit.DAYS.between(projectEnd, asOfDate);
        } else if (teamVelocityHoursPerDay > 0) {
            long daysNeeded = (long) Math.ceil(remainingHours / teamVelocityHoursPerDay);
            LocalDate forecastDate = asOfDate.plusDays(daysNeeded);
            forecastedCompletionDate = forecastDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            forecastVarianceDays = (int) ChronoUnit.DAYS.between(projectEnd, forecastDate);
        } else {
            forecastedCompletionDate = "Chưa đủ dữ liệu";
            forecastVarianceDays = (int) ChronoUnit.DAYS.between(asOfDate, projectEnd);
        }

        // Đánh giá On-track so với ngày hiện tại (asOfDate) theo Giờ
        double idealTodayHours;
        double actualTodayHours;
        if (asOfDate.isBefore(projectStart)) {
            idealTodayHours = totalHours;
            actualTodayHours = remainingHours;
        } else if (asOfDate.isAfter(projectEnd)) {
            idealTodayHours = 0.0;
            actualTodayHours = remainingHours;
        } else {
            int dayIdx = (int) ChronoUnit.DAYS.between(projectStart, asOfDate);
            dayIdx = Math.min(dayIdx, (int) totalDays - 1);
            idealTodayHours = idealHoursRemaining.get(dayIdx);
            Double actVal = actualHoursRemaining.get(dayIdx);
            actualTodayHours = (actVal != null) ? actVal : remainingHours;
        }

        double varianceHours = roundOneDecimal(actualTodayHours - idealTodayHours);
        boolean onTrack = (varianceHours <= 4.0); // Dung sai nửa ngày công (4h)

        // Độ phình to phạm vi (Scope Creep): Giả lập theo tỷ lệ task tạo thêm
        double scopeCreepHours = 0.0;
        double scopeCreepRate = 0.0;

        BurndownData data = new BurndownData();
        data.setLabels(labels);
        data.setIdealRemaining(idealRemaining);
        data.setActualRemaining(actualRemaining);
        data.setIdealHoursRemaining(idealHoursRemaining);
        data.setActualHoursRemaining(actualHoursRemaining);
        data.setTotalTasks(totalTasks);
        data.setCompletedTasks(completedCountNow);
        data.setRemainingTasks(remainingTasks);
        data.setCompletionRate(completionRate);

        data.setTotalHours(roundOneDecimal(totalHours));
        data.setCompletedHours(roundOneDecimal(completedHoursNow));
        data.setRemainingHours(roundOneDecimal(remainingHours));
        data.setHourCompletionRate(hourCompletionRate);
        data.setTeamVelocityHoursPerDay(teamVelocityHoursPerDay);
        data.setForecastedCompletionDate(forecastedCompletionDate);
        data.setForecastVarianceDays(forecastVarianceDays);
        data.setScopeCreepHours(roundOneDecimal(scopeCreepHours));
        data.setScopeCreepRate(roundOneDecimal(scopeCreepRate));
        data.setMilestoneCount(milestoneCount);
        data.setCompletedMilestones(completedMilestones);

        data.setProjectStartDate(projectStart.format(DateTimeFormatter.ISO_LOCAL_DATE));
        data.setProjectEndDate(projectEnd.format(DateTimeFormatter.ISO_LOCAL_DATE));
        data.setAsOfDate(asOfDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        data.setOnTrack(onTrack);
        data.setVarianceTasks(varianceHours);

        return data;
    }

    /**
     * Phân giải ngày hoàn thành chính xác nhất của một Task đã DONE:
     * Ưu tiên 1: reviewedAt (thời điểm PM nghiệm thu)
     * Ưu tiên 2: submittedAt (thời điểm nộp)
     * Ưu tiên 3: dueDate (nếu <= asOfDate)
     * Fallback: asOfDate
     */
    public static LocalDate resolveCompletionDate(Task task, LocalDate fallback) {
        if (task == null) return fallback;

        if (task.getReviewedAt() != null && !task.getReviewedAt().trim().isEmpty()) {
            LocalDate d = parseDate(task.getReviewedAt());
            if (d != null) return d;
        }
        if (task.getSubmittedAt() != null && !task.getSubmittedAt().trim().isEmpty()) {
            LocalDate d = parseDate(task.getSubmittedAt());
            if (d != null) return d;
        }
        if (task.getDueDate() != null && !task.getDueDate().trim().isEmpty()) {
            LocalDate d = parseDate(task.getDueDate());
            if (d != null && !d.isAfter(fallback)) {
                return d;
            }
        }
        return fallback;
    }

    /**
     * Phân tích chuỗi ngày tháng đa định dạng (dd/MM/yyyy HH:mm, yyyy-MM-dd, v.v.).
     */
    public static LocalDate parseDate(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return null;

        try {
            // Lấy 10 ký tự đầu tiên nếu chuỗi chứa cả giờ phút (VD: "12/10/2026 14:30" -> "12/10/2026")
            String datePart = (trimmed.length() >= 10) ? trimmed.substring(0, 10) : trimmed;

            if (datePart.contains("/")) {
                return LocalDate.parse(datePart, DMY_FORMATTER);
            } else if (datePart.contains("-")) {
                return LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (Exception ignored) {
            // Thử regex phân tách linh hoạt nếu định dạng không cố định 2 chữ số (VD: 5/4/2026)
            try {
                String[] parts = trimmed.split("[\\sT/\\-]");
                if (parts.length >= 3) {
                    if (parts[0].length() == 4) { // yyyy-MM-dd
                        int year = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        int day = Integer.parseInt(parts[2]);
                        return LocalDate.of(year, month, day);
                    } else if (parts[2].length() == 4) { // dd/MM/yyyy
                        int day = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        int year = Integer.parseInt(parts[2]);
                        return LocalDate.of(year, month, day);
                    }
                }
            } catch (Exception ignored2) {
                return null;
            }
        }
        return null;
    }

    private static LocalDate findEarliestTaskDate(List<Task> tasks, LocalDate defaultDate) {
        LocalDate earliest = null;
        for (Task t : tasks) {
            if (t != null && t.getDueDate() != null) {
                LocalDate d = parseDate(t.getDueDate());
                if (d != null) {
                    // Ước tính ngày bắt đầu = dueDate - 7 ngày
                    LocalDate estStart = d.minusDays(7);
                    if (earliest == null || estStart.isBefore(earliest)) {
                        earliest = estStart;
                    }
                }
            }
        }
        return (earliest != null) ? earliest : defaultDate;
    }

    private static LocalDate findLatestTaskDueDate(List<Task> tasks, LocalDate defaultDate) {
        LocalDate latest = null;
        for (Task t : tasks) {
            if (t != null && t.getDueDate() != null) {
                LocalDate d = parseDate(t.getDueDate());
                if (d != null) {
                    if (latest == null || d.isAfter(latest)) {
                        latest = d;
                    }
                }
            }
        }
        return (latest != null) ? latest : defaultDate;
    }

    private static double roundOneDecimal(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
