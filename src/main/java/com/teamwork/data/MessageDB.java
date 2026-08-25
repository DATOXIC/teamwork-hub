package com.teamwork.data;

import com.teamwork.business.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * Tầng Data Layer: Quản lý kho dữ liệu Tin nhắn & Bình luận (In-Memory Message Database trên RAM).
 * Cung cấp các thao tác CRUD cho:
 * - Kênh Chat chung của Dự án (khi taskId == 0)
 * - Luồng Bình luận theo từng Công việc (khi taskId > 0)
 */
public class MessageDB {

    // 1. Danh sách tĩnh lưu trữ toàn bộ tin nhắn trong hệ thống trên RAM
    private static List<Message> messages = new ArrayList<>();
    private static int nextId = 1; // Biến tự tăng cấp ID cho tin nhắn mới

    // 2. Khối khởi tạo tĩnh (Static Initializer): Tạo sẵn các tin nhắn mẫu (Seed Data)
    // chứa cả #doc-X, #task-X và @username để kiểm thử ngay bộ máy Mention
    static {
        // --- CÁC TIN NHẮN CHAT CHUNG CHO DỰ ÁN 1 (projectId = 1, taskId = 0) ---
        
        // Tin nhắn 1: Admin nhắc nhở tài liệu
        messages.add(new Message(
            nextId++,
            1, // projectId = 1
            0, // taskId = 0 (Chat chung dự án)
            1, // authorId = 1
            "Trưởng Nhóm Admin",
            "Chào cả nhóm! Mọi người vui lòng đọc kỹ tài liệu #doc-1 trước khi bắt đầu nhận việc nhé. @NguyenVanAn hãy chú ý kỹ quy tắc phân tầng MVC.",
            "23/08/2026 09:30"
        ));

        // Tin nhắn 2: Nguyễn Văn An phản hồi
        messages.add(new Message(
            nextId++,
            1,
            0,
            2, // authorId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An",
            "Em đã đọc xong #doc-1 rồi ạ! Hiện em đang hoàn thành #task-1 và thấy cấu trúc rất rõ ràng.",
            "23/08/2026 10:15"
        ));

        // Tin nhắn 3: Admin hướng dẫn tiếp theo
        messages.add(new Message(
            nextId++,
            1,
            0,
            1,
            "Trưởng Nhóm Admin",
            "Tốt lắm! Sau khi xong #task-1, nhớ đọc tiếp #doc-3 để chuẩn bị triển khai lên Render nhé.",
            "24/08/2026 14:00"
        ));

        // --- BÌNH LUẬN RIÊNG CHO TASK 3 TRONG DỰ ÁN 1 (projectId = 1, taskId = 3) ---
        messages.add(new Message(
            nextId++,
            1,
            3, // taskId = 3 (Bình luận của Task 3)
            1,
            "Trưởng Nhóm Admin",
            "Task này cần lưu ý cấu hình đúng port 8080 và biến môi trường theo hướng dẫn trong #doc-3 nhé!",
            "24/08/2026 15:30"
        ));

        // --- TIN NHẮN CHAT CHO DỰ ÁN 2 (projectId = 2, taskId = 0) ---
        messages.add(new Message(
            nextId++,
            2,
            0,
            1,
            "Trưởng Nhóm Admin",
            "Chào mừng đến với dự án Mobile App! Mọi người xem qua thiết kế giao diện tại #doc-4 nhé.",
            "25/08/2026 08:30"
        ));
    }

    /**
     * HÀM 1: Lấy danh sách tin nhắn CHAT CHUNG của MỘT DỰ ÁN (taskId == 0)
     * Dùng để đổ dữ liệu vào dòng thời gian tin nhắn trong giao diện chat.jsp.
     */
    public static List<Message> selectByProjectId(int projectId) {
        List<Message> resultList = new ArrayList<>();
        for (Message m : messages) {
            // Chỉ lấy tin nhắn thuộc dự án này VÀ có taskId == 0 (chat chung)
            if (m.getProjectId() == projectId && m.getTaskId() == 0) {
                resultList.add(m);
            }
        }
        return resultList;
    }

    /**
     * HÀM 2: Lấy danh sách BÌNH LUẬN của MỘT CÔNG VIỆC CỤ THỂ (taskId > 0)
     * Dùng để hiển thị luồng bình luận trong Modal chi tiết của Task đó.
     */
    public static List<Message> selectByTaskId(int taskId) {
        List<Message> resultList = new ArrayList<>();
        for (Message m : messages) {
            if (m.getTaskId() == taskId) {
                resultList.add(m);
            }
        }
        return resultList;
    }

    /**
     * HÀM 3: Tìm một tin nhắn cụ thể theo ID
     */
    public static Message selectById(int id) {
        for (Message m : messages) {
            if (m.getId() == id) {
                return m;
            }
        }
        return null;
    }

    /**
     * HÀM 4: Thêm một tin nhắn mới vào kho dữ liệu trên RAM
     * Dùng khi người dùng gửi tin nhắn trong kênh chat hoặc gửi bình luận trong Task.
     */
    public static int insert(Message message) {
        message.setId(nextId++); // Cấp ID tự động tăng
        messages.add(message);   // Cất vào danh sách trên RAM
        return message.getId();  // Trả về ID vừa tạo
    }

    /**
     * HÀM 5: Đếm tổng số thảo luận (cả chat chung và bình luận) của một Dự án
     * Dùng để hiển thị thống kê "💬 X thảo luận" trên Card dự án ở trang Dashboard.
     */
    public static int countByProject(int projectId) {
        int count = 0;
        for (Message m : messages) {
            if (m.getProjectId() == projectId) {
                count = count + 1;
            }
        }
        return count;
    }

    /**
     * HÀM 6: Xóa một tin nhắn theo ID
     */
    public static boolean delete(int id) {
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            if (m.getId() == id) {
                messages.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * HÀM 7: Xóa toàn bộ bình luận của một Task khi Task đó bị xóa
     */
    public static void deleteByTaskId(int taskId) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message m = messages.get(i);
            if (m.getTaskId() == taskId) {
                messages.remove(i);
            }
        }
    }
}
