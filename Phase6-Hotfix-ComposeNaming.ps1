param(
    [string]$ProjectRoot = (Get-Location).Path
)

$ErrorActionPreference = "Stop"

$editorConfig = Join-Path $ProjectRoot ".editorconfig"

if (-not (Test-Path $editorConfig)) {
    throw "Could not find .editorconfig in: $ProjectRoot`nRun this script from the ProjectLedger root folder."
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backup = "$editorConfig.phase6-compose-$timestamp.bak"
Copy-Item $editorConfig $backup -Force

$content = [System.IO.File]::ReadAllText($editorConfig)
$propertyPattern = '(?m)^\s*ktlint_function_naming_ignore_when_annotated_with\s*=.*$'
$propertyLine = 'ktlint_function_naming_ignore_when_annotated_with = Composable'

if ([System.Text.RegularExpressions.Regex]::IsMatch($content, $propertyPattern)) {
    $content = [System.Text.RegularExpressions.Regex]::Replace(
        $content,
        $propertyPattern,
        $propertyLine
    )
} else {
    $anchorPattern = '(?m)^ktlint_standard_filename\s*=\s*disabled\s*$'

    if ([System.Text.RegularExpressions.Regex]::IsMatch($content, $anchorPattern)) {
        $content = [System.Text.RegularExpressions.Regex]::Replace(
            $content,
            $anchorPattern,
            "ktlint_standard_filename = disabled`n$propertyLine",
            1
        )
    } else {
        $content = $content.TrimEnd() +
            "`n`n[*.{kt,kts}]`n$propertyLine`n"
    }
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($editorConfig, $content, $utf8NoBom)

Write-Host ""
Write-Host "Phase 6 Compose naming hotfix applied." -ForegroundColor Green
Write-Host "Updated: $editorConfig"
Write-Host "Backup:  $backup"
Write-Host ""
Write-Host "Added:"
Write-Host "  $propertyLine" -ForegroundColor Cyan
Write-Host ""
Write-Host "Now rerun:"
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase6.ps1 -InstallOnPhone" -ForegroundColor Yellow
