package com.teamwork.controllers;

import com.teamwork.business.Notification;
import com.teamwork.business.User;
import com.teamwork.data.NotificationDB;
import com.teamwork.util.RedirectUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * NotificationServlet — Controller quản lý Trung Tâm Thông Báo 🔔 (/notification).
 *
 * <p><b>Các luồng được xử lý:</b></p>
 * <ul>
 *   <li>GET /notification?action=read&id=X&redirect=...  → {@link #handleMarkAsReadAndRedirect} — Đọc 1 thông báo + Deep-link tới mục tiêu</li>
 *   <li>GET /notification?action=readAll                 → {@link #handleMarkAllAsRead} — Đọc tất cả (xóa số đỏ 🔴)</li>
 *   <li>GET /notification?action=delete&id=X             → {@link #handleDelete} — Xóa 1 thông báo</li>
 * </ul>
 *
 * <p><b>Lưu ý thiết kế:</b> Cả {@code doGet} và {@code doPost} đều được định tuyến về cùng
 * một logic, giúp form HTML có thể gọi qua POST mà không cần thêm handler riêng.</p>
 *
 * <p><b>TODO — Điểm mở rộng phổ biến (Extension Points):</b></p>
 * <ul>
 *   <li>Thêm action mới "deleteAll" — xóa toàn bộ thông báo đã đọc</li>
 *   <li>Thêm action "list" — hiển thị trang lịch sử tất cả thông báo</li>
 *   <li>Thêm filter theo loại thông báo (INVITE / TASK / SYSTEM)</li>
 * </ul>
 */
@WebServlet("/notification")
public class NotificationServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // GET chỉ cho "read" (bấm vào 1 thông báo để mở link đích). readAll / delete phải qua POST:
        // link GET có thể bị kích hoạt từ trang khác chỉ bằng <img src> (CSRF).
        dispatch(request, response, false);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        dispatch(request, response, true);
    }

    private void dispatch(HttpServletRequest request, HttpServletResponse response, boolean isPost)
            throws IOException {

        HttpSession session = request.getSession(false);
        User currentUser = currentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        switch (action) {
            case "read":
                handleMarkAsReadAndRedirect(request, response, currentUser);
                break;
            case "readAll":
                if (isPost) {
                    handleMarkAllAsRead(request, response, currentUser);
                } else {
                    response.sendRedirect(request.getContextPath() + "/project?action=list");
                }
                break;
            case "delete":
                if (isPost) {
                    handleDelete(request, response, currentUser);
                } else {
                    response.sendRedirect(request.getContextPath() + "/project?action=list");
                }
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/project?action=list");
                break;
        }
    }

    /**
     * Xử lý khi người dùng NHẤP VÀO 1 THÔNG BÁO:
     * 1. Đổi trạng thái isRead = true
     * 2. Tự động chuyển hướng (Redirect) thẳng tới đúng Task/Dự án mục tiêu (Deep-linking)
     */
    private void handleMarkAsReadAndRedirect(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        int notifId = 0;
        try {
            notifId = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        NotificationDB.markAsRead(notifId, currentUser.getId());

        // Nhảy tới link đích của thông báo — CHỈ đường dẫn nội bộ (chặn ?redirect=//trang-gia-mao.com)
        response.sendRedirect(RedirectUtil.localOr(request, request.getParameter("redirect"), "/project?action=list"));
    }

    /**
     * Xử lý nút: "Đánh dấu tất cả là đã đọc"
     */
    private void handleMarkAllAsRead(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        NotificationDB.markAllAsRead(currentUser.getId());

        // Quay lại trang trước đó (Referer cùng host) hoặc về trang chủ dự án
        response.sendRedirect(RedirectUtil.backOr(request, "/project?action=list"));
    }

    /**
     * Xử lý xóa một thông báo
     */
    private void handleDelete(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        int notifId = 0;
        try {
            notifId = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        NotificationDB.delete(notifId, currentUser.getId());

        response.sendRedirect(RedirectUtil.backOr(request, "/project?action=list"));
    }
}
