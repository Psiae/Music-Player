plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("io.realm.kotlin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":base:base-common-jvm"))

    api(libs.io.realm.kotlin.library.base)
}