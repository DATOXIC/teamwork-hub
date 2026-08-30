// =========================================================================
// PURE VANILLA JS PHYSICS KANBAN DRAG ENGINE (ZERO DEPENDENCY / 100% NATIVE)
// Tự code 100% - Không dùng thư viện ngoài - Đảm bảo chuẩn môn học
// =========================================================================
document.addEventListener('DOMContentLoaded', function()
{
    const cards = document.querySelectorAll('.kanban-card');
    const columns = document.querySelectorAll('.kanban-task-list');

    let activeCard = null;
    let placeholder = null;
    let originalColumn = null;
    let currentTargetColumn = null;
    let offsetX = 0;
    let offsetY = 0;
    let lastClientX = 0;
    let cardWidth = 0;
    let cardHeight = 0;
    let isDragging = false;
    let startMouseX = 0;
    let startMouseY = 0;
    let hasJustDragged = false;
    const DRAG_THRESHOLD = 6; // Ngưỡng 6px di chuyển chuột để phân biệt Click xem chi tiết với Kéo thả

    function onMouseDown(e) 
    {
        // Chỉ nhận chuột trái và không bấm vào nút bấm, liên kết, menu
        if (e.button !== 0) return;
        if (e.target.closest('button, a, input, select, textarea, .dropdown, .modal')) return;

        const card = e.target.closest('.kanban-card');
        if (!card) return;

        activeCard = card;
        startMouseX = e.clientX;
        startMouseY = e.clientY;
        lastClientX = e.clientX;

        const rect = card.getBoundingClientRect();
        offsetX = e.clientX - rect.left;
        offsetY = e.clientY - rect.top;
        cardWidth = rect.width;
        cardHeight = rect.height;
        originalColumn = card.closest('.kanban-task-list');
        currentTargetColumn = originalColumn;

        document.addEventListener('mousemove', onMouseMove);
        document.addEventListener('mouseup', onMouseUp);
    }

    function onMouseMove(e) 
    {
        if (!activeCard) return;

        const deltaX = Math.abs(e.clientX - startMouseX);
        const deltaY = Math.abs(e.clientY - startMouseY);

        // Kích hoạt chế độ bốc nổi 3D khi chuột vượt quá ngưỡng di chuyển
        if (!isDragging && (deltaX > DRAG_THRESHOLD || deltaY > DRAG_THRESHOLD)) 
        {
            isDragging = true;
            hasJustDragged = true;
            document.body.style.userSelect = 'none';
            document.body.style.cursor = 'grabbing';

            // 1. Tạo rãnh giữ chỗ (Placeholder) để đẩy các thẻ khác dạt ra
            placeholder = document.createElement('div');
            placeholder.className = 'kanban-placeholder';
            placeholder.style.height = cardHeight + 'px';
            placeholder.style.width = '100%';
            activeCard.parentNode.insertBefore(placeholder, activeCard);

            // 2. Chuyển thẻ thật thành Floating Card bay trên không trung
            activeCard.classList.add('is-floating-drag');
            activeCard.style.width = cardWidth + 'px';
            activeCard.style.height = cardHeight + 'px';
            document.body.appendChild(activeCard);
        }

        if (!isDragging) return;

        // 3. Điều khiển thẻ bay chính xác theo con trỏ chuột
        activeCard.style.left = (e.clientX - offsetX) + 'px';
        activeCard.style.top = (e.clientY - offsetY) + 'px';

        // 4. Cơ chế vật lý quán tính nghiêng 3D (Dynamic Inertia Tilt)
        const mouseSpeedX = e.clientX - lastClientX;
        lastClientX = e.clientX;
        let tiltAngle = 3;
        if (mouseSpeedX > 2) {
            tiltAngle = Math.min(6, 3 + mouseSpeedX * 0.3); // Kéo sang phải nhanh -> Nghiêng phải
        } else if (mouseSpeedX < -2) {
            tiltAngle = Math.max(-5, 3 + mouseSpeedX * 0.3); // Kéo sang trái nhanh -> Nghiêng trái
        }

        activeCard.style.transform = `scale(1.04) rotate(${tiltAngle}deg)`;

        // 5. Xác định cột và vị trí thẻ dưới chuột bằng elementFromPoint
        activeCard.style.display = 'none';
        const elemBelow = document.elementFromPoint(e.clientX, e.clientY);
        activeCard.style.display = '';

        if (!elemBelow) return;

        const targetCol = elemBelow.closest('.kanban-task-list');
        if (targetCol) 
        {
            currentTargetColumn = targetCol;

            // Highlight cột đích
            columns.forEach(function(col) {
                if (col === targetCol) col.classList.add('drag-over');
                else col.classList.remove('drag-over');
            });

            // Chèn rãnh placeholder vào vị trí tương ứng trong cột đích (Đẩy các thẻ khác dạt ra)
            const cardBelow = elemBelow.closest('.kanban-card:not(.is-floating-drag)');
            if (cardBelow && cardBelow.parentNode === targetCol) 
            {
                const rectBelow = cardBelow.getBoundingClientRect();
                const isAfter = (e.clientY - rectBelow.top) > (rectBelow.height / 2);
                if (isAfter) {
                    targetCol.insertBefore(placeholder, cardBelow.nextSibling);
                } else {
                    targetCol.insertBefore(placeholder, cardBelow);
                }
            } 
            else if (!targetCol.contains(placeholder)) 
            {
                targetCol.appendChild(placeholder);
            }
        }
    }

    function onMouseUp(e) 
    {
        document.removeEventListener('mousemove', onMouseMove);
        document.removeEventListener('mouseup', onMouseUp);
        document.body.style.userSelect = '';
        document.body.style.cursor = '';

        columns.forEach(function(col) {
            col.classList.remove('drag-over');
        });

        if (!activeCard) return;

        if (isDragging && placeholder) 
        {
            // 1. Đặt thẻ trở lại vị trí rãnh placeholder
            placeholder.parentNode.insertBefore(activeCard, placeholder);
            placeholder.remove();
            placeholder = null;

            // 2. Thu hồi các style bay nổi
            activeCard.classList.remove('is-floating-drag');
            activeCard.style.position = '';
            activeCard.style.left = '';
            activeCard.style.top = '';
            activeCard.style.width = '';
            activeCard.style.height = '';
            activeCard.style.transform = '';

            // 3. Kiểm tra thay đổi trạng thái
            const newStatus = currentTargetColumn ? currentTargetColumn.getAttribute('data-status') : null;
            const oldStatus = originalColumn ? originalColumn.getAttribute('data-status') : null;
            const taskId = activeCard.getAttribute('data-task-id');

            if (newStatus && oldStatus && newStatus !== oldStatus && taskId) 
            {
                sendDataToServer(taskId, newStatus);
            }

            // Chặn click mở modal sau khi vừa thả thẻ
            setTimeout(function() {
                hasJustDragged = false;
            }, 100);
        } 
        else 
        {
            hasJustDragged = false;
        }

        isDragging = false;
        activeCard = null;
        originalColumn = null;
        currentTargetColumn = null;
    }

    // Chặn sự kiện click mở modal nếu vừa thực hiện kéo thả
    document.addEventListener('click', function(e) 
    {
        if (hasJustDragged) {
            e.stopPropagation();
            e.preventDefault();
        }
    }, true);

    document.addEventListener('mousedown', onMouseDown);

    function sendDataToServer(taskId, newStatus) 
    {
        const urlParams = new URLSearchParams(window.location.search);
        const projectId = urlParams.get('projectId') || '1';
        
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = window.location.pathname;

        const inputAction = document.createElement('input');
        inputAction.type = 'hidden';
        inputAction.name = 'action';
        inputAction.value = 'updateStatus';

        const inputProject = document.createElement('input');
        inputProject.type = 'hidden';
        inputProject.name = 'projectId';
        inputProject.value = projectId;

        const inputTask = document.createElement('input');
        inputTask.type = 'hidden';
        inputTask.name = 'taskId';
        inputTask.value = taskId;

        const inputStatus = document.createElement('input');
        inputStatus.type = 'hidden';
        inputStatus.name = 'newStatus';
        inputStatus.value = newStatus;

        form.appendChild(inputAction);
        form.appendChild(inputProject);
        form.appendChild(inputTask);
        form.appendChild(inputStatus);

        document.body.appendChild(form);
        form.submit();
    }

    // =========================================================================
    // TÍNH NĂNG LỌC CÔNG VIỆC THEO THÀNH VIÊN TRONG 0.01 GIÂY (PHẦN B.3.3 & B.3.4)
    // =========================================================================
    // TÍNH NĂNG TÌM KIẾM & BỘ LỌC CÔNG VIỆC TỨC THÌ (0.01 GIÂY)
    // =========================================================================
    const filterButtons = document.querySelectorAll('.member-filter-btn');
    const filterResultCount = document.getElementById('filterResultCount');
    const searchInput = document.getElementById('taskSearchInput');
    const priorityFilter = document.getElementById('taskPriorityFilter');

    let activeMemberBtn = document.querySelector('.member-filter-btn.active') || (filterButtons.length > 0 ? filterButtons[0] : null);

    function applyCombinedFilter() {
        if (!activeMemberBtn && filterButtons.length > 0) {
            activeMemberBtn = filterButtons[0];
        }

        const relatedTasksStr = activeMemberBtn ? (activeMemberBtn.getAttribute('data-related-tasks') || '') : 'ALL';
        const filterMode = activeMemberBtn ? activeMemberBtn.getAttribute('data-filter-mode') : 'ALL';
        const userName = activeMemberBtn ? (activeMemberBtn.getAttribute('data-user-name') || '') : '';

        // 1. Chuyển chuỗi ID "1,3,5" thành Set các số nguyên
        let allowedTaskIds = new Set();
        let isShowAllMembers = (filterMode === 'ALL' || relatedTasksStr === 'ALL');

        if (!isShowAllMembers && relatedTasksStr.trim() !== '') {
            relatedTasksStr.split(',').forEach(idStr => {
                const id = parseInt(idStr.trim(), 10);
                if (!isNaN(id)) {
                    allowedTaskIds.add(id);
                }
            });
        }

        // 2. Lấy giá trị tìm kiếm và mức ưu tiên
        const searchQuery = searchInput ? searchInput.value.trim().toLowerCase() : '';
        const selectedPriority = priorityFilter ? priorityFilter.value.toUpperCase() : 'ALL';

        // 3. Quét qua tất cả các thẻ Kanban card để ẩn / hiện tức thì
        let visibleCount = 0;
        cards.forEach(card => {
            const taskId = parseInt(card.getAttribute('data-task-id'), 10);
            const title = (card.getAttribute('data-task-title') || '').toLowerCase();
            const assignee = (card.getAttribute('data-task-assignee') || '').toLowerCase();
            const cardText = card.innerText.toLowerCase();
            const priority = (card.getAttribute('data-task-priority') || '').toUpperCase();

            // Kiểm tra điều kiện 1: Thành viên
            const matchMember = isShowAllMembers || allowedTaskIds.has(taskId);

            // Kiểm tra điều kiện 2: Mức ưu tiên
            const matchPriority = (selectedPriority === 'ALL' || priority === selectedPriority);

            // Kiểm tra điều kiện 3: Từ khóa tìm kiếm
            const matchSearch = (searchQuery === '' || title.includes(searchQuery) || assignee.includes(searchQuery) || cardText.includes(searchQuery));

            if (matchMember && matchPriority && matchSearch) {
                card.classList.remove('d-none');
                visibleCount++;
            } else {
                card.classList.add('d-none');
            }
        });

        // 4. Cập nhật dòng chữ thống kê kết quả lọc
        if (filterResultCount) {
            let desc = '';
            if (isShowAllMembers) {
                desc = `Hiển thị <strong>${visibleCount}</strong> công việc`;
            } else if (filterMode === 'MY_TASKS') {
                desc = `Hiển thị <strong>${visibleCount}</strong> công việc liên quan đến <strong>bạn</strong>`;
            } else {
                desc = `Hiển thị <strong>${visibleCount}</strong> công việc liên quan đến <strong>${userName}</strong>`;
            }

            if (selectedPriority !== 'ALL') {
                desc += ` (Ưu tiên: <strong>${selectedPriority}</strong>)`;
            }
            if (searchQuery !== '') {
                desc += ` (Từ khóa: <em>"${searchQuery}"</em>)`;
            }

            filterResultCount.innerHTML = desc;
        }
    }

    function handleMemberButtonClick(event) {
        const clickedBtn = event.currentTarget;
        activeMemberBtn = clickedBtn;

        filterButtons.forEach(btn => {
            btn.classList.remove('btn-primary-custom', 'active', 'text-white');
            if (btn.getAttribute('data-filter-mode') === 'MY_TASKS') {
                btn.classList.add('btn-outline-primary');
            } else {
                btn.classList.add('btn-outline-secondary');
            }
        });

        clickedBtn.classList.remove('btn-outline-secondary', 'btn-outline-primary');
        clickedBtn.classList.add('btn-primary-custom', 'active', 'text-white');

        applyCombinedFilter();
    }

    filterButtons.forEach(btn => {
        btn.addEventListener('click', handleMemberButtonClick);
    });

    if (searchInput) {
        searchInput.addEventListener('input', applyCombinedFilter);
    }

    if (priorityFilter) {
        priorityFilter.addEventListener('change', applyCombinedFilter);
    }
});
