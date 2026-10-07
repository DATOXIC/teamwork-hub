package com.teamwork.business;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Trạng thái của Nhiệm vụ (Sub-task) và BẢNG CHUYỂN TRẠNG THÁI hợp lệ.
 *
 * <pre>
 *  Có duyệt (Quality Gate):  TODO ──tick/nộp──▶ SUBMITTED ──Task Lead──▶ APPROVED (khóa)
 *                                                   │ trả sửa / từ chối
 *                                                   ▼
 *                                            REVISE / REJECTED ──nộp lại──▶ SUBMITTED
 *  Không duyệt (Fast-track):  TODO ◀──tick / bỏ tick──▶ DONE
 * </pre>
 *
 * APPROVED là kết quả nghiệm thu của Task Lead nên KHÔNG được quay lui bằng cách bỏ tick.
 */
public enum SubTaskStatus {
    TODO("Cần làm"),
    SUBMITTED("Chờ nghiệm thu"),
    REVISE("Cần chỉnh sửa"),
    REJECTED("Chưa đạt yêu cầu"),
    DONE("Hoàn thành"),
    APPROVED("Đã nghiệm thu");

    private final String label;

    SubTaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    private static final Map<SubTaskStatus, Set<SubTaskStatus>> NEXT = new EnumMap<>(SubTaskStatus.class);

    static {
        NEXT.put(TODO, EnumSet.of(SUBMITTED, DONE));
        NEXT.put(SUBMITTED, EnumSet.of(APPROVED, REVISE, REJECTED, TODO)); // TODO = người làm rút lại bài nộp
        NEXT.put(REVISE, EnumSet.of(SUBMITTED, DONE, TODO));
        NEXT.put(REJECTED, EnumSet.of(SUBMITTED, DONE, TODO));
        NEXT.put(DONE, EnumSet.of(TODO, SUBMITTED));   // bỏ tick; hoặc công việc cha vừa bật duyệt
        NEXT.put(APPROVED, EnumSet.noneOf(SubTaskStatus.class));
    }

    /** Đọc chuỗi trong DB / request. Không phân biệt hoa thường; null hoặc lạ → null. */
    public static SubTaskStatus of(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase();
        for (SubTaskStatus t : values()) {
            if (t.name().equals(s)) return t;
        }
        return null;
    }

    public boolean is(String raw) {
        return of(raw) == this;
    }

    /** Đã xong và được tính vào tiến độ: DONE (không duyệt) hoặc APPROVED (đã nghiệm thu). */
    public static boolean isFinished(String raw) {
        SubTaskStatus s = of(raw);
        return s == DONE || s == APPROVED;
    }

    public boolean canMoveTo(SubTaskStatus target) {
        return target != null && (target == this || NEXT.get(this).contains(target));
    }

    /** Như {@link #canMoveTo} nhưng nhận chuỗi; trạng thái lạ → false. Chưa có trạng thái coi như TODO. */
    public static boolean canMove(String fromRaw, String toRaw) {
        SubTaskStatus from = of(fromRaw);
        if (from == null && (fromRaw == null || fromRaw.trim().isEmpty())) from = TODO;
        return from != null && from.canMoveTo(of(toRaw));
    }

    public Set<SubTaskStatus> next() {
        return Collections.unmodifiableSet(NEXT.get(this));
    }
}
