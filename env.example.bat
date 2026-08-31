:: =============================================================================
:: FILE MẪU CẤU HÌNH MÁY TÍNH CÁ NHÂN (ENV.EXAMPLE.BAT)
:: Hướng dẫn: Đổi tên file này thành "env.bat" và điền đường dẫn máy của bạn.
:: File "env.bat" đã nằm trong .gitignore nên KHÔNG bị commit đè lên máy người khác.
:: =============================================================================

:: 1. Đường dẫn thư mục cài đặt Java JDK 17 hoặc 21 trên máy bạn
set "JAVA_HOME=C:\Program Files\Java\jdk-21"

:: 2. Đường dẫn thư mục cài đặt Apache Tomcat 10.1.x trên máy bạn
set "CATALINA_HOME=C:\apache-tomcat-10.1"

:: 3. Tên ứng dụng web khi chạy trên Tomcat
set "APP_NAME=teamwork-hub"
