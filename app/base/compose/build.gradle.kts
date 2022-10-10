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
    api(project(":common:androidx:viewmodel:compose"))

    dependencies {
        val v = "2.2.2"
        api("io.coil-kt:coil-compose:$v")
    }
}