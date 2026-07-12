[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$catalogFile = Join-Path $ProjectRoot "gradle\libs.versions.toml"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $catalogFile)) {
    throw "Version catalog was not found: $catalogFile"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase8-compose-foundation-$timestamp"
$backupFile = Join-Path $backupRoot "gradle\libs.versions.toml"

New-Item -ItemType Directory -Path (Split-Path $backupFile -Parent) -Force | Out-Null
Copy-Item -LiteralPath $catalogFile -Destination $backupFile -Force

$content = [System.IO.File]::ReadAllText($catalogFile)

$foundationDeclaration =
    'androidx-compose-foundation = { module = "androidx.compose.foundation:foundation" }'

if (-not $content.Contains($foundationDeclaration)) {
    $anchor =
        'androidx-compose-ui = { module = "androidx.compose.ui:ui" }'

    if (-not $content.Contains($anchor)) {
        throw "Could not find the Compose UI dependency anchor in libs.versions.toml."
    }

    $content = $content.Replace(
        $anchor,
        $anchor + "`r`n" + $foundationDeclaration
    )
}

$bundlePattern = '(?s)(compose\s*=\s*\[\s*)(.*?)(\s*\])'
$bundleMatch = [System.Text.RegularExpressions.Regex]::Match($content, $bundlePattern)

if (-not $bundleMatch.Success) {
    throw "Could not find the Compose dependency bundle in libs.versions.toml."
}

$bundleBody = $bundleMatch.Groups[2].Value

if ($bundleBody -notmatch '"androidx-compose-foundation"') {
    $newBundleBody =
        '    "androidx-compose-foundation",' + "`r`n" + $bundleBody.TrimStart()

    $replacement =
        $bundleMatch.Groups[1].Value +
        $newBundleBody +
        $bundleMatch.Groups[3].Value

    $content =
        $content.Substring(0, $bundleMatch.Index) +
        $replacement +
        $content.Substring($bundleMatch.Index + $bundleMatch.Length)
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($catalogFile, $content, $utf8NoBom)

$written = [System.IO.File]::ReadAllText($catalogFile)

$required = @(
    $foundationDeclaration,
    '"androidx-compose-foundation"'
)

foreach ($item in $required) {
    if (-not $written.Contains($item)) {
        throw "Hotfix validation failed. Missing text: $item"
    }
}

Write-Host ""
Write-Host "PHASE 8 COMPOSE FOUNDATION HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Added an explicit Compose Foundation dependency."
Write-Host "Updated: gradle\libs.versions.toml"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host "Now rerun:" -ForegroundColor Cyan
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase8.ps1 -InstallOnPhone"
