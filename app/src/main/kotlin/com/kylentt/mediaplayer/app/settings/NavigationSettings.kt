package com.kylentt.mediaplayer.app.settings

import androidx.annotation.FloatRange
import kotlinx.serialization.Serializable

@Serializable
data class NavigationSettings(
  val bnvSettings: BottomNavigationSettings
) {

  companion object {
    val DEFAULT = NavigationSettings(bnvSettings = BottomNavigationSettings.DEFAULT)
  }
}

@Serializable
data class BottomNavigationSettings(
  val itemAnimation: ItemAnimation,
  val itemAlignment: ItemAlignment,
  @FloatRange(from = 0.0, to = 100.0) val visibility: Float
) {

  enum class ItemAlignment {
    VERTICAL,
    HORIZONTAL
  }

  enum class ItemAnimation {
    NOTHING,
    VISIBILITY
  }

  companion object {
    val DEFAULT = BottomNavigationSettings(
      ItemAnimation.VISIBILITY,
      ItemAlignment.VERTICAL,
      100f
    )
  }
}
