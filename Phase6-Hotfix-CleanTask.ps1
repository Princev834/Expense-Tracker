[CmdletBinding()]
param(
    [string]$ProjectPath = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$buildFile = Join-Path $ProjectPath "build.gradle.kts"
if (-not (Test-Path $buildFile)) {
    throw "build.gradle.kts was not found in: $ProjectPath`nRun this script from the ProjectLedger root folder."
}

$content = Get-Content -Path $buildFile -Raw
$old = 'tasks.register<Delete>("clean") {'
$new = 'tasks.named<Delete>("clean") {'

if ($content.Contains($new)) {
    Write-Host "Phase 6 clean-task hotfix is already applied." -ForegroundColor Green
    exit 0
}

if (-not $content.Contains($old)) {
    throw "The expected clean-task block was not found. No files were changed."
}

$backup = "$buildFile.phase6-clean-task-backup"
Copy-Item -Path $buildFile -Destination $backup -Force
$content = $content.Replace($old, $new)
Set-Content -Path $buildFile -Value $content -Encoding utf8

Write-Host "Phase 6 clean-task hotfix applied successfully." -ForegroundColor Green
Write-Host "Backup: $backup"
Write-Host "Now rerun:"
Write-Host "powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase6.ps1 -InstallOnPhone"
