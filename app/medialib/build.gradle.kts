import java.util.Properties
import java.io.File
import java.io.FileInputStream

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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    implementation(project("lint"))

    implementation("androidx.core:core-ktx:1.9.0-alpha01")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.android.material:material:1.6.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    /* Androidx.media3 */
    dependencies {
        val v = "1.0.0-beta02"
        implementation("androidx.media3:media3-exoplayer:$v")
        implementation("androidx.media3:media3-session:$v")
        implementation("androidx.media3:media3-common:$v")
        implementation("androidx.media3:media3-ui:$v")
    }

    /* Google.guava */
    dependencies {
        val v = "31.1-android"
        implementation("com.google.guava:guava:$v")
    }

    @Suppress("SpellCheckingInspection")
    /* JakeWharton.timber */
    dependencies {
        val v = "5.0.1"
        implementation("com.jakewharton.timber:timber:$v")
    }

    dependencies {
        val lintVersion = "30.4.0-alpha02"
        compileOnly("com.android.tools.lint:lint-api:$lintVersion")
        compileOnly("com.android.tools.lint:lint-checks:$lintVersion")
    }

    lintChecks(project("lint"))
    lintPublish(project("lint"))
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