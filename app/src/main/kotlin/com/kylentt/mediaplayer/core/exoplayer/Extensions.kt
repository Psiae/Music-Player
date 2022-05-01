package com.kylentt.mediaplayer.core.exoplayer

import androidx.media3.common.Player

/**
 * Extension Functions for [Player] Interface related property
 * @author Kylentt
 * @since 2022/04/30
 * @see [PlayerHelper]
 * @see [PlayerConstants]
 */

object PlayerExtension {

  /**
   * Extension Function for [Player.State]
   * @param [] Int Representation of [Player.State]
   * @return true if @param == referenced [Player.State]
   */
  @JvmStatic
  fun @Player.State Int.isStateIdle(): Boolean = this == Player.STATE_IDLE
  @JvmStatic
  fun @Player.State Int.isStateBuffering(): Boolean = this == Player.STATE_BUFFERING
  @JvmStatic
  fun @Player.State Int.isStateReady(): Boolean = this == Player.STATE_READY
  @JvmStatic
  fun @Player.State Int.isStateEnded(): Boolean = this == Player.STATE_ENDED

  /**
   * Extension Function for [Player.RepeatMode]
   * @param [] Int Representation of [Player.RepeatMode]
   * @return true if @param == referenced [Player.RepeatMode]
   */
  @JvmStatic
  fun @Player.RepeatMode Int.isRepeatOff(): Boolean = this == Player.REPEAT_MODE_OFF
  @JvmStatic
  fun @Player.RepeatMode Int.isRepeatOne(): Boolean = this == Player.REPEAT_MODE_ONE
  @JvmStatic
  fun @Player.RepeatMode Int.isRepeatAll(): Boolean = this == Player.REPEAT_MODE_ALL
}
