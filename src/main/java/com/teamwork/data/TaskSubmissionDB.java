package com.teamwork.data;

import com.teamwork.business.TaskSubmission;
import com.teamwork.business.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý dữ liệu Đợt nộp bàn giao (TaskSubmission) qua JPA / Hibernate.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn tính toàn vẹn dữ liệu và hỗ trợ đa phiên bản (Multi-revision)
 * - Tối ưu hóa truy vấn batch nạp theo projectId (tránh N+1 query khi mở bảng Kanban / Drawer)
 * - Tự động tính số phiên bản kế tiếp (revisionNumber = max + 1)
 */
public class TaskSubmissionDB {

    private static final Logger LOGGER = Logger.getLogger(TaskSubmissionDB.class.getName());

    /**
     * Nạp tên người nộp (submitterName) và người duyệt (reviewerName) an toàn từ User cache / DB.
     */
    private static void populateUserNames(EntityManager em, TaskSubmission sub) {
        if (sub == null) return;
        if (sub.getSubmittedBy() != null && sub.getSubmittedBy() > 0) {
            try {
                User u = em.find(User.class, sub.getSubmittedBy());
                sub.setSubmitterName(u != null && u.getFullName() != null && !u.getFullName().isEmpty()
                        ? u.getFullName()
                        : (u != null ? u.getUsername() : "Thành viên"));
            } catch (Exception e) {
                sub.setSubmitterName("Thành viên");
            }
        } else {
            sub.setSubmitterName("Thành viên");
        }

        if (sub.getReviewedBy() != null && sub.getReviewedBy() > 0) {
            try {
                User u = em.find(User.class, sub.getReviewedBy());
                sub.setReviewerName(u != null && u.getFullName() != null && !u.getFullName().isEmpty()
                        ? u.getFullName()
                        : (u != null ? u.getUsername() : "Trưởng Dự Án"));
            } catch (Exception e) {
                sub.setReviewerName("Trưởng Dự Án");
            }
        } else {
            sub.setReviewerName("Trưởng Dự Án");
        }
    }

    /**
     * HÀM 1: Thêm một đợt nộp bài mới vào CSDL.
     */
    public static boolean insert(TaskSubmission submission) {
        if (submission == null || submission.getTaskId() <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(submission);
            tx.commit();
            return true;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu TaskSubmission cho Task ID: " + submission.getTaskId(), e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 2: Tìm đợt nộp bài theo ID duy nhất.
     */
    public static TaskSubmission selectById(int id) {
        if (id <= 0) return null;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TaskSubmission sub = em.find(TaskSubmission.class, id);
            if (sub != null) {
                populateUserNames(em, sub);
            }
            return sub;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm TaskSubmission ID: " + id, e);
            return null;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 3: Lấy toàn bộ các lần nộp của MỘT Task, sắp xếp giảm dần theo revisionNumber DESC (đợt mới nhất lên đầu).
     */
    public static List<TaskSubmission> selectByTaskId(int taskId) {
        if (taskId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<TaskSubmission> list = em.createQuery(
                "SELECT s FROM TaskSubmission s WHERE s.taskId = :taskId ORDER BY s.revisionNumber DESC",
                TaskSubmission.class)
                .setParameter("taskId", taskId)
                .getResultList();
            for (TaskSubmission sub : list) {
                populateUserNames(em, sub);
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách TaskSubmission theo Task ID: " + taskId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 4: Lấy đợt nộp mới nhất của MỘT Task.
     */
    public static TaskSubmission selectLatestByTaskId(int taskId) {
        if (taskId <= 0) return null;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<TaskSubmission> list = em.createQuery(
                "SELECT s FROM TaskSubmission s WHERE s.taskId = :taskId ORDER BY s.revisionNumber DESC",
                TaskSubmission.class)
                .setParameter("taskId", taskId)
                .setMaxResults(1)
                .getResultList();
            if (!list.isEmpty()) {
                TaskSubmission latest = list.get(0);
                populateUserNames(em, latest);
                return latest;
            }
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy TaskSubmission mới nhất của Task ID: " + taskId, e);
            return null;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 5: BATCH LOAD - Lấy toàn bộ TaskSubmission của cả một Dự án trong 1 câu SQL duy nhất.
     */
    public static List<TaskSubmission> selectByProjectId(int projectId) {
        if (projectId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<TaskSubmission> list = em.createQuery(
                "SELECT s FROM TaskSubmission s JOIN s.task t WHERE t.projectId = :pid ORDER BY s.taskId ASC, s.revisionNumber DESC",
                TaskSubmission.class)
                .setParameter("pid", projectId)
                .getResultList();
            for (TaskSubmission sub : list) {
                populateUserNames(em, sub);
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi nạp batch TaskSubmission theo Project ID: " + projectId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 6: Tính số phiên bản tiếp theo cho một Task (revisionNumber = MAX + 1).
     */
    public static int getNextRevisionNumber(int taskId) {
        if (taskId <= 0) return 1;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            Integer maxRev = em.createQuery(
                "SELECT MAX(s.revisionNumber) FROM TaskSubmission s WHERE s.taskId = :taskId",
                Integer.class)
                .setParameter("taskId", taskId)
                .getSingleResult();
            return (maxRev != null ? maxRev : 0) + 1;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tính next revision number cho Task ID: " + taskId, e);
            return 1;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 7: Trưởng Dự Án (PM) cập nhật thẩm định / phản hồi cho một đợt nộp cụ thể.
     */
    public static boolean updateReview(int submissionId, String status, String pmFeedback, int qualityRating,
                                       Integer reviewerId, String reviewedAt) {
        if (submissionId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            TaskSubmission managed = em.find(TaskSubmission.class, submissionId);
            if (managed != null) {
                managed.setStatus(status != null ? status.trim().toUpperCase() : "SUBMITTED");
                managed.setPmFeedback(pmFeedback != null ? pmFeedback.trim() : "");
                managed.setQualityRating(qualityRating > 0 ? qualityRating : 0);
                managed.setReviewedBy(reviewerId);
                managed.setReviewedAt(reviewedAt != null ? reviewedAt.trim() : "");
                em.merge(managed);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật review cho TaskSubmission ID: " + submissionId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }
}
