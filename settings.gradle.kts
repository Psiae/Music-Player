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
include(":base")
include(":base:compose")
include(":base:lifecycle")
include(":base:media")
include(":base:media:medialib")
include(":base:media:medialib:lint")
include(":common")
include(":common:android")
include(":common:android:kotlin")
include(":common:androidx")
include(":common:androidx:viewmodel")
include(":common:androidx:viewmodel:compose")
include(":common:androidx:lifecycle")
include(":common:kotlin")
include(":feature:library")
include(":feature:user")
include(":feature:home")
include(":feature:search")
