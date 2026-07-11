[CmdletBinding()]
param(
    [switch]$InstallOnPhone
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$androidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"
$androidSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$adb = Join-Path $androidSdk "platform-tools\adb.exe"

if (-not (Test-Path (Join-Path $androidStudioJbr "bin\java.exe"))) {
    throw "Android Studio's bundled JDK was not found at: $androidStudioJbr"
}

if (-not (Test-Path $androidSdk)) {
    throw "Android SDK was not found at: $androidSdk"
}

if (-not (Test-Path $adb)) {
    throw "ADB was not found at: $adb"
}

$env:JAVA_HOME = $androidStudioJbr
$env:Path = "$(Join-Path $androidStudioJbr 'bin');$env:Path"

# Gradle's local.properties format requires escaped Windows separators.
$escapedSdk = $androidSdk.Replace("\", "\\").Replace(":", "\:")
"sdk.dir=$escapedSdk" | Set-Content -Path (Join-Path $projectRoot "local.properties") -Encoding ASCII

Write-Host ""
Write-Host "JAVA_HOME: $env:JAVA_HOME"
Write-Host "Android SDK: $androidSdk"
Write-Host ""

Write-Host "Connected Android devices:"
& $adb devices -l
if ($LASTEXITCODE -ne 0) {
    throw "ADB device check failed."
}

Write-Host ""
Write-Host "Running clean, unit tests, and debug APK build..."
& (Join-Path $projectRoot "gradlew.bat") --no-daemon clean test assembleDebug
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with exit code $LASTEXITCODE."
}

$apk = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) {
    throw "Gradle reported success, but the APK was not found at: $apk"
}

Write-Host ""
Write-Host "BUILD SUCCESSFUL"
Write-Host "Debug APK: $apk"

if ($InstallOnPhone) {
    Write-Host ""
    Write-Host "Installing the debug APK on the connected phone..."
    & $adb install -r $apk
    if ($LASTEXITCODE -ne 0) {
        throw "APK installation failed with exit code $LASTEXITCODE."
    }
    Write-Host "APK installation completed."
}
