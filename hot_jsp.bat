@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

:: =========================================================================
::  HOT RELOAD JSP / CSS / JS  —  KHONG restart Tomcat, KHONG mat dang nhap
::
::  Dung khi ban CHI sua file .jsp, .css, .js (KHONG sua file .java).
::  Cach dung:  hot_jsp.bat                 : day toan bo jsp/css/js
::              hot_jsp.bat projects.jsp    : day dung 1 file cho nhanh
::
::  Sau khi chay: chi can F5 tren trinh duyet. Jasper tu bien dich lai JSP.
::  Neu ban sua .java  :  PHAI dung build_and_run.bat (restart Tomcat).
:: =========================================================================

set PROJECT_DIR=%~dp0
set WEBAPP_DIR=%PROJECT_DIR%src\main\webapp

:: 1. Nap cau hinh ca nhan (env.bat nam trong .gitignore)
if exist "%PROJECT_DIR%env.bat" call "%PROJECT_DIR%env.bat"
if not defined APP_NAME set APP_NAME=teamwork-hub
if defined CATALINA_HOME set CATALINA_HOME=%CATALINA_HOME:"=%
if defined APP_NAME set APP_NAME=%APP_NAME:"=%

:: 2. Tu do tim Tomcat neu env.bat chua khai bao
if not exist "%CATALINA_HOME%\bin\catalina.bat" (
    for /d %%t in ("C:\apache-tomcat-10.1*" "C:\apache-tomcat-10*" "%ProgramFiles%\Apache Software Foundation\Tomcat 10.1*" "D:\apache-tomcat-10.1*" "D:\apache-tomcat-10*") do (
        if exist "%%t\bin\catalina.bat" set "CATALINA_HOME=%%t"
    )
)

if not exist "%CATALINA_HOME%\bin\catalina.bat" (
    echo [LOI] Khong tim thay Tomcat. Hay chay build_and_run.bat mot lan de tao env.bat truoc.
    pause
    exit /b 1
)

set DEPLOY_DIR=%CATALINA_HOME%\webapps\%APP_NAME%

if not exist "%DEPLOY_DIR%\WEB-INF" (
    echo [LOI] Ung dung chua duoc deploy tai "%DEPLOY_DIR%".
    echo       Hay chay build_and_run.bat mot lan truoc da.
    pause
    exit /b 1
)

:: 3. Che do 1 file: hot_jsp.bat projects.jsp
if not "%~1"=="" (
    if exist "%WEBAPP_DIR%\%~1" (
        xcopy /Y /Q "%WEBAPP_DIR%\%~1" "%DEPLOY_DIR%\%~1" >nul
        echo [OK] Da day: %~1
        echo      ==^> Bam F5 tren trinh duyet.
        exit /b 0
    )
    :: Thu tim trong thu muc includes
    if exist "%WEBAPP_DIR%\includes\%~1" (
        xcopy /Y /Q "%WEBAPP_DIR%\includes\%~1" "%DEPLOY_DIR%\includes\%~1" >nul
        echo [OK] Da day: includes\%~1
        echo      ==^> Bam F5 tren trinh duyet.
        exit /b 0
    )
    echo [LOI] Khong tim thay file "%~1" trong src\main\webapp
    exit /b 1
)

:: 4. Che do day toan bo View (jsp + css + js + images)
echo [*] Dang day toan bo JSP / CSS / JS vao Tomcat...
for %%e in (jsp css js png jpg svg) do (
    xcopy /Y /S /Q "%WEBAPP_DIR%\*.%%e" "%DEPLOY_DIR%\" >nul 2>nul
)

echo.
echo =========================================================================
echo   [OK] HOT RELOAD XONG — Tomcat VAN DANG CHAY, ban KHONG bi dang xuat.
echo   ==^> Chi can bam F5 tai http://localhost:8080/%APP_NAME%/
echo =========================================================================
