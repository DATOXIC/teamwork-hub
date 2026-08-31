package com.teamwork.data;

import com.teamwork.business.Task;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tầng Data Layer: Quản lý kho dữ liệu Thẻ công việc (In-Memory Task Database trên RAM)
 * - Quản lý 5 trạng thái: TODO, IN_PROGRESS, SUBMITTED (🟡), REVISE (🔵), REJECTED (🔴), DONE (🟢)
 * - Cung cấp các thao tác Bàn giao của Task Lead và Phê duyệt Nghiệm thu của Trưởng Dự Án (PM)
 * - Đảm bảo an toàn đa luồng (Thread-Safe)
 */
public class TaskDB {

    // 1. Danh sách tĩnh luồng an toàn lưu toàn bộ task trong hệ thống
    private static List<Task> tasks = new CopyOnWriteArrayList<>();
    private static int nextId = 1; // Biến tự tăng cấp ID cho thẻ task mới

    // 2. Khối khởi tạo tĩnh (Static Initializer): Tạo sẵn các task mẫu cho Dự án 1 và Dự án 2
    static {
        LocalDate today = LocalDate.now();
        String datePlus5 = today.plusDays(5).toString();   // Còn 5 ngày (🟢 Đúng tiến độ)
        String datePlus1 = today.plusDays(1).toString();   // Ngày mai (🟠 Sắp đến hạn)
        String dateMinus2 = today.minusDays(2).toString(); // Quá hạn 2 ngày (🔴 Quá hạn)
        String dateMinus7 = today.minusDays(7).toString(); // Đã xong trong quá khứ (✅ Đúng hạn)
        String datePlus10 = today.plusDays(10).toString(); // Còn 10 ngày (🟢 Đúng tiến độ)

        // --- CÁC TASK MẪU CHO DỰ ÁN 1 (projectId = 1) ---
        
        // Task 1: 🟣 CHỜ PM DUYỆT KẾ HOẠCH PHÂN RÃ (Cổng 1 - Còn 5 ngày)
        Task t1 = new Task(
            nextId++,
            1, // projectId = 1
            "Thiết kế CSDL quan hệ & Model JavaBean",
            "Xây dựng toàn bộ sơ đồ ERD, các bảng quan hệ và các lớp JavaBean Model chuẩn Serializable.",
            "PLANNING",
            "HIGH",
            datePlus5,
            2, // assigneeId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An",
            "",
            "",
            "27/08/2026 19:30",
            "",
            "",
            5,
            "Đã phân rã đầy đủ 3 việc con cốt lõi (1 sơ đồ ERD, 1 JavaBean Models, 1 RAM Data Layer CRUD). Đội ngũ sẵn sàng bắt tay thực hiện ngay khi PM duyệt khóa kế hoạch!",
            ""
        );
        t1.setLabels("BACKEND,FEATURE");
        tasks.add(t1);

        // Task 2: 🟡 ĐÃ HOÀN THÀNH 100% VIỆC CON & ĐÃ BÀN GIAO CHO PM (Chờ PM duyệt nghiệm thu Cổng 3 - Hạn chót ngày mai)
        Task t2 = new Task(
            nextId++,
            1,
            "Tích hợp cổng thanh toán trực tuyến",
            "Nghiên cứu tài liệu Sandbox và viết Servlet xử lý callback thanh toán VNPAY.",
            "SUBMITTED",
            "HIGH",
            datePlus1,
            2,
            "Nguyễn Văn An",
            "📌 [Tóm tắt kết quả]: Đã hoàn thiện 100% module thanh toán VNPAY, tích hợp mã QR động và thanh toán thẻ ATM nội địa.\n\n🌐 [Link Demo/Sản phẩm]: https://demo.teamworkhub.vn/payment-vnpay\n\n💻 [Link Mã nguồn/PR]: https://github.com/teamwork-hub/teamwork-platform/pull/24\n\n🧪 [Kết quả kiểm thử]: Đã kiểm thử thành công 10/10 ca giao dịch sandbox VNPAY (Tỷ lệ pass 100%).\n\n🧭 [Hướng dẫn PM nghiệm thu]: PM dùng thẻ test 9704198526191432152, ngày phát hành 07/15, OTP 123456 để thử giao dịch.",
            "",
            "27/08/2026 21:00",
            "",
            "Bao_Cao_Nghiem_Thu_Thanh_Toan_VNPAY.pdf",
            5,
            "Đã phân rã 3 việc con và được PM phê duyệt khóa kế hoạch.",
            "25/08/2026 09:00"
        );
        t2.setLabels("BACKEND,FEATURE,URGENT");
        tasks.add(t2);

        // Task 3: ⚪ CẦN LÀM (TODO) - Minh họa trạng thái QUÁ HẠN 2 NGÀY (🔴)
        Task t3 = new Task(
            nextId++,
            1,
            "Xây dựng Filter bảo mật & Kiểm tra quyền truy cập",
            "Chặn người dùng chưa đăng nhập truy cập trực tiếp vào các URL nội bộ.",
            "TODO",
            "HIGH",
            dateMinus2,
            2, // assigneeId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An"
        );
        t3.setLabels("BUG,BACKEND");
        tasks.add(t3);

        // Task 4: 🟢 ĐÃ HOÀN THÀNH (DONE) - ĐÃ QUA ĐỦ 3 CỔNG, ĐƯỢC PM DUYỆT ĐẠT 5 SAO ⭐⭐⭐⭐⭐
        Task t4 = new Task(
            nextId++,
            1,
            "Xây dựng giao diện Landing Page với Bootstrap 5",
            "Hoàn thiện trang chủ responsive, thanh điều hướng và chân trang chuẩn.",
            "DONE",
            "LOW",
            dateMinus7,
            2,
            "Nguyễn Văn An",
            "📌 [Tóm tắt kết quả]: Đã hoàn thiện Landing Page chuẩn responsive mobile & desktop.\n\n🌐 [Link Demo/Sản phẩm]: https://teamworkhub.vn/home\n\n💻 [Link Mã nguồn/PR]: https://github.com/teamwork-hub/teamwork-platform/pull/12",
            "PM phê duyệt: Giao diện đạt chuẩn UX/UI Basecamp, tốc độ tải trang cực nhanh! Đạt xuất sắc 5 sao.",
            dateMinus7 + " 15:00",
            dateMinus7 + " 16:30",
            "Bien_Ban_Ban_Giao_Landing_Page.pdf",
            5,
            "Đã phân rã 2 việc con.",
            "18/08/2026 08:30"
        );
        t4.setLabels("UI,FEATURE");
        tasks.add(t4);

        // Task 5: ⚪ CẦN LÀM (TODO) - Còn 10 ngày (Giao cho Trần Thị Bình để demo luồng Cổng 1)
        Task t5 = new Task(
            nextId++,
            1,
            "Thiết kế giao diện Dark Mode & Tối ưu Responsive",
            "Nghiên cứu bảng màu Dark Palette, thiết kế chuyển đổi theme và tối ưu UX trên thiết bị di động.",
            "TODO",
            "MEDIUM",
            datePlus10,
            3,
            "Trần Thị Bình"
        );
        t5.setLabels("UI,DOCS");
        tasks.add(t5);
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
     * - TODO: Task có trạng thái TODO (Đang lập kế hoạch)
     * - IN_PROGRESS: Task có trạng thái PLANNING (🟣), IN_PROGRESS (🚀), SUBMITTED (🟡), REVISE (🔵), REJECTED (🔴)
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
                    if ("PLANNING".equalsIgnoreCase(t.getStatus()) ||
                        "IN_PROGRESS".equalsIgnoreCase(t.getStatus()) ||
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
     * HÀM 7: Task Lead Bàn Giao & Nộp Báo Cáo Task Lớn Kèm Tệp Đính Kèm ➔ Chuyển sang 🟡 SUBMITTED
     */
    public static boolean submitTaskDeliverable(int taskId, String note, String deliverableFile, String submittedAt) {
        Task t = selectById(taskId);
        if (t != null) {
            t.setStatus("SUBMITTED");
            t.setFinalDeliverableNote(note != null ? note.trim() : "");
            t.setDeliverableFile(deliverableFile != null ? deliverableFile.trim() : "");
            t.setSubmittedAt(submittedAt);
            return true;
        }
        return false;
    }

    public static boolean submitTaskDeliverable(int taskId, String note, String submittedAt) {
        return submitTaskDeliverable(taskId, note, "", submittedAt);
    }

    /**
     * HÀM 8: Trưởng Dự Án (PM) Phê Duyệt Nghiệm Thu ĐẠT Kèm Đánh Giá Sao ➔ Chuyển sang 🟢 DONE (100%)
     */
    public static boolean pmApproveTask(int taskId, String feedback, int qualityRating, String reviewedAt) {
        Task t = selectById(taskId);
        if (t != null) {
            t.setStatus("DONE");
            t.setPmFeedback(feedback != null ? feedback.trim() : "PM đã phê duyệt nghiệm thu xuất sắc!");
            t.setQualityRating(qualityRating > 0 ? qualityRating : 5);
            t.setReviewedAt(reviewedAt);
            return true;
        }
        return false;
    }

    public static boolean pmApproveTask(int taskId, String feedback, String reviewedAt) {
        return pmApproveTask(taskId, feedback, 5, reviewedAt);
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
     * HÀM 13: Task Lead Trình Kế Hoạch Phân Rã Sub-Tasks Cho PM Thẩm Định (CỔNG 1) ➔ Chuyển sang 🟣 PLANNING
     */
    public static boolean submitPlanningRequest(int taskId, String planningNote, String submittedAt) {
        Task t = selectById(taskId);
        if (t != null) {
            t.setStatus("PLANNING");
            t.setPlanningNote(planningNote != null ? planningNote.trim() : "");
            t.setSubmittedAt(submittedAt);
            return true;
        }
        return false;
    }

    /**
     * HÀM 14: Trưởng Dự Án (PM) Phê Duyệt Kế Hoạch & KHÓA PHÂN RÃ (SCOPE LOCK - CỔNG 1) ➔ Chuyển sang 🚀 IN_PROGRESS
     */
    public static boolean pmApprovePlanning(int taskId, String pmFeedback, String reviewedAt) {
        Task t = selectById(taskId);
        if (t != null) {
            t.setStatus("IN_PROGRESS");
            t.setPlanningReviewedAt(reviewedAt);
            if (pmFeedback != null && !pmFeedback.trim().isEmpty()) {
                t.setPmFeedback(pmFeedback.trim());
            }
            return true;
        }
        return false;
    }

    /**
     * HÀM 15: Trưởng Dự Án (PM) Yêu Cầu Task Lead Bổ Sung / Chỉnh Sửa Kế Hoạch Phân Rã ➔ Trả về ⚪ TODO
     */
    public static boolean pmRejectPlanning(int taskId, String pmFeedback, String reviewedAt) {
        Task t = selectById(taskId);
        if (t != null) {
            t.setStatus("TODO");
            t.setPmFeedback(pmFeedback != null ? pmFeedback.trim() : "");
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

    /**
     * HÀM 13: Hủy phân công Task lớn cho một thành viên khi rời nhóm hoặc bị kick
     */
    public static void unassignUserFromProject(int projectId, int userId) {
        for (Task t : tasks) {
            if (t.getProjectId() == projectId && t.getAssigneeId() == userId) {
                t.setAssigneeId(0);
                t.setAssigneeName("Chưa phân công");
            }
        }
    }

    /**
     * HÀM 14: Đồng bộ tên người phụ trách mới sang toàn bộ các Task lớn
     */
    public static void syncAssigneeName(int userId, String newFullName) {
        if (newFullName != null && !newFullName.trim().isEmpty()) {
            for (Task t : tasks) {
                if (t.getAssigneeId() == userId) {
                    t.setAssigneeName(newFullName.trim());
                }
            }
        }
    }
}
