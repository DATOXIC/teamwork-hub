package com.teamwork.controllers;

import com.teamwork.business.Doc;
import com.teamwork.business.Message;
import com.teamwork.business.Project;
import com.teamwork.business.ProjectMember;
import com.teamwork.business.Task;
import com.teamwork.business.User;
import com.teamwork.data.DocDB;
import com.teamwork.data.MessageDB;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.ProjectMemberDB;
import com.teamwork.data.TaskDB;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Controller phụ trách Phân hệ Thảo luận & Chat nhóm (Team Chat & Comment Threads):
 * - FLOW 1: Mở xem kênh chat chung của dự án (GET /chat?action=view)
 * - FLOW 2: Gửi tin nhắn chat chung dự án (POST /chat với action=sendProjectMessage)
 * - FLOW 3: Gửi bình luận của một Task cụ thể (POST /chat với action=sendTaskComment)
 * - FLOW 4: Xóa tin nhắn bảo mật đa tầng, chống IDOR (GET/POST /chat với action=delete)
 */
@WebServlet("/chat")
public class ChatServlet extends HttpServlet {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Tiện ích parse số nguyên an toàn, chống NumberFormatException
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

    // =========================================================================
    // HÀM doGet: XỬ LÝ TOÀN BỘ CÁC YÊU CẦU ĐỌC, XEM VÀ XÓA TIN NHẮN
    // =========================================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Kiểm tra xác thực người dùng
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        // 2. Lấy và kiểm tra an toàn tham số projectId từ URL
        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        if (projectId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 3. Kiểm tra quyền thành viên trong dự án
        if (!ProjectMemberDB.isMember(projectId, currentUser.getId())) {
            if (session != null) {
                session.setAttribute("toastError", "Bạn không có quyền truy cập vào dự án này!");
            }
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 4. Đọc tham số action từ URL (Mặc định là "view")
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "view";
        }

        // 5. Phân nhánh hành động GET
        switch (action) {
            case "view":
                handleShowChat(request, response, projectId);
                break;

            case "delete":
                handleDeleteMessage(request, response, currentUser, projectId);
                break;

            default:
                handleShowChat(request, response, projectId);
                break;
        }
    }

    // =========================================================================
    // HÀM doPost: TIẾP NHẬN FORM GỬI TIN NHẮN, BÌNH LUẬN & XÓA (FLOW 2, 3, 4)
    // =========================================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Lấy thông tin User đang đăng nhập từ Session để bảo mật danh tính người gửi
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        if (projectId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // Kiểm tra quyền thành viên dự án
        if (!ProjectMemberDB.isMember(projectId, currentUser.getId())) {
            if (session != null) {
                session.setAttribute("toastError", "Bạn không có quyền thao tác trong dự án này!");
            }
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 2. Đọc action từ Form gửi lên
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "sendProjectMessage";
        }

        // 3. Phân nhánh hành động POST
        switch (action) {
            case "sendProjectMessage":
                handleSendProjectMessage(request, response, currentUser, projectId);
                break;

            case "sendTaskComment":
                handleSendTaskComment(request, response, currentUser, projectId);
                break;

            case "delete":
                handleDeleteMessage(request, response, currentUser, projectId);
                break;

            default:
                response.sendRedirect(request.getContextPath() + "/chat?action=view&projectId=" + projectId);
                break;
        }
    }

    // =========================================================================
    // CHI TIẾT NGHIỆP VỤ 1: FLOW 1 - MỞ TRANG CHAT DỰ ÁN (GET)
    // =========================================================================
    /**
     * Nghiệp vụ 1: Thu thập đầy đủ dữ liệu (Project, Messages, Tasks, Docs, Users) 
     * và chuyển giao cho giao diện chat.jsp hiển thị dòng thời gian tin nhắn.
     */
    private void handleShowChat(HttpServletRequest request, HttpServletResponse response, int projectId)
            throws ServletException, IOException {

        // 1. Lấy thông tin dự án hiện tại
        Project project = ProjectDB.selectById(projectId);
        if (project == null) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 2. Lấy danh sách 50 tin nhắn chat chung gần đây nhất của dự án này (chỉ lấy các tin có taskId == 0)
        List<Message> messageList = MessageDB.selectRecentByProjectId(projectId, 50);

        // 3. Lấy danh sách toàn bộ Tài liệu Wiki của dự án (để hỗ trợ gợi ý khi gõ #doc-...)
        List<Doc> docList = DocDB.selectByProjectId(projectId);

        // 4. Lấy danh sách toàn bộ Công việc Kanban của dự án (để hỗ trợ gợi ý khi gõ #task-...)
        List<Task> taskList = TaskDB.selectByProjectId(projectId);

        // 5. Lấy danh sách thành viên thực tế của dự án này (phục vụ danh sách thành viên và gợi ý @mention)
        List<ProjectMember> memberList = ProjectMemberDB.selectByProjectId(projectId);
        List<User> userList = new ArrayList<>();
        for (ProjectMember pm : memberList) {
            User u = UserDB.selectById(pm.getUserId());
            if (u != null) {
                userList.add(u);
            }
        }

        // 6. Đóng gói toàn bộ vào Request Scope
        request.setAttribute("project", project);
        request.setAttribute("messageList", messageList);
        request.setAttribute("docList", docList);
        request.setAttribute("taskList", taskList);
        request.setAttribute("userList", userList);
        request.setAttribute("activeNav", "chat"); // Bật sáng menu Thảo luận

        // 7. Chuyển giao sang giao diện chat.jsp để hiển thị
        request.getRequestDispatcher("/chat.jsp").forward(request, response);
    }

    // =========================================================================
    // CHI TIẾT NGHIỆP VỤ 2: FLOW 2 - GỬI TIN NHẮN CHAT DỰ ÁN (POST)
    // =========================================================================
    /**
     * Nghiệp vụ 2: Tiếp nhận nội dung tin nhắn chat chung từ form và lưu vào RAM với taskId = 0.
     */
    private void handleSendProjectMessage(HttpServletRequest request, HttpServletResponse response, User currentUser, int projectId)
            throws IOException {

        String content = request.getParameter("content");

        // Kiểm tra chống gửi tin nhắn rỗng / khoảng trắng
        if (content == null || content.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/chat?action=view&projectId=" + projectId);
            return;
        }

        // Lấy thời gian realtime hiện tại (Định dạng: dd/MM/yyyy HH:mm)
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        // Tạo đối tượng Message mới với taskId = 0 (Kênh chat chung dự án)
        Message newMessage = new Message(
            0,                          // ID tự tăng
            projectId,                  // Thuộc dự án này
            0,                          // taskId = 0 (Chat chung)
            currentUser.getId(),        // ID người gửi từ Session
            currentUser.getFullName(),   // Tên người gửi từ Session
            content.trim(),             // Nội dung tin nhắn
            now                         // Thời gian gửi
        );

        // Lưu vào kho dữ liệu RAM
        MessageDB.insert(newMessage);

        // Áp dụng chuẩn PRG: Redirect về lại kênh chat để tải tin nhắn mới nhất
        response.sendRedirect(request.getContextPath() + "/chat?action=view&projectId=" + projectId);
    }

    // =========================================================================
    // CHI TIẾT NGHIỆP VỤ 3: FLOW 3 - GỬI BÌNH LUẬN TRONG TASK (POST)
    // =========================================================================
    /**
     * Nghiệp vụ 3: Tiếp nhận bình luận thuộc về một Task cụ thể từ Modal và lưu với taskId > 0.
     * Kiểm tra chặt chẽ tính hợp lệ của Task và chống IDOR xuyên dự án.
     */
    private void handleSendTaskComment(HttpServletRequest request, HttpServletResponse response, User currentUser, int projectId)
            throws IOException {

        HttpSession session = request.getSession();
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String content = request.getParameter("content");

        if (taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // BẢO VỆ CHỐNG IDOR: Đảm bảo Task tồn tại và thuộc đúng Project hiện tại
        Task task = TaskDB.selectById(taskId);
        if (task == null || task.getProjectId() != projectId) {
            if (session != null) {
                session.setAttribute("toastError", "Công việc không tồn tại hoặc không thuộc dự án này!");
            }
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Kiểm tra nội dung bình luận
        if (content == null || content.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Lấy thời gian realtime hiện tại
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        // Tạo đối tượng Message mới với taskId > 0 (Bình luận của Task)
        Message commentMessage = new Message(
            0,
            projectId,
            taskId,                     // taskId cụ thể của công việc
            currentUser.getId(),
            currentUser.getFullName(),
            content.trim(),
            now
        );

        // Lưu vào kho dữ liệu RAM
        MessageDB.insert(commentMessage);

        // Áp dụng PRG: Redirect về lại bảng Kanban của dự án
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    // =========================================================================
    // CHI TIẾT NGHIỆP VỤ 4: XÓA TIN NHẮN (BẢO VỆ ĐA TẦNG & CHỐNG IDOR)
    // =========================================================================
    /**
     * Nghiệp vụ 4: Xóa một tin nhắn theo messageId.
     * Bảo mật 3 lớp:
     * 1. Tin nhắn phải thuộc đúng projectId hiện tại (Chống IDOR).
     * 2. Chỉ chính chủ người gửi HOẶC Trưởng Dự Án (PM) mới có quyền xóa.
     */
    private void handleDeleteMessage(HttpServletRequest request, HttpServletResponse response, User currentUser, int projectId)
            throws IOException {

        HttpSession session = request.getSession();
        int messageId = safeParseInt(request.getParameter("messageId"), 0);

        if (messageId <= 0) {
            response.sendRedirect(request.getContextPath() + "/chat?action=view&projectId=" + projectId);
            return;
        }

        Message msg = MessageDB.selectById(messageId);
        Project project = ProjectDB.selectById(projectId);

        if (msg != null && project != null) {
            // RÀO BẢO MẬT 1: Chống IDOR xuyên dự án (Tin nhắn phải thuộc đúng dự án này)
            if (msg.getProjectId() != projectId) {
                if (session != null) {
                    session.setAttribute("toastError", "Cảnh báo bảo mật: Tin nhắn không thuộc dự án này!");
                }
                response.sendRedirect(request.getContextPath() + "/chat?action=view&projectId=" + projectId);
                return;
            }

            // RÀO BẢO MẬT 2: Phân quyền (Chính tác giả tin nhắn HOẶC PM của dự án)
            if (currentUser.getId() == msg.getAuthorId() || currentUser.getId() == project.getOwnerId()) {
                MessageDB.delete(messageId);
                if (session != null) {
                    session.setAttribute("toastSuccess", "Đã xóa tin nhắn thành công.");
                }
            } else {
                if (session != null) {
                    session.setAttribute("toastError", "Bạn không có quyền xóa tin nhắn của người khác!");
                }
            }
        }

        // Redirect về lại trang chat
        response.sendRedirect(request.getContextPath() + "/chat?action=view&projectId=" + projectId);
    }
}
