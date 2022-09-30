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
include(":app:library")
include(":app:medialib")
include(":app:medialib:lint")
include(":common")
include(":common:android")
include(":common:kotlin")
include(":common:android:kotlin")
