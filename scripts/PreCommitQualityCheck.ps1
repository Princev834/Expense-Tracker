Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$studioJdk = "C:\Program Files\Android\Android Studio\jbr"
if (Test-Path (Join-Path $studioJdk "bin\java.exe")) {
    $env:JAVA_HOME = $studioJdk
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
}

Write-Host "Running pre-commit formatting and static-analysis checks..." -ForegroundColor Cyan
& .\gradlew.bat qualityCheck --no-daemon --max-workers=1 --console=plain
exit $LASTEXITCODE
