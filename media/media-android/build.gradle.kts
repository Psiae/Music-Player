plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.dexsr.klio.media"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    buildTypes {
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":base:base-common-android"))
    api(project(":media:media-jvm"))
    implementation(libs.androidx.core.core.ktx)
}