package com.teamwork.listeners;

import com.teamwork.data.ActivityLogDB;
import com.teamwork.data.JPAUtil;
import com.teamwork.util.DeadlineReminder;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.time.ZonedDateTime;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dọn tài nguyên khi Tomcat dừng hoặc redeploy ứng dụng.
 *
 * <p>Nếu không dọn: luồng nền ghi log, connection pool và JDBC driver vẫn giữ ClassLoader cũ,
 * Tomcat báo "memory leak" và sau vài lần redeploy sẽ hết bộ nhớ / hết kết nối tới Supabase.</p>
 *
 * Khi khởi động: lên lịch job nhắc hạn chót hằng ngày (xem {@link DeadlineReminder}).
 *
 * Thứ tự: dừng luồng ghi log (để nó kịp ghi xong) → đóng EntityManagerFactory (đóng pool kết nối)
 * → gỡ JDBC driver do chính webapp nạp.
 */
@WebListener
public class AppLifecycleListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(AppLifecycleListener.class.getName());

    /** Luồng chạy job nhắc hạn hằng ngày (null nếu đã tắt bằng TEAMWORK_REMINDERS=off). */
    private ScheduledExecutorService scheduler;

    /**
     * Khởi động job NHẮC HẠN CHÓT: mỗi ngày lúc TEAMWORK_REMINDER_HOUR giờ (mặc định 8h, giờ Việt Nam).
     * Nếu app khởi động SAU giờ đó (vd Render vừa "thức dậy"), chạy bù một lần sau 2 phút — DeadlineReminder
     * tự chống gửi trùng nên chạy bù không làm ai nhận 2 lần.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (!DeadlineReminder.enabled()) {
            LOGGER.info("Nhắc hạn: đã tắt (TEAMWORK_REMINDERS=off).");
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "deadline-reminder");
            t.setDaemon(true);
            return t;
        });
        int hour = DeadlineReminder.hour();
        ZonedDateTime now = ZonedDateTime.now(DeadlineReminder.VN);
        long delay = DeadlineReminder.initialDelayMillis(now, hour);
        scheduler.scheduleAtFixedRate(new DeadlineReminder(), delay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
        if (now.getHour() >= hour) {
            scheduler.schedule(new DeadlineReminder(), 2, TimeUnit.MINUTES);
        }
        LOGGER.info("Nhắc hạn: chạy hằng ngày lúc " + hour + "h (giờ Việt Nam), lần kế tiếp sau "
                + TimeUnit.MILLISECONDS.toMinutes(delay) + " phút.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
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
