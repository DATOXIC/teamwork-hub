package com.teamwork.business;

import com.teamwork.business.gantt.DependencyGraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử thuật toán phát hiện chu trình phụ thuộc (Circular Dependency Detection)")
public class DependencyCycleTest {

    @Test
    @DisplayName("Tự phụ thuộc (Self Dependency) A -> A phải luôn phát hiện chu trình")
    void testSelfDependencyIsCycle() {
        DependencyGraph graph = new DependencyGraph();
        assertTrue(graph.wouldCauseCycle(1, 1), "Task không thể phụ thuộc vào chính nó");
    }

    @Test
    @DisplayName("Quan hệ đơn tuyến hợp lệ: A -> B không có chu trình")
    void testSimpleValidDependency() {
        DependencyGraph graph = new DependencyGraph();
        // Task 2 phụ thuộc Task 1 (1 -> 2)
        assertFalse(graph.wouldCauseCycle(2, 1), "Thêm 1 -> 2 vào đồ thị rỗng không tạo chu trình");
        graph.addEdge(1, 2);

        // Task 3 phụ thuộc Task 2 (2 -> 3)
        assertFalse(graph.wouldCauseCycle(3, 2), "Thêm 2 -> 3 không tạo chu trình");
    }

    @Test
    @DisplayName("Chu trình 2 đỉnh trực tiếp: A -> B rồi B -> A phải bị chặn")
    void testDirectTwoNodeCycle() {
        DependencyGraph graph = new DependencyGraph();
        // Task 2 phụ thuộc Task 1 (1 -> 2)
        graph.addEdge(1, 2);

        // Giờ thử thêm: Task 1 phụ thuộc Task 2 (2 -> 1) => Chu trình 1 -> 2 -> 1
        assertTrue(graph.wouldCauseCycle(1, 2), "1 phụ thuộc 2 khi 2 đã phụ thuộc 1 phải phát hiện chu trình");
    }

    @Test
    @DisplayName("Chu trình đa đỉnh gián tiếp: A -> B -> C -> D -> A phải bị chặn")
    void testMultiNodeCycle() {
        DependencyGraph graph = new DependencyGraph();
        // 1 -> 2 -> 3 -> 4
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);
        graph.addEdge(3, 4);

        // Thêm 4 -> 1 (Task 1 phụ thuộc Task 4) => Chu trình
        assertTrue(graph.wouldCauseCycle(1, 4), "1 phụ thuộc 4 khi đã có đường đi 1 -> 4 phải tạo chu trình");

        // Nhưng 5 phụ thuộc 4 thì hoàn toàn hợp lệ
        assertFalse(graph.wouldCauseCycle(5, 4), "Thêm node mới 5 phụ thuộc 4 là hợp lệ");
    }

    @Test
    @DisplayName("Mô hình kim cương (Diamond DAG) không có chu trình")
    void testDiamondDagIsValid() {
        // 1 -> 2, 1 -> 3, 2 -> 4, 3 -> 4 (4 phụ thuộc cả 2 và 3, cả 2 và 3 cùng phụ thuộc 1)
        DependencyGraph graph = new DependencyGraph();
        graph.addEdge(1, 2);
        graph.addEdge(1, 3);
        graph.addEdge(2, 4);

        assertFalse(graph.wouldCauseCycle(4, 3), "Cạnh 3 -> 4 trong hình kim cương hợp lệ (DAG)");
        graph.addEdge(3, 4);

        assertFalse(graph.hasAnyCycle(), "Đồ thị kim cương không có chu trình");
    }

    @Test
    @DisplayName("Sắp xếp tô-pô (Topological Sort) trả về thứ tự thực thi chính xác")
    void testTopologicalSortOrder() {
        DependencyGraph graph = new DependencyGraph();
        // 1 -> 2 -> 3
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);

        List<Integer> order = graph.topologicalSort();
        assertEquals(3, order.size());
        assertTrue(order.indexOf(1) < order.indexOf(2), "1 phải xuất hiện trước 2");
        assertTrue(order.indexOf(2) < order.indexOf(3), "2 phải xuất hiện trước 3");
    }

    @Test
    @DisplayName("Hàm tiện ích tĩnh wouldCauseCycle từ danh sách TaskDependency")
    void testStaticWouldCauseCycle() {
        List<TaskDependency> list = new ArrayList<>();
        list.add(new TaskDependency(10, 2, 1)); // 1 -> 2
        list.add(new TaskDependency(10, 3, 2)); // 2 -> 3

        assertTrue(DependencyGraph.wouldCauseCycle(1, 3, list), "1 -> 3 khi đã có 1 -> 2 -> 3 phải báo chu trình");
        assertFalse(DependencyGraph.wouldCauseCycle(4, 3, list), "4 -> 3 hợp lệ");
    }
}
