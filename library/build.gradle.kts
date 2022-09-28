plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    compileSdk = 32

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }

    dependencies {
        /* Project Local */
        dependencies {
            implementation(project(":app:medialib"))
        }

        /* Androidx.compose */
        dependencies {
            val v = "1.3.0-alpha01"

            // Core UI
            implementation("androidx.compose.ui:ui:$v")
            implementation("androidx.compose.ui:ui-tooling-preview:$v")

            // Foundation
            implementation("androidx.compose.foundation:foundation:$v")

            // Material
            implementation("androidx.compose.material:material:$v")
            implementation("androidx.compose.material:material-icons-core:$v")

            // Material3
            val vMaterial3 = "1.0.0-alpha14"
            implementation("androidx.compose.material3:material3:$vMaterial3")

            // Debug
            debugImplementation("androidx.compose.ui:ui-tooling:$v")
        }

        /* Androidx.core */
        dependencies {
            val v = "1.9.0-alpha01"
            implementation("androidx.core:core-ktx:$v")
        }

        /* Androidx.lifecycle */
        dependencies {
            val v = "2.6.0-alpha01"
            implementation("androidx.lifecycle:lifecycle-runtime-ktx:$v")
            implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$v")
            implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$v")
        }

        /* coil-kt */
        dependencies  {
            val v = "2.2.0"
            implementation("io.coil-kt:coil:$v")
            implementation("io.coil-kt:coil-compose:$v")
        }

        /* Google.dagger */
        dependencies {
            val vHiltAndroid = "2.43.2"
            implementation("com.google.dagger:hilt-android:$vHiltAndroid")
            ksp("com.google.dagger:hilt-android-compiler:$vHiltAndroid")
        }
    }
}