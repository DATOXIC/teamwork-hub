package com.teamwork.data;

import com.teamwork.business.Doc;
import java.util.ArrayList;
import java.util.List;

/**
 * Tầng Data Layer: Quản lý kho dữ liệu Tài liệu & Ghi chú Wiki (In-Memory Doc Database trên RAM)
 * Cung cấp đầy đủ các thao tác CRUD: Lấy danh sách bài viết theo dự án, Đọc chi tiết, Thêm mới, Cập nhật và Xóa bài.
 */
public class DocDB {

    // 1. Danh sách tĩnh lưu trữ toàn bộ bài viết tài liệu trên RAM
    private static List<Doc> docs = new ArrayList<>();
    private static int nextId = 1; // Biến tự tăng cấp ID cho bài viết mới

    // 2. Khối khởi tạo tĩnh (Static Initializer): Tạo sẵn các bài viết mẫu (Seed Data)
    static {
        // --- CÁC BÀI VIẾT MẪU CHO DỰ ÁN 1 (projectId = 1) ---
        
        // Bài 1: Quy chuẩn code
        docs.add(new Doc(
            nextId++,
            1, // projectId = 1
            "Quy chuẩn phát triển Web MVC Model 2",
            "1. CẤU TRÚC PHÂN TẦNG:\n"
            + "- Model (com.teamwork.business): Định nghĩa các JavaBean thuần túy có Serializable.\n"
            + "- Data Layer (com.teamwork.data): Quản lý kho dữ liệu CRUD trên RAM.\n"
            + "- Controller (com.teamwork.controllers): Kế thừa HttpServlet, điều phối luồng doGet và doPost.\n"
            + "- View (src/main/webapp): Dùng thuần thẻ JSTL <c:...> và Jakarta EL ${...}, TUYỆT ĐỐI KHÔNG dùng Scriptlet Java <% ... %>.\n\n"
            + "2. QUY TẮC ĐIỀU PHỐI URL (PRG PATTERN):\n"
            + "- Mọi thao tác ghi dữ liệu (POST) sau khi xử lý xong bắt buộc phải gọi response.sendRedirect() về lại lệnh GET để tránh việc người dùng bấm F5 bị lặp lại dữ liệu.",
            1, // authorId = 1 (Trưởng Nhóm Admin)
            "Trưởng Nhóm Admin",
            "15/08/2026 09:00",
            "15/08/2026 09:00"
        ));

        // Bài 2: Biên bản họp
        docs.add(new Doc(
            nextId++,
            1,
            "Biên bản họp Kickoff Dự án & Phân công nhiệm vụ",
            "THÔNG TIN CUỘC HỌP:\n"
            + "- Thời gian: 14:00 - 15:30 ngày 18/08/2026.\n"
            + "- Thành phần tham dự: Trưởng Nhóm Admin, Nguyễn Văn An.\n\n"
            + "NỘI DUNG THỐNG NHẤT:\n"
            + "1. Sprint 1 & 2: Hoàn thành Đăng nhập, Session Filter và Dashboard dự án.\n"
            + "2. Sprint 3: Hoàn thành Bảng Kanban kéo thả HTML5 Drag & Drop.\n"
            + "3. Sprint 4: Triển khai phân hệ Wiki tài liệu nhóm phong cách Notion.\n"
            + "4. Sprint 5: Tích hợp kênh Chat thảo luận realtime theo dự án.",
            2, // authorId = 2 (Nguyễn Văn An)
            "Nguyễn Văn An",
            "18/08/2026 14:30",
            "18/08/2026 14:30"
        ));

        // Bài 3: Hướng dẫn Deploy
        docs.add(new Doc(
            nextId++,
            1,
            "Hướng dẫn đóng gói Docker & Triển khai lên Render.com",
            "CÁC BƯỚC TRIỂN KHAI CLOUD:\n"
            + "1. Dockerfile Multi-stage: Dùng Maven JDK 21 để build file teamwork-hub-1.0-SNAPSHOT.war.\n"
            + "2. Runtime: Copy sang image Tomcat 10.1 và đổi tên thành ROOT.war để chạy trực tiếp tại root URL.\n"
            + "3. Port cấu hình: EXPOSE 8080 để Render tự động định tuyến tên miền.\n"
            + "4. Tự động hóa: Mỗi khi git push origin main, Render sẽ tự động kéo code về và build lại.",
            1,
            "Trưởng Nhóm Admin",
            "22/08/2026 16:00",
            "22/08/2026 16:00"
        ));

        // --- BÀI VIẾT MẪU CHO DỰ ÁN 2 (projectId = 2) ---
        docs.add(new Doc(
            nextId++,
            2, // projectId = 2
            "Tài liệu thiết kế giao diện Mobile App với Flutter",
            "BẢN THIẾT KẾ GIAO DIỆN DI ĐỘNG:\n"
            + "- Áp dụng Material Design 3.\n"
            + "- Tương thích đa nền tảng iOS & Android.\n"
            + "- Sử dụng Riverpod để quản lý State tập trung.",
            1,
            "Trưởng Nhóm Admin",
            "20/08/2026 10:00",
            "20/08/2026 10:00"
        ));
    }

    /**
     * HÀM 1: Lấy danh sách tất cả các bài viết trong hệ thống
     */
    public static List<Doc> selectAll() {
        return new ArrayList<>(docs);
    }

    /**
     * HÀM 2: Lấy danh sách toàn bộ tài liệu thuộc về MỘT DỰ ÁN cụ thể
     * Dùng để đổ danh mục bài viết ở cột bên trái của giao diện docs.jsp.
     */
    public static List<Doc> selectByProjectId(int projectId) {
        List<Doc> resultList = new ArrayList<>();
        for (Doc d : docs) {
            if (d.getProjectId() == projectId) {
                resultList.add(d);
            }
        }
        return resultList;
    }

    /**
     * HÀM 3: Tìm một bài viết chi tiết theo ID của bài viết
     * Dùng khi người dùng bấm vào một bài viết từ danh mục để đọc nội dung.
     */
    public static Doc selectById(int id) {
        for (Doc d : docs) {
            if (d.getId() == id) {
                return d; // Tìm thấy bài viết
            }
        }
        return null; // Không tìm thấy
    }

    /**
     * HÀM 4: Thêm một bài viết tài liệu mới vào dự án
     * Dùng khi người dùng bấm nút "+ Tạo tài liệu mới" (UC09)
     */
    public static int insert(Doc doc) {
        doc.setId(nextId++); // Cấp ID tự động tăng
        docs.add(doc);       // Cất vào danh sách trên RAM
        return doc.getId();  // Trả về ID của bài vừa tạo
    }

    /**
     * HÀM 5: Cập nhật nội dung bài viết sau khi chỉnh sửa
     * Dùng khi người dùng sửa tiêu đề hoặc nội dung và bấm "Lưu thay đổi"
     */
    public static boolean update(Doc updatedDoc) {
        for (Doc d : docs) {
            if (d.getId() == updatedDoc.getId()) {
                d.setTitle(updatedDoc.getTitle());
                d.setContent(updatedDoc.getContent());
                d.setAuthorId(updatedDoc.getAuthorId());
                d.setAuthorName(updatedDoc.getAuthorName());
                d.setUpdatedAt(updatedDoc.getUpdatedAt()); // Cập nhật mốc thời gian sửa đổi
                return true; // Cập nhật thành công
            }
        }
        return false; // Không tìm thấy bài viết để sửa
    }

    /**
     * HÀM 6: Xóa một bài viết tài liệu khỏi dự án
     * Dùng khi người dùng bấm nút Xóa bài
     */
    public static boolean delete(int docId) {
        for (int i = 0; i < docs.size(); i++) {
            Doc d = docs.get(i);
            if (d.getId() == docId) {
                docs.remove(i); // Xóa khỏi danh sách RAM
                return true;    // Xóa thành công
            }
        }
        return false; // Không tìm thấy bài viết để xóa
    }

    /**
     * HÀM 7: Đếm tổng số tài liệu của một dự án
     */
    public static int countDocs(int projectId) {
        int count = 0;
        for (Doc d : docs) {
            if (d.getProjectId() == projectId) {
                count = count + 1;
            }
        }
        return count;
    }
}
