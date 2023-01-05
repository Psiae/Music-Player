plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.flammky.androidx.viewmodel"
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
    api(project(":base:common:androidx"))
    api(project(":base:common:androidx:lifecycle"))

    /* Androidx.lifecycle */
    dependencies {
        val v = "2.6.0-alpha01"
        api("androidx.lifecycle:lifecycle-runtime-ktx:$v")
        api("androidx.lifecycle:lifecycle-viewmodel-ktx:$v")
    }
}