@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

set PROJECT_DIR=%~dp0

echo =========================================================================
echo   TEAMWORK-HUB — SCRIPT TỰ ĐỘNG BIÊN DỊCH VÀ CHẠY DỰ ÁN
echo =========================================================================

:: 1. ĐỌC CẤU HÌNH RIÊNG TỪ FILE env.bat (NẾU CÓ)
if exist "%PROJECT_DIR%env.bat" (
    call "%PROJECT_DIR%env.bat"
    echo [OK] Da nap cau hinh rieng tu file env.bat
) else (
    echo [!] Chua co file env.bat. He thong se tu dong tao env.bat cho may ban...
    copy "%PROJECT_DIR%env.example.bat" "%PROJECT_DIR%env.bat" >nul
    call "%PROJECT_DIR%env.bat"
)

if not defined APP_NAME set APP_NAME=teamwork-hub

:: 2. TỰ ĐỘNG BÙ ĐƯỜNG DẪN NẾU JAVA_HOME CHƯA KHỚP
if not exist "%JAVA_HOME%\bin\javac.exe" (
    for /f "tokens=*" %%i in ('where javac.exe 2^>nul') do (
        set "JAVAC_PATH=%%i"
        set "JAVA_HOME=!JAVAC_PATH:\bin\javac.exe=!"
    )
)

echo [1/4] Kiem tra Java JDK:
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo [LOI] Khong tim thay JDK tai: "%JAVA_HOME%"
    echo -> Vui long mo file env.bat va sua lai duong dan JAVA_HOME cua may ban!
    pause
    exit /b 1
)
echo       -> Su dung: "%JAVA_HOME%"

:: 3. TỰ ĐỘNG BÙ ĐƯỜNG DẪN NẾU CATALINA_HOME CHƯA KHỚP
echo [2/4] Kiem tra Apache Tomcat 10.1+:
if not exist "%CATALINA_HOME%\bin\catalina.bat" (
    echo [LOI] Khong tim thay Tomcat tai: "%CATALINA_HOME%"
    echo -> Vui long mo file env.bat va sua lai duong dan CATALINA_HOME cua may ban!
    pause
    exit /b 1
)
echo       -> Su dung: "%CATALINA_HOME%"

set DEPLOY_DIR=%CATALINA_HOME%\webapps\%APP_NAME%

:: 4. COPY WEBAPP VÀ RESOURCES VÀO TOMCAT
echo [3/4] Copy Webapp va Resources vao Tomcat...
if exist "%DEPLOY_DIR%" rmdir /S /Q "%DEPLOY_DIR%"
mkdir "%DEPLOY_DIR%\WEB-INF\classes"
mkdir "%DEPLOY_DIR%\WEB-INF\lib"

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
echo       -> Bien dich Java thanh cong 100%%!

:: Xóa cache work của Tomcat
if exist "%CATALINA_HOME%\work\Catalina\localhost\%APP_NAME%" (
    rmdir /S /Q "%CATALINA_HOME%\work\Catalina\localhost\%APP_NAME%" 2>nul
)

:: 6. KHỞI ĐỘNG TOMCAT
echo =========================================================================
echo   UNG DUNG DA SAN SANG TAI: http://localhost:8080/%APP_NAME%/
echo =========================================================================

taskkill /F /IM java.exe 2>nul
timeout /t 1 /nobreak >nul

call "%CATALINA_HOME%\bin\catalina.bat" run
pause
