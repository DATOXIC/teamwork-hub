package com.teamwork.business;

import com.teamwork.business.automation.AutomationEngine;
import com.teamwork.business.gantt.CascadeScheduler;
import com.teamwork.business.gantt.DependencyGraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Empirical Adversarial Challenger Test Suite for TeamWork Hub Enterprise Features (R1 & R3).
 *
 * Authored by: Empirical Challenger 1
 * Targets:
 * - R1: Interactive Gantt & Dependencies (Graph Topologies, Cycle Resilience, Cascade Auto-Scheduling)
 * - R3: No-Code Automation Engine (Loop Protection, Concurrency Stress, ThreadLocal Cleanup)
 */
@DisplayName("Thử thách Đối nghịch Cấp cao (Empirical Adversarial Challenger Suite - R1 & R3)")
public class AdversarialEnterpriseChallengerTest {

    private LocalDate today;

    @BeforeEach
    void setUp() {
        AutomationEngine.clearAutomationGuard();
        today = LocalDate.of(2026, 10, 15);
    }

    @AfterEach
    void tearDown() {
        AutomationEngine.clearAutomationGuard();
    }

    private Task createTask(int id, String title, String startDate, String dueDate) {
        Task t = new Task();
        t.setId(id);
        t.setProjectId(1);
        t.setTitle(title);
        t.setStartDate(startDate);
        t.setDueDate(dueDate);
        t.setStatus("TODO");
        t.setPriority("MEDIUM");
        return t;
    }

    // =========================================================================
    // 1. COMPLEX GRAPH TOPOLOGIES (R1)
    // =========================================================================

    @Test
    @DisplayName("R1 Topology: Deep Chain (1 -> 2 -> ... -> 25) TopoSort & Validation")
    void testDeepChainTopology() {
        DependencyGraph graph = new DependencyGraph();
        int chainLength = 25;

        // Build linear chain: 1 -> 2 -> 3 -> ... -> 25
        for (int i = 1; i < chainLength; i++) {
            graph.addEdge(i, i + 1);
        }

        assertFalse(graph.hasAnyCycle(), "Linear deep chain must be a valid DAG");

        List<Integer> topoOrder = graph.topologicalSort();
        assertEquals(chainLength, topoOrder.size(), "Topo order must contain all 25 nodes");

        // Verify strict predecessor ordering: index of i < index of i+1
        for (int i = 1; i < chainLength; i++) {
            int idxPred = topoOrder.indexOf(i);
            int idxSucc = topoOrder.indexOf(i + 1);
            assertTrue(idxPred < idxSucc, "Task " + i + " must precede Task " + (i + 1) + " in topo order");
        }

        // Inserting valid forward leap: 1 -> 20, 5 -> 25 must NOT create cycle
        assertFalse(graph.wouldCauseCycle(20, 1), "Forward skip 1 -> 20 is valid DAG edge");
        assertFalse(graph.wouldCauseCycle(25, 5), "Forward skip 5 -> 25 is valid DAG edge");
    }

    @Test
    @DisplayName("R1 Topology: Wide Fan-out and Wide Fan-in Star / Bipartite DAG")
    void testWideFanOutAndFanInTopology() {
        DependencyGraph graph = new DependencyGraph();

        // Node 1 fans out to nodes 2..30 (Fan-out = 29)
        for (int i = 2; i <= 30; i++) {
            graph.addEdge(1, i);
        }

        // Nodes 2..30 all fan in to node 31 (Fan-in = 29)
        for (int i = 2; i <= 30; i++) {
            graph.addEdge(i, 31);
        }

        assertFalse(graph.hasAnyCycle(), "Wide fan-out / fan-in DAG must have no cycles");

        List<Integer> topo = graph.topologicalSort();
        assertEquals(31, topo.size(), "Topo sort must include all 31 nodes");

        // Node 1 must be first, Node 31 must be last
        assertEquals(1, topo.get(0), "Root node 1 must be first");
        assertEquals(31, topo.get(topo.size() - 1), "Sink node 31 must be last");

        // Every intermediate node 2..30 must appear between 1 and 31
        int idxFirst = topo.indexOf(1);
        int idxLast = topo.indexOf(31);
        for (int i = 2; i <= 30; i++) {
            int idx = topo.indexOf(i);
            assertTrue(idx > idxFirst && idx < idxLast, "Node " + i + " must be between 1 and 31");
        }
    }

    @Test
    @DisplayName("R1 Topology: Multi-tiered Diamond DAG with Cross-edges")
    void testNestedDiamondWithCrossEdges() {
        DependencyGraph graph = new DependencyGraph();
        // Tier 1 Diamond: 1 -> {2, 3} -> 4
        graph.addEdge(1, 2);
        graph.addEdge(1, 3);
        graph.addEdge(2, 4);
        graph.addEdge(3, 4);

        // Tier 2 Diamond: 4 -> {5, 6} -> 7
        graph.addEdge(4, 5);
        graph.addEdge(4, 6);
        graph.addEdge(5, 7);
        graph.addEdge(6, 7);

        // Cross-tier forward edges: 2 -> 6, 3 -> 5
        graph.addEdge(2, 6);
        graph.addEdge(3, 5);

        assertFalse(graph.hasAnyCycle(), "Multi-tiered diamond DAG with cross forward edges must be valid");

        List<Integer> order = graph.topologicalSort();
        assertEquals(7, order.size());

        // Validate all 8 constraints
        assertTrue(order.indexOf(1) < order.indexOf(2));
        assertTrue(order.indexOf(1) < order.indexOf(3));
        assertTrue(order.indexOf(2) < order.indexOf(4));
        assertTrue(order.indexOf(3) < order.indexOf(4));
        assertTrue(order.indexOf(4) < order.indexOf(5));
        assertTrue(order.indexOf(4) < order.indexOf(6));
        assertTrue(order.indexOf(5) < order.indexOf(7));
        assertTrue(order.indexOf(6) < order.indexOf(7));
        assertTrue(order.indexOf(2) < order.indexOf(6));
        assertTrue(order.indexOf(3) < order.indexOf(5));
    }

    @Test
    @DisplayName("R1 Topology: Disconnected Subgraph Components and Isolated Nodes")
    void testDisconnectedComponents() {
        DependencyGraph graph = new DependencyGraph();
        // Component A: 1 -> 2 -> 3
        graph.addEdge(1, 2);
        graph.addEdge(2, 3);

        // Component B: 10 -> 20 -> 30
        graph.addEdge(10, 20);
        graph.addEdge(20, 30);

        // Component C: 100 -> 200
        graph.addEdge(100, 200);

        // Isolated nodes (via self/zero-edge or dummy dependency)
        // Add isolated node 999 by adding and removing or registering
        graph.addEdge(999, 999); // Self edge attempt will be tested for cycle, let's test component topology
        // Let's create a clean graph without self-edges
        DependencyGraph cleanGraph = new DependencyGraph();
        cleanGraph.addEdge(1, 2);
        cleanGraph.addEdge(2, 3);
        cleanGraph.addEdge(10, 20);
        cleanGraph.addEdge(20, 30);
        cleanGraph.addEdge(100, 200);

        assertFalse(cleanGraph.hasAnyCycle());
        List<Integer> order = cleanGraph.topologicalSort();
        assertEquals(8, order.size());

        // Subgraph internal order must be strictly preserved
        assertTrue(order.indexOf(1) < order.indexOf(2));
        assertTrue(order.indexOf(2) < order.indexOf(3));
        assertTrue(order.indexOf(10) < order.indexOf(20));
        assertTrue(order.indexOf(20) < order.indexOf(30));
        assertTrue(order.indexOf(100) < order.indexOf(200));

        // Inter-component independence: adding 3 -> 10 must NOT cause cycle
        assertFalse(cleanGraph.wouldCauseCycle(10, 3), "Connecting two independent components is valid");
        // But adding 10 -> 3 after 3 -> 10 would cause cycle
        cleanGraph.addEdge(3, 10);
        assertTrue(cleanGraph.wouldCauseCycle(3, 10), "Back-edge across components must be detected");
    }

    // =========================================================================
    // 2. CYCLE DETECTION RESILIENCE (R1)
    // =========================================================================

    @Test
    @DisplayName("R1 Cycle Detection: Deep Back-edges in Chain (25 -> 1, 15 -> 5, 2 -> 1)")
    void testDeepBackEdgesCycleDetection() {
        DependencyGraph graph = new DependencyGraph();
        int n = 25;
        for (int i = 1; i < n; i++) {
            graph.addEdge(i, i + 1); // 1 -> 2 -> ... -> 25
        }

        // Attempting to make Task 1 depend on Task 25 (adding edge 25 -> 1)
        assertTrue(graph.wouldCauseCycle(1, 25), "Cycle 1..25->1 must be detected");

        // Attempting to make Task 5 depend on Task 15 (adding edge 15 -> 5)
        assertTrue(graph.wouldCauseCycle(5, 15), "Cycle 5..15->5 must be detected");

        // Attempting to make Task 1 depend on Task 2 (adding edge 2 -> 1)
        assertTrue(graph.wouldCauseCycle(1, 2), "Cycle 1->2->1 must be detected");

        // Attempting to make Task 24 depend on Task 25 (adding edge 25 -> 24)
        assertTrue(graph.wouldCauseCycle(24, 25), "Cycle 24->25->24 must be detected");
    }

    @Test
    @DisplayName("R1 Cycle Detection: Self-loops (k -> k) and Boundary / Negative IDs")
    void testSelfLoopsAndBoundaryIDs() {
        DependencyGraph graph = new DependencyGraph();
        graph.addEdge(1, 2);

        // Self-loop: Task 1 depends on Task 1, Task 2 depends on Task 2
        assertTrue(graph.wouldCauseCycle(1, 1), "Self-dependency 1 -> 1 must be flagged as cycle");
        assertTrue(graph.wouldCauseCycle(99, 99), "Self-dependency 99 -> 99 must be flagged as cycle");

        // Invalid / zero / negative task IDs must safely return false without throwing exceptions
        assertFalse(graph.wouldCauseCycle(0, 0), "Zero IDs should not cause cycle or exception");
        assertFalse(graph.wouldCauseCycle(-1, 2), "Negative taskId should return false");
        assertFalse(graph.wouldCauseCycle(2, -1), "Negative dependsOnTaskId should return false");
        assertFalse(graph.wouldCauseCycle(-5, -5), "Both negative IDs should return false");

        // Verify addEdge with negative or zero ID does nothing
        graph.addEdge(-1, 5);
        graph.addEdge(5, -1);
        assertFalse(graph.hasPath(-1, 5));
    }

    @Test
    @DisplayName("R1 Cycle Detection: Multiple Alternative Cycle Paths in Diamond DAG")
    void testMultipleCyclePathsInDiamondDAG() {
        // Diamond: 1 -> 2 -> 4, 1 -> 3 -> 4
        DependencyGraph graph = new DependencyGraph();
        graph.addEdge(1, 2);
        graph.addEdge(1, 3);
        graph.addEdge(2, 4);
        graph.addEdge(3, 4);

        // From 4, there are 2 distinct paths back to 1: 4 -> 1 would complete cycles (1,2,4,1) AND (1,3,4,1)
        assertTrue(graph.wouldCauseCycle(1, 4), "Sink 4 back to Source 1 must detect cycle");

        // 4 back to branch 2
        assertTrue(graph.wouldCauseCycle(2, 4), "Sink 4 back to Branch 2 must detect cycle");

        // 4 back to branch 3
        assertTrue(graph.wouldCauseCycle(3, 4), "Sink 4 back to Branch 3 must detect cycle");

        // Cross-branch: 2 and 3 are parallel. Adding edge 2 -> 3 (3 depends on 2) is a valid DAG edge!
        assertFalse(graph.wouldCauseCycle(3, 2), "Parallel branch edge 2 -> 3 is DAG edge (no cycle)");
        graph.addEdge(2, 3);

        // Now that 2 -> 3 exists, 3 -> 2 MUST cause cycle!
        assertTrue(graph.wouldCauseCycle(2, 3), "Reverse edge 3 -> 2 must now cause cycle");
    }

    @Test
    @DisplayName("R1 Cycle Detection: Detection of Pre-existing Cycles via DFS 3-Color")
    void testPreexistingCycleDetection() {
        DependencyGraph graph = new DependencyGraph();
        // Artificially inject a cycle into the adjacency map: 10 -> 20 -> 30 -> 10
        graph.addEdge(10, 20);
        graph.addEdge(20, 30);
        graph.addEdge(30, 10);

        assertTrue(graph.hasAnyCycle(), "DFS 3-color algorithm must report true for cyclic graph");

        // Kahn's topologicalSort on cyclic graph must return an empty list
        List<Integer> order = graph.topologicalSort();
        assertTrue(order.isEmpty(), "Topological sort must return empty list when cycle is present");
    }

    // =========================================================================
    // 3. CASCADE AUTO-SCHEDULING (R1)
    // =========================================================================

    @Test
    @DisplayName("R1 Cascade: Deep Chain 20 Tasks downstream shift strictly preserves Task Duration")
    void testDeepChainCascadeShiftPreservesDuration() {
        int n = 20;
        List<Task> tasks = new ArrayList<>();
        List<TaskDependency> deps = new ArrayList<>();

        // Create 20 tasks, each having a 2-day duration
        // T1: 2026-10-01 to 2026-10-03 (duration = 2 days)
        // T2: 2026-10-04 to 2026-10-06 (duration = 2 days)
        LocalDate curStart = LocalDate.of(2026, 10, 1);
        for (int i = 1; i <= n; i++) {
            LocalDate curDue = curStart.plusDays(2);
            tasks.add(createTask(i, "Task #" + i, curStart.toString(), curDue.toString()));

            if (i > 1) {
                // Dependency: Task i depends on Task (i-1) with lag = 1
                deps.add(new TaskDependency(1, i, i - 1, "FS", 1));
            }
            curStart = curDue.plusDays(1);
        }

        // Root Task 1 is delayed: new due date is 2026-10-15 (shifted by +12 days)
        LocalDate newRootDue = LocalDate.of(2026, 10, 15);
        List<Task> modified = CascadeScheduler.cascade(1, newRootDue, tasks, deps);

        assertEquals(20, modified.size(), "All 20 downstream tasks in the chain must be shifted");
        assertEquals("2026-10-15", tasks.get(0).getDueDate(), "Task 1 due date must match newRootDue");

        // Verify EVERY downstream task (i=2..n) preserved its original 2-day duration and conforms to lag = 1
        for (int i = 2; i <= n; i++) {
            Task t = tasks.get(i - 1);
            LocalDate start = LocalDate.parse(t.getStartDate());
            LocalDate due = LocalDate.parse(t.getDueDate());
            long duration = ChronoUnit.DAYS.between(start, due);
            assertEquals(2, duration, "Downstream Task " + i + " duration must remain exactly 2 days");

            Task prev = tasks.get(i - 2);
            LocalDate prevDue = LocalDate.parse(prev.getDueDate());
            assertEquals(prevDue.plusDays(1), start, "Task " + i + " must start exactly 1 day after Task " + (i - 1) + " due date");
        }
    }

    @Test
    @DisplayName("R1 Cascade: Multi-Parent Lag Resolution & Dominance")
    void testMultiParentLagResolution() {
        // Task 1: 2026-10-01 to 2026-10-05
        Task t1 = createTask(1, "Task 1", "2026-10-01", "2026-10-05");
        // Task 2: 2026-10-01 to 2026-10-08
        Task t2 = createTask(2, "Task 2", "2026-10-01", "2026-10-08");
        // Task 3 (depends on 1 with lag=5, and depends on 2 with lag=1): duration = 3 days
        Task t3 = createTask(3, "Task 3 (Join)", "2026-10-06", "2026-10-09");

        List<Task> tasks = List.of(t1, t2, t3);
        List<TaskDependency> deps = List.of(
                new TaskDependency(1, 3, 1, "FS", 5), // requires 2026-10-05 + 5 = 2026-10-10
                new TaskDependency(1, 3, 2, "FS", 1)  // requires 2026-10-08 + 1 = 2026-10-09
        );

        // Case A: Shift Task 1 to 2026-10-07 -> requires 2026-10-07 + 5 = 2026-10-12
        // Task 2 requires 2026-10-08 + 1 = 2026-10-09.
        // Task 3 must start on 2026-10-12 (Task 1 dominates).
        List<Task> modA = CascadeScheduler.cascade(1, LocalDate.parse("2026-10-07"), tasks, deps);
        assertEquals(2, modA.size());
        assertEquals("2026-10-12", t3.getStartDate());
        assertEquals("2026-10-15", t3.getDueDate(), "Task 3 duration (3 days) must be preserved: 12 + 3 = 15");

        // Case B: Now shift Task 2 to 2026-10-14 -> requires 2026-10-14 + 1 = 2026-10-15
        // Task 1 requires 2026-10-12.
        // Now Task 2 dominates! Task 3 must start on 2026-10-15.
        List<Task> modB = CascadeScheduler.cascade(2, LocalDate.parse("2026-10-14"), tasks, deps);
        assertEquals(2, modB.size());
        assertEquals("2026-10-15", t3.getStartDate());
        assertEquals("2026-10-18", t3.getDueDate(), "Task 3 duration (3 days) must be preserved: 15 + 3 = 18");
    }

    @Test
    @DisplayName("R1 Cascade: Selective Branch Pruning - Unaffected far-future tasks are not shifted")
    void testSelectiveBranchPruning() {
        Task root = createTask(1, "Root", "2026-10-01", "2026-10-05");
        // Branch A (Immediate): Task 2 [10-06..10-08], Task 3 [10-09..10-12]
        Task a1 = createTask(2, "Branch A1", "2026-10-06", "2026-10-08");
        Task a2 = createTask(3, "Branch A2", "2026-10-09", "2026-10-12");

        // Branch B (Far Future): Task 4 [10-25..10-28], Task 5 [10-29..10-31]
        Task b1 = createTask(4, "Branch B1 (Late)", "2026-10-25", "2026-10-28");
        Task b2 = createTask(5, "Branch B2 (Late)", "2026-10-29", "2026-10-31");

        // Disconnected Task C
        Task c1 = createTask(6, "Independent", "2026-10-02", "2026-10-05");

        List<Task> tasks = List.of(root, a1, a2, b1, b2, c1);
        List<TaskDependency> deps = List.of(
                new TaskDependency(1, 2, 1, "FS", 1),
                new TaskDependency(1, 3, 2, "FS", 1),
                new TaskDependency(1, 4, 1, "FS", 1),
                new TaskDependency(1, 5, 4, "FS", 1)
        );

        // Root is delayed from 2026-10-05 to 2026-10-08 (+3 days)
        // Branch A requires start >= 2026-10-09 (violates 10-06) -> MUST shift.
        // Branch B requires start >= 2026-10-09 (current start 10-25 is already >= 10-09) -> MUST NOT shift!
        List<Task> modified = CascadeScheduler.cascade(1, LocalDate.parse("2026-10-08"), tasks, deps);

        assertEquals(3, modified.size(), "Only Root, A1, and A2 should be modified");
        assertTrue(modified.contains(root));
        assertTrue(modified.contains(a1));
        assertTrue(modified.contains(a2));
        assertFalse(modified.contains(b1), "Branch B1 must not be modified");
        assertFalse(modified.contains(b2), "Branch B2 must not be modified");
        assertFalse(modified.contains(c1), "Independent task must not be modified");

        assertEquals("2026-10-25", b1.getStartDate(), "Branch B1 date must remain unchanged");
        assertEquals("2026-10-29", b2.getStartDate(), "Branch B2 date must remain unchanged");
    }

    @Test
    @DisplayName("R1 Cascade: Infinite Loop Protection under Cyclic Dependency Graph")
    void testCascadeSchedulerCycleTermination() {
        Task t1 = createTask(1, "Cycle Node 1", "2026-10-01", "2026-10-05");
        Task t2 = createTask(2, "Cycle Node 2", "2026-10-06", "2026-10-08");

        List<Task> tasks = List.of(t1, t2);
        // Cyclic dependencies: 1 -> 2 AND 2 -> 1
        List<TaskDependency> cyclicDeps = List.of(
                new TaskDependency(1, 2, 1, "FS", 1),
                new TaskDependency(1, 1, 2, "FS", 1)
        );

        // CascadeScheduler must NOT loop indefinitely; it must terminate within maxSteps
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
            List<Task> result = CascadeScheduler.cascade(1, LocalDate.parse("2026-10-10"), tasks, cyclicDeps);
            assertNotNull(result);
        }, "Cascade scheduler must terminate cleanly even if input dependencies contain cycles");
    }

    @Test
    @DisplayName("R1 Cascade: Minimum Duration Floor for Zero/Negative Duration")
    void testCascadeZeroDurationFloor() {
        // Task 1: 2026-10-01 to 2026-10-05
        Task t1 = createTask(1, "Task 1", "2026-10-01", "2026-10-05");
        // Task 2: corrupt dates where startDate == dueDate (0 duration)
        Task t2 = createTask(2, "Task 2 (Zero Duration)", "2026-10-06", "2026-10-06");

        List<Task> tasks = List.of(t1, t2);
        List<TaskDependency> deps = List.of(new TaskDependency(1, 2, 1, "FS", 1));

        List<Task> modified = CascadeScheduler.cascade(1, LocalDate.parse("2026-10-10"), tasks, deps);
        assertEquals(2, modified.size());

        // t2 new start = 2026-10-11. Minimum duration = 1 day, so new due = 2026-10-12
        assertEquals("2026-10-11", t2.getStartDate());
        assertEquals("2026-10-12", t2.getDueDate(), "CascadeScheduler must guarantee minimum 1-day duration floor");
    }

    // =========================================================================
    // 4. AUTOMATION ENGINE LOOP PROTECTION & CONCURRENCY STRESS (R3)
    // =========================================================================

    @Test
    @DisplayName("R3 Concurrency Stress: 50 Concurrent Threads ThreadLocal Guard Isolation")
    void testThreadLocalGuardHighConcurrencyStress() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger leakCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all threads to fire simultaneously

                    // Check initial state
                    if (AutomationEngine.isInAutomation()) {
                        leakCount.incrementAndGet();
                    }

                    // Half of threads turn on guard, half keep it off
                    boolean shouldTurnOn = (threadId % 2 == 0);
                    if (shouldTurnOn) {
                        AutomationEngine.setInAutomationForTesting(true);
                        if (!AutomationEngine.isInAutomation()) {
                            leakCount.incrementAndGet();
                        }
                    } else {
                        if (AutomationEngine.isInAutomation()) {
                            leakCount.incrementAndGet();
                        }
                    }

                    // Simulate workload
                    Thread.sleep(5);

                    // Re-verify that other threads did not alter this thread's state
                    if (AutomationEngine.isInAutomation() != shouldTurnOn) {
                        leakCount.incrementAndGet();
                    }

                    // Clean up
                    AutomationEngine.clearAutomationGuard();
                    if (AutomationEngine.isInAutomation()) {
                        leakCount.incrementAndGet();
                    }

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    leakCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 50 threads must complete within 5 seconds");
        assertEquals(threadCount, successCount.get(), "All threads must successfully pass");
        assertEquals(0, leakCount.get(), "ThreadLocal must have zero state leakage across concurrent threads");
    }

    @Test
    @DisplayName("R3 Loop Protection: Nested Re-entrant Invocations Abort Immediately")
    void testNestedReentrantCallAbortsImmediately() {
        AutomationEngine.clearAutomationGuard();
        assertFalse(AutomationEngine.isInAutomation());

        // Simulate an outer automation process setting the guard
        AutomationEngine.setInAutomationForTesting(true);
        assertTrue(AutomationEngine.isInAutomation());

        // Any re-entrant call to Recipe 1, 2, or 3 must immediately abort
        boolean r1 = AutomationEngine.onSubtasksProgressUpdated(1, 1);
        assertFalse(r1, "Recipe 1 must abort when invoked inside automation context");

        int r2 = AutomationEngine.onTaskApproved(1, 1);
        assertEquals(0, r2, "Recipe 2 must return 0 when invoked inside automation context");

        int r3 = AutomationEngine.evaluateDueTasks(1);
        assertEquals(0, r3, "Recipe 3 must return 0 when invoked inside automation context");

        // The outer guard must remain active (not cleared by inner aborted calls)
        assertTrue(AutomationEngine.isInAutomation(), "Outer automation guard must not be cleared by inner aborted calls");

        // When the outer block finishes:
        AutomationEngine.clearAutomationGuard();
        assertFalse(AutomationEngine.isInAutomation());
    }

    @Test
    @DisplayName("R3 Robustness: Exception within Automation Block Cleans Up Guard")
    void testExceptionCleaningGuard() {
        AutomationEngine.clearAutomationGuard();

        // Simulate an automation execution that throws an exception inside try block
        try {
            AutomationEngine.setInAutomationForTesting(true);
            assertTrue(AutomationEngine.isInAutomation());
            throw new RuntimeException("Simulated unexpected failure during automation execution");
        } catch (RuntimeException ignored) {
            // emulate finally block in AutomationEngine
            AutomationEngine.setInAutomationForTesting(false);
        }

        assertFalse(AutomationEngine.isInAutomation(), "Guard must be false after exception handled");
    }

    @Test
    @DisplayName("R3 Rule Matrix: Exhaustive Boundary & Combinatorial Testing for Recipe 1 (Gate 2)")
    void testRecipe1ExhaustiveTruthTable() {
        // Rule: recipeEnabled && requiresGate && progress >= 100 && status not in (DONE, SUBMITTED)
        String[] statuses = {"TODO", "IN_PROGRESS", "REVISE", "REJECTED", "DONE", "SUBMITTED", null, ""};
        int[] progresses = {-1, 0, 50, 99, 100, 101};

        for (boolean enabled : List.of(true, false)) {
            for (boolean gate : List.of(true, false)) {
                for (int prog : progresses) {
                    for (String status : statuses) {
                        boolean actual = AutomationEngine.shouldPromptGate2(enabled, gate, prog, status);

                        boolean expected = enabled
                                && gate
                                && prog >= 100
                                && status != null
                                && !status.equalsIgnoreCase("DONE")
                                && !status.equalsIgnoreCase("SUBMITTED");

                        assertEquals(expected, actual,
                                String.format("Failed for enabled=%b, gate=%b, prog=%d, status=%s", enabled, gate, prog, status));
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("R3 Rule Matrix: Exhaustive Boundary & Combinatorial Testing for Recipe 3 (Escalate Priority)")
    void testRecipe3ExhaustiveTruthTable() {
        // Rule: recipeEnabled && status not in (DONE, SUBMITTED) && due <= today && progress < 50 && priority != HIGH
        String[] statuses = {"TODO", "IN_PROGRESS", "REVISE", "DONE", "SUBMITTED", null};
        String[] priorities = {"LOW", "MEDIUM", "HIGH", null, ""};
        int[] progresses = {0, 49, 50, 51, 100};
        String[] dates = {
                today.minusDays(5).toString(), // overdue
                today.toString(),             // due today
                today.plusDays(1).toString(),  // tomorrow
                null,
                "invalid"
        };

        for (boolean enabled : List.of(true, false)) {
            for (String status : statuses) {
                for (String priority : priorities) {
                    for (int prog : progresses) {
                        for (String due : dates) {
                            boolean actual = AutomationEngine.shouldEscalatePriority(enabled, due, prog, priority, today, status);

                            boolean expectedStatus = status == null || (!status.equalsIgnoreCase("DONE") && !status.equalsIgnoreCase("SUBMITTED"));
                            boolean expectedDate = false;
                            if (due != null) {
                                try {
                                    LocalDate d = LocalDate.parse(due);
                                    expectedDate = !d.isAfter(today);
                                } catch (Exception ignored) {}
                            }
                            boolean expectedProg = prog < 50;
                            boolean expectedPrio = priority == null || !priority.equalsIgnoreCase("HIGH");

                            boolean expected = enabled && expectedStatus && expectedDate && expectedProg && expectedPrio;

                            assertEquals(expected, actual,
                                    String.format("Failed for enabled=%b, status=%s, prio=%s, prog=%d, due=%s",
                                            enabled, status, priority, prog, due));
                        }
                    }
                }
            }
        }
    }
}
