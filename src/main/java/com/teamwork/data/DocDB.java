package com.teamwork.data;

import com.teamwork.business.Doc;
import com.teamwork.business.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý Tài liệu & Ghi chú Wiki qua JPA 3.1 / Hibernate.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng JPA EntityManager và JPQL chuẩn, tương thích cả PostgreSQL và MySQL
 * - Tự động nạp tên tác giả (authorName) an toàn
 */
public class DocDB {

    private static final Logger LOGGER = Logger.getLogger(DocDB.class.getName());

    private static void populateAuthorName(EntityManager em, Doc d) {
        if (d == null) return;
        if (d.getAuthorId() > 0) {
            User u = em.find(User.class, d.getAuthorId());
            if (u != null && u.getFullName() != null && !u.getFullName().trim().isEmpty()) {
                d.setAuthorName(u.getFullName().trim());
            } else {
                d.setAuthorName("Ẩn danh");
            }
        } else {
            d.setAuthorName("Ẩn danh");
        }
    }

    /**
     * HÀM 1: Lấy danh sách tất cả các bài viết trong hệ thống
     */
    public static List<Doc> selectAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Doc> query = em.createQuery(
                "SELECT d FROM Doc d ORDER BY d.id ASC",
                Doc.class
            );
            List<Doc> list = query.getResultList();
            for (Doc d : list) {
                populateAuthorName(em, d);
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy toàn bộ Doc qua JPA", e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 2: Lấy danh sách toàn bộ tài liệu thuộc về MỘT DỰ ÁN cụ thể
     */
    public static List<Doc> selectByProjectId(int projectId) {
        if (projectId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Doc> query = em.createQuery(
                "SELECT d FROM Doc d WHERE d.projectId = :projectId ORDER BY d.id ASC",
                Doc.class
            );
            query.setParameter("projectId", projectId);
            List<Doc> list = query.getResultList();
            for (Doc d : list) {
                populateAuthorName(em, d);
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy Doc theo Project ID qua JPA: " + projectId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 3: Tìm một tài liệu cụ thể theo ID
     */
    public static Doc selectById(int id) {
        if (id <= 0) return null;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            Doc d = em.find(Doc.class, id);
            if (d != null) {
                populateAuthorName(em, d);
            }
            return d;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm Doc ID qua JPA: " + id, e);
            return null;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 4: Thêm một bài viết tài liệu mới
     */
    public static int insert(Doc doc) {
        if (doc == null || doc.getTitle() == null || doc.getTitle().trim().isEmpty()) {
            return 0;
        }

        if (doc.getContent() == null) doc.setContent("");

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(doc);
            tx.commit();
            return doc.getId();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi thêm Doc mới qua JPA: " + doc.getTitle(), e);
            return 0;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 5: Cập nhật nội dung bài viết tài liệu
     */
    public static boolean update(Doc updatedDoc) {
        if (updatedDoc == null || updatedDoc.getId() <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Doc existing = em.find(Doc.class, updatedDoc.getId());
            if (existing != null) {
                existing.setTitle(updatedDoc.getTitle() != null ? updatedDoc.getTitle().trim() : "");
                existing.setContent(updatedDoc.getContent() != null ? updatedDoc.getContent().trim() : "");
                em.merge(existing);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi update Doc ID qua JPA: " + updatedDoc.getId(), e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 6: Xóa một bài viết theo ID
     */
    public static boolean delete(int id) {
        if (id <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Doc doc = em.find(Doc.class, id);
            if (doc != null) {
                em.remove(doc);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa Doc ID qua JPA: " + id, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 7: Đếm tổng số tài liệu của một Dự án
     */
    public static int countByProject(int projectId) {
        if (projectId <= 0) return 0;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(d) FROM Doc d WHERE d.projectId = :projectId",
                Long.class
            )
            .setParameter("projectId", projectId)
            .getSingleResult();

            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi đếm Doc Project ID qua JPA: " + projectId, e);
            return 0;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }
}
