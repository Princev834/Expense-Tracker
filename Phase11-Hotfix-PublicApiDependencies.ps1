[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$dashboardGradle = Join-Path $ProjectRoot "feature\dashboard\build.gradle.kts"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $dashboardGradle)) {
    throw "Dashboard Gradle file was not found: $dashboardGradle"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase11-public-api-dependencies-$timestamp"
$backupFile = Join-Path $backupRoot "feature\dashboard\build.gradle.kts"

New-Item -ItemType Directory -Path (Split-Path $backupFile -Parent) -Force | Out-Null
Copy-Item -LiteralPath $dashboardGradle -Destination $backupFile -Force

$content = [System.IO.File]::ReadAllText($dashboardGradle)

$replacements = [ordered]@{
    'implementation(project(":core:common"))' = 'api(project(":core:common"))'
    'implementation(project(":core:model"))' = 'api(project(":core:model"))'
}

foreach ($entry in $replacements.GetEnumerator()) {
    if ($content.Contains($entry.Key)) {
        $content = $content.Replace($entry.Key, $entry.Value)
    } elseif (-not $content.Contains($entry.Value)) {
        throw "Could not find expected dependency declaration: $($entry.Key)"
    }
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($dashboardGradle, $content, $utf8NoBom)

$updated = [System.IO.File]::ReadAllText($dashboardGradle)

$required = @(
    'api(project(":core:common"))',
    'api(project(":core:model"))',
    'implementation(project(":core:designsystem"))'
)

foreach ($line in $required) {
    if (-not $updated.Contains($line)) {
        throw "Hotfix validation failed. Missing dependency declaration: $line"
    }
}

if ($updated.Contains('implementation(project(":core:common"))')) {
    throw "Hotfix validation failed: core:common is still hidden as an implementation dependency."
}

if ($updated.Contains('implementation(project(":core:model"))')) {
    throw "Hotfix validation failed: core:model is still hidden as an implementation dependency."
}

Write-Host ""
Write-Host "PHASE 11 PUBLIC API DEPENDENCY HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Updated: feature\dashboard\build.gradle.kts"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host "core:common and core:model are now exposed to modules that consume" -ForegroundColor Cyan
Write-Host "DashboardUiState, which publicly references their types."
Write-Host ""
Write-Host "Now rerun:" -ForegroundColor Yellow
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase11.ps1 -InstallOnPhone"
