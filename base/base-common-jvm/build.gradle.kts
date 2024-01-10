import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    // TODO: move to base-auth-jvm
    id("org.jetbrains.kotlin.plugin.serialization")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    api(project(":core:core-jvm"))

    //
    // Kotlin-common
    //

    /* jetbrains.kotlinx */
    dependencies {
        // Collections.Immutable
        val vCollection_Immutable = "0.3.5"
        api("org.jetbrains.kotlinx:kotlinx-collections-immutable:$vCollection_Immutable")

        // coroutines-guava
        val vCoroutines_Guava = "kotlinx-coroutines-guava:1.6.4"
        api("org.jetbrains.kotlinx:$vCoroutines_Guava")

        // Serialization-Json
        val vSerialization_Json = "1.4.0"
        api("org.jetbrains.kotlinx:kotlinx-serialization-json:$vSerialization_Json")
    }

    /* jetbrains.kotlin */
    dependencies {

        // reflect
        val vReflect = "1.9.0"
        api("org.jetbrains.kotlin:kotlin-reflect:$vReflect")
    }
}

tasks.configureEach {
    if (name == "transformAtomicfuClasses") {
        dependsOn(":core:core-jvm:transformAtomicfuClasses")
    }
}