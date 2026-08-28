package com.teamwork.data;

import com.teamwork.business.User;
import com.teamwork.util.PasswordUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tầng Data Layer: Quản lý Kho Người Dùng (In-Memory Database trên RAM)
 * - Quản lý tài khoản đăng nhập, đăng ký và tìm kiếm người dùng theo username/email
 * - Cung cấp hàm update(User) để lưu lại thay đổi hồ sơ cá nhân
 * - Bảo mật mật khẩu bằng thuật toán băm SHA-256 + Salt
 * - Đảm bảo Thread-Safe khi nhiều thread truy cập đồng thời
 */
public class UserDB {
    
    // 1. Danh sách luồng an toàn (Thread-Safe) để làm Database tạm thời trên RAM
    private static List<User> users = new CopyOnWriteArrayList<>();
    private static int nextId = 1;

    // 2. Khối khởi tạo tĩnh: Tạo sẵn 4 tài khoản mẫu kèm mật khẩu đã được băm SHA-256 + Salt
    static {
        // Tài khoản 1: Trưởng nhóm (ADMIN) - Mật khẩu: admin123
        users.add(new User(
            nextId++, 
            "admin", 
            PasswordUtil.hashPassword("admin123"), 
            "Trưởng Nhóm Admin", 
            "admin@teamwork.com", 
            "Project Manager", 
            "images/default_avatar.png",
            "Trưởng dự án, chuyên gia quản lý tiến độ, phân quyền kiến trúc hệ thống và điều phối nhóm.",
            "Project Management, Agile, Scrum, Java, Architecture",
            "https://github.com",
            "https://linkedin.com"
        ));

        // Tài khoản 2: Thành viên Nguyễn Văn An - Mật khẩu: pass123
        users.add(new User(
            nextId++, 
            "member1", 
            PasswordUtil.hashPassword("pass123"), 
            "Nguyễn Văn An", 
            "an@teamwork.com", 
            "Senior Backend Developer", 
            "images/default_avatar.png",
            "Đam mê kiến trúc Clean Architecture, tối ưu hóa Jakarta Servlet & Cơ sở dữ liệu quan hệ.",
            "Java, Jakarta EE, MySQL, Docker, RESTful API",
            "https://github.com",
            "https://linkedin.com"
        ));

        // Tài khoản 3: Thành viên Trần Thị Bình - Mật khẩu: pass123
        users.add(new User(
            nextId++, 
            "binh", 
            PasswordUtil.hashPassword("pass123"), 
            "Trần Thị Bình", 
            "binh@teamwork.com", 
            "UI/UX Designer & Frontend", 
            "images/default_avatar.png",
            "Chuyên gia thiết kế trải nghiệm người dùng, Design System và xây dựng giao diện hiện đại.",
            "UI/UX, Figma, HTML/CSS, Bootstrap, JavaScript",
            "https://github.com",
            "https://linkedin.com"
        ));

        // Tài khoản 4: Thành viên Lê Văn Chi - Mật khẩu: pass123
        users.add(new User(
            nextId++, 
            "chi", 
            PasswordUtil.hashPassword("pass123"), 
            "Lê Văn Chi", 
            "chi@teamwork.com", 
            "QA Engineer & Tester", 
            "images/default_avatar.png",
            "Kỹ sư kiểm thử phần mềm, đảm bảo chất lượng, bảo mật luồng nghiệp vụ và tính đúng đắn dữ liệu.",
            "Software Testing, QA, JUnit, Test Automation, Git",
            "https://github.com",
            "https://linkedin.com"
        ));
    }

    /**
     * Hàm 1: Xác thực tài khoản (Dùng khi người dùng bấm ĐĂNG NHẬP)
     * So sánh mật khẩu an toàn theo thời gian không đổi (Constant-Time Compare)
     */
    public static User selectByCredentials(String username, String plainPassword) {
        if (username == null || plainPassword == null) {
            return null;
        }
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username.trim())) {
                if (PasswordUtil.verifyPassword(plainPassword.trim(), u.getPassword())) {
                    return u;
                }
            }
        }
        return null;
    }

    /**
     * Hàm 2: Tìm người dùng theo ID
     */
    public static User selectById(int id) {
        for (User u : users) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }

    /**
     * Hàm 3: Tìm người dùng theo Username chính xác
     */
    public static User selectByUsername(String username) {
        if (username == null) return null;
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username.trim())) {
                return u;
            }
        }
        return null;
    }

    /**
     * Hàm 4: Tìm kiếm người dùng theo Username HOẶC Email
     */
    public static User selectByUsernameOrEmail(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        String cleanKeyword = keyword.trim();
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(cleanKeyword) || u.getEmail().equalsIgnoreCase(cleanKeyword)) {
                return u;
            }
        }
        return null;
    }

    /**
     * Hàm 5: Thêm người dùng mới (Dùng khi ĐĂNG KÝ)
     * Tự động mã hóa băm mật khẩu trước khi lưu vào RAM
     */
    public static int insert(User user) {
        if (user == null) return 0;
        user.setId(nextId++);
        if (user.getPassword() != null && !user.getPassword().isEmpty() && user.getPassword().length() != 64) {
            user.setPassword(PasswordUtil.hashPassword(user.getPassword()));
        }
        users.add(user);
        return user.getId();
    }

    /**
     * Hàm 6: Cập nhật thông tin Hồ Sơ Cá Nhân của Người Dùng
     */
    public static boolean update(User updatedUser) {
        if (updatedUser == null) {
            return false;
        }
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            if (u.getId() == updatedUser.getId()) {
                u.setFullName(updatedUser.getFullName());
                u.setRole(updatedUser.getRole());
                u.setAvatar(updatedUser.getAvatar());
                u.setBio(updatedUser.getBio());
                u.setSkills(updatedUser.getSkills());
                u.setGithubUrl(updatedUser.getGithubUrl());
                u.setLinkedinUrl(updatedUser.getLinkedinUrl());
                return true;
            }
        }
        return false;
    }

    /**
     * Hàm 7: Lấy danh sách toàn bộ người dùng
     */
    public static List<User> selectAll() {
        return new ArrayList<>(users);
    }
}
