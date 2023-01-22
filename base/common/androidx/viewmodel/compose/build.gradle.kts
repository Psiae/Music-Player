plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.flammky.androidx.viewmodel.compose"
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

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.0"
    }
}

dependencies {

    api(project(":base:common:androidx:viewmodel"))

    /* Androidx.compose */
    dependencies {
        val v = "1.3.0-alpha01"

        // Core UI
        implementation("androidx.compose.ui:ui:$v")

        // Foundation
        implementation("androidx.compose.foundation:foundation:$v")
    }

    /* Androidx.lifecycle */
    dependencies {
        val v = "2.6.0-alpha01"
        api("androidx.lifecycle:lifecycle-viewmodel-compose:$v")
    }
}