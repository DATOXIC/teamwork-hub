package com.teamwork.controllers.task;

import com.teamwork.business.Doc;
import com.teamwork.business.Label;
import com.teamwork.business.Message;
import com.teamwork.business.Notification;
import com.teamwork.business.Project;
import com.teamwork.business.SubTask;
import com.teamwork.business.Task;
import com.teamwork.business.TaskDoc;
import com.teamwork.business.User;
import com.teamwork.data.DocDB;
import com.teamwork.data.LabelDB;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.teamwork.business.ProjectInvite;
import com.teamwork.data.NotificationDB;
import com.teamwork.data.ProjectInviteDB;
import com.teamwork.business.UserWorkload;
import com.teamwork.business.ActivityLog;
import com.teamwork.data.ActivityLogDB;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import static com.teamwork.controllers.task.TaskAccess.*;
import static com.teamwork.controllers.task.TaskBoardHandler.*;
import static com.teamwork.controllers.task.TaskCrudHandler.*;
import static com.teamwork.controllers.task.SubTaskHandler.*;
import static com.teamwork.controllers.task.TaskWorkflowHandler.*;

/**
 * Tiện ích trả JSON cho các request AJAX của /task (kéo-thả, quick-add, tạo nhãn).
 *
 * <p>Được gọi từ {@link com.teamwork.controllers.TaskServlet} (bộ điều phối duy nhất của URL /task).
 * Tên hàm {@code handleXxx} khớp với tham số {@code action} trong form / fetch của tasks.jsp.</p>
 */
public final class TaskJson {

    private TaskJson() {}

    public static void writeLabelJson(HttpServletResponse response, int status, Label label, String error) throws IOException {
        response.setStatus(status);
        // ▶ JS: trả JSON cho fetch() trong js/tasks-board.js, js/tasks.js (không forward JSP, trang không reload)
        response.setContentType("application/json;charset=UTF-8");
        String json;
        if (label != null) {
            json = "{\"ok\":true,\"key\":" + jsonString(label.getKey())
                    + ",\"name\":" + jsonString(label.getName())
                    + ",\"buttonClass\":" + jsonString(label.getButtonClass())
                    + ",\"emoji\":" + jsonString(label.getIconEmoji()) + "}";
        } else {
            json = "{\"ok\":false,\"message\":" + jsonString(error) + "}";
        }
        response.getWriter().write(json);
    }

    public static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * Nhận diện request AJAX từ Fetch API, XMLHttpRequest hoặc cờ ajax=true
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        String xReq = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        String ajaxParam = request.getParameter("ajax");
        return "XMLHttpRequest".equalsIgnoreCase(xReq)
                || "true".equalsIgnoreCase(ajaxParam)
                || (accept != null && accept.contains("application/json"));
    }

    /**
     * Gửi phản hồi chuẩn JSON cho các tương tác Single-Page không reload
     */
    public static void sendJsonResponse(HttpServletResponse response, boolean success, String message, String dataJson) throws IOException {
        // ▶ JS: trả JSON cho fetch() trong js/tasks-board.js, js/tasks.js (không forward JSP, trang không reload)
        response.setContentType("application/json;charset=UTF-8");
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":").append(success);
        if (message != null) {
            sb.append(",\"message\":\"").append(escapeJson(message)).append("\"");
        }
        if (dataJson != null && !dataJson.trim().isEmpty()) {
            sb.append(",\"data\":").append(dataJson);
        }
        sb.append("}");
        response.getWriter().write(sb.toString());
    }

    /**
     * Escape ký tự đặc biệt cho chuỗi JSON an toàn
     */
    public static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
