[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$verifyFile = Join-Path $ProjectRoot "scripts\Verify-Phase23.ps1"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $verifyFile)) {
    throw "Verify-Phase23.ps1 was not found: $verifyFile"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot `
    ".phase-backups\phase23-keyguard-verifier-$timestamp"
$backupFile = Join-Path $backupRoot "scripts\Verify-Phase23.ps1"

New-Item `
    -ItemType Directory `
    -Path (Split-Path $backupFile -Parent) `
    -Force |
    Out-Null

Copy-Item `
    -LiteralPath $verifyFile `
    -Destination $backupFile `
    -Force

$content = [System.IO.File]::ReadAllText($verifyFile)

$oldLaunchPreparation = @'
    & $adb logcat -c
    & $adb shell am force-stop $packageName

    $launchOutput = & $adb shell am start `
'@

$newLaunchPreparation = @'
    & $adb logcat -c

    Write-Host "Waking the phone and requesting keyguard dismissal"
    & $adb shell input keyevent KEYCODE_WAKEUP | Out-Null
    Start-Sleep -Seconds 1
    & $adb shell wm dismiss-keyguard | Out-Null
    & $adb shell input keyevent 82 | Out-Null
    Start-Sleep -Seconds 1

    & $adb shell am force-stop $packageName

    $launchOutput = & $adb shell am start `
'@

$oldUiBlock = @'
    & $adb shell uiautomator dump `
        /sdcard/project-ledger-phase23.xml |
        Out-Null
    $uiXml = (
        & $adb shell cat /sdcard/project-ledger-phase23.xml
    ) -join ""

    $hasFatalException =
'@

$newUiBlock = @'
    $uiXml = ""
    $appUiVisible = $false
    $systemUiVisible = $false
    $appPackageMarker = 'package="' + $packageName + '"'
    $systemUiPackageMarker = 'package="com.android.systemui"'

    for ($uiAttempt = 1; $uiAttempt -le 15; $uiAttempt += 1) {
        & $adb shell uiautomator dump `
            /sdcard/project-ledger-phase23.xml |
            Out-Null
        $uiXml = (
            & $adb shell cat /sdcard/project-ledger-phase23.xml
        ) -join ""

        $appUiVisible = $uiXml.Contains($appPackageMarker)
        $systemUiVisible = $uiXml.Contains($systemUiPackageMarker)

        if ($appUiVisible) {
            break
        }

        & $adb shell input keyevent KEYCODE_WAKEUP | Out-Null
        & $adb shell wm dismiss-keyguard | Out-Null
        & $adb shell input keyevent 82 | Out-Null
        Start-Sleep -Seconds 1
    }

    $hasFatalException =
'@

$oldFailureCondition = @'
        -not $hasHistoryFactoryLog -or
        -not $hasBootstrapReadyLog -or
        -not $hasTitle -or
'@

$newFailureCondition = @'
        -not $hasHistoryFactoryLog -or
        -not $hasBootstrapReadyLog -or
        -not $appUiVisible -or
        -not $hasTitle -or
'@

$oldReportLines = @'
            "Bootstrap-ready log found: $hasBootstrapReadyLog",
            "Transactions title found: $hasTitle",
'@

$newReportLines = @'
            "Bootstrap-ready log found: $hasBootstrapReadyLog",
            "Project Ledger UI package found: $appUiVisible",
            "Android System UI package found: $systemUiVisible",
            "Transactions title found: $hasTitle",
'@

$oldThrow = @'
        throw "The Phase 23 history screen did not pass verification. See $failureReport."
'@

$newThrow = @'
        if (-not $appUiVisible -and $systemUiVisible) {
            throw "The phone remained locked during UI verification. Unlock it, keep the screen on, and rerun Verify-Phase23.ps1. See $failureReport."
        }

        throw "The Phase 23 history screen did not pass verification. See $failureReport."
'@

$replacements = @(
    @($oldLaunchPreparation, $newLaunchPreparation),
    @($oldUiBlock, $newUiBlock),
    @($oldFailureCondition, $newFailureCondition),
    @($oldReportLines, $newReportLines),
    @($oldThrow, $newThrow)
)

$changedCount = 0

foreach ($replacement in $replacements) {
    $oldText = $replacement[0]
    $newText = $replacement[1]

    if ($content.Contains($newText)) {
        continue
    }

    if (-not $content.Contains($oldText)) {
        throw "An expected Phase 23 verifier block was not found. No file was changed."
    }

    $content = $content.Replace($oldText, $newText)
    $changedCount += 1
}

if ($changedCount -gt 0) {
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText(
        $verifyFile,
        $content,
        $utf8NoBom
    )
} else {
    Write-Host `
        "The Phase 23 keyguard verifier hotfix is already applied." `
        -ForegroundColor Yellow
}

$updatedContent = [System.IO.File]::ReadAllText($verifyFile)

$requiredContracts = @(
    "Waking the phone and requesting keyguard dismissal",
    '$appUiVisible = $false',
    '$systemUiVisible = $false',
    '$appPackageMarker = ''package="'' + $packageName + ''"''',
    "-not `$appUiVisible -or",
    "The phone remained locked during UI verification"
)

foreach ($contract in $requiredContracts) {
    if (-not $updatedContent.Contains($contract)) {
        throw "Hotfix validation failed. Missing verifier contract: $contract"
    }
}

Write-Host ""
Write-Host `
    "PHASE 23 KEYGUARD VERIFIER HOTFIX APPLIED" `
    -ForegroundColor Green
Write-Host "Updated: scripts\Verify-Phase23.ps1"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host `
    "Unlock the phone and keep its screen on before verification." `
    -ForegroundColor Cyan
Write-Host ""
Write-Host "Now rerun the complete verifier:" -ForegroundColor Yellow
Write-Host `
    "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase23.ps1 -InstallOnPhone"
