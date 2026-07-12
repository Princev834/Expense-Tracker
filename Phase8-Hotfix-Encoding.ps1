[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$verifyFile = Join-Path $ProjectRoot "scripts\Verify-Phase8.ps1"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $verifyFile)) {
    throw "Phase 8 verification script was not found: $verifyFile"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase8-encoding-hotfix-$timestamp"
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

$backupFile = Join-Path $backupRoot "scripts\Verify-Phase8.ps1"
New-Item -ItemType Directory -Path (Split-Path $backupFile -Parent) -Force | Out-Null
Copy-Item -LiteralPath $verifyFile -Destination $backupFile -Force

$lines = [System.IO.File]::ReadAllLines($verifyFile)
$replacement = 'Assert-FileContains -Path ".\feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\FoundationDashboardUiState.kt" -ExpectedText "design-token foundation"'

$matchCount = 0
for ($index = 0; $index -lt $lines.Length; $index++) {
    if (
        $lines[$index] -like '*Assert-FileContains*FoundationDashboardUiState.kt*ExpectedText*'
    ) {
        $lines[$index] = $replacement
        $matchCount++
    }
}

if ($matchCount -ne 1) {
    throw "Expected to replace exactly one Phase 8 dashboard assertion, but found $matchCount."
}

$utf8Bom = New-Object System.Text.UTF8Encoding($true)
[System.IO.File]::WriteAllLines($verifyFile, $lines, $utf8Bom)

$updated = [System.IO.File]::ReadAllText($verifyFile)
if (-not $updated.Contains('-ExpectedText "design-token foundation"')) {
    throw "Hotfix validation failed: the ASCII-safe assertion was not written."
}

Write-Host ""
Write-Host "PHASE 8 ENCODING HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Updated: scripts\Verify-Phase8.ps1"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host "Now run:" -ForegroundColor Cyan
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase8.ps1 -InstallOnPhone"
