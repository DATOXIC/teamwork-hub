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
 * Táº§ng Data Access Object (DAO): Quáº£n lÃ½ lÆ°u váº¿t vÃ  truy váº¥n Nháº­t kÃ½ hoáº¡t Ä‘á»™ng (Activity Log).
 * TÆ°Æ¡ng thÃ­ch HikariCP vÃ  Supabase PostgreSQL.
 * 
 * Ãp dá»¥ng nguyÃªn táº¯c Backend Code Mastery:
 * - Äáº£m báº£o tÃ­nh sáºµn sÃ ng cao (High Availability): Báº£ng tá»± Ä‘á»™ng táº¡o náº¿u chÆ°a tá»“n táº¡i.
 * - Non-blocking: Ghi log cÃ³ cÆ¡ cháº¿ an toÃ n tuyá»‡t Ä‘á»‘i, lá»—i ghi log khÃ´ng lÃ m giÃ¡n Ä‘oáº¡n transaction chÃ­nh.
 * - TrÃ¡nh n+1 query: JOIN báº£ng users láº¥y tÃªn vÃ  avatar trong 1 truy váº¥n duy nháº¥t.
 */
public class ActivityLogDB {

    private static final Logger LOGGER = Logger.getLogger(ActivityLogDB.class.getName());
    private static final ExecutorService ASYNC_POOL = Executors.newFixedThreadPool(2);
    private static volatile boolean tableVerified = false;

    /**
     * Tá»± Ä‘á»™ng khá»Ÿi táº¡o báº£ng activity_logs vÃ  index náº¿u chÆ°a tá»“n táº¡i trÃªn PostgreSQL.
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
            LOGGER.info("ActivityLogDB: Báº£ng activity_logs Ä‘Ã£ sáºµn sÃ ng.");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Lá»—i khi kiá»ƒm tra/táº¡o báº£ng activity_logs", e);
        }
    }

    static {
        // Tá»± Ä‘á»™ng kiá»ƒm tra schema khi náº¡p class
        try {
            ensureTableExists();
        } catch (Exception ignored) {}
    }

    /**
     * Ghi nháº­n má»™t hÃ nh Ä‘á»™ng hoáº¡t Ä‘á»™ng vÃ o nháº­t kÃ½ dá»± Ã¡n (Synchronous an toÃ n).
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
            // KhÃ´ng bao giá» Ä‘á»ƒ lá»—i log lÃ m crash luá»“ng chÃ­nh
            LOGGER.log(Level.WARNING, "KhÃ´ng thá»ƒ ghi activity log cho projectId=" + projectId, e);
        }
    }

    /**
     * Ghi nháº­n hoáº¡t Ä‘á»™ng cháº¡y ná»n báº¥t Ä‘á»“ng bá»™ (Non-blocking) Ä‘á»ƒ tá»‘i Æ°u Ä‘á»™ trá»… request.
     */
    public static void logAsync(int projectId, int userId, String actionType,
                                String targetType, int targetId, String targetTitle, String description) {
        ASYNC_POOL.submit(() -> {
            try {
                log(projectId, userId, actionType, targetType, targetId, targetTitle, description);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Lá»—i khi ghi async activity log", e);
            }
        });
    }

    /**
     * Láº¥y danh sÃ¡ch lá»‹ch sá»­ hoáº¡t Ä‘á»™ng má»›i nháº¥t cá»§a má»™t dá»± Ã¡n.
     * @param projectId ID cá»§a dá»± Ã¡n
     * @param limit Sá»‘ lÆ°á»£ng báº£n ghi tá»‘i Ä‘a (vÃ­ dá»¥ 50)
     */
    public static List<ActivityLog> selectByProjectId(int projectId, int limit) {
        List<ActivityLog> list = new ArrayList<>();
        if (projectId <= 0) return list;
        ensureTableExists();

        int maxRows = limit > 0 ? limit : 50;
        String sql = 
            "SELECT a.id, a.project_id, a.user_id, " +
            "       COALESCE(u.fullname, 'ThÃ nh viÃªn') AS user_name, " +
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
            LOGGER.log(Level.SEVERE, "Lá»—i khi láº¥y danh sÃ¡ch ActivityLog cho projectId=" + projectId, e);
        }
        return list;
    }
}