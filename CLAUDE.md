# Quit Challenge

Android app (Kotlin, Jetpack Compose, Material 3) that helps people quit habits and addictions:
timed challenges, daily check-in notifications, app time limits via usage access, SOS urge surfing,
pressure insights, app lock and a home screen widget. All data stays on the phone (JSON file in app
storage); the app has no Internet permission and no Accessibility service, on purpose, for Google Play.

The owner speaks Bengali: reply in Bengali. App UI text is English.

## Layout

- `app/src/main/java/com/mostafa/quitchallenge/`
  - `data/` models, pure logic (`Logic.kt`, unit-tested), JSON `Repository`, `PrivacyPolicy.kt`
  - `ui/` Compose screens; `MainActivity.kt` holds string-based navigation
  - `guard/` background work: usage limits, check-in notifications and alarms, overlay warning screen
  - `security/` PIN + biometric app lock
  - `widget/` home screen widget
- `app/src/test/` JVM unit tests for `data/`
- `docs/` privacy policy page (GitHub Pages), Play Store checklist, feature research
- `scripts/setup-windows.ps1` one-time PC setup

## Build and test

On Windows (PowerShell), from the project folder:

```powershell
.\gradlew.bat testDebugUnitTest      # unit tests
.\gradlew.bat assembleDebug          # APK at app\build\outputs\apk\debug\app-debug.apk
```

Run the unit tests before every push. CI (`.github/workflows/android.yml`) runs the same on every
push and publishes the APK as the `latest` GitHub release.

## Running on the connected phone (adb)

`adb` is in `%LOCALAPPDATA%\Android\Sdk\platform-tools` (on PATH after setup).

```powershell
adb devices                                                    # phone must show "device"
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.mostafa.quitchallenge/.MainActivity
adb exec-out screencap -p > screen.png                         # then read screen.png to see the screen
adb shell uiautomator dump /sdcard/ui.xml; adb pull /sdcard/ui.xml   # element text + bounds for tapping
adb shell input tap X Y                                        # tap; also: input text "hello", input keyevent 4 (back)
adb logcat -d --pid=$(adb shell pidof com.mostafa.quitchallenge)     # app logs, crashes
adb logcat -d -b crash                                         # recent crash stack traces
```

Grant permissions without going through Settings (testing only):

```powershell
adb shell pm grant com.mostafa.quitchallenge android.permission.POST_NOTIFICATIONS
adb shell appops set com.mostafa.quitchallenge GET_USAGE_STATS allow
adb shell appops set com.mostafa.quitchallenge SYSTEM_ALERT_WINDOW allow
```

Testing loop: build -> install -> open -> screenshot / UI dump -> tap through the flow -> check logcat
for crashes -> fix -> repeat. Ask the owner before anything that wipes app data on their phone
(`adb uninstall`, `pm clear`): it is their real data.

## Git

- Work on branch `claude/new-app-development-j2flg3` (the repo's default branch).
- Commit with clear messages and `git push origin claude/new-app-development-j2flg3`.
- Bump `versionCode` / `versionName` in `app/build.gradle.kts` for user-visible releases.

## Conventions

- Keep logic that can be tested in `data/` as pure functions and add tests in `LogicTest.kt`.
- Saved JSON must stay backward compatible: add fields with defaults, never remove enum values
  (`UrgeSource.KEYWORD` stays so old data loads).
- `PrivacyPolicy.kt` and `docs/privacy-policy.html` must say the same thing; update both together.
- Don't add the Internet permission, analytics or an Accessibility service without asking the owner:
  the privacy policy and the Play Store plan depend on not having them.
