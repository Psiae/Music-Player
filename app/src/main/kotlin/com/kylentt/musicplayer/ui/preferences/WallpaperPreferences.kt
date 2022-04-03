package com.kylentt.musicplayer.ui.preferences

import kotlinx.serialization.Serializable

@Serializable
sealed class WallpaperPreferences(
    val wallpaperSource: Source
) {

    @Serializable
    object Defaults {
        fun defaultWallpaper() = NormalWallpaper(Source.DEFAULT)
        fun defaultDeviceWallpaper() = NormalWallpaper(Source.DEVICE_WALLPAPER)
        fun defaultMediaItemWallpaper() = NormalWallpaper(Source.MEDIA_ITEM)

        fun defaultNavWallpaper() = NavWallpaper(Source.DEFAULT)
        fun defaultNavDeviceWallpaper() = NavWallpaper(Source.DEVICE_WALLPAPER)
        fun defaultNavMediaItemWallpaper() = NavWallpaper(Source.MEDIA_ITEM)
    }

    @Serializable
    data class NavWallpaper(val source: Source) : WallpaperPreferences(source)
    @Serializable
    data class NormalWallpaper(val source: Source) : WallpaperPreferences(source)

    @Serializable
    sealed class Source {

        @Serializable
        object MEDIA_ITEM : Source() {}

        @Serializable
        object DEVICE_WALLPAPER : Source() {}

        @Serializable
        object DEFAULT : Source()

    }
}