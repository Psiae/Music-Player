package com.kylentt.mediaplayer.core.exoplayer

import com.kylentt.mediaplayer.core.exoplayer.PlayerConstants.MAX_VOLUME
import com.kylentt.mediaplayer.core.exoplayer.PlayerConstants.MIN_VOLUME
import androidx.media3.common.Player

/**
 * [Player] Interface Property Constants
 * @see [PlayerConstants]
 * @see [PlayerHelper]
 * @author Kylentt
 * @since 2022/04/30
 */

object PlayerConstants {
  const val MAX_VOLUME = 1F
  const val MIN_VOLUME = 0F
}

/**
 * [Player] Interface Property Helper Function
 * @see [PlayerConstants]
 * @see [PlayerExtension]
 * @author Kylentt
 * @since 2022/04/30
 */

object PlayerHelper {

  /**
   * @param [vol] the requested Volume as [Float]
   * @return [MIN_VOLUME] if [vol] is under [MIN_VOLUME]
   * @return [MAX_VOLUME] if [vol] is over [MAX_VOLUME]
   * otherwise @return [vol]
   */

  @JvmStatic fun fixVolumeToRange(vol: Float): Float {
    return when {
      vol < MIN_VOLUME -> MIN_VOLUME
      vol > MAX_VOLUME -> MAX_VOLUME
      else -> vol
    }
  }
}
