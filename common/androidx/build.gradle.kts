plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.flammky.androidx"
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
    api("androidx.core:core-ktx:1.9.0-alpha01")
}