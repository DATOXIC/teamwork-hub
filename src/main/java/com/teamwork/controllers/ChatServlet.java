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
import com.teamwork.util.MentionNotifier;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatServlet — Controller phụ trách Phân hệ Thảo luận &amp; Chat nhóm (Team Chat &amp; Comment Threads).
 *
 * <p><b>Các luồng được xử lý:</b></p>
 * <ul>
 *   <li>GET  /chat?action=view&projectId=X      → Mở kênh chat chung của dự án X</li>
 *   <li>POST /chat (action=sendProjectMessage)  → Gửi tin nhắn chat chung dự án</li>
 *   <li>POST /chat (action=sendTaskComment)     → Gửi bình luận của một Task cụ thể</li>
 *   <li>GET/POST /chat (action=delete)          → Xóa tin nhắn (bảo mật đa tầng, chống IDOR)</li>
 * </ul>
 *
 * <p><b>Kiến trúc MVC:</b></p>
 * <pre>
 *   Browser → ChatServlet (Controller) → MessageDB (Model) → chat.jsp (View)
 * </pre>
 *
 * <p><b>TODO — Điểm mở rộng phổ biến (Extension Points):</b></p>
 * <ul>
 *   <li>Thêm chức năng chỉnh sửa tin nhắn (Edit Message)</li>
 *   <li>Thêm chức năng phản hồi / trả lời tin nhắn (Reply Thread)</li>
 *   <li>Thêm emoji reaction cho từng tin nhắn</li>
 *   <li>Tích hợp WebSocket cho real-time chat mà không cần refresh</li>
 *   <li>Thêm giới hạn độ dài tin nhắn và validate nội dung</li>
 * </ul>
 */
@WebServlet("/chat")
public class ChatServlet extends BaseServlet {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private boolean isAjaxRequest(HttpServletRequest request) {
        String xrw = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(xrw)
                || (accept != null && accept.contains("application/json"));
    }

    // =========================================================================
    // HÀM doGet: XỬ LÝ TOÀN BỘ CÁC YÊU CẦU ĐỌC, XEM VÀ XÓA TIN NHẮN
    // =========================================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Kiểm tra xác thực người dùng
        HttpSession session = request.getSession(false);
        User currentUser = currentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        // 2. Lấy và kiểm tra an toàn tham số projectId từ URL
        int projectId = intParam(request, "projectId", 0);
        if (!requireMember(request, response, currentUser, projectId, "Bạn không có quyền truy cập vào dự án này!")) {
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

            // "delete" chỉ nhận qua POST (doPost): link GET có thể bị kích hoạt từ trang khác (CSRF)

            case "poll":
                handlePoll(response, projectId);
                break;

            default:
                handleShowChat(request, response, projectId);
                break;
        }
    }

    /**
     * Chữ ký (dấu vân tay) của danh sách tin nhắn đang hiển thị: đổi khi có tin mới, tin bị sửa hoặc bị xóa.
     * Trình duyệt so sánh chữ ký này để biết có cần tải lại khung chat hay không.
     */
    private static String chatSignature(List<Message> messages) {
        long h = 17;
        for (Message m : messages) {
            h = h * 31 + m.getId();
            h = h * 31 + (m.getContent() == null ? 0 : m.getContent().hashCode());
        }
        return messages.size() + "-" + Long.toHexString(h);
    }

    /**
     * Nghiệp vụ: Trình duyệt hỏi định kỳ "có thay đổi gì không?" (GET /chat?action=poll&projectId=X).
     * Chỉ trả về chữ ký rất nhỏ dạng JSON, không dựng lại cả trang.
     */
    private void handlePoll(HttpServletResponse response, int projectId) throws IOException {
        String sig = chatSignature(MessageDB.selectRecentByProjectId(projectId, 50));
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"sig\":\"" + sig + "\"}");
    }

    // =========================================================================
    // HÀM doPost: TIẾP NHẬN FORM GỬI TIN NHẮN, BÌNH LUẬN & XÓA (FLOW 2, 3, 4)
    // =========================================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Lấy thông tin User đang đăng nhập từ Session để bảo mật danh tính người gửi
        HttpSession session = request.getSession(false);
        User currentUser = currentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        int projectId = intParam(request, "projectId", 0);
        if (!requireMember(request, response, currentUser, projectId, "Bạn không có quyền thao tác trong dự án này!")) {
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

            case "editProjectMessage":
                handleEditProjectMessage(request, response, currentUser, projectId);
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
        // Một câu IN (...) cho cả nhóm thay vì selectById cho từng thành viên
        List<Integer> memberIds = new ArrayList<>();
        for (ProjectMember pm : memberList) {
            memberIds.add(pm.getUserId());
        }
        List<User> userList = UserDB.selectByIds(memberIds);

        // 6. Đóng gói toàn bộ vào Request Scope
        // ▶ JSP: tasks.jsp, command_palette.jsp, project_report.jsp đọc bằng ${project}
        request.setAttribute("project", project);
        // ▶ JSP: chat.jsp đọc bằng ${messageList}
        request.setAttribute("messageList", messageList);
        // ▶ JSP: chat.jsp đọc bằng ${chatSig} (chat.js so sánh với action=poll để tự làm mới khung chat)
        request.setAttribute("chatSig", chatSignature(messageList));
        // ▶ JSP: chat.jsp, tasks.jsp đọc bằng ${docList}
        request.setAttribute("docList", docList);
        // ▶ JSP: chat.jsp đọc bằng ${taskList}
        request.setAttribute("taskList", taskList);
        // ▶ JSP: chat.jsp, tasks.jsp đọc bằng ${userList}
        request.setAttribute("userList", userList);
        // ▶ JSP: navbar.jsp đọc bằng ${activeNav}
        request.setAttribute("activeNav", "chat"); // Bật sáng menu Thảo luận

        // 7. Chuyển giao sang giao diện chat.jsp để hiển thị
        // ▶ forward → chat.jsp (các setAttribute ở trên chính là dữ liệu JSP hiển thị)
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
            if (isAjaxRequest(request)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("Cache-Control", "no-store");
                response.getWriter().write("{\"success\":false,\"error\":\"Nội dung không được để trống\"}");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/chat?action=view&projectId=" + projectId);
            return;
        }

        // Lấy thời gian realtime hiện tại theo múi giờ Việt Nam (UTC+7)
        String now = LocalDateTime.now(VN_ZONE).format(DATE_FORMATTER);

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

        // Lưu vào kho dữ liệu
        int newId = MessageDB.insert(newMessage);

        // @nhắc tên → thông báo 🔔 cho người được nhắc (chỉ thành viên dự án)
        if (newId > 0) MentionNotifier.notifyMentions(projectId, currentUser, content.trim(), 0, null);

        // Hỗ trợ AJAX: Trả về JSON để client cập nhật giao diện mà KHÔNG cần reload/F5 trang
        if (isAjaxRequest(request)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write("{\"success\":true,\"messageId\":" + newId + "}");
            return;
        }

        // Áp dụng chuẩn PRG: Redirect về lại kênh chat (hoặc ClickUp Shell nếu gửi từ tab Chat của TaskServlet)
        String source = request.getParameter("source");
        if ("taskShell".equalsIgnoreCase(source)) {
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId + "&view=chat");
        } else {
            response.sendRedirect(request.getContextPath() + "/chat?action=view&projectId=" + projectId);
        }
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
        int taskId = intParam(request, "taskId", 0);
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

        // Lấy thời gian realtime hiện tại theo múi giờ Việt Nam (UTC+7)
        String now = LocalDateTime.now(VN_ZONE).format(DATE_FORMATTER);

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

        // Lưu vào kho dữ liệu
        int commentId = MessageDB.insert(commentMessage);

        // @nhắc tên trong bình luận → thông báo 🔔 cho người được nhắc
        if (commentId > 0) MentionNotifier.notifyMentions(projectId, currentUser, content.trim(), taskId, task.getTitle());

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
        int messageId = intParam(request, "messageId", 0);

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
            //
            // KHÔNG được dùng User.role để phân quyền: cột đó là "Chuyên môn / Chức danh"
            // do chính người dùng tự nhập ở trang Hồ sơ (profile.jsp), nên bất kỳ ai cũng có
            // thể gõ "ADMIN" vào đó để tự cấp quyền cho mình. Chỉ so sánh ID do server cung
            // cấp (authorId, ownerId) mới là căn cứ phân quyền an toàn.
            boolean isAuthor = (currentUser.getId() == msg.getAuthorId());
            boolean isProjectOwner = ProjectAccess.isOwner(currentUser, project);

            if (isAuthor || isProjectOwner) {
                MessageDB.delete(messageId);
                if (session != null) {
                    // ▶ JSP: docs.jsp, tasks.jsp đọc bằng ${toastSuccess}
                    session.setAttribute("toastSuccess", "Đã xóa tin nhắn thành công.");
                }
                if (isAjaxRequest(request)) {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("Cache-Control", "no-store");
                    response.getWriter().write("{\"success\":true}");
                    return;
                }
            } else {
                if (session != null) {
                    session.setAttribute("toastError", "Bạn không có quyền xóa tin nhắn của người khác!");
                }
                if (isAjaxRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("Cache-Control", "no-store");
                    response.getWriter().write("{\"success\":false,\"error\":\"Bạn không có quyền xóa tin nhắn của người khác!\"}");
                    return;
                }
            }
        }

        // Redirect về lại trang chat (hoặc ClickUp Shell nếu thao tác từ tab Chat của TaskServlet)
        String source = request.getParameter("source");
        if ("taskShell".equalsIgnoreCase(source)) {
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId + "&view=chat");
        } else {
            response.sendRedirect(request.getContextPath() + "/chat?action=view&projectId=" + projectId);
        }
    }

    // =========================================================================
    // CHI TIẾT NGHIỆP VỤ 5: SỬA TIN NHẮN (BẢO VỆ ĐA TẦNG & CHỐNG IDOR)
    // =========================================================================
    private void handleEditProjectMessage(HttpServletRequest request, HttpServletResponse response, User currentUser, int projectId)
            throws IOException {

        HttpSession session = request.getSession();
        int messageId = intParam(request, "messageId", 0);
        String content = request.getParameter("content");

        if (messageId <= 0 || content == null || content.trim().isEmpty()) {
            if (isAjaxRequest(request)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("Cache-Control", "no-store");
                response.getWriter().write("{\"success\":false,\"error\":\"Dữ liệu không hợp lệ\"}");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/chat?action=view&projectId=" + projectId);
            return;
        }

        Message msg = MessageDB.selectById(messageId);
        Project project = ProjectDB.selectById(projectId);

        if (msg != null && project != null) {
            // RÀO BẢO MẬT 1: Chống IDOR xuyên dự án
            if (msg.getProjectId() != projectId) {
                if (session != null) {
                    session.setAttribute("toastError", "Cảnh báo bảo mật: Tin nhắn không thuộc dự án này!");
                }
                if (isAjaxRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("Cache-Control", "no-store");
                    response.getWriter().write("{\"success\":false,\"error\":\"Tin nhắn không thuộc dự án này\"}");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/chat?action=view&projectId=" + projectId);
                return;
            }

            // RÀO BẢO MẬT 2: Phân quyền (Chính tác giả tin nhắn HOẶC PM của dự án)
            boolean isAuthor = (currentUser.getId() == msg.getAuthorId());
            boolean isProjectOwner = ProjectAccess.isOwner(currentUser, project);

            if (isAuthor || isProjectOwner) {
                boolean success = MessageDB.update(messageId, content.trim());
                if (session != null) {
                    if (success) {
                        session.setAttribute("toastSuccess", "Đã cập nhật nội dung tin nhắn thành công.");
                    } else {
                        session.setAttribute("toastError", "Không thể cập nhật tin nhắn. Vui lòng thử lại!");
                    }
                }
                if (isAjaxRequest(request)) {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("Cache-Control", "no-store");
                    response.getWriter().write("{\"success\":" + success + "}");
                    return;
                }
            } else {
                if (session != null) {
                    session.setAttribute("toastError", "Bạn không có quyền chỉnh sửa tin nhắn của người khác!");
                }
                if (isAjaxRequest(request)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("Cache-Control", "no-store");
                    response.getWriter().write("{\"success\":false,\"error\":\"Không có quyền chỉnh sửa tin nhắn\"}");
                    return;
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/chat?action=view&projectId=" + projectId);
    }
}
