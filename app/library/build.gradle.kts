plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
}

android {
    namespace = "com.flammky.musicplayer.library"
    compileSdk = 32

    defaultConfig {
        minSdk = 24
        targetSdk = 32
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    api(project(":app:base"))
    api(project(":app:base:media"))

    /* androidx.compose */
    dependencies {
        val v = "1.3.0-alpha01"

        // Core UI
        implementation("androidx.compose.ui:ui:$v")
        implementation("androidx.compose.ui:ui-tooling-preview:$v")

        // Foundation
        implementation("androidx.compose.foundation:foundation:$v")

        // Material
        implementation("androidx.compose.material:material:$v")
        implementation("androidx.compose.material:material-icons-core:$v")

        // Material3
        val vMaterial3 = "1.0.0-alpha14"
        implementation("androidx.compose.material3:material3:$vMaterial3")

        // Debug
        debugImplementation("androidx.compose.ui:ui-tooling:$v")
    }

    /* androidx.navigation */
    dependencies {

        // Compose
        val vCompose = "2.5.0"
        implementation("androidx.navigation:navigation-compose:$vCompose")
    }

    /* google.dagger */
    dependencies {

        // Hilt-Android
        val vHiltAndroid = "2.43.2"
        implementation("com.google.dagger:hilt-android:$vHiltAndroid")
        kapt("com.google.dagger:hilt-android-compiler:$vHiltAndroid")
    }
}