package com.teamwork.controllers;

import com.teamwork.business.User;
import com.teamwork.util.RequestUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Lớp cha của mọi Controller: gom các thao tác servlet nào cũng cần (lấy user, đọc tham số số,
 * gửi toast, chặn người ngoài dự án, trả JSON) để không phải chép lại ở từng file.
 */
public abstract class BaseServlet extends HttpServlet {

    /** Trang quay về khi không có quyền vào dự án. */
    protected static final String PROJECT_LIST = "/project?action=list";

    /** User đang đăng nhập, hoặc null. Không tạo session mới. Public để các handler của /task dùng chung. */
    public static User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (session != null) ? (User) session.getAttribute("currentUser") : null;
    }

    /** Đọc tham số dạng số nguyên; thiếu hoặc sai định dạng thì trả {@code defaultValue}. */
    protected static int intParam(HttpServletRequest request, String name, int defaultValue) {
        return RequestUtil.parseInt(request.getParameter(name), defaultValue);
    }

    /** Ghi thông báo một lần (toastError / toastSuccess...) để trang kế tiếp hiển thị. */
    protected static void flash(HttpServletRequest request, String key, String message) {
        request.getSession().setAttribute(key, message);
    }

    /** Redirect tới đường dẫn nội bộ (tự thêm context path). */
    protected static void redirect(HttpServletRequest request, HttpServletResponse response, String localPath)
            throws IOException {
        response.sendRedirect(request.getContextPath() + localPath);
    }

    /**
     * Chặn người không thuộc dự án: báo lỗi bằng toast rồi đưa về danh sách dự án.
     *
     * @return true nếu được phép đi tiếp; false nếu đã redirect (servlet phải {@code return} ngay).
     */
    protected static boolean requireMember(HttpServletRequest request, HttpServletResponse response,
                                           User user, int projectId, String deniedMessage) throws IOException {
        if (ProjectAccess.isMember(user, projectId)) {
            return true;
        }
        if (projectId > 0 && deniedMessage != null) {
            flash(request, "toastError", deniedMessage);
        }
        redirect(request, response, PROJECT_LIST);
        return false;
    }

    /** Trả một chuỗi JSON đã dựng sẵn với mã HTTP cho trước. */
    protected static void writeJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}
