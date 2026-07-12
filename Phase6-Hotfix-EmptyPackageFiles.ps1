param(
    [string]$ProjectRoot = (Get-Location).Path
)

$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
if (-not (Test-Path $settingsFile)) {
    throw "Could not find settings.gradle.kts in: $ProjectRoot`nRun this script from the ProjectLedger root folder."
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase6-empty-package-files-$timestamp"
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

$files = Get-ChildItem -Path $ProjectRoot -Recurse -File -Filter "package-info.kt" |
    Where-Object {
        $_.FullName -notlike "*\.gradle\*" -and
        $_.FullName -notlike "*\build\*" -and
        [string]::IsNullOrWhiteSpace([System.IO.File]::ReadAllText($_.FullName))
    }

if (-not $files -or $files.Count -eq 0) {
    Write-Host ""
    Write-Host "No empty package-info.kt files were found." -ForegroundColor Yellow
    Write-Host "Nothing was changed."
    exit 0
}

foreach ($file in $files) {
    $relative = $file.FullName.Substring($ProjectRoot.Length).TrimStart('\')
    $backupFile = Join-Path $backupRoot $relative
    $backupDirectory = Split-Path $backupFile -Parent

    New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
    Copy-Item $file.FullName $backupFile -Force
    Remove-Item $file.FullName -Force

    Write-Host "Removed empty placeholder: $relative" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "Phase 6 empty-file hotfix applied successfully." -ForegroundColor Green
Write-Host "Removed files: $($files.Count)"
Write-Host "Backup folder: $backupRoot"
Write-Host ""
Write-Host "Now rerun:"
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase6.ps1 -InstallOnPhone" -ForegroundColor Yellow
