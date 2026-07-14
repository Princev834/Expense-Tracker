[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$accountTypeFile = Join-Path $ProjectRoot `
    "core\model\src\main\kotlin\com\princevekariya\projectledger\core\model\AccountType.kt"
$testFile = Join-Path $ProjectRoot `
    "core\database\src\test\kotlin\com\princevekariya\projectledger\core\database\repository\RoomTransactionRepositoryTest.kt"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $accountTypeFile)) {
    throw "AccountType.kt was not found: $accountTypeFile"
}

if (-not (Test-Path $testFile)) {
    throw "RoomTransactionRepositoryTest.kt was not found: $testFile"
}

$accountTypeContent = [System.IO.File]::ReadAllText($accountTypeFile)
if (-not $accountTypeContent.Contains("BANK_ACCOUNT")) {
    throw "The expected AccountType.BANK_ACCOUNT enum value was not found."
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot `
    ".phase-backups\phase21-bank-account-test-$timestamp"
$backupFile = Join-Path $backupRoot `
    "core\database\src\test\kotlin\com\princevekariya\projectledger\core\database\repository\RoomTransactionRepositoryTest.kt"

New-Item `
    -ItemType Directory `
    -Path (Split-Path $backupFile -Parent) `
    -Force |
    Out-Null

Copy-Item `
    -LiteralPath $testFile `
    -Destination $backupFile `
    -Force

$content = [System.IO.File]::ReadAllText($testFile)
$oldText = "type = AccountType.BANK,"
$newText = "type = AccountType.BANK_ACCOUNT,"

if ($content.Contains($newText)) {
    Write-Host `
        "The Phase 21 bank-account test hotfix is already applied." `
        -ForegroundColor Yellow
} elseif ($content.Contains($oldText)) {
    $content = $content.Replace($oldText, $newText)

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText(
        $testFile,
        $content,
        $utf8NoBom
    )
} else {
    throw "The invalid AccountType.BANK test value was not found. No file was changed."
}

$updatedContent = [System.IO.File]::ReadAllText($testFile)

if ($updatedContent.Contains("AccountType.BANK,")) {
    throw "Hotfix validation failed: AccountType.BANK still exists."
}

if (-not $updatedContent.Contains("AccountType.BANK_ACCOUNT")) {
    throw "Hotfix validation failed: AccountType.BANK_ACCOUNT is missing."
}

if (-not $updatedContent.Contains(
    "fun balanceAwareSaveRejectsMismatchedAccount"
)) {
    throw "Hotfix validation failed: the mismatched-account test is missing."
}

Write-Host ""
Write-Host `
    "PHASE 21 BANK ACCOUNT TEST HOTFIX APPLIED" `
    -ForegroundColor Green
Write-Host `
    "Updated: core\database\...\RoomTransactionRepositoryTest.kt"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host `
    "Now rerun the complete verifier:" `
    -ForegroundColor Yellow
Write-Host `
    "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase21.ps1 -InstallOnPhone"
