package com.teamwork.data;

import com.teamwork.business.User;
import java.util.ArrayList;
import java.util.List;

/**
 * Tầng Data Layer: Quản lý Kho Người Dùng (In-Memory Database trên RAM)
 * - Quản lý tài khoản đăng nhập, đăng ký và tìm kiếm người dùng theo username/email
 */
public class UserDB {
    
    // 1. Danh sách liên kết tĩnh để làm Database tạm thời trên RAM
    private static List<User> users = new ArrayList<>();
    private static int nextId = 1;

    // 2. Khối khởi tạo tĩnh: Tạo sẵn các tài khoản mẫu đồng bộ với hệ thống
    static {
        // Tài khoản 1: Trưởng nhóm (ADMIN)
        users.add(new User(
            nextId++, 
            "admin", 
            "admin123", 
            "Trưởng Nhóm Admin", 
            "admin@teamwork.com", 
            "Project Manager", 
            "images/default_avatar.png"
        ));

        // Tài khoản 2: Thành viên Nguyễn Văn An
        users.add(new User(
            nextId++, 
            "member1", 
            "pass123", 
            "Nguyễn Văn An", 
            "an@teamwork.com", 
            "Developer", 
            "images/default_avatar.png"
        ));

        // Tài khoản 3: Thành viên Trần Thị Bình
        users.add(new User(
            nextId++, 
            "binh", 
            "pass123", 
            "Trần Thị Bình", 
            "binh@teamwork.com", 
            "Designer", 
            "images/default_avatar.png"
        ));

        // Tài khoản 4: Thành viên Lê Văn Chi
        users.add(new User(
            nextId++, 
            "chi", 
            "pass123", 
            "Lê Văn Chi", 
            "chi@teamwork.com", 
            "Tester", 
            "images/default_avatar.png"
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
     * Phục vụ Ràng buộc 3 của Luồng Mời Thành Viên
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
     * Hàm 6: Lấy danh sách toàn bộ người dùng
     */
    public static List<User> selectAll() {
        return new ArrayList<>(users);
    }
}
