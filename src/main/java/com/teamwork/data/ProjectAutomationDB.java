package com.teamwork.data;

import com.teamwork.business.ProjectAutomation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý cấu hình quy tắc tự động hóa dự án (Project Automations) qua JPA.
 */
public class ProjectAutomationDB {

    private static final Logger LOGGER = Logger.getLogger(ProjectAutomationDB.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Lấy toàn bộ danh sách quy tắc tự động hóa của một dự án.
     * Nếu dự án chưa có quy tắc nào trong CSDL, hàm sẽ tự động khởi tạo (seed) 3 quy tắc mặc định với trạng thái bật (is_enabled = true).
     *
     * @param projectId ID của dự án
     * @return Danh sách các quy tắc tự động hóa của dự án
     */
    public static List<ProjectAutomation> selectByProjectId(int projectId) {
        if (projectId <= 0) {
            return Collections.emptyList();
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<ProjectAutomation> query = em.createQuery(
                "SELECT a FROM ProjectAutomation a WHERE a.projectId = :pid ORDER BY a.id ASC",
                ProjectAutomation.class
            );
            query.setParameter("pid", projectId);
            List<ProjectAutomation> list = query.getResultList();

            if (list == null || list.isEmpty()) {
                // Tự động seed 3 quy tắc mặc định
                return seedDefaultRecipes(projectId);
            }
            return new ArrayList<>(list);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách ProjectAutomation cho projectId=" + projectId, e);
            return Collections.emptyList();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Khởi tạo 3 quy tắc tự động hóa mặc định cho dự án nếu chưa tồn tại.
     */
    private static List<ProjectAutomation> seedDefaultRecipes(int projectId) {
        if (projectId <= 0) return Collections.emptyList();

        List<ProjectAutomation> defaults = new ArrayList<>();
        defaults.add(new ProjectAutomation(
            projectId,
            ProjectAutomation.AUTO_PROMPT_GATE2,
            "Tự động nhắc nộp Gate 2 khi hoàn thành Subtasks",
            "Khi tất cả công việc phụ (subtasks) của một công việc đạt 100%, tự động gửi thông báo đến Task Lead nhắc nộp sản phẩm hoàn thiện Gate 2.",
            true
        ));
        defaults.add(new ProjectAutomation(
            projectId,
            ProjectAutomation.AUTO_CLOSE_SUBTASKS,
            "Tự động đóng Subtasks khi PM duyệt Task hoàn thành",
            "Khi Quản lý dự án (PM) duyệt công việc sang trạng thái Hoàn thành (DONE), tự động đồng bộ và đóng tất cả công việc phụ còn lại sang APPROVED/DONE.",
            true
        ));
        defaults.add(new ProjectAutomation(
            projectId,
            ProjectAutomation.AUTO_ESCALATE_PRIORITY,
            "Tự động nâng độ ưu tiên công việc sắp đến hạn chót",
            "Khi công việc có hạn chót là hôm nay hoặc đã quá hạn mà tiến độ còn dưới 50%, tự động nâng mức độ ưu tiên lên KHẨN CẤP (HIGH) và cảnh báo PM.",
            true
        ));

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            for (ProjectAutomation recipe : defaults) {
                // Kiểm tra xem đã tồn tại chưa để tránh lỗi trùng lặp khóa duy nhất (uq_proj_auto_recipe)
                Long count = em.createQuery(
                    "SELECT COUNT(a) FROM ProjectAutomation a WHERE a.projectId = :pid AND a.recipeId = :rid", Long.class)
                    .setParameter("pid", projectId)
                    .setParameter("rid", recipe.getRecipeId())
                    .getSingleResult();

                if (count == null || count == 0) {
                    em.persist(recipe);
                }
            }
            tx.commit();

            // Truy vấn lại danh sách hoàn chỉnh từ CSDL
            TypedQuery<ProjectAutomation> q = em.createQuery(
                "SELECT a FROM ProjectAutomation a WHERE a.projectId = :pid ORDER BY a.id ASC",
                ProjectAutomation.class
            );
            q.setParameter("pid", projectId);
            return new ArrayList<>(q.getResultList());
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.WARNING, "Không thể seed tự động công thức cho projectId=" + projectId + " (có thể đã được seed đồng thời)", e);
            // Thử query lại lần nữa
            try {
                return em.createQuery("SELECT a FROM ProjectAutomation a WHERE a.projectId = :pid ORDER BY a.id ASC", ProjectAutomation.class)
                         .setParameter("pid", projectId)
                         .getResultList();
            } catch (Exception ex) {
                return defaults;
            }
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Kiểm tra một quy tắc tự động hóa cụ thể có đang được bật cho dự án hay không.
     *
     * @param projectId ID dự án
     * @param recipeId Mã quy tắc (AUTO_PROMPT_GATE2, AUTO_CLOSE_SUBTASKS, AUTO_ESCALATE_PRIORITY)
     * @return true nếu quy tắc đang bật, false nếu bị tắt hoặc không tồn tại
     */
    public static boolean isRecipeEnabled(int projectId, String recipeId) {
        if (projectId <= 0 || recipeId == null || recipeId.trim().isEmpty()) {
            return false;
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Boolean> query = em.createQuery(
                "SELECT a.isEnabled FROM ProjectAutomation a WHERE a.projectId = :pid AND a.recipeId = :rid",
                Boolean.class
            );
            query.setParameter("pid", projectId);
            query.setParameter("rid", recipeId.trim());
            List<Boolean> results = query.getResultList();

            if (results != null && !results.isEmpty()) {
                return Boolean.TRUE.equals(results.get(0));
            }

            // Nếu chưa có cấu hình trong CSDL, thực hiện seed và kiểm tra lại
            List<ProjectAutomation> seeded = selectByProjectId(projectId);
            for (ProjectAutomation a : seeded) {
                if (recipeId.trim().equals(a.getRecipeId())) {
                    return a.isEnabled();
                }
            }
            return true; // Mặc định quy tắc được bật
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi kiểm tra trạng thái recipe: " + recipeId + " cho projectId=" + projectId, e);
            return true; // Fallback an toàn: bật mặc định
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Bật hoặc tắt một quy tắc tự động hóa cho dự án.
     *
     * @param projectId ID dự án
     * @param recipeId Mã quy tắc
     * @param enabled Trạng thái mới (true: bật, false: tắt)
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    public static boolean toggleRecipe(int projectId, String recipeId, boolean enabled) {
        if (projectId <= 0 || recipeId == null || recipeId.trim().isEmpty()) {
            return false;
        }

        // Đảm bảo các quy tắc đã được seed trước khi toggle
        selectByProjectId(projectId);

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Query query = em.createQuery(
                "UPDATE ProjectAutomation a SET a.isEnabled = :en WHERE a.projectId = :pid AND a.recipeId = :rid"
            );
            query.setParameter("en", enabled);
            query.setParameter("pid", projectId);
            query.setParameter("rid", recipeId.trim());
            int affected = query.executeUpdate();
            tx.commit();
            return affected > 0;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi toggle recipe " + recipeId + " cho projectId=" + projectId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Ghi nhận một lần kích hoạt thành công của quy tắc tự động hóa: tăng trigger_count và cập nhật last_triggered_at.
     *
     * @param projectId ID dự án
     * @param recipeId Mã quy tắc
     * @return true nếu cập nhật thành công
     */
    public static boolean recordTrigger(int projectId, String recipeId) {
        if (projectId <= 0 || recipeId == null || recipeId.trim().isEmpty()) {
            return false;
        }

        String now = LocalDateTime.now().format(DATE_FORMATTER);
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Query query = em.createQuery(
                "UPDATE ProjectAutomation a SET a.triggerCount = a.triggerCount + 1, a.lastTriggeredAt = :now " +
                "WHERE a.projectId = :pid AND a.recipeId = :rid"
            );
            query.setParameter("now", now);
            query.setParameter("pid", projectId);
            query.setParameter("rid", recipeId.trim());
            int affected = query.executeUpdate();
            tx.commit();
            return affected > 0;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.WARNING, "Không thể ghi nhận lượt kích hoạt automation cho recipe " + recipeId, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Lấy chi tiết cấu hình một quy tắc tự động hóa theo dự án và mã quy tắc.
     */
    public static ProjectAutomation selectByProjectAndRecipe(int projectId, String recipeId) {
        if (projectId <= 0 || recipeId == null) return null;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<ProjectAutomation> q = em.createQuery(
                "SELECT a FROM ProjectAutomation a WHERE a.projectId = :pid AND a.recipeId = :rid",
                ProjectAutomation.class
            );
            q.setParameter("pid", projectId);
            q.setParameter("rid", recipeId.trim());
            List<ProjectAutomation> results = q.getResultList();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi truy vấn ProjectAutomation", e);
            return null;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }
}
