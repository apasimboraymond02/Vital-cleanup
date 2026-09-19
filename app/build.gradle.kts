plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.teraxes.vital"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.teraxes.vital"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Release signing is configured via environment variables / gradle.properties.
        // Create your own keystore locally and DO NOT commit it.
        // See ../local.properties.example and README for setup.
        val release by signingConfigs.creating {
            val keystorePath = (findProperty("VITAL_RELEASE_STORE_FILE") as String?)
                ?: System.getenv("VITAL_RELEASE_STORE_FILE")
                ?: "${rootDir}/release-key.keystore"
            val keystorePass = (findProperty("VITAL_RELEASE_STORE_PASSWORD") as String?)
                ?: System.getenv("VITAL_RELEASE_STORE_PASSWORD")
            val aliasValue = (findProperty("VITAL_RELEASE_KEY_ALIAS") as String?)
                ?: System.getenv("VITAL_RELEASE_KEY_ALIAS")
            val keyPass = (findProperty("VITAL_RELEASE_KEY_PASSWORD") as String?)
                ?: System.getenv("VITAL_RELEASE_KEY_PASSWORD")
            if (file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                if (keystorePass != null) storePassword = keystorePass
                if (aliasValue != null) keyAlias = aliasValue
                if (keyPass != null) keyPassword = keyPass
            }
        }
        getByName("debug") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    testImplementation(libs.junit)
}
