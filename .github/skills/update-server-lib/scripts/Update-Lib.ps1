<#
.SYNOPSIS
    Decompiles HytaleServer.jar and updates the lib folder with source code and assets.

.DESCRIPTION
    Uses the HytaleModding/patcher tool to decompile the HytaleServer.jar using Vineflower,
    then copies the decompiled source code and server assets to the lib folder.

.PARAMETER ServerVersion
    The version folder name in the extracted directory. If not specified, uses the most recent.

.PARAMETER ExtractDir
    Directory where extracted server files are located.
    Default: C:\hytale-downloader\extracted

.PARAMETER PatcherDir
    Directory where the patcher tool will be cloned/used.
    Default: C:\hytale-downloader\patcher

.PARAMETER LibDir
    Target lib directory in the workspace.
    Default: <WorkspaceRoot>\lib

.PARAMETER SkipDecompile
    Skip decompilation and only copy assets. Useful if decompilation was already done.

.PARAMETER WorkspaceRoot
    Root of the Hyforged workspace. Auto-detected from script location.

.EXAMPLE
    .\Update-Lib.ps1
    Auto-detects the latest version and updates lib.

.EXAMPLE
    .\Update-Lib.ps1 -ServerVersion "2026.01.28-87d03be09"
    Updates lib with the specified version.

.EXAMPLE
    .\Update-Lib.ps1 -SkipDecompile
    Only copies assets without decompiling (if source already exists).
#>

[CmdletBinding()]
param(
    [Parameter()]
    [string]$ServerVersion = "",

    [Parameter()]
    [string]$ExtractDir = "C:\hytale-downloader\extracted",

    [Parameter()]
    [string]$PatcherDir = "C:\hytale-downloader\patcher",

    [Parameter()]
    [string]$LibDir = "",

    [Parameter()]
    [string]$WorkspaceRoot = "",

    [Parameter()]
    [switch]$SkipDecompile
)

$ErrorActionPreference = "Stop"

# Determine workspace root from script location
if (-not $WorkspaceRoot) {
    $scriptPath = $PSScriptRoot
    # Navigate up from .github/skills/update-server-lib/scripts to workspace root
    $WorkspaceRoot = (Get-Item $scriptPath).Parent.Parent.Parent.Parent.FullName
}

if (-not $LibDir) {
    $LibDir = Join-Path $WorkspaceRoot "lib"
}

Write-Host "=== Hytale Server Lib Updater ===" -ForegroundColor Cyan
Write-Host "Workspace: $WorkspaceRoot" -ForegroundColor Gray
Write-Host "Lib Dir:   $LibDir" -ForegroundColor Gray
Write-Host ""

# Find the server version to use
if (-not $ServerVersion) {
    Write-Host "Auto-detecting latest extracted version..." -ForegroundColor Yellow
    $latestFolder = Get-ChildItem -Path $ExtractDir -Directory | 
        Sort-Object Name -Descending | 
        Select-Object -First 1
    
    if (-not $latestFolder) {
        Write-Error "No extracted server versions found in: $ExtractDir"
        exit 1
    }
    
    $ServerVersion = $latestFolder.Name
    Write-Host "Using version: $ServerVersion" -ForegroundColor Green
}

$serverExtractPath = Join-Path $ExtractDir $ServerVersion
if (-not (Test-Path $serverExtractPath)) {
    Write-Error "Server version folder not found: $serverExtractPath"
    exit 1
}

# Find HytaleServer.jar
$hytaleJar = Get-ChildItem -Path $serverExtractPath -Recurse -Filter "HytaleServer.jar" | Select-Object -First 1
if (-not $hytaleJar) {
    Write-Error "HytaleServer.jar not found in: $serverExtractPath"
    exit 1
}
$hytaleJarPath = $hytaleJar.FullName
Write-Host "Found HytaleServer.jar: $hytaleJarPath" -ForegroundColor Gray

# Find Assets folder
$assetsPath = Get-ChildItem -Path $serverExtractPath -Recurse -Directory -Filter "Assets" | 
    Where-Object { Test-Path (Join-Path $_.FullName "Server") } | 
    Select-Object -First 1
if ($assetsPath) {
    $assetsPath = $assetsPath.FullName
    Write-Host "Found Assets: $assetsPath" -ForegroundColor Gray
}

# ============================================
# DECOMPILATION
# ============================================

if (-not $SkipDecompile) {
    Write-Host ""
    Write-Host "=== Setting up Patcher Tool ===" -ForegroundColor Cyan
    
    # Check prerequisites
    Write-Host "Checking prerequisites..." -ForegroundColor Gray
    
    $prereqFailed = $false
    
    # Check Python
    $pythonCmd = Get-Command "py" -ErrorAction SilentlyContinue
    if (-not $pythonCmd) {
        $pythonCmd = Get-Command "python" -ErrorAction SilentlyContinue
    }
    if (-not $pythonCmd) {
        Write-Host "  [FAIL] Python not found" -ForegroundColor Red
        $prereqFailed = $true
    } else {
        Write-Host "  [OK] Python: $($pythonCmd.Source)" -ForegroundColor Green
    }
    
    # Check Java
    $javaVersion = & java --version 2>&1 | Select-Object -First 1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FAIL] Java not found" -ForegroundColor Red
        $prereqFailed = $true
    } else {
        Write-Host "  [OK] Java: $javaVersion" -ForegroundColor Green
    }
    
    # Check Maven
    $mvnVersion = & mvn --version 2>&1 | Select-Object -First 1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FAIL] Maven not found" -ForegroundColor Red
        $prereqFailed = $true
    } else {
        Write-Host "  [OK] Maven: $mvnVersion" -ForegroundColor Green
    }
    
    # Check Git
    $gitVersion = & git --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FAIL] Git not found" -ForegroundColor Red
        $prereqFailed = $true
    } else {
        Write-Host "  [OK] Git: $gitVersion" -ForegroundColor Green
    }
    
    # Check jar command
    $jarVersion = & jar --version 2>&1 | Select-Object -First 1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FAIL] jar command not found (part of JDK)" -ForegroundColor Red
        $prereqFailed = $true
    } else {
        Write-Host "  [OK] jar: $jarVersion" -ForegroundColor Green
    }
    
    if ($prereqFailed) {
        Write-Error "Prerequisites check failed. Please install missing tools."
        exit 1
    }
    
    # Clone or update patcher repo
    if (Test-Path $PatcherDir) {
        Write-Host ""
        Write-Host "Updating patcher repository..." -ForegroundColor Cyan
        Push-Location $PatcherDir
        try {
            & git pull --ff-only
        } catch {
            Write-Host "Warning: Could not update patcher, using existing version" -ForegroundColor Yellow
        }
        Pop-Location
    } else {
        Write-Host ""
        Write-Host "Cloning patcher repository..." -ForegroundColor Cyan
        & git clone "https://github.com/HytaleModding/patcher.git" $PatcherDir
    }
    
    # Setup Python venv
    $venvPath = Join-Path $PatcherDir ".venv"
    $venvPython = Join-Path $venvPath "Scripts\python.exe"
    
    if (-not (Test-Path $venvPython)) {
        Write-Host ""
        Write-Host "Creating Python virtual environment..." -ForegroundColor Cyan
        Push-Location $PatcherDir
        & py -3 -m venv .venv
        Pop-Location
    }
    
    # Install requirements
    Write-Host ""
    Write-Host "Installing Python dependencies..." -ForegroundColor Cyan
    $requirementsPath = Join-Path $PatcherDir "requirements.txt"
    if (Test-Path $requirementsPath) {
        & $venvPython -m pip install -r $requirementsPath --quiet
    }
    
    # Copy HytaleServer.jar to patcher directory
    Write-Host ""
    Write-Host "Copying HytaleServer.jar to patcher..." -ForegroundColor Cyan
    $patcherJar = Join-Path $PatcherDir "HytaleServer.jar"
    Copy-Item -Path $hytaleJarPath -Destination $patcherJar -Force
    
    # Run the patcher setup
    Write-Host ""
    Write-Host "=== Running Decompilation (this may take several minutes) ===" -ForegroundColor Cyan
    Write-Host "Decompiling com.hypixel package using Vineflower..." -ForegroundColor Yellow
    
    Push-Location $PatcherDir
    try {
        # Set environment variable for jar path
        $env:HYTALESERVER_JAR_PATH = $patcherJar
        
        & $venvPython run.py setup 2>&1 | Tee-Object -Variable patcherOutput
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Patcher output:" -ForegroundColor Red
            $patcherOutput | ForEach-Object { Write-Host $_ }
            Write-Error "Decompilation failed with exit code: $LASTEXITCODE"
            exit 1
        }
    } finally {
        Pop-Location
        Remove-Item Env:\HYTALESERVER_JAR_PATH -ErrorAction SilentlyContinue
    }
    
    Write-Host "Decompilation complete!" -ForegroundColor Green
}

# ============================================
# COPY TO LIB
# ============================================

Write-Host ""
Write-Host "=== Updating lib folder ===" -ForegroundColor Cyan

# Create lib directories
$libHytaleServer = Join-Path $LibDir "hytale-server"
$libServerSrc = Join-Path $libHytaleServer "src\main\java"
$libServer = Join-Path $LibDir "Server"
$libUI = Join-Path $LibDir "UI"

# Copy decompiled source
$decompilePath = Join-Path $PatcherDir "hytale-server\src\main\java\com"
if (Test-Path $decompilePath) {
    Write-Host "Copying decompiled source code..." -ForegroundColor Cyan
    
    # Clean existing source
    $existingCom = Join-Path $libServerSrc "com"
    if (Test-Path $existingCom) {
        Write-Host "  Removing existing source..." -ForegroundColor Gray
        Remove-Item -Path $existingCom -Recurse -Force
    }
    
    # Create directory structure
    New-Item -ItemType Directory -Path $libServerSrc -Force | Out-Null
    
    # Copy new source
    Write-Host "  Copying new source..." -ForegroundColor Gray
    Copy-Item -Path $decompilePath -Destination $libServerSrc -Recurse -Force
    
    Write-Host "  Source code copied to: $libServerSrc" -ForegroundColor Green
} else {
    Write-Host "Warning: Decompiled source not found at: $decompilePath" -ForegroundColor Yellow
    Write-Host "  Run without -SkipDecompile to generate source" -ForegroundColor Yellow
}

# Copy HytaleServer.jar
Write-Host ""
Write-Host "Copying HytaleServer.jar..." -ForegroundColor Cyan
$libJar = Join-Path $LibDir "HytaleServer.jar"
Copy-Item -Path $hytaleJarPath -Destination $libJar -Force
Write-Host "  JAR copied to: $libJar" -ForegroundColor Green

# Copy Server assets
if ($assetsPath) {
    $sourceServer = Join-Path $assetsPath "Server"
    if (Test-Path $sourceServer) {
        Write-Host ""
        Write-Host "Copying Server assets..." -ForegroundColor Cyan
        
        if (Test-Path $libServer) {
            Write-Host "  Removing existing Server assets..." -ForegroundColor Gray
            Remove-Item -Path $libServer -Recurse -Force
        }
        
        Copy-Item -Path $sourceServer -Destination $libServer -Recurse -Force
        Write-Host "  Server assets copied to: $libServer" -ForegroundColor Green
    }
    
    # Copy UI assets (from Common/UI or similar)
    $sourceCommon = Join-Path $assetsPath "Common"
    if (Test-Path $sourceCommon) {
        # Look for UI folder
        $sourceUI = Get-ChildItem -Path $sourceCommon -Recurse -Directory -Filter "UI" | Select-Object -First 1
        if (-not $sourceUI) {
            # Try root level
            $sourceUI = Get-ChildItem -Path $assetsPath -Directory -Filter "UI" | Select-Object -First 1
        }
    }
    
    # Also check for UI directly in assets
    if (-not $sourceUI) {
        $sourceUI = Join-Path $assetsPath "UI"
        if (-not (Test-Path $sourceUI)) {
            $sourceUI = $null
        } else {
            $sourceUI = Get-Item $sourceUI
        }
    }
    
    if ($sourceUI) {
        Write-Host ""
        Write-Host "Copying UI assets..." -ForegroundColor Cyan
        
        if (Test-Path $libUI) {
            Write-Host "  Removing existing UI assets..." -ForegroundColor Gray
            Remove-Item -Path $libUI -Recurse -Force
        }
        
        Copy-Item -Path $sourceUI.FullName -Destination $libUI -Recurse -Force
        Write-Host "  UI assets copied to: $libUI" -ForegroundColor Green
    }
}

# Save version info
$versionFile = Join-Path $PSScriptRoot "..\LAST_VERSION.txt"
$ServerVersion | Out-File -FilePath $versionFile -Encoding UTF8 -NoNewline
Write-Host ""
Write-Host "Version saved to: $versionFile" -ForegroundColor Gray

# ============================================
# COMPLETE
# ============================================

Write-Host ""
Write-Host "=== Update Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "Updated to version: $ServerVersion" -ForegroundColor White
Write-Host ""
Write-Host "Lib folder structure:" -ForegroundColor Cyan
Write-Host "  lib/" -ForegroundColor White
Write-Host "    HytaleServer.jar          (original JAR)" -ForegroundColor Gray
Write-Host "    hytale-server/src/main/   (decompiled source)" -ForegroundColor Gray
Write-Host "    Server/                   (server assets)" -ForegroundColor Gray
Write-Host "    UI/                       (UI assets)" -ForegroundColor Gray
Write-Host ""
Write-Host "Remember: Decompiled code may have errors - it's for reference only." -ForegroundColor Yellow
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Review changes with: git diff lib/" -ForegroundColor Gray
Write-Host "  2. Test your plugin with: Build and Deploy Plugin task" -ForegroundColor Gray

return @{
    Version = $ServerVersion
    LibDir = $LibDir
    Success = $true
}
