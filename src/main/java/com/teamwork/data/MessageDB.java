package com.teamwork.data;

import com.teamwork.business.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý Tin nhắn Chat & Bình luận Task kết nối Supabase PostgreSQL.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng PreparedStatement an toàn chống SQL Injection
 * - Quản lý tài nguyên bằng try-with-resources
 */
public class MessageDB {

    private static final Logger LOGGER = Logger.getLogger(MessageDB.class.getName());

    private static Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int projectId = rs.getInt("project_id");
        int taskId = rs.getInt("task_id"); // If NULL in DB, rs.getInt returns 0
        int authorId = rs.getInt("author_id");
        String authorName = rs.getString("author_name");
        String content = rs.getString("content");
        String sentAt = rs.getString("sent_at_str");

        return new Message(
            id,
            projectId,
            taskId,
            authorId,
            authorName != null ? authorName : "Ẩn danh",
            content != null ? content : "",
            sentAt != null ? sentAt : ""
        );
    }

    private static final String BASE_SELECT_SQL =
        "SELECT id, project_id, COALESCE(task_id, 0) AS task_id, author_id, author_name, content, " +
        "       to_char(sent_at, 'DD/MM/YYYY HH24:MI') AS sent_at_str " +
        "FROM messages ";

    /**
     * HÀM 1: Lấy danh sách tin nhắn CHAT CHUNG của MỘT DỰ ÁN (taskId == 0)
     */
    public static List<Message> selectByProjectId(int projectId) {
        List<Message> list = new ArrayList<>();
        if (projectId <= 0) return list;

        String sql = BASE_SELECT_SQL + "WHERE project_id = ? AND task_id IS NULL ORDER BY sent_at ASC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMessage(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy tin nhắn chat Project ID: " + projectId, e);
        }
        return list;
    }

    /**
     * HÀM 1b: Lấy danh sách N tin nhắn CHAT CHUNG gần đây nhất
     */
    public static List<Message> selectRecentByProjectId(int projectId, int limit) {
        List<Message> list = new ArrayList<>();
        if (projectId <= 0) return list;

        int safeLimit = limit > 0 ? limit : 50;
        String sql = "SELECT * FROM (" +
                     BASE_SELECT_SQL + "WHERE project_id = ? AND task_id IS NULL ORDER BY sent_at DESC LIMIT ?" +
                     ") sub ORDER BY sent_at_str ASC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);
            ps.setInt(2, safeLimit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMessage(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy tin nhắn recent Project ID: " + projectId, e);
        }
        return list;
    }

    /**
     * HÀM 2: Lấy danh sách BÌNH LUẬN của MỘT CÔNG VIỆC CỤ THỂ (taskId > 0)
     */
    public static List<Message> selectByTaskId(int taskId) {
        List<Message> list = new ArrayList<>();
        if (taskId <= 0) return list;

        String sql = BASE_SELECT_SQL + "WHERE task_id = ? ORDER BY sent_at ASC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMessage(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy bình luận Task ID: " + taskId, e);
        }
        return list;
    }

    /**
     * HÀM 3: Tìm một tin nhắn cụ thể theo ID
     */
    public static Message selectById(int id) {
        if (id <= 0) return null;

        String sql = BASE_SELECT_SQL + "WHERE id = ? LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMessage(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm tin nhắn ID: " + id, e);
        }
        return null;
    }

    /**
     * HÀM 4: Thêm một tin nhắn mới
     */
    public static int insert(Message message) {
        if (message == null || message.getContent() == null || message.getContent().trim().isEmpty()) {
            return 0;
        }

        String sql = "INSERT INTO messages (project_id, task_id, author_id, author_name, content, sent_at) " +
                     "VALUES (?, ?, ?, ?, ?, NOW()) RETURNING id";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, message.getProjectId());

            if (message.getTaskId() > 0) {
                ps.setInt(2, message.getTaskId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            if (message.getAuthorId() > 0) {
                ps.setInt(3, message.getAuthorId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }

            ps.setString(4, message.getAuthorName() != null ? message.getAuthorName().trim() : "Ẩn danh");
            ps.setString(5, message.getContent().trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int genId = rs.getInt(1);
                    message.setId(genId);
                    return genId;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi gửi tin nhắn", e);
        }
        return 0;
    }

    /**
     * HÀM 5: Đếm tổng số thảo luận của một Dự án
     */
    public static int countByProject(int projectId) {
        if (projectId <= 0) return 0;

        String sql = "SELECT COUNT(*) FROM messages WHERE project_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi đếm tin nhắn Project ID: " + projectId, e);
        }
        return 0;
    }

    /**
     * HÀM 6: Xóa một tin nhắn theo ID
     */
    public static boolean delete(int id) {
        if (id <= 0) return false;

        String sql = "DELETE FROM messages WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa tin nhắn ID: " + id, e);
        }
        return false;
    }

    /**
     * HÀM 7: Xóa toàn bộ bình luận của một Task khi Task bị xóa
     */
    public static void deleteByTaskId(int taskId) {
        if (taskId <= 0) return;

        String sql = "DELETE FROM messages WHERE task_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa bình luận theo Task ID: " + taskId, e);
        }
    }

    /**
     * HÀM BATCH MỚI: Lấy toàn bộ bình luận của TẤT CẢ các Task trong một Dự Án trong 1 câu SQL duy nhất!
     * Giúp loại bỏ N+1 query problem, tăng tốc độ tải trang gấp nhiều lần.
     */
    public static List<Message> selectTaskCommentsByProjectId(int projectId) {
        List<Message> list = new ArrayList<>();
        if (projectId <= 0) return list;

        String sql = BASE_SELECT_SQL + "WHERE project_id = ? AND task_id IS NOT NULL ORDER BY sent_at ASC, id ASC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMessage(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy batch Task Comments theo Project ID: " + projectId, e);
        }
        return list;
    }
}

