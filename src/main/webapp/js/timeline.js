/**
 * =========================================================================
 * TEAMWORK HUB — INTERACTIVE GANTT TIMELINE & ROADMAP ENGINE (timeline.js)
 * 100% Pure Vanilla JS — Không phụ thuộc thư viện ngoài — Chuẩn môn học
 * Tương thích ClickUp 3.0, Linear Cycles, Jira Roadmap
 * =========================================================================
 */

(function () {
    'use strict';

    // State quản lý toàn cục của phân hệ Lộ trình
    const state = {
        zoom: 'day', // 'day' | 'week' | 'month'
        dayWidth: 44, // px mỗi ngày ở chế độ Day
        tasks: [],
        filteredTasks: [],
        todayStr: '',
        startDate: null,
        endDate: null,
        totalDays: 0,
        projectId: 0,
        contextPath: '',
        searchQuery: '',
        filterMember: 'ALL',
        filterPriority: 'ALL',
        filterStatus: 'ALL'
    };

    const DAY_NAMES = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

    // Khởi tạo động cơ khi DOM sẵn sàng
    document.addEventListener('DOMContentLoaded', function () {
        initEngine();
    });

    function initEngine() {
        if (!window.timelineConfig) return;

        state.projectId = window.timelineConfig.projectId;
        state.contextPath = window.timelineConfig.contextPath || '';
        state.todayStr = window.timelineConfig.today;
        state.tasks = Array.isArray(window.timelineTasksData) ? window.timelineTasksData : [];
        state.filteredTasks = [...state.tasks];

        calculateHorizon();
        bindControls();
        renderGantt();

        // Tự động cuộn đến vạch "Hôm nay" sau khi vẽ xong
        setTimeout(function () {
            scrollToToday();
        }, 250);
    }

    /**
     * 1. Tính toán khung thời gian bao quát toàn bộ dự án
     */
    function calculateHorizon() {
        const today = parseDate(state.todayStr) || new Date();
        let minD = new Date(today);
        minD.setDate(minD.getDate() - 7); // Mặc định lùi 1 tuần

        let maxD = new Date(today);
        maxD.setDate(maxD.getDate() + 21); // Mặc định tiến 3 tuần

        state.tasks.forEach(function (t) {
            if (t.dueDate) {
                const d = parseDate(t.dueDate);
                if (d) {
                    if (d < minD) {
                        minD = new Date(d);
                        minD.setDate(minD.getDate() - 3);
                    }
                    if (d > maxD) {
                        maxD = new Date(d);
                        maxD.setDate(maxD.getDate() + 7);
                    }
                }
            }
        });

        // Chuẩn hóa thời gian về 00:00:00
        minD.setHours(0, 0, 0, 0);
        maxD.setHours(0, 0, 0, 0);

        state.startDate = minD;
        state.endDate = maxD;
        state.totalDays = Math.max(28, Math.round((maxD - minD) / (1000 * 60 * 60 * 24)) + 1);
    }

    /**
     * 2. Gắn kết sự kiện cho thanh công cụ và bộ lọc
     */
    function bindControls() {
        // Nút chuyển chế độ Zoom: Ngày / Tuần / Tháng
        document.querySelectorAll('.timeline-zoom-btn').forEach(function (btn) {
            btn.addEventListener('click', function () {
                document.querySelectorAll('.timeline-zoom-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                state.zoom = btn.getAttribute('data-zoom') || 'day';
                if (state.zoom === 'day') state.dayWidth = 44;
                else if (state.zoom === 'week') state.dayWidth = 24;
                else if (state.zoom === 'month') state.dayWidth = 12;

                renderGantt();
            });
        });

        // Nút cuộn về "Hôm nay"
        const btnToday = document.getElementById('btnScrollToday');
        if (btnToday) {
            btnToday.addEventListener('click', function () {
                scrollToToday();
            });
        }

        // Ô tìm kiếm từ khóa
        const searchInput = document.getElementById('timelineSearchInput');
        if (searchInput) {
            searchInput.addEventListener('input', function () {
                state.searchQuery = this.value.trim().toLowerCase();
                applyFilters();
            });
        }

        // Lọc theo thành viên
        const memberSelect = document.getElementById('timelineMemberFilter');
        if (memberSelect) {
            memberSelect.addEventListener('change', function () {
                state.filterMember = this.value;
                applyFilters();
            });
        }

        // Lọc theo mức ưu tiên
        const prioritySelect = document.getElementById('timelinePriorityFilter');
        if (prioritySelect) {
            prioritySelect.addEventListener('change', function () {
                state.filterPriority = this.value;
                applyFilters();
            });
        }

        // Lọc theo trạng thái
        const statusSelect = document.getElementById('timelineStatusFilter');
        if (statusSelect) {
            statusSelect.addEventListener('change', function () {
                state.filterStatus = this.value;
                applyFilters();
            });
        }

        // Đồng bộ thanh cuộn dọc giữa Khung Trái và Khung Phải
        const leftBody = document.getElementById('ganttLeftBody');
        const rightPane = document.getElementById('ganttRightPane');
        if (leftBody && rightPane) {
            leftBody.addEventListener('scroll', function () {
                rightPane.scrollTop = leftBody.scrollTop;
            });
            rightPane.addEventListener('scroll', function () {
                leftBody.scrollTop = rightPane.scrollTop;
            });
        }

        // Phím tắt bàn phím tiện lợi
        document.addEventListener('keydown', function (e) {
            if (e.target.closest('input, textarea, select')) return;
            if (e.key === 't' || e.key === 'T') {
                e.preventDefault();
                scrollToToday();
            } else if (e.key === '1') {
                const btn = document.querySelector('.timeline-zoom-btn[data-zoom="day"]');
                if (btn) btn.click();
            } else if (e.key === '2') {
                const btn = document.querySelector('.timeline-zoom-btn[data-zoom="week"]');
                if (btn) btn.click();
            } else if (e.key === '3') {
                const btn = document.querySelector('.timeline-zoom-btn[data-zoom="month"]');
                if (btn) btn.click();
            }
        });
    }

    /**
     * 3. Áp dụng bộ lọc đa tiêu chí tức thì (< 0.01 giây)
     */
    function applyFilters() {
        state.filteredTasks = state.tasks.filter(function (t) {
            // Kiểm tra tìm kiếm
            if (state.searchQuery) {
                const title = (t.title || '').toLowerCase();
                const assignee = (t.assigneeName || '').toLowerCase();
                if (!title.includes(state.searchQuery) && !assignee.includes(state.searchQuery)) {
                    return false;
                }
            }

            // Kiểm tra thành viên
            if (state.filterMember !== 'ALL') {
                if (String(t.assigneeId) !== String(state.filterMember)) {
                    return false;
                }
            }

            // Kiểm tra mức ưu tiên
            if (state.filterPriority !== 'ALL') {
                if ((t.priority || '').toUpperCase() !== state.filterPriority.toUpperCase()) {
                    return false;
                }
            }

            // Kiểm tra trạng thái
            if (state.filterStatus !== 'ALL') {
                if (state.filterStatus === 'OVERDUE') {
                    if (!t.isOverdue) return false;
                } else if (state.filterStatus === 'UNSCHEDULED') {
                    if (t.dueDate) return false;
                } else if ((t.status || '').toUpperCase() !== state.filterStatus.toUpperCase()) {
                    return false;
                }
            }

            return true;
        });

        renderGantt();

        const countBadge = document.getElementById('filterMatchCount');
        if (countBadge) {
            countBadge.textContent = state.filteredTasks.length + ' / ' + state.tasks.length;
        }
    }

    /**
     * 4. Vẽ toàn bộ cấu trúc Sơ đồ Gantt (Header, Lưới, Hàng, Thanh Bar)
     */
    function renderGantt() {
        renderHeader();
        renderRows();
    }

    /**
     * 4.1. Vẽ Header dòng thời gian (Tháng & Ngày)
     */
    function renderHeader() {
        const headerContainer = document.getElementById('ganttTimelineHeader');
        if (!headerContainer) return;

        const totalWidth = state.totalDays * state.dayWidth;
        headerContainer.style.width = totalWidth + 'px';

        // 1. Dòng Tháng (Tier Months)
        const tierMonths = document.createElement('div');
        tierMonths.className = 'gantt-header-tier-months';

        // 2. Dòng Ngày (Tier Days)
        const tierDays = document.createElement('div');
        tierDays.className = 'gantt-header-tier-days';

        let curMonth = -1;
        let curMonthWidth = 0;
        let curMonthLabel = '';
        let monthBlocks = [];

        const todayObj = parseDate(state.todayStr);

        for (let i = 0; i < state.totalDays; i++) {
            const date = new Date(state.startDate);
            date.setDate(date.getDate() + i);

            const m = date.getMonth();
            const y = date.getFullYear();
            const dayNum = date.getDate();
            const dayOfWeek = date.getDay();
            const isWeekend = (dayOfWeek === 0 || dayOfWeek === 6);
            const isToday = isSameDay(date, todayObj);

            if (m !== curMonth) {
                if (curMonth !== -1) {
                    monthBlocks.push({ label: curMonthLabel, width: curMonthWidth });
                }
                curMonth = m;
                curMonthWidth = state.dayWidth;
                curMonthLabel = 'Tháng ' + (m + 1) + ', ' + y;
            } else {
                curMonthWidth += state.dayWidth;
            }

            // Cell Ngày
            const dayCell = document.createElement('div');
            dayCell.className = 'gantt-day-cell' + (isWeekend ? ' is-weekend' : '') + (isToday ? ' is-today' : '');
            dayCell.style.width = state.dayWidth + 'px';

            if (state.zoom === 'day') {
                dayCell.innerHTML = '<span class="day-of-week">' + DAY_NAMES[dayOfWeek] + '</span><span class="gantt-day-number">' + dayNum + '</span>';
            } else if (state.zoom === 'week') {
                dayCell.innerHTML = '<span class="gantt-day-number">' + dayNum + '</span>';
            } else {
                dayCell.innerHTML = '<span class="fs-10">' + (dayNum === 1 ? (m + 1) + 'M' : dayNum) + '</span>';
            }

            tierDays.appendChild(dayCell);
        }

        if (curMonthWidth > 0) {
            monthBlocks.push({ label: curMonthLabel, width: curMonthWidth });
        }

        monthBlocks.forEach(function (mb) {
            const mDiv = document.createElement('div');
            mDiv.className = 'gantt-month-block';
            mDiv.style.width = mb.width + 'px';
            mDiv.textContent = mb.label;
            tierMonths.appendChild(mDiv);
        });

        headerContainer.innerHTML = '';
        headerContainer.appendChild(tierMonths);
        headerContainer.appendChild(tierDays);
    }

    /**
     * 4.2. Vẽ danh sách hàng Task (Bên trái) và Canvas Gantt Bar (Bên phải)
     */
    function renderRows() {
        const leftBody = document.getElementById('ganttLeftBody');
        const gridCanvas = document.getElementById('ganttGridCanvas');
        if (!leftBody || !gridCanvas) return;

        leftBody.innerHTML = '';
        gridCanvas.innerHTML = '';

        const totalWidth = state.totalDays * state.dayWidth;
        gridCanvas.style.width = totalWidth + 'px';

        // 1. Vẽ các cột sọc nền (Grid Columns)
        for (let i = 0; i < state.totalDays; i++) {
            const date = new Date(state.startDate);
            date.setDate(date.getDate() + i);
            const isWeekend = (date.getDay() === 0 || date.getDay() === 6);

            const colStrip = document.createElement('div');
            colStrip.className = 'gantt-column-strip' + (isWeekend ? ' is-weekend' : '');
            colStrip.style.left = (i * state.dayWidth) + 'px';
            colStrip.style.width = state.dayWidth + 'px';
            gridCanvas.appendChild(colStrip);
        }

        // 2. Vẽ đường kẻ đỏ mốc "Hôm nay" (Today Marker)
        const todayObj = parseDate(state.todayStr);
        if (todayObj && todayObj >= state.startDate && todayObj <= state.endDate) {
            const diffDays = Math.round((todayObj - state.startDate) / (1000 * 60 * 60 * 24));
            const todayX = (diffDays * state.dayWidth) + (state.dayWidth / 2);

            const todayLine = document.createElement('div');
            todayLine.id = 'ganttTodayMarker';
            todayLine.className = 'gantt-today-marker-line';
            todayLine.style.left = todayX + 'px';

            const badgePin = document.createElement('div');
            badgePin.className = 'gantt-today-badge-pin';
            badgePin.innerHTML = '<i class="bi bi-clock-fill me-1"></i> Hôm nay';
            todayLine.appendChild(badgePin);

            gridCanvas.appendChild(todayLine);
        }

        // 3. Nếu danh sách sau khi lọc trống
        if (state.filteredTasks.length === 0) {
            leftBody.innerHTML = '<div class="p-4 text-center text-muted fs-8"><i class="bi bi-search me-1"></i> Không tìm thấy công việc nào phù hợp!</div>';
            return;
        }

        // 4. Vẽ từng hàng Task
        state.filteredTasks.forEach(function (task, idx) {
            const taskId = task.id;

            // --- A. CỘT TRÁI (Left Row) ---
            const leftRow = document.createElement('div');
            leftRow.className = 'gantt-task-row-left';
            leftRow.setAttribute('data-task-id', taskId);

            const priorityClass = (task.priority || 'medium').toLowerCase();
            const dot = '<span class="gantt-priority-dot ' + priorityClass + '" title="Ưu tiên: ' + task.priority + '"></span>';
            const assigneeAvatar = '<span class="badge bg-light text-dark border rounded-pill px-2 py-0-5 fs-9 text-truncate" style="max-width: 90px;">' +
                escapeHtml(task.assigneeName || 'Chưa gán') + '</span>';

            let dueBadge = '<span class="badge bg-secondary-subtle text-muted fs-9">Chưa đặt</span>';
            if (task.dueDate) {
                const isOverdue = task.isOverdue;
                dueBadge = '<span class="badge ' + (isOverdue ? 'bg-danger-subtle text-danger' : 'bg-primary-subtle text-primary') + ' fs-9 fw-semibold">' +
                    escapeHtml(formatShortDate(task.dueDate)) + '</span>';
            }

            leftRow.innerHTML = dot +
                '<span class="gantt-task-title-cell" title="' + escapeHtml(task.title) + '">' + escapeHtml(task.title) + '</span>' +
                assigneeAvatar + dueBadge;

            leftRow.addEventListener('click', function () {
                openTaskQuickModal(task);
            });

            leftRow.addEventListener('mouseenter', function () {
                syncHover(taskId, true);
            });
            leftRow.addEventListener('mouseleave', function () {
                syncHover(taskId, false);
            });

            leftBody.appendChild(leftRow);

            // --- B. CANVAS PHẢI (Right Canvas Row) ---
            const canvasRow = document.createElement('div');
            canvasRow.className = 'gantt-task-row-canvas';
            canvasRow.setAttribute('data-task-id', taskId);

            if (task.dueDate) {
                const dueD = parseDate(task.dueDate);
                if (dueD) {
                    // Tính ngày bắt đầu ước tính: lùi từ deadline theo khối lượng subtask
                    const subCount = task.subtaskCount || 0;
                    const durationDays = Math.max(3, Math.min(14, subCount > 0 ? (subCount * 2 + 1) : 4));
                    
                    const startD = new Date(dueD);
                    startD.setDate(startD.getDate() - durationDays + 1);

                    const startOffsetDays = Math.round((startD - state.startDate) / (1000 * 60 * 60 * 24));
                    const endOffsetDays = Math.round((dueD - state.startDate) / (1000 * 60 * 60 * 24));

                    const barLeft = Math.max(0, startOffsetDays * state.dayWidth);
                    const barWidth = Math.max(state.dayWidth, (endOffsetDays - startOffsetDays + 1) * state.dayWidth - 4);

                    const bar = document.createElement('div');
                    const statusClass = 'status-' + (task.status || 'todo').toLowerCase();
                    const overdueClass = task.isOverdue ? ' is-overdue' : '';

                    bar.className = 'gantt-bar ' + statusClass + overdueClass;
                    bar.style.left = barLeft + 'px';
                    bar.style.width = barWidth + 'px';
                    bar.setAttribute('data-task-id', taskId);

                    // Thanh tiến độ % việc con tô đậm bên trong
                    const progressPct = task.progress || 0;
                    const progressFill = document.createElement('div');
                    progressFill.className = 'gantt-bar-progress-fill';
                    progressFill.style.width = progressPct + '%';
                    bar.appendChild(progressFill);

                    // Nội dung chữ trong thanh
                    const content = document.createElement('div');
                    content.className = 'gantt-bar-content';
                    content.innerHTML = '<span class="text-truncate">' + escapeHtml(task.title) + '</span>' +
                        (progressPct > 0 ? '<span class="badge bg-dark bg-opacity-25 rounded-pill px-1-5 py-0 fs-10">' + progressPct + '%</span>' : '');
                    bar.appendChild(content);

                    // Sự kiện tương tác thanh Gantt
                    bar.addEventListener('click', function (e) {
                        e.stopPropagation();
                        openTaskQuickModal(task);
                    });

                    bar.addEventListener('mouseenter', function (e) {
                        syncHover(taskId, true);
                        showTooltip(e, task);
                    });

                    bar.addEventListener('mouseleave', function () {
                        syncHover(taskId, false);
                        hideTooltip();
                    });

                    canvasRow.appendChild(bar);
                }
            }

            canvasRow.addEventListener('mouseenter', function () {
                syncHover(taskId, true);
            });
            canvasRow.addEventListener('mouseleave', function () {
                syncHover(taskId, false);
            });

            gridCanvas.appendChild(canvasRow);
        });
    }

    /**
     * 5. Đồng bộ hiệu ứng Hover giữa 2 khung
     */
    function syncHover(taskId, isHover) {
        const leftEl = document.querySelector('.gantt-task-row-left[data-task-id="' + taskId + '"]');
        const rightEl = document.querySelector('.gantt-task-row-canvas[data-task-id="' + taskId + '"]');

        if (leftEl) {
            if (isHover) leftEl.classList.add('is-hovered');
            else leftEl.classList.remove('is-hovered');
        }
        if (rightEl) {
            if (isHover) rightEl.classList.add('is-hovered');
            else rightEl.classList.remove('is-hovered');
        }
    }

    /**
     * 6. Cuộn mượt màn hình đến vạch Hôm nay
     */
    function scrollToToday() {
        const rightPane = document.getElementById('ganttRightPane');
        const marker = document.getElementById('ganttTodayMarker');
        if (!rightPane || !marker) return;

        const markerX = parseFloat(marker.style.left) || 0;
        const targetScroll = Math.max(0, markerX - (rightPane.clientWidth / 2));

        rightPane.scrollTo({
            left: targetScroll,
            behavior: 'smooth'
        });
    }

    /**
     * 7. Hiển thị Tooltip thông tin chi tiết công việc khi hover
     */
    let tooltipEl = null;

    function showTooltip(e, task) {
        if (!tooltipEl) {
            tooltipEl = document.createElement('div');
            tooltipEl.className = 'gantt-tooltip-popover';
            document.body.appendChild(tooltipEl);
        }

        const statusLabel = {
            'TODO': 'Cần làm',
            'IN_PROGRESS': 'Đang làm',
            'SUBMITTED': 'Chờ nghiệm thu (Gate 2)',
            'DONE': 'Hoàn thành'
        }[task.status] || task.status;

        tooltipEl.innerHTML = '<div class="fw-bold text-dark mb-1">' + escapeHtml(task.title) + '</div>' +
            '<div class="d-flex align-items-center gap-1 mb-1 fs-9 text-muted">' +
            '<span>Phụ trách: <strong>' + escapeHtml(task.assigneeName || 'Chưa có') + '</strong></span>' +
            '</div>' +
            '<div class="d-flex justify-content-between align-items-center gap-2 mb-1 fs-9">' +
            '<span>Trạng thái: <strong>' + statusLabel + '</strong></span>' +
            '<span class="badge bg-primary-subtle text-primary">' + (task.progress || 0) + '% việc con</span>' +
            '</div>' +
            '<div class="fs-9 ' + (task.isOverdue ? 'text-danger fw-bold' : 'text-muted') + '">' +
            '<i class="bi bi-calendar-event me-1"></i> Hạn chót: ' + (task.dueDate || 'Chưa thiết lập') +
            (task.isOverdue ? ' (Quá hạn!)' : '') +
            '</div>';

        const rect = e.target.getBoundingClientRect();
        tooltipEl.style.left = Math.min(window.innerWidth - 300, Math.max(10, rect.left)) + 'px';
        tooltipEl.style.top = (rect.bottom + 8) + 'px';
        tooltipEl.classList.add('visible');
    }

    function hideTooltip() {
        if (tooltipEl) {
            tooltipEl.classList.remove('visible');
        }
    }

    /**
     * 8. Modal xem và chỉnh sửa hạn chót nhanh tức thì
     */
    function openTaskQuickModal(task) {
        hideTooltip();

        const modalEl = document.getElementById('quickEditTimelineModal');
        if (!modalEl || !window.bootstrap) return;

        document.getElementById('modalTaskId').value = task.id;
        document.getElementById('modalTaskTitle').textContent = task.title;
        document.getElementById('modalTaskAssignee').textContent = task.assigneeName || 'Chưa phân công';
        document.getElementById('modalTaskStatus').textContent = task.status;
        document.getElementById('modalTaskDueDate').value = task.dueDate || '';

        const progressEl = document.getElementById('modalTaskProgress');
        if (progressEl) {
            progressEl.style.width = (task.progress || 0) + '%';
            progressEl.textContent = (task.progress || 0) + '%';
        }

        const jumpBtn = document.getElementById('modalJumpKanbanBtn');
        if (jumpBtn) {
            jumpBtn.href = state.contextPath + '/task?action=list&projectId=' + state.projectId;
        }

        const bsModal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
        bsModal.show();
    }

    /**
     * 9. Cập nhật hạn chót qua AJAX không cần tải lại trang
     */
    window.submitQuickDueDateUpdate = function () {
        const taskId = document.getElementById('modalTaskId').value;
        const newDueDate = document.getElementById('modalTaskDueDate').value;

        if (!taskId) return;

        const body = new URLSearchParams();
        body.append('action', 'updateDueDate');
        body.append('projectId', state.projectId);
        body.append('taskId', taskId);
        body.append('newDueDate', newDueDate);
        body.append('isAjax', 'true');

        fetch(state.contextPath + '/timeline', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: body.toString()
        })
        .then(res => res.json())
        .then(data => {
            if (data.ok) {
                // Cập nhật state cục bộ
                const t = state.tasks.find(x => String(x.id) === String(taskId));
                if (t) {
                    t.dueDate = newDueDate;
                    t.isOverdue = data.isOverdue;
                    t.deadlineStatus = data.deadlineStatus;
                }

                calculateHorizon();
                renderGantt();

                // Đóng modal
                const modalEl = document.getElementById('quickEditTimelineModal');
                if (modalEl && window.bootstrap) {
                    const bsModal = bootstrap.Modal.getInstance(modalEl);
                    if (bsModal) bsModal.hide();
                }

                if (window.showToast) {
                    window.showToast('Đã lưu hạn chót mới thành công!', 'success');
                } else {
                    alert('Đã lưu hạn chót mới thành công!');
                }
            } else {
                alert(data.message || 'Lỗi khi cập nhật hạn chót!');
            }
        })
        .catch(err => {
            console.error('Lỗi kết nối máy chủ:', err);
            alert('Không thể kết nối máy chủ để lưu hạn chót!');
        });
    };

    /**
     * Tiện ích ngày tháng
     */
    function parseDate(str) {
        if (!str || typeof str !== 'string') return null;
        const parts = str.trim().split('-');
        if (parts.length === 3) {
            const y = parseInt(parts[0], 10);
            const m = parseInt(parts[1], 10) - 1;
            const d = parseInt(parts[2], 10);
            return new Date(y, m, d, 0, 0, 0, 0);
        }
        return null;
    }

    function isSameDay(d1, d2) {
        if (!d1 || !d2) return false;
        return d1.getFullYear() === d2.getFullYear() &&
               d1.getMonth() === d2.getMonth() &&
               d1.getDate() === d2.getDate();
    }

    function formatShortDate(str) {
        if (!str) return '';
        const p = str.split('-');
        if (p.length === 3) return p[2] + '/' + p[1];
        return str;
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

})();
