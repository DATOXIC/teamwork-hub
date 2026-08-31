package com.teamwork.data;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility Class quản lý kết nối Cơ Sở Dữ Liệu PostgreSQL (Supabase).
 * 
 * Áp dụng nguyên tắc Backend Code Mastery:
 * - Đọc cấu hình từ file db.properties một cách an toàn
 * - Nạp PostgreSQL JDBC Driver
 * - Kết nối qua Transaction Pooler IPv4 (aws-0-ap-northeast-2.pooler.supabase.com:6543)
 * - Cung cấp hàm đóng tài nguyên Connection/Statement/ResultSet chống rò rỉ bộ nhớ (Memory Leak)
 */
public class DBUtil {

    private static final Logger LOGGER = Logger.getLogger(DBUtil.class.getName());
    
    private static String driver = "org.postgresql.Driver";
    private static String url = "jdbc:postgresql://aws-0-ap-northeast-2.pooler.supabase.com:6543/postgres?sslmode=require";
    private static String username = "postgres.ioogfazyclnbcegtkxrq";
    private static String password = "matkhaudenho123";

    static {
        try {
            // Đọc cấu hình từ file db.properties trong classpath
            Properties props = new Properties();
            try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in != null) {
                    props.load(in);
                    if (props.getProperty("db.driver") != null) driver = props.getProperty("db.driver").trim();
                    if (props.getProperty("db.url") != null) url = props.getProperty("db.url").trim();
                    if (props.getProperty("db.username") != null) username = props.getProperty("db.username").trim();
                    if (props.getProperty("db.password") != null) password = props.getProperty("db.password").trim();
                    LOGGER.info("DBUtil: Nạp cấu hình từ db.properties thành công.");
                } else {
                    LOGGER.info("DBUtil: Sử dụng cấu hình mặc định kết nối Supabase Pooler.");
                }
            }

            // Nạp Driver vào bộ nhớ
            Class.forName(driver);
            LOGGER.info("DBUtil: Nạp PostgreSQL JDBC Driver thành công.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "DBUtil: Khởi tạo kết nối thất bại", e);
        }
    }

    /**
     * Mở một kết nối mới tới Supabase PostgreSQL
     * @return Connection đối tượng kết nối JDBC
     * @throws SQLException nếu kết nối thất bại
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Đóng an toàn các tài nguyên JDBC (ResultSet, Statement, Connection)
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
                conn.close();
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
