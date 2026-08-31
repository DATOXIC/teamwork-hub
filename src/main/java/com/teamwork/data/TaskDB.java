package com.teamwork.data;

import com.teamwork.business.Task;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý dữ liệu Thẻ công việc (Task) kết nối Supabase PostgreSQL.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng LEFT JOIN users để lấy tên người phụ trách (assigneeName) chính xác
 * - Hỗ trợ đầy đủ các cổng phê duyệt 2 tầng (Planning Gate & Deliverable Review)
 * - Sử dụng PreparedStatement an toàn chống SQL Injection
 * - Quản lý tài nguyên bằng try-with-resources
 */
public class TaskDB {

    private static final Logger LOGGER = Logger.getLogger(TaskDB.class.getName());

    /**
     * Ánh xạ 1 dòng ResultSet sang đối tượng JavaBean Task
     */
    private static Task mapResultSetToTask(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int projectId = rs.getInt("project_id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        String status = rs.getString("status");
        String priority = rs.getString("priority");
        String dueDate = rs.getString("due_date_str");
        int assigneeId = rs.getInt("assignee_id");
        String assigneeName = rs.getString("assignee_name");
        String finalDeliverableNote = rs.getString("final_deliverable_note");
        String pmFeedback = rs.getString("pm_feedback");
        String submittedAt = rs.getString("submitted_at_str");
        String reviewedAt = rs.getString("reviewed_at_str");
        String deliverableFile = rs.getString("deliverable_file");
        int qualityRating = rs.getInt("quality_rating");
        String planningNote = rs.getString("planning_note");
        String planningReviewedAt = rs.getString("planning_reviewed_at_str");
        String labels = rs.getString("labels");

        Task task = new Task(
            id,
            projectId,
            title != null ? title : "",
            description != null ? description : "",
            status != null ? status : "TODO",
            priority != null ? priority : "MEDIUM",
            dueDate != null ? dueDate : "",
            assigneeId,
            assigneeName != null && !assigneeName.trim().isEmpty() ? assigneeName : "Chưa phân công",
            finalDeliverableNote != null ? finalDeliverableNote : "",
            pmFeedback != null ? pmFeedback : "",
            submittedAt != null ? submittedAt : "",
            reviewedAt != null ? reviewedAt : "",
            deliverableFile != null ? deliverableFile : "",
            qualityRating > 0 ? qualityRating : 5,
            planningNote != null ? planningNote : "",
            planningReviewedAt != null ? planningReviewedAt : ""
        );
        task.setLabels(labels != null ? labels : "");
        return task;
    }

    private static final String BASE_SELECT_SQL =
        "SELECT t.id, t.project_id, t.title, t.description, t.status::text AS status, t.priority::text AS priority, " +
        "       to_char(t.due_date, 'YYYY-MM-DD') AS due_date_str, " +
        "       t.assignee_id, COALESCE(u.full_name, 'Chưa phân công') AS assignee_name, " +
        "       t.final_deliverable_note, t.pm_feedback, " +
        "       to_char(t.submitted_at, 'DD/MM/YYYY HH24:MI') AS submitted_at_str, " +
        "       to_char(t.reviewed_at, 'DD/MM/YYYY HH24:MI') AS reviewed_at_str, " +
        "       t.deliverable_file, t.quality_rating, " +
        "       t.planning_note, " +
        "       to_char(t.planning_reviewed_at, 'DD/MM/YYYY HH24:MI') AS planning_reviewed_at_str, " +
        "       t.labels " +
        "FROM tasks t " +
        "LEFT JOIN users u ON t.assignee_id = u.id ";

    /**
     * HÀM 1: Lấy toàn bộ task trong hệ thống
     */
    public static List<Task> selectAll() {
        List<Task> list = new ArrayList<>();
        String sql = BASE_SELECT_SQL + "ORDER BY t.id ASC";

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy toàn bộ Task", e);
        }
        return list;
    }

    /**
     * HÀM 2: Lấy danh sách tất cả các task thuộc về MỘT DỰ ÁN cụ thể
     */
    public static List<Task> selectByProjectId(int projectId) {
        List<Task> list = new ArrayList<>();
        if (projectId <= 0) return list;

        String sql = BASE_SELECT_SQL + "WHERE t.project_id = ? ORDER BY t.id ASC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy Task theo Project ID: " + projectId, e);
        }
        return list;
    }

    /**
     * HÀM 3: Lấy danh sách task của một dự án ĐƯỢC LỌC THEO 3 CỘT KANBAN:
     * - TODO: Task có trạng thái TODO
     * - IN_PROGRESS: PLANNING, IN_PROGRESS, SUBMITTED, REVISE, REJECTED
     * - DONE: DONE, APPROVED
     */
    public static List<Task> selectByProjectAndStatus(int projectId, String status) {
        List<Task> list = new ArrayList<>();
        if (projectId <= 0 || status == null) return list;

        String sql;
        if ("TODO".equalsIgnoreCase(status)) {
            sql = BASE_SELECT_SQL + "WHERE t.project_id = ? AND t.status = 'TODO' ORDER BY t.id ASC";
        } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            sql = BASE_SELECT_SQL + "WHERE t.project_id = ? AND t.status IN ('PLANNING', 'IN_PROGRESS', 'SUBMITTED', 'REVISE', 'REJECTED') ORDER BY t.id ASC";
        } else if ("DONE".equalsIgnoreCase(status)) {
            sql = BASE_SELECT_SQL + "WHERE t.project_id = ? AND t.status IN ('DONE', 'APPROVED') ORDER BY t.id ASC";
        } else {
            sql = BASE_SELECT_SQL + "WHERE t.project_id = ? AND t.status::text = ? ORDER BY t.id ASC";
        }

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);
            if (!"TODO".equalsIgnoreCase(status) && !"IN_PROGRESS".equalsIgnoreCase(status) && !"DONE".equalsIgnoreCase(status)) {
                ps.setString(2, status.trim().toUpperCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lọc Task theo Status", e);
        }
        return list;
    }

    /**
     * HÀM 4: Tìm task theo ID duy nhất
     */
    public static Task selectById(int id) {
        if (id <= 0) return null;

        String sql = BASE_SELECT_SQL + "WHERE t.id = ? LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTask(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tìm Task ID: " + id, e);
        }
        return null;
    }

    /**
     * HÀM 5: Thêm task mới
     */
    public static int insert(Task task) {
        if (task == null || task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return 0;
        }

        String sql = "INSERT INTO tasks (project_id, title, description, status, priority, due_date, assignee_id, labels, " +
                     "final_deliverable_note, pm_feedback, deliverable_file, quality_rating, planning_note, created_at) " +
                     "VALUES (?, ?, ?, ?::task_status_enum, ?::priority_enum, ?, ?, ?, ?, ?, ?, ?, ?, NOW()) RETURNING id";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, task.getProjectId());
            ps.setString(2, task.getTitle().trim());
            ps.setString(3, task.getDescription() != null ? task.getDescription().trim() : "");
            ps.setString(4, task.getStatus() != null && !task.getStatus().trim().isEmpty() ? task.getStatus().trim().toUpperCase() : "TODO");
            ps.setString(5, task.getPriority() != null && !task.getPriority().trim().isEmpty() ? task.getPriority().trim().toUpperCase() : "MEDIUM");

            if (task.getDueDate() != null && !task.getDueDate().trim().isEmpty()) {
                try {
                    ps.setDate(6, Date.valueOf(task.getDueDate().trim()));
                } catch (IllegalArgumentException ex) {
                    ps.setNull(6, Types.DATE);
                }
            } else {
                ps.setNull(6, Types.DATE);
            }

            if (task.getAssigneeId() > 0) {
                ps.setInt(7, task.getAssigneeId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }

            ps.setString(8, task.getLabels() != null ? task.getLabels().trim() : "");
            ps.setString(9, task.getFinalDeliverableNote() != null ? task.getFinalDeliverableNote().trim() : "");
            ps.setString(10, task.getPmFeedback() != null ? task.getPmFeedback().trim() : "");
            ps.setString(11, task.getDeliverableFile() != null ? task.getDeliverableFile().trim() : "");
            ps.setInt(12, task.getQualityRating() > 0 ? task.getQualityRating() : 5);
            ps.setString(13, task.getPlanningNote() != null ? task.getPlanningNote().trim() : "");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int genId = rs.getInt(1);
                    task.setId(genId);
                    return genId;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi thêm Task mới: " + task.getTitle(), e);
        }
        return 0;
    }

    /**
     * HÀM 6: Cập nhật trạng thái Task (kéo thả Kanban)
     */
    public static boolean updateStatus(int id, String newStatus) {
        if (id <= 0 || newStatus == null || newStatus.trim().isEmpty()) return false;

        String sql = "UPDATE tasks SET status = ?::task_status_enum, updated_at = NOW() WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus.trim().toUpperCase());
            ps.setInt(2, id);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật status Task ID: " + id, e);
        }
        return false;
    }

    /**
     * HÀM 7: Task Lead Bàn Giao & Nộp Báo Cáo Task Kèm Tệp Đính Kèm ➔ SUBMITTED
     */
    public static boolean submitTaskDeliverable(int taskId, String note, String deliverableFile, String submittedAt) {
        if (taskId <= 0) return false;

        String sql = "UPDATE tasks SET status = 'SUBMITTED', final_deliverable_note = ?, deliverable_file = ?, submitted_at = NOW(), updated_at = NOW() WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, note != null ? note.trim() : "");
            ps.setString(2, deliverableFile != null ? deliverableFile.trim() : "");
            ps.setInt(3, taskId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi submit deliverable Task ID: " + taskId, e);
        }
        return false;
    }

    public static boolean submitTaskDeliverable(int taskId, String note, String submittedAt) {
        return submitTaskDeliverable(taskId, note, "", submittedAt);
    }

    /**
     * HÀM 8: Trưởng Dự Án (PM) Phê Duyệt Nghiệm Thu ĐẠT Kèm Đánh Giá Sao ➔ DONE (100%)
     */
    public static boolean pmApproveTask(int taskId, String feedback, int qualityRating, String reviewedAt) {
        if (taskId <= 0) return false;

        String sql = "UPDATE tasks SET status = 'DONE', pm_feedback = ?, quality_rating = ?, reviewed_at = NOW(), updated_at = NOW() WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, feedback != null ? feedback.trim() : "PM đã phê duyệt nghiệm thu xuất sắc!");
            ps.setInt(2, qualityRating > 0 ? qualityRating : 5);
            ps.setInt(3, taskId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi PM duyệt Task ID: " + taskId, e);
        }
        return false;
    }

    public static boolean pmApproveTask(int taskId, String feedback, String reviewedAt) {
        return pmApproveTask(taskId, feedback, 5, reviewedAt);
    }

    /**
     * HÀM 9: Trưởng Dự Án (PM) Yêu Cầu Cân Chỉnh Nhỏ ➔ REVISE
     */
    public static boolean pmReviseTask(int taskId, String feedback, String reviewedAt) {
        if (taskId <= 0) return false;

        String sql = "UPDATE tasks SET status = 'REVISE', pm_feedback = ?, reviewed_at = NOW(), updated_at = NOW() WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, feedback != null ? feedback.trim() : "");
            ps.setInt(2, taskId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi PM yêu cầu revise Task ID: " + taskId, e);
        }
        return false;
    }

    /**
     * HÀM 10: Trưởng Dự Án (PM) Trả Về Do Chưa Đạt ➔ REJECTED
     */
    public static boolean pmRejectTask(int taskId, String feedback, String reviewedAt) {
        if (taskId <= 0) return false;

        String sql = "UPDATE tasks SET status = 'REJECTED', pm_feedback = ?, reviewed_at = NOW(), updated_at = NOW() WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, feedback != null ? feedback.trim() : "");
            ps.setInt(2, taskId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi PM reject Task ID: " + taskId, e);
        }
        return false;
    }

    /**
     * HÀM 11: Task Lead Trình Kế Hoạch Phân Rã (CỔNG 1) ➔ PLANNING
     */
    public static boolean submitPlanningRequest(int taskId, String planningNote, String submittedAt) {
        if (taskId <= 0) return false;

        String sql = "UPDATE tasks SET status = 'PLANNING', planning_note = ?, submitted_at = NOW(), updated_at = NOW() WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, planningNote != null ? planningNote.trim() : "");
            ps.setInt(2, taskId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi submit planning Task ID: " + taskId, e);
        }
        return false;
    }

    /**
     * HÀM 12: Trưởng Dự Án (PM) Phê Duyệt Kế Hoạch & KHÓA PHÂN RÃ ➔ IN_PROGRESS
     */
    public static boolean pmApprovePlanning(int taskId, String pmFeedback, String reviewedAt) {
        if (taskId <= 0) return false;

        String sql = "UPDATE tasks SET status = 'IN_PROGRESS', pm_feedback = ?, planning_reviewed_at = NOW(), updated_at = NOW() WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pmFeedback != null ? pmFeedback.trim() : "");
            ps.setInt(2, taskId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi PM approve planning Task ID: " + taskId, e);
        }
        return false;
    }

    /**
     * HÀM 13: Trưởng Dự Án (PM) Yêu Cầu Chỉnh Sửa Kế Hoạch Phân Rã ➔ TODO
     */
    public static boolean pmRejectPlanning(int taskId, String pmFeedback, String reviewedAt) {
        if (taskId <= 0) return false;

        String sql = "UPDATE tasks SET status = 'TODO', pm_feedback = ?, reviewed_at = NOW(), updated_at = NOW() WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pmFeedback != null ? pmFeedback.trim() : "");
            ps.setInt(2, taskId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi PM reject planning Task ID: " + taskId, e);
        }
        return false;
    }

    /**
     * HÀM 14: Cập nhật thông tin toàn diện của Task
     */
    public static boolean update(Task updatedTask) {
        if (updatedTask == null || updatedTask.getId() <= 0) return false;

        String sql = "UPDATE tasks SET title = ?, description = ?, status = ?::task_status_enum, priority = ?::priority_enum, " +
                     "due_date = ?, assignee_id = ?, labels = ?, updated_at = NOW() WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, updatedTask.getTitle() != null ? updatedTask.getTitle().trim() : "");
            ps.setString(2, updatedTask.getDescription() != null ? updatedTask.getDescription().trim() : "");
            ps.setString(3, updatedTask.getStatus() != null && !updatedTask.getStatus().trim().isEmpty() ? updatedTask.getStatus().trim().toUpperCase() : "TODO");
            ps.setString(4, updatedTask.getPriority() != null && !updatedTask.getPriority().trim().isEmpty() ? updatedTask.getPriority().trim().toUpperCase() : "MEDIUM");

            if (updatedTask.getDueDate() != null && !updatedTask.getDueDate().trim().isEmpty()) {
                try {
                    ps.setDate(5, Date.valueOf(updatedTask.getDueDate().trim()));
                } catch (IllegalArgumentException ex) {
                    ps.setNull(5, Types.DATE);
                }
            } else {
                ps.setNull(5, Types.DATE);
            }

            if (updatedTask.getAssigneeId() > 0) {
                ps.setInt(6, updatedTask.getAssigneeId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            ps.setString(7, updatedTask.getLabels() != null ? updatedTask.getLabels().trim() : "");
            ps.setInt(8, updatedTask.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi update Task ID: " + updatedTask.getId(), e);
        }
        return false;
    }

    /**
     * HÀM 15: Xóa task theo ID
     */
    public static boolean delete(int id) {
        if (id <= 0) return false;

        String sql = "DELETE FROM tasks WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa Task ID: " + id, e);
        }
        return false;
    }

    /**
     * HÀM 16: Hủy phân công Task lớn cho một thành viên khi rời nhóm
     */
    public static void unassignUserFromProject(int projectId, int userId) {
        if (projectId <= 0 || userId <= 0) return;

        String sql = "UPDATE tasks SET assignee_id = NULL, updated_at = NOW() WHERE project_id = ? AND assignee_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi unassign user", e);
        }
    }

    /**
     * HÀM 17: Đồng bộ tên người phụ trách (tự động xử lý qua JOIN bảng users trong DB)
     */
    public static void syncAssigneeName(int userId, String newFullName) {
        // Tự động đồng bộ qua JOIN users trong Database
    }
}
