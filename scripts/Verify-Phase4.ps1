[CmdletBinding()]
param(
    [string]$ProjectPath,
    [switch]$InstallPersonalOnPhone,
    [switch]$InstallBothOnPhone
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

function Get-ApkDump {
    param(
        [string]$Aapt2Path,
        [string]$Command,
        [string]$ApkPath
    )

    $output = (& $Aapt2Path dump $Command $ApkPath 2>&1) -join "`n"
    if ($LASTEXITCODE -ne 0) {
        throw "aapt2 could not inspect '$ApkPath'.`n$output"
    }
    return $output
}

function Assert-PackageName {
    param(
        [string]$Aapt2Path,
        [string]$ApkPath,
        [string]$ExpectedPackage
    )

    $badging = Get-ApkDump -Aapt2Path $Aapt2Path -Command "badging" -ApkPath $ApkPath
    $match = [regex]::Match($badging, "package: name='([^']+)'", [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    if (-not $match.Success) {
        throw "Could not read the application ID from '$ApkPath'."
    }

    $actualPackage = $match.Groups[1].Value
    if ($actualPackage -ne $ExpectedPackage) {
        throw "Unexpected application ID in '$ApkPath'. Expected '$ExpectedPackage' but found '$actualPackage'."
    }
    Write-Host "Verified package: $actualPackage" -ForegroundColor Green
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

Write-Step "Checking Phase 4 files"
$requiredFiles = @(
    ".\app\src\personal\AndroidManifest.xml",
    ".\app\src\play\AndroidManifest.xml",
    ".\app\src\personalDebug\res\values\strings.xml",
    ".\app\src\playDebug\res\values\strings.xml",
    ".\app\src\main\java\com\princevekariya\projectledger\config\AppDistribution.kt",
    ".\docs\PHASE-4-PRODUCT-FLAVORS.md"
)
foreach ($file in $requiredFiles) {
    if (-not (Test-Path $file)) {
        throw "Missing required file: $file"
    }
    Write-Host "Found: $file"
}

Invoke-CheckedCommand -Label "Stopping previous Gradle daemons" -Command {
    & .\gradlew.bat --stop
}

Invoke-CheckedCommand -Label "Testing and building all Phase 4 variants" -Command {
    & .\gradlew.bat `
        clean `
        testPersonalDebugUnitTest `
        testPlayDebugUnitTest `
        assemblePersonalDebug `
        assemblePlayDebug `
        assemblePersonalRelease `
        assemblePlayRelease `
        --no-daemon `
        --max-workers=1 `
        --console=plain
}

$apkPaths = @{
    PersonalDebug = Join-Path $projectRoot "app\build\outputs\apk\personal\debug\app-personal-debug.apk"
    PlayDebug = Join-Path $projectRoot "app\build\outputs\apk\play\debug\app-play-debug.apk"
    PersonalRelease = Join-Path $projectRoot "app\build\outputs\apk\personal\release\app-personal-release-unsigned.apk"
    PlayRelease = Join-Path $projectRoot "app\build\outputs\apk\play\release\app-play-release-unsigned.apk"
}

Write-Step "Checking APK outputs"
foreach ($entry in $apkPaths.GetEnumerator()) {
    if (-not (Test-Path $entry.Value)) {
        throw "$($entry.Key) APK was not created at '$($entry.Value)'."
    }
    Write-Host "$($entry.Key): $($entry.Value)" -ForegroundColor Green
}

Write-Step "Finding Android APK inspection tool"
$buildToolsRoot = Join-Path $androidSdk "build-tools"
$aapt2 = Get-ChildItem -Path $buildToolsRoot -Directory |
    Sort-Object Name -Descending |
    ForEach-Object { Join-Path $_.FullName "aapt2.exe" } |
    Where-Object { Test-Path $_ } |
    Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($aapt2)) {
    throw "aapt2.exe was not found under '$buildToolsRoot'."
}
Write-Host "aapt2: $aapt2" -ForegroundColor Green

Write-Step "Verifying application IDs"
Assert-PackageName -Aapt2Path $aapt2 -ApkPath $apkPaths.PersonalDebug -ExpectedPackage "com.princevekariya.projectledger.personal.debug"
Assert-PackageName -Aapt2Path $aapt2 -ApkPath $apkPaths.PlayDebug -ExpectedPackage "com.princevekariya.projectledger.debug"
Assert-PackageName -Aapt2Path $aapt2 -ApkPath $apkPaths.PersonalRelease -ExpectedPackage "com.princevekariya.projectledger.personal"
Assert-PackageName -Aapt2Path $aapt2 -ApkPath $apkPaths.PlayRelease -ExpectedPackage "com.princevekariya.projectledger"

Write-Step "Verifying SMS permission separation"
$personalPermissions = Get-ApkDump -Aapt2Path $aapt2 -Command "permissions" -ApkPath $apkPaths.PersonalDebug
$playPermissions = Get-ApkDump -Aapt2Path $aapt2 -Command "permissions" -ApkPath $apkPaths.PlayDebug

foreach ($permission in @("android.permission.READ_SMS", "android.permission.RECEIVE_SMS")) {
    if ($personalPermissions -notmatch [regex]::Escape($permission)) {
        throw "The personal APK is missing required permission '$permission'."
    }
    if ($playPermissions -match [regex]::Escape($permission)) {
        throw "The Play Store APK must not contain restricted permission '$permission'."
    }
    Write-Host "Separated correctly: $permission" -ForegroundColor Green
}

$shouldInstall = $InstallPersonalOnPhone -or $InstallBothOnPhone
if ($shouldInstall) {
    Write-Step "Checking connected Android phone"
    $adb = Join-Path $androidSdk "platform-tools\adb.exe"
    if (-not (Test-Path $adb)) {
        throw "ADB was not found at '$adb'."
    }

    $deviceLines = & $adb devices
    $authorized = $deviceLines | Where-Object { $_ -match "\sdevice$" }
    if (-not $authorized) {
        throw "No authorized Android phone was detected. Connect and unlock the phone, then accept USB debugging."
    }
    & $adb devices -l

    Invoke-CheckedCommand -Label "Installing Project Ledger Personal Dev" -Command {
        & $adb install -r $apkPaths.PersonalDebug
    }

    if ($InstallBothOnPhone) {
        Invoke-CheckedCommand -Label "Installing Project Ledger Play Dev" -Command {
            & $adb install -r $apkPaths.PlayDebug
        }
    }
}

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "PHASE 4 VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Primary development variant: personalDebug"
Write-Host "Personal debug package: com.princevekariya.projectledger.personal.debug"
Write-Host "Play debug package: com.princevekariya.projectledger.debug"
Write-Host "Restricted SMS permissions exist only in the personal flavor."
Write-Host "SMS reading functionality is intentionally not implemented yet."
Write-Host "Release APKs remain unsigned at this stage."
