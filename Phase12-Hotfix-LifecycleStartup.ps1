[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$catalogFile = Join-Path $ProjectRoot "gradle\libs.versions.toml"
$verifyFile = Join-Path $ProjectRoot "scripts\Verify-Phase12.ps1"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

foreach ($requiredFile in @($catalogFile, $verifyFile)) {
    if (-not (Test-Path $requiredFile)) {
        throw "Required file was not found: $requiredFile"
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase12-lifecycle-startup-$timestamp"

function Backup-ProjectFile {
    param([Parameter(Mandatory = $true)][string]$Path)

    $relativePath = $Path.Substring($ProjectRoot.Length).TrimStart('\')
    $backupPath = Join-Path $backupRoot $relativePath
    New-Item -ItemType Directory -Path (Split-Path $backupPath -Parent) -Force | Out-Null
    Copy-Item -LiteralPath $Path -Destination $backupPath -Force
}

Backup-ProjectFile -Path $catalogFile
Backup-ProjectFile -Path $verifyFile

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

# Lifecycle 2.8.0 has a known compatibility failure with Compose 1.6.x.
$catalogContent = [System.IO.File]::ReadAllText($catalogFile)
$oldVersionPattern = '(?m)^lifecycle-runtime-ktx\s*=\s*"2\.8\.0"\s*$'
$newVersionLine = 'lifecycle-runtime-ktx = "2.8.2"'

if ([System.Text.RegularExpressions.Regex]::IsMatch($catalogContent, $oldVersionPattern)) {
    $catalogContent = [System.Text.RegularExpressions.Regex]::Replace(
        $catalogContent,
        $oldVersionPattern,
        $newVersionLine,
        1
    )
} elseif (-not $catalogContent.Contains($newVersionLine)) {
    throw "Expected Lifecycle 2.8.0 or 2.8.2 was not found in gradle\libs.versions.toml."
}

[System.IO.File]::WriteAllText($catalogFile, $catalogContent, $utf8NoBom)

# Add a real launch-and-stay-running smoke test to Phase 12 verification.
$verifyContent = [System.IO.File]::ReadAllText($verifyFile)
$smokeMarker = 'PHASE12_STARTUP_SMOKE_TEST'
$finalOutputAnchor = @'
Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "PHASE 12 VERIFICATION PASSED" -ForegroundColor Green
'@

if (-not $verifyContent.Contains($smokeMarker)) {
    if (-not $verifyContent.Contains($finalOutputAnchor)) {
        throw "Could not find the Phase 12 final-output anchor in Verify-Phase12.ps1."
    }

    $smokeBlock = @'
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

'@

    $verifyContent = $verifyContent.Replace(
        $finalOutputAnchor,
        $smokeBlock + $finalOutputAnchor
    )

    [System.IO.File]::WriteAllText($verifyFile, $verifyContent, $utf8NoBom)
}

$updatedCatalog = [System.IO.File]::ReadAllText($catalogFile)
$updatedVerify = [System.IO.File]::ReadAllText($verifyFile)

if (-not $updatedCatalog.Contains('lifecycle-runtime-ktx = "2.8.2"')) {
    throw "Hotfix validation failed: Lifecycle 2.8.2 was not configured."
}

if (-not $updatedVerify.Contains($smokeMarker)) {
    throw "Hotfix validation failed: startup smoke test was not added."
}

if (-not $updatedVerify.Contains("CompositionLocal LocalLifecycleOwner not present")) {
    throw "Hotfix validation failed: the known lifecycle crash is not checked."
}

Write-Host ""
Write-Host "PHASE 12 LIFECYCLE STARTUP HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Lifecycle: 2.8.0 -> 2.8.2"
Write-Host "Verification: added launch-and-stay-running smoke test"
Write-Host "Backup: $backupRoot"
Write-Host ""
Write-Host "Now rerun:" -ForegroundColor Cyan
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase12.ps1 -InstallOnPhone"
