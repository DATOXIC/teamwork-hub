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
import jakarta.servlet.http.HttpServlet;
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
import com.teamwork.data.ProjectMemberDB;
import jakarta.servlet.http.HttpSession;
import com.teamwork.business.UserWorkload;
import com.teamwork.business.ActivityLog;
import com.teamwork.data.ActivityLogDB;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import static com.teamwork.controllers.task.TaskAccess.*;
import static com.teamwork.controllers.task.TaskJson.*;
import static com.teamwork.controllers.task.TaskCrudHandler.*;
import static com.teamwork.controllers.task.SubTaskHandler.*;
import static com.teamwork.controllers.task.TaskWorkflowHandler.*;

/**
 * Nhóm HIỂN THỊ bảng Kanban: GET /task?action=list (→ tasks.jsp), exportCsv, delete Task.
 *
 * <p>Được gọi từ {@link com.teamwork.controllers.TaskServlet} (bộ điều phối duy nhất của URL /task).
 * Tên hàm {@code handleXxx} khớp với tham số {@code action} trong form / fetch của tasks.jsp.</p>
 */
public final class TaskBoardHandler {

    private TaskBoardHandler() {}


    /**
     * Nghiệp vụ 1: Lấy toàn bộ dữ liệu 3 cột Kanban, danh sách tài liệu dự án,
     * map liên kết TaskDoc, map bình luận TaskComments, map Việc Con SubTasks và % Tiến độ
     */
    public static void handleShowKanban(HttpServletRequest request, HttpServletResponse response, int projectId)
            throws ServletException, IOException {

        // 1. Kiểm tra dự án tồn tại
        Project project = ProjectDB.selectById(projectId);

        // Quay về trang Project
        if (project == null) 
        {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 1.5. Ghi nhớ Cookie dự án truy cập gần nhất (hạn 30 ngày)
        jakarta.servlet.http.Cookie lastProjectCookie = new jakarta.servlet.http.Cookie("last_project_id", String.valueOf(projectId));
        lastProjectCookie.setMaxAge(30 * 24 * 60 * 60);
        lastProjectCookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        lastProjectCookie.setHttpOnly(true);
        response.addCookie(lastProjectCookie);

        // 2. TỐI ƯU HÓA: Lấy TẤT CẢ Task của Dự án trong 1 câu SQL duy nhất
        List<Task> allProjectTasks = TaskDB.selectByProjectId(projectId);
        List<Task> todoTasks = new ArrayList<>();
        List<Task> inProgressTasks = new ArrayList<>();
        List<Task> doneTasks = new ArrayList<>();
        int submittedCount = 0;

        for (Task t : allProjectTasks) {
            String st = t.getStatus() != null ? t.getStatus().toUpperCase() : "TODO";
            if ("DONE".equals(st) || "APPROVED".equals(st)) {
                doneTasks.add(t);
            } else if ("IN_PROGRESS".equals(st) || "SUBMITTED".equals(st) || "REVISE".equals(st) || "REJECTED".equals(st)) {
                inProgressTasks.add(t);
                if ("SUBMITTED".equals(st)) {
                    submittedCount++;
                }
            } else {
                todoTasks.add(t);
            }
        }

        // 2.5. Nạp nhãn của dự án (tự tạo 5 nhãn mặc định nếu dự án chưa có) và gắn bảng tra màu cho từng Task
        List<Label> projectLabels = LabelDB.ensureDefaults(projectId);
        Map<String, Label> labelLookup = new HashMap<>();
        for (Label l : projectLabels) {
            labelLookup.put(l.getKey(), l);
        }
        for (Task t : allProjectTasks) {
            t.setLabelLookup(labelLookup);
        }

        // 3. Lấy thành viên dự án và CHỈ nạp thông tin của các thành viên đó (1 câu truy vấn).
        // Không nạp / cache toàn bộ user hệ thống vào session: lộ dữ liệu người ngoài dự án và dữ liệu cũ.
        List<ProjectMember> projectMemberList = ProjectMemberDB.selectByProjectId(projectId);
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("cached_system_users"); // dọn cache cũ của các phiên trước bản sửa này
        }
        List<Integer> memberIds = new ArrayList<>();
        for (ProjectMember pm : projectMemberList) {
            memberIds.add(pm.getUserId());
        }
        Map<Integer, User> memberUserMap = new HashMap<>();
        for (User u : UserDB.selectByIds(memberIds)) {
            memberUserMap.put(u.getId(), u);
        }

        List<User> userList = new ArrayList<>();
        for (ProjectMember pm : projectMemberList) {
            User u = memberUserMap.get(pm.getUserId());
            if (u != null) {
                userList.add(u);
            }
        }

        // 4. Lấy danh sách toàn bộ tài liệu Wiki của dự án này (1 câu SQL)
        List<Doc> docList = DocDB.selectByProjectId(projectId);

        // 5. BATCH LOAD: Lấy toàn bộ TaskDoc của cả dự án trong 1 câu SQL duy nhất
        List<TaskDoc> allTaskDocs = TaskDocDB.selectByProjectId(projectId);
        Map<Integer, List<TaskDoc>> taskDocsMap = new HashMap<>();
        for (TaskDoc td : allTaskDocs) {
            taskDocsMap.computeIfAbsent(td.getTaskId(), k -> new ArrayList<>()).add(td);
        }

        // 6. BATCH LOAD: Lấy toàn bộ Bình luận của các Task trong 1 câu SQL duy nhất
        List<Message> allTaskComments = MessageDB.selectTaskCommentsByProjectId(projectId);
        Map<Integer, List<Message>> taskCommentsMap = new HashMap<>();
        for (Message m : allTaskComments) {
            taskCommentsMap.computeIfAbsent(m.getTaskId(), k -> new ArrayList<>()).add(m);
        }

        // 7. BATCH LOAD: Lấy toàn bộ Việc con của các Task trong 1 câu SQL duy nhất
        List<SubTask> allSubTasks = SubTaskDB.selectByProjectId(projectId);
        Map<Integer, List<SubTask>> taskSubTasksMap = new HashMap<>();
        for (SubTask st : allSubTasks) {
            taskSubTasksMap.computeIfAbsent(st.getTaskId(), k -> new ArrayList<>()).add(st);
        }

        // 8. TÍNH TOÁN % TIẾN ĐỘ TRÊN BỘ NHỚ RAM (CỰC NHANH, 0.001ms, KHÔNG GỌI DATABASE)
        Map<Integer, Integer> taskProgressMap = new HashMap<>();
        for (Task t : allProjectTasks) {
            List<SubTask> subList = taskSubTasksMap.get(t.getId());
            if (subList == null || subList.isEmpty()) {
                boolean isDone = "DONE".equalsIgnoreCase(t.getStatus()) || "APPROVED".equalsIgnoreCase(t.getStatus());
                taskProgressMap.put(t.getId(), isDone ? 100 : 0);
            } else {
                long doneCount = subList.stream()
                        .filter(s -> "APPROVED".equalsIgnoreCase(s.getStatus()) || "DONE".equalsIgnoreCase(s.getStatus()))
                        .count();
                int pct = (int) Math.round(((double) doneCount / subList.size()) * 100);
                taskProgressMap.put(t.getId(), pct);
            }
        }

        // 8.4. Điểm sức khỏe Task (hạn chót vs tiến độ)
        Map<Integer, com.teamwork.util.TaskHealth> taskHealthMap = new HashMap<>();
        for (Task t : allProjectTasks) {
            taskHealthMap.put(t.getId(), com.teamwork.util.TaskHealth.of(t, taskProgressMap.get(t.getId())));
        }
        // ▶ JSP: risk_panel.jsp, tasks.jsp, health_badge.jsp đọc bằng ${taskHealthMap}
        request.setAttribute("taskHealthMap", taskHealthMap);

        // Chỉ hiện nút "Tải về" khi tệp bàn giao thật sự có trong /uploads/deliverables (chưa có chức năng upload)
        request.setAttribute("downloadableDeliverables", findDownloadableDeliverables(request, allProjectTasks));

        // 8.5. Tính toán khối lượng công việc của từng thành viên (UserWorkload DTO) cho Dải Avatar B.3
        List<Task> allTasks = new ArrayList<>(allProjectTasks);
        List<UserWorkload> userWorkloadList = computeUserWorkloads(userList, allTasks, taskSubTasksMap);

        // 8.6. Lấy danh sách lời mời của dự án (Chặng C.3)
        List<ProjectInvite> projectInviteList = ProjectInviteDB.selectByProjectId(projectId);
        int memberCount = ProjectMemberDB.countMembers(projectId);

        // Không còn danh sách "ứng viên" (toàn bộ user hệ thống): PM mời bằng cách nhập chính xác
        // username hoặc email (ProjectInviteServlet đã tìm theo usernameOrEmail).

        // 8.7. Xử lý Flash Message (Toast)
        if (session != null) 
        {
            String toastSuccess = (String) session.getAttribute("toastSuccess");
            if (toastSuccess != null) 
            {
                // ▶ JSP: docs.jsp, tasks.jsp đọc bằng ${toastSuccess}
                request.setAttribute("toastSuccess", toastSuccess);
                session.removeAttribute("toastSuccess");
            }
            String toastError = (String) session.getAttribute("toastError");
            if (toastError != null) 
            {
                // ▶ JSP: docs.jsp đọc bằng ${toastError}
                request.setAttribute("toastError", toastError);
                session.removeAttribute("toastError");
            }
        }

        // 8.8. Lấy danh sách toàn bộ dự án của User (cho Sidebar Spaces chuẩn ClickUp)
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        List<Project> userProjects = (currentUser != null) ? ProjectMemberDB.selectProjectsByUserId(currentUser.getId()) : new ArrayList<>();
        int unreadNotifCount = (currentUser != null) ? NotificationDB.countUnread(currentUser.getId()) : 0;
        List<Notification> userNotifications = (currentUser != null) ? NotificationDB.selectByRecipientId(currentUser.getId()) : new ArrayList<>();

        // 8.9. Lấy tin nhắn chat dự án (cho tab # Chat tích hợp)
        List<Message> projectChatMessages = MessageDB.selectRecentByProjectId(projectId, 50);

        // 8.10. Xác định tab đang xem (view: tasks, chat, docs, metrics)
        String currentView = request.getParameter("view");
        if (currentView == null || currentView.trim().isEmpty()) {
            currentView = "tasks";
        }

        // Chế độ xem task (taskView: list hoặc board)
        String taskView = request.getParameter("taskView");
        if (taskView == null || taskView.trim().isEmpty()) {
            // Đọc Cookie preferred_task_view nếu có
            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                    if ("preferred_task_view".equals(c.getName()) && c.getValue() != null && !c.getValue().trim().isEmpty()) {
                        taskView = c.getValue().trim().toLowerCase();
                        break;
                    }
                }
            }
            if (taskView == null || (!"board".equals(taskView) && !"list".equals(taskView))) {
                taskView = "list"; // Mặc định mở List View phân cấp chuẩn ClickUp
            }
        } else {
            taskView = taskView.trim().toLowerCase();
            if (!"board".equals(taskView) && !"list".equals(taskView)) {
                taskView = "list";
            }
            // Lưu Cookie preferred_task_view (hạn 30 ngày)
            jakarta.servlet.http.Cookie viewCookie = new jakarta.servlet.http.Cookie("preferred_task_view", taskView);
            viewCookie.setMaxAge(30 * 24 * 60 * 60);
            viewCookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
            response.addCookie(viewCookie);
        }

        // 8.11. Đọc Cookie preferred_subtask_mode nếu có (mặc định: collapsed)
        String subtaskMode = "collapsed";
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                if ("preferred_subtask_mode".equals(c.getName()) && c.getValue() != null && !c.getValue().trim().isEmpty()) {
                    subtaskMode = c.getValue().trim().toLowerCase();
                    break;
                }
            }
        }
        if (!"expanded".equals(subtaskMode) && !"hidden".equals(subtaskMode)) {
            subtaskMode = "collapsed";
        }

        // 9. Đóng gói dữ liệu gửi sang tasks.jsp
        // ▶ JSP: tasks.jsp, command_palette.jsp, project_report.jsp đọc bằng ${project}
        request.setAttribute("project", project);
        // ▶ JSP: tasks.jsp, risk_panel.jsp đọc bằng ${todoTasks}
        request.setAttribute("todoTasks", todoTasks);
        // ▶ JSP: tasks.jsp, risk_panel.jsp đọc bằng ${inProgressTasks}
        request.setAttribute("inProgressTasks", inProgressTasks);
        // ▶ JSP: tasks.jsp đọc bằng ${doneTasks}
        request.setAttribute("doneTasks", doneTasks);
        // ▶ JSP: tasks.jsp đọc bằng ${allProjectTasks}
        request.setAttribute("allProjectTasks", allProjectTasks);
        // ▶ JSP: tasks.jsp đọc bằng ${projectLabels}
        request.setAttribute("projectLabels", projectLabels);
        // ▶ JSP: chat.jsp, tasks.jsp đọc bằng ${userList}
        request.setAttribute("userList", userList);
        // ▶ JSP: chat.jsp, tasks.jsp đọc bằng ${docList}
        request.setAttribute("docList", docList);
        // ▶ JSP: tasks.jsp đọc bằng ${taskDocsMap}
        request.setAttribute("taskDocsMap", taskDocsMap);
        // ▶ JSP: tasks.jsp đọc bằng ${taskCommentsMap}
        request.setAttribute("taskCommentsMap", taskCommentsMap);
        // ▶ JSP: tasks.jsp đọc bằng ${taskSubTasksMap}
        request.setAttribute("taskSubTasksMap", taskSubTasksMap);
        // ▶ JSP: tasks.jsp đọc bằng ${taskProgressMap}
        request.setAttribute("taskProgressMap", taskProgressMap);
        // ▶ JSP: tasks.jsp đọc bằng ${userWorkloadList}
        request.setAttribute("userWorkloadList", userWorkloadList);
        // ▶ JSP: project_report.jsp, tasks.jsp đọc bằng ${submittedCount}
        request.setAttribute("submittedCount", submittedCount);
        // ▶ JSP: tasks.jsp đọc bằng ${projectMemberList}
        request.setAttribute("projectMemberList", projectMemberList);
        // ▶ JSP: tasks.jsp đọc bằng ${projectInviteList}
        request.setAttribute("projectInviteList", projectInviteList);
        // ▶ JSP: tasks.jsp, project_report.jsp đọc bằng ${memberCount}
        request.setAttribute("memberCount", memberCount);
        // ▶ JSP: profile.jsp, tasks.jsp đọc bằng ${userProjects}
        request.setAttribute("userProjects", userProjects);
        // ▶ JSP: navbar.jsp, tasks.jsp đọc bằng ${unreadNotifCount}
        request.setAttribute("unreadNotifCount", unreadNotifCount);
        // ▶ JSP: tasks.jsp đọc bằng ${userNotifications}
        request.setAttribute("userNotifications", userNotifications);
        request.setAttribute("projectChatMessages", projectChatMessages);
        List<ActivityLog> activityLogs = ActivityLogDB.selectByProjectId(projectId, 60);
        // ▶ JSP: tasks.jsp đọc bằng ${activityLogs}
        request.setAttribute("activityLogs", activityLogs);
        // ▶ JSP: tasks.jsp đọc bằng ${currentView}
        request.setAttribute("currentView", currentView);
        // ▶ JSP: tasks.jsp đọc bằng ${taskView}
        request.setAttribute("taskView", taskView);
        // ▶ JSP: tasks.jsp đọc bằng ${subtaskMode}
        request.setAttribute("subtaskMode", subtaskMode);
        // ▶ JSP: navbar.jsp đọc bằng ${activeNav}
        request.setAttribute("activeNav", "projects");

        // 10. Forward sang giao diện tasks.jsp
        // ▶ forward → tasks.jsp (các setAttribute ở trên chính là dữ liệu JSP hiển thị)
        request.getRequestDispatcher("/tasks.jsp").forward(request, response);
    }

    /**
     * Nghiệp vụ 2: Xóa một task lớn khỏi dự án theo taskId (BẢO VỆ PHÂN QUYỀN TASK LEAD / PM)
     */
    public static void handleDeleteTask(HttpServletRequest request, HttpServletResponse response, int projectId)
            throws IOException {

        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        if (taskId > 0) {
            User currentUser = getCurrentUser(request);
            Task task = TaskDB.selectById(taskId);
            Project project = ProjectDB.selectById(projectId);

            if (task != null && task.getProjectId() == projectId && (isTaskLead(currentUser, task) || isProjectOwner(currentUser, project)))
            {
                // RÀNG BUỘC KHÓA BẤT BIẾN: KHÔNG ĐƯỢC XÓA TASK ĐÃ DONE ĐỂ BẢO VỆ DỮ LIỆU & AUDIT LOG
                if ("DONE".equalsIgnoreCase(task.getStatus())) {
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        // ▶ JSP: docs.jsp đọc bằng ${toastError}
                        session.setAttribute("toastError", "🔒 Công việc [" + task.getTitle() + "] đã hoàn thành và được khóa vĩnh viễn, không thể xóa!");
                    }
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                // 1. Dọn dẹp các liên kết Task-Doc trên RAM
                TaskDocDB.deleteByTaskId(taskId);

                // 2. Dọn dẹp các việc con thuộc Task này trên RAM
                SubTaskDB.deleteByTaskId(taskId);

                // 3. Dọn dẹp các bình luận của Task này trên RAM
                MessageDB.deleteByTaskId(taskId);

                // 4. Xóa Task trong TaskDB
                TaskDB.delete(taskId);
            }
        }

        // Xóa xong -> Redirect về lại bảng Kanban của dự án
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 2.5: Xuất toàn bộ danh sách công việc của dự án ra file CSV chuẩn UTF-8 BOM.
     * UTF-8 BOM (\uFEFF) giúp Microsoft Excel trên Windows tự động nhận diện tiếng Việt có dấu 100% không bị vỡ font.
     */
    public static void handleExportCsv(HttpServletRequest request, HttpServletResponse response, int projectId)
            throws IOException {
        Project project = ProjectDB.selectById(projectId);
        String projectCode = (project != null && project.getProjectCode() != null && !project.getProjectCode().trim().isEmpty())
                ? project.getProjectCode().trim().replaceAll("[^a-zA-Z0-9.-]", "_")
                : ("project-" + projectId);

        List<Task> taskList = TaskDB.selectByProjectId(projectId);

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String filename = "teamwork-tasks-" + projectCode.toLowerCase() + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        // Ghi mã byte UTF-8 BOM (0xEF, 0xBB, 0xBF)
        OutputStream os = response.getOutputStream();
        os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

        // Dòng tiêu đề các cột chuẩn
        writer.println("Mã công việc,Tiêu đề công việc,Mô tả tóm tắt,Trạng thái,Mức ưu tiên,Người phụ trách,Hạn chót,Số nhiệm vụ,Tiến độ hoàn thành (%),Ngày nộp bàn giao,Ngày duyệt");

        for (Task t : taskList) {
            List<SubTask> subs = SubTaskDB.selectByTaskId(t.getId());
            int subCount = (subs != null) ? subs.size() : 0;
            int doneSubs = 0;
            if (subs != null) {
                for (SubTask st : subs) {
                    if ("DONE".equalsIgnoreCase(st.getStatus()) || "APPROVED".equalsIgnoreCase(st.getStatus())) {
                        doneSubs++;
                    }
                }
            }
            int progress = subCount > 0 ? (int) Math.round(((double) doneSubs / subCount) * 100) : ("DONE".equalsIgnoreCase(t.getStatus()) || "APPROVED".equalsIgnoreCase(t.getStatus()) ? 100 : 0);

            String statusLabel = t.getStatus();
            if ("TODO".equalsIgnoreCase(statusLabel)) statusLabel = "Cần làm";
            else if ("PLANNING".equalsIgnoreCase(statusLabel)) statusLabel = "Lập kế hoạch";
            else if ("IN_PROGRESS".equalsIgnoreCase(statusLabel)) statusLabel = "Đang làm";
            else if ("SUBMITTED".equalsIgnoreCase(statusLabel)) statusLabel = "Chờ duyệt";
            else if ("REVISE".equalsIgnoreCase(statusLabel)) statusLabel = "Cần chỉnh sửa";
            else if ("REJECTED".equalsIgnoreCase(statusLabel)) statusLabel = "Bị từ chối";
            else if ("DONE".equalsIgnoreCase(statusLabel) || "APPROVED".equalsIgnoreCase(statusLabel)) statusLabel = "Hoàn thành";

            String priorityLabel = t.getPriority();
            if ("HIGH".equalsIgnoreCase(priorityLabel)) priorityLabel = "Cao";
            else if ("MEDIUM".equalsIgnoreCase(priorityLabel)) priorityLabel = "Trung bình";
            else if ("LOW".equalsIgnoreCase(priorityLabel)) priorityLabel = "Thấp";

            writer.println(
                csvCell("#" + t.getId()) + "," +
                csvCell(t.getTitle()) + "," +
                csvCell(t.getDescription()) + "," +
                csvCell(statusLabel) + "," +
                csvCell(priorityLabel) + "," +
                csvCell(t.getAssigneeName()) + "," +
                csvCell(t.getDueDate()) + "," +
                subCount + "," +
                progress + "%," +
                csvCell(t.getSubmittedAt()) + "," +
                csvCell(t.getReviewedAt())
            );
        }

        writer.flush();
    }

    /** Tên tệp hợp lệ: không chứa "/" hay "\" nên không thể trỏ ra ngoài thư mục deliverables. */
    private static final java.util.regex.Pattern SAFE_FILE_NAME = java.util.regex.Pattern.compile("^[\\p{L}\\p{N} ._()-]{1,150}$");

    /**
     * Trả về ID các task có tệp bàn giao tồn tại thật trong thư mục /uploads/deliverables của webapp.
     */
    static Set<Integer> findDownloadableDeliverables(HttpServletRequest request, List<Task> tasks) {
        Set<Integer> result = new HashSet<>();
        String dir = request.getServletContext().getRealPath("/uploads/deliverables");
        if (dir == null) {
            return result;
        }
        for (Task t : tasks) {
            String name = t.getDeliverableFile();
            if (name != null && SAFE_FILE_NAME.matcher(name.trim()).matches() && !name.contains("..")
                    && new java.io.File(dir, name.trim()).isFile()) {
                result.add(t.getId());
            }
        }
        return result;
    }

    public static String csvCell(String value) {
        if (value == null) return "\"\"";
        String clean = value.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").replace("\"", "\"\"");
        return "\"" + clean + "\"";
    }

    /**
     * Thuật toán tổng hợp Khối Lượng Công Việc (Workload Engine) cho từng thành viên:
     * 1. Đếm số Task lớn làm Lead & gom danh sách chi tiết các Task đó (leadTasks)
     * 2. Đếm số Việc Con được giao & số Việc Con đã xong [☑]
     * 3. Thu thập mảng relatedTaskIds để JavaScript lọc Kanban trong 0.01 giây
     */
    public static List<UserWorkload> computeUserWorkloads(List<User> userList, List<Task> allTasks, Map<Integer, List<SubTask>> taskSubTasksMap) {
        List<UserWorkload> workloadList = new ArrayList<>();
        // 1. Duyệt qua từng thành viên trong hệ thống
        for (User u : userList) {
            int leadTaskCount = 0;
            int subTaskCount = 0;
            int completedSubTaskCount = 0;
            int todoCount = 0;
            int inProgressCount = 0;
            int submittedCount = 0;
            int doneCount = 0;
            int overdueCount = 0;

            List<Integer> relatedTaskIds = new ArrayList<>();
            List<Task> leadTasks = new ArrayList<>();

            // 2. Quét qua tất cả các Task của dự án
            for (Task t : allTasks) {
                // a. Nếu người này là Task Lead của Task lớn
                if (t.getAssigneeId() == u.getId()) {
                    leadTaskCount++;
                    leadTasks.add(t);
                    if (!relatedTaskIds.contains(t.getId())) {
                        relatedTaskIds.add(t.getId());
                    }

                    String st = t.getStatus() != null ? t.getStatus().toUpperCase() : "TODO";
                    if ("DONE".equals(st) || "APPROVED".equals(st)) {
                        doneCount++;
                    } else if ("SUBMITTED".equals(st)) {
                        submittedCount++;
                    } else if ("IN_PROGRESS".equals(st) || "REVISE".equals(st) || "REJECTED".equals(st)) {
                        inProgressCount++;
                    } else {
                        todoCount++;
                    }

                    if (t.isOverdue()) {
                        overdueCount++;
                    }
                }
                // b. Quét các Việc Con bên trong Task lớn này từ Map RAM
                List<SubTask> subTasks = (taskSubTasksMap != null) ? taskSubTasksMap.get(t.getId()) : null;
                if (subTasks != null) {
                    for (SubTask st : subTasks) {
                        if (st.getAssigneeId() == u.getId()) {
                            subTaskCount++;
                            if (st.isCompleted()) {
                                completedSubTaskCount++;
                            }
                            if (!relatedTaskIds.contains(t.getId())) {
                                relatedTaskIds.add(t.getId());
                            }
                        }
                    }
                }
            }
            // 3. Đóng gói vào đối tượng UserWorkload
            UserWorkload uw = new UserWorkload(u, leadTaskCount, subTaskCount, completedSubTaskCount, relatedTaskIds, leadTasks);
            uw.setTodoCount(todoCount);
            uw.setInProgressCount(inProgressCount);
            uw.setSubmittedCount(submittedCount);
            uw.setDoneCount(doneCount);
            uw.setOverdueCount(overdueCount);
            workloadList.add(uw);
        }
        return workloadList;
    }
}
