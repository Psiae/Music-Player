plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlinx-atomicfu")
}

android {
    namespace = "dev.dexsr.klio.core"
    compileSdk = 33

    defaultConfig {
        minSdk = 24
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            isMinifyEnabled = false
            resValue(type = "string", name = "app_name", "MusicPlayer_Debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.7.1")
    api(project(":core:core-jvm"))

    //
    // Debug
    //

    @Suppress("SpellCheckingInspection")
    /* jakeWharton.timber */
    dependencies {
        val v = "5.0.1"
        api("com.jakewharton.timber:timber:$v")
    }

    //
    // Misc
    //
    /* androidx.startup */
    dependencies {
        val v = "1.1.1"
        api("androidx.startup:startup-runtime:$v")
    }

    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    api("org.jetbrains.kotlinx:atomicfu:0.19.0")
}