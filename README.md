# TeamWork Hub

Hệ thống quản lý làm việc nhóm — **Java 21 · Jakarta Servlet 6 / JSP · JPA (Hibernate 6) · Tomcat 10.1 · Supabase PostgreSQL**, theo mô hình **MVC Model 2**.

## Chạy dự án (Windows)

1. Cài **JDK 21** và **Apache Tomcat 10.1+**.
2. (Tùy chọn) Copy `env.example.bat` → `env.bat` rồi điền `JAVA_HOME`, `CATALINA_HOME`. Nếu không, script tự dò; không thấy sẽ hỏi và tự lưu lại.
3. Chạy **`build_and_run.bat`** (biên dịch + deploy + khởi động Tomcat) → mở <http://localhost:8080/teamwork-hub/>.
4. Chỉ sửa `.jsp` / `.css` / `.js`? Dùng **`hot_jsp.bat`** (hoặc `hot_jsp.bat tasks.jsp`) rồi F5 — không cần restart, không mất đăng nhập. Sửa `.java` thì phải chạy lại `build_and_run.bat`.
5. Gửi mail OTP quên mật khẩu: copy `src/main/resources/mail.properties.example` → `mail.properties` và điền mật khẩu ứng dụng Gmail (file này không bị commit).

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
├── src/main/resources/  persistence.xml, db.properties, mail.properties.example
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
