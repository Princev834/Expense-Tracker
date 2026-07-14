[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$databaseGradle = Join-Path $ProjectRoot "core\database\build.gradle.kts"
$verifyFile = Join-Path $ProjectRoot "scripts\Verify-Phase16.ps1"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

foreach ($requiredFile in @($databaseGradle, $verifyFile)) {
    if (-not (Test-Path $requiredFile)) {
        throw "Required file was not found: $requiredFile"
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase16-room-api-$timestamp"

function Backup-ProjectFile {
    param([Parameter(Mandatory = $true)][string]$Path)

    $relativePath = $Path.Substring($ProjectRoot.Length).TrimStart('\')
    $backupPath = Join-Path $backupRoot $relativePath
    New-Item -ItemType Directory -Path (Split-Path $backupPath -Parent) -Force | Out-Null
    Copy-Item -LiteralPath $Path -Destination $backupPath -Force
}

Backup-ProjectFile -Path $databaseGradle
Backup-ProjectFile -Path $verifyFile

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

Write-Host "Updating Room runtime dependency visibility..." -ForegroundColor Cyan

$databaseContent = [System.IO.File]::ReadAllText($databaseGradle)

if ($databaseContent.Contains("implementation(libs.androidx.room.runtime)")) {
    $databaseContent = $databaseContent.Replace(
        "implementation(libs.androidx.room.runtime)",
        "api(libs.androidx.room.runtime)"
    )
} elseif (-not $databaseContent.Contains("api(libs.androidx.room.runtime)")) {
    throw "Expected Room runtime dependency declaration was not found."
}

[System.IO.File]::WriteAllText($databaseGradle, $databaseContent, $utf8NoBom)

Write-Host "Strengthening Phase 16 verification..." -ForegroundColor Cyan

$verifyContent = [System.IO.File]::ReadAllText($verifyFile)
$verificationLine = "    -ExpectedText 'api(libs.androidx.room.runtime)'"

if (-not $verifyContent.Contains($verificationLine)) {
    $anchor = @'
Assert-FileContains `
    -Path ".\core\database\build.gradle.kts" `
    -ExpectedText 'api(project(":domain:transactions"))'
'@

    $addition = @'
Assert-FileContains `
    -Path ".\core\database\build.gradle.kts" `
    -ExpectedText 'api(project(":domain:transactions"))'
Assert-FileContains `
    -Path ".\core\database\build.gradle.kts" `
    -ExpectedText 'api(libs.androidx.room.runtime)'
'@

    if (-not $verifyContent.Contains($anchor)) {
        throw "Could not find the Phase 16 database dependency verification anchor."
    }

    $verifyContent = $verifyContent.Replace($anchor, $addition)
    [System.IO.File]::WriteAllText($verifyFile, $verifyContent, $utf8NoBom)
}

$updatedDatabase = [System.IO.File]::ReadAllText($databaseGradle)
$updatedVerify = [System.IO.File]::ReadAllText($verifyFile)

if (-not $updatedDatabase.Contains("api(libs.androidx.room.runtime)")) {
    throw "Hotfix validation failed: Room runtime is not exposed as an API dependency."
}

if ($updatedDatabase.Contains("implementation(libs.androidx.room.runtime)")) {
    throw "Hotfix validation failed: the hidden Room runtime declaration still exists."
}

if (-not $updatedVerify.Contains("api(libs.androidx.room.runtime)")) {
    throw "Hotfix validation failed: Verify-Phase16.ps1 was not updated."
}

Write-Host ""
Write-Host "PHASE 16 ROOM API HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Updated: core\database\build.gradle.kts"
Write-Host "Updated: scripts\Verify-Phase16.ps1"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host "RoomDatabase is now visible to modules consuming ProjectLedgerDatabase." -ForegroundColor Cyan
Write-Host ""
Write-Host "Now rerun:" -ForegroundColor Yellow
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase16.ps1 -InstallOnPhone"
