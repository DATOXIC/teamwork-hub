@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

set PROJECT_DIR=%~dp0

echo =========================================================================
echo   TEAMWORK-HUB — SCRIPT TỰ ĐỘNG BIÊN DỊCH VÀ CHẠY DỰ ÁN
echo =========================================================================

:: 1. LƯU LẠI BIẾN MÔI TRƯỜNG HỆ THỐNG CỦA MÁY (NẾU ĐÃ CÓ TRÊN WINDOWS)
set "SYS_JAVA_HOME=%JAVA_HOME%"
set "SYS_CATALINA_HOME=%CATALINA_HOME%"

:: 2. ĐỌC CẤU HÌNH RIÊNG TỪ FILE env.bat (NẰM TRONG .GITIGNORE - KHÔNG XUNG ĐỘT GIT)
if exist "%PROJECT_DIR%env.bat" (
    call "%PROJECT_DIR%env.bat"
    echo [OK] Da nap cau hinh ca nhan tu env.bat
)

if not defined APP_NAME set APP_NAME=teamwork-hub

:: Loại bỏ dấu ngoặc kép thừa nếu có
if defined JAVA_HOME set JAVA_HOME=%JAVA_HOME:"=%
if defined CATALINA_HOME set CATALINA_HOME=%CATALINA_HOME:"=%
if defined APP_NAME set APP_NAME=%APP_NAME:"=%

:: 3. CHUỖI TỰ ĐỘNG NHẬN DIỆN JAVA JDK (4 TẦNG ƯU TIÊN)
:: Tầng 1: Đã hợp lệ từ env.bat
if not exist "%JAVA_HOME%\bin\javac.exe" (
    :: Tầng 2: Sử dụng biến môi trường hệ thống Windows nếu hợp lệ
    if exist "%SYS_JAVA_HOME%\bin\javac.exe" (
        set "JAVA_HOME=%SYS_JAVA_HOME%"
    ) else (
        :: Tầng 3: Tự dò tìm lệnh javac trên PATH
        for /f "tokens=*" %%i in ('where javac.exe 2^>nul') do (
            set "JAVAC_PATH=%%i"
            set "JAVA_HOME=!JAVAC_PATH:\bin\javac.exe=!"
        )
        :: Nếu vẫn chưa thấy, quét các thư mục cài đặt Java tiêu chuẩn
        if not exist "!JAVA_HOME!\bin\javac.exe" (
            for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jdk-*" "%ProgramFiles%\Java\jdk-*" "%LocalAppData%\Programs\Eclipse Adoptium\jdk-*") do (
                if exist "%%d\bin\javac.exe" set "JAVA_HOME=%%d"
            )
        )
    )
)

echo [1/4] Kiem tra Java JDK:
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo [LOI] Khong the tu dong tim thay Java JDK tren may ban!
    echo ==^> Vui long mo file env.bat va dien duong dan JAVA_HOME cua may ban.
    pause
    exit /b 1
)
echo       ==^> Su dung: "%JAVA_HOME%"

:: 4. CHUỖI TỰ ĐỘNG NHẬN DIỆN APACHE TOMCAT (4 TẦNG ƯU TIÊN)
:: Tầng 1: Đã hợp lệ từ env.bat
if not exist "%CATALINA_HOME%\bin\catalina.bat" (
    :: Tầng 2: Sử dụng biến môi trường hệ thống Windows nếu hợp lệ
    if exist "%SYS_CATALINA_HOME%\bin\catalina.bat" (
        set "CATALINA_HOME=%SYS_CATALINA_HOME%"
    ) else (
        :: Tầng 3: Tự dò tìm trong các thư mục Tomcat phổ biến nhất
        for /d %%t in ("C:\apache-tomcat-10.1*" "C:\apache-tomcat-10*" "%ProgramFiles%\Apache Software Foundation\Tomcat 10.1*" "D:\apache-tomcat-10.1*" "D:\apache-tomcat-10*") do (
            if exist "%%t\bin\catalina.bat" set "CATALINA_HOME=%%t"
        )
    )
)

:: Tầng 4: Nếu vẫn chưa tìm thấy, hỏi trực tiếp người dùng trên màn hình console và tự lưu vào env.bat
if not exist "%CATALINA_HOME%\bin\catalina.bat" (
    echo.
    echo =========================================================================
    echo [!] KHONG TIM THAY THU MUC APACHE TOMCAT 10.1+
    echo     Moi may cua thanh vien co the cai Tomcat o mot thu muc khac nhau.
    echo =========================================================================
    echo Vi du: C:\apache-tomcat-10.1.57 hoac C:\Program Files\Apache Software Foundation\Tomcat 10.1
    set /p "INPUT_TOMCAT===> Vui long dan (paste) duong dan thu muc Tomcat cua ban vao day: "
    set "INPUT_TOMCAT=!INPUT_TOMCAT:"=!"
    if exist "!INPUT_TOMCAT!\bin\catalina.bat" (
        set "CATALINA_HOME=!INPUT_TOMCAT!"
        echo set "CATALINA_HOME=!INPUT_TOMCAT!">> "%PROJECT_DIR%env.bat"
        echo [OK] Da tu dong luu duong dan vao file env.bat rieng cua may ban!
    ) else (
        echo [LOI] Duong dan ban nhap khong hop le hoac khong chua bin\catalina.bat!
        pause
        exit /b 1
    )
)

echo [2/4] Kiem tra Apache Tomcat 10.1+:
echo       ==^> Su dung: "%CATALINA_HOME%"

:: Đóng Tomcat/Java cũ trước khi dọn dẹp để tránh lỗi bị khóa file (Access Denied)
echo [*] Kiem tra va giai phong tien trinh Java/Tomcat cu (neu co)...
taskkill /F /IM java.exe 2>nul
ping 127.0.0.1 -n 2 >nul

set DEPLOY_DIR=%CATALINA_HOME%\webapps\%APP_NAME%

:: 4. COPY WEBAPP VÀ RESOURCES VÀO TOMCAT
echo [3/4] Copy Webapp va Resources vao Tomcat...
if exist "%DEPLOY_DIR%" rmdir /S /Q "%DEPLOY_DIR%"
mkdir "%DEPLOY_DIR%\WEB-INF\classes" 2>nul
mkdir "%DEPLOY_DIR%\WEB-INF\lib" 2>nul

xcopy /Y /S /Q "%PROJECT_DIR%src\main\webapp\*" "%DEPLOY_DIR%\" >nul

if exist "%PROJECT_DIR%src\main\resources" (
    xcopy /Y /S /Q "%PROJECT_DIR%src\main\resources\*" "%DEPLOY_DIR%\WEB-INF\classes\" >nul
)

:: 5. BIÊN DỊCH TOÀN BỘ CODE JAVA
echo [4/4] Bien dich code Java (MVC, Servlet, Model, DAO)...
dir /s /b "%PROJECT_DIR%src\main\java\*.java" > "%PROJECT_DIR%sources.txt" 2>nul

set "CP=%CATALINA_HOME%\lib\*;%DEPLOY_DIR%\WEB-INF\lib\*;%DEPLOY_DIR%\WEB-INF\classes"

"%JAVA_HOME%\bin\javac" -encoding UTF-8 -cp "%CP%" -d "%DEPLOY_DIR%\WEB-INF\classes" @"%PROJECT_DIR%sources.txt"
if %errorlevel% neq 0 (
    echo [LOI] Bien dich Java that bai! Vui long kiem tra lai code.
    if exist "%PROJECT_DIR%sources.txt" del "%PROJECT_DIR%sources.txt"
    pause
    exit /b %errorlevel%
)
if exist "%PROJECT_DIR%sources.txt" del "%PROJECT_DIR%sources.txt"
echo       ==^> Bien dich Java thanh cong 100%%!

:: Xóa cache work của Tomcat
if exist "%CATALINA_HOME%\work\Catalina\localhost\%APP_NAME%" (
    rmdir /S /Q "%CATALINA_HOME%\work\Catalina\localhost\%APP_NAME%" 2>nul
)

:: 6. KHỞI ĐỘNG TOMCAT
echo =========================================================================
echo   UNG DUNG DA SAN SANG TAI: http://localhost:8080/%APP_NAME%/
echo =========================================================================

:: Thiết lập JVM và Console sang UTF-8 để hiển thị Tiếng Việt mượt mà, không lỗi font
set "JAVA_OPTS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 %JAVA_OPTS%"

call "%CATALINA_HOME%\bin\catalina.bat" run
pause
