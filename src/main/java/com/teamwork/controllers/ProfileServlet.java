package com.teamwork.controllers;

import com.teamwork.business.Project;
import com.teamwork.business.SubTask;
import com.teamwork.business.Task;
import com.teamwork.business.User;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.ProjectMemberDB;
import com.teamwork.data.SubTaskDB;
import com.teamwork.data.TaskDB;
import com.teamwork.data.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ProfileServlet — Controller quản lý Hồ Sơ Cá Nhân Công Khai (/profile).
 *
 * <p><b>Các luồng được xử lý:</b></p>
 * <ul>
 *   <li>GET  /profile?userId=X   → Hiển thị hồ sơ công khai của người dùng X</li>
 *   <li>GET  /profile             → Hiển thị hồ sơ của chính mình (userId = currentUser)</li>
 *   <li>POST /profile (action=update) → {@link #handleUpdateProfile} — Cập nhật thông tin hồ sơ</li>
 * </ul>
 *
 * <p><b>Tính năng nổi bật:</b></p>
 * <ul>
 *   <li>Tính toán chỉ số năng suất Real-time từ DB (Lead Task, Sub-task, Completion Rate)</li>
 *   <li>Bảo mật chính chủ: chỉ chủ hồ sơ mới được chỉnh sửa (chống IDOR)</li>
 *   <li>Đồng bộ tên sang toàn bộ hệ thống (ProjectMember / Task / SubTask) khi đổi tên</li>
 *   <li>Sanitize URL mạng xã hội: chặn javascript: và data: scheme, tự thêm https://</li>
 * </ul>
 *
 * <p><b>TODO — Điểm mở rộng phổ biến (Extension Points):</b></p>
 * <ul>
 *   <li>Thêm chức năng upload ảnh đại diện (Avatar Upload) với Multipart Servlet</li>
 *   <li>Thêm trường mới vào form profile (ví dụ: số điện thoại, chuyên ngành)</li>
 *   <li>Thêm bảng xếp hạng năng suất (Leaderboard) so sánh giữa các thành viên</li>
 *   <li>Thêm validation email khi đổi thông tin profile</li>
 * </ul>
 */
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    /**
     * Tiện ích parse số nguyên an toàn
     */
    private int safeParseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Tiện ích chuẩn hóa và lọc URL mạng xã hội an toàn (chống XSS & tự động thêm https://)
     */
    private String sanitizeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        String trimmed = url.trim();
        // Chặn các scheme nguy hiểm
        if (trimmed.toLowerCase().startsWith("javascript:") || trimmed.toLowerCase().startsWith("data:")) {
            return "";
        }
        // Tự động gắn protocol https:// nếu chưa có http/https
        if (!trimmed.toLowerCase().startsWith("http://") && !trimmed.toLowerCase().startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        return trimmed;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        // 1. Đọc userId từ tham số URL (nếu không truyền -> mặc định lấy ID của chính mình)
        int targetUserId = safeParseInt(request.getParameter("userId"), currentUser.getId());

        // 2. Tìm thông tin User theo ID
        User profileUser = UserDB.selectById(targetUserId);
        if (profileUser == null) {
            session.setAttribute("toastError", "Không tìm thấy hồ sơ người dùng này!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 3. Lấy danh sách các Dự án mà người này đang tham gia
        List<Project> userProjects = ProjectMemberDB.selectProjectsByUserId(profileUser.getId());

        // 4. THUẬT TOÁN TÍNH CHỈ SỐ NĂNG SUẤT REAL-TIME (DỮ LIỆU KHÁCH QUAN)
        int leadTaskCount = 0;
        int totalSubTasks = 0;
        int completedSubTasks = 0;

        List<Task> allTasks = TaskDB.selectAll();
        for (Task t : allTasks) {
            // Đếm số Task lớn làm Lead
            if (t.getAssigneeId() == profileUser.getId()) {
                leadTaskCount++;
            }
            // Đếm số Việc con được giao & đã hoàn thành
            List<SubTask> subs = SubTaskDB.selectByTaskId(t.getId());
            for (SubTask st : subs) {
                if (st.getAssigneeId() == profileUser.getId()) {
                    totalSubTasks++;
                    if (st.isCompleted()) {
                        completedSubTasks++;
                    }
                }
            }
        }

        int completionRate = (totalSubTasks > 0) ? (int) Math.round((completedSubTasks * 100.0) / totalSubTasks) : 100;

        // 5. Nếu người xem là PM, gom danh sách Dự án của PM mà người này CHƯA THAM GIA (cho nút Mời nhanh)
        List<Project> availableProjectsToInvite = new ArrayList<>();
        if (currentUser.getId() != profileUser.getId()) {
            List<Project> allProjects = ProjectDB.selectAll();
            for (Project p : allProjects) {
                if (p.getOwnerId() == currentUser.getId()) {
                    if (!ProjectMemberDB.isMember(p.getId(), profileUser.getId())) {
                        availableProjectsToInvite.add(p);
                    }
                }
            }
        }

        // 6. Xử lý Flash Message (Toast)
        String toastSuccess = (String) session.getAttribute("toastSuccess");
        if (toastSuccess != null) {
            request.setAttribute("toastSuccess", toastSuccess);
            session.removeAttribute("toastSuccess");
        }
        String toastError = (String) session.getAttribute("toastError");
        if (toastError != null) {
            request.setAttribute("toastError", toastError);
            session.removeAttribute("toastError");
        }

        // 7. Đóng gói dữ liệu gửi sang profile.jsp
        request.setAttribute("profileUser", profileUser);
        request.setAttribute("userProjects", userProjects);
        request.setAttribute("leadTaskCount", leadTaskCount);
        request.setAttribute("totalSubTasks", totalSubTasks);
        request.setAttribute("completedSubTasks", completedSubTasks);
        request.setAttribute("completionRate", completionRate);
        request.setAttribute("availableProjectsToInvite", availableProjectsToInvite);
        request.setAttribute("isOwner", (currentUser.getId() == profileUser.getId()));

        request.getRequestDispatcher("/profile.jsp").forward(request, response);
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
        if ("update".equalsIgnoreCase(action)) {
            handleUpdateProfile(request, response, currentUser);
        } else {
            response.sendRedirect(request.getContextPath() + "/profile");
        }
    }

    /**
     * Xử lý cập nhật thông tin hồ sơ (BẢO VỆ PHÂN QUYỀN CHÍNH CHỦ & ĐỒNG BỘ TOÀN HỆ THỐNG)
     */
    private void handleUpdateProfile(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        HttpSession session = request.getSession();
        int targetUserId = safeParseInt(request.getParameter("userId"), 0);
        if (targetUserId <= 0) {
            response.sendRedirect(request.getContextPath() + "/profile");
            return;
        }

        // --- BẢO MẬT CHÍNH CHỦ: Chỉ cho phép người dùng tự sửa hồ sơ của chính mình ---
        if (currentUser.getId() != targetUserId) {
            session.setAttribute("toastError", "Cảnh báo bảo mật: Bạn không có quyền chỉnh sửa hồ sơ của người khác!");
            response.sendRedirect(request.getContextPath() + "/profile?userId=" + targetUserId);
            return;
        }

        String fullName = request.getParameter("fullName");
        String role = request.getParameter("role");
        String bio = request.getParameter("bio");
        String skills = request.getParameter("skills");
        String githubUrl = sanitizeUrl(request.getParameter("githubUrl"));
        String linkedinUrl = sanitizeUrl(request.getParameter("linkedinUrl"));

        if (fullName == null || fullName.trim().isEmpty()) {
            session.setAttribute("toastError", "Họ và tên không được để trống!");
            response.sendRedirect(request.getContextPath() + "/profile?userId=" + targetUserId);
            return;
        }

        // Cắt ngắn Bio nếu vượt quá 250 ký tự
        if (bio != null && bio.trim().length() > 250) {
            bio = bio.trim().substring(0, 250);
        }

        // Cập nhật thông tin vào đối tượng currentUser
        currentUser.setFullName(fullName.trim());
        currentUser.setRole((role != null && !role.trim().isEmpty()) ? role.trim() : "Developer");
        currentUser.setBio((bio != null) ? bio.trim() : "");
        currentUser.setSkills((skills != null) ? skills.trim() : "");
        currentUser.setGithubUrl(githubUrl);
        currentUser.setLinkedinUrl(linkedinUrl);

        // 1. Lưu vào kho dữ liệu UserDB
        UserDB.update(currentUser);

        // 2. ĐỒNG BỘ TÊN MỚI SANG TOÀN BỘ CÁC BẢNG TRONG HỆ THỐNG
        ProjectMemberDB.syncUserName(currentUser.getId(), currentUser.getFullName());
        TaskDB.syncAssigneeName(currentUser.getId(), currentUser.getFullName());
        SubTaskDB.syncAssigneeName(currentUser.getId(), currentUser.getFullName());

        // 3. Cập nhật lại session
        session.setAttribute("currentUser", currentUser);
        session.setAttribute("toastSuccess", "Đã cập nhật thông tin hồ sơ cá nhân thành công!");

        response.sendRedirect(request.getContextPath() + "/profile?userId=" + targetUserId);
    }
}
