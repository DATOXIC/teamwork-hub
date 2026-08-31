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

            // Cấu hình tối ưu cho Supabase Pooler Singapore
            config.setMaximumPoolSize(10);          // Duy trì tối đa 10 kết nối đồng thời
            config.setMinimumIdle(3);              // Luôn giữ 3 kết nối sẵn sàng (Warm-up)
            config.setIdleTimeout(60000);          // 60 giây không dùng thì giải phóng bớt
            config.setConnectionTimeout(10000);     // Chờ lấy kết nối tối đa 10 giây
            config.setMaxLifetime(600000);         // Tái tạo kết nối mỗi 10 phút để tránh đứt socket
            config.setKeepaliveTime(30000);        // Bắn ping giữ kết nối mỗi 30 giây

            // Tối ưu hóa bộ nhớ đệm Prepared Statement của PostgreSQL Driver
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("tcpKeepAlive", "true");

            dataSource = new HikariDataSource(config);
            LOGGER.info("DBUtil: Khởi tạo HikariCP Connection Pool (Singapore) thành công!");
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
