[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$verifyFile = Join-Path $ProjectRoot "scripts\Verify-Phase22.ps1"
$viewModelFile = Join-Path $ProjectRoot `
    "feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\DashboardViewModel.kt"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $verifyFile)) {
    throw "Verify-Phase22.ps1 was not found: $verifyFile"
}

if (-not (Test-Path $viewModelFile)) {
    throw "DashboardViewModel.kt was not found: $viewModelFile"
}

$viewModelContent = [System.IO.File]::ReadAllText($viewModelFile)
$requiredSourceContracts = @(
    "repositories.accounts.observeAll()",
    "repositories.transactions.observeAll()",
    "repositories.categories.observeActive",
    "repositories.merchants.observeActive()"
)

foreach ($contract in $requiredSourceContracts) {
    if (-not $viewModelContent.Contains($contract)) {
        throw "The expected DashboardViewModel contract was not found: $contract"
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot `
    ".phase-backups\phase22-repository-verifier-$timestamp"
$backupFile = Join-Path $backupRoot "scripts\Verify-Phase22.ps1"

New-Item `
    -ItemType Directory `
    -Path (Split-Path $backupFile -Parent) `
    -Force |
    Out-Null

Copy-Item `
    -LiteralPath $verifyFile `
    -Destination $backupFile `
    -Force

$content = [System.IO.File]::ReadAllText($verifyFile)

$replacements = [ordered]@{
    "accountRepository.observeAll()" =
        "repositories.accounts.observeAll()"
    "transactionRepository.observeAll()" =
        "repositories.transactions.observeAll()"
    "categoryRepository.observeActive" =
        "repositories.categories.observeActive"
    "merchantRepository.observeActive()" =
        "repositories.merchants.observeActive()"
}

$changedCount = 0

foreach ($oldText in $replacements.Keys) {
    $newText = $replacements[$oldText]

    if ($content.Contains($oldText)) {
        $content = $content.Replace($oldText, $newText)
        $changedCount += 1
    } elseif (-not $content.Contains($newText)) {
        throw "Neither the old nor corrected verifier assertion was found: $oldText"
    }
}

if ($changedCount -gt 0) {
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText(
        $verifyFile,
        $content,
        $utf8NoBom
    )
} else {
    Write-Host `
        "The Phase 22 repository verifier hotfix is already applied." `
        -ForegroundColor Yellow
}

$updatedContent = [System.IO.File]::ReadAllText($verifyFile)

foreach ($oldText in $replacements.Keys) {
    if ($updatedContent.Contains($oldText)) {
        throw "Hotfix validation failed: obsolete assertion remains: $oldText"
    }
}

foreach ($newText in $replacements.Values) {
    if (-not $updatedContent.Contains($newText)) {
        throw "Hotfix validation failed: corrected assertion is missing: $newText"
    }
}

Write-Host ""
Write-Host `
    "PHASE 22 REPOSITORY VERIFIER HOTFIX APPLIED" `
    -ForegroundColor Green
Write-Host "Updated: scripts\Verify-Phase22.ps1"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host "Run the configuration check:" -ForegroundColor Yellow
Write-Host `
    "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase22.ps1 -ConfigurationOnly"
Write-Host ""
Write-Host "Then run complete verification:" -ForegroundColor Yellow
Write-Host `
    "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase22.ps1 -InstallOnPhone"
