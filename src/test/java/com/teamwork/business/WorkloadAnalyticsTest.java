package com.teamwork.business;

import com.teamwork.business.analytics.WorkloadAnalytics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Phân Bổ Tải & Cảnh Báo Quá Tải (WorkloadAnalytics)")
class WorkloadAnalyticsTest {

    @Test
    @DisplayName("Phát hiện quá tải khi số việc đang làm in-progress >= 5")
    void testOverloadDetectionInProgressThreshold() {
        User u = new User();
        u.setId(10);
        u.setFullName("Nguyễn Văn A");

        UserWorkload uw = new UserWorkload();
        uw.setUser(u);
        uw.setInProgressCount(5); // Chạm ngưỡng 5
        uw.setOverdueCount(0);

        List<WorkloadAnalytics> list = WorkloadAnalytics.fromWorkloads(List.of(uw));

        assertEquals(1, list.size());
        WorkloadAnalytics wa = list.get(0);
        assertTrue(wa.isOverloaded());
        assertEquals("HIGH", wa.getLoadLevel());
        assertTrue(wa.getOverloadReason().contains("5 việc"));
    }

    @Test
    @DisplayName("Phát hiện báo động đỏ (CRITICAL) khi có việc bị quá hạn overdue > 0")
    void testOverloadDetectionOverdue() {
        User u = new User();
        u.setId(20);
        u.setFullName("Trần Thị B");

        UserWorkload uw = new UserWorkload();
        uw.setUser(u);
        uw.setInProgressCount(2); // Dưới 5
        uw.setOverdueCount(1);    // Nhưng có 1 việc quá hạn

        List<WorkloadAnalytics> list = WorkloadAnalytics.fromWorkloads(List.of(uw));

        assertEquals(1, list.size());
        WorkloadAnalytics wa = list.get(0);
        assertTrue(wa.isOverloaded());
        assertEquals("CRITICAL", wa.getLoadLevel());
        assertTrue(wa.getOverloadReason().contains("quá hạn"));
    }

    @Test
    @DisplayName("Thành viên có tải làm việc bình thường (NORMAL)")
    void testBalancedWorkload() {
        User u = new User();
        u.setId(30);
        u.setFullName("Lê Văn C");

        UserWorkload uw = new UserWorkload();
        uw.setUser(u);
        uw.setInProgressCount(3);
        uw.setDoneCount(10);
        uw.setOverdueCount(0);

        List<WorkloadAnalytics> list = WorkloadAnalytics.fromWorkloads(List.of(uw));

        assertEquals(1, list.size());
        WorkloadAnalytics wa = list.get(0);
        assertFalse(wa.isOverloaded());
        assertEquals("NORMAL", wa.getLoadLevel());
    }

    @Test
    @DisplayName("Xử lý danh sách rỗng an toàn")
    void testEmptyWorkloadList() {
        List<WorkloadAnalytics> list = WorkloadAnalytics.fromWorkloads(Collections.emptyList());
        assertNotNull(list);
        assertTrue(list.isEmpty());

        List<WorkloadAnalytics> listNull = WorkloadAnalytics.fromWorkloads(null);
        assertNotNull(listNull);
        assertTrue(listNull.isEmpty());
    }
}
