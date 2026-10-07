# TeamWork Hub

Hệ thống quản lý làm việc nhóm — **Java 21 · Jakarta Servlet 6 / JSP · JPA (Hibernate 6) · Tomcat 10.1 · Supabase PostgreSQL**, theo mô hình **MVC Model 2**.

## Chạy dự án (Windows)

1. Cài **JDK 21** và **Apache Tomcat 10.1+**.
2. (Tùy chọn) Copy `env.example.bat` → `env.bat` rồi điền `JAVA_HOME`, `CATALINA_HOME`. Nếu không, script tự dò; không thấy sẽ hỏi và tự lưu lại.
3. **Bắt buộc — mật khẩu DB:** copy `src/main/resources/db.properties.example` → `db.properties` (không bị commit) và điền `db.password` (hỏi trưởng nhóm). Trên Render/Docker đặt biến môi trường `DB_PASSWORD` (tùy chọn `DB_URL`, `DB_USERNAME`, `DB_JPA_UNIT`).
   - Muốn bật **Ghi nhớ đăng nhập**: đặt thêm `TEAMWORK_REMEMBER_SECRET` (chuỗi ngẫu nhiên ≥ 32 ký tự; chạy máy cá nhân thì khai báo trong `env.bat`, xem `env.example.bat`). Không đặt thì tính năng này tự tắt.
   - Tệp bàn giao (upload, tối đa 20 MB) lưu ngoài webapp, mặc định `~/teamwork-hub-uploads`; đổi bằng `TEAMWORK_UPLOAD_DIR`. **Trên Render/Docker:** ổ đĩa container bị xóa mỗi lần deploy lại — gắn persistent disk vào thư mục đó (vd `/var/data/uploads`) và đặt `TEAMWORK_UPLOAD_DIR` trỏ tới, nếu không tệp đã nộp sẽ mất sau mỗi lần deploy.
   - **Nhắc hạn chót:** mỗi ngày lúc `TEAMWORK_REMINDER_HOUR` giờ (mặc định 8h, giờ VN), công việc / việc con hết hạn vào ngày mai và chưa xong → người phụ trách nhận thông báo 🔔 (+ email nếu đã cấu hình `mail.properties`). Bật mặc định; tắt bằng `TEAMWORK_REMINDERS=off` — máy cá nhân nên tắt (đã có sẵn trong `env.example.bat`) vì DB dùng chung, chỉ server chính nên chạy.
   - **Tài khoản mẫu cho buổi demo** (hiện sẵn dưới form đăng nhập, bấm để tự điền): `admin`/`admin123` (Trưởng dự án), `alice`/`alice123`, `bob`/`bob123`, `david`/`david123`. Ẩn khung này bằng `TEAMWORK_DEMO_ACCOUNTS=off`. Đừng đổi mật khẩu các tài khoản này (qua Hồ sơ / Quên mật khẩu), nếu không nút đăng nhập nhanh sẽ sai mật khẩu.
4. Chạy **`build_and_run.bat`** (biên dịch + deploy + khởi động Tomcat) → mở <http://localhost:8080/teamwork-hub/>.
5. Chỉ sửa `.jsp` / `.css` / `.js`? Dùng **`hot_jsp.bat`** (hoặc `hot_jsp.bat tasks.jsp`) rồi F5 — không cần restart, không mất đăng nhập. Sửa `.java` thì phải chạy lại `build_and_run.bat`.
6. Gửi mail OTP quên mật khẩu: copy `src/main/resources/mail.properties.example` → `mail.properties` và điền mật khẩu ứng dụng Gmail (file này không bị commit).

Docker / Render: `Dockerfile` (build bằng Maven). Chạy test: `mvn test` (unit) và `node tests/e2e/runner.js` (E2E tĩnh trên JSP).

## Bản đồ thư mục

```
teamwork-hub/
├── src/main/java/com/teamwork/
│   ├── business/        Entity (Model)         ─ ánh xạ bảng DB
│   ├── data/            DAO (JPA)              ─ TaskDB, ProjectDB, JPAUtil…
│   ├── controllers/     Servlet (Controller)   ─ mỗi servlet một URL
│   │   └── task/        Handler của /task      ─ board / crud / subtask / workflow
│   ├── filters/         AuthFilter             ─ chặn người chưa đăng nhập
│   └── util/            Mật khẩu, OTP, mail, chống brute-force
├── src/main/webapp/     View
│   ├── *.jsp            Trang;  includes/ = khung dùng chung (header, navbar, footer, toast…)
│   ├── js/  styles/  images/   Tài nguyên tĩnh
│   └── WEB-INF/         web.xml + lib/ (jar dùng cho build_and_run.bat)
├── src/main/resources/  persistence.xml, db.properties.example, mail.properties.example
├── src/test/            Unit test (JUnit) + manual/JPAManualCheck (chạy tay)
├── database/            schema_*.sql + migrations/
├── tests/e2e/           Bộ test E2E (Node) kiểm tra JSP
├── docs/                CODE_MAP.md · ARCHITECTURE.md · TECH_SPEC.md · DEMO_DEFENSE.md · ai-notes/
└── build_and_run.bat · hot_jsp.bat · env.example.bat · Dockerfile · pom.xml
```

## Đọc gì trước?

| Mục đích | Tài liệu |
|---|---|
| **Tìm nhanh chức năng nằm ở đâu (URL → Servlet → JSP)** | [docs/CODE_MAP.md](docs/CODE_MAP.md) |
| **Servlet ↔ JSP nối nhau ở đâu** (tự sinh, có số dòng; trong code tìm ký hiệu `▶`/`◀`) | [docs/LINK_MAP.md](docs/LINK_MAP.md) — làm mới: `node docs/tools/gen_link_map.js` |
| Chuẩn bị trả lời giảng viên, có lệnh demo | [docs/DEMO_DEFENSE.md](docs/DEMO_DEFENSE.md) |
| Thiết kế hệ thống, DB, luồng nghiệp vụ | [docs/TECH_SPEC.md](docs/TECH_SPEC.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| Dựng CSDL mới | `database/schema_postgres_supabase.sql` (hoặc `schema_sqlserver.sql` cho SSMS) |
| Quy ước làm việc nhóm / môi trường | [docs/ai-notes/TEAM_GUARDRAILS.md](docs/ai-notes/TEAM_GUARDRAILS.md) |
