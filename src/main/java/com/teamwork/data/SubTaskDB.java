package com.teamwork.data;

import com.teamwork.business.SubTask;
import java.util.ArrayList;
import java.util.List;

/**
 * Tầng Data Layer: Quản lý kho dữ liệu Việc Con & Vòng Đời Nghiệm Thu 5 Cấp Độ (In-Memory RAM Data Layer).
 * Cung cấp các thao tác:
 * - Lấy danh sách việc con theo Task cha
 * - Cấp dưới Nộp kết quả (submitDeliverable) ➔ 🟡 SUBMITTED
 * - Task Lead Duyệt Đạt (approveDeliverable) ➔ 🟢 APPROVED
 * - Task Lead Yêu cầu Cân chỉnh nhỏ (reviseDeliverable) ➔ 🔵 REVISE
 * - Task Lead Trả về do Chưa đạt (rejectDeliverable) ➔ 🔴 REJECTED
 * - Tự động tính toán % tiến độ hoàn thành dựa trên số lượng việc ĐÃ DUYỆT (APPROVED)
 */
public class SubTaskDB {

    // 1. Danh sách tĩnh lưu trữ toàn bộ các việc con trên RAM
    private static List<SubTask> subTasks = new ArrayList<>();
    private static int nextId = 1; // Biến tự tăng cấp ID cho việc con mới

    // 2. Khối khởi tạo tĩnh (Static Initializer): Tạo sẵn các việc con mẫu đa dạng 5 trạng thái
    static {
        // --- CÁC VIỆC CON CỦA TASK 1: "Thiết kế Cơ sở Dữ liệu & Model JavaBean" ---
        // Việc 1: 🟢 ĐÃ NGHIỆM THU
        subTasks.add(new SubTask(
            nextId++,
            1, // taskId = 1
            "Thiết kế sơ đồ quan hệ ERD và các bảng User, Task, Doc, Message",
            1, // assigneeId = 1 (Trưởng Nhóm Admin)
            "Trưởng Nhóm Admin",
            "APPROVED",
            "Đã vẽ xong sơ đồ trên draw.io và export file PNG",
            "Rất tốt, sơ đồ chuẩn quan hệ 1-N và N-N",
            "27/08/2026 18:00",
            "27/08/2026 18:30"
        ));

        // Việc 2: 🟡 ĐANG CHỜ DUYỆT (Nguyễn Văn An vừa nộp bài)
        subTasks.add(new SubTask(
            nextId++,
            1,
            "Viết các JavaBean Model kế thừa Serializable và đầy đủ getter/setter",
            2, // assigneeId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An",
            "SUBMITTED",
            "Đã hoàn thành toàn bộ các Model User, Task, SubTask, ProjectMember chuẩn JavaBean",
            "",
            "27/08/2026 19:30",
            ""
        ));

        // Việc 3: 🔵 CẦN CÂN CHỈNH (Task Lead dặn dò chỉnh thêm vài chi tiết nhỏ)
        subTasks.add(new SubTask(
            nextId++,
            1,
            "Xây dựng kho dữ liệu RAM Data Layer với các hàm truy vấn CRUD",
            2, // assigneeId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An",
            "REVISE",
            "Đã viết xong các hàm select, insert, update",
            "Cơ bản đã rất tốt, em bổ sung thêm chú thích tiếng Việt cho hàm calculateProgress nhé!",
            "27/08/2026 19:40",
            "27/08/2026 19:50"
        ));

        // --- CÁC VIỆC CON CỦA TASK 3: "Triển khai ứng dụng lên Cloud Render.com" ---
        subTasks.add(new SubTask(
            nextId++,
            3, // taskId = 3
            "Viết cấu hình Dockerfile multi-stage build và tối ưu file WAR",
            1, // assigneeId = 1 (Trưởng Nhóm Admin)
            "Trưởng Nhóm Admin",
            "APPROVED",
            "Build Docker image chạy thành công trên máy ảo",
            "Đạt chuẩn",
            "27/08/2026 18:00",
            "27/08/2026 18:15"
        ));

        subTasks.add(new SubTask(
            nextId++,
            3,
            "Cấu hình Web Service và thiết lập biến môi trường trên Render.com",
            2, // assigneeId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An",
            "TODO",
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
}
