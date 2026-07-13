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

function Assert-NoEmptyKotlinFiles {
    param([string]$Root)

    $files = Get-ChildItem -Path $Root -Recurse -File -Filter "*.kt" |
        Where-Object {
            $_.FullName -notlike "*\.gradle\*" -and
            $_.FullName -notlike "*\build\*" -and
            $_.FullName -notlike "*\.phase-backups\*" -and
            $_.FullName -notlike "*\phase-*-update\*"
        }

    foreach ($file in $files) {
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

$componentRoot = ".\core\designsystem\src\main\java\com\princevekariya\projectledger\core\designsystem\component"
$dashboardFile = ".\feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\FoundationDashboardUiState.kt"
$requiredFiles = @(
    "$componentRoot\LedgerButton.kt",
    "$componentRoot\LedgerCard.kt",
    "$componentRoot\LedgerMetricTone.kt",
    "$componentRoot\LedgerTextField.kt",
    "$componentRoot\LedgerTransactionRow.kt",
    "$componentRoot\LedgerTransactionDirection.kt",
    "$componentRoot\LedgerState.kt",
    $dashboardFile,
    ".\docs\PHASE-9-REUSABLE-COMPONENTS.md",
    ".\scripts\Verify-Phase9.ps1"
)

Write-Step "Checking Phase 9 files and component contracts"
foreach ($file in $requiredFiles) {
    if (-not (Test-Path $file)) {
        throw "Missing required file: $file"
    }
    Write-Host "Found: $file"
}

Assert-FileContains -Path "$componentRoot\LedgerButton.kt" -ExpectedText "fun LedgerPrimaryButton("
Assert-FileContains -Path "$componentRoot\LedgerButton.kt" -ExpectedText "fun LedgerSecondaryButton("
Assert-FileContains -Path "$componentRoot\LedgerCard.kt" -ExpectedText "fun LedgerSurfaceCard("
Assert-FileContains -Path "$componentRoot\LedgerCard.kt" -ExpectedText "fun LedgerMetricCard("
Assert-FileContains -Path "$componentRoot\LedgerMetricTone.kt" -ExpectedText "enum class LedgerMetricTone"
Assert-FileContains -Path "$componentRoot\LedgerTextField.kt" -ExpectedText "fun LedgerTextField("
Assert-FileContains -Path "$componentRoot\LedgerTextField.kt" -ExpectedText "fun LedgerAmountField("
Assert-FileContains -Path "$componentRoot\LedgerTransactionRow.kt" -ExpectedText "fun LedgerTransactionRow("
Assert-FileContains -Path "$componentRoot\LedgerTransactionDirection.kt" -ExpectedText "enum class LedgerTransactionDirection"
Assert-FileContains -Path "$componentRoot\LedgerState.kt" -ExpectedText "fun LedgerEmptyState("
Assert-FileContains -Path "$componentRoot\LedgerState.kt" -ExpectedText "fun LedgerLoadingState("
Assert-FileContains -Path $dashboardFile -ExpectedText "Phase 9 - reusable component foundation"
Assert-FileContains -Path ".\gradle\libs.versions.toml" -ExpectedText "androidx-compose-foundation"
Assert-FileContains -Path ".\core\designsystem\src\main\java\com\princevekariya\projectledger\core\designsystem\theme\LedgerMotion.kt" -ExpectedText "const val INSTANT_MILLIS"

$weightImports = Get-ChildItem -Path $projectRoot -Recurse -File -Filter "*.kt" |
    Where-Object {
        $_.FullName -notlike "*\.gradle\*" -and
        $_.FullName -notlike "*\build\*" -and
        $_.FullName -notlike "*\.phase-backups\*" -and
        $_.FullName -notlike "*\phase-*-update\*"
    } |
    Select-String -SimpleMatch "import androidx.compose.foundation.layout.weight"

if ($weightImports) {
    throw "An explicit scoped weight import was found. Remove it and use weight only inside RowScope or ColumnScope."
}

$camelCaseConstants = Get-ChildItem -Path $projectRoot -Recurse -File -Filter "*.kt" |
    Where-Object {
        $_.FullName -notlike "*\.gradle\*" -and
        $_.FullName -notlike "*\build\*" -and
        $_.FullName -notlike "*\.phase-backups\*" -and
        $_.FullName -notlike "*\phase-*-update\*"
    } |
    Select-String -Pattern "\bconst\s+val\s+[a-z]" -CaseSensitive

if ($camelCaseConstants) {
    throw "A camelCase const val remains in the source tree. Constants must use upper snake case."
}

Assert-NoEmptyKotlinFiles -Root $projectRoot
Write-Host "Phase 9 configuration checks passed." -ForegroundColor Green

if ($ConfigurationOnly) {
    Write-Host "`nPHASE 9 CONFIGURATION CHECK PASSED" -ForegroundColor Green
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

Invoke-CheckedCommand -Label "Running quality checks, tests, and both debug builds" -Command {
    & .\gradlew.bat `
        qualityCheck `
        :core:common:test `
        :core:model:test `
        :domain:transactions:test `
        :core:designsystem:assembleDebug `
        :feature:dashboard:assembleDebug `
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
Write-Host "PHASE 9 VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Reusable buttons: primary and secondary"
Write-Host "Reusable cards: surface and financial metric"
Write-Host "Reusable fields: text and amount"
Write-Host "Reusable rows and states: transaction, empty, and loading"
Write-Host "Primary development variant: personalDebug"
