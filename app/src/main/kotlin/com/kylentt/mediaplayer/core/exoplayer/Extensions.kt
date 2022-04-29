package com.kylentt.mediaplayer.core.exoplayer

import androidx.media3.common.Player

object PlayerExtension {

  /** Player.State */
  fun @Player.State Int.isStateIdle(): Boolean {
    return this == Player.STATE_IDLE
  }
  fun @Player.State Int.isStateBuffering(): Boolean {
    return this == Player.STATE_BUFFERING
  }
  fun @Player.State Int.isStateReady(): Boolean {
    return this == Player.STATE_READY
  }
  fun @Player.State Int.isStateEnded(): Boolean {
    return this == Player.STATE_ENDED
  }

  /** Player.RepeatMode */
  fun @Player.RepeatMode Int.isRepeatOff(): Boolean {
    return this == Player.REPEAT_MODE_OFF
  }
  fun @Player.RepeatMode Int.isRepeatOne(): Boolean {
    return this == Player.REPEAT_MODE_ONE
  }
  fun @Player.RepeatMode Int.isRepeatAll(): Boolean {
    return this == Player.REPEAT_MODE_ALL
  }
}
