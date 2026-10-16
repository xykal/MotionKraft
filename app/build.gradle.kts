plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "xyz.xyverse.motionkraft"
    compileSdk = 34

    defaultConfig {
        applicationId = "xyz.xyverse.motionkraft"
        minSdk = 24
        targetSdk = 34
        versionCode = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME") ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
        resourceConfigurations += listOf("en")
    }

    // Split APK per ABI (menghasilkan APK terpisah untuk arm64-v8a dan armeabi-v7a
    // plus universal APK, masing-masing dengan nama file yang jelas).
    // Version code per-split diatur otomatis oleh AGP (arm64 offset > armv7 offset > universal).
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    applicationVariants.configureEach {
        val variant = this
        outputs.configureEach {
            val out = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            val abi = out.getFilter(com.android.build.VariantOutput.ABI) ?: "universal"
            out.outputFileName = "MotionKraft-${variant.versionName}-b${variant.versionCode}-$abi.apk"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/motionkraft-release.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                ?: (project.findProperty("SIGNING_STORE_PASSWORD") as? String ?: "")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                ?: (project.findProperty("SIGNING_KEY_ALIAS") as? String ?: "motionkraft")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                ?: (project.findProperty("SIGNING_KEY_PASSWORD") as? String ?: "")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,DEPENDENCIES,NOTICE,LICENSE}"
            excludes += "/DebugProbesKt.bin"
        }
    }
    bundle {
        language { enableSplit = false }
        density { enableSplit = true }
        abi { enableSplit = true }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")
    implementation("androidx.media3:media3-transformer:1.2.1")

    implementation("dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.7")

    implementation("io.coil-kt:coil-compose:2.5.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
