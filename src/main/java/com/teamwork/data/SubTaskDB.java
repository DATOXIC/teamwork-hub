package com.teamwork.data;

import com.teamwork.business.SubTask;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tầng Data Layer: Quản lý kho dữ liệu Việc Con & Vòng Đời Nghiệm Thu 5 Cấp Độ (In-Memory RAM Data Layer).
 * Cung cấp các thao tác:
 * - Lấy danh sách việc con theo Task cha
 * - Cấp dưới Nộp kết quả (submitDeliverable) ➔ 🟡 SUBMITTED
 * - Task Lead Duyệt Đạt (approveDeliverable) ➔ 🟢 APPROVED
 * - Task Lead Yêu cầu Cân chỉnh nhỏ (reviseDeliverable) ➔ 🔵 REVISE
 * - Task Lead Trả về do Chưa đạt (rejectDeliverable) ➔ 🔴 REJECTED
 * - Tự động tính toán % tiến độ hoàn thành dựa trên số lượng việc ĐÃ DUYỆT (APPROVED)
 * - Đảm bảo an toàn đa luồng (Thread-Safe)
 */
public class SubTaskDB {

    // 1. Danh sách tĩnh luồng an toàn lưu trữ toàn bộ các việc con trên RAM
    private static List<SubTask> subTasks = new CopyOnWriteArrayList<>();
    private static int nextId = 1; // Biến tự tăng cấp ID cho việc con mới

    // 2. Khối khởi tạo tĩnh (Static Initializer): Tạo sẵn các việc con mẫu đa dạng 5 trạng thái
    static {
        LocalDate today = LocalDate.now();
        String datePlus2 = today.plusDays(2).toString();
        String datePlus3 = today.plusDays(3).toString();
        String datePlus4 = today.plusDays(4).toString();
        String datePlus5 = today.plusDays(5).toString();
        String dateMinus1 = today.minusDays(1).toString();
        String dateMinus2 = today.minusDays(2).toString();
        String dateMinus7 = today.minusDays(7).toString();
        String dateMinus8 = today.minusDays(8).toString();

        // --- CÁC VIỆC CON CỦA TASK 1: "Thiết kế Cơ sở Dữ liệu & Model JavaBean" (Trạng thái PLANNING - Task cha hạn còn 5 ngày) ---
        subTasks.add(new SubTask(
            nextId++,
            1, // taskId = 1
            "Thiết kế sơ đồ quan hệ ERD và các bảng User, Task, Doc, Message",
            1, // assigneeId = 1 (Trưởng Nhóm Admin)
            "Trưởng Nhóm Admin",
            "TODO",
            datePlus2, // Hạn chót: Còn 2 ngày
            "",
            "",
            "",
            ""
        ));

        subTasks.add(new SubTask(
            nextId++,
            1,
            "Viết các JavaBean Model kế thừa Serializable và đầy đủ getter/setter",
            2, // assigneeId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An",
            "TODO",
            datePlus3, // Hạn chót: Còn 3 ngày
            "",
            "",
            "",
            ""
        ));

        subTasks.add(new SubTask(
            nextId++,
            1,
            "Xây dựng kho dữ liệu RAM Data Layer với các hàm truy vấn CRUD",
            2, // assigneeId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An",
            "TODO",
            datePlus4, // Hạn chót: Còn 4 ngày
            "",
            "",
            "",
            ""
        ));

        // --- CÁC VIỆC CON CỦA TASK 2: "Tích hợp cổng thanh toán trực tuyến" (Tiến độ: 100% - SUBMITTED ĐÃ BÀN GIAO CHO PM) ---
        subTasks.add(new SubTask(
            nextId++,
            2, // taskId = 2
            "Thiết kế giao diện Form thanh toán VNPAY Responsive",
            2, // assigneeId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An",
            "APPROVED",
            dateMinus1,
            "Đã hoàn thiện giao diện chọn cổng thanh toán và quét mã QR VNPAY",
            "Giao diện chuẩn UI Bootstrap, màu sắc hài hòa",
            dateMinus1 + " 14:00",
            dateMinus1 + " 14:30"
        ));

        subTasks.add(new SubTask(
            nextId++,
            2,
            "Viết Servlet xử lý IPN Callback và mã hóa Checksum SHA-256",
            2,
            "Nguyễn Văn An",
            "APPROVED",
            dateMinus1,
            "Đã viết Servlet kiểm tra chữ ký số SHA-256 và cập nhật đơn hàng",
            "Mã nguồn bảo mật tốt, xử lý bắt ngoại lệ đầy đủ",
            dateMinus1 + " 16:30",
            dateMinus1 + " 17:00"
        ));

        subTasks.add(new SubTask(
            nextId++,
            2,
            "Kiểm thử tự động 10 ca thanh toán trên môi trường Sandbox VNPAY",
            2,
            "Nguyễn Văn An",
            "APPROVED",
            today.toString(),
            "Đã chạy thử 10/10 ca test thẻ test, quét QR thành công 100%",
            "Nghiệm thu đạt 100%, sẵn sàng bàn giao cho PM",
            today.toString() + " 10:00",
            today.toString() + " 10:30"
        ));

        // --- CÁC VIỆC CON CỦA TASK 4: "Xây dựng giao diện Landing Page với Bootstrap 5" (Tiến độ: 100% - DONE ĐÃ DUYỆT 5 SAO) ---
        subTasks.add(new SubTask(
            nextId++,
            4, // taskId = 4
            "Thiết kế Hero Section và Navigation Bar",
            2,
            "Nguyễn Văn An",
            "APPROVED",
            dateMinus8,
            "Đã hoàn thành header cố định và banner động",
            "Đạt chuẩn",
            dateMinus8 + " 14:00",
            dateMinus8 + " 14:30"
        ));

        subTasks.add(new SubTask(
            nextId++,
            4,
            "Xây dựng phần Bảng Giá & Chân Trang Footer",
            2,
            "Nguyễn Văn An",
            "APPROVED",
            dateMinus7,
            "Đã hoàn thành bảng giá 3 gói dịch vụ và footer bản quyền",
            "Đạt chuẩn",
            dateMinus7 + " 15:00",
            dateMinus7 + " 15:30"
        ));

        // --- CÁC VIỆC CON CỦA TASK 3: "Xây dựng Filter bảo mật & Kiểm tra quyền truy cập" (Trạng thái TODO - Quá hạn) ---
        subTasks.add(new SubTask(
            nextId++,
            3, // taskId = 3
            "Cấu hình UrlPattern cho AuthFilter & Chặn truy cập trực tiếp",
            2, // assigneeId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An",
            "TODO",
            dateMinus2,
            "",
            "",
            "",
            ""
        ));

        subTasks.add(new SubTask(
            nextId++,
            3,
            "Viết unit test kiểm thử phân quyền và chuyển hướng đăng nhập",
            2,
            "Nguyễn Văn An",
            "TODO",
            dateMinus2,
            "",
            "",
            "",
            ""
        ));

        // --- CÁC VIỆC CON CỦA TASK 5: "Thiết kế giao diện Dark Mode & Tối ưu Responsive" (Trạng thái TODO - Còn 10 ngày) ---
        subTasks.add(new SubTask(
            nextId++,
            5, // taskId = 5
            "Nghiên cứu bảng màu Dark Palette chuẩn độ tương phản WCAG 2.1",
            3, // assigneeId = 3 (Trần Thị Bình)
            "Trần Thị Bình",
            "TODO",
            datePlus4,
            "",
            "",
            "",
            ""
        ));

        subTasks.add(new SubTask(
            nextId++,
            5,
            "Viết CSS Custom Properties và JavaScript lưu trạng thái theme",
            3,
            "Trần Thị Bình",
            "TODO",
            datePlus4,
            "",
            "",
            "",
            ""
        ));
    }

    /**
     * HÀM 1: Lấy danh sách tất cả các việc con của MỘT TASK CHA CỤ THỂ
     */
    public static List<SubTask> selectByTaskId(int taskId) {
        List<SubTask> resultList = new ArrayList<>();
        for (SubTask st : subTasks) {
            if (st.getTaskId() == taskId) {
                resultList.add(st);
            }
        }
        return resultList;
    }

    /**
     * HÀM 2: Tìm một việc con theo ID
     */
    public static SubTask selectById(int id) {
        for (SubTask st : subTasks) {
            if (st.getId() == id) {
                return st;
            }
        }
        return null;
    }

    /**
     * HÀM 3: Thêm một việc con mới và phân công cho thành viên
     */
    public static int insert(SubTask subTask) {
        subTask.setId(nextId++);
        if (subTask.getStatus() == null || subTask.getStatus().trim().isEmpty()) {
            subTask.setStatus("TODO");
        }
        subTasks.add(subTask);
        return subTask.getId();
    }

    /**
     * HÀM 4: Cấp dưới Nộp Báo Cáo Kết Quả ➔ Chuyển sang 🟡 SUBMITTED
     */
    public static boolean submitDeliverable(int subTaskId, String submissionNote, String submittedAt) {
        SubTask st = selectById(subTaskId);
        if (st != null) {
            st.setStatus("SUBMITTED");
            st.setSubmissionNote(submissionNote != null ? submissionNote.trim() : "");
            st.setSubmittedAt(submittedAt);
            return true;
        }
        return false;
    }

    /**
     * HÀM 5: Task Lead Duyệt Nghiệm Thu Đạt ➔ Chuyển sang 🟢 APPROVED (Đã xong 100%)
     */
    public static boolean approveDeliverable(int subTaskId, String reviewedAt) {
        SubTask st = selectById(subTaskId);
        if (st != null) {
            st.setStatus("APPROVED");
            st.setCompleted(true);
            st.setReviewedAt(reviewedAt);
            return true;
        }
        return false;
    }

    /**
     * HÀM 6: Task Lead Yêu Cầu Cân Chỉnh Nhỏ ➔ Chuyển sang 🔵 REVISE (Màu Xanh Dương)
     */
    public static boolean reviseDeliverable(int subTaskId, String feedbackNote, String reviewedAt) {
        SubTask st = selectById(subTaskId);
        if (st != null) {
            st.setStatus("REVISE");
            st.setFeedbackNote(feedbackNote != null ? feedbackNote.trim() : "");
            st.setReviewedAt(reviewedAt);
            return true;
        }
        return false;
    }

    /**
     * HÀM 7: Task Lead Trả Về Do Chưa Đạt Yêu Cầu ➔ Chuyển sang 🔴 REJECTED (Màu Đỏ)
     */
    public static boolean rejectDeliverable(int subTaskId, String feedbackNote, String reviewedAt) {
        SubTask st = selectById(subTaskId);
        if (st != null) {
            st.setStatus("REJECTED");
            st.setFeedbackNote(feedbackNote != null ? feedbackNote.trim() : "");
            st.setReviewedAt(reviewedAt);
            return true;
        }
        return false;
    }

    /**
     * HÀM 8: Cập nhật trạng thái hoàn thành trực tiếp (tương thích ngược)
     */
    public static boolean updateStatus(int id, boolean completed) {
        SubTask st = selectById(id);
        if (st != null) {
            st.setStatus(completed ? "APPROVED" : "TODO");
            return true;
        }
        return false;
    }

    /**
     * HÀM 9: Xóa một việc con theo ID
     */
    public static boolean delete(int id) {
        return subTasks.removeIf(st -> st.getId() == id);
    }

    /**
     * HÀM 10: Xóa toàn bộ các việc con thuộc một Task cha (Cascade Delete)
     */
    public static void deleteByTaskId(int taskId) {
        subTasks.removeIf(st -> st.getTaskId() == taskId);
    }

    /**
     * HÀM 11: Tính toán % tiến độ hoàn thành dựa trên các việc con ĐÃ DUYỆT (APPROVED)
     */
    public static int calculateProgress(int taskId) {
        List<SubTask> list = selectByTaskId(taskId);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int approvedCount = 0;
        for (SubTask st : list) {
            if ("APPROVED".equalsIgnoreCase(st.getStatus())) {
                approvedCount++;
            }
        }
        return (int) Math.round(((double) approvedCount / list.size()) * 100);
    }

    /**
     * HÀM 12: Cập nhật thông tin chi tiết của một việc con (tiêu đề, người phụ trách, hạn chót)
     */
    public static boolean update(SubTask updatedSubTask) {
        if (updatedSubTask == null) return false;
        for (int i = 0; i < subTasks.size(); i++) {
            if (subTasks.get(i).getId() == updatedSubTask.getId()) {
                subTasks.set(i, updatedSubTask);
                return true;
            }
        }
        return false;
    }
}
