package com.teamwork.data;

import com.teamwork.business.TaskDoc;
import java.util.ArrayList;
import java.util.List;

/**
 * Tầng Data Layer: Quản lý kho dữ liệu liên kết giữa Công việc và Tài liệu (Task ↔ Doc) trên RAM.
 * Cung cấp các thao tác tìm kiếm, thêm mới, và tự động dọn dẹp liên kết khi Task hoặc Doc bị xóa.
 */
public class TaskDocDB {

    // 1. Danh sách tĩnh lưu trữ toàn bộ các cặp liên kết (Task - Doc) trên RAM
    private static List<TaskDoc> taskDocs = new ArrayList<>();

    // 2. Khối khởi tạo tĩnh (Static Initializer): Tạo sẵn các liên kết mẫu (Seed Data)
    static {
        // Task 1 (Thiết kế CSDL E-Commerce) 🔗 đính kèm Doc 1 (Quy chuẩn phát triển Web MVC Model 2)
        taskDocs.add(new TaskDoc(1, 1, "Quy chuẩn phát triển Web MVC Model 2"));

        // Task 3 (Triển khai Docker & Render) 🔗 đính kèm Doc 3 (Hướng dẫn đóng gói Docker & Triển khai lên Render.com)
        taskDocs.add(new TaskDoc(3, 3, "Hướng dẫn đóng gói Docker & Triển khai lên Render.com"));

        // Task 5 (Thiết kế giao diện Mobile App) 🔗 đính kèm Doc 4 (Tài liệu thiết kế giao diện Mobile App với Flutter)
        taskDocs.add(new TaskDoc(5, 4, "Tài liệu thiết kế giao diện Mobile App với Flutter"));
    }

    /**
     * HÀM 1: Lấy danh sách tất cả các tài liệu đính kèm của MỘT CÔNG VIỆC (Task)
     * Dùng để hiển thị danh sách tài liệu hướng dẫn trong Modal chi tiết Task và thẻ Kanban.
     */
    public static List<TaskDoc> selectByTaskId(int taskId) {
        List<TaskDoc> resultList = new ArrayList<>();
        for (TaskDoc td : taskDocs) {
            if (td.getTaskId() == taskId) {
                resultList.add(td);
            }
        }
        return resultList;
    }

    /**
     * HÀM 2: Lấy danh sách ID của các Task đang tham chiếu đến MỘT TÀI LIỆU (Doc)
     * Dùng để hiển thị mục "Các công việc đang dùng tài liệu này" ở cuối bài viết trong docs.jsp.
     */
    public static List<Integer> selectTaskIdsByDocId(int docId) {
        List<Integer> taskIds = new ArrayList<>();
        for (TaskDoc td : taskDocs) {
            if (td.getDocId() == docId) {
                taskIds.add(td.getTaskId());
            }
        }
        return taskIds;
    }

    /**
     * HÀM 3: Thêm một liên kết mới giữa Task và Doc
     * Kiểm tra tránh trùng lặp trước khi thêm vào danh sách RAM.
     */
    public static boolean insert(int taskId, int docId, String docTitle) {
        // Kiểm tra xem liên kết này đã tồn tại chưa
        for (TaskDoc td : taskDocs) {
            if (td.getTaskId() == taskId && td.getDocId() == docId) {
                return false; // Đã tồn tại, không thêm trùng
            }
        }

        // Tạo liên kết mới và thêm vào danh sách
        TaskDoc newLink = new TaskDoc(taskId, docId, docTitle);
        taskDocs.add(newLink);
        return true;
    }

    /**
     * HÀM 4: Xóa toàn bộ liên kết đính kèm của một Task
     * Dùng khi một Task bị xóa khỏi hệ thống (để không để lại dữ liệu rác trên RAM).
     */
    public static void deleteByTaskId(int taskId) {
        for (int i = taskDocs.size() - 1; i >= 0; i--) {
            TaskDoc td = taskDocs.get(i);
            if (td.getTaskId() == taskId) {
                taskDocs.remove(i);
            }
        }
    }

    /**
     * HÀM 5: Xóa toàn bộ liên kết của một Doc
     * Dùng khi một bài viết tài liệu bị xóa khỏi hệ thống.
     */
    public static void deleteByDocId(int docId) {
        for (int i = taskDocs.size() - 1; i >= 0; i--) {
            TaskDoc td = taskDocs.get(i);
            if (td.getDocId() == docId) {
                taskDocs.remove(i);
            }
        }
    }
}
