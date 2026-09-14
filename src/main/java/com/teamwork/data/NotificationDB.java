package com.teamwork.data;

import com.teamwork.business.Notification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý Thông báo Hệ thống qua JPA 3.1 / Hibernate.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng JPA EntityManager và JPQL chuẩn, tương thích cả PostgreSQL và MySQL
 */
public class NotificationDB {

    private static final Logger LOGGER = Logger.getLogger(NotificationDB.class.getName());

    /**
     * Hàm 1: Phát tín hiệu gửi thông báo nhanh
     */
    public static void send(int recipientId, String title, String content, String link, String type) {
        if (recipientId <= 0 || title == null || title.trim().isEmpty()) {
            return;
        }

        Notification n = new Notification();
        n.setRecipientId(recipientId);
        n.setTitle(title.trim());
        n.setContent(content != null ? content.trim() : "");
        n.setLink(link != null && !link.trim().isEmpty() ? link.trim() : "#");
        n.setType(type != null && !type.trim().isEmpty() ? type.trim().toUpperCase() : "GENERAL");
        n.setRead(false);

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(n);
            tx.commit();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi gửi thông báo qua JPA cho User ID: " + recipientId, e);
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 2: Lấy danh sách tất cả thông báo của một người dùng (Mới nhất nằm ở trên đầu)
     */
    public static List<Notification> selectByRecipientId(int recipientId) {
        if (recipientId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Notification> query = em.createQuery(
                "SELECT n FROM Notification n WHERE n.recipientId = :recipientId ORDER BY n.id DESC",
                Notification.class
            );
            query.setParameter("recipientId", recipientId);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy thông báo của User ID qua JPA: " + recipientId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 3: Đếm số lượng thông báo CHƯA ĐỌC của một người dùng (cho Quả chuông 🔴 Header)
     */
    public static int countUnread(int recipientId) {
        if (recipientId <= 0) return 0;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId AND n.isRead = FALSE",
                Long.class
            )
            .setParameter("recipientId", recipientId)
            .getSingleResult();

            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi đếm thông báo chưa đọc qua JPA", e);
            return 0;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 4: Đánh dấu một thông báo cụ thể là Đã Đọc
     */
    public static void markAsRead(int notificationId) {
        if (notificationId <= 0) return;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Notification n = em.find(Notification.class, notificationId);
            if (n != null) {
                n.setRead(true);
                em.merge(n);
            }
            tx.commit();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi markAsRead Notification ID qua JPA: " + notificationId, e);
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 5: Đánh dấu TẤT CẢ thông báo của người dùng là Đã Đọc
     */
    public static void markAllAsRead(int recipientId) {
        if (recipientId <= 0) return;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("UPDATE Notification n SET n.isRead = TRUE WHERE n.recipientId = :recipientId")
              .setParameter("recipientId", recipientId)
              .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi markAllAsRead User ID qua JPA: " + recipientId, e);
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 6: Xóa một thông báo
     */
    public static boolean delete(int notificationId) {
        if (notificationId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Notification n = em.find(Notification.class, notificationId);
            if (n != null) {
                em.remove(n);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa Notification ID qua JPA: " + notificationId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }
}
