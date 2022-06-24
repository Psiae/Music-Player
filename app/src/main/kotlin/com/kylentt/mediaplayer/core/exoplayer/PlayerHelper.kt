package com.kylentt.mediaplayer.core.exoplayer

import androidx.media3.common.Player
import com.kylentt.mediaplayer.core.extenstions.clamp

/**
 * [Player] Interface Property Helper Function
 * @see [PlayerConstants]
 * @see [PlayerExtension]
 * @author Kylentt
 * @since 2022/04/30
 */

object PlayerHelper {
  private const val minVol = PlayerConstants.MIN_VOLUME
  private const val maxVol = PlayerConstants.MAX_VOLUME

  /**
   * @param [vol] the requested Volume as [Float]
   * @return proper Volume according to [androidx.media3.session.MediaController.setVolume] range
   */

  @JvmStatic fun fixVolumeToRange(vol: Float): Float = vol.clamp(minVol, maxVol)
}
