@echo off
setlocal enabledelayedexpansion

REM ============================================
REM Hytale Server Lib Updater
REM Decompiles and updates lib folder
REM ============================================

set "EXTRACT_DIR=C:\hytale-downloader\extracted"
set "PATCHER_DIR=C:\hytale-downloader\patcher"
set "DOWNLOAD_DIR=C:\hytale-downloader\downloads"

REM Get workspace root (4 levels up from script location)
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%\..\..\..\..\") do set "WORKSPACE_ROOT=%%~fI"
set "LIB_DIR=%WORKSPACE_ROOT%lib"

echo ============================================
echo   Hytale Server Lib Updater
echo ============================================
echo.
echo Workspace: %WORKSPACE_ROOT%
echo Lib Dir:   %LIB_DIR%
echo.

REM Get server version - either from argument or latest
set "SERVER_VERSION=%~1"
if "%SERVER_VERSION%"=="" (
    if exist "%DOWNLOAD_DIR%\LATEST_VERSION.txt" (
        set /p SERVER_VERSION=<"%DOWNLOAD_DIR%\LATEST_VERSION.txt"
        REM Trim whitespace
        for /f "tokens=* delims= " %%a in ("!SERVER_VERSION!") do set "SERVER_VERSION=%%a"
    )
)

if "%SERVER_VERSION%"=="" (
    REM Find latest folder in extract dir
    for /f "tokens=*" %%d in ('dir /b /ad /o-n "%EXTRACT_DIR%" 2^>nul') do (
        set "SERVER_VERSION=%%d"
        goto :found_version
    )
)
:found_version

if "%SERVER_VERSION%"=="" (
    echo ERROR: No server version found. Run Download-Server.cmd first.
    exit /b 1
)

set "SERVER_EXTRACT_PATH=%EXTRACT_DIR%\%SERVER_VERSION%"
if not exist "%SERVER_EXTRACT_PATH%" (
    echo ERROR: Server version folder not found: %SERVER_EXTRACT_PATH%
    exit /b 1
)

echo Using version: %SERVER_VERSION%
echo.

REM Find HytaleServer.jar
set "HYTALE_JAR="
for /r "%SERVER_EXTRACT_PATH%" %%f in (HytaleServer.jar) do (
    if exist "%%f" set "HYTALE_JAR=%%f"
)

if not defined HYTALE_JAR (
    echo ERROR: HytaleServer.jar not found in: %SERVER_EXTRACT_PATH%
    exit /b 1
)

echo Found HytaleServer.jar: %HYTALE_JAR%

REM Find Assets folder
set "ASSETS_PATH="
for /r "%SERVER_EXTRACT_PATH%" /d %%d in (Assets) do (
    if exist "%%d\Server" set "ASSETS_PATH=%%d"
)

if defined ASSETS_PATH (
    echo Found Assets: %ASSETS_PATH%
)

echo.
echo ============================================
echo   Checking Prerequisites
echo ============================================
echo.

REM Check Python
where py >nul 2>nul
if %ERRORLEVEL% neq 0 (
    where python >nul 2>nul
    if %ERRORLEVEL% neq 0 (
        echo [FAIL] Python not found
        set "PREREQ_FAIL=1"
    ) else (
        echo [OK] Python found
        set "PYTHON_CMD=python"
    )
) else (
    echo [OK] Python found
    set "PYTHON_CMD=py -3"
)

REM Check Java
java --version >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [FAIL] Java not found
    set "PREREQ_FAIL=1"
) else (
    echo [OK] Java found
)

REM Check Maven
mvn --version >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [FAIL] Maven not found
    set "PREREQ_FAIL=1"
) else (
    echo [OK] Maven found
)

REM Check Git
git --version >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [FAIL] Git not found
    set "PREREQ_FAIL=1"
) else (
    echo [OK] Git found
)

REM Check jar command
jar --version >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [FAIL] jar command not found
    set "PREREQ_FAIL=1"
) else (
    echo [OK] jar found
)

if defined PREREQ_FAIL (
    echo.
    echo ERROR: Prerequisites check failed. Please install missing tools.
    exit /b 1
)

echo.
echo ============================================
echo   Setting up Patcher Tool
echo ============================================
echo.

REM Clone or update patcher repo
if exist "%PATCHER_DIR%" (
    echo Updating patcher repository...
    pushd "%PATCHER_DIR%"
    git pull --ff-only
    popd
) else (
    echo Cloning patcher repository...
    git clone "https://github.com/HytaleModding/patcher.git" "%PATCHER_DIR%"
)

REM Setup Python venv
set "VENV_PATH=%PATCHER_DIR%\.venv"
set "VENV_PYTHON=%VENV_PATH%\Scripts\python.exe"

if not exist "%VENV_PYTHON%" (
    echo.
    echo Creating Python virtual environment...
    pushd "%PATCHER_DIR%"
    %PYTHON_CMD% -m venv .venv
    popd
)

REM Install requirements
echo.
echo Installing Python dependencies...
if exist "%PATCHER_DIR%\requirements.txt" (
    "%VENV_PYTHON%" -m pip install -r "%PATCHER_DIR%\requirements.txt" --quiet
)

REM Copy HytaleServer.jar to patcher directory
echo.
echo Copying HytaleServer.jar to patcher...
copy /y "%HYTALE_JAR%" "%PATCHER_DIR%\HytaleServer.jar" >nul

echo.
echo ============================================
echo   Running Decompilation
echo ============================================
echo.
echo This may take several minutes...
echo Decompiling com.hypixel package using Vineflower...
echo.

pushd "%PATCHER_DIR%"
set "HYTALESERVER_JAR_PATH=%PATCHER_DIR%\HytaleServer.jar"
"%VENV_PYTHON%" run.py setup
set "DECOMPILE_RESULT=%ERRORLEVEL%"
popd

if %DECOMPILE_RESULT% neq 0 (
    echo.
    echo ERROR: Decompilation failed with exit code: %DECOMPILE_RESULT%
    exit /b 1
)

echo.
echo Decompilation complete!

echo.
echo ============================================
echo   Updating lib folder
echo ============================================
echo.

REM Copy decompiled source
set "DECOMPILE_PATH=%PATCHER_DIR%\hytale-server\src\main\java\com"
set "LIB_SERVER_SRC=%LIB_DIR%\hytale-server\src\main\java"

if exist "%DECOMPILE_PATH%" (
    echo Copying decompiled source code...
    
    if exist "%LIB_SERVER_SRC%\com" (
        echo   Removing existing source...
        rmdir /s /q "%LIB_SERVER_SRC%\com"
    )
    
    if not exist "%LIB_SERVER_SRC%" mkdir "%LIB_SERVER_SRC%"
    
    echo   Copying new source...
    xcopy /s /e /i /q "%DECOMPILE_PATH%" "%LIB_SERVER_SRC%\com" >nul
    
    echo   Source code copied to: %LIB_SERVER_SRC%
) else (
    echo Warning: Decompiled source not found at: %DECOMPILE_PATH%
)

REM Copy HytaleServer.jar
echo.
echo Copying HytaleServer.jar...
copy /y "%HYTALE_JAR%" "%LIB_DIR%\HytaleServer.jar" >nul
echo   JAR copied to: %LIB_DIR%\HytaleServer.jar

REM Copy Server assets
if defined ASSETS_PATH (
    set "SOURCE_SERVER=%ASSETS_PATH%\Server"
    if exist "!SOURCE_SERVER!" (
        echo.
        echo Copying Server assets...
        
        if exist "%LIB_DIR%\Server" (
            echo   Removing existing Server assets...
            rmdir /s /q "%LIB_DIR%\Server"
        )
        
        xcopy /s /e /i /q "!SOURCE_SERVER!" "%LIB_DIR%\Server" >nul
        echo   Server assets copied to: %LIB_DIR%\Server
    )
    
    REM Copy UI assets
    set "SOURCE_COMMON=%ASSETS_PATH%\Common"
    if exist "!SOURCE_COMMON!" (
        for /d %%u in ("!SOURCE_COMMON!\UI") do (
            if exist "%%u" (
                echo.
                echo Copying UI assets...
                
                if exist "%LIB_DIR%\UI" (
                    echo   Removing existing UI assets...
                    rmdir /s /q "%LIB_DIR%\UI"
                )
                
                xcopy /s /e /i /q "%%u" "%LIB_DIR%\UI" >nul
                echo   UI assets copied to: %LIB_DIR%\UI
            )
        )
    )
)

REM Save version info
echo %SERVER_VERSION%> "%SCRIPT_DIR%..\LAST_VERSION.txt"

echo.
echo ============================================
echo   Update Complete
echo ============================================
echo.
echo Updated to version: %SERVER_VERSION%
echo.
echo Lib folder structure:
echo   lib/
echo     HytaleServer.jar          (original JAR)
echo     hytale-server/src/main/   (decompiled source)
echo     Server/                   (server assets)
echo     UI/                       (UI assets)
echo.
echo Remember: Decompiled code may have errors - it's for reference only.
echo.
echo Next steps:
echo   1. Review changes with: git diff lib/
echo   2. Test your plugin with: Build and Deploy Plugin task
echo.

endlocal
