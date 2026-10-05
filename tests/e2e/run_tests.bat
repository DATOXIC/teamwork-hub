@echo off
rem ==============================================================================
rem TeamWork Hub - E2E Test Suite Runner (Windows Batch Wrapper)
rem Adheres strictly to docs/ai-notes/TEAM_GUARDRAILS.md: UTF-8 encoding (chcp 65001) & zero hardcoded paths.
rem ==============================================================================
chcp 65001 >nul

setlocal enabledelayedexpansion

rem Determine repository root relative to this script directory
set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%..\..\"
pushd "%ROOT_DIR%"

rem Detect Node.js in PATH
where node.exe >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Node.js is required to execute the E2E test suite but was not found in PATH.
    echo Please install Node.js or ensure node.exe is added to PATH.
    popd
    exit /b 1
)

rem Execute the test runner passing all command-line arguments
node "%SCRIPT_DIR%runner.js" %*
set "TEST_EXIT_CODE=%ERRORLEVEL%"

popd
exit /b %TEST_EXIT_CODE%
