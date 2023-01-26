plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
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
        kotlinCompilerExtensionVersion = "1.4.0"
    }
}

dependencies {
    api(project(":base"))
    api(project(":base:common:androidx:viewmodel:compose"))

    /* androidx.activity */
    dependencies {
        val v = "1.6.1"
        api("androidx.activity:activity-compose:$v")
    }

    /* androidx.compose */
    dependencies {
        val vui = "1.3.3"

        // Core UI
        api("androidx.compose.ui:ui:$vui")
        api("androidx.compose.ui:ui-tooling-preview:$vui")
        api("androidx.compose.ui:ui-util:$vui")

        // Debug
        debugApi("androidx.compose.ui:ui-tooling:$vui")

        val vf = "1.3.1"
        // Foundation
        api("androidx.compose.foundation:foundation:$vf")

        // Material
        api("androidx.compose.material:material:$vf")
        api("androidx.compose.material:material-icons-core:$vf")

        // Material3
        val vMaterial3 = "1.0.1"
        api("androidx.compose.material3:material3:$vMaterial3")
    }

    /* androidx.hilt */
    dependencies {
        val v = "1.0.0"
        api("androidx.hilt:hilt-navigation-compose:$v")
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

    @Suppress("SpellCheckingInspection")
    /* google.accompanist */
    dependencies  {
        val v = "0.28.0"

        // Drawable
        api("com.google.accompanist:accompanist-drawablepainter:$v")

        // FlowLayout
        api("com.google.accompanist:accompanist-flowlayout:$v")

        // Navigation
        api("com.google.accompanist:accompanist-navigation-animation:$v")
        api("com.google.accompanist:accompanist-navigation-material:$v")

        // Pager
        api("com.google.accompanist:accompanist-pager:$v")
        api("com.google.accompanist:accompanist-pager-indicators:$v")

        // Permissions
        api("com.google.accompanist:accompanist-permissions:$v")

        // PlaceHolder
        api("com.google.accompanist:accompanist-placeholder:$v")

        // Swipe-refresh
        api("com.google.accompanist:accompanist-swiperefresh:$v")

        // SysUI
        api("com.google.accompanist:accompanist-systemuicontroller:$v")
    }

    /* google.dagger */
    dependencies {

        // Hilt-Android
        val vHiltAndroid = "2.44"
        api("com.google.dagger:hilt-android:$vHiltAndroid")
        kapt("com.google.dagger:hilt-android-compiler:$vHiltAndroid")
    }
}