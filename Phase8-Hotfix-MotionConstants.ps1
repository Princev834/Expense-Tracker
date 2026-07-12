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
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase8-motion-constants-$timestamp"
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

function Backup-File {
    param([Parameter(Mandatory = $true)][string]$Path)

    $relative = $Path.Substring($ProjectRoot.Length).TrimStart('\')
    $destination = Join-Path $backupRoot $relative
    New-Item -ItemType Directory -Path (Split-Path $destination -Parent) -Force | Out-Null
    Copy-Item -LiteralPath $Path -Destination $destination -Force
}

$sourceFiles = Get-ChildItem -Path $ProjectRoot -Recurse -File -Filter "*.kt" |
    Where-Object {
        $_.FullName -notlike "*\.gradle\*" -and
        $_.FullName -notlike "*\build\*" -and
        $_.FullName -notlike "*\.phase-backups\*" -and
        $_.FullName -notlike "*\phase-*-update\*"
    }

$replacements = [ordered]@{
    "LedgerMotion.instantMillis" = "LedgerMotion.INSTANT_MILLIS"
    "LedgerMotion.quickMillis" = "LedgerMotion.QUICK_MILLIS"
    "LedgerMotion.standardMillis" = "LedgerMotion.STANDARD_MILLIS"
    "LedgerMotion.deliberateMillis" = "LedgerMotion.DELIBERATE_MILLIS"
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$changedFiles = New-Object System.Collections.Generic.List[string]

foreach ($file in $sourceFiles) {
    $original = [System.IO.File]::ReadAllText($file.FullName)
    $updated = $original

    foreach ($entry in $replacements.GetEnumerator()) {
        $updated = $updated.Replace($entry.Key, $entry.Value)
    }

    if ($updated -ne $original) {
        Backup-File -Path $file.FullName
        [System.IO.File]::WriteAllText($file.FullName, $updated, $utf8NoBom)
        $changedFiles.Add($file.FullName) | Out-Null
    }
}

Backup-File -Path $motionFile

$correctedMotion = @'
package com.princevekariya.projectledger.core.designsystem.theme

object LedgerMotion {
    const val INSTANT_MILLIS: Int = 90
    const val QUICK_MILLIS: Int = 160
    const val STANDARD_MILLIS: Int = 240
    const val DELIBERATE_MILLIS: Int = 360
}
'@

[System.IO.File]::WriteAllText(
    $motionFile,
    $correctedMotion.Replace("`r`n", "`n").TrimEnd() + "`n",
    $utf8NoBom
)

$projectKotlinFiles = Get-ChildItem -Path $ProjectRoot -Recurse -File -Filter "*.kt" |
    Where-Object {
        $_.FullName -notlike "*\.gradle\*" -and
        $_.FullName -notlike "*\build\*" -and
        $_.FullName -notlike "*\.phase-backups\*" -and
        $_.FullName -notlike "*\phase-*-update\*"
    }

$oldUsages = $projectKotlinFiles | Select-String -Pattern 'LedgerMotion\.(instantMillis|quickMillis|standardMillis|deliberateMillis)'
if ($oldUsages) {
    $details = ($oldUsages | ForEach-Object {
        "$($_.Path):$($_.LineNumber): $($_.Line.Trim())"
    }) -join "`n"
    throw "Old LedgerMotion property usages still exist:`n$details"
}

$invalidConstNames = $projectKotlinFiles | Select-String -Pattern '\bconst\s+val\s+[a-z]' -CaseSensitive
if ($invalidConstNames) {
    $details = ($invalidConstNames | ForEach-Object {
        "$($_.Path):$($_.LineNumber): $($_.Line.Trim())"
    }) -join "`n"
    throw "Camel-case const properties still exist:`n$details"
}

$writtenMotion = [System.IO.File]::ReadAllText($motionFile)
$requiredConstants = @(
    "const val INSTANT_MILLIS: Int = 90",
    "const val QUICK_MILLIS: Int = 160",
    "const val STANDARD_MILLIS: Int = 240",
    "const val DELIBERATE_MILLIS: Int = 360"
)

foreach ($constant in $requiredConstants) {
    if (-not $writtenMotion.Contains($constant)) {
        throw "Hotfix validation failed. Missing: $constant"
    }
}

Write-Host ""
Write-Host "PHASE 8 MOTION CONSTANTS HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Updated LedgerMotion constants to SCREAMING_SNAKE_CASE."
Write-Host "Updated usage files: $($changedFiles.Count)"
Write-Host "Backup: $backupRoot"
Write-Host ""
Write-Host "Now rerun:" -ForegroundColor Cyan
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase8.ps1 -InstallOnPhone"
