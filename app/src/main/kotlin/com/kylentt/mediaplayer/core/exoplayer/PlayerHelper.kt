package com.kylentt.mediaplayer.core.exoplayer

import com.kylentt.mediaplayer.core.exoplayer.PlayerConstants.MAX_VOLUME
import com.kylentt.mediaplayer.core.exoplayer.PlayerConstants.MIN_VOLUME

object PlayerConstants {
  const val MAX_VOLUME = 1F
  const val MIN_VOLUME = 0F
}

object PlayerHelper {
  fun fixVolumeToRange(vol: Float): Float {
    return when {
      vol < MIN_VOLUME -> MIN_VOLUME
      vol > MAX_VOLUME -> MAX_VOLUME
      else -> vol
    }
  }
}
