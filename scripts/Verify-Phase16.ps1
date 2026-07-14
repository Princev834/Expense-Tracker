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

Write-Step "Checking Phase 16 application-container contracts"

$appRoot = ".\app\src\main\java\com\princevekariya\projectledger"
$appTestRoot = ".\app\src\test\java\com\princevekariya\projectledger"
$databaseRoot = `
    ".\core\database\src\main\kotlin\com\princevekariya\projectledger\core\database"
$dashboardFile = `
    ".\feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\FoundationDashboard.kt"

$requiredFiles = @(
    "$appRoot\di\AppContainer.kt",
    "$appRoot\di\DefaultAppContainer.kt",
    "$appRoot\ProjectLedgerApplication.kt",
    "$appRoot\MainActivity.kt",
    "$appTestRoot\di\DefaultAppContainerTest.kt",
    ".\docs\PHASE-16-APPLICATION-CONTAINER.md",
    ".\scripts\Verify-Phase16.ps1"
)

foreach ($requiredFile in $requiredFiles) {
    Assert-FileExists -Path $requiredFile
}

Assert-FileContains `
    -Path "$appRoot\di\AppContainer.kt" `
    -ExpectedText "val appLogger: AppLogger"
Assert-FileContains `
    -Path "$appRoot\di\AppContainer.kt" `
    -ExpectedText "val repositories: LedgerRepositories"
Assert-FileContains `
    -Path "$appRoot\ProjectLedgerApplication.kt" `
    -ExpectedText "lateinit var appContainer: AppContainer"
Assert-FileContains `
    -Path "$appRoot\ProjectLedgerApplication.kt" `
    -ExpectedText "repositories = database.createRepositories()"
Assert-FileContains `
    -Path "$appRoot\ProjectLedgerApplication.kt" `
    -ExpectedText 'event = "application_container_ready"'
Assert-FileExcludes `
    -Path "$appRoot\ProjectLedgerApplication.kt" `
    -ForbiddenText "lateinit var database"
Assert-FileExcludes `
    -Path "$appRoot\ProjectLedgerApplication.kt" `
    -ForbiddenText "lateinit var appLogger"
Assert-FileContains `
    -Path "$appRoot\MainActivity.kt" `
    -ExpectedText "(application as ProjectLedgerApplication).appContainer"
Assert-FileContains `
    -Path "$appRoot\MainActivity.kt" `
    -ExpectedText "val appLogger = appContainer.appLogger"
Assert-FileContains `
    -Path "$databaseRoot\repository\LedgerRepositories.kt" `
    -ExpectedText "fun ProjectLedgerDatabase.createRepositories()"
Assert-FileContains `
    -Path ".\core\database\build.gradle.kts" `
    -ExpectedText 'api(project(":domain:transactions"))'
Assert-FileContains `
    -Path ".\core\database\build.gradle.kts" `
    -ExpectedText 'api(libs.androidx.room.runtime)'
Assert-FileContains `
    -Path ".\gradle\libs.versions.toml" `
    -ExpectedText 'lifecycle-runtime-ktx = "2.8.2"'
Assert-FileContains `
    -Path $dashboardFile `
    -ExpectedText "Phase 16 - application dependency container"

$databaseFile = "$databaseRoot\ProjectLedgerDatabase.kt"
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

Write-Host "Phase 16 configuration checks passed." -ForegroundColor Green

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
    -Label "Running quality checks, container tests, repository tests, and debug builds" `
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

    Write-Step "Running application-container startup smoke test"
    $packageName = "com.princevekariya.projectledger.personal.debug"
    $activityName = "com.princevekariya.projectledger.MainActivity"
    $failureReport = ".\Phase16-StartupSmokeFailure.txt"

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

    # PHASE16_FOCUSED_LOGCAT_V2
    $processIdText = ((& $adb shell pidof $packageName 2>$null) | Out-String).Trim()

    # Read only Project Ledger and AndroidRuntime entries. The previous verifier
    # requested the final 3000 lines from every MIUI process, which could discard
    # the early application_container_ready entry on very noisy devices.
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
    $hasKnownLifecycleCrash =
        $focusedLogText.Contains("CompositionLocal LocalLifecycleOwner not present")
    $hasContainerInitializationFailure =
        $focusedLogText.Contains("UninitializedPropertyAccessException") -and
        $focusedLogText.Contains("appContainer")
    $hasContainerReadyLog =
        $focusedLogText.Contains("application_container_ready")

    if (
        [string]::IsNullOrWhiteSpace($processIdText) -or
        $hasFatalException -or
        $hasKnownLifecycleCrash -or
        $hasContainerInitializationFailure -or
        -not $hasContainerReadyLog
    ) {
        $diagnosticLogLines = & $adb logcat -d -v threadtime -t 5000
        $diagnosticLogText = $diagnosticLogLines -join [Environment]::NewLine

        $reportLines = @(
            "Project Ledger Phase 16 startup smoke-test failure",
            "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')",
            "Package: $packageName",
            "Process ID after launch: $processIdText",
            "Container-ready log found: $hasContainerReadyLog",
            "",
            "=== Launch output ===",
            ($launchOutput -join [Environment]::NewLine),
            "",
            "=== Focused Project Ledger Logcat ===",
            $focusedLogText,
            "",
            "=== Diagnostic Logcat ===",
            $diagnosticLogText
        )
        $reportLines | Set-Content -Path $failureReport -Encoding UTF8
        throw "The app did not pass the Phase 16 startup smoke test. See $failureReport."
    }

    Write-Host "Startup smoke test passed. Running PID: $processIdText" -ForegroundColor Green
    Write-Host "Application container initialization was confirmed in Logcat." -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "PHASE 16 VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Composition root: ProjectLedgerApplication"
Write-Host "Container: shared logger and five repository dependencies"
Write-Host "Feature boundary: no DAOs or Room entities exposed"
Write-Host "Schema safety: Room version 1 remains unchanged"
Write-Host "Primary development variant: personalDebug"
