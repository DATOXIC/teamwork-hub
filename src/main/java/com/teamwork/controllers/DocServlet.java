package com.teamwork.controllers;

import com.teamwork.business.Doc;
import com.teamwork.business.Project;
import com.teamwork.business.ProjectMember;
import com.teamwork.business.Task;
import com.teamwork.business.User;
import com.teamwork.data.DocDB;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.ProjectMemberDB;
import com.teamwork.data.TaskDB;
import com.teamwork.data.TaskDocDB;
import com.teamwork.data.ActivityLogDB;
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
 * Controller phụ trách Quản lý Tài liệu & Ghi chú Wiki nhóm (/doc):
 * - Xem danh mục bài viết, đọc chi tiết bài và danh sách các Task đang áp dụng bài viết này (GET /doc?action=list hoặc action=view)
 * - Tạo bài viết mới (POST /doc?action=create)
 * - Chỉnh sửa cập nhật bài viết an toàn, chống IDOR (POST /doc?action=update)
 * - Xóa bài viết và tự động dọn dẹp các liên kết TaskDoc (GET/POST /doc?action=delete)
 */
@WebServlet("/doc")
public class DocServlet extends HttpServlet {

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

    // ========================================================
    // HÀM doGet: XỬ LÝ CÁC YÊU CẦU ĐỌC, XEM VÀ XÓA TÀI LIỆU
    // ========================================================
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

        // 4. Đọc action từ URL (Mặc định là "list" nếu không truyền)
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        // 5. Phân nhánh hành động GET
        switch (action) {
            case "list":
            case "view":
                handleShowDocs(request, response, projectId);
                break;

            case "delete":
                handleDeleteDoc(request, response, currentUser, projectId);
                break;

            default:
                handleShowDocs(request, response, projectId);
                break;
        }
    }

    // ========================================================
    // HÀM doPost: XỬ LÝ CÁC YÊU CẦU GHI, SỬA VÀ XÓA TÀI LIỆU
    // ========================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Lấy thông tin User đang đăng nhập từ Session
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
            action = "create";
        }

        // 3. Phân nhánh hành động POST
        switch (action) {
            case "create":
                handleCreateDoc(request, response, currentUser, projectId);
                break;

            case "update":
                handleUpdateDoc(request, response, currentUser, projectId);
                break;

            case "delete":
                handleDeleteDoc(request, response, currentUser, projectId);
                break;

            default:
                response.sendRedirect(request.getContextPath() + "/doc?action=list&projectId=" + projectId);
                break;
        }
    }

    // ========================================================
    // CÁC HÀM NGHIỆP VỤ CON (PRIVATE METHODS)
    // ========================================================

    /**
     * Nghiệp vụ 1: Lấy danh mục bài viết (bên trái), bài viết đang đọc (bên phải) 
     * VÀ danh sách các Công việc đang áp dụng tài liệu này (relatedTasks) đưa sang docs.jsp
     * Bảo vệ chống rò rỉ tài liệu khác dự án (Fix DOC-03).
     */
    private void handleShowDocs(HttpServletRequest request, HttpServletResponse response, int projectId)
            throws ServletException, IOException {

        // 1. Lấy thông tin dự án hiện tại
        Project project = ProjectDB.selectById(projectId);
        if (project == null) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 2. Lấy toàn bộ danh sách tài liệu của dự án này
        List<Doc> docs = DocDB.selectByProjectId(projectId);

        // 3. Xác định bài viết nào sẽ được mở đọc ở khung bên phải (selectedDoc)
        int docId = safeParseInt(request.getParameter("docId"), 0);
        Doc selectedDoc = null;

        if (docId > 0) {
            Doc candidate = DocDB.selectById(docId);
            // BẢO MẬT: Chỉ nhận bài viết nếu nó thuộc đúng dự án hiện tại
            if (candidate != null && candidate.getProjectId() == projectId) {
                selectedDoc = candidate;
            }
        }

        // Nếu chưa chọn bài nào hoặc docId không hợp lệ/khác dự án -> Tự động mở bài đầu tiên trong danh sách của dự án
        if (selectedDoc == null && docs != null && !docs.isEmpty()) {
            selectedDoc = docs.get(0);
        }

        // 4. LẤY DANH SÁCH CÁC TASK ĐANG THAM CHIẾU TÀI LIỆU NÀY (TaskDoc)
        List<Task> relatedTasks = new ArrayList<>();
        if (selectedDoc != null) {
            List<Integer> relatedTaskIds = TaskDocDB.selectTaskIdsByDocId(selectedDoc.getId());
            for (Integer taskId : relatedTaskIds) {
                Task t = TaskDB.selectById(taskId);
                if (t != null && t.getProjectId() == projectId) {
                    relatedTasks.add(t);
                }
            }
        }

        // 5. Đóng gói dữ liệu vào Request Attribute
        request.setAttribute("project", project);
        request.setAttribute("docs", docs);
        request.setAttribute("selectedDoc", selectedDoc);
        request.setAttribute("relatedTasks", relatedTasks); // Danh sách task liên quan
        request.setAttribute("activeNav", "docs"); // Bật sáng menu Tài liệu

        // 6. Chuyển giao cho giao diện docs.jsp hiển thị
        request.getRequestDispatcher("/docs.jsp").forward(request, response);
    }

    /**
     * Nghiệp vụ 2: Xóa một bài viết tài liệu theo docId kèm dọn dẹp các liên kết TaskDoc
     * Bảo mật chống IDOR xóa xuyên dự án (Fix DOC-02).
     */
    private void handleDeleteDoc(HttpServletRequest request, HttpServletResponse response, User currentUser, int projectId)
            throws IOException {

        HttpSession session = request.getSession(false);
        int docId = safeParseInt(request.getParameter("docId"), 0);

        if (docId <= 0) {
            response.sendRedirect(request.getContextPath() + "/doc?action=list&projectId=" + projectId);
            return;
        }

        Doc doc = DocDB.selectById(docId);
        Project project = ProjectDB.selectById(projectId);

        if (doc != null && project != null) {
            // RÀO BẢO MẬT 1: Chống IDOR (Tài liệu phải thuộc đúng dự án này)
            if (doc.getProjectId() != projectId) {
                if (session != null) {
                    session.setAttribute("toastError", "Cảnh báo bảo mật: Tài liệu không thuộc dự án này!");
                }
                response.sendRedirect(request.getContextPath() + "/doc?action=list&projectId=" + projectId);
                return;
            }

            // RÀO BẢO MẬT 2: Phân quyền (Chính tác giả bài viết HOẶC PM dự án)
            if (currentUser.getId() == doc.getAuthorId() || currentUser.getId() == project.getOwnerId()) {
                TaskDocDB.deleteByDocId(docId);
                DocDB.delete(docId);
                if (session != null) {
                    session.setAttribute("toastSuccess", "Đã xóa tài liệu thành công!");
                }
            } else {
                if (session != null) {
                    session.setAttribute("toastError", "Bạn không có quyền xóa tài liệu của người khác!");
                }
            }
        }

        // Xóa xong -> Redirect về lại danh sách tài liệu của dự án
        response.sendRedirect(request.getContextPath() + "/doc?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 3: Tạo bài viết tài liệu mới từ Form
     */
    private void handleCreateDoc(HttpServletRequest request, HttpServletResponse response, User currentUser, int projectId)
            throws IOException {

        HttpSession session = request.getSession();
        String title = request.getParameter("title");
        String content = request.getParameter("content");

        // 1. Validation: Tiêu đề không được để trống
        if (title == null || title.trim().isEmpty()) {
            if (session != null) {
                session.setAttribute("toastError", "Tiêu đề tài liệu không được để trống!");
            }
            response.sendRedirect(request.getContextPath() + "/doc?action=list&projectId=" + projectId);
            return;
        }

        // 2. Lấy thời gian realtime hiện tại (Định dạng: dd/MM/yyyy HH:mm)
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        // 3. Tạo đối tượng Doc mới và lưu vào RAM
        Doc newDoc = new Doc(
            0,
            projectId,
            title.trim(),
            (content != null ? content.trim() : ""),
            currentUser.getId(),
            currentUser.getFullName(),
            now, // Ngày tạo
            now  // Ngày cập nhật ban đầu trùng ngày tạo
        );

        int newDocId = DocDB.insert(newDoc);

        // Ghi nhật ký hoạt động dự án
        ActivityLogDB.logAsync(projectId, currentUser.getId(), "DOC_CREATE", "DOC", newDocId, newDoc.getTitle(), "Đã tải lên tài liệu mới: " + newDoc.getTitle());

        if (session != null) {
            session.setAttribute("toastSuccess", "Đã tạo tài liệu mới thành công!");
        }

        // 4. Mở thẳng vào bài viết vừa tạo xong!
        response.sendRedirect(request.getContextPath() + "/doc?action=view&projectId=" + projectId + "&docId=" + newDocId);
    }

    /**
     * Nghiệp vụ 4: Lưu chỉnh sửa nội dung bài viết cũ
     * Bảo mật chống IDOR ghi đè & chiếm đoạt bài viết xuyên dự án (Fix DOC-01).
     */
    private void handleUpdateDoc(HttpServletRequest request, HttpServletResponse response, User currentUser, int projectId)
            throws IOException {

        HttpSession session = request.getSession();
        int docId = safeParseInt(request.getParameter("docId"), 0);
        String title = request.getParameter("title");
        String content = request.getParameter("content");

        if (docId <= 0) {
            response.sendRedirect(request.getContextPath() + "/doc?action=list&projectId=" + projectId);
            return;
        }

        Doc existingDoc = DocDB.selectById(docId);
        Project project = ProjectDB.selectById(projectId);

        // RÀO BẢO MẬT 1: Kiểm tra tồn tại & Chống IDOR xuyên dự án
        if (existingDoc == null || project == null || existingDoc.getProjectId() != projectId) {
            if (session != null) {
                session.setAttribute("toastError", "Tài liệu không tồn tại hoặc không thuộc dự án này!");
            }
            response.sendRedirect(request.getContextPath() + "/doc?action=list&projectId=" + projectId);
            return;
        }

        // RÀO BẢO MẬT 2: Phân quyền (Chính tác giả bài viết HOẶC PM của dự án mới được sửa)
        if (currentUser.getId() != existingDoc.getAuthorId() && currentUser.getId() != project.getOwnerId()) {
            if (session != null) {
                session.setAttribute("toastError", "Bạn không có quyền chỉnh sửa tài liệu của người khác!");
            }
            response.sendRedirect(request.getContextPath() + "/doc?action=view&projectId=" + projectId + "&docId=" + docId);
            return;
        }

        // 2. Validation tiêu đề
        if (title == null || title.trim().isEmpty()) {
            if (session != null) {
                session.setAttribute("toastError", "Tiêu đề tài liệu không được để trống!");
            }
            response.sendRedirect(request.getContextPath() + "/doc?action=view&projectId=" + projectId + "&docId=" + docId);
            return;
        }

        // 3. Lấy thời gian sửa đổi hiện tại
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        // 4. Cập nhật thông tin an toàn trên chính đối tượng gốc (Giữ nguyên createdAt và projectId gốc)
        existingDoc.setTitle(title.trim());
        existingDoc.setContent(content != null ? content.trim() : "");
        existingDoc.setUpdatedAt(now);

        // 5. Cập nhật vào DB
        DocDB.update(existingDoc);

        if (session != null) {
            session.setAttribute("toastSuccess", "Đã cập nhật nội dung tài liệu thành công!");
        }

        // 6. Tải lại chính bài viết vừa sửa xong
        response.sendRedirect(request.getContextPath() + "/doc?action=view&projectId=" + projectId + "&docId=" + docId);
    }
}
