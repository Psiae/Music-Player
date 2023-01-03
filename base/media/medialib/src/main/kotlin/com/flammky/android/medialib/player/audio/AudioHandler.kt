package com.flammky.android.medialib.player.audio

import androidx.media3.common.AudioAttributes

/**
 * Defines how player should handle certain Audio related events
 */
class AudioHandler private constructor(
	val audioAttributes: AudioAttributes,
	val handleAudioReroute: Boolean
) {

	class Builder() {

	}
}
