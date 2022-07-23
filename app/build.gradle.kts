import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {

    compileSdk = 32

    defaultConfig {
        applicationId = "com.kylentt.musicplayer"
        minSdk = 24
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isDebuggable = false
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.1.1"
    }

    @Suppress("SpellCheckingInspection")
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }

    namespace = "com.kylentt.musicplayer"
}

dependencies {

    /* Android.material */
    dependencies {
        val v = "1.6.1"
        implementation("com.google.android.material:material:$v")
    }

    /* Androidx.activity */
    dependencies {
        val v = "1.5.0"
        implementation("androidx.activity:activity-compose:$v")
    }

    /* Androidx.compose */
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
        implementation("androidx.compose.material:material-icons-extended:$v")

        // Material3
        val vMaterial3 = "1.0.0-alpha14"
        implementation("androidx.compose.material3:material3:$vMaterial3")

        // Debug
        debugImplementation("androidx.compose.ui:ui-tooling:$v")
    }

    /* Androidx.concurrent */
    dependencies {

        // Futures
        val vFutures = "1.1.0"
        implementation("androidx.concurrent:concurrent-futures:$vFutures")
    }

    /* Androidx.core */
    dependencies {
        val vKTX = "1.8.0"
        val vSplashScreen = "1.0.0-rc01"

        implementation("androidx.core:core-ktx:$vKTX")
        implementation("androidx.core:core-splashscreen:$vSplashScreen")
    }

    /* Androidx.datastore */
    dependencies {
        val v = "1.0.0"

        implementation("androidx.datastore:datastore:$v")
    }

    /* Androidx.hilt */
    dependencies {
        val v = "1.0.0"
        implementation("androidx.hilt:hilt-navigation-compose:$v")
    }

    /* Androidx.lifecycle */
    dependencies {
        val v = "2.6.0-alpha01"
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:$v")
    }

    /* Androidx.media3 */
    dependencies {
        val v = "1.0.0-beta01"
        implementation("androidx.media3:media3-exoplayer:$v")
        implementation("androidx.media3:media3-session:$v")
        implementation("androidx.media3:media3-common:$v")
        implementation("androidx.media3:media3-ui:$v")
    }

    /* Androidx.navigation */
    dependencies {

        // Compose
        val vCompose = "2.5.0"
        implementation("androidx.navigation:navigation-compose:$vCompose")
    }

    /* Androidx.Room */
    dependencies {
        val v = "2.4.2"

        implementation("androidx.room:room-runtime:$v")
        implementation("androidx.room:room-ktx:$v")
        kapt("androidx.room:room-compiler:$v")
    }

    /* Androidx.startup */
    dependencies {
        val v = "1.1.1"
        implementation("androidx.startup:startup-runtime:$v")
    }

    /* coil-kt */
    dependencies  {
        val v = "2.1.0"
        implementation("io.coil-kt:coil:$v")
        implementation("io.coil-kt:coil-compose:$v")
    }


    @Suppress("SpellCheckingInspection")
    /* Github.Adonai */
    dependencies {

        // JAudioTagger
        val vJAudioTagger = "2.3.15"
        implementation("com.github.Adonai:jaudiotagger:$vJAudioTagger")
    }

    @Suppress("SpellCheckingInspection")
    /* Github.wseeman */
    dependencies {

        // FFmpegMediaMetadataRetriever
        implementation("com.github.wseemann:FFmpegMediaMetadataRetriever-core:1.0.16")
        implementation("com.github.wseemann:FFmpegMediaMetadataRetriever-native:1.0.16")
    }


    @Suppress("SpellCheckingInspection")
    /* Google.accompanist */
    dependencies  {
        val v = "0.24.3-alpha"
        implementation("com.google.accompanist:accompanist-drawablepainter:$v")
        implementation("com.google.accompanist:accompanist-navigation-material:$v")
        implementation("com.google.accompanist:accompanist-permissions:$v")
        implementation("com.google.accompanist:accompanist-placeholder:$v")
        implementation("com.google.accompanist:accompanist-systemuicontroller:$v")

        // Pager
        val vPager = "0.24.4-alpha"
        implementation("com.google.accompanist:accompanist-pager:$vPager")

        // Navigation-Animation
        val vNavAnimation = "0.24.4-alpha"
        implementation("com.google.accompanist:accompanist-navigation-animation:$vNavAnimation")
    }

    /* Google.android */
    dependencies {

        // Exoplayer
        val vExoPlayer = "2.18.0"
        implementation("com.google.android.exoplayer:extension-mediasession:$vExoPlayer")
    }

    /* Google.dagger */
    dependencies {

        // Hilt-Android
        val vHiltAndroid = "2.41"
        implementation("com.google.dagger:hilt-android:$vHiltAndroid")
        kapt("com.google.dagger:hilt-android-compiler:$vHiltAndroid")
    }

    /* Google.guava */
    dependencies {
        val v = "31.0.1-android"
        implementation("com.google.guava:guava:$v")
    }

    /* Instrumentation Test */
    dependencies {

        // JUnit
        val vJUnitExt = "1.1.3"
        val vJUnit4Compose = "1.1.1"
        androidTestImplementation("androidx.test.ext:junit:$vJUnitExt")
        androidTestImplementation("androidx.compose.ui:ui-test-junit4:$vJUnit4Compose")

        // Espresso
        val vEspressoCore = "3.4.0"
        androidTestImplementation("androidx.test.espresso:espresso-core:$vEspressoCore")
    }


    @Suppress("SpellCheckingInspection")
    /* JakeWharton.timber */
    dependencies {
        val v = "5.0.1"
        implementation("com.jakewharton.timber:timber:$v")
    }

    /* Jetbrains.kotlin */
    dependencies {

        // reflect
        val vReflect = "1.7.10"
        implementation("org.jetbrains.kotlin:kotlin-reflect:$vReflect")
    }

    /* Jetbrains.kotlinx */
    dependencies {

        // Coroutines-Guava
        val vCoroutinesGuava = "1.6.1"
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:$vCoroutinesGuava")

        // Serialization-Json
        val vSerializationJson = "1.3.2"
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$vSerializationJson")
    }


    @Suppress("SpellCheckingInspection")
    /* SquareUp.leakcanary */
    dependencies {
        val v = "2.9.1"
        debugImplementation("com.squareup.leakcanary:leakcanary-android:$v")
    }

    /* Unit Test */
    dependencies {
        val v = "4.13.2"

        // JUnit
        testImplementation("junit:junit:$v")
    }

    /* wasabeef.transformers*/
    dependencies {
        val vCoil = "1.0.6"
        implementation("jp.wasabeef.transformers:coil:$vCoil")
        implementation("jp.wasabeef.transformers:coil-gpu:$vCoil")
    }
}

fun getProp(file: File): Properties {
    require(file.exists()) {
        "couldn't find $file, make sure File is correct"
    }
    val prop = Properties()
    prop.load(FileInputStream(file))
    return prop
}

fun getRootProp(propName: String): Properties {
    val propFile = rootProject.file(propName)
    require(propFile.exists()) {
        "couldn't find $propName in $rootProject"
    }
    return getProp(propFile)
}

fun getLocalProp(): Properties = getRootProp("local.properties")
fun getKeystoreProp(): Properties = getRootProp("keystore.properties")