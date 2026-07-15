[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$screenFile = Join-Path $ProjectRoot `
    "feature\reports\src\main\java\com\princevekariya\projectledger\feature\reports\MonthlyReportScreen.kt"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $screenFile)) {
    throw "MonthlyReportScreen.kt was not found: $screenFile"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot `
    ".phase-backups\phase24-unused-spacing-$timestamp"
$backupFile = Join-Path $backupRoot `
    "feature\reports\src\main\java\com\princevekariya\projectledger\feature\reports\MonthlyReportScreen.kt"

New-Item `
    -ItemType Directory `
    -Path (Split-Path $backupFile -Parent) `
    -Force |
    Out-Null

Copy-Item `
    -LiteralPath $screenFile `
    -Destination $backupFile `
    -Force

$content = [System.IO.File]::ReadAllText($screenFile)

$oldBlock = @'
private fun CategoryExpenseCard(
    category: MonthlyCategoryExpenseItem,
) {
    val spacing = MaterialTheme.ledgerSpacing
    val expenseColor = MaterialTheme.ledgerColors.expense
'@

$newBlock = @'
private fun CategoryExpenseCard(
    category: MonthlyCategoryExpenseItem,
) {
    val expenseColor = MaterialTheme.ledgerColors.expense
'@

if ($content.Contains($newBlock) -and -not $content.Contains($oldBlock)) {
    Write-Host `
        "The Phase 24 unused-spacing hotfix is already applied." `
        -ForegroundColor Yellow
} elseif ($content.Contains($oldBlock)) {
    $content = $content.Replace($oldBlock, $newBlock)

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText(
        $screenFile,
        $content,
        $utf8NoBom
    )
} else {
    throw "The expected CategoryExpenseCard block was not found. No file was changed."
}

$updatedContent = [System.IO.File]::ReadAllText($screenFile)

if ($updatedContent.Contains($oldBlock)) {
    throw "Hotfix validation failed: the unused spacing declaration remains."
}

if (-not $updatedContent.Contains($newBlock)) {
    throw "Hotfix validation failed: CategoryExpenseCard is not in the expected state."
}

if (-not $updatedContent.Contains(
    'SectionTitle(title = "Spending by category")'
)) {
    throw "Hotfix validation failed: the category report section is missing."
}

if (-not $updatedContent.Contains(
    "Phase 24 - live monthly reports"
)) {
    throw "Hotfix validation failed: the Phase 24 report screen marker is missing."
}

Write-Host ""
Write-Host `
    "PHASE 24 UNUSED SPACING HOTFIX APPLIED" `
    -ForegroundColor Green
Write-Host "Updated: feature\reports\...\MonthlyReportScreen.kt"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host "First run the focused Reports compilation:" -ForegroundColor Yellow
Write-Host `
    "  .\gradlew.bat :feature:reports:compileDebugKotlin --no-daemon --max-workers=1 --console=plain"
Write-Host ""
Write-Host "Then rerun complete Phase 24 verification:" -ForegroundColor Yellow
Write-Host `
    "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase24.ps1 -InstallOnPhone"
