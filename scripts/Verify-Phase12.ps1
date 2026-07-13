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

Write-Step "Checking Phase 12 files and dependency contracts"

$requiredFiles = @(
    ".\core\common\src\main\kotlin\com\princevekariya\projectledger\core\common\AppLogLevel.kt",
    ".\core\common\src\main\kotlin\com\princevekariya\projectledger\core\common\AppLogger.kt",
    ".\core\common\src\main\kotlin\com\princevekariya\projectledger\core\common\NoOpAppLogger.kt",
    ".\core\common\src\main\kotlin\com\princevekariya\projectledger\core\common\SensitiveDataRedactor.kt",
    ".\core\common\src\main\kotlin\com\princevekariya\projectledger\core\common\UserFacingError.kt",
    ".\platform\device\src\main\java\com\princevekariya\projectledger\platform\device\AndroidAppLogger.kt",
    ".\platform\device\src\main\java\com\princevekariya\projectledger\platform\device\AndroidProcessErrorReporter.kt",
    ".\app\src\main\java\com\princevekariya\projectledger\ProjectLedgerApplication.kt",
    ".\docs\PHASE-12-LOGGING-ERROR-HANDLING.md",
    ".\scripts\Verify-Phase12.ps1"
)

foreach ($file in $requiredFiles) {
    Assert-FileExists -Path $file
}

Assert-FileContains `
    -Path ".\feature\dashboard\build.gradle.kts" `
    -ExpectedText 'api(project(":core:common"))'
Assert-FileContains `
    -Path ".\feature\dashboard\build.gradle.kts" `
    -ExpectedText 'api(project(":core:model"))'
Assert-FileContains `
    -Path ".\platform\device\build.gradle.kts" `
    -ExpectedText 'implementation(project(":core:common"))'
Assert-FileContains `
    -Path ".\app\build.gradle.kts" `
    -ExpectedText 'implementation(project(":core:common"))'
Assert-FileContains `
    -Path ".\app\src\main\AndroidManifest.xml" `
    -ExpectedText 'android:name=".ProjectLedgerApplication"'
Assert-FileContains `
    -Path ".\core\common\src\main\kotlin\com\princevekariya\projectledger\core\common\SensitiveDataRedactor.kt" `
    -ExpectedText '[redacted-number]'
Assert-FileContains `
    -Path ".\platform\device\src\main\java\com\princevekariya\projectledger\platform\device\AndroidAppLogger.kt" `
    -ExpectedText 'includeThrowableDetails'
Assert-FileContains `
    -Path ".\feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\DashboardViewModel.kt" `
    -ExpectedText 'transaction_draft_validated'

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

Write-Host "Phase 12 configuration checks passed." -ForegroundColor Green

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
    -Label "Running quality checks, error tests, state tests, and both debug builds" `
    -Command {
        .\gradlew.bat `
            qualityCheck `
            :core:common:test `
            :core:model:test `
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
}

# PHASE12_STARTUP_SMOKE_TEST
if ($InstallOnPhone) {
    Write-Step "Running application startup smoke test"

    $packageName = "com.princevekariya.projectledger.personal.debug"
    $activityName = "com.princevekariya.projectledger.MainActivity"
    $failureReport = ".\Phase12-StartupSmokeFailure.txt"

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
    $logLines = & $adb logcat -d -v threadtime
    $logText = $logLines -join [Environment]::NewLine

    $hasProjectFatalException =
        $logText.Contains("FATAL EXCEPTION") -and
        $logText.Contains($packageName)

    $hasKnownLifecycleCrash =
        $logText.Contains("CompositionLocal LocalLifecycleOwner not present")

    if (
        [string]::IsNullOrWhiteSpace($processIdText) -or
        $hasProjectFatalException -or
        $hasKnownLifecycleCrash
    ) {
        $reportLines = @(
            "Project Ledger Phase 12 startup smoke-test failure",
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
Write-Host "PHASE 12 VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Logging: structured levels with release-safe redaction"
Write-Host "Process failures: recorded and delegated to Android's handler"
Write-Host "User errors: stable messages without internal exception details"
Write-Host "Dashboard drafts: validated before future persistence"
Write-Host "Primary development variant: personalDebug"
