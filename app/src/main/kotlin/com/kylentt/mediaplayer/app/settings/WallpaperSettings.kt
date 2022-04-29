package com.kylentt.mediaplayer.app.settings

import kotlinx.serialization.Serializable

@Serializable
data class WallpaperSettings(
  val mode: Mode,
  val source: Source,
  val sourceALT: Source
) {

  enum class Mode {
    NORMAL,
    NAVIGATION,
  }

  enum class Source {
    DEVICE_WALLPAPER,
    MEDIA_ITEM,
    SYSTEM_MODE
  }

  companion object {
    val DEFAULT = WallpaperSettings(
      mode = Mode.NAVIGATION,
      source = Source.MEDIA_ITEM,
      sourceALT = Source.DEVICE_WALLPAPER
    )
  }
}
