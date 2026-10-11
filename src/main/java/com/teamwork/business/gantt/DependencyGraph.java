package com.teamwork.business.gantt;

import com.teamwork.business.TaskDependency;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Động cơ phát hiện chu trình (Circular Dependency Detection) và phân tích đồ thị phụ thuộc công việc (DAG).
 * 
 * Quy ước hướng cạnh (Directed Edge):
 * Nếu Task B phụ thuộc vào Task A (B depends on A, A hoàn thành trước B),
 * ta biểu diễn cạnh có hướng A -> B (Predecessor -> Successor).
 * 
 * Kiểm tra chu trình khi thêm phụ thuộc "Task B phụ thuộc Task A":
 * - Cạnh mới thêm: A -> B.
 * - Chu trình xuất hiện nếu và chỉ nếu B đã có đường đi (path) tới A trong đồ thị hiện tại.
 * - Nếu B == A (tự phụ thuộc chính mình), luôn tạo chu trình.
 */
public class DependencyGraph {

    // Danh sách kề: Node A -> Danh sách các task phụ thuộc vào A (Successors của A)
    private final Map<Integer, Set<Integer>> adjacency = new HashMap<>();

    public DependencyGraph() {
    }

    public DependencyGraph(Collection<TaskDependency> dependencies) {
        if (dependencies != null) {
            for (TaskDependency dep : dependencies) {
                if (dep != null) {
                    addEdge(dep.getDependsOnTaskId(), dep.getTaskId());
                }
            }
        }
    }

    /**
     * Thêm cạnh có hướng: predecessor -> successor.
     */
    public void addEdge(int predecessorId, int successorId) {
        if (predecessorId <= 0 || successorId <= 0) return;
        adjacency.computeIfAbsent(predecessorId, k -> new HashSet<>()).add(successorId);
        adjacency.putIfAbsent(successorId, new HashSet<>());
    }

    /**
     * Kiểm tra xem việc thêm quan hệ "taskId phụ thuộc vào dependsOnTaskId"
     * (nghĩa là thêm cạnh dependsOnTaskId -> taskId) có gây ra chu trình hay không.
     * 
     * @param taskId Công việc chịu phụ thuộc (Successor)
     * @param dependsOnTaskId Công việc tiên quyết (Predecessor)
     * @return true nếu gây chu trình, false nếu hợp lệ (DAG)
     */
    public boolean wouldCauseCycle(int taskId, int dependsOnTaskId) {
        if (taskId <= 0 || dependsOnTaskId <= 0) return false;
        if (taskId == dependsOnTaskId) return true; // Tự phụ thuộc

        // Kiểm tra xem từ taskId đã có đường đi tới dependsOnTaskId hay chưa
        return hasPath(taskId, dependsOnTaskId);
    }

    /**
     * Kiểm tra tồn tại đường đi từ startNode tới targetNode bằng BFS.
     */
    public boolean hasPath(int startNode, int targetNode) {
        if (startNode == targetNode) return true;
        if (!adjacency.containsKey(startNode)) return false;

        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new ArrayDeque<>();

        visited.add(startNode);
        queue.add(startNode);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            Set<Integer> neighbors = adjacency.get(current);
            if (neighbors == null) continue;

            for (int next : neighbors) {
                if (next == targetNode) {
                    return true;
                }
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }

        return false;
    }

    /**
     * Kiểm tra xem toàn bộ đồ thị hiện tại có chứa chu trình bất kỳ hay không (Tarjan/DFS 3 màu).
     */
    public boolean hasAnyCycle() {
        // 0: Unvisited, 1: Visiting (trong call stack), 2: Visited (hoàn thành)
        Map<Integer, Integer> state = new HashMap<>();

        for (int node : adjacency.keySet()) {
            if (!state.containsKey(node)) {
                if (dfsDetectCycle(node, state)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dfsDetectCycle(int current, Map<Integer, Integer> state) {
        state.put(current, 1); // Đang duyệt nhánh này

        Set<Integer> neighbors = adjacency.get(current);
        if (neighbors != null) {
            for (int next : neighbors) {
                Integer s = state.get(next);
                if (s != null && s == 1) {
                    return true; // Gặp lại node đang trên call stack -> có chu trình!
                }
                if (s == null) {
                    if (dfsDetectCycle(next, state)) {
                        return true;
                    }
                }
            }
        }

        state.put(current, 2); // Đã duyệt xong
        return false;
    }

    /**
     * Sắp xếp tô-pô (Topological Sort) bằng thuật toán Kahn.
     * Trả về danh sách taskId theo thứ tự từ việc tiên quyết tới việc phụ thuộc sau.
     * Nếu đồ thị có chu trình, trả về danh sách rỗng.
     */
    public List<Integer> topologicalSort() {
        Map<Integer, Integer> inDegree = new HashMap<>();

        // Khởi tạo bán bậc vào (in-degree) cho mọi node
        for (int u : adjacency.keySet()) {
            inDegree.putIfAbsent(u, 0);
            for (int v : adjacency.get(u)) {
                inDegree.put(v, inDegree.getOrDefault(v, 0) + 1);
            }
        }

        Queue<Integer> zeroInDegreeQueue = new ArrayDeque<>();
        for (Map.Entry<Integer, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                zeroInDegreeQueue.add(entry.getKey());
            }
        }

        List<Integer> order = new ArrayList<>();
        while (!zeroInDegreeQueue.isEmpty()) {
            int u = zeroInDegreeQueue.poll();
            order.add(u);

            Set<Integer> neighbors = adjacency.get(u);
            if (neighbors != null) {
                for (int v : neighbors) {
                    int updated = inDegree.get(v) - 1;
                    inDegree.put(v, updated);
                    if (updated == 0) {
                        zeroInDegreeQueue.add(v);
                    }
                }
            }
        }

        if (order.size() < inDegree.size()) {
            // Có chu trình, không thể sắp xếp topo hợp lệ
            return Collections.emptyList();
        }

        return order;
    }

    /**
     * Lấy danh sách các task trực tiếp phụ thuộc vào taskId (Immediate Successors).
     */
    public Set<Integer> getDirectSuccessors(int taskId) {
        Set<Integer> s = adjacency.get(taskId);
        return s != null ? new HashSet<>(s) : Collections.emptySet();
    }

    /**
     * Hàm tiện ích tĩnh: kiểm tra nhanh quan hệ mới có gây chu trình không
     * dựa trên danh sách TaskDependency sẵn có.
     */
    public static boolean wouldCauseCycle(int taskId, int dependsOnTaskId, Collection<TaskDependency> existingDependencies) {
        DependencyGraph graph = new DependencyGraph(existingDependencies);
        return graph.wouldCauseCycle(taskId, dependsOnTaskId);
    }
}
