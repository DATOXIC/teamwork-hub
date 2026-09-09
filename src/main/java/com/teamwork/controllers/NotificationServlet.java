package com.teamwork.controllers;

import com.teamwork.business.Notification;
import com.teamwork.business.User;
import com.teamwork.data.NotificationDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Controller: Quản lý Quả Chuông 🔔 & Trung Tâm Thông Báo (/notification)
 * - Đánh dấu thông báo đã đọc & Tự động điều hướng đến đúng Task/Dự án (Deep-linking)
 * - Đánh dấu tất cả thông báo là đã đọc (Xóa số đỏ 🔴 trên chuông)
 */
@WebServlet("/notification")
public class NotificationServlet extends HttpServlet {

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
        if (action == null || action.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        switch (action) {
            case "read":
                handleMarkAsReadAndRedirect(request, response, currentUser);
                break;
            case "readAll":
                handleMarkAllAsRead(request, response, currentUser);
                break;
            case "delete":
                handleDelete(request, response, currentUser);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/project?action=list");
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
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

        NotificationDB.markAsReadForRecipient(notifId, currentUser.getId());

        // Lấy link mục tiêu để nhảy tới
        String targetLink = request.getParameter("redirect");
        if (targetLink == null || targetLink.trim().isEmpty() || "#".equals(targetLink.trim())) {
            targetLink = "/project?action=list";
        }

        // Đảm bảo không bị lặp context path
        if (!targetLink.startsWith(request.getContextPath())) {
            if (!targetLink.startsWith("/")) {
                targetLink = "/" + targetLink;
            }
            targetLink = request.getContextPath() + targetLink;
        }

        response.sendRedirect(targetLink);
    }

    /**
     * Xử lý nút: "Đánh dấu tất cả là đã đọc"
     */
    private void handleMarkAllAsRead(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {

        NotificationDB.markAllAsRead(currentUser.getId());

        // Quay lại trang trước đó (Referer header) hoặc về trang chủ dự án
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.trim().isEmpty()) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
        }
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

        NotificationDB.deleteForRecipient(notifId, currentUser.getId());

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.trim().isEmpty()) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
        }
    }
}
