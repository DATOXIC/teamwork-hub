@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

echo =========================================================================
echo   TEAMWORK-HUB — SCRIPT TỰ ĐỘNG BUILD VÀ CHẠY DỰ ÁN (DÙNG CHO TẤT CẢ THÀNH VIÊN)
echo =========================================================================

set APP_NAME=teamwork-hub
set PROJECT_DIR=%~dp0

:: 1. TỰ ĐỘNG PHÁT HIỆN JAVA_HOME
if not defined JAVA_HOME (
    for /f "tokens=*" %%i in ('where javac.exe 2^>nul') do (
        set "JAVAC_PATH=%%i"
        set "JAVA_HOME=!JAVAC_PATH:\bin\javac.exe=!"
    )
)

if not defined JAVA_HOME (
    if exist "C:\Users\To Phuong Dat\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.8-hotspot" (
        set "JAVA_HOME=C:\Users\To Phuong Dat\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
    ) else if exist "C:\Program Files\Java\jdk-21" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-21" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21"
    )
)

echo [1/5] Kiem tra Java JDK:
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo [LOI] Khong tim thay JDK tren may cua ban!
    echo Vui long cai dat JDK 17 hoac 21 va set bien moi truong JAVA_HOME.
    pause
    exit /b 1
)
echo       -> Su dung: %JAVA_HOME%

:: 2. TỰ ĐỘNG PHÁT HIỆN CATALINA_HOME (TOMCAT)
if not defined CATALINA_HOME (
    if exist "C:\apache-tomcat-10.1\apache-tomcat-10.1.57" (
        set "CATALINA_HOME=C:\apache-tomcat-10.1\apache-tomcat-10.1.57"
    ) else if exist "C:\apache-tomcat-10.1" (
        set "CATALINA_HOME=C:\apache-tomcat-10.1"
    ) else if exist "C:\Program Files\Apache Software Foundation\Tomcat 10.1" (
        set "CATALINA_HOME=C:\Program Files\Apache Software Foundation\Tomcat 10.1"
    )
)

echo [2/5] Kiem tra Apache Tomcat 10.1+:
if not exist "%CATALINA_HOME%\bin\catalina.bat" (
    echo [LOI] Khong tim thay thu muc Tomcat 10.1!
    echo Vui long dat bien moi truong CATALINA_HOME tro den thu muc cai dat Tomcat cua ban.
    pause
    exit /b 1
)
echo       -> Su dung: %CATALINA_HOME%

set DEPLOY_DIR=%CATALINA_HOME%\webapps\%APP_NAME%

:: 3. COPY WEBAPP VA RESOURCES SANG TOMCAT
echo [3/5] Don dep va copy Webapp vao Tomcat...
if exist "%DEPLOY_DIR%" rmdir /S /Q "%DEPLOY_DIR%"
mkdir "%DEPLOY_DIR%\WEB-INF\classes"
mkdir "%DEPLOY_DIR%\WEB-INF\lib"

xcopy /Y /S /Q "%PROJECT_DIR%src\main\webapp\*" "%DEPLOY_DIR%\" >nul

if exist "%PROJECT_DIR%src\main\resources" (
    xcopy /Y /S /Q "%PROJECT_DIR%src\main\resources\*" "%DEPLOY_DIR%\WEB-INF\classes\" >nul
)

:: 4. BIÊN DỊCH TOÀN BỘ CODE JAVA
echo [4/5] Bien dich ma nguon Java (MVC, Servlet, DAO, Model)...
dir /s /b "%PROJECT_DIR%src\main\java\*.java" > "%PROJECT_DIR%sources.txt" 2>nul

set "CP=%CATALINA_HOME%\lib\*;%DEPLOY_DIR%\WEB-INF\lib\*;%DEPLOY_DIR%\WEB-INF\classes"

"%JAVA_HOME%\bin\javac" -encoding UTF-8 -cp "%CP%" -d "%DEPLOY_DIR%\WEB-INF\classes" @"%PROJECT_DIR%sources.txt"
if %errorlevel% neq 0 (
    echo [LOI] Bien dich Java that bai! Vui long kiem tra loi code o tren.
    if exist "%PROJECT_DIR%sources.txt" del "%PROJECT_DIR%sources.txt"
    pause
    exit /b %errorlevel%
)
if exist "%PROJECT_DIR%sources.txt" del "%PROJECT_DIR%sources.txt"
echo       -> Bien dich Java thanh cong 100%%!

:: Xóa cache work của Tomcat để nạp mới JSP
if exist "%CATALINA_HOME%\work\Catalina\localhost\%APP_NAME%" (
    rmdir /S /Q "%CATALINA_HOME%\work\Catalina\localhost\%APP_NAME%" 2>nul
)

:: 5. KHỞI ĐỘNG TOMCAT
echo [5/5] Khoi dong Apache Tomcat...
echo =========================================================================
echo   UN G DUNG DA SAN SANG TAI: http://localhost:8080/%APP_NAME%/
echo =========================================================================

taskkill /F /IM java.exe 2>nul
timeout /t 1 /nobreak >nul

call "%CATALINA_HOME%\bin\catalina.bat" run
pause
