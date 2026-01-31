---
name: update-server-lib
description: Updates the Hytale server reference files in lib/ by downloading the latest pre-release server, decompiling the JAR using Vineflower, and updating server assets. Use when needing to update to a new Hytale server version, refreshing decompiled source code, or syncing with the latest pre-release. Triggers - update server, download server, decompile jar, vineflower, update lib, new server version, sync server, refresh server.
---

# Update Server Lib Skill

Updates the `lib/` folder with the latest Hytale pre-release server files including decompiled source code and server assets.

## Prerequisites

Before running these scripts, ensure the following are installed and on PATH:

- **Hytale Downloader**: Located at `C:\hytale-downloader\hytale-downloader-windows-amd64.exe` (already authenticated)
- **Python 3.13+**: For running the patcher tool
- **Java 25+**: `java --version` should show 25.x
- **Maven**: `mvn --version` should work
- **Git**: `git --version` should work
- **7-Zip** (optional): For faster zip extraction, falls back to Expand-Archive

## Directory Structure

```
C:\hytale-downloader\
├── hytale-downloader-windows-amd64.exe
├── .hytale-downloader-credentials.json
├── downloads\                              # Created by script
│   └── <version>.zip                       # Downloaded server package
└── extracted\                              # Created by script
    └── <version>\                          # Extracted server files
        ├── Server\
        │   └── HytaleServer.jar
        └── Assets\
            ├── Common\
            ├── Cosmetics\
            └── Server\
```

## Usage

Run the scripts in order from the workspace root (`c:\Users\JBurl\source\repos\Reign-software\hyforged`):

### Step 1: Download and Extract Latest Pre-Release

```powershell
.\.github\skills\update-server-lib\scripts\Download-Server.ps1
```

This script:
- Downloads the latest pre-release server using the Hytale downloader
- Extracts the server zip file
- Extracts the Assets.zip within it
- Outputs the version and paths for the next step

### Step 2: Decompile and Update Lib

```powershell
.\.github\skills\update-server-lib\scripts\Update-Lib.ps1 -ServerVersion "<version>"
```

Or let it auto-detect the latest downloaded version:

```powershell
.\.github\skills\update-server-lib\scripts\Update-Lib.ps1
```

This script:
- Clones/updates the HytaleModding/patcher tool
- Sets up Python virtual environment
- Runs Vineflower decompilation on HytaleServer.jar
- Copies decompiled source to `lib/hytale-server/src/main/java`
- Copies Server assets to `lib/Server`
- Copies UI assets to `lib/UI`
- Updates HytaleServer.jar in lib root

### Full Update (Both Steps)

```powershell
.\.github\skills\update-server-lib\scripts\Full-Update.ps1
```

## Script Details

### Download-Server.ps1 Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `-DownloaderPath` | `C:\hytale-downloader` | Path to hytale-downloader folder |
| `-DownloadDir` | `<DownloaderPath>\downloads` | Where to save downloaded zips |
| `-ExtractDir` | `<DownloaderPath>\extracted` | Where to extract server files |
| `-Patchline` | `pre-release` | Patchline to download from |

### Update-Lib.ps1 Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `-ServerVersion` | (auto-detect) | Version folder name in extracted dir |
| `-ExtractDir` | `C:\hytale-downloader\extracted` | Where extracted server files are |
| `-PatcherDir` | `C:\hytale-downloader\patcher` | Where to clone/use patcher tool |
| `-LibDir` | `<WorkspaceRoot>\lib` | Target lib directory |
| `-SkipDecompile` | `$false` | Skip decompilation, only copy assets |

## Troubleshooting

### Authentication Errors
If you get 401 or authentication errors, delete `C:\hytale-downloader\.hytale-downloader-credentials.json` and run the downloader manually to re-authenticate.

### Decompilation Fails
- Ensure Python 3.13+ is installed: `py -3.13 --version`
- Ensure Java 25 is on PATH: `java --version`
- Ensure Maven is on PATH: `mvn --version`
- Check the patcher output for specific errors

### Incomplete Extraction
If extraction fails, delete the partially extracted folder and run again.

## Version Tracking

After a successful update, the script creates `.github/skills/update-server-lib/LAST_VERSION.txt` with the downloaded version for reference.

## Notes

- The decompiled code may have compilation errors - this is expected. It's for reference/exploration only.
- Server assets in `lib/Server` are read-only references; don't modify them directly.
- UI assets in `lib/UI` are for reference when building custom UIs.
- Always test your plugin after updating to ensure compatibility with the new server version.
