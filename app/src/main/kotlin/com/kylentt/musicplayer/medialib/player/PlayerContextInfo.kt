package com.kylentt.musicplayer.medialib.player

import androidx.media3.common.AudioAttributes
import com.kylentt.musicplayer.medialib.player.options.FallbackInfo
import com.kylentt.musicplayer.medialib.player.playback.PlaybackControlInfo

class PlayerContextInfo(
	val audioAttributes: AudioAttributes,
	val handleAudioReroute: Boolean,
	val fallbackInfo: FallbackInfo,
	val playbackControlInfo: PlaybackControlInfo
) {
}
