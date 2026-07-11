# Phase 2 verification

Follow these steps after extracting the project.

## 1. Open the correct folder

In Android Studio, select **Open** and choose the `ProjectLedger` folder that contains:

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradlew.bat`
- `app`

Choose **Trust Project** if Android Studio asks.

## 2. Select the embedded JDK

Open:

`File > Settings > Build, Execution, Deployment > Build Tools > Gradle`

Set **Gradle JDK** to Android Studio's embedded JDK / `jbr-17`.

## 3. Wait for Gradle sync

The first sync downloads Gradle and Android libraries, so internet access is required. Accept SDK licences if Android Studio requests them.

## 4. Verify by terminal

The easiest option is the included verification script. Open PowerShell in the project root and run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase2.ps1
```

It selects Android Studio's bundled JDK, creates `local.properties`, checks ADB, runs unit tests, and builds the debug APK.

Alternatively, after Android Studio has synchronized the project, run:

```powershell
.\gradlew.bat --no-daemon clean test assembleDebug
```

Expected ending:

```text
BUILD SUCCESSFUL
```

Expected APK:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 5. Run on the connected phone

You can build and install in one command:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Verify-Phase2.ps1 -InstallOnPhone
```

Or use Android Studio:

- Keep the phone unlocked.
- Confirm USB debugging is still authorized.
- Select the physical phone in Android Studio's device menu.
- Select the `app` run configuration.
- Press Run.

The app should display a dark card with:

- Project Ledger
- Android project initialized successfully.
- Phase 2 checkpoint

## 6. Initialize Git locally

From the project root:

```powershell
git init
git add .
git commit -m "chore: initialize Project Ledger Android app"
```

Do not create the remote GitHub repository until the local build succeeds.

## Send back

Send the complete output of:

```powershell
.\gradlew.bat clean test assembleDebug
```

Also tell ChatGPT whether the starter screen opened on the phone.
