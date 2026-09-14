package com.teamwork.data;

import com.teamwork.business.ActivityLog;
import com.teamwork.business.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý lưu vết và truy vấn Nhật ký hoạt động (Activity Log) qua JPA 3.1 / Hibernate.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Non-blocking: Ghi log có cơ chế an toàn tuyệt đối, lỗi ghi log không làm gián đoạn transaction chính.
 * - Tự động nạp tên và avatar của người dùng qua quan hệ User.
 */
public class ActivityLogDB {

    private static final Logger LOGGER = Logger.getLogger(ActivityLogDB.class.getName());
    private static final ExecutorService ASYNC_POOL = Executors.newFixedThreadPool(2);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Tương thích ngược: kiểm tra bảng
     */
    public static void ensureTableExists() {
        // Hibernate tự động quản lý hoặc xác thực schema
    }

    /**
     * Ghi nhận một hành động hoạt động vào nhật ký dự án (Synchronous an toàn).
     */
    public static void log(int projectId, int userId, String actionType,
                           String targetType, int targetId, String targetTitle, String description) {
        if (projectId <= 0) return;

        ActivityLog log = new ActivityLog();
        log.setProjectId(projectId);
        if (userId > 0) {
            log.setUserId(userId);
        }
        log.setActionType(actionType != null ? actionType : "UPDATE");
        log.setTargetType(targetType != null ? targetType : "TASK");
        log.setTargetId(targetId);
        log.setTargetTitle(targetTitle != null ? (targetTitle.length() > 250 ? targetTitle.substring(0, 247) + "..." : targetTitle) : "");
        log.setDescription(description != null ? description : "");
        log.setRawCreatedAt(new Timestamp(System.currentTimeMillis()));

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(log);
            tx.commit();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.WARNING, "Không thể ghi activity log qua JPA cho projectId=" + projectId, e);
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Ghi nhận hoạt động chạy nền bất đồng bộ (Non-blocking) để tối ưu độ trễ request.
     */
    public static void logAsync(int projectId, int userId, String actionType,
                                String targetType, int targetId, String targetTitle, String description) {
        ASYNC_POOL.submit(() -> {
            try {
                log(projectId, userId, actionType, targetType, targetId, targetTitle, description);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Lỗi khi ghi async activity log qua JPA", e);
            }
        });
    }

    /**
     * Lấy danh sách lịch sử hoạt động mới nhất của một dự án.
     */
    public static List<ActivityLog> selectByProjectId(int projectId, int limit) {
        if (projectId <= 0) return new ArrayList<>();

        int maxRows = limit > 0 ? limit : 50;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<ActivityLog> query = em.createQuery(
                "SELECT a FROM ActivityLog a WHERE a.projectId = :projectId ORDER BY a.id DESC",
                ActivityLog.class
            );
            query.setParameter("projectId", projectId);
            query.setMaxResults(maxRows);
            List<ActivityLog> list = query.getResultList();

            for (ActivityLog a : list) {
                if (a.getUserId() > 0) {
                    User u = em.find(User.class, a.getUserId());
                    if (u != null) {
                        a.setUserName(u.getFullName() != null ? u.getFullName() : "Thành viên");
                        a.setUserAvatar(u.getAvatar() != null ? u.getAvatar() : "");
                    } else {
                        a.setUserName("Thành viên");
                        a.setUserAvatar("");
                    }
                } else {
                    a.setUserName("Hệ thống");
                    a.setUserAvatar("");
                }

                if (a.getRawCreatedAt() != null) {
                    try {
                        a.setCreatedAt(a.getRawCreatedAt().toLocalDateTime().format(DATE_FORMATTER));
                    } catch (Exception ignored) {}
                }
            }
            return list;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách ActivityLog qua JPA cho projectId=" + projectId, e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }
}