[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$motionFile = Join-Path $ProjectRoot "core\designsystem\src\main\java\com\princevekariya\projectledger\core\designsystem\theme\LedgerMotion.kt"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $motionFile)) {
    throw "LedgerMotion.kt was not found: $motionFile"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase8-motion-token-hotfix-$timestamp"
$backupFile = Join-Path $backupRoot "core\designsystem\src\main\java\com\princevekariya\projectledger\core\designsystem\theme\LedgerMotion.kt"

New-Item -ItemType Directory -Path (Split-Path $backupFile -Parent) -Force | Out-Null
Copy-Item -LiteralPath $motionFile -Destination $backupFile -Force

$correctedContent = @'
package com.princevekariya.projectledger.core.designsystem.theme

object LedgerMotion {
    val instantMillis: Int = 90
    val quickMillis: Int = 160
    val standardMillis: Int = 240
    val deliberateMillis: Int = 360
}
'@

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText(
    $motionFile,
    $correctedContent.Replace("`r`n", "`n").TrimEnd() + "`n",
    $utf8NoBom
)

$written = [System.IO.File]::ReadAllText($motionFile)

$requiredLines = @(
    "val instantMillis: Int = 90",
    "val quickMillis: Int = 160",
    "val standardMillis: Int = 240",
    "val deliberateMillis: Int = 360"
)

foreach ($line in $requiredLines) {
    if (-not $written.Contains($line)) {
        throw "Hotfix validation failed. Missing line: $line"
    }
}

if ($written.Contains("const val")) {
    throw "Hotfix validation failed: LedgerMotion.kt still contains const val."
}

$invalidConstants = Get-ChildItem -Path $ProjectRoot -Recurse -File -Filter "*.kt" |
    Where-Object {
        $_.FullName -notlike "*\.gradle\*" -and
        $_.FullName -notlike "*\build\*" -and
        $_.FullName -notlike "*\.phase-backups\*"
    } |
    Select-String -Pattern '\bconst\s+val\s+[a-z]' -CaseSensitive

if ($invalidConstants) {
    $details = ($invalidConstants | ForEach-Object {
        "$($_.Path):$($_.LineNumber): $($_.Line.Trim())"
    }) -join "`n"

    throw "Camel-case const properties still exist:`n$details"
}

Write-Host ""
Write-Host "PHASE 8 MOTION TOKEN HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Updated: core\designsystem\...\LedgerMotion.kt"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host "The motion tokens remain camelCase but are now regular immutable vals." -ForegroundColor Cyan
Write-Host "They do not need compile-time const semantics."
Write-Host ""
Write-Host "Now rerun:" -ForegroundColor Yellow
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase8.ps1 -InstallOnPhone"
