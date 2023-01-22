
import java.io.FileInputStream
import java.util.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "com.flammky.musicplayer"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(getKeystoreProp()["FILE_PATH"]!!)
            storePassword = getKeystoreProp()["PASSWORD"]?.toString()
            keyAlias = getKeystoreProp()["KEY_ALIAS"]?.toString()
            keyPassword = getKeystoreProp()["KEY_PASSWORD"]?.toString()
        }
    }

    buildTypes {

        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            isDebuggable = false
            resValue(type = "string", name = "app_name", "Music Player")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            resValue(type = "string", name = "app_name", "MusicPlayer_Debug")
        }
    }

    buildFeatures {
        compose = true
        composeOptions.kotlinCompilerExtensionVersion = "1.4.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    @Suppress("SpellCheckingInspection")
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        freeCompilerArgs = freeCompilerArgs + "-Xjvm-default=all"
        freeCompilerArgs = freeCompilerArgs + "-Xsam-conversions=class"

    }

    packagingOptions {

    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }

    namespace = "com.flammky.musicplayer"
}

dependencies {

    implementation(project(":core"))
    implementation(project(":base"))
    implementation(project(":base:common"))
    implementation(project(":base:compose"))
    implementation(project(":base:media"))
    implementation(project(":base:compose"))
    implementation(project(":base:lifecycle"))
    implementation(project(":base:media:medialib"))
    implementation(project(":feature:base"))
    implementation(project(":feature:home"))
    implementation(project(":feature:search"))
    implementation(project(":feature:library"))
    implementation(project(":feature:user"))
    implementation(project(":feature:player"))

    /* google.dagger */
    dependencies {

        // Hilt-Android
        val vHiltAndroid = "2.44"
        implementation("com.google.dagger:hilt-android:$vHiltAndroid")
        kapt("com.google.dagger:hilt-android-compiler:$vHiltAndroid")
    }

    /* Google.guava */
    dependencies {
        val v = "31.1-android"
        implementation("com.google.guava:guava:$v")
    }

    /* Instrumentation Test */
    dependencies {

        // JUnit
        val vJUnitExt = "1.1.3"
        val vJUnit4Compose = "1.1.1"
        androidTestImplementation("androidx.test.ext:junit:$vJUnitExt")
        androidTestImplementation("androidx.compose.ui:ui-test-junit4:$vJUnit4Compose")

        // Espresso
        val vEspressoCore = "3.4.0"
        androidTestImplementation("androidx.test.espresso:espresso-core:$vEspressoCore")
    }

    /* Jetbrains.kotlinx */
    dependencies {

        // Collections.Immutable
        val vCollectionImmutable = "0.3.5"
        implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$vCollectionImmutable")

        // Coroutines-Guava
        val vCoroutinesGuava = "1.6.4"
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:$vCoroutinesGuava")

        // Serialization-Json
        val vSerializationJson = "1.4.0"
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$vSerializationJson")
    }


    @Suppress("SpellCheckingInspection")
    /* SquareUp.leakcanary */
    dependencies {
        val v = "2.9.1"
        debugImplementation("com.squareup.leakcanary:leakcanary-android:$v")
    }

    /* Unit Test */
    dependencies {
        val v = "4.13.2"

        // JUnit
        testImplementation("junit:junit:$v")
    }
}

fun getProp(file: File): Properties {
    require(file.exists()) {
        "couldn't find $file, make sure File is correct"
    }
    val prop = Properties()
    prop.load(FileInputStream(file))
    return prop
}

fun getRootProp(propName: String): Properties {
    val propFile = rootProject.file(propName)
    require(propFile.exists()) {
        "couldn't find $propName in $rootProject"
    }
    return getProp(propFile)
}

fun getLocalProp(): Properties = getRootProp("local.properties")
fun getKeystoreProp(): Properties = getRootProp("keystore.properties")