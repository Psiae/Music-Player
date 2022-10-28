// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    val vKotlin = "1.7.20"
    val vHilt = "2.44"
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:$vHilt")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$vKotlin")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$vKotlin")
    }
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    id("com.android.application") version "7.3.0" apply false
    id("com.android.library") version "7.3.0" apply false
    id("com.google.devtools.ksp") version "1.7.20-1.0.6" apply false
    id("org.jetbrains.kotlin.android") version "1.7.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.20" apply false
    id("org.jetbrains.kotlin.jvm") version "1.7.20" apply false
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
repositories {
    google()
}
