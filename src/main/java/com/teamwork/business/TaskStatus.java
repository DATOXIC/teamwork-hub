package com.teamwork.business;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Trạng thái của Công việc (Task) và BẢNG CHUYỂN TRẠNG THÁI hợp lệ.
 *
 * <p>Cột {@code status} trong DB vẫn lưu chuỗi (tên enum) để không phải đổi schema;
 * mọi so sánh trong code đi qua enum này thay vì gõ tay {@code "DONE"}, {@code "TODO"}...</p>
 *
 * <pre>
 *  TODO ──gửi kế hoạch──▶ PLANNING ──PM duyệt──▶ IN_PROGRESS ──nộp bàn giao──▶ SUBMITTED ──PM duyệt──▶ DONE
 *   ▲                        │ PM trả lại            │                             │ PM trả sửa / từ chối
 *   └────────────────────────┘                       │                             ▼
 *                                                    └── (không bật duyệt) ──▶ DONE   REVISE / REJECTED ──nộp lại──▶ SUBMITTED
 * </pre>
 *
 * Việc "có được đi cạnh này không" còn phụ thuộc vai trò và chế độ duyệt (Quality Gate);
 * phần đó do các handler kiểm tra. Bảng này chỉ trả lời: cạnh này có tồn tại trong quy trình không.
 */
public enum TaskStatus {
    TODO("Cần làm"),
    PLANNING("Chờ duyệt kế hoạch"),
    IN_PROGRESS("Đang làm"),
    SUBMITTED("Chờ nghiệm thu"),
    REVISE("Cần chỉnh sửa"),
    REJECTED("Chưa đạt yêu cầu"),
    DONE("Hoàn thành");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    private static final Map<TaskStatus, Set<TaskStatus>> NEXT = new EnumMap<>(TaskStatus.class);

    static {
        NEXT.put(TODO, EnumSet.of(PLANNING, IN_PROGRESS, SUBMITTED, DONE));
        NEXT.put(PLANNING, EnumSet.of(TODO, IN_PROGRESS));           // PM trả lại / PM duyệt kế hoạch
        NEXT.put(IN_PROGRESS, EnumSet.of(TODO, SUBMITTED, DONE));
        NEXT.put(SUBMITTED, EnumSet.of(DONE, REVISE, REJECTED));      // chỉ PM xử lý bài đã nộp
        NEXT.put(REVISE, EnumSet.of(TODO, IN_PROGRESS, SUBMITTED, DONE));
        NEXT.put(REJECTED, EnumSet.of(TODO, IN_PROGRESS, SUBMITTED, DONE));
        NEXT.put(DONE, EnumSet.noneOf(TaskStatus.class));             // đã nghiệm thu: khóa vĩnh viễn
    }

    /**
     * Đọc chuỗi trong DB / request. Không phân biệt hoa thường; null hoặc lạ → null.
     * "APPROVED" là giá trị cũ của Task (trước khi thống nhất) và được coi là DONE.
     */
    public static TaskStatus of(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase();
        if ("APPROVED".equals(s)) return DONE;
        for (TaskStatus t : values()) {
            if (t.name().equals(s)) return t;
        }
        return null;
    }

    /** {@code TaskStatus.DONE.is(task.getStatus())} thay cho {@code "DONE".equalsIgnoreCase(...)}. */
    public boolean is(String raw) {
        return of(raw) == this;
    }

    /** Task đã hoàn thành (kể cả dữ liệu cũ ghi "APPROVED"). */
    public static boolean isDone(String raw) {
        return of(raw) == DONE;
    }

    /** Còn ở giai đoạn lập kế hoạch: chưa được PM khóa phạm vi (TODO hoặc PLANNING). */
    public static boolean isBeforePlanLock(String raw) {
        TaskStatus s = of(raw);
        return s == TODO || s == PLANNING;
    }

    /** Được thêm việc con: khi đang lập kế hoạch (TODO) hoặc đang làm (phát sinh việc). */
    public static boolean allowsNewSubTasks(String raw) {
        TaskStatus s = of(raw);
        return s == TODO || s == IN_PROGRESS;
    }

    /** Cạnh {@code this → target} có trong quy trình không. Đứng yên (cùng trạng thái) luôn hợp lệ. */
    public boolean canMoveTo(TaskStatus target) {
        return target != null && (target == this || NEXT.get(this).contains(target));
    }

    /** Như {@link #canMoveTo(TaskStatus)} nhưng nhận chuỗi; trạng thái lạ → false. Task chưa có trạng thái coi như TODO. */
    public static boolean canMove(String fromRaw, String toRaw) {
        TaskStatus from = of(fromRaw);
        if (from == null && (fromRaw == null || fromRaw.trim().isEmpty())) from = TODO;
        return from != null && from.canMoveTo(of(toRaw));
    }

    /** Các trạng thái kế tiếp hợp lệ (chỉ đọc). */
    public Set<TaskStatus> next() {
        return Collections.unmodifiableSet(NEXT.get(this));
    }
}
