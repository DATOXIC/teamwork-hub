package com.teamwork.controllers;

import com.teamwork.business.Project;
import com.teamwork.business.ProjectMember;
import com.teamwork.business.SubTask;
import com.teamwork.business.Task;
import com.teamwork.business.TaskSubmission;
import com.teamwork.business.User;
import com.teamwork.business.UserWorkload;
import com.teamwork.business.analytics.BurndownCalculator;
import com.teamwork.business.analytics.BurndownData;
import com.teamwork.business.analytics.QualityMetrics;
import com.teamwork.business.analytics.QualityVelocityCalculator;
import com.teamwork.business.analytics.WorkloadAnalytics;
import com.teamwork.controllers.task.TaskBoardHandler;
import com.teamwork.data.ProjectDB;
import com.teamwork.data.ProjectMemberDB;
import com.teamwork.data.SubTaskDB;
import com.teamwork.data.TaskDB;
import com.teamwork.data.TaskSubmissionDB;
import com.teamwork.data.UserDB;
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

            Map<Integer, List<SubTask>> taskSubTasksMap = new HashMap<>();
            for (SubTask st : allSubTasks) {
                taskSubTasksMap.computeIfAbsent(st.getTaskId(), k -> new ArrayList<>()).add(st);
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

            // 3. Tính toán khối lượng công việc (Workload)
            List<UserWorkload> workloadList = TaskBoardHandler.computeUserWorkloads(userList, allTasks, taskSubTasksMap);
            List<WorkloadAnalytics> workloadAnalytics = WorkloadAnalytics.fromWorkloads(workloadList);

            // 4. Động cơ tính toán POJO thuần túy: Burndown & Quality Velocity
            BurndownData burndownData = BurndownCalculator.calculate(project, allTasks);
            QualityMetrics qualityMetrics = QualityVelocityCalculator.calculate(allSubmissions, allTasks, workloadList, burndownData);

            // 5. Kiểm tra yêu cầu dữ liệu JSON (AJAX live refresh)
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
                jsonBuilder.append("\"workloads\":[");
                for (int i = 0; i < workloadAnalytics.size(); i++) {
                    if (i > 0) jsonBuilder.append(",");
                    jsonBuilder.append(workloadAnalytics.get(i).toJson());
                }
                jsonBuilder.append("]");
                jsonBuilder.append("}");

                writeJson(response, HttpServletResponse.SC_OK, jsonBuilder.toString());
                return;
            }

            // 6. Đóng gói Model cho View JSP
            request.setAttribute("project", project);
            request.setAttribute("burndownData", burndownData);
            request.setAttribute("qualityMetrics", qualityMetrics);
            request.setAttribute("workloadList", workloadList);
            request.setAttribute("workloadAnalytics", workloadAnalytics);

            // Chuỗi JSON nhúng sẵn để Chart.js khởi tạo tức thì không cần đợi vòng gọi AJAX thứ hai
            request.setAttribute("burndownJson", burndownData.toJson());
            request.setAttribute("qualityJson", qualityMetrics.toJson());

            StringBuilder workloadsJsonSb = new StringBuilder("[");
            for (int i = 0; i < workloadAnalytics.size(); i++) {
                if (i > 0) workloadsJsonSb.append(",");
                workloadsJsonSb.append(workloadAnalytics.get(i).toJson());
            }
            workloadsJsonSb.append("]");
            request.setAttribute("workloadsJson", workloadsJsonSb.toString());

            request.setAttribute("activeSubNav", "analytics");
            request.setAttribute("currentNav", "analytics");

            request.getRequestDispatcher("/project-analytics.jsp").forward(request, response);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xử lý dữ liệu ProjectAnalyticsServlet cho Project ID: " + projectId, e);
            flash(request, "toastError", "Đã xảy ra lỗi khi tạo báo cáo phân tích dự án: " + e.getMessage());
            redirect(request, response, "/project?action=report&projectId=" + projectId);
        }
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
