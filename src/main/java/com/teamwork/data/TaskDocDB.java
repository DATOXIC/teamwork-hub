package com.teamwork.data;

import com.teamwork.business.TaskDoc;
import com.teamwork.business.TaskDocId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý liên kết Nhiều-Nhiều Task ↔ Doc sử dụng JPA / Hibernate ORM.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng JOIN FETCH bảng Doc để lấy tiêu đề tài liệu (docTitle) nhanh chóng, tránh N+1 query
 * - Sử dụng JPQL chuẩn, tương thích cả PostgreSQL (Cloud) lẫn MySQL (Local)
 * - Quản lý Transaction an toàn và giải phóng EntityManager trong finally
 */
public class TaskDocDB {

    private static final Logger LOGGER = Logger.getLogger(TaskDocDB.class.getName());

    /**
     * HÀM 1: Lấy danh sách tất cả các tài liệu đính kèm của MỘT CÔNG VIỆC (Task)
     */
    public static List<TaskDoc> selectByTaskId(int taskId) {
        if (taskId <= 0) return new ArrayList<>();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<TaskDoc> list = em.createQuery(
                "SELECT td FROM TaskDoc td LEFT JOIN FETCH td.doc WHERE td.taskId = :taskId ORDER BY td.docId ASC",
                TaskDoc.class)
                .setParameter("taskId", taskId)
                .getResultList();
            for (TaskDoc td : list) {
                if (td.getDoc() != null) {
                    td.setDocTitle(td.getDoc().getTitle());
                }
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy TaskDoc theo Task ID: " + taskId, e);
            return new ArrayList<>();
        } finally {
            em.close();
        }
    }

    /**
     * HÀM 2: Lấy danh sách ID của các Task đang tham chiếu đến MỘT TÀI LIỆU (Doc)
     */
    public static List<Integer> selectTaskIdsByDocId(int docId) {
        if (docId <= 0) return new ArrayList<>();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT td.taskId FROM TaskDoc td WHERE td.docId = :docId ORDER BY td.taskId ASC",
                Integer.class)
                .setParameter("docId", docId)
                .getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy taskIds theo Doc ID: " + docId, e);
            return new ArrayList<>();
        } finally {
            em.close();
        }
    }

    /**
     * HÀM 3: Thêm một liên kết mới giữa Task và Doc
     */
    public static boolean insert(int taskId, int docId, String docTitle) {
        if (taskId <= 0 || docId <= 0) return false;
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            TaskDoc existing = em.find(TaskDoc.class, new TaskDocId(taskId, docId));
            if (existing != null) {
                tx.rollback();
                return false;
            }
            TaskDoc td = new TaskDoc(taskId, docId, docTitle);
            em.persist(td);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            LOGGER.log(Level.SEVERE, "Lỗi khi chèn TaskDoc", e);
            return false;
        } finally {
            em.close();
        }
    }

    /**
     * HÀM 4: Xóa toàn bộ liên kết đính kèm của một Task
     */
    public static void deleteByTaskId(int taskId) {
        if (taskId <= 0) return;
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM TaskDoc td WHERE td.taskId = :taskId")
              .setParameter("taskId", taskId)
              .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa TaskDoc theo Task ID: " + taskId, e);
        } finally {
            em.close();
        }
    }

    /**
     * HÀM 5: Xóa toàn bộ liên kết của một Doc
     */
    public static void deleteByDocId(int docId) {
        if (docId <= 0) return;
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM TaskDoc td WHERE td.docId = :docId")
              .setParameter("docId", docId)
              .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa TaskDoc theo Doc ID: " + docId, e);
        } finally {
            em.close();
        }
    }

    /**
     * HÀM BATCH: Lấy toàn bộ TaskDoc của TẤT CẢ các Task trong một Dự Án trong 1 câu JPQL duy nhất!
     * Giúp loại bỏ N+1 query problem, tăng tốc độ tải trang gấp nhiều lần.
     */
    public static List<TaskDoc> selectByProjectId(int projectId) {
        if (projectId <= 0) return new ArrayList<>();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<TaskDoc> list = em.createQuery(
                "SELECT td FROM TaskDoc td LEFT JOIN FETCH td.doc d JOIN td.task t WHERE t.projectId = :projectId ORDER BY d.id ASC",
                TaskDoc.class)
                .setParameter("projectId", projectId)
                .getResultList();
            for (TaskDoc td : list) {
                if (td.getDoc() != null) {
                    td.setDocTitle(td.getDoc().getTitle());
                }
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy batch TaskDoc theo Project ID: " + projectId, e);
            return new ArrayList<>();
        } finally {
            em.close();
        }
    }
}
