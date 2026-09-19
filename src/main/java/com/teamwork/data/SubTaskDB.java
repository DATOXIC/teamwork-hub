package com.teamwork.data;

import com.teamwork.business.SubTask;
import com.teamwork.business.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý dữ liệu Việc Con (SubTask) & Quy Trình Nghiệm Thu 5 Trạng Thái qua JPA 3.1 / Hibernate.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng JPA EntityManager và JPQL chuẩn, tương thích cả PostgreSQL và MySQL
 * - Tự động nạp tên người phụ trách (assigneeName) an toàn
 */
public class SubTaskDB {

    private static final Logger LOGGER = Logger.getLogger(SubTaskDB.class.getName());

    private static void populateAssigneeName(EntityManager em, SubTask st) {
        if (st == null) return;
        if (st.getAssigneeId() > 0) {
            User u = em.find(User.class, st.getAssigneeId());
            if (u != null && u.getFullName() != null && !u.getFullName().trim().isEmpty()) {
                st.setAssigneeName(u.getFullName().trim());
            } else {
                st.setAssigneeName("Chưa phân công");
            }
        } else {
            st.setAssigneeName("Chưa phân công");
        }
    }

    /**
     * HÀM 1: Lấy danh sách tất cả các việc con của MỘT TASK CHA CỤ THỂ
     */
    public static List<SubTask> selectByTaskId(int taskId) {
        if (taskId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<SubTask> query = em.createQuery(
                "SELECT st FROM SubTask st WHERE st.taskId = :taskId ORDER BY st.id ASC",
                SubTask.class
            );
            query.setParameter("taskId", taskId);
            List<SubTask> list = query.getResultList();
            for (SubTask st : list) {
                populateAssigneeName(em, st);
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy SubTask theo Task ID qua JPA: " + taskId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 2: Tìm một việc con theo ID
     */
    public static SubTask selectById(int id) {
        if (id <= 0) return null;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            SubTask st = em.find(SubTask.class, id);
            if (st != null) {
                populateAssigneeName(em, st);
            }
            return st;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm SubTask ID qua JPA: " + id, e);
            return null;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 3: Thêm một việc con mới và phân công cho thành viên
     */
    public static int insert(SubTask subTask) {
        if (subTask == null || subTask.getTitle() == null || subTask.getTitle().trim().isEmpty()) {
            return 0;
        }

        if (subTask.getStatus() == null || subTask.getStatus().trim().isEmpty()) {
            subTask.setStatus("TODO");
        }
        if (subTask.getDueDate() == null) subTask.setDueDate("");
        if (subTask.getSubmissionNote() == null) subTask.setSubmissionNote("");
        if (subTask.getFeedbackNote() == null) subTask.setFeedbackNote("");

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(subTask);
            tx.commit();
            return subTask.getId();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi thêm SubTask mới qua JPA: " + subTask.getTitle(), e);
            return 0;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 4: Cấp dưới Nộp Báo Cáo Kết Quả ➔ SUBMITTED
     */
    public static boolean submitDeliverable(int subTaskId, String submissionNote, String submittedAt) {
        if (subTaskId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            SubTask st = em.find(SubTask.class, subTaskId);
            if (st != null) {
                st.setStatus("SUBMITTED");
                st.setSubmissionNote(submissionNote != null ? submissionNote.trim() : "");
                em.merge(st);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi submit deliverable SubTask ID qua JPA: " + subTaskId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 5: Task Lead Duyệt Nghiệm Thu Đạt ➔ APPROVED
     */
    public static boolean approveDeliverable(int subTaskId, String reviewedAt) {
        if (subTaskId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            SubTask st = em.find(SubTask.class, subTaskId);
            if (st != null) {
                st.setStatus("APPROVED");
                st.setCompleted(true);
                em.merge(st);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi approve deliverable SubTask ID qua JPA: " + subTaskId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 6: Task Lead Yêu Cầu Cân Chỉnh Nhỏ ➔ REVISE
     */
    public static boolean reviseDeliverable(int subTaskId, String feedbackNote, String reviewedAt) {
        if (subTaskId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            SubTask st = em.find(SubTask.class, subTaskId);
            if (st != null) {
                st.setStatus("REVISE");
                st.setFeedbackNote(feedbackNote != null ? feedbackNote.trim() : "");
                em.merge(st);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi revise deliverable SubTask ID qua JPA: " + subTaskId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 7: Task Lead Trả Về Do Chưa Đạt Yêu Cầu ➔ REJECTED
     */
    public static boolean rejectDeliverable(int subTaskId, String feedbackNote, String reviewedAt) {
        if (subTaskId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            SubTask st = em.find(SubTask.class, subTaskId);
            if (st != null) {
                st.setStatus("REJECTED");
                st.setFeedbackNote(feedbackNote != null ? feedbackNote.trim() : "");
                em.merge(st);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi reject deliverable SubTask ID qua JPA: " + subTaskId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 8b: Ghi trực tiếp một trạng thái đích cho việc con (dùng cho ô tick trên bảng).
     * - Chế độ Quality Gate  : ghi "SUBMITTED" — người làm đã nộp, CHỜ Task Lead nghiệm thu.
     * - Chế độ Fast-track/Solo: ghi "DONE"      — xong hẳn, không ai phải duyệt.
     * KHÔNG gọi setCompleted() ở đây: setStatus() đã tự suy ra cờ completed, còn
     * setCompleted(true) sẽ ghi đè "SUBMITTED" thành "DONE" (xem SubTask.setCompleted).
     */
    public static boolean updateStatus(int id, String targetStatus) {
        if (id <= 0 || targetStatus == null || targetStatus.trim().isEmpty()) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            SubTask st = em.find(SubTask.class, id);
            if (st != null) {
                st.setStatus(targetStatus);
                em.merge(st);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi ghi status [" + targetStatus + "] cho SubTask ID qua JPA: " + id, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 9: Xóa một việc con theo ID
     */
    public static boolean delete(int id) {
        if (id <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            SubTask st = em.find(SubTask.class, id);
            if (st != null) {
                em.remove(st);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa SubTask ID qua JPA: " + id, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 10: Xóa toàn bộ các việc con thuộc một Task cha (Cascade Delete)
     */
    public static void deleteByTaskId(int taskId) {
        if (taskId <= 0) return;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM SubTask st WHERE st.taskId = :taskId")
              .setParameter("taskId", taskId)
              .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa SubTasks theo Task ID qua JPA: " + taskId, e);
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 11: Tính toán % tiến độ hoàn thành dựa trên các việc con ĐÃ HOÀN THÀNH (DONE hoặc APPROVED)
     * - DONE = Assignee đánh dấu đã làm xong (chưa qua kiểm duyệt)
     * - APPROVED = Task Lead đã thẩm định và duyệt ĐẠT
     */
    public static int calculateProgress(int taskId) {
        if (taskId <= 0) return 0;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long total = em.createQuery(
                "SELECT COUNT(st) FROM SubTask st WHERE st.taskId = :taskId", Long.class)
                .setParameter("taskId", taskId)
                .getSingleResult();

            if (total == null || total == 0) return 0;

            Long doneCount = em.createQuery(
                "SELECT COUNT(st) FROM SubTask st WHERE st.taskId = :taskId AND st.status IN ('DONE', 'APPROVED')", Long.class)
                .setParameter("taskId", taskId)
                .getSingleResult();

            long doneVal = doneCount != null ? doneCount : 0;
            return (int) Math.round(((double) doneVal / total) * 100);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tính tiến độ SubTask qua JPA", e);
            return 0;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 11b: Kiểm tra xem TẤT CẢ việc con đã được Task Lead DUYỆT NGHIỆM THU (APPROVED) hay chưa.
     * Dùng cho chế độ Quality Gate: Task cha chỉ auto-complete khi mọi subtask đều APPROVED.
     */
    public static boolean areAllSubtasksApproved(int taskId) {
        if (taskId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long total = em.createQuery(
                "SELECT COUNT(st) FROM SubTask st WHERE st.taskId = :taskId", Long.class)
                .setParameter("taskId", taskId)
                .getSingleResult();

            if (total == null || total == 0) return false;

            Long approvedCount = em.createQuery(
                "SELECT COUNT(st) FROM SubTask st WHERE st.taskId = :taskId AND st.status = 'APPROVED'", Long.class)
                .setParameter("taskId", taskId)
                .getSingleResult();

            return approvedCount != null && approvedCount.equals(total);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi kiểm tra trạng thái duyệt SubTask qua JPA", e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 12: Cập nhật thông tin chi tiết của một việc con
     */
    public static boolean update(SubTask updatedSubTask) {
        if (updatedSubTask == null || updatedSubTask.getId() <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            SubTask existing = em.find(SubTask.class, updatedSubTask.getId());
            if (existing != null) {
                existing.setTitle(updatedSubTask.getTitle() != null ? updatedSubTask.getTitle().trim() : "");
                existing.setAssigneeId(updatedSubTask.getAssigneeId());
                existing.setStatus(updatedSubTask.getStatus() != null ? updatedSubTask.getStatus().trim().toUpperCase() : "TODO");
                existing.setDueDate(updatedSubTask.getDueDate() != null ? updatedSubTask.getDueDate().trim() : "");
                em.merge(existing);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi update SubTask ID qua JPA: " + updatedSubTask.getId(), e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 13: Hủy phân công Việc Con cho một thành viên khi rời nhóm
     */
    public static void unassignUserFromProject(int projectId, int userId) {
        if (projectId <= 0 || userId <= 0) return;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery(
                "UPDATE SubTask st SET st.assigneeId = NULL " +
                "WHERE st.assigneeId = :userId AND st.taskId IN (SELECT t.id FROM Task t WHERE t.projectId = :projectId)"
            )
            .setParameter("userId", userId)
            .setParameter("projectId", projectId)
            .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi unassign user trong subtasks qua JPA", e);
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 14: Đồng bộ tên người phụ trách mới
     */
    public static void syncAssigneeName(int userId, String newFullName) {
        // Tự động đồng bộ qua JOIN User trong JPA
    }

    /**
     * HÀM BATCH: Lấy toàn bộ subtasks của TẤT CẢ các task trong một Project trong 1 câu JPQL duy nhất!
     */
    public static List<SubTask> selectByProjectId(int projectId) {
        if (projectId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<SubTask> query = em.createQuery(
                "SELECT st FROM SubTask st JOIN Task t ON st.taskId = t.id " +
                "WHERE t.projectId = :projectId ORDER BY st.id ASC",
                SubTask.class
            );
            query.setParameter("projectId", projectId);
            List<SubTask> list = query.getResultList();
            for (SubTask st : list) {
                populateAssigneeName(em, st);
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy batch SubTasks theo Project ID qua JPA: " + projectId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }
}

