package com.teamwork.data;

import com.teamwork.business.ProjectInvite;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý Lời Mời & Yêu Cầu Xin Gia Nhập 2 Chiều kết nối Supabase PostgreSQL.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng JOIN bảng projects, users để lấy đầy đủ tên dự án, mã dự án, tên người gửi, tên người nhận
 * - Sử dụng PreparedStatement an toàn chống SQL Injection
 * - Quản lý tài nguyên bằng try-with-resources
 */
public class ProjectInviteDB {

    private static final Logger LOGGER = Logger.getLogger(ProjectInviteDB.class.getName());

    private static final String BASE_SELECT_SQL =
        "SELECT pi.id, pi.project_id, p.name AS project_name, p.project_code, " +
        "       pi.type::text AS type, pi.sender_id, u_sender.full_name AS sender_name, " +
        "       pi.receiver_id, u_receiver.full_name AS receiver_name, " +
        "       pi.status::text AS status, " +
        "       to_char(pi.created_at, 'DD/MM/YYYY HH24:MI') AS created_at_str, " +
        "       to_char(pi.expired_at, 'DD/MM/YYYY HH24:MI') AS expired_at_str " +
        "FROM project_invites pi " +
        "JOIN projects p ON pi.project_id = p.id " +
        "JOIN users u_sender ON pi.sender_id = u_sender.id " +
        "JOIN users u_receiver ON pi.receiver_id = u_receiver.id ";

    private static ProjectInvite mapResultSetToProjectInvite(ResultSet rs) throws SQLException {
        return new ProjectInvite(
            rs.getInt("id"),
            rs.getInt("project_id"),
            rs.getString("project_name"),
            rs.getString("project_code"),
            rs.getString("type"),
            rs.getInt("sender_id"),
            rs.getString("sender_name"),
            rs.getInt("receiver_id"),
            rs.getString("receiver_name"),
            rs.getString("status"),
            rs.getString("created_at_str"),
            rs.getString("expired_at_str")
        );
    }

    /**
     * Hàm 1: Thêm Lời mời / Yêu cầu mới
     */
    public static int insert(ProjectInvite invite) {
        if (invite == null || invite.getProjectId() <= 0 || invite.getSenderId() <= 0 || invite.getReceiverId() <= 0) {
            return 0;
        }

        String sql = "INSERT INTO project_invites (project_id, type, sender_id, receiver_id, status, created_at, expired_at) " +
                     "VALUES (?, ?::invite_type_enum, ?, ?, ?::invite_status_enum, NOW(), NOW() + INTERVAL '7 days') RETURNING id";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, invite.getProjectId());
            String type = invite.getType() != null && invite.getType().equalsIgnoreCase("JOIN_REQUEST") ? "JOIN_REQUEST" : "INVITATION";
            ps.setString(2, type);
            ps.setInt(3, invite.getSenderId());
            ps.setInt(4, invite.getReceiverId());
            String status = invite.getStatus() != null && !invite.getStatus().trim().isEmpty() ? invite.getStatus().trim().toUpperCase() : "PENDING";
            ps.setString(5, status);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int genId = rs.getInt(1);
                    invite.setId(genId);
                    return genId;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi chèn ProjectInvite", e);
        }
        return 0;
    }

    /**
     * Hàm 2: Tìm Lời mời theo ID duy nhất
     */
    public static ProjectInvite selectById(int id) {
        if (id <= 0) return null;

        String sql = BASE_SELECT_SQL + "WHERE pi.id = ? LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProjectInvite(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm ProjectInvite ID: " + id, e);
        }
        return null;
    }

    /**
     * Hàm 3: Lấy danh sách Lời mời / Yêu cầu đang PENDING mà người dùng này CẦN DUYỆT
     */
    public static List<ProjectInvite> selectPendingByReceiverId(int receiverId) {
        List<ProjectInvite> result = new ArrayList<>();
        if (receiverId <= 0) return result;

        String sql = BASE_SELECT_SQL + "WHERE pi.receiver_id = ? AND pi.status = 'PENDING' AND pi.expired_at > NOW() ORDER BY pi.id DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, receiverId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToProjectInvite(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy pending invites của User ID: " + receiverId, e);
        }
        return result;
    }

    /**
     * Hàm 4: Lấy danh sách tất cả Lời mời / Yêu cầu của một Dự Án cụ thể
     */
    public static List<ProjectInvite> selectByProjectId(int projectId) {
        List<ProjectInvite> result = new ArrayList<>();
        if (projectId <= 0) return result;

        String sql = BASE_SELECT_SQL + "WHERE pi.project_id = ? ORDER BY pi.id DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToProjectInvite(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy invites theo Project ID: " + projectId, e);
        }
        return result;
    }

    /**
     * Hàm 5: Kiểm tra xem đã có Lời mời / Yêu cầu PENDING giữa 2 người trong dự án chưa (2 chiều)
     */
    public static boolean hasPendingInvite(int projectId, int user1Id, int user2Id) {
        if (projectId <= 0 || user1Id <= 0 || user2Id <= 0) return false;

        String sql = "SELECT 1 FROM project_invites " +
                     "WHERE project_id = ? AND status = 'PENDING' AND expired_at > NOW() " +
                     "  AND ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)) " +
                     "LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);
            ps.setInt(2, user1Id);
            ps.setInt(3, user2Id);
            ps.setInt(4, user2Id);
            ps.setInt(5, user1Id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi kiểm tra pending invite", e);
        }
        return false;
    }

    /**
     * Hàm 6: Cập nhật trạng thái Lời mời ("ACCEPTED", "REJECTED", "REVOKED", "EXPIRED")
     */
    public static void updateStatus(int id, String newStatus) {
        if (id <= 0 || newStatus == null || newStatus.trim().isEmpty()) return;

        String sql = "UPDATE project_invites SET status = ?::invite_status_enum WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus.trim().toUpperCase());
            ps.setInt(2, id);

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật trạng thái Invite ID: " + id, e);
        }
    }

    /**
     * Hàm 7: Xóa lời mời
     */
    public static boolean delete(int id) {
        if (id <= 0) return false;

        String sql = "DELETE FROM project_invites WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa Invite ID: " + id, e);
        }
        return false;
    }
}
