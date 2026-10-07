# PLAN — Khảo sát & kế hoạch cải thiện Teamwork Hub

> Ngày khảo sát: 07/10/2026 · Commit: `253b6bf` · Phạm vi: toàn bộ repo (chưa sửa code)
>
> **Ràng buộc đã xác nhận với chủ dự án**
> - Đây là **đồ án môn học**: giữ Java Servlet/JSP thuần + JPA, không chuyển sang Spring/React.
> - Repo GitHub **public**: mọi bí mật đã commit coi như **đã lộ**.
> - Trang dự án **chỉ hiện dự án mình tham gia** (ẩn "Dự án khác").
> - Trong dự án nhóm, **chỉ Trưởng dự án (PM) được tạo và giao task**; Task Lead chỉ phân rã subtask.

---

## 0. Tóm tắt — 5 vấn đề cần xử lý ngay (góc nhìn đồ án cuối kỳ)

Tiêu chí chọn: vấn đề nào có thể **làm hỏng buổi demo**, **bị giảng viên hỏi/trừ điểm khi chấm môn Lập trình Web** (HTTP, Servlet/JSP, MVC, session, bảo mật cơ bản), hoặc **làm đặc tả của nhóm và sản phẩm mâu thuẫn**. Các vấn đề chỉ quan trọng khi có người dùng thật (chịu tải, nhiều dữ liệu, vận hành lâu dài) được để xuống sau.

| # | Vấn đề | Vì sao quan trọng với đồ án | Vị trí | Công sức |
|---|---|---|---|---|
| 1 | **Mật khẩu DB Supabase nằm trong repo public** | Bất kỳ ai cũng có thể xóa hoặc sửa dữ liệu demo ngay trước buổi bảo vệ. Giảng viên đọc repo thấy mật khẩu viết cứng thì gần như chắc chắn trừ điểm. | `db.properties:10`, `persistence.xml:33` (lịch sử từ `fbb5d0c`) | S — đổi mật khẩu, đọc từ biến môi trường (task 1.1, 1.2) |
| 2 | **Không escape dữ liệu khi in ra JSP (XSS)** — ~1.000 chỗ `${...}` không qua `<c:out>`; `chat.js:77` gán tin nhắn vào `innerHTML` | Đây đúng là kiến thức JSP/JSTL của môn học, giảng viên rất hay thử: gõ `<b>abc</b>` hoặc `<script>` vào tên task hay tin nhắn là giao diện vỡ ngay trước mặt hội đồng. | `tasks.jsp:656,770,1078…`, `chat.js:77,444`, `projects.jsp`, `profile.jsp`, `navbar.jsp` | M–L — ưu tiên các màn hình demo trước (task 1.3, 1.4) |
| 3 | **Luồng chính lỗi hoặc dở dang khi demo** — mời lại người từng từ chối/bị kick thì thất bại nhưng vẫn báo "thành công"; "Nộp bàn giao kèm tệp" chỉ nhập tên tệp, bấm tải về thì 404 | Lỗi xuất hiện đúng lúc trình diễn tính năng mời thành viên và quy trình nghiệm thu, vốn là điểm nhấn của đề tài. | `schema_postgres_supabase.sql:212`, `ProjectInviteServlet:217,304`, `tasks.jsp:2591-2594` | S cho lỗi mời lại (task 1.14); M nếu làm upload thật (task 4.1), hoặc ẩn nút tải khi chưa có tệp |
| 4 | **Phân quyền chạy khác với đặc tả của nhóm** — thành viên tạo và giao task cho người khác; Task Lead tự tắt bước duyệt của PM; mọi thành viên đổi được hạn chót; PM duyệt được task chưa nộp; tự đặt `ADMIN` trong hồ sơ | Hội đồng thường hỏi "thành viên thường có làm được X không?". Nếu demo cho thấy làm được trong khi tài liệu nói không, nhóm mất điểm phần phân tích thiết kế. | `TaskCrudHandler:75,172,477-481,612`, `TimelineServlet:248`, `TaskWorkflowHandler:376,443,493`, `ProfileServlet:243` | M (task 1.7, 1.8, 1.9) |
| 5 | **Dùng GET để thay đổi dữ liệu, không có CSRF token** — xóa task, chấp nhận/từ chối lời mời, xóa tin nhắn/tài liệu/thông báo, đăng xuất đều chạy qua link GET | Ngữ nghĩa GET/POST và mô hình Request/Response là trọng tâm môn (TECH_SPEC mục 1.1). Đây là câu hỏi dễ gặp khi bảo vệ, và chuyển sang POST cũng không tốn nhiều công. | `TaskServlet:192`, `ProjectInviteServlet:67-75`, `ChatServlet:115`, `DocServlet:116`, `NotificationServlet:61,71-73` | M (task 1.5, 1.6) |

**Nên chuẩn bị câu trả lời, sửa nếu còn thời gian:** băm mật khẩu SHA-256 với salt chung (`PasswordUtil:16`) nên đổi sang PBKDF2 có sẵn trong JDK (task 1.10). File `tasks.jsp` dài 4.238 dòng và code lặp ở các servlet có thể bị hỏi về tổ chức MVC (task 2.1, 2.10). Chưa có test cho phân quyền (task 2.11).

**Có thể để sau buổi bảo vệ:** tối ưu N+1 và polling (dữ liệu demo nhỏ nên chưa thấy chậm), khóa lạc quan khi nhiều người cùng sửa, WebSocket, CI/CD, thống nhất hai cách build.

---

## 1. Tổng quan dự án

### 1.1 Tech stack & kiến trúc

| Hạng mục | Hiện trạng |
|---|---|
| Ngôn ngữ / runtime | Java 21, Tomcat 10.1, Jakarta Servlet 6 / JSP 3.1 / JSTL 3 |
| Kiến trúc | MVC Model 2: `business/` (Entity) → `data/` (DAO tĩnh, JPA/Hibernate 6.5) → `controllers/` (Servlet, `/task` tách 5 handler) → JSP |
| CSDL | Supabase PostgreSQL (mặc định), có persistence unit cho MySQL & SQL Server; schema bằng SQL tay + 2 migration |
| Routing | `@WebServlet` theo URL + tham số `action` (riêng `/timeline` khai báo trong `web.xml`) |
| Xác thực | Session + `AuthFilter` (whitelist), cookie remember-me ký HMAC, OTP quên mật khẩu qua Gmail SMTP |
| Phân quyền | Không có lớp tập trung; mỗi handler tự gọi `isMember / isProjectOwner / isTaskLead` |
| Frontend | JSP render server, Bootstrap 5.3 + Bootstrap Icons (CDN), 13 file JS thuần, 23 file CSS; Excalidraw (React UMD) cho whiteboard, Jitsi cho meeting |
| State phía client | Cookie (`last_project_id`, `preferred_task_view`…), localStorage (nháp chat), reload toàn trang sau thao tác |
| Build | 2 đường: `build_and_run.bat` (javac + 29 jar trong `WEB-INF/lib`) và Maven/Dockerfile |
| Test | 17 unit test (chỉ `util/`), E2E tĩnh bằng Node kiểm tra cú pháp JSP |

### 1.2 Kết quả build / lint / test

| Lệnh | Kết quả |
|---|---|
| `mvn clean package` | ✅ Thành công |
| `mvn test` | ✅ 17/17 pass (`LoginAttemptLimiterTest`, `OtpChallengeTest`, `PasswordUtilTest`) — **không có test nào cho phân quyền hay nghiệp vụ** |
| `javac -Xlint:all` | 27 cảnh báo `[serial]` (thiếu `serialVersionUID`), không có lỗi |
| E2E `node tests/e2e/runner.js` | ⚠️ Không chạy được: máy chưa cài Node |
| Linter JS/CSS, type-check | Không có cấu hình |

### 1.3 Tính năng & trạng thái

| Tính năng | Trạng thái | Ghi chú |
|---|---|---|
| Đăng ký / đăng nhập / ghi nhớ / đăng xuất | Hoàn thiện | Có giới hạn đăng nhập sai, đổi session ID; hash yếu (xem mục 0.5) |
| Quên mật khẩu bằng OTP email | Hoàn thiện | Không giới hạn theo IP → có thể spam email |
| Dự án SOLO/TEAM: tạo, sửa | Hoàn thiện | **Chưa có xóa/lưu trữ dự án** |
| Mời / xin gia nhập / rời / kick (tối đa 10 người) | **Lỗi** | Mời lại người từng từ chối/bị kick thất bại im lặng (mục 2.1) |
| Task: list view, board kéo thả, nhãn, ưu tiên, hạn chót | Hoàn thiện | Lỗ hổng phân quyền (mục 0.4) |
| Subtask + quy trình 2 cổng (duyệt kế hoạch → nghiệm thu) | Hoàn thiện, **logic còn hở** | Thiếu kiểm tra trạng thái ở bước duyệt |
| Nộp bàn giao kèm tệp | **Dở dang** | Chỉ nhập *tên* tệp, không có upload; link tới `uploads/deliverables/<tên>` thường bị 404 |
| Chat dự án + bình luận task | Hoạt động, **lỗi XSS** | Polling 4 giây; `@mention` chỉ tô màu, không gửi thông báo |
| Wiki / tài liệu | Hoàn thiện | Văn bản thuần, không có lịch sử phiên bản, sửa đồng thời thì người sau ghi đè |
| Thông báo trong app | Hoàn thiện | Chỉ cập nhật khi tải lại trang |
| Timeline / Gantt | Hoạt động | Mọi thành viên đổi được hạn chót |
| Báo cáo dự án, sức khỏe dự án, workload | Hoàn thiện | |
| Hồ sơ cá nhân | Hoạt động | Rất chậm (quét toàn bộ task trong hệ thống) |
| Whiteboard (Excalidraw) | Hoạt động | Ghi đè cả bảng (last-write-wins), body tối đa 8 MB mỗi lần lưu |
| Họp video (Jitsi) | Hoạt động | Tên phòng đoán được, phòng Jitsi public |
| Command palette, xuất CSV | Hoàn thiện | CSV có thể bị chèn công thức Excel |

---

## 2. Danh sách vấn đề

Mức độ: **NT** = Nghiêm trọng · **C** = Cao · **TB** = Trung bình · **T** = Thấp

### 2.1 Lỗi chức năng

| ID | File:dòng | Mô tả | Mức |
|---|---|---|---|
| F1 | `database/schema_postgres_supabase.sql:212` + `ProjectInviteServlet:217,304` | Ràng buộc `UNIQUE(project_id, sender_id, receiver_id, type)` không tính đến `status` → không mời lại được người từng từ chối, hết hạn hoặc bị kick. Servlet không kiểm tra giá trị trả về `0` nên vẫn báo "thành công" và vẫn gửi thông báo. | C |
| F2 | `TaskWorkflowHandler:376,443,493` | `pmApproveTask` / `pmReviseTask` / `pmRejectTask` không kiểm tra trạng thái `SUBMITTED` → PM duyệt task TODO chưa có subtask, hoặc từ chối task đã DONE. | TB |
| F3 | `SubTaskHandler:538,617,668` | Duyệt/trả/từ chối subtask không kiểm tra trạng thái `SUBMITTED`. | TB |
| F4 | `TaskCrudHandler:154,448`, `SubTaskHandler:80` | `dueDate` lưu nguyên chuỗi, không validate (chỉ Timeline có parse) → chuỗi sai làm hỏng `isOverdue`, Gantt và báo cáo. | TB |
| F5 | Toàn bộ `business/*` | Ngày giờ lưu dạng chuỗi `dd/MM/yyyy HH:mm` → sắp xếp sai (`ProjectMemberDB:48 ORDER BY pm.joinedAt`), không tính được khoảng thời gian, lệch múi giờ. | TB |
| F6 | `TaskBoardHandler:375-384`, `ProjectInviteServlet:516-520,579-583`, `ProjectServlet:278-289` | Thao tác nhiều bước (xóa task + subtask + comment + doc link; kick + bỏ giao việc; tạo dự án + thêm owner) mỗi bước một transaction riêng → hỏng giữa chừng thì dữ liệu nửa vời. | TB |
| F7 | `ProjectDB.java:138` + `ProjectServlet:289` | Owner được chèn vào `project_members` 2 lần; lần 2 luôn vi phạm khóa chính → log lỗi mỗi lần tạo dự án. | T |
| F8 | Task / Doc / Whiteboard (không có `@Version`) | Nhiều người cùng sửa thì người lưu sau ghi đè, không có cảnh báo xung đột. | TB |
| F9 | `tasks-board.js:476,511,610,761`, `chat.js`, `timeline.js` | Thao tác AJAX xong là `location.reload()`; mất mạng thì phần lớn chỉ có `.catch` tối thiểu, không retry, không báo rõ. | TB |
| F10 | Kiểm tra "đã khóa" | Chỗ thì so `DONE`, chỗ thì `DONE \|\| APPROVED` (`TaskCrudHandler:419`, `TaskBoardHandler:364`). Trạng thái `APPROVED` cũ còn lẫn trong code. | T |
| F11 | `ProjectDB.java:124` | Mã dự án tự sinh `PRJ-` + `millis % 100000` có thể trùng → insert lỗi. | T |
| F12 | Kiểm tra 10 thành viên (`ProjectInviteServlet:164,356`) | Kiểm tra rồi mới chèn, không atomic → hai người chấp nhận cùng lúc có thể vượt giới hạn. | T |
| F13 | Không có `ServletContextListener` | `EntityManagerFactory` và thread pool của `ActivityLogDB` không bao giờ được đóng → rò kết nối mỗi lần redeploy. | TB |

### 2.2 Bảo mật

| ID | File:dòng | Mô tả | Mức |
|---|---|---|---|
| S1 | `db.properties:10`, `persistence.xml:31-33` | Thông tin đăng nhập DB production commit vào repo public. | NT |
| S2 | JSP (EL không escape), `js/chat.js:77,444`, `tasks-board.js:964` | Stored XSS: tên task/subtask, họ tên, tên dự án, mô tả, hoạt động, tin nhắn… | NT |
| S3 | Toàn app | Không có CSRF token; cookie không đặt `SameSite`/`Secure`; nhiều action ghi dữ liệu qua GET (xem mục 0.3). | C |
| S4 | `TaskCrudHandler:172,477-481,612` | Task Lead/thành viên tự quyết `requiresGate` → bỏ qua cổng duyệt của PM. | C |
| S5 | `TimelineServlet:248-312` | `updateDueDate` / `quickAddTask` chỉ cần là thành viên; không kiểm tra PM/Lead hay trạng thái khóa. | C |
| S6 | `TaskCrudHandler:75-206,540-643`, `TimelineServlet:251` | Mọi thành viên tạo task và giao cho bất kỳ ai → trái với quyết định "chỉ PM". | C |
| S7 | `ProfileServlet:243` + `TimelineServlet:211` | `User.role` vừa là chức danh tự nhập vừa là cờ quyền `ADMIN`. | TB |
| S8 | `PasswordUtil:16-48`, `AuthServlet:261,385` | SHA-256 + salt cố định; mật khẩu bị `trim()` trước khi băm. | C |
| S9 | `RememberMeToken:26` | Secret mặc định viết cứng khi thiếu biến môi trường. | C |
| S10 | `ProjectServlet:332,372`, `NotificationServlet:95-108` | Open redirect qua `redirectUrl` / `redirect` (khi deploy ROOT, `//evil.com` lọt qua kiểm tra). | TB |
| S11 | `TaskCrudHandler:193-202` | Gắn `docIds` của dự án khác vào task → lộ tiêu đề tài liệu ngoài dự án. | TB |
| S12 | `ProjectServlet:134-162`, `TaskBoardHandler:206-211`, `ProfileServlet:96-108` | Lộ danh sách mọi dự án, mọi người dùng (ứng viên mời) và dự án của bất kỳ ai. | TB |
| S13 | `AuthServlet:460-512` | Gửi OTP chỉ chặn trong 1 session → tạo session mới để spam email. | TB |
| S14 | `web.xml` | Không có header bảo mật (CSP, `X-Frame-Options`, `X-Content-Type-Options`); không cấu hình `cookie-config`. | TB |
| S15 | `ChatServlet:470` | PM sửa được nội dung tin nhắn của người khác (giả mạo lời nói). | T |
| S16 | `MeetingServlet:34-44` | Tên phòng Jitsi suy ra từ id + tên dự án, không có salt bí mật. | T |
| S17 | `TaskBoardHandler:464` | CSV không chặn ô bắt đầu bằng `= + - @` (formula injection). | T |
| S18 | `AuthFilter:62` | Whitelist `/includes/` → các JSP mảnh truy cập trực tiếp được khi chưa đăng nhập. | T |
| S19 | `TaskBoardHandler:124-129` | Session giữ `cached_system_users` (kèm hash mật khẩu), dữ liệu cũ tới khi hết session. | T |

### 2.3 Hiệu năng

| ID | File:dòng | Mô tả | Mức |
|---|---|---|---|
| P1 | `ProfileServlet:115-130` | Mỗi lần xem hồ sơ: `TaskDB.selectAll()` + 1 truy vấn subtask **cho từng task trong toàn hệ thống** (N+1, tăng theo dữ liệu). | C |
| P2 | `TaskBoardHandler:69-346` + `tasks.jsp` (4.238 dòng) | Trang task chạy khoảng 17 truy vấn, nạp mọi comment/subtask/chat/activity/thông báo, render mọi modal cho mọi task; lại bị reload sau mỗi thao tác. | C |
| P3 | `ProjectServlet:186-188` | `countMembers` cho từng dự án trong hệ thống (N+1). Sẽ hết khi ẩn "Dự án khác". | TB |
| P4 | `chat.js:429-475` | Poll 4 giây/lần, mỗi lần server đọc 50 tin; có thay đổi thì tải lại cả trang HTML rồi thay `innerHTML`. | TB |
| P5 | `whiteboard.js:29,136` | Pull 5 giây/lần; mỗi lần lưu gửi toàn bộ scene (tối đa 8 MB). | TB |
| P6 | `ChatServlet:242-246`, `TaskBoardHandler:420` | N+1: `UserDB.selectById` cho từng thành viên; `SubTaskDB.selectByTaskId` cho từng task khi xuất CSV. | T |
| P7 | `ProfileServlet:138-145` | `ProjectDB.selectAll()` + `isMember` cho từng dự án. | T |
| P8 | Frontend | 23 file CSS (≈ 12.200 dòng, 1.022 `!important`), không minify/bundle; Bootstrap tải từ CDN. | T |

### 2.4 Chất lượng code

| ID | Vị trí | Mô tả | Mức |
|---|---|---|---|
| Q1 | Mọi servlet | Lặp lại: lấy `currentUser`, `safeParseInt` (6 bản sao), flash toast, `DateTimeFormatter`, bảng nhãn trạng thái (`ProjectServlet:459`, `TaskBoardHandler:432`, JSP). | TB |
| Q2 | `tasks.jsp` 4.238 dòng, `page-components.css` 2.330, `tasks-board.js` 1.558 | File quá lớn, khó review, dễ conflict khi làm nhóm. | TB |
| Q3 | Status/priority/type | Chuỗi rời rạc (`"TODO"`, `"PLANNING"`…), không có enum → dễ gõ sai, khó biết luồng chuyển hợp lệ. | TB |
| Q4 | `TaskJson`, các handler | JSON ghép chuỗi tay; `jsonString` không escape ký tự điều khiển. | T |
| Q5 | Entity | Phi chuẩn hóa (`assigneeName`, `projectName`, `senderName` trong invite) → phải đồng bộ thủ công (`ProfileServlet:253-255`). | T |
| Q6 | `WEB-INF/lib` (29 jar) + `pom.xml` | Hai nguồn dependency có thể lệch phiên bản. | T |
| Q7 | Comment | Rất nhiều comment kiểu "▶ JSP: … đọc bằng …" và mô tả quảng cáo ("tăng tốc 50 lần") → nhiễu khi đọc. | T |
| Q8 | `src/test` | Không có test cho servlet, phân quyền hay workflow. | TB |

---

## 3. Đánh giá UI/UX

### 3.1 Đánh giá chung (so với Linear / Trello / Asana)

| Tiêu chí | Hiện trạng | Đánh giá |
|---|---|---|
| Design system | `base.css` có token (`--brand-*`, `--radius-*`, `--font-sans`), nhưng 8 file CSS khác tự định nghĩa token riêng; **192 mã màu hex khác nhau**, 138 `style=""` inline, 1.022 `!important`; trộn lẫn class Bootstrap (`fs-8`, `text-dark`) với class tự đặt | Chưa có design system thật — chỉ có palette |
| Typography | Ít nhất 5 khai báo `font-family` khác nhau (Inter, SF Pro, Segoe…) | Không nhất quán |
| Nút / icon | Kết hợp Bootstrap Icons với emoji trong văn bản hệ thống (🏆🚀🛡️) | Thiếu chuyên nghiệp, đọc khó |
| Tương phản | `--brand-text-subtle #8AAEE0` trên nền trắng ≈ 2,3:1; `--brand-text-muted #627D98` ≈ 4,3:1 | Không đạt WCAG AA (4,5:1) với chữ nhỏ |
| Responsive | Chỉ khoảng 12 media query cho cả app; `tasks.jsp` dùng nhiều độ rộng cố định (`max-width: 380px`) và bảng nhiều cột | Desktop ổn; tablet/mobile kém, đặc biệt là list view và Gantt |
| Loading / empty / error | Có toast và một số empty state ("Chưa có…"); **không có skeleton/spinner** cho dữ liệu; lỗi mạng gần như im lặng; sau thao tác thì reload toàn trang | Thiếu |
| Phản hồi thao tác | Toast tốt; nhưng reload làm mất vị trí cuộn và trạng thái mở rộng | Trung bình |
| Bàn phím | 157 `onclick` inline, có `<div onclick>`/`<tr onclick>` không focus được; command palette là điểm cộng | Kém |
| Label form | `tasks.jsp` có 146 ô nhập nhưng chỉ 40 `<label>` | Kém |

### 3.2 Luồng chính

| Luồng | Vấn đề | Đề xuất |
|---|---|---|
| Tạo / giao task | Form dài trong modal; PM và member thấy cùng form | Quick-add 1 dòng (tiêu đề + Enter) cho PM; chọn người/hạn bằng popover; ẩn hẳn với member |
| Đổi trạng thái | Kéo thả có, nhưng lỗi cổng duyệt hiện thành toast dài kèm emoji | Thông báo ngắn, chỉ dẫn hành động kế tiếp ("Gửi duyệt kế hoạch"); chặn ngay từ UI (cột bị khóa hiện xám) |
| Xem tiến độ nhóm | Có báo cáo, health badge, workload | Gom về một trang "Tổng quan" có burndown/biểu đồ theo tuần; số liệu nhất quán giữa các trang |
| Mời thành viên | Danh sách ứng viên = mọi user (lộ dữ liệu); lỗi mời lại im lặng | Mời bằng email/username chính xác + link mời có hạn; hiển thị trạng thái lời mời |
| Thông báo | Chỉ cập nhật khi tải trang; icon lẫn class CSS trong dữ liệu (`"bi-check-circle-fill text-success"`) | Badge tự cập nhật (poll nhẹ), nhóm theo ngày, đánh dấu đã đọc ngay tại chỗ |

### 3.3 Đề xuất theo màn hình

| Màn hình | Đề xuất cụ thể |
|---|---|
| Landing (`index.jsp`) | Giảm hiệu ứng; dùng ảnh chụp sản phẩm thật; CTA rõ "Đăng nhập / Tạo tài khoản" |
| Đăng nhập / quên MK | Thêm `autocomplete`, hiện/ẩn mật khẩu, thông báo lỗi gắn `aria-live`; kiểm tra độ mạnh mật khẩu |
| Danh sách dự án (`projects.jsp`) | Chỉ hiện dự án của tôi; thẻ dự án hiện tiến độ, hạn gần nhất, avatar thành viên; empty state hướng dẫn "Tạo dự án / Nhập mã" |
| Task (`tasks.jsp`) | Tách thành các fragment (list, board, drawer, modal); drawer chi tiết nạp bằng AJAX khi mở thay vì render sẵn cho mọi task; cập nhật DOM tại chỗ thay vì reload; filter dạng chip; phím tắt (`C` tạo, `/` tìm) |
| Drawer chi tiết task | Bố cục 2 cột (nội dung \| thuộc tính); bình luận có thời gian tương đối; lịch sử hoạt động gộp gọn |
| Timeline | Cuộn ngang mượt trên mobile; kéo thanh để đổi hạn (chỉ PM/Lead); vạch "Hôm nay" cố định |
| Chat | Render bằng `textContent` + tạo node cho mention; nhóm tin liên tiếp cùng người; thêm chỉ báo "đang tải"/"mất kết nối" |
| Wiki | Markdown an toàn (escape trước, render sau) + xem trước; hiện "sửa lần cuối bởi…"; cảnh báo khi có người khác vừa sửa |
| Báo cáo | Bỏ mô tả dài dòng; biểu đồ có chú thích & bảng số liệu thay thế; nút in/xuất PDF |
| Hồ sơ | Chỉ hiện dự án chung với người xem; ô "Chức danh" tách khỏi quyền |
| Trang lỗi 404/500 | Đã có; bổ sung mã lỗi tham chiếu để báo cáo |

---

## 4. Kế hoạch theo giai đoạn

Độ khó: **S** ≤ nửa ngày · **M** 1–2 ngày · **L** 3–5 ngày

### Giai đoạn 1 — Sửa lỗi nghiêm trọng & lỗ hổng bảo mật

| ID | Task | File liên quan | Độ khó | Phụ thuộc | Cách kiểm tra đã xong |
|---|---|---|---|---|---|
| 1.1 | **Đổi mật khẩu DB Supabase ngay**; chuyển thông tin kết nối sang biến môi trường (`DB_URL`, `DB_USER`, `DB_PASSWORD`) và nạp vào `Persistence.createEntityManagerFactory(unit, overrides)`; `db.properties` → `db.properties.example` | `JPAUtil.java`, `persistence.xml`, `db.properties`, `.gitignore`, `README.md`, `Dockerfile`, `build_and_run.bat` | S | — | Mật khẩu cũ bị Supabase từ chối; `git grep -i password src/main/resources` không còn giá trị thật; app chạy được với env |
| 1.2 | Xóa bí mật khỏi lịch sử git (`git filter-repo`), force-push, báo cả nhóm clone lại | Toàn repo | S | 1.1 | `git log -p --all \| grep <mật khẩu cũ>` trả về rỗng |
| 1.3 | Chống XSS phía server: thay mọi `${...}` hiển thị dữ liệu người dùng bằng `<c:out>`/`${fn:escapeXml()}`; escape đúng ngữ cảnh khi chèn vào thuộc tính / JS (`onclick="...(${id})"` chỉ dùng số) | Tất cả `*.jsp`, `includes/*.jsp` | L | — | Tạo task/họ tên/tin nhắn chứa `<img src=x onerror=alert(1)>` → hiện nguyên văn ở mọi màn hình; thêm spec E2E quét EL chưa escape |
| 1.4 | Chống XSS phía client: `chat.js` escape HTML trước khi chuyển mention, hoặc dựng DOM bằng `textContent`; rà 38 chỗ `innerHTML` | `js/chat.js`, `js/tasks-board.js`, `js/app.js`, `js/timeline.js` | M | — | Như 1.3 cho chat và comment; review mọi chỗ `innerHTML` có ghép dữ liệu |
| 1.5 | Thêm CSRF: `CsrfFilter` sinh token theo session, kiểm tra mọi POST; JSP thêm `<input type=hidden name=_csrf>`; `fetch` gửi header `X-CSRF-Token` | `filters/CsrfFilter.java` (mới), `web.xml`, `includes/header.jsp`, mọi form, `js/*.js` | M | — | POST thiếu/sai token → 403; mọi form và AJAX vẫn chạy |
| 1.6 | Chuyển mọi action ghi dữ liệu từ GET sang POST (xóa task/tin nhắn/tài liệu/thông báo, accept/reject/revoke, đăng xuất) | `TaskServlet`, `ChatServlet`, `DocServlet`, `NotificationServlet`, `ProjectInviteServlet`, `AuthServlet`, các JSP tương ứng | M | 1.5 | Gọi GET các URL đó → không thay đổi dữ liệu (405 hoặc redirect) |
| 1.7 | Áp quyết định phân quyền: chỉ PM tạo/giao task (add, quickAdd, timeline quickAdd); chỉ PM bật/tắt `requiresGate`; đổi hạn chót chỉ PM/Lead và không áp dụng cho task đã khóa | `TaskCrudHandler`, `TimelineServlet`, `tasks.jsp`, `timeline.jsp` | M | — | Test: member gọi `action=add` → bị từ chối; Lead gửi `requiresGate=false` → không đổi; member gọi `updateDueDate` → 403 |
| 1.8 | Bổ sung kiểm tra trạng thái ở các bước duyệt (PM duyệt/trả/từ chối task chỉ khi `SUBMITTED`; duyệt subtask chỉ khi `SUBMITTED`) | `TaskWorkflowHandler`, `SubTaskHandler` | S | — | Unit test bảng chuyển trạng thái: chuyển sai → bị từ chối |
| 1.9 | Bỏ cờ `ADMIN` khỏi `User.role`; tách `jobTitle` (tự nhập) khỏi quyền | `ProfileServlet`, `TimelineServlet:211`, `User.java`, `profile.jsp`, migration SQL | S | — | Đặt chức danh "ADMIN" không thay đổi quyền ở bất kỳ màn hình nào |
| 1.10 | Băm mật khẩu bằng PBKDF2-HMAC-SHA256 (có sẵn trong JDK, salt riêng, ≥ 210.000 vòng); tự nâng cấp hash cũ khi đăng nhập thành công; bỏ `trim()` mật khẩu | `PasswordUtil`, `UserDB.selectByCredentials`, `AuthServlet` | M | 1.1 | Hash mới có định dạng `pbkdf2$iter$salt$hash`; user cũ vẫn đăng nhập được và hash được thay; test mới pass |
| 1.11 | Remember-me: bắt buộc `TEAMWORK_REMEMBER_SECRET` (không có thì tắt tính năng); cookie `Secure` + `SameSite=Lax`; cấu hình `<cookie-config>` cho session | `RememberMeToken`, `AuthServlet`, `web.xml` (hoặc `context.xml`) | S | 1.1 | Không có env → không phát cookie ghi nhớ; DevTools thấy cờ `Secure`, `HttpOnly`, `SameSite` |
| 1.12 | Chặn open redirect: chỉ chấp nhận đường dẫn nội bộ bắt đầu bằng `/` nhưng không phải `//` | `ProjectServlet`, `NotificationServlet` | S | — | `redirect=//evil.com` và `https://evil.com` → về trang mặc định |
| 1.13 | Ẩn dữ liệu ngoài phạm vi: danh sách dự án chỉ lấy dự án của tôi; bỏ danh sách "mọi user" làm ứng viên mời; hồ sơ chỉ hiện dự án chung; `docIds` phải thuộc cùng dự án | `ProjectServlet`, `projects.jsp`, `TaskBoardHandler`, `ProfileServlet`, `TaskCrudHandler:193` | M | — | User A không thấy tên dự án/người dùng không liên quan; gắn doc dự án khác → bị bỏ qua |
| 1.14 | Sửa lỗi mời lại (F1): đổi ràng buộc thành unique một phần `WHERE status='PENDING'` (Postgres) hoặc cập nhật bản ghi cũ về `PENDING`; servlet kiểm tra kết quả insert | `ProjectInviteServlet`, `ProjectInviteDB`, migration mới | S | — | Mời → từ chối → mời lại thành công; insert lỗi → toast lỗi, không gửi thông báo |
| 1.15 | Giới hạn gửi OTP theo IP/username (tái dùng `LoginAttemptLimiter`) | `AuthServlet` | S | — | Gửi quá N lần trong 15 phút từ một IP → bị chặn |
| 1.16 | Header bảo mật: `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, CSP cơ bản cho phép CDN đang dùng | `filters/SecurityHeadersFilter.java` (mới), `web.xml` | S | 1.4 | Header xuất hiện trong response; không trang nào vỡ (kiểm tra console) |

### Giai đoạn 2 — Ổn định nền tảng

| ID | Task | File liên quan | Độ khó | Phụ thuộc | Cách kiểm tra đã xong |
|---|---|---|---|---|---|
| 2.1 | Lớp tiện ích chung: `BaseServlet` (lấy user, parse int, flash toast, trả JSON) + `ProjectAccess.require(user, projectId, Role)` dùng cho mọi servlet | `controllers/BaseServlet.java` (mới), mọi servlet | M | GĐ1 | Không còn bản sao `safeParseInt`; mọi kiểm tra quyền đi qua một chỗ |
| 2.2 | Enum `TaskStatus`, `SubTaskStatus`, `Priority`, `ProjectRole` + bảng chuyển trạng thái hợp lệ (gom logic cổng duyệt về một nơi) | `business/`, các handler | M | 2.1 | Unit test bảng chuyển trạng thái phủ mọi cặp; dọn hết `"APPROVED"` cũ |
| 2.3 | Transaction cho thao tác nhiều bước (xóa task, kick/rời, tạo dự án); bỏ chèn owner 2 lần | `TaskDB`, `ProjectDB`, `ProjectMemberDB`, `ProjectInviteServlet` | M | 2.1 | Giả lập lỗi giữa chừng → không còn dữ liệu mồ côi; tạo dự án không còn log lỗi |
| 2.4 | Chuyển cột ngày giờ sang `TIMESTAMP`/`DATE` (`LocalDateTime`/`LocalDate`), validate `dueDate` ở server | Entity, DAO, JSP hiển thị, migration | L | 2.2 | Sắp xếp đúng; nhập ngày sai → báo lỗi; báo cáo và Gantt khớp |
| 2.5 | Khóa lạc quan `@Version` cho Task, Doc, Whiteboard; trả 409 kèm thông báo "Có người vừa cập nhật, tải lại?" | Entity, DAO, JS tương ứng | M | 2.4 | Hai tab cùng sửa → tab lưu sau nhận cảnh báo, không mất dữ liệu |
| 2.6 | Xử lý lỗi chung: `ErrorHandler` ghi log có mã lỗi; AJAX dùng helper `apiFetch()` (timeout, báo lỗi mạng, retry 1 lần với GET) | `js/app.js`, `500.jsp`, `BaseServlet` | M | 2.1 | Ngắt mạng khi thao tác → toast "Mất kết nối" rõ ràng; 500 hiện mã lỗi |
| 2.7 | `AppLifecycleListener` đóng EMF và thread pool khi tắt app | `listeners/AppLifecycleListener.java` (mới) | S | — | Redeploy nhiều lần không còn cảnh báo rò thread/kết nối trong log Tomcat |
| 2.8 | Sửa N+1: hồ sơ (P1) dùng truy vấn đếm có `WHERE assignee_id`; chat, CSV, danh sách dự án dùng truy vấn gộp | `ProfileServlet`, `TaskDB`, `SubTaskDB`, `ChatServlet`, `ProjectServlet` | M | — | Bật `hibernate.show_sql`: mỗi trang ≤ ~10 truy vấn, không phụ thuộc số task |
| 2.9 | Design system: gom token về `styles/tokens.css` (màu, cỡ chữ, khoảng cách 4/8px, bo góc, bóng, z-index) + bản dark; chỉnh màu chữ phụ đạt AA; bỏ `!important` và màu hex rải rác theo từng file | `styles/*.css` | L | — | Số màu hex ngoài `tokens.css` ≈ 0; `!important` giảm > 80%; kiểm tra tương phản AA |
| 2.10 | Chia nhỏ `tasks.jsp` thành fragment (`list.jspf`, `board.jspf`, `task-drawer.jspf`, `modals/*.jspf`); `tasks-board.js` tách module theo tính năng | `tasks.jsp`, `includes/`, `js/` | L | 1.3 | Không file JSP nào > 800 dòng; E2E tier1–3 pass |
| 2.11 | Test: unit test cho phân quyền và workflow (mock DAO hoặc H2), thêm test E2E chống XSS; CI GitHub Actions chạy `mvn test` + E2E | `src/test`, `tests/e2e`, `.github/workflows/ci.yml` | M | 2.1, 2.2 | CI xanh trên mỗi PR; độ phủ ≥ 60% cho `controllers/task` |
| 2.12 | Thống nhất build: `build_and_run.bat` gọi Maven (hoặc copy jar từ `target/`), bỏ jar commit trong `WEB-INF/lib` | `build_and_run.bat`, `WEB-INF/lib`, `.gitignore` | S | — | Clone mới chạy được bằng cả bat và Docker; repo nhẹ hơn ~30 MB |

### Giai đoạn 3 — Nâng cấp UI/UX

| ID | Task | File liên quan | Độ khó | Phụ thuộc | Cách kiểm tra đã xong |
|---|---|---|---|---|---|
| 3.1 | Thư viện component thống nhất (button, input, badge trạng thái, avatar, empty state, skeleton) dựa trên token | `styles/components.css`, `includes/` | M | 2.9 | Trang demo `/styleguide.jsp` (chỉ dev) hiển thị đủ component; các trang dùng chung class |
| 3.2 | Cập nhật tại chỗ thay vì reload (đổi trạng thái, thêm subtask, comment) + optimistic UI có hoàn tác | `js/tasks-board.js`, `tasks.js`, handler trả JSON | L | 2.6, 2.10 | Không còn `location.reload()` trong luồng task; thao tác < 300 ms cảm nhận |
| 3.3 | Drawer chi tiết task nạp theo yêu cầu (`/task?action=detail&taskId=`) | `TaskBoardHandler`, `task-drawer.jspf`, JS | M | 2.10 | HTML trang task giảm > 50%; mở drawer có skeleton |
| 3.4 | Trạng thái loading / rỗng / lỗi cho mọi danh sách (task, chat, docs, thông báo, timeline) | JSP + JS | M | 3.1 | Checklist từng màn hình: có đủ 3 trạng thái |
| 3.5 | Responsive: board cuộn ngang theo cột, list view chuyển dạng thẻ < 768 px, sidebar thành off-canvas, Gantt cuộn được | CSS, `tasks.jsp`, `timeline.jsp` | L | 2.9 | Không có cuộn ngang toàn trang ở 375 px; các luồng chính dùng được trên điện thoại |
| 3.6 | Accessibility: `<button>` thay cho `div/tr onclick`, `label` cho mọi input, focus ring, `aria-live` cho toast, phím tắt có tài liệu, kéo thả có phương án dùng bàn phím | Tất cả JSP, JS | M | 3.1 | Lighthouse Accessibility ≥ 90 cho các trang chính; dùng Tab đi hết luồng tạo task |
| 3.7 | Văn bản hệ thống gọn, bỏ emoji trong thông báo/tin nhắn hệ thống, thống nhất thuật ngữ (Task/Nhiệm vụ/Công việc) | Handler, JSP | S | — | Bảng thuật ngữ trong `docs/`; rà soát chuỗi |
| 3.8 | Trang Tổng quan dự án: tiến độ theo tuần, việc sắp đến hạn của tôi, việc chờ tôi duyệt | `ProjectServlet`, JSP mới | M | 2.4 | PM thấy ngay việc cần duyệt; member thấy việc của mình |

### Giai đoạn 4 — Tính năng mới đáng làm

| ID | Tính năng | Lý do | File liên quan | Độ khó | Phụ thuộc | Cách kiểm tra đã xong |
|---|---|---|---|---|---|---|
| 4.1 | **Upload tệp bàn giao thật** (`@MultipartConfig`, giới hạn dung lượng/đuôi, lưu ngoài webroot, tải qua servlet có kiểm tra quyền) | Hoàn thiện tính năng đang dở; thể hiện kiến thức Servlet (multipart, stream) | `TaskWorkflowHandler`, `FileServlet` (mới), `tasks.jsp` | M | 1.5, 2.1 | Nộp PDF → PM tải được; người ngoài dự án nhận 403 |
| 4.2 | **@mention gửi thông báo + email nhắc hạn** (job hằng ngày bằng `ScheduledExecutorService` trong listener) | Thông báo là xương sống của app teamwork; tận dụng `MailUtil` sẵn có | `ChatServlet`, `NotificationDB`, `AppLifecycleListener` | M | 2.7 | Nhắc `@user` → người đó có thông báo; task còn 1 ngày → email |
| 4.3 | **Thông báo / chat thời gian thực bằng WebSocket** (`jakarta.websocket`, có sẵn trong Tomcat) thay polling | Giảm tải server, trải nghiệm như app thật; vẫn thuộc chuẩn Jakarta EE | `ws/ProjectSocket.java` (mới), `chat.js`, `app.js` | L | 1.5, 2.6 | Hai trình duyệt: tin nhắn hiện < 1 s, không còn poll 4 s |
| 4.4 | **Xóa / lưu trữ dự án, chuyển quyền PM** | Thiếu chức năng cơ bản của quản lý dự án | `ProjectServlet`, `ProjectDB`, `projects.jsp` | M | 2.3 | PM lưu trữ → dự án chỉ đọc; chuyển PM → quyền đổi ngay |
| 4.5 | **Lịch sử thay đổi task + Wiki có phiên bản** | Truy vết khi nhiều người cùng làm; bổ trợ cho 2.5 | `ActivityLog`, bảng `doc_versions` (mới) | M | 2.4 | Xem và khôi phục được phiên bản trước |
| 4.6 | **Tìm kiếm toàn dự án** (task, subtask, docs, chat) gắn vào command palette | Dự án lớn cần tìm nhanh; command palette đã có sẵn | `SearchServlet` (mới), `command-palette.js` | M | 1.3 | Gõ từ khóa → kết quả nhóm theo loại, có điều hướng |
| 4.7 | **Link mời có hạn (token)** thay vì tìm user theo tên | An toàn hơn và dễ dùng hơn khi đã ẩn danh sách người dùng | `ProjectInviteServlet`, migration | S | 1.13 | Mở link → vào dự án; link hết hạn/đã dùng → báo lỗi |
| 4.8 | **Mẫu dự án / mẫu task lặp lại** (sprint tuần, checklist) | Giảm thao tác lặp cho nhóm sinh viên | `ProjectServlet`, `TaskDB` | M | 2.2 | Tạo dự án từ mẫu → đủ nhãn/task mặc định |

---

## 5. Lộ trình làm tuần tự

Làm **từng bước một**, theo đúng thứ tự. Xong bước nào thì đánh `[x]`, build + chạy thử, commit, rồi mới sang bước tiếp theo.

| Bước | Trạng thái | Nội dung | Task gốc | Ai làm |
|---|---|---|---|---|
| 1 | [x] | Đưa thông tin DB ra khỏi code: đọc từ biến môi trường / file `db.properties` cục bộ (không commit) | 1.1 | Claude |
| 1b | [ ] | Đổi mật khẩu DB trên Supabase, cập nhật `db.properties` cục bộ + biến môi trường trên Render | 1.1 | **Bạn** |
| 1c | [ ] | Xóa mật khẩu cũ khỏi lịch sử git (`git filter-repo`) + force-push, báo nhóm clone lại | 1.2 | **Bạn** (Claude hướng dẫn) |
| 2 | [x] | Chống XSS ở chat: `chat.js` escape trước khi tạo mention, bỏ gán `innerHTML` thô | 1.4 | Claude |
| 3 | [x] | Chống XSS ở `tasks.jsp` (màn hình demo chính) | 1.3 | Claude |
| 4 | [x] | Chống XSS ở các JSP còn lại + `includes/` + các chỗ `innerHTML` trong JS khác | 1.3, 1.4 | Claude |
| 5 | [x] | Sửa lỗi mời lại thành viên (ràng buộc UNIQUE + kiểm tra kết quả insert); ẩn nút tải tệp bàn giao khi chưa có tệp | 1.14 | Claude (không cần migration) |
| 6 | [x] | Phân quyền: chỉ PM tạo/giao task, chỉ PM bật/tắt cổng duyệt, đổi hạn chót chỉ PM/Lead | 1.7 | Claude |
| 7 | [ ] | Kiểm tra trạng thái ở các bước duyệt (task & subtask phải `SUBMITTED`) | 1.8 | Claude |
| 8 | [ ] | Tách chức danh khỏi quyền (bỏ cờ `ADMIN` trong `role`) | 1.9 | Claude |
| 9 | [ ] | Chuyển các thao tác xóa / chấp nhận / từ chối / đăng xuất từ GET sang POST | 1.6 | Claude |
| 10 | [ ] | Thêm CSRF token cho mọi form và `fetch` | 1.5 | Claude |
| 11 | [ ] | Ẩn dữ liệu ngoài phạm vi (chỉ hiện dự án của tôi, bỏ danh sách mọi user, kiểm `docIds`) | 1.13 | Claude |
| 12 | [ ] | Chặn open redirect | 1.12 | Claude |
| 13 | [ ] | Băm mật khẩu PBKDF2 + tự nâng cấp hash cũ; secret remember-me bắt buộc từ env, cookie `SameSite` | 1.10, 1.11 | Claude |
| 14 | [ ] | Giới hạn gửi OTP theo IP; thêm header bảo mật | 1.15, 1.16 | Claude |
| — | | **Mốc kiểm tra:** chạy toàn bộ kịch bản demo với dữ liệu chứa `<script>`; đăng nhập bằng tài khoản thành viên thường để thử phân quyền | | Bạn + Claude |
| 15 | [ ] | `BaseServlet` + kiểm tra quyền tập trung | 2.1 | Claude |
| 16 | [ ] | Enum trạng thái + bảng chuyển trạng thái | 2.2 | Claude |
| 17 | [ ] | Transaction cho thao tác nhiều bước | 2.3 | Claude |
| 18 | [ ] | Sửa N+1 (hồ sơ, chat, CSV, danh sách dự án) | 2.8 | Claude |
| 19 | [ ] | Unit test phân quyền & workflow | 2.11 | Claude |
| 20 | [ ] | Đóng EMF / thread pool khi tắt app; xử lý lỗi mạng chung `apiFetch()` | 2.7, 2.6 | Claude |
| 21 | [ ] | Chia nhỏ `tasks.jsp` thành fragment | 2.10 | Claude |
| 22 | [ ] | Gom design token, sửa tương phản | 2.9 | Claude |
| 23 | [ ] | Loading / rỗng / lỗi cho các danh sách | 3.4 | Claude |
| 24 | [ ] | Accessibility (button thật, label, focus) | 3.6 | Claude |
| 25 | [ ] | Cập nhật tại chỗ thay vì reload trang | 3.2 | Claude |
| 26 | [ ] | Responsive mobile | 3.5 | Claude |
| 27 | [ ] | Upload tệp bàn giao thật | 4.1 | Claude |
| 28 | [ ] | @mention gửi thông báo + email nhắc hạn | 4.2 | Claude |
| 29+ | [ ] | Tùy thời gian: 2.4, 2.5, 3.3, 3.8, 4.3 – 4.8 | | |

> Ghi chú: E2E chưa chạy được trên máy khảo sát vì thiếu Node.js; nên cài Node và chạy `node tests/e2e/runner.js` để có baseline.
