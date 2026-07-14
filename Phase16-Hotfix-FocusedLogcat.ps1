[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$verifyFile = Join-Path $ProjectRoot "scripts\Verify-Phase16.ps1"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $verifyFile)) {
    throw "Verify-Phase16.ps1 was not found: $verifyFile"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase16-focused-logcat-$timestamp"
$backupFile = Join-Path $backupRoot "scripts\Verify-Phase16.ps1"

New-Item -ItemType Directory -Path (Split-Path $backupFile -Parent) -Force | Out-Null
Copy-Item -LiteralPath $verifyFile -Destination $backupFile -Force

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$verifyContent = [System.IO.File]::ReadAllText($verifyFile)
$marker = "PHASE16_FOCUSED_LOGCAT_V2"

if (-not $verifyContent.Contains($marker)) {
    $oldBlock = @'
    $processIdText = ((& $adb shell pidof $packageName 2>$null) | Out-String).Trim()
    $logLines = & $adb logcat -d -v threadtime -t 3000
    $logText = $logLines -join [Environment]::NewLine

    $hasFatalException =
        $logText.Contains("FATAL EXCEPTION") -and
        $logText.Contains($packageName)
    $hasKnownLifecycleCrash =
        $logText.Contains("CompositionLocal LocalLifecycleOwner not present")
    $hasContainerInitializationFailure =
        $logText.Contains("UninitializedPropertyAccessException") -and
        $logText.Contains("appContainer")
    $hasContainerReadyLog =
        $logText.Contains("application_container_ready")

    if (
        [string]::IsNullOrWhiteSpace($processIdText) -or
        $hasFatalException -or
        $hasKnownLifecycleCrash -or
        $hasContainerInitializationFailure -or
        -not $hasContainerReadyLog
    ) {
        $reportLines = @(
            "Project Ledger Phase 16 startup smoke-test failure",
            "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')",
            "Package: $packageName",
            "Process ID after launch: $processIdText",
            "Container-ready log found: $hasContainerReadyLog",
            "",
            "=== Launch output ===",
            ($launchOutput -join [Environment]::NewLine),
            "",
            "=== Logcat ===",
            $logText
        )
        $reportLines | Set-Content -Path $failureReport -Encoding UTF8
        throw "The app did not pass the Phase 16 startup smoke test. See $failureReport."
    }
'@

    $newBlock = @'
    # PHASE16_FOCUSED_LOGCAT_V2
    $processIdText = ((& $adb shell pidof $packageName 2>$null) | Out-String).Trim()

    # Read only Project Ledger and AndroidRuntime entries. The previous verifier
    # requested the final 3000 lines from every MIUI process, which could discard
    # the early application_container_ready entry on very noisy devices.
    $focusedLogLines = & $adb logcat `
        -d `
        -v threadtime `
        -s `
        "ProjectLedger:V" `
        "AndroidRuntime:E" `
        "*:S"
    $focusedLogText = $focusedLogLines -join [Environment]::NewLine

    $hasFatalException =
        $focusedLogText.Contains("FATAL EXCEPTION") -and
        $focusedLogText.Contains($packageName)
    $hasKnownLifecycleCrash =
        $focusedLogText.Contains("CompositionLocal LocalLifecycleOwner not present")
    $hasContainerInitializationFailure =
        $focusedLogText.Contains("UninitializedPropertyAccessException") -and
        $focusedLogText.Contains("appContainer")
    $hasContainerReadyLog =
        $focusedLogText.Contains("application_container_ready")

    if (
        [string]::IsNullOrWhiteSpace($processIdText) -or
        $hasFatalException -or
        $hasKnownLifecycleCrash -or
        $hasContainerInitializationFailure -or
        -not $hasContainerReadyLog
    ) {
        $diagnosticLogLines = & $adb logcat -d -v threadtime -t 5000
        $diagnosticLogText = $diagnosticLogLines -join [Environment]::NewLine

        $reportLines = @(
            "Project Ledger Phase 16 startup smoke-test failure",
            "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')",
            "Package: $packageName",
            "Process ID after launch: $processIdText",
            "Container-ready log found: $hasContainerReadyLog",
            "",
            "=== Launch output ===",
            ($launchOutput -join [Environment]::NewLine),
            "",
            "=== Focused Project Ledger Logcat ===",
            $focusedLogText,
            "",
            "=== Diagnostic Logcat ===",
            $diagnosticLogText
        )
        $reportLines | Set-Content -Path $failureReport -Encoding UTF8
        throw "The app did not pass the Phase 16 startup smoke test. See $failureReport."
    }
'@

    if (-not $verifyContent.Contains($oldBlock)) {
        throw "The expected Phase 16 Logcat verification block was not found."
    }

    $verifyContent = $verifyContent.Replace($oldBlock, $newBlock)
    [System.IO.File]::WriteAllText($verifyFile, $verifyContent, $utf8NoBom)
}

$updatedContent = [System.IO.File]::ReadAllText($verifyFile)

if (-not $updatedContent.Contains($marker)) {
    throw "Hotfix validation failed: focused Logcat verification was not installed."
}

if (-not $updatedContent.Contains('"ProjectLedger:V"')) {
    throw "Hotfix validation failed: ProjectLedger tag filtering is missing."
}

if (-not $updatedContent.Contains('"AndroidRuntime:E"')) {
    throw "Hotfix validation failed: AndroidRuntime crash filtering is missing."
}

Write-Host ""
Write-Host "PHASE 16 FOCUSED LOGCAT HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Updated: scripts\Verify-Phase16.ps1"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host "The verifier now reads the ProjectLedger tag directly instead of" -ForegroundColor Cyan
Write-Host "depending on the last 3000 noisy system-wide Logcat lines."
Write-Host ""
Write-Host "Now rerun:" -ForegroundColor Yellow
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase16.ps1 -InstallOnPhone"
