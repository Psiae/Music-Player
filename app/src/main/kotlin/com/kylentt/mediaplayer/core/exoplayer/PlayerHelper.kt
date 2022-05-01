package com.kylentt.mediaplayer.core.exoplayer

import com.kylentt.mediaplayer.core.exoplayer.PlayerConstants.MAX_VOLUME
import com.kylentt.mediaplayer.core.exoplayer.PlayerConstants.MIN_VOLUME
import androidx.media3.common.Player

/**
 * [Player] Interface Property Constants
 * @author Kylentt
 * @since 2022/04/30
 * @see [PlayerConstants]
 * @see [PlayerHelper]
 */

object PlayerConstants {
  const val MAX_VOLUME = 1F
  const val MIN_VOLUME = 0F
}

/**
 * [Player] Interface Property Helper Function
 * @author Kylentt
 * @since 2022/04/30
 * @see [PlayerConstants]
 * @see [PlayerExtension]
 */

object PlayerHelper {
  @JvmStatic fun fixVolumeToRange(vol: Float): Float {
    return when {
      vol < MIN_VOLUME -> MIN_VOLUME
      vol > MAX_VOLUME -> MAX_VOLUME
      else -> vol
    }
  }
}
