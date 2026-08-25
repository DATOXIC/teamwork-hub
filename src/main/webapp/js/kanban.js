// Bước 1: Dùng tay cầm một món đồ lên → Trình duyệt gọi là sự kiện dragstart.
// Bước 2: Cầm món đồ rê qua một cái bàn khác → Trình duyệt gọi là sự kiện dragover.
// Bước 3: Đặt món đồ xuống cái bàn mới → Trình duyệt gọi là sự kiện drop.
// Bước 4: Báo với Quản lý (Servlet): "Tôi vừa chuyển đồ số 3 sang Bàn Đang Làm!" → Gửi dữ liệu lên Server.


document.addEventListener('DOMContentLoaded', function()
{
    // ĐỢI HTML LOAD HẾT CÁC THẺ LÊN
    // 1. Tìm tất cả các thẻ công việc có trên màn hình
    const cards = document.querySelectorAll('.kanban-card');
    // 2. Tìm 3 vùng chứa cột (Cần làm, Đang làm, Đã xong)
    const columns = document.querySelectorAll('.kanban-task-list');
    // 3. Biến tạm để ghi nhớ xem mình đang cầm thẻ Task số mấy
    let draggedTaskId = null;
    console.log("Đã tìm thấy " + cards.length + " thẻ task và " + columns.length + " cột!");

    // TỚI ĐÂY LÀ ĐÃ LOAD XONG

    // GẮN SỰ KIỆN CHO CÁC THẺ
    // Hàm 1: Xử lý khi NGƯỜI DÙNG BẮT ĐẦU NHẤC THẺ LÊN
    function handleDragStart(event) 
    {
        // Lấy chính xác chiếc thẻ đang được nhấc (event.currentTarget)
        const currentCard = event.currentTarget;
        // 1. Cất ID của task vào biến tạm (ví dụ: "1", "2")
        draggedTaskId = currentCard.getAttribute('data-task-id');
        // 2. Làm mờ thẻ đi 50% để tạo hiệu ứng đang bay trên không
        currentCard.classList.add('opacity-50');
        console.log("Đang cầm trên tay Task ID: " + draggedTaskId);
    }

    // Hàm 2: Xử lý khi NGƯỜI DÙNG THẢ TAY RA (hoặc hủy kéo)
    function handleDragEnd(event) 
    {
        const currentCard = event.currentTarget;
        // Trả lại độ nét 100% bình thường cho thẻ
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
        // Lệnh thần chú bắt buộc của trình duyệt: Cho phép thả đồ vào đây
        event.preventDefault();
    }

    // Xử lý khoảnh khắc THẺ RƠI XUỐNG CỘT (Sự kiện drop)
    function handleDrop(event) 
    {
        // Ngăn chặn các hành vi mặc định khác của trình duyệt
        event.preventDefault();
        // 1. Xác định chiếc cột vừa nhận thẻ
        const targetColumn = event.currentTarget;
        // 2. Lấy trạng thái của cột này (Ví dụ: "IN_PROGRESS" hoặc "DONE")
        const newStatus = targetColumn.getAttribute('data-status');
        // 3. Kiểm tra an toàn: Nếu đang cầm task trên tay và có trạng thái cột mới
        if (draggedTaskId != null && newStatus != null) 
        {
            console.log("Thành công: Thả Task ID " + draggedTaskId + " vào cột " + newStatus);
            // 4. Gọi hàm gửi dữ liệu lên Server
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
        
        // 1. Lấy projectId hiện tại từ thanh địa chỉ URL (ví dụ: projectId=1)
        const urlParams = new URLSearchParams(window.location.search);
        const projectId = urlParams.get('projectId') || '1';
        // 2. Tự động tạo một chiếc <form method="POST" action="/teamwork-hub/task">
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = window.location.pathname; // Gửi về chính URL /task hiện tại
        // 3. Tạo các ô ẩn (hidden inputs) chứa thông tin cần gửi
        // Ô 1: action = "updateStatus"
        const inputAction = document.createElement('input');
        inputAction.type = 'hidden';
        inputAction.name = 'action';
        inputAction.value = 'updateStatus';
        // Ô 2: projectId = 1
        const inputProject = document.createElement('input');
        inputProject.type = 'hidden';
        inputProject.name = 'projectId';
        inputProject.value = projectId;
        // Ô 3: taskId = 3
        const inputTask = document.createElement('input');
        inputTask.type = 'hidden';
        inputTask.name = 'taskId';
        inputTask.value = taskId;
        // Ô 4: newStatus = "IN_PROGRESS"
        const inputStatus = document.createElement('input');
        inputStatus.type = 'hidden';
        inputStatus.name = 'newStatus';
        inputStatus.value = newStatus;
        // 4. Nhét 4 ô này vào bên trong form
        form.appendChild(inputAction);
        form.appendChild(inputProject);
        form.appendChild(inputTask);
        form.appendChild(inputStatus);
        // 5. Gắn form vào trang web và BẤM SUBMIT NGAY LẬP TỨC!
        document.body.appendChild(form);
        form.submit();
    }
});

