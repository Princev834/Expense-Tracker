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

Write-Step "Checking Phase 22 live-dashboard contracts"

$dashboardRoot = `
    ".\feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard"
$dashboardTestRoot = `
    ".\feature\dashboard\src\test\java\com\princevekariya\projectledger\feature\dashboard"
$appRoot = ".\app\src\main\java\com\princevekariya\projectledger"

$requiredFiles = @(
    "$dashboardRoot\DashboardTransactionItem.kt",
    "$dashboardRoot\DashboardRepositories.kt",
    "$dashboardRoot\DashboardSourceData.kt",
    "$dashboardRoot\DashboardDataMapper.kt",
    "$dashboardRoot\DashboardUiState.kt",
    "$dashboardRoot\DashboardViewModel.kt",
    "$dashboardRoot\DashboardViewModelFactory.kt",
    "$dashboardRoot\DashboardRoute.kt",
    "$dashboardRoot\FoundationDashboard.kt",
    "$dashboardTestRoot\MainDispatcherRule.kt",
    "$dashboardTestRoot\DashboardViewModelTest.kt",
    "$appRoot\di\AppContainer.kt",
    "$appRoot\di\DefaultAppContainer.kt",
    "$appRoot\ProjectLedgerApplication.kt",
    "$appRoot\MainActivity.kt",
    ".\docs\PHASE-22-LIVE-ROOM-DASHBOARD.md",
    ".\scripts\Verify-Phase22.ps1"
)

foreach ($requiredFile in $requiredFiles) {
    Assert-FileExists -Path $requiredFile
}

Assert-FileContains `
    -Path ".\feature\dashboard\build.gradle.kts" `
    -ExpectedText 'api(project(":domain:transactions"))'
Assert-FileContains `
    -Path ".\feature\dashboard\build.gradle.kts" `
    -ExpectedText "api(libs.androidx.lifecycle.viewmodel.ktx)"
Assert-FileContains `
    -Path ".\feature\dashboard\build.gradle.kts" `
    -ExpectedText "api(libs.kotlinx.coroutines.core)"
Assert-FileContains `
    -Path ".\feature\dashboard\build.gradle.kts" `
    -ExpectedText "testImplementation(libs.kotlinx.coroutines.test)"
Assert-FileContains `
    -Path "$dashboardRoot\DashboardRepositories.kt" `
    -ExpectedText "val transactions: TransactionRepository"
Assert-FileContains `
    -Path "$dashboardRoot\DashboardUiState.kt" `
    -ExpectedText "val totalBalance: Money"
Assert-FileContains `
    -Path "$dashboardRoot\DashboardRepositories.kt" `
    -ExpectedText "val transactions: TransactionRepository"
Assert-FileContains `
    -Path "$dashboardRoot\DashboardUiState.kt" `
    -ExpectedText "val recentTransactions: List<DashboardTransactionItem>"
Assert-FileExcludes `
    -Path "$dashboardRoot\DashboardUiState.kt" `
    -ForbiddenText 'Money.fromMajorUnits("12,500")'
Assert-FileContains `
    -Path "$dashboardRoot\DashboardViewModel.kt" `
    -ExpectedText "repositories.accounts.observeAll()"
Assert-FileContains `
    -Path "$dashboardRoot\DashboardViewModel.kt" `
    -ExpectedText "repositories.transactions.observeAll()"
Assert-FileContains `
    -Path "$dashboardRoot\DashboardViewModel.kt" `
    -ExpectedText "repositories.categories.observeActive"
Assert-FileContains `
    -Path "$dashboardRoot\DashboardViewModel.kt" `
    -ExpectedText "repositories.merchants.observeActive()"
Assert-FileContains `
    -Path "$dashboardRoot\DashboardDataMapper.kt" `
    -ExpectedText "RECENT_TRANSACTION_LIMIT: Int = 5"
Assert-FileContains `
    -Path "$dashboardRoot\DashboardDataMapper.kt" `
    -ExpectedText "withDayOfMonth(1)"
Assert-FileContains `
    -Path "$dashboardRoot\FoundationDashboard.kt" `
    -ExpectedText 'title = "Total balance"'
Assert-FileContains `
    -Path "$dashboardRoot\FoundationDashboard.kt" `
    -ExpectedText 'SectionTitle(title = "Recent activity")'
Assert-FileExcludes `
    -Path "$dashboardRoot\FoundationDashboard.kt" `
    -ForbiddenText "College canteen"
Assert-FileExcludes `
    -Path "$dashboardRoot\FoundationDashboard.kt" `
    -ForbiddenText "State is ready"
Assert-FileContains `
    -Path "$appRoot\di\AppContainer.kt" `
    -ExpectedText "createDashboardViewModelFactory"
Assert-FileContains `
    -Path "$appRoot\ProjectLedgerApplication.kt" `
    -ExpectedText "DashboardViewModelFactory("
Assert-FileContains `
    -Path "$appRoot\ProjectLedgerApplication.kt" `
    -ExpectedText 'event = "live_dashboard_factory_ready"'
Assert-FileContains `
    -Path "$appRoot\MainActivity.kt" `
    -ExpectedText "appContainer.createDashboardViewModelFactory"
Assert-FileContains `
    -Path "$dashboardRoot\FoundationDashboard.kt" `
    -ExpectedText "Phase 22 - live Room dashboard"

Assert-FileExcludes `
    -Path ".\domain\transactions\src\test\kotlin\com\princevekariya\projectledger\domain\transactions\balance\AccountBalanceProjectorTest.kt" `
    -ForbiddenText "CurrencyCode.USD"
Assert-FileContains `
    -Path ".\core\database\src\test\kotlin\com\princevekariya\projectledger\core\database\repository\RoomTransactionRepositoryTest.kt" `
    -ExpectedText "AccountType.BANK_ACCOUNT"
Assert-FileExcludes `
    -Path ".\core\database\src\test\kotlin\com\princevekariya\projectledger\core\database\repository\RoomTransactionRepositoryTest.kt" `
    -ForbiddenText "AccountType.BANK,"
Assert-FileContains `
    -Path ".\scripts\Verify-Phase20.ps1" `
    -ExpectedText 'label = "Amount"'

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

Write-Host "Phase 22 configuration checks passed." -ForegroundColor Green

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
    -Label "Running live-dashboard, balance, repository, and debug verification" `
    -Command {
        .\gradlew.bat `
            qualityCheck `
            :core:common:test `
            :core:model:test `
            :domain:transactions:test `
            :core:database:testDebugUnitTest `
            :core:database:assembleDebug `
            :feature:dashboard:testDebugUnitTest `
            :feature:dashboard:assembleDebug `
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

    Write-Step "Launching the live Home dashboard"
    $packageName = `
        "com.princevekariya.projectledger.personal.debug"
    $activityName = `
        "com.princevekariya.projectledger.MainActivity"
    $failureReport = ".\Phase22-LiveDashboardFailure.txt"

    & $adb logcat -c
    & $adb shell am force-stop $packageName

    $launchOutput = & $adb shell am start `
        -W `
        -n "$packageName/$activityName" `
        -a "android.intent.action.VIEW" `
        -d "projectledger://home" `
        2>&1

    if ($LASTEXITCODE -ne 0) {
        throw "Android could not launch the Home deep link."
    }

    $processIdText = ""
    $focusedLogText = ""
    $hasDashboardFactoryLog = $false
    $hasBootstrapReadyLog = $false
    $uiXml = ""
    $hasTotalBalance = $false
    $hasIncome = $false
    $hasExpenses = $false

    for ($attempt = 1; $attempt -le 18; $attempt += 1) {
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
        $hasDashboardFactoryLog = $focusedLogText.Contains(
            "live_dashboard_factory_ready"
        )
        $hasBootstrapReadyLog = $focusedLogText.Contains(
            "default_ledger_data_ready"
        )

        & $adb shell uiautomator dump `
            /sdcard/project-ledger-phase22.xml |
            Out-Null
        $uiXml = (
            & $adb shell cat /sdcard/project-ledger-phase22.xml
        ) -join ""
        $hasTotalBalance = $uiXml.Contains('text="Total balance"')
        $hasIncome = $uiXml.Contains('text="Income"')
        $hasExpenses = $uiXml.Contains('text="Expenses"')

        if (
            -not [string]::IsNullOrWhiteSpace($processIdText) -and
            $hasDashboardFactoryLog -and
            $hasBootstrapReadyLog -and
            $hasTotalBalance -and
            $hasIncome -and
            $hasExpenses
        ) {
            break
        }
    }

    $hasFatalException =
        $focusedLogText.Contains("FATAL EXCEPTION") -and
        $focusedLogText.Contains($packageName)
    $hasOldDemoState = $uiXml.Contains('text="State is ready"')

    if (
        [string]::IsNullOrWhiteSpace($processIdText) -or
        $hasFatalException -or
        -not $hasDashboardFactoryLog -or
        -not $hasBootstrapReadyLog -or
        -not $hasTotalBalance -or
        -not $hasIncome -or
        -not $hasExpenses -or
        $hasOldDemoState
    ) {
        $diagnosticLogLines =
            & $adb logcat -d -v threadtime -t 5000
        $diagnosticLogText =
            $diagnosticLogLines -join [Environment]::NewLine
        $reportLines = @(
            "Project Ledger Phase 22 live-dashboard failure",
            "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')",
            "Package: $packageName",
            "Process ID after launch: $processIdText",
            "Dashboard factory log found: $hasDashboardFactoryLog",
            "Bootstrap-ready log found: $hasBootstrapReadyLog",
            "Total balance found: $hasTotalBalance",
            "Income found: $hasIncome",
            "Expenses found: $hasExpenses",
            "Old demo state found: $hasOldDemoState",
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
        throw "The Phase 22 live dashboard did not pass verification. See $failureReport."
    }

    Write-Host `
        "Live dashboard smoke test passed. Running PID: $processIdText" `
        -ForegroundColor Green
    Write-Host `
        "Real balance, income, and expense sections were confirmed." `
        -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "PHASE 22 VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Balance: sum of active Room account balances"
Write-Host "Monthly totals: live income and expenses for the local month"
Write-Host "Recent activity: five newest Room transactions"
Write-Host "Schema safety: Room version 1 remains unchanged"
Write-Host "Primary development variant: personalDebug"
