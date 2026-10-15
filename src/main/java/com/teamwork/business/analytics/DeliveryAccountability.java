package com.teamwork.business.analytics;

import com.teamwork.business.Task;
import com.teamwork.business.User;
import com.teamwork.business.UserWorkload;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Transfer Object & Engine: Đo lường tính chịu trách nhiệm bàn giao (Delivery Accountability)
 * và độ tin cậy thực hiện cam kết (Say-Do Ratio, On-Time Delivery Rate).
 * 
 * Chuẩn POJO thuần túy, tương thích 100% với JUnit 5 offline.
 */
public class DeliveryAccountability implements Serializable {

    private static final long serialVersionUID = 1L;

    private int userId;
    private String fullName = "";
    private String role = "";

    private int totalAssignedTasks = 0;   // Tổng số việc được giao
    private int completedTasks = 0;       // Số việc đã hoàn thành
    private int onTimeCompletedTasks = 0; // Số việc hoàn thành đúng hạn hoặc trước hạn
    private int lateCompletedTasks = 0;   // Số việc hoàn thành nhưng bị trễ hạn
    private int overdueActiveTasks = 0;   // Số việc chưa xong và hiện đang quá hạn
    private int totalRescheduledTasks = 0;// Tổng số lần xin dời hạn chót của các công việc

    private int onTimeDeliveryRate = 100; // Tỷ lệ giao hàng đúng hẹn (OTDR 0 - 100%)
    private int sayDoRatio = 0;           // Tỷ lệ cam kết hoàn thành (Say-Do Ratio 0 - 100%)
    private String reliabilityRating = "EXCELLENT"; // EXCELLENT (>=90%), GOOD (75-89%), NEEDS_IMPROVEMENT (<75%)

    public DeliveryAccountability() {}

    /**
     * Thuật toán phân tích tính chịu trách nhiệm từ danh sách thành viên và các đầu việc được giao.
     */
    public static List<DeliveryAccountability> analyze(List<User> users, List<Task> allTasks, LocalDate asOfDate) {
        if (users == null || allTasks == null) return Collections.emptyList();
        if (asOfDate == null) asOfDate = LocalDate.now();

        List<DeliveryAccountability> result = new ArrayList<>();

        for (User u : users) {
            if (u == null) continue;

            DeliveryAccountability da = new DeliveryAccountability();
            da.userId = u.getId();
            da.fullName = (u.getFullName() != null && !u.getFullName().trim().isEmpty())
                    ? u.getFullName().trim() : ("Thành viên #" + u.getId());
            da.role = u.getRole() != null ? u.getRole() : "MEMBER";

            int assigned = 0;
            int done = 0;
            int onTime = 0;
            int late = 0;
            int overdueActive = 0;
            int rescheduled = 0;

            for (Task t : allTasks) {
                if (t == null) continue;
                if (t.getAssigneeId() != u.getId()) continue;

                assigned++;
                if (t.getRescheduleCount() > 0) {
                    rescheduled += t.getRescheduleCount();
                }

                boolean isDone = "DONE".equalsIgnoreCase(t.getStatus());

                if (isDone) {
                    done++;
                    LocalDate compDate = BurndownCalculator.resolveCompletionDate(t, asOfDate);
                    LocalDate dueDate = BurndownCalculator.parseDate(t.getDueDate());

                    if (dueDate != null && compDate != null) {
                        if (compDate.isAfter(dueDate)) {
                            late++;
                        } else {
                            onTime++;
                        }
                    } else {
                        onTime++; // Mặc định không có deadline coi như đạt
                    }
                } else {
                    // Việc chưa xong: kiểm tra xem hiện tại đã quá hạn chưa
                    LocalDate dueDate = BurndownCalculator.parseDate(t.getDueDate());
                    if (dueDate != null && dueDate.isBefore(asOfDate)) {
                        overdueActive++;
                    }
                }
            }

            da.totalAssignedTasks = assigned;
            da.completedTasks = done;
            da.onTimeCompletedTasks = onTime;
            da.lateCompletedTasks = late;
            da.overdueActiveTasks = overdueActive;
            da.totalRescheduledTasks = rescheduled;

            // Tính On-Time Delivery Rate (OTDR)
            if (done > 0) {
                da.onTimeDeliveryRate = (int) Math.round(((double) onTime / done) * 100.0);
            } else {
                da.onTimeDeliveryRate = 100; // Chưa có task done nào bị trễ
            }

            // Tính Say-Do Ratio (Cam kết vs Hoàn thành)
            if (assigned > 0) {
                da.sayDoRatio = (int) Math.round(((double) done / assigned) * 100.0);
            } else {
                da.sayDoRatio = 100;
            }

            // Xếp hạng độ tin cậy: Nếu dời deadline quá nhiều lần (>= 3) thì không thể xếp EXCELLENT
            if (da.onTimeDeliveryRate >= 90 && da.overdueActiveTasks == 0 && da.totalRescheduledTasks <= 1) {
                da.reliabilityRating = "EXCELLENT";
            } else if (da.onTimeDeliveryRate >= 75 && da.overdueActiveTasks <= 1 && da.totalRescheduledTasks <= 3) {
                da.reliabilityRating = "GOOD";
            } else {
                da.reliabilityRating = "NEEDS_IMPROVEMENT";
            }

            result.add(da);
        }

        return result;
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

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    public int getTotalAssignedTasks() {
        return totalAssignedTasks;
    }
    public int getCompletedTasks() {
        return completedTasks;
    }
    public int getOnTimeCompletedTasks() {
        return onTimeCompletedTasks;
    }
    public int getLateCompletedTasks() {
        return lateCompletedTasks;
    }
    public int getOverdueActiveTasks() {
        return overdueActiveTasks;
    }
    public int getTotalRescheduledTasks() {
        return totalRescheduledTasks;
    }

    public int getOnTimeDeliveryRate() {
        return onTimeDeliveryRate;
    }
    public int getSayDoRatio() {
        return sayDoRatio;
    }
    public String getReliabilityRating() {
        return reliabilityRating;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"userId\":").append(userId).append(",");
        sb.append("\"fullName\":\"").append(escapeJson(fullName)).append("\",");
        sb.append("\"role\":\"").append(escapeJson(role)).append("\",");
        sb.append("\"totalAssignedTasks\":").append(totalAssignedTasks).append(",");
        sb.append("\"completedTasks\":").append(completedTasks).append(",");
        sb.append("\"onTimeCompletedTasks\":").append(onTimeCompletedTasks).append(",");
        sb.append("\"lateCompletedTasks\":").append(lateCompletedTasks).append(",");
        sb.append("\"overdueActiveTasks\":").append(overdueActiveTasks).append(",");
        sb.append("\"totalRescheduledTasks\":").append(totalRescheduledTasks).append(",");
        sb.append("\"onTimeDeliveryRate\":").append(onTimeDeliveryRate).append(",");
        sb.append("\"sayDoRatio\":").append(sayDoRatio).append(",");
        sb.append("\"reliabilityRating\":\"").append(reliabilityRating).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
