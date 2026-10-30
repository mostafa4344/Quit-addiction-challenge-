# Only for the on-device test APK when testing the shrunk release build.
# Annotation-only dependencies of androidx.test that are not shipped at runtime:
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
# The test runner finds tests and rules by reflection.
-keep class androidx.test.** { *; }
-keep class org.junit.** { *; }
-keep class junit.** { *; }
-keep class org.hamcrest.** { *; }
-keep class com.mostafa.quitchallenge.** { *; }
