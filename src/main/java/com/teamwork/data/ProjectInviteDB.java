package com.teamwork.data;

import com.teamwork.business.Project;
import com.teamwork.business.ProjectInvite;
import com.teamwork.business.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý Lời Mời & Yêu Cầu Xin Gia Nhập 2 Chiều qua JPA 3.1 / Hibernate.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng JPA EntityManager và JPQL chuẩn, tương thích cả PostgreSQL và MySQL
 * - Tự động nạp tên dự án, mã dự án, tên người gửi và người nhận
 */
public class ProjectInviteDB {

    private static final Logger LOGGER = Logger.getLogger(ProjectInviteDB.class.getName());

    private static void populateInviteDetails(EntityManager em, ProjectInvite pi) {
        if (pi == null) return;
        if (pi.getProjectId() > 0) {
            Project p = em.find(Project.class, pi.getProjectId());
            if (p != null) {
                pi.setProjectName(p.getName() != null ? p.getName() : "");
                pi.setProjectCode(p.getProjectCode() != null ? p.getProjectCode() : "");
            }
        }
        if (pi.getSenderId() > 0) {
            User sender = em.find(User.class, pi.getSenderId());
            if (sender != null) {
                pi.setSenderName(sender.getFullName() != null ? sender.getFullName() : "");
            }
        }
        if (pi.getReceiverId() > 0) {
            User receiver = em.find(User.class, pi.getReceiverId());
            if (receiver != null) {
                pi.setReceiverName(receiver.getFullName() != null ? receiver.getFullName() : "");
            }
        }
    }

    /**
     * Hàm 1: Thêm Lời mời / Yêu cầu mới
     */
    public static int insert(ProjectInvite invite) {
        if (invite == null || invite.getProjectId() <= 0 || invite.getSenderId() <= 0 || invite.getReceiverId() <= 0) {
            return 0;
        }

        if (invite.getType() == null || invite.getType().trim().isEmpty()) {
            invite.setType("INVITATION");
        }
        if (invite.getStatus() == null || invite.getStatus().trim().isEmpty()) {
            invite.setStatus("PENDING");
        }

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(invite);
            tx.commit();
            return invite.getId();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi chèn ProjectInvite qua JPA", e);
            return 0;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 2: Tìm Lời mời theo ID duy nhất
     */
    public static ProjectInvite selectById(int id) {
        if (id <= 0) return null;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            ProjectInvite pi = em.find(ProjectInvite.class, id);
            if (pi != null) {
                populateInviteDetails(em, pi);
            }
            return pi;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm ProjectInvite ID qua JPA: " + id, e);
            return null;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 3: Lấy danh sách Lời mời / Yêu cầu đang PENDING mà người dùng này CẦN DUYỆT
     */
    public static List<ProjectInvite> selectPendingByReceiverId(int receiverId) {
        if (receiverId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<ProjectInvite> query = em.createQuery(
                "SELECT pi FROM ProjectInvite pi WHERE pi.receiverId = :receiverId AND pi.status = 'PENDING' ORDER BY pi.id DESC",
                ProjectInvite.class
            );
            query.setParameter("receiverId", receiverId);
            List<ProjectInvite> list = query.getResultList();
            for (ProjectInvite pi : list) {
                populateInviteDetails(em, pi);
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy pending invites của User ID qua JPA: " + receiverId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 4: Lấy danh sách tất cả Lời mời / Yêu cầu của một Dự Án cụ thể
     */
    public static List<ProjectInvite> selectByProjectId(int projectId) {
        if (projectId <= 0) return new ArrayList<>();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<ProjectInvite> query = em.createQuery(
                "SELECT pi FROM ProjectInvite pi WHERE pi.projectId = :projectId ORDER BY pi.id DESC",
                ProjectInvite.class
            );
            query.setParameter("projectId", projectId);
            List<ProjectInvite> list = query.getResultList();
            for (ProjectInvite pi : list) {
                populateInviteDetails(em, pi);
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy invites theo Project ID qua JPA: " + projectId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 5: Kiểm tra xem đã có Lời mời / Yêu cầu PENDING giữa 2 người trong dự án chưa (2 chiều)
     */
    public static boolean hasPendingInvite(int projectId, int user1Id, int user2Id) {
        if (projectId <= 0 || user1Id <= 0 || user2Id <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(pi) FROM ProjectInvite pi " +
                "WHERE pi.projectId = :projectId AND pi.status = 'PENDING' " +
                "  AND ((pi.senderId = :u1 AND pi.receiverId = :u2) OR (pi.senderId = :u2 AND pi.receiverId = :u1))",
                Long.class
            )
            .setParameter("projectId", projectId)
            .setParameter("u1", user1Id)
            .setParameter("u2", user2Id)
            .getSingleResult();

            return count != null && count > 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi kiểm tra pending invite qua JPA", e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 6: Cập nhật trạng thái Lời mời ("ACCEPTED", "REJECTED", "REVOKED", "EXPIRED")
     */
    public static void updateStatus(int id, String newStatus) {
        if (id <= 0 || newStatus == null || newStatus.trim().isEmpty()) return;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            ProjectInvite pi = em.find(ProjectInvite.class, id);
            if (pi != null) {
                pi.setStatus(newStatus.trim().toUpperCase());
                em.merge(pi);
            }
            tx.commit();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật trạng thái Invite ID qua JPA: " + id, e);
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 7: Xóa lời mời
     */
    public static boolean delete(int id) {
        if (id <= 0) return false;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            ProjectInvite pi = em.find(ProjectInvite.class, id);
            if (pi != null) {
                em.remove(pi);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa Invite ID qua JPA: " + id, e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Hàm 8: Thu hồi toàn bộ lời mời đang PENDING của dự án (khi chuyển sang Solo)
     */
    public static void revokeAllPendingByProjectId(int projectId) {
        if (projectId <= 0) return;

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("UPDATE ProjectInvite pi SET pi.status = 'REVOKED' WHERE pi.projectId = :projectId AND pi.status = 'PENDING'")
              .setParameter("projectId", projectId)
              .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi thu hồi invites của Project ID qua JPA: " + projectId, e);
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }
}
