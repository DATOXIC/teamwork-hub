package com.teamwork.controllers;

import com.teamwork.business.Project;
import com.teamwork.business.User;
import com.teamwork.data.ProjectMemberDB;

/**
 * NƠI DUY NHẤT trả lời câu hỏi "người này có quyền gì trong dự án?".
 *
 * <p>Mọi servlet và handler gọi qua đây thay vì tự so sánh {@code ownerId} hay tự gọi
 * {@link ProjectMemberDB#isMember}. Muốn đổi luật phân quyền chỉ cần sửa một file.</p>
 *
 * <p>Vai trò trong dự án (theo quyết định của nhóm):</p>
 * <ul>
 *   <li><b>Trưởng dự án (PM)</b> = người tạo dự án ({@code project.ownerId}): tạo/giao task, duyệt task, quản lý thành viên.</li>
 *   <li><b>Thành viên</b>: xem dự án, làm việc được giao, chat, tài liệu.</li>
 * </ul>
 * Quyền ADMIN của hệ thống KHÔNG cho phép xem dự án của người khác.
 */
public final class ProjectAccess {

    private ProjectAccess() {}

    /** Người dùng có thuộc dự án không (PM cũng là thành viên). An toàn với user null / id không hợp lệ. */
    public static boolean isMember(User user, int projectId) {
        return user != null && projectId > 0 && ProjectMemberDB.isMember(projectId, user.getId());
    }

    /** Người dùng có phải Trưởng dự án (PM) không. */
    public static boolean isOwner(User user, Project project) {
        return user != null && project != null && user.getId() == project.getOwnerId();
    }

    /** Một userId bất kỳ có phải PM của dự án không (dùng khi xét người khác, ví dụ không cho kick PM). */
    public static boolean isOwnerId(int userId, Project project) {
        return project != null && userId > 0 && userId == project.getOwnerId();
    }

    /** Tác giả nội dung (tài liệu, tin nhắn...) hoặc PM mới được sửa/xóa nội dung đó. */
    public static boolean isAuthorOrOwner(User user, int authorId, Project project) {
        return user != null && (user.getId() == authorId || isOwner(user, project));
    }
}
