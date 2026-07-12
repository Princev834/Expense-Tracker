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

Write-Step "Checking Phase 6 configuration files"
$requiredFiles = @(
    ".\.editorconfig",
    ".\config\detekt\detekt.yml",
    ".\.githooks\pre-commit",
    ".\scripts\Run-QualityChecks.ps1",
    ".\scripts\PreCommitQualityCheck.ps1",
    ".\docs\PHASE-6-CODE-QUALITY.md"
)
foreach ($file in $requiredFiles) {
    if (-not (Test-Path $file)) {
        throw "Missing required file: $file"
    }
    Write-Host "Found: $file"
}

Assert-FileContains -Path ".\gradle\libs.versions.toml" -ExpectedText 'detekt = "1.23.6"'
Assert-FileContains -Path ".\gradle\libs.versions.toml" -ExpectedText 'spotless = "6.25.0"'
Assert-FileContains -Path ".\build.gradle.kts" -ExpectedText 'tasks.register("qualityCheck")'
Assert-FileContains -Path ".\build.gradle.kts" -ExpectedText 'allWarningsAsErrors'
Assert-FileContains -Path ".\gradle.properties" -ExpectedText 'warningsAsErrors=true'
Write-Host "Quality configuration looks correct." -ForegroundColor Green

if (Test-Path ".\.git") {
    $hooksPath = (& git config --get core.hooksPath) -join ""
    if ($hooksPath -ne ".githooks") {
        throw "Git hooks path is '$hooksPath'. Expected '.githooks'. Re-run Apply-Phase6.ps1."
    }
    Write-Host "Git pre-commit hook path: $hooksPath" -ForegroundColor Green
} else {
    Write-Warning "No local Git repository was detected; hook-path verification was skipped."
}

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

Invoke-CheckedCommand -Label "Running unit tests and building the personal debug APK" -Command {
    & .\gradlew.bat `
        :core:common:test `
        :core:model:test `
        :domain:transactions:test `
        :app:testPersonalDebugUnitTest `
        :app:assemblePersonalDebug `
        --no-daemon `
        --max-workers=1 `
        --console=plain
}

$personalApk = Join-Path $projectRoot "app\build\outputs\apk\personal\debug\app-personal-debug.apk"
if (-not (Test-Path $personalApk)) {
    throw "Expected APK was not created: $personalApk"
}
Write-Host "Built: $personalApk" -ForegroundColor Green

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
Write-Host "PHASE 6 VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Formatting: Spotless + ktlint"
Write-Host "Static analysis: detekt"
Write-Host "Kotlin compiler warnings: treated as errors"
Write-Host "Git quality gate: .githooks/pre-commit"
Write-Host "Primary development variant: personalDebug"
