package com.teamwork.data;

import com.teamwork.business.TaskDoc;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng Data Access Object (DAO): Quản lý liên kết Nhiều-Nhiều Task ↔ Doc kết nối Supabase PostgreSQL.
 * 
 * Áp dụng nguyên tắc Backend Code Mastery & Database API Design:
 * - Bảo toàn 100% hợp đồng giao tiếp (Method Signatures & Return Types)
 * - Sử dụng JOIN bảng docs để lấy tiêu đề tài liệu (docTitle) chính xác
 * - Sử dụng PreparedStatement an toàn chống SQL Injection
 * - Quản lý tài nguyên bằng try-with-resources
 */
public class TaskDocDB {

    private static final Logger LOGGER = Logger.getLogger(TaskDocDB.class.getName());

    /**
     * HÀM 1: Lấy danh sách tất cả các tài liệu đính kèm của MỘT CÔNG VIỆC (Task)
     */
    public static List<TaskDoc> selectByTaskId(int taskId) {
        List<TaskDoc> resultList = new ArrayList<>();
        if (taskId <= 0) return resultList;

        String sql = "SELECT td.task_id, td.doc_id, d.title AS doc_title " +
                     "FROM task_docs td " +
                     "JOIN docs d ON td.doc_id = d.id " +
                     "WHERE td.task_id = ? " +
                     "ORDER BY d.id ASC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultList.add(new TaskDoc(
                        rs.getInt("task_id"),
                        rs.getInt("doc_id"),
                        rs.getString("doc_title")
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy TaskDoc theo Task ID: " + taskId, e);
        }
        return resultList;
    }

    /**
     * HÀM 2: Lấy danh sách ID của các Task đang tham chiếu đến MỘT TÀI LIỆU (Doc)
     */
    public static List<Integer> selectTaskIdsByDocId(int docId) {
        List<Integer> taskIds = new ArrayList<>();
        if (docId <= 0) return taskIds;

        String sql = "SELECT task_id FROM task_docs WHERE doc_id = ? ORDER BY task_id ASC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, docId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    taskIds.add(rs.getInt("task_id"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy taskIds theo Doc ID: " + docId, e);
        }
        return taskIds;
    }

    /**
     * HÀM 3: Thêm một liên kết mới giữa Task và Doc
     */
    public static boolean insert(int taskId, int docId, String docTitle) {
        if (taskId <= 0 || docId <= 0) return false;

        String sql = "INSERT INTO task_docs (task_id, doc_id) VALUES (?, ?) ON CONFLICT DO NOTHING";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.setInt(2, docId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi chèn TaskDoc", e);
        }
        return false;
    }

    /**
     * HÀM 4: Xóa toàn bộ liên kết đính kèm của một Task
     */
    public static void deleteByTaskId(int taskId) {
        if (taskId <= 0) return;

        String sql = "DELETE FROM task_docs WHERE task_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa TaskDoc theo Task ID: " + taskId, e);
        }
    }

    /**
     * HÀM 5: Xóa toàn bộ liên kết của một Doc
     */
    public static void deleteByDocId(int docId) {
        if (docId <= 0) return;

        String sql = "DELETE FROM task_docs WHERE doc_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, docId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa TaskDoc theo Doc ID: " + docId, e);
        }
    }
}
