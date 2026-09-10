/**
 * =========================================================================
 * TEAMWORK HUB - CLICKUP 3.0 CALENDAR & SCHEDULE ENGINE
 * Tự phát triển 100% Vanilla JS (Zero Dependency / Native Drag & Drop)
 * Quản trị Lưới Tháng, Lưới Tuần, Kéo Thả Dời Hạn Chót, Thanh Bên Chưa Xếp Lịch
 * =========================================================================
 */
(function() {
    'use strict';

    // State của Cuốn Lịch
    const CalendarState = {
        currentDate: new Date(),
        viewMode: 'month', // 'month' | 'week'
        activeSubView: 'calendar', // 'calendar' | 'lanes'
        filters: {
            hideClosed: false,
            assigneeId: 'ALL',
            priority: 'ALL',
            keyword: ''
        },
        unscheduledSidebarOpen: true,
        draggedTaskId: null
    };

    // Chuỗi tên các thứ trong tuần (Bắt đầu từ Thứ 2 theo chuẩn ISO/Việt Nam)
    const DAY_NAMES_SHORT = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
    const DAY_NAMES_FULL = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ Nhật'];
    const MONTH_NAMES = [
        'Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
        'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'
    ];

    /**
     * Định dạng Date object thành chuỗi "YYYY-MM-DD" an toàn múi giờ địa phương
     */
    function formatDateIso(d) {
        if (!d || isNaN(d.getTime())) return '';
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    /**
     * Parse chuỗi "YYYY-MM-DD" thành Date object chuẩn không lệch múi giờ
     */
    function parseDateIso(str) {
        if (!str || typeof str !== 'string') return null;
        const parts = str.trim().split('-');
        if (parts.length !== 3) return null;
        const y = parseInt(parts[0], 10);
        const m = parseInt(parts[1], 10) - 1;
        const d = parseInt(parts[2], 10);
        return new Date(y, m, d);
    }

    /**
     * Kiểm tra xem 2 ngày có cùng ngày/tháng/năm hay không
     */
    function isSameDate(d1, d2) {
        if (!d1 || !d2) return false;
        return d1.getFullYear() === d2.getFullYear() &&
               d1.getMonth() === d2.getMonth() &&
               d1.getDate() === d2.getDate();
    }

    /**
     * Thoát ký tự HTML chống tấn công XSS
     */
    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    /**
     * Lấy danh sách công việc đã được lọc
     */
    function getFilteredTasks() {
        const allTasks = window.projectCalendarTasks || [];
        return allTasks.filter(t => {
            // Lọc việc đã đóng (DONE / APPROVED)
            if (CalendarState.filters.hideClosed) {
                const s = (t.status || '').toUpperCase();
                if (s === 'DONE' || s === 'APPROVED') return false;
            }
            // Lọc theo người phụ trách
            if (CalendarState.filters.assigneeId !== 'ALL') {
                if (CalendarState.filters.assigneeId === 'MY_TASKS') {
                    const curUserId = window.currentSessionUserId || 0;
                    if (t.assigneeId !== curUserId) return false;
                } else {
                    const targetId = parseInt(CalendarState.filters.assigneeId, 10);
                    if (t.assigneeId !== targetId) return false;
                }
            }
            // Lọc theo mức ưu tiên
            if (CalendarState.filters.priority !== 'ALL') {
                if ((t.priority || '').toUpperCase() !== CalendarState.filters.priority) return false;
            }
            // Lọc theo từ khóa tìm kiếm
            if (CalendarState.filters.keyword) {
                const kw = CalendarState.filters.keyword.toLowerCase();
                const titleMatch = (t.title || '').toLowerCase().includes(kw);
                const assigneeMatch = (t.assigneeName || '').toLowerCase().includes(kw);
                if (!titleMatch && !assigneeMatch) return false;
            }
            return true;
        });
    }

    /**
     * Khởi tạo giao diện Cuốn Lịch sau khi DOM tải hoàn tất
     */
    function initCalendar() {
        const scheduleContainer = document.getElementById('clickup-view-schedule');
        if (!scheduleContainer) return;

        // Nạp danh sách công việc an toàn từ DOM elements nếu có
        const rawItems = document.querySelectorAll('.raw-calendar-task-item');
        if (rawItems && rawItems.length > 0) {
            window.projectCalendarTasks = Array.from(rawItems).map(el => ({
                id: parseInt(el.getAttribute('data-id'), 10),
                projectId: parseInt(el.getAttribute('data-project-id'), 10),
                title: el.getAttribute('data-title') || '',
                status: el.getAttribute('data-status') || 'TODO',
                statusLabel: el.getAttribute('data-status-label') || '',
                statusBadgeClass: el.getAttribute('data-status-badge') || '',
                priority: el.getAttribute('data-priority') || 'MEDIUM',
                priorityLabel: el.getAttribute('data-priority-label') || '',
                priorityBadgeClass: el.getAttribute('data-priority-badge') || '',
                dueDate: el.getAttribute('data-due-date') || '',
                assigneeId: parseInt(el.getAttribute('data-assignee-id'), 10) || 0,
                assigneeName: el.getAttribute('data-assignee-name') || 'Chưa giao',
                isOverdue: el.getAttribute('data-is-overdue') === 'true'
            }));
        }

        // Gắn sự kiện thanh công cụ
        bindToolbarEvents();

        // Gắn sự kiện đóng popover khi click ngoài
        document.addEventListener('click', function(e) {
            const popover = document.getElementById('calendarDayPopover');
            if (popover && !popover.contains(e.target) && !e.target.closest('.calendar-more-badge')) {
                popover.remove();
            }
        });

        // Đọc cấu hình ẩn việc đã xong từ localStorage nếu có
        const savedHideClosed = localStorage.getItem('clickup_cal_hide_closed');
        if (savedHideClosed !== null) {
            CalendarState.filters.hideClosed = (savedHideClosed === 'true');
            const chk = document.getElementById('calFilterHideClosed');
            if (chk) chk.checked = CalendarState.filters.hideClosed;
        }

        // Render lần đầu
        renderCalendar();
        renderUnscheduledTasks();
    }

    /**
     * Gắn sự kiện cho các nút trên Toolbar
     */
    function bindToolbarEvents() {
        // Nút chuyển chế độ Tháng / Tuần
        const btnMonth = document.getElementById('calBtnViewMonth');
        const btnWeek = document.getElementById('calBtnViewWeek');
        if (btnMonth && btnWeek) {
            btnMonth.addEventListener('click', () => {
                CalendarState.viewMode = 'month';
                btnMonth.classList.add('active');
                btnWeek.classList.remove('active');
                renderCalendar();
            });
            btnWeek.addEventListener('click', () => {
                CalendarState.viewMode = 'week';
                btnWeek.classList.add('active');
                btnMonth.classList.remove('active');
                renderCalendar();
            });
        }

        // Nút điều hướng thời gian
        const btnPrev = document.getElementById('calBtnPrev');
        const btnNext = document.getElementById('calBtnNext');
        const btnToday = document.getElementById('calBtnToday');

        if (btnPrev) {
            btnPrev.addEventListener('click', () => {
                if (CalendarState.viewMode === 'month') {
                    CalendarState.currentDate.setMonth(CalendarState.currentDate.getMonth() - 1);
                } else {
                    CalendarState.currentDate.setDate(CalendarState.currentDate.getDate() - 7);
                }
                renderCalendar();
            });
        }

        if (btnNext) {
            btnNext.addEventListener('click', () => {
                if (CalendarState.viewMode === 'month') {
                    CalendarState.currentDate.setMonth(CalendarState.currentDate.getMonth() + 1);
                } else {
                    CalendarState.currentDate.setDate(CalendarState.currentDate.getDate() + 7);
                }
                renderCalendar();
            });
        }

        if (btnToday) {
            btnToday.addEventListener('click', () => {
                CalendarState.currentDate = new Date();
                renderCalendar();
            });
        }

        // Bộ lọc "Ẩn việc đã xong"
        const chkHideClosed = document.getElementById('calFilterHideClosed');
        if (chkHideClosed) {
            chkHideClosed.addEventListener('change', (e) => {
                CalendarState.filters.hideClosed = e.target.checked;
                localStorage.setItem('clickup_cal_hide_closed', e.target.checked);
                renderCalendar();
                renderUnscheduledTasks();
            });
        }

        // Bộ lọc Người phụ trách
        const selAssignee = document.getElementById('calFilterAssignee');
        if (selAssignee) {
            selAssignee.addEventListener('change', (e) => {
                CalendarState.filters.assigneeId = e.target.value;
                renderCalendar();
                renderUnscheduledTasks();
            });
        }

        // Bộ lọc Mức độ ưu tiên
        const selPriority = document.getElementById('calFilterPriority');
        if (selPriority) {
            selPriority.addEventListener('change', (e) => {
                CalendarState.filters.priority = e.target.value;
                renderCalendar();
                renderUnscheduledTasks();
            });
        }

        // Nút bật/tắt Thanh bên Chưa xếp lịch
        const btnToggleUnscheduled = document.getElementById('calBtnToggleUnscheduled');
        const sidebar = document.getElementById('calendarUnscheduledSidebar');
        if (btnToggleUnscheduled && sidebar) {
            btnToggleUnscheduled.addEventListener('click', () => {
                CalendarState.unscheduledSidebarOpen = !CalendarState.unscheduledSidebarOpen;
                if (CalendarState.unscheduledSidebarOpen) {
                    sidebar.classList.remove('d-none');
                } else {
                    sidebar.classList.add('d-none');
                }
            });
        }

        const btnCloseSidebar = document.getElementById('calBtnCloseUnscheduled');
        if (btnCloseSidebar && sidebar) {
            btnCloseSidebar.addEventListener('click', () => {
                CalendarState.unscheduledSidebarOpen = false;
                sidebar.classList.add('d-none');
            });
        }

        // Nút chuyển đổi [📅 Xem Lịch] ⇄ [📊 4 Cột Hạn Chót]
        const btnSubCalendar = document.getElementById('subViewToggleCalendar');
        const btnSubLanes = document.getElementById('subViewToggleLanes');
        const calWrapper = document.getElementById('clickupCalendarWrapper');
        const lanesContainer = document.getElementById('scheduleLanesContainer');

        if (btnSubCalendar && btnSubLanes && calWrapper && lanesContainer) {
            btnSubCalendar.addEventListener('click', () => {
                CalendarState.activeSubView = 'calendar';
                btnSubCalendar.classList.add('active');
                btnSubLanes.classList.remove('active');
                calWrapper.classList.remove('d-none');
                lanesContainer.classList.add('d-none');
            });

            btnSubLanes.addEventListener('click', () => {
                CalendarState.activeSubView = 'lanes';
                btnSubLanes.classList.add('active');
                btnSubCalendar.classList.remove('active');
                calWrapper.classList.add('d-none');
                lanesContainer.classList.remove('d-none');
            });
        }
    }

    /**
     * Render Cuốn Lịch tùy theo chế độ (Tháng hoặc Tuần)
     */
    function renderCalendar() {
        updateToolbarTitle();

        const monthContainer = document.getElementById('calendarMonthView');
        const weekContainer = document.getElementById('calendarWeekView');

        if (!monthContainer || !weekContainer) return;

        if (CalendarState.viewMode === 'month') {
            monthContainer.classList.remove('d-none');
            weekContainer.classList.add('d-none');
            renderMonthGrid();
        } else {
            monthContainer.classList.add('d-none');
            weekContainer.classList.remove('d-none');
            renderWeekGrid();
        }
    }

    /**
     * Cập nhật tiêu đề hiển thị tháng/tuần trên Toolbar
     */
    function updateToolbarTitle() {
        const titleEl = document.getElementById('calTitleDisplay');
        if (!titleEl) return;

        const cur = CalendarState.currentDate;
        const year = cur.getFullYear();
        const month = cur.getMonth();

        if (CalendarState.viewMode === 'month') {
            titleEl.textContent = `${MONTH_NAMES[month]}, ${year}`;
        } else {
            // Tính ngày đầu và cuối của tuần
            const startOfWeek = getStartOfWeek(cur);
            const endOfWeek = new Date(startOfWeek);
            endOfWeek.setDate(endOfWeek.getDate() + 6);

            const startStr = `${startOfWeek.getDate()} Th${startOfWeek.getMonth() + 1}`;
            const endStr = `${endOfWeek.getDate()} Th${endOfWeek.getMonth() + 1}, ${endOfWeek.getFullYear()}`;
            titleEl.textContent = `${startStr} - ${endStr}`;
        }
    }

    /**
     * Lấy ngày Thứ Hai của tuần chứa ngày d
     */
    function getStartOfWeek(d) {
        const date = new Date(d);
        const day = date.getDay(); // 0 = CN, 1 = T2, ..., 6 = T7
        const diff = date.getDate() - day + (day === 0 ? -6 : 1);
        return new Date(date.setDate(diff));
    }

    /**
     * =========================================================================
     * RENDER LƯỚI THÁNG (MONTH GRID - 7 CỘT x 5-6 HÀNG)
     * =========================================================================
     */
    function renderMonthGrid() {
        const gridEl = document.getElementById('calendarMonthGrid');
        if (!gridEl) return;
        gridEl.innerHTML = '';

        const year = CalendarState.currentDate.getFullYear();
        const month = CalendarState.currentDate.getMonth();
        const today = new Date();

        // Ngày đầu tiên của tháng
        const firstDayOfMonth = new Date(year, month, 1);
        // Ngày cuối cùng của tháng
        const lastDayOfMonth = new Date(year, month + 1, 0);

        // Thứ của ngày đầu tiên (0: Chủ Nhật, 1: T2, ..., 6: T7)
        let firstDayWeekIndex = firstDayOfMonth.getDay();
        // Đưa về chỉ số bắt đầu từ Thứ Hai (T2 = 0, ..., CN = 6)
        let mondayIndex = (firstDayWeekIndex === 0) ? 6 : (firstDayWeekIndex - 1);

        // Ngày bắt đầu vẽ trên lịch (có thể rơi vào tháng trước)
        const startDate = new Date(firstDayOfMonth);
        startDate.setDate(startDate.getDate() - mondayIndex);

        // Tổng số ngày cần vẽ: đảm bảo chia hết cho 7 (35 hoặc 42 ô)
        const totalDaysDrawn = (mondayIndex + lastDayOfMonth.getDate() > 35) ? 42 : 35;

        const filteredTasks = getFilteredTasks();

        // Gom nhóm tasks theo due_date dạng { "YYYY-MM-DD": [tasks...] }
        const taskMap = {};
        filteredTasks.forEach(t => {
            if (t.dueDate) {
                if (!taskMap[t.dueDate]) taskMap[t.dueDate] = [];
                taskMap[t.dueDate].push(t);
            }
        });

        const iterDate = new Date(startDate);

        for (let i = 0; i < totalDaysDrawn; i++) {
            const dateStr = formatDateIso(iterDate);
            const isCurMonth = (iterDate.getMonth() === month);
            const isToday = isSameDate(iterDate, today);
            const dayTasks = taskMap[dateStr] || [];

            const cell = document.createElement('div');
            cell.className = 'calendar-day-cell';
            if (!isCurMonth) cell.classList.add('is-other-month');
            if (isToday) cell.classList.add('is-today');
            cell.dataset.date = dateStr;

            // Top bar của ô ngày: Số ngày + Nút thêm nhanh
            const topBar = document.createElement('div');
            topBar.className = 'calendar-cell-top';

            const numSpan = document.createElement('span');
            numSpan.className = 'calendar-cell-num';
            numSpan.textContent = iterDate.getDate();

            const addBtn = document.createElement('button');
            addBtn.type = 'button';
            addBtn.className = 'calendar-cell-add-btn';
            addBtn.title = `Tạo công việc mới ngày ${dateStr}`;
            addBtn.innerHTML = '<i class="bi bi-plus-lg"></i>';
            addBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                openQuickCreateTask(dateStr);
            });

            topBar.appendChild(numSpan);
            topBar.appendChild(addBtn);
            cell.appendChild(topBar);

            // Container chứa các task chip
            const tasksContainer = document.createElement('div');
            tasksContainer.className = 'calendar-cell-tasks';

            // Hiển thị tối đa 3 task
            const MAX_VISIBLE = 3;
            const visibleTasks = dayTasks.slice(0, MAX_VISIBLE);
            const overflowCount = dayTasks.length - MAX_VISIBLE;

            visibleTasks.forEach(t => {
                const chip = createTaskChip(t);
                tasksContainer.appendChild(chip);
            });

            if (overflowCount > 0) {
                const moreBadge = document.createElement('div');
                moreBadge.className = 'calendar-more-badge';
                moreBadge.textContent = `+${overflowCount} việc khác`;
                moreBadge.addEventListener('click', (e) => {
                    e.stopPropagation();
                    openDayPopover(e.currentTarget, dateStr, dayTasks);
                });
                tasksContainer.appendChild(moreBadge);
            }

            cell.appendChild(tasksContainer);

            // Cho phép click vào khoảng trống của ô để tạo nhanh công việc
            cell.addEventListener('click', (e) => {
                if (e.target.closest('.calendar-task-chip') || e.target.closest('.calendar-more-badge') || e.target.closest('.calendar-cell-add-btn')) {
                    return;
                }
                openQuickCreateTask(dateStr);
            });

            // Gắn sự kiện Drag & Drop cho ô ngày
            bindDragDropEvents(cell, dateStr);

            gridEl.appendChild(cell);

            // Tăng sang ngày tiếp theo
            iterDate.setDate(iterDate.getDate() + 1);
        }
    }

    /**
     * =========================================================================
     * RENDER LƯỚI TUẦN (WEEK VIEW GRID - 7 CỘT LỚN CHI TIẾT)
     * =========================================================================
     */
    function renderWeekGrid() {
        const gridEl = document.getElementById('calendarWeekGrid');
        if (!gridEl) return;
        gridEl.innerHTML = '';

        const startOfWeek = getStartOfWeek(CalendarState.currentDate);
        const today = new Date();
        const filteredTasks = getFilteredTasks();

        const taskMap = {};
        filteredTasks.forEach(t => {
            if (t.dueDate) {
                if (!taskMap[t.dueDate]) taskMap[t.dueDate] = [];
                taskMap[t.dueDate].push(t);
            }
        });

        const iterDate = new Date(startOfWeek);

        for (let i = 0; i < 7; i++) {
            const dateStr = formatDateIso(iterDate);
            const isToday = isSameDate(iterDate, today);
            const dayTasks = taskMap[dateStr] || [];

            const col = document.createElement('div');
            col.className = 'calendar-week-col';
            if (isToday) col.classList.add('is-today');
            col.dataset.date = dateStr;

            // Header cột ngày
            const header = document.createElement('div');
            header.className = 'calendar-week-header';

            const dayName = document.createElement('div');
            dayName.className = 'calendar-week-day-name';
            dayName.textContent = DAY_NAMES_FULL[i];

            const dayNum = document.createElement('div');
            dayNum.className = 'calendar-week-day-num';
            dayNum.textContent = iterDate.getDate();

            header.appendChild(dayName);
            header.appendChild(dayNum);
            col.appendChild(header);

            // Danh sách task chi tiết của tuần
            const tasksList = document.createElement('div');
            tasksList.className = 'd-flex flex-column gap-2 flex-grow-1';

            dayTasks.forEach(t => {
                const card = createWeekTaskCard(t);
                tasksList.appendChild(card);
            });

            if (dayTasks.length === 0) {
                const emptyMsg = document.createElement('div');
                emptyMsg.className = 'text-center py-4 text-muted fs-9 fst-italic';
                emptyMsg.textContent = 'Trống';
                tasksList.appendChild(emptyMsg);
            }

            col.appendChild(tasksList);

            // Nút thêm việc nhanh ở dưới chân cột
            const quickAddBtn = document.createElement('button');
            quickAddBtn.type = 'button';
            quickAddBtn.className = 'btn btn-xs btn-outline-secondary rounded-pill w-100 py-1 fs-9 mt-auto border-dashed';
            quickAddBtn.innerHTML = '<i class="bi bi-plus-lg me-1"></i>Thêm việc';
            quickAddBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                openQuickCreateTask(dateStr);
            });
            col.appendChild(quickAddBtn);

            // Gắn sự kiện Drag & Drop
            bindDragDropEvents(col, dateStr);

            gridEl.appendChild(col);

            iterDate.setDate(iterDate.getDate() + 1);
        }
    }

    /**
     * Tạo phần tử thẻ nhỏ (Task Chip) hiển thị trên Lưới Tháng
     */
    function createTaskChip(t) {
        const chip = document.createElement('div');
        chip.className = `calendar-task-chip chip-status-${t.status || 'TODO'}`;
        chip.draggable = true;
        chip.dataset.taskId = t.id;
        chip.title = `${t.title} [${t.priorityLabel || t.priority || ''}] - ${t.assigneeName || 'Chưa giao'}`;

        // Chấm màu biểu thị mức độ ưu tiên
        const dot = document.createElement('span');
        dot.className = `chip-priority-dot chip-priority-${t.priority || 'MEDIUM'}`;

        // Tiêu đề công việc
        const titleSpan = document.createElement('span');
        titleSpan.className = 'task-chip-title';
        titleSpan.textContent = t.title || 'Không có tiêu đề';

        chip.appendChild(dot);
        chip.appendChild(titleSpan);

        // Sự kiện click mở drawer chi tiết công việc
        chip.addEventListener('click', (e) => {
            e.stopPropagation();
            if (window.openClickUpTask) {
                window.openClickUpTask(t.id);
            }
        });

        // Kéo thả bắt đầu
        chip.addEventListener('dragstart', (e) => {
            CalendarState.draggedTaskId = t.id;
            chip.classList.add('is-dragging');
            e.dataTransfer.setData('text/plain', String(t.id));
            e.dataTransfer.effectAllowed = 'move';
        });

        chip.addEventListener('dragend', () => {
            CalendarState.draggedTaskId = null;
            chip.classList.remove('is-dragging');
        });

        return chip;
    }

    /**
     * Tạo thẻ công việc lớn chi tiết cho Lưới Tuần (Week View)
     */
    function createWeekTaskCard(t) {
        const card = document.createElement('div');
        card.className = 'calendar-week-task-card';
        card.draggable = true;
        card.dataset.taskId = t.id;

        const isDone = (t.status === 'DONE' || t.status === 'APPROVED');

        card.innerHTML = `
            <div class="d-flex align-items-center justify-content-between gap-1 mb-1">
                <span class="badge ${t.priorityBadgeClass || 'bg-secondary'} rounded-pill px-1-5 py-0 fs-10">● ${escapeHtml(t.priorityLabel || t.priority)}</span>
                <span class="badge ${t.statusBadgeClass || 'bg-light text-secondary'} rounded-pill px-1-5 py-0 fs-10">${escapeHtml(t.statusLabel || t.status)}</span>
            </div>
            <div class="fs-8 fw-bold text-dark text-truncate-2 mb-1 ${isDone ? 'text-decoration-line-through text-muted' : ''}">${escapeHtml(t.title)}</div>
            <div class="d-flex align-items-center justify-content-between fs-9 text-muted pt-1 border-top">
                <span class="text-truncate" style="max-width: 120px;"><i class="bi bi-person me-1"></i>${escapeHtml(t.assigneeName || 'Chưa giao')}</span>
                <span class="fs-10 ${t.isOverdue ? 'text-danger fw-bold' : ''}">#${t.id}</span>
            </div>
        `;

        card.addEventListener('click', (e) => {
            e.stopPropagation();
            if (window.openClickUpTask) {
                window.openClickUpTask(t.id);
            }
        });

        card.addEventListener('dragstart', (e) => {
            CalendarState.draggedTaskId = t.id;
            card.classList.add('is-dragging');
            e.dataTransfer.setData('text/plain', String(t.id));
            e.dataTransfer.effectAllowed = 'move';
        });

        card.addEventListener('dragend', () => {
            CalendarState.draggedTaskId = null;
            card.classList.remove('is-dragging');
        });

        return card;
    }

    /**
     * Gắn bộ lắng nghe Drag & Drop HTML5 chuẩn cho một ô nhận ngày
     */
    function bindDragDropEvents(dropZoneEl, dateStr) {
        dropZoneEl.addEventListener('dragover', (e) => {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            dropZoneEl.classList.add('drag-over');
        });

        dropZoneEl.addEventListener('dragleave', (e) => {
            // Chỉ xóa class khi thực sự rời khỏi vùng bao
            if (!dropZoneEl.contains(e.relatedTarget)) {
                dropZoneEl.classList.remove('drag-over');
            }
        });

        dropZoneEl.addEventListener('drop', (e) => {
            e.preventDefault();
            dropZoneEl.classList.remove('drag-over');

            const taskIdStr = e.dataTransfer.getData('text/plain') || CalendarState.draggedTaskId;
            if (!taskIdStr) return;

            const taskId = parseInt(taskIdStr, 10);
            if (taskId > 0) {
                handleTaskDueDateDrop(taskId, dateStr);
            }
        });
    }

    /**
     * Xử lý nghiệp vụ khi một thẻ việc được thả vào một ngày trên lịch
     */
    function handleTaskDueDateDrop(taskId, targetDateStr) {
        const allTasks = window.projectCalendarTasks || [];
        const task = allTasks.find(t => t.id === taskId);
        if (!task) return;

        // Nếu ngày không đổi, bỏ qua
        if (task.dueDate === targetDateStr) return;

        // Ràng buộc bất biến: Task đã DONE không thể dời hạn
        if (task.status === 'DONE' || task.status === 'APPROVED') {
            showToastMessage('warning', '🔒 Công việc đã hoàn thành (DONE) được khóa vĩnh viễn, không thể dời hạn chót!');
            return;
        }

        const previousDueDate = task.dueDate;

        // Cập nhật lạc quan (Optimistic Update)
        task.dueDate = targetDateStr;
        renderCalendar();
        renderUnscheduledTasks();

        // Gửi AJAX POST cập nhật backend
        const projectId = window.currentProjectId || 0;
        const contextPath = window.appContextPath || '';

        const formData = new URLSearchParams();
        formData.append('action', 'updateDueDate');
        formData.append('projectId', projectId);
        formData.append('taskId', taskId);
        formData.append('dueDate', targetDateStr);
        formData.append('ajax', 'true');

        fetch(`${contextPath}/task`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: formData.toString()
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                showToastMessage('success', data.message || `Đã dời hạn chót sang ngày ${targetDateStr}!`);
            } else {
                // Rollback dữ liệu nếu lỗi
                task.dueDate = previousDueDate;
                renderCalendar();
                renderUnscheduledTasks();
                showToastMessage('danger', data.message || 'Lỗi khi cập nhật hạn chót!');
            }
        })
        .catch(err => {
            console.error('Lỗi khi cập nhật hạn chót:', err);
            task.dueDate = previousDueDate;
            renderCalendar();
            renderUnscheduledTasks();
            showToastMessage('danger', 'Lỗi kết nối mạng đến máy chủ!');
        });
    }

    /**
     * =========================================================================
     * RENDER DANH SÁCH CÔNG VIỆC CHƯA XẾP LỊCH (UNSCHEDULED TASKS)
     * =========================================================================
     */
    function renderUnscheduledTasks() {
        const bodyEl = document.getElementById('unscheduledTasksList');
        const badgeEl = document.getElementById('unscheduledCountBadge');
        if (!bodyEl) return;

        const allTasks = window.projectCalendarTasks || [];
        // Lọc các task chưa có dueDate và thỏa mãn bộ lọc hiện tại
        const unscheduled = allTasks.filter(t => {
            if (t.dueDate && t.dueDate.trim() !== '') return false;

            if (CalendarState.filters.hideClosed) {
                const s = (t.status || '').toUpperCase();
                if (s === 'DONE' || s === 'APPROVED') return false;
            }
            if (CalendarState.filters.assigneeId !== 'ALL') {
                if (CalendarState.filters.assigneeId === 'MY_TASKS') {
                    const curUserId = window.currentSessionUserId || 0;
                    if (t.assigneeId !== curUserId) return false;
                } else {
                    const targetId = parseInt(CalendarState.filters.assigneeId, 10);
                    if (t.assigneeId !== targetId) return false;
                }
            }
            if (CalendarState.filters.priority !== 'ALL') {
                if ((t.priority || '').toUpperCase() !== CalendarState.filters.priority) return false;
            }
            return true;
        });

        if (badgeEl) {
            badgeEl.textContent = unscheduled.length;
        }

        bodyEl.innerHTML = '';

        if (unscheduled.length === 0) {
            bodyEl.innerHTML = `
                <div class="text-center py-4 text-muted fs-9">
                    <i class="bi bi-check2-circle fs-3 text-success d-block mb-1 opacity-75"></i>
                    Tất cả công việc đã được xếp lịch! 🎉
                </div>
            `;
            return;
        }

        unscheduled.forEach(t => {
            const card = document.createElement('div');
            card.className = 'unscheduled-task-card';
            card.draggable = true;
            card.dataset.taskId = t.id;

            card.innerHTML = `
                <div class="d-flex align-items-center justify-content-between gap-1 mb-1">
                    <span class="badge ${t.priorityBadgeClass || 'bg-secondary'} rounded-pill px-1-5 py-0 fs-10">● ${escapeHtml(t.priorityLabel || t.priority)}</span>
                    <span class="badge ${t.statusBadgeClass || 'bg-light text-secondary'} rounded-pill px-1-5 py-0 fs-10">${escapeHtml(t.statusLabel || t.status)}</span>
                </div>
                <div class="fs-8 fw-bold text-dark text-truncate-2 mb-1">${escapeHtml(t.title)}</div>
                <div class="d-flex align-items-center justify-content-between fs-9 text-muted pt-1 border-top">
                    <span class="text-truncate" style="max-width: 130px;"><i class="bi bi-person me-1"></i>${escapeHtml(t.assigneeName || 'Chưa giao')}</span>
                    <span class="text-primary fw-semibold" title="Kéo thả vào ngày trên lịch"><i class="bi bi-arrows-move"></i></span>
                </div>
            `;

            card.addEventListener('click', (e) => {
                e.stopPropagation();
                if (window.openClickUpTask) {
                    window.openClickUpTask(t.id);
                }
            });

            card.addEventListener('dragstart', (e) => {
                CalendarState.draggedTaskId = t.id;
                card.classList.add('is-dragging');
                e.dataTransfer.setData('text/plain', String(t.id));
                e.dataTransfer.effectAllowed = 'move';
            });

            card.addEventListener('dragend', () => {
                CalendarState.draggedTaskId = null;
                card.classList.remove('is-dragging');
            });

            bodyEl.appendChild(card);
        });
    }

    /**
     * Mở Popover nổi hiển thị danh sách đầy đủ công việc của ngày khi bấm "+N việc khác"
     */
    function openDayPopover(triggerEl, dateStr, tasks) {
        // Đóng popover cũ nếu đang mở
        const existing = document.getElementById('calendarDayPopover');
        if (existing) existing.remove();

        const popover = document.createElement('div');
        popover.id = 'calendarDayPopover';
        popover.className = 'calendar-day-popover shadow-lg';

        // Header popover
        const d = parseDateIso(dateStr);
        const dayLabel = d ? `${DAY_NAMES_FULL[d.getDay() === 0 ? 6 : d.getDay() - 1]}, ${d.getDate()} Tháng ${d.getMonth() + 1}` : dateStr;

        popover.innerHTML = `
            <div class="d-flex align-items-center justify-content-between border-bottom pb-2">
                <div>
                    <h6 class="fw-bold text-dark fs-8 mb-0">${escapeHtml(dayLabel)}</h6>
                    <span class="fs-9 text-muted">${tasks.length} công việc</span>
                </div>
                <button type="button" class="btn-close fs-9" id="closeCalPopoverBtn" aria-label="Đóng"></button>
            </div>
            <div class="d-flex flex-column gap-1-5 overflow-y-auto" style="max-height: 240px;" id="popoverTasksContainer">
            </div>
            <div class="pt-2 border-top mt-1">
                <button type="button" class="btn btn-xs btn-primary-custom rounded-pill w-100 py-1 fs-9" id="popoverAddBtn">
                    <i class="bi bi-plus-lg me-1"></i>Thêm việc vào ngày này
                </button>
            </div>
        `;

        const listContainer = popover.querySelector('#popoverTasksContainer');
        tasks.forEach(t => {
            const item = document.createElement('div');
            item.className = 'p-1-5 px-2 bg-light rounded-2 border d-flex align-items-center justify-content-between gap-2 hover-bg-white';
            item.style.cursor = 'pointer';

            item.innerHTML = `
                <div class="d-flex align-items-center gap-1-5 text-truncate">
                    <span class="chip-priority-dot chip-priority-${t.priority || 'MEDIUM'}"></span>
                    <span class="fs-8 fw-semibold text-dark text-truncate ${t.status === 'DONE' ? 'text-decoration-line-through text-muted' : ''}">${escapeHtml(t.title)}</span>
                </div>
                <span class="badge ${t.statusBadgeClass || 'bg-secondary'} rounded-pill px-1-5 py-0 fs-10 flex-shrink-0">${escapeHtml(t.statusLabel || t.status)}</span>
            `;

            item.addEventListener('click', () => {
                popover.remove();
                if (window.openClickUpTask) {
                    window.openClickUpTask(t.id);
                }
            });

            listContainer.appendChild(item);
        });

        document.body.appendChild(popover);

        // Định vị popover sát ô trigger
        const rect = triggerEl.getBoundingClientRect();
        let left = rect.left;
        let top = rect.bottom + 6;

        // Tránh tràn màn hình bên phải
        if (left + 300 > window.innerWidth) {
            left = window.innerWidth - 310;
        }
        // Tránh tràn màn hình bên dưới
        if (top + 280 > window.innerHeight) {
            top = rect.top - 270;
        }

        popover.style.left = `${Math.max(10, left)}px`;
        popover.style.top = `${Math.max(10, top)}px`;

        popover.querySelector('#closeCalPopoverBtn').addEventListener('click', () => popover.remove());
        popover.querySelector('#popoverAddBtn').addEventListener('click', () => {
            popover.remove();
            openQuickCreateTask(dateStr);
        });
    }

    /**
     * Mở modal thêm công việc mới với ngày hạn chót đã được điền sẵn
     */
    function openQuickCreateTask(dateStr) {
        const dueDateInput = document.getElementById('taskDueDate');
        if (dueDateInput) {
            dueDateInput.value = dateStr;
        }
        const modalEl = document.getElementById('addTaskModal');
        if (modalEl && window.bootstrap && window.bootstrap.Modal) {
            const modal = window.bootstrap.Modal.getOrCreateInstance(modalEl);
            modal.show();
            // Focus vào ô tiêu đề
            setTimeout(() => {
                const titleInput = document.getElementById('taskTitle');
                if (titleInput) titleInput.focus();
            }, 300);
        }
    }

    /**
     * Hiển thị Toast thông báo nổi đồng bộ với hệ thống App Toast
     */
    function showToastMessage(type, message) {
        if (window.showAppToast) {
            window.showAppToast(type, message);
            return;
        }

        // Fallback tự tạo toast nếu chưa có hàm toàn cục
        let container = document.getElementById('appFloatingToastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'appFloatingToastContainer';
            container.className = 'position-fixed bottom-0 end-0 p-3';
            container.style.zIndex = '1090';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        const bgClass = (type === 'success') ? 'bg-success text-white' : (type === 'danger' ? 'bg-danger text-white' : 'bg-dark text-white');
        toast.className = `toast align-items-center ${bgClass} border-0 show shadow-lg mb-2`;
        toast.role = 'alert';
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body fs-8 py-2 px-3 fw-medium">
                    ${escapeHtml(message)}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto fs-9" data-bs-dismiss="toast" aria-label="Đóng"></button>
            </div>
        `;
        container.appendChild(toast);
        setTimeout(() => {
            toast.remove();
        }, 3500);
    }

    // Tự động khởi chạy khi trang đã sẵn sàng
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initCalendar);
    } else {
        initCalendar();
    }

    // Xuất ra window để các hàm bên ngoài có thể gọi nếu cần
    window.refreshClickUpCalendar = function() {
        renderCalendar();
        renderUnscheduledTasks();
    };

})();
