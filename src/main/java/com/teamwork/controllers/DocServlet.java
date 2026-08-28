package com.teamwork.controllers;

import com.teamwork.business.Doc;
import com.teamwork.business.Project;
import com.teamwork.business.Task;
import com.teamwork.business.User;
import com.teamwork.data.DocDB;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.TaskDB;
import com.teamwork.data.TaskDocDB;
import com.teamwork.data.ProjectMemberDB;
import jakarta.servlet.ServletException;
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
 * Controller phụ trách Quản lý Tài liệu & Ghi chú Wiki nhóm:
 * - Xem danh mục bài viết, đọc chi tiết bài và danh sách các Task đang áp dụng bài viết này (GET /doc?action=list hoặc action=view)
 * - Tạo bài viết mới (POST /doc?action=create)
 * - Chỉnh sửa cập nhật bài viết (POST /doc?action=update)
 * - Xóa bài viết và tự động dọn dẹp các liên kết TaskDoc (GET /doc?action=delete)
 */
public class DocServlet extends HttpServlet {

    // ========================================================
    // HÀM doGet: XỬ LÝ CÁC YÊU CẦU ĐỌC & XEM TÀI LIỆU
    // ========================================================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Lấy và kiểm tra an toàn tham số projectId từ URL
        String projectIdParam = request.getParameter("projectId");
        int projectId = 0;

        if (projectIdParam == null || projectIdParam.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        try {
            projectId = Integer.parseInt(projectIdParam.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 2. Đọc action từ URL (Mặc định là "list" nếu không truyền)
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser != null && !ProjectMemberDB.isMember(projectId, currentUser.getId())) {
            session.setAttribute("toastError", "Bạn không có quyền truy cập vào dự án này!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 3. Phân nhánh hành động GET
        switch (action) {
            case "list":
            case "view":
                handleShowDocs(request, response, projectId);
                break;

            case "delete":
                handleDeleteDoc(request, response, projectId);
                break;

            default:
                handleShowDocs(request, response, projectId);
                break;
        }
    }

    // ========================================================
    // HÀM doPost: XỬ LÝ CÁC YÊU CẦU GHI & SỬA TÀI LIỆU
    // ========================================================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Lấy thông tin User đang đăng nhập từ Session để biết ai là tác giả bài viết
        HttpSession session = request.getSession(false);
        User currentUser = null;
        if (session != null) {
            currentUser = (User) session.getAttribute("currentUser");
        }

        // 2. Đọc action từ Form gửi lên
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "create"; // Mặc định là tạo bài mới
        }

        String projectIdParam = request.getParameter("projectId");
        if (projectIdParam != null) {
            try {
                int projectId = Integer.parseInt(projectIdParam.trim());
                if (currentUser != null && !ProjectMemberDB.isMember(projectId, currentUser.getId())) {
                    session.setAttribute("toastError", "Bạn không có quyền thao tác trong dự án này!");
                    response.sendRedirect(request.getContextPath() + "/project?action=list");
                    return;
                }
            } catch (Exception e) {}
        }

        // 3. Phân nhánh hành động POST
        switch (action) {
            case "create":
                handleCreateDoc(request, response, currentUser);
                break;

            case "update":
                handleUpdateDoc(request, response, currentUser);
                break;

            default:
                response.sendRedirect(request.getContextPath() + "/project?action=list");
                break;
        }
    }

    // ========================================================
    // CÁC HÀM NGHIỆP VỤ CON (PRIVATE METHODS)
    // ========================================================

    /**
     * Nghiệp vụ 1: Lấy danh mục bài viết (bên trái), bài viết đang đọc (bên phải) 
     * VÀ danh sách các Công việc đang áp dụng tài liệu này (relatedTasks) đưa sang docs.jsp
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
        String docIdParam = request.getParameter("docId");
        Doc selectedDoc = null;

        // Nếu người dùng bấm vào 1 bài cụ thể có docId trên URL
        if (docIdParam != null && !docIdParam.trim().isEmpty()) {
            try {
                int docId = Integer.parseInt(docIdParam.trim());
                selectedDoc = DocDB.selectById(docId);
            } catch (NumberFormatException e) {
                selectedDoc = null;
            }
        }

        // Nếu chưa chọn bài nào hoặc docId không hợp lệ -> Tự động mở bài đầu tiên trong danh sách
        if (selectedDoc == null && docs != null && !docs.isEmpty()) {
            selectedDoc = docs.get(0);
        }

        // 4. LẤY DANH SÁCH CÁC TASK ĐANG THAM CHIẾU TÀI LIỆU NÀY (TaskDoc)
        List<Task> relatedTasks = new ArrayList<>();
        if (selectedDoc != null) {
            List<Integer> relatedTaskIds = TaskDocDB.selectTaskIdsByDocId(selectedDoc.getId());
            for (Integer taskId : relatedTaskIds) {
                Task t = TaskDB.selectById(taskId);
                if (t != null) {
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
     */
    private void handleDeleteDoc(HttpServletRequest request, HttpServletResponse response, int projectId)
            throws IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        String docIdParam = request.getParameter("docId");
        if (docIdParam != null && !docIdParam.trim().isEmpty() && currentUser != null) {
            try {
                int docId = Integer.parseInt(docIdParam.trim());
                Doc doc = DocDB.selectById(docId);
                Project project = ProjectDB.selectById(projectId);

                if (doc != null && project != null) {
                    if (currentUser.getId() == doc.getAuthorId() || currentUser.getId() == project.getOwnerId()) {
                        TaskDocDB.deleteByDocId(docId);
                        DocDB.delete(docId);
                        if (session != null) session.setAttribute("toastSuccess", "Đã xóa tài liệu thành công!");
                    } else {
                        if (session != null) session.setAttribute("toastError", "Bạn không có quyền xóa tài liệu của người khác!");
                    }
                }
            } catch (NumberFormatException e) {
                // Bỏ qua nếu docId không hợp lệ
            }
        }

        // Xóa xong -> Redirect về lại danh sách tài liệu của dự án
        response.sendRedirect(request.getContextPath() + "/doc?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 3: Tạo bài viết tài liệu mới từ Form
     */
    private void handleCreateDoc(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        // 1. Đọc dữ liệu từ form
        String projectIdParam = request.getParameter("projectId");
        String title = request.getParameter("title");
        String content = request.getParameter("content");

        int projectId = 0;
        try {
            projectId = Integer.parseInt(projectIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 2. Validation: Tiêu đề không được để trống
        if (title == null || title.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/doc?action=list&projectId=" + projectId);
            return;
        }

        // 3. Lấy thời gian realtime hiện tại (Định dạng: dd/MM/yyyy HH:mm)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String now = LocalDateTime.now().format(formatter);

        // 4. Lấy thông tin tác giả
        int authorId = (currentUser != null) ? currentUser.getId() : 0;
        String authorName = (currentUser != null) ? currentUser.getFullName() : "Ẩn danh";

        // 5. Tạo đối tượng Doc mới và lưu vào RAM
        Doc newDoc = new Doc(
            0,
            projectId,
            title.trim(),
            (content != null ? content.trim() : ""),
            authorId,
            authorName,
            now, // Ngày tạo
            now  // Ngày cập nhật ban đầu trùng ngày tạo
        );

        int newDocId = DocDB.insert(newDoc);

        // 6. Mở thẳng vào bài viết vừa tạo xong!
        response.sendRedirect(request.getContextPath() + "/doc?action=view&projectId=" + projectId + "&docId=" + newDocId);
    }

    /**
     * Nghiệp vụ 4: Lưu chỉnh sửa nội dung bài viết cũ
     */
    private void handleUpdateDoc(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        // 1. Đọc dữ liệu từ form sửa
        String projectIdParam = request.getParameter("projectId");
        String docIdParam = request.getParameter("docId");
        String title = request.getParameter("title");
        String content = request.getParameter("content");

        int projectId = 0;
        int docId = 0;

        try {
            projectId = Integer.parseInt(projectIdParam.trim());
            docId = Integer.parseInt(docIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 2. Validation tiêu đề
        if (title == null || title.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/doc?action=view&projectId=" + projectId + "&docId=" + docId);
            return;
        }

        // 3. Lấy thời gian sửa đổi hiện tại
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String now = LocalDateTime.now().format(formatter);

        // 4. Lấy thông tin người sửa
        int authorId = (currentUser != null) ? currentUser.getId() : 0;
        String authorName = (currentUser != null) ? currentUser.getFullName() : "Ẩn danh";

        // 5. Tạo đối tượng Doc chứa thông tin cập nhật
        Doc updatedDoc = new Doc(
            docId,
            projectId,
            title.trim(),
            (content != null ? content.trim() : ""),
            authorId,
            authorName,
            "",  // createdAt giữ nguyên ở trong DB
            now  // updatedAt mới
        );

        // 6. Cập nhật vào DB
        DocDB.update(updatedDoc);

        // 7. Tải lại chính bài viết vừa sửa xong
        response.sendRedirect(request.getContextPath() + "/doc?action=view&projectId=" + projectId + "&docId=" + docId);
    }
}
