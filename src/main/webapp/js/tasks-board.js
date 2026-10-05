/*
 * tasks-board.js — Điều khiển giao diện trang Bảng công việc (tasks.jsp):
 * sidebar, tab, List/Board view, drawer chi tiết Task, inline-create, bộ lọc, yêu thích, pháo hoa.
 *
 * Dữ liệu từ Servlet/JSP được truyền qua đối tượng toàn cục TASK_PAGE, khai báo ngay trước
 * thẻ <script src> trong tasks.jsp (mục "TASK_PAGE"). Muốn thêm giá trị từ server → thêm vào đó.
 * File này KHÔNG chứa EL/JSTL nên có thể sửa và F5 mà không cần restart Tomcat (hot_jsp.bat).
 */
    // 1. Sidebar Toggle & State persistence in localStorage and Cookie
    function toggleClickUpSidebar() {
        var sidebar = document.getElementById('clickupSidebar');
        var expandBtns = document.querySelectorAll('.btn-expand-sidebar');
        if (!sidebar) return;
        var isCollapsed = sidebar.classList.toggle('collapsed');
        localStorage.setItem('clickup_sidebar_collapsed', isCollapsed ? '1' : '0');
        var basePath = window.location.pathname.startsWith('/teamwork-hub') ? '/teamwork-hub' : '/';
        document.cookie = "sidebar_collapsed=" + (isCollapsed ? 'true' : 'false') + "; path=" + basePath + "; max-age=" + (30 * 24 * 60 * 60);
        expandBtns.forEach(function(btn) {
            if (isCollapsed) {
                btn.classList.remove('d-none');
            } else {
                btn.classList.add('d-none');
            }
        });
    }

    // Initialize sidebar state on page load
    document.addEventListener('DOMContentLoaded', function() {
        var isCollapsed = localStorage.getItem('clickup_sidebar_collapsed') === '1';
        var sidebar = document.getElementById('clickupSidebar');
        var expandBtns = document.querySelectorAll('.btn-expand-sidebar');
        if (isCollapsed && sidebar) {
            sidebar.classList.add('collapsed');
            expandBtns.forEach(function(btn) {
                btn.classList.remove('d-none');
            });
        }
    });

    // 1.5. Cây thư mục phân hệ dự án trong Sidebar (ui-ux-pro-max)
    function toggleProjectTree(projectId, event) {
        if (event) {
            event.preventDefault();
            event.stopPropagation();
        }
        var sublist = document.getElementById('tree-sublist-' + projectId);
        var chevron = document.getElementById('tree-chevron-' + projectId);
        var btn = event ? event.currentTarget : document.querySelector('#project-tree-' + projectId + ' .tree-toggle-btn');
        if (!sublist) return;

        var isCollapsed = sublist.classList.toggle('d-none');
        if (chevron) {
            if (isCollapsed) {
                chevron.classList.remove('bi-chevron-down');
                chevron.classList.add('bi-chevron-right', 'collapsed');
            } else {
                chevron.classList.remove('bi-chevron-right', 'collapsed');
                chevron.classList.add('bi-chevron-down');
            }
        }
        if (btn) {
            btn.setAttribute('aria-expanded', !isCollapsed);
        }
        try {
            localStorage.setItem('project_tree_collapsed_' + projectId, isCollapsed ? 'true' : 'false');
        } catch(e) {}
    }

    // Khôi phục trạng thái thu gọn/mở rộng cây thư mục từ localStorage
    document.addEventListener('DOMContentLoaded', function() {
        var currentProjectId = String(TASK_PAGE.projectId);
        if (currentProjectId) {
            try {
                var savedTreeState = localStorage.getItem('project_tree_collapsed_' + currentProjectId);
                if (savedTreeState === 'true') {
                    toggleProjectTree(currentProjectId);
                }
            } catch(e) {}
        }
    });

    // 2. Tab switching: tasks, metrics, activity (chat & docs load dedicated pages)
    function switchClickUpTab(tab) {
        if (tab === 'chat') {
            window.location.href = TASK_PAGE.contextPath + '/chat?action=view&projectId=' + TASK_PAGE.projectId;
            return;
        }
        if (tab === 'docs') {
            window.location.href = TASK_PAGE.contextPath + '/doc?action=list&projectId=' + TASK_PAGE.projectId;
            return;
        }

        document.querySelectorAll('.clickup-tab-link').forEach(function(el) {
            el.classList.remove('active');
        });
        var activeBtn = document.getElementById('tab-btn-' + tab);
        if (activeBtn) activeBtn.classList.add('active');

        document.querySelectorAll('.clickup-view-pane').forEach(function(el) {
            el.classList.add('d-none');
        });
        var targetPane = document.getElementById('clickup-view-' + tab);
        if (targetPane) targetPane.classList.remove('d-none');

        // Header dự án luôn hiển thị để người dùng chuyển tab mượt mà; Toolbar lọc chỉ hiển thị ở tab tasks
        var mainHeader = document.querySelector('.clickup-main-header');
        var controlToolbar = document.querySelector('.clickup-control-toolbar');
        if (mainHeader) mainHeader.classList.remove('d-none');
        if (controlToolbar) {
            if (tab === 'tasks') {
                controlToolbar.classList.remove('d-none');
            } else {
                controlToolbar.classList.add('d-none');
            }
        }

        var switcher = document.getElementById('taskViewSwitcher');
        if (switcher) {
            if (tab === 'tasks') {
                switcher.classList.remove('d-none');
            } else {
                switcher.classList.add('d-none');
            }
        }

        try {
            var url = new URL(window.location.href);
            url.searchParams.set('view', tab);
            window.history.replaceState({}, '', url);
        } catch(e) {}
    }

    // 3. Task subview switching: list vs board
    function switchTaskSubView(view) {
        var btnList = document.getElementById('btn-view-list');
        var btnBoard = document.getElementById('btn-view-board');
        var viewList = document.getElementById('task-subview-list');
        var viewBoard = document.getElementById('task-subview-board');

        if (view === 'list') {
            if (btnList) btnList.classList.add('active');
            if (btnBoard) btnBoard.classList.remove('active');
            if (viewList) viewList.classList.remove('d-none');
            if (viewBoard) viewBoard.classList.add('d-none');
        } else {
            if (btnList) btnList.classList.remove('active');
            if (btnBoard) btnBoard.classList.add('active');
            if (viewList) viewList.classList.add('d-none');
            if (viewBoard) viewBoard.classList.remove('d-none');
        }

        // Lưu Cookie preferred_task_view (hạn 30 ngày)
        var basePath = window.location.pathname.startsWith('/teamwork-hub') ? '/teamwork-hub' : '/';
        document.cookie = "preferred_task_view=" + encodeURIComponent(view) + "; path=" + basePath + "; max-age=" + (30 * 24 * 60 * 60);

        try {
            var url = new URL(window.location.href);
            url.searchParams.set('taskView', view);
            window.history.replaceState({}, '', url);
        } catch(e) {}
    }

    // Project members data for dynamic selects
    window.projectMembersList = TASK_PAGE.members;

    // 4. Toggle collapsing of group in List View
    window.toggleClickUpGroup = function(groupId) {
        var rows = document.querySelectorAll('.group-' + groupId + '-row');
        var icon = document.getElementById('chevron-' + groupId);
        if (!rows || rows.length === 0) return;

        var isCurrentlyExpanded = icon ? icon.classList.contains('bi-chevron-down') : !rows[0].classList.contains('d-none');
        var willHide = isCurrentlyExpanded;

        rows.forEach(function(r) {
            // Never show inline create task or subtask rows when expanding group
            if (r.classList.contains('clickup-inline-create-row') || r.classList.contains('clickup-inline-subtask-row')) {
                r.classList.add('d-none');
                return;
            }
            if (willHide) {
                r.classList.add('d-none');
            } else {
                // If it's a subtask row and subtask preference is not expanded, keep it hidden
                var subtaskCookie = document.cookie.split('; ').find(function(c) { return c.startsWith('preferred_subtask_mode='); });
                var subMode = subtaskCookie ? decodeURIComponent(subtaskCookie.split('=')[1]) : TASK_PAGE.subtaskMode;
                if (r.classList.contains('clickup-subtask-row') && subMode !== 'expanded') {
                    return;
                }
                r.classList.remove('d-none');
            }
        });

        if (willHide && typeof cancelInlineCreateTask === 'function') {
            cancelInlineCreateTask(groupId);
        }

        if (icon) {
            icon.className = willHide ? 'bi bi-chevron-right me-1' : 'bi bi-chevron-down me-1';
        }

        try {
            localStorage.setItem('clickup_group_' + groupId + '_collapsed', willHide ? 'true' : 'false');
        } catch(e) {}
    };

    // 5. Toggle subtasks visibility for a parent task
    window.toggleSubtasks = function(taskId, event) {
        if (event) event.stopPropagation();
        var subRows = document.querySelectorAll('.clickup-subtask-row[data-parent-id="' + taskId + '"]');
        var caret = document.getElementById('caret-' + taskId);
        var caretIcon = caret ? caret.querySelector('i') : document.getElementById('subtask-caret-icon-' + taskId);
        var toggleBtn = document.getElementById('subtask-toggle-' + taskId);
        var isExpanding = false;

        subRows.forEach(function(row) {
            if (row.classList.contains('d-none')) {
                row.classList.remove('d-none');
                isExpanding = true;
            } else {
                row.classList.add('d-none');
                isExpanding = false;
            }
        });

        if (caretIcon) {
            caretIcon.className = isExpanding ? 'bi bi-chevron-down' : 'bi bi-chevron-right';
        }
        if (caret) {
            if (isExpanding) caret.classList.add('is-expanded');
            else caret.classList.remove('is-expanded');
        }
        if (toggleBtn) {
            var icon = toggleBtn.querySelector('i');
            if (icon) {
                icon.className = isExpanding ? 'bi bi-chevron-down' : 'bi bi-chevron-right';
            }
        }
    };

    // 5.1. Subtask Display 2-Mode Toggle (Đóng ⇄ Mở rộng)
    window.applySubtaskMode = function(mode) {
        var basePath = window.location.pathname.startsWith('/teamwork-hub') ? '/teamwork-hub' : '/';
        document.cookie = "preferred_subtask_mode=" + encodeURIComponent(mode) + "; path=" + basePath + "; max-age=" + (30 * 24 * 60 * 60);

        var label = document.getElementById('subtaskToggleLabel');
        var icon = document.getElementById('subtaskToggleIcon');
        var isExpanded = (mode === 'expanded');

        if (label) {
            label.textContent = isExpanded ? 'Nhiệm vụ: Mở rộng' : 'Nhiệm vụ: Đóng';
        }
        if (icon) {
            icon.className = isExpanded ? 'bi bi-chevron-down text-primary' : 'bi bi-chevron-right text-secondary';
        }

        var allSubRows = document.querySelectorAll('.clickup-subtask-row');
        var allCarets = document.querySelectorAll('.subtask-caret');

        if (isExpanded) {
            allSubRows.forEach(function(r) { r.classList.remove('d-none'); });
            allCarets.forEach(function(c) {
                c.classList.remove('d-none');
                c.classList.add('is-expanded');
                var i = c.querySelector('i');
                if (i) i.className = 'bi bi-chevron-down';
            });
        } else {
            allSubRows.forEach(function(r) { r.classList.add('d-none'); });
            allCarets.forEach(function(c) {
                c.classList.remove('d-none', 'is-expanded');
                var i = c.querySelector('i');
                if (i) i.className = 'bi bi-chevron-right';
            });
        }
    };

    window.toggleAllSubtasks = function() {
        var label = document.getElementById('subtaskToggleLabel');
        var isCurrentlyExpanded = label ? label.textContent.includes('Mở rộng') : false;
        var newMode = isCurrentlyExpanded ? 'collapsed' : 'expanded';
        window.applySubtaskMode(newMode);
        if (window.showToast) {
            window.showToast(newMode === 'expanded' ? 'Đã mở rộng tất cả nhiệm vụ' : 'Đã đóng tất cả nhiệm vụ', 'info');
        }
    };

    // Aliases
    window.setSubtaskMode = window.applySubtaskMode;

    // 5.2. ClickUp Floating Status Popover
    var currentStatusTarget = { id: 0, isSubtask: false, currentStatus: '', parentTaskId: 0 };

    window.openStatusDropdown = function(event, id, isSubtask, currentStatus, parentTaskId) {
        if (event) {
            if (typeof event.stopPropagation === 'function') event.stopPropagation();
            if (typeof event.preventDefault === 'function') event.preventDefault();
        }

        if (!isSubtask && currentStatus === 'DONE') {
            if (window.showToast) window.showToast('Công việc đã hoàn thành đã bị khóa, không thể thay đổi trạng thái!', 'warning');
            return;
        }
        if (isSubtask && parentTaskId) {
            var parentRow = document.querySelector('.clickup-task-row[data-task-id="' + parentTaskId + '"]');
            if (parentRow && parentRow.classList.contains('group-done-row')) {
                if (window.showToast) window.showToast('Công việc cha đã hoàn thành. Toàn bộ nhiệm vụ đã bị khóa!', 'warning');
                return;
            }
        }

        // Chế độ Solo: Subtask là 1-click checklist (tick là toggle DONE/TODO tức thì)
        if (isSubtask && TASK_PAGE.soloProject) {
            currentStatusTarget = {
                id: id,
                isSubtask: isSubtask,
                currentStatus: currentStatus,
                parentTaskId: parentTaskId || 0
            };
            var isDone = (currentStatus === 'APPROVED' || currentStatus === 'DONE');
            window.executeInlineStatusChange(isDone ? 'TODO' : 'DONE');
            return;
        }

        var popover = document.getElementById('clickupStatusPopover');
        if (!popover) return;

        // Nếu popover đang mở cho chính đối tượng này -> đóng lại (Toggle)
        if ((popover.classList.contains('show') || popover.style.display === 'block') &&
            currentStatusTarget.id === id && currentStatusTarget.isSubtask === isSubtask) {
            popover.classList.remove('show');
            popover.classList.add('d-none');
            popover.style.display = 'none';
            return;
        }

        currentStatusTarget = {
            id: id,
            isSubtask: isSubtask,
            currentStatus: currentStatus,
            parentTaskId: parentTaskId || 0
        };

        var header = document.getElementById('clickupPopoverHeader');
        var container = document.getElementById('clickupPopoverOptions');
        if (!container) return;

        if (isSubtask) {
            if (header) header.textContent = 'Trạng thái nhiệm vụ';
            var isDone = (currentStatus === 'APPROVED' || currentStatus === 'DONE' || currentStatus === 'SUBMITTED');
            // Ở chế độ Quality Gate, tick nghĩa là NỘP BÀI chờ Task Lead duyệt (server ghi SUBMITTED);
            // Fast-track/Solo thì tick là xong hẳn (server ghi DONE). Nhãn phải nói đúng điều đó.
            var gateRow = document.querySelector('.clickup-task-row[data-task-id="' + parentTaskId + '"]');
            var gateOn = !!(gateRow && gateRow.getAttribute('data-requires-gate') === 'true');
            var doneLabel = gateOn ? 'Nộp bài (chờ trưởng nhóm công việc duyệt)' : 'Hoàn thành';
            container.innerHTML = 
                '<button type="button" class="btn btn-sm w-100 text-start d-flex align-items-center gap-2 py-2 px-2-5 rounded-2 hover-bg-light border-0 mb-1 ' + (!isDone ? 'bg-light fw-bold text-primary' : 'text-dark') + '" onclick="event.stopPropagation(); executeInlineStatusChange(\'TODO\')">' +
                    '<span class="clickup-status-dot dot-todo"></span>' +
                    '<span class="fs-8">Chưa xong (TO DO)</span>' +
                '</button>' +
                '<button type="button" class="btn btn-sm w-100 text-start d-flex align-items-center gap-2 py-2 px-2-5 rounded-2 hover-bg-light border-0 ' + (isDone ? 'bg-light fw-bold text-success' : 'text-dark') + '" onclick="event.stopPropagation(); executeInlineStatusChange(\'DONE\')">' +
                    '<span class="clickup-status-dot dot-done"><i class="bi bi-check text-white fs-9"></i></span>' +
                    '<span class="fs-8">' + doneLabel + '</span>' +
                '</button>';
        } else {
            if (header) header.textContent = 'Trạng thái công việc';
            container.innerHTML = 
                '<button type="button" class="btn btn-sm w-100 text-start d-flex align-items-center gap-2 py-2 px-2-5 rounded-2 hover-bg-light border-0 mb-1 ' + (currentStatus === 'TODO' ? 'bg-light fw-bold text-primary' : 'text-dark') + '" onclick="event.stopPropagation(); executeInlineStatusChange(\'TODO\')">' +
                    '<span class="clickup-status-dot dot-todo"></span>' +
                    '<span class="fs-8">TO DO (Cần làm)</span>' +
                '</button>' +
                '<button type="button" class="btn btn-sm w-100 text-start d-flex align-items-center gap-2 py-2 px-2-5 rounded-2 hover-bg-light border-0 mb-1 ' + (currentStatus === 'IN_PROGRESS' ? 'bg-light fw-bold text-info' : 'text-dark') + '" onclick="event.stopPropagation(); executeInlineStatusChange(\'IN_PROGRESS\')">' +
                    '<span class="clickup-status-dot dot-inprog"></span>' +
                    '<span class="fs-8">Đang làm</span>' +
                '</button>' +
                '<button type="button" class="btn btn-sm w-100 text-start d-flex align-items-center gap-2 py-2 px-2-5 rounded-2 hover-bg-light border-0 ' + (currentStatus === 'DONE' ? 'bg-light fw-bold text-success' : 'text-dark') + '" onclick="event.stopPropagation(); executeInlineStatusChange(\'DONE\')">' +
                    '<span class="clickup-status-dot dot-done"><i class="bi bi-check text-white fs-9"></i></span>' +
                    '<span class="fs-8">Hoàn thành</span>' +
                '</button>';
        }

        // Tìm phần tử kích hoạt (target element) an toàn tuyệt đối
        var targetEl = null;
        if (event) {
            if (event.currentTarget && event.currentTarget.classList && event.currentTarget.classList.contains('clickup-status-dot')) {
                targetEl = event.currentTarget;
            } else if (event.target) {
                targetEl = event.target.closest('.clickup-status-dot');
            }
        }
        if (!targetEl) {
            targetEl = document.getElementById((isSubtask ? 'subtask-status-dot-' : 'status-dot-') + id);
        }

        if (targetEl) {
            var rect = targetEl.getBoundingClientRect();
            var popoverWidth = 220;
            var leftPos = rect.left;
            var topPos = rect.bottom + 6;

            if (leftPos + popoverWidth > window.innerWidth - 12) {
                leftPos = Math.max(10, window.innerWidth - popoverWidth - 16);
            }
            if (topPos + 180 > window.innerHeight) {
                topPos = Math.max(10, rect.top - 170);
            }

            popover.style.position = 'fixed';
            popover.style.top = topPos + 'px';
            popover.style.left = leftPos + 'px';
            popover.style.zIndex = '999999';
        } else {
            popover.style.position = 'fixed';
            popover.style.top = '30%';
            popover.style.left = '40%';
            popover.style.zIndex = '999999';
        }

        popover.style.display = 'block';
        popover.classList.remove('d-none');
        popover.classList.add('show');
    };

    window.executeInlineStatusChange = function(newStatus) {
        var popover = document.getElementById('clickupStatusPopover');
        if (popover) {
            popover.classList.remove('show');
            popover.classList.add('d-none');
            popover.style.display = 'none';
        }

        var basePath = window.location.pathname.startsWith('/teamwork-hub') ? '/teamwork-hub' : '';
        var params = new URLSearchParams();

        if (currentStatusTarget.isSubtask) {
            var isCompleted = (newStatus === 'DONE');
            params.append('action', 'toggleSubTask');
            params.append('projectId', String(TASK_PAGE.projectId));
            params.append('subTaskId', currentStatusTarget.id);
            params.append('completed', isCompleted ? 'true' : 'false');
            params.append('ajax', 'true');

            // ▶ SERVLET: POST /task → TaskServlet.doPost() → case "toggleSubTask" → SubTaskHandler.handleToggleSubTask()
            //   Servlet trả JSON (không forward JSP) → .then(res => res.json()) cập nhật giao diện, trang không reload
            fetch(basePath + '/task', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: params.toString()
            })
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data.success) {
                    if (window.showToast) window.showToast(data.message, 'success');
                    if (isCompleted && window.fireConfettiCelebration) {
                        window.fireConfettiCelebration();
                    }
                    var dot = document.getElementById('subtask-status-dot-' + currentStatusTarget.id);
                    var titleEl = document.getElementById('subtask-title-text-' + currentStatusTarget.id);
                    var badge = document.getElementById('subtask-badge-' + currentStatusTarget.id);
                    if (dot) {
                        dot.className = 'clickup-status-dot ' + (isCompleted ? 'dot-done' : 'dot-todo');
                        dot.innerHTML = isCompleted ? '<i class="bi bi-check text-white"></i>' : '';
                    }
                    if (titleEl) {
                        if (isCompleted) titleEl.classList.add('text-decoration-line-through', 'text-muted');
                        else titleEl.classList.remove('text-decoration-line-through', 'text-muted');
                    }
                    if (badge) {
                        badge.className = 'badge ' + (isCompleted ? 'bg-success-subtle text-success border border-success-subtle' : 'bg-light text-secondary border') + ' rounded-pill px-1-5 py-0 fs-9';
                        badge.textContent = isCompleted ? 'Đã xong' : 'Đang làm';
                    }
                    if (data.data && data.data.parentStatus && data.data.parentStatus === 'DONE') {
                        setTimeout(function() { window.location.reload(); }, 500);
                    }
                } else {
                    if (window.showToast) window.showToast(data.message, 'error');
                    else alert(data.message);
                }
            })
            .catch(function(err) {
                console.error(err);
                if (window.showToast) window.showToast('Lỗi khi cập nhật trạng thái nhiệm vụ!', 'error');
            });
        } else {
            params.append('action', 'updateStatus');
            params.append('projectId', String(TASK_PAGE.projectId));
            params.append('taskId', currentStatusTarget.id);
            params.append('newStatus', newStatus);
            params.append('ajax', 'true');

            // ▶ SERVLET: POST /task → TaskServlet.doPost() → case "updateStatus" → TaskCrudHandler.handleUpdateTaskStatus()
            //   Servlet trả JSON (không forward JSP) → .then(res => res.json()) cập nhật giao diện, trang không reload
            fetch(basePath + '/task', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: params.toString()
            })
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data.success) {
                    if (window.showToast) window.showToast(data.message, 'success');
                    if ((newStatus === 'DONE' || newStatus === 'IN_PROGRESS') && window.fireConfettiCelebration) {
                        window.fireConfettiCelebration();
                    }
                    setTimeout(function() { window.location.reload(); }, 400);
                } else {
                    if (window.showToast) window.showToast(data.message, 'error');
                    else alert(data.message);
                }
            })
            .catch(function(err) {
                console.error(err);
                if (window.showToast) window.showToast('Lỗi khi cập nhật trạng thái công việc!', 'error');
            });
        }
    };

    // 5.3. Quick Add Parent Task
    window.showInlineCreateTask = function(groupId) {
        var icon = document.getElementById('chevron-' + groupId);
        if (icon && icon.classList.contains('bi-chevron-right')) {
            window.toggleClickUpGroup(groupId);
        }
        var row = document.getElementById('inline-task-row-' + groupId);
        if (row) {
            row.classList.remove('d-none');
            var input = document.getElementById('inline-task-title-' + groupId);
            if (input) {
                input.focus();
                input.select();
            }
        }
    };

    window.cancelInlineCreateTask = function(groupId) {
        var row = document.getElementById('inline-task-row-' + groupId);
        if (row) {
            row.classList.add('d-none');
            var input = document.getElementById('inline-task-title-' + groupId);
            if (input) input.value = '';
        }
    };

    window.handleInlineTaskKey = function(event, groupId) {
        if (event.key === 'Enter') {
            event.preventDefault();
            window.submitInlineCreateTask(groupId);
        } else if (event.key === 'Escape') {
            event.preventDefault();
            window.cancelInlineCreateTask(groupId);
        }
    };

    window.submitInlineCreateTask = function(groupId) {
        if (groupId === 'done') {
            if (window.showToast) window.showToast('Không thể tạo trực tiếp công việc ở trạng thái Hoàn thành!', 'error');
            return;
        }
        var titleEl = document.getElementById('inline-task-title-' + groupId);
        var assigneeEl = document.getElementById('inline-task-assignee-' + groupId);
        var priorityEl = document.getElementById('inline-task-priority-' + groupId);
        var dueEl = document.getElementById('inline-task-due-' + groupId);

        var title = titleEl ? titleEl.value.trim() : '';
        if (!title) {
            if (window.showToast) window.showToast('Vui lòng nhập tiêu đề công việc!', 'error');
            else alert('Vui lòng nhập tiêu đề công việc!');
            if (titleEl) titleEl.focus();
            return;
        }

        var statusMap = { 'done': 'DONE', 'inprog': 'IN_PROGRESS', 'todo': 'TODO' };
        var status = statusMap[groupId] || 'TODO';
        var assigneeId = assigneeEl ? assigneeEl.value : '0';
        var priority = priorityEl ? priorityEl.value : 'MEDIUM';
        var dueDate = dueEl ? dueEl.value : '';

        var params = new URLSearchParams();
        params.append('action', 'quickAddParentTask');
        params.append('projectId', String(TASK_PAGE.projectId));
        params.append('status', status);
        params.append('title', title);
        params.append('priority', priority);
        params.append('assigneeId', assigneeId);
        params.append('dueDate', dueDate);
        params.append('ajax', 'true');

        var basePath = window.location.pathname.startsWith('/teamwork-hub') ? '/teamwork-hub' : '';
        // ▶ SERVLET: POST /task → TaskServlet.doPost() → case "quickAddParentTask" → TaskCrudHandler.handleQuickAddParentTask()
        //   Servlet trả JSON (không forward JSP) → .then(res => res.json()) cập nhật giao diện, trang không reload
        fetch(basePath + '/task', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: params.toString()
        })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data.success) {
                if (window.showToast) window.showToast(data.message, 'success');
                window.cancelInlineCreateTask(groupId);
                setTimeout(function() { window.location.reload(); }, 400);
            } else {
                if (window.showToast) window.showToast(data.message || 'Lỗi khi tạo công việc', 'error');
                else alert(data.message);
            }
        })
        .catch(function(err) {
            console.error(err);
            if (window.showToast) window.showToast('Đã có lỗi xảy ra trong quá trình gửi yêu cầu!', 'error');
        });
    };

    // 5.4. Quick Add Subtask
    window.showInlineCreateSubtask = function(parentTaskId, event) {
        if (event) event.stopPropagation();

        var parentRow = document.querySelector('tr[data-task-id="' + parentTaskId + '"]');
        if (!parentRow) return;
        if (parentRow.classList.contains('group-done-row')) {
            if (window.showToast) window.showToast('Không thể thêm nhiệm vụ vào công việc đã hoàn thành!', 'warning');
            return;
        }

        var subRows = document.querySelectorAll('.clickup-subtask-row[data-parent-id="' + parentTaskId + '"]');
        subRows.forEach(function(r) { r.classList.remove('d-none'); });
        var caret = document.getElementById('caret-' + parentTaskId);
        var caretIcon = caret ? caret.querySelector('i') : document.getElementById('subtask-caret-icon-' + parentTaskId);
        if (caretIcon) caretIcon.className = 'bi bi-chevron-down';
        if (caret) caret.classList.add('is-expanded');

        var existingRow = document.getElementById('inline-subtask-row-' + parentTaskId);
        if (existingRow) {
            existingRow.classList.remove('d-none');
            var input = document.getElementById('inline-subtask-title-' + parentTaskId);
            if (input) { input.focus(); input.select(); }
            return;
        }

        var groupClass = '';
        parentRow.classList.forEach(function(cls) {
            if (cls.startsWith('group-') && cls.endsWith('-row')) {
                groupClass = cls;
            }
        });

        var lastTarget = parentRow;
        if (subRows && subRows.length > 0) {
            lastTarget = subRows[subRows.length - 1];
        }

        var memberOpts = '<option value="0">Chưa gán</option>';
        if (window.projectMembersList && window.projectMembersList.length > 0) {
            window.projectMembersList.forEach(function(m) {
                memberOpts += '<option value="' + m.id + '">' + m.name + '</option>';
            });
        }

        var assigneeTd = TASK_PAGE.teamProject ? (
            '<td>' +
                '<select id="inline-subtask-assignee-' + parentTaskId + '" class="form-select form-select-sm py-0 fs-8" style="max-width: 120px;">' +
                    memberOpts +
                '</select>' +
            '</td>'
        ) : '';

        var newRow = document.createElement('tr');
        newRow.id = 'inline-subtask-row-' + parentTaskId;
        newRow.className = 'clickup-inline-subtask-row clickup-inline-create-row ' + groupClass;
        newRow.innerHTML = 
            '<td>' +
                '<div class="d-flex align-items-center gap-2 ps-4" style="padding-left: 36px !important;">' +
                    '<span class="clickup-status-dot dot-todo"></span>' +
                    '<input type="text" id="inline-subtask-title-' + parentTaskId + '" class="form-control form-control-sm clickup-inline-input fs-8" placeholder="Tên nhiệm vụ mới (Enter để lưu, Esc để hủy)..." onkeydown="handleInlineSubtaskKey(event, ' + parentTaskId + ')" />' +
                '</div>' +
            '</td>' +
            assigneeTd +
            '<td>' +
                '<span class="fs-9 text-muted">Nhiệm vụ</span>' +
            '</td>' +
            '<td>' +
                '<span class="badge bg-light text-secondary border rounded-pill px-1-5 py-0 fs-9">Đang làm</span>' +
            '</td>' +
            '<td style="text-align: right;">' +
                '<button type="button" class="btn btn-sm btn-primary py-0 px-2 fs-8 me-1" onclick="submitInlineCreateSubtask(' + parentTaskId + ')"><i class="bi bi-check-lg"></i> Lưu</button>' +
                '<button type="button" class="btn btn-sm btn-light border py-0 px-2 fs-8" onclick="cancelInlineCreateSubtask(' + parentTaskId + ')"><i class="bi bi-x-lg"></i></button>' +
            '</td>';

        lastTarget.parentNode.insertBefore(newRow, lastTarget.nextSibling);

        var titleInput = document.getElementById('inline-subtask-title-' + parentTaskId);
        if (titleInput) {
            titleInput.focus();
        }
    };

    window.cancelInlineCreateSubtask = function(parentTaskId) {
        var row = document.getElementById('inline-subtask-row-' + parentTaskId);
        if (row) {
            row.remove();
        }
    };

    window.handleInlineSubtaskKey = function(event, parentTaskId) {
        if (event.key === 'Enter') {
            event.preventDefault();
            window.submitInlineCreateSubtask(parentTaskId);
        } else if (event.key === 'Escape') {
            event.preventDefault();
            window.cancelInlineCreateSubtask(parentTaskId);
        }
    };

    window.submitInlineCreateSubtask = function(parentTaskId) {
        var titleEl = document.getElementById('inline-subtask-title-' + parentTaskId);
        var assigneeEl = document.getElementById('inline-subtask-assignee-' + parentTaskId);

        var title = titleEl ? titleEl.value.trim() : '';
        if (!title) {
            if (window.showToast) window.showToast('Vui lòng nhập tiêu đề nhiệm vụ!', 'error');
            else alert('Vui lòng nhập tiêu đề nhiệm vụ!');
            if (titleEl) titleEl.focus();
            return;
        }

        var assigneeId = assigneeEl ? assigneeEl.value : (TASK_PAGE.soloProject ? TASK_PAGE.currentUserId : '0');

        var params = new URLSearchParams();
        params.append('action', 'quickAddSubTask');
        params.append('projectId', String(TASK_PAGE.projectId));
        params.append('taskId', parentTaskId);
        params.append('title', title);
        params.append('assigneeId', assigneeId);
        params.append('dueDate', '');
        params.append('ajax', 'true');

        var basePath = window.location.pathname.startsWith('/teamwork-hub') ? '/teamwork-hub' : '';
        // ▶ SERVLET: POST /task → TaskServlet.doPost() → case "quickAddSubTask" → SubTaskHandler.handleQuickAddSubTask()
        //   Servlet trả JSON (không forward JSP) → .then(res => res.json()) cập nhật giao diện, trang không reload
        fetch(basePath + '/task', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: params.toString()
        })
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data.success) {
                if (window.showToast) window.showToast(data.message, 'success');
                window.cancelInlineCreateSubtask(parentTaskId);
                setTimeout(function() { window.location.reload(); }, 400);
            } else {
                if (window.showToast) window.showToast(data.message || 'Lỗi khi tạo nhiệm vụ', 'error');
                else alert(data.message);
            }
        })
        .catch(function(err) {
            console.error(err);
            if (window.showToast) window.showToast('Đã có lỗi xảy ra trong quá trình gửi yêu cầu!', 'error');
        });
    };

    // Close status popover when clicking anywhere outside or scrolling
    document.addEventListener('click', function(e) {
        var popover = document.getElementById('clickupStatusPopover');
        if (popover && (popover.classList.contains('show') || popover.style.display === 'block')) {
            if (!popover.contains(e.target) && !e.target.closest('.clickup-status-dot')) {
                popover.classList.remove('show');
                popover.classList.add('d-none');
                popover.style.display = 'none';
            }
        }
    });

    window.addEventListener('scroll', function() {
        var popover = document.getElementById('clickupStatusPopover');
        if (popover && (popover.classList.contains('show') || popover.style.display === 'block')) {
            popover.classList.remove('show');
            popover.classList.add('d-none');
            popover.style.display = 'none';
        }
    }, true);

    // Restore group collapse states from localStorage on DOM ready
    document.addEventListener('DOMContentLoaded', function() {
        ['done', 'inprog', 'todo'].forEach(function(gid) {
            try {
                var isCollapsed = localStorage.getItem('clickup_group_' + gid + '_collapsed');
                if (isCollapsed === 'true') {
                    var rows = document.querySelectorAll('.group-' + gid + '-row');
                    rows.forEach(function(r) { r.classList.add('d-none'); });
                    var icon = document.getElementById('chevron-' + gid);
                    if (icon) icon.className = 'bi bi-chevron-right me-1';
                }
            } catch(e) {}
        });
    });

    // 6. Search tasks across all views
    // Bỏ dấu tiếng Việt khi so khớp (giống Bảng lệnh Ctrl+K), đồng bộ lại số đếm của từng nhóm/cột
    // theo đúng số dòng đang hiển thị, và báo rõ khi không tìm thấy công việc nào phù hợp.
    function normalizeSearchText(str) {
        if (!str) return '';
        return str.toString().toLowerCase()
            .normalize('NFD')
            .replace(/[̀-ͯ]/g, '')
            .replace(/đ/g, 'd')
            .trim();
    }

    window.searchClickUpTasks = function(query) {
        var q = normalizeSearchText(query);

        // A. Chế độ Danh sách: lọc từng dòng, đồng thời đếm số dòng khớp trong mỗi nhóm trạng thái
        var groupMatchCount = { todo: 0, inprog: 0, done: 0 };
        var parentRows = document.querySelectorAll('.clickup-task-row');
        parentRows.forEach(function(pRow) {
            var title = normalizeSearchText(pRow.getAttribute('data-task-title') || pRow.innerText || '');
            var taskId = pRow.getAttribute('data-task-id');
            var subRows = taskId ? document.querySelectorAll('.clickup-subtask-row[data-parent-id="' + taskId + '"]') : [];
            var matchParent = !q || title.indexOf(q) > -1;
            var matchAnySub = false;

            subRows.forEach(function(sRow) {
                var sTitle = normalizeSearchText(sRow.getAttribute('data-subtask-title') || sRow.innerText || '');
                var sMatch = !q || sTitle.indexOf(q) > -1;
                if (sMatch) matchAnySub = true;
                sRow.style.display = (matchParent || sMatch) ? '' : 'none';
            });

            var rowMatches = matchParent || matchAnySub;
            pRow.style.display = rowMatches ? '' : 'none';

            if (rowMatches) {
                if (pRow.classList.contains('group-todo-row')) groupMatchCount.todo++;
                else if (pRow.classList.contains('group-inprog-row')) groupMatchCount.inprog++;
                else if (pRow.classList.contains('group-done-row')) groupMatchCount.done++;
            }
        });

        // Cập nhật số đếm & ẩn hẳn dải tiêu đề của nhóm không còn dòng nào khớp
        var listVisibleTotal = 0;
        ['todo', 'inprog', 'done'].forEach(function (groupId) {
            var countEl = document.getElementById('group-count-' + groupId);
            var headerRow = document.querySelector('.group-header-' + groupId);
            if (countEl) {
                if (!countEl.dataset.originalCount) {
                    countEl.dataset.originalCount = countEl.textContent.trim();
                }
                countEl.textContent = q ? String(groupMatchCount[groupId]) : countEl.dataset.originalCount;
            }
            if (headerRow) {
                headerRow.classList.toggle('d-none', !!q && groupMatchCount[groupId] === 0);
            }
            listVisibleTotal += groupMatchCount[groupId];
        });

        var listEmptyState = document.getElementById('listSearchEmptyState');
        if (listEmptyState) {
            listEmptyState.classList.toggle('d-none', !(q && listVisibleTotal === 0));
        }

        // B. Chế độ Bảng: lọc từng thẻ, đồng thời đếm số thẻ khớp trong mỗi cột
        var colMatchCount = { TODO: 0, IN_PROGRESS: 0, DONE: 0 };
        var kanbanCards = document.querySelectorAll('.kanban-card');
        kanbanCards.forEach(function(card) {
            var title = normalizeSearchText(card.getAttribute('data-task-title') || card.innerText || '');
            var cardMatches = !q || title.indexOf(q) > -1;
            card.style.display = cardMatches ? '' : 'none';
            if (cardMatches) {
                var status = card.getAttribute('data-task-status');
                if (colMatchCount.hasOwnProperty(status)) colMatchCount[status]++;
            }
        });

        var boardVisibleTotal = 0;
        [['TODO', 'column-TODO'], ['IN_PROGRESS', 'column-IN_PROGRESS'], ['DONE', 'column-DONE']].forEach(function (pair) {
            var status = pair[0], colArea = document.getElementById(pair[1]);
            var kanbanColumn = colArea ? colArea.closest('.kanban-column') : null;
            var pillCount = kanbanColumn ? kanbanColumn.querySelector('.pill-count') : null;
            if (pillCount) {
                if (!pillCount.dataset.originalCount) {
                    pillCount.dataset.originalCount = pillCount.textContent.trim();
                }
                pillCount.textContent = q ? String(colMatchCount[status]) : pillCount.dataset.originalCount;
            }
            boardVisibleTotal += colMatchCount[status];
        });

        var boardHeaderCount = document.getElementById('boardVisibleTotalCount');
        if (boardHeaderCount) {
            if (!boardHeaderCount.dataset.originalCount) {
                boardHeaderCount.dataset.originalCount = boardHeaderCount.textContent.trim();
            }
            boardHeaderCount.textContent = q ? String(boardVisibleTotal) : boardHeaderCount.dataset.originalCount;
        }

        var boardEmptyState = document.getElementById('boardSearchEmptyState');
        if (boardEmptyState) {
            boardEmptyState.classList.toggle('d-none', !(q && boardVisibleTotal === 0));
        }
    };

    // 7. Filter tasks by Member, Status, or Scope (With Active Feedback Banner & Metric Sync)

    window.filterClickUpTasks = function(mode, userId, userName) {
        document.querySelectorAll('.clickup-sidebar .clickup-nav-link').forEach(function(el) {
            el.classList.remove('active');
        });
        document.querySelectorAll('.assignee-avatar-btn, .assignee-filter-btn, .status-filter-btn').forEach(function(b) {
            b.classList.remove('active');
        });

        var currentUserId = TASK_PAGE.currentUserId;
        var filterTargetUserId = null;
        var filterTargetStatus = null;
        var banner = document.getElementById('activeFilterBanner');
        var bannerText = document.getElementById('activeFilterText');


        // Chuyển tab tasks chỉ khi không ở tab Thống kê (cho phép người dùng lọc ngay tại tab Thống kê)
        var activeTab = document.querySelector('.clickup-tab-link.active');
        var activeTabId = activeTab ? activeTab.id : '';
        if (activeTabId !== 'tab-btn-metrics' && activeTabId !== 'tab-btn-tasks') {
            window.switchClickUpTab('tasks');
        }

        if (mode === 'ALL') {
            var allBtn = document.getElementById('filter-all-btn');
            if (allBtn) allBtn.classList.add('active');
            var tbAll = document.getElementById('assignee-btn-all');
            if (tbAll) tbAll.classList.add('active');
            if (banner) { banner.classList.add('d-none'); banner.classList.remove('d-flex'); }
            filterTargetUserId = null;
            filterTargetStatus = null;
        } else if (mode === 'MY_TASKS') {
            var myBtn = document.getElementById('nav-mytasks');
            if (myBtn) myBtn.classList.add('active');
            var tbMe = document.getElementById('assignee-btn-me');
            if (tbMe) tbMe.classList.add('active');
            filterTargetUserId = currentUserId;
            if (banner && bannerText) {
                bannerText.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Đang lọc: <strong>Công việc của tôi</strong>';
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
        } else if (mode === 'USER') {
            var memberItem = document.querySelector('.member-filter-item[data-user-id="' + userId + '"]');
            if (memberItem) memberItem.classList.add('active');
            var tbAvatar = document.querySelector('.assignee-avatar-btn[data-user-id="' + userId + '"]');
            if (tbAvatar) tbAvatar.classList.add('active');
            filterTargetUserId = userId;
            if (banner && bannerText) {
                bannerText.innerHTML = '<i class="bi bi-person me-1"></i> Đang lọc theo thành viên: <strong>' + (userName || 'Thành viên') + '</strong>';
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
        } else if (mode === 'STATUS_DONE') {
            filterTargetStatus = 'DONE';
            if (banner && bannerText) {
                bannerText.innerHTML = '<i class="bi bi-check-circle-fill text-success me-1"></i> Đang lọc: <strong>Công việc đã nghiệm thu</strong>';
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
        } else if (mode === 'STATUS_IN_PROGRESS') {
            filterTargetStatus = 'IN_PROGRESS';
            if (banner && bannerText) {
                bannerText.innerHTML = '<i class="bi bi-play-circle-fill text-primary me-1"></i> Đang lọc: <strong>Công việc đang thực hiện</strong>';
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
        } else if (mode === 'STATUS_TODO') {
            filterTargetStatus = 'TODO';
            if (banner && bannerText) {
                bannerText.innerHTML = '<i class="bi bi-circle text-secondary me-1"></i> Đang lọc: <strong>Công việc chờ thực hiện (TO DO)</strong>';
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
        } else if (mode === 'STATUS_SUBMITTED') {
            filterTargetStatus = 'SUBMITTED';
            var tbSub = document.getElementById('assignee-btn-submitted');
            if (tbSub) tbSub.classList.add('active');
            if (banner && bannerText) {
                bannerText.innerHTML = '<i class="bi bi-send-check text-purple me-1"></i> Đang lọc: <strong>Công việc chờ trưởng dự án duyệt</strong>';
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
        }

        // Đồng bộ KPI tổng quan, Donut Chart và Bảng Workload
        if (typeof updateMetricsAndWorkloadForUser === 'function') {
            if (mode === 'ALL') {
                updateMetricsAndWorkloadForUser(null, null);
            } else if (mode === 'MY_TASKS') {
                updateMetricsAndWorkloadForUser(currentUserId, 'Việc của tôi');
            } else if (mode === 'USER') {
                updateMetricsAndWorkloadForUser(userId, userName);
            }
        }

        // List View filtering
        var count = 0;
        var parentRows = document.querySelectorAll('.clickup-task-row');
        parentRows.forEach(function(pRow) {
            var assigneeId = pRow.getAttribute('data-assignee-id');
            var taskId = pRow.getAttribute('data-task-id');
            var taskStatus = pRow.getAttribute('data-task-status');
            var subRows = taskId ? document.querySelectorAll('.clickup-subtask-row[data-parent-id="' + taskId + '"]') : [];

            var show = false;
            if (filterTargetStatus) {
                if (filterTargetStatus === 'DONE' && pRow.classList.contains('group-done-row')) show = true;
                else if (filterTargetStatus === 'IN_PROGRESS' && pRow.classList.contains('group-inprog-row')) show = true;
                else if (filterTargetStatus === 'TODO' && pRow.classList.contains('group-todo-row')) show = true;
                else if (filterTargetStatus === 'SUBMITTED' && taskStatus === 'SUBMITTED') show = true;

                subRows.forEach(function(sRow) {
                    sRow.style.display = show ? '' : 'none';
                });
            } else if (filterTargetUserId) {
                var matchParent = assigneeId == filterTargetUserId;
                var matchSub = false;

                subRows.forEach(function(sRow) {
                    var sAssigneeId = sRow.getAttribute('data-assignee-id');
                    var sMatch = sAssigneeId == filterTargetUserId || matchParent;
                    if (sAssigneeId == filterTargetUserId) matchSub = true;
                    sRow.style.display = sMatch ? '' : 'none';
                });

                show = matchParent || matchSub;
            } else {
                show = true;
                subRows.forEach(function(sRow) {
                    sRow.style.display = '';
                });
            }

            if (show) count++;
            pRow.style.display = show ? '' : 'none';
        });

        // Board View filtering
        var kanbanCards = document.querySelectorAll('.kanban-card');
        kanbanCards.forEach(function(card) {
            var show = true;
            if (filterTargetUserId) {
                var assigneeId = card.getAttribute('data-assignee-id');
                show = assigneeId == filterTargetUserId;
            } else if (filterTargetStatus) {
                var col = card.closest('.kanban-col-todo, .kanban-col-in-progress, .kanban-col-done');
                var cardStatus = card.getAttribute('data-task-status');
                if (filterTargetStatus === 'DONE') show = col && col.classList.contains('kanban-col-done');
                else if (filterTargetStatus === 'IN_PROGRESS') show = col && col.classList.contains('kanban-col-in-progress');
                else if (filterTargetStatus === 'TODO') show = col && col.classList.contains('kanban-col-todo');
                else if (filterTargetStatus === 'SUBMITTED') show = cardStatus === 'SUBMITTED';
            }
            card.style.display = show ? '' : 'none';
        });

        if ((filterTargetUserId || filterTargetStatus) && bannerText) {
            bannerText.innerHTML += ' <span class="badge bg-primary text-white rounded-pill ms-1">' + count + ' việc</span>';
        }
    };

    // ClickUp 3.0 Avatar click handler with toggle behavior
    window.handleToolbarAvatarClick = function(userId, userName) {
        var btn = document.querySelector('.assignee-avatar-btn[data-user-id="' + userId + '"]');
        if (btn && btn.classList.contains('active')) {
            // Toggle off -> reset to ALL
            window.filterClickUpTasks('ALL');
        } else {
            window.filterClickUpTasks('USER', userId, userName);
        }
    };

    // Đồng bộ trạng thái dòng được chọn trong Bảng Workload khi lọc theo thành viên
    window.updateMetricsAndWorkloadForUser = function(userId, userName) {
        var rows = document.querySelectorAll('#workloadTable tbody tr.workload-row');
        if (!userId) {
            rows.forEach(function(row) {
                row.classList.remove('workload-row-active', 'workload-row-dimmed');
            });
            return;
        }

        rows.forEach(function(row) {
            if (row.getAttribute('data-user-id') == userId) {
                row.classList.add('workload-row-active');
                row.classList.remove('workload-row-dimmed');
                try {
                    row.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                } catch(e) {}
            } else {
                row.classList.remove('workload-row-active');
                row.classList.add('workload-row-dimmed');
            }
        });
    };

    // Xử lý khi click vào một hàng trong Bảng Workload
    window.handleWorkloadRowClick = function(userId, userName) {
        var activeAvatar = document.querySelector('.assignee-avatar-btn[data-user-id="' + userId + '"].active');
        if (activeAvatar) {
            window.filterClickUpTasks('ALL');
        } else {
            window.filterClickUpTasks('USER', userId, userName);
        }
    };

    // Sắp xếp nhanh các cột trong Bảng Workload
    var currentSortCol = -1;
    var currentSortAsc = true;

    window.sortWorkloadTable = function(colIdx) {
        var table = document.getElementById('workloadTable');
        if (!table) return;
        var tbody = table.querySelector('tbody');
        if (!tbody) return;
        var rows = Array.from(tbody.querySelectorAll('tr.workload-row'));

        if (currentSortCol === colIdx) {
            currentSortAsc = !currentSortAsc;
        } else {
            currentSortCol = colIdx;
            currentSortAsc = false; // Mặc định giảm dần với các chỉ số số học
            if (colIdx === 0) currentSortAsc = true; // A-Z với tên
        }

        // Cập nhật icon sort
        for (var i = 0; i <= 7; i++) {
            var icon = document.getElementById('sort-icon-' + i);
            if (icon) {
                if (i === colIdx) {
                    icon.className = currentSortAsc ? 'bi bi-sort-up sort-icon text-primary opacity-100' : 'bi bi-sort-down sort-icon text-primary opacity-100';
                } else {
                    icon.className = 'bi bi-arrow-down-up sort-icon';
                }
            }
        }

        var capRank = { 'overload': 3, 'busy': 2, 'optimal': 1, 'ready': 0 };

        rows.sort(function(a, b) {
            var valA, valB;
            if (colIdx === 0) {
                valA = (a.getAttribute('data-name') || '').toLowerCase();
                valB = (b.getAttribute('data-name') || '').toLowerCase();
                return currentSortAsc ? valA.localeCompare(valB, 'vi') : valB.localeCompare(valA, 'vi');
            } else if (colIdx === 1) {
                valA = capRank[a.getAttribute('data-capacity')] || 0;
                valB = capRank[b.getAttribute('data-capacity')] || 0;
            } else if (colIdx === 2) {
                valA = parseFloat(a.getAttribute('data-total')) || 0;
                valB = parseFloat(b.getAttribute('data-total')) || 0;
            } else if (colIdx === 3) {
                valA = parseFloat(a.getAttribute('data-todo')) || 0;
                valB = parseFloat(b.getAttribute('data-todo')) || 0;
            } else if (colIdx === 4) {
                valA = parseFloat(a.getAttribute('data-inprog')) || 0;
                valB = parseFloat(b.getAttribute('data-inprog')) || 0;
            } else if (colIdx === 5) {
                valA = parseFloat(a.getAttribute('data-done')) || 0;
                valB = parseFloat(b.getAttribute('data-done')) || 0;
            } else if (colIdx === 6) {
                valA = parseFloat(a.getAttribute('data-overdue')) || 0;
                valB = parseFloat(b.getAttribute('data-overdue')) || 0;
            } else if (colIdx === 7) {
                valA = parseFloat(a.getAttribute('data-rate')) || 0;
                valB = parseFloat(b.getAttribute('data-rate')) || 0;
            }

            return currentSortAsc ? (valA - valB) : (valB - valA);
        });

        rows.forEach(function(row) {
            tbody.appendChild(row);
            var uid = row.getAttribute('data-user-id');
            var detailRow = document.getElementById('wl-tasks-' + uid);
            if (detailRow) {
                tbody.appendChild(detailRow);
            }
        });
    };

    // Lọc nhanh theo tình trạng công suất tải việc (Workload Capacity Filters)
    window.filterWorkloadCapacity = function(type, tabEl, cardEl) {
        // Cập nhật active trên các nút tab filter
        var tabs = document.querySelectorAll('.workload-capacity-filter-btn');
        tabs.forEach(function(t) {
            if (t.getAttribute('data-filter') === type) {
                t.classList.add('active');
            } else {
                t.classList.remove('active');
            }
        });

        // Cập nhật active trên các thẻ KPI Capacity Pulse
        var cards = document.querySelectorAll('.capacity-summary-card');
        cards.forEach(function(c) { c.classList.remove('active'); });
        if (type === 'ALL') {
            var cAll = document.getElementById('cap-card-all');
            if (cAll) cAll.classList.add('active');
        } else if (type === 'OVERLOAD') {
            var cOver = document.getElementById('cap-card-overload');
            if (cOver) cOver.classList.add('active');
        } else if (type === 'BUSY') {
            var cBusy = document.getElementById('cap-card-busy');
            if (cBusy) cBusy.classList.add('active');
        } else if (type === 'OPTIMAL') {
            var cOpt = document.getElementById('cap-card-optimal');
            if (cOpt) cOpt.classList.add('active');
        }

        var rows = document.querySelectorAll('#workloadTable tbody tr.workload-row');
        var matched = 0;
        rows.forEach(function(r) {
            var cap = r.getAttribute('data-capacity') || '';
            var overdue = parseInt(r.getAttribute('data-overdue') || '0', 10);
            var inprog = parseInt(r.getAttribute('data-inprog') || '0', 10);
            var uid = r.getAttribute('data-user-id');
            var detailRow = document.getElementById('wl-tasks-' + uid);

            var show = false;
            if (type === 'ALL') {
                show = true;
            } else if (type === 'OVERLOAD') {
                show = (cap === 'overload' || overdue > 0);
            } else if (type === 'BUSY') {
                show = (cap === 'busy' || inprog >= 3);
            } else if (type === 'OPTIMAL') {
                show = (cap === 'optimal' || cap === 'ready');
            }

            r.style.display = show ? '' : 'none';
            if (detailRow && !show) {
                detailRow.classList.add('d-none');
                var chev = document.getElementById('wl-chevron-' + uid);
                if (chev) chev.classList.remove('rotate-180');
            }
            if (show) matched++;
        });

        var countBadge = document.getElementById('workload-member-count');
        if (countBadge) {
            countBadge.textContent = (type === 'ALL') ? (rows.length + ' thành viên') : (matched + '/' + rows.length + ' thành viên');
        }
    };

    // Đóng / Mở danh sách công việc của thành viên (Inline Accordion)
    window.toggleWorkloadMemberTasks = function(userId, event) {
        if (event) event.stopPropagation();
        var detailRow = document.getElementById('wl-tasks-' + userId);
        var chevron = document.getElementById('wl-chevron-' + userId);
        if (!detailRow) return;

        var isHidden = detailRow.classList.contains('d-none');
        if (isHidden) {
            detailRow.classList.remove('d-none');
            if (chevron) chevron.classList.add('rotate-180');
        } else {
            detailRow.classList.add('d-none');
            if (chevron) chevron.classList.remove('rotate-180');
        }
    };

    // Chuyển nhanh sang tab Tasks và lọc trực tiếp theo thành viên
    window.viewMemberTasks = function(userId, userName, event) {
        if (event) event.stopPropagation();
        switchClickUpTab('tasks');
        if (typeof filterClickUpTasks === 'function') {
            filterClickUpTasks('USER', userId, userName);
        }
    };

    // Tìm kiếm nhanh thành viên trong Bảng Workload
    window.filterWorkloadTable = function(query) {
        var q = (query || '').trim().toLowerCase();
        var rows = document.querySelectorAll('#workloadTable tbody tr.workload-row');
        var matched = 0;
        rows.forEach(function(r) {
            var name = (r.getAttribute('data-name') || '').toLowerCase();
            var uid = r.getAttribute('data-user-id');
            var detailRow = document.getElementById('wl-tasks-' + uid);
            var isMatch = !q || name.indexOf(q) > -1;
            r.style.display = isMatch ? '' : 'none';
            if (detailRow && !isMatch) {
                detailRow.classList.add('d-none');
                var chev = document.getElementById('wl-chevron-' + uid);
                if (chev) chev.classList.remove('rotate-180');
            }
            if (isMatch) matched++;
        });
        var countBadge = document.getElementById('workload-member-count');
        if (countBadge) {
            countBadge.textContent = q ? (matched + '/' + rows.length + ' người') : (rows.length + ' thành viên');
        }
    };

    // Lọc theo khoảng thời gian hạn chót
    window.filterTasksByTimeframe = function(timeframe, label) {
        var labelEl = document.getElementById('currentTimeframeLabel');
        if (labelEl) labelEl.textContent = label;

        // Cập nhật active trong dropdown
        var dropdownEl = document.getElementById('timeframeDropdownBtn');
        if (dropdownEl && dropdownEl.parentElement) {
            var menu = dropdownEl.parentElement.querySelector('.dropdown-menu');
            if (menu) {
                menu.querySelectorAll('.dropdown-item').forEach(function(item) {
                    if (item.textContent.indexOf(label) > -1) item.classList.add('active');
                    else item.classList.remove('active');
                });
            }
        }

        // Lọc trong danh sách List View & Board View
        var parentRows = document.querySelectorAll('.clickup-task-row');
        var now = new Date();
        var startOfWeek = new Date(now);
        startOfWeek.setDate(now.getDate() - now.getDay());
        var endOfWeek = new Date(now);
        endOfWeek.setDate(startOfWeek.getDate() + 6);

        var matchCount = 0;
        parentRows.forEach(function(pRow) {
            var taskId = pRow.getAttribute('data-task-id');
            var subRows = taskId ? document.querySelectorAll('.clickup-subtask-row[data-parent-id="' + taskId + '"]') : [];
            var lastTd = pRow.querySelector('td:last-child');
            var dueDateStr = lastTd ? lastTd.textContent.trim() : '';

            var show = true;
            if (timeframe === 'ALL') {
                show = true;
            } else if (timeframe === 'OVERDUE') {
                var isOverdue = pRow.querySelector('.badge.bg-danger') || (dueDateStr && dueDateStr !== '—' && new Date(dueDateStr) < now && !pRow.classList.contains('group-done-row'));
                show = !!isOverdue;
            } else if (timeframe === 'THIS_WEEK' || timeframe === 'THIS_MONTH') {
                if (!dueDateStr || dueDateStr === '—') {
                    show = false;
                } else {
                    var d = new Date(dueDateStr);
                    if (isNaN(d.getTime())) {
                        show = true;
                    } else if (timeframe === 'THIS_WEEK') {
                        show = (d >= startOfWeek && d <= endOfWeek);
                    } else if (timeframe === 'THIS_MONTH') {
                        show = (d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear());
                    }
                }
            }

            pRow.style.display = show ? '' : 'none';
            subRows.forEach(function(sRow) {
                sRow.style.display = show ? '' : 'none';
            });
            if (show) matchCount++;
        });

        // Banner phản hồi
        var banner = document.getElementById('activeFilterBanner');
        var bannerText = document.getElementById('activeFilterText');
        if (timeframe !== 'ALL') {
            if (banner && bannerText) {
                bannerText.innerHTML = '<i class="bi bi-calendar3 text-primary me-1"></i> Đang lọc theo thời gian: <strong>' + label + '</strong> <span class="badge bg-primary text-white rounded-pill ms-1">' + matchCount + ' việc</span>';
                banner.classList.remove('d-none');
                banner.classList.add('d-flex');
            }
        } else {
            if (banner) { banner.classList.add('d-none'); banner.classList.remove('d-flex'); }
        }
    };

    // 8. Open Inbox Drawer safely
    window.openInboxDrawer = function() {
        var el = document.getElementById('inboxDrawer');
        if (el && window.bootstrap && window.bootstrap.Offcanvas) {
            var bsOffcanvas = bootstrap.Offcanvas.getInstance(el) || new bootstrap.Offcanvas(el);
            bsOffcanvas.show();
        }
    };

    // 9. ClickUp 3.0 Unified Side-Peek Task & Subtask Drawer Controller
    window.openClickUpTask = function(taskId, subtaskId) {
        if (!taskId) return;

        // A. Ẩn toàn bộ các Pane Task và Subtask đang mở
        var allPanes = document.querySelectorAll('.task-detail-pane, .subtask-detail-pane');
        allPanes.forEach(function(pane) {
            pane.classList.add('d-none');
        });

        // B. Kích hoạt Pane được chỉ định
        var targetPane = null;
        if (subtaskId) {
            targetPane = document.getElementById('subtaskPane-' + subtaskId);
        } else {
            targetPane = document.getElementById('taskPane-' + taskId);
        }

        if (targetPane) {
            targetPane.classList.remove('d-none');
            var drawerContent = document.getElementById('clickupDrawerContent');
            if (drawerContent) {
                drawerContent.scrollTop = 0;
            }
        }

        // C. Mở Offcanvas Drawer từ mép phải
        var drawerEl = document.getElementById('clickupTaskDrawer');
        if (drawerEl && window.bootstrap && window.bootstrap.Offcanvas) {
            var bsOffcanvas = bootstrap.Offcanvas.getInstance(drawerEl) || new bootstrap.Offcanvas(drawerEl);
            bsOffcanvas.show();
        }
    };

    // 10. ClickUp & Asana Style Confetti Celebration Animation
    window.fireConfettiCelebration = function() {
        if (typeof confetti === 'function') {
            var count = 200;
            var defaults = {
                origin: { y: 0.7 },
                zIndex: 99999
            };

            function fire(particleRatio, opts) {
                confetti(Object.assign({}, defaults, opts, {
                    particleCount: Math.floor(count * particleRatio)
                }));
            }

            fire(0.25, { spread: 26, startVelocity: 55, colors: ['#4f46e5', '#38bdf8', '#10b981', '#f59e0b', '#ec4899'] });
            fire(0.2, { spread: 60, colors: ['#6366f1', '#06b6d4', '#34d399', '#fbbf24'] });
            fire(0.35, { spread: 100, decay: 0.91, scalar: 0.8 });
            fire(0.1, { spread: 120, startVelocity: 25, decay: 0.92, colors: ['#a855f7', '#3b82f6', '#10b981'] });
            fire(0.1, { spread: 120, startVelocity: 45 });
        }
    };

    // 11. Quick Toggle Subtasks (Đóng ⇄ Mở rộng)
    window.quickToggleSubtaskMode = function() {
        if (typeof window.toggleAllSubtasks === 'function') {
            window.toggleAllSubtasks();
        }
    };

    // 12. ClickUp 3.0 Favorite Projects (Starred Spaces via localStorage)
    window.getFavoriteProjects = function() {
        try {
            var raw = localStorage.getItem('teamwork_fav_projects');
            return raw ? JSON.parse(raw) : [];
        } catch(e) {
            return [];
        }
    };

    window.saveFavoriteProjects = function(list) {
        try {
            localStorage.setItem('teamwork_fav_projects', JSON.stringify(list));
        } catch(e) {}
    };

    window.toggleProjectFavorite = function(projId, projName) {
        var list = window.getFavoriteProjects();
        var index = list.findIndex(function(item) { return item.id === projId; });
        var starBtn = document.getElementById('btnStarProject');

        if (index > -1) {
            list.splice(index, 1);
            if (starBtn) {
                starBtn.classList.remove('text-warning', 'bi-star-fill');
                starBtn.classList.add('text-muted', 'bi-star');
            }
            if (window.showToast) window.showToast('Đã bỏ dự án khỏi mục yêu thích', 'info');
        } else {
            list.push({ id: projId, name: projName });
            if (starBtn) {
                starBtn.classList.remove('text-muted', 'bi-star');
                starBtn.classList.add('text-warning', 'bi-star-fill');
            }
            if (window.showToast) window.showToast('Đã thêm dự án vào mục yêu thích ⭐', 'success');
            if (window.fireConfettiCelebration) window.fireConfettiCelebration();
        }
        window.saveFavoriteProjects(list);
        window.renderSidebarFavorites();
    };

    window.renderSidebarFavorites = function() {
        var container = document.getElementById('favoritesList');
        if (!container) return;
        var list = window.getFavoriteProjects();
        var currentProjectId = TASK_PAGE.projectId;

        if (list.length === 0) {
            container.innerHTML = '<span class="fs-9 text-muted px-2 py-1 fst-italic">Chưa có dự án yêu thích</span>';
            return;
        }

        var html = '';
        list.forEach(function(item) {
            var isActive = item.id === currentProjectId;
            var url = TASK_PAGE.contextPath + '/task?action=list&projectId=' + item.id;
            html += '<a href="' + url + '" class="clickup-space-item ' + (isActive ? 'active' : '') + '">';
            html += '<span class="clickup-space-icon text-warning"><i class="bi bi-star-fill"></i></span>';
            html += '<span class="text-truncate flex-grow-1 fs-8 fw-medium">' + item.name + '</span>';
            html += '</a>';
        });
        container.innerHTML = html;
    };

    // Khởi tạo Bootstrap tooltips, Favorites và Auto Confetti khi DOM sẵn sàng
    document.addEventListener('DOMContentLoaded', function() {
        if (window.bootstrap && bootstrap.Tooltip) {
            var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            tooltipTriggerList.map(function(el) {
                return new bootstrap.Tooltip(el);
            });
        }

        // Khởi tạo trạng thái Ngôi sao và Danh sách Yêu thích Sidebar
        var favs = window.getFavoriteProjects();
        var isFav = favs.some(function(item) { return item.id === TASK_PAGE.projectId; });
        var starBtn = document.getElementById('btnStarProject');
        if (starBtn && isFav) {
            starBtn.classList.remove('text-muted', 'bi-star');
            starBtn.classList.add('text-warning', 'bi-star-fill');
        }
        window.renderSidebarFavorites();

        // Tự động bắn pháo hoa khi hoàn thành bất kỳ cột mốc nào (Gate 1, Gate 2, Task In Progress/Done, Subtask)
        if (TASK_PAGE.toastSuccess) {
            var toastMsg = String(TASK_PAGE.toastSuccess).toLowerCase();
            if (toastMsg.indexOf('duyệt') > -1 || 
                toastMsg.indexOf('nghiệm thu') > -1 || 
                toastMsg.indexOf('hoàn thành') > -1 || 
                toastMsg.indexOf('thành công') > -1 || 
                toastMsg.indexOf('tiến hành') > -1 || 
                toastMsg.indexOf('đang làm') > -1 || 
                toastMsg.indexOf('bắt đầu') > -1 || 
                toastMsg.indexOf('in progress') > -1 || 
                toastMsg.indexOf('khóa kế hoạch') > -1 || 
                toastMsg.indexOf('subtask') > -1) {
                setTimeout(function() {
                    if (window.fireConfettiCelebration) window.fireConfettiCelebration();
                }, 400);
            }
        }
    });
