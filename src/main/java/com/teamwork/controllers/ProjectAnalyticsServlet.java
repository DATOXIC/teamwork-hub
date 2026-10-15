package com.teamwork.controllers;

import com.teamwork.business.Project;
import com.teamwork.business.ProjectMember;
import com.teamwork.business.SubTask;
import com.teamwork.business.Task;
import com.teamwork.business.TaskDependency;
import com.teamwork.business.TaskSubmission;
import com.teamwork.business.User;
import com.teamwork.business.UserWorkload;
import com.teamwork.business.analytics.BurndownCalculator;
import com.teamwork.business.analytics.BurndownData;
import com.teamwork.business.analytics.DeliveryAccountability;
import com.teamwork.business.analytics.QualityMetrics;
import com.teamwork.business.analytics.QualityVelocityCalculator;
import com.teamwork.business.analytics.StaleBlockerDetector;
import com.teamwork.business.analytics.WorkloadAnalytics;
import com.teamwork.controllers.task.TaskBoardHandler;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.ProjectMemberDB;
import com.teamwork.data.SubTaskDB;
import com.teamwork.data.TaskDB;
import com.teamwork.data.TaskDependencyDB;
import com.teamwork.data.TaskSubmissionDB;
import com.teamwork.data.UserDB;
import com.teamwork.data.NotificationDB;
import com.teamwork.data.ActivityLogDB;
import com.teamwork.controllers.ProjectAccess;
import java.time.LocalDate;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller điều phối Trung tâm Phân Tích Động Lực & Sức Khỏe Dự Án (Enterprise Analytics):
 * - Biểu đồ Burndown tiến độ thực tế vs lý tưởng (Chart.js 4.4+)
 * - Chỉ số chất lượng bàn giao & Tốc độ thẩm định của PM (First-time Pass Rate, Velocity)
 * - Ma trận phân bổ khối lượng làm việc & Cảnh báo quá tải (Workload Balance Heatmap)
 * - Điểm sức khỏe dự án tổng hợp (Project Health Index)
 * 
 * Hỗ trợ đồng thời 2 chế độ:
 * 1. Render giao diện JSP chuẩn SaaS (/project-analytics hoặc /analytics)
 * 2. Cung cấp API dữ liệu JSON thời gian thực (action=data)
 */
@WebServlet(name = "ProjectAnalyticsServlet", urlPatterns = {"/analytics", "/project-analytics"})
public class ProjectAnalyticsServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ProjectAnalyticsServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = currentUser(request);
        if (user == null) {
            redirect(request, response, "/auth?action=viewLogin");
            return;
        }

        int projectId = intParam(request, "projectId", 0);
        if (projectId <= 0) {
            List<Project> userProjects = ProjectMemberDB.selectProjectsByUserId(user.getId());
            if (userProjects != null && !userProjects.isEmpty()) {
                projectId = userProjects.get(0).getId();
            } else {
                flash(request, "toastError", "Bạn chưa tham gia dự án nào để xem phân tích.");
                redirect(request, response, PROJECT_LIST);
                return;
            }
        }

        // Kiểm tra quyền thành viên trong dự án
        if (!requireMember(request, response, user, projectId, "Bạn không có quyền xem phân tích của dự án này.")) {
            return;
        }

        Project project = ProjectDB.selectById(projectId);
        if (project == null) {
            flash(request, "toastError", "Dự án không tồn tại hoặc đã bị xóa.");
            redirect(request, response, PROJECT_LIST);
            return;
        }

        try {
            // 1. Tải toàn bộ công việc và lịch sử nghiệm thu bằng batch queries
            List<Task> allTasks = TaskDB.selectByProjectId(projectId);
            List<TaskSubmission> allSubmissions = TaskSubmissionDB.selectByProjectId(projectId);
            List<SubTask> allSubTasks = SubTaskDB.selectByProjectId(projectId);
            List<TaskDependency> dependencies = TaskDependencyDB.selectByProjectId(projectId);
            LocalDate today = LocalDate.now();

            Map<Integer, List<SubTask>> taskSubTasksMap = new HashMap<>();
            for (SubTask st : allSubTasks) {
                taskSubTasksMap.computeIfAbsent(st.getTaskId(), k -> new ArrayList<>()).add(st);
            }

            for (Task t : allTasks) {
                if (t != null && taskSubTasksMap.containsKey(t.getId())) {
                    t.setSubtasks(taskSubTasksMap.get(t.getId()));
                }
            }

            // 2. Tải danh sách thành viên dự án
            List<ProjectMember> projectMemberList = ProjectMemberDB.selectByProjectId(projectId);
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

            // 3. Tính toán khối lượng công việc (Workload) & Năng lực khả dụng (Capacity Balancing)
            List<UserWorkload> workloadList = TaskBoardHandler.computeUserWorkloads(userList, allTasks, taskSubTasksMap);
            List<WorkloadAnalytics> workloadAnalytics = WorkloadAnalytics.fromWorkloads(workloadList, projectMemberList, allTasks, today);
            List<WorkloadAnalytics.RebalanceSuggestion> rebalanceSuggestions = WorkloadAnalytics.generateRebalanceSuggestions(workloadAnalytics, allTasks);

            // 4. Đo lường tính chịu trách nhiệm (Delivery Accountability) & Radar điểm nghẽn (Blocker/Stale)
            List<DeliveryAccountability> accountabilityList = DeliveryAccountability.analyze(userList, allTasks, today);
            StaleBlockerDetector.StaleBlockerReport staleBlockerReport = StaleBlockerDetector.detect(allTasks, dependencies, today);

            // 5. Động cơ tính toán POJO thuần túy: Burndown & Quality Velocity
            BurndownData burndownData = BurndownCalculator.calculate(project, allTasks);
            QualityMetrics qualityMetrics = QualityVelocityCalculator.calculate(allSubmissions, allTasks, workloadList, burndownData);

            // 6. Kiểm tra yêu cầu dữ liệu JSON (AJAX live refresh)
            String action = request.getParameter("action");
            String acceptHeader = request.getHeader("Accept");
            boolean isJsonRequest = "data".equalsIgnoreCase(action)
                    || (acceptHeader != null && acceptHeader.contains("application/json"));

            if (isJsonRequest) {
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("{");
                jsonBuilder.append("\"success\":true,");
                jsonBuilder.append("\"projectId\":").append(projectId).append(",");
                jsonBuilder.append("\"projectName\":\"").append(escapeJson(project.getName())).append("\",");
                jsonBuilder.append("\"burndown\":").append(burndownData.toJson()).append(",");
                jsonBuilder.append("\"quality\":").append(qualityMetrics.toJson()).append(",");
                jsonBuilder.append("\"staleBlocker\":").append(staleBlockerReport.toJson()).append(",");

                jsonBuilder.append("\"workloads\":[");
                for (int i = 0; i < workloadAnalytics.size(); i++) {
                    if (i > 0) jsonBuilder.append(",");
                    jsonBuilder.append(workloadAnalytics.get(i).toJson());
                }
                jsonBuilder.append("],");

                jsonBuilder.append("\"accountability\":[");
                for (int i = 0; i < accountabilityList.size(); i++) {
                    if (i > 0) jsonBuilder.append(",");
                    jsonBuilder.append(accountabilityList.get(i).toJson());
                }
                jsonBuilder.append("],");

                jsonBuilder.append("\"rebalanceSuggestions\":[");
                for (int i = 0; i < rebalanceSuggestions.size(); i++) {
                    if (i > 0) jsonBuilder.append(",");
                    jsonBuilder.append(rebalanceSuggestions.get(i).toJson());
                }
                jsonBuilder.append("]");

                jsonBuilder.append("}");

                writeJson(response, HttpServletResponse.SC_OK, jsonBuilder.toString());
                return;
            }

            // 7. Đóng gói Model cho View JSP
            request.setAttribute("project", project);
            request.setAttribute("burndownData", burndownData);
            request.setAttribute("qualityMetrics", qualityMetrics);
            request.setAttribute("workloadList", workloadList);
            request.setAttribute("workloadAnalytics", workloadAnalytics);
            request.setAttribute("accountabilityList", accountabilityList);
            request.setAttribute("staleBlockerReport", staleBlockerReport);
            request.setAttribute("rebalanceSuggestions", rebalanceSuggestions);

            // Chuỗi JSON nhúng sẵn để Chart.js khởi tạo tức thì không cần đợi vòng gọi AJAX thứ hai
            request.setAttribute("burndownJson", burndownData.toJson());
            request.setAttribute("qualityJson", qualityMetrics.toJson());
            request.setAttribute("staleBlockerJson", staleBlockerReport.toJson());

            StringBuilder workloadsJsonSb = new StringBuilder("[");
            for (int i = 0; i < workloadAnalytics.size(); i++) {
                if (i > 0) workloadsJsonSb.append(",");
                workloadsJsonSb.append(workloadAnalytics.get(i).toJson());
            }
            workloadsJsonSb.append("]");
            request.setAttribute("workloadsJson", workloadsJsonSb.toString());

            StringBuilder accountabilityJsonSb = new StringBuilder("[");
            for (int i = 0; i < accountabilityList.size(); i++) {
                if (i > 0) accountabilityJsonSb.append(",");
                accountabilityJsonSb.append(accountabilityList.get(i).toJson());
            }
            accountabilityJsonSb.append("]");
            request.setAttribute("accountabilityJson", accountabilityJsonSb.toString());

            StringBuilder rebalanceJsonSb = new StringBuilder("[");
            for (int i = 0; i < rebalanceSuggestions.size(); i++) {
                if (i > 0) rebalanceJsonSb.append(",");
                rebalanceJsonSb.append(rebalanceSuggestions.get(i).toJson());
            }
            rebalanceJsonSb.append("]");
            request.setAttribute("rebalanceJson", rebalanceJsonSb.toString());

            request.setAttribute("activeSubNav", "analytics");
            request.setAttribute("currentNav", "analytics");

            request.getRequestDispatcher("/project-analytics.jsp").forward(request, response);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xử lý dữ liệu ProjectAnalyticsServlet cho Project ID: " + projectId, e);
            flash(request, "toastError", "Đã xảy ra lỗi khi tạo báo cáo phân tích dự án: " + e.getMessage());
            redirect(request, response, "/project?action=report&projectId=" + projectId);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJsonDirect(response, false, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            return;
        }

        String action = request.getParameter("action");
        if ("applyRebalance".equalsIgnoreCase(action)) {
            int projectId = intParam(request, "projectId", 0);
            int taskId = intParam(request, "taskId", 0);
            int newAssigneeId = intParam(request, "newAssigneeId", 0);

            Project project = ProjectDB.selectById(projectId);
            if (project == null || !com.teamwork.controllers.task.TaskAccess.canCreateTask(user, project)) {
                writeJsonDirect(response, false, "Bạn không có quyền Trưởng Dự Án (PM) để thực hiện điều phối san tải!");
                return;
            }

            Task task = TaskDB.selectById(taskId);
            if (task == null || task.getProjectId() != projectId) {
                writeJsonDirect(response, false, "Không tìm thấy công việc cần san tải trong dự án!");
                return;
            }

            User newAssignee = UserDB.selectById(newAssigneeId);
            if (newAssignee == null) {
                writeJsonDirect(response, false, "Không tìm thấy thành viên đích để chuyển giao công việc!");
                return;
            }

            int oldAssigneeId = task.getAssigneeId();
            String oldAssigneeName = task.getAssigneeName();

            boolean success = TaskDB.updateAssignee(taskId, newAssigneeId);
            if (success) {
                // Bắn thông báo cho người mới nhận
                NotificationDB.send(
                    newAssigneeId,
                    "Điều Phối San Tải Công Việc",
                    "Trưởng Dự Án vừa phân bổ công việc [" + task.getTitle() + "] cho bạn nhằm cân bằng tải cho đội ngũ.",
                    "/task?action=list&projectId=" + projectId + "#task-" + taskId,
                    "TASK"
                );

                // Bắn thông báo cho người cũ nếu có
                if (oldAssigneeId > 0 && oldAssigneeId != newAssigneeId) {
                    NotificationDB.send(
                        oldAssigneeId,
                        "Điều Phối Giảm Tải Công Việc",
                        "Công việc [" + task.getTitle() + "] đã được Trưởng Dự Án chuyển giao sang cho " + newAssignee.getFullName() + " để giúp bạn cân đối khối lượng làm việc.",
                        "/task?action=list&projectId=" + projectId,
                        "TASK"
                    );
                }

                // Ghi nhận Activity Log
                ActivityLogDB.logAsync(projectId, user.getId(), "REBALANCE_TASK", "TASK", taskId, task.getTitle(),
                    "Điều phối san tải: Chuyển công việc từ " + oldAssigneeName + " sang " + newAssignee.getFullName());

                writeJsonDirect(response, true, "Đã điều phối san tải thành công công việc [" + task.getTitle() + "] sang cho " + newAssignee.getFullName() + "!");
            } else {
                writeJsonDirect(response, false, "Không thể cập nhật người phụ trách trong cơ sở dữ liệu.");
            }
            return;
        }

        doGet(request, response);
    }

    private static void writeJsonDirect(HttpServletResponse response, boolean success, String message) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("{\"success\":" + success + ",\"message\":\"" + escapeJson(message) + "\"}");
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("<", "\\u003c")
                .replace(">", "\\u003e")
                .replace("&", "\\u0026");
    }
}
