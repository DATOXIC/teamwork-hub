package com.teamwork.business;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JavaBean & JPA Entity đại diện cho một Tin nhắn Thảo luận (Message) hoặc Bình luận công việc (Comment).
 * - Khi taskId == null (hoặc 0): Là tin nhắn thảo luận chung trong kênh Chat của Dự án.
 * - Khi taskId > 0: Là bình luận chi tiết thuộc về một Công việc (Task) cụ thể.
 */
@Entity
@Table(name = "messages")
public class Message implements Serializable {

    // ===================== CÁC THUỘC TÍNH =====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;             // Khóa chính định danh tin nhắn

    @Column(name = "project_id", nullable = false)
    private int projectId;      // Thuộc dự án nào (Khóa ngoại trỏ đến Project.id)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @Column(name = "task_id")
    private Integer taskId;     // Thuộc task nào (null = Chat chung dự án, > 0 = Comment của task)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private Task task;

    @Column(name = "author_id")
    private Integer authorId;   // ID người gửi (Khóa ngoại trỏ đến User.id, null-safe)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", insertable = false, updatable = false)
    private User author;

    @Column(name = "author_name")
    private String authorName;  // Tên hiển thị người gửi (Lưu sẵn để JSP không phải join bảng)

    @Column(name = "content", nullable = false)
    private String content;     // Nội dung tin nhắn (Hỗ trợ #task-3, #doc-2, @username)

    @Column(name = "sent_at", insertable = false, updatable = false)
    private String sentAt;      // Thời điểm gửi tin nhắn (Định dạng: dd/MM/yyyy HH:mm)

    // ===================== CONSTRUCTOR MẶC ĐỊNH =====================

    /**
     * Constructor không tham số: Bắt buộc theo chuẩn JavaBean.
     */
    public Message() {
        this.id = 0;
        this.projectId = 0;
        this.taskId = null;   // null = chat chung (cột task_id có khóa ngoại, không được lưu số 0)
        this.authorId = null; // null = tin hệ thống (cột author_id có khóa ngoại, không được lưu số 0)
        this.authorName = "Ẩn danh";
        this.content = "";
        this.sentAt = "";
    }

    // ===================== CONSTRUCTOR ĐẦY ĐỦ THAM SỐ =====================

    /**
     * Constructor đầy đủ tham số: Dùng trong MessageDB để tạo nhanh tin nhắn mẫu (Seed Data)
     * hoặc tạo tin nhắn mới từ Controller.
     */
    public Message(int id, int projectId, int taskId, int authorId,
                   String authorName, String content, String sentAt) {
        this.id = id;
        this.projectId = projectId;
        this.taskId = taskId > 0 ? taskId : null;       // 0 -> NULL để không vi phạm khóa ngoại fk_messages_task
        this.authorId = authorId > 0 ? authorId : null; // 0 (tin hệ thống) -> NULL để không vi phạm fk_messages_author
        this.authorName = authorName;
        this.content = content;
        this.sentAt = sentAt;
    }

    // ===================== CÁC HÀM TIỆN ÍCH CHO GIAO DIỆN =====================

    /**
     * Kiểm tra xem tin nhắn này có phải là bình luận của một Task cụ thể hay không.
     */
    public boolean isTaskComment() {
        return getTaskId() > 0;
    }

    /**
     * Lấy ký tự chữ cái đầu tiên của tên tác giả để vẽ Avatar tròn nếu chưa có ảnh.
     * Ví dụ: "Nguyễn Văn An" -> "N", "Trưởng Nhóm Admin" -> "T".
     */
    public String getAuthorInitial() {
        if (this.authorName == null || this.authorName.trim().isEmpty()) {
            return "U";
        }
        return this.authorName.trim().substring(0, 1).toUpperCase();
    }

    // ===================== GETTERS & SETTERS =====================

    public int getId() {
        return this.id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return this.projectId;
    }
    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getTaskId() {
        return this.taskId != null ? this.taskId : 0;
    }
    public void setTaskId(int taskId) {
        this.taskId = taskId > 0 ? taskId : null;
    }
    public void setTaskId(Integer taskId) {
        this.taskId = (taskId != null && taskId > 0) ? taskId : null;
    }

    public Task getTask() {
        return this.task;
    }
    public void setTask(Task task) {
        this.task = task;
    }

    public Project getProject() {
        return this.project;
    }
    public void setProject(Project project) {
        this.project = project;
    }

    public int getAuthorId() {
        return this.authorId != null ? this.authorId : 0;
    }
    public void setAuthorId(int authorId) {
        this.authorId = authorId > 0 ? authorId : null;
    }
    public void setAuthorId(Integer authorId) {
        this.authorId = (authorId != null && authorId > 0) ? authorId : null;
    }

    public User getAuthor() {
        return this.author;
    }
    public void setAuthor(User author) {
        this.author = author;
    }

    public String getAuthorName() {
        return this.authorName;
    }
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getContent() {
        return this.content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getSentAt() {
        return this.sentAt;
    }
    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }

    private static final java.time.ZoneId VN_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * Chuyển đổi chuỗi sentAt thành ZonedDateTime theo múi giờ Việt Nam (Asia/Ho_Chi_Minh, UTC+7).
     * Tương thích với:
     * - Định dạng TIMESTAMPTZ của PostgreSQL (VD: 2026-10-07 13:05:49.482369+00)
     * - Định dạng ISO-8601 (VD: 2026-10-07T13:05:49Z)
     * - Định dạng ngày giờ Việt Nam (VD: 07/10/2026 20:05 hoặc 2026-10-07 20:05:00)
     */
    public java.time.ZonedDateTime getParsedSentAt() {
        if (this.sentAt == null || this.sentAt.trim().isEmpty()) {
            return null;
        }
        String s = this.sentAt.trim();
        try {
            // Pattern 1: ISO/Postgres: yyyy-MM-dd[ T]HH:mm[:ss]...(+00, Z, etc.)
            java.util.regex.Matcher mIso = java.util.regex.Pattern.compile(
                "^(\\d{4})-(\\d{2})-(\\d{2})[ T](\\d{2}):(\\d{2})(?::(\\d{2}))?(?:\\.\\d+)?([+-]\\d{2}(?::?\\d{2})?|Z)?"
            ).matcher(s);
            if (mIso.find()) {
                int year = Integer.parseInt(mIso.group(1));
                int month = Integer.parseInt(mIso.group(2));
                int day = Integer.parseInt(mIso.group(3));
                int hour = Integer.parseInt(mIso.group(4));
                int minute = Integer.parseInt(mIso.group(5));
                int second = mIso.group(6) != null ? Integer.parseInt(mIso.group(6)) : 0;
                String tzStr = mIso.group(7);

                if (tzStr != null && !tzStr.isEmpty()) {
                    java.time.ZoneOffset offset;
                    if ("Z".equalsIgnoreCase(tzStr)) {
                        offset = java.time.ZoneOffset.UTC;
                    } else {
                        String cleanTz = tzStr;
                        if (!cleanTz.contains(":") && cleanTz.length() == 3) {
                            cleanTz = cleanTz + ":00";
                        }
                        offset = java.time.ZoneOffset.of(cleanTz);
                    }
                    java.time.OffsetDateTime odt = java.time.OffsetDateTime.of(year, month, day, hour, minute, second, 0, offset);
                    return odt.atZoneSameInstant(VN_ZONE);
                } else {
                    return java.time.LocalDateTime.of(year, month, day, hour, minute, second).atZone(VN_ZONE);
                }
            }

            // Pattern 2: dd/MM/yyyy HH:mm[:ss]
            java.util.regex.Matcher mDmy = java.util.regex.Pattern.compile(
                "^(\\d{2})/(\\d{2})/(\\d{4})\\s+(\\d{2}):(\\d{2})(?::(\\d{2}))?"
            ).matcher(s);
            if (mDmy.find()) {
                int day = Integer.parseInt(mDmy.group(1));
                int month = Integer.parseInt(mDmy.group(2));
                int year = Integer.parseInt(mDmy.group(3));
                int hour = Integer.parseInt(mDmy.group(4));
                int minute = Integer.parseInt(mDmy.group(5));
                int second = mDmy.group(6) != null ? Integer.parseInt(mDmy.group(6)) : 0;
                return java.time.LocalDateTime.of(year, month, day, hour, minute, second).atZone(VN_ZONE);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Rút gọn chuỗi thời gian hiển thị tinh tế trên giao diện chat (chuẩn giờ Việt Nam UTC+7):
     * - Nếu trong ngày hôm nay: "HH:mm" (ví dụ: "20:05")
     * - Nếu là hôm qua: "Hôm qua HH:mm"
     * - Nếu khác ngày: "dd/MM HH:mm" (ví dụ: "07/10 20:05")
     */
    public String getShortSentAt() {
        java.time.ZonedDateTime zdt = getParsedSentAt();
        if (zdt != null) {
            java.time.LocalDate msgDate = zdt.toLocalDate();
            java.time.LocalDate today = java.time.LocalDate.now(VN_ZONE);
            String timeStr = String.format("%02d:%02d", zdt.getHour(), zdt.getMinute());

            if (msgDate.isEqual(today)) {
                return timeStr;
            } else if (msgDate.isEqual(today.minusDays(1))) {
                return "Hôm qua " + timeStr;
            } else {
                return String.format("%02d/%02d %s", zdt.getDayOfMonth(), zdt.getMonthValue(), timeStr);
            }
        }
        if (this.sentAt == null || this.sentAt.trim().isEmpty()) {
            return "";
        }
        return this.sentAt.trim();
    }

    /**
     * Chuỗi thời gian đầy đủ hiển thị cho tooltip / title (ví dụ: "20:05 - 07/10/2026")
     */
    public String getFormattedSentAt() {
        java.time.ZonedDateTime zdt = getParsedSentAt();
        if (zdt != null) {
            return String.format("%02d:%02d - %02d/%02d/%04d",
                zdt.getHour(), zdt.getMinute(), zdt.getDayOfMonth(), zdt.getMonthValue(), zdt.getYear());
        }
        return this.sentAt != null ? this.sentAt.trim() : "";
    }
}
