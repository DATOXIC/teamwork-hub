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
import com.teamwork.data.ProjectDB;
import com.teamwork.data.SubTaskDB;
import com.teamwork.data.TaskDB;
import com.teamwork.data.UserDB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
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
import static com.teamwork.controllers.task.TaskBoardHandler.*;
import static com.teamwork.controllers.task.TaskCrudHandler.*;
import static com.teamwork.controllers.task.SubTaskHandler.*;
import static com.teamwork.controllers.task.TaskWorkflowHandler.*;

/**
 * TaskServlet — Controller phụ trách Bảng Công Việc Kanban (Tasks Module).
 *
 * <p><b>Các luồng GET được xử lý:</b></p>
 * <ul>
 *   <li>GET /task?action=list&projectId=X  → Hiển thị Bảng Kanban 3 cột (TODO / IN_PROGRESS / DONE)</li>
 *   <li>GET /task?action=delete&taskId=X   → Xóa Task lớn (kiểm soát quyền Task Lead / PM)</li>
 * </ul>
 *
 * <p><b>Các luồng POST được xử lý:</b></p>
 * <ul>
 *   <li>action=add              → Thêm Task cha mới kèm đính kèm tài liệu</li>
 *   <li>action=updateStatus     → Cập nhật trạng thái khi kéo thả HTML5 Drag-and-Drop</li>
 *   <li>action=addSubTask       → Thêm Việc con (Sub-task) có kiểm soát quyền 3 bên</li>
 *   <li>action=toggleSubTask    → Tick ☑ hoàn thành Việc con</li>
 *   <li>action=deleteSubTask    → Xóa Việc con có kiểm soát quyền</li>
 *   <li>action=submitSubTask    → Thành viên nộp báo cáo Việc con kèm ghi chú</li>
 *   <li>action=approveSubTask   → Task Lead duyệt Đạt Việc con</li>
 *   <li>action=reviseSubTask    → Task Lead yêu cầu Cân Chỉnh Việc con</li>
 *   <li>action=rejectSubTask    → Task Lead Trả Về Việc con</li>
 *   <li>action=submitParentTask → Thành viên nộp bàn giao Task lớn lên PM</li>
 *   <li>action=pmApproveTask    → PM Nghiệm thu Task lớn: Duyệt Đạt</li>
 *   <li>action=pmReviseTask     → PM Nghiệm thu Task lớn: Cân Chỉnh</li>
 *   <li>action=pmRejectTask     → PM Nghiệm thu Task lớn: Trả Về</li>
 * </ul>
 *
 * <p><b>BẢN ĐỒ: action nào nằm ở file nào</b> (thư mục {@code controllers/task/}). TaskServlet chỉ điều phối,
 * logic nằm trong các handler:</p>
 * <pre>
 *   TaskBoardHandler    : list (hiện Kanban → tasks.jsp), exportCsv, delete
 *   TaskCrudHandler     : add, updateStatus, editTask, quickAddParentTask, createLabel
 *   SubTaskHandler      : addSubTask, toggleSubTask, editSubTask, deleteSubTask, quickAddSubTask,
 *                         submitSubTask, approveSubTask, reviseSubTask, rejectSubTask
 *   TaskWorkflowHandler : submitParentTask, submitPlanningRequest, pmApprovePlanning, pmRejectPlanning,
 *                         pmApproveTask, pmReviseTask, pmRejectTask
 *   TaskAccess / TaskJson : hàm dùng chung (kiểm tra quyền / trả JSON)
 * </pre>
 *
 * <p><b>Kiến trúc MVC:</b></p>
 * <pre>
 *   Browser → TaskServlet (Controller) → TaskDB / SubTaskDB / ProjectMemberDB (Model) → tasks.jsp (View)
 * </pre>
 *
 * <p><b>Mô hình Quality Gate 2 tầng (chế độ TEAM):</b></p>
 * <pre>
 *   Tầng 1: Task Lead / Task Assignee ← duyệt từng Sub-task (Việc con)
 *   Tầng 2: PM (Project Owner)        ← nghiệm thu Task cha tổng thể
 * </pre>
 *
 * <p><b>TODO — Điểm mở rộng phổ biến (Extension Points):</b></p>
 * <ul>
 *   <li>Thêm action mới vào switch trong {@code doPost} theo mẫu các action hiện có</li>
 *   <li>Thêm trường dữ liệu mới cho Task (ví dụ: estimated hours, sprint number)</li>
 *   <li>Thêm bộ lọc/sắp xếp Task theo priority hoặc deadline trong {@code showKanbanBoard}</li>
 *   <li>Thêm rule kiểm tra business khi đổi trạng thái (ví dụ: không cho DONE nếu còn sub-task chưa xong)</li>
 *   <li>Tích hợp gửi email notification khi Task thay đổi trạng thái</li>
 * </ul>
 */
@WebServlet("/task")
// Nhận form có tệp (multipart): tệp bàn giao của "submitParentTask" (xem DeliverableStorage). Giới hạn 20 MB / tệp.
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 20L * 1024 * 1024, maxRequestSize = 25L * 1024 * 1024)
public class TaskServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Xác định ID của Project từ URL
        String projectIdParam = request.getParameter("projectId");
        int projectId = 0;

        if (projectIdParam == null || projectIdParam.trim().isEmpty()) {
            // Tối ưu hóa Cookie: Đọc dự án truy cập gần nhất để vào thẳng mà không cần query lại
            HttpSession session = request.getSession(false);
            User currentUser = currentUser(request);

            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                    if ("last_project_id".equals(c.getName()) && c.getValue() != null && !c.getValue().trim().isEmpty()) {
                        try {
                            int cachedProjectId = Integer.parseInt(c.getValue().trim());
                            // Kiểm tra an toàn: Dự án hợp lệ VÀ người dùng hiện tại là thành viên dự án
                            if (ProjectAccess.isMember(currentUser, cachedProjectId)) {
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
        User currentUser = currentUser(request);

        // TASK-01: Bắt buộc đăng nhập — nếu chưa có Session, redirect về trang login
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=viewLogin");
            return;
        }

        if (!requireMember(request, response, currentUser, projectId, "Bạn không có quyền truy cập vào dự án này!")) {
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

            // "delete" chỉ nhận qua POST (doPost): link GET có thể bị kích hoạt từ trang khác (CSRF)

            case "exportCsv":
                handleExportCsv(request, response, projectId);
                break;

            case "timeline":
                response.sendRedirect(request.getContextPath() + "/timeline?projectId=" + projectId);
                return;

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
                User currentUser = currentUser(request);

                // TASK-01: Bắt buộc đăng nhập ngay trong doPost
                if (currentUser == null) {
                    response.sendRedirect(request.getContextPath() + "/auth?action=viewLogin");
                    return;
                }

                if (!requireMember(request, response, currentUser, projectId, "Bạn không có quyền thao tác trong dự án này!")) {
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

            case "createLabel":
                handleCreateLabel(request, response);
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

            case "delete":
                // handleDeleteTask tự kiểm tra task thuộc projectId và người xóa là PM / Task Lead
                handleDeleteTask(request, response, intParam(request, "projectId", 0));
                break;

            default:
                response.sendRedirect(request.getContextPath() + "/project?action=list");
                break;
        }
    }
}
