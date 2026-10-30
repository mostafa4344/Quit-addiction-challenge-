# Creates the app's release signing key ("upload key") on this PC, one time only.
#
#   powershell -ExecutionPolicy Bypass -File scripts\create-release-key.ps1
#
# What it does:
#   1. Makes %USERPROFILE%\QuitChallenge-signing\upload-key.jks with a strong random password.
#   2. Writes keystore.properties in the project folder (git ignores it) so local release builds are signed.
#   3. Puts the key into the GitHub repository secrets (with the GitHub CLI if it is installed and
#      signed in), so the Release workflow can build signed versions. Otherwise it writes the four
#      values to a file and tells you where to paste them.
#
# BACK UP the QuitChallenge-signing folder (USB drive + a private cloud folder). Google Play App
# Signing can replace a lost upload key, but it takes days; the website APK can only be updated by
# people who installed it if the same key is used.

$ErrorActionPreference = "Stop"
$project = Split-Path -Parent $PSScriptRoot
$keyDir = Join-Path $env:USERPROFILE "QuitChallenge-signing"
$keyFile = Join-Path $keyDir "upload-key.jks"
$alias = "upload"
$repo = "mostafa4344/Quit-addiction-challenge-"

function Say($text, $color = "Cyan") { Write-Host $text -ForegroundColor $color }

if (Test-Path $keyFile) {
    Say "A key already exists at $keyFile. Not touching it (replacing it would break updates)." Yellow
    Say "Delete it by hand only if you are sure it was never used for a release." Yellow
    exit 1
}

# keytool comes with Java 17 (installed by setup-windows.ps1).
$keytool = $null
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\keytool.exe"))) {
    $keytool = Join-Path $env:JAVA_HOME "bin\keytool.exe"
} else {
    $found = Get-Command keytool -ErrorAction SilentlyContinue
    if ($found) { $keytool = $found.Source }
}
if (-not $keytool) { throw "keytool not found. Run scripts\setup-windows.ps1 first (it installs Java 17)." }

# 32 random letters and digits (PKCS12 uses the same password for the store and the key).
$chars = [char[]]"ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$password = -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })

New-Item -ItemType Directory -Force -Path $keyDir | Out-Null
Say "Creating the signing key..."
& $keytool -genkeypair -v -keystore $keyFile -storetype PKCS12 -alias $alias `
    -keyalg RSA -keysize 4096 -validity 10000 `
    -dname "CN=Quit Challenge, O=Mostafa, C=BD" `
    -storepass $password -keypass $password | Out-Null
if ($LASTEXITCODE -ne 0) { throw "keytool failed" }

# Local signed builds (.\gradlew.bat bundleRelease) read this file.
$storePath = $keyFile -replace "\\", "/"
@"
storeFile=$storePath
storePassword=$password
keyAlias=$alias
keyPassword=$password
"@ | Set-Content -Encoding ASCII (Join-Path $project "keystore.properties")

# A copy of the password next to the key, for the backup.
@"
Quit Challenge release signing key
Key file : upload-key.jks
Alias    : $alias
Password : $password   (store and key)
Created  : $(Get-Date -Format "yyyy-MM-dd")

Keep this folder backed up and private. Never put it in git or send it to anyone.
"@ | Set-Content -Encoding UTF8 (Join-Path $keyDir "README-KEY.txt")

$base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keyFile))
$secrets = [ordered]@{
    QC_KEYSTORE_BASE64   = $base64
    QC_KEYSTORE_PASSWORD = $password
    QC_KEY_ALIAS         = $alias
    QC_KEY_PASSWORD      = $password
}

$gh = Get-Command gh -ErrorAction SilentlyContinue
$ghReady = $false
if ($gh) { & gh auth status 2>$null | Out-Null; $ghReady = ($LASTEXITCODE -eq 0) }

if ($ghReady) {
    foreach ($name in $secrets.Keys) {
        $secrets[$name] | & gh secret set $name --repo $repo
        if ($LASTEXITCODE -ne 0) { throw "Could not set secret $name" }
    }
    Say "GitHub secrets saved. The Release workflow can now build signed versions." Green
} else {
    $out = Join-Path $keyDir "github-secrets.txt"
    ($secrets.GetEnumerator() | ForEach-Object { "$($_.Key)`r`n$($_.Value)`r`n" }) -join "`r`n" |
        Set-Content -Encoding ASCII $out
    Say "GitHub CLI not ready, so add the secrets by hand:" Yellow
    Say "  1. Open https://github.com/$repo/settings/secrets/actions"
    Say "  2. 'New repository secret' four times, name and value from: $out"
    Say "  3. Then delete $out (the key stays in the folder)."
}

Say ""
Say "Done. Key folder: $keyDir" Green
Say "BACK IT UP NOW (USB drive and a private cloud folder)." Yellow
