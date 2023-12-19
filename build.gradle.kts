// Top-level build file where you can add configuration options common to all sub-projectsmodules.
buildscript {
    val vKotlin = "1.9.0"
    val vHilt = "2.48"
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
    id("com.google.devtools.ksp") version "1.9.0-1.0.13" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.0" apply false
    kotlin("kapt") version "1.9.0"
}

apply(plugin = "kotlinx-atomicfu")

task<Delete>("clean") {
    delete(rootProject.buildDir)
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            if (project.findProperty("composeCompilerReports") == "true") {
                freeCompilerArgs += listOf(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${project.buildDir.absolutePath}/compose_compiler"
                )
            }
            if (project.findProperty("composeCompilerMetrics") == "true") {
                freeCompilerArgs += listOf(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${project.buildDir.absolutePath}/compose_compiler"
                )
            }
        }
    }
}