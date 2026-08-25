# TEAMWORK HUB — TÀI LIỆU KIẾN TRÚC MẠNG LƯỚI HOÀN CHỈNH
> Phiên bản: 2.0 — Ngày thiết kế lại: 25/08/2026
> Mục đích: Tài liệu tham chiếu nội bộ để AI nhớ lại toàn bộ quyết định kiến trúc và flow triển khai.

---

## 1. VẤN ĐỀ CỦA KIẾN TRÚC CŨ (v1.0)

Kiến trúc cũ (Sprint 1-4 ban đầu) các phân hệ hoạt động **độc lập với nhau**:
- `Project` biết đến `Task` (qua projectId) — nhưng chỉ là con số thống kê cứng (totalTasks, doneTasks).
- `Doc` biết đến `Project` (qua projectId) — nhưng `Task` và `Doc` **không biết nhau**.
- `Chat` chưa có, sẽ là một module đứng một mình.
- Kết quả: Giống như nhiều nền tảng riêng biệt nằm cùng một mái nhà, chứ không phải một hệ sinh thái thật sự.

**Quyết định của người dùng:** Tái kiến trúc toàn bộ theo mô hình **Mạng lưới Quan hệ Chặt chẽ (Interconnected Network)**, nơi mọi thực thể đều có mối liên hệ logic và nghiệp vụ thực sự với nhau.

---

## 2. SƠ ĐỒ QUAN HỆ THỰC THỂ (Entity Relationship Diagram - ERD)

```
                         ┌──────────────────────────────────────┐
                         │              👤 USER                 │
                         │  id, username, password,             │
                         │  fullName, role (ADMIN/MEMBER)       │
                         └──────────────────┬───────────────────┘
                                            │
                    ┌───────────────────────┼───────────────────────┐
                    │ (Sở hữu/Tham gia)     │ (Viết Doc)   (Gửi MSG)│
                    ▼                       ▼                       ▼
   ┌────────────────────────┐  [User tạo Doc]  [User gửi Message]
   │       📁 PROJECT       │ ◄─────────────────────────────────────
   │  id, name, description,│
   │  ownerId, createdAt    │ ◄── TRUNG TÂM CỦA MỌI THỨ
   └────────────┬───────────┘
                │
      ┌─────────┼──────────┬─────────────────┐
      │         │          │                 │
      ▼         ▼          ▼                 ▼
┌──────────┐ ┌──────┐ ┌─────────┐      ┌──────────────┐
│  ✅ TASK │ │📑 DOC│ │💬 MESSAGE│      │  THỐNG KÊ    │
│          │ │      │ │(Chat)    │      │  (tính real- │
│ -id      │ │-id   │ │ -id      │      │  time từ DB) │
│ -projectId│ │-proj.│ │ -projectId│      │  -docCount   │
│ -title   │ │  Id  │ │ -taskId  │      │  -msgCount   │
│ -desc    │ │-title│ │  (0=proj │      │              │
│ -status  │ │-cont.│ │   chat)  │      └──────────────┘
│ -priority│ │-auth.│ │ -authorId│
│ -dueDate │ │  Id  │ │ -authorN.│
│ -assignId│ │-auth.│ │ -content │
│ -assignN.│ │  Name│ │ -sentAt  │
└────┬─────┘ │-creat│ └──────────┘
     │       │  At  │
     │       │-updat│
     │       │  At  │
     │       └──────┘
     │           ▲
     │           │ (QUAN HỆ NHIỀU-NHIỀU)
     ▼           │
┌──────────────────────────────────────┐
│           🔗 TASK_DOC                │
│  (Bảng liên kết Task ↔ Doc)         │
│  taskId  (Khóa ngoại → Task.id)     │
│  docId   (Khóa ngoại → Doc.id)      │
│  docTitle (Lưu sẵn tên doc)         │
│                                      │
│  Ý nghĩa: 1 Task có thể đính kèm   │
│  nhiều Doc hướng dẫn. 1 Doc có thể  │
│  được nhiều Task tham chiếu.         │
└──────────────────────────────────────┘
```

---

## 3. CHI TIẾT TỪNG THỰC THỂ VÀ CÁC QUYẾT ĐỊNH THIẾT KẾ

### 3.1. User (Người dùng) — KHÔNG THAY ĐỔI
```
Thuộc tính: id, username, password, fullName, role
Vai trò trong mạng lưới:
  - Sở hữu Project (Project.ownerId → User.id)
  - Được giao Task (Task.assigneeId → User.id)
  - Viết Doc (Doc.authorId → User.id)
  - Gửi Message (Message.authorId → User.id)
  - Được @mention trong chat (Message.content chứa "@fullName")
```

### 3.2. Project (Dự án) — CẦN CẬP NHẬT
```
Thuộc tính hiện tại: id, name, description, ownerId, createdAt, totalTasks, doneTasks
Thuộc tính CẦN THÊM: (Không thêm vào Model - tính động từ DB)
  - docCount: Đếm từ DocDB.countDocs(projectId) mỗi lần load Dashboard
  - messageCount: Đếm từ MessageDB.countByProject(projectId)

Lý do KHÔNG thêm docCount/messageCount vào Project.java:
  Nếu thêm vào Model thì khi có Doc mới được tạo, ta phải nhớ gọi
  project.setDocCount(project.getDocCount()+1) — dễ bị bỏ sót và gây lỗi không đồng bộ.
  Tốt hơn là ProjectServlet sẽ tính toán realtime khi load Dashboard bằng cách
  gọi trực tiếp DocDB.countDocs(p.getId()) và MessageDB.countByProject(p.getId())
  rồi đặt vào Map để JSP đọc.

Cập nhật projects.jsp: Card mỗi dự án sẽ hiển thị:
  - Tiến độ Task: [===60%===] (đã có)
  - 📑 3 tài liệu
  - 💬 12 thảo luận
```

### 3.3. Task (Công việc) — CẦN CẬP NHẬT NHẸ
```
Thuộc tính hiện tại: id, projectId, title, description, status, priority, dueDate,
                     assigneeId, assigneeName
Thuộc tính CẦN THÊM: KHÔNG CẦN thêm vào Task.java.
  Quan hệ Task-Doc được quản lý hoàn toàn qua TaskDocDB (bảng liên kết).

Cập nhật giao diện tasks.jsp:
  - Mỗi thẻ Kanban Card: Nếu TaskDocDB.selectByTaskId(task.id) trả về list không rỗng
    → Hiển thị badge nhỏ: 📑 2 tài liệu (bấm vào mở modal chi tiết task)
  - Click vào thẻ Task → Mở Modal Chi tiết Task (PHƯƠNG ÁN A đã chọn) gồm:
      • Tiêu đề, Mô tả đầy đủ
      • Trạng thái, Mức ưu tiên, Hạn chót
      • Người phụ trách
      • Danh sách Tài liệu đính kèm (có thể bấm sang đọc)
      • Ô Bình luận: Danh sách comment + Form gửi comment mới

Lưu ý: Khi TẠO Task mới (Modal "+ Thêm công việc"):
  - Thêm multi-select dropdown: "Đính kèm tài liệu hướng dẫn (có thể chọn nhiều)"
  - Liệt kê DocDB.selectByProjectId(projectId) vào <select multiple>
  - Sau khi TaskDB.insert() → gọi TaskDocDB.insert() cho từng docId được chọn
```

### 3.4. Doc (Tài liệu Wiki) — CẦN THÊM PHẦN LIÊN KẾT NGƯỢC
```
Thuộc tính: id, projectId, title, content, authorId, authorName, createdAt, updatedAt
KHÔNG thêm thuộc tính. Nhưng CẦN cập nhật giao diện docs.jsp:

Ở cuối mỗi bài viết, thêm khung "Công việc tham chiếu tài liệu này":
  Gọi TaskDocDB.selectByDocId(docId) → Lấy danh sách Task ID → TaskDB.selectById() từng cái
  Hiển thị mini-list:
    ┌─────────────────────────────────┐
    │ 📌 Công việc đang dùng tài liệu này │
    │ ✅ [DONE] Thiết kế Dockerfile  │
    │ 🔄 [IN_PROGRESS] Deploy Render │
    └─────────────────────────────────┘
  Mỗi dòng là link bấm được → mở bảng Kanban và highlight task đó
```

### 3.5. TaskDoc (Bảng liên kết Task ↔ Doc) — THỰC THỂ HOÀN TOÀN MỚI
```
Đây là cốt lõi của quan hệ NHIỀU-NHIỀU giữa Task và Doc.

Thuộc tính:
  - taskId (int): Trỏ đến Task.id
  - docId (int): Trỏ đến Doc.id
  - docTitle (String): Lưu sẵn tên Doc để hiển thị nhanh, không cần join

File cần tạo:
  - com.teamwork.business.TaskDoc.java
  - com.teamwork.data.TaskDocDB.java

TaskDocDB.java cần các hàm:
  - selectByTaskId(int taskId) → List<TaskDoc>: Lấy tất cả Doc đính kèm của 1 Task
  - selectByDocId(int docId) → List<Integer> taskIds: Lấy tất cả Task tham chiếu 1 Doc
  - insert(int taskId, int docId, String docTitle) → boolean
  - deleteByTaskId(int taskId): Xóa tất cả liên kết khi xóa Task
  - deleteByDocId(int docId): Xóa tất cả liên kết khi xóa Doc

Seed Data TaskDocDB (Đồng bộ với TaskDB và DocDB hiện có):
  - Task 1 (Thiết kế CSDL) ← đính kèm → Doc 1 (Quy chuẩn MVC)
  - Task 3 (Deploy Render) ← đính kèm → Doc 3 (Hướng dẫn Docker)
  - Task 5 (Thiết kế UI)   ← đính kèm → Doc 1 (Quy chuẩn MVC)
```

### 3.6. Message (Tin nhắn Chat) — THỰC THỂ HOÀN TOÀN MỚI
```
Đây là thực thể đặc biệt nhất vì nó phục vụ HAI mục đích cùng lúc (Phương án C đã chọn):
  A. Kênh Chat chung của Dự án (taskId = 0)
  B. Luồng Bình luận của một Task cụ thể (taskId != 0)

Thuộc tính:
  - id (int): Khóa chính
  - projectId (int): Thuộc dự án nào (LUÔN CÓ)
  - taskId (int): Bình luận của Task nào. = 0 nghĩa là tin nhắn chat chung dự án
  - authorId (int): Người gửi (→ User.id)
  - authorName (String): Tên hiển thị (lưu sẵn)
  - content (String): Nội dung tin nhắn (có thể chứa #task-3, #doc-2, @NguyenVanAn)
  - sentAt (String): Thời gian gửi (dd/MM/yyyy HH:mm)

Quy tắc phân biệt loại tin nhắn:
  - taskId == 0  → Tin nhắn CHAT CHUNG của dự án → hiển thị ở chat.jsp
  - taskId != 0  → Bình luận của Task cụ thể → hiển thị trong Modal Chi tiết Task

File cần tạo:
  - com.teamwork.business.Message.java
  - com.teamwork.data.MessageDB.java
  - com.teamwork.controllers.ChatServlet.java (/chat)
  - src/main/webapp/chat.jsp
  - src/main/webapp/js/chat.js

MessageDB.java cần các hàm:
  - selectByProject(int projectId) → List<Message>: Lấy chat chung (taskId=0)
  - selectByTask(int taskId) → List<Message>: Lấy comment của 1 task
  - insert(Message msg) → int: Lưu tin nhắn mới
  - countByProject(int projectId) → int: Đếm cho Dashboard card
  - delete(int id) → boolean

Seed Data:
  Project 1 chat (taskId=0):
    - Admin: "Mọi người hãy xem quy chuẩn code tại #doc-1 trước khi bắt đầu Sprint!"
    - NguyenVanAn: "Đã đọc rồi ạ. @Trưởng Nhóm Admin task #task-3 em đang làm xong hôm nay."
  Task 3 comments (taskId=3):
    - Admin: "Nhớ đọc #doc-3 trước khi triển khai nhé."
```

---

## 4. TÍNH NĂNG #MENTION VÀ @MENTION TRONG CHAT

### 4.1. Cách hoạt động (Client-side Rendering)
```
Người dùng gõ tin nhắn: "Hãy xem #doc-1 và làm theo task #task-3 nhé @NguyenVanAn"
                                   ↓
                         Lưu vào MessageDB nguyên văn (plain text)
                                   ↓
              chat.js / JavaScript phía trình duyệt xử lý khi hiển thị:
                                   ↓
  - Tìm pattern "#doc-{id}" → Thay bằng <a href="/doc?action=view&docId={id}">📑 #doc-1</a>
  - Tìm pattern "#task-{id}" → Thay bằng <a href="/task?action=list&taskId={id}">✅ #task-3</a>
  - Tìm pattern "@{tên}" → Thay bằng <span class="mention-user">@NguyenVanAn</span> (highlight xanh)
```

### 4.2. File chat.js — Hàm renderMentions(content)
```javascript
// Hàm này nhận nội dung tin nhắn thô và trả về HTML có link bấm được
function renderMentions(rawContent, projectId)
{
    let result = rawContent;

    // Bước 1: Thay thế #doc-{id} thành link sang tài liệu
    result = result.replace(/#doc-(\d+)/g, function(match, docId)
    {
        let url = contextPath + "/doc?action=view&projectId=" + projectId + "&docId=" + docId;
        return '<a href="' + url + '" class="mention-doc">📑 #doc-' + docId + '</a>';
    });

    // Bước 2: Thay thế #task-{id} thành link sang bảng Kanban highlight task
    result = result.replace(/#task-(\d+)/g, function(match, taskId)
    {
        let url = contextPath + "/task?action=list&projectId=" + projectId + "&highlight=" + taskId;
        return '<a href="' + url + '" class="mention-task">✅ #task-' + taskId + '</a>';
    });

    // Bước 3: Thay thế @tên thành span highlight người dùng
    result = result.replace(/@(\S+)/g, function(match, name)
    {
        return '<span class="mention-user fw-bold text-primary">@' + name + '</span>';
    });

    return result;
}
```

---

## 5. LUỒNG DỮ LIỆU ĐẦY ĐỦ CỦA TỪNG USE CASE SAU TÁI KIẾN TRÚC

### UC-A: Người dùng tạo Task mới và đính kèm Tài liệu hướng dẫn
```
[1] User bấm "+ Thêm công việc" trên tasks.jsp
      ↓
[2] Modal mở ra — TaskServlet đã đổ sẵn List<Doc> của dự án vào request attribute "docList"
      ↓
[3] User điền tiêu đề, mô tả, priority, dueDate
    User chọn 1 hoặc nhiều Doc từ dropdown <select multiple name="docIds">
      ↓
[4] Form POST → /task?action=add
      ↓
[5] TaskServlet.handleAddTask():
    a. Tạo Task mới → TaskDB.insert(newTask) → nhận được newTaskId
    b. Đọc mảng docIds[] từ request.getParameterValues("docIds")
    c. Với mỗi docId: TaskDocDB.insert(newTaskId, docId, doc.getTitle())
    d. sendRedirect về /task?action=list&projectId=...
```

### UC-B: Người dùng bấm vào thẻ Task để xem chi tiết và bình luận
```
[1] User bấm vào thẻ Kanban Card (tasks.jsp)
      ↓
[2] JavaScript bắt sự kiện click → Gọi AJAX GET /task?action=detail&taskId=3
      hoặc: Data đã được đổ sẵn vào data attribute của card, Modal đọc từ data attribute
      (Chọn cách đơn giản hơn: Pre-render modal trong JSP với JSTL)
      ↓
[3] tasks.jsp đã render sẵn Modal cho TỪNG task trong vòng lặp c:forEach
    Modal chứa:
      - Thông tin task (title, desc, priority, status, dueDate, assignee)
      - List tài liệu đính kèm: TaskDocDB.selectByTaskId(task.id)
        → Mỗi doc là link bấm sang docs.jsp
      - Danh sách bình luận: MessageDB.selectByTask(task.id)
        → Hiển thị với renderMentions()
      - Form gửi bình luận mới: POST /chat?action=comment&taskId=...&projectId=...
      ↓
[4] Sau khi submit comment: ChatServlet nhận, MessageDB.insert(), sendRedirect
```

### UC-C: Người dùng đọc Doc và thấy các Task tham chiếu tài liệu này
```
[1] User đang đọc Doc 3 "Hướng dẫn Docker & Render" trên docs.jsp
      ↓
[2] DocServlet.handleShowDocs() gọi thêm:
    List<Integer> relatedTaskIds = TaskDocDB.selectTaskIdsByDocId(selectedDoc.getId())
    List<Task> relatedTasks = new ArrayList<>()
    for (int taskId : relatedTaskIds):
        Task t = TaskDB.selectById(taskId)
        if t != null: relatedTasks.add(t)
    request.setAttribute("relatedTasks", relatedTasks)
      ↓
[3] docs.jsp hiển thị khung ở cuối bài viết:
    "📌 Các công việc đang dùng tài liệu này"
    - [DONE] Thiết kế Dockerfile (link → /task?action=list&projectId=1)
    - [IN_PROGRESS] Deploy lên Render (link → /task?action=list&projectId=1)
```

### UC-D: Người dùng Chat và sử dụng #mention
```
[1] User gõ: "Mọi người xem #doc-1 và cập nhật #task-3 nhé @NguyenVanAn"
    User bấm Gửi
      ↓
[2] Form POST /chat?action=sendProjectMessage&projectId=1
      ↓
[3] ChatServlet:
    - Đọc content từ form
    - Lấy authorId, authorName từ Session
    - Tạo Message: (id, projectId=1, taskId=0, authorId, authorName, content, now)
    - MessageDB.insert(message)
    - sendRedirect /chat?action=view&projectId=1
      ↓
[4] chat.jsp hiển thị tin nhắn, JavaScript gọi renderMentions(content, projectId):
    "Mọi người xem 📑 #doc-1 và cập nhật ✅ #task-3 nhé @NguyenVanAn" (đã là HTML link)
```

### UC-E: Dashboard hiển thị thống kê đầy đủ từ mạng lưới
```
[1] User vào /project?action=list
      ↓
[2] ProjectServlet.handleListProjects():
    List<Project> projects = ProjectDB.selectAll()

    Tạo Map để lưu số liệu thống kê:
    Map<Integer, Integer> docCountMap = new HashMap<>()
    Map<Integer, Integer> msgCountMap = new HashMap<>()

    for (Project p : projects):
        docCountMap.put(p.getId(), DocDB.countDocs(p.getId()))
        msgCountMap.put(p.getId(), MessageDB.countByProject(p.getId()))

    request.setAttribute("projects", projects)
    request.setAttribute("docCountMap", docCountMap)
    request.setAttribute("msgCountMap", msgCountMap)
      ↓
[3] projects.jsp mỗi card hiển thị:
    - Tên dự án, Mô tả
    - Thanh tiến độ: ${p.progressPercentage}%
    - 📑 ${docCountMap[p.id]} tài liệu
    - 💬 ${msgCountMap[p.id]} thảo luận
```

---

## 6. KẾ HOẠCH TRIỂN KHAI (PHASED IMPLEMENTATION PLAN)

### GIAI ĐOẠN 1: Cơ sở hạ tầng Quan hệ Task ↔ Doc (CÁC FILE CẦN TẠO/SỬA)

#### [NEW] com.teamwork.business.TaskDoc.java
```
- Thuộc tính: taskId (int), docId (int), docTitle (String)
- Constructor đầy đủ tham số + mặc định + Getters/Setters
```

#### [NEW] com.teamwork.data.TaskDocDB.java
```
- Seed Data: 3 liên kết mẫu đồng bộ với TaskDB và DocDB
- Hàm selectByTaskId(int taskId) → List<TaskDoc>
- Hàm selectTaskIdsByDocId(int docId) → List<Integer>
- Hàm insert(int taskId, int docId, String docTitle)
- Hàm deleteByTaskId(int taskId)
- Hàm deleteByDocId(int docId)
```

#### [MODIFY] src/main/webapp/tasks.jsp
```
- TaskServlet phải đổ thêm: docList (List<Doc> của project) vào request attribute
- Trong form "+ Thêm công việc": Thêm <select multiple name="docIds">
- Mỗi thẻ Kanban Card: Thêm badge 📑 nếu có doc đính kèm
- Pre-render Modal Chi tiết Task cho TỪNG task trong c:forEach
  Modal chứa: thông tin task + list doc đính kèm + list bình luận (từ MessageDB)
```

#### [MODIFY] com.teamwork.controllers.TaskServlet.java
```
- doGet action="list": Thêm đổ docList vào request attribute
- doPost action="add": Sau insert task, xử lý docIds[] và gọi TaskDocDB.insert()
- doGet action="delete": Trước khi xóa task, gọi TaskDocDB.deleteByTaskId()
```

#### [MODIFY] src/main/webapp/docs.jsp
```
- DocServlet phải đổ thêm: relatedTasks (List<Task>) vào request attribute
- Ở cuối bài viết: Render khung "Công việc tham chiếu tài liệu này"
```

#### [MODIFY] com.teamwork.controllers.DocServlet.java
```
- handleShowDocs(): Gọi TaskDocDB.selectTaskIdsByDocId() rồi map sang Task objects
- handleDeleteDoc(): Gọi TaskDocDB.deleteByDocId(docId) trước khi xóa
```

---

### GIAI ĐOẠN 2: Hệ thống Chat & Bình luận (Sprint 5)

#### [NEW] com.teamwork.business.Message.java
```
- Thuộc tính: id, projectId, taskId, authorId, authorName, content, sentAt
- taskId = 0 → Chat chung dự án; taskId != 0 → Comment của task
```

#### [NEW] com.teamwork.data.MessageDB.java
```
- Seed Data: 2 tin nhắn chat chung + 1 comment task mẫu (có dùng #mention)
- Hàm selectByProject(int projectId) → List<Message> (taskId=0)
- Hàm selectByTask(int taskId) → List<Message> (taskId!=0)
- Hàm insert(Message msg) → int
- Hàm countByProject(int projectId) → int
- Hàm delete(int id) → boolean
```

#### [NEW] com.teamwork.controllers.ChatServlet.java (/chat)
```
doGet:
  - action="view": Lấy chat chung của dự án → forward chat.jsp
doPost:
  - action="sendProjectMessage": Lưu tin nhắn chat chung (taskId=0)
  - action="sendTaskComment": Lưu bình luận task (taskId != 0)
  - Cả 2 đều: Lấy User từ Session, tạo Message, MessageDB.insert(), sendRedirect
```

#### [NEW] src/main/webapp/chat.jsp
```
- Layout 2 cột:
  • Cột trái: Danh sách kênh = [Kênh Chung Dự Án] (chỉ 1 kênh theo dự án)
  • Cột phải: Dòng thời gian tin nhắn (cuộn từ trên xuống, tin mới nhất ở dưới)
    Mỗi tin nhắn: Avatar (chữ cái đầu), Tên, Thời gian, Nội dung (đã render mention)
  • Form gửi tin nhắn ở dưới cùng
- Gợi ý #mention hiện ra khi gõ "#" hoặc "@"
- Tự động cuộn xuống tin mới nhất khi tải trang
```

#### [NEW] src/main/webapp/js/chat.js
```
- Hàm renderMentions(content, projectId): Xử lý #doc-X, #task-X, @name → HTML links
- Hàm scrollToBottom(): Tự động cuộn xuống dòng cuối khi tải chat
- Hàm suggestMentions(): Khi gõ "#" → hiện dropdown gợi ý task/doc hiện có
```

#### [MODIFY] src/main/webapp/WEB-INF/web.xml
```
- Thêm: ChatServlet mapping /chat
```

---

### GIAI ĐOẠN 3: Nâng cấp Dashboard (Project Card)

#### [MODIFY] com.teamwork.controllers.ProjectServlet.java
```
- action="list": Tính thêm docCountMap, msgCountMap rồi đổ vào request attribute
```

#### [MODIFY] src/main/webapp/projects.jsp
```
- Mỗi Project Card thêm:
  📑 ${docCountMap[p.id]} tài liệu | 💬 ${msgCountMap[p.id]} thảo luận
```

#### [MODIFY] tasks.jsp và docs.jsp
```
- Thêm nút Tab thứ 3: [Kanban] [Tài liệu] [💬 Chat] trong thanh điều hướng
```

---

## 7. BẢNG KIỂM SOÁT FILE — TOÀN BỘ HỆ THỐNG SAU KHI HOÀN THÀNH

| File | Trạng thái | Mô tả |
|---|---|---|
| `User.java` | ✅ Giữ nguyên | Không cần thay đổi |
| `Project.java` | ✅ Giữ nguyên | Thống kê tính động ở Servlet |
| `Task.java` | ✅ Giữ nguyên | Quan hệ Doc qua TaskDocDB |
| `Doc.java` | ✅ Giữ nguyên | Quan hệ Task qua TaskDocDB |
| `TaskDoc.java` | 🆕 TẠO MỚI | Thực thể liên kết Task-Doc |
| `Message.java` | 🆕 TẠO MỚI | Tin nhắn chat + comment task |
| `UserDB.java` | ✅ Giữ nguyên | Không cần thay đổi |
| `ProjectDB.java` | ✅ Giữ nguyên | Không cần thay đổi |
| `TaskDB.java` | ✅ Giữ nguyên | Không cần thay đổi |
| `DocDB.java` | ✅ Giữ nguyên | Đã có đủ hàm cần thiết |
| `TaskDocDB.java` | 🆕 TẠO MỚI | Quản lý quan hệ Task-Doc |
| `MessageDB.java` | 🆕 TẠO MỚI | Quản lý chat + comment |
| `AuthFilter.java` | ✅ Giữ nguyên | Không cần thay đổi |
| `AuthServlet.java` | ✅ Giữ nguyên | Không cần thay đổi |
| `ProjectServlet.java` | 🔧 CẬP NHẬT | Thêm tính docCount, msgCount |
| `TaskServlet.java` | 🔧 CẬP NHẬT | Xử lý docIds khi add/delete task |
| `DocServlet.java` | 🔧 CẬP NHẬT | Thêm relatedTasks khi view doc |
| `ChatServlet.java` | 🆕 TẠO MỚI | Controller chat chung + comment |
| `web.xml` | 🔧 CẬP NHẬT | Thêm ChatServlet /chat |
| `login.jsp` | ✅ Giữ nguyên | Không cần thay đổi |
| `projects.jsp` | 🔧 CẬP NHẬT | Thêm docCount, msgCount trên card |
| `tasks.jsp` | 🔧 CẬP NHẬT | Badge doc, Modal chi tiết, multi-select |
| `docs.jsp` | 🔧 CẬP NHẬT | Thêm relatedTasks ở cuối bài |
| `chat.jsp` | 🆕 TẠO MỚI | Giao diện chat dự án |
| `tasks.js` | ✅ Giữ nguyên | Drag & Drop (không cần thay đổi) |
| `chat.js` | 🆕 TẠO MỚI | renderMentions, scrollToBottom |

---

## 8. CÁC ĐIỂM KỸ THUẬT QUAN TRỌNG CẦN GHI NHỚ

### 8.1. Quy tắc Code Style của User (BẮT BUỘC)
```
- LUÔN dùng vòng lặp for...of hoặc for(Type item : list) thay vì forEach()
- LUÔN dùng hàm có tên rõ ràng (named function) thay vì anonymous function
- Viết code dài hơn nhưng dễ hiểu hơn, tường minh hơn
- Comment tiếng Việt giải thích từng khối code
- TUYỆT ĐỐI KHÔNG dùng Java Scriptlet <% %> trong JSP
- Dùng JSTL <c:...> và EL ${...} cho mọi logic trong JSP
```

### 8.2. Quy tắc Routing PRG Pattern (BẮT BUỘC)
```
- Mọi thao tác POST (tạo/sửa/xóa) sau khi xử lý PHẢI sendRedirect về GET
- Lý do: Tránh người dùng bấm F5 gửi lại form → tạo dữ liệu trùng lặp
```

### 8.3. Quy tắc JSP về c:choose (ĐÃ BỊ LỖI 1 LẦN)
```
- KHÔNG BAO GIỜ đặt comment HTML <!-- --> hoặc text/khoảng trắng
  trực tiếp bên trong <c:choose>
- Chỉ được có <c:when> và <c:otherwise> là con trực tiếp của <c:choose>
- Comment được phép trong <c:when> và <c:otherwise>
```

### 8.4. Quy tắc Đặt Tên File (Đã thống nhất)
```
AuthServlet   ↔ login.jsp
ProjectServlet ↔ projects.jsp
TaskServlet   ↔ tasks.jsp + tasks.js
DocServlet    ↔ docs.jsp
ChatServlet   ↔ chat.jsp + chat.js
```

### 8.5. AuthFilter Whitelist (Cần cập nhật nếu thêm /chat)
```
Hiện tại /chat chưa có trong protected paths.
AuthFilter tự động bảo vệ /chat vì nó không nằm trong whitelist.
Không cần thay đổi AuthFilter.java.
```

### 8.6. Deployment
```
- Local: C:\apache-tomcat-10.1\apache-tomcat-10.1.57
- JDK: C:\Users\To Phuong Dat\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.8-hotspot
- Cloud: https://teamwork-hub-lnn4.onrender.com (Render.com Docker)
- GitHub: https://github.com/DATOXIC/teamwork-hub
- Compile command: javac -encoding UTF-8 -cp "tomcat/lib/jakarta.servlet-api.jar;..."
```

---

## 9. THỨ TỰ TRIỂN KHAI (EXECUTION ORDER)

```
Bước 1:  Tạo TaskDoc.java (Model)
Bước 2:  Tạo TaskDocDB.java (Data + Seed Data)
Bước 3:  Cập nhật TaskServlet.java (xử lý docIds khi add task)
Bước 4:  Cập nhật tasks.jsp (multi-select, badge, modal chi tiết task)
Bước 5:  Cập nhật DocServlet.java (thêm relatedTasks)
Bước 6:  Cập nhật docs.jsp (thêm khung công việc liên quan)
--- Giai đoạn 1 hoàn thành, test và commit ---
Bước 7:  Tạo Message.java (Model)
Bước 8:  Tạo MessageDB.java (Data + Seed Data có #mention)
Bước 9:  Tạo ChatServlet.java (Controller)
Bước 10: Cập nhật web.xml (thêm /chat)
Bước 11: Tạo chat.jsp (View 2 cột)
Bước 12: Tạo chat.js (renderMentions, scrollToBottom)
--- Giai đoạn 2 hoàn thành, test và commit ---
Bước 13: Cập nhật ProjectServlet.java (tính docCount, msgCount)
Bước 14: Cập nhật projects.jsp (hiển thị thêm thống kê)
Bước 15: Cập nhật tasks.jsp và docs.jsp (thêm nút Tab Chat)
--- Giai đoạn 3 hoàn thành, test cuối cùng và push lên GitHub/Render ---
```

---

*Tài liệu này được tạo lúc 25/08/2026 20:36 và phản ánh đầy đủ tầm nhìn kiến trúc v2.0 đã được người dùng xác nhận.*
