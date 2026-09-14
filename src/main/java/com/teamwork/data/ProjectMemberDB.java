package com.teamwork.data;

import com.teamwork.business.Project;
import com.teamwork.business.ProjectMember;
import com.teamwork.business.ProjectMemberId;
import com.teamwork.business.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý Danh Sách Thành Viên của từng Dự Án qua JPA 3.1 / Hibernate.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng JPA EntityManager và JPQL chuẩn, tương thích cả PostgreSQL và MySQL
 * - Tự động nạp tên, email, vai trò của thành viên từ quan hệ User
 */
public class ProjectMemberDB {

    private static final Logger LOGGER = Logger.getLogger(ProjectMemberDB.class.getName());

    private static void populateUserInfo(EntityManager em, ProjectMember pm) {
        if (pm == null) return;
        if (pm.getUserId() > 0) {
            User u = em.find(User.class, pm.getUserId());
            if (u != null) {
                pm.setUserName(u.getFullName() != null ? u.getFullName() : "");
                pm.setUserEmail(u.getEmail() != null ? u.getEmail() : "");
                pm.setUserRole(u.getRole() != null ? u.getRole() : "");
            }
        }
    }

    /**
     * Hàm 1: Lấy toàn bộ danh sách thành viên của một dự án cụ thể
     */
    public static List<ProjectMember> selectByProjectId(int projectId) {
        if (projectId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<ProjectMember> query = em.createQuery(
                "SELECT pm FROM ProjectMember pm WHERE pm.projectId = :projectId ORDER BY pm.joinedAt ASC",
                ProjectMember.class
            );
            query.setParameter("projectId", projectId);
            List<ProjectMember> list = query.getResultList();
            for (ProjectMember pm : list) {
                populateUserInfo(em, pm);
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách thành viên Project ID qua JPA: " + projectId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 2: Lấy tất cả các Dự án mà một người dùng đang tham gia
     */
    public static List<Project> selectProjectsByUserId(int userId) {
        if (userId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Integer> query = em.createQuery(
                "SELECT pm.projectId FROM ProjectMember pm WHERE pm.userId = :userId ORDER BY pm.projectId ASC",
                Integer.class
            );
            query.setParameter("userId", userId);
            List<Integer> projectIds = query.getResultList();

            List<Project> result = new ArrayList<>();
            for (Integer pId : projectIds) {
                Project p = ProjectDB.selectById(pId);
                if (p != null) {
                    result.add(p);
                }
            }
            return result;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách Project của User ID qua JPA: " + userId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 3: Kiểm tra xem một người dùng đã là thành viên của dự án hay chưa
     */
    public static boolean isMember(int projectId, int userId) {
        if (projectId <= 0 || userId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            ProjectMember pm = em.find(ProjectMember.class, new ProjectMemberId(projectId, userId));
            return pm != null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi kiểm tra tư cách thành viên qua JPA", e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 4: Đếm tổng số lượng thành viên hiện tại của một dự án
     */
    public static int countMembers(int projectId) {
        if (projectId <= 0) return 0;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(pm) FROM ProjectMember pm WHERE pm.projectId = :projectId",
                Long.class
            )
            .setParameter("projectId", projectId)
            .getSingleResult();

            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi đếm số thành viên Project ID qua JPA: " + projectId, e);
            return 0;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 5: Thêm thành viên mới vào dự án
     */
    public static void insert(ProjectMember member) {
        if (member == null || member.getProjectId() <= 0 || member.getUserId() <= 0) {
            return;
        }

        if (member.getProjectRole() == null || member.getProjectRole().trim().isEmpty()) {
            member.setProjectRole("MEMBER");
        }

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            ProjectMember existing = em.find(ProjectMember.class, new ProjectMemberId(member.getProjectId(), member.getUserId()));
            if (existing == null) {
                em.persist(member);
            }
            tx.commit();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi thêm thành viên vào Project qua JPA", e);
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 6: Xóa thành viên ra khỏi dự án
     */
    public static boolean delete(int projectId, int userId) {
        if (projectId <= 0 || userId <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            ProjectMember pm = em.find(ProjectMember.class, new ProjectMemberId(projectId, userId));
            if (pm != null) {
                em.remove(pm);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa thành viên khỏi Project qua JPA", e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 7: Đồng bộ tên thành viên mới
     */
    public static void syncUserName(int userId, String newFullName) {
        // Tự động đồng bộ qua JOIN User trong JPA
    }
}
