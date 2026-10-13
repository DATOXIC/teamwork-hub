package com.teamwork.business.gantt;

import com.teamwork.business.Task;
import com.teamwork.business.TaskDependency;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Động cơ tự động dời lịch dây chuyền (Cascade Auto-Scheduling Engine).
 * 
 * Khi một công việc tiên quyết (Predecessor) bị trễ hạn hoặc được kéo dãn ngày hoàn thành (dueDate),
 * động cơ này sẽ tự động duyệt xuôi theo đồ thị phụ thuộc (Topological / BFS queue)
 * và đẩy ngày bắt đầu (startDate) cũng như ngày kết thúc (dueDate) của các công việc phụ thuộc (Successors)
 * xuôi về tương lai để bảo toàn tính toàn vẹn quan hệ Finish-to-Start và khoảng cách trễ (lagDays).
 */
public class CascadeScheduler {

    private static final Logger LOGGER = Logger.getLogger(CascadeScheduler.class.getName());

    /**
     * Tự động dời lịch dây chuyền khi công việc gốc (rootTaskId) thay đổi dueDate.
     * 
     * @param rootTaskId ID công việc thay đổi
     * @param newDueDate Ngày hoàn thành mới của công việc thay đổi
     * @param allTasks Danh sách toàn bộ các task trong dự án
     * @param dependencies Danh sách quan hệ phụ thuộc trong dự án
     * @return Danh sách các Task đã bị thay đổi ngày (bao gồm cả rootTask và các successor bị đẩy dời)
     */
    public static List<Task> cascade(int rootTaskId, LocalDate newDueDate,
                                    Collection<Task> allTasks,
                                    Collection<TaskDependency> dependencies) {
        return cascade(rootTaskId, null, newDueDate, allTasks, dependencies);
    }

    /**
     * Tự động dời lịch dây chuyền khi công việc gốc (rootTaskId) thay đổi startDate và/hoặc dueDate.
     * 
     * @param rootTaskId ID công việc thay đổi
     * @param newStartDate Ngày bắt đầu mới (có thể null nếu chỉ đổi dueDate)
     * @param newDueDate Ngày hoàn thành mới (có thể null nếu chỉ đổi startDate)
     * @param allTasks Danh sách toàn bộ các task trong dự án
     * @param dependencies Danh sách quan hệ phụ thuộc trong dự án
     * @return Danh sách các Task đã bị thay đổi ngày
     */
    public static List<Task> cascade(int rootTaskId, LocalDate newStartDate, LocalDate newDueDate,
                                    Collection<Task> allTasks,
                                    Collection<TaskDependency> dependencies) {
        if (rootTaskId <= 0 || allTasks == null || allTasks.isEmpty()) {
            return new ArrayList<>();
        }

        // Tạo bản đồ tra cứu task nhanh theo ID
        Map<Integer, Task> taskMap = new HashMap<>();
        for (Task t : allTasks) {
            if (t != null) {
                taskMap.put(t.getId(), t);
            }
        }

        Task rootTask = taskMap.get(rootTaskId);
        if (rootTask == null) {
            return new ArrayList<>();
        }

        // Tạo bản đồ phụ thuộc:
        // outgoing: Predecessor -> Danh sách các dependency mà Predecessor là dependsOnTaskId
        // incoming: Successor -> Danh sách các dependency mà Successor là taskId
        Map<Integer, List<TaskDependency>> outgoing = new HashMap<>();
        Map<Integer, List<TaskDependency>> incoming = new HashMap<>();

        if (dependencies != null) {
            for (TaskDependency dep : dependencies) {
                if (dep != null) {
                    outgoing.computeIfAbsent(dep.getDependsOnTaskId(), k -> new ArrayList<>()).add(dep);
                    incoming.computeIfAbsent(dep.getTaskId(), k -> new ArrayList<>()).add(dep);
                }
            }
        }

        // Áp dụng ngày mới cho rootTask
        boolean rootChanged = false;
        if (newStartDate != null && !newStartDate.toString().equals(rootTask.getStartDate())) {
            rootTask.setStartDate(newStartDate.toString());
            rootChanged = true;
        }
        if (newDueDate != null && !newDueDate.toString().equals(rootTask.getDueDate())) {
            rootTask.setDueDate(newDueDate.toString());
            rootChanged = true;
        }

        Set<Integer> modifiedTaskIds = new HashSet<>();
        if (rootChanged) {
            modifiedTaskIds.add(rootTaskId);
        }

        // Hàng đợi lan truyền các task có ngày thay đổi
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(rootTaskId);

        // Giới hạn số bước lặp phòng ngừa lặp vô tận nếu đồ thị bị vòng
        int maxSteps = allTasks.size() * 3;
        int steps = 0;

        while (!queue.isEmpty() && steps++ < maxSteps) {
            int currentId = queue.poll();
            Task currentTask = taskMap.get(currentId);
            if (currentTask == null) continue;

            List<TaskDependency> outDeps = outgoing.get(currentId);
            if (outDeps == null || outDeps.isEmpty()) continue;

            for (TaskDependency dep : outDeps) {
                int successorId = dep.getTaskId();
                Task successorTask = taskMap.get(successorId);
                if (successorTask == null) continue;

                // Tính toán ngày bắt đầu sớm nhất có thể của successorTask
                // dựa trên TẤT CẢ các predecessor của nó
                LocalDate maxRequiredStart = calculateEarliestStartDate(successorId, incoming.get(successorId), taskMap);
                if (maxRequiredStart == null) continue;

                LocalDate succStart = parseDateSafe(successorTask.getEffectiveStartDate(), LocalDate.now());
                LocalDate succDue = parseDateSafe(successorTask.getDueDate(), succStart.plusDays(3));

                long duration = ChronoUnit.DAYS.between(succStart, succDue);
                if (duration < 1) duration = 1;

                // Nếu ngày bắt đầu hiện tại của successor vi phạm (nhỏ hơn ngày yêu cầu sớm nhất)
                if (succStart.isBefore(maxRequiredStart)) {
                    LocalDate newSuccStart = maxRequiredStart;
                    LocalDate newSuccDue = newSuccStart.plusDays(duration);

                    successorTask.setStartDate(newSuccStart.toString());
                    successorTask.setDueDate(newSuccDue.toString());

                    modifiedTaskIds.add(successorId);
                    queue.add(successorId); // Tiếp tục lan truyền tới các việc phụ thuộc sau
                }
            }
        }

        List<Task> result = new ArrayList<>();
        for (int id : modifiedTaskIds) {
            Task t = taskMap.get(id);
            if (t != null) {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * Tính ngày bắt đầu sớm nhất của một task dựa trên tất cả các task tiên quyết của nó.
     */
    private static LocalDate calculateEarliestStartDate(int taskId,
                                                         List<TaskDependency> inDeps,
                                                         Map<Integer, Task> taskMap) {
        if (inDeps == null || inDeps.isEmpty()) {
            return null;
        }

        LocalDate maxStart = null;
        for (TaskDependency dep : inDeps) {
            Task predTask = taskMap.get(dep.getDependsOnTaskId());
            if (predTask == null) continue;

            LocalDate predDue = parseDateSafe(predTask.getDueDate(), null);
            if (predDue == null) {
                predDue = parseDateSafe(predTask.getEffectiveStartDate(), LocalDate.now());
            }

            // Khoảng cách ngày (Finish-to-Start):
            // Nếu lagDays > 0: cộng lagDays ngày.
            // Nếu lagDays == 0: mặc định sang ngày tiếp theo (1 ngày) để việc trước kết thúc trọn vẹn.
            int lag = dep.getLagDays() > 0 ? dep.getLagDays() : 1;
            LocalDate candidateStart = predDue.plusDays(lag);

            if (maxStart == null || candidateStart.isAfter(maxStart)) {
                maxStart = candidateStart;
            }
        }

        return maxStart;
    }

    private static LocalDate parseDateSafe(String dateStr, LocalDate fallback) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return fallback;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
