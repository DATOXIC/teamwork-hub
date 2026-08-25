package com.teamwork.data;

// Import class User từ ngăn tủ business để làm việc
import com.teamwork.business.User;
import java.util.ArrayList;
import java.util.List;

public class UserDB {
    
    // 1. Khai báo danh sách liên kết tĩnh (static List) để làm Database tạm thời trên RAM
    private static List<User> users = new ArrayList<>();
    private static int nextId = 1; // Biến static tự tăng để cấp ID cho user mới

    // 2. Khối khởi tạo tĩnh (Static Initializer Block)
    // Chạy duy nhất 1 lần khi Tomcat nạp Class này.
    // Dùng để tạo sẵn 2 tài khoản mẫu (Seed Data) để test chức năng đăng nhập ngay.
    static {
        // Tài khoản 1: Trưởng nhóm (ADMIN)
        users.add(new User(
            nextId++, 
            "admin", 
            "admin123", 
            "Trưởng Nhóm Admin", 
            "admin@teamwork.com", 
            "ADMIN", 
            "images/default_avatar.png"
        ));

        // Tài khoản 2: Thành viên (MEMBER)
        users.add(new User(
            nextId++, 
            "member1", 
            "pass123", 
            "Nguyễn Văn An", 
            "an.nv@teamwork.com", 
            "MEMBER", 
            "images/default_avatar.png"
        ));
    }

    /**
     * Hàm 1: Xác thực tài khoản (Dùng khi người dùng bấm ĐĂNG NHẬP)
     * Tìm xem có user nào khớp cả username và password hay không.
     */
    public static User selectByCredentials(String username, String password) {
        for (User u : users) {
            // Dùng equals() để so sánh chuỗi trong Java
            if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
                return u; // Khớp -> Trả về đối tượng User đầy đủ thông tin
            }
        }
        return null; // Không khớp -> Trả về null
    }

    public static User selectById(int id) 
    {
        for ( User u: users)
        {
            if(u.getId() == id)
                return u;
        }
        return null;
    }

    /**
     * Hàm 2: Kiểm tra sự tồn tại (Dùng khi người dùng ĐĂNG KÝ)
     * Đảm bảo không cho phép đăng ký tài khoản trùng tên.
     */
    public static User selectByUsername(String username) {
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username)) {
                return u; // Đã tồn tại user trùng tên
            }
        }
        return null; // Username này hợp lệ (chưa ai dùng)
    }

    /**
     * Hàm 3: Thêm người dùng mới (Dùng khi ĐĂNG KÝ thành công)
     */
    public static int insert(User user) 
    {
        user.setId(nextId++); // Gán ID tự tăng tự động
        users.add(user);      // Thêm vào danh sách RAM
        return user.getId();  // Trả về ID vừa tạo
    }

    /**
     * Hàm 4: Lấy danh sách toàn bộ người dùng (Dùng để hiển thị thành viên trong dự án)
     */
    public static List<User> selectAll() {
        // Trả về một bản sao của danh sách để tránh việc bên ngoài chỉnh sửa trực tiếp danh sách gốc
        return new ArrayList<>(users);
    }
}
