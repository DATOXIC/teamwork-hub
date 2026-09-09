package com.teamwork.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility Class quản lý kết nối Cơ Sở Dữ Liệu PostgreSQL (Supabase Singapore - ap-southeast-1).
 * Sử dụng HikariCP Connection Pool hiệu năng cao:
 * - Giữ các kết nối TCP/SSL luôn mở sẵn (Warm-up & Keep-Alive)
 * - Đặt tại Region Singapore cho tốc độ mạng thấp nhất từ Việt Nam (< 50ms)
 * - Tự động tái sử dụng kết nối an toàn đa luồng (Thread-Safe)
 */
public class DBUtil {

    private static final Logger LOGGER = Logger.getLogger(DBUtil.class.getName());
    private static HikariDataSource dataSource;

    static {
        try {
            Properties props = new Properties();
            String driver = "org.postgresql.Driver";
            String url = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require";
            String username = "postgres.nppsoolbarfagwqdtcdm";
            String password = "matkhaudenho123";

            try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in != null) {
                    props.load(in);
                    if (props.getProperty("db.driver") != null) driver = props.getProperty("db.driver").trim();
                    if (props.getProperty("db.url") != null) url = props.getProperty("db.url").trim();
                    if (props.getProperty("db.username") != null) username = props.getProperty("db.username").trim();
                    if (props.getProperty("db.password") != null) password = props.getProperty("db.password").trim();
                    LOGGER.info("DBUtil: Nạp cấu hình từ db.properties thành công.");
                } else {
                    LOGGER.info("DBUtil: Sử dụng cấu hình mặc định Supabase Singapore Pooler.");
                }
            }

            HikariConfig config = new HikariConfig();
            config.setDriverClassName(driver);
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);

            // ─── CẤU HÌNH POOL SIZE (Tinh chỉnh cho Render Free 512MB RAM) ───────────
            // Render Free chỉ có 512MB RAM dùng chung cho JVM + Tomcat + HikariCP.
            // Mỗi connection JDBC tốn ~10-15MB RAM → Giới hạn 5 connection = ~50-75MB.
            // Dự án đồ án sinh viên < 10 người dùng đồng thời nên 5 connection là dư.
            config.setMaximumPoolSize(5);           // Tối đa 5 kết nối đồng thời
            config.setMinimumIdle(2);               // Luôn giữ 2 kết nối sẵn sàng

            // ─── TIMEOUT (Tinh chỉnh cho môi trường Cloud Free - Kết nối bất ổn) ─────
            config.setConnectionTimeout(15000);     // Chờ lấy kết nối từ pool tối đa 15 giây
            config.setIdleTimeout(300000);          // 5 phút không dùng mới giải phóng (UptimeRobot ping mỗi 5 phút giữ pool ấm)
            config.setMaxLifetime(1800000);         // Tái tạo kết nối mỗi 30 phút (tránh bị Supabase server-side timeout)
            config.setKeepaliveTime(60000);         // Ping giữ kết nối mỗi 60 giây (tránh firewall cắt TCP idle)

            // ─── XÁC THỰC KẾT NỐI (Phòng kết nối "ma" từ Cloud bị đứt ngầm) ─────────
            config.setConnectionTestQuery("SELECT 1");  // Kiểm tra kết nối còn sống trước khi dùng
            config.setValidationTimeout(3000);          // Timeout kiểm tra tối đa 3 giây

            // ─── TỐI ƯU HOÁ PREPARED STATEMENT CACHE ─────────────────────────────────
            // Cache lại các câu lệnh SQL đã biên dịch (Giảm 30-50% overhead mỗi query)
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");       // Cache 250 câu SQL khác nhau
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048"); // SQL tối đa 2048 ký tự
            config.addDataSourceProperty("useServerPrepStmts", "true");    // Dùng Server-Side Prepared Stmt

            // ─── GIỮ KẾT NỐI TCP BỀN VỮNG ───────────────────────────────────────────
            config.addDataSourceProperty("tcpKeepAlive", "true");           // Bật TCP Keep-Alive ở tầng socket

            // ─── TÊN POOL (Dễ theo dõi trong log Tomcat) ─────────────────────────────
            config.setPoolName("TeamworkHub-HikariPool");

            dataSource = new HikariDataSource(config);
            LOGGER.info("DBUtil: Khởi tạo HikariCP Pool [" + config.getPoolName() + "] → Supabase Singapore (Session Mode :5432) thành công!");

            // ─── TỰ ĐỘNG CẬP NHẬT CỘT MỚI (MIGRATION IDEMPOTENT) ─────────────────
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE projects ADD COLUMN IF NOT EXISTS project_type VARCHAR(20) NOT NULL DEFAULT 'TEAM'");
                stmt.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS requires_gate BOOLEAN NOT NULL DEFAULT TRUE");
                LOGGER.info("DBUtil: Migration kiểm tra cấu trúc schema (project_type, requires_gate) thành công.");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "DBUtil: Lưu ý khi chạy migration cập nhật schema: " + e.getMessage());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "DBUtil: Khởi tạo HikariCP thất bại", e);
        }
    }

    /**
     * Mượn 1 kết nối có sẵn từ HikariCP Pool (Cực nhanh, không tốn thời gian bắt tay SSL/TCP)
     * @return Connection đối tượng kết nối JDBC
     * @throws SQLException nếu kết nối thất bại
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        throw new SQLException("DBUtil: HikariDataSource chưa được khởi tạo!");
    }

    /**
     * Đóng an toàn các tài nguyên JDBC (Trả kết nối lại cho Pool để tái sử dụng)
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Lỗi khi đóng ResultSet", e);
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Lỗi khi đóng Statement", e);
            }
        }
        if (conn != null) {
            try {
                conn.close(); // Trả kết nối về Pool, KHÔNG đóng socket mạng vật lý
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Lỗi khi đóng Connection", e);
            }
        }
    }

    /**
     * Đóng an toàn Connection và Statement
     */
    public static void close(Connection conn, Statement stmt) {
        close(conn, stmt, null);
    }

    /**
     * Đóng an toàn Connection
     */
    public static void close(Connection conn) {
        close(conn, null, null);
    }
}
