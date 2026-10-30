import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Release signing key. Kept out of git: either keystore.properties in the project folder
// (made by scripts/create-release-key.ps1) or QC_* environment variables (GitHub Actions secrets).
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun signingValue(key: String, env: String): String? =
    keystoreProps.getProperty(key)?.takeIf { it.isNotBlank() } ?: System.getenv(env)?.takeIf { it.isNotBlank() }
val releaseStoreFile = signingValue("storeFile", "QC_KEYSTORE_FILE")

android {
    namespace = "com.mostafa.quitchallenge"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mostafa.quitchallenge"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "0.7.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingValue("storePassword", "QC_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "QC_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "QC_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Smaller, faster app: R8 removes unused library code. Rules in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            testProguardFiles("proguard-test-rules.pro")
            signingConfig = when {
                releaseStoreFile != null -> signingConfigs.getByName("release")
                // Only for testing the shrunk build on emulators; never upload such a build.
                providers.gradleProperty("debugSignRelease").isPresent -> signingConfigs.getByName("debug")
                else -> null
            }
        }
    }
    // Device tests normally run on the debug build; CI also runs them on the release build.
    testBuildType = providers.gradleProperty("testBuildType").getOrElse("debug")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.3")

    testImplementation("junit:junit:4.13.2")

    // On-device tests that drive the real app like a user (run on emulators in CI).
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
