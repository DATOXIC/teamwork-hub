package com.teamwork.controllers;

import com.teamwork.business.Task;
import com.teamwork.business.TaskSubmission;
import com.teamwork.business.User;
import com.teamwork.data.TaskDB;
import com.teamwork.data.TaskSubmissionDB;
import com.teamwork.util.DeliverableStorage;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tải tệp bàn giao của công việc: GET /task-file?taskId=...[&submissionId=...]
 *
 * <p>Tệp nằm ngoài webapp (xem {@link DeliverableStorage}) nên chỉ lấy được qua servlet này,
 * và servlet chỉ trả tệp cho THÀNH VIÊN của dự án chứa công việc (người ngoài nhận 403).</p>
 *
 * <p>Hỗ trợ đa phiên bản: Nếu truyền {@code submissionId}, servlet tải đúng tệp đính kèm của đợt nộp đó;
 * nếu không truyền, servlet mặc định tải tệp từ công việc (tương thích ngược hoàn toàn).</p>
 *
 * <p>Luôn trả dạng tệp đính kèm (Content-Disposition: attachment) + nosniff (SecurityHeadersFilter):
 * trình duyệt không mở / chạy nội dung tệp ngay trong trang của app.</p>
 */
@WebServlet("/task-file")
public class TaskFileServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = currentUser(request);
        if (currentUser == null) {
            redirect(request, response, "/auth?action=viewLogin");
            return;
        }

        Task task = TaskDB.selectById(intParam(request, "taskId", 0));
        if (task == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy công việc.");
            return;
        }
        if (!ProjectAccess.isMember(currentUser, task.getProjectId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không thuộc dự án của công việc này.");
            return;
        }

        int submissionId = intParam(request, "submissionId", 0);
        String stored;

        if (submissionId > 0) {
            TaskSubmission sub = TaskSubmissionDB.selectById(submissionId);
            if (sub == null || sub.getTaskId() != task.getId()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy đợt nộp bàn giao tương ứng.");
                return;
            }
            stored = sub.getFilePath();
        } else {
            stored = task.getDeliverableFile();
        }

        if (stored == null || stored.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Đợt nộp này không có tệp đính kèm.");
            return;
        }

        Path file = DeliverableStorage.resolve(task.getId(), stored);
        if (file == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Tệp bàn giao không còn trên máy chủ.");
            return;
        }

        String fileName = DeliverableStorage.displayName(stored);
        // filename*: tên có dấu tiếng Việt (RFC 5987); filename: bản ASCII cho trình duyệt cũ
        String ascii = fileName.replaceAll("[^A-Za-z0-9._()\\- ]", "_");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(DeliverableStorage.contentType(stored));
        response.setContentLengthLong(Files.size(file));
        response.setHeader("Content-Disposition", "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded);
        response.setHeader("Cache-Control", "private, no-store");

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(file, out);
        }
    }
}
