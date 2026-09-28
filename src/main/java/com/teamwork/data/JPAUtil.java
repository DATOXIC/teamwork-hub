package com.teamwork.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility Class quản lý vòng đời EntityManagerFactory và EntityManager (Jakarta Persistence / Hibernate).
 * - Cung cấp Singleton EntityManagerFactory tối ưu hóa tài nguyên
 * - Hỗ trợ nạp động Persistence Unit ("teamwork-cloud" hoặc "teamwork-mysql")
 * - Cung cấp các hàm phụ trợ quản lý Transaction (begin, commit, rollback) an toàn
 */
public class JPAUtil {

    private static final Logger LOGGER = Logger.getLogger(JPAUtil.class.getName());
    private static final int MAX_INIT_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 1000;

    private static volatile EntityManagerFactory emf;
    private static String persistenceUnitName = "teamwork-cloud"; // Mặc định Cloud Supabase
    // private static String persistenceUnitName = "teamwork-sqlserver";

    static {
        // Kiểm tra cấu hình trong db.properties nếu có ghi đè persistence unit
        try (InputStream in = JPAUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                String unit = props.getProperty("jpa.unit");
                if (unit != null && !unit.trim().isEmpty()) {
                    persistenceUnitName = unit.trim();
                }
            }
        } catch (Exception e) {
            LOGGER.warning("JPAUtil: Không thể đọc jpa.unit từ db.properties, dùng mặc định: " + persistenceUnitName);
        }
    }

    /**
     * Khởi tạo EntityManagerFactory khi cần và cho phép thử lại ở lần gọi sau.
     * Không tạo trong khối static: nếu lần kết nối đầu bị ngắt (ví dụ TLS handshake tới Supabase),
     * khối static hỏng sẽ làm JPAUtil không dùng được cho tới khi khởi động lại Tomcat.
     */
    private static EntityManagerFactory getFactory() {
        EntityManagerFactory current = emf;
        if (current != null && current.isOpen()) {
            return current;
        }

        synchronized (JPAUtil.class) {
            if (emf != null && emf.isOpen()) {
                return emf;
            }

            RuntimeException lastError = null;
            for (int attempt = 1; attempt <= MAX_INIT_ATTEMPTS; attempt++) {
                try {
                    LOGGER.info("JPAUtil: Đang khởi tạo EntityManagerFactory cho Persistence Unit: [" + persistenceUnitName
                            + "] (lần " + attempt + "/" + MAX_INIT_ATTEMPTS + ")...");
                    emf = Persistence.createEntityManagerFactory(persistenceUnitName);
                    LOGGER.info("JPAUtil: Khởi tạo EntityManagerFactory [" + persistenceUnitName + "] thành công!");
                    return emf;
                } catch (RuntimeException ex) {
                    lastError = ex;
                    LOGGER.log(Level.SEVERE, "JPAUtil: Khởi tạo EntityManagerFactory thất bại (lần " + attempt + ")!", ex);
                    if (attempt < MAX_INIT_ATTEMPTS) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            throw new IllegalStateException("Không thể kết nối cơ sở dữ liệu, vui lòng thử lại sau ít phút!", lastError);
        }
    }

    /**
     * Mở một EntityManager mới cho từng nghiệp vụ / Request
     * @return EntityManager đối tượng quản lý Entity
     */
    public static EntityManager getEntityManager() {
        return getFactory().createEntityManager();
    }

    /**
     * Đóng an toàn EntityManager
     */
    public static void closeEntityManager(EntityManager em) {
        if (em != null && em.isOpen()) {
            try {
                em.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "JPAUtil: Lỗi khi đóng EntityManager", e);
            }
        }
    }

    /**
     * Helper rollback an toàn khi xảy ra lỗi trong Transaction
     */
    public static void rollbackIfActive(EntityTransaction tx) {
        if (tx != null && tx.isActive()) {
            try {
                tx.rollback();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "JPAUtil: Lỗi khi rollback Transaction", e);
            }
        }
    }

    /**
     * Đóng toàn bộ EntityManagerFactory khi ứng dụng tắt
     */
    public static void close() {
        if (emf != null && emf.isOpen()) {
            try {
                emf.close();
                LOGGER.info("JPAUtil: Đã đóng EntityManagerFactory thành công.");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "JPAUtil: Lỗi khi đóng EntityManagerFactory", e);
            }
        }
    }

    public static String getPersistenceUnitName() {
        return persistenceUnitName;
    }
}