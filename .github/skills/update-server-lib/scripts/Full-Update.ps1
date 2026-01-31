<#
.SYNOPSIS
    Full update: downloads the latest server and updates the lib folder in one step.

.DESCRIPTION
    Combines Download-Server.ps1 and Update-Lib.ps1 into a single operation.
    Downloads the latest pre-release Hytale server, decompiles it, and updates
    the lib folder with source code and assets.

.PARAMETER DownloaderPath
    Path to the hytale-downloader folder.
    Default: C:\hytale-downloader

.PARAMETER Patchline
    The patchline to download from.
    Default: pre-release

.PARAMETER SkipDecompile
    Skip decompilation and only copy assets.

.EXAMPLE
    .\Full-Update.ps1
    Downloads latest pre-release and fully updates lib.

.EXAMPLE
    .\Full-Update.ps1 -SkipDecompile
    Downloads latest pre-release but skips decompilation (faster, assets only).
#>

[CmdletBinding()]
param(
    [Parameter()]
    [string]$DownloaderPath = "C:\hytale-downloader",

    [Parameter()]
    [string]$Patchline = "pre-release",

    [Parameter()]
    [switch]$SkipDecompile
)

$ErrorActionPreference = "Stop"
$scriptDir = $PSScriptRoot

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Hytale Server Full Update" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Download
Write-Host ">>> Step 1/2: Downloading server..." -ForegroundColor Magenta
Write-Host ""

$downloadScript = Join-Path $scriptDir "Download-Server.ps1"
$downloadResult = & $downloadScript -DownloaderPath $DownloaderPath -Patchline $Patchline

if (-not $downloadResult -or -not $downloadResult.Version) {
    Write-Error "Download step failed"
    exit 1
}

$serverVersion = $downloadResult.Version
Write-Host ""
Write-Host "Download complete. Version: $serverVersion" -ForegroundColor Green
Write-Host ""

# Step 2: Update lib
Write-Host ">>> Step 2/2: Updating lib folder..." -ForegroundColor Magenta
Write-Host ""

$updateScript = Join-Path $scriptDir "Update-Lib.ps1"
$updateParams = @{
    ServerVersion = $serverVersion
    ExtractDir = Join-Path $DownloaderPath "extracted"
    PatcherDir = Join-Path $DownloaderPath "patcher"
}

if ($SkipDecompile) {
    $updateParams.SkipDecompile = $true
}

$updateResult = & $updateScript @updateParams

if (-not $updateResult -or -not $updateResult.Success) {
    Write-Error "Update step failed"
    exit 1
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  Full Update Complete!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "Version: $serverVersion" -ForegroundColor White
Write-Host ""
Write-Host "Your lib folder is now updated with:" -ForegroundColor Cyan
Write-Host "  - Latest HytaleServer.jar" -ForegroundColor White
if (-not $SkipDecompile) {
    Write-Host "  - Decompiled source code (for reference)" -ForegroundColor White
}
Write-Host "  - Server assets" -ForegroundColor White
Write-Host "  - UI assets" -ForegroundColor White
Write-Host ""
Write-Host "Run 'Build and Deploy Plugin' task to test your plugin!" -ForegroundColor Yellow

return @{
    Version = $serverVersion
    Success = $true
}
