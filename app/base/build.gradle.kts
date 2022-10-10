plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
}

android {
    namespace = "com.flammky.musicplayer.base"
    compileSdk = 32

    defaultConfig {
        minSdk = 24
        targetSdk = 32
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    api(project(":common:kotlin"))
    api(project(":common:android"))
    api(project(":common:androidx"))
    api(project(":common:androidx:viewmodel"))

    /* coil-kt */
    dependencies {
        val v = "2.2.2"
        api("io.coil-kt:coil:$v")
    }

    /* google.dagger */
    dependencies {

        // Hilt-Android
        val vHiltAndroid = "2.44"
        implementation("com.google.dagger:hilt-android:$vHiltAndroid")
        kapt("com.google.dagger:hilt-android-compiler:$vHiltAndroid")
    }
}