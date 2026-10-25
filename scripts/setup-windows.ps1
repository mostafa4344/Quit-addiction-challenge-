# Quit Challenge - one-time setup for a Windows PC.
#
# Installs Git, Java 17, the Android SDK (with adb), Claude Code, downloads the project,
# builds the app and installs it on a phone connected by USB (if there is one).
# Safe to run again: steps that are already done are skipped.
#
# Run in PowerShell:
#   powershell -ExecutionPolicy Bypass -File setup-windows.ps1
# or straight from GitHub:
#   irm https://raw.githubusercontent.com/mostafa4344/Quit-addiction-challenge-/refs/heads/claude/new-app-development-j2flg3/scripts/setup-windows.ps1 | iex

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"   # makes Invoke-WebRequest much faster

$RepoUrl    = "https://github.com/mostafa4344/Quit-addiction-challenge-.git"
$Branch     = "claude/new-app-development-j2flg3"
$ProjectDir = Join-Path $env:USERPROFILE "Projects\Quit-addiction-challenge-"
$SdkDir     = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$PackageId  = "com.mostafa.quitchallenge"

function Step($text)  { Write-Host "`n==> $text" -ForegroundColor Cyan }
function Ok($text)    { Write-Host "    [OK] $text" -ForegroundColor Green }
function Warn($text)  { Write-Host "    [!] $text" -ForegroundColor Yellow }

function Refresh-Path {
    $env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
                [Environment]::GetEnvironmentVariable("Path", "User")
}

function Add-UserPath($dir) {
    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    if (-not $userPath) { $userPath = "" }
    if (($userPath -split ";") -notcontains $dir) {
        [Environment]::SetEnvironmentVariable("Path", ($userPath.TrimEnd(";") + ";" + $dir).TrimStart(";"), "User")
    }
    if (($env:Path -split ";") -notcontains $dir) { $env:Path = "$env:Path;$dir" }
}

function Install-WithWinget($id, $name) {
    if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
        throw "winget was not found. Install 'App Installer' from the Microsoft Store, then run this script again."
    }
    Write-Host "    Installing $name..."
    winget install --id $id -e --silent --accept-package-agreements --accept-source-agreements | Out-Host
    Refresh-Path
}

# ---------------------------------------------------------------------------
Step "1/7 Git"
if (Get-Command git -ErrorAction SilentlyContinue) { Ok "Git is already installed" }
else { Install-WithWinget "Git.Git" "Git"; Ok "Git installed" }

# ---------------------------------------------------------------------------
Step "2/7 Java 17 (needed to build Android apps)"
function Find-Jdk17 {
    $roots = @("$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Java", "$env:ProgramFiles\Microsoft")
    foreach ($root in $roots) {
        if (Test-Path $root) {
            $jdk = Get-ChildItem $root -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($jdk) { return $jdk.FullName }
        }
    }
    return $null
}
$jdk = Find-Jdk17
if (-not $jdk) {
    Install-WithWinget "EclipseAdoptium.Temurin.17.JDK" "Java 17 (Temurin)"
    $jdk = Find-Jdk17
}
if (-not $jdk) { throw "Java 17 could not be found after installing. Install 'Temurin 17 JDK' manually and run again." }
[Environment]::SetEnvironmentVariable("JAVA_HOME", $jdk, "User")
$env:JAVA_HOME = $jdk
Add-UserPath "$jdk\bin"
Ok "Java 17 at $jdk"

# ---------------------------------------------------------------------------
Step "3/7 Android SDK and adb"
$sdkManager = Join-Path $SdkDir "cmdline-tools\latest\bin\sdkmanager.bat"
if (-not (Test-Path $sdkManager)) {
    New-Item -ItemType Directory -Force -Path $SdkDir | Out-Null
    $zip = Join-Path $env:TEMP "android-cmdline-tools.zip"
    $downloaded = $false
    foreach ($ver in @("13114758", "11076708")) {
        try {
            Write-Host "    Downloading Android command-line tools ($ver)..."
            Invoke-WebRequest "https://dl.google.com/android/repository/commandlinetools-win-${ver}_latest.zip" -OutFile $zip
            $downloaded = $true
            break
        } catch { Warn "Version $ver not available, trying another" }
    }
    if (-not $downloaded) { throw "Could not download the Android command-line tools." }
    $unpack = Join-Path $env:TEMP "android-cmdline-tools"
    if (Test-Path $unpack) { Remove-Item $unpack -Recurse -Force }
    Expand-Archive $zip -DestinationPath $unpack
    New-Item -ItemType Directory -Force -Path (Join-Path $SdkDir "cmdline-tools") | Out-Null
    $latest = Join-Path $SdkDir "cmdline-tools\latest"
    if (Test-Path $latest) { Remove-Item $latest -Recurse -Force }
    Move-Item (Join-Path $unpack "cmdline-tools") $latest
    Remove-Item $zip -Force
}
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $SdkDir, "User")
$env:ANDROID_HOME = $SdkDir
Add-UserPath (Join-Path $SdkDir "platform-tools")
Add-UserPath (Join-Path $SdkDir "cmdline-tools\latest\bin")

Write-Host "    Accepting Android SDK licenses..."
$yes = ("y`n" * 30)
$yes | & $sdkManager --licenses | Out-Null
Write-Host "    Installing platform-tools (adb), Android 14 platform and build tools (a few minutes)..."
& $sdkManager "platform-tools" "platforms;android-34" "build-tools;34.0.0" | Out-Null
if (-not (Test-Path (Join-Path $SdkDir "platform-tools\adb.exe"))) { throw "adb was not installed. Check your internet connection and run again." }
Ok "Android SDK at $SdkDir"

# ---------------------------------------------------------------------------
Step "4/7 Download the project"
if (Test-Path (Join-Path $ProjectDir ".git")) {
    Push-Location $ProjectDir
    git fetch origin $Branch | Out-Host
    git checkout $Branch | Out-Host
    git pull --ff-only origin $Branch | Out-Host
    Pop-Location
    Ok "Project updated in $ProjectDir"
} else {
    New-Item -ItemType Directory -Force -Path (Split-Path $ProjectDir) | Out-Null
    git clone --branch $Branch $RepoUrl $ProjectDir | Out-Host
    Ok "Project downloaded to $ProjectDir"
}
# Tell Gradle where the SDK is (this file is not committed to git).
"sdk.dir=" + ($SdkDir -replace "\\", "/") | Set-Content -Encoding ASCII (Join-Path $ProjectDir "local.properties")

# ---------------------------------------------------------------------------
Step "5/7 Build the app (first build downloads Gradle, ~5-10 minutes)"
Push-Location $ProjectDir
& .\gradlew.bat --no-daemon testDebugUnitTest assembleDebug
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "The build failed. Scroll up for the error." }
Pop-Location
$apk = Join-Path $ProjectDir "app\build\outputs\apk\debug\app-debug.apk"
Ok "APK built: $apk"

# ---------------------------------------------------------------------------
Step "6/7 Install on your phone (USB)"
$adb = Join-Path $SdkDir "platform-tools\adb.exe"
& $adb start-server | Out-Null
$devices = & $adb devices | Select-String "\tdevice$"
if ($devices) {
    & $adb install -r $apk | Out-Host
    & $adb shell am start -n "$PackageId/.MainActivity" | Out-Null
    Ok "Installed and opened on your phone"
} else {
    Warn "No phone found. To connect one:"
    Write-Host "      1. Phone: Settings > About phone > tap 'Build number' 7 times (turns on Developer options)."
    Write-Host "      2. Settings > System > Developer options > turn on 'USB debugging'."
    Write-Host "      3. Connect the phone by USB and tap 'Allow' on the 'Allow USB debugging?' popup."
    Write-Host "      4. Then run:  adb install -r `"$apk`""
}

# ---------------------------------------------------------------------------
Step "7/7 Claude Code"
Refresh-Path
Add-UserPath (Join-Path $env:USERPROFILE ".local\bin")
if (Get-Command claude -ErrorAction SilentlyContinue) {
    Ok "Claude Code is already installed"
} else {
    Write-Host "    Installing Claude Code..."
    Invoke-RestMethod https://claude.ai/install.ps1 | Invoke-Expression
    Refresh-Path
    Add-UserPath (Join-Path $env:USERPROFILE ".local\bin")
    Ok "Claude Code installed"
}

Write-Host "`nAll done!" -ForegroundColor Green
Write-Host "Next:"
Write-Host "  1. Close this window and open a NEW PowerShell window (so the new settings load)."
Write-Host "  2. Run:"
Write-Host "       cd `"$ProjectDir`""
Write-Host "       claude"
Write-Host "  3. Sign in when asked, connect your phone by USB, and tell Claude what to do."
Write-Host "     Claude reads CLAUDE.md in the project to learn how to build, install and test on the phone."
