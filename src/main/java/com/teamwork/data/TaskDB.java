package com.teamwork.data;

import com.teamwork.business.Task;
import java.util.ArrayList;
import java.util.List;

/**
 * Tầng Data Layer: Quản lý kho dữ liệu Thẻ công việc (In-Memory Task Database trên RAM)
 * - Quản lý 5 trạng thái: TODO, IN_PROGRESS, SUBMITTED (🟡), REVISE (🔵), REJECTED (🔴), DONE (🟢)
 * - Cung cấp các thao tác Bàn giao của Task Lead và Phê duyệt Nghiệm thu của Trưởng Dự Án (PM)
 */
public class TaskDB {

    // 1. Danh sách tĩnh lưu toàn bộ task trong hệ thống
    private static List<Task> tasks = new ArrayList<>();
    private static int nextId = 1; // Biến tự tăng cấp ID cho thẻ task mới

    // 2. Khối khởi tạo tĩnh (Static Initializer): Tạo sẵn các task mẫu cho Dự án 1 và Dự án 2
    static {
        // --- CÁC TASK MẪU CHO DỰ ÁN 1 (projectId = 1) ---
        
        // Task 1: 🚀 ĐANG LÀM (Task Lead An đang đôn đốc đội ngũ hoàn thiện 3 việc con)
        tasks.add(new Task(
            nextId++,
            1, // projectId = 1
            "Thiết kế CSDL quan hệ & Model JavaBean",
            "Xây dựng toàn bộ sơ đồ ERD, các bảng quan hệ và các lớp JavaBean Model chuẩn Serializable.",
            "IN_PROGRESS",
            "HIGH",
            "2026-08-30",
            2, // assigneeId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An"
        ));

        // Task 2: 🔵 PM YÊU CẦU CÂN CHỈNH (Màu Xanh Dương)
        tasks.add(new Task(
            nextId++,
            1,
            "Tích hợp cổng thanh toán trực tuyến",
            "Nghiên cứu tài liệu Sandbox và viết Servlet xử lý callback thanh toán.",
            "REVISE",
            "MEDIUM",
            "2026-09-05",
            2,
            "Nguyễn Văn An",
            "Đã viết xong Servlet tích hợp sandbox VNPAY",
            "Giao diện thanh toán rất đẹp, em bổ sung thêm log ghi vết mã giao dịch vào console nhé!",
            "27/08/2026 19:00",
            "27/08/2026 19:30"
        ));

        // Task 3: 🚀 ĐANG LÀM
        tasks.add(new Task(
            nextId++,
            1,
            "Xây dựng Filter bảo mật & Kiểm tra quyền truy cập",
            "Chặn người dùng chưa đăng nhập truy cập trực tiếp vào các URL nội bộ.",
            "IN_PROGRESS",
            "HIGH",
            "2026-08-28",
            1, // assigneeId = 1 (Admin)
            "Trưởng Nhóm Admin"
        ));

        // Task 4: 🟢 ĐÃ HOÀN THÀNH (DONE)
        tasks.add(new Task(
            nextId++,
            1,
            "Xây dựng giao diện Landing Page với Bootstrap 5",
            "Hoàn thiện trang chủ responsive, thanh điều hướng và chân trang chuẩn.",
            "DONE",
            "LOW",
            "2026-08-20",
            2,
            "Nguyễn Văn An",
            "Đã hoàn thành trang chủ chuẩn UX/UI Basecamp",
            "PM phê duyệt: Giao diện đạt chuẩn, chạy mượt mà trên mobile!",
            "20/08/2026 15:00",
            "20/08/2026 16:30"
        ));

        // Task 5: ⚪ CẦN LÀM (TODO)
        tasks.add(new Task(
            nextId++,
            1,
            "Viết tài liệu Hướng dẫn sử dụng & Báo cáo đồ án",
            "Soạn thảo tài liệu PDF và slide thuyết trình bảo vệ đồ án.",
            "TODO",
            "MEDIUM",
            "2026-09-10",
            1,
            "Trưởng Nhóm Admin"
        ));
    }

    /**
     * HÀM 1: Lấy toàn bộ task trong hệ thống
     */
    public static List<Task> selectAll() {
        return new ArrayList<>(tasks);
    }

    /**
     * HÀM 2: Lấy danh sách tất cả các task thuộc về MỘT DỰ ÁN cụ thể
     */
    public static List<Task> selectByProjectId(int projectId) {
        List<Task> resultList = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getProjectId() == projectId) {
                resultList.add(t);
            }
        }
        return resultList;
    }

    /**
     * HÀM 3: Lấy danh sách task của một dự án ĐƯỢC LỌC THEO 3 CỘT KANBAN:
     * - TODO: Task có trạng thái TODO
     * - IN_PROGRESS: Task có trạng thái IN_PROGRESS, SUBMITTED (🟡), REVISE (🔵), REJECTED (🔴)
     * - DONE: Task có trạng thái DONE hoặc APPROVED (🟢)
     */
    public static List<Task> selectByProjectAndStatus(int projectId, String status) {
        List<Task> resultList = new ArrayList<>();
        if (status == null) return resultList;

        for (Task t : tasks) {
            if (t.getProjectId() == projectId) {
                if ("TODO".equalsIgnoreCase(status) && "TODO".equalsIgnoreCase(t.getStatus())) {
                    resultList.add(t);
                } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                    if ("IN_PROGRESS".equalsIgnoreCase(t.getStatus()) ||
                        "SUBMITTED".equalsIgnoreCase(t.getStatus()) ||
                        "REVISE".equalsIgnoreCase(t.getStatus()) ||
                        "REJECTED".equalsIgnoreCase(t.getStatus())) {
                        resultList.add(t);
                    }
                } else if ("DONE".equalsIgnoreCase(status)) {
                    if ("DONE".equalsIgnoreCase(t.getStatus()) || "APPROVED".equalsIgnoreCase(t.getStatus())) {
                        resultList.add(t);
                    }
                }
            }
        }
        return resultList;
    }

    /**
     * HÀM 4: Tìm task theo ID
     */
    public static Task selectById(int id) {
        for (Task t : tasks) {
            if (t.getId() == id) {
                return t;
            }
        }
        return null;
    }

    /**
     * HÀM 5: Thêm task mới
     */
    public static int insert(Task task) {
        task.setId(nextId++);
        tasks.add(task);
        return task.getId();
    }

    /**
     * HÀM 6: Cập nhật trạng thái Task (kéo thả HTML5)
     */
    public static boolean updateStatus(int id, String newStatus) {
        Task t = selectById(id);
        if (t != null && newStatus != null) {
            t.setStatus(newStatus.trim().toUpperCase());
            return true;
        }
        return false;
    }

    /**
     * HÀM 7: Task Lead Bàn Giao & Nộp Báo Cáo Task Lớn ➔ Chuyển sang 🟡 SUBMITTED
     */
    public static boolean submitTaskDeliverable(int taskId, String note, String submittedAt) {
        Task t = selectById(taskId);
        if (t != null) {
            t.setStatus("SUBMITTED");
            t.setFinalDeliverableNote(note != null ? note.trim() : "");
            t.setSubmittedAt(submittedAt);
            return true;
        }
        return false;
    }

    /**
     * HÀM 8: Trưởng Dự Án (PM) Phê Duyệt Nghiệm Thu ĐẠT ➔ Chuyển sang 🟢 DONE (100%)
     */
    public static boolean pmApproveTask(int taskId, String feedback, String reviewedAt) {
        Task t = selectById(taskId);
        if (t != null) {
            t.setStatus("DONE");
            t.setPmFeedback(feedback != null ? feedback.trim() : "PM đã phê duyệt nghiệm thu xuất sắc!");
            t.setReviewedAt(reviewedAt);
            return true;
        }
        return false;
    }

    /**
     * HÀM 9: Trưởng Dự Án (PM) Yêu Cầu Cân Chỉnh Nhỏ ➔ Chuyển sang 🔵 REVISE (Màu Xanh Dương)
     */
    public static boolean pmReviseTask(int taskId, String feedback, String reviewedAt) {
        Task t = selectById(taskId);
        if (t != null) {
            t.setStatus("REVISE");
            t.setPmFeedback(feedback != null ? feedback.trim() : "");
            t.setReviewedAt(reviewedAt);
            return true;
        }
        return false;
    }

    /**
     * HÀM 10: Trưởng Dự Án (PM) Trả Về Do Chưa Đạt ➔ Chuyển sang 🔴 REJECTED (Màu Đỏ)
     */
    public static boolean pmRejectTask(int taskId, String feedback, String reviewedAt) {
        Task t = selectById(taskId);
        if (t != null) {
            t.setStatus("REJECTED");
            t.setPmFeedback(feedback != null ? feedback.trim() : "");
            t.setReviewedAt(reviewedAt);
            return true;
        }
        return false;
    }

    /**
     * HÀM 11: Cập nhật thông tin toàn diện của Task
     */
    public static boolean update(Task updatedTask) {
        if (updatedTask == null) return false;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId() == updatedTask.getId()) {
                tasks.set(i, updatedTask);
                return true;
            }
        }
        return false;
    }

    /**
     * HÀM 12: Xóa task theo ID
     */
    public static boolean delete(int id) {
        return tasks.removeIf(t -> t.getId() == id);
    }
}
