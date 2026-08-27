package com.teamwork.data;

import com.teamwork.business.User;
import java.util.ArrayList;
import java.util.List;

/**
 * Tầng Data Layer: Quản lý Kho Người Dùng (In-Memory Database trên RAM)
 * - Quản lý tài khoản đăng nhập, đăng ký và tìm kiếm người dùng theo username/email
 * - Cung cấp hàm update(User) để lưu lại thay đổi hồ sơ cá nhân
 */
public class UserDB {
    
    // 1. Danh sách liên kết tĩnh để làm Database tạm thời trên RAM
    private static List<User> users = new ArrayList<>();
    private static int nextId = 1;

    // 2. Khối khởi tạo tĩnh: Tạo sẵn 4 tài khoản mẫu kèm hồ sơ chuyên môn
    static {
        // Tài khoản 1: Trưởng nhóm (ADMIN)
        users.add(new User(
            nextId++, 
            "admin", 
            "admin123", 
            "Trưởng Nhóm Admin", 
            "admin@teamwork.com", 
            "Project Manager", 
            "images/default_avatar.png",
            "Trưởng dự án, chuyên gia quản lý tiến độ, phân quyền kiến trúc hệ thống và điều phối nhóm.",
            "Project Management, Agile, Scrum, Java, Architecture",
            "https://github.com",
            "https://linkedin.com"
        ));

        // Tài khoản 2: Thành viên Nguyễn Văn An
        users.add(new User(
            nextId++, 
            "member1", 
            "pass123", 
            "Nguyễn Văn An", 
            "an@teamwork.com", 
            "Senior Backend Developer", 
            "images/default_avatar.png",
            "Đam mê kiến trúc Clean Architecture, tối ưu hóa Jakarta Servlet & Cơ sở dữ liệu quan hệ.",
            "Java, Jakarta EE, MySQL, Docker, RESTful API",
            "https://github.com",
            "https://linkedin.com"
        ));

        // Tài khoản 3: Thành viên Trần Thị Bình
        users.add(new User(
            nextId++, 
            "binh", 
            "pass123", 
            "Trần Thị Bình", 
            "binh@teamwork.com", 
            "UI/UX Designer & Frontend", 
            "images/default_avatar.png",
            "Chuyên gia thiết kế trải nghiệm người dùng, Design System và xây dựng giao diện hiện đại.",
            "UI/UX, Figma, HTML/CSS, Bootstrap, JavaScript",
            "https://github.com",
            "https://linkedin.com"
        ));

        // Tài khoản 4: Thành viên Lê Văn Chi
        users.add(new User(
            nextId++, 
            "chi", 
            "pass123", 
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
     */
    public static User selectByCredentials(String username, String password) {
        for (User u : users) {
            if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
                return u;
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
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username)) {
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
     */
    public static int insert(User user) {
        user.setId(nextId++);
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
