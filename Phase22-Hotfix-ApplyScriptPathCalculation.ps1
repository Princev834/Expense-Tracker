[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$applyFile = Join-Path $ProjectRoot "phase-22-update\Apply-Phase22.ps1"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $applyFile)) {
    throw "Apply-Phase22.ps1 was not found: $applyFile"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase22-apply-script-$timestamp"
$backupFile = Join-Path $backupRoot "phase-22-update\Apply-Phase22.ps1"

New-Item `
    -ItemType Directory `
    -Path (Split-Path $backupFile -Parent) `
    -Force |
    Out-Null

Copy-Item `
    -LiteralPath $applyFile `
    -Destination $backupFile `
    -Force

$content = [System.IO.File]::ReadAllText($applyFile)

$brokenBlock = @'
    $relativePath = $payloadFile.FullName
        .Substring($payloadRoot.Length)
        .TrimStart('\')
'@

$fixedBlock = @'
    $relativePath = $payloadFile.FullName.Substring(
        $payloadRoot.Length
    ).TrimStart('\')
'@

$brokenCount = ([regex]::Matches(
    $content,
    [regex]::Escape($brokenBlock)
)).Count

if ($brokenCount -eq 0 -and $content.Contains($fixedBlock)) {
    Write-Host "The Phase 22 apply-script hotfix is already applied." -ForegroundColor Yellow
} elseif ($brokenCount -eq 2) {
    $content = $content.Replace($brokenBlock, $fixedBlock)

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText(
        $applyFile,
        $content,
        $utf8NoBom
    )
} else {
    throw "Expected two broken Phase 22 path-calculation blocks, found $brokenCount. No file was changed."
}

$updatedContent = [System.IO.File]::ReadAllText($applyFile)

if ($updatedContent.Contains($brokenBlock)) {
    throw "Hotfix validation failed: a broken multiline method call remains."
}

$fixedCount = ([regex]::Matches(
    $updatedContent,
    [regex]::Escape($fixedBlock)
)).Count

if ($fixedCount -ne 2) {
    throw "Hotfix validation failed: expected two corrected path calculations, found $fixedCount."
}

Write-Host ""
Write-Host "PHASE 22 APPLY-SCRIPT HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Updated: phase-22-update\Apply-Phase22.ps1"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host "Now rerun Phase 22 apply:" -ForegroundColor Yellow
Write-Host "  powershell -ExecutionPolicy Bypass -File .\phase-22-update\Apply-Phase22.ps1"
