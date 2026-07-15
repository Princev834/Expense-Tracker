[CmdletBinding()]
param(
    [switch]$InstallOnPhone,
    [switch]$ConfigurationOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step {
    param([Parameter(Mandatory = $true)][string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Assert-FileExists {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path $Path)) {
        throw "Required file was not found: $Path"
    }
    Write-Host "Found: $Path"
}

function Assert-FileContains {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ExpectedText
    )
    $content = [System.IO.File]::ReadAllText($Path)
    if (-not $content.Contains($ExpectedText)) {
        throw "Expected text '$ExpectedText' was not found in '$Path'."
    }
}

function Assert-FileExcludes {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ForbiddenText
    )
    $content = [System.IO.File]::ReadAllText($Path)
    if ($content.Contains($ForbiddenText)) {
        throw "Forbidden text '$ForbiddenText' was found in '$Path'."
    }
}

function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)][scriptblock]$Command
    )
    Write-Step $Label
    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE."
    }
}

Write-Step "Checking Phase 23 live transaction-history contracts"

$featureRoot = `
    ".\feature\transactions\src\main\java\com\princevekariya\projectledger\feature\transactions"
$appRoot = ".\app\src\main\java\com\princevekariya\projectledger"

$requiredFiles = @(
    "$featureRoot\TransactionHistoryFilter.kt",
    "$featureRoot\TransactionHistoryItem.kt",
    "$featureRoot\TransactionHistoryUiState.kt",
    "$featureRoot\TransactionHistoryAction.kt",
    "$featureRoot\TransactionHistoryRepositories.kt",
    "$featureRoot\TransactionHistorySourceData.kt",
    "$featureRoot\TransactionHistoryDataMapper.kt",
    "$featureRoot\TransactionHistoryViewModel.kt",
    "$featureRoot\TransactionHistoryViewModelFactory.kt",
    "$featureRoot\TransactionHistoryRoute.kt",
    "$featureRoot\TransactionHistoryScreen.kt",
    ".\feature\transactions\src\test\java\com\princevekariya\projectledger\feature\transactions\TransactionHistoryViewModelTest.kt",
    "$appRoot\di\AppContainer.kt",
    "$appRoot\di\DefaultAppContainer.kt",
    "$appRoot\ProjectLedgerApplication.kt",
    "$appRoot\MainActivity.kt",
    "$appRoot\navigation\ProjectLedgerApp.kt",
    "$appRoot\navigation\ProjectLedgerNavHost.kt",
    ".\docs\PHASE-23-LIVE-TRANSACTION-HISTORY.md",
    ".\scripts\Verify-Phase23.ps1"
)

foreach ($requiredFile in $requiredFiles) {
    Assert-FileExists -Path $requiredFile
}

Assert-FileContains `
    -Path "$featureRoot\TransactionHistoryViewModel.kt" `
    -ExpectedText "repositories.transactions.observeAll()"
Assert-FileContains `
    -Path "$featureRoot\TransactionHistoryViewModel.kt" `
    -ExpectedText "repositories.accounts.observeAll()"
Assert-FileContains `
    -Path "$featureRoot\TransactionHistoryViewModel.kt" `
    -ExpectedText "repositories.categories.observeActive"
Assert-FileContains `
    -Path "$featureRoot\TransactionHistoryViewModel.kt" `
    -ExpectedText "repositories.merchants.observeActive()"
Assert-FileContains `
    -Path "$featureRoot\TransactionHistoryDataMapper.kt" `
    -ExpectedText "compareByDescending<LedgerTransaction>"
Assert-FileContains `
    -Path "$featureRoot\TransactionHistoryDataMapper.kt" `
    -ExpectedText "selectedFilter.accepts"
Assert-FileContains `
    -Path "$featureRoot\TransactionHistoryScreen.kt" `
    -ExpectedText 'text = "Transactions"'
Assert-FileContains `
    -Path "$featureRoot\TransactionHistoryScreen.kt" `
    -ExpectedText "TransactionHistoryFilter.values()"
Assert-FileContains `
    -Path "$featureRoot\TransactionHistoryScreen.kt" `
    -ExpectedText "Phase 23 - live transaction history"
Assert-FileContains `
    -Path "$appRoot\di\AppContainer.kt" `
    -ExpectedText "val transactionHistoryViewModelFactory"
Assert-FileContains `
    -Path "$appRoot\ProjectLedgerApplication.kt" `
    -ExpectedText "TransactionHistoryViewModelFactory("
Assert-FileContains `
    -Path "$appRoot\ProjectLedgerApplication.kt" `
    -ExpectedText 'event = "transaction_history_factory_ready"'
Assert-FileContains `
    -Path "$appRoot\navigation\ProjectLedgerNavHost.kt" `
    -ExpectedText "TransactionHistoryRoute(factory = factory)"
Assert-FileExcludes `
    -Path "$appRoot\navigation\ProjectLedgerNavHost.kt" `
    -ForbiddenText "TransactionsPlaceholderScreen"
Assert-FileContains `
    -Path ".\scripts\Verify-Phase22.ps1" `
    -ExpectedText "repositories.accounts.observeAll()"
Assert-FileExcludes `
    -Path ".\scripts\Verify-Phase22.ps1" `
    -ForbiddenText "accountRepository.observeAll()"

$databaseFile = `
    ".\core\database\src\main\kotlin\com\princevekariya\projectledger\core\database\ProjectLedgerDatabase.kt"
Assert-FileContains -Path $databaseFile -ExpectedText "DATABASE_VERSION: Int = 1"
Assert-FileExcludes `
    -Path $databaseFile `
    -ForbiddenText "fallbackToDestructiveMigration"

$sourceFiles = Get-ChildItem -Path . -Recurse -File |
    Where-Object {
        $_.Extension -in @(".kt", ".kts") -and
        $_.FullName -notlike "*\.gradle\*" -and
        $_.FullName -notlike "*\build\*" -and
        $_.FullName -notlike "*\.phase-backups\*" -and
        $_.FullName -notlike "*\phase-*-update\*"
    }

$emptyKotlinFiles = $sourceFiles |
    Where-Object {
        $_.Extension -eq ".kt" -and
        [string]::IsNullOrWhiteSpace(
            [System.IO.File]::ReadAllText($_.FullName)
        )
    }
if ($emptyKotlinFiles) {
    throw "Empty Kotlin files were found: $($emptyKotlinFiles.FullName -join ', ')"
}

$badConstNames = $sourceFiles |
    Select-String -Pattern '\bconst\s+val\s+[a-z]' -CaseSensitive
if ($badConstNames) {
    throw "Camel-case const val declarations were found."
}

$badWeightImports = $sourceFiles |
    Select-String `
        -SimpleMatch `
        "import androidx.compose.foundation.layout.weight"
if ($badWeightImports) {
    throw "An explicit scoped Compose weight import was found."
}

Write-Host "Phase 23 configuration checks passed." -ForegroundColor Green

if ($ConfigurationOnly) {
    exit 0
}

Write-Step "Configuring Android Studio's JDK 17"
$studioJava = "C:\Program Files\Android\Android Studio\jbr"
if (-not (Test-Path (Join-Path $studioJava "bin\java.exe"))) {
    throw "Android Studio's bundled JDK was not found: $studioJava"
}
$env:JAVA_HOME = $studioJava
$env:Path = "$studioJava\bin;$env:Path"
& java -version

Write-Step "Configuring Android SDK"
$androidSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
if (-not (Test-Path $androidSdk)) {
    throw "Android SDK was not found: $androidSdk"
}
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk
Write-Host "Android SDK: $androidSdk"

Invoke-CheckedCommand -Label "Stopping previous Gradle daemons" -Command {
    .\gradlew.bat --stop
}

Invoke-CheckedCommand -Label "Applying deterministic formatting" -Command {
    .\gradlew.bat `
        spotlessApply `
        --no-daemon `
        --max-workers=1 `
        --console=plain
}

Invoke-CheckedCommand `
    -Label "Running history, dashboard, repository, and debug verification" `
    -Command {
        .\gradlew.bat `
            qualityCheck `
            :core:common:test `
            :core:model:test `
            :domain:transactions:test `
            :core:database:testDebugUnitTest `
            :core:database:assembleDebug `
            :feature:dashboard:testDebugUnitTest `
            :feature:transactions:testDebugUnitTest `
            :feature:transactions:assembleDebug `
            :app:testPersonalDebugUnitTest `
            :app:testPlayDebugUnitTest `
            :app:assemblePersonalDebug `
            :app:assemblePlayDebug `
            --no-daemon `
            --max-workers=1 `
            --console=plain `
            --stacktrace
    }

Write-Step "Confirming Room schema remains unchanged"
$schemaFile = `
    ".\core\database\schemas\com.princevekariya.projectledger.core.database.ProjectLedgerDatabase\1.json"
Assert-FileExists -Path $schemaFile
$schema = Get-Content -Raw -Path $schemaFile | ConvertFrom-Json
if ($schema.database.version -ne 1) {
    throw "Room schema version changed unexpectedly."
}
if ($schema.database.entities.Count -ne 5) {
    throw "Expected five Room entities, found $($schema.database.entities.Count)."
}
Write-Host "Room schema version 1 still contains five entities." -ForegroundColor Green

if ($InstallOnPhone) {
    Write-Step "Checking connected Android phone"
    $adb = Join-Path $androidSdk "platform-tools\adb.exe"
    if (-not (Test-Path $adb)) {
        throw "ADB was not found: $adb"
    }

    & $adb devices -l
    $authorizedDevices = & $adb devices |
        Select-String -Pattern "\sdevice$"
    if (-not $authorizedDevices) {
        throw "No authorized Android phone was detected."
    }

    $apk = `
        ".\app\build\outputs\apk\personal\debug\app-personal-debug.apk"
    Assert-FileExists -Path $apk
    Invoke-CheckedCommand `
        -Label "Installing Project Ledger Personal Dev" `
        -Command {
            & $adb install -r $apk
        }

    Write-Step "Launching the live Transactions destination"
    $packageName = `
        "com.princevekariya.projectledger.personal.debug"
    $activityName = `
        "com.princevekariya.projectledger.MainActivity"
    $failureReport = ".\Phase23-TransactionHistoryFailure.txt"

    & $adb logcat -c

    Write-Host "Waking the phone and requesting keyguard dismissal"
    & $adb shell input keyevent KEYCODE_WAKEUP | Out-Null
    Start-Sleep -Seconds 1
    & $adb shell wm dismiss-keyguard | Out-Null
    & $adb shell input keyevent 82 | Out-Null
    Start-Sleep -Seconds 1

    & $adb shell am force-stop $packageName

    $launchOutput = & $adb shell am start `
        -W `
        -n "$packageName/$activityName" `
        -a "android.intent.action.VIEW" `
        -d "projectledger://transactions" `
        2>&1

    if ($LASTEXITCODE -ne 0) {
        throw "Android could not launch the Transactions deep link."
    }

    $processIdText = ""
    $focusedLogText = ""
    $hasHistoryFactoryLog = $false
    $hasBootstrapReadyLog = $false

    for ($attempt = 1; $attempt -le 15; $attempt += 1) {
        Start-Sleep -Seconds 1
        $processIdText = (
            (& $adb shell pidof $packageName 2>$null) |
                Out-String
        ).Trim()

        $focusedLogLines = & $adb logcat `
            -d `
            -v threadtime `
            -s `
            "ProjectLedger:V" `
            "AndroidRuntime:E" `
            "*:S"
        $focusedLogText =
            $focusedLogLines -join [Environment]::NewLine
        $hasHistoryFactoryLog = $focusedLogText.Contains(
            "transaction_history_factory_ready"
        )
        $hasBootstrapReadyLog = $focusedLogText.Contains(
            "default_ledger_data_ready"
        )

        if (
            -not [string]::IsNullOrWhiteSpace($processIdText) -and
            $hasHistoryFactoryLog -and
            $hasBootstrapReadyLog
        ) {
            break
        }
    }

    $uiXml = ""
    $appUiVisible = $false
    $systemUiVisible = $false
    $appPackageMarker = 'package="' + $packageName + '"'
    $systemUiPackageMarker = 'package="com.android.systemui"'

    for ($uiAttempt = 1; $uiAttempt -le 15; $uiAttempt += 1) {
        & $adb shell uiautomator dump `
            /sdcard/project-ledger-phase23.xml |
            Out-Null
        $uiXml = (
            & $adb shell cat /sdcard/project-ledger-phase23.xml
        ) -join ""

        $appUiVisible = $uiXml.Contains($appPackageMarker)
        $systemUiVisible = $uiXml.Contains($systemUiPackageMarker)

        if ($appUiVisible) {
            break
        }

        & $adb shell input keyevent KEYCODE_WAKEUP | Out-Null
        & $adb shell wm dismiss-keyguard | Out-Null
        & $adb shell input keyevent 82 | Out-Null
        Start-Sleep -Seconds 1
    }

    $hasFatalException =
        $focusedLogText.Contains("FATAL EXCEPTION") -and
        $focusedLogText.Contains($packageName)
    $hasTitle = $uiXml.Contains('text="Transactions"')
    $hasAllFilter = $uiXml.Contains('text="All"')
    $hasExpenseFilter = $uiXml.Contains('text="Expense"')
    $hasIncomeFilter = $uiXml.Contains('text="Income"')
    $hasOldPlaceholder = $uiXml.Contains(
        "Your searchable expense and income history will appear here."
    )

    if (
        [string]::IsNullOrWhiteSpace($processIdText) -or
        $hasFatalException -or
        -not $hasHistoryFactoryLog -or
        -not $hasBootstrapReadyLog -or
        -not $appUiVisible -or
        -not $hasTitle -or
        -not $hasAllFilter -or
        -not $hasExpenseFilter -or
        -not $hasIncomeFilter -or
        $hasOldPlaceholder
    ) {
        $diagnosticLogLines =
            & $adb logcat -d -v threadtime -t 5000
        $diagnosticLogText =
            $diagnosticLogLines -join [Environment]::NewLine
        $reportLines = @(
            "Project Ledger Phase 23 transaction-history failure",
            "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')",
            "Package: $packageName",
            "Process ID after launch: $processIdText",
            "History-factory log found: $hasHistoryFactoryLog",
            "Bootstrap-ready log found: $hasBootstrapReadyLog",
            "Project Ledger UI package found: $appUiVisible",
            "Android System UI package found: $systemUiVisible",
            "Transactions title found: $hasTitle",
            "All filter found: $hasAllFilter",
            "Expense filter found: $hasExpenseFilter",
            "Income filter found: $hasIncomeFilter",
            "Old placeholder found: $hasOldPlaceholder",
            "",
            "=== Launch output ===",
            ($launchOutput -join [Environment]::NewLine),
            "",
            "=== Focused Project Ledger Logcat ===",
            $focusedLogText,
            "",
            "=== Diagnostic Logcat ===",
            $diagnosticLogText,
            "",
            "=== UI hierarchy ===",
            $uiXml
        )
        $reportLines |
            Set-Content -Path $failureReport -Encoding UTF8
        if (-not $appUiVisible -and $systemUiVisible) {
            throw "The phone remained locked during UI verification. Unlock it, keep the screen on, and rerun Verify-Phase23.ps1. See $failureReport."
        }

        throw "The Phase 23 history screen did not pass verification. See $failureReport."
    }

    Write-Host `
        "Transaction history smoke test passed. Running PID: $processIdText" `
        -ForegroundColor Green
    Write-Host `
        "Transactions title and all three filters were confirmed." `
        -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "PHASE 23 VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "History: all Room transactions ordered newest first"
Write-Host "Details: category, account, local date, and payment method"
Write-Host "Filters: all, expense, and income"
Write-Host "Schema safety: Room version 1 remains unchanged"
Write-Host "Primary development variant: personalDebug"
