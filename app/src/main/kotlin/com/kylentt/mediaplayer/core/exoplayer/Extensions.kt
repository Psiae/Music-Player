package com.kylentt.mediaplayer.core.exoplayer

import androidx.media3.common.Player

/**
 * Extension Functions for [Player] Interface related property
 * @see [PlayerHelper]
 * @see [PlayerConstants]
 * @author Kylentt
 * @since 2022/04/30
 */

// TODO: Consider Removing Extension on Primitives Type

object PlayerExtension {

  private const val STATE_IDLE_STRING = "STATE_IDLE"
  private const val STATE_BUFFERING_STRING = "STATE_BUFFERING"
  private const val STATE_READY_STRING = "STATE_READY"
  private const val STATE_ENDED_STRING = "STATE_ENDED"

  private const val REPEAT_OFF_STRING = "REPEAT_OFF"
  private const val REPEAT_ONE_STRING = "REPEAT_ONE"
  private const val REPEAT_ALL_STRING = "REPEAT_ALL"

  @JvmStatic fun @Player.State Int.isStateIdle(): Boolean = this == Player.STATE_IDLE
  @JvmStatic fun @Player.State Int.isStateBuffering(): Boolean = this == Player.STATE_BUFFERING
  @JvmStatic fun @Player.State Int.isStateReady(): Boolean = this == Player.STATE_READY
  @JvmStatic fun @Player.State Int.isStateEnded(): Boolean = this == Player.STATE_ENDED
  @JvmStatic fun @Player.State Int.isOngoing(): Boolean = isStateReady() || isStateBuffering()
  @JvmStatic fun @Player.RepeatMode Int.isRepeatOff(): Boolean = this == Player.REPEAT_MODE_OFF
  @JvmStatic fun @Player.RepeatMode Int.isRepeatOne(): Boolean = this == Player.REPEAT_MODE_ONE
  @JvmStatic fun @Player.RepeatMode Int.isRepeatAll(): Boolean = this == Player.REPEAT_MODE_ALL

  /**
   * for Debugging purposes
   * @param [state] IntDef of [Player.State]
   * @return [String] representation of @param [state]
   * @throws [IllegalArgumentException] if @param [state] is not [Player.State]
   */

  @JvmStatic
  fun toPlayerStateString(@Player.State state: Int): String {
    return when (state) {
      Player.STATE_IDLE -> STATE_IDLE_STRING
      Player.STATE_BUFFERING -> STATE_BUFFERING_STRING
      Player.STATE_READY -> STATE_READY_STRING
      Player.STATE_ENDED -> STATE_ENDED_STRING
      else -> throw IllegalArgumentException("Invalid Player.State $state")
    }
  }

  /**
   * for Debugging purposes
   * @param [mode] IntDef of [Player.RepeatMode]
   * @return [String] representation of @param [mode]
   * @throws [IllegalArgumentException] if @param [mode] is not [Player.RepeatMode]
   */

  @JvmStatic
  fun toPlayerRepeatModeString(@Player.RepeatMode mode: Int): String {
    return when (mode) {
      Player.REPEAT_MODE_OFF -> REPEAT_OFF_STRING
      Player.REPEAT_MODE_ONE -> REPEAT_ONE_STRING
      Player.REPEAT_MODE_ALL -> REPEAT_ALL_STRING
      else -> throw IllegalArgumentException("Invalid Player.RepeatMode $mode")
    }
  }
}
