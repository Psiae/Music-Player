plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.dexsr.klio.navigation.compose"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
        composeOptions.kotlinCompilerExtensionVersion = ComposeVersion.kotlinCompilerExtension
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(libs.androidx.core.core.ktx)
    implementation(libs.androidx.compose.foundation.foundation.asProvider())
    implementation(libs.androidx.compose.foundation.foundation.layout)
    implementation(libs.androidx.compose.ui.ui.asProvider())
    implementation(libs.androidx.compose.ui.ui.util)
    implementation(libs.androidx.compose.material.material)
    implementation(libs.androidx.compose.material3.material3)
}