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

    return (Split-Path -Parent $candidate)
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

$projectRoot = Resolve-ProjectRoot -RequestedPath $ProjectPath
Set-Location $projectRoot

$themeRoot = ".\core\designsystem\src\main\java\com\princevekariya\projectledger\core\designsystem\theme"
$requiredFiles = @(
    "$themeRoot\Color.kt",
    "$themeRoot\LedgerExtendedColors.kt",
    "$themeRoot\LedgerSpacing.kt",
    "$themeRoot\LedgerElevation.kt",
    "$themeRoot\LedgerMotion.kt",
    "$themeRoot\LedgerShapes.kt",
    "$themeRoot\LedgerTypography.kt",
    "$themeRoot\Theme.kt",
    ".\feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\FoundationDashboardUiState.kt",
    ".\docs\PHASE-8-DESIGN-TOKENS.md",
    ".\scripts\Verify-Phase8.ps1"
)

Write-Step "Checking Phase 8 files and token contracts"
foreach ($file in $requiredFiles) {
    if (-not (Test-Path $file)) {
        throw "Missing required file: $file"
    }
    Write-Host "Found: $file"
}

if (Test-Path "$themeRoot\Type.kt") {
    throw "Legacy Type.kt still exists. Re-run Apply-Phase8.ps1."
}

Assert-FileContains -Path "$themeRoot\Theme.kt" -ExpectedText "LedgerDarkColorScheme"
Assert-FileContains -Path "$themeRoot\Theme.kt" -ExpectedText "LocalLedgerExtendedColors provides LedgerDarkExtendedColors"
Assert-FileContains -Path "$themeRoot\Theme.kt" -ExpectedText "LocalLedgerSpacing provides DefaultLedgerSpacing"
Assert-FileContains -Path "$themeRoot\Theme.kt" -ExpectedText "typography = LedgerTypography"
Assert-FileContains -Path "$themeRoot\Theme.kt" -ExpectedText "shapes = LedgerShapes"
Assert-FileContains -Path "$themeRoot\LedgerExtendedColors.kt" -ExpectedText "val MaterialTheme.ledgerColors"
Assert-FileContains -Path "$themeRoot\LedgerSpacing.kt" -ExpectedText "val MaterialTheme.ledgerSpacing"
Assert-FileContains -Path ".\feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\FoundationDashboardUiState.kt" -ExpectedText "design-token foundation"
Assert-FileContains -Path ".\.github\workflows\android-ci.yml" -ExpectedText "android-actions/setup-android@v4"

Write-Host "Phase 8 configuration checks passed." -ForegroundColor Green

if ($ConfigurationOnly) {
    Write-Host "`nPHASE 8 CONFIGURATION CHECK PASSED" -ForegroundColor Green
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

Invoke-CheckedCommand -Label "Running formatting and static-analysis gates" -Command {
    & .\gradlew.bat `
        qualityCheck `
        --no-daemon `
        --max-workers=1 `
        --console=plain
}

Invoke-CheckedCommand -Label "Compiling design-system and dashboard modules" -Command {
    & .\gradlew.bat `
        :core:designsystem:compileDebugKotlin `
        :feature:dashboard:compileDebugKotlin `
        --no-daemon `
        --max-workers=1 `
        --console=plain
}

Invoke-CheckedCommand -Label "Testing and building both debug editions" -Command {
    & .\gradlew.bat `
        :app:testPersonalDebugUnitTest `
        :app:testPlayDebugUnitTest `
        :app:assemblePersonalDebug `
        :app:assemblePlayDebug `
        --no-daemon `
        --max-workers=1 `
        --console=plain
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
Write-Host "PHASE 8 VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Theme: Material 3 dark finance palette"
Write-Host "Extended roles: income, expense, warning, and information"
Write-Host "Token groups: colors, typography, shapes, spacing, elevation, and motion"
Write-Host "Primary development variant: personalDebug"
