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

Write-Step "Checking Phase 15 repository contracts"

$domainRoot = `
    ".\domain\transactions\src\main\kotlin\com\princevekariya\projectledger\domain\transactions"
$databaseRoot = `
    ".\core\database\src\main\kotlin\com\princevekariya\projectledger\core\database"
$databaseTestRoot = `
    ".\core\database\src\test\kotlin\com\princevekariya\projectledger\core\database\repository"

$requiredFiles = @(
    "$domainRoot\repository\AccountRepository.kt",
    "$domainRoot\repository\BudgetRepository.kt",
    "$domainRoot\repository\CategoryRepository.kt",
    "$domainRoot\repository\MerchantRepository.kt",
    "$domainRoot\repository\TransactionRepository.kt",
    "$databaseRoot\repository\RoomAccountRepository.kt",
    "$databaseRoot\repository\RoomBudgetRepository.kt",
    "$databaseRoot\repository\RoomCategoryRepository.kt",
    "$databaseRoot\repository\RoomMerchantRepository.kt",
    "$databaseRoot\repository\RoomTransactionRepository.kt",
    "$databaseRoot\repository\LedgerRepositories.kt",
    "$databaseTestRoot\RoomAccountRepositoryTest.kt",
    "$databaseTestRoot\RoomBudgetRepositoryTest.kt",
    "$databaseTestRoot\RoomCategoryRepositoryTest.kt",
    "$databaseTestRoot\RoomMerchantRepositoryTest.kt",
    "$databaseTestRoot\RoomTransactionRepositoryTest.kt",
    ".\docs\PHASE-15-REPOSITORY-BOUNDARY.md",
    ".\scripts\Verify-Phase15.ps1"
)

foreach ($requiredFile in $requiredFiles) {
    Assert-FileExists -Path $requiredFile
}

Assert-FileContains `
    -Path ".\gradle\libs.versions.toml" `
    -ExpectedText 'coroutines = "1.8.1"'
Assert-FileContains `
    -Path ".\domain\transactions\build.gradle.kts" `
    -ExpectedText '`java-library`'
Assert-FileContains `
    -Path ".\domain\transactions\build.gradle.kts" `
    -ExpectedText 'api(project(":core:model"))'
Assert-FileContains `
    -Path ".\domain\transactions\build.gradle.kts" `
    -ExpectedText 'api(libs.kotlinx.coroutines.core)'
Assert-FileContains `
    -Path ".\core\database\build.gradle.kts" `
    -ExpectedText 'api(project(":domain:transactions"))'
Assert-FileContains `
    -Path "$databaseRoot\repository\RoomTransactionRepository.kt" `
    -ExpectedText 'require(limit > 0)'
Assert-FileContains `
    -Path "$databaseRoot\repository\RoomMerchantRepository.kt" `
    -ExpectedText 'lowercase(Locale.ENGLISH)'
Assert-FileContains `
    -Path "$databaseRoot\repository\LedgerRepositories.kt" `
    -ExpectedText 'transactions = RoomTransactionRepository'
$dashboardFile = `
    ".\feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\FoundationDashboard.kt"
Assert-FileContains `
    -Path $dashboardFile `
    -ExpectedText "Phase 15 - repository boundary"

$databaseFile = "$databaseRoot\ProjectLedgerDatabase.kt"
Assert-FileContains -Path $databaseFile -ExpectedText "DATABASE_VERSION: Int = 1"
$databaseContent = [System.IO.File]::ReadAllText($databaseFile)
if ($databaseContent.Contains("fallbackToDestructiveMigration")) {
    throw "A destructive Room migration fallback was found."
}

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

Write-Host "Phase 15 configuration checks passed." -ForegroundColor Green

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
    -Label "Running quality checks, repository tests, and both debug builds" `
    -Command {
        .\gradlew.bat `
            qualityCheck `
            :core:common:test `
            :core:model:test `
            :domain:transactions:test `
            :core:database:testDebugUnitTest `
            :core:database:assembleDebug `
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

    Write-Step "Running application startup smoke test"
    $packageName = "com.princevekariya.projectledger.personal.debug"
    $activityName = "com.princevekariya.projectledger.MainActivity"
    $failureReport = ".\Phase15-StartupSmokeFailure.txt"

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
    $hasRepositoryLinkageFailure =
        $logText.Contains("NoClassDefFoundError") -and
        $logText.Contains("domain.transactions.repository")

    if (
        [string]::IsNullOrWhiteSpace($processIdText) -or
        $hasFatalException -or
        $hasKnownLifecycleCrash -or
        $hasRepositoryLinkageFailure
    ) {
        $reportLines = @(
            "Project Ledger Phase 15 startup smoke-test failure",
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
Write-Host "PHASE 15 VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Domain: repository contracts expose models and Flow without Room types"
Write-Host "Local data: five Room-backed repository implementations"
Write-Host "Validation: identifiers, search keys, and recent limits checked"
Write-Host "Schema safety: Room version 1 remains unchanged"
Write-Host "Primary development variant: personalDebug"
