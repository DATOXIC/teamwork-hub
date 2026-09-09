package com.teamwork.data;

import com.teamwork.business.ActivityLog;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tang Data Access Object (DAO): Quan ly luu vet va truy van Nhat ky hoat dong (Activity Log).
 * Tuong thich HikariCP va Supabase PostgreSQL.
 * 
 * Ap dung nguyen tac Backend Code Mastery:
 * - Dam bao tinh san sang cao (High Availability): Bang tu dong tao neu chua ton tai.
 * - Non-blocking: Ghi log co co che an toan tuyet doi, loi ghi log khong lam gian doan transaction chinh.
 * - Tranh n+1 query: JOIN bang users lay ten va avatar trong 1 truy van duy nhat.
 */
public class ActivityLogDB {

    private static final Logger LOGGER = Logger.getLogger(ActivityLogDB.class.getName());
    private static final ExecutorService ASYNC_POOL = Executors.newFixedThreadPool(2);
    private static volatile boolean tableVerified = false;

    /**
     * Tu dong khoi tao bang activity_logs va index neu chua ton tai tren PostgreSQL.
     */
    public static synchronized void ensureTableExists() {
        if (tableVerified) return;

        String ddl = 
            "CREATE TABLE IF NOT EXISTS activity_logs (" +
            "    id SERIAL PRIMARY KEY," +
            "    project_id INT NOT NULL," +
            "    user_id INT," +
            "    action_type VARCHAR(50) NOT NULL," +
            "    target_type VARCHAR(50) NOT NULL," +
            "    target_id INT DEFAULT 0," +
            "    target_title VARCHAR(255) DEFAULT ''," +
            "    description TEXT DEFAULT ''," +
            "    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP" +
            ");" +
            "CREATE INDEX IF NOT EXISTS idx_activity_logs_project ON activity_logs(project_id, created_at DESC);" +
            "CREATE INDEX IF NOT EXISTS idx_activity_logs_user ON activity_logs(user_id);";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
            tableVerified = true;
            LOGGER.info("ActivityLogDB: Bang activity_logs da san sang.");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Loi khi kiem tra hoac tao bang activity_logs", e);
        }
    }

    static {
        // Tu dong kiem tra schema khi nap class
        try {
            ensureTableExists();
        } catch (Exception ignored) {}
    }

    /**
     * Ghi nhan mot hanh dong hoat dong vao nhat ky du an (Synchronous an toan).
     */
    public static void log(int projectId, int userId, String actionType,
                           String targetType, int targetId, String targetTitle, String description) {
        if (projectId <= 0) return;
        ensureTableExists();

        String sql = "INSERT INTO activity_logs (project_id, user_id, action_type, target_type, target_id, target_title, description, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);
            if (userId > 0) {
                ps.setInt(2, userId);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, actionType != null ? actionType : "UPDATE");
            ps.setString(4, targetType != null ? targetType : "TASK");
            ps.setInt(5, targetId);
            ps.setString(6, targetTitle != null ? (targetTitle.length() > 250 ? targetTitle.substring(0, 247) + "..." : targetTitle) : "");
            ps.setString(7, description != null ? description : "");

            ps.executeUpdate();
        } catch (Exception e) {
            // Khong bao gio de loi log lam crash luong chinh
            LOGGER.log(Level.WARNING, "Khong the ghi activity log cho projectId=" + projectId, e);
        }
    }

    /**
     * Ghi nhan hoat dong chay nen bat dong bo (Non-blocking) de toi uu do tre request.
     */
    public static void logAsync(int projectId, int userId, String actionType,
                                String targetType, int targetId, String targetTitle, String description) {
        ASYNC_POOL.submit(() -> {
            try {
                log(projectId, userId, actionType, targetType, targetId, targetTitle, description);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Loi khi ghi async activity log", e);
            }
        });
    }

    /**
     * Lay danh sach lich su hoat dong moi nhat cua mot du an.
     * @param projectId ID cua du an
     * @param limit So luong ban ghi toi da (vi du 50)
     */
    public static List<ActivityLog> selectByProjectId(int projectId, int limit) {
        List<ActivityLog> list = new ArrayList<>();
        if (projectId <= 0) return list;
        ensureTableExists();

        int maxRows = limit > 0 ? limit : 50;
        String sql = 
            "SELECT a.id, a.project_id, a.user_id, " +
            "       COALESCE(u.full_name, '') AS user_name, " +
            "       COALESCE(u.avatar, '') AS user_avatar, " +
            "       a.action_type, a.target_type, a.target_id, a.target_title, a.description, " +
            "       to_char(a.created_at, 'DD/MM/YYYY HH24:MI') AS created_at_str, " +
            "       a.created_at AS raw_created_at " +
            "FROM activity_logs a " +
            "LEFT JOIN users u ON a.user_id = u.id " +
            "WHERE a.project_id = ? " +
            "ORDER BY a.created_at DESC, a.id DESC " +
            "LIMIT ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);
            ps.setInt(2, maxRows);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int pId = rs.getInt("project_id");
                    int uId = rs.getInt("user_id");
                    String userName = rs.getString("user_name");
                    String userAvatar = rs.getString("user_avatar");
                    String actionType = rs.getString("action_type");
                    String targetType = rs.getString("target_type");
                    int targetId = rs.getInt("target_id");
                    String targetTitle = rs.getString("target_title");
                    String description = rs.getString("description");
                    String createdAtStr = rs.getString("created_at_str");
                    Timestamp rawCreatedAt = rs.getTimestamp("raw_created_at");

                    list.add(new ActivityLog(
                        id, pId, uId, userName, userAvatar, actionType,
                        targetType, targetId, targetTitle, description,
                        createdAtStr, rawCreatedAt
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Loi khi lay danh sach ActivityLog cho projectId=" + projectId, e);
        }
        return list;
    }
}