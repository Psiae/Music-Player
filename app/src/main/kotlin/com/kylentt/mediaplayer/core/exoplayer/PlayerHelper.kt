package com.kylentt.mediaplayer.core.exoplayer

import androidx.media3.common.Player

/**
 * [Player] Interface Property Helper Function
 * @see [PlayerConstants]
 * @see [PlayerExtension]
 * @author Kylentt
 * @since 2022/04/30
 */

object PlayerHelper {
  val minVol = PlayerConstants.MIN_VOLUME
  val maxVol = PlayerConstants.MAX_VOLUME

  /**
   * @param [vol] the requested Volume as [Float]
   * @return proper Volume according to [androidx.media3.session.MediaController.setVolume] range
   */

  @JvmStatic fun fixVolumeToRange(vol: Float): Float {
    return when {
      vol < minVol -> minVol
      vol > maxVol -> maxVol
      else -> vol
    }
  }
}
