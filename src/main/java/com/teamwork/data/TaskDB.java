package com.teamwork.data;

import com.teamwork.business.Task;
import com.teamwork.business.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý dữ liệu Thẻ công việc (Task) qua Jakarta Persistence (JPA) / Hibernate.
 * 
 * - Sử dụng EntityManager và JPQL chuẩn hóa, hoạt động độc lập với RDBMS.
 * - Quản lý trạng thái Kanban và Quality Gate 2 tầng an toàn giao dịch.
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types) cho các Servlet.
 */
public class TaskDB {

    private static final Logger LOGGER = Logger.getLogger(TaskDB.class.getName());

    /**
     * Hàm phụ trợ lấy tên người phụ trách (assigneeName) an toàn qua JPA
     */
    private static void populateAssigneeName(EntityManager em, Task task) {
        if (task == null) return;
        if (task.getAssigneeId() > 0) {
            try {
                User u = em.find(User.class, task.getAssigneeId());
                task.setAssigneeName(u != null && u.getFullName() != null && !u.getFullName().isEmpty() 
                    ? u.getFullName() 
                    : (u != null ? u.getUsername() : "Chưa phân công"));
            } catch (Exception e) {
                task.setAssigneeName("Chưa phân công");
            }
        } else {
            task.setAssigneeName("Chưa phân công");
        }
    }

    /**
     * HÀM 1: Lấy toàn bộ task trong hệ thống
     */
    public static List<Task> selectAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Task> list = em.createQuery("SELECT t FROM Task t ORDER BY t.id ASC", Task.class).getResultList();
            for (Task t : list) {
                populateAssigneeName(em, t);
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy toàn bộ Task qua JPA", e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 2: Lấy danh sách tất cả các task thuộc về MỘT DỰ ÁN cụ thể
     */
    public static List<Task> selectByProjectId(int projectId) {
        if (projectId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Task> list = em.createQuery("SELECT t FROM Task t WHERE t.projectId = :pid ORDER BY t.id ASC", Task.class)
                .setParameter("pid", projectId)
                .getResultList();
            for (Task t : list) {
                populateAssigneeName(em, t);
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy Task theo Project ID qua JPA: " + projectId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 3: Lấy danh sách task của một dự án ĐƯỢC LỌC THEO 3 CỘT KANBAN
     */
    public static List<Task> selectByProjectAndStatus(int projectId, String status) {
        if (projectId <= 0 || status == null) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql;
            if ("TODO".equalsIgnoreCase(status)) {
                jpql = "SELECT t FROM Task t WHERE t.projectId = :pid AND t.status = 'TODO' ORDER BY t.id ASC";
            } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                jpql = "SELECT t FROM Task t WHERE t.projectId = :pid AND t.status IN ('PLANNING', 'IN_PROGRESS', 'SUBMITTED', 'REVISE', 'REJECTED') ORDER BY t.id ASC";
            } else if ("DONE".equalsIgnoreCase(status)) {
                jpql = "SELECT t FROM Task t WHERE t.projectId = :pid AND t.status IN ('DONE', 'APPROVED') ORDER BY t.id ASC";
            } else {
                jpql = "SELECT t FROM Task t WHERE t.projectId = :pid AND UPPER(t.status) = :st ORDER BY t.id ASC";
            }

            var query = em.createQuery(jpql, Task.class).setParameter("pid", projectId);
            if (!"TODO".equalsIgnoreCase(status) && !"IN_PROGRESS".equalsIgnoreCase(status) && !"DONE".equalsIgnoreCase(status)) {
                query.setParameter("st", status.trim().toUpperCase());
            }

            List<Task> list = query.getResultList();
            for (Task t : list) {
                populateAssigneeName(em, t);
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lọc Task theo Status qua JPA", e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 4: Tìm task theo ID duy nhất
     */
    public static Task selectById(int id) {
        if (id <= 0) return null;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            Task task = em.find(Task.class, id);
            if (task != null) {
                populateAssigneeName(em, task);
            }
            return task;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm Task ID qua JPA: " + id, e);
            return null;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 5: Thêm task mới qua em.persist()
     */
    public static int insert(Task task) {
        if (task == null || task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return 0;
        }

        if (task.getStatus() == null || task.getStatus().trim().isEmpty()) task.setStatus("TODO");
        if (task.getPriority() == null || task.getPriority().trim().isEmpty()) task.setPriority("MEDIUM");
        if (task.getDescription() == null) task.setDescription("");
        if (task.getLabels() == null) task.setLabels("");
        if (task.getFinalDeliverableNote() == null) task.setFinalDeliverableNote("");
        if (task.getPmFeedback() == null) task.setPmFeedback("");
        if (task.getDeliverableFile() == null) task.setDeliverableFile("");
        if (task.getPlanningNote() == null) task.setPlanningNote("");
        if (task.getQualityRating() <= 0) task.setQualityRating(5);

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(task);
            tx.commit();
            return task.getId();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi thêm Task mới qua JPA: " + task.getTitle(), e);
            return 0;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 6: Cập nhật trạng thái Task (kéo thả Kanban)
     */
    public static boolean updateStatus(int id, String newStatus) {
        if (id <= 0 || newStatus == null || newStatus.trim().isEmpty()) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Task t = em.find(Task.class, id);
            if (t != null) {
                t.setStatus(newStatus.trim().toUpperCase());
                em.merge(t);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật status Task ID qua JPA: " + id, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 7: Task Lead Bàn Giao & Nộp Báo Cáo Task Kèm Tệp Đính Kèm ➔ SUBMITTED
     */
    public static boolean submitTaskDeliverable(int taskId, String note, String deliverableFile, String submittedAt) {
        if (taskId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Task t = em.find(Task.class, taskId);
            if (t != null) {
                t.setStatus("SUBMITTED");
                t.setFinalDeliverableNote(note != null ? note.trim() : "");
                t.setDeliverableFile(deliverableFile != null ? deliverableFile.trim() : "");
                em.merge(t);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi submit deliverable Task ID qua JPA: " + taskId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    public static boolean submitTaskDeliverable(int taskId, String note, String submittedAt) {
        return submitTaskDeliverable(taskId, note, "", submittedAt);
    }

    /**
     * HÀM 8: Trưởng Dự Án (PM) Phê Duyệt Nghiệm Thu ĐẠT Kèm Đánh Giá Sao ➔ DONE (100%)
     */
    public static boolean pmApproveTask(int taskId, String feedback, int qualityRating, String reviewedAt) {
        if (taskId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Task t = em.find(Task.class, taskId);
            if (t != null) {
                t.setStatus("DONE");
                t.setPmFeedback(feedback != null ? feedback.trim() : "PM đã phê duyệt nghiệm thu xuất sắc!");
                t.setQualityRating(qualityRating > 0 ? qualityRating : 5);
                em.merge(t);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi PM duyệt Task ID qua JPA: " + taskId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    public static boolean pmApproveTask(int taskId, String feedback, String reviewedAt) {
        return pmApproveTask(taskId, feedback, 5, reviewedAt);
    }

    /**
     * HÀM 9: Trưởng Dự Án (PM) Yêu Cầu Cân Chỉnh Nhỏ ➔ REVISE
     */
    public static boolean pmReviseTask(int taskId, String feedback, String reviewedAt) {
        if (taskId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Task t = em.find(Task.class, taskId);
            if (t != null) {
                t.setStatus("REVISE");
                t.setPmFeedback(feedback != null ? feedback.trim() : "");
                em.merge(t);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi PM yêu cầu revise Task ID qua JPA: " + taskId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 10: Trưởng Dự Án (PM) Trả Về Do Chưa Đạt ➔ REJECTED
     */
    public static boolean pmRejectTask(int taskId, String feedback, String reviewedAt) {
        if (taskId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Task t = em.find(Task.class, taskId);
            if (t != null) {
                t.setStatus("REVISE");
                t.setStatus("REJECTED");
                t.setPmFeedback(feedback != null ? feedback.trim() : "");
                em.merge(t);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi PM reject Task ID qua JPA: " + taskId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 11: Task Lead Trình Kế Hoạch Phân Rã (CỔNG 1) ➔ PLANNING
     */
    public static boolean submitPlanningRequest(int taskId, String planningNote, String submittedAt) {
        if (taskId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Task t = em.find(Task.class, taskId);
            if (t != null) {
                t.setStatus("PLANNING");
                t.setPlanningNote(planningNote != null ? planningNote.trim() : "");
                em.merge(t);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi submit planning Task ID qua JPA: " + taskId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 12: Trưởng Dự Án (PM) Phê Duyệt Kế Hoạch & KHÓA PHÂN RÃ ➔ IN_PROGRESS
     */
    public static boolean pmApprovePlanning(int taskId, String pmFeedback, String reviewedAt) {
        if (taskId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Task t = em.find(Task.class, taskId);
            if (t != null) {
                t.setStatus("IN_PROGRESS");
                t.setPmFeedback(pmFeedback != null ? pmFeedback.trim() : "");
                em.merge(t);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi PM approve planning Task ID qua JPA: " + taskId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 13: Trưởng Dự Án (PM) Yêu Cầu Chỉnh Sửa Kế Hoạch Phân Rã ➔ TODO
     */
    public static boolean pmRejectPlanning(int taskId, String pmFeedback, String reviewedAt) {
        if (taskId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Task t = em.find(Task.class, taskId);
            if (t != null) {
                t.setStatus("TODO");
                t.setPmFeedback(pmFeedback != null ? pmFeedback.trim() : "");
                em.merge(t);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi PM reject planning Task ID qua JPA: " + taskId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 14: Cập nhật thông tin toàn diện của Task qua em.merge()
     */
    public static boolean update(Task updatedTask) {
        if (updatedTask == null || updatedTask.getId() <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Task managed = em.find(Task.class, updatedTask.getId());
            if (managed != null) {
                managed.setTitle(updatedTask.getTitle() != null ? updatedTask.getTitle().trim() : "");
                managed.setDescription(updatedTask.getDescription() != null ? updatedTask.getDescription().trim() : "");
                managed.setStatus(updatedTask.getStatus() != null ? updatedTask.getStatus().trim().toUpperCase() : "TODO");
                managed.setPriority(updatedTask.getPriority() != null ? updatedTask.getPriority().trim().toUpperCase() : "MEDIUM");
                managed.setDueDate(updatedTask.getDueDate());
                managed.setAssigneeId(updatedTask.getAssigneeId());
                managed.setLabels(updatedTask.getLabels() != null ? updatedTask.getLabels().trim() : "");
                managed.setRequiresGate(updatedTask.isRequiresGate());
                em.merge(managed);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi update Task ID qua JPA: " + updatedTask.getId(), e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Cập nhật riêng cờ requires_gate cho Task
     */
    public static boolean updateRequiresGate(int taskId, boolean requiresGate) {
        if (taskId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Task t = em.find(Task.class, taskId);
            if (t != null) {
                t.setRequiresGate(requiresGate);
                em.merge(t);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi update requires_gate Task ID qua JPA: " + taskId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 15: Xóa task theo ID qua em.remove()
     */
    public static boolean delete(int id) {
        if (id <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Task t = em.find(Task.class, id);
            if (t != null) {
                em.remove(t);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa Task ID qua JPA: " + id, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 16: Hủy phân công Task lớn cho một thành viên khi rời nhóm
     */
    public static void unassignUserFromProject(int projectId, int userId) {
        if (projectId <= 0 || userId <= 0) return;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("UPDATE Task t SET t.assigneeId = 0 WHERE t.projectId = :pid AND t.assigneeId = :uid")
                .setParameter("pid", projectId)
                .setParameter("uid", userId)
                .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi unassign user qua JPA", e);
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * HÀM 17: Đồng bộ tên người phụ trách (tự động xử lý qua JPA)
     */
    public static void syncAssigneeName(int userId, String newFullName) {
        // Tự động giải quyết qua relationship User -> fullName trong JPA
    }

    /**
     * HÀM 18: Tự động mở khóa toàn bộ công việc khi chuyển dự án sang chế độ Cá Nhân (Solo)
     */
    public static void unlockAllTasksForSolo(int projectId, int ownerId) {
        if (projectId <= 0) return;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("UPDATE Task t SET t.requiresGate = false, t.assigneeId = :oid WHERE t.projectId = :pid")
                .setParameter("oid", ownerId)
                .setParameter("pid", projectId)
                .executeUpdate();

            em.createQuery("UPDATE Task t SET t.status = 'IN_PROGRESS' WHERE t.projectId = :pid AND t.status IN ('PLANNING', 'SUBMITTED')")
                .setParameter("pid", projectId)
                .executeUpdate();

            tx.commit();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi mở khóa tasks cho chế độ Solo qua JPA", e);
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }
}