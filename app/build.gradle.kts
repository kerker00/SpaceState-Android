import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The release key stays outside the repository. Without the file, release builds
// stay unsigned; pass -PreleaseSigning=<path> to use another one.
val releaseSigning = providers.gradleProperty("releaseSigning")
    .orElse("${System.getProperty("user.home")}/.android/spacestate-release.properties")
    .map { file(it) }
    .get()
    .takeIf { it.isFile }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }

android {
    namespace = "net.grafixmafia.spacestate"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "net.grafixmafia.spacestate"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // SpacePush serves the spaces' state and sends notifications; the app falls
        // back to reading the spaces directly. For a local SpacePush on the emulator,
        // use "http://10.0.2.2:8080" (and allow cleartext for that host).
        buildConfigField("String", "SPACEPUSH_URL", "\"https://push.grafixmafia.net\"")
    }

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = file(releaseSigning.getProperty("storeFile"))
                storePassword = releaseSigning.getProperty("storePassword")
                keyAlias = releaseSigning.getProperty("keyAlias")
                keyPassword = releaseSigning.getProperty("keyPassword")
            }
        }
    }

    // One APK per ABI for downloads outside the Play Store; app bundles split by ABI anyway.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }

    // Compressed native libraries make the APK download about a third of its uncompressed size.
    packaging {
        jniLibs.useLegacyPackaging = true
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                // The whole app: limiting R8 to androidx and Kotlin packages moved a class
                // away from its package-private superclass (IllegalAccessError at launch).
                enable = true
            }
        }
    }
    ndkVersion = "30.0.16248370"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    androidResources {
        // Lets Android 13+ offer the app's languages in the system settings.
        generateLocaleConfig = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    testLogging {
        events("started", "passed", "skipped", "failed")
    }
}

dependencies {
    implementation(libs.spacestatekit)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
