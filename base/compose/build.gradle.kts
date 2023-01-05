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
    api(project(":base"))
    api(project(":base:common:androidx:viewmodel:compose"))

    /* androidx.compose */
    dependencies {
        val v = "1.3.0-alpha01"

        // Core UI
        api("androidx.compose.ui:ui:$v")
        api("androidx.compose.ui:ui-tooling-preview:$v")
        api("androidx.compose.ui:ui-util:$v")

        // Foundation
        api("androidx.compose.foundation:foundation:$v")

        // Material
        api("androidx.compose.material:material:$v")
        api("androidx.compose.material:material-icons-core:$v")

        // Material3
        val vMaterial3 = "1.0.0-alpha14"
        api("androidx.compose.material3:material3:$vMaterial3")
    }

    /* androidx.navigation */
    dependencies {

        // Compose
        val vCompose = "2.6.0-alpha04"
        api("androidx.navigation:navigation-compose:$vCompose")
    }

    /* coil-kt */
    dependencies {
        val v = "2.2.2"
        api("io.coil-kt:coil-compose:$v")
    }

    /* lottie */
    dependencies {
        val v = "5.2.0"
        api("com.airbnb.android:lottie:$v")
        api("com.airbnb.android:lottie-compose:$v")
    }
}