plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// The single source of the version: the release workflow checks the tag against it,
// and versionCode follows it (1.2.3 -> 10203) so it can never be forgotten.
val appVersion = "0.3.0"

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
        }
        release {
            isMinifyEnabled = false
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
}
