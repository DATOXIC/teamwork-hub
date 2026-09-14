package com.teamwork.data;

import com.teamwork.business.Project;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.List;
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
     * Hàm phụ trợ ánh xạ 1 dòng từ ResultSet sang đối tượng JavaBean Project (Tương thích ngược JDBC)
     */
    public static Project mapResultSetToProject(java.sql.ResultSet rs) throws java.sql.SQLException {
        int id = rs.getInt("id");
        String projectCode = rs.getString("project_code");
        String name = rs.getString("name");
        String description = rs.getString("description");
        String projectType = "TEAM";
        try {
            String pt = rs.getString("project_type");
            if (pt != null && !pt.trim().isEmpty()) projectType = pt.trim().toUpperCase();
        } catch (java.sql.SQLException ignored) {}
        int ownerId = rs.getInt("owner_id");
        String createdAt = "";
        try {
            createdAt = rs.getString("created_at_str");
        } catch (java.sql.SQLException ignored) {}
        int totalTasks = 0;
        int doneTasks = 0;
        try {
            totalTasks = rs.getInt("total_tasks");
            doneTasks = rs.getInt("done_tasks");
        } catch (java.sql.SQLException ignored) {}

        return new Project(
            id,
            projectCode != null ? projectCode : "PRJ-" + id,
            name != null ? name : "",
            description != null ? description : "",
            projectType,
            ownerId,
            createdAt != null ? createdAt : "",
            totalTasks,
            doneTasks
        );
    }

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
     * Hàm 1: Lấy toàn bộ danh sách dự án bằng JPQL
     */
    public static List<Project> selectAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Project> list = em.createQuery("SELECT p FROM Project p ORDER BY p.id ASC", Project.class)
                .getResultList();
            for (Project p : list) {
                populateTaskStats(em, p);
            }
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
            em.persist(project);

            // Thêm Owner vào project_members
            try {
                em.createNativeQuery("INSERT INTO project_members (project_id, user_id, project_role, joined_at) " +
                                     "VALUES (:pid, :uid, 'OWNER', NOW())")
                    .setParameter("pid", project.getId())
                    .setParameter("uid", project.getOwnerId())
                    .executeUpdate();
            } catch (Exception memberEx) {
                LOGGER.warning("Lưu ý khi thêm Owner vào project_members: " + memberEx.getMessage());
            }

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