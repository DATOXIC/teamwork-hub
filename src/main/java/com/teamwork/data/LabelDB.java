package com.teamwork.data;

import com.teamwork.business.Label;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý dữ liệu Nhãn phân loại (Label) qua JPA 3.1 / Hibernate.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng JPA EntityManager và JPQL chuẩn, tương thích cả PostgreSQL và MySQL
 */
public class LabelDB {

    private static final Logger LOGGER = Logger.getLogger(LabelDB.class.getName());

    /**
     * Nghiệp vụ 1: Lấy tất cả nhãn thuộc về một dự án cụ thể
     */
    public static List<Label> selectByProjectId(int projectId) {
        if (projectId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Label> query = em.createQuery(
                "SELECT l FROM Label l WHERE l.projectId = :projectId ORDER BY l.id ASC",
                Label.class
            );
            query.setParameter("projectId", projectId);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách nhãn Project ID qua JPA: " + projectId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Nghiệp vụ 2: Tìm nhãn theo ID
     */
    public static Label selectById(int id) {
        if (id <= 0) return null;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Label.class, id);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm nhãn ID qua JPA: " + id, e);
            return null;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Nghiệp vụ 3: Kiểm tra tên nhãn đã tồn tại trong dự án chưa
     */
    public static boolean existsByName(int projectId, String name, int excludeId) {
        if (name == null || name.trim().isEmpty() || projectId <= 0) {
            return false;
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(l) FROM Label l WHERE l.projectId = :projectId AND LOWER(l.name) = LOWER(:name) AND l.id <> :excludeId",
                Long.class
            )
            .setParameter("projectId", projectId)
            .setParameter("name", name.trim())
            .setParameter("excludeId", excludeId)
            .getSingleResult();

            return count != null && count > 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi kiểm tra trùng tên nhãn qua JPA", e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Nghiệp vụ 4: Thêm một nhãn mới vào dự án
     */
    public static int insert(Label label) {
        if (label == null || label.getProjectId() <= 0 || label.getName() == null || label.getName().trim().isEmpty()) {
            return 0;
        }

        if (label.getColorKey() == null || label.getColorKey().trim().isEmpty()) {
            label.setColorKey("blue");
        }
        if (label.getIcon() == null || label.getIcon().trim().isEmpty()) {
            label.setIcon("bi-tag-fill");
        }

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(label);
            tx.commit();
            return label.getId();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi thêm nhãn mới qua JPA: " + label.getName(), e);
            return 0;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Nghiệp vụ 5: Cập nhật thông tin nhãn
     */
    public static boolean update(Label updatedLabel) {
        if (updatedLabel == null || updatedLabel.getId() <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Label existing = em.find(Label.class, updatedLabel.getId());
            if (existing != null) {
                existing.setName(updatedLabel.getName() != null ? updatedLabel.getName().trim() : "");
                existing.setColorKey(updatedLabel.getColorKey() != null ? updatedLabel.getColorKey().trim().toLowerCase() : "blue");
                existing.setIcon(updatedLabel.getIcon() != null ? updatedLabel.getIcon().trim() : "bi-tag-fill");
                em.merge(existing);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi update nhãn ID qua JPA: " + updatedLabel.getId(), e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Nghiệp vụ 6: Xóa nhãn theo ID
     */
    public static boolean delete(int id) {
        if (id <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Label label = em.find(Label.class, id);
            if (label != null) {
                em.remove(label);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa nhãn ID qua JPA: " + id, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }
}