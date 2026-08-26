// Bước 1: Dùng tay cầm một món đồ lên → Trình duyệt gọi là sự kiện dragstart.
// Bước 2: Cầm món đồ rê qua một cái bàn khác → Trình duyệt gọi là sự kiện dragover.
// Bước 3: Đặt món đồ xuống cái bàn mới → Trình duyệt gọi là sự kiện drop.
// Bước 4: Báo với Quản lý (Servlet): "Tôi vừa chuyển đồ số 3 sang Bàn Đang Làm!" → Gửi dữ liệu lên Server.

document.addEventListener('DOMContentLoaded', function()
{
    // 1. Tìm tất cả các thẻ công việc có trên màn hình
    const cards = document.querySelectorAll('.kanban-card');
    // 2. Tìm 3 vùng chứa cột (Cần làm, Đang làm, Đã xong)
    const columns = document.querySelectorAll('.kanban-task-list');
    // 3. Biến tạm để ghi nhớ xem mình đang cầm thẻ Task số mấy
    let draggedTaskId = null;

    // GẮN SỰ KIỆN KÉO THẢ CHO CÁC THẺ
    function handleDragStart(event) 
    {
        const currentCard = event.currentTarget;
        draggedTaskId = currentCard.getAttribute('data-task-id');
        currentCard.classList.add('opacity-50');
    }

    function handleDragEnd(event) 
    {
        const currentCard = event.currentTarget;
        currentCard.classList.remove('opacity-50');
    }

    for (const card of cards) 
    {
        card.addEventListener('dragstart', handleDragStart);
        card.addEventListener('dragend', handleDragEnd);
    }

    // Hàm 3: Cho phép rê thẻ bay qua cột này (Mở khóa vùng thả)
    function handleDragOver(event) 
    {
        event.preventDefault();
    }

    // Xử lý khoảnh khắc THẺ RƠI XUỐNG CỘT (Sự kiện drop)
    function handleDrop(event) 
    {
        event.preventDefault();
        const targetColumn = event.currentTarget;
        const newStatus = targetColumn.getAttribute('data-status');

        if (draggedTaskId != null && newStatus != null) 
        {
            sendDataToServer(draggedTaskId, newStatus);
        }
    }

    for (const column of columns) 
    {
        column.addEventListener('dragover', handleDragOver);
        column.addEventListener('drop', handleDrop);
    }

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
    const filterButtons = document.querySelectorAll('.member-filter-btn');
    const filterResultCount = document.getElementById('filterResultCount');

    function applyMemberFilter(event) {
        const clickedBtn = event.currentTarget;
        const relatedTasksStr = clickedBtn.getAttribute('data-related-tasks') || '';
        const filterMode = clickedBtn.getAttribute('data-filter-mode');
        const userName = clickedBtn.getAttribute('data-user-name') || '';

        // 1. Cập nhật trạng thái active cho các nút bấm
        filterButtons.forEach(btn => {
            btn.classList.remove('btn-primary-custom', 'active', 'text-white');
            if (btn.getAttribute('data-filter-mode') === 'MY_TASKS') {
                btn.classList.add('btn-outline-primary');
            } else if (btn.getAttribute('data-filter-mode') === 'ALL') {
                btn.classList.add('btn-outline-secondary');
            } else {
                btn.classList.add('btn-outline-secondary');
            }
        });

        clickedBtn.classList.remove('btn-outline-secondary', 'btn-outline-primary');
        clickedBtn.classList.add('btn-primary-custom', 'active', 'text-white');

        // 2. Chuyển chuỗi ID "1,3,5" thành Set các số nguyên
        let allowedTaskIds = new Set();
        let isShowAll = (filterMode === 'ALL' || relatedTasksStr === 'ALL');

        if (!isShowAll && relatedTasksStr.trim() !== '') {
            relatedTasksStr.split(',').forEach(idStr => {
                const id = parseInt(idStr.trim(), 10);
                if (!isNaN(id)) {
                    allowedTaskIds.add(id);
                }
            });
        }

        // 3. Quét qua tất cả các thẻ Kanban card để ẩn / hiện tức thì
        let visibleCount = 0;
        cards.forEach(card => {
            const taskId = parseInt(card.getAttribute('data-task-id'), 10);

            if (isShowAll || allowedTaskIds.has(taskId)) {
                card.classList.remove('d-none');
                visibleCount++;
            } else {
                card.classList.add('d-none');
            }
        });

        // 4. Cập nhật dòng chữ thống kê kết quả lọc
        if (filterResultCount) {
            if (isShowAll) {
                filterResultCount.innerHTML = `Hiển thị tất cả <strong>${visibleCount}</strong> công việc`;
            } else if (filterMode === 'MY_TASKS') {
                filterResultCount.innerHTML = `Hiển thị <strong>${visibleCount}</strong> công việc liên quan đến <strong>bạn</strong>`;
            } else {
                filterResultCount.innerHTML = `Hiển thị <strong>${visibleCount}</strong> công việc liên quan đến <strong>${userName}</strong>`;
            }
        }
    }

    filterButtons.forEach(btn => {
        btn.addEventListener('click', applyMemberFilter);
    });
});
