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

function Assert-XmlParses {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        throw "Required XML file is missing: $Path"
    }

    try {
        $parsedXml = [xml](Get-Content -LiteralPath $Path -Raw)
        if ($null -eq $parsedXml.DocumentElement) {
            throw "XML has no document element."
        }
    } catch {
        throw "Invalid XML in '$Path': $($_.Exception.Message)"
    }
}

$projectRoot = Resolve-ProjectRoot -RequestedPath $ProjectPath
Set-Location $projectRoot

$navigationRoot = ".\app\src\main\java\com\princevekariya\projectledger\navigation"
$dashboardFile = ".\feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\FoundationDashboardUiState.kt"
$transactionsScreen = ".\feature\transactions\src\main\java\com\princevekariya\projectledger\feature\transactions\TransactionsPlaceholderScreen.kt"
$manifestFile = ".\app\src\main\AndroidManifest.xml"
$catalogFile = ".\gradle\libs.versions.toml"
$appBuildFile = ".\app\build.gradle.kts"

$requiredFiles = @(
    "$navigationRoot\LedgerDestination.kt",
    "$navigationRoot\ProjectLedgerApp.kt",
    "$navigationRoot\ProjectLedgerBottomBar.kt",
    "$navigationRoot\ProjectLedgerNavHost.kt",
    "$navigationRoot\FutureFeatureScreen.kt",
    $transactionsScreen,
    ".\app\src\test\java\com\princevekariya\projectledger\navigation\LedgerDestinationTest.kt",
    ".\app\src\main\res\drawable\ic_navigation_home.xml",
    ".\app\src\main\res\drawable\ic_navigation_transactions.xml",
    ".\app\src\main\res\drawable\ic_navigation_reports.xml",
    ".\app\src\main\res\drawable\ic_navigation_settings.xml",
    ".\docs\PHASE-10-NAVIGATION-SHELL.md",
    ".\scripts\Verify-Phase10.ps1"
)

Write-Step "Checking Phase 10 files and navigation contracts"
foreach ($file in $requiredFiles) {
    if (-not (Test-Path $file)) {
        throw "Missing required file: $file"
    }
    Write-Host "Found: $file"
}

Assert-FileContains -Path $catalogFile -ExpectedText 'navigation-compose = "2.7.7"'
Assert-FileContains -Path $catalogFile -ExpectedText "androidx-navigation-compose"
Assert-FileContains -Path $appBuildFile -ExpectedText 'implementation(project(":feature:transactions"))'
Assert-FileContains -Path $appBuildFile -ExpectedText "implementation(libs.bundles.compose)"
Assert-FileContains -Path $appBuildFile -ExpectedText "implementation(libs.androidx.navigation.compose)"
Assert-FileContains -Path ".\app\src\main\java\com\princevekariya\projectledger\MainActivity.kt" `
    -ExpectedText "ProjectLedgerApp("
Assert-FileContains -Path "$navigationRoot\LedgerDestination.kt" -ExpectedText "enum class LedgerDestination"
Assert-FileContains -Path "$navigationRoot\ProjectLedgerBottomBar.kt" -ExpectedText "NavigationBar("
Assert-FileContains -Path "$navigationRoot\ProjectLedgerNavHost.kt" -ExpectedText "NavHost("
Assert-FileContains -Path "$navigationRoot\ProjectLedgerNavHost.kt" -ExpectedText "TransactionsPlaceholderScreen()"
Assert-FileContains -Path $transactionsScreen -ExpectedText "fun TransactionsPlaceholderScreen("
Assert-FileContains -Path $dashboardFile -ExpectedText "Phase 10 - navigation shell foundation"

$destinations = @("home", "transactions", "reports", "settings")
$destinationPath = (Resolve-Path "$navigationRoot\LedgerDestination.kt").Path
$manifestPath = (Resolve-Path $manifestFile).Path
$destinationFile = [System.IO.File]::ReadAllText($destinationPath)
$manifestContent = [System.IO.File]::ReadAllText($manifestPath)

foreach ($destination in $destinations) {
    $routeText = "route = `"$destination`""
    $deepLinkText = "projectledger://$destination"
    $manifestHostText = "android:host=`"$destination`""

    if (-not $destinationFile.Contains($routeText)) {
        throw "Destination route is missing: $destination"
    }
    if (-not $destinationFile.Contains($deepLinkText)) {
        throw "Destination deep link is missing: $deepLinkText"
    }
    if (-not $manifestContent.Contains($manifestHostText)) {
        throw "Manifest deep-link host is missing: $destination"
    }
}

$routeMatches = [regex]::Matches($destinationFile, 'route\s*=\s*"([^"]+)"')
$routeValues = @($routeMatches | ForEach-Object { $_.Groups[1].Value })
if ($routeValues.Count -ne 4) {
    throw "Expected 4 navigation routes, but found $($routeValues.Count)."
}
if (($routeValues | Select-Object -Unique).Count -ne $routeValues.Count) {
    throw "Navigation routes must be unique."
}

Assert-XmlParses -Path $manifestFile
Assert-XmlParses -Path ".\app\src\main\res\drawable\ic_navigation_home.xml"
Assert-XmlParses -Path ".\app\src\main\res\drawable\ic_navigation_transactions.xml"
Assert-XmlParses -Path ".\app\src\main\res\drawable\ic_navigation_reports.xml"
Assert-XmlParses -Path ".\app\src\main\res\drawable\ic_navigation_settings.xml"

$weightImports = Get-ChildItem -Path $projectRoot -Recurse -File -Filter "*.kt" |
    Where-Object {
        $_.FullName -notlike "*\.gradle\*" -and
        $_.FullName -notlike "*\build\*" -and
        $_.FullName -notlike "*\.phase-backups\*" -and
        $_.FullName -notlike "*\phase-*-update\*"
    } |
    Select-String -SimpleMatch "import androidx.compose.foundation.layout.weight"

if ($weightImports) {
    throw "An explicit scoped weight import was found."
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
    throw "A camelCase const val remains in the source tree."
}

Assert-NoEmptyKotlinFiles -Root $projectRoot
Write-Host "Phase 10 configuration checks passed." -ForegroundColor Green

if ($ConfigurationOnly) {
    Write-Host "`nPHASE 10 CONFIGURATION CHECK PASSED" -ForegroundColor Green
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
Write-Host "PHASE 10 VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Bottom destinations: Home, Transactions, Reports, and Settings"
Write-Host "Navigation behavior: single-top selection with state restoration"
Write-Host "Deep-link scheme: projectledger"
Write-Host "Primary development variant: personalDebug"
