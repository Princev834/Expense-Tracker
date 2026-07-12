[CmdletBinding()]
param(
    [string]$ProjectPath,
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

    throw "Project root could not be resolved. Run this script from the ProjectLedger repository."
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

$projectRoot = Resolve-ProjectRoot -RequestedPath $ProjectPath
Set-Location $projectRoot

Write-Step "Checking Phase 7 files"
$workflowFile = ".\.github\workflows\android-ci.yml"
$requiredFiles = @(
    $workflowFile,
    ".\docs\PHASE-7-CONTINUOUS-INTEGRATION.md",
    ".\scripts\Verify-Phase7.ps1"
)

foreach ($file in $requiredFiles) {
    if (-not (Test-Path $file)) {
        throw "Missing required file: $file"
    }
    Write-Host "Found: $file"
}

Assert-FileContains -Path $workflowFile -ExpectedText "actions/checkout@v6"
Assert-FileContains -Path $workflowFile -ExpectedText "actions/setup-java@v5"
Assert-FileContains -Path $workflowFile -ExpectedText "actions/upload-artifact@v6"
Assert-FileContains -Path $workflowFile -ExpectedText "qualityCheck"
Assert-FileContains -Path $workflowFile -ExpectedText ":app:assemblePersonalDebug"
Assert-FileContains -Path $workflowFile -ExpectedText ":app:assemblePlayDebug"
Assert-FileContains -Path $workflowFile -ExpectedText "permissions:"
Assert-FileContains -Path $workflowFile -ExpectedText "contents: read"

$workflowContent = [System.IO.File]::ReadAllText($workflowFile)
if ($workflowContent.Contains("`t")) {
    throw "The GitHub Actions workflow contains a tab character. YAML indentation must use spaces."
}

if ($workflowContent.Contains("pull_request_target:")) {
    throw "Unsafe trigger detected: pull_request_target must not be used in this workflow."
}

Write-Host "Workflow structure and security checks passed." -ForegroundColor Green

if (Test-Path ".\.git") {
    $gradlewIndexEntry = (& git ls-files --stage -- gradlew) -join ""
    if ([string]::IsNullOrWhiteSpace($gradlewIndexEntry)) {
        throw "gradlew is not tracked by Git. Add it before pushing Phase 7."
    }

    if (-not $gradlewIndexEntry.StartsWith("100755 ")) {
        throw "gradlew is not executable in Git. Run: git update-index --chmod=+x gradlew"
    }

    Write-Host "Git executable mode for gradlew: 100755" -ForegroundColor Green
} else {
    Write-Warning "No local Git repository was detected; executable-mode verification was skipped."
}

if ($ConfigurationOnly) {
    Write-Host "`nPHASE 7 CONFIGURATION CHECK PASSED" -ForegroundColor Green
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

Invoke-CheckedCommand -Label "Running the same quality, test, and build gates used by GitHub" -Command {
    & .\gradlew.bat `
        qualityCheck `
        :core:common:test `
        :core:model:test `
        :domain:transactions:test `
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

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "PHASE 7 LOCAL VERIFICATION PASSED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Workflow: .github/workflows/android-ci.yml"
Write-Host "GitHub permissions: contents read-only"
Write-Host "Variants built: personalDebug and playDebug"
Write-Host "Next checkpoint: commit, push, and confirm a green Android CI run on GitHub."
