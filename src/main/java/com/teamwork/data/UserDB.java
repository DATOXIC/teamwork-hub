package com.teamwork.data;

import com.teamwork.business.User;
import com.teamwork.util.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý dữ liệu Người Dùng kết nối Supabase PostgreSQL.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng PreparedStatement phòng chống triệt để tấn công SQL Injection
 * - Quản lý tài nguyên bằng try-with-resources tự động giải phóng Connection/Statement/ResultSet
 * - Xử lý input rỗng, null-safe và hỗ trợ xác thực mật khẩu an toàn
 */
public class UserDB {

    private static final Logger LOGGER = Logger.getLogger(UserDB.class.getName());

    /**
     * Hàm phụ trợ ánh xạ 1 dòng từ ResultSet sang đối tượng JavaBean User
     * Khớp chính xác với cấu trúc bảng 'users' trong schema.sql
     */
    private static User mapResultSetToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String fullName = rs.getString("full_name");
        String email = rs.getString("email");
        String role = rs.getString("role");
        String avatar = rs.getString("avatar");
        String bio = rs.getString("bio");
        String skills = rs.getString("skills");
        String githubUrl = rs.getString("github_url");
        String linkedinUrl = rs.getString("linkedin_url");

        return new User(
            id,
            username != null ? username : "",
            password != null ? password : "",
            fullName != null ? fullName : "",
            email != null ? email : "",
            role != null ? role : "Developer",
            avatar != null ? avatar : "images/default_avatar.png",
            bio != null ? bio : "",
            skills != null ? skills : "",
            githubUrl != null ? githubUrl : "",
            linkedinUrl != null ? linkedinUrl : ""
        );
    }

    /**
     * Hàm 1: Xác thực tài khoản (Dùng khi người dùng bấm ĐĂNG NHẬP)
     * So sánh mật khẩu an toàn (hỗ trợ cả mật khẩu băm SHA-256 + Salt lẫn mật khẩu mẫu)
     * 
     * @param username Tên đăng nhập
     * @param plainPassword Mật khẩu người dùng nhập
     * @return Đối tượng User nếu hợp lệ, ngược lại trả về null
     */
    public static User selectByCredentials(String username, String plainPassword) {
        if (username == null || plainPassword == null || username.trim().isEmpty() || plainPassword.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    String storedPassword = user.getPassword();

                    // Kiểm tra bằng PasswordUtil (SHA-256 + Salt) hoặc so sánh trực tiếp
                    if (PasswordUtil.verifyPassword(plainPassword.trim(), storedPassword) 
                            || plainPassword.trim().equals(storedPassword)) {
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xác thực người dùng: " + username, e);
        }
        return null;
    }

    /**
     * Hàm 2: Tìm người dùng theo ID khóa chính
     * 
     * @param id Khóa chính định danh người dùng
     * @return Đối tượng User nếu tìm thấy, ngược lại trả về null
     */
    public static User selectById(int id) {
        if (id <= 0) {
            return null;
        }

        String sql = "SELECT * FROM users WHERE id = ? LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi truy vấn User theo ID: " + id, e);
        }
        return null;
    }

    /**
     * Hàm 3: Tìm người dùng theo Username chính xác
     * 
     * @param username Tên đăng nhập cần tìm
     * @return Đối tượng User nếu tồn tại, ngược lại trả về null
     */
    public static User selectByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?) LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi truy vấn User theo Username: " + username, e);
        }
        return null;
    }

    /**
     * Hàm 4: Tìm kiếm người dùng theo Username HOẶC Email (Dùng khi kiểm tra trùng hoặc mời thành viên)
     * 
     * @param keyword Từ khóa tìm kiếm (Username hoặc Email)
     * @return Đối tượng User nếu tìm thấy, ngược lại trả về null
     */
    public static User selectByUsernameOrEmail(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?) OR LOWER(email) = LOWER(?) LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String clean = keyword.trim();
            ps.setString(1, clean);
            ps.setString(2, clean);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi truy vấn User theo Username hoặc Email: " + keyword, e);
        }
        return null;
    }

    /**
     * Hàm 5: Thêm người dùng mới vào Supabase (Dùng khi ĐĂNG KÝ)
     * Tự động băm mật khẩu bằng SHA-256 + Salt trước khi ghi xuống CSDL
     * 
     * @param user Đối tượng User chứa thông tin đăng ký
     * @return ID tự tăng được tạo mới trong CSDL, hoặc 0 nếu thất bại
     */
    public static int insert(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return 0;
        }

        String sql = "INSERT INTO users (username, password, full_name, email, role, avatar, bio, skills, github_url, linkedin_url) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        // Tự động băm mật khẩu nếu chưa được băm
        String rawPassword = user.getPassword();
        String hashedPassword = (rawPassword != null && rawPassword.length() == 64) 
                ? rawPassword 
                : PasswordUtil.hashPassword(rawPassword != null ? rawPassword : "");

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername().trim());
            ps.setString(2, hashedPassword);
            ps.setString(3, user.getFullName() != null && !user.getFullName().trim().isEmpty() ? user.getFullName().trim() : user.getUsername().trim());
            ps.setString(4, user.getEmail() != null ? user.getEmail().trim() : "");
            ps.setString(5, user.getRole() != null && !user.getRole().trim().isEmpty() ? user.getRole().trim() : "Developer");
            ps.setString(6, user.getAvatar() != null && !user.getAvatar().trim().isEmpty() ? user.getAvatar().trim() : "images/default_avatar.png");
            ps.setString(7, user.getBio() != null ? user.getBio().trim() : "");
            ps.setString(8, user.getSkills() != null ? user.getSkills().trim() : "");
            ps.setString(9, user.getGithubUrl() != null ? user.getGithubUrl().trim() : "");
            ps.setString(10, user.getLinkedinUrl() != null ? user.getLinkedinUrl().trim() : "");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    user.setId(generatedId);
                    user.setPassword(hashedPassword);
                    return generatedId;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi chèn User mới: " + user.getUsername(), e);
        }
        return 0;
    }

    /**
     * Hàm 6: Cập nhật thông tin Hồ Sơ Cá Nhân của Người Dùng
     * 
     * @param updatedUser Đối tượng User chứa thông tin mới cần cập nhật
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public static boolean update(User updatedUser) {
        if (updatedUser == null || updatedUser.getId() <= 0) {
            return false;
        }

        String sql = "UPDATE users SET full_name = ?, role = ?, avatar = ?, bio = ?, skills = ?, github_url = ?, linkedin_url = ? " +
                     "WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, updatedUser.getFullName() != null ? updatedUser.getFullName().trim() : "");
            ps.setString(2, updatedUser.getRole() != null ? updatedUser.getRole().trim() : "Developer");
            ps.setString(3, updatedUser.getAvatar() != null ? updatedUser.getAvatar().trim() : "images/default_avatar.png");
            ps.setString(4, updatedUser.getBio() != null ? updatedUser.getBio().trim() : "");
            ps.setString(5, updatedUser.getSkills() != null ? updatedUser.getSkills().trim() : "");
            ps.setString(6, updatedUser.getGithubUrl() != null ? updatedUser.getGithubUrl().trim() : "");
            ps.setString(7, updatedUser.getLinkedinUrl() != null ? updatedUser.getLinkedinUrl().trim() : "");
            ps.setInt(8, updatedUser.getId());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật User ID: " + updatedUser.getId(), e);
        }
        return false;
    }

    /**
     * Hàm 7: Lấy danh sách toàn bộ người dùng trong hệ thống
     * 
     * @return Danh sách List<User> sắp xếp theo ID tăng dần (không bao giờ null)
     */
    public static List<User> selectAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id ASC";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy danh sách toàn bộ User", e);
        }
        return list;
    }
}
