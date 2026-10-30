# Added to the app ONLY when the device tests run against the release build (-PtestBuildType=release).
# The test runner borrows these libraries from the app APK, so R8 must not remove them there.
# The shipped app is built without this file.
-keep class androidx.tracing.** { *; }
-keep class androidx.concurrent.futures.** { *; }
-keep class androidx.annotation.** { *; }
