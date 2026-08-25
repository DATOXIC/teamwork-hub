package com.teamwork.data;

import com.teamwork.business.Task;
import java.util.ArrayList;
import java.util.List;

/**
 * Tầng Data Layer: Quản lý kho dữ liệu Thẻ công việc (In-Memory Task Database trên RAM)
 * Cung cấp đầy đủ các thao tác CRUD: Tạo mới, Lấy danh sách theo 3 cột, Cập nhật trạng thái và Xóa task.
 */
public class TaskDB 
{

    // 1. Danh sách tĩnh lưu toàn bộ task trong hệ thống
    private static List<Task> tasks = new ArrayList<>();
    private static int nextId = 1; // Biến tự tăng cấp ID cho thẻ task mới

    // 2. Khối khởi tạo tĩnh (Static Initializer): Chạy 1 lần duy nhất khi Server nạp class này
    // Tạo sẵn các task mẫu cho Dự án 1 (Website E-Commerce) và Dự án 2 (Mobile App)
    static {
        // --- CÁC TASK MẪU CHO DỰ ÁN 1 (projectId = 1) ---
        
        // Cột 1: CẦN LÀM (TODO)
        tasks.add(new Task(
            nextId++,
            1, // projectId = 1
            "Thiết kế CSDL quan hệ cho Giỏ hàng & Đơn hàng",
            "Xây dựng lược đồ bảng Cart, CartItem, Order, OrderDetail và ràng buộc khóa ngoại.",
            "TODO",
            "HIGH",
            "2026-08-30",
            2, // assigneeId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An"
        ));

        tasks.add(new Task(
            nextId++,
            1,
            "Tích hợp cổng thanh toán trực tuyến",
            "Nghiên cứu tài liệu Sandbox và viết Servlet xử lý callback thanh toán.",
            "TODO",
            "MEDIUM",
            "2026-09-05",
            2,
            "Nguyễn Văn An"
        ));

        // Cột 2: ĐANG LÀM (IN_PROGRESS)
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

        // Cột 3: ĐÃ HOÀN THÀNH (DONE)
        tasks.add(new Task(
            nextId++,
            1,
            "Xây dựng giao diện Landing Page với Bootstrap 5",
            "Hoàn thiện trang chủ responsive, thanh điều hướng và chân trang chuẩn.",
            "DONE",
            "LOW",
            "2026-08-20",
            2,
            "Nguyễn Văn An"
        ));

        tasks.add(new Task(
            nextId++,
            1,
            "Khởi tạo cấu trúc dự án Maven & MVC Model 2",
            "Cấu hình file pom.xml, web.xml và tổ chức các package chuẩn.",
            "DONE",
            "MEDIUM",
            "2026-08-15",
            1,
            "Trưởng Nhóm Admin"
        ));

        // --- CÁC TASK MẪU CHO DỰ ÁN 2 (projectId = 2) ---
        tasks.add(new Task(
            nextId++,
            2, // projectId = 2
            "Phác thảo Wireframe giao diện Mobile App",
            "Thiết kế bố cục màn hình Kanban và màn hình Chat trên Figma.",
            "TODO",
            "HIGH",
            "2026-09-01",
            1,
            "Trưởng Nhóm Admin"
        ));

        tasks.add(new Task(
            nextId++,
            2,
            "Cấu hình môi trường Flutter & SDK 21",
            "Cài đặt Flutter framework và kiểm tra máy ảo Android Emulator.",
            "DONE",
            "LOW",
            "2026-08-18",
            2,
            "Nguyễn Văn An"
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
     * Dùng khi muốn tính tổng số lượng việc của dự án đó.
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
     * HÀM 3: Lấy danh sách task của một dự án ĐƯỢC LỌC THEO CỘT TRẠNG THÁI (TODO, IN_PROGRESS, DONE)
     * Đây là hàm quan trọng nhất phục vụ việc hiển thị 3 cột của Bảng Kanban.
     */
    public static List<Task> selectByProjectAndStatus(int projectId, String status) 
    {
        List<Task> resultList = new ArrayList<>();
        for (Task t : tasks) 
        {
            // Kiểm tra khớp cả projectId và status (không phân biệt chữ hoa/thường)
            if (t.getProjectId() == projectId) 
            {
                if (t.getStatus() != null && t.getStatus().equalsIgnoreCase(status)) 
                {
                    resultList.add(t);
                }
            }
        }
        return resultList;
    }

    /**
     * HÀM 4: Tìm một thẻ task duy nhất theo ID của nó
     */
    public static Task selectById(int id) 
    {
        for (Task t : tasks) 
        {
            if (t.getId() == id) 
            {
                return t;
            }
        }
        return null; // Không tìm thấy
    }

    /**
     * HÀM 5: Thêm một thẻ công việc mới vào dự án
     * Dùng khi người dùng submit form "+ Thêm công việc" (UC05)
     */
    public static int insert(Task task) 
    {
        task.setId(nextId++); // Cấp ID tự động tăng
        tasks.add(task);      // Cất vào danh sách trên RAM
        return task.getId();
    }

    /**
     * HÀM 6: Cập nhật trạng thái của Task (Ví dụ: Chuyển từ TODO sang IN_PROGRESS hoặc DONE)
     * Dùng khi người dùng KÉO THẢ chuột trên bảng Kanban (UC06)
     */
    public static boolean updateStatus(int taskId, String newStatus) 
    {
        for (Task t : tasks) 
        {
            if (t.getId() == taskId) 
            {
                t.setStatus(newStatus); // Cập nhật trạng thái mới
                return true;            // Cập nhật thành công
            }
        }
        return false; // Không tìm thấy task với id tương ứng
    }

    /**
     * HÀM 7: Xóa một thẻ công việc khỏi dự án
     * Dùng khi người dùng bấm biểu tượng thùng rác (UC07)
     */
    public static boolean delete(int taskId) 
    {
        for (int i = 0; i < tasks.size(); i++) 
        {
            Task t = tasks.get(i);
            if (t.getId() == taskId) 
            {
                tasks.remove(i); // Xóa khỏi danh sách
                return true;     // Xóa thành công
            }
        }
        return false; // Không tìm thấy để xóa
    }

    /**
     * HÀM 8: Đếm tổng số task của một dự án
     */
    public static int countTotalTasks(int projectId) 
    {
        int count = 0;
        for (Task t : tasks) {
            if (t.getProjectId() == projectId) 
            {
                count = count + 1;
            }
        }
        return count;
    }

    /**
     * HÀM 9: Đếm số task ĐÃ XONG (DONE) của một dự án
     */
    public static int countDoneTasks(int projectId) 
    {
        int count = 0;
        for (Task t : tasks) {
            if (t.getProjectId() == projectId) 
            {
                if (t.getStatus() != null && t.getStatus().equalsIgnoreCase("DONE")) 
                {
                    count = count + 1;
                }
            }
        }
        return count;
    }
}
