package com.teamwork.data;

import com.teamwork.business.Doc;
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
 * Tầng Data Access Object (DAO): Quản lý Tài liệu & Ghi chú Wiki kết nối Supabase PostgreSQL.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng LEFT JOIN users để lấy tên tác giả (authorName) chính xác
 * - Sử dụng PreparedStatement an toàn chống SQL Injection
 * - Quản lý tài nguyên bằng try-with-resources
 */
public class DocDB {

    private static final Logger LOGGER = Logger.getLogger(DocDB.class.getName());

    private static final String BASE_SELECT_SQL =
        "SELECT d.id, d.project_id, d.title, d.content, d.author_id, " +
        "       COALESCE(u.full_name, 'Ẩn danh') AS author_name, " +
        "       to_char(d.created_at, 'DD/MM/YYYY HH24:MI') AS created_at_str, " +
        "       to_char(d.updated_at, 'DD/MM/YYYY HH24:MI') AS updated_at_str " +
        "FROM docs d " +
        "LEFT JOIN users u ON d.author_id = u.id ";

    private static Doc mapResultSetToDoc(ResultSet rs) throws SQLException {
        return new Doc(
            rs.getInt("id"),
            rs.getInt("project_id"),
            rs.getString("title"),
            rs.getString("content"),
            rs.getInt("author_id"),
            rs.getString("author_name"),
            rs.getString("created_at_str"),
            rs.getString("updated_at_str")
        );
    }

    /**
     * HÀM 1: Lấy danh sách tất cả các bài viết trong hệ thống
     */
    public static List<Doc> selectAll() {
        List<Doc> list = new ArrayList<>();
        String sql = BASE_SELECT_SQL + "ORDER BY d.id ASC";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToDoc(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy toàn bộ Doc", e);
        }
        return list;
    }

    /**
     * HÀM 2: Lấy danh sách toàn bộ tài liệu thuộc về MỘT DỰ ÁN cụ thể
     */
    public static List<Doc> selectByProjectId(int projectId) {
        List<Doc> list = new ArrayList<>();
        if (projectId <= 0) return list;

        String sql = BASE_SELECT_SQL + "WHERE d.project_id = ? ORDER BY d.id ASC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDoc(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy Doc theo Project ID: " + projectId, e);
        }
        return list;
    }

    /**
     * HÀM 3: Tìm một tài liệu cụ thể theo ID
     */
    public static Doc selectById(int id) {
        if (id <= 0) return null;

        String sql = BASE_SELECT_SQL + "WHERE d.id = ? LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDoc(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm Doc ID: " + id, e);
        }
        return null;
    }

    /**
     * HÀM 4: Thêm một bài viết tài liệu mới
     */
    public static int insert(Doc doc) {
        if (doc == null || doc.getTitle() == null || doc.getTitle().trim().isEmpty()) {
            return 0;
        }

        String sql = "INSERT INTO docs (project_id, title, content, author_id, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, NOW(), NOW()) RETURNING id";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, doc.getProjectId());
            ps.setString(2, doc.getTitle().trim());
            ps.setString(3, doc.getContent() != null ? doc.getContent().trim() : "");

            if (doc.getAuthorId() > 0) {
                ps.setInt(4, doc.getAuthorId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int genId = rs.getInt(1);
                    doc.setId(genId);
                    return genId;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi thêm Doc mới: " + doc.getTitle(), e);
        }
        return 0;
    }

    /**
     * HÀM 5: Cập nhật nội dung bài viết tài liệu
     */
    public static boolean update(Doc updatedDoc) {
        if (updatedDoc == null || updatedDoc.getId() <= 0) return false;

        String sql = "UPDATE docs SET title = ?, content = ?, updated_at = NOW() WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, updatedDoc.getTitle() != null ? updatedDoc.getTitle().trim() : "");
            ps.setString(2, updatedDoc.getContent() != null ? updatedDoc.getContent().trim() : "");
            ps.setInt(3, updatedDoc.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi update Doc ID: " + updatedDoc.getId(), e);
        }
        return false;
    }

    /**
     * HÀM 6: Xóa một bài viết theo ID
     */
    public static boolean delete(int id) {
        if (id <= 0) return false;

        String sql = "DELETE FROM docs WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa Doc ID: " + id, e);
        }
        return false;
    }

    /**
     * HÀM 7: Đếm tổng số tài liệu của một Dự án
     */
    public static int countByProject(int projectId) {
        if (projectId <= 0) return 0;

        String sql = "SELECT COUNT(*) FROM docs WHERE project_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi đếm Doc Project ID: " + projectId, e);
        }
        return 0;
    }
}
