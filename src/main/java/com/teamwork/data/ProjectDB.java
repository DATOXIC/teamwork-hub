package com.teamwork.data;

import com.teamwork.business.Project;
import com.teamwork.business.ProjectMember;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý dữ liệu Dự án (Project) qua Jakarta Persistence (JPA) / Hibernate.
 * 
 * - Sử dụng EntityManager và JPQL chuẩn hóa, loại bỏ hoàn toàn các cú pháp đặc thù riêng của từng DB.
 * - Tự động tính toán tiến độ (totalTasks, doneTasks) bằng câu truy vấn JPQL tổng quát.
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types) cho các Servlet.
 */
public class ProjectDB {

    private static final Logger LOGGER = Logger.getLogger(ProjectDB.class.getName());

    /**
     * Hàm phụ trợ tính toán số lượng task và task hoàn thành độc lập với loại DB
     */
    private static void populateTaskStats(EntityManager em, Project project) {
        if (project == null || project.getId() <= 0) return;
        try {
            Long total = em.createQuery(
                "SELECT COUNT(t) FROM Task t WHERE t.projectId = :pid", Long.class)
                .setParameter("pid", project.getId())
                .getSingleResult();

            Long done = em.createQuery(
                "SELECT COUNT(t) FROM Task t WHERE t.projectId = :pid AND (t.status = 'DONE' OR t.status = 'APPROVED')", Long.class)
                .setParameter("pid", project.getId())
                .getSingleResult();

            project.setTotalTasks(total != null ? total.intValue() : 0);
            project.setDoneTasks(done != null ? done.intValue() : 0);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Không thể tính thống kê task cho Project ID: " + project.getId(), e);
        }
    }

    /**
     * Thống kê task cho NHIỀU dự án bằng MỘT câu GROUP BY (thay vì 2 câu COUNT cho mỗi dự án).
     */
    private static void populateTaskStats(EntityManager em, List<Project> projects) {
        if (projects == null || projects.isEmpty()) return;
        Map<Integer, Project> byId = new HashMap<>();
        for (Project p : projects) {
            p.setTotalTasks(0);
            p.setDoneTasks(0);
            byId.put(p.getId(), p);
        }
        try {
            List<Object[]> rows = em.createQuery(
                "SELECT t.projectId, COUNT(t), "
                + "SUM(CASE WHEN t.status IN ('DONE', 'APPROVED') THEN 1 ELSE 0 END) "
                + "FROM Task t WHERE t.projectId IN :ids GROUP BY t.projectId", Object[].class)
                .setParameter("ids", byId.keySet())
                .getResultList();
            for (Object[] r : rows) {
                Project p = byId.get(((Number) r[0]).intValue());
                if (p != null) {
                    p.setTotalTasks(((Number) r[1]).intValue());
                    p.setDoneTasks(r[2] != null ? ((Number) r[2]).intValue() : 0);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Không thể tính thống kê task cho danh sách dự án", e);
        }
    }

    /**
     * Các dự án mà một người đang tham gia: 1 câu lấy dự án + 1 câu thống kê
     * (trước đây: mỗi dự án mở một kết nối và chạy 3 câu riêng).
     */
    public static List<Project> selectByMember(int userId) {
        if (userId <= 0) return new ArrayList<>();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Project> list = em.createQuery(
                "SELECT p FROM Project p WHERE p.id IN "
                + "(SELECT pm.projectId FROM ProjectMember pm WHERE pm.userId = :uid) ORDER BY p.id ASC", Project.class)
                .setParameter("uid", userId)
                .getResultList();
            populateTaskStats(em, list);
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy dự án của User ID: " + userId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Dự án do {@code ownerId} làm PM mà {@code userId} CHƯA tham gia (gợi ý "Mời vào dự án" ở trang hồ sơ).
     */
    public static List<Project> selectOwnedWithoutMember(int ownerId, int userId) {
        if (ownerId <= 0 || userId <= 0) return new ArrayList<>();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                "SELECT p FROM Project p WHERE p.ownerId = :owner AND NOT EXISTS "
                + "(SELECT pm FROM ProjectMember pm WHERE pm.projectId = p.id AND pm.userId = :uid) ORDER BY p.id ASC",
                Project.class)
                .setParameter("owner", ownerId)
                .setParameter("uid", userId)
                .getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy dự án có thể mời User ID: " + userId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 1: Lấy toàn bộ danh sách dự án bằng JPQL
     */
    public static List<Project> selectAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Project> list = em.createQuery("SELECT p FROM Project p ORDER BY p.id ASC", Project.class)
                .getResultList();
            populateTaskStats(em, list);
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy toàn bộ danh sách Project qua JPA", e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 2: Tìm dự án theo ID duy nhất
     */
    public static Project selectById(int id) {
        if (id <= 0) return null;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            Project project = em.find(Project.class, id);
            if (project != null) {
                populateTaskStats(em, project);
            }
            return project;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm Project theo ID qua JPA: " + id, e);
            return null;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 3: Tìm dự án theo Mã Dự Án (projectCode) bằng JPQL
     */
    public static Project selectByCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT p FROM Project p WHERE UPPER(p.projectCode) = UPPER(:code)";
            List<Project> list = em.createQuery(jpql, Project.class)
                .setParameter("code", code.trim())
                .setMaxResults(1)
                .getResultList();

            if (!list.isEmpty()) {
                Project project = list.get(0);
                populateTaskStats(em, project);
                return project;
            }
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm Project theo Code qua JPA: " + code, e);
            return null;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 4: Thêm dự án mới qua em.persist()
     */
    public static int insert(Project project) {
        if (project == null || project.getName() == null || project.getName().trim().isEmpty()) {
            return 0;
        }

        String projectCode = project.getProjectCode();
        if (projectCode == null || projectCode.trim().isEmpty()) {
            projectCode = "PRJ-" + System.currentTimeMillis() % 100000;
        }
        project.setProjectCode(projectCode.trim().toUpperCase());
        if (project.getDescription() == null) project.setDescription("");
        if (project.getProjectType() == null) project.setProjectType("TEAM");

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(project); // IDENTITY: id được cấp ngay tại đây

            // Người tạo trở thành Trưởng dự án (OWNER) — CÙNG transaction với việc tạo dự án:
            // không bao giờ có dự án "vô chủ" (đã tạo nhưng chưa có thành viên OWNER).
            // (Trước đây lệnh này nằm trong try/catch riêng: trên PostgreSQL, một câu lỗi làm hỏng cả
            //  transaction nên commit sau đó cũng thất bại; và servlet còn chèn OWNER lần thứ hai.)
            ProjectMember owner = new ProjectMember();
            owner.setProjectId(project.getId());
            owner.setUserId(project.getOwnerId());
            owner.setProjectRole("OWNER");
            em.persist(owner);

            tx.commit();
            return project.getId();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi chèn Project mới qua JPA: " + project.getName(), e);
            return 0;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 5: Cập nhật thông tin dự án qua em.merge()
     */
    public static boolean update(Project project) {
        if (project == null || project.getId() <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Project managed = em.find(Project.class, project.getId());
            if (managed != null) {
                managed.setName(project.getName().trim());
                managed.setDescription(project.getDescription() != null ? project.getDescription().trim() : "");
                managed.setProjectType(project.getProjectType() != null ? project.getProjectType() : "TEAM");
                em.merge(managed);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật Project ID qua JPA: " + project.getId(), e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }
}