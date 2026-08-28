package com.teamwork.controllers;

import com.teamwork.business.Project;
import com.teamwork.business.ProjectInvite;
import com.teamwork.business.ProjectMember;
import com.teamwork.business.User;
import com.teamwork.data.NotificationDB;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.ProjectInviteDB;
import com.teamwork.data.ProjectMemberDB;
import com.teamwork.data.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller: Quản lý Luồng Mời & Xin Gia Nhập Dự Án 2 Chiều (/invite)
 * - Thực thi nghiêm ngặt 5 Hàng Rào Bảo Mật (Authorization, Quota <= 10, Entity Existence, Expiration 7 days, Anti-duplicate)
 * - Tự động phát tín hiệu sang Trung Tâm Thông Báo (NotificationDB)
 */
@WebServlet("/invite")
public class ProjectInviteServlet extends HttpServlet {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        switch (action) {
            case "accept":
                handleAccept(request, response, currentUser);
                break;
            case "reject":
                handleReject(request, response, currentUser);
                break;
            case "revoke":
                handleRevoke(request, response, currentUser);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/project?action=list");
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        switch (action) {
            case "sendInvite":
                handleSendInvite(request, response, currentUser);
                break;
            case "requestJoin":
                handleRequestJoin(request, response, currentUser);
                break;
            case "accept":
                handleAccept(request, response, currentUser);
                break;
            case "reject":
                handleReject(request, response, currentUser);
                break;
            case "revoke":
                handleRevoke(request, response, currentUser);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/project?action=list");
                break;
        }
    }

    // =========================================================================
    // LUỒNG 1: PM CHỦ ĐỘNG MỜI THÀNH VIÊN (CHIỀU 1 - TOP-DOWN PUSH)
    // =========================================================================
    private void handleSendInvite(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        HttpSession session = request.getSession();
        int projectId = 0;
        try {
            projectId = Integer.parseInt(request.getParameter("projectId"));
        } catch (NumberFormatException e) {
            session.setAttribute("toastError", "Mã ID dự án không hợp lệ!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        String usernameOrEmail = request.getParameter("usernameOrEmail");
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            session.setAttribute("toastError", "Vui lòng nhập Username hoặc Email của người cần mời!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        Project project = ProjectDB.selectById(projectId);
        if (project == null) {
            session.setAttribute("toastError", "Dự án không tồn tại!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // --- RÀO BẢO MẬT 1: QUYỀN HẠN (Chỉ PM mới có quyền mời) ---
        if (project.getOwnerId() != currentUser.getId()) {
            session.setAttribute("toastError", "Chỉ Trưởng Dự Án (PM) mới có thẩm quyền gửi lời mời!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // --- RÀO BẢO MẬT 5: HẠN NGẠCH QUOTA (Tối đa 10 thành viên) ---
        if (ProjectMemberDB.countMembers(projectId) >= 10) {
            session.setAttribute("toastError", "Dự án đã đạt giới hạn tối đa 10 thành viên!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // --- RÀO BẢO MẬT 3: KIỂM TRA TỒN TẠI TÀI KHOẢN ---
        User targetUser = UserDB.selectByUsernameOrEmail(usernameOrEmail);
        if (targetUser == null) {
            session.setAttribute("toastError", "Không tìm thấy người dùng '" + usernameOrEmail + "' trong hệ thống!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // --- RÀO BẢO MẬT 2.A: CHỐNG TỰ MỜI CHÍNH MÌNH ---
        if (targetUser.getId() == currentUser.getId()) {
            session.setAttribute("toastError", "Bạn không thể tự gửi lời mời cho chính mình!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // --- RÀO BẢO MẬT 2.B: CHỐNG MỜI NGƯỜI ĐÃ LÀ THÀNH VIÊN ---
        if (ProjectMemberDB.isMember(projectId, targetUser.getId())) {
            session.setAttribute("toastError", "Người dùng '" + targetUser.getFullName() + "' đã là thành viên của dự án này rồi!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // --- RÀO BẢO MẬT 2.C: CHỐNG GỬI TRÙNG LỜI MỜI ĐANG PENDING ---
        if (ProjectInviteDB.hasPendingInvite(projectId, currentUser.getId(), targetUser.getId())) {
            session.setAttribute("toastError", "Đã có lời mời đang chờ '" + targetUser.getFullName() + "' phản hồi, không thể gửi trùng!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // TẤT CẢ HỢP LỆ -> TẠO LỜI MỜI MỚI (HẠN 7 NGÀY)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusDays(7);

        ProjectInvite invite = new ProjectInvite(
            0,
            projectId,
            project.getName(),
            project.getProjectCode(),
            "INVITATION",
            currentUser.getId(),
            currentUser.getFullName(),
            targetUser.getId(),
            targetUser.getFullName(),
            "PENDING",
            now.format(DATE_FORMATTER),
            expireTime.format(DATE_FORMATTER)
        );
        ProjectInviteDB.insert(invite);

        // BẮN THÔNG BÁO TỚI NGƯỜI NHẬN
        NotificationDB.send(
            targetUser.getId(),
            "Lời Mời Tham Gia Dự Án",
            currentUser.getFullName() + " đã mời bạn tham gia vào dự án [" + project.getName() + " (" + project.getProjectCode() + ")].",
            "/project?action=list",
            "INVITE"
        );

        session.setAttribute("toastSuccess", "Đã gửi lời mời tham gia dự án thành công tới " + targetUser.getFullName() + " (Hạn phản hồi: 7 ngày)!");
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    // =========================================================================
    // LUỒNG 2: THÀNH VIÊN NHẬP MÃ DỰ ÁN XIN GIA NHẬP (CHIỀU 2 - BOTTOM-UP PULL)
    // =========================================================================
    private void handleRequestJoin(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        HttpSession session = request.getSession();
        String projectCode = request.getParameter("projectCode");

        if (projectCode == null || projectCode.trim().isEmpty()) {
            session.setAttribute("toastError", "Vui lòng nhập Mã Dự Án (Project Code)!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // --- RÀO BẢO MẬT 3: TỒN TẠI MÃ DỰ ÁN ---
        Project project = ProjectDB.selectByCode(projectCode);
        if (project == null) {
            session.setAttribute("toastError", "Không tìm thấy dự án nào có mã '" + projectCode.toUpperCase() + "'!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // --- RÀO BẢO MẬT 5: QUOTA DỰ ÁN ---
        if (ProjectMemberDB.countMembers(project.getId()) >= 10) {
            session.setAttribute("toastError", "Dự án [" + project.getName() + "] đã đạt giới hạn tối đa 10 thành viên, không thể xin vào!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // --- RÀO BẢO MẬT 2.A: CHỐNG XIN VÀO DỰ ÁN DO CHÍNH MÌNH LÀM CHỦ ---
        if (project.getOwnerId() == currentUser.getId()) {
            session.setAttribute("toastError", "Bạn chính là Trưởng Dự Án của dự án [" + project.getName() + "] rồi!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // --- RÀO BẢO MẬT 2.B: CHỐNG XIN VÀO KHI ĐÃ LÀ THÀNH VIÊN ---
        if (ProjectMemberDB.isMember(project.getId(), currentUser.getId())) {
            session.setAttribute("toastError", "Bạn đã là thành viên chính thức của dự án [" + project.getName() + "] rồi!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // --- RÀO BẢO MẬT 2.C: CHỐNG GỬI TRÙNG YÊU CẦU ĐANG CHỜ ---
        if (ProjectInviteDB.hasPendingInvite(project.getId(), currentUser.getId(), project.getOwnerId())) {
            session.setAttribute("toastError", "Bạn đã gửi yêu cầu xin gia nhập dự án này trước đó, vui lòng chờ PM duyệt!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // TẤT CẢ HỢP LỆ -> TẠO YÊU CẦU XIN GIA NHẬP MỚI
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusDays(7);
        User owner = UserDB.selectById(project.getOwnerId());
        String ownerName = (owner != null) ? owner.getFullName() : "Trưởng Nhóm";

        ProjectInvite requestInvite = new ProjectInvite(
            0,
            project.getId(),
            project.getName(),
            project.getProjectCode(),
            "JOIN_REQUEST",
            currentUser.getId(),
            currentUser.getFullName(),
            project.getOwnerId(),
            ownerName,
            "PENDING",
            now.format(DATE_FORMATTER),
            expireTime.format(DATE_FORMATTER)
        );
        ProjectInviteDB.insert(requestInvite);

        // BẮN THÔNG BÁO TỚI PM
        NotificationDB.send(
            project.getOwnerId(),
            "Yêu Cầu Gia Nhập Dự Án",
            currentUser.getFullName() + " vừa gửi yêu cầu xin gia nhập dự án [" + project.getName() + "].",
            "/task?action=list&projectId=" + project.getId(),
            "INVITE"
        );

        session.setAttribute("toastSuccess", "Đã gửi yêu cầu xin gia nhập dự án [" + project.getName() + "] tới Trưởng Nhóm!");
        response.sendRedirect(request.getContextPath() + "/project?action=list");
    }

    // =========================================================================
    // LUỒNG 3: ĐỒNG Ý GIA NHẬP / DUYỆT VÀO NHÓM (ACCEPT)
    // =========================================================================
    private void handleAccept(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        HttpSession session = request.getSession();
        int inviteId = 0;
        try {
            inviteId = Integer.parseInt(request.getParameter("inviteId"));
        } catch (NumberFormatException e) {
            session.setAttribute("toastError", "Mã lời mời không hợp lệ!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        ProjectInvite invite = ProjectInviteDB.selectById(inviteId);
        if (invite == null || !"PENDING".equalsIgnoreCase(invite.getStatus())) {
            session.setAttribute("toastError", "Lời mời không tồn tại hoặc đã được xử lý!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // KIỂM TRA THẨM QUYỀN DUYỆT (Chỉ người nhận mới được bấm duyệt)
        if (currentUser.getId() != invite.getReceiverId()) {
            session.setAttribute("toastError", "Bạn không có quyền duyệt yêu cầu này!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // --- RÀO BẢO MẬT 4: KIỂM TRA HẾT HẠN 7 NGÀY ---
        if (invite.isExpired()) {
            ProjectInviteDB.updateStatus(inviteId, "EXPIRED");
            session.setAttribute("toastError", "Lời mời này đã quá thời hạn 7 ngày và đã hết hiệu lực!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // --- RÀO BẢO MẬT 5: KIỂM TRA QUOTA DỰ ÁN ---
        if (ProjectMemberDB.countMembers(invite.getProjectId()) >= 10) {
            session.setAttribute("toastError", "Dự án đã đủ tối đa 10 thành viên, không thể nhận thêm!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // HỢP LỆ -> CẬP NHẬT TRẠNG THÁI ACCEPTED
        ProjectInviteDB.updateStatus(inviteId, "ACCEPTED");

        // XÁC ĐỊNH NGƯỜI ĐƯỢC KẾT NẠP VÀO DỰ ÁN
        int newMemberId = ("INVITATION".equalsIgnoreCase(invite.getType())) ? invite.getReceiverId() : invite.getSenderId();
        User newMemberUser = UserDB.selectById(newMemberId);

        if (newMemberUser != null) {
            LocalDateTime now = LocalDateTime.now();
            ProjectMember member = new ProjectMember(
                invite.getProjectId(),
                newMemberUser.getId(),
                newMemberUser.getFullName(),
                newMemberUser.getEmail(),
                newMemberUser.getRole(),
                "MEMBER",
                now.format(DATE_FORMATTER)
            );
            ProjectMemberDB.insert(member);

            // BẮN THÔNG BÁO KẾT NỐI TỚI NGƯỜI KIA
            if ("INVITATION".equalsIgnoreCase(invite.getType())) {
                NotificationDB.send(
                    invite.getSenderId(),
                    "Thành Viên Mới Gia Nhập",
                    newMemberUser.getFullName() + " đã đồng ý tham gia vào dự án [" + invite.getProjectName() + "]!",
                    "/task?action=list&projectId=" + invite.getProjectId(),
                    "INVITE"
                );
            } else {
                NotificationDB.send(
                    invite.getSenderId(),
                    "Yêu Cầu Được Phê Duyệt",
                    "Yêu cầu xin gia nhập dự án [" + invite.getProjectName() + "] của bạn đã được Trưởng Dự Án chấp thuận!",
                    "/task?action=list&projectId=" + invite.getProjectId(),
                    "INVITE"
                );
            }
        }

        session.setAttribute("toastSuccess", "Chúc mừng! Đã kết nạp thành viên vào dự án [" + invite.getProjectName() + "] thành công!");
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + invite.getProjectId());
    }

    // =========================================================================
    // LUỒNG 4: TỪ CHỐI LỜI MỜI / YÊU CẦU (REJECT)
    // =========================================================================
    private void handleReject(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        HttpSession session = request.getSession();
        int inviteId = 0;
        try {
            inviteId = Integer.parseInt(request.getParameter("inviteId"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        ProjectInvite invite = ProjectInviteDB.selectById(inviteId);
        if (invite != null && "PENDING".equalsIgnoreCase(invite.getStatus())) {
            if (currentUser.getId() == invite.getReceiverId()) {
                ProjectInviteDB.updateStatus(inviteId, "REJECTED");
                session.setAttribute("toastSuccess", "Đã từ chối lời mời vào dự án [" + invite.getProjectName() + "].");
            }
        }
        response.sendRedirect(request.getContextPath() + "/project?action=list");
    }

    // =========================================================================
    // LUỒNG 5: PM THU HỒI / HỦY LỜI MỜI ĐÃ GỬI (REVOKE)
    // =========================================================================
    private void handleRevoke(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        HttpSession session = request.getSession();
        int inviteId = 0;
        try {
            inviteId = Integer.parseInt(request.getParameter("inviteId"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        ProjectInvite invite = ProjectInviteDB.selectById(inviteId);
        if (invite != null && "PENDING".equalsIgnoreCase(invite.getStatus())) {
            Project project = ProjectDB.selectById(invite.getProjectId());
            if (project != null && project.getOwnerId() == currentUser.getId()) {
                ProjectInviteDB.updateStatus(inviteId, "REVOKED");
                session.setAttribute("toastSuccess", "Đã thu hồi lời mời tham gia dự án thành công!");
                response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + invite.getProjectId());
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/project?action=list");
    }
}
