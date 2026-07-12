param(
    [string]$ProjectRoot = (Get-Location).Path
)

$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
if (-not (Test-Path $settingsFile)) {
    throw "Could not find settings.gradle.kts in: $ProjectRoot`nRun this script from the ProjectLedger root folder."
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase6-detekt-fixes-$timestamp"
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

function Backup-ProjectFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath
    )

    if (-not (Test-Path $FilePath)) {
        return
    }

    $relativePath = $FilePath.Substring($ProjectRoot.Length).TrimStart('\')
    $backupPath = Join-Path $backupRoot $relativePath
    $backupDirectory = Split-Path $backupPath -Parent

    New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
    Copy-Item $FilePath $backupPath -Force
}

function Rename-KotlinFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SourceRelativePath,

        [Parameter(Mandatory = $true)]
        [string]$DestinationRelativePath
    )

    $sourcePath = Join-Path $ProjectRoot $SourceRelativePath
    $destinationPath = Join-Path $ProjectRoot $DestinationRelativePath

    if (Test-Path $sourcePath) {
        if (Test-Path $destinationPath) {
            throw "Cannot rename '$SourceRelativePath' because '$DestinationRelativePath' already exists."
        }

        Backup-ProjectFile -FilePath $sourcePath
        Move-Item -Path $sourcePath -Destination $destinationPath
        Write-Host "Renamed: $SourceRelativePath" -ForegroundColor Cyan
        Write-Host "      to: $DestinationRelativePath" -ForegroundColor Cyan
        return
    }

    if (Test-Path $destinationPath) {
        Write-Host "Already renamed: $DestinationRelativePath" -ForegroundColor DarkGray
        return
    }

    throw "Could not find either '$SourceRelativePath' or '$DestinationRelativePath'."
}

Rename-KotlinFile `
    -SourceRelativePath "app\src\main\java\com\princevekariya\projectledger\config\AppDistribution.kt" `
    -DestinationRelativePath "app\src\main\java\com\princevekariya\projectledger\config\CurrentAppVariant.kt"

Rename-KotlinFile `
    -SourceRelativePath "feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\FoundationDashboard.kt" `
    -DestinationRelativePath "feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\FoundationDashboardUiState.kt"

$dashboardFile = Join-Path $ProjectRoot "feature\dashboard\src\main\java\com\princevekariya\projectledger\feature\dashboard\FoundationDashboardUiState.kt"

if (-not (Test-Path $dashboardFile)) {
    throw "Dashboard file was not found after rename: $dashboardFile"
}

Backup-ProjectFile -FilePath $dashboardFile

$content = [System.IO.File]::ReadAllText($dashboardFile)

$alreadySuppressedPattern = '(?s)@Suppress\("UnusedPrivateMember"\)\s*private\s+fun\s+FoundationDashboardPreview'
$previewFunctionPattern = '(?m)^(\s*)private\s+fun\s+FoundationDashboardPreview'

if ([System.Text.RegularExpressions.Regex]::IsMatch($content, $alreadySuppressedPattern)) {
    Write-Host "Preview suppression already present." -ForegroundColor DarkGray
} elseif ([System.Text.RegularExpressions.Regex]::IsMatch($content, $previewFunctionPattern)) {
    $content = [System.Text.RegularExpressions.Regex]::Replace(
        $content,
        $previewFunctionPattern,
        '$1@Suppress("UnusedPrivateMember")' + "`r`n" + '$1private fun FoundationDashboardPreview',
        1
    )

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($dashboardFile, $content, $utf8NoBom)

    Write-Host "Added @Suppress(""UnusedPrivateMember"") to FoundationDashboardPreview." -ForegroundColor Cyan
} else {
    throw "Could not find the private FoundationDashboardPreview function."
}

Write-Host ""
Write-Host "Phase 6 consolidated Detekt hotfix applied successfully." -ForegroundColor Green
Write-Host "Backup folder: $backupRoot"
Write-Host ""
Write-Host "Now rerun:"
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase6.ps1 -InstallOnPhone" -ForegroundColor Yellow
