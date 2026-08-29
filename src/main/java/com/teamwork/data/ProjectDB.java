package com.teamwork.data;

import com.teamwork.business.Project;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tầng Data Layer: Quản lý kho dữ liệu Dự án (In-Memory Database trên RAM)
 * - Cung cấp hàm tìm kiếm theo Mã Dự Án (selectByCode) phục vụ luồng Xin Gia Nhập (Chiều 2)
 * - Đảm bảo an toàn đa luồng (Thread-Safe)
 */
public class ProjectDB {

    // 1. Danh sách tĩnh luồng an toàn lưu trữ các dự án trên RAM
    private static List<Project> projects = new CopyOnWriteArrayList<>();
    private static int nextId = 1; // Biến tự tăng để cấp ID cho dự án mới

    // 2. Khối khởi tạo tĩnh (Static Initializer): Chạy 1 lần duy nhất khi nạp Class
    static {
        // Dự án mẫu 1: Mã "TW-HUB-01"
        projects.add(new Project(
            nextId++,
            "TW-HUB-01",
            "Website E-Commerce TeamWork",
            "Nền tảng mua sắm trực tuyến tích hợp thanh toán và quản lý đơn hàng.",
            1,            // ownerId = 1 (Trưởng nhóm Admin)
            "2026-08-01", // Ngày tạo
            8,            // totalTasks (Tổng số việc: 8)
            5             // doneTasks (Đã xong: 5 việc)
        ));

        // Dự án mẫu 2: Mã "ECOMMERCE-99"
        projects.add(new Project(
            nextId++,
            "ECOMMERCE-99",
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
     */
    public static List<Project> selectAll() 
    {
        return new ArrayList<>(projects);
    }

    /**
     * Hàm 2: Tìm dự án theo ID duy nhất
     */
    public static Project selectById(int id) {
        for (Project p : projects) {
            if (p.getId() == id) {
                return p;
            }
        }
        return null;
    }

    /**
     * Hàm 3: Tìm dự án theo Mã Dự Án (projectCode)
     * Phục vụ Chiều 2: Thành viên nhập mã dự án để gửi yêu cầu Xin Gia Nhập
     */
    public static Project selectByCode(String code) 
    {
        if (code == null || code.trim().isEmpty()) 
        {
            return null;
        }
        String cleanCode = code.trim().toUpperCase();
        for (Project p : projects) 
        {
            if (p.getProjectCode() != null && p.getProjectCode().equalsIgnoreCase(cleanCode)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Hàm 4: Thêm dự án mới (Tự động cấp ID và tự động sinh Mã Dự Án nếu chưa có)
     */
    public static int insert(Project project) 
    {
        project.setId(nextId++);
        if (project.getProjectCode() == null || project.getProjectCode().trim().isEmpty()) {
            project.setProjectCode("PRJ-" + String.format("%03d", project.getId()));
        }
        projects.add(project);
        return project.getId();
    }
}