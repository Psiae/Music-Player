plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
    id("kotlin-kapt")
}

android {
    namespace = "com.flammky.musicplayer.base"
    compileSdk = 32
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    api(project(":common:kotlin"))
    api(project(":common:android"))
    api(project(":common:androidx"))

    /* google.dagger */
    dependencies {

        // Hilt-Android
        val vHiltAndroid = "2.43.2"
        implementation("com.google.dagger:hilt-android:$vHiltAndroid")
        kapt("com.google.dagger:hilt-android-compiler:$vHiltAndroid")
    }
}