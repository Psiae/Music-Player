plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.flammky.musicplayer.base"
    compileSdk = 33

    defaultConfig {
        minSdk = 24
        targetSdk = 33
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    api(project(":core"))
    api(project("common:kotlin"))
    api(project("common:android"))
    api(project("common:androidx"))

    //
    // UI
    //

    /* android.material */
    dependencies {
        val v = "1.7.0"
        api("com.google.android.material:material:$v")
    }

    /* androidx.core */
    dependencies {
        val vKTX = "1.9.0"
        val vSplashScreen = "1.0.0"

        api("androidx.core:core-ktx:$vKTX")
        api("androidx.core:core-splashscreen:$vSplashScreen")
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

    //
    // Kotlin-common
    //

    /* jetbrains.kotlinx */
    dependencies {
        // Collections.Immutable
        val vCollection_Immutable = "0.3.5"
        api("org.jetbrains.kotlinx:kotlinx-collections-immutable:$vCollection_Immutable")

        // coroutines-guava
        val vCoroutines_Guava = "kotlinx-coroutines-guava:1.6.4"
        api("org.jetbrains.kotlinx:$vCoroutines_Guava")

        // Serialization-Json
        val vSerialization_Json = "1.4.0"
        api("org.jetbrains.kotlinx:kotlinx-serialization-json:$vSerialization_Json")
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
    // Concurrency
    //

    /* Androidx.concurrent */
    dependencies {

        // Futures
        val vFutures = "1.1.0"
        api("androidx.concurrent:concurrent-futures:$vFutures")
        api("androidx.concurrent:concurrent-futures-ktx:$vFutures")
    }

    //
    // DI
    //

    /* google.dagger */
    dependencies {

        // Hilt-Android
        val vHiltAndroid = "2.44"
        api("com.google.dagger:hilt-android:$vHiltAndroid")
        kapt("com.google.dagger:hilt-android-compiler:$vHiltAndroid")
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

    //
    // Persistence
    //

    /* androidx.datastore */
    dependencies {
        val v = "1.1.0-alpha01"
        api("androidx.datastore:datastore:$v")
    }

    /* androidx.room */
    dependencies {
        val v = "2.4.3"
        api("androidx.room:room-runtime:$v")
        api("androidx.room:room-ktx:$v")
        api("androidx.room:room-guava:$v")
        ksp("androidx.room:room-compiler:$v")
    }

    /* google.firebase */
    dependencies {
        val vAuth = "21.0.8"
        api("com.google.firebase:firebase-auth-ktx:$vAuth")
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
    // Misc
    //
    /* androidx.startup */
    dependencies {
        val v = "1.1.1"
        api("androidx.startup:startup-runtime:$v")
    }

    /* jetbrains.kotlin */
    dependencies {

        // reflect
        val vReflect = "1.7.10"
        api("org.jetbrains.kotlin:kotlin-reflect:$vReflect")
    }
}

