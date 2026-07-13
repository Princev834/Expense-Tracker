[CmdletBinding()]
param(
    [string]$PackageName = "com.princevekariya.projectledger.personal.debug",
    [string]$ActivityName = "com.princevekariya.projectledger.MainActivity",
    [string]$OutputFile = ".\Phase12-CrashLog.txt"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $adb)) {
    throw "ADB was not found at: $adb"
}

$devices = & $adb devices
if (-not ($devices -match "\sdevice$")) {
    throw "No authorized Android device is connected."
}

Write-Host "Clearing old Logcat entries..."
& $adb logcat -c | Out-Null

Write-Host "Stopping the app..."
& $adb shell am force-stop $PackageName | Out-Null

Write-Host "Launching the app..."
$launchOutput = & $adb shell am start -W -n "$PackageName/$ActivityName" 2>&1

Start-Sleep -Seconds 4

$processId = (& $adb shell pidof $PackageName 2>$null).Trim()

$logcat = & $adb logcat -d -v threadtime -t 2500

$important = $logcat | Select-String -Pattern (
    "AndroidRuntime|" +
    "FATAL EXCEPTION|" +
    "Process: $([regex]::Escape($PackageName))|" +
    "Caused by:|" +
    "ProjectLedger|" +
    "$([regex]::Escape($PackageName))"
) -Context 0,25

$report = New-Object System.Collections.Generic.List[string]
$report.Add("Project Ledger startup crash report")
$report.Add("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')")
$report.Add("Package: $PackageName")
$report.Add("Activity: $ActivityName")
$report.Add("")
$report.Add("=== Launch output ===")
$report.Add(($launchOutput -join [Environment]::NewLine))
$report.Add("")
$report.Add("=== Process status after 4 seconds ===")

if ([string]::IsNullOrWhiteSpace($processId)) {
    $report.Add("Process is NOT running. The app likely crashed during startup.")
} else {
    $report.Add("Process is running with PID: $processId")
}

$report.Add("")
$report.Add("=== Relevant Logcat ===")

if ($important) {
    $report.Add(($important | Out-String))
} else {
    $report.Add("No filtered crash lines were found.")
    $report.Add("")
    $report.Add("=== Last 400 raw Logcat lines ===")
    $report.Add(($logcat | Select-Object -Last 400 | Out-String))
}

$report | Set-Content -Path $OutputFile -Encoding UTF8

Write-Host ""
Write-Host "Crash report created:" -ForegroundColor Green
Write-Host (Resolve-Path $OutputFile)
Write-Host ""
Write-Host "Upload Phase12-CrashLog.txt in the chat." -ForegroundColor Yellow
