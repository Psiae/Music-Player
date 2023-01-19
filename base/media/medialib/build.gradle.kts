
import java.io.FileInputStream
import java.util.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.flammky.android.medialib"
    compileSdk = 33

    defaultConfig {
        minSdk = 24
        targetSdk = 33
    }

    signingConfigs {
        create("release") {
            storeFile = file(getKeystoreProp()["FILE_PATH"]!!)
            storePassword = getKeystoreProp()["PASSWORD"]?.toString()
            keyAlias = getKeystoreProp()["KEY_ALIAS"]?.toString()
            keyPassword = getKeystoreProp()["KEY_PASSWORD"]?.toString()
        }
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
}

dependencies {

    // Project Local
    api(project(":base:common:android"))
    api(project(":base:common:androidx"))
    api(project(":base:common:androidx:lifecycle"))
    api(project(":base:common:kotlin"))
    api(project(":base"))

    api("androidx.media:media:1.6.0")

    /* androidx.media3 */
    dependencies {
        val v = "1.0.0-beta03"
        api("androidx.media3:media3-exoplayer:$v")
        api("androidx.media3:media3-session:$v")
        api("androidx.media3:media3-common:$v")
        api("androidx.media3:media3-ui:$v")
    }

    /* androidx.annotation */
    dependencies {
        val v = "1.5.0"
        api("androidx.annotation:annotation:$v")
    }

    /* google.android */
    dependencies {

        // Exoplayer
        val vExoPlayer = "2.18.2"
        api("com.google.android.exoplayer:extension-mediasession:$vExoPlayer")
    }

    /* google.guava */
    dependencies {
        val v = "31.1-android"
        api("com.google.guava:guava:$v")
    }

    @Suppress("SpellCheckingInspection")
    /* JakeWharton.timber */
    dependencies {
        val v = "5.0.1"
        implementation("com.jakewharton.timber:timber:$v")
    }

    /* wasabeef.transformers*/
    dependencies {
        val vCoil = "1.0.6"
        implementation("jp.wasabeef.transformers:coil:$vCoil")
        implementation("jp.wasabeef.transformers:coil-gpu:$vCoil")
    }

    /* google.dagger */
    dependencies {

        // Hilt-Android
        val vHiltAndroid = "2.44"
        implementation("com.google.dagger:hilt-android:$vHiltAndroid")
        kapt("com.google.dagger:hilt-android-compiler:$vHiltAndroid")
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