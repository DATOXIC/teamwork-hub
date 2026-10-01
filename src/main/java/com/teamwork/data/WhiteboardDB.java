package com.teamwork.data;

import com.teamwork.business.Whiteboard;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO quản lý Bảng vẽ (Whiteboard) của dự án qua JPA. Mỗi dự án có tối đa một bảng vẽ.
 */
public class WhiteboardDB {

    private static final Logger LOGGER = Logger.getLogger(WhiteboardDB.class.getName());

    /** Lấy nội dung JSON bảng vẽ của dự án; chuỗi rỗng nếu chưa có. */
    public static String selectContentByProjectId(int projectId) {
        if (projectId <= 0) return "";
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Whiteboard> list = em.createQuery(
                    "SELECT w FROM Whiteboard w WHERE w.projectId = :pid", Whiteboard.class)
                    .setParameter("pid", projectId)
                    .setMaxResults(1)
                    .getResultList();
            return (list.isEmpty() || list.get(0).getContent() == null) ? "" : list.get(0).getContent();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi đọc Whiteboard của dự án " + projectId, e);
            return "";
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /** Lưu (tạo mới hoặc cập nhật) bảng vẽ của dự án. */
    public static boolean save(int projectId, String content, int userId) {
        if (projectId <= 0 || content == null) return false;
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            List<Whiteboard> list = em.createQuery(
                    "SELECT w FROM Whiteboard w WHERE w.projectId = :pid", Whiteboard.class)
                    .setParameter("pid", projectId)
                    .setMaxResults(1)
                    .getResultList();
            Whiteboard w;
            if (list.isEmpty()) {
                w = new Whiteboard();
                w.setProjectId(projectId);
                w.setContent(content);
                w.setUpdatedBy(userId);
                em.persist(w);
            } else {
                w = list.get(0);
                w.setContent(content);
                w.setUpdatedBy(userId);
            }
            tx.commit();
            return true;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu Whiteboard của dự án " + projectId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }
}
