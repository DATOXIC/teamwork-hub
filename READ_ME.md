# TÀI LIỆU ĐẶC TẢ KỸ THUẬT VÀ THIẾT KẾ HỆ THỐNG
## ĐỒ ÁN MÔN HỌC: LẬP TRÌNH WEB (WEB PROGRAMMING)
---
**TÊN ĐỀ TÀI:** **TeamWork Hub — Nền Tảng Không Gian Làm Việc & Quản Lý Dự Án Nhóm Toàn Diện**  
**KIẾN TRÚC:** MVC Model 2 (Java Servlet / JSP / JavaBean / Jakarta EE)  
**MÔI TRƯỜNG CHẠY:** Java JDK 21 | Apache Tomcat 10.1.x | Jakarta Servlet 6.0 | JSP 3.1 | Jakarta EL 5.0  

---

## MỤC LỤC
1. [Bối Cảnh & Lý Do Chọn Đề Tài](#1-bối-cảnh--lý-do-chọn-đề-tài)
2. [Mô Tả Tổng Quan Dự Án](#2-mô-tả-tổng-quan-dự-án)
3. [Quy Chuẩn Công Nghệ & Ràng Buộc Kiến Trúc](#3-quy-chuẩn-công-nghệ--ràng-buộc-kiến-trúc)
4. [Kiến Trúc Hệ Thống MVC Model 2 & Luồng Xử Lý Dữ Liệu](#4-kiến-trúc-hệ-thống-mvc-model-2--luồng-xử-lý-dữ-liệu)
5. [Thiết Kế Cơ Sở Dữ Liệu & Mô Hình Đối Tượng (JavaBean Models)](#5-thiết-kế-cơ-sở-dữ-liệu--mô-hình-đối-tượng-javabean-models)
6. [Đặc Tả Use Case Hệ Thống](#6-đặc-tả-use-case-hệ-thống)
7. [Danh Sách Chức Năng Chi Tiết Theo Phân Hệ](#7-danh-sách-chức-năng-chi-tiết-theo-phân-hệ)
8. [Bản Đồ URL Routing & Phân Rã Controller](#8-bản-đồ-url-routing--phân-rã-controller)
9. [Cấu Trúc Thư Mục Chuẩn Dự Án](#9-cấu-trúc-thư-mục-chuẩn-dự-án)
10. [Mã Nguồn Mẫu Chuẩn Cho Từng Lớp (Reference Code Templates)](#10-mã-nguồn-mẫu-chuẩn-cho-từng-lớp-reference-code-templates)

---

## 1. BỐI CẢNH & LÝ DO CHỌN ĐỀ TÀI

### 1.1. Thực trạng & Vấn đề giải quyết
Trong quá trình học tập và làm việc nhóm, sinh viên và các nhóm phát triển thường gặp phải các vấn đề nhức nhối:
* **Phân mảnh công cụ (Tool Fragmentation):** Phải mở đồng thời Trello (quản lý task), Google Docs (viết tài liệu), Zalo/Slack (trao đổi thảo luận) dẫn đến phân tán thông tin, trôi deadline và giảm năng suất.
* **Quy trình quản lý tiến độ thiếu trực quan:** Khó nắm bắt ai đang làm gì, việc nào bị nghẽn, việc nào đã hoàn thành.
* **Yêu cầu môn học Web Programming:** Đòi hỏi sinh viên phải hiểu sâu về bản chất của Web (giao thức HTTP, mô hình Request/Response, Session Management, Server-Side Rendering với JSP/Servlet, kiến trúc MVC Model 2) mà không bị che giấu bởi các framework frontend hiện đại như React hay Vue.

### 1.2. Mục tiêu dự án
Xây dựng một hệ thống **TeamWork Hub** tập trung: tích hợp bảng Kanban kéo thả, soạn thảo tài liệu nhóm, trao đổi tin nhắn và biểu đồ thống kê tiến độ trên nền tảng **Java Servlet/JSP thuần**, giao diện hiện đại, tinh gọn với Bootstrap 5, mang lại trải nghiệm làm việc mượt mà và trực quan.

---

## 2. MÔ TẢ TỔNG QUAN DỰ ÁN

**TeamWork Hub** là ứng dụng web quản lý làm việc nhóm theo mô hình Client-Server truyền thống:
* **Đối tượng sử dụng chính:**
  * **Trưởng nhóm (Admin / Project Owner):** Khởi tạo dự án, phân quyền, quản lý thành viên, theo dõi tiến độ tổng thể.
  * **Thành viên (Member):** Nhận công việc, kéo thả trạng thái trên bảng Kanban, viết tài liệu ghi chú, tham gia thảo luận.
  * **Khách (Guest / Viewer):** Xem tiến độ công việc và tài liệu ở chế độ chỉ đọc.
* **4 Trụ cột chức năng chính:**
  1. **Kanban Board:** Bảng công việc 3 cột (*To Do, In Progress, Done*) hỗ trợ kéo thả trực quan.
  2. **Team Wiki / Docs:** Soạn thảo, lưu trữ và xuất bản tài liệu hướng dẫn, ghi chú cuộc họp.
  3. **Discussion Hub:** Kênh trao đổi tin nhắn theo dự án.
  4. **Analytics Dashboard:** Báo cáo tỷ lệ hoàn thành công việc, cảnh báo việc trễ hạn.

---

## 3. QUY CHUẨN CÔNG NGHỆ & RÀNG BUỘC KIẾN TRÚC

> [!IMPORTANT]
> **Toàn bộ dự án phải tuân thủ nghiêm ngặt các quy tắc kỹ thuật sau đây:**

| Thành phần | Phiên bản / Chuẩn | Quy tắc bắt buộc |
| :--- | :--- | :--- |
| **Ngôn ngữ** | Java JDK 21 | Lập trình hướng đối tượng, tuân thủ nghiêm ngặt chuẩn JavaBean. |
| **Web Server** | Apache Tomcat 10.1.x | Chạy trên nền tảng Jakarta EE 10. |
| **Servlet API** | `jakarta.servlet 6.0` | **BẮT BUỘC** import `jakarta.servlet.*`. **CẤM** import `javax.servlet.*`. |
| **View Engine** | JSP 3.1 + Jakarta EL 5.0 | **CHỈ** dùng Expression Language `${...}` để hiển thị. **CẤM** dùng Java Scriptlet (`<% ... %>`) trong View. |
| **Deployment Descriptor** | `WEB-INF/web.xml` | Khai báo URL Mapping toàn bộ trong `web.xml`. **KHÔNG** dùng annotation `@WebServlet`. |
| **Giao diện (Frontend)** | HTML5, CSS3, Bootstrap 5.3 (CDN) | Giao diện hiện đại, responsive. **KHÔNG** dùng React, Vue, Angular, Node.js build tools. |
| **Tương tác Client** | Vanilla JavaScript (ES6) | Sử dụng HTML5 Drag & Drop API, DOM Manipulation, Fetch API. |
| **Mã hóa ký tự** | UTF-8 | Luôn thiết lập `request.setCharacterEncoding("UTF-8")` và `response.setContentType("text/html;charset=UTF-8")`. |

---

## 4. KIẾN TRÚC HỆ THỐNG MVC MODEL 2 & LUỒNG XỬ LÝ DỮ LIỆU

### 4.1. Sơ đồ luồng xử lý chuẩn MVC Model 2

```
[ BROWSER (CLIENT) ]
       │
       ├─ (1) Gửi HTTP Request (GET/POST) kèm action=xxx và form data
       ▼
[ WEB SERVER (TOMCAT 10.1) ]
       │
       ├─ (2) Tra cứu routing table trong WEB-INF/web.xml
       ▼
[ CONTROLLER LAYER: XxxServlet.java ]
       ├─ request.setCharacterEncoding("UTF-8");
       ├─ Đọc action = request.getParameter("action");
       ├─ Server-side Validation (Bắt buộc kiểm tra rỗng, kiểu dữ liệu, logic nghiệp vụ);
       ├─ Gọi Data Layer (XxxDB) để truy vấn hoặc cập nhật;
       ├─ Lưu kết quả vào request: request.setAttribute("dataKey", dataValue);
       └─ Forward sang View: getServletContext().getRequestDispatcher("/view.jsp").forward(request, response);
       │
       ▼
[ DATA LAYER: XxxDB.java ] ◄──► [ IN-MEMORY STORE / JDBC DATA SOURCE ]
       │
       ▼
[ MODEL LAYER: Xxx.java (JavaBean) ]
       │
       ▼
[ VIEW LAYER: view.jsp ]
       ├─ Truy xuất dữ liệu hoàn toàn bằng Jakarta EL: ${dataKey.property}
       ├─ Không chứa mã logic Java bên trong JSP
       └─ Sinh mã HTML hoàn chỉnh
       │
       ▼
[ BROWSER HIỂN THỊ GIAO DIỆN ]
```

### 4.2. Nguyên tắc phân lớp
1. **Model Layer (`com.teamwork.business`):** Chứa các JavaBean đại diện cho thực thể dữ liệu.
2. **Data Access Layer (`com.teamwork.data`):** Chứa các lớp `XxxDB` cung cấp các phương thức CRUD static thao tác với kho dữ liệu.
3. **Controller Layer (`com.teamwork.controllers`):** Các `HttpServlet` tiếp nhận request, validate, điều phối luồng và forward tới JSP.
4. **View Layer (`/src/main/webapp/*.jsp`):** Chỉ làm nhiệm vụ hiển thị dữ liệu qua EL, tái sử dụng layout với `jsp:include`.

---

## 5. THIẾT KẾ CƠ SỞ DỮ LIỆU & MÔ HÌNH ĐỐI TƯỢNG (JAVABEAN MODELS)

Mọi lớp Model đều phải triển khai `java.io.Serializable`, có constructor mặc định không đối số và đầy đủ các hàm Getter/Setter.

```
       ┌────────────────┐                   ┌────────────────┐
       │      User      │ 1               * │    Project     │
       ├────────────────┼───────────────────┼────────────────┤
       │ id (PK)        │                   │ id (PK)        │
       │ username       │                   │ name           │
       │ password       │                   │ description    │
       │ fullName       │                   │ ownerId (FK)   │
       │ role           │                   │ createdAt      │
       └───────┬────────┘                   └───────┬────────┘
               │ 1                                  │ 1
               │                                    │
               ├─────────────────┬──────────────────┤
               │ *               │ *                │ *
       ┌───────▼────────┐ ┌──────▼────────┐ ┌───────▼────────┐
       │      Task      │ │   Document    │ │    Message     │
       ├────────────────┤ ├───────────────┤ ├────────────────┤
       │ id (PK)        │ │ id (PK)       │ │ id (PK)        │
       │ projectId (FK) │ │ projectId(FK) │ │ projectId (FK) │
       │ title          │ │ title         │ │ senderId (FK)  │
       │ description    │ │ content       │ │ senderName     │
       │ status         │ │ authorId (FK) │ │ content        │
       │ priority       │ │ updatedAt     │ │ sentAt         │
       │ dueDate        │ └───────────────┘ └────────────────┘
       │ assigneeId(FK) │
       │ assigneeName   │
       └────────────────┘
```

### 5.1. Thực thể `User.java`
| Thuộc tính | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `id` | `int` | Khóa chính định danh người dùng |
| `username` | `String` | Tên đăng nhập duy nhất (Unique) |
| `password` | `String` | Mật khẩu xác thực |
| `fullName` | `String` | Họ và tên đầy đủ hiển thị |
| `role` | `String` | Vai trò hệ thống (`ADMIN`, `MEMBER`, `GUEST`) |
| `avatar` | `String` | Đường dẫn ảnh đại diện |

### 5.2. Thực thể `Project.java`
| Thuộc tính | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `id` | `int` | Khóa chính định danh dự án |
| `name` | `String` | Tên dự án |
| `description` | `String` | Mô tả mục tiêu dự án |
| `ownerId` | `int` | ID người tạo / trưởng dự án |
| `createdAt` | `String` | Ngày khởi tạo (YYYY-MM-DD) |

### 5.3. Thực thể `Task.java`
| Thuộc tính | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `id` | `int` | Khóa chính thẻ công việc |
| `projectId` | `int` | Khóa ngoại tham chiếu đến Project |
| `title` | `String` | Tiêu đề công việc |
| `description` | `String` | Mô tả chi tiết yêu cầu công việc |
| `status` | `String` | Trạng thái (`TODO`, `IN_PROGRESS`, `DONE`) |
| `priority` | `String` | Mức độ ưu tiên (`HIGH`, `MEDIUM`, `LOW`) |
| `dueDate` | `String` | Hạn chót hoàn thành (YYYY-MM-DD) |
| `assigneeId` | `int` | ID người được giao việc |
| `assigneeName`| `String` | Tên người phụ trách (phục vụ EL hiển thị) |

### 5.4. Thực thể `Document.java`
| Thuộc tính | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `id` | `int` | Khóa chính tài liệu |
| `projectId` | `int` | Khóa ngoại tham chiếu đến Project |
| `title` | `String` | Tiêu đề tài liệu / ghi chú |
| `content` | `String` | Nội dung văn bản chi tiết |
| `authorId` | `int` | ID tác giả |
| `authorName` | `String` | Tên tác giả |
| `updatedAt` | `String` | Thời gian cập nhật gần nhất |

### 5.5. Thực thể `Message.java`
| Thuộc tính | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `id` | `int` | Khóa chính tin nhắn |
| `projectId` | `int` | Khóa ngoại tham chiếu đến Project |
| `senderId` | `int` | ID người gửi |
| `senderName` | `String` | Tên hiển thị người gửi |
| `content` | `String` | Nội dung tin nhắn thảo luận |
| `sentAt` | `String` | Thời điểm gửi (HH:mm dd/MM/yyyy) |

---

## 6. ĐẶC TẢ USE CASE HỆ THỐNG

### 6.1. Bảng Ma Trận Phân Quyền Use Case

| Mã Use Case | Tên Use Case | Khách (Guest) | Thành viên (Member) | Trưởng nhóm (Admin) |
| :--- | :--- | :---: | :---: | :---: |
| **UC01** | Đăng ký & Đăng nhập tài khoản | ✅ | ✅ | ✅ |
| **UC02** | Xem tổng quan dự án (Dashboard) | ✅ | ✅ | ✅ |
| **UC03** | Tạo mới & Quản lý thông tin dự án | ❌ | ❌ | ✅ |
| **UC04** | Xem Bảng công việc Kanban | ✅ (Chỉ xem) | ✅ | ✅ |
| **UC05** | Thêm mới thẻ công việc (Task) | ❌ | ✅ | ✅ |
| **UC06** | Kéo thả / Đổi trạng thái Task | ❌ | ✅ | ✅ |
| **UC07** | Xóa thẻ công việc | ❌ | ❌ | ✅ |
| **UC08** | Xem danh sách tài liệu Wiki | ✅ | ✅ | ✅ |
| **UC09** | Soạn thảo & Cập nhật tài liệu | ❌ | ✅ | ✅ |
| **UC10** | Gửi tin nhắn thảo luận nhóm | ❌ | ✅ | ✅ |
| **UC11** | Đăng xuất khỏi hệ thống | ✅ | ✅ | ✅ |

### 6.2. Chi tiết các Use Case chính

#### UC01: Đăng Nhập Hệ Thống (Login)
* **Tác nhân:** Người dùng (Guest / Member / Admin).
* **Tiền điều kiện:** Người dùng đang ở trang `login.jsp`.
* **Luồng sự kiện chính:**
  1. Người dùng nhập `username` và `password`, nhấn nút "Đăng nhập".
  2. Form gửi HTTP POST đến `/auth?action=login`.
  3. `AuthServlet` kiểm tra dữ liệu:
     * Nếu rỗng: set message `"Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu"`, forward về `login.jsp`.
     * Nếu sai tài khoản: set message `"Tên đăng nhập hoặc mật khẩu không chính xác"`, forward về `login.jsp`.
     * Nếu đúng: lưu đối tượng `User` vào `HttpSession`, redirect sang `/project?action=list`.

#### UC06: Cập Nhật Trạng Thái Task Bằng Kéo Thả (Drag & Drop Kanban)
* **Tác nhân:** Thành viên, Trưởng nhóm.
* **Tiền điều kiện:** Đang xem bảng Kanban tại `kanban.jsp`.
* **Luồng sự kiện chính:**
  1. Người dùng cầm chuột kéo thẻ Task từ cột `TODO` thả sang cột `IN_PROGRESS`.
  2. JavaScript (`kanban.js`) bắt sự kiện `drop`, lấy `taskId` và `newStatus`.
  3. Gửi HTTP POST tới `/task` với tham số `action=updateStatus&taskId=...&newStatus=IN_PROGRESS&projectId=...`.
  4. `TaskServlet` nhận request, gọi `TaskDB.updateStatus(taskId, newStatus)`.
  5. Servlet forward/redirect về `/task?action=list&projectId=...` để cập nhật giao diện đồng bộ.

---

## 7. DANH SÁCH CHỨC NĂNG CHI TIẾT THEO PHÂN HỆ

### 7.1. Phân Hệ 1: Xác Thực & Quản Lý Phiên (Authentication & Session)
* **Đăng ký tài khoản:** Xác thực phía máy chủ (Username tối thiểu 4 ký tự, mật khẩu tối thiểu 6 ký tự, kiểm tra trùng lặp).
* **Đăng nhập & Lưu Session:** Thiết lập `session.setAttribute("user", currentUser)` với thời gian timeout cấu hình trong `web.xml` (30 phút).
* **Đăng xuất an toàn:** Hủy phiên làm việc `session.invalidate()` và điều hướng về trang chủ.
* **Filter kiểm tra quyền truy cập:** Ngăn chặn người dùng chưa đăng nhập truy cập trực tiếp vào các tài nguyên nội bộ.

### 7.2. Phân Hệ 2: Quản Lý Dự Án (Project Management & Dashboard)
* **Danh sách dự án:** Hiển thị thẻ card các dự án mà người dùng tham gia kèm tiến độ phần trăm.
* **Tạo dự án mới:** Cho phép Trưởng nhóm nhập tên dự án, mô tả và ngày bắt đầu.
* **Thống kê tổng quan:** Hiển thị tổng số việc, số việc đã xong, số việc đang làm ngay trên từng card dự án.

### 7.3. Phân Hệ 3: Bảng Công Việc Kéo Thả (Kanban Board)
* **Bảng 3 cột chuẩn:** *To Do (Cần làm)*, *In Progress (Đang làm)*, *Done (Đã hoàn thành)*.
* **Kéo thả HTML5 Drag & Drop:** Kéo thả thẻ task giữa các cột, đổi màu viền trực quan khi hover.
* **Thêm mới công việc nhanh:** Modal pop-up nhập tiêu đề, mô tả, mức độ ưu tiên (*High/Medium/Low*), hạn chót và gán người phụ trách.
* **Bộ lọc thông minh:** Lọc nhanh task theo mức độ ưu tiên hoặc người phụ trách.

### 7.4. Phân Hệ 4: Tài Liệu & Ghi Chú Nhóm (Team Wiki & Docs)
* **Danh sách tài liệu dự án:** Xem danh mục các bài viết, ghi chú hướng dẫn kỹ thuật.
* **Soạn thảo văn bản:** Trình soạn thảo trực quan hỗ trợ định dạng tiêu đề, danh sách, khối mã lệnh.
* **Tự động lưu nháp phía Client:** Lưu tạm vào `localStorage` chống mất nội dung khi rớt mạng.
* **Xuất bản & Cập nhật:** Ghi nhận thời gian cập nhật và tên tác giả sửa đổi lần cuối.

### 7.5. Phân Hệ 5: Thảo Luận & Trao Đổi (Team Chat)
* **Kênh trao đổi theo dự án:** Xem dòng thời gian tin nhắn của các thành viên trong nhóm.
* **Gửi tin nhắn tức thì:** Nhập nội dung thảo luận, gửi form HTTP POST và tự động cuộn xuống tin nhắn mới nhất.
* **Hiển thị thông tin người gửi:** Avatar, tên thành viên và nhãn thời gian rõ ràng.

### 7.6. Phân Hệ 6: Báo Cáo & Xử Lý Lỗi Hệ Thống
* **Biểu đồ tiến độ dự án:** Vẽ thanh tiến độ (Progress Bar) phần trăm hoàn thành bằng CSS/HTML5.
* **Trang thông báo lỗi chuẩn:** Trang `error_404.jsp` (khi gõ sai đường dẫn) và `error_java.jsp` (khi phát sinh lỗi ngoại lệ) cấu hình tự động trong `web.xml`.

---

## 8. BẢN ĐỒ URL ROUTING & PHÂN RÃ CONTROLLER

Toàn bộ mapping được đăng ký trong `WEB-INF/web.xml`:

| URL Pattern | Servlet Class | HTTP Method | `action` param | Nghiệp vụ xử lý | View JSP đích |
| :--- | :--- | :---: | :--- | :--- | :--- |
| `/auth` | `AuthServlet` | POST | `login` | Kiểm tra username/password, tạo Session | `/dashboard.jsp` / `/login.jsp` |
| | | POST | `register` | Validate dữ liệu, tạo User mới | `/login.jsp` |
| | | GET | `logout` | Xóa Session, giải phóng bộ nhớ | `/index.jsp` |
| `/project` | `ProjectServlet` | GET | `list` | Lấy danh sách dự án của user | `/dashboard.jsp` |
| | | POST | `create` | Tạo mới dự án, validate tên dự án | Redirect `/project?action=list` |
| | | GET | `detail` | Lấy chi tiết dự án, forward sang Kanban | `/kanban.jsp` |
| `/task` | `TaskServlet` | GET | `list` | Lấy danh sách task theo 3 trạng thái | `/kanban.jsp` |
| | | POST | `add` | Thêm mới task vào cột TODO | Redirect `/task?action=list` |
| | | POST/GET | `updateStatus`| Cập nhật status của task (`TODO` ➔ `DONE`)| Redirect `/task?action=list` |
| | | POST | `delete` | Xóa task khỏi dự án | Redirect `/task?action=list` |
| `/doc` | `DocServlet` | GET | `list` | Lấy toàn bộ tài liệu của dự án | `/docs.jsp` |
| | | GET | `view` | Xem chi tiết nội dung 1 tài liệu | `/doc_detail.jsp` |
| | | POST | `save` | Tạo mới hoặc cập nhật tài liệu | Redirect `/doc?action=list` |
| `/chat` | `ChatServlet` | GET | `list` | Lấy lịch sử tin nhắn của dự án | `/chat.jsp` |
| | | POST | `send` | Thêm tin nhắn mới vào dự án | Redirect `/chat?action=list` |

---

## 9. CẤU TRÚC THƯ MỤC CHUẨN DỰ ÁN

```
teamwork-hub/
├── src/main/java/
│   └── com/teamwork/
│       ├── business/                   # MODEL LAYER (JavaBeans chuẩn)
│       │   ├── User.java
│       │   ├── Project.java
│       │   ├── Task.java
│       │   ├── Document.java
│       │   └── Message.java
│       ├── data/                       # DATA ACCESS LAYER (CRUD Helper)
│       │   ├── UserDB.java
│       │   ├── ProjectDB.java
│       │   ├── TaskDB.java
│       │   ├── DocumentDB.java
│       │   └── MessageDB.java
│       └── controllers/                # CONTROLLER LAYER (Jakarta Servlets)
│           ├── AuthServlet.java
│           ├── ProjectServlet.java
│           ├── TaskServlet.java
│           ├── DocServlet.java
│           └── ChatServlet.java
├── src/main/webapp/
│   ├── WEB-INF/
│   │   └── web.xml                     # Deployment Descriptor (BẮT BUỘC)
│   ├── styles/
│   │   └── main.css                    # Tùy biến giao diện bổ trợ Bootstrap 5
│   ├── js/
│   │   └── kanban.js                   # Xử lý kéo thả HTML5 Drag & Drop
│   ├── includes/                       # Layout tái sử dụng
│   │   ├── header.jsp
│   │   ├── navbar.jsp
│   │   └── footer.jsp
│   ├── index.jsp                       # Trang giới thiệu (Landing Page)
│   ├── login.jsp                       # Trang Đăng nhập & Đăng ký
│   ├── dashboard.jsp                   # Bảng điều khiển danh sách dự án
│   ├── kanban.jsp                      # Bảng công việc Kanban kéo thả
│   ├── docs.jsp                        # Danh sách tài liệu nhóm
│   ├── doc_detail.jsp                  # Chi tiết / Soạn thảo tài liệu
│   ├── chat.jsp                        # Kênh thảo luận nhóm
│   ├── error_404.jsp                   # Trang báo lỗi 404 (Không tìm thấy trang)
│   └── error_java.jsp                  # Trang báo lỗi 500 (Ngoại lệ Java)
└── pom.xml                             # Cấu hình Maven Dependencies
```

---

## 10. MÃ NGUỒN MẪU CHUẨN CHO TỪNG LỚP (REFERENCE CODE TEMPLATES)

### 10.1. File Cấu Hình `pom.xml` (Maven Standard)
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.teamwork</groupId>
    <artifactId>teamwork-hub</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>war</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Jakarta Servlet API 6.0 cho Tomcat 10.1+ -->
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <version>6.0.0</version>
            <scope>provided</scope>
        </dependency>
        <!-- Jakarta EL API -->
        <dependency>
            <groupId>jakarta.el</groupId>
            <artifactId>jakarta.el-api</artifactId>
            <version>5.0.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>3.4.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

### 10.2. Deployment Descriptor `WEB-INF/web.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                             https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0">

    <display-name>TeamWork Hub</display-name>

    <context-param>
        <param-name>appName</param-name>
        <param-value>TeamWork Hub Collaboration Platform</param-value>
    </context-param>

    <!-- AuthServlet Mapping -->
    <servlet>
        <servlet-name>AuthServlet</servlet-name>
        <servlet-class>com.teamwork.controllers.AuthServlet</servlet-class>
    </servlet>
    <servlet-mapping>
        <servlet-name>AuthServlet</servlet-name>
        <url-pattern>/auth</url-pattern>
    </servlet-mapping>

    <!-- ProjectServlet Mapping -->
    <servlet>
        <servlet-name>ProjectServlet</servlet-name>
        <servlet-class>com.teamwork.controllers.ProjectServlet</servlet-class>
    </servlet>
    <servlet-mapping>
        <servlet-name>ProjectServlet</servlet-name>
        <url-pattern>/project</url-pattern>
    </servlet-mapping>

    <!-- TaskServlet Mapping -->
    <servlet>
        <servlet-name>TaskServlet</servlet-name>
        <servlet-class>com.teamwork.controllers.TaskServlet</servlet-class>
    </servlet>
    <servlet-mapping>
        <servlet-name>TaskServlet</servlet-name>
        <url-pattern>/task</url-pattern>
    </servlet-mapping>

    <!-- DocServlet Mapping -->
    <servlet>
        <servlet-name>DocServlet</servlet-name>
        <servlet-class>com.teamwork.controllers.DocServlet</servlet-class>
    </servlet>
    <servlet-mapping>
        <servlet-name>DocServlet</servlet-name>
        <url-pattern>/doc</url-pattern>
    </servlet-mapping>

    <!-- ChatServlet Mapping -->
    <servlet>
        <servlet-name>ChatServlet</servlet-name>
        <servlet-class>com.teamwork.controllers.ChatServlet</servlet-class>
    </servlet>
    <servlet-mapping>
        <servlet-name>ChatServlet</servlet-name>
        <url-pattern>/chat</url-pattern>
    </servlet-mapping>

    <!-- Session Timeout (30 Phút) -->
    <session-config>
        <session-timeout>30</session-timeout>
    </session-config>

    <!-- Xử lý trang lỗi chuẩn -->
    <error-page>
        <error-code>404</error-code>
        <location>/error_404.jsp</location>
    </error-page>
    <error-page>
        <exception-type>java.lang.Throwable</exception-type>
        <location>/error_java.jsp</location>
    </error-page>

    <welcome-file-list>
        <welcome-file>index.jsp</welcome-file>
    </welcome-file-list>
</web-app>
```

### 10.3. JavaBean Chuẩn: `Task.java`
```java
package com.teamwork.business;

import java.io.Serializable;

public class Task implements Serializable {
    private int id;
    private int projectId;
    private String title;
    private String description;
    private String status; // TODO, IN_PROGRESS, DONE
    private String priority; // HIGH, MEDIUM, LOW
    private String dueDate;
    private int assigneeId;
    private String assigneeName;

    // Constructor rỗng (BẮT BUỘC theo chuẩn JavaBean)
    public Task() {
        this.id = 0;
        this.projectId = 0;
        this.title = "";
        this.description = "";
        this.status = "TODO";
        this.priority = "MEDIUM";
        this.dueDate = "";
        this.assigneeId = 0;
        this.assigneeName = "Chưa phân công";
    }

    public Task(int id, int projectId, String title, String description, String status, 
                String priority, String dueDate, int assigneeId, String assigneeName) {
        this.id = id;
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public int getAssigneeId() { return assigneeId; }
    public void setAssigneeId(int assigneeId) { this.assigneeId = assigneeId; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
}
```

### 10.4. Data Access Layer Chuẩn: `TaskDB.java`
```java
package com.teamwork.data;

import com.teamwork.business.Task;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskDB {
    private static final List<Task> taskStore = new ArrayList<>();
    private static final AtomicInteger idGenerator = new AtomicInteger(1);

    static {
        // Dữ liệu mẫu ban đầu
        insert(new Task(0, 1, "Thiết kế kiến trúc MVC", "Xây dựng sơ đồ lớp và routing", "DONE", "HIGH", "2026-08-25", 1, "Nguyễn Văn A"));
        insert(new Task(0, 1, "Tạo giao diện Kanban JSP", "Ghép Bootstrap 5 và HTML5 Drag & Drop", "IN_PROGRESS", "HIGH", "2026-08-28", 2, "Trần Thị B"));
        insert(new Task(0, 1, "Viết Unit Test Servlet", "Kiểm thử các case validation form", "TODO", "MEDIUM", "2026-08-30", 1, "Nguyễn Văn A"));
    }

    public static synchronized List<Task> selectByProject(int projectId) {
        List<Task> result = new ArrayList<>();
        for (Task t : taskStore) {
            if (t.getProjectId() == projectId) {
                result.add(t);
            }
        }
        return result;
    }

    public static synchronized List<Task> selectByStatus(int projectId, String status) {
        List<Task> result = new ArrayList<>();
        for (Task t : taskStore) {
            if (t.getProjectId() == projectId && t.getStatus().equalsIgnoreCase(status)) {
                result.add(t);
            }
        }
        return result;
    }

    public static synchronized Task select(int taskId) {
        for (Task t : taskStore) {
            if (t.getId() == taskId) return t;
        }
        return null;
    }

    public static synchronized int insert(Task task) {
        task.setId(idGenerator.getAndIncrement());
        taskStore.add(task);
        return 1;
    }

    public static synchronized int updateStatus(int taskId, String newStatus) {
        Task t = select(taskId);
        if (t != null) {
            t.setStatus(newStatus);
            return 1;
        }
        return 0;
    }

    public static synchronized int delete(int taskId) {
        return taskStore.removeIf(t -> t.getId() == taskId) ? 1 : 0;
    }
}
```

### 10.5. Controller Servlet Chuẩn: `TaskServlet.java`
```java
package com.teamwork.controllers;

import java.io.*;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import com.teamwork.business.Task;
import com.teamwork.business.User;
import com.teamwork.data.TaskDB;

public class TaskServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");

        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth?action=login");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) { action = "list"; }

        String projectIdStr = request.getParameter("projectId");
        int projectId = (projectIdStr != null && !projectIdStr.isEmpty()) ? Integer.parseInt(projectIdStr) : 1;

        switch (action) {
            case "list":
                List<Task> todoList = TaskDB.selectByStatus(projectId, "TODO");
                List<Task> inProgressList = TaskDB.selectByStatus(projectId, "IN_PROGRESS");
                List<Task> doneList = TaskDB.selectByStatus(projectId, "DONE");

                request.setAttribute("todoList", todoList);
                request.setAttribute("inProgressList", inProgressList);
                request.setAttribute("doneList", doneList);
                request.setAttribute("projectId", projectId);

                getServletContext().getRequestDispatcher("/kanban.jsp").forward(request, response);
                break;

            case "add":
                String title = request.getParameter("title");
                String description = request.getParameter("description");
                String priority = request.getParameter("priority");
                String dueDate = request.getParameter("dueDate");

                // SERVER-SIDE VALIDATION
                if (title == null || title.trim().isEmpty()) {
                    request.setAttribute("message", "Tiêu đề công việc không được để trống!");
                    // Nạp lại danh sách để forward lại
                    request.setAttribute("todoList", TaskDB.selectByStatus(projectId, "TODO"));
                    request.setAttribute("inProgressList", TaskDB.selectByStatus(projectId, "IN_PROGRESS"));
                    request.setAttribute("doneList", TaskDB.selectByStatus(projectId, "DONE"));
                    request.setAttribute("projectId", projectId);
                    getServletContext().getRequestDispatcher("/kanban.jsp").forward(request, response);
                    return;
                }

                Task newTask = new Task(0, projectId, title, description, "TODO", 
                                        priority, dueDate, currentUser.getId(), currentUser.getFullName());
                TaskDB.insert(newTask);

                response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                break;

            case "updateStatus":
                int taskId = Integer.parseInt(request.getParameter("taskId"));
                String newStatus = request.getParameter("newStatus");
                TaskDB.updateStatus(taskId, newStatus);
                response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                break;

            case "delete":
                int delId = Integer.parseInt(request.getParameter("taskId"));
                TaskDB.delete(delId);
                response.sendRedirect(request.getContextPath() + "/task?action=list&projectId=" + projectId);
                break;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}
```

### 10.6. View JSP Chuẩn: `kanban.jsp` (Thuần Jakarta EL `${...}`)
```html
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bảng Công Việc Kanban - TeamWork Hub</title>
    <!-- Bootstrap 5 CSS CDN -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">
    <link rel="stylesheet" href="styles/main.css">
</head>
<body class="bg-light">

    <!-- Header & Navigation -->
    <jsp:include page="/includes/navbar.jsp" />

    <div class="container-fluid py-4 px-4">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <h3 class="fw-bold mb-0">📋 Bảng Công Việc Kanban</h3>
                <span class="text-muted">Dự án #${projectId}</span>
            </div>
            <button class="btn btn-primary shadow-sm" data-bs-toggle="modal" data-bs-target="#addTaskModal">
                <i class="bi bi-plus-circle me-1"></i> Thêm Công Việc Mới
            </button>
        </div>

        <%-- Thông báo lỗi validation nếu có --%>
        <c:if test="${not empty message}">
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-2"></i> ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <!-- 3 CỘT KANBAN -->
        <div class="row g-4">
            <!-- CỘT 1: CẦN LÀM (TODO) -->
            <div class="col-md-4">
                <div class="card shadow-sm border-0 rounded-3">
                    <div class="card-header bg-secondary text-white py-3">
                        <h6 class="mb-0 fw-bold"><i class="bi bi-list-task me-2"></i> CẦN LÀM (TO DO)</h6>
                    </div>
                    <div class="card-body bg-light p-3 kanban-column" id="TODO" ondrop="drop(event)" ondragover="allowDrop(event)">
                        <c:forEach var="task" items="${todoList}">
                            <div class="card mb-3 shadow-sm border-0 task-card" draggable="true" ondragstart="drag(event)" id="task-${task.id}" data-task-id="${task.id}">
                                <div class="card-body p-3">
                                    <div class="d-flex justify-content-between">
                                        <h6 class="fw-bold text-dark mb-1">${task.title}</h6>
                                        <span class="badge ${task.priority == 'HIGH' ? 'bg-danger' : (task.priority == 'MEDIUM' ? 'bg-warning text-dark' : 'bg-info text-dark')}">
                                            ${task.priority}
                                        </span>
                                    </div>
                                    <p class="text-muted small mb-2">${task.description}</p>
                                    <div class="d-flex justify-content-between align-items-center text-muted small">
                                        <span><i class="bi bi-person me-1"></i> ${task.assigneeName}</span>
                                        <span><i class="bi bi-calendar me-1"></i> ${task.dueDate}</span>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>

            <!-- CỘT 2: ĐANG LÀM (IN_PROGRESS) -->
            <div class="col-md-4">
                <div class="card shadow-sm border-0 rounded-3">
                    <div class="card-header bg-primary text-white py-3">
                        <h6 class="mb-0 fw-bold"><i class="bi bi-arrow-repeat me-2"></i> ĐANG LÀM (IN PROGRESS)</h6>
                    </div>
                    <div class="card-body bg-light p-3 kanban-column" id="IN_PROGRESS" ondrop="drop(event)" ondragover="allowDrop(event)">
                        <c:forEach var="task" items="${inProgressList}">
                            <div class="card mb-3 shadow-sm border-0 task-card" draggable="true" ondragstart="drag(event)" id="task-${task.id}" data-task-id="${task.id}">
                                <div class="card-body p-3">
                                    <div class="d-flex justify-content-between">
                                        <h6 class="fw-bold text-dark mb-1">${task.title}</h6>
                                        <span class="badge ${task.priority == 'HIGH' ? 'bg-danger' : (task.priority == 'MEDIUM' ? 'bg-warning text-dark' : 'bg-info text-dark')}">
                                            ${task.priority}
                                        </span>
                                    </div>
                                    <p class="text-muted small mb-2">${task.description}</p>
                                    <div class="d-flex justify-content-between align-items-center text-muted small">
                                        <span><i class="bi bi-person me-1"></i> ${task.assigneeName}</span>
                                        <span><i class="bi bi-calendar me-1"></i> ${task.dueDate}</span>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>

            <!-- CỘT 3: HOÀN THÀNH (DONE) -->
            <div class="col-md-4">
                <div class="card shadow-sm border-0 rounded-3">
                    <div class="card-header bg-success text-white py-3">
                        <h6 class="mb-0 fw-bold"><i class="bi bi-check2-circle me-2"></i> HOÀN THÀNH (DONE)</h6>
                    </div>
                    <div class="card-body bg-light p-3 kanban-column" id="DONE" ondrop="drop(event)" ondragover="allowDrop(event)">
                        <c:forEach var="task" items="${doneList}">
                            <div class="card mb-3 shadow-sm border-0 task-card bg-white opacity-75" draggable="true" ondragstart="drag(event)" id="task-${task.id}" data-task-id="${task.id}">
                                <div class="card-body p-3">
                                    <div class="d-flex justify-content-between">
                                        <h6 class="fw-bold text-decoration-line-through text-secondary mb-1">${task.title}</h6>
                                        <span class="badge bg-success">DONE</span>
                                    </div>
                                    <p class="text-muted small mb-2">${task.description}</p>
                                    <div class="d-flex justify-content-between align-items-center text-muted small">
                                        <span><i class="bi bi-person me-1"></i> ${task.assigneeName}</span>
                                        <span><i class="bi bi-calendar me-1"></i> ${task.dueDate}</span>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- MODAL THÊM CÔNG VIỆC MỚI -->
    <div class="modal fade" id="addTaskModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="task" method="post">
                    <input type="hidden" name="action" value="add">
                    <input type="hidden" name="projectId" value="${projectId}">
                    
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold">Thêm Công Việc Mới</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label fw-semibold">Tiêu đề công việc <span class="text-danger">*</span></label>
                            <input type="text" name="title" class="form-control" placeholder="Ví dụ: Thiết kế giao diện Dashboard" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-semibold">Mô tả chi tiết</label>
                            <textarea name="description" class="form-control" rows="3" placeholder="Mô tả các yêu cầu cần hoàn thành..."></textarea>
                        </div>
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label class="form-label fw-semibold">Mức độ ưu tiên</label>
                                <select name="priority" class="form-select">
                                    <option value="HIGH">Cao (High)</option>
                                    <option value="MEDIUM" selected>Trung bình (Medium)</option>
                                    <option value="LOW">Thấp (Low)</option>
                                </select>
                            </div>
                            <div class="col-md-6 mb-3">
                                <label class="form-label fw-semibold">Hạn chót (Due Date)</label>
                                <input type="date" name="dueDate" class="form-control">
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary">Lưu công việc</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Bootstrap 5 JS Bundle & JavaScript Kéo Thả -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="js/kanban.js"></script>
</body>
</html>
```

### 10.7. Client-Side Drag & Drop: `kanban.js`
```javascript
// Cho phép thả phần tử
function allowDrop(ev) {
    ev.preventDefault();
}

// Bắt đầu kéo
function drag(ev) {
    ev.dataTransfer.setData("text", ev.target.id);
    ev.dataTransfer.setData("taskId", ev.target.getAttribute("data-task-id"));
}

// Thả vào cột mới
function drop(ev) {
    ev.preventDefault();
    const data = ev.dataTransfer.getData("text");
    const taskId = ev.dataTransfer.getData("taskId");
    const draggedElement = document.getElementById(data);

    let dropTarget = ev.target;
    while (!dropTarget.classList.contains("kanban-column") && dropTarget.parentElement) {
        dropTarget = dropTarget.parentElement;
    }

    if (dropTarget.classList.contains("kanban-column")) {
        dropTarget.appendChild(draggedElement);
        const newStatus = dropTarget.id; // TODO, IN_PROGRESS, DONE
        
        // Gửi form POST cập nhật trạng thái về Servlet
        const form = document.createElement("form");
        form.method = "POST";
        form.action = "task";

        const actionInput = document.createElement("input");
        actionInput.type = "hidden";
        actionInput.name = "action";
        actionInput.value = "updateStatus";
        form.appendChild(actionInput);

        const idInput = document.createElement("input");
        idInput.type = "hidden";
        idInput.name = "taskId";
        idInput.value = taskId;
        form.appendChild(idInput);

        const statusInput = document.createElement("input");
        statusInput.type = "hidden";
        statusInput.name = "newStatus";
        statusInput.value = newStatus;
        form.appendChild(statusInput);

        document.body.appendChild(form);
        form.submit();
    }
}
```

---

## 11. HƯỚNG DẪN BIÊN DỊCH VÀ TRIỂN KHAI (BUILD & DEPLOYMENT)

1. **Yêu cầu môi trường:**
   * Cài đặt JDK 21 (đã cấu hình biến môi trường `JAVA_HOME`).
   * Cài đặt Apache Tomcat 10.1.x.
   * Apache Maven 3.9+.
2. **Lệnh đóng gói ứng dụng:**
   ```bash
   mvn clean package
   ```
   Lệnh trên sẽ sinh ra file `teamwork-hub.war` trong thư mục `target/`.
3. **Deploy lên Tomcat:**
   * Copy file `teamwork-hub.war` vào thư mục `webapps/` của Apache Tomcat.
   * Khởi động Tomcat bằng `bin/startup.bat` (Windows) hoặc `bin/startup.sh` (Linux/macOS).
   * Truy cập ứng dụng tại: `http://localhost:8080/teamwork-hub/`.
