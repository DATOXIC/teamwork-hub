package com.teamwork.data;

import com.teamwork.business.SubTask;
import java.util.ArrayList;
import java.util.List;

/**
 * Tầng Data Layer: Quản lý kho dữ liệu Việc Con (In-Memory SubTask Database trên RAM).
 * Cung cấp các thao tác:
 * - Lấy danh sách việc con theo Task cha
 * - Thêm việc con mới và phân công cho thành viên
 * - Đổi trạng thái hoàn thành [☑]
 * - Tự động tính toán % tiến độ hoàn thành của Task cha
 * - Dọn dẹp việc con khi Task cha bị xóa (Cascade Delete)
 */
public class SubTaskDB {

    // 1. Danh sách tĩnh lưu trữ toàn bộ các việc con trên RAM
    private static List<SubTask> subTasks = new ArrayList<>();
    private static int nextId = 1; // Biến tự tăng cấp ID cho việc con mới

    // 2. Khối khởi tạo tĩnh (Static Initializer): Tạo sẵn các việc con mẫu (Seed Data)
    static {
        // --- CÁC VIỆC CON CỦA TASK 1: "Thiết kế Cơ sở Dữ liệu & Model JavaBean" ---
        subTasks.add(new SubTask(
            nextId++,
            1, // taskId = 1
            "Thiết kế sơ đồ quan hệ ERD và các bảng User, Task, Doc, Message",
            1, // assigneeId = 1 (Trưởng Nhóm Admin)
            "Trưởng Nhóm Admin",
            true // Đã hoàn thành
        ));

        subTasks.add(new SubTask(
            nextId++,
            1,
            "Viết các JavaBean Model kế thừa Serializable và đầy đủ getter/setter",
            2, // assigneeId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An",
            true // Đã hoàn thành
        ));

        subTasks.add(new SubTask(
            nextId++,
            1,
            "Xây dựng kho dữ liệu RAM Data Layer với các hàm truy vấn CRUD",
            2, // assigneeId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An",
            false // Đang làm
        ));

        // --- CÁC VIỆC CON CỦA TASK 3: "Triển khai ứng dụng lên Cloud Render.com" ---
        subTasks.add(new SubTask(
            nextId++,
            3, // taskId = 3
            "Viết cấu hình Dockerfile multi-stage build và tối ưu file WAR",
            1, // assigneeId = 1 (Trưởng Nhóm Admin)
            "Trưởng Nhóm Admin",
            true // Đã hoàn thành
        ));

        subTasks.add(new SubTask(
            nextId++,
            3,
            "Cấu hình Web Service và thiết lập biến môi trường trên Render.com",
            2, // assigneeId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An",
            false // Đang làm
        ));
    }

    /**
     * HÀM 1: Lấy danh sách tất cả các việc con của MỘT TASK CHA CỤ THỂ
     * Dùng để hiển thị danh sách checklist trong Modal Task Mini-Hub.
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
        subTask.setId(nextId++); // Cấp ID tự động tăng
        subTasks.add(subTask);   // Cất vào danh sách trên RAM
        return subTask.getId();  // Trả về ID vừa tạo
    }

    /**
     * HÀM 4: Cập nhật trạng thái hoàn thành [☑] của một việc con
     */
    public static boolean updateStatus(int id, boolean completed) {
        SubTask st = selectById(id);
        if (st != null) {
            st.setCompleted(completed);
            return true;
        }
        return false;
    }

    /**
     * HÀM 5: Xóa một việc con theo ID
     */
    public static boolean delete(int id) {
        for (int i = 0; i < subTasks.size(); i++) {
            SubTask st = subTasks.get(i);
            if (st.getId() == id) {
                subTasks.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * HÀM 6: Xóa toàn bộ việc con khi Task cha bị xóa (Bảo toàn tính toàn vẹn dữ liệu)
     */
    public static void deleteByTaskId(int taskId) {
        for (int i = subTasks.size() - 1; i >= 0; i--) {
            SubTask st = subTasks.get(i);
            if (st.getTaskId() == taskId) {
                subTasks.remove(i);
            }
        }
    }

    /**
     * HÀM 7: Tự động tính toán % tiến độ hoàn thành của Task cha dựa trên các việc con
     * Ví dụ: Có 3 việc con, 2 việc đã xong -> Trả về 67 (%)
     */
    public static int calculateProgress(int taskId) {
        List<SubTask> taskSubTasks = selectByTaskId(taskId);
        if (taskSubTasks.isEmpty()) {
            return 0;
        }

        int completedCount = 0;
        for (SubTask st : taskSubTasks) {
            if (st.isCompleted()) {
                completedCount++;
            }
        }

        return (int) Math.round(((double) completedCount / taskSubTasks.size()) * 100);
    }
}
