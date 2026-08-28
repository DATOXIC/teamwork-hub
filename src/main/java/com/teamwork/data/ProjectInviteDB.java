package com.teamwork.data;

import com.teamwork.business.ProjectInvite;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tầng Data Layer: Quản lý Kho Lời Mời & Yêu Cầu Xin Gia Nhập 2 Chiều (In-Memory RAM Data Layer).
 * - Lưu trữ các lời mời PENDING, ACCEPTED, REJECTED, REVOKED, EXPIRED
 * - Chống gửi trùng lặp yêu cầu đang chờ (hasPendingInvite)
 * - Cung cấp danh sách lời mời cho Dashboard của Thành viên và Trang Quản lý của PM
 * - Đảm bảo an toàn đa luồng (Thread-Safe)
 */
public class ProjectInviteDB {

    // 1. Danh sách tĩnh luồng an toàn lưu trữ các Lời mời / Yêu cầu trên RAM
    private static List<ProjectInvite> invites = new CopyOnWriteArrayList<>();
    private static int nextId = 1;

    // 2. Khối khởi tạo tĩnh (Static Initializer): Seed Data mẫu để kiểm thử ngay
    static {
        // Lời mời mẫu 1: PM Admin (ID=1) mời bạn Lê Văn Chi (ID=4) vào Dự án 1 ("TW-HUB-01")
        invites.add(new ProjectInvite(
            nextId++,
            1,
            "Website E-Commerce TeamWork",
            "TW-HUB-01",
            "INVITATION",
            1,
            "Trưởng Nhóm Admin",
            4,
            "Lê Văn Chi",
            "PENDING",
            "2026-08-25 10:00",
            "2026-09-01 10:00" // Hết hạn sau 7 ngày
        ));
    }

    /**
     * Hàm 1: Thêm Lời mời / Yêu cầu mới vào kho RAM
     */
    public static int insert(ProjectInvite invite) {
        if (invite != null) {
            invite.setId(nextId++);
            invites.add(invite);
            return invite.getId();
        }
        return 0;
    }

    /**
     * Hàm 2: Tìm Lời mời theo ID duy nhất
     */
    public static ProjectInvite selectById(int id) {
        for (ProjectInvite pi : invites) {
            if (pi.getId() == id) {
                return pi;
            }
        }
        return null;
    }

    /**
     * Hàm 3: Lấy danh sách Lời mời / Yêu cầu đang PENDING mà người dùng này CẦN DUYỆT
     * Phục vụ hiển thị Hộp thư nhận lời mời trên Dashboard của User (Phương án A)
     */
    public static List<ProjectInvite> selectPendingByReceiverId(int receiverId) {
        List<ProjectInvite> result = new ArrayList<>();
        for (ProjectInvite pi : invites) {
            if (pi.getReceiverId() == receiverId && "PENDING".equalsIgnoreCase(pi.getStatus())) {
                // Tự động kiểm tra quá hạn
                if (pi.isExpired()) {
                    pi.setStatus("EXPIRED");
                } else {
                    result.add(pi);
                }
            }
        }
        return result;
    }

    /**
     * Hàm 4: Lấy danh sách tất cả Lời mời / Yêu cầu của một Dự Án cụ thể
     * Phục vụ PM xem và quản lý trong Modal "Đội ngũ dự án" (kèm nút Hủy / Thu hồi)
     */
    public static List<ProjectInvite> selectByProjectId(int projectId) {
        List<ProjectInvite> result = new ArrayList<>();
        for (ProjectInvite pi : invites) {
            if (pi.getProjectId() == projectId) {
                if (pi.isExpired() && "PENDING".equalsIgnoreCase(pi.getStatus())) {
                    pi.setStatus("EXPIRED");
                }
                result.add(pi);
            }
        }
        return result;
    }

    /**
     * Hàm 5: Kiểm tra xem đã có Lời mời / Yêu cầu PENDING giữa 2 người trong dự án này chưa
     * Phục vụ Ràng buộc 2: Chống gửi trùng lặp lời mời khi người kia chưa phản hồi
     */
    public static boolean hasPendingInvite(int projectId, int senderId, int receiverId) {
        for (ProjectInvite pi : invites) {
            if (pi.getProjectId() == projectId 
                && pi.getSenderId() == senderId 
                && pi.getReceiverId() == receiverId 
                && "PENDING".equalsIgnoreCase(pi.getStatus())) {
                
                if (!pi.isExpired()) {
                    return true;
                } else {
                    pi.setStatus("EXPIRED");
                }
            }
        }
        return false;
    }

    /**
     * Hàm 6: Cập nhật trạng thái Lời mời ("ACCEPTED", "REJECTED", "REVOKED", "EXPIRED")
     */
    public static void updateStatus(int id, String newStatus) {
        ProjectInvite pi = selectById(id);
        if (pi != null && newStatus != null) {
            pi.setStatus(newStatus.trim().toUpperCase());
        }
    }

    /**
     * Hàm 7: Xóa lời mời khỏi kho RAM
     */
    public static boolean delete(int id) {
        return invites.removeIf(pi -> pi.getId() == id);
    }
}
