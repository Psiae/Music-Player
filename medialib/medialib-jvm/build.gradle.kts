plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(project(":base:base-common-jvm"))
}

// AGP 8.0

tasks.configureEach {
    if (name == "transformAtomicfuClasses") {
        dependsOn(":base:base-common-jvm:transformAtomicfuClasses")
    }
}
