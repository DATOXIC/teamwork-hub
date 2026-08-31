package com.teamwork.data;

import com.teamwork.business.Label;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý dữ liệu Nhãn phân loại (Label) kết nối Supabase PostgreSQL.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng PreparedStatement an toàn chống SQL Injection
 * - Quản lý tài nguyên bằng try-with-resources
 */
public class LabelDB {

    private static final Logger LOGGER = Logger.getLogger(LabelDB.class.getName());

    private static Label mapResultSetToLabel(ResultSet rs) throws SQLException {
        return new Label(
            rs.getInt("id"),
            rs.getInt("project_id"),
            rs.getString("name"),
            rs.getString("color_key"),
            rs.getString("icon")
        );
    }

    /**
     * Nghiệp vụ 1: Lấy tất cả nhãn thuộc về một dự án cụ thể
     */
    public static List<Label> selectByProjectId(int projectId) {
        List<Label> result = new ArrayList<>();
        if (projectId <= 0) return result;

        String sql = "SELECT id, project_id, name, color_key::text AS color_key, icon FROM labels WHERE project_id = ? ORDER BY id ASC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToLabel(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách nhãn Project ID: " + projectId, e);
        }
        return result;
    }

    /**
     * Nghiệp vụ 2: Tìm nhãn theo ID
     */
    public static Label selectById(int id) {
        if (id <= 0) return null;

        String sql = "SELECT id, project_id, name, color_key::text AS color_key, icon FROM labels WHERE id = ? LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLabel(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm nhãn ID: " + id, e);
        }
        return null;
    }

    /**
     * Nghiệp vụ 3: Kiểm tra tên nhãn đã tồn tại trong dự án chưa
     */
    public static boolean existsByName(int projectId, String name, int excludeId) {
        if (name == null || name.trim().isEmpty() || projectId <= 0) {
            return false;
        }

        String sql = "SELECT 1 FROM labels WHERE project_id = ? AND LOWER(name) = LOWER(?) AND id <> ? LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);
            ps.setString(2, name.trim());
            ps.setInt(3, excludeId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi kiểm tra trùng tên nhãn", e);
        }
        return false;
    }

    /**
     * Nghiệp vụ 4: Thêm một nhãn mới vào dự án
     */
    public static int insert(Label label) {
        if (label == null || label.getProjectId() <= 0 || label.getName() == null || label.getName().trim().isEmpty()) {
            return 0;
        }

        String sql = "INSERT INTO labels (project_id, name, color_key, icon) " +
                     "VALUES (?, ?, ?::label_color_enum, ?) RETURNING id";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, label.getProjectId());
            ps.setString(2, label.getName().trim());
            String color = label.getColorKey() != null && !label.getColorKey().trim().isEmpty() ? label.getColorKey().trim().toLowerCase() : "blue";
            ps.setString(3, color);
            ps.setString(4, label.getIcon() != null && !label.getIcon().trim().isEmpty() ? label.getIcon().trim() : "bi-tag-fill");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int genId = rs.getInt(1);
                    label.setId(genId);
                    return genId;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi thêm nhãn mới: " + label.getName(), e);
        }
        return 0;
    }

    /**
     * Nghiệp vụ 5: Cập nhật thông tin nhãn
     */
    public static boolean update(Label updatedLabel) {
        if (updatedLabel == null || updatedLabel.getId() <= 0) return false;

        String sql = "UPDATE labels SET name = ?, color_key = ?::label_color_enum, icon = ? WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, updatedLabel.getName() != null ? updatedLabel.getName().trim() : "");
            String color = updatedLabel.getColorKey() != null ? updatedLabel.getColorKey().trim().toLowerCase() : "blue";
            ps.setString(2, color);
            ps.setString(3, updatedLabel.getIcon() != null ? updatedLabel.getIcon().trim() : "bi-tag-fill");
            ps.setInt(4, updatedLabel.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi update nhãn ID: " + updatedLabel.getId(), e);
        }
        return false;
    }

    /**
     * Nghiệp vụ 6: Xóa nhãn theo ID
     */
    public static boolean delete(int id) {
        if (id <= 0) return false;

        String sql = "DELETE FROM labels WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa nhãn ID: " + id, e);
        }
        return false;
    }
}