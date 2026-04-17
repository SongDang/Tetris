param(
    [string]$Version = "1.0",
    [switch]$SkipClean,
    [string]$WixBin = ""
)

$ErrorActionPreference = "Stop"
# powershell -ExecutionPolicy Bypass -File .\build-installer.ps1 -Version 1.2 -SkipClean
function Add-WixToPathIfNeeded {
    if (-not [string]::IsNullOrWhiteSpace($WixBin) -and (Test-Path $WixBin)) {
        $env:PATH = "$WixBin;$env:PATH"
    }

    $hasCandle = Get-Command "candle" -ErrorAction SilentlyContinue
    $hasLight = Get-Command "light" -ErrorAction SilentlyContinue
    if ($hasCandle -and $hasLight) {
        return
    }

    $programFilesX86 = [Environment]::GetFolderPath("ProgramFilesX86")
    $candidates = @(
        (Join-Path $env:ProgramFiles "WiX Toolset v3.14\bin"),
        (Join-Path $env:ProgramFiles "WiX Toolset v3.11\bin"),
        (Join-Path $env:ProgramFiles "WiX Toolset v3.10\bin")
    )

    if (-not [string]::IsNullOrWhiteSpace($programFilesX86)) {
        $candidates += @(
            (Join-Path $programFilesX86 "WiX Toolset v3.14\bin"),
            (Join-Path $programFilesX86 "WiX Toolset v3.11\bin"),
            (Join-Path $programFilesX86 "WiX Toolset v3.10\bin")
        )
    }

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            $env:PATH = "$candidate;$env:PATH"
            break
        }
    }

    $hasCandle = Get-Command "candle" -ErrorAction SilentlyContinue
    $hasLight = Get-Command "light" -ErrorAction SilentlyContinue
    if (-not ($hasCandle -and $hasLight)) {
        throw "WiX not found. Pass -WixBin with your WiX bin path, for example: -WixBin 'C:\Program Files\WiX Toolset v3.14\bin'"
    }
}

function Assert-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $Name"
    }
}

Assert-Command "mvn"
Assert-Command "jpackage"
Add-WixToPathIfNeeded

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $projectRoot

try {
    Write-Host "Building runtime image..."
    if ($SkipClean) {
        mvn javafx:jlink
    }
    else {
        mvn clean javafx:jlink
    }

    if (-not (Test-Path "target/app")) {
        throw "Runtime image not found at target/app"
    }

    New-Item -ItemType Directory -Force -Path "target/jpackage-out" | Out-Null
    New-Item -ItemType Directory -Force -Path "target/installer" | Out-Null

    if (Test-Path "target/jpackage-out/Tetris") {
        Remove-Item "target/jpackage-out/Tetris" -Recurse -Force
    }

    $installerPath = Join-Path $projectRoot "target/installer/Tetris-$Version.exe"
    if (Test-Path $installerPath) {
        try {
            Remove-Item $installerPath -Force
        }
        catch {
            Write-Warning "Could not remove existing installer file. Close it if open, or use another -Version value."
        }
    }

    Write-Host "Creating app-image..."
    jpackage --type app-image --name Tetris --app-version $Version --runtime-image target/app --module-path target/classes --module com.se330.tetris/com.se330.tetris.core.TetrisApp --dest target/jpackage-out

    Write-Host "Creating installer exe..."
    jpackage --type exe --name Tetris --app-version $Version --app-image target/jpackage-out/Tetris --dest target/installer --win-menu --win-shortcut

    if (Test-Path $installerPath) {
        Write-Host "Installer ready: $installerPath"
    }
    else {
        Write-Host "Installer created, but expected filename differs. Available files:"
        Get-ChildItem "target/installer" -Filter "*.exe" | Select-Object FullName
    }
}
finally {
    Pop-Location
}
