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

Write-Step "Checking Phase 20 transaction-entry screen contracts"

$appRoot = ".\app\src\main\java\com\princevekariya\projectledger"
$featureRoot = `
    ".\feature\transactions\src\main\java\com\princevekariya\projectledger\feature\transactions"
$dashboardFile = `
    ".\feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\FoundationDashboard.kt"

$requiredFiles = @(
    "$featureRoot\TransactionEntryRoute.kt",
    "$featureRoot\TransactionEntryScreen.kt",
    "$featureRoot\TransactionEntryViewModel.kt",
    "$featureRoot\TransactionEntryViewModelFactory.kt",
    "$appRoot\navigation\TransactionEntryDestination.kt",
    "$appRoot\navigation\ProjectLedgerApp.kt",
    "$appRoot\navigation\ProjectLedgerNavHost.kt",
    ".\app\src\test\java\com\princevekariya\projectledger\navigation\TransactionEntryDestinationTest.kt",
    ".\docs\PHASE-20-TRANSACTION-ENTRY-SCREEN.md",
    ".\scripts\Verify-Phase20.ps1"
)

foreach ($requiredFile in $requiredFiles) {
    Assert-FileExists -Path $requiredFile
}

Assert-FileContains `
    -Path ".\feature\transactions\build.gradle.kts" `
    -ExpectedText "implementation(libs.androidx.lifecycle.runtime.compose)"
Assert-FileContains `
    -Path ".\feature\transactions\build.gradle.kts" `
    -ExpectedText "implementation(libs.androidx.lifecycle.viewmodel.compose)"
Assert-FileContains `
    -Path "$featureRoot\TransactionEntryRoute.kt" `
    -ExpectedText "collectAsStateWithLifecycle()"
Assert-FileContains `
    -Path "$featureRoot\TransactionEntryScreen.kt" `
    -ExpectedText 'label = "Amount"'
Assert-FileContains `
    -Path "$featureRoot\TransactionEntryScreen.kt" `
    -ExpectedText 'label = "Account"'
Assert-FileContains `
    -Path "$featureRoot\TransactionEntryScreen.kt" `
    -ExpectedText 'label = "Category"'
Assert-FileContains `
    -Path "$featureRoot\TransactionEntryScreen.kt" `
    -ExpectedText 'label = "Payment method"'
Assert-FileContains `
    -Path "$featureRoot\TransactionEntryScreen.kt" `
    -ExpectedText "LedgerPrimaryButton("
Assert-FileContains `
    -Path "$featureRoot\TransactionEntryViewModel.kt" `
    -ExpectedText "if (!state.canSave)"
Assert-FileExcludes `
    -Path "$featureRoot\TransactionEntryViewModel.kt" `
    -ForbiddenText "amount == null ||"
Assert-FileContains `
    -Path "$appRoot\navigation\ProjectLedgerNavHost.kt" `
    -ExpectedText "TransactionEntryRoute("
Assert-FileContains `
    -Path "$appRoot\navigation\ProjectLedgerApp.kt" `
    -ExpectedText "currentRoute != TransactionEntryDestination.ROUTE_PATTERN"
Assert-FileContains `
    -Path "$dashboardFile" `
    -ExpectedText "onAddExpense = onAddExpense"
Assert-FileExcludes `
    -Path "$dashboardFile" `
    -ForbiddenText "InputSection("
Assert-FileContains `
    -Path "$dashboardFile" `
    -ExpectedText "Phase 20 - real transaction entry screen"
Assert-FileContains `
    -Path ".\app\src\main\AndroidManifest.xml" `
    -ExpectedText 'android:host="entry"'

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
        [string]::IsNullOrWhiteSpace([System.IO.File]::ReadAllText($_.FullName))
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
    Select-String -SimpleMatch "import androidx.compose.foundation.layout.weight"
if ($badWeightImports) {
    throw "An explicit scoped Compose weight import was found."
}

Write-Host "Phase 20 configuration checks passed." -ForegroundColor Green

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
    .\gradlew.bat spotlessApply --no-daemon --max-workers=1 --console=plain
}

Invoke-CheckedCommand `
    -Label "Running quality checks, UI compilation, tests, and debug builds" `
    -Command {
        .\gradlew.bat `
            qualityCheck `
            :core:common:test `
            :core:model:test `
            :domain:transactions:test `
            :core:database:testDebugUnitTest `
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
    $authorizedDevices = & $adb devices | Select-String -Pattern "\sdevice$"
    if (-not $authorizedDevices) {
        throw "No authorized Android phone was detected."
    }

    $apk = ".\app\build\outputs\apk\personal\debug\app-personal-debug.apk"
    Assert-FileExists -Path $apk
    Invoke-CheckedCommand -Label "Installing Project Ledger Personal Dev" -Command {
        & $adb install -r $apk
    }

    Write-Step "Launching the expense-entry deep link"
    $packageName = "com.princevekariya.projectledger.personal.debug"
    $activityName = "com.princevekariya.projectledger.MainActivity"
    $failureReport = ".\Phase20-EntryScreenFailure.txt"

    & $adb logcat -c
    & $adb shell am force-stop $packageName

    $launchOutput = & $adb shell am start `
        -W `
        -n "$packageName/$activityName" `
        -a "android.intent.action.VIEW" `
        -d "projectledger://entry/expense" `
        2>&1

    if ($LASTEXITCODE -ne 0) {
        throw "Android could not launch the expense-entry deep link."
    }

    Start-Sleep -Seconds 6

    $processIdText = ((& $adb shell pidof $packageName 2>$null) | Out-String).Trim()
    & $adb shell uiautomator dump /sdcard/project-ledger-phase20.xml | Out-Null
    $uiXml = (& $adb shell cat /sdcard/project-ledger-phase20.xml) -join ""
    $focusedLogLines = & $adb logcat `
        -d `
        -v threadtime `
        -s `
        "ProjectLedger:V" `
        "AndroidRuntime:E" `
        "*:S"
    $focusedLogText = $focusedLogLines -join [Environment]::NewLine

    $hasFatalException =
        $focusedLogText.Contains("FATAL EXCEPTION") -and
        $focusedLogText.Contains($packageName)
    $hasEntryTitle = $uiXml.Contains('text="Add expense"')
    $hasAmountField = $uiXml.Contains('text="Amount"')
    $hasSaveButton = $uiXml.Contains('text="Save expense"')
    $hasDashboardDraftField = $uiXml.Contains('text="What did you spend on?"')

    if (
        [string]::IsNullOrWhiteSpace($processIdText) -or
        $hasFatalException -or
        -not $hasEntryTitle -or
        -not $hasAmountField -or
        -not $hasSaveButton -or
        $hasDashboardDraftField
    ) {
        $reportLines = @(
            "Project Ledger Phase 20 entry-screen failure",
            "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')",
            "Package: $packageName",
            "Process ID after launch: $processIdText",
            "Entry title found: $hasEntryTitle",
            "Amount field found: $hasAmountField",
            "Save button found: $hasSaveButton",
            "Old dashboard draft field found: $hasDashboardDraftField",
            "",
            "=== Launch output ===",
            ($launchOutput -join [Environment]::NewLine),
            "",
            "=== Focused Logcat ===",
            $focusedLogText,
            "",
            "=== UI hierarchy ===",
            $uiXml
        )
        $reportLines | Set-Content -Path $failureReport -Encoding UTF8
        throw "The Phase 20 entry screen did not pass verification. See $failureReport."
    }

    Write-Host "Entry screen smoke test passed. Running PID: $processIdText" -ForegroundColor Green
    Write-Host "Expense title, amount field, and bottom save action were confirmed." -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "PHASE 20 VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Navigation: Home quick actions open expense or income entry"
Write-Host "Layout: fields first with the save action after all inputs"
Write-Host "State: lifecycle-aware Phase 19 ViewModel and one-off messages"
Write-Host "Schema safety: Room version 1 remains unchanged"
Write-Host "Primary development variant: personalDebug"
