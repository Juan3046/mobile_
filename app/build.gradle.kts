import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.rmgames.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rmgames"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-phase0"
    }

    // Release signing (docs/04 "Release build"; no Google Play — APKs are shared directly): the key lives OUTSIDE git, described by a
    // git-ignored `keystore.properties` (storeFile, storePassword, keyAlias, keyPassword). Without
    // it, release is signed with the local debug key so it can still be installed for testing —
    // don't share that APK.
    val keystoreFile = rootProject.file("keystore.properties")
    val releaseKey = if (keystoreFile.exists()) Properties().apply { keystoreFile.inputStream().use(::load) } else null
    signingConfigs {
        if (releaseKey != null) {
            create("release") {
                storeFile = rootProject.file(releaseKey.getProperty("storeFile"))
                storePassword = releaseKey.getProperty("storePassword")
                keyAlias = releaseKey.getProperty("keyAlias")
                keyPassword = releaseKey.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: shrink + obfuscate. Libraries (CameraX, ML Kit, Firebase, DataStore) ship their
            // own consumer rules; ours only covers what R8 can't see (proguard-rules.pro).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:audio"))
    implementation(project(":core:online"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth) // FirebaseApp check in OnlineWiring
    implementation(project(":core:sensors"))
    implementation(project(":core:vision"))
    implementation(project(":feature:home"))
    implementation(project(":feature:session"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:ranking"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:game:casino"))
    implementation(project(":feature:game:onion"))
    implementation(project(":feature:game:blink"))
    implementation(project(":feature:game:compass"))
    implementation(project(":feature:game:soda"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
}

// Firebase (plan 09 stream E): the config file is per-developer and git-ignored. Without it the
// app builds and runs offline with the local stand-ins (AppContainer checks FirebaseApp at runtime).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
