package com.teamwork.listeners;

import com.teamwork.data.ActivityLogDB;
import com.teamwork.data.JPAUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dọn tài nguyên khi Tomcat dừng hoặc redeploy ứng dụng.
 *
 * <p>Nếu không dọn: luồng nền ghi log, connection pool và JDBC driver vẫn giữ ClassLoader cũ,
 * Tomcat báo "memory leak" và sau vài lần redeploy sẽ hết bộ nhớ / hết kết nối tới Supabase.</p>
 *
 * Thứ tự: dừng luồng ghi log (để nó kịp ghi xong) → đóng EntityManagerFactory (đóng pool kết nối)
 * → gỡ JDBC driver do chính webapp nạp.
 */
@WebListener
public class AppLifecycleListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(AppLifecycleListener.class.getName());

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ActivityLogDB.shutdown();
        JPAUtil.close();
        deregisterJdbcDrivers();
        stopMysqlCleanupThread();
        LOGGER.info("TeamWork Hub: đã dọn tài nguyên khi tắt ứng dụng.");
    }

    /** Chỉ gỡ driver do ClassLoader của webapp này nạp (không đụng tới driver của Tomcat / app khác). */
    private static void deregisterJdbcDrivers() {
        ClassLoader webappLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == webappLoader) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Không gỡ được JDBC driver " + driver, e);
                }
            }
        }
    }

    /** Driver MySQL (có trong pom cho cấu hình dự phòng) tự chạy một luồng dọn kết nối; phải dừng thủ công. */
    private static void stopMysqlCleanupThread() {
        try {
            Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread")
                    .getMethod("checkedShutdown")
                    .invoke(null);
        } catch (ClassNotFoundException e) {
            // Không dùng MySQL: bỏ qua
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Không dừng được luồng dọn kết nối MySQL", e);
        }
    }
}
