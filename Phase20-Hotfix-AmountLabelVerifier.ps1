[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$verifyFile = Join-Path $ProjectRoot "scripts\Verify-Phase20.ps1"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $verifyFile)) {
    throw "Verify-Phase20.ps1 was not found: $verifyFile"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase20-amount-label-verifier-$timestamp"
$backupFile = Join-Path $backupRoot "scripts\Verify-Phase20.ps1"

New-Item -ItemType Directory -Path (Split-Path $backupFile -Parent) -Force | Out-Null
Copy-Item -LiteralPath $verifyFile -Destination $backupFile -Force

$content = [System.IO.File]::ReadAllText($verifyFile)
$oldText = '-ExpectedText ''text = "Amount"'''
$newText = '-ExpectedText ''label = "Amount"'''

if ($content.Contains($newText)) {
    Write-Host "The Phase 20 amount-label verifier fix is already applied." -ForegroundColor Yellow
} elseif ($content.Contains($oldText)) {
    $content = $content.Replace($oldText, $newText)
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($verifyFile, $content, $utf8NoBom)
} else {
    throw "The expected incorrect Phase 20 verifier assertion was not found."
}

$updatedContent = [System.IO.File]::ReadAllText($verifyFile)

if (-not $updatedContent.Contains($newText)) {
    throw "Hotfix validation failed: the corrected amount-label assertion is missing."
}

if ($updatedContent.Contains($oldText)) {
    throw "Hotfix validation failed: the incorrect amount assertion still exists."
}

Write-Host ""
Write-Host "PHASE 20 AMOUNT LABEL VERIFIER HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Updated: scripts\Verify-Phase20.ps1"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host "Now rerun the configuration check:" -ForegroundColor Yellow
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase20.ps1 -ConfigurationOnly"
Write-Host ""
Write-Host "After that succeeds, run full verification:" -ForegroundColor Yellow
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase20.ps1 -InstallOnPhone"
