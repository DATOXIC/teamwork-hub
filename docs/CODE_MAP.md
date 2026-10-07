# BẢN ĐỒ MÃ NGUỒN — "Hỏi gì thì mở file nào"

> Dùng khi bảo vệ: giảng viên hỏi chức năng X → tra mục 1 để biết **URL → Servlet → file xử lý → JSP**.
> Cách mở nhanh trong VS Code: `Ctrl+P` gõ tên file, rồi `Ctrl+F` gõ tên hàm (số dòng sẽ lệch khi code đổi nên bản đồ này
> chỉ ghi **tên hàm**, không ghi số dòng). Cần số dòng chính xác thì chạy `/demo-where <Tên>`.

## 0. Luồng MVC Model 2 (một câu)

`Trình duyệt → URL (/task, /project, …) → Servlet (Controller) → DB class (DAO, JPA) → Entity (Model) → request.setAttribute → forward sang JSP (View) → JSP đọc bằng EL/JSTL`

| Tầng | Thư mục | Vai trò |
|---|---|---|
| Model (Entity) | `src/main/java/com/teamwork/business/` | Class ánh xạ bảng DB (`@Entity`) + getter cho EL |
| DAO | `src/main/java/com/teamwork/data/` | Truy vấn JPA/JPQL (`TaskDB`, `ProjectDB`, …), `JPAUtil` tạo EntityManager |
| Controller | `src/main/java/com/teamwork/controllers/` | Servlet nhận request, kiểm tra quyền, nạp dữ liệu, forward |
| Filter | `src/main/java/com/teamwork/filters/AuthFilter.java` | Chặn mọi URL chưa đăng nhập (trừ /auth, /styles, /js, /images) |
| Util | `src/main/java/com/teamwork/util/` | Mã hóa mật khẩu, OTP, gửi mail, chống brute-force, tính "sức khỏe" Task |
| View | `src/main/webapp/*.jsp`, `includes/` | Giao diện; `js/`, `styles/`, `images/` là tài nguyên tĩnh |
| Cấu hình | `src/main/resources/` + `WEB-INF/web.xml` | `persistence.xml` (JPA), `db.properties` (mật khẩu DB, không commit — mẫu `db.properties.example`; hoặc env `DB_PASSWORD`), `mail.properties` (SMTP) |

## 1. Chức năng → URL → Servlet → JSP

| Chức năng | URL | Servlet | JSP trả về | DAO chính |
|---|---|---|---|---|
| Đăng nhập / đăng ký / đăng xuất | `/auth?action=login\|register\|logout\|viewLogin` | `AuthServlet` | `login.jsp` | `UserDB` |
| Quên mật khẩu (OTP email) | `/auth?action=forgotRequest\|forgotVerify\|forgotReset` | `AuthServlet` + `util/OtpChallenge`, `util/MailUtil` | `forgot-password.jsp` | `UserDB` |
| Trang chủ | `/` | (không qua servlet) | `index.jsp` | — |
| Danh sách dự án, tạo dự án | `/project?action=list\|create\|update` | `ProjectServlet` | `projects.jsp` | `ProjectDB`, `ProjectMemberDB` |
| Báo cáo chất lượng dự án | `/project?action=report` | `ProjectServlet` | `project_report.jsp` | `ProjectDB`, `TaskDB` |
| Lời mời / xin vào dự án / kick / rời | `/invite?action=sendInvite\|requestJoin\|accept\|reject\|revoke\|leave\|kick` | `ProjectInviteServlet` | (redirect) | `ProjectInviteDB`, `ProjectMemberDB` |
| **Bảng công việc (Kanban/List)** | `/task?action=list&projectId=` | `TaskServlet` → `task/TaskBoardHandler` | `tasks.jsp` | `TaskDB`, `SubTaskDB` |
| Timeline / Gantt | `/timeline`, `/task?action=timeline` | `TimelineServlet` | `timeline.jsp` | `TaskDB` |
| Chat dự án + bình luận Task | `/chat?action=view\|sendProjectMessage\|sendTaskComment` | `ChatServlet` | `chat.jsp` | `MessageDB` |
| Tài liệu (Wiki) | `/doc?action=list\|view\|create\|update\|delete` | `DocServlet` | `docs.jsp` | `DocDB`, `TaskDocDB` |
| Bảng vẽ | `/whiteboard` | `WhiteboardServlet` | `whiteboard.jsp` | `WhiteboardDB` |
| Họp trực tuyến | `/meeting` | `MeetingServlet` | `meeting.jsp` | `ProjectDB` |
| Hồ sơ cá nhân | `/profile` | `ProfileServlet` | `profile.jsp` | `UserDB` |
| Thông báo (chuông) | `/notification?action=read\|readAll\|delete` | `NotificationServlet` | (JSON/redirect) | `NotificationDB` |
| Trang lỗi | 404 / 500 | `web.xml` `<error-page>` | `404.jsp`, `500.jsp` | — |

**Dùng chung cho mọi servlet:** `BaseServlet` (lớp cha: `currentUser`, `intParam`, `flash`, `requireMember`, `writeJson`) và `ProjectAccess` (nơi duy nhất trả lời "có phải thành viên / PM / tác giả không"). Đổi luật phân quyền thì sửa `ProjectAccess`.

**Trạng thái:** `business/TaskStatus` và `SubTaskStatus` (enum + bảng chuyển trạng thái hợp lệ; `TaskDB`/`SubTaskDB` từ chối cạnh không có trong bảng). **Transaction nhiều bước:** `JPAUtil.inTransaction` — dùng ở `TaskDB.deleteWithChildren`, `ProjectMemberDB.removeFromProject`, `DocDB.deleteWithLinks`, `ProjectDB.insert` (tạo dự án + OWNER).

## 2. `/task` — servlet lớn nhất, đã chia nhỏ

`TaskServlet.java` **chỉ điều phối** (`doGet`/`doPost` + `switch(action)`); logic nằm trong `controllers/task/`.
Tra **tên `action`** (là giá trị `name="action"` của form / `fetch` trong `tasks.jsp`) ở bảng dưới để biết hàm nào xử lý:

| File | `action` | Hàm (Ctrl+F) | Việc làm |
|---|---|---|---|
| `TaskBoardHandler` | `list` (GET) | `handleShowKanban` | Nạp toàn bộ dữ liệu → forward `tasks.jsp` |
| | `exportCsv` | `handleExportCsv` | Xuất CSV |
| | `delete` (GET) | `handleDeleteTask` | Xóa Task cha |
| `TaskCrudHandler` | `add` | `handleAddTask` | Thêm Task cha (+ đính kèm tài liệu) |
| | `updateStatus` | `handleUpdateTaskStatus` | Kéo-thả đổi cột (AJAX) |
| | `editTask` | `handleEditTask` | Sửa Task |
| | `quickAddParentTask` | `handleQuickAddParentTask` | Thêm nhanh (AJAX) |
| | `createLabel` | `handleCreateLabel` | Tạo nhãn (JSON) |
| `SubTaskHandler` | `addSubTask`, `quickAddSubTask` | `handleAddSubTask`, `handleQuickAddSubTask` | Thêm việc con |
| | `toggleSubTask` | `handleToggleSubTask` | Tick hoàn thành |
| | `editSubTask`, `deleteSubTask` | `handleEditSubTask`, `handleDeleteSubTask` | Sửa / xóa |
| | `submitSubTask` | `handleSubmitSubTask` | Thành viên nộp báo cáo |
| | `approveSubTask` / `reviseSubTask` / `rejectSubTask` | `handleApproveSubTask` / `handleReviseSubTask` / `handleRejectSubTask` | **Cổng 1** — Task Lead duyệt / cân chỉnh / trả về |
| `TaskWorkflowHandler` | `submitParentTask` | `handleSubmitParentTask` | Nộp bàn giao Task cha lên PM |
| | `submitPlanningRequest`, `pmApprovePlanning`, `pmRejectPlanning` | `handleSubmitPlanningRequest`, `handlePmApprovePlanning`, `handlePmRejectPlanning` | Duyệt kế hoạch (Planning) |
| | `pmApproveTask` / `pmReviseTask` / `pmRejectTask` | `handlePmApproveTask` / `handlePmReviseTask` / `handlePmRejectTask` | **Cổng 2** — PM nghiệm thu / cân chỉnh / trả về |
| `TaskAccess` | — | `isProjectOwner`, `isTaskLead`, `canManageSubTask`, … | Phân quyền dùng chung (PM / Task Lead / Assignee); phần PM/thành viên gọi sang `ProjectAccess` |
| `TaskJson` | — | `sendJsonResponse`, `isAjaxRequest` | Trả JSON cho AJAX |

**Quy trình duyệt 2 cổng:** Việc con → (Cổng 1: Task Lead) → Task cha → (Cổng 2: PM). Trạng thái: `TODO → IN_PROGRESS → SUBMITTED → DONE/APPROVED`, nhánh `REVISE`, `REJECTED`.

## 3. Giao diện — JSP, JS, CSS đi cùng nhau

Mọi trang (trừ `login`, `forgot-password`) nhúng khung chung: `includes/header.jsp` (CSS chung + Bootstrap) → `navbar.jsp` → nội dung → `footer.jsp` (Bootstrap JS, `app.js`, `command-palette.js`).
Trang trong dự án còn nhúng `includes/project_subnav.jsp` (thanh Công việc / Timeline / Chat / Tài liệu / Báo cáo).

| JSP | JS riêng | CSS riêng | Ghi chú |
|---|---|---|---|
| `tasks.jsp` | `js/tasks-board.js` (UI), `js/tasks.js` (kéo-thả + lọc) | `base.css` … (nạp tự động qua header), `page-components.css` | Dữ liệu server → JS qua đối tượng `TASK_PAGE` (khối `<script>` ngay trước `tasks-board.js`) |
| `projects.jsp` | `js/projects.js` | `page-components.css` | |
| `project_report.jsp` | `js/project_report.js` | `styles/report.css` | |
| `timeline.jsp` | `js/timeline.js` | `styles/timeline.css` | |
| `chat.jsp` | `js/chat.js` | `styles/chat.css` | |
| `index.jsp` | `js/home.js` | `styles/home.css` | |
| `login.jsp` | `js/login.js` | `styles/login.css` | |
| `forgot-password.jsp` | `js/forgot-password.js` | `styles/login.css` | |
| `whiteboard.jsp` | `js/whiteboard.js` (cấu hình qua `WB_CONFIG`) | `page-components.css` | |
| `meeting.jsp` | `js/meeting.js` (cấu hình qua `MEET_CONFIG`) | `page-components.css` | |
| `docs`, `profile` | (JS nhỏ nhúng trong trang) | `base.css` … (nạp tự động qua header), `page-components.css` | |

Fragment dùng lại: `includes/health_badge.jsp` (huy hiệu sức khỏe Task), `includes/risk_panel.jsp` (khối rủi ro), `includes/toast.jsp` (thông báo `toastSuccess`/`toastError`), `includes/command_palette.jsp` (Ctrl+K).

## 4. Entity → bảng DB → DAO

| Entity (`business/`) | Bảng | DAO (`data/`) |
|---|---|---|
| `User` | `users` | `UserDB` |
| `Project` | `projects` | `ProjectDB` |
| `ProjectMember` (+`ProjectMemberId`) | `project_members` | `ProjectMemberDB` |
| `ProjectInvite` | `project_invites` | `ProjectInviteDB` |
| `Task` | `tasks` | `TaskDB` |
| `SubTask` | `subtasks` | `SubTaskDB` |
| `Label` | `labels` | `LabelDB` |
| `Doc` | `docs` | `DocDB` |
| `TaskDoc` (+`TaskDocId`) | `task_docs` | `TaskDocDB` |
| `Message` | `messages` | `MessageDB` |
| `Notification` | `notifications` | `NotificationDB` |
| `ActivityLog` | `activity_logs` | `ActivityLogDB` |
| `Whiteboard` | `whiteboards` | `WhiteboardDB` |
| `UserWorkload` | (không có bảng — tính khi chạy) | tính trong `TaskBoardHandler.computeUserWorkloads` |

Script tạo bảng: `database/schema_postgres_supabase.sql` (Supabase, đang dùng) và `database/schema_sqlserver.sql` (SSMS). Migration: `database/migrations/`.

## 5. "Tôi muốn sửa X thì sửa ở đâu?"

| Muốn… | Sửa |
|---|---|
| Đổi giao diện một trang | file `.jsp` tương ứng + CSS riêng (mục 3); xem ngay bằng `hot_jsp.bat <file>.jsp` rồi F5 |
| Thêm một action mới cho Task | thêm `case` trong `TaskServlet.doPost` + hàm `handleXxx` trong handler phù hợp (mục 2) |
| Thêm cột/thuộc tính | `business/<Entity>.java` (+getter) → `database/*.sql` → hiển thị trong JSP bằng `${obj.field}` |
| Đổi DB (Supabase ↔ MySQL ↔ SQL Server) | khóa `jpa.unit` trong `db.properties` (mặc định `teamwork-cloud`) — các unit khai báo trong `persistence.xml`; `data/JPAUtil.java` đọc giá trị này |
| Đổi quyền truy cập URL | `filters/AuthFilter.java` |

## 6. CSS dùng chung (đã tách từ `main.css` cũ)

`includes/header.jsp` nạp theo **đúng thứ tự** (CSS sau ghi đè CSS trước — đừng đổi thứ tự):
`login` → `base` (biến màu, typography, navbar, nút) → `kanban` → `task-detail` → `projects` → `docs` → `chat-stream` → `components` (toast, empty state) → `profile` → `landing` (trang chủ) → `labels-toolbar` → `app-layout` → `workspace-shell` (khung ClickUp của tasks.jsp) → `task-drawer` → `metrics` → `workload` → `workspace-typography`,
sau đó `command-palette.css` và `page-components.css`. Tìm một class → `Ctrl+Shift+F` tên class trong thư mục `styles/`.
