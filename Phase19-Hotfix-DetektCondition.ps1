[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$viewModelFile = Join-Path $ProjectRoot `
    "feature\transactions\src\main\java\com\princevekariya\projectledger\feature\transactions\TransactionEntryViewModel.kt"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $viewModelFile)) {
    throw "TransactionEntryViewModel.kt was not found: $viewModelFile"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase19-detekt-condition-$timestamp"
$backupFile = Join-Path $backupRoot `
    "feature\transactions\src\main\java\com\princevekariya\projectledger\feature\transactions\TransactionEntryViewModel.kt"

New-Item -ItemType Directory -Path (Split-Path $backupFile -Parent) -Force | Out-Null
Copy-Item -LiteralPath $viewModelFile -Destination $backupFile -Force

$oldBlock = @'
        val state = mutableUiState.value
        val amount = state.parsedAmount
        val accountId = state.selectedAccountId
        val categoryId = state.selectedCategoryId

        if (
            amount == null ||
            !amount.isPositive ||
            accountId == null ||
            categoryId == null
        ) {
            appLogger.warning(
                event = "transaction_entry_save_rejected",
                message = "The transaction form was incomplete or invalid.",
            )
            showMessage(text = "Complete the required transaction details.")
            return
        }

        mutableUiState.update { current ->
'@

$newBlock = @'
        val state = mutableUiState.value

        if (!state.canSave) {
            appLogger.warning(
                event = "transaction_entry_save_rejected",
                message = "The transaction form was incomplete or invalid.",
            )
            showMessage(text = "Complete the required transaction details.")
            return
        }

        val amount = requireNotNull(state.parsedAmount)
        val accountId = requireNotNull(state.selectedAccountId)
        val categoryId = requireNotNull(state.selectedCategoryId)

        mutableUiState.update { current ->
'@

$content = [System.IO.File]::ReadAllText($viewModelFile)

if ($content.Contains($newBlock)) {
    Write-Host "The Phase 19 Detekt hotfix is already applied." -ForegroundColor Yellow
} elseif ($content.Contains($oldBlock)) {
    $content = $content.Replace($oldBlock, $newBlock)
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($viewModelFile, $content, $utf8NoBom)
} else {
    throw "The expected Phase 19 validation block was not found. No file was changed."
}

$updatedContent = [System.IO.File]::ReadAllText($viewModelFile)

if (-not $updatedContent.Contains("if (!state.canSave)")) {
    throw "Hotfix validation failed: state.canSave validation was not installed."
}

if (-not $updatedContent.Contains("val amount = requireNotNull(state.parsedAmount)")) {
    throw "Hotfix validation failed: validated amount extraction is missing."
}

if ($updatedContent.Contains('amount == null ||')) {
    throw "Hotfix validation failed: the old complex condition still exists."
}

Write-Host ""
Write-Host "PHASE 19 DETEKT CONDITION HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Updated: feature\transactions\...\TransactionEntryViewModel.kt"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host "First run the focused check:" -ForegroundColor Yellow
Write-Host "  .\gradlew.bat detekt :feature:transactions:testDebugUnitTest --no-daemon --max-workers=1 --console=plain"
Write-Host ""
Write-Host "After that succeeds, rerun the complete verifier:" -ForegroundColor Yellow
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase19.ps1 -InstallOnPhone"
