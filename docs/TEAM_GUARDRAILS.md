# Teamwork & Environment Configuration Guardrails

## 1. Không bao giờ hardcode đường dẫn cục bộ (Never Hardcode Local Paths)
- Tuyệt đối KHÔNG viết cứng đường dẫn tuyệt đối của máy cá nhân (ví dụ: `C:\Users\...\`, `D:\Tomcat\...`) vào bất kỳ tệp tin nào được theo dõi bởi Git (`build_and_run.bat`, `pom.xml`, config XML, v.v.).
- Mọi đường dẫn phụ thuộc môi trường máy phát triển PHẢI được tách biệt vào tệp cấu hình máy cá nhân (`env.bat`, `.env`, `local.properties`).

## 2. Kỷ luật Git Ignore (Strict Git-Ignore Discipline)
- Tệp chứa cấu hình máy cá nhân (`env.bat`, `local.env.bat`) PHẢI được khai báo trong `.gitignore` ngay từ đầu và KHÔNG BAO GIỜ được commit lên repository.
- Chỉ commit tệp mẫu (`env.example.bat`) với các giá trị placeholder mang tính tham khảo và kèm ghi chú hướng dẫn rõ ràng.

## 3. Chuỗi 4 Tầng Ưu Tiên Cấu Hình Tự Thích Ứng (4-Tier Fallback Priority)
Mọi script chạy tự động (`.bat`, `.sh`) phục vụ cả nhóm PHẢI thiết kế theo chuỗi kiểm tra từ cao xuống thấp:
1. **Tầng 1 (Local Override)**: Nếu có `env.bat` và đường dẫn hợp lệ, sử dụng đường dẫn đó.
2. **Tầng 2 (System Environment)**: Nếu biến `%JAVA_HOME%` hoặc `%CATALINA_HOME%` đã tồn tại sẵn trên Windows của thành viên và hợp lệ, giữ nguyên và sử dụng ngay (KHÔNG được ghi đè bằng giá trị mẫu).
3. **Tầng 3 (Smart Auto-Detection)**:
   - Với Java: Tự động dò qua lệnh `where javac.exe` hoặc quét các thư mục cài đặt tiêu chuẩn (`%ProgramFiles%\Java`, `%ProgramFiles%\Eclipse Adoptium`, `%LocalAppData%\Programs\Eclipse Adoptium`).
   - Với Tomcat: Tự động quét tìm các thư mục khớp mẫu phổ biến (`C:\apache-tomcat-10.1*`, `C:\apache-tomcat-10*`, `%ProgramFiles%\Apache Software Foundation\Tomcat 10.1*`, `D:\apache-tomcat-10.1*`).
4. **Tầng 4 (Interactive CLI Prompt & Auto-Save)**:
   - Nếu cả 3 tầng trên đều không phát hiện được, script KHÔNG được dừng đột ngột mà phải hiển thị lời nhắc thân thiện trên console cho phép thành viên dán (paste) đường dẫn Tomcat của họ.
   - Script tự động lưu giá trị đó vào `env.bat` (đã gitignore) để lần chạy sau hoàn toàn tự động 100%.

## 4. Đồng Bộ Bảng Mã UTF-8 Trên Windows
- Mọi script batch chạy trên Windows phải thiết lập `chcp 65001 >nul`.
- Java JVM khi chạy phải kèm cờ: `-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8` để console không bao giờ bị lỗi font tiếng Việt.
