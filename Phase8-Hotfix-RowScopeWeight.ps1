[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase8-weight-import-$timestamp"
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

$sourceFiles = Get-ChildItem -Path $ProjectRoot -Recurse -File -Filter "*.kt" |
    Where-Object {
        $_.FullName -notlike "*\.gradle\*" -and
        $_.FullName -notlike "*\build\*" -and
        $_.FullName -notlike "*\.phase-backups\*" -and
        $_.FullName -notlike "*\phase-*-update\*"
    }

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$changedFiles = New-Object System.Collections.Generic.List[string]

foreach ($file in $sourceFiles) {
    $lines = [System.IO.File]::ReadAllLines($file.FullName)
    $filtered = @(
        $lines | Where-Object {
            $_.Trim() -ne "import androidx.compose.foundation.layout.weight"
        }
    )

    if ($filtered.Count -ne $lines.Count) {
        $relative = $file.FullName.Substring($ProjectRoot.Length).TrimStart('\')
        $backupFile = Join-Path $backupRoot $relative

        New-Item -ItemType Directory -Path (Split-Path $backupFile -Parent) -Force | Out-Null
        Copy-Item -LiteralPath $file.FullName -Destination $backupFile -Force

        [System.IO.File]::WriteAllLines($file.FullName, $filtered, $utf8NoBom)
        $changedFiles.Add($relative) | Out-Null
        Write-Host "Removed scoped weight import: $relative" -ForegroundColor Cyan
    }
}

if ($changedFiles.Count -eq 0) {
    throw "No explicit androidx.compose.foundation.layout.weight import was found."
}

$remainingImports = Get-ChildItem -Path $ProjectRoot -Recurse -File -Filter "*.kt" |
    Where-Object {
        $_.FullName -notlike "*\.gradle\*" -and
        $_.FullName -notlike "*\build\*" -and
        $_.FullName -notlike "*\.phase-backups\*" -and
        $_.FullName -notlike "*\phase-*-update\*"
    } |
    Select-String -SimpleMatch "import androidx.compose.foundation.layout.weight"

if ($remainingImports) {
    throw "Hotfix validation failed: an explicit weight import still exists."
}

$dashboardFile = Join-Path $ProjectRoot "feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\FoundationDashboardUiState.kt"
if (-not (Test-Path $dashboardFile)) {
    throw "Dashboard source file was not found after applying the hotfix."
}

$dashboardContent = [System.IO.File]::ReadAllText($dashboardFile)
if (-not $dashboardContent.Contains("Modifier.weight(1f)")) {
    throw "Dashboard weight usages are missing. The hotfix should remove only the import, not the modifiers."
}

Write-Host ""
Write-Host "PHASE 8 ROWSCOPE WEIGHT HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Changed files: $($changedFiles.Count)"
Write-Host "Backup: $backupRoot"
Write-Host ""
Write-Host "Modifier.weight remains inside the Row content scope, where RowScope provides it." -ForegroundColor Cyan
Write-Host ""
Write-Host "Now rerun:" -ForegroundColor Yellow
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase8.ps1 -InstallOnPhone"
