plugins {
    id("java-library")
    kotlin("jvm") version "1.9.0"
}

repositories{
    mavenCentral()
    google()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}