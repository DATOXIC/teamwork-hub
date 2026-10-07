:: =============================================================================
:: CẤU HÌNH ĐƯỜNG DẪN MÁY CÁ NHÂN (ENV.EXAMPLE.BAT)
::
:: HƯỚNG DẪN DÀNH CHO THÀNH VIÊN TRONG NHÓM:
:: 1. Copy file này và đổi tên thành "env.bat".
:: 2. File "env.bat" ĐÃ NẰM TRONG .GITIGNORE nên KHÔNG BAO GIỜ bị commit đẩy lên Git,
::    hoàn toàn không lo bị xung đột (conflict) với các thành viên khác!
:: 3. LƯU Ý ĐẶC BIỆT: Nếu máy bạn đã cài sẵn biến môi trường JAVA_HOME hoặc
::    CATALINA_HOME trên Windows, script sẽ TỰ ĐỘNG NHẬN DIỆN, bạn không cần sửa gì cả!
::    Chỉ điền vào dưới đây nếu bạn muốn chỉ định một thư mục riêng biệt.
:: =============================================================================

:: [TÙY CHỌN] Đường dẫn Java JDK (nếu muốn chỉ định riêng):
:: Ví dụ:
:: set "JAVA_HOME=C:\Program Files\Java\jdk-21"
:: set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"

:: [TÙY CHỌN] Đường dẫn Apache Tomcat 10.1+ (nếu muốn chỉ định riêng):
:: Ví dụ:
:: set "CATALINA_HOME=C:\apache-tomcat-10.1"
:: set "CATALINA_HOME=C:\apache-tomcat-10.1\apache-tomcat-10.1.57"
:: set "CATALINA_HOME=C:\Program Files\Apache Software Foundation\Tomcat 10.1"

:: Tên Web Application Context Path (mặc định: teamwork-hub)
set "APP_NAME=teamwork-hub"

:: [TÙY CHỌN] Bật "Ghi nhớ đăng nhập": chuỗi ngẫu nhiên tối thiểu 32 ký tự, KHÔNG chia sẻ / commit.
:: Không đặt thì tính năng ghi nhớ tự tắt (đăng nhập bình thường vẫn chạy).
:: Tạo nhanh trong PowerShell: [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
:: set "TEAMWORK_REMEMBER_SECRET=dan-chuoi-ngau-nhien-cua-ban-vao-day"

:: [TÙY CHỌN] Thư mục lưu tệp bàn giao (upload). Mặc định: %USERPROFILE%\teamwork-hub-uploads
:: Nằm NGOÀI webapp nên redeploy không mất tệp; tải về qua /task-file (chỉ thành viên dự án).
:: set "TEAMWORK_UPLOAD_DIR=D:\teamwork-hub-uploads"

:: Nhắc hạn chót hằng ngày (thông báo + email, chạy lúc TEAMWORK_REMINDER_HOUR giờ VN, mặc định 8h).
:: DB dùng chung cả nhóm → CHỈ server chính (Render) nên chạy; máy cá nhân tắt để tránh nhắc trùng.
set "TEAMWORK_REMINDERS=off"
:: set "TEAMWORK_REMINDER_HOUR=8"

