package com.teamwork.data;

import com.teamwork.business.Message;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý Tin nhắn Chat & Bình luận Task qua JPA 3.1 / Hibernate.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng JPA EntityManager và JPQL chuẩn, tương thích cả PostgreSQL và MySQL
 */
public class MessageDB {

    private static final Logger LOGGER = Logger.getLogger(MessageDB.class.getName());

    /**
     * HÀM 1: Lấy danh sách tin nhắn CHAT CHUNG của MỘT DỰ ÁN (taskId == 0 hoặc null)
     */
    public static List<Message> selectByProjectId(int projectId) {
        if (projectId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Message> query = em.createQuery(
                "SELECT m FROM Message m WHERE m.projectId = :projectId AND m.taskId IS NULL ORDER BY m.id ASC",
                Message.class
            );
            query.setParameter("projectId", projectId);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy tin nhắn chat Project ID qua JPA: " + projectId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 1b: Lấy danh sách N tin nhắn CHAT CHUNG gần đây nhất theo thứ tự thời gian tăng dần
     */
    public static List<Message> selectRecentByProjectId(int projectId, int limit) {
        if (projectId <= 0) return new ArrayList<>();

        int safeLimit = limit > 0 ? limit : 50;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Message> query = em.createQuery(
                "SELECT m FROM Message m WHERE m.projectId = :projectId AND m.taskId IS NULL ORDER BY m.id DESC",
                Message.class
            );
            query.setParameter("projectId", projectId);
            query.setMaxResults(safeLimit);
            List<Message> list = new ArrayList<>(query.getResultList());
            Collections.reverse(list); // Đảo ngược để hiển thị theo thứ tự thời gian tăng dần (cũ trên, mới dưới)
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy tin nhắn recent Project ID qua JPA: " + projectId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 2: Lấy danh sách BÌNH LUẬN của MỘT CÔNG VIỆC CỤ THỂ (taskId > 0)
     */
    public static List<Message> selectByTaskId(int taskId) {
        if (taskId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Message> query = em.createQuery(
                "SELECT m FROM Message m WHERE m.taskId = :taskId ORDER BY m.id ASC",
                Message.class
            );
            query.setParameter("taskId", taskId);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy bình luận Task ID qua JPA: " + taskId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 3: Tìm một tin nhắn cụ thể theo ID
     */
    public static Message selectById(int id) {
        if (id <= 0) return null;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Message.class, id);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm tin nhắn ID qua JPA: " + id, e);
            return null;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 4: Thêm một tin nhắn mới
     */
    public static int insert(Message message) {
        if (message == null || message.getContent() == null || message.getContent().trim().isEmpty()) {
            return 0;
        }

        if (message.getAuthorName() == null || message.getAuthorName().trim().isEmpty()) {
            message.setAuthorName("Ẩn danh");
        }

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(message);
            tx.commit();
            return message.getId();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi gửi tin nhắn qua JPA", e);
            return 0;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 5: Đếm tổng số thảo luận của một Dự án
     */
    public static int countByProject(int projectId) {
        if (projectId <= 0) return 0;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(m) FROM Message m WHERE m.projectId = :projectId",
                Long.class
            )
            .setParameter("projectId", projectId)
            .getSingleResult();

            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi đếm tin nhắn Project ID qua JPA: " + projectId, e);
            return 0;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 6: Xóa một tin nhắn theo ID
     */
    public static boolean delete(int id) {
        if (id <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Message m = em.find(Message.class, id);
            if (m != null) {
                em.remove(m);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa tin nhắn ID qua JPA: " + id, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 6b: Cập nhật nội dung một tin nhắn (Sửa tin nhắn)
     */
    public static boolean update(int id, String content) {
        if (id <= 0 || content == null || content.trim().isEmpty()) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Message m = em.find(Message.class, id);
            if (m != null) {
                m.setContent(content.trim());
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật tin nhắn ID qua JPA: " + id, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 7: Xóa toàn bộ bình luận của một Task khi Task bị xóa
     */
    public static void deleteByTaskId(int taskId) {
        if (taskId <= 0) return;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM Message m WHERE m.taskId = :taskId")
              .setParameter("taskId", taskId)
              .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa bình luận theo Task ID qua JPA: " + taskId, e);
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM BATCH: Lấy toàn bộ bình luận của TẤT CẢ các Task trong một Dự Án trong 1 câu JPQL duy nhất!
     */
    public static List<Message> selectTaskCommentsByProjectId(int projectId) {
        if (projectId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Message> query = em.createQuery(
                "SELECT m FROM Message m WHERE m.projectId = :projectId AND m.taskId IS NOT NULL ORDER BY m.id ASC",
                Message.class
            );
            query.setParameter("projectId", projectId);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy batch Task Comments theo Project ID qua JPA: " + projectId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }
}
