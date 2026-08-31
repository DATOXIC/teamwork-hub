package com.teamwork.data;

import com.teamwork.business.Project;
import com.teamwork.business.ProjectMember;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý Danh Sách Thành Viên của từng Dự Án kết nối Supabase.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng JOIN giữa bảng project_members và users để lấy đầy đủ thông tin tên, email, chuyên môn
 * - Sử dụng PreparedStatement an toàn chống SQL Injection
 * - Quản lý tài nguyên bằng try-with-resources
 */
public class ProjectMemberDB {

    private static final Logger LOGGER = Logger.getLogger(ProjectMemberDB.class.getName());

    /**
     * Hàm 1: Lấy toàn bộ danh sách thành viên của một dự án cụ thể
     * Phục vụ hiển thị Modal "Đội ngũ dự án (X/10)" và Form phân công công việc
     * 
     * @param projectId ID của dự án
     * @return Danh sách List<ProjectMember> chứa đầy đủ thông tin thành viên
     */
    public static List<ProjectMember> selectByProjectId(int projectId) {
        List<ProjectMember> result = new ArrayList<>();
        if (projectId <= 0) {
            return result;
        }

        String sql = "SELECT pm.project_id, pm.user_id, pm.project_role::text AS project_role, " +
                     "       to_char(pm.joined_at, 'YYYY-MM-DD HH24:MI') AS joined_at_str, " +
                     "       u.full_name AS user_name, u.email AS user_email, u.role AS user_role " +
                     "FROM project_members pm " +
                     "JOIN users u ON pm.user_id = u.id " +
                     "WHERE pm.project_id = ? " +
                     "ORDER BY pm.joined_at ASC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new ProjectMember(
                        rs.getInt("project_id"),
                        rs.getInt("user_id"),
                        rs.getString("user_name"),
                        rs.getString("user_email"),
                        rs.getString("user_role"),
                        rs.getString("project_role"),
                        rs.getString("joined_at_str")
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách thành viên Project ID: " + projectId, e);
        }
        return result;
    }

    /**
     * Hàm 2: Lấy tất cả các Dự án mà một người dùng đang tham gia
     * Phục vụ hiển thị Dashboard của người dùng đó
     * 
     * @param userId ID của người dùng
     * @return Danh sách List<Project> mà người dùng là OWNER hoặc MEMBER
     */
    public static List<Project> selectProjectsByUserId(int userId) {
        List<Project> result = new ArrayList<>();
        if (userId <= 0) {
            return result;
        }

        String sql = "SELECT p.id, p.project_code, p.name, p.description, p.owner_id, " +
                     "       to_char(p.created_at, 'YYYY-MM-DD') AS created_at_str, " +
                     "       COUNT(t.id) AS total_tasks, " +
                     "       COUNT(t.id) FILTER (WHERE t.status = 'DONE' OR t.status = 'APPROVED') AS done_tasks " +
                     "FROM projects p " +
                     "JOIN project_members pm ON p.id = pm.project_id " +
                     "LEFT JOIN tasks t ON p.id = t.project_id " +
                     "WHERE pm.user_id = ? " +
                     "GROUP BY p.id, p.project_code, p.name, p.description, p.owner_id, p.created_at " +
                     "ORDER BY p.id ASC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(ProjectDB.mapResultSetToProject(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách Project của User ID: " + userId, e);
        }
        return result;
    }

    /**
     * Hàm 3: Kiểm tra xem một người dùng đã là thành viên của dự án hay chưa
     * Phục vụ Ràng buộc 2: Chống mời trùng lặp người đã ở trong dự án
     * 
     * @param projectId ID của dự án
     * @param userId ID của người dùng
     * @return true nếu đã là thành viên, ngược lại false
     */
    public static boolean isMember(int projectId, int userId) {
        if (projectId <= 0 || userId <= 0) {
            return false;
        }

        String sql = "SELECT 1 FROM project_members WHERE project_id = ? AND user_id = ? LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi kiểm tra tư cách thành viên", e);
        }
        return false;
    }

    /**
     * Hàm 4: Đếm tổng số lượng thành viên hiện tại của một dự án
     * Phục vụ Ràng buộc 5: Chặn khi đạt Quota tối đa 10 người/dự án
     * 
     * @param projectId ID của dự án
     * @return Số lượng thành viên hiện có
     */
    public static int countMembers(int projectId) {
        if (projectId <= 0) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM project_members WHERE project_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi đếm số thành viên Project ID: " + projectId, e);
        }
        return 0;
    }

    /**
     * Hàm 5: Thêm thành viên mới vào dự án
     * Dùng khi thành viên bấm "Chấp nhận lời mời" hoặc PM bấm "Duyệt xin gia nhập"
     * 
     * @param member Đối tượng ProjectMember cần thêm
     */
    public static void insert(ProjectMember member) {
        if (member == null || member.getProjectId() <= 0 || member.getUserId() <= 0) {
            return;
        }

        String sql = "INSERT INTO project_members (project_id, user_id, project_role, joined_at) " +
                     "VALUES (?, ?, ?::project_role_enum, NOW()) ON CONFLICT (project_id, user_id) DO NOTHING";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, member.getProjectId());
            ps.setInt(2, member.getUserId());
            String role = member.getProjectRole() != null && member.getProjectRole().equalsIgnoreCase("OWNER") ? "OWNER" : "MEMBER";
            ps.setString(3, role);

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi thêm thành viên vào Project", e);
        }
    }

    /**
     * Hàm 6: Xóa thành viên ra khỏi dự án
     * Dùng khi PM thu hồi quyền hoặc thành viên rời dự án
     * 
     * @param projectId ID của dự án
     * @param userId ID của người dùng cần xóa
     * @return true nếu xóa thành công, ngược lại false
     */
    public static boolean delete(int projectId, int userId) {
        if (projectId <= 0 || userId <= 0) {
            return false;
        }

        String sql = "DELETE FROM project_members WHERE project_id = ? AND user_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);
            ps.setInt(2, userId);

            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa thành viên khỏi Project", e);
        }
        return false;
    }

    /**
     * Hàm 7: Đồng bộ tên thành viên mới sang toàn bộ danh sách thành viên dự án
     * (Trong mô hình RDBMS, tên hiển thị được tự động lấy qua JOIN bảng users nên hàm này không cần cập nhật thừa)
     */
    public static void syncUserName(int userId, String newFullName) {
        // Tự động đồng bộ qua JOIN users table trong Database
    }
}
