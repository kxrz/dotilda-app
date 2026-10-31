plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.roborazzi)
}

// The single source of the version: the release workflow checks the tag against it,
// and versionCode follows it (1.2.3 -> 10203) so it can never be forgotten.
val appVersion = "0.6.2"

android {
    namespace = "com.dotilda.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dotilda.app"
        minSdk = 30
        targetSdk = 35
        versionCode = appVersion.split('.').fold(0) { code, part -> code * 100 + part.toInt() }
        versionName = appVersion
    }

    signingConfigs {
        // Committed on purpose: every CI debug build shares this key, so each one installs over the last.
        getByName("debug") {
            storeFile = file("debug.keystore")
        }
        // The release key lives outside the repo, injected by CI from secrets (see .github/workflows/build.yml).
        System.getenv("RELEASE_KEYSTORE")?.let { path ->
            create("release") {
                storeFile = file(path)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // A separate app on the device, so test builds and the released app coexist.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Shrunk like the release, so every test APK also checks the R8 rules on the device.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Screenshot tests (Robolectric + Roborazzi) render the real screens on the JVM.
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.testLogging {
                events("started", "passed", "failed")
                showStandardStreams = true
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.mmd)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.serialization.json)
    implementation(libs.datastore.preferences)
    testImplementation(libs.junit)
    testImplementation(libs.epubcheck)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
