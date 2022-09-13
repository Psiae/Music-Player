package com.flammky.android.medialib.temp.player

import androidx.media3.common.AudioAttributes
import com.flammky.android.medialib.temp.player.options.FallbackInfo
import com.flammky.android.medialib.temp.player.playback.PlaybackControlInfo

class PlayerContextInfo(
	val audioAttributes: AudioAttributes,
	val handleAudioReroute: Boolean,
	val fallbackInfo: FallbackInfo,
	val playbackControlInfo: PlaybackControlInfo
) {
}
