[CmdletBinding()]
param(
    [string]$ProjectRoot = (Get-Location).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$settingsFile = Join-Path $ProjectRoot "settings.gradle.kts"
$workflowFile = Join-Path $ProjectRoot ".github\workflows\android-ci.yml"
$verifyFile = Join-Path $ProjectRoot "scripts\Verify-Phase7.ps1"

if (-not (Test-Path $settingsFile)) {
    throw "settings.gradle.kts was not found. Run this script from the ProjectLedger root folder."
}

if (-not (Test-Path $workflowFile)) {
    throw "Phase 7 workflow was not found: $workflowFile"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $ProjectRoot ".phase-backups\phase7-android-sdk-hotfix-$timestamp"
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

function Backup-File {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path $Path)) {
        return
    }

    $relative = $Path.Substring($ProjectRoot.Length).TrimStart('\')
    $destination = Join-Path $backupRoot $relative
    New-Item -ItemType Directory -Path (Split-Path $destination -Parent) -Force | Out-Null
    Copy-Item -LiteralPath $Path -Destination $destination -Force
}

Backup-File -Path $workflowFile
Backup-File -Path $verifyFile

$correctedWorkflow = @'
name: Android CI

on:
  push:
    branches:
      - main
      - master
      - develop
  pull_request:
    branches:
      - main
      - master
      - develop
  workflow_dispatch:

permissions:
  contents: read

concurrency:
  group: android-ci-${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  verify:
    name: Quality, tests, and debug APKs
    runs-on: ubuntu-24.04
    timeout-minutes: 40

    steps:
      - name: Check out repository
        uses: actions/checkout@v6

      - name: Set up JDK 17
        uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "17"
          cache: gradle

      - name: Set up Android SDK
        uses: android-actions/setup-android@v4
        with:
          packages: "platform-tools platforms;android-34 build-tools;34.0.0"

      - name: Make Gradle wrapper executable
        run: chmod +x gradlew

      - name: Run quality checks, tests, and debug builds
        run: |
          ./gradlew \
            qualityCheck \
            :core:common:test \
            :core:model:test \
            :domain:transactions:test \
            :app:testPersonalDebugUnitTest \
            :app:testPlayDebugUnitTest \
            :app:assemblePersonalDebug \
            :app:assemblePlayDebug \
            --no-daemon \
            --max-workers=2 \
            --console=plain \
            --stacktrace

      - name: Upload personal debug APK
        if: success()
        uses: actions/upload-artifact@v6
        with:
          name: project-ledger-personal-debug
          path: app/build/outputs/apk/personal/debug/app-personal-debug.apk
          if-no-files-found: error
          retention-days: 14

      - name: Upload Play-safe debug APK
        if: success()
        uses: actions/upload-artifact@v6
        with:
          name: project-ledger-play-debug
          path: app/build/outputs/apk/play/debug/app-play-debug.apk
          if-no-files-found: error
          retention-days: 14

      - name: Upload diagnostic reports
        if: failure()
        uses: actions/upload-artifact@v6
        with:
          name: project-ledger-ci-reports
          path: |
            **/build/reports/**
            **/build/test-results/**
          if-no-files-found: ignore
          retention-days: 7
'@

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($workflowFile, $correctedWorkflow + "`n", $utf8NoBom)

if (Test-Path $verifyFile) {
    $verifyContent = [System.IO.File]::ReadAllText($verifyFile)

    if (-not $verifyContent.Contains('Assert-FileContains -Path $workflowFile -ExpectedText "android-actions/setup-android@v4"')) {
        $anchor = 'Assert-FileContains -Path $workflowFile -ExpectedText "actions/setup-java@v5"'
        if ($verifyContent.Contains($anchor)) {
            $replacement = $anchor + "`r`n" +
                'Assert-FileContains -Path $workflowFile -ExpectedText "android-actions/setup-android@v4"' + "`r`n" +
                'Assert-FileContains -Path $workflowFile -ExpectedText "platforms;android-34"' + "`r`n" +
                'Assert-FileContains -Path $workflowFile -ExpectedText "build-tools;34.0.0"'
            $verifyContent = $verifyContent.Replace($anchor, $replacement)
        }
    }

    [System.IO.File]::WriteAllText($verifyFile, $verifyContent, $utf8NoBom)
}

$written = [System.IO.File]::ReadAllText($workflowFile)
$required = @(
    "android-actions/setup-android@v4",
    "platforms;android-34",
    "build-tools;34.0.0",
    ":app:assemblePersonalDebug",
    ":app:assemblePlayDebug"
)

foreach ($item in $required) {
    if (-not $written.Contains($item)) {
        throw "Hotfix validation failed. Missing workflow text: $item"
    }
}

if ($written.Contains('run: sdkmanager "platforms;android-34"')) {
    throw "Hotfix validation failed: the old unconfigured sdkmanager step still exists."
}

Write-Host ""
Write-Host "PHASE 7 ANDROID SDK HOTFIX APPLIED" -ForegroundColor Green
Write-Host "Workflow: .github/workflows/android-ci.yml"
Write-Host "Backup:   $backupRoot"
Write-Host ""
Write-Host "Next commands:" -ForegroundColor Cyan
Write-Host "  powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase7.ps1"
Write-Host "  git add .github/workflows/android-ci.yml scripts/Verify-Phase7.ps1"
Write-Host '  git commit -m "fix: configure Android SDK in CI"'
Write-Host "  git push"
