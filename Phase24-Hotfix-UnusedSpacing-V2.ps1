[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$screenFile = Join-Path $ProjectRoot `
    "feature\reports\src\main\java\com\princevekariya\projectledger\feature\reports\MonthlyReportScreen.kt"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $screenFile)) {
    throw "MonthlyReportScreen.kt was not found: $screenFile"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot `
    ".phase-backups\phase24-unused-spacing-v2-$timestamp"
$backupFile = Join-Path $backupRoot `
    "feature\reports\src\main\java\com\princevekariya\projectledger\feature\reports\MonthlyReportScreen.kt"

New-Item `
    -ItemType Directory `
    -Path (Split-Path $backupFile -Parent) `
    -Force |
    Out-Null

Copy-Item `
    -LiteralPath $screenFile `
    -Destination $backupFile `
    -Force

$lines = [System.Collections.Generic.List[string]]::new()
[System.IO.File]::ReadAllLines($screenFile) |
    ForEach-Object {
        [void]$lines.Add($_)
    }

$functionStart = -1
$functionEnd = -1
$braceDepth = 0
$functionBodyStarted = $false

for ($index = 0; $index -lt $lines.Count; $index += 1) {
    if (
        $functionStart -lt 0 -and
        $lines[$index] -match '^\s*private\s+fun\s+CategoryExpenseCard\s*\('
    ) {
        $functionStart = $index
    }

    if ($functionStart -ge 0 -and $functionEnd -lt 0) {
        $openCount = (
            [regex]::Matches($lines[$index], '\{')
        ).Count
        $closeCount = (
            [regex]::Matches($lines[$index], '\}')
        ).Count

        if ($openCount -gt 0) {
            $functionBodyStarted = $true
        }

        $braceDepth += $openCount
        $braceDepth -= $closeCount

        if (
            $functionBodyStarted -and
            $braceDepth -eq 0
        ) {
            $functionEnd = $index
            break
        }
    }
}

if ($functionStart -lt 0) {
    throw "CategoryExpenseCard was not found. No file was changed."
}

if ($functionEnd -lt 0) {
    throw "CategoryExpenseCard boundaries could not be determined. No file was changed."
}

$matchingIndexes = @()

for (
    $index = $functionStart;
    $index -le $functionEnd;
    $index += 1
) {
    if (
        $lines[$index] -match
        '^\s*val\s+spacing\s*=\s*MaterialTheme\.ledgerSpacing\s*$'
    ) {
        $matchingIndexes += $index
    }
}

if ($matchingIndexes.Count -eq 0) {
    $wholeContent = [System.IO.File]::ReadAllText($screenFile)

    if (
        $wholeContent -notmatch
        'val\s+spacing\s*=\s*MaterialTheme\.ledgerSpacing'
    ) {
        Write-Host `
            "The unused spacing declaration is already removed." `
            -ForegroundColor Yellow
    } else {
        throw "A spacing declaration exists, but not inside CategoryExpenseCard. No file was changed."
    }
} elseif ($matchingIndexes.Count -eq 1) {
    $lines.RemoveAt($matchingIndexes[0])

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines(
        $screenFile,
        $lines,
        $utf8NoBom
    )
} else {
    throw "More than one unused spacing declaration was found inside CategoryExpenseCard. No file was changed."
}

$updatedContent = [System.IO.File]::ReadAllText($screenFile)

$categoryFunctionPattern =
    '(?s)private\s+fun\s+CategoryExpenseCard\s*\(.*?(?=\r?\nprivate\s+fun|\z)'
$categoryFunctionMatch = [regex]::Match(
    $updatedContent,
    $categoryFunctionPattern
)

if (-not $categoryFunctionMatch.Success) {
    throw "Hotfix validation failed: CategoryExpenseCard is missing."
}

if (
    $categoryFunctionMatch.Value -match
    'val\s+spacing\s*=\s*MaterialTheme\.ledgerSpacing'
) {
    throw "Hotfix validation failed: the unused spacing declaration remains."
}

if (
    $categoryFunctionMatch.Value -notmatch
    'val\s+expenseColor\s*=\s*MaterialTheme\.ledgerColors\.expense'
) {
    throw "Hotfix validation failed: the category expense color logic is missing."
}

if (
    $updatedContent -notmatch
    'Phase 24 - live monthly reports'
) {
    throw "Hotfix validation failed: the Phase 24 report marker is missing."
}

Write-Host ""
Write-Host `
    "PHASE 24 UNUSED SPACING HOTFIX V2 APPLIED" `
    -ForegroundColor Green
Write-Host "Updated: feature\reports\...\MonthlyReportScreen.kt"
Write-Host "Backup:  $backupRoot"
Write-Host ""
Write-Host "Run focused Reports compilation:" -ForegroundColor Yellow
Write-Host `
    "  .\gradlew.bat :feature:reports:compileDebugKotlin --no-daemon --max-workers=1 --console=plain"
Write-Host ""
Write-Host "Then run complete Phase 24 verification:" -ForegroundColor Yellow
Write-Host `
    "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase24.ps1 -InstallOnPhone"
