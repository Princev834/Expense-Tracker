[CmdletBinding()]
param(
    [string]$ProjectPath,
    [switch]$ApplyFormatting
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

$projectRoot = Resolve-ProjectRoot -RequestedPath $ProjectPath
Set-Location $projectRoot

$studioJdk = "C:\Program Files\Android\Android Studio\jbr"
if (-not (Test-Path (Join-Path $studioJdk "bin\java.exe"))) {
    throw "Android Studio bundled JDK was not found at '$studioJdk'."
}
$env:JAVA_HOME = $studioJdk
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

if ($ApplyFormatting) {
    Write-Step "Applying automatic formatting"
    & .\gradlew.bat spotlessApply --no-daemon --max-workers=1 --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "spotlessApply failed with exit code $LASTEXITCODE."
    }
}

Write-Step "Running the Project Ledger quality gate"
& .\gradlew.bat qualityCheck --no-daemon --max-workers=1 --console=plain
if ($LASTEXITCODE -ne 0) {
    throw "Quality checks failed with exit code $LASTEXITCODE."
}

Write-Host "`nQUALITY CHECKS PASSED" -ForegroundColor Green
