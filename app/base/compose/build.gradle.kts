plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.flammky.musicplayer.base.compose"
    compileSdk = 32

    defaultConfig {
        minSdk = 24
        targetSdk = 32
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
    }
}

dependencies {
    api(project(":app:base"))
    api(project(":common:androidx:viewmodel:compose"))

    /* Androidx.compose */
    dependencies {
        val v = "1.3.0-alpha01"

        // Core UI
        implementation("androidx.compose.ui:ui:$v")

        // Foundation
        implementation("androidx.compose.foundation:foundation:$v")
    }

    dependencies {
        val v = "2.2.2"
        api("io.coil-kt:coil-compose:$v")
    }
}