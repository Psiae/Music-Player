package com.kylentt.mediaplayer.app.settings

import androidx.annotation.IntRange
import kotlinx.serialization.Serializable

@Serializable
data class NavigationSettings(
  val bnvSettings: BottomNavigationSettings
) {

  companion object {
    val DEFAULT = NavigationSettings(
      bnvSettings = BottomNavigationSettings.DEFAULT
    )
  }
}

@Serializable
data class BottomNavigationSettings(
  val itemAnim: ItemAnim,
  val itemOrientation: ItemOrientation,
  @IntRange(from = 0, to = 100) val visibility: Int
) {

  companion object {
    val DEFAULT = BottomNavigationSettings(
      ItemAnim.VISIBILITY,
      ItemOrientation.HORIZONTAL,
      100
    )
  }

  enum class ItemOrientation {
    VERTICAL,
    HORIZONTAL
  }

  enum class ItemAnim {
    NOTHING,
    VISIBILITY
  }

}
