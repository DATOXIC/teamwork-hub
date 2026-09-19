package com.teamwork.controllers;

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

/**
 * Controller phụ trách Bảng công việc Kanban (Tasks Module) & Cây Phân Cấp Việc Con (Sub-tasks):
 * - Hiển thị 3 cột công việc TODO, IN_PROGRESS, DONE kèm Tài liệu, Bình luận, Việc con & % Tiến độ (GET /task?action=list)
 * - Thêm công việc lớn (Task Cha) kèm đính kèm tài liệu (POST /task?action=add)
 * - Cập nhật trạng thái công việc khi kéo thả HTML5 (POST /task?action=updateStatus)
 * - Xóa công việc lớn có kiểm soát thẩm quyền Task Lead / PM (GET /task?action=delete)
 * - Thêm việc con có kiểm soát thẩm quyền Task Lead / PM (POST /task?action=addSubTask)
 * - Tick chọn hoàn thành việc con [☑] có kiểm soát thẩm quyền 3 bên (POST /task?action=toggleSubTask)
 * - Xóa việc con có kiểm soát thẩm quyền Task Lead / PM (POST /task?action=deleteSubTask)
 * - Nộp báo cáo việc con kèm ghi chú (POST /task?action=submitSubTask)
 * - Nghiệm thu việc con: Duyệt Đạt / Cân Chỉnh / Trả Về (POST /task?action=approveSubTask / reviseSubTask / rejectSubTask)
 * - Nộp bàn giao Task lớn lên cho PM (POST /task?action=submitParentTask)
 * - PM Nghiệm thu Task lớn: Duyệt Đạt / Cân Chỉnh / Trả Về (POST /task?action=pmApproveTask / pmReviseTask / pmRejectTask)
 */
@WebServlet("/task")
public class TaskServlet extends HttpServlet {

    /**
     * Ba trạng thái DUY NHẤT mà thao tác kéo-thả / đổi trạng thái trên bảng Kanban được phép đặt.
     * Các trạng thái thuộc quy trình nghiệm thu (PLANNING, SUBMITTED, REVISE, REJECTED, APPROVED)
     * chỉ được đặt bởi handler chuyên trách của chúng, không nhận từ tham số request.
     */
    private static final Set<String> BOARD_STATUS = Set.of("TODO", "IN_PROGRESS", "DONE");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Xác định ID của Project từ URL
        String projectIdParam = request.getParameter("projectId");
        int projectId = 0;

        if (projectIdParam == null || projectIdParam.trim().isEmpty()) {
            // Tối ưu hóa Cookie: Đọc dự án truy cập gần nhất để vào thẳng mà không cần query lại
            HttpSession session = request.getSession(false);
            User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                    if ("last_project_id".equals(c.getName()) && c.getValue() != null && !c.getValue().trim().isEmpty()) {
                        try {
                            int cachedProjectId = Integer.parseInt(c.getValue().trim());
                            // Kiểm tra an toàn: Dự án hợp lệ VÀ người dùng hiện tại là thành viên dự án
                            if (cachedProjectId > 0 && currentUser != null && ProjectMemberDB.isMember(cachedProjectId, currentUser.getId())) {
                                response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + cachedProjectId);
                                return;
                            } else {
                                // Nếu không có quyền hoặc dự án không hợp lệ -> Xóa cookie cũ
                                jakarta.servlet.http.Cookie deleteCookie = new jakarta.servlet.http.Cookie("last_project_id", "");
                                deleteCookie.setMaxAge(0);
                                deleteCookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
                                deleteCookie.setHttpOnly(true);
                                response.addCookie(deleteCookie);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        try 
        {
            projectId = Integer.parseInt(projectIdParam.trim());
        } 
        catch (NumberFormatException e) 
        {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // TASK-01: Bắt buộc đăng nhập — nếu chưa có Session, redirect về trang login
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=viewLogin");
            return;
        }

        if (!ProjectMemberDB.isMember(projectId, currentUser.getId())) {
            session.setAttribute("toastError", "Bạn không có quyền truy cập vào dự án này!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // 2. Xử lý hành động của người dùng (list hoặc delete)
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        switch (action) {
            case "list":
                handleShowKanban(request, response, projectId);
                break;

            case "delete":
                handleDeleteTask(request, response, projectId);
                break;

            case "exportCsv":
                handleExportCsv(request, response, projectId);
                break;

            default:
                handleShowKanban(request, response, projectId);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "add";
        }

        String projectIdParam = request.getParameter("projectId");
        if (projectIdParam != null) 
        {
            try 
            {
                int projectId = Integer.parseInt(projectIdParam.trim());
                HttpSession session = request.getSession(false);
                User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

                // TASK-01: Bắt buộc đăng nhập ngay trong doPost
                if (currentUser == null) {
                    response.sendRedirect(request.getContextPath() + "/auth?action=viewLogin");
                    return;
                }

                if (!ProjectMemberDB.isMember(projectId, currentUser.getId())) {
                    session.setAttribute("toastError", "Bạn không có quyền thao tác trong dự án này!");
                    response.sendRedirect(request.getContextPath() + "/project?action=list");
                    return;
                }
            } 
            catch (Exception e) 
            {
                //
            }
        }

        switch (action) 
        {
            case "add":
                handleAddTask(request, response);
                break;

            case "updateStatus":
                handleUpdateTaskStatus(request, response);
                break;

            case "quickAddParentTask":
                handleQuickAddParentTask(request, response);
                break;

            case "quickAddSubTask":
                handleQuickAddSubTask(request, response);
                break;

            case "addSubTask":
                handleAddSubTask(request, response);
                break;

            case "toggleSubTask":
                handleToggleSubTask(request, response);
                break;

            case "submitSubTask":
                handleSubmitSubTask(request, response);
                break;

            case "approveSubTask":
                handleApproveSubTask(request, response);
                break;

            case "reviseSubTask":
                handleReviseSubTask(request, response);
                break;

            case "rejectSubTask":
                handleRejectSubTask(request, response);
                break;

            case "submitParentTask":
                handleSubmitParentTask(request, response);
                break;

            case "submitPlanningRequest":
                handleSubmitPlanningRequest(request, response);
                break;

            case "pmApprovePlanning":
                handlePmApprovePlanning(request, response);
                break;

            case "pmRejectPlanning":
                handlePmRejectPlanning(request, response);
                break;

            case "pmApproveTask":
                handlePmApproveTask(request, response);
                break;

            case "pmReviseTask":
                handlePmReviseTask(request, response);
                break;

            case "pmRejectTask":
                handlePmRejectTask(request, response);
                break;

            case "deleteSubTask":
                handleDeleteSubTask(request, response);
                break;

            case "editTask":
                handleEditTask(request, response);
                break;

            case "editSubTask":
                handleEditSubTask(request, response);
                break;

            default:
                response.sendRedirect(request.getContextPath() + "/project?action=list");
                break;
        }
    }

    /**
     * Nghiệp vụ 1: Lấy toàn bộ dữ liệu 3 cột Kanban, danh sách tài liệu dự án,
     * map liên kết TaskDoc, map bình luận TaskComments, map Việc Con SubTasks và % Tiến độ
     */
    private void handleShowKanban(HttpServletRequest request, HttpServletResponse response, int projectId)
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

        // 3. Lấy thành viên dự án và nạp thông tin user từ danh sách hệ thống (Tối ưu Session Cache)
        List<ProjectMember> projectMemberList = ProjectMemberDB.selectByProjectId(projectId);
        HttpSession session = request.getSession(false);
        @SuppressWarnings("unchecked")
        List<User> allSystemUsers = (session != null) ? (List<User>) session.getAttribute("cached_system_users") : null;
        if (allSystemUsers == null) {
            allSystemUsers = UserDB.selectAll();
            if (session != null) {
                session.setAttribute("cached_system_users", allSystemUsers);
            }
        }
        Map<Integer, User> systemUserMap = new HashMap<>();
        for (User u : allSystemUsers) {
            systemUserMap.put(u.getId(), u);
        }

        List<User> userList = new ArrayList<>();
        for (ProjectMember pm : projectMemberList) {
            User u = systemUserMap.get(pm.getUserId());
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

        // 8.5. Tính toán khối lượng công việc của từng thành viên (UserWorkload DTO) cho Dải Avatar B.3
        List<Task> allTasks = new ArrayList<>(allProjectTasks);
        List<UserWorkload> userWorkloadList = computeUserWorkloads(userList, allTasks, taskSubTasksMap);

        // 8.6. Lấy danh sách lời mời của dự án (Chặng C.3)
        List<ProjectInvite> projectInviteList = ProjectInviteDB.selectByProjectId(projectId);
        int memberCount = ProjectMemberDB.countMembers(projectId);

        // Danh sách ứng viên trong hệ thống chưa tham gia dự án (Tối ưu hóa trong RAM)
        Set<Integer> memberUserIds = new HashSet<>();
        for (ProjectMember pm : projectMemberList) {
            memberUserIds.add(pm.getUserId());
        }

        List<User> inviteCandidates = new ArrayList<>();
        for (User u : allSystemUsers) {
            if (!memberUserIds.contains(u.getId())) {
                inviteCandidates.add(u);
            }
        }

        // 8.7. Xử lý Flash Message (Toast)
        if (session != null) 
        {
            String toastSuccess = (String) session.getAttribute("toastSuccess");
            if (toastSuccess != null) 
            {
                request.setAttribute("toastSuccess", toastSuccess);
                session.removeAttribute("toastSuccess");
            }
            String toastError = (String) session.getAttribute("toastError");
            if (toastError != null) 
            {
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
        request.setAttribute("project", project);
        request.setAttribute("todoTasks", todoTasks);
        request.setAttribute("inProgressTasks", inProgressTasks);
        request.setAttribute("doneTasks", doneTasks);
        request.setAttribute("allProjectTasks", allProjectTasks);
        request.setAttribute("userList", userList);
        request.setAttribute("docList", docList);
        request.setAttribute("taskDocsMap", taskDocsMap);
        request.setAttribute("taskCommentsMap", taskCommentsMap);
        request.setAttribute("taskSubTasksMap", taskSubTasksMap);
        request.setAttribute("taskProgressMap", taskProgressMap);
        request.setAttribute("userWorkloadList", userWorkloadList);
        request.setAttribute("submittedCount", submittedCount);
        request.setAttribute("projectMemberList", projectMemberList);
        request.setAttribute("projectInviteList", projectInviteList);
        request.setAttribute("memberCount", memberCount);
        request.setAttribute("inviteCandidates", inviteCandidates);
        request.setAttribute("userProjects", userProjects);
        request.setAttribute("unreadNotifCount", unreadNotifCount);
        request.setAttribute("userNotifications", userNotifications);
        request.setAttribute("projectChatMessages", projectChatMessages);
        List<ActivityLog> activityLogs = ActivityLogDB.selectByProjectId(projectId, 60);
        request.setAttribute("activityLogs", activityLogs);
        request.setAttribute("currentView", currentView);
        request.setAttribute("taskView", taskView);
        request.setAttribute("subtaskMode", subtaskMode);
        request.setAttribute("activeNav", "projects");

        // 10. Forward sang giao diện tasks.jsp
        request.getRequestDispatcher("/tasks.jsp").forward(request, response);
    }

    /**
     * Nghiệp vụ 2: Xóa một task lớn khỏi dự án theo taskId (BẢO VỆ PHÂN QUYỀN TASK LEAD / PM)
     */
    private void handleDeleteTask(HttpServletRequest request, HttpServletResponse response, int projectId)
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
                        session.setAttribute("toastError", "🔒 Công việc [" + task.getTitle() + "] đã hoàn thành (DONE) và được khóa vĩnh viễn, không thể xóa!");
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
    private void handleExportCsv(HttpServletRequest request, HttpServletResponse response, int projectId)
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
        writer.println("Mã công việc,Tiêu đề công việc,Mô tả tóm tắt,Trạng thái,Mức ưu tiên,Người phụ trách,Hạn chót,Số việc con,Tiến độ hoàn thành (%),Ngày nộp bàn giao,Ngày duyệt");

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

    private static String csvCell(String value) {
        if (value == null) return "\"\"";
        String clean = value.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").replace("\"", "\"\"");
        return "\"" + clean + "\"";
    }

    /**
     * Nghiệp vụ 3: Tiếp nhận form thêm task mới và lưu liên kết tài liệu đính kèm vào TaskDocDB
     */
    private void handleAddTask(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        if (projectId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String priority = request.getParameter("priority");
        String dueDate = request.getParameter("dueDate");
        String[] selectedDocIds = request.getParameterValues("docIds");

        HttpSession session = request.getSession(false);

        if (title == null || title.trim().isEmpty()) {
            if (session != null) {
                session.setAttribute("toastError", "Tiêu đề công việc không được để trống!");
            }
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        if (!"LOW".equalsIgnoreCase(priority) && !"HIGH".equalsIgnoreCase(priority)) 
        {
            priority = "MEDIUM";
        } 
        else 
        {
            priority = priority.toUpperCase();
        }

        int assigneeId = safeParseInt(request.getParameter("assigneeId"), 0);
        String assigneeName = "";
        boolean assigneeProvided = assigneeId > 0; // Ghi nhớ người dùng có chọn ai chưa

        if (assigneeId > 0 && ProjectMemberDB.isMember(projectId, assigneeId)) 
        {
            User assignee = UserDB.selectById(assigneeId);
            if (assignee != null) 
            {
                assigneeName = assignee.getFullName();
            } 
            else 
            {
                assigneeId = 0;
            }
        } else {
            assigneeId = 0;
        }

        // TASK-02: Phân biệt rõ hai trường hợp lỗi để thông báo chính xác hơn
        if (assigneeId <= 0) {
            if (session != null) {
                if (assigneeProvided) {
                    // Người dùng đã chọn nhưng người đó không còn là thành viên dự án
                    session.setAttribute("toastError", "Người phụ trách được chọn không còn là thành viên của dự án này! Vui lòng chọn lại.");
                } else {
                    // Người dùng chưa chọn ai
                    session.setAttribute("toastError", "Vui lòng chọn người phụ trách (Task Lead) trong danh sách thành viên dự án!");
                }
            }
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Mọi công việc mới tạo bắt buộc bắt đầu từ TO DO theo chuẩn quy trình ClickUp/Agile
        String status = "TODO";

        Task newTask = new Task(
            0,
            projectId,
            title.trim(),
            (description != null ? description.trim() : ""),
            status,
            priority,
            (dueDate != null ? dueDate.trim() : ""),
            assigneeId,
            assigneeName
        );

        String labels = request.getParameter("labels");
        newTask.setLabels(labels != null ? labels.trim() : "");

        Project curPrj = ProjectDB.selectById(projectId);
        if (curPrj != null && curPrj.isSoloProject()) {
            User curUser = getCurrentUser(request);
            if (curUser != null) {
                newTask.setAssigneeId(curUser.getId());
                newTask.setAssigneeName(curUser.getFullName());
            }
            newTask.setRequiresGate(false);
        } else {
            boolean defaultRequiresGate = (curPrj != null && curPrj.isTeamProject());
            String reqGateParam = request.getParameter("requiresGate");
            if (reqGateParam != null) {
                newTask.setRequiresGate("true".equalsIgnoreCase(reqGateParam.trim()) || "on".equalsIgnoreCase(reqGateParam.trim()) || "1".equals(reqGateParam.trim()));
            } else {
                // Nếu form có cờ hasRequiresGateControl nhưng checkbox không được tick
                String hasControl = request.getParameter("hasRequiresGateControl");
                if (hasControl != null && !hasControl.trim().isEmpty()) {
                    newTask.setRequiresGate(false);
                } else {
                    newTask.setRequiresGate(defaultRequiresGate);
                }
            }
        }

        int newTaskId = TaskDB.insert(newTask);

        // Ghi nhận Activity Log
        User curUser = getCurrentUser(request);
        int curUserId = curUser != null ? curUser.getId() : 0;
        ActivityLogDB.logAsync(projectId, curUserId, "TASK_CREATE", "TASK", newTaskId, newTask.getTitle(), "Tạo công việc mới: " + newTask.getTitle());

        if (selectedDocIds != null && selectedDocIds.length > 0) {
            for (String docIdStr : selectedDocIds) {
                int docId = safeParseInt(docIdStr, 0);
                if (docId > 0) {
                    Doc doc = DocDB.selectById(docId);
                    if (doc != null) {
                        TaskDocDB.insert(newTaskId, docId, doc.getTitle());
                    }
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 4: Cập nhật trạng thái Task khi người dùng kéo thả
     */
    private void handleUpdateTaskStatus(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String newStatus = request.getParameter("newStatus");
        if (newStatus == null || newStatus.trim().isEmpty()) {
            newStatus = request.getParameter("status");
        }
        boolean isAjax = isAjaxRequest(request);

        if (taskId > 0 && projectId > 0 && newStatus != null && !newStatus.trim().isEmpty()) {
            Task task = TaskDB.selectById(taskId);
            if (task != null && task.getProjectId() == projectId) {
                String status = newStatus.trim().toUpperCase();
                HttpSession session = request.getSession(false);

                // CHỐT 0 — LỌC TRẠNG THÁI ĐẦU VÀO:
                // Thiếu chốt này, request gửi "newStatus=APPROVED" sẽ đi xuyên qua Cổng 2 bên dưới
                // (vì Cổng 2 chỉ so sánh với chuỗi "DONE"), trong khi giao diện lại xếp APPROVED
                // vào cột Đã xong với nhãn "Đã nghiệm thu" — tức hoàn tất task mà không qua PM.
                if (!BOARD_STATUS.contains(status)) {
                    String errMsg = "Trạng thái [" + status + "] không hợp lệ cho thao tác trên bảng công việc!";
                    if (isAjax) {
                        sendJsonResponse(response, false, errMsg, null);
                        return;
                    }
                    if (session != null) session.setAttribute("toastError", errMsg);
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                Project currentPrj = ProjectDB.selectById(projectId);

                // CHỐT 1 — THẨM QUYỀN TRÊN TỪNG CÔNG VIỆC:
                // Trước đây hàm này KHÔNG kiểm tra quyền (handler duy nhất trong TaskServlet bị thiếu),
                // nên mọi thành viên dự án đều kéo-thả / đổi được trạng thái task của người khác.
                // doPost() chỉ chặn tới mức "có phải thành viên dự án không", chưa xét quyền trên task.
                // Quy ước lấy theo handleEditTask và handleDeleteTask: PM hoặc Task Lead của chính task đó.
                User currentUser = getCurrentUser(request);
                boolean isPm = isProjectOwner(currentUser, currentPrj);
                boolean isLead = isTaskLead(currentUser, task);

                if (!isPm && !isLead) {
                    String errMsg = "Bạn không có quyền đổi trạng thái công việc [" + task.getTitle()
                            + "]! Chỉ Người phụ trách (Task Lead) hoặc Trưởng Dự Án (PM) mới được phép.";
                    if (isAjax) {
                        sendJsonResponse(response, false, errMsg, null);
                        return;
                    }
                    if (session != null) session.setAttribute("toastError", errMsg);
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                // RÀNG BUỘC KHÓA BẤT BIẾN: TASK ĐÃ HOÀN TẤT THÌ KHÔNG ĐỔI TRẠNG THÁI ĐƯỢC NỮA.
                // Xét cả "APPROVED" vì handleShowKanban coi nó tương đương DONE, và dữ liệu cũ
                // có thể đã mang giá trị này từ trước khi có CHỐT 0.
                if ("DONE".equalsIgnoreCase(task.getStatus()) || "APPROVED".equalsIgnoreCase(task.getStatus())) {
                    String errMsg = "🔒 Công việc [" + task.getTitle() + "] đã hoàn thành và được khóa vĩnh viễn, không thể thay đổi trạng thái!";
                    if (isAjax) {
                        sendJsonResponse(response, false, errMsg, null);
                        return;
                    }
                    if (session != null) session.setAttribute("toastError", errMsg);
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                boolean isGateEnforced = (currentPrj != null && currentPrj.isTeamProject() && task.isRequiresGate());

                // =========================================================================
                // RÀNG BUỘC GIAI ĐOẠN 1: CHUYỂN TỪ TODO SANG IN_PROGRESS (ĐANG LÀM)
                // =========================================================================
                if ("IN_PROGRESS".equalsIgnoreCase(status) && "TODO".equalsIgnoreCase(task.getStatus())) {
                    String taskTitle = task.getTitle();

                    // Ràng buộc Quality Gate: Chặn kéo thả trực tiếp từ TODO sang IN_PROGRESS.
                    // Bắt buộc phải nộp Kế hoạch WBS và được PM phê duyệt (Gate 1)
                    if (isGateEnforced) {
                        String errMsg = "🛡️ [Quality Gate 1] Công việc này áp dụng Cổng Kế Hoạch! Vui lòng mở chi tiết công việc, phân rã việc con (WBS) và bấm 'Gửi duyệt kế hoạch' để PM phê duyệt trước khi bắt đầu.";
                        if (isAjax) {
                            sendJsonResponse(response, false, errMsg, null);
                            return;
                        }
                        if (session != null) session.setAttribute("toastError", errMsg);
                        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                        return;
                    }

                    // Đối với Fast-track: Bắt buộc phải có Người phụ trách (Task Lead)
                    if (task.getAssigneeId() <= 0) {
                        User curUser = getCurrentUser(request);
                        if (curUser != null && currentPrj != null && currentPrj.isSoloProject()) {
                            // Solo mode: Tự động gán cho chính mình
                            task.setAssigneeId(curUser.getId());
                            task.setAssigneeName(curUser.getFullName());
                            TaskDB.update(task);
                        } else {
                            String errMsg = "⚠️ Không thể bắt đầu Task [" + taskTitle + "]! Công việc chưa được phân công. Vui lòng bấm 'Chỉnh sửa' để chọn Người phụ trách trước.";
                            if (isAjax) {
                                sendJsonResponse(response, false, errMsg, null);
                                return;
                            }
                            if (session != null) session.setAttribute("toastError", errMsg);
                            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                            return;
                        }
                    }
                }

                // =========================================================================
                // RÀNG BUỘC GIAI ĐOẠN 2: CHUYỂN SANG DONE (HOÀN THÀNH)
                // =========================================================================
                if ("DONE".equalsIgnoreCase(status)) {
                    if (isGateEnforced) {
                        // CỔNG 2 — Task bật Quality Gate CHỈ hoàn tất được qua đúng một đường:
                        // Task Lead nộp bàn giao (handleSubmitParentTask) -> PM phê duyệt (handlePmApproveTask).
                        //
                        // KHÔNG xét việc con nữa. Điều kiện cũ là:
                        //     (subTasks != null && !subTasks.isEmpty() && progress < 100)
                        // nên MỌI task chưa có việc con đều đi lọt thẳng sang DONE — bỏ qua cả hai cổng.
                        // Câu hỏi đúng ở đây là "PM đã phê duyệt chưa?" (thẩm quyền),
                        // không phải "việc con xong chưa?" (tiến độ) — tiến độ vô nghĩa khi chưa có việc con.
                        String errMsg = "🛡️ [Quality Gate 2] Task [" + task.getTitle() + "] bắt buộc qua nghiệm thu! "
                                + "Task Lead hãy mở chi tiết công việc và bấm 'Nộp bàn giao' để PM phê duyệt.";
                        if (isAjax) {
                            sendJsonResponse(response, false, errMsg, null);
                            return;
                        }
                        if (session != null) session.setAttribute("toastError", errMsg);
                        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                        return;
                    }
                }

                TaskDB.updateStatus(taskId, status);

                // Ghi nhận Activity Log
                User curUser = getCurrentUser(request);
                int curUserId = curUser != null ? curUser.getId() : 0;
                String actionDesc = "Chuyển trạng thái sang: " + status;
                if ("DONE".equalsIgnoreCase(status)) actionDesc = "Đã hoàn tất toàn bộ công việc";
                else if ("IN_PROGRESS".equalsIgnoreCase(status)) actionDesc = "Bắt đầu thực hiện công việc";
                else if ("TODO".equalsIgnoreCase(status)) actionDesc = "Chuyển về danh mục Cần làm";
                ActivityLogDB.logAsync(projectId, curUserId, "STATUS_CHANGE", "TASK", taskId, task.getTitle(), actionDesc);

                String toastSuccessMsg = "DONE".equalsIgnoreCase(status) ? "🎉 Chúc mừng! Thẻ công việc đã được hoàn tất thành công."
                        : ("IN_PROGRESS".equalsIgnoreCase(status) ? "🚀 Bắt đầu thực hiện công việc [" + task.getTitle() + "] thành công!"
                        : "Đã chuyển công việc về danh mục Cần làm.");

                if (isAjax) {
                    sendJsonResponse(response, true, toastSuccessMsg, "{\"taskId\":" + taskId + ",\"newStatus\":\"" + escapeJson(status) + "\"}");
                    return;
                }

                if (session != null) {
                    session.setAttribute("toastSuccess", toastSuccessMsg);
                }
            } else if (isAjax) {
                sendJsonResponse(response, false, "Không tìm thấy công việc tương ứng trong dự án!", null);
                return;
            }
        } else if (isAjax) {
            sendJsonResponse(response, false, "Dữ liệu yêu cầu không hợp lệ!", null);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 5: Thêm Việc Con (Sub-task) mới và phân công cho thành viên (BẢO VỆ PHÂN QUYỀN TASK LEAD / PM)
     */
    private void handleAddSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String title = request.getParameter("title");
        String dueDateParam = request.getParameter("dueDate");
        String subDueDate = (dueDateParam != null) ? dueDateParam.trim() : "";

        if (projectId <= 0 || taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task parentTask = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        
        if (parentTask == null || project == null || parentTask.getProjectId() != projectId) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        int assigneeId = safeParseInt(request.getParameter("assigneeId"), 0);
        String assigneeName = "Chưa phân công";
        if (project.isSoloProject()) {
            assigneeId = currentUser != null ? currentUser.getId() : 0;
            assigneeName = currentUser != null ? currentUser.getFullName() : "Chưa phân công";
        } else if (assigneeId > 0 && ProjectMemberDB.isMember(projectId, assigneeId)) {
            User u = UserDB.selectById(assigneeId);
            if (u != null) {
                assigneeName = u.getFullName();
            } else {
                assigneeId = 0;
            }
        } else {
            assigneeId = 0;
        }

        HttpSession session = request.getSession(false);

        // KIỂM SOÁT THẨM QUYỀN & KHÓA PHẠM VI (SCOPE LOCK):
        // 1. Chỉ Task Lead của chính Task này HOẶC Trưởng Dự Án mới được thêm việc con.
        // 2. Chỉ được thêm việc con khi Task đang ở trạng thái TODO (Giai đoạn Lập Kế Hoạch). Khi đã trình PM hoặc đã khóa thì không được thêm tự do.
        if (isTaskLead(currentUser, parentTask) || isProjectOwner(currentUser, project)) {
            // Cho phép thêm việc con khi Task đang ở TODO hoặc IN_PROGRESS (phục vụ phát sinh việc con)
            // Khóa lại khi Task đã nộp nghiệm thu (SUBMITTED, REVISE, REJECTED, DONE)
            if (!"TODO".equalsIgnoreCase(parentTask.getStatus()) && !"IN_PROGRESS".equalsIgnoreCase(parentTask.getStatus())) {
                if (session != null) {
                    session.setAttribute("toastError", 
                        "⚠️ Công việc này đã nộp hoặc hoàn tất nghiệm thu, không thể thêm việc con mới!");
                }
                response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                return;
            }

            // RÀNG BUỘC BẤT BIẾN CÂY PHÂN CẤP (HIERARCHICAL DEADLINE INVARIANT):
            // Hạn chót của việc con không được vượt quá Hạn chót của Task cha
            if (!subDueDate.isEmpty() && parentTask.getDueDate() != null && !parentTask.getDueDate().trim().isEmpty()) {
                try {
                    LocalDate subDate = LocalDate.parse(subDueDate);
                    LocalDate parentDate = LocalDate.parse(parentTask.getDueDate().trim());
                    if (subDate.isAfter(parentDate)) {
                        if (session != null) {
                            session.setAttribute("toastError", 
                                "Việc con phải được hoàn thành trước hạn chót của công việc lớn (" + parentTask.getDueDate().trim() + ")!");
                        }
                        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                        return;
                    }
                } catch (Exception ignored) {
                    // Nếu lỗi định dạng ngày, tiếp tục lưu dạng chuỗi an toàn
                }
            }

            if (title != null && !title.trim().isEmpty()) {
                SubTask newSubTask = new SubTask(
                    0, 
                    taskId, 
                    title.trim(), 
                    assigneeId, 
                    assigneeName, 
                    "TODO", 
                    subDueDate, 
                    "", 
                    "", 
                    "", 
                    ""
                );
                SubTaskDB.insert(newSubTask);
                if (session != null) session.setAttribute("toastSuccess", "Đã thêm việc con vào kế hoạch phân rã thành công!");
            }
        } else {
            if (session != null) session.setAttribute("toastError", "Bạn không có quyền phân rã việc con cho Task này!");
        }

        // Áp dụng PRG: Redirect về lại bảng Kanban
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 6: Đổi trạng thái hoàn thành [☑] của Việc Con (SubTask) (BẢO VỆ PHÂN QUYỀN 3 BÊN)
     * Kiểm tra các sub task của một task lớn đã hoàn thành xong hết chưa, nếu xong hết rồi thì chuyển cái Task lớn sang trạng thái Done đúng không.
     */
    private void handleToggleSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int subTaskId = safeParseInt(request.getParameter("subTaskId"), 0);
        boolean isCompleted = Boolean.parseBoolean(request.getParameter("completed"));
        boolean isAjax = isAjaxRequest(request);

        if (projectId <= 0 || subTaskId <= 0) {
            if (isAjax) {
                sendJsonResponse(response, false, "Dữ liệu yêu cầu không hợp lệ!", null);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }
        
        // Xác định User và SubTask
        User currentUser = getCurrentUser(request);
        SubTask st = SubTaskDB.selectById(subTaskId);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && "DONE".equalsIgnoreCase(parentTask.getStatus())) {
                String errMsg = "🔒 Công việc [" + parentTask.getTitle() + "] đã hoàn thành (DONE) và được khóa, không thể thay đổi việc con!";
                if (isAjax) {
                    sendJsonResponse(response, false, errMsg, null);
                    return;
                }
                HttpSession session = request.getSession(false);
                if (session != null) session.setAttribute("toastError", errMsg);
                response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                return;
            }

            if (parentTask != null && project != null && parentTask.getProjectId() == projectId && canManageSubTask(currentUser, st, parentTask, project)) {
                // Xác định chế độ TRƯỚC khi ghi — cờ này quyết định ô tick mang ý nghĩa gì.
                boolean isGateEnforced = (project.isTeamProject() && parentTask.isRequiresGate());

                // 1. Ghi trạng thái theo đúng chế độ của dự án:
                //    - Quality Gate (nhóm) : tick = NỘP BÀI  -> "SUBMITTED", chờ Task Lead nghiệm thu.
                //    - Fast-track / Solo   : tick = XONG HẲN -> "DONE", không ai phải duyệt.
                //    Người làm KHÔNG bao giờ tự ghi được "APPROVED" — đó là đặc quyền của Task Lead.
                String targetStatus = isCompleted ? (isGateEnforced ? "SUBMITTED" : "DONE") : "TODO";
                SubTaskDB.updateStatus(subTaskId, targetStatus);

                int newProgress = SubTaskDB.calculateProgress(st.getTaskId());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                // 2. CƠ CHẾ TỰ ĐỘNG CHUYỂN CỘT KANBAN CHO TASK LỚN:
                if (!isGateEnforced) {
                    // Chế độ Fast-track (Tự do / Solo): Hoàn tất 100% subtask thì tự động hoàn thành Task
                    if (newProgress == 100 && !"DONE".equals(parentTask.getStatus())) 
                    {
                        TaskDB.updateStatus(parentTask.getId(), "DONE");
                        String celebrationText = "🏆 CHÚC MỪNG: Tất cả việc con đã hoàn tất (100%)! Thẻ công việc [" + parentTask.getTitle() + "] đã tự động chuyển sang trạng thái ĐÃ XONG!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", celebrationText, now));
                    } 
                    else if (newProgress > 0 && newProgress < 100 && "TODO".equals(parentTask.getStatus())) 
                    {
                        TaskDB.updateStatus(parentTask.getId(), "IN_PROGRESS");
                        String progressText = "🚀 BẮT ĐẦU THỰC HIỆN: Đã hoàn thành " + newProgress + "% việc con. Task [" + parentTask.getTitle() + "] đã tự động chuyển sang ĐANG LÀM!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", progressText, now));
                    } 
                    else if (newProgress < 100 && "DONE".equals(parentTask.getStatus())) 
                    {
                        TaskDB.updateStatus(parentTask.getId(), "IN_PROGRESS");
                        String reopenText = "⚠️ CẬP NHẬT: Còn việc con chưa xong (" + newProgress + "%). Task [" + parentTask.getTitle() + "] đã được mở lại sang ĐANG LÀM!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", reopenText, now));
                    }
                } else {
                    // Chế độ Quality Gate (Nhóm): việc con vừa chuyển sang SUBMITTED nên CHƯA tính vào tiến độ.
                    // Tiến độ chỉ tăng khi Task Lead duyệt (APPROVED) — xem handleApproveSubTask.
                    // Vì vậy ở đây không dựa vào newProgress, chỉ ghi nhận "đã bắt đầu thực hiện".
                    if (isCompleted && "TODO".equals(parentTask.getStatus())) {
                        TaskDB.updateStatus(parentTask.getId(), "IN_PROGRESS");
                    }
                }

                // 3. Ghi nhận lên Luồng Thảo Luận (nội dung khác nhau theo chế độ)
                if (isCompleted) {
                    String notificationText = isGateEnforced
                        ? "📤 " + st.getAssigneeName() + " vừa nộp việc con: [" + st.getTitle() + "] — Đang chờ Task Lead nghiệm thu."
                        : "🎉 " + st.getAssigneeName() + " vừa hoàn thành việc con: [" + st.getTitle() + "] — Đóng góp đưa tiến độ Task lên " + newProgress + "%!";
                    Message systemMessage = new Message(0, projectId, st.getTaskId(), 0, "Hệ Thống", notificationText, now);
                    MessageDB.insert(systemMessage);

                    // 4. Bắn thông báo 🔔 cho Task Lead khi Assignee đánh dấu hoàn thành (chế độ Quality Gate)
                    // Để Task Lead biết cần vào duyệt nghiệm thu, tránh việc con "trôi" mà không ai kiểm duyệt.
                    if (isGateEnforced && parentTask.getAssigneeId() > 0 && parentTask.getAssigneeId() != currentUser.getId()) {
                        NotificationDB.send(
                            parentTask.getAssigneeId(),
                            "📋 Cần duyệt việc con",
                            st.getAssigneeName() + " vừa nộp việc con [" + st.getTitle() + "]. Hãy vào kiểm tra và duyệt nghiệm thu!",
                            "/task?action=list&projectId=" + projectId,
                            "bi-clipboard-check text-warning"
                        );
                    }
                }

                if (isAjax) {
                    String msg = isCompleted
                            ? (isGateEnforced ? "Đã nộp việc con — chờ Task Lead duyệt nghiệm thu!" : "Đã đánh dấu hoàn thành việc con!")
                            : "Đã chuyển việc con về cần làm.";
                    sendJsonResponse(response, true, msg, "{\"subTaskId\":" + subTaskId + ",\"parentTaskId\":" + st.getTaskId() + ",\"isCompleted\":" + isCompleted + ",\"newProgress\":" + newProgress + ",\"parentStatus\":\"" + escapeJson(parentTask.getStatus()) + "\"}");
                    return;
                }
            } else if (isAjax) {
                sendJsonResponse(response, false, "Bạn không có quyền thao tác trên việc con này!", null);
                return;
            }
        } else if (isAjax) {
            sendJsonResponse(response, false, "Không tìm thấy việc con tương ứng!", null);
            return;
        }

        // Áp dụng PRG: Redirect về lại bảng Kanban
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 7: Xóa một việc con (BẢO VỆ PHÂN QUYỀN TASK LEAD / PM & KHÓA PHẠM VI)
     */
    private void handleDeleteSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int subTaskId = safeParseInt(request.getParameter("subTaskId"), 0);

        if (projectId <= 0 || subTaskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        SubTask st = SubTaskDB.selectById(subTaskId);
        HttpSession session = request.getSession(false);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null && parentTask.getProjectId() == projectId) {
                // RÀNG BUỘC KHÓA PHẠM VI: Không được xóa khi đã trình PM hoặc đã khóa
                if (!"TODO".equalsIgnoreCase(parentTask.getStatus())) {
                    if (session != null) {
                        session.setAttribute("toastError", 
                            "⚠️ Kế hoạch phân rã đã được trình PM hoặc đã khóa (Scope Lock). Không thể xóa việc con!");
                    }
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                // KIỂM SOÁT THẨM QUYỀN: Chỉ Task Lead của chính Task này HOẶC Trưởng Dự Án mới được xóa việc con
                if (isTaskLead(currentUser, parentTask) || isProjectOwner(currentUser, project)) {
                    SubTaskDB.delete(subTaskId);
                    if (session != null) session.setAttribute("toastSuccess", "Đã xóa việc con khỏi kế hoạch phân rã!");
                } else {
                    if (session != null) session.setAttribute("toastError", "Bạn không có quyền xóa việc con này!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 7.1: Chỉnh sửa thông tin công việc lớn (Task cha)
     * Thẩm quyền: Trưởng Dự Án (PM) hoặc Trưởng Nhóm Task (Task Lead)
     */
    private void handleEditTask(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String priority = request.getParameter("priority");
        String dueDate = request.getParameter("dueDate");
        String assigneeIdParam = request.getParameter("assigneeId");

        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task == null || project == null || task.getProjectId() != projectId) {
            if (session != null) session.setAttribute("toastError", "Không tìm thấy thông tin công việc!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // RÀNG BUỘC KHÓA BẤT BIẾN: KHÔNG THỂ CHỈNH SỬA THÔNG TIN TASK ĐÃ DONE
        if ("DONE".equalsIgnoreCase(task.getStatus())) {
            if (session != null) session.setAttribute("toastError", "🔒 Công việc [" + task.getTitle() + "] đã hoàn thành (DONE) và được khóa vĩnh viễn, không thể chỉnh sửa!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Kiểm tra thẩm quyền: PM hoặc Task Lead của task này
        boolean isPm = isProjectOwner(currentUser, project);
        boolean isLead = isTaskLead(currentUser, task);

        if (!isPm && !isLead) {
            if (session != null) session.setAttribute("toastError", "Bạn không có quyền chỉnh sửa thông tin công việc này!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Validate tiêu đề
        if (title == null || title.trim().isEmpty()) {
            if (session != null) session.setAttribute("toastError", "Tiêu đề công việc không được để trống!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Cập nhật các trường cơ bản
        task.setTitle(title.trim());
        task.setDescription(description != null ? description.trim() : "");
        if (priority != null && ("LOW".equalsIgnoreCase(priority) || "MEDIUM".equalsIgnoreCase(priority) || "HIGH".equalsIgnoreCase(priority))) {
            task.setPriority(priority.toUpperCase());
        }
        task.setDueDate(dueDate != null ? dueDate.trim() : "");

        if (project.isSoloProject()) {
            task.setAssigneeId(currentUser.getId());
            task.setAssigneeName(currentUser.getFullName());
            task.setRequiresGate(false);
        } else {
            // Chỉ PM mới được đổi người phụ trách (assigneeId)
            if (isPm && assigneeIdParam != null && !assigneeIdParam.trim().isEmpty()) {
                int newAssigneeId = safeParseInt(assigneeIdParam, 0);
                if (newAssigneeId > 0 && newAssigneeId != task.getAssigneeId() && ProjectMemberDB.isMember(projectId, newAssigneeId)) {
                    User newAssignee = UserDB.selectById(newAssigneeId);
                    if (newAssignee != null) {
                        task.setAssigneeId(newAssigneeId);
                        task.setAssigneeName(newAssignee.getFullName());

                        // Gửi thông báo cho người mới được giao task
                        NotificationDB.send(
                            newAssigneeId,
                            "Phân Công Nhiệm Vụ Mới",
                            "Bạn vừa được Trưởng Dự Án phân công làm Task Lead cho công việc [" + task.getTitle() + "].",
                            "/task?action=list&projectId=" + projectId,
                            "TASK"
                        );
                    }
                }
            }

            // Cập nhật chế độ Quality Gate nếu form có gửi cờ điều khiển
            String hasControl = request.getParameter("hasRequiresGateControl");
            if (hasControl != null && !hasControl.trim().isEmpty()) {
                String reqGateParam = request.getParameter("requiresGate");
                task.setRequiresGate("true".equalsIgnoreCase(reqGateParam) || "on".equalsIgnoreCase(reqGateParam) || "1".equals(reqGateParam));
            }
        }

        TaskDB.update(task);

        if (session != null) {
            session.setAttribute("toastSuccess", "Đã cập nhật thông tin công việc [" + task.getTitle() + "] thành công!");
        }
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 7.2: Chỉnh sửa thông tin việc con (Sub-task)
     * Thẩm quyền: Trưởng Dự Án (PM) hoặc Trưởng Nhóm Task (Task Lead)
     */
    private void handleEditSubTask(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int subTaskId = safeParseInt(request.getParameter("subTaskId"), 0);
        String title = request.getParameter("title");
        String assigneeIdParam = request.getParameter("assigneeId");
        String dueDate = request.getParameter("dueDate");

        SubTask subTask = SubTaskDB.selectById(subTaskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (subTask == null || project == null) {
            if (session != null) session.setAttribute("toastError", "Không tìm thấy việc con cần sửa!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        Task parentTask = TaskDB.selectById(subTask.getTaskId());
        if (parentTask == null || parentTask.getProjectId() != projectId) {
            if (session != null) session.setAttribute("toastError", "Không tìm thấy công việc cha thuộc dự án!");
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        // RÀNG BUỘC KHÓA BẤT BIẾN: KHÔNG THỂ SỬA VIỆC CON KHI TASK CHA ĐÃ DONE
        if ("DONE".equalsIgnoreCase(parentTask.getStatus())) {
            if (session != null) session.setAttribute("toastError", "🔒 Công việc [" + parentTask.getTitle() + "] đã hoàn thành (DONE) và được khóa, không thể sửa việc con!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Kiểm tra thẩm quyền: PM hoặc Task Lead của parentTask
        if (!isProjectOwner(currentUser, project) && !isTaskLead(currentUser, parentTask)) {
            if (session != null) session.setAttribute("toastError", "Chỉ Trưởng Dự Án hoặc Task Lead mới có quyền sửa việc con!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        // Validate tiêu đề
        if (title == null || title.trim().isEmpty()) {
            if (session != null) session.setAttribute("toastError", "Tiêu đề việc con không được để trống!");
            response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
            return;
        }

        subTask.setTitle(title.trim());
        subTask.setDueDate(dueDate != null ? dueDate.trim() : "");

        // Cập nhật người phụ trách việc con
        if (assigneeIdParam != null && !assigneeIdParam.trim().isEmpty()) {
            int newAssigneeId = safeParseInt(assigneeIdParam, 0);
            if (newAssigneeId > 0 && ProjectMemberDB.isMember(projectId, newAssigneeId)) {
                User newAssignee = UserDB.selectById(newAssigneeId);
                if (newAssignee != null) {
                    subTask.setAssigneeId(newAssigneeId);
                    subTask.setAssigneeName(newAssignee.getFullName());
                }
            }
        }

        SubTaskDB.update(subTask);

        if (session != null) {
            session.setAttribute("toastSuccess", "Đã cập nhật thông tin việc con [" + subTask.getTitle() + "] thành công!");
        }
        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 8: Thành viên Nộp Báo Cáo / Kết Quả Việc Con (Chuyển sang 🟡 SUBMITTED)
     * KÈM BẮN THÔNG BÁO CHO TASK LEAD
     */
    private void handleSubmitSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int subTaskId = safeParseInt(request.getParameter("subTaskId"), 0);
        String submissionNote = request.getParameter("submissionNote");

        if (projectId <= 0 || subTaskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        SubTask st = SubTaskDB.selectById(subTaskId);
        HttpSession session = request.getSession(false);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null && parentTask.getProjectId() == projectId) {
                // KIỂM SOÁT THẨM QUYỀN NGHIỆM THU TẦNG 1:
                // 1. Nếu việc con đã gán cho ai (assigneeId > 0): CHỈ chính thành viên đó mới được nộp kết quả.
                // 2. Nếu việc con chưa gán cho ai (assigneeId == 0): Task Lead hoặc PM có thể nộp.
                boolean isAssignedMember = isSubTaskAssignee(currentUser, st);
                boolean isUnassignedAndLeadOrOwner = (st.getAssigneeId() == 0 && (isTaskLead(currentUser, parentTask) || isProjectOwner(currentUser, project)));

                if (isAssignedMember || isUnassignedAndLeadOrOwner) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String now = LocalDateTime.now().format(formatter);

                    SubTaskDB.submitDeliverable(subTaskId, submissionNote, now);

                    // Bắn thông báo thời gian thực 🔔 cho Task Lead (nếu Task Lead không phải chính mình)
                    int leadId = parentTask.getAssigneeId();
                    if (leadId > 0 && leadId != currentUser.getId()) {
                        NotificationDB.send(
                            leadId,
                            "🟡 Báo cáo nộp việc con",
                            currentUser.getFullName() + " vừa nộp kết quả việc con [" + st.getTitle() + "], mời bạn nghiệm thu!",
                            "/task?action=list&projectId=" + projectId,
                            "bi-hourglass-split text-warning"
                        );
                    }

                    // Thông báo lên Luồng Thảo luận
                    String msgContent = "📤 " + currentUser.getFullName() + " vừa nộp kết quả việc con: [" + st.getTitle() + "] — Ghi chú: \"" + (submissionNote != null && !submissionNote.trim().isEmpty() ? submissionNote : "Đã hoàn tất công việc") + "\"";
                    MessageDB.insert(new Message(0, projectId, st.getTaskId(), 0, "Hệ Thống", msgContent, now));

                    if (session != null) session.setAttribute("toastSuccess", "Đã nộp báo cáo kết quả việc con thành công! Đang chờ Task Lead duyệt.");
                } else {
                    if (session != null) session.setAttribute("toastError", "Bạn không có quyền nộp bài cho việc con của người khác!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 9: Task Lead Duyệt Nghiệm Thu ĐẠT (Chuyển sang 🟢 APPROVED)
     * KÈM CƠ CHẾ DOMINO TỰ ĐỘNG NÂNG % TIẾN ĐỘ VÀ BAY SANG CỘT DONE
     */
    private void handleApproveSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int subTaskId = safeParseInt(request.getParameter("subTaskId"), 0);

        if (projectId <= 0 || subTaskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        SubTask st = SubTaskDB.selectById(subTaskId);
        HttpSession session = request.getSession(false);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null && parentTask.getProjectId() == projectId) {
                // KIỂM SOÁT BẢO MẬT PHÂN TẦNG NGHIỆM THU:
                // Thẩm quyền duyệt việc con (Tầng 1) thuộc về TRƯỞNG NHÓM TASK (Task Lead) của Task này.
                // Nếu Task lớn chưa phân công (assigneeId == 0), PM mới được tạm quyền duyệt.
                if (canReviewSubTask(currentUser, parentTask, project)) 
                {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String now = LocalDateTime.now().format(formatter);

                    SubTaskDB.approveDeliverable(subTaskId, now);

                    int newProgress = SubTaskDB.calculateProgress(st.getTaskId());

                    // CƠ CHẾ DOMINO TỰ ĐỘNG CHUYỂN CỘT KANBAN:
                    // Chỉ auto-complete khi TẤT CẢ subtask đều APPROVED (không chỉ DONE)
                    boolean allApproved = SubTaskDB.areAllSubtasksApproved(st.getTaskId());
                    if (allApproved && !"DONE".equals(parentTask.getStatus())) {
                        TaskDB.updateStatus(parentTask.getId(), "DONE");
                        String celebrationText = "🏆 CHÚC MỪNG TOÀN ĐỘI: Tất cả việc con đã được duyệt nghiệm thu ĐẠT (100%)! Thẻ công việc [" + parentTask.getTitle() + "] đã tự động chuyển sang trạng thái ĐÃ XONG!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", celebrationText, now));
                    } 
                    else if (newProgress > 0 && newProgress < 100 && "TODO".equals(parentTask.getStatus())) {
                        TaskDB.updateStatus(parentTask.getId(), "IN_PROGRESS");
                        String progressText = "🚀 BẮT ĐẦU THỰC HIỆN: Đã nghiệm thu " + newProgress + "% việc con. Task [" + parentTask.getTitle() + "] chuyển sang ĐANG LÀM!";
                        MessageDB.insert(new Message(0, projectId, parentTask.getId(), 0, "Hệ Thống", progressText, now));
                    }

                    // Bắn thông báo thời gian thực 🔔 cho Người phụ trách việc con
                    if (st.getAssigneeId() > 0 && st.getAssigneeId() != currentUser.getId()) {
                        NotificationDB.send(
                            st.getAssigneeId(),
                            "🟢 Nghiệm thu ĐẠT",
                            "Việc con [" + st.getTitle() + "] của bạn đã được Leader duyệt đạt 100%!",
                            "/task?action=list&projectId=" + projectId,
                            "bi-check-circle-fill text-success"
                        );
                    }

                    if (session != null) session.setAttribute("toastSuccess", "Đã duyệt nghiệm thu ĐẠT cho việc con!");
                } else {
                    if (session != null) session.setAttribute("toastError", "Thẩm quyền thẩm định việc con thuộc về Trưởng Nhóm Task (Task Lead) của công việc này!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 10: Task Lead Yêu Cầu Cân Chỉnh Nhỏ (Chuyển sang 🔵 REVISE - Màu Xanh Dương)
     */
    private void handleReviseSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int subTaskId = safeParseInt(request.getParameter("subTaskId"), 0);
        String feedbackNote = request.getParameter("feedbackNote");

        if (projectId <= 0 || subTaskId <= 0) 
        {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        SubTask st = SubTaskDB.selectById(subTaskId);
        HttpSession session = request.getSession(false);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null && parentTask.getProjectId() == projectId) {
                if (canReviewSubTask(currentUser, parentTask, project)) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String now = LocalDateTime.now().format(formatter);

                    SubTaskDB.reviseDeliverable(subTaskId, feedbackNote, now);

                    // Bắn thông báo thời gian thực 🔔 cho Thành viên phụ trách
                    if (st.getAssigneeId() > 0 && st.getAssigneeId() != currentUser.getId()) {
                        NotificationDB.send(
                            st.getAssigneeId(),
                            "🔵 Yêu cầu cân chỉnh việc con",
                            "Leader dặn dò: \"" + (feedbackNote != null ? feedbackNote : "Cần cân chỉnh một số chi tiết") + "\" đối với việc con [" + st.getTitle() + "]",
                            "/task?action=list&projectId=" + projectId,
                            "bi-pencil-square text-primary"
                        );
                    }

                    if (session != null) session.setAttribute("toastSuccess", "Đã gửi yêu cầu cân chỉnh nhỏ (🔵 Xanh Dương) tới thành viên!");
                } else {
                    if (session != null) session.setAttribute("toastError", "Thẩm quyền thẩm định việc con thuộc về Trưởng Nhóm Task (Task Lead) của công việc này!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 11: Task Lead Trả Về Do Chưa Đạt Yêu Cầu (Chuyển sang 🔴 REJECTED - Màu Đỏ)
     */
    private void handleRejectSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int subTaskId = safeParseInt(request.getParameter("subTaskId"), 0);
        String feedbackNote = request.getParameter("feedbackNote");

        if (projectId <= 0 || subTaskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        SubTask st = SubTaskDB.selectById(subTaskId);
        HttpSession session = request.getSession(false);

        if (st != null && currentUser != null) {
            Task parentTask = TaskDB.selectById(st.getTaskId());
            Project project = ProjectDB.selectById(projectId);

            if (parentTask != null && project != null && parentTask.getProjectId() == projectId) {
                if (canReviewSubTask(currentUser, parentTask, project)) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String now = LocalDateTime.now().format(formatter);

                    SubTaskDB.rejectDeliverable(subTaskId, feedbackNote, now);

                    // Bắn thông báo thời gian thực 🔔 cho Thành viên phụ trách
                    if (st.getAssigneeId() > 0 && st.getAssigneeId() != currentUser.getId()) {
                        NotificationDB.send(
                            st.getAssigneeId(),
                            "🔴 Việc con chưa đạt yêu cầu",
                            "Leader phản hồi lỗi: \"" + (feedbackNote != null ? feedbackNote : "Chưa đạt yêu cầu đề ra") + "\" đối với việc con [" + st.getTitle() + "]",
                            "/task?action=list&projectId=" + projectId,
                            "bi-exclamation-triangle-fill text-danger"
                        );
                    }

                    if (session != null) session.setAttribute("toastSuccess", "Đã trả về việc con và gửi phản hồi (🔴 Màu Đỏ) cho thành viên!");
                } else {
                    if (session != null) session.setAttribute("toastError", "Thẩm quyền thẩm định việc con thuộc về Trưởng Nhóm Task (Task Lead) của công việc này!");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 12: Task Lead Bàn Giao & Nộp Báo Cáo Task Lớn Lên Cho PM (Chuyển sang 🟡 SUBMITTED)
     */
    private void handleSubmitParentTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        
        // Thu thập 5 trường thông tin báo cáo bàn giao có cấu trúc + Tệp đính kèm
        String summary = request.getParameter("summary");
        String demoUrl = request.getParameter("demoUrl");
        String codeUrl = request.getParameter("codeUrl");
        String testResult = request.getParameter("testResult");
        String testingGuide = request.getParameter("testingGuide");
        String deliverableNote = request.getParameter("deliverableNote");
        String deliverableFile = request.getParameter("deliverableFile");

        if (projectId <= 0 || taskId <= 0) 
        {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task != null && project != null && task.getProjectId() == projectId && currentUser != null) {
            // KIỂM SOÁT THẨM QUYỀN NGHIỆM THU TẦNG 2:
            // 1. Nếu Task lớn đã gán cho Task Lead cụ thể: CHỈ chính Task Lead đó mới được nộp bàn giao.
            // 2. Nếu Task lớn chưa gán cho ai: PM mới được nộp.
            if (canReviewSubTask(currentUser, task, project)) {
                // RÀNG BUỘC CHẤT LƯỢNG: Task Lead chỉ được nộp bàn giao khi toàn bộ việc con đã hoàn tất 100%
                List<SubTask> subTasks = SubTaskDB.selectByTaskId(taskId);
                int progress = SubTaskDB.calculateProgress(taskId);
                if (subTasks != null && !subTasks.isEmpty() && progress < 100) {
                    if (session != null) {
                        session.setAttribute("toastError", 
                            "⚠️ Không thể nộp bàn giao Task [" + task.getTitle() + "] cho PM khi danh sách việc con chưa đạt 100% (Tiến độ hiện tại: " + progress + "%). Hãy hoàn thành các việc con trước!");
                    }
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                // Ghép 5 trường dữ liệu thành Bản Báo Cáo Bàn Giao Chuẩn Cấu Trúc
                StringBuilder sb = new StringBuilder();
                if (summary != null && !summary.trim().isEmpty()) {
                    sb.append("📌 [Tóm tắt kết quả]: ").append(summary.trim()).append("\n\n");
                }
                if (demoUrl != null && !demoUrl.trim().isEmpty()) {
                    sb.append("🌐 [Link Demo/Sản phẩm]: ").append(demoUrl.trim()).append("\n\n");
                }
                if (codeUrl != null && !codeUrl.trim().isEmpty()) {
                    sb.append("💻 [Link Mã nguồn/PR]: ").append(codeUrl.trim()).append("\n\n");
                }
                if (testResult != null && !testResult.trim().isEmpty()) {
                    sb.append("🧪 [Kết quả kiểm thử]: ").append(testResult.trim()).append("\n\n");
                }
                if (testingGuide != null && !testingGuide.trim().isEmpty()) {
                    sb.append("🧭 [Hướng dẫn PM nghiệm thu]: ").append(testingGuide.trim());
                }

                String finalNote = sb.toString().trim();
                if (finalNote.isEmpty()) {
                    finalNote = (deliverableNote != null && !deliverableNote.trim().isEmpty()) 
                        ? deliverableNote.trim() 
                        : "Đã hoàn thành toàn bộ công việc theo yêu cầu.";
                }

                if (deliverableFile == null || deliverableFile.trim().isEmpty()) {
                    deliverableFile = "Bao_Cao_Nghiem_Thu_Task_" + task.getId() + ".pdf";
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.submitTaskDeliverable(taskId, finalNote, deliverableFile.trim(), now);

                // Ghi nhận Activity Log
                ActivityLogDB.logAsync(projectId, currentUser.getId(), "TASK_SUBMIT", "TASK", taskId, task.getTitle(), "Đã nộp hồ sơ bàn giao nghiệm thu kèm tệp [" + deliverableFile.trim() + "] lên PM");

                // Bắn thông báo thời gian thực 🔔 cho Trưởng Dự Án (PM)
                if (project.getOwnerId() > 0 && project.getOwnerId() != currentUser.getId()) {
                    NotificationDB.send(
                        project.getOwnerId(),
                        "🟡 Bàn giao Task lớn",
                        currentUser.getFullName() + " vừa nộp báo cáo bàn giao Task [" + task.getTitle() + "] kèm tệp đính kèm, kính mời PM nghiệm thu!",
                        "/task?action=list&projectId=" + projectId,
                        "bi-box-seam-fill text-warning"
                    );
                }

                // Thông báo lên Luồng Thảo luận
                String msgContent = "📦 [BÀN GIAO TASK]: " + currentUser.getFullName() + " đã nộp hồ sơ bàn giao Task [" + task.getTitle() + "] kèm tệp [" + deliverableFile.trim() + "] lên PM!";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                if (session != null) session.setAttribute("toastSuccess", "Đã nộp báo cáo bàn giao Task lớn thành công! Đang chờ PM phê duyệt.");
            } else {
                if (session != null) session.setAttribute("toastError", "Bạn không phải là Task Lead của thẻ công việc này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 12.5: Task Lead Trình Kế Hoạch Phân Rã Việc Con Cho PM Thẩm Định (CỔNG 1 ➔ Chuyển sang 🟣 PLANNING)
     */
    private void handleSubmitPlanningRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String planningNote = request.getParameter("planningNote");

        if (projectId <= 0 || taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task != null && project != null && task.getProjectId() == projectId && currentUser != null) {
            // KIỂM SOÁT THẨM QUYỀN: Task Lead của task hoặc PM
            if (canReviewSubTask(currentUser, task, project)) {
                // RÀNG BUỘC CHẤT LƯỢNG: Phải phân rã ít nhất 1 việc con mới được trình PM
                List<SubTask> subTasks = SubTaskDB.selectByTaskId(taskId);
                if (subTasks == null || subTasks.isEmpty()) {
                    if (session != null) {
                        session.setAttribute("toastError", 
                            "⚠️ Không thể trình kế hoạch rỗng! Vui lòng phân rã ít nhất 1 việc con (Sub-task) trước khi gửi PM duyệt.");
                    }
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.submitPlanningRequest(taskId, planningNote, now);

                // Bắn thông báo thời gian thực 🔔 cho Trưởng Dự Án (PM)
                if (project.getOwnerId() > 0 && project.getOwnerId() != currentUser.getId()) {
                    NotificationDB.send(
                        project.getOwnerId(),
                        "🟣 Trình Kế Hoạch Phân Rã Việc Con",
                        currentUser.getFullName() + " vừa trình kế hoạch phân rã " + subTasks.size() + " việc con cho Task [" + task.getTitle() + "], kính mời PM xem xét và khóa kế hoạch!",
                        "/task?action=list&projectId=" + projectId,
                        "bi-diagram-3-fill text-primary"
                    );
                }

                // Thông báo lên Luồng Thảo luận
                String msgContent = "📋 [TRÌNH KẾ HOẠCH PHÂN RÃ]: " + currentUser.getFullName() + " đã phân rã xong " + subTasks.size() + " việc con cho Task [" + task.getTitle() + "] và trình lên Trưởng Dự Án (PM) phê duyệt khóa phạm vi!";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                if (session != null) session.setAttribute("toastSuccess", "Đã trình kế hoạch phân rã việc con lên Trưởng Dự Án (PM) thành công! Đang chờ PM phê duyệt khóa phạm vi.");
            } else {
                if (session != null) session.setAttribute("toastError", "Bạn không phải là Task Lead của thẻ công việc này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 12.6: Trưởng Dự Án (PM) Phê Duyệt Kế Hoạch & KHÓA PHÂN RÃ (SCOPE LOCK ➔ Chuyển sang 🚀 IN_PROGRESS)
     */
    private void handlePmApprovePlanning(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String feedback = request.getParameter("feedback");

        if (projectId <= 0 || taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task != null && project != null && task.getProjectId() == projectId && currentUser != null) {
            // KIỂM SOÁT BẢO MẬT: Chỉ DUY NHẤT Trưởng Dự Án (PM) mới được duyệt kế hoạch Cổng 1
            if (isProjectOwner(currentUser, project)) {
                List<SubTask> subTasks = SubTaskDB.selectByTaskId(taskId);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmApprovePlanning(taskId, feedback, now);

                // Bắn thông báo thời gian thực 🔔 cho Task Lead
                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "🔒 PM Đã Phê Duyệt & Khóa Kế Hoạch",
                        "Trưởng Dự Án đã duyệt ma trận phân rã " + (subTasks != null ? subTasks.size() : 0) + " việc con của Task [" + task.getTitle() + "]. Kế hoạch đã khóa (Scope Lock), đội ngũ bắt tay thực thi!",
                        "/task?action=list&projectId=" + projectId,
                        "bi-lock-fill text-success"
                    );
                }

                // Thông báo lên Luồng Thảo luận
                String msgContent = "🔒 [PM KHÓA KẾ HOẠCH PHÂN RÃ]: Trưởng Dự Án đã duyệt danh mục " + (subTasks != null ? subTasks.size() : 0) + " việc con của Task [" + task.getTitle() + "]! Phạm vi công việc chính thức được KHÓA (Scope Baseline Lock). Đội ngũ bắt đầu thực thi!";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                if (session != null) session.setAttribute("toastSuccess", "Trưởng Dự Án đã phê duyệt và khóa kế hoạch phân rã thành công! Task chuyển sang Đang Làm.");
            } else {
                if (session != null) session.setAttribute("toastError", "Chỉ Trưởng Dự Án (PM) mới có thẩm quyền duyệt và khóa kế hoạch phân rã!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 12.7: Trưởng Dự Án (PM) Yêu Cầu Task Lead Bổ Sung / Chỉnh Sửa Kế Hoạch (Trả về ⚪ TODO)
     */
    private void handlePmRejectPlanning(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String feedback = request.getParameter("feedback");

        if (projectId <= 0 || taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task != null && project != null && task.getProjectId() == projectId && currentUser != null) {
            if (isProjectOwner(currentUser, project)) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmRejectPlanning(taskId, feedback, now);

                // Bắn thông báo thời gian thực 🔔 cho Task Lead
                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "↩️ PM Yêu Cầu Chỉnh Sửa Kế Hoạch",
                        "Trưởng Dự Án yêu cầu bổ sung kế hoạch Task [" + task.getTitle() + "]: \"" + (feedback != null ? feedback : "Cần bóc tách thêm việc con") + "\"",
                        "/task?action=list&projectId=" + projectId,
                        "bi-arrow-counterclockwise text-warning"
                    );
                }

                // Thông báo lên Luồng Thảo luận
                String msgContent = "↩️ [PM YÊU CẦU ĐIỀU CHỈNH KẾ HOẠCH]: Trưởng Dự Án yêu cầu Task Lead hoàn thiện lại danh mục việc con của Task [" + task.getTitle() + "]. Lý do: \"" + (feedback != null && !feedback.trim().isEmpty() ? feedback : "Cần phân rã chi tiết hơn") + "\"";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                if (session != null) session.setAttribute("toastSuccess", "Đã trả về kế hoạch phân rã để Task Lead tiếp tục hoàn thiện.");
            } else {
                if (session != null) session.setAttribute("toastError", "Chỉ Trưởng Dự Án mới có quyền đưa ra quyết định này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 13: Trưởng Dự Án (PM) Phê Duyệt Nghiệm Thu Task Lớn ĐẠT (Chuyển sang 🟢 DONE)
     */
    private void handlePmApproveTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String feedback = request.getParameter("feedback");
        int qualityRating = Math.max(1, Math.min(5, safeParseInt(request.getParameter("qualityRating"), 5)));

        if (projectId <= 0 || taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task != null && project != null && task.getProjectId() == projectId && currentUser != null) {
            // KIỂM SOÁT BẢO MẬT: Chỉ DUY NHẤT Trưởng Dự Án (PM) mới có quyền PHÊ DUYỆT TỐI CAO
            if (isProjectOwner(currentUser, project)) {
                // RÀNG BUỘC CHẤT LƯỢNG NGHIỆM THU: PM chỉ duyệt đạt khi toàn bộ việc con đã đạt 100%
                List<SubTask> subTasks = SubTaskDB.selectByTaskId(taskId);
                int progress = SubTaskDB.calculateProgress(taskId);
                if (subTasks != null && !subTasks.isEmpty() && progress < 100) {
                    if (session != null) {
                        session.setAttribute("toastError", 
                            "⚠️ Không thể duyệt đạt Task [" + task.getTitle() + "]! Vẫn còn " + (100 - progress) + "% việc con chưa được hoàn tất nghiệm thu.");
                    }
                    response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                    return;
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmApproveTask(taskId, feedback, qualityRating, now);

                // Ghi nhận Activity Log
                String approveDesc = "PM đã phê duyệt nghiệm thu (" + qualityRating + " ⭐): " + (feedback != null && !feedback.trim().isEmpty() ? feedback : "Đạt chất lượng xuất sắc!");
                ActivityLogDB.logAsync(projectId, currentUser.getId(), "PM_APPROVE", "TASK", taskId, task.getTitle(), approveDesc);

                // Bắn thông báo thời gian thực 🔔 cho Task Lead
                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "🏆 PM Phê Duyệt Nghiệm Thu (" + qualityRating + " ⭐)",
                        "Trưởng Dự Án đã chính thức ký duyệt nghiệm thu hoàn tất 100% và chấm " + qualityRating + " sao cho Task [" + task.getTitle() + "]!",
                        "/task?action=list&projectId=" + projectId,
                        "bi-trophy-fill text-warning"
                    );
                }

                // Thông báo cúp vàng lên Thảo luận
                String msgContent = "🏆 [PM KÝ DUYỆT ĐÓNG TASK]: Trưởng Dự Án đã nghiệm thu hoàn thành 100% (Đánh giá: " + qualityRating + " ⭐) cho Task [" + task.getTitle() + "]! Lời nhận xét: \"" + (feedback != null && !feedback.trim().isEmpty() ? feedback : "Đạt chất lượng xuất sắc!") + "\"";
                MessageDB.insert(new Message(0, projectId, task.getId(), 0, "Hệ Thống", msgContent, now));

                if (session != null) session.setAttribute("toastSuccess", "Trưởng Dự Án đã phê duyệt nghiệm thu thành công! Task đã hoàn tất 100%.");
            } else {
                if (session != null) session.setAttribute("toastError", "Chỉ Trưởng Dự Án (PM) mới có thẩm quyền phê duyệt nghiệm thu tối cao!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 14: Trưởng Dự Án (PM) Yêu Cầu Cân Chỉnh Nhỏ (Chuyển sang 🔵 REVISE - Màu Xanh Dương)
     */
    private void handlePmReviseTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String feedback = request.getParameter("feedback");

        if (projectId <= 0 || taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task != null && project != null && task.getProjectId() == projectId && currentUser != null) {
            if (isProjectOwner(currentUser, project)) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmReviseTask(taskId, feedback, now);

                // Ghi nhận Activity Log
                String reviseDesc = "PM yêu cầu cân chỉnh nhỏ: " + (feedback != null && !feedback.trim().isEmpty() ? feedback : "Cần hoàn thiện thêm chi tiết");
                ActivityLogDB.logAsync(projectId, currentUser.getId(), "PM_REVISE", "TASK", taskId, task.getTitle(), reviseDesc);

                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "🔵 PM Yêu Cầu Cân Chỉnh",
                        "Trưởng Dự Án dặn dò: \"" + (feedback != null ? feedback : "Cần cân chỉnh thêm một số chi tiết") + "\" đối với Task [" + task.getTitle() + "]",
                        "/task?action=list&projectId=" + projectId,
                        "bi-pencil-square text-primary"
                    );
                }

                if (session != null) session.setAttribute("toastSuccess", "Đã gửi yêu cầu cân chỉnh nhỏ (🔵 Xanh Dương) tới Task Lead!");
            } else {
                if (session != null) session.setAttribute("toastError", "Chỉ Trưởng Dự Án mới có quyền đưa ra quyết định này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Nghiệp vụ 15: Trưởng Dự Án (PM) Trả Về Do Chưa Đạt (Chuyển sang 🔴 REJECTED - Màu Đỏ)
     */
    private void handlePmRejectTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String feedback = request.getParameter("feedback");

        if (projectId <= 0 || taskId <= 0) {
            response.sendRedirect(request.getContextPath() + "/project?action=list");
            return;
        }

        User currentUser = getCurrentUser(request);
        Task task = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        HttpSession session = request.getSession(false);

        if (task != null && project != null && task.getProjectId() == projectId && currentUser != null) {
            if (isProjectOwner(currentUser, project)) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String now = LocalDateTime.now().format(formatter);

                TaskDB.pmRejectTask(taskId, feedback, now);

                // Ghi nhận Activity Log
                String rejectDesc = "PM từ chối nghiệm thu: " + (feedback != null && !feedback.trim().isEmpty() ? feedback : "Chưa đạt yêu cầu đề ra");
                ActivityLogDB.logAsync(projectId, currentUser.getId(), "PM_REJECT", "TASK", taskId, task.getTitle(), rejectDesc);

                if (task.getAssigneeId() > 0 && task.getAssigneeId() != currentUser.getId()) {
                    NotificationDB.send(
                        task.getAssigneeId(),
                        "🔴 PM Chưa Đạt Yêu Cầu",
                        "Trưởng Dự Án phản hồi lỗi: \"" + (feedback != null ? feedback : "Chưa đạt chuẩn đề ra") + "\" đối với Task [" + task.getTitle() + "]",
                        "/task?action=list&projectId=" + projectId,
                        "bi-exclamation-triangle-fill text-danger"
                    );
                }

                if (session != null) session.setAttribute("toastSuccess", "Đã trả về Task lớn và gửi phản hồi (🔴 Màu Đỏ) cho Task Lead!");
            } else {
                if (session != null) session.setAttribute("toastError", "Chỉ Trưởng Dự Án mới có quyền đưa ra quyết định này!");
            }
        }

        response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
    }

    /**
     * Thuật toán tổng hợp Khối Lượng Công Việc (Workload Engine) cho từng thành viên:
     * 1. Đếm số Task lớn làm Lead & gom danh sách chi tiết các Task đó (leadTasks)
     * 2. Đếm số Việc Con được giao & số Việc Con đã xong [☑]
     * 3. Thu thập mảng relatedTaskIds để JavaScript lọc Kanban trong 0.01 giây
     */
    private List<UserWorkload> computeUserWorkloads(List<User> userList, List<Task> allTasks, Map<Integer, List<SubTask>> taskSubTasksMap) {
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

    // ==================== BỘ TIỆN ÍCH TRÍCH XUẤT & PHÂN QUYỀN CHUẨN BACKEND CODE MASTERY ====================

    /**
     * Lấy User hiện tại đang đăng nhập từ Session một cách an toàn (tránh tạo Session rác).
     */
    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return (session != null) ? (User) session.getAttribute("currentUser") : null;
    }

    /**
     * Phân tích chuỗi thành số nguyên an toàn, trả về defaultValue nếu null, rỗng hoặc sai định dạng.
     */
    private int safeParseInt(String param, int defaultValue) {
        if (param == null || param.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(param.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Kiểm tra xem người dùng có phải là Trưởng Dự Án (PM / Project Owner) không.
     */
    private boolean isProjectOwner(User user, Project project) {
        return user != null && project != null && user.getId() == project.getOwnerId();
    }

    /**
     * Kiểm tra xem người dùng có phải là Task Lead của thẻ công việc lớn không.
     */
    private boolean isTaskLead(User user, Task task) {
        return user != null && task != null && task.getAssigneeId() > 0 && user.getId() == task.getAssigneeId();
    }

    /**
     * Kiểm tra xem người dùng có phải là Người thực hiện việc con (SubTask Assignee) không.
     */
    private boolean isSubTaskAssignee(User user, SubTask subTask) {
        return user != null && subTask != null && subTask.getAssigneeId() > 0 && user.getId() == subTask.getAssigneeId();
    }

    /**
     * Kiểm tra thẩm quyền quản lý Việc con 3 Bên: Người làm việc con HOẶC Task Lead HOẶC Trưởng Dự Án.
     */
    private boolean canManageSubTask(User user, SubTask st, Task parentTask, Project project) {
        return isSubTaskAssignee(user, st) || isTaskLead(user, parentTask) || isProjectOwner(user, project);
    }

    /**
     * Kiểm tra thẩm quyền thẩm định / duyệt Việc con (Cổng 2): Task Lead HOẶC PM (nếu task chưa có lead).
     */
    private boolean canReviewSubTask(User user, Task parentTask, Project project) {
        return isTaskLead(user, parentTask) || (parentTask != null && parentTask.getAssigneeId() == 0 && isProjectOwner(user, project));
    }

    /**
     * Nghiệp vụ AJAX: Tạo nhanh Task lớn trực tiếp từ dòng inline của nhóm (List View)
     */
    private void handleQuickAddParentTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        String title = request.getParameter("title");
        String status = request.getParameter("status");
        String priority = request.getParameter("priority");
        String dueDate = request.getParameter("dueDate");
        int assigneeId = safeParseInt(request.getParameter("assigneeId"), 0);

        if (projectId <= 0 || title == null || title.trim().isEmpty()) {
            sendJsonResponse(response, false, "Tiêu đề công việc không được để trống!", null);
            return;
        }

        User currentUser = getCurrentUser(request);
        if (currentUser == null || !ProjectMemberDB.isMember(projectId, currentUser.getId())) {
            sendJsonResponse(response, false, "Bạn không có quyền thao tác trong dự án này!", null);
            return;
        }

        // Mọi công việc mới tạo bắt buộc bắt đầu từ TO DO theo chuẩn quy trình ClickUp/Agile
        status = "TODO";

        if (priority == null || priority.trim().isEmpty()) {
            priority = "MEDIUM";
        }
        priority = priority.trim().toUpperCase();
        if (!"HIGH".equals(priority) && !"MEDIUM".equals(priority) && !"LOW".equals(priority)) {
            priority = "MEDIUM";
        }

        String assigneeName = "Chưa phân công";
        if (assigneeId > 0 && ProjectMemberDB.isMember(projectId, assigneeId)) {
            User u = UserDB.selectById(assigneeId);
            if (u != null) {
                assigneeName = u.getFullName();
            } else {
                assigneeId = 0;
            }
        } else {
            assigneeId = 0;
        }

        String cleanDueDate = (dueDate != null && !dueDate.trim().isEmpty()) ? dueDate.trim() : "";

        Task newTask = new Task(
            0,
            projectId,
            title.trim(),
            "",
            status,
            priority,
            cleanDueDate,
            assigneeId,
            assigneeName,
            "",
            "",
            "",
            "",
            "",
            5,
            "",
            ""
        );

        Project currentPrj = ProjectDB.selectById(projectId);
        if (currentPrj != null && currentPrj.isSoloProject()) {
            newTask.setAssigneeId(currentUser.getId());
            newTask.setAssigneeName(currentUser.getFullName());
            newTask.setRequiresGate(false);
        } else {
            boolean defaultRequiresGate = (currentPrj != null && currentPrj.isTeamProject());
            String reqGateParam = request.getParameter("requiresGate");
            if (reqGateParam != null) {
                newTask.setRequiresGate("true".equalsIgnoreCase(reqGateParam.trim()) || "on".equalsIgnoreCase(reqGateParam.trim()) || "1".equals(reqGateParam.trim()));
            } else {
                newTask.setRequiresGate(defaultRequiresGate);
            }
        }

        int newTaskId = TaskDB.insert(newTask);
        if (newTaskId <= 0) {
            sendJsonResponse(response, false, "Lỗi khi lưu công việc vào cơ sở dữ liệu!", null);
            return;
        }
        newTask.setId(newTaskId);

        // Ghi nhận Activity Log
        ActivityLogDB.logAsync(projectId, currentUser.getId(), "TASK_CREATE", "TASK", newTaskId, newTask.getTitle(), "Tạo nhanh công việc: " + newTask.getTitle());

        String dataJson = String.format(
            "{\"id\":%d,\"projectId\":%d,\"title\":\"%s\",\"status\":\"%s\",\"priority\":\"%s\",\"dueDate\":\"%s\",\"assigneeId\":%d,\"assigneeName\":\"%s\"}",
            newTask.getId(),
            projectId,
            escapeJson(newTask.getTitle()),
            escapeJson(newTask.getStatus()),
            escapeJson(newTask.getPriority()),
            escapeJson(newTask.getDueDate()),
            newTask.getAssigneeId(),
            escapeJson(newTask.getAssigneeName())
        );

        sendJsonResponse(response, true, "Đã tạo nhanh công việc thành công!", dataJson);
    }

    /**
     * Nghiệp vụ AJAX: Tạo nhanh Việc con (Subtask) trực tiếp dưới Task cha (List View)
     */
    private void handleQuickAddSubTask(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int projectId = safeParseInt(request.getParameter("projectId"), 0);
        int taskId = safeParseInt(request.getParameter("taskId"), 0);
        String title = request.getParameter("title");
        String dueDateParam = request.getParameter("dueDate");
        String subDueDate = (dueDateParam != null) ? dueDateParam.trim() : "";
        int assigneeId = safeParseInt(request.getParameter("assigneeId"), 0);

        if (projectId <= 0 || taskId <= 0 || title == null || title.trim().isEmpty()) {
            sendJsonResponse(response, false, "Tiêu đề việc con không được để trống!", null);
            return;
        }

        User currentUser = getCurrentUser(request);
        if (currentUser == null || !ProjectMemberDB.isMember(projectId, currentUser.getId())) {
            sendJsonResponse(response, false, "Bạn không có quyền thao tác trong dự án này!", null);
            return;
        }

        Task parentTask = TaskDB.selectById(taskId);
        Project project = ProjectDB.selectById(projectId);
        if (parentTask == null || parentTask.getProjectId() != projectId) {
            sendJsonResponse(response, false, "Không tìm thấy công việc cha!", null);
            return;
        }

        if (!isTaskLead(currentUser, parentTask) && !isProjectOwner(currentUser, project)) {
            sendJsonResponse(response, false, "Bạn không có quyền phân rã việc con cho Task này!", null);
            return;
        }

        if (!"TODO".equalsIgnoreCase(parentTask.getStatus()) && !"IN_PROGRESS".equalsIgnoreCase(parentTask.getStatus())) {
            sendJsonResponse(response, false, "Công việc này đã nộp hoặc hoàn tất nghiệm thu, không thể thêm việc con mới!", null);
            return;
        }

        String assigneeName = "Chưa phân công";
        if (project.isSoloProject()) {
            assigneeId = currentUser.getId();
            assigneeName = currentUser.getFullName();
        } else if (assigneeId > 0 && ProjectMemberDB.isMember(projectId, assigneeId)) {
            User u = UserDB.selectById(assigneeId);
            if (u != null) {
                assigneeName = u.getFullName();
            } else {
                assigneeId = 0;
            }
        } else {
            assigneeId = 0;
        }

        SubTask newSubTask = new SubTask(
            0,
            taskId,
            title.trim(),
            assigneeId,
            assigneeName,
            "TODO",
            subDueDate,
            "",
            "",
            "",
            ""
        );
        int subId = SubTaskDB.insert(newSubTask);
        if (subId <= 0) {
            sendJsonResponse(response, false, "Lỗi khi lưu việc con vào cơ sở dữ liệu!", null);
            return;
        }
        newSubTask.setId(subId);

        // Ghi nhận Activity Log
        ActivityLogDB.logAsync(projectId, currentUser.getId(), "SUBTASK_CREATE", "SUBTASK", subId, newSubTask.getTitle(), "Thêm việc con: " + newSubTask.getTitle() + " cho Task #" + taskId);

        String dataJson = String.format(
            "{\"id\":%d,\"taskId\":%d,\"title\":\"%s\",\"status\":\"%s\",\"dueDate\":\"%s\",\"assigneeId\":%d,\"assigneeName\":\"%s\"}",
            newSubTask.getId(),
            newSubTask.getTaskId(),
            escapeJson(newSubTask.getTitle()),
            escapeJson(newSubTask.getStatus()),
            escapeJson(newSubTask.getDueDate()),
            newSubTask.getAssigneeId(),
            escapeJson(newSubTask.getAssigneeName())
        );

        sendJsonResponse(response, true, "Đã thêm việc con thành công!", dataJson);
    }

    /**
     * Nhận diện request AJAX từ Fetch API, XMLHttpRequest hoặc cờ ajax=true
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
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
    private void sendJsonResponse(HttpServletResponse response, boolean success, String message, String dataJson) throws IOException {
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
    private String escapeJson(String s) {
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


