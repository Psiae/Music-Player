plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
}

android {
    namespace = "com.flammky.musicplayer.base.media"
    compileSdk = 32

    defaultConfig {
        minSdk = 24
        targetSdk = 32
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
    api(project(":core"))
    api(project(":base"))
    api(project(":base:common"))
    api(project(":base:common:android"))
    api(project(":base:common:kotlin"))
    api(project("medialib"))


    // temporary
    api(project("medialib"))

    /* google.dagger */
    dependencies {

        // Hilt-Android
        val vHiltAndroid = "2.44"
        implementation("com.google.dagger:hilt-android:$vHiltAndroid")
        kapt("com.google.dagger:hilt-android-compiler:$vHiltAndroid")
    }
}