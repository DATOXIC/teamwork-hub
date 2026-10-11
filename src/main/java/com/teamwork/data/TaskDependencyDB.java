package com.teamwork.data;

import com.teamwork.business.Task;
import com.teamwork.business.TaskDependency;
import com.teamwork.business.TaskStatus;
import com.teamwork.business.gantt.CascadeScheduler;
import com.teamwork.business.gantt.DependencyGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý quan hệ phụ thuộc giữa các công việc (Task Dependencies) qua JPA.
 */
public class TaskDependencyDB {

    private static final Logger LOGGER = Logger.getLogger(TaskDependencyDB.class.getName());

    /**
     * Thêm mới một quan hệ phụ thuộc công việc (Task Dependency).
     * 
     * @param dep Quan hệ phụ thuộc (taskId phụ thuộc vào dependsOnTaskId)
     * @return ID bản ghi vừa tạo nếu thành công, -1 nếu phát hiện chu trình hoặc dữ liệu không hợp lệ
     */
    public static int insert(TaskDependency dep) {
        if (dep == null || dep.getTaskId() <= 0 || dep.getDependsOnTaskId() <= 0 || dep.getProjectId() <= 0) {
            LOGGER.warning("Dữ liệu TaskDependency không hợp lệ: taskId hoặc dependsOnTaskId <= 0");
            return -1;
        }

        // Tự phụ thuộc
        if (dep.getTaskId() == dep.getDependsOnTaskId()) {
            LOGGER.warning("Không thể tạo phụ thuộc: Một công việc không thể tự phụ thuộc chính nó (ID: " + dep.getTaskId() + ")");
            return -1;
        }

        // Kiểm tra chu trình trước khi thêm
        if (wouldCreateCycle(dep.getProjectId(), dep.getTaskId(), dep.getDependsOnTaskId())) {
            LOGGER.warning("Phát hiện chu trình phụ thuộc (Cycle Detected) giữa Task " 
                    + dep.getTaskId() + " và Task " + dep.getDependsOnTaskId());
            return -1;
        }

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            // Kiểm tra xem quan hệ này đã tồn tại chưa
            Long count = em.createQuery(
                "SELECT COUNT(d) FROM TaskDependency d WHERE d.taskId = :tid AND d.dependsOnTaskId = :pid", Long.class)
                .setParameter("tid", dep.getTaskId())
                .setParameter("pid", dep.getDependsOnTaskId())
                .getSingleResult();

            if (count != null && count > 0) {
                // Đã tồn tại, lấy ID trả về
                TaskDependency existing = em.createQuery(
                    "SELECT d FROM TaskDependency d WHERE d.taskId = :tid AND d.dependsOnTaskId = :pid", TaskDependency.class)
                    .setParameter("tid", dep.getTaskId())
                    .setParameter("pid", dep.getDependsOnTaskId())
                    .getSingleResult();
                return existing != null ? existing.getId() : 0;
            }

            tx.begin();
            em.persist(dep);
            tx.commit();
            return dep.getId();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi lưu TaskDependency qua JPA", e);
            return -1;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Xóa quan hệ phụ thuộc theo ID.
     */
    public static boolean delete(int id) {
        if (id <= 0) return false;
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            TaskDependency d = em.find(TaskDependency.class, id);
            if (d != null) {
                em.remove(d);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa TaskDependency ID: " + id, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Xóa quan hệ phụ thuộc theo cặp (taskId, dependsOnTaskId).
     */
    public static boolean deleteByPair(int taskId, int dependsOnTaskId) {
        if (taskId <= 0 || dependsOnTaskId <= 0) return false;
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            int deleted = em.createQuery(
                "DELETE FROM TaskDependency d WHERE d.taskId = :tid AND d.dependsOnTaskId = :pid")
                .setParameter("tid", taskId)
                .setParameter("pid", dependsOnTaskId)
                .executeUpdate();
            tx.commit();
            return deleted > 0;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa TaskDependency theo cặp taskId=" + taskId + ", predId=" + dependsOnTaskId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Lấy toàn bộ các quan hệ phụ thuộc thuộc một dự án cụ thể.
     */
    public static List<TaskDependency> selectByProjectId(int projectId) {
        if (projectId <= 0) return new ArrayList<>();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<TaskDependency> list = em.createQuery(
                "SELECT d FROM TaskDependency d WHERE d.projectId = :pid ORDER BY d.id ASC", TaskDependency.class)
                .setParameter("pid", projectId)
                .getResultList();

            if (!list.isEmpty()) {
                // Populate transient names and statuses for UI
                List<Task> tasks = em.createQuery(
                    "SELECT t FROM Task t WHERE t.projectId = :pid", Task.class)
                    .setParameter("pid", projectId)
                    .getResultList();
                Map<Integer, Task> map = new HashMap<>();
                for (Task t : tasks) {
                    map.put(t.getId(), t);
                }
                for (TaskDependency dep : list) {
                    Task t = map.get(dep.getTaskId());
                    Task p = map.get(dep.getDependsOnTaskId());
                    if (t != null) {
                        dep.setTaskTitle(t.getTitle());
                    }
                    if (p != null) {
                        dep.setDependsOnTaskTitle(p.getTitle());
                        dep.setDependsOnTaskStatus(p.getStatus());
                    }
                }
            }

            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy TaskDependency theo Project ID: " + projectId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Lấy các công việc tiên quyết (Prerequisites) mà taskId phụ thuộc vào.
     */
    public static List<TaskDependency> selectPrerequisitesByTaskId(int taskId) {
        if (taskId <= 0) return new ArrayList<>();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT d FROM TaskDependency d WHERE d.taskId = :tid ORDER BY d.id ASC", TaskDependency.class)
                .setParameter("tid", taskId)
                .getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy Prerequisites của Task ID: " + taskId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Lấy các công việc phụ thuộc (Successors) vào taskId.
     */
    public static List<TaskDependency> selectDependentsByTaskId(int taskId) {
        if (taskId <= 0) return new ArrayList<>();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT d FROM TaskDependency d WHERE d.dependsOnTaskId = :tid ORDER BY d.id ASC", TaskDependency.class)
                .setParameter("tid", taskId)
                .getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy Dependents của Task ID: " + taskId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Kiểm tra xem việc thêm quan hệ "taskId phụ thuộc dependsOnTaskId" có tạo ra chu trình hay không.
     */
    public static boolean wouldCreateCycle(int projectId, int taskId, int dependsOnTaskId) {
        if (projectId <= 0 || taskId <= 0 || dependsOnTaskId <= 0) return false;
        if (taskId == dependsOnTaskId) return true;

        List<TaskDependency> existingList = selectByProjectId(projectId);
        return DependencyGraph.wouldCauseCycle(taskId, dependsOnTaskId, existingList);
    }

    /**
     * Kiểm tra xem taskId có công việc tiên quyết nào CHƯA HOÀN THÀNH (chưa DONE) hay không.
     * Dùng để khóa (Lock) không cho phép chuyển trạng thái sang IN_PROGRESS.
     */
    public static boolean hasUnfinishedPrerequisites(int taskId) {
        if (taskId <= 0) return false;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(t) FROM TaskDependency d, Task t "
                + "WHERE d.dependsOnTaskId = t.id AND d.taskId = :tid "
                + "AND UPPER(t.status) NOT IN ('DONE', 'APPROVED')", Long.class)
                .setParameter("tid", taskId)
                .getSingleResult();
            return count != null && count > 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi kiểm tra unfinished prerequisites cho Task ID: " + taskId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Lấy danh sách các công việc tiên quyết CHƯA HOÀN THÀNH của taskId (dùng để hiển thị cảnh báo cho người dùng).
     */
    public static List<Task> getUnfinishedPrerequisites(int taskId) {
        if (taskId <= 0) return Collections.emptyList();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT t FROM TaskDependency d, Task t "
                + "WHERE d.dependsOnTaskId = t.id AND d.taskId = :tid "
                + "AND UPPER(t.status) NOT IN ('DONE', 'APPROVED') "
                + "ORDER BY t.id ASC", Task.class)
                .setParameter("tid", taskId)
                .getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách unfinished prerequisites cho Task ID: " + taskId, e);
            return Collections.emptyList();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Dời lịch dây chuyền (Cascade Shift) khi dueDate của taskId thay đổi.
     * Cập nhật tự động ngày bắt đầu & kết thúc của các task phụ thuộc xuôi dòng vào CSDL.
     * 
     * @param taskId ID công việc bị trễ hoặc thay đổi hạn
     * @param newDueDate Hạn chót mới
     * @return Danh sách các Task đã được cập nhật ngày trong CSDL
     */
    public static List<Task> cascadeShiftDueDates(int taskId, LocalDate newDueDate) {
        return cascadeShiftDates(taskId, null, newDueDate);
    }

    /**
     * Dời lịch dây chuyền khi startDate và/hoặc dueDate của taskId thay đổi.
     */
    public static List<Task> cascadeShiftDates(int taskId, LocalDate newStartDate, LocalDate newDueDate) {
        if (taskId <= 0 || (newStartDate == null && newDueDate == null)) {
            return Collections.emptyList();
        }

        Task task = TaskDB.selectById(taskId);
        if (task == null || task.getProjectId() <= 0) {
            return Collections.emptyList();
        }

        int projectId = task.getProjectId();
        List<Task> allTasks = TaskDB.selectByProjectId(projectId);
        List<TaskDependency> dependencies = selectByProjectId(projectId);

        List<Task> shifted = CascadeScheduler.cascade(taskId, newStartDate, newDueDate, allTasks, dependencies);
        if (shifted == null || shifted.isEmpty()) {
            return Collections.emptyList();
        }

        // Lưu toàn bộ các thay đổi vào CSDL trong một transaction duy nhất
        boolean ok = JPAUtil.inTransaction("cascade shift task dates for project " + projectId, false, em -> {
            for (Task t : shifted) {
                Task managed = em.find(Task.class, t.getId());
                if (managed != null) {
                    managed.setStartDate(t.getStartDate());
                    managed.setDueDate(t.getDueDate());
                    em.merge(managed);
                }
            }
            return true;
        });

        return ok ? shifted : Collections.emptyList();
    }
}
