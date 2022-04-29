package com.kylentt.mediaplayer.core.exoplayer

import com.kylentt.mediaplayer.core.exoplayer.Constants.MAX_VOLUME
import com.kylentt.mediaplayer.core.exoplayer.Constants.MIN_VOLUME

object Constants {
  const val MAX_VOLUME = 1F
  const val MIN_VOLUME = 0F
}

object Helper {
  fun fixVolumeToRange(vol: Float): Float {
    if (vol < MIN_VOLUME) return MIN_VOLUME
    if (vol > MAX_VOLUME) return MAX_VOLUME
    return vol
  }
}
