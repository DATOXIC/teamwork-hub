package com.teamwork.data;

import com.teamwork.business.Notification;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tầng Data Layer: Quản lý Kho Thông Báo Toàn Hệ Thống (In-Memory RAM Data Layer).
 * - Cung cấp hàm phát tín hiệu thông báo nhanh NotificationDB.send(...)
 * - Cung cấp số lượng thông báo chưa đọc (countUnread) cho Quả chuông 🔴 trên Header
 * - Quản lý trạng thái Đã đọc / Chưa đọc
 * - Đảm bảo an toàn đa luồng (Thread-Safe)
 */
public class NotificationDB {

    // 1. Danh sách tĩnh luồng an toàn lưu trữ các thông báo trên RAM
    private static List<Notification> notifications = new CopyOnWriteArrayList<>();
    private static int nextId = 1;

    // 2. Khối khởi tạo tĩnh (Static Initializer): Seed Data mẫu ban đầu
    static {
        // Thông báo mẫu cho Admin (ID=1)
        notifications.add(new Notification(
            nextId++,
            1,
            "Hệ Thống Sẵn Sàng",
            "Hệ thống Quản lý Dự án & Mạng xã hội nhóm Teamwork Hub đã sẵn sàng hoạt động!",
            "/project?action=list",
            "GENERAL",
            false,
            "27/08/2026 18:00"
        ));

        // Thông báo mẫu cho Nguyễn Văn An (ID=2)
        notifications.add(new Notification(
            nextId++,
            2,
            "Giao Việc Mới",
            "Trưởng Nhóm Admin đã chỉ định bạn làm Task Lead của [Thiết kế CSDL & Model JavaBean].",
            "/task?action=list&projectId=1",
            "TASK_ASSIGNED",
            false,
            "27/08/2026 18:05"
        ));

        // Thông báo mẫu cho Lê Văn Chi (ID=4)
        notifications.add(new Notification(
            nextId++,
            4,
            "Lời Mời Dự Án",
            "Trưởng Nhóm Admin vừa gửi lời mời bạn tham gia vào dự án [Website E-Commerce TeamWork (TW-HUB-01)].",
            "/project?action=list",
            "INVITE",
            false,
            "27/08/2026 18:10"
        ));
    }

    /**
     * Hàm 1: Phát tín hiệu gửi thông báo nhanh (Tiện ích dùng ở khắp mọi nơi trong Controller)
     */
    public static void send(int recipientId, String title, String content, String link, String type) {
        if (recipientId > 0 && title != null && !title.trim().isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String now = LocalDateTime.now().format(formatter);
            
            Notification notif = new Notification(
                nextId++,
                recipientId,
                title.trim(),
                (content != null ? content.trim() : ""),
                (link != null && !link.trim().isEmpty() ? link.trim() : "#"),
                (type != null ? type.trim().toUpperCase() : "GENERAL"),
                false, // Mặc định là Chưa đọc
                now
            );
            notifications.add(notif);
        }
    }

    /**
     * Hàm 2: Lấy danh sách tất cả thông báo của một người dùng (Mới nhất nằm ở trên đầu)
     */
    public static List<Notification> selectByRecipientId(int recipientId) {
        List<Notification> result = new ArrayList<>();
        for (Notification n : notifications) {
            if (n.getRecipientId() == recipientId) {
                result.add(n);
            }
        }
        // Đảo ngược danh sách để thông báo mới nhất hiển thị trên cùng
        Collections.reverse(result);
        return result;
    }

    /**
     * Hàm 3: Đếm số lượng thông báo CHƯA ĐỌC của một người dùng
     * Phục vụ hiển thị số đỏ trên Quả chuông 🔴 Header
     */
    public static int countUnread(int recipientId) {
        int count = 0;
        for (Notification n : notifications) {
            if (n.getRecipientId() == recipientId && !n.isRead()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Hàm 4: Đánh dấu một thông báo cụ thể là Đã Đọc
     */
    public static void markAsRead(int notificationId) {
        for (Notification n : notifications) {
            if (n.getId() == notificationId) {
                n.setRead(true);
                break;
            }
        }
    }

    /**
     * Hàm 5: Đánh dấu TẤT CẢ thông báo của người dùng là Đã Đọc
     */
    public static void markAllAsRead(int recipientId) {
        for (Notification n : notifications) {
            if (n.getRecipientId() == recipientId) {
                n.setRead(true);
            }
        }
    }

    /**
     * Hàm 6: Xóa một thông báo
     */
    public static boolean delete(int notificationId) {
        return notifications.removeIf(n -> n.getId() == notificationId);
    }
}
