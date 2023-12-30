import org.gradle.api.file.DuplicatesStrategy

// Top-level build file where you can add configuration options common to all sub-projectsmodules.
buildscript {
    val vKotlin = "1.9.0"
    val vHilt = "2.48"
    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.19.0")
        classpath("org.jetbrains.kotlin:atomicfu:$vKotlin")
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
    // TODO: figure out why app:dexBuilderDebug fails on AGP 8.0 when depend on Java module
    id("com.android.application") version "7.4.0" apply false
    id("com.android.library") version "7.4.0" apply false
    id("com.google.devtools.ksp") version "1.9.0-1.0.13" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.0" apply false
    kotlin("kapt") version "1.9.0"

    id("io.realm.kotlin") version "1.13.0" apply false
}

allprojects {
    apply(plugin = "kotlinx-atomicfu")
}

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

subprojects {
    tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
        // TODO
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}