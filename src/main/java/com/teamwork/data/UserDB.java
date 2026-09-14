package com.teamwork.data;

import com.teamwork.business.User;
import com.teamwork.util.PasswordUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý dữ liệu Người Dùng qua Jakarta Persistence (JPA) / Hibernate.
 * 
 * - Sử dụng EntityManager và JPQL chuẩn hóa, hoạt động độc lập với RDBMS (PostgreSQL Supabase lẫn MySQL).
 * - Quản lý Transaction an toàn với rollback tự động khi có lỗi.
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types) phục vụ các Servlet.
 */
public class UserDB {

    private static final Logger LOGGER = Logger.getLogger(UserDB.class.getName());

    /**
     * Xác thực tài khoản đăng nhập (Username + Password)
     */
    public static User selectByCredentials(String username, String plainPassword) {
        if (username == null || plainPassword == null || username.trim().isEmpty() || plainPassword.trim().isEmpty()) {
            return null;
        }

        User user = selectByUsername(username.trim());
        if (user != null) {
            String storedPassword = user.getPassword();
            if (PasswordUtil.verifyPassword(plainPassword.trim(), storedPassword) 
                    || plainPassword.trim().equals(storedPassword)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Tìm người dùng theo ID khóa chính bằng em.find()
     */
    public static User selectById(int id) {
        if (id <= 0) {
            return null;
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(User.class, id);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi truy vấn User theo ID: " + id, e);
            return null;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Tìm người dùng theo Username bằng JPQL
     */
    public static User selectByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username)";
            List<User> list = em.createQuery(jpql, User.class)
                .setParameter("username", username.trim())
                .setMaxResults(1)
                .getResultList();
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi truy vấn User theo Username: " + username, e);
            return null;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Tìm kiếm người dùng theo Username HOẶC Email bằng JPQL
     */
    public static User selectByUsernameOrEmail(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:keyword) OR LOWER(u.email) = LOWER(:keyword)";
            List<User> list = em.createQuery(jpql, User.class)
                .setParameter("keyword", keyword.trim())
                .setMaxResults(1)
                .getResultList();
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi truy vấn User theo Username hoặc Email: " + keyword, e);
            return null;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Thêm người dùng mới vào Database qua em.persist() và EntityTransaction
     */
    public static int insert(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return 0;
        }

        // Tự động băm mật khẩu nếu chưa được băm
        String rawPassword = user.getPassword();
        String hashedPassword = (rawPassword != null && rawPassword.length() == 64) 
                ? rawPassword 
                : PasswordUtil.hashPassword(rawPassword != null ? rawPassword : "");
        user.setPassword(hashedPassword);

        // Đảm bảo các trường không null
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            user.setFullName(user.getUsername().trim());
        }
        if (user.getEmail() == null) user.setEmail("");
        if (user.getRole() == null || user.getRole().trim().isEmpty()) user.setRole("Developer");
        if (user.getAvatar() == null || user.getAvatar().trim().isEmpty()) user.setAvatar("images/default_avatar.png");
        if (user.getBio() == null) user.setBio("");
        if (user.getSkills() == null) user.setSkills("");
        if (user.getGithubUrl() == null) user.setGithubUrl("");
        if (user.getLinkedinUrl() == null) user.setLinkedinUrl("");

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(user);
            tx.commit();
            return user.getId();
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi chèn User mới qua JPA: " + user.getUsername(), e);
            return 0;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Cập nhật thông tin Hồ Sơ Cá Nhân qua em.merge() và EntityTransaction
     */
    public static boolean update(User updatedUser) {
        if (updatedUser == null || updatedUser.getId() <= 0) {
            return false;
        }

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            User managed = em.find(User.class, updatedUser.getId());
            if (managed != null) {
                managed.setFullName(updatedUser.getFullName() != null ? updatedUser.getFullName().trim() : "");
                managed.setRole(updatedUser.getRole() != null ? updatedUser.getRole().trim() : "Developer");
                managed.setAvatar(updatedUser.getAvatar() != null ? updatedUser.getAvatar().trim() : "images/default_avatar.png");
                managed.setBio(updatedUser.getBio() != null ? updatedUser.getBio().trim() : "");
                managed.setSkills(updatedUser.getSkills() != null ? updatedUser.getSkills().trim() : "");
                managed.setGithubUrl(updatedUser.getGithubUrl() != null ? updatedUser.getGithubUrl().trim() : "");
                managed.setLinkedinUrl(updatedUser.getLinkedinUrl() != null ? updatedUser.getLinkedinUrl().trim() : "");
                em.merge(managed);
                tx.commit();
                return true;
            }
            tx.commit();
            return false;
        } catch (Exception e) {
            JPAUtil.rollbackIfActive(tx);
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật User ID qua JPA: " + updatedUser.getId(), e);
            return false;
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }

    /**
     * Lấy danh sách toàn bộ người dùng bằng JPQL
     */
    public static List<User> selectAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u ORDER BY u.id ASC", User.class).getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách toàn bộ User qua JPA", e);
            return new ArrayList<>();
        } finally {
            JPAUtil.closeEntityManager(em);
        }
    }
}