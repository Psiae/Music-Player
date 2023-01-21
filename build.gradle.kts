// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    val vKotlin = "1.8.0"
    val vHilt = "2.44"
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:$vHilt")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$vKotlin")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$vKotlin")
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.19.0")
    }
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    id("com.android.application") version "7.3.1" apply false
    id("com.android.library") version "7.3.1" apply false
    id("com.google.devtools.ksp") version "1.8.0-1.0.8" apply false
    id("org.jetbrains.kotlin.android") version "1.8.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.0" apply false
    id("org.jetbrains.kotlin.jvm") version "1.8.0" apply false
}

apply(plugin = "kotlinx-atomicfu")

task<Delete>("clean") {
    delete(rootProject.buildDir)
}