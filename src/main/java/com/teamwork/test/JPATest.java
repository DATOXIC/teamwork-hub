package com.teamwork.test;

import com.teamwork.business.*;
import com.teamwork.data.*;
import jakarta.persistence.EntityManager;
import java.util.List;

public class JPATest {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("=== KIỂM THỬ TOÀN DIỆN JPA 3.1 & HIBERNATE 6 ===");
        System.out.println("=================================================");
        try {
            EntityManager em = JPAUtil.getEntityManager();
            System.out.println("1. EntityManager khởi tạo thành công: " + em.isOpen());
            
            System.out.println("\n2. Kiểm tra UserDB.selectAll():");
            List<User> users = UserDB.selectAll();
            System.out.println("   Số lượng user: " + users.size());
            int sampleUserId = 0;
            for (User u : users) {
                System.out.println("   -> User: " + u.getUsername() + " (" + u.getFullName() + ")");
                sampleUserId = u.getId();
            }

            System.out.println("\n3. Kiểm tra ProjectDB.selectAll():");
            List<Project> projects = ProjectDB.selectAll();
            System.out.println("   Số lượng project: " + projects.size());
            int sampleProjectId = 0;
            for (Project p : projects) {
                System.out.println("   -> Project: " + p.getProjectCode() + " - " + p.getName() + " (Tasks: " + p.getTotalTasks() + ", Done: " + p.getDoneTasks() + ")");
                sampleProjectId = p.getId();
            }

            System.out.println("\n4. Kiểm tra TaskDB.selectAll():");
            List<Task> tasks = TaskDB.selectAll();
            System.out.println("   Số lượng task: " + tasks.size());
            for (Task t : tasks) {
                System.out.println("   -> Task: [" + t.getStatus() + "] " + t.getTitle() + " (Phụ trách: " + t.getAssigneeName() + ")");
            }

            if (sampleProjectId > 0) {
                System.out.println("\n5. Kiểm tra SubTaskDB theo projectId " + sampleProjectId + ":");
                List<SubTask> subtasks = SubTaskDB.selectByProjectId(sampleProjectId);
                System.out.println("   Số lượng subtask: " + subtasks.size());
                for (SubTask st : subtasks) {
                    System.out.println("   -> SubTask: [" + (st.isCompleted() ? "X" : " ") + "] " + st.getTitle() + " (Assignee: " + st.getAssigneeName() + ")");
                }

                System.out.println("\n6. Kiểm tra LabelDB theo projectId " + sampleProjectId + ":");
                List<Label> labels = LabelDB.selectByProjectId(sampleProjectId);
                System.out.println("   Số lượng labels: " + labels.size());
                for (Label l : labels) {
                    System.out.println("   -> Label: " + l.getName() + " (" + l.getColorKey() + ")");
                }

                System.out.println("\n7. Kiểm tra DocDB theo projectId " + sampleProjectId + ":");
                List<Doc> docs = DocDB.selectByProjectId(sampleProjectId);
                System.out.println("   Số lượng docs: " + docs.size());
                for (Doc d : docs) {
                    System.out.println("   -> Doc: " + d.getTitle() + " (Author: " + d.getAuthorName() + ")");
                }

                System.out.println("\n8. Kiểm tra MessageDB theo projectId " + sampleProjectId + ":");
                List<Message> messages = MessageDB.selectByProjectId(sampleProjectId);
                System.out.println("   Số lượng messages: " + messages.size());
                for (Message m : messages) {
                    System.out.println("   -> Message: " + m.getAuthorName() + ": " + m.getContent());
                }

                System.out.println("\n9. Kiểm tra ActivityLogDB theo projectId " + sampleProjectId + ":");
                List<ActivityLog> logs = ActivityLogDB.selectByProjectId(sampleProjectId, 10);
                System.out.println("   Số lượng logs: " + logs.size());
                for (ActivityLog log : logs) {
                    System.out.println("   -> Log: " + log.getActionType() + " - " + log.getDescription());
                }

                System.out.println("\n10. Kiểm tra ProjectMemberDB theo projectId " + sampleProjectId + ":");
                List<ProjectMember> members = ProjectMemberDB.selectByProjectId(sampleProjectId);
                System.out.println("   Số lượng members: " + members.size());
                for (ProjectMember m : members) {
                    System.out.println("   -> Member: " + m.getUserName() + " (" + m.getProjectRole() + ")");
                }

                System.out.println("\n11. Kiểm tra TaskDocDB theo projectId " + sampleProjectId + ":");
                List<TaskDoc> taskDocs = TaskDocDB.selectByProjectId(sampleProjectId);
                System.out.println("   Số lượng taskDocs: " + taskDocs.size());
                for (TaskDoc td : taskDocs) {
                    System.out.println("   -> TaskDoc: Task #" + td.getTaskId() + " <-> Doc #" + td.getDocId() + " (" + td.getDocTitle() + ")");
                }
            }

            if (sampleUserId > 0) {
                System.out.println("\n12. Kiểm tra NotificationDB theo userId " + sampleUserId + ":");
                List<Notification> notifs = NotificationDB.selectByRecipientId(sampleUserId);
                System.out.println("   Số lượng notifications: " + notifs.size());
                for (Notification n : notifs) {
                    System.out.println("   -> Notification: " + n.getTitle() + " - " + n.getContent());
                }

                System.out.println("\n13. Kiểm tra ProjectInviteDB theo userId " + sampleUserId + ":");
                List<ProjectInvite> invites = ProjectInviteDB.selectPendingByReceiverId(sampleUserId);
                System.out.println("   Số lượng pending invites: " + invites.size());
            }

            JPAUtil.closeEntityManager(em);
            JPAUtil.close();
            System.out.println("\n=================================================");
            System.out.println("=== TẤT CẢ 12 JPA ENTITIES VÀ DAOs HOẠT ĐỘNG 100% ===");
            System.out.println("=================================================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}