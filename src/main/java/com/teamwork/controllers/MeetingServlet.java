package com.teamwork.controllers;

import com.teamwork.business.Project;
import com.teamwork.business.User;
import com.teamwork.data.ProjectDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * MeetingServlet — Phòng họp video của dự án qua Jitsi Meet (/meeting).
 *
 * <p>Mỗi dự án có một phòng cố định. Tên phòng là mã băm khó đoán từ id và tên dự án,
 * nên người ngoài không thể đoán ra phòng chỉ từ id dự án.</p>
 */
@WebServlet("/meeting")
public class MeetingServlet extends BaseServlet {

    private String roomName(Project project) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(("teamwork-hub|" + project.getId() + "|" + project.getName())
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("TeamworkHub");
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", h[i]));
            return sb.toString();
        } catch (Exception e) {
            return "TeamworkHub" + project.getId();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = currentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        int projectId = intParam(request, "projectId", 0);
        if (!requireMember(request, response, currentUser, projectId, null)) {
            return;
        }

        Project project = ProjectDB.selectById(projectId);
        if (project == null) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // ▶ JSP: tasks.jsp, command_palette.jsp, project_report.jsp đọc bằng ${project}
        request.setAttribute("project", project);
        // ▶ JSP: meeting.jsp đọc bằng ${roomName}
        request.setAttribute("roomName", roomName(project));
        // ▶ forward → meeting.jsp (các setAttribute ở trên chính là dữ liệu JSP hiển thị)
        request.getRequestDispatcher("/meeting.jsp").forward(request, response);
    }
}
