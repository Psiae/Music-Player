
import java.io.FileInputStream
import java.util.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.flammky.android.medialib"
    compileSdk = 32

    defaultConfig {
        minSdk = 24
        targetSdk = 32
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
    api(project(":common:android"))
    api(project(":common:androidx"))
    api(project(":common:androidx:lifecycle"))
    api(project(":common:kotlin"))

    /* androidx.media3 */
    dependencies {
        val v = "1.0.0-beta02"
        implementation("androidx.media3:media3-exoplayer:$v")
        implementation("androidx.media3:media3-session:$v")
        implementation("androidx.media3:media3-common:$v")
        implementation("androidx.media3:media3-ui:$v")
    }

    /* androidx.annotation */
    dependencies {
        val v = "1.5.0"
        api("androidx.annotation:annotation:$v")
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