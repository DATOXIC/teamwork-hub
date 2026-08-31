package com.teamwork.data;

import com.teamwork.business.Notification;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý Thông báo Hệ thống kết nối Supabase PostgreSQL.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sắp xếp created_at DESC để thông báo mới nhất luôn ở trên đầu
 * - Sử dụng PreparedStatement an toàn chống SQL Injection
 * - Quản lý tài nguyên bằng try-with-resources
 */
public class NotificationDB {

    private static final Logger LOGGER = Logger.getLogger(NotificationDB.class.getName());

    private static Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        return new Notification(
            rs.getInt("id"),
            rs.getInt("recipient_id"),
            rs.getString("title"),
            rs.getString("content"),
            rs.getString("link"),
            rs.getString("type"),
            rs.getBoolean("is_read"),
            rs.getString("created_at_str")
        );
    }

    /**
     * Hàm 1: Phát tín hiệu gửi thông báo nhanh
     */
    public static void send(int recipientId, String title, String content, String link, String type) {
        if (recipientId <= 0 || title == null || title.trim().isEmpty()) {
            return;
        }

        String sql = "INSERT INTO notifications (recipient_id, title, content, link, type, is_read, created_at) " +
                     "VALUES (?, ?, ?, ?, ?::notification_type_enum, FALSE, NOW())";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, recipientId);
            ps.setString(2, title.trim());
            ps.setString(3, content != null ? content.trim() : "");
            ps.setString(4, link != null && !link.trim().isEmpty() ? link.trim() : "#");
            ps.setString(5, type != null && !type.trim().isEmpty() ? type.trim().toUpperCase() : "GENERAL");

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi gửi thông báo cho User ID: " + recipientId, e);
        }
    }

    /**
     * Hàm 2: Lấy danh sách tất cả thông báo của một người dùng (Mới nhất nằm ở trên đầu)
     */
    public static List<Notification> selectByRecipientId(int recipientId) {
        List<Notification> result = new ArrayList<>();
        if (recipientId <= 0) return result;

        String sql = "SELECT id, recipient_id, title, content, link, type::text AS type, is_read, " +
                     "       to_char(created_at, 'DD/MM/YYYY HH24:MI') AS created_at_str " +
                     "FROM notifications " +
                     "WHERE recipient_id = ? " +
                     "ORDER BY created_at DESC, id DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, recipientId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy thông báo của User ID: " + recipientId, e);
        }
        return result;
    }

    /**
     * Hàm 3: Đếm số lượng thông báo CHƯA ĐỌC của một người dùng (cho Quả chuông 🔴 Header)
     */
    public static int countUnread(int recipientId) {
        if (recipientId <= 0) return 0;

        String sql = "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND is_read = FALSE";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, recipientId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi đếm thông báo chưa đọc", e);
        }
        return 0;
    }

    /**
     * Hàm 4: Đánh dấu một thông báo cụ thể là Đã Đọc
     */
    public static void markAsRead(int notificationId) {
        if (notificationId <= 0) return;

        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi markAsRead Notification ID: " + notificationId, e);
        }
    }

    /**
     * Hàm 5: Đánh dấu TẤT CẢ thông báo của người dùng là Đã Đọc
     */
    public static void markAllAsRead(int recipientId) {
        if (recipientId <= 0) return;

        String sql = "UPDATE notifications SET is_read = TRUE WHERE recipient_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, recipientId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi markAllAsRead User ID: " + recipientId, e);
        }
    }

    /**
     * Hàm 6: Xóa một thông báo
     */
    public static boolean delete(int notificationId) {
        if (notificationId <= 0) return false;

        String sql = "DELETE FROM notifications WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, notificationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa Notification ID: " + notificationId, e);
        }
        return false;
    }
}
