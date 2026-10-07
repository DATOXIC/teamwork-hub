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
import com.teamwork.data.MessageDB;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.SubTaskDB;
import com.teamwork.data.TaskDB;
import com.teamwork.data.TaskDocDB;
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
import com.teamwork.business.ProjectMember;
import com.teamwork.data.NotificationDB;
import com.teamwork.data.ProjectInviteDB;
import com.teamwork.business.UserWorkload;
import com.teamwork.business.ActivityLog;
import com.teamwork.data.ActivityLogDB;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import com.teamwork.controllers.BaseServlet;
import com.teamwork.controllers.ProjectAccess;
import com.teamwork.util.RequestUtil;
import static com.teamwork.controllers.task.TaskJson.*;
import static com.teamwork.controllers.task.TaskBoardHandler.*;
import static com.teamwork.controllers.task.TaskCrudHandler.*;
import static com.teamwork.controllers.task.SubTaskHandler.*;
import static com.teamwork.controllers.task.TaskWorkflowHandler.*;

/**
 * Hàm kiểm tra quyền dùng chung cho các handler của /task: lấy user hiện tại, Project Owner (PM), Task Lead, người được giao Sub-task.
 *
 * <p>Được gọi từ {@link com.teamwork.controllers.TaskServlet} (bộ điều phối duy nhất của URL /task).
 * Tên hàm {@code handleXxx} khớp với tham số {@code action} trong form / fetch của tasks.jsp.</p>
 */
public final class TaskAccess {

    private TaskAccess() {}

    // ==================== BỘ TIỆN ÍCH TRÍCH XUẤT & PHÂN QUYỀN CHUẨN BACKEND CODE MASTERY ====================

    /**
     * Lấy User hiện tại đang đăng nhập từ Session một cách an toàn (tránh tạo Session rác).
     */
    public static User getCurrentUser(HttpServletRequest request) {
        return BaseServlet.currentUser(request);
    }

    /**
     * Phân tích chuỗi thành số nguyên an toàn, trả về defaultValue nếu null, rỗng hoặc sai định dạng.
     */
    public static int safeParseInt(String param, int defaultValue) {
        return RequestUtil.parseInt(param, defaultValue);
    }

    /**
     * Kiểm tra xem người dùng có phải là Trưởng Dự Án (PM / Project Owner) không.
     */
    public static boolean isProjectOwner(User user, Project project) {
        return ProjectAccess.isOwner(user, project);
    }

    /**
     * Kiểm tra xem người dùng có phải là Task Lead của thẻ công việc lớn không.
     */
    public static boolean isTaskLead(User user, Task task) {
        return user != null && task != null && task.getAssigneeId() > 0 && user.getId() == task.getAssigneeId();
    }

    /**
     * Kiểm tra xem người dùng có phải là Người thực hiện việc con (SubTask Assignee) không.
     */
    public static boolean isSubTaskAssignee(User user, SubTask subTask) {
        return user != null && subTask != null && subTask.getAssigneeId() > 0 && user.getId() == subTask.getAssigneeId();
    }

    /**
     * Kiểm tra thẩm quyền quản lý Việc con 3 Bên: Người làm việc con HOẶC Task Lead HOẶC Trưởng Dự Án.
     */
    public static boolean canManageSubTask(User user, SubTask st, Task parentTask, Project project) {
        return isSubTaskAssignee(user, st) || isTaskLead(user, parentTask) || isProjectOwner(user, project);
    }

    /**
     * Kiểm tra thẩm quyền thẩm định / duyệt Việc con (Cổng 2): Task Lead HOẶC PM (nếu task chưa có lead).
     */
    public static boolean canReviewSubTask(User user, Task parentTask, Project project) {
        return isTaskLead(user, parentTask) || (parentTask != null && parentTask.getAssigneeId() == 0 && isProjectOwner(user, project));
    }
}
