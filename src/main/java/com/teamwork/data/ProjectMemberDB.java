package com.teamwork.data;

import com.teamwork.business.Project;
import com.teamwork.business.ProjectMember;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tầng Data Layer: Quản lý Danh Sách Thành Viên của từng Dự Án (In-Memory RAM Data Layer).
 * - Cung cấp hàm kiểm tra Quota (countMembers <= 10)
 * - Cung cấp hàm chống trùng lặp thành viên (isMember)
 * - Cung cấp danh sách các dự án của một người dùng (selectProjectsByUserId)
 * - Đảm bảo an toàn đa luồng (Thread-Safe)
 */
public class ProjectMemberDB {

    // 1. Danh sách tĩnh luồng an toàn lưu trữ các mối quan hệ Thành Viên - Dự Án trên RAM
    private static List<ProjectMember> members = new CopyOnWriteArrayList<>();

    // 2. Khối khởi tạo tĩnh (Static Initializer): Seed Data mẫu ban đầu
    static {
        // DỰ ÁN 1: "TW-HUB-01" (Website E-Commerce TeamWork) -> Có 3 thành viên
        members.add(new ProjectMember(1, 1, "Trưởng Nhóm Admin", "admin@teamwork.com", "Project Manager", "OWNER", "2026-08-01 08:00"));
        members.add(new ProjectMember(1, 2, "Nguyễn Văn An", "an@teamwork.com", "Developer", "MEMBER", "2026-08-02 09:30"));
        members.add(new ProjectMember(1, 3, "Trần Thị Bình", "binh@teamwork.com", "Designer", "MEMBER", "2026-08-05 14:15"));

        // DỰ ÁN 2: "ECOMMERCE-99" (Mobile App Quản Lý Công Việc) -> Có 2 thành viên
        members.add(new ProjectMember(2, 1, "Trưởng Nhóm Admin", "admin@teamwork.com", "Project Manager", "OWNER", "2026-08-15 10:00"));
        members.add(new ProjectMember(2, 4, "Lê Văn Chi", "chi@teamwork.com", "Tester", "MEMBER", "2026-08-16 11:20"));
    }

    /**
     * Hàm 1: Lấy toàn bộ danh sách thành viên của một dự án cụ thể
     * Phục vụ hiển thị Modal "Đội ngũ dự án (X/10)" và Form phân công công việc
     */
    public static List<ProjectMember> selectByProjectId(int projectId) {
        List<ProjectMember> result = new ArrayList<>();
        for (ProjectMember pm : members) {
            if (pm.getProjectId() == projectId) {
                result.add(pm);
            }
        }
        return result;
    }

    /**
     * Hàm 2: Lấy tất cả các Dự án mà một người dùng đang tham gia
     * Phục vụ hiển thị Dashboard của người dùng đó
     */
    public static List<Project> selectProjectsByUserId(int userId) {
        List<Project> result = new ArrayList<>();
        for (ProjectMember pm : members) {
            if (pm.getUserId() == userId) {
                Project p = ProjectDB.selectById(pm.getProjectId());
                if (p != null && !result.contains(p)) {
                    result.add(p);
                }
            }
        }
        return result;
    }

    /**
     * Hàm 3: Kiểm tra xem một người dùng đã là thành viên của dự án hay chưa
     * Phục vụ Ràng buộc 2: Chống mời trùng lặp người đã ở trong dự án
     */
    public static boolean isMember(int projectId, int userId) {
        for (ProjectMember pm : members) {
            if (pm.getProjectId() == projectId && pm.getUserId() == userId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hàm 4: Đếm tổng số lượng thành viên hiện tại của một dự án
     * Phục vụ Ràng buộc 5: Chặn khi đạt Quota tối đa 10 người/dự án
     */
    public static int countMembers(int projectId) {
        int count = 0;
        for (ProjectMember pm : members) {
            if (pm.getProjectId() == projectId) {
                count++;
            }
        }
        return count;
    }

    /**
     * Hàm 5: Thêm thành viên mới vào dự án
     * Dùng khi thành viên bấm "Chấp nhận lời mời" hoặc PM bấm "Duyệt xin gia nhập"
     */
    public static void insert(ProjectMember member) {
        if (member != null && !isMember(member.getProjectId(), member.getUserId())) {
            members.add(member);
        }
    }

    /**
     * Hàm 6: Xóa thành viên ra khỏi dự án
     * Dùng khi PM thu hồi quyền hoặc thành viên rời dự án
     */
    public static boolean delete(int projectId, int userId) {
        return members.removeIf(pm -> pm.getProjectId() == projectId && pm.getUserId() == userId);
    }
}
