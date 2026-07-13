[CmdletBinding()]
param(
    [string]$ProjectPath,
    [switch]$InstallOnPhone,
    [switch]$ConfigurationOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Resolve-ProjectRoot {
    param([string]$RequestedPath)

    if (-not [string]::IsNullOrWhiteSpace($RequestedPath)) {
        return (Resolve-Path $RequestedPath).Path
    }

    $candidate = Split-Path -Parent $PSScriptRoot
    if (Test-Path (Join-Path $candidate "settings.gradle.kts")) {
        return $candidate
    }

    throw "Could not locate the ProjectLedger root folder."
}

function Invoke-CheckedCommand {
    param(
        [string]$Label,
        [scriptblock]$Command
    )

    Write-Step $Label
    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE."
    }
}

function Assert-FileContains {
    param(
        [string]$Path,
        [string]$ExpectedText
    )

    if (-not (Test-Path $Path)) {
        throw "Required file is missing: $Path"
    }

    $content = [System.IO.File]::ReadAllText($Path)
    if (-not $content.Contains($ExpectedText)) {
        throw "Expected text '$ExpectedText' was not found in '$Path'."
    }
}

function Assert-FileDoesNotContain {
    param(
        [string]$Path,
        [string]$UnexpectedText
    )

    if (-not (Test-Path $Path)) {
        throw "Required file is missing: $Path"
    }

    $content = [System.IO.File]::ReadAllText($Path)
    if ($content.Contains($UnexpectedText)) {
        throw "Unexpected text '$UnexpectedText' was found in '$Path'."
    }
}

function Get-ProjectKotlinFiles {
    param([string]$Root)

    return Get-ChildItem -Path $Root -Recurse -File -Filter "*.kt" |
        Where-Object {
            $_.FullName -notlike "*\.gradle\*" -and
            $_.FullName -notlike "*\build\*" -and
            $_.FullName -notlike "*\.phase-backups\*" -and
            $_.FullName -notlike "*\phase-*-update\*"
        }
}

function Assert-NoEmptyKotlinFiles {
    param([string]$Root)

    foreach ($file in Get-ProjectKotlinFiles -Root $Root) {
        $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
        $text = [System.Text.Encoding]::UTF8.GetString($bytes)
        $text = $text.TrimStart([char]0xFEFF).Trim()
        if ([string]::IsNullOrWhiteSpace($text)) {
            throw "Empty Kotlin source file found: $($file.FullName)"
        }
    }
}

$projectRoot = Resolve-ProjectRoot -RequestedPath $ProjectPath
Set-Location $projectRoot

$commonRoot = ".\core\common\src\main\kotlin\com\princevekariya\projectledger\core\common"
$dashboardRoot = ".\feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard"
$dashboardTestRoot = ".\feature\dashboard\src\test\java\com\princevekariya\projectledger\feature\dashboard"
$navigationRoot = ".\app\src\main\java\com\princevekariya\projectledger\navigation"
$catalogFile = ".\gradle\libs.versions.toml"
$dashboardBuildFile = ".\feature\dashboard\build.gradle.kts"
$ledgerStateFile = ".\core\designsystem\src\main\java\com\princevekariya\projectledger\core\designsystem\component\LedgerState.kt"
$workflowFile = ".\.github\workflows\android-ci.yml"
$oldDashboardFile = "$dashboardRoot\FoundationDashboardUiState.kt"

$requiredFiles = @(
    "$commonRoot\UiLoadState.kt",
    "$commonRoot\UiMessage.kt",
    ".\core\common\src\test\kotlin\com\princevekariya\projectledger\core\common\UiStateContractTest.kt",
    "$dashboardRoot\DashboardAction.kt",
    "$dashboardRoot\DashboardRoute.kt",
    "$dashboardRoot\DashboardUiState.kt",
    "$dashboardRoot\DashboardViewModel.kt",
    "$dashboardRoot\DashboardViewModelFactory.kt",
    "$dashboardRoot\FoundationDashboard.kt",
    "$dashboardTestRoot\DashboardViewModelTest.kt",
    $ledgerStateFile,
    "$navigationRoot\ProjectLedgerApp.kt",
    "$navigationRoot\ProjectLedgerNavHost.kt",
    ".\app\src\main\java\com\princevekariya\projectledger\MainActivity.kt",
    ".\docs\PHASE-11-APPLICATION-STATE.md",
    ".\scripts\Verify-Phase11.ps1"
)

Write-Step "Checking Phase 11 files and state contracts"
foreach ($file in $requiredFiles) {
    if (-not (Test-Path $file)) {
        throw "Missing required file: $file"
    }
    Write-Host "Found: $file"
}

if (Test-Path $oldDashboardFile) {
    throw "Obsolete combined dashboard file still exists: $oldDashboardFile"
}

Assert-FileContains -Path $catalogFile -ExpectedText "androidx-lifecycle-runtime-compose"
Assert-FileContains -Path $catalogFile -ExpectedText "androidx-lifecycle-viewmodel-ktx"
Assert-FileContains -Path $catalogFile -ExpectedText "androidx-lifecycle-viewmodel-compose"
Assert-FileContains -Path $dashboardBuildFile -ExpectedText 'api(project(":core:common"))'
Assert-FileContains -Path $dashboardBuildFile -ExpectedText 'api(project(":core:model"))'
Assert-FileContains -Path $dashboardBuildFile -ExpectedText "implementation(libs.androidx.lifecycle.runtime.compose)"
Assert-FileContains -Path $dashboardBuildFile -ExpectedText "implementation(libs.androidx.lifecycle.viewmodel.ktx)"
Assert-FileContains -Path $dashboardBuildFile -ExpectedText "implementation(libs.androidx.lifecycle.viewmodel.compose)"
Assert-FileContains -Path $dashboardBuildFile -ExpectedText "testImplementation(libs.junit4)"
Assert-FileContains -Path "$commonRoot\UiLoadState.kt" -ExpectedText "sealed interface UiLoadState"
Assert-FileContains -Path "$commonRoot\UiLoadState.kt" -ExpectedText "data object Loading"
Assert-FileContains -Path "$commonRoot\UiLoadState.kt" -ExpectedText "data class Error("
Assert-FileContains -Path "$commonRoot\UiMessage.kt" -ExpectedText "data class UiMessage("
Assert-FileContains -Path "$dashboardRoot\DashboardAction.kt" -ExpectedText "sealed interface DashboardAction"
Assert-FileContains -Path "$dashboardRoot\DashboardUiState.kt" -ExpectedText "data class DashboardUiState("
Assert-FileContains -Path "$dashboardRoot\DashboardViewModel.kt" -ExpectedText "MutableStateFlow(initialState)"
Assert-FileContains -Path "$dashboardRoot\DashboardViewModel.kt" -ExpectedText "fun onAction(action: DashboardAction)"
Assert-FileContains -Path "$dashboardRoot\DashboardRoute.kt" -ExpectedText "collectAsStateWithLifecycle()"
Assert-FileContains -Path "$dashboardRoot\DashboardRoute.kt" -ExpectedText "SnackbarHostState()"
Assert-FileContains -Path "$dashboardRoot\FoundationDashboard.kt" -ExpectedText "Phase 11 - application state foundation"
Assert-FileDoesNotContain -Path "$dashboardRoot\FoundationDashboard.kt" -UnexpectedText "mutableStateOf("
Assert-FileDoesNotContain -Path "$dashboardRoot\FoundationDashboard.kt" -UnexpectedText "remember {"
Assert-FileContains -Path $ledgerStateFile -ExpectedText "fun LedgerErrorState("
Assert-FileContains -Path "$navigationRoot\ProjectLedgerNavHost.kt" -ExpectedText "DashboardRoute(initialState = dashboardInitialState)"
Assert-FileContains -Path ".\app\src\main\java\com\princevekariya\projectledger\MainActivity.kt" `
    -ExpectedText "dashboardInitialState = DashboardUiState("
Assert-FileContains -Path $workflowFile -ExpectedText ":feature:dashboard:testDebugUnitTest"

$weightImports = Get-ProjectKotlinFiles -Root $projectRoot |
    Select-String -SimpleMatch "import androidx.compose.foundation.layout.weight"
if ($weightImports) {
    throw "An explicit scoped Compose weight import was found."
}

$camelCaseConstants = Get-ProjectKotlinFiles -Root $projectRoot |
    Select-String -Pattern "\bconst\s+val\s+[a-z]" -CaseSensitive
if ($camelCaseConstants) {
    throw "A camelCase const val remains in the source tree."
}

Assert-NoEmptyKotlinFiles -Root $projectRoot
Write-Host "Phase 11 configuration checks passed." -ForegroundColor Green

if ($ConfigurationOnly) {
    Write-Host "`nPHASE 11 CONFIGURATION CHECK PASSED" -ForegroundColor Green
    exit 0
}

if (-not (Test-Path ".\gradlew.bat")) {
    throw "gradlew.bat was not found in '$projectRoot'."
}

Write-Step "Configuring Android Studio's JDK 17"
$studioJdk = "C:\Program Files\Android\Android Studio\jbr"
if (-not (Test-Path (Join-Path $studioJdk "bin\java.exe"))) {
    throw "Android Studio bundled JDK was not found at '$studioJdk'."
}
$env:JAVA_HOME = $studioJdk
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
& "$env:JAVA_HOME\bin\java.exe" -version

Write-Step "Configuring Android SDK"
$androidSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
if (-not (Test-Path $androidSdk)) {
    throw "Android SDK was not found at '$androidSdk'."
}
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk
Write-Host "Android SDK: $androidSdk" -ForegroundColor Green

Invoke-CheckedCommand -Label "Stopping previous Gradle daemons" -Command {
    & .\gradlew.bat --stop
}

Invoke-CheckedCommand -Label "Applying deterministic formatting" -Command {
    & .\gradlew.bat `
        spotlessApply `
        --no-daemon `
        --max-workers=1 `
        --console=plain
}

Invoke-CheckedCommand -Label "Running quality checks, state tests, and both debug builds" -Command {
    & .\gradlew.bat `
        qualityCheck `
        :core:common:test `
        :core:model:test `
        :domain:transactions:test `
        :feature:dashboard:testDebugUnitTest `
        :core:designsystem:assembleDebug `
        :feature:dashboard:assembleDebug `
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

$personalApk = Join-Path $projectRoot "app\build\outputs\apk\personal\debug\app-personal-debug.apk"
$playApk = Join-Path $projectRoot "app\build\outputs\apk\play\debug\app-play-debug.apk"

foreach ($apk in @($personalApk, $playApk)) {
    if (-not (Test-Path $apk)) {
        throw "Expected APK was not created: $apk"
    }
    Write-Host "Built: $apk" -ForegroundColor Green
}

if ($InstallOnPhone) {
    Write-Step "Checking connected Android phone"
    $adb = Join-Path $androidSdk "platform-tools\adb.exe"
    if (-not (Test-Path $adb)) {
        throw "ADB was not found at '$adb'."
    }

    $deviceLines = & $adb devices
    $authorized = $deviceLines | Where-Object { $_ -match "\sdevice$" }
    if (-not $authorized) {
        throw "No authorized Android phone was detected. Connect and unlock the phone."
    }
    & $adb devices -l

    Invoke-CheckedCommand -Label "Updating Project Ledger Personal Dev on the phone" -Command {
        & $adb install -r $personalApk
    }
}

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "PHASE 11 VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "State owner: lifecycle-aware DashboardViewModel"
Write-Host "State flow: immutable DashboardUiState through StateFlow"
Write-Host "User input: DashboardAction with one-way data flow"
Write-Host "One-off feedback: identified UiMessage displayed through Snackbar"
Write-Host "Primary development variant: personalDebug"
