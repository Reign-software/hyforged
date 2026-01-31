<#
.SYNOPSIS
    Downloads and extracts the latest Hytale pre-release server files.

.DESCRIPTION
    Uses the Hytale downloader CLI to download the latest pre-release server,
    then extracts both the main zip and the Assets.zip within it.

.PARAMETER DownloaderPath
    Path to the hytale-downloader folder containing the executable.
    Default: C:\hytale-downloader

.PARAMETER DownloadDir
    Directory where downloaded zip files will be saved.
    Default: <DownloaderPath>\downloads

.PARAMETER ExtractDir
    Directory where server files will be extracted.
    Default: <DownloaderPath>\extracted

.PARAMETER Patchline
    The patchline to download from. Use 'pre-release' for plugin development.
    Default: pre-release

.EXAMPLE
    .\Download-Server.ps1
    Downloads and extracts the latest pre-release server.

.EXAMPLE
    .\Download-Server.ps1 -Patchline release
    Downloads and extracts the latest release server.
#>

[CmdletBinding()]
param(
    [Parameter()]
    [string]$DownloaderPath = "C:\hytale-downloader",

    [Parameter()]
    [string]$DownloadDir = "",

    [Parameter()]
    [string]$ExtractDir = "",

    [Parameter()]
    [string]$Patchline = "pre-release"
)

$ErrorActionPreference = "Stop"

# Set default directories relative to downloader path
if (-not $DownloadDir) {
    $DownloadDir = Join-Path $DownloaderPath "downloads"
}
if (-not $ExtractDir) {
    $ExtractDir = Join-Path $DownloaderPath "extracted"
}

# Ensure directories exist
New-Item -ItemType Directory -Path $DownloadDir -Force | Out-Null
New-Item -ItemType Directory -Path $ExtractDir -Force | Out-Null

$DownloaderExe = Join-Path $DownloaderPath "hytale-downloader-windows-amd64.exe"

if (-not (Test-Path $DownloaderExe)) {
    Write-Error "Hytale downloader not found at: $DownloaderExe"
    exit 1
}

Write-Host "=== Hytale Server Downloader ===" -ForegroundColor Cyan
Write-Host "Patchline: $Patchline" -ForegroundColor Yellow
Write-Host ""

# Get the current version first
Write-Host "Checking current version..." -ForegroundColor Cyan
$versionOutput = & $DownloaderExe -patchline $Patchline -print-version -skip-update-check 2>&1
$versionLine = $versionOutput | Select-String -Pattern "^\d{4}\.\d{2}\.\d{2}-[a-f0-9]+" | Select-Object -First 1
if ($versionLine) {
    $ServerVersion = $versionLine.Matches[0].Value
    Write-Host "Server version: $ServerVersion" -ForegroundColor Green
} else {
    # Try to extract from download output instead
    Write-Host "Could not parse version from print-version output, will extract from download..." -ForegroundColor Yellow
    $ServerVersion = $null
}

# Define download path with timestamp to avoid collisions
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$downloadZip = Join-Path $DownloadDir "server-$Patchline-$timestamp.zip"

Write-Host ""
Write-Host "Downloading server package..." -ForegroundColor Cyan
Write-Host "Download path: $downloadZip" -ForegroundColor Gray

& $DownloaderExe -patchline $Patchline -download-path $downloadZip -skip-update-check

if (-not (Test-Path $downloadZip)) {
    Write-Error "Download failed - zip file not found at: $downloadZip"
    exit 1
}

Write-Host "Download complete!" -ForegroundColor Green

# Try to determine version from the zip contents if we didn't get it earlier
if (-not $ServerVersion) {
    # Use a pattern based on typical Hytale version format
    $zipInfo = Get-Item $downloadZip
    # Extract first to get manifest or folder name
    $tempExtract = Join-Path $env:TEMP "hytale-version-check-$timestamp"
    try {
        Expand-Archive -Path $downloadZip -DestinationPath $tempExtract -Force
        $manifestPath = Get-ChildItem -Path $tempExtract -Recurse -Filter "manifest.json" | Select-Object -First 1
        if ($manifestPath) {
            $manifest = Get-Content $manifestPath.FullName | ConvertFrom-Json
            $ServerVersion = $manifest.version
        }
    } catch {
        # Fallback to timestamp-based version
        $ServerVersion = $timestamp
    } finally {
        if (Test-Path $tempExtract) {
            Remove-Item -Path $tempExtract -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
    
    if (-not $ServerVersion) {
        $ServerVersion = $timestamp
    }
    Write-Host "Detected version: $ServerVersion" -ForegroundColor Yellow
}

# Rename the download to include version
$versionedZip = Join-Path $DownloadDir "$ServerVersion.zip"
if ($downloadZip -ne $versionedZip) {
    if (Test-Path $versionedZip) {
        Write-Host "Removing existing zip: $versionedZip" -ForegroundColor Yellow
        Remove-Item $versionedZip -Force
    }
    Move-Item $downloadZip $versionedZip -Force
    $downloadZip = $versionedZip
}

# Extract the main server zip
$serverExtractPath = Join-Path $ExtractDir $ServerVersion
if (Test-Path $serverExtractPath) {
    Write-Host "Removing existing extracted folder: $serverExtractPath" -ForegroundColor Yellow
    Remove-Item -Path $serverExtractPath -Recurse -Force
}

Write-Host ""
Write-Host "Extracting server package to: $serverExtractPath" -ForegroundColor Cyan

# Check for 7-Zip for faster extraction
$sevenZip = Get-Command "7z" -ErrorAction SilentlyContinue
if ($sevenZip) {
    Write-Host "Using 7-Zip for extraction..." -ForegroundColor Gray
    & 7z x $downloadZip -o"$serverExtractPath" -y | Out-Null
} else {
    Write-Host "Using Expand-Archive (7-Zip not found, may be slower)..." -ForegroundColor Gray
    Expand-Archive -Path $downloadZip -DestinationPath $serverExtractPath -Force
}

Write-Host "Main package extracted!" -ForegroundColor Green

# Find and extract Assets.zip
$assetsZip = Get-ChildItem -Path $serverExtractPath -Recurse -Filter "Assets.zip" | Select-Object -First 1
if ($assetsZip) {
    Write-Host ""
    Write-Host "Extracting Assets.zip..." -ForegroundColor Cyan
    
    $assetsExtractPath = Join-Path $assetsZip.DirectoryName "Assets"
    if (Test-Path $assetsExtractPath) {
        Remove-Item -Path $assetsExtractPath -Recurse -Force
    }
    
    if ($sevenZip) {
        & 7z x $assetsZip.FullName -o"$assetsExtractPath" -y | Out-Null
    } else {
        Expand-Archive -Path $assetsZip.FullName -DestinationPath $assetsExtractPath -Force
    }
    
    Write-Host "Assets extracted!" -ForegroundColor Green
} else {
    Write-Host "Warning: Assets.zip not found in extracted files" -ForegroundColor Yellow
}

# Find key paths
$serverFolder = Get-ChildItem -Path $serverExtractPath -Recurse -Directory -Filter "Server" | 
    Where-Object { Test-Path (Join-Path $_.FullName "HytaleServer.jar") } | 
    Select-Object -First 1
$hytaleJar = if ($serverFolder) { Join-Path $serverFolder.FullName "HytaleServer.jar" } else { $null }

# Also check root for HytaleServer.jar
if (-not $hytaleJar -or -not (Test-Path $hytaleJar)) {
    $hytaleJar = Get-ChildItem -Path $serverExtractPath -Recurse -Filter "HytaleServer.jar" | Select-Object -First 1
    if ($hytaleJar) { $hytaleJar = $hytaleJar.FullName }
}

Write-Host ""
Write-Host "=== Download Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "Version:        $ServerVersion" -ForegroundColor White
Write-Host "Extracted to:   $serverExtractPath" -ForegroundColor White
if ($hytaleJar -and (Test-Path $hytaleJar)) {
    Write-Host "HytaleServer:   $hytaleJar" -ForegroundColor White
}
if ($assetsExtractPath -and (Test-Path $assetsExtractPath)) {
    Write-Host "Assets:         $assetsExtractPath" -ForegroundColor White
}
Write-Host ""
Write-Host "Next step: Run Update-Lib.ps1 to decompile and update lib folder" -ForegroundColor Cyan
Write-Host "  .\.github\skills\update-server-lib\scripts\Update-Lib.ps1 -ServerVersion `"$ServerVersion`"" -ForegroundColor Gray

# Output for scripting
return @{
    Version = $ServerVersion
    ExtractPath = $serverExtractPath
    HytaleJar = $hytaleJar
    AssetsPath = $assetsExtractPath
}
