plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
    id("io.realm.kotlin")
}

android {
    namespace = "com.flammky.musicplayer.library"
    compileSdk = 33

    defaultConfig {
        minSdk = 24
        targetSdk = 33
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = ComposeVersion.kotlinCompilerExtension
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":base:media"))
    implementation(project(":feature:base"))

    /* android.accompanist */
    dependencies {
        val v = "0.25.1"

        // PlaceHolder
        implementation("com.google.accompanist:accompanist-placeholder:$v")

        // Swipe-refresh
        implementation("com.google.accompanist:accompanist-swiperefresh:$v")
    }

    /* androidx.compose */
    dependencies {
        val v = "1.3.0-alpha01"

        // Animation
        implementation("androidx.compose.animation:animation:$v")
        implementation("androidx.compose.animation:animation-core:$v")
        implementation("androidx.compose.animation:animation-graphics:$v")

        // Core UI
        implementation("androidx.compose.ui:ui:$v")
        implementation("androidx.compose.ui:ui-tooling:$v")
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
        val vCompose = "2.5.2"
        implementation("androidx.navigation:navigation-compose:$vCompose")
    }

    /* google.dagger */
    dependencies {

        // Hilt-Android
        val vHiltAndroid = "2.48"
        implementation("com.google.dagger:hilt-android:$vHiltAndroid")
        kapt("com.google.dagger:hilt-android-compiler:$vHiltAndroid")
    }

    @Suppress("SpellCheckingInspection")
    /* JakeWharton.timber */
    dependencies {
        val v = "5.0.1"
        implementation("com.jakewharton.timber:timber:$v")
    }
}