package com.teamwork.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
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

    // Thông tin kết nối ghi đè lên persistence.xml (mật khẩu KHÔNG được nằm trong code/repo)
    private static final Map<String, String> CONNECTION_OVERRIDES = new HashMap<>();

    static {
        // 1. File db.properties cục bộ (nằm trong .gitignore, xem db.properties.example)
        Properties props = new Properties();
        try (InputStream in = JPAUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            LOGGER.warning("JPAUtil: Không thể đọc db.properties, chỉ dùng biến môi trường.");
        }

        String unit = firstNonBlank(System.getenv("DB_JPA_UNIT"), props.getProperty("jpa.unit"));
        if (unit != null) {
            persistenceUnitName = unit;
        }

        // 2. Biến môi trường (Render/Docker) được ưu tiên hơn file cục bộ
        putOverride("jakarta.persistence.jdbc.url", System.getenv("DB_URL"), props.getProperty("db.url"));
        putOverride("jakarta.persistence.jdbc.user", System.getenv("DB_USERNAME"), props.getProperty("db.username"));
        putOverride("jakarta.persistence.jdbc.password", System.getenv("DB_PASSWORD"), props.getProperty("db.password"));

        if ("teamwork-cloud".equals(persistenceUnitName) && !CONNECTION_OVERRIDES.containsKey("jakarta.persistence.jdbc.password")) {
            LOGGER.severe("JPAUtil: Chưa cấu hình mật khẩu DB. Đặt biến môi trường DB_PASSWORD "
                    + "hoặc tạo src/main/resources/db.properties từ db.properties.example.");
        }
    }

    private static void putOverride(String key, String envValue, String fileValue) {
        String value = firstNonBlank(envValue, fileValue);
        if (value != null) {
            CONNECTION_OVERRIDES.put(key, value);
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a.trim();
        if (b != null && !b.trim().isEmpty()) return b.trim();
        return null;
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
                    emf = Persistence.createEntityManagerFactory(persistenceUnitName, CONNECTION_OVERRIDES);
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

    /** Một khối công việc chạy trong transaction (xem {@link #inTransaction}). */
    @FunctionalInterface
    public interface TxWork<T> {
        T run(EntityManager em) throws Exception;
    }

    /**
     * Chạy nhiều câu lệnh trong MỘT transaction: tất cả cùng thành công, hoặc lỗi ở bất kỳ bước nào
     * thì rollback toàn bộ (không để lại dữ liệu dở dang như task đã mất việc con nhưng vẫn còn task).
     *
     * @param what mô tả ngắn để ghi log khi lỗi
     * @param fallback giá trị trả về khi lỗi (ví dụ false / 0)
     */
    public static <T> T inTransaction(String what, T fallback, TxWork<T> work) {
        EntityManager em = getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            T result = work.run(em);
            tx.commit();
            return result;
        } catch (Exception e) {
            rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Transaction thất bại, đã rollback: " + what, e);
            return fallback;
        } finally {
            closeEntityManager(em);
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