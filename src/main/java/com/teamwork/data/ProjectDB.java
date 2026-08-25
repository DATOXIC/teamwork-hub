package com.teamwork.data;

import com.teamwork.business.Project;
import java.util.ArrayList;
import java.util.List;

/**
 * Tầng Data Layer: Quản lý kho dữ liệu Dự án (In-Memory Database trên RAM)
 */
public class ProjectDB {

    // 1. Danh sách tĩnh lưu trữ các dự án trên RAM
    private static List<Project> projects = new ArrayList<>();
    private static int nextId = 1; // Biến tự tăng để cấp ID cho dự án mới

    // 2. Khối khởi tạo tĩnh (Static Initializer): Chạy 1 lần duy nhất khi nạp Class
    // Tạo sẵn 2 dự án mẫu để kiểm thử ngay tiến độ % hoàn thành
    static 
    {
        // Dự án mẫu 1: Đã làm được 5/8 tasks (~63%)
        projects.add(new Project(
            nextId++,
            "Website E-Commerce TeamWork",
            "Nền tảng mua sắm trực tuyến tích hợp thanh toán và quản lý đơn hàng.",
            1,            // ownerId = 1 (Trưởng nhóm Admin)
            "2026-08-01", // Ngày tạo
            8,            // totalTasks (Tổng số việc: 8)
            5             // doneTasks (Đã xong: 5 việc)
        ));

        // Dự án mẫu 2: Đã làm được 1/5 tasks (20%)
        projects.add(new Project(
            nextId++,
            "Mobile App Quản Lý Công Việc",
            "Ứng dụng di động đa nền tảng Flutter kết nối RESTful API.",
            1,            // ownerId = 1
            "2026-08-15",
            5,            // totalTasks: 5
            1             // doneTasks: 1
        ));
    }

    /**
     * Hàm 1: Lấy toàn bộ danh sách dự án
     * Dùng khi mở trang Dashboard (UC02)
     */
    public static List<Project> selectAll()
    {
        // Trả về bản sao ArrayList để đảm bảo an toàn dữ liệu
        return new ArrayList<>(projects);
    }

    /**
     * Hàm 2: Tìm dự án theo ID duy nhất
     * Dùng khi bấm vào xem chi tiết một dự án để vào bảng Kanban (UC04)
     */
    public static Project selectById(int id) 
    {
        for (Project p : projects) 
        {
            if (p.getId() == id) return p;
        }
        return null; // Không tìm thấy
    }

    /**
     * Hàm 3: Thêm dự án mới
     * Dùng khi người dùng bấm "+ Tạo dự án mới" (UC03)
     */
    public static int insert(Project project) 
    {
        project.setId(nextId++); // Cấp ID tự động tăng
        projects.add(project);   // Cất vào danh sách trên RAM
        return project.getId();  // Trả về ID vừa tạo
    }
}