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

Write-Step "Checking Phase 14 files and Room contracts"

$databaseRoot = ".\core\database"
$sourceRoot = "$databaseRoot\src\main\kotlin\com\princevekariya\projectledger\core\database"
$requiredFiles = @(
    "$databaseRoot\build.gradle.kts",
    "$databaseRoot\src\main\AndroidManifest.xml",
    "$sourceRoot\ProjectLedgerDatabase.kt",
    "$sourceRoot\entity\AccountEntity.kt",
    "$sourceRoot\entity\CategoryEntity.kt",
    "$sourceRoot\entity\MerchantEntity.kt",
    "$sourceRoot\entity\TransactionEntity.kt",
    "$sourceRoot\entity\BudgetEntity.kt",
    "$sourceRoot\dao\AccountDao.kt",
    "$sourceRoot\dao\CategoryDao.kt",
    "$sourceRoot\dao\MerchantDao.kt",
    "$sourceRoot\dao\TransactionDao.kt",
    "$sourceRoot\dao\BudgetDao.kt",
    "$sourceRoot\mapper\AccountEntityMapper.kt",
    "$sourceRoot\mapper\CategoryEntityMapper.kt",
    "$sourceRoot\mapper\MerchantEntityMapper.kt",
    "$sourceRoot\mapper\TransactionEntityMapper.kt",
    "$sourceRoot\mapper\BudgetEntityMapper.kt",
    "$databaseRoot\src\test\kotlin\com\princevekariya\projectledger\core\database\EntityMapperTest.kt",
    ".\docs\PHASE-14-ROOM-DATABASE.md",
    ".\scripts\Verify-Phase14.ps1"
)

foreach ($file in $requiredFiles) {
    Assert-FileExists -Path $file
}

Assert-FileContains -Path ".\settings.gradle.kts" -ExpectedText 'include(":core:database")'
Assert-FileContains -Path ".\gradle\libs.versions.toml" -ExpectedText 'room = "2.6.1"'
Assert-FileContains -Path ".\gradle\libs.versions.toml" -ExpectedText 'org.jetbrains.kotlin.kapt'
Assert-FileContains -Path "$databaseRoot\build.gradle.kts" -ExpectedText 'api(project(":core:model"))'
Assert-FileContains -Path "$databaseRoot\build.gradle.kts" -ExpectedText 'room.schemaLocation'
Assert-FileContains -Path "$sourceRoot\ProjectLedgerDatabase.kt" -ExpectedText 'exportSchema = true'
Assert-FileContains -Path "$sourceRoot\ProjectLedgerDatabase.kt" -ExpectedText 'DATABASE_VERSION: Int = 1'
Assert-FileContains -Path "$sourceRoot\dao\TransactionDao.kt" -ExpectedText '@Upsert'
Assert-FileContains -Path "$sourceRoot\dao\TransactionDao.kt" -ExpectedText 'Flow<List<TransactionEntity>>'
Assert-FileContains `
    -Path ".\app\src\main\java\com\princevekariya\projectledger\ProjectLedgerApplication.kt" `
    -ExpectedText 'ProjectLedgerDatabase.create(context = this)'
Assert-FileContains `
    -Path ".\feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\FoundationDashboard.kt" `
    -ExpectedText 'Phase 14 - Room database foundation'

$databaseSource = Get-ChildItem -Path $databaseRoot -Recurse -File -Include *.kt,*.kts
$destructiveMigration = $databaseSource |
    Select-String -SimpleMatch "fallbackToDestructiveMigration"
if ($destructiveMigration) {
    throw "Destructive Room migration fallback is not allowed."
}

$entityFiles = Get-ChildItem -Path "$sourceRoot\entity" -File -Filter *.kt
$unsafeMoneyColumns = $entityFiles | Select-String -Pattern '\b(Double|Float)\b' -CaseSensitive
if ($unsafeMoneyColumns) {
    throw "Room entities contain a Float or Double financial column."
}

$transactionEntity = [System.IO.File]::ReadAllText("$sourceRoot\entity\TransactionEntity.kt")
$foreignKeyColumns = @(
    'Index(value = ["account_id"])',
    'Index(value = ["destination_account_id"])',
    'Index(value = ["category_id"])',
    'Index(value = ["merchant_id"])'
)
foreach ($indexContract in $foreignKeyColumns) {
    if (-not $transactionEntity.Contains($indexContract)) {
        throw "Transaction foreign-key index is missing: $indexContract"
    }
}

$sourceFiles = Get-ChildItem -Recurse -File -Include *.kt,*.kts |
    Where-Object {
        $_.FullName -notlike "*\build\*" -and
        $_.FullName -notlike "*\.gradle\*" -and
        $_.FullName -notlike "*\.phase-backups\*" -and
        $_.FullName -notlike "*\phase-*-update\*"
    }

$emptyKotlinFiles = $sourceFiles | Where-Object {
    $_.Extension -eq ".kt" -and
    [string]::IsNullOrWhiteSpace([System.IO.File]::ReadAllText($_.FullName))
}
if ($emptyKotlinFiles) {
    throw "Empty Kotlin files were found: $($emptyKotlinFiles.FullName -join ', ')"
}

$badConstNames = $sourceFiles | Select-String -Pattern '\bconst\s+val\s+[a-z]' -CaseSensitive
if ($badConstNames) {
    throw "Camel-case const val declarations were found."
}

$badWeightImports = $sourceFiles |
    Select-String -SimpleMatch "import androidx.compose.foundation.layout.weight"
if ($badWeightImports) {
    throw "An explicit scoped Compose weight import was found."
}

Write-Host "Phase 14 configuration checks passed." -ForegroundColor Green

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
    -Label "Running quality checks, database tests, and both debug builds" `
    -Command {
        .\gradlew.bat `
            qualityCheck `
            :core:common:test `
            :core:model:test `
            :core:database:testDebugUnitTest `
            :core:database:assembleDebug `
            :domain:transactions:test `
            :feature:dashboard:testDebugUnitTest `
            :app:testPersonalDebugUnitTest `
            :app:testPlayDebugUnitTest `
            :app:assemblePersonalDebug `
            :app:assemblePlayDebug `
            --no-daemon `
            --max-workers=1 `
            --console=plain `
            --stacktrace
    }

Write-Step "Checking the exported Room schema"
$schemaFile = `
    ".\core\database\schemas\com.princevekariya.projectledger.core.database.ProjectLedgerDatabase\1.json"
Assert-FileExists -Path $schemaFile
$schema = Get-Content -Raw -Path $schemaFile | ConvertFrom-Json
if ($schema.database.version -ne 1) {
    throw "Room schema version 1 was not exported correctly."
}
if ($schema.database.entities.Count -ne 5) {
    throw "Expected five Room entities, found $($schema.database.entities.Count)."
}
Write-Host "Room schema version 1 contains five entities." -ForegroundColor Green

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

    Write-Step "Running application startup smoke test"
    $packageName = "com.princevekariya.projectledger.personal.debug"
    $activityName = "com.princevekariya.projectledger.MainActivity"
    $failureReport = ".\Phase14-StartupSmokeFailure.txt"

    & $adb logcat -c
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to clear Logcat before the startup smoke test."
    }

    & $adb shell am force-stop $packageName
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to stop the app before the startup smoke test."
    }

    $launchOutput = & $adb shell am start -W -n "$packageName/$activityName" 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Android could not launch Project Ledger Personal Dev.`n$($launchOutput -join [Environment]::NewLine)"
    }

    Start-Sleep -Seconds 5

    $processIdText = ((& $adb shell pidof $packageName 2>$null) | Out-String).Trim()
    $logLines = & $adb logcat -d -v threadtime -t 3000
    $logText = $logLines -join [Environment]::NewLine
    $hasFatalException =
        $logText.Contains("FATAL EXCEPTION") -and
        $logText.Contains($packageName)
    $hasKnownLifecycleCrash =
        $logText.Contains("CompositionLocal LocalLifecycleOwner not present")
    $hasRoomStartupFailure =
        $logText.Contains("ProjectLedgerDatabase_Impl") -and
        $logText.Contains("ClassNotFoundException")

    if (
        [string]::IsNullOrWhiteSpace($processIdText) -or
        $hasFatalException -or
        $hasKnownLifecycleCrash -or
        $hasRoomStartupFailure
    ) {
        $reportLines = @(
            "Project Ledger Phase 14 startup smoke-test failure",
            "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')",
            "Package: $packageName",
            "Process ID after launch: $processIdText",
            "",
            "=== Launch output ===",
            ($launchOutput -join [Environment]::NewLine),
            "",
            "=== Logcat ===",
            $logText
        )
        $reportLines | Set-Content -Path $failureReport -Encoding UTF8
        throw "The app did not pass the startup smoke test. See $failureReport."
    }

    Write-Host "Startup smoke test passed. Running PID: $processIdText" -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "PHASE 14 VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Database: Room schema version 1 with five normalized tables"
Write-Host "Queries: compile-time checked DAOs with Flow reads and suspending writes"
Write-Host "Money storage: exact Long minor units with explicit currency code"
Write-Host "Safety: exported schema and no destructive migration fallback"
Write-Host "Primary development variant: personalDebug"
