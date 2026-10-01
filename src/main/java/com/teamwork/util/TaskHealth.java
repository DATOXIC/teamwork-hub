package com.teamwork.util;

import com.teamwork.business.Task;

/**
 * Điểm "sức khỏe" của Task (0-100) tính từ hạn chót so với % tiến độ.
 * level: "ok" (xanh), "warn" (vàng), "risk" (đỏ), "none" (không đánh giá).
 */
public class TaskHealth {
    private final int score;
    private final String level;
    private final String reason;

    private TaskHealth(int score, String level, String reason) {
        this.score = score;
        this.level = level;
        this.reason = reason;
    }

    public int getScore() { return score; }
    public String getLevel() { return level; }
    public String getReason() { return reason; }

    public static TaskHealth of(Task t, int progressPct) {
        String st = t.getStatus() == null ? "" : t.getStatus().toUpperCase();
        if (st.equals("DONE") || st.equals("APPROVED")) return new TaskHealth(100, "none", "Đã hoàn thành");
        long days = t.getDaysRemaining();
        if (days == Long.MAX_VALUE) return new TaskHealth(100, "none", "Chưa đặt hạn chót");
        if (days < 0) return new TaskHealth(0, "risk", "Trễ " + (-days) + " ngày");

        // Thời gian còn lại càng ít mà tiến độ càng thấp thì điểm càng thấp
        int timePressure = days <= 0 ? 100 : days <= 2 ? 80 : days <= 5 ? 50 : days <= 10 ? 25 : 0;
        int gap = Math.max(0, timePressure - progressPct);
        int score = Math.max(0, 100 - gap);
        if (score < 40) return new TaskHealth(score, "risk", "Còn " + days + " ngày, mới xong " + progressPct + "%");
        if (score < 75) return new TaskHealth(score, "warn", "Còn " + days + " ngày, xong " + progressPct + "%");
        return new TaskHealth(score, "ok", "Đúng tiến độ (" + progressPct + "%)");
    }
}
