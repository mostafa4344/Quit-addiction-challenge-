#!/usr/bin/env bash
# Runs the on-device UI tests on the connected emulator and collects screenshots and logs,
# keeping the test result as the exit code.
set -u
mkdir -p device-results
adb shell settings put system font_scale "${FONT_SCALE:-1.0}"
adb logcat -c
VARIANT="${VARIANT:-Debug}"
extra=""
if [ "$VARIANT" = Release ]; then extra="-PtestBuildType=release -PdebugSignRelease"; fi
./gradlew --no-daemon $extra connected${VARIANT}AndroidTest
status=$?
adb pull /data/local/tmp/qc-shots device-results/screenshots || true
adb logcat -d > device-results/logcat.txt || true
adb logcat -d -b crash > device-results/crash.txt || true
exit $status
