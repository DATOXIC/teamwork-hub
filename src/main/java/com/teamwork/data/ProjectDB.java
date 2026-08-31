package com.teamwork.data;

import com.teamwork.business.Project;
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
 * Tầng Data Access Object (DAO): Quản lý dữ liệu Dự án (Project) kết nối Supabase PostgreSQL.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Tự động tính toán tiến độ (totalTasks, doneTasks) trực tiếp qua câu lệnh SQL tối ưu
 * - Sử dụng PreparedStatement an toàn chống SQL Injection
 * - Quản lý tài nguyên bằng try-with-resources
 */
public class ProjectDB {

    private static final Logger LOGGER = Logger.getLogger(ProjectDB.class.getName());

    /**
     * Hàm phụ trợ ánh xạ 1 dòng từ ResultSet sang đối tượng JavaBean Project
     * Tự động đọc số lượng task và task đã hoàn thành tính từ DB
     */
    public static Project mapResultSetToProject(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String projectCode = rs.getString("project_code");
        String name = rs.getString("name");
        String description = rs.getString("description");
        int ownerId = rs.getInt("owner_id");
        String createdAt = rs.getString("created_at_str");
        int totalTasks = rs.getInt("total_tasks");
        int doneTasks = rs.getInt("done_tasks");

        return new Project(
            id,
            projectCode != null ? projectCode : "PRJ-" + id,
            name != null ? name : "",
            description != null ? description : "",
            ownerId,
            createdAt != null ? createdAt : "",
            totalTasks,
            doneTasks
        );
    }

    /**
     * Hàm 1: Lấy toàn bộ danh sách dự án
     * 
     * @return Danh sách List<Project> kèm thông số tiến độ công việc
     */
    public static List<Project> selectAll() {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT p.id, p.project_code, p.name, p.description, p.owner_id, " +
                     "       to_char(p.created_at, 'YYYY-MM-DD') AS created_at_str, " +
                     "       COUNT(t.id) AS total_tasks, " +
                     "       COUNT(t.id) FILTER (WHERE t.status = 'DONE' OR t.status = 'APPROVED') AS done_tasks " +
                     "FROM projects p " +
                     "LEFT JOIN tasks t ON p.id = t.project_id " +
                     "GROUP BY p.id, p.project_code, p.name, p.description, p.owner_id, p.created_at " +
                     "ORDER BY p.id ASC";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToProject(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy toàn bộ danh sách Project", e);
        }
        return list;
    }

    /**
     * Hàm 2: Tìm dự án theo ID duy nhất
     * 
     * @param id Khóa chính định danh dự án
     * @return Đối tượng Project nếu tìm thấy, ngược lại trả về null
     */
    public static Project selectById(int id) {
        if (id <= 0) {
            return null;
        }

        String sql = "SELECT p.id, p.project_code, p.name, p.description, p.owner_id, " +
                     "       to_char(p.created_at, 'YYYY-MM-DD') AS created_at_str, " +
                     "       COUNT(t.id) AS total_tasks, " +
                     "       COUNT(t.id) FILTER (WHERE t.status = 'DONE' OR t.status = 'APPROVED') AS done_tasks " +
                     "FROM projects p " +
                     "LEFT JOIN tasks t ON p.id = t.project_id " +
                     "WHERE p.id = ? " +
                     "GROUP BY p.id, p.project_code, p.name, p.description, p.owner_id, p.created_at " +
                     "LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProject(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm Project theo ID: " + id, e);
        }
        return null;
    }

    /**
     * Hàm 3: Tìm dự án theo Mã Dự Án (projectCode)
     * Phục vụ Chiều 2: Thành viên nhập mã dự án để gửi yêu cầu Xin Gia Nhập
     * 
     * @param code Mã dự án (Ví dụ: "TW-HUB-01")
     * @return Đối tượng Project nếu tìm thấy, ngược lại trả về null
     */
    public static Project selectByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT p.id, p.project_code, p.name, p.description, p.owner_id, " +
                     "       to_char(p.created_at, 'YYYY-MM-DD') AS created_at_str, " +
                     "       COUNT(t.id) AS total_tasks, " +
                     "       COUNT(t.id) FILTER (WHERE t.status = 'DONE' OR t.status = 'APPROVED') AS done_tasks " +
                     "FROM projects p " +
                     "LEFT JOIN tasks t ON p.id = t.project_id " +
                     "WHERE UPPER(p.project_code) = UPPER(?) " +
                     "GROUP BY p.id, p.project_code, p.name, p.description, p.owner_id, p.created_at " +
                     "LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProject(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm Project theo Code: " + code, e);
        }
        return null;
    }

    /**
     * Hàm 4: Thêm dự án mới (Tự động cấp ID và tự động sinh Mã Dự Án nếu chưa có)
     * Đồng thời tự động thêm người tạo làm OWNER trong bảng project_members
     * 
     * @param project Đối tượng Project chứa thông tin dự án mới
     * @return ID tự tăng được tạo mới trong CSDL, hoặc 0 nếu thất bại
     */
    public static int insert(Project project) {
        if (project == null || project.getName() == null || project.getName().trim().isEmpty()) {
            return 0;
        }

        String projectCode = project.getProjectCode();
        if (projectCode == null || projectCode.trim().isEmpty()) {
            projectCode = "PRJ-" + System.currentTimeMillis() % 100000;
        }
        projectCode = projectCode.trim().toUpperCase();

        String sql = "INSERT INTO projects (project_code, name, description, owner_id, created_at) " +
                     "VALUES (?, ?, ?, ?, NOW()) RETURNING id";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, projectCode);
            ps.setString(2, project.getName().trim());
            ps.setString(3, project.getDescription() != null ? project.getDescription().trim() : "");
            ps.setInt(4, project.getOwnerId());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    project.setId(generatedId);
                    project.setProjectCode(projectCode);

                    // Tự động thêm Owner vào bảng project_members
                    String memberSql = "INSERT INTO project_members (project_id, user_id, project_role, joined_at) " +
                                       "VALUES (?, ?, 'OWNER', NOW()) ON CONFLICT (project_id, user_id) DO NOTHING";
                    try (PreparedStatement memberPs = conn.prepareStatement(memberSql)) {
                        memberPs.setInt(1, generatedId);
                        memberPs.setInt(2, project.getOwnerId());
                        memberPs.executeUpdate();
                    }

                    return generatedId;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi chèn Project mới: " + project.getName(), e);
        }
        return 0;
    }
}