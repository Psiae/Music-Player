plugins {
    id("java-library")
    kotlin("jvm") version "1.9.0"
}

repositories{
    mavenCentral()
    google()
}

java {
    // AGP 8.0+ require minimum JDK 17
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}