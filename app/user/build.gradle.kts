plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.flammky.musicplayer.user"
    compileSdk = 32

    defaultConfig {
        minSdk = 24
        targetSdk = 32
    }
    buildFeatures {
        compose = true
    }
    buildTypes {
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
    api(project(":app:base:compose"))
    api(project(":app:base:media"))
    api(project(":app:media"))
}