pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MusicPlayer"
include(":app")
include(":app:base")
include(":app:base:compose")
include(":app:base:media")
include(":app:library")
include(":app:medialib")
include(":app:medialib:lint")
include(":common")
include(":common:android")
include(":common:android:kotlin")
include(":common:androidx")
include(":common:androidx:viewmodel")
include(":common:androidx:viewmodel:compose")
include(":common:androidx:lifecycle")
include(":common:kotlin")
include(":app:media")
include(":app:base:lifecycle")
