[CmdletBinding()]
param(
    [string]$ProjectPath,
    [switch]$InstallOnPhone
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

    $content = Get-Content -Raw -Path $Path
    if (-not $content.Contains($ExpectedText)) {
        throw "Expected text '$ExpectedText' was not found in '$Path'."
    }
}

$projectRoot = Resolve-ProjectRoot -RequestedPath $ProjectPath
Set-Location $projectRoot

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

Write-Step "Checking Phase 5 module declarations"
$expectedModules = @(
    ':app',
    ':core:common',
    ':core:model',
    ':core:designsystem',
    ':domain:transactions',
    ':feature:dashboard',
    ':feature:transactions',
    ':platform:device'
)
foreach ($module in $expectedModules) {
    Assert-FileContains -Path ".\settings.gradle.kts" -ExpectedText "include(`"$module`")"
    Write-Host "Declared: $module" -ForegroundColor Green
}

Write-Step "Checking required Phase 5 files"
$requiredFiles = @(
    ".\core\common\build.gradle.kts",
    ".\core\model\build.gradle.kts",
    ".\core\designsystem\build.gradle.kts",
    ".\domain\transactions\build.gradle.kts",
    ".\feature\dashboard\build.gradle.kts",
    ".\feature\transactions\build.gradle.kts",
    ".\platform\device\build.gradle.kts",
    ".\feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\FoundationDashboard.kt",
    ".\platform\device\src\main\java\com\princevekariya\projectledger\platform\device\AndroidDeviceInfoProvider.kt",
    ".\docs\PHASE-5-MODULE-STRUCTURE.md"
)
foreach ($file in $requiredFiles) {
    if (-not (Test-Path $file)) {
        throw "Missing required file: $file"
    }
    Write-Host "Found: $file"
}

Write-Step "Checking dependency boundaries"
Assert-FileContains -Path ".\app\build.gradle.kts" -ExpectedText 'implementation(project(":feature:dashboard"))'
Assert-FileContains -Path ".\feature\transactions\build.gradle.kts" -ExpectedText 'implementation(project(":domain:transactions"))'
Assert-FileContains -Path ".\domain\transactions\build.gradle.kts" -ExpectedText 'implementation(project(":core:model"))'

$domainBuild = Get-Content -Raw ".\domain\transactions\build.gradle.kts"
if ($domainBuild -match "com.android.library" -or $domainBuild -match "kotlin.android") {
    throw "The domain module must remain pure Kotlin and must not apply Android plugins."
}

$oldThemePath = ".\app\src\main\java\com\princevekariya\projectledger\ui\theme"
if (Test-Path $oldThemePath) {
    throw "The old app-local theme directory still exists: $oldThemePath"
}
Write-Host "Dependency boundaries look correct." -ForegroundColor Green

Invoke-CheckedCommand -Label "Stopping previous Gradle daemons" -Command {
    & .\gradlew.bat --stop
}

Invoke-CheckedCommand -Label "Testing pure Kotlin modules" -Command {
    & .\gradlew.bat `
        :core:common:test `
        :core:model:test `
        :domain:transactions:test `
        --no-daemon `
        --max-workers=1 `
        --console=plain
}

Invoke-CheckedCommand -Label "Compiling Android library modules and app variants" -Command {
    & .\gradlew.bat `
        :core:designsystem:assembleDebug `
        :feature:dashboard:assembleDebug `
        :feature:transactions:assembleDebug `
        :platform:device:assembleDebug `
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

    Invoke-CheckedCommand -Label "Installing the Phase 5 personal debug app" -Command {
        & $adb install -r $personalApk
    }
}

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "PHASE 5 VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Gradle modules: 8"
Write-Host "Architecture layers: app, core, domain, feature, platform"
Write-Host "Primary development variant: personalDebug"
Write-Host "The app module is now a thin composition root."
