package com.teamwork.controllers;

import com.teamwork.business.Project;
import com.teamwork.business.User;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.ProjectMemberDB;
import com.teamwork.data.WhiteboardDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * WhiteboardServlet — Bảng vẽ cộng tác của dự án (/whiteboard).
 *
 * <ul>
 *   <li>GET  /whiteboard?projectId=X → Trang bảng vẽ (Excalidraw nhúng trong whiteboard.jsp)</li>
 *   <li>POST /whiteboard?projectId=X → Lưu JSON bảng vẽ (gọi bằng fetch tự động lưu)</li>
 * </ul>
 */
@WebServlet("/whiteboard")
public class WhiteboardServlet extends HttpServlet {

    private static final int MAX_BODY_BYTES = 8 * 1024 * 1024;

    private int safeParseInt(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
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

        int projectId = safeParseInt(request.getParameter("projectId"));
        if (projectId <= 0 || !ProjectMemberDB.isMember(projectId, currentUser.getId())) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        Project project = ProjectDB.selectById(projectId);
        if (project == null) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // Escape '<' để JSON nhúng an toàn trong thẻ <script> (chống thoát khỏi script)
        String json = WhiteboardDB.selectContentByProjectId(projectId).replace("<", "\\u003c");
        // ▶ JSP: tasks.jsp, command_palette.jsp, project_report.jsp đọc bằng ${project}
        request.setAttribute("project", project);
        // ▶ JSP: whiteboard.jsp đọc bằng ${whiteboardJson}
        request.setAttribute("whiteboardJson", json);
        // ▶ forward → whiteboard.jsp (các setAttribute ở trên chính là dữ liệu JSP hiển thị)
        request.getRequestDispatcher("/whiteboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // ▶ JS: trả JSON cho fetch(saveUrl) trong js/whiteboard.js (lưu bảng vẽ; không forward JSP)
        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"ok\":false}");
            return;
        }

        int projectId = safeParseInt(request.getParameter("projectId"));
        if (projectId <= 0 || !ProjectMemberDB.isMember(projectId, currentUser.getId())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"ok\":false}");
            return;
        }

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (InputStream in = request.getInputStream()) {
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) != -1) {
                buf.write(chunk, 0, n);
                if (buf.size() > MAX_BODY_BYTES) {
                    response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                    response.getWriter().write("{\"ok\":false,\"error\":\"too_large\"}");
                    return;
                }
            }
        }
        String body = new String(buf.toByteArray(), StandardCharsets.UTF_8);

        boolean ok = WhiteboardDB.save(projectId, body, currentUser.getId());
        if (!ok) response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write(ok ? "{\"ok\":true}" : "{\"ok\":false}");
    }
}
