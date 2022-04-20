package com.kylentt.mediaplayer.app.settings

import kotlinx.serialization.Serializable

@Serializable
data class WallpaperSettings(
  val mode: Mode,
  val source: Source,
  val sourceALT: Source
) {

  companion object {
    val DEFAULT = WallpaperSettings(
      mode = Mode.NAVIGATION,
      source = Source.SYSTEM_MODE,
      sourceALT = Source.SYSTEM_MODE
    )
  }

  enum class Mode {
    NORMAL,
    NAVIGATION,
  }

  enum class Source {
    DEVICE_WALLPAPER,
    MEDIA_ITEM,
    SYSTEM_MODE
  }
}
