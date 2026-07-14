[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$testFile = Join-Path $ProjectRoot `
    "domain\transactions\src\test\kotlin\com\princevekariya\projectledger\domain\transactions\balance\AccountBalanceProjectorTest.kt"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $testFile)) {
    throw "AccountBalanceProjectorTest.kt was not found: $testFile"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase21-inr-test-$timestamp"
$backupFile = Join-Path $backupRoot `
    "domain\transactions\src\test\kotlin\com\princevekariya\projectledger\domain\transactions\balance\AccountBalanceProjectorTest.kt"

New-Item -ItemType Directory -Path (Split-Path $backupFile -Parent) -Force | Out-Null
Copy-Item -LiteralPath $testFile -Destination $backupFile -Force

$content = [System.IO.File]::ReadAllText($testFile)

$currencyImport = @'
import com.princevekariya.projectledger.core.model.CurrencyCode
'@

$invalidTest = @'
    @Test
    fun currencyMismatchIsRejected() {
        val failure = runCatching {
            projector.project(
                account = account(
                    openingMinorUnits = 0L,
                    currentMinorUnits = 0L,
                ),
                transactionType = TransactionType.INCOME,
                amount = Money(
                    minorUnits = 100L,
                    currency = CurrencyCode.USD,
                ),
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

'@

if (-not $content.Contains("CurrencyCode.USD")) {
    Write-Host "The Phase 21 INR-only test hotfix is already applied." -ForegroundColor Yellow
} else {
    if (-not $content.Contains($invalidTest)) {
        throw "The expected invalid USD test block was not found. No file was changed."
    }

    $content = $content.Replace($invalidTest, "")
    $content = $content.Replace($currencyImport, "")

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($testFile, $content, $utf8NoBom)
}

$updatedContent = [System.IO.File]::ReadAllText($testFile)

if ($updatedContent.Contains("CurrencyCode.USD")) {
    throw "Hotfix validation failed: CurrencyCode.USD still exists."
}

if ($updatedContent.Contains("import com.princevekariya.projectledger.core.model.CurrencyCode")) {
    throw "Hotfix validation failed: the unused CurrencyCode import still exists."
}

if (-not $updatedContent.Contains("fun expenseReducesCurrentBalanceWithoutChangingOpeningBalance")) {
    throw "Hotfix validation failed: the expense balance test is missing."
}

if (-not $updatedContent.Contains("fun incomeIncreasesCurrentBalance")) {
    throw "Hotfix validation failed: the income balance test is missing."
}

if (-not $updatedContent.Contains("fun transferRequiresTheDedicatedTwoAccountFlow")) {
    throw "Hotfix validation failed: the transfer rejection test is missing."
}

Write-Host ""
Write-Host "PHASE 21 INR-ONLY TEST HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Updated: domain\transactions\...\AccountBalanceProjectorTest.kt"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host "The production currency guard remains unchanged." -ForegroundColor Cyan
Write-Host ""
Write-Host "Now rerun the complete verifier:" -ForegroundColor Yellow
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase21.ps1 -InstallOnPhone"
