/**
 * =========================================================================
 * TEAMWORK HUB — INTERACTIVE GANTT TIMELINE & DEPENDENCY ENGINE (timeline.js)
 * Tích hợp Frappe Gantt 0.6.1 + Vanilla JS Module
 * 
 * Tính năng chính:
 * 1. Biểu đồ Frappe Gantt 0.6.1 với mũi tên liên kết phụ thuộc (Dependency Arrows)
 * 2. Kéo thả (Drag & Drop) và co giãn thời gian (Resize) cập nhật ngày qua AJAX
 * 3. Tự động dời lịch dây chuyền (Cascade Auto-Scheduling) khi việc trước bị trễ
 * 4. Huy hiệu khóa (Prerequisite Lock Badge) khi việc tiên quyết chưa DONE
 * 5. Quản lý liên kết phụ thuộc (Thêm / Xóa / Cảnh báo vòng lặp Cycle Detection)
 * 6. Bộ lọc tức thì: Thành viên, Mức ưu tiên, Trạng thái, Ô tìm kiếm
 * 7. Đồng bộ danh sách công việc bên trái với thanh tiến độ Gantt bên phải
 * =========================================================================
 */

(function () {
    'use strict';

    // State quản lý toàn cục
    const state = {
        zoom: 'Day', // 'Day' | 'Week' | 'Month'
        tasks: [],
        dependencies: [],
        filteredTasks: [],
        todayStr: '',
        projectId: 0,
        contextPath: '',
        searchQuery: '',
        filterMember: 'ALL',
        filterPriority: 'ALL',
        filterStatus: 'ALL'
    };

    let frappeGanttInstance = null;

    document.addEventListener('DOMContentLoaded', function () {
        initEngine();
    });

    /**
     * Khởi tạo động cơ
     */
    function initEngine() {
        if (!window.timelineConfig) return;

        state.projectId = window.timelineConfig.projectId;
        state.contextPath = window.timelineConfig.contextPath || '';
        state.todayStr = window.timelineConfig.today || new Date().toISOString().split('T')[0];
        state.tasks = Array.isArray(window.timelineTasksData) ? window.timelineTasksData : [];
        state.dependencies = Array.isArray(window.timelineDependenciesData) ? window.timelineDependenciesData : [];
        state.filteredTasks = [...state.tasks];

        bindControls();
        applyFilters();
        renderLeftPane();
        renderDependenciesTable();
        initFrappeGantt();

        // Cuộn về mốc hôm nay sau khi vẽ
        setTimeout(function () {
            scrollToToday();
        }, 300);
    }

    /**
     * Chuyển đổi dữ liệu sang định dạng yêu cầu của Frappe Gantt
     */
    function transformTasksForFrappe(taskList, depList) {
        // Gom danh sách phụ thuộc: Successor ID -> Mảng các Predecessor IDs
        const predMap = {};
        (depList || []).forEach(function (dep) {
            const succId = String(dep.taskId);
            const predId = String(dep.dependsOnTaskId);
            if (!predMap[succId]) predMap[succId] = [];
            if (!predMap[succId].includes(predId)) {
                predMap[succId].push(predId);
            }
        });

        const activeTaskIds = new Set(taskList.map(function (t) { return String(t.id); }));

        return taskList.map(function (t) {
            const tid = String(t.id);

            // Xác định ngày bắt đầu và kết thúc chuẩn YYYY-MM-DD
            let start = t.effectiveStartDate || t.startDate;
            if (!start || start.trim() === '') {
                start = t.dueDate ? shiftDateStr(t.dueDate, -3) : state.todayStr;
            }

            let end = t.dueDate;
            if (!end || end.trim() === '') {
                end = shiftDateStr(start, 3);
            }

            // Đảm bảo end không trước start
            if (end < start) {
                end = start;
            }

            // Lọc các dependencies chỉ thuộc danh sách task đang hiển thị
            const preds = (predMap[tid] || []).filter(function (pid) {
                return activeTaskIds.has(pid);
            });
            const depStr = preds.join(', ');

            // Kiểm tra trạng thái khóa (Prerequisite Lock)
            const isLocked = checkTaskLocked(t.id, depList, state.tasks);

            // CSS Class tùy biến
            const statusKey = (t.status || 'TODO').toLowerCase();
            let customClass = 'gantt-bar-' + statusKey;
            if (t.isOverdue) customClass += ' gantt-bar-overdue';
            if (isLocked) customClass += ' gantt-bar-locked';

            const progress = typeof t.progress === 'number' ? t.progress : (t.status === 'DONE' ? 100 : 0);

            return {
                id: tid,
                name: t.title || 'Công việc #' + tid,
                start: start,
                end: end,
                progress: progress,
                dependencies: depStr,
                custom_class: customClass,
                rawTask: t,
                isLocked: isLocked
            };
        });
    }

    /**
     * Khởi tạo hoặc cập nhật Frappe Gantt
     */
    function initFrappeGantt() {
        const targetSvg = document.getElementById('frappeGanttChart');
        const container = document.getElementById('frappeGanttContainer');
        if (!targetSvg || !container) return;

        if (typeof window.Gantt === 'undefined') {
            console.error('Thư viện Frappe Gantt chưa được nạp!');
            return;
        }

        const frappeTasks = transformTasksForFrappe(state.filteredTasks, state.dependencies);

        if (frappeTasks.length === 0) {
            container.innerHTML = '<div class="empty-state py-5 text-center">'
                + '<i class="bi bi-calendar2-range fs-1 text-muted"></i>'
                + '<div class="fw-semibold mt-2 text-dark">Không có công việc nào khớp bộ lọc</div>'
                + '<p class="text-muted fs-8">Hãy thử thay đổi từ khóa tìm kiếm hoặc bỏ chọn các bộ lọc phía trên.</p>'
                + '</div>';
            return;
        }

        // Đảm bảo thẻ svg tồn tại
        if (!document.getElementById('frappeGanttChart')) {
            container.innerHTML = '<svg id="frappeGanttChart"></svg>';
        }

        try {
            frappeGanttInstance = new Gantt('#frappeGanttChart', frappeTasks, {
                header_height: 50,
                column_width: state.zoom === 'Month' ? 110 : (state.zoom === 'Week' ? 55 : 36),
                step: 24,
                view_modes: ['Day', 'Week', 'Month'],
                bar_height: 28,
                bar_corner_radius: 6,
                arrow_curve: 5,
                padding: 16,
                view_mode: state.zoom,
                date_format: 'YYYY-MM-DD',
                popup_trigger: 'click',
                custom_popup_html: function (task) {
                    return generatePopupHtml(task);
                },
                on_click: function (task) {
                    highlightTask(task.id);
                },
                on_date_change: function (task, start, end) {
                    const startStr = formatDateObj(start);
                    const endStr = formatDateObj(end);
                    handleDateChange(task.id, startStr, endStr);
                },
                on_progress_change: function (task, progress) {
                    // Tiến độ được tự động đồng bộ từ Subtasks
                },
                on_view_change: function (mode) {
                    state.zoom = mode;
                }
            });
        } catch (e) {
            console.error('Lỗi khi vẽ biểu đồ Frappe Gantt:', e);
        }
    }

    /**
     * Sinh HTML Tooltip Popup cho Task trên Gantt
     */
    function generatePopupHtml(task) {
        const raw = task.rawTask || state.tasks.find(x => String(x.id) === String(task.id)) || {};
        const isLocked = checkTaskLocked(raw.id, state.dependencies, state.tasks);

        let lockNotice = '';
        if (isLocked) {
            const unfinishedTitles = getUnfinishedPrereqTitles(raw.id, state.dependencies, state.tasks);
            lockNotice = '<div class="alert alert-danger py-1 px-2 fs-9 mb-2 rounded-2 border-0">'
                + '<i class="bi bi-lock-fill me-1"></i><strong>Đang bị khóa:</strong> Cần hoàn thành '
                + escapeHtml(unfinishedTitles.join(', '))
                + ' trước khi bắt đầu!</div>';
        }

        const priorityLabel = raw.priority === 'HIGH' ? '🔴 Cao' : (raw.priority === 'LOW' ? '🟢 Thấp' : '🟡 Trung bình');
        const statusLabel = getStatusLabel(raw.status);

        return '<div class="gantt-custom-popup p-3 shadow-lg rounded-3 border bg-white" style="min-width: 260px; max-width: 320px;">'
            + '<div class="d-flex justify-content-between align-items-start gap-2 mb-2">'
            + '  <h6 class="fw-bold mb-0 text-dark fs-8 text-truncate" title="' + escapeHtml(task.name) + '">#' + task.id + ' ' + escapeHtml(task.name) + '</h6>'
            + '  <span class="badge ' + getStatusBadgeClass(raw.status) + ' fs-10">' + statusLabel + '</span>'
            + '</div>'
            + lockNotice
            + '<div class="fs-9 text-muted mb-2 lh-base">'
            + '  <div><i class="bi bi-person me-1"></i> Phụ trách: <strong class="text-dark">' + escapeHtml(raw.assigneeName || 'Chưa phân công') + '</strong></div>'
            + '  <div><i class="bi bi-flag me-1"></i> Mức ưu tiên: <strong class="text-dark">' + priorityLabel + '</strong></div>'
            + '  <div><i class="bi bi-calendar-event me-1"></i> Bắt đầu: <strong class="text-dark">' + task.start + '</strong></div>'
            + '  <div><i class="bi bi-calendar-check me-1"></i> Hạn chót: <strong class="text-dark">' + task.end + '</strong></div>'
            + '  <div><i class="bi bi-pie-chart me-1"></i> Tiến độ: <strong class="text-success">' + task.progress + '%</strong></div>'
            + '</div>'
            + '<div class="d-flex gap-2 pt-2 border-top">'
            + '  <button type="button" class="btn btn-xs btn-outline-primary rounded-pill px-2-5 fs-9" onclick="window.openQuickEditModal(' + task.id + ')">'
            + '    <i class="bi bi-pencil me-1"></i> Đổi hạn'
            + '  </button>'
            + '  <button type="button" class="btn btn-xs btn-outline-secondary rounded-pill px-2-5 fs-9" onclick="window.openDependencyModalForTask(' + task.id + ')">'
            + '    <i class="bi bi-diagram-3 me-1"></i> Nối phụ thuộc'
            + '  </button>'
            + '</div>'
            + '</div>';
    }

    /**
     * Xử lý sự kiện kéo thả hoặc co giãn thanh Gantt Bar -> Gửi AJAX lưu lịch & Cascade
     */
    function handleDateChange(taskId, newStart, newEnd) {
        const body = new URLSearchParams();
        body.append('action', 'updateDates');
        body.append('projectId', state.projectId);
        body.append('taskId', taskId);
        body.append('startDate', newStart);
        body.append('dueDate', newEnd);
        body.append('isAjax', 'true');

        window.apiFetch(state.contextPath + '/timeline', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: body.toString()
        })
        .then(function (data) {
            if (data && data.ok) {
                if (data.shiftedCount && data.shiftedCount > 1) {
                    showToast('Đã lưu lịch và tự động dời lịch dây chuyền (Cascade) ' + (data.shiftedCount - 1) + ' công việc phụ thuộc!', 'success');
                } else {
                    showToast('Đã cập nhật lịch trình thành công!', 'success');
                }
                // Nạp lại toàn bộ dữ liệu mới nhất từ server để đồng bộ mọi mũi tên và thanh trượt
                reloadGanttData();
            } else {
                const msg = (data && data.message) ? data.message : 'Không thể lưu lịch trình mới!';
                showToast(msg, 'danger');
                reloadGanttData(); // Khôi phục lại trạng thái cũ
            }
        })
        .catch(function (err) {
            console.error('Lỗi khi gửi cập nhật lịch trình:', err);
            showToast('Lỗi kết nối khi cập nhật ngày!', 'danger');
            reloadGanttData();
        });
    }

    /**
     * Nạp lại dữ liệu Gantt từ Server qua JSON endpoint (action=ganttData)
     */
    function reloadGanttData() {
        window.apiFetch(state.contextPath + '/timeline?action=ganttData&projectId=' + state.projectId, {
            headers: { 'Accept': 'application/json' }
        })
        .then(function (data) {
            if (data && data.ok && Array.isArray(data.tasks)) {
                state.tasks = data.tasks;
                state.dependencies = data.dependencies || [];
                applyFilters();
                renderLeftPane();
                renderDependenciesTable();
                if (frappeGanttInstance) {
                    const fTasks = transformTasksForFrappe(state.filteredTasks, state.dependencies);
                    frappeGanttInstance.refresh(fTasks);
                } else {
                    initFrappeGantt();
                }
            }
        })
        .catch(function (err) {
            console.error('Lỗi nạp dữ liệu Gantt:', err);
        });
    }

    /**
     * Vẽ danh sách cây công việc bên trái kèm huy hiệu Khóa
     */
    function renderLeftPane() {
        const leftBody = document.getElementById('ganttLeftBody');
        const filterMatchCount = document.getElementById('filterMatchCount');
        if (!leftBody) return;

        leftBody.innerHTML = '';
        if (filterMatchCount) {
            filterMatchCount.textContent = state.filteredTasks.length + ' / ' + state.tasks.length;
        }

        if (state.filteredTasks.length === 0) {
            leftBody.innerHTML = '<div class="p-3 text-center text-muted fs-9">Không có công việc nào khớp bộ lọc</div>';
            return;
        }

        state.filteredTasks.forEach(function (task) {
            const row = document.createElement('div');
            row.className = 'gantt-task-row-left d-flex align-items-center justify-content-between px-3 py-2 border-bottom';
            row.setAttribute('data-task-id', task.id);

            const isLocked = checkTaskLocked(task.id, state.dependencies, state.tasks);

            const priorityClass = (task.priority || 'medium').toLowerCase();
            const dot = '<span class="gantt-priority-dot ' + priorityClass + '" title="Ưu tiên: ' + task.priority + '"></span>';

            let lockBadge = '';
            if (isLocked) {
                lockBadge = '<span class="badge bg-danger-subtle text-danger border border-danger-subtle rounded-pill px-1-5 py-0 fs-10 ms-1" title="Khóa: Việc tiên quyết chưa hoàn thành">'
                    + '<i class="bi bi-lock-fill"></i> Khóa</span>';
            }

            const titleSpan = '<span class="gantt-task-title-cell text-truncate fw-medium text-dark flex-grow-1 mx-2" style="max-width: 170px;" title="' + escapeHtml(task.title) + '">'
                + escapeHtml(task.title) + '</span>';

            const assigneeSpan = '<span class="badge bg-light text-secondary border rounded-pill px-2 py-0-5 fs-10 text-truncate" style="max-width: 85px;">'
                + escapeHtml(task.assigneeName || 'Chưa gán') + '</span>';

            row.innerHTML = '<div class="d-flex align-items-center flex-grow-1 overflow-hidden">'
                + dot + titleSpan + lockBadge + '</div>'
                + assigneeSpan;

            row.addEventListener('click', function () {
                window.openQuickEditModal(task.id);
            });

            row.addEventListener('mouseenter', function () {
                highlightTask(task.id);
            });

            leftBody.appendChild(row);
        });
    }

    /**
     * Bảng danh sách liên kết phụ thuộc trong Modal
     */
    function renderDependenciesTable() {
        const tbody = document.getElementById('dependenciesTableBody');
        const countBadge = document.getElementById('depListCount');
        if (!tbody) return;

        tbody.innerHTML = '';
        if (countBadge) {
            countBadge.textContent = state.dependencies.length;
        }

        if (state.dependencies.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-muted fs-8">'
                + '<i class="bi bi-diagram-3 fs-3 d-block mb-1 text-muted"></i>'
                + 'Chưa có quan hệ phụ thuộc nào trong dự án này. Hãy tạo liên kết ở khung phía trên!</td></tr>';
            return;
        }

        state.dependencies.forEach(function (dep) {
            const tr = document.createElement('tr');

            const predTask = state.tasks.find(t => t.id === dep.dependsOnTaskId);
            const succTask = state.tasks.find(t => t.id === dep.taskId);

            const predTitle = predTask ? predTask.title : (dep.dependsOnTaskTitle || 'Task #' + dep.dependsOnTaskId);
            const succTitle = succTask ? succTask.title : (dep.taskTitle || 'Task #' + dep.taskId);

            const predStatus = predTask ? predTask.status : (dep.dependsOnTaskStatus || '');
            const predStatusBadge = getStatusBadgeClass(predStatus);

            tr.innerHTML = '<td class="ps-3">'
                + '  <span class="fw-semibold text-dark">#' + dep.dependsOnTaskId + ' ' + escapeHtml(predTitle) + '</span>'
                + '  <span class="badge ' + predStatusBadge + ' fs-10 ms-1">' + getStatusLabel(predStatus) + '</span>'
                + '</td>'
                + '<td class="text-center text-muted"><i class="bi bi-arrow-right-circle-fill text-primary"></i></td>'
                + '<td><span class="fw-semibold text-dark">#' + dep.taskId + ' ' + escapeHtml(succTitle) + '</span></td>'
                + '<td><span class="badge bg-light text-secondary border">Finish-to-Start' + (dep.lagDays > 0 ? ' +' + dep.lagDays + 'd' : '') + '</span></td>'
                + '<td class="text-end pe-3">'
                + '  <button type="button" class="btn btn-xs btn-outline-danger rounded-pill px-2 py-0-5" title="Xóa liên kết" onclick="window.deleteDependency(' + dep.id + ')">'
                + '    <i class="bi bi-trash"></i>'
                + '  </button>'
                + '</td>';

            tbody.appendChild(tr);
        });
    }

    /**
     * Gắn sự kiện cho các điều khiển trên thanh công cụ
     */
    function bindControls() {
        // Nút Zoom: Ngày / Tuần / Tháng
        document.querySelectorAll('.timeline-zoom-btn').forEach(function (btn) {
            btn.addEventListener('click', function () {
                document.querySelectorAll('.timeline-zoom-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                const zoomMode = btn.getAttribute('data-zoom');
                const frappeMode = zoomMode === 'month' ? 'Month' : (zoomMode === 'week' ? 'Week' : 'Day');
                state.zoom = frappeMode;
                if (frappeGanttInstance) {
                    frappeGanttInstance.change_view_mode(frappeMode);
                }
            });
        });

        // Nút Cuộn về Hôm nay
        const btnToday = document.getElementById('btnScrollToday');
        if (btnToday) {
            btnToday.addEventListener('click', function () {
                scrollToToday();
            });
        }

        // Ô tìm kiếm công việc
        const searchInput = document.getElementById('timelineSearchInput');
        if (searchInput) {
            searchInput.addEventListener('input', function () {
                state.searchQuery = this.value.trim().toLowerCase();
                applyFilters();
                renderLeftPane();
                if (frappeGanttInstance) {
                    frappeGanttInstance.refresh(transformTasksForFrappe(state.filteredTasks, state.dependencies));
                }
            });
        }

        // Bộ lọc thành viên
        const memberFilter = document.getElementById('timelineMemberFilter');
        if (memberFilter) {
            memberFilter.addEventListener('change', function () {
                state.filterMember = this.value;
                applyFilters();
                renderLeftPane();
                if (frappeGanttInstance) {
                    frappeGanttInstance.refresh(transformTasksForFrappe(state.filteredTasks, state.dependencies));
                }
            });
        }

        // Bộ lọc mức ưu tiên
        const priorityFilter = document.getElementById('timelinePriorityFilter');
        if (priorityFilter) {
            priorityFilter.addEventListener('change', function () {
                state.filterPriority = this.value;
                applyFilters();
                renderLeftPane();
                if (frappeGanttInstance) {
                    frappeGanttInstance.refresh(transformTasksForFrappe(state.filteredTasks, state.dependencies));
                }
            });
        }

        // Bộ lọc trạng thái
        const statusFilter = document.getElementById('timelineStatusFilter');
        if (statusFilter) {
            statusFilter.addEventListener('change', function () {
                state.filterStatus = this.value;
                applyFilters();
                renderLeftPane();
                if (frappeGanttInstance) {
                    frappeGanttInstance.refresh(transformTasksForFrappe(state.filteredTasks, state.dependencies));
                }
            });
        }
    }

    /**
     * Lọc danh sách công việc theo từ khóa & các dropdown
     */
    function applyFilters() {
        state.filteredTasks = state.tasks.filter(function (t) {
            if (state.searchQuery) {
                const title = (t.title || '').toLowerCase();
                const assignee = (t.assigneeName || '').toLowerCase();
                if (!title.includes(state.searchQuery) && !assignee.includes(state.searchQuery)) {
                    return false;
                }
            }

            if (state.filterMember !== 'ALL') {
                if (String(t.assigneeId) !== String(state.filterMember)) {
                    return false;
                }
            }

            if (state.filterPriority !== 'ALL') {
                if ((t.priority || '').toUpperCase() !== state.filterPriority) {
                    return false;
                }
            }

            if (state.filterStatus !== 'ALL') {
                if (state.filterStatus === 'OVERDUE') {
                    if (!t.isOverdue) return false;
                } else if ((t.status || '').toUpperCase() !== state.filterStatus) {
                    return false;
                }
            }

            return true;
        });
    }

    /**
     * Cuộn khung nhìn Frappe Gantt về vị trí mốc Hôm nay
     */
    function scrollToToday() {
        const container = document.getElementById('frappeGanttContainer');
        if (!container) return;

        // Tìm kiếm phần tử đánh dấu hôm nay hoặc tính toán tỷ lệ cuộn
        const todayEl = container.querySelector('.current-date-highlight') || container.querySelector('.today-highlight');
        if (todayEl) {
            todayEl.scrollIntoView({ behavior: 'smooth', inline: 'center' });
        } else {
            // Cuộn 1/3 khung nhìn để thấy được các task đang chạy
            container.scrollTo({ left: 150, behavior: 'smooth' });
        }
    }

    /**
     * Highlight hàng công việc khi tương tác
     */
    function highlightTask(taskId) {
        document.querySelectorAll('.gantt-task-row-left').forEach(function (el) {
            el.classList.remove('is-active');
        });
        const target = document.querySelector('.gantt-task-row-left[data-task-id="' + taskId + '"]');
        if (target) {
            target.classList.add('is-active');
        }
    }

    // =========================================================================
    // MODAL FUNCTIONS (GLOBAL ATTACHMENT)
    // =========================================================================

    /**
     * Mở modal chỉnh sửa nhanh hạn chót & ngày bắt đầu
     */
    window.openQuickEditModal = function (taskId) {
        const task = state.tasks.find(t => String(t.id) === String(taskId));
        if (!task) return;

        document.getElementById('modalTaskId').value = task.id;
        document.getElementById('modalTaskTitle').textContent = task.title;
        document.getElementById('modalTaskAssignee').textContent = task.assigneeName || 'Chưa phân công';
        document.getElementById('modalTaskStatus').textContent = getStatusLabel(task.status);

        const progEl = document.getElementById('modalTaskProgress');
        if (progEl) {
            const p = task.progress || 0;
            progEl.style.width = p + '%';
            progEl.textContent = p + '%';
        }

        const startInput = document.getElementById('modalTaskStartDate');
        if (startInput) {
            startInput.value = task.startDate || task.effectiveStartDate || '';
        }

        const dueInput = document.getElementById('modalTaskDueDate');
        if (dueInput) {
            dueInput.value = task.dueDate || '';
        }

        const jumpBtn = document.getElementById('modalJumpKanbanBtn');
        if (jumpBtn) {
            jumpBtn.href = state.contextPath + '/task?action=list&projectId=' + state.projectId;
        }

        const modalEl = document.getElementById('quickEditTimelineModal');
        if (modalEl && window.bootstrap) {
            const bsModal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
            bsModal.show();
        }
    };

    /**
     * Lưu thay đổi ngày từ Quick Edit Modal
     */
    window.submitQuickDueDateUpdate = function () {
        const taskId = document.getElementById('modalTaskId').value;
        const newStartDate = document.getElementById('modalTaskStartDate') ? document.getElementById('modalTaskStartDate').value : '';
        const newDueDate = document.getElementById('modalTaskDueDate').value;

        if (!taskId) return;

        if (newStartDate && newDueDate && newDueDate < newStartDate) {
            alert('Hạn chót không được sớm hơn ngày bắt đầu!');
            return;
        }

        handleDateChange(taskId, newStartDate, newDueDate);

        const modalEl = document.getElementById('quickEditTimelineModal');
        if (modalEl && window.bootstrap) {
            const bsModal = bootstrap.Modal.getInstance(modalEl);
            if (bsModal) bsModal.hide();
        }
    };

    /**
     * Mở modal quản lý phụ thuộc với Successor được chọn sẵn
     */
    window.openDependencyModalForTask = function (taskId) {
        const succSelect = document.getElementById('depSuccessorSelect');
        if (succSelect) {
            succSelect.value = taskId;
        }
        const modalEl = document.getElementById('dependencyManagerModal');
        if (modalEl && window.bootstrap) {
            const bsModal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
            bsModal.show();
        }
    };

    /**
     * Thêm mới liên kết phụ thuộc qua AJAX
     */
    window.submitAddDependency = function () {
        const predId = document.getElementById('depPredecessorSelect').value;
        const succId = document.getElementById('depSuccessorSelect').value;
        const warningEl = document.getElementById('depCycleWarning');

        if (warningEl) warningEl.classList.add('d-none');

        if (predId === '0' || succId === '0') {
            alert('Vui lòng chọn cả công việc tiên quyết và công việc chịu phụ thuộc!');
            return;
        }

        if (predId === succId) {
            alert('Một công việc không thể tự phụ thuộc chính nó!');
            return;
        }

        const body = new URLSearchParams();
        body.append('action', 'addDependency');
        body.append('projectId', state.projectId);
        body.append('taskId', succId);
        body.append('dependsOnTaskId', predId);
        body.append('isAjax', 'true');

        window.apiFetch(state.contextPath + '/timeline', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: body.toString()
        })
        .then(function (data) {
            if (data && data.ok) {
                showToast('Đã thiết lập liên kết phụ thuộc thành công!', 'success');
                // Reset form
                document.getElementById('depPredecessorSelect').value = '0';
                document.getElementById('depSuccessorSelect').value = '0';
                reloadGanttData();
            } else {
                const msg = (data && data.message) ? data.message : 'Không thể lưu liên kết phụ thuộc!';
                if (warningEl) {
                    warningEl.textContent = '⚠️ ' + msg;
                    warningEl.classList.remove('d-none');
                } else {
                    alert(msg);
                }
            }
        })
        .catch(function (err) {
            console.error('Lỗi khi thêm liên kết phụ thuộc:', err);
            alert('Không thể kết nối máy chủ để lưu liên kết phụ thuộc!');
        });
    };

    /**
     * Xóa liên kết phụ thuộc qua AJAX
     */
    window.deleteDependency = function (depId) {
        if (!confirm('Bạn có chắc chắn muốn hủy bỏ liên kết phụ thuộc này?')) return;

        const body = new URLSearchParams();
        body.append('action', 'deleteDependency');
        body.append('projectId', state.projectId);
        body.append('dependencyId', depId);
        body.append('isAjax', 'true');

        window.apiFetch(state.contextPath + '/timeline', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: body.toString()
        })
        .then(function (data) {
            if (data && data.ok) {
                showToast('Đã xóa liên kết phụ thuộc thành công!', 'success');
                reloadGanttData();
            } else {
                alert((data && data.message) ? data.message : 'Không thể xóa liên kết phụ thuộc!');
            }
        })
        .catch(function (err) {
            console.error('Lỗi khi xóa liên kết phụ thuộc:', err);
            alert('Lỗi kết nối khi xóa liên kết!');
        });
    };

    // =========================================================================
    // TIỆN ÍCH HELPER FUNCTIONS
    // =========================================================================

    function checkTaskLocked(taskId, depList, allTasks) {
        if (!depList || depList.length === 0) return false;
        const taskMap = {};
        (allTasks || []).forEach(t => { taskMap[t.id] = t; });

        for (let i = 0; i < depList.length; i++) {
            const dep = depList[i];
            if (dep.taskId === taskId) {
                const pred = taskMap[dep.dependsOnTaskId];
                if (pred && (pred.status || '').toUpperCase() !== 'DONE' && (pred.status || '').toUpperCase() !== 'APPROVED') {
                    return true;
                }
            }
        }
        return false;
    }

    function getUnfinishedPrereqTitles(taskId, depList, allTasks) {
        const titles = [];
        const taskMap = {};
        (allTasks || []).forEach(t => { taskMap[t.id] = t; });

        (depList || []).forEach(function (dep) {
            if (dep.taskId === taskId) {
                const pred = taskMap[dep.dependsOnTaskId];
                if (pred && (pred.status || '').toUpperCase() !== 'DONE' && (pred.status || '').toUpperCase() !== 'APPROVED') {
                    titles.push(pred.title || ('Task #' + pred.id));
                }
            }
        });
        return titles;
    }

    function shiftDateStr(dateStr, days) {
        if (!dateStr) return '';
        try {
            const p = dateStr.split('-');
            const d = new Date(parseInt(p[0], 10), parseInt(p[1], 10) - 1, parseInt(p[2], 10));
            d.setDate(d.getDate() + days);
            return formatDateObj(d);
        } catch (e) {
            return dateStr;
        }
    }

    function formatDateObj(d) {
        if (!d || !(d instanceof Date)) return '';
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return y + '-' + m + '-' + day;
    }

    function getStatusLabel(status) {
        switch ((status || '').toUpperCase()) {
            case 'DONE': return 'Đã hoàn thành';
            case 'IN_PROGRESS': return 'Đang thực hiện';
            case 'SUBMITTED': return 'Chờ nghiệm thu';
            case 'PLANNING': return 'Lập kế hoạch';
            case 'REVISE': return 'Yêu cầu sửa';
            case 'REJECTED': return 'Từ chối';
            default: return 'Cần làm';
        }
    }

    function getStatusBadgeClass(status) {
        switch ((status || '').toUpperCase()) {
            case 'DONE': return 'bg-success-subtle text-success border border-success-subtle';
            case 'IN_PROGRESS': return 'bg-primary-subtle text-primary border border-primary-subtle';
            case 'SUBMITTED': return 'bg-warning-subtle text-dark border border-warning-subtle';
            case 'PLANNING': return 'bg-info-subtle text-info border border-info-subtle';
            case 'REVISE': return 'bg-warning text-dark';
            case 'REJECTED': return 'bg-danger text-white';
            default: return 'bg-secondary-subtle text-secondary border';
        }
    }

    function showToast(message, type) {
        if (window.showToast) {
            window.showToast(message, type || 'info');
        } else {
            console.log('[' + (type || 'info') + '] ' + message);
        }
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
