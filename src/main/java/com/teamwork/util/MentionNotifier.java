package com.teamwork.util;

import com.teamwork.business.ProjectMember;
import com.teamwork.business.User;
import com.teamwork.data.NotificationDB;
import com.teamwork.data.ProjectMemberDB;
import com.teamwork.data.UserDB;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Gửi thông báo 🔔 cho những người được @nhắc tên trong tin nhắn chat / bình luận công việc.
 *
 * <p>Chỉ thành viên của CHÍNH dự án đó mới được nhắc (người ngoài không nhận được nội dung dự án);
 * người viết tự nhắc mình thì không gửi.</p>
 */
public final class MentionNotifier {

    private MentionNotifier() {}

    /** Độ dài tối đa đoạn trích tin nhắn trong thông báo. */
    static final int SNIPPET = 120;

    static String snippet(String content) {
        String s = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        return s.length() <= SNIPPET ? s : s.substring(0, SNIPPET - 1) + "…";
    }

    /**
     * @param taskId    0 = kênh chat chung của dự án; > 0 = bình luận trong công việc đó
     * @param taskTitle tên công việc (khi taskId > 0) để ghi vào thông báo
     * @return số người đã được gửi thông báo
     */
    public static int notifyMentions(int projectId, User author, String content, int taskId, String taskTitle) {
        if (projectId <= 0 || author == null || content == null || content.indexOf('@') < 0) return 0;

        List<Integer> memberIds = new ArrayList<>();
        for (ProjectMember m : ProjectMemberDB.selectByProjectId(projectId)) {
            memberIds.add(m.getUserId());
        }
        Set<Integer> mentioned = MentionParser.findMentionedUserIds(content, UserDB.selectByIds(memberIds));
        mentioned.remove(author.getId());
        if (mentioned.isEmpty()) return 0;

        String where = taskId > 0
                ? "bình luận công việc [" + (taskTitle == null ? "#" + taskId : taskTitle) + "]"
                : "kênh thảo luận của dự án";
        String link = taskId > 0
                ? "/task?action=list&projectId=" + projectId
                : "/chat?action=view&projectId=" + projectId;
        String body = author.getFullName() + " đã nhắc bạn trong " + where + ": \"" + snippet(content) + "\"";
        for (int userId : mentioned) {
            // Kiểu COMMENT: cột type là enum trong DB (INVITE, TASK_ASSIGNED, PROGRESS, COMMENT, GENERAL)
            NotificationDB.send(userId, "Bạn được nhắc tên", body, link, "COMMENT");
        }
        return mentioned.size();
    }
}
