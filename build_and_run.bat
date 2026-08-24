@echo off
SET JAVA_HOME=C:\Users\To Phuong Dat\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.8-hotspot
SET CATALINA_HOME=C:\apache-tomcat-10.1\apache-tomcat-10.1.57
SET APP_NAME=teamwork-hub
SET DEPLOY_DIR=%CATALINA_HOME%\webapps\%APP_NAME%
SET PROJECT_DIR=%~dp0

echo ========================================
echo  BUOC 1: Don dep thu muc deploy cu
echo ========================================
if exist "%DEPLOY_DIR%" rmdir /S /Q "%DEPLOY_DIR%"
mkdir "%DEPLOY_DIR%\WEB-INF\classes"
mkdir "%DEPLOY_DIR%\styles"
mkdir "%DEPLOY_DIR%\js"
mkdir "%DEPLOY_DIR%\includes"

echo.
echo ========================================
echo  BUOC 2: Compile ma nguon Java
echo ========================================
dir /s /b "%PROJECT_DIR%src\main\java\*.java" > "%PROJECT_DIR%sources.txt" 2>nul
findstr /m "java" "%PROJECT_DIR%sources.txt" >nul 2>&1
if %errorlevel% equ 0 (
    "%JAVA_HOME%\bin\javac" -encoding UTF-8 -cp "%CATALINA_HOME%\lib\jakarta.servlet-api.jar;%CATALINA_HOME%\lib\servlet-api.jar" -d "%DEPLOY_DIR%\WEB-INF\classes" @"%PROJECT_DIR%sources.txt"
    if %errorlevel% neq 0 (
        echo [LOI] Compile that bai! Vui long kiem tra code Java.
        del "%PROJECT_DIR%sources.txt"
        pause
        exit /b %errorlevel%
    )
    echo Compile Java thanh cong!
) else (
    echo Chua co file Java nao, bo qua compile.
)
if exist "%PROJECT_DIR%sources.txt" del "%PROJECT_DIR%sources.txt"

echo.
echo ========================================
echo  BUOC 3: Copy Web Resources vao Tomcat
echo ========================================
xcopy /Y /S /Q "%PROJECT_DIR%src\main\webapp\*" "%DEPLOY_DIR%\"

echo.
echo ========================================
echo  BUOC 4: Khoi dong lai Tomcat
echo  URL: http://localhost:8080/%APP_NAME%/
echo ========================================
taskkill /F /IM java.exe 2>nul
timeout /t 2 /nobreak >nul

call "%CATALINA_HOME%\bin\catalina.bat" run

pause
