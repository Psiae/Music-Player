plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // TODO: move to base-auth-android
    id("org.jetbrains.kotlin.plugin.serialization")

    id("dagger.hilt.android.plugin")

    id("kotlin-kapt")
    id("com.google.devtools.ksp")
}

android {
    namespace = "dev.dexsr.klio.base.common"
    compileSdk = 33

    defaultConfig {
        minSdk = 24
        targetSdk = 33
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            resValue(type = "string", name = "app_name", "MusicPlayer_Debug")
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
        buildConfig = true
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = ComposeVersion.kotlinCompilerExtension
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(project(":base:base-common-jvm")) {
        exclude(group = project(":core:core-jvm").group, project(":core:core-jvm").name)
    }

    api(project(":core:core-android"))

    api("androidx.core:core-ktx:1.9.0")

    /* android.material */
    dependencies {
        val v = "1.7.0"
        api("com.google.android.material:material:$v")
    }

    // TODO
    /* androidx.core */
    dependencies {
        val vKTX = "1.9.0"
        val vSplashScreen = "1.0.0"

        api("androidx.core:core-ktx:$vKTX")
        api("androidx.core:core-splashscreen:$vSplashScreen")
    }

    //
    // Java-common
    //

    /* google.guava */
    dependencies {

        // android
        val vAndroid = "31.1-android"
        api("com.google.guava:guava:$vAndroid")
    }

    //
    // Debug
    //

    @Suppress("SpellCheckingInspection")
    /* jakeWharton.timber */
    dependencies {
        val v = "5.0.1"
        api("com.jakewharton.timber:timber:$v")
    }

    /* androidx.datastore */
    dependencies {
        val v = "1.1.0-alpha01"
        api("androidx.datastore:datastore:$v")
    }


    //
    // DI
    //

    /* google.dagger */
    dependencies {

        // Hilt-Android
        val vHiltAndroid = "2.48"
        api("com.google.dagger:hilt-android:$vHiltAndroid")
        kapt("com.google.dagger:hilt-android-compiler:$vHiltAndroid")
    }

    /* Androidx.lifecycle */
    dependencies {
        val v = "2.6.0-alpha01"
        api("androidx.lifecycle:lifecycle-common:2.6.0-alpha01")
        api("androidx.lifecycle:lifecycle-runtime-ktx:$v")
        // do we still need viewmodel ?
        api("androidx.lifecycle:lifecycle-viewmodel-ktx:$v")
    }

    //
    // Compose
    //

    /* Androidx.compose */
    dependencies {
        val v = "1.3.0-alpha01"

        // Core UI
        implementation("androidx.compose.ui:ui:$v")

        // Foundation
        implementation("androidx.compose.foundation:foundation:$v")
    }

    /* Androidx.lifecycle */
    dependencies {
        val v = "2.6.0-alpha01"
        api("androidx.lifecycle:lifecycle-viewmodel-compose:$v")
    }

    /* insert-koin.koin */
    dependencies {
        api(libs.koin.android)
    }

    //
    // Secure
    //

    /* androidx.secure */
    dependencies {
        val v = "1.0.0"
        api("androidx.security:security-crypto:$v")
    }

    //
    // Concurrency
    //

    /* Androidx.concurrent */
    dependencies {

        // Futures
        val vFutures = "1.1.0"
        api("androidx.concurrent:concurrent-futures:$vFutures")
        api("androidx.concurrent:concurrent-futures-ktx:$vFutures")
    }

    /* androidx.lifecycle */
    dependencies {
        val v = "2.6.0-alpha03"
        api("androidx.lifecycle:lifecycle-runtime-ktx:$v")
    }

    /* androidx.palette */
    dependencies {
        val v = "1.0.0"
        api("androidx.palette:palette:$v")
    }

    //
    // Graphics
    //

    /* coil-kt */
    dependencies {
        val v = "2.2.2"
        api("io.coil-kt:coil:$v")
        api("io.coil-kt:coil-gif:$v")
    }

    /* wasabeef.transformers*/
    dependencies {
        val vCoil = "1.0.6"
        api("jp.wasabeef.transformers:coil:$vCoil")
        api("jp.wasabeef.transformers:coil-gpu:$vCoil")
    }


    /// Move to compose
    /* androidx.activity */
    dependencies {
        val v = "1.6.1"
        api("androidx.activity:activity-compose:$v")
    }

    /* androidx.compose */
    dependencies {


        // Core UI
        api(libs.androidx.compose.ui.ui.asProvider())
        api(libs.androidx.compose.ui.ui.util)
        api(libs.androidx.compose.ui.ui.tooling.preview)

        // Debug
        debugApi(libs.androidx.compose.ui.ui.tooling.asProvider())

        // Foundation
        api(libs.androidx.compose.foundation.foundation.asProvider())
        api(libs.androidx.compose.foundation.foundation.layout)

        // Material
        api(libs.androidx.compose.material.material)
        api("androidx.compose.material:material-icons-core:1.3.1")

        // Material3
        val vMaterial3 = "1.0.1"
        api(libs.androidx.compose.material3.material3)
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

    dependencies {
        api(libs.koin.androidx.compose)
    }
}