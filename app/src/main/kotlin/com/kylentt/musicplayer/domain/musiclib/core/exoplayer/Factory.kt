package com.kylentt.musicplayer.domain.musiclib.core.exoplayer

import android.content.Context
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer

object ExoPlayerFactory {

	fun newBuilder(context: Context): ExoPlayer.Builder = ExoPlayer.Builder(context)

	fun createMusicExoPlayer(
		context: Context,
		handleAudioFocus: Boolean,
		handleAudioReroute: Boolean,
		looper: Looper = Looper.myLooper()!!
	): ExoPlayer {
		val builder = newBuilder(context)
		return with(builder) {
			setAudioAttributes(getMusicAudioAttributes(), handleAudioFocus)
			setHandleAudioBecomingNoisy(handleAudioReroute)
			setLooper(looper)
			build()
		}
	}

	private fun getMusicAudioAttributes(): AudioAttributes {
		return AudioAttributes.Builder()
			.setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
			.setUsage(C.USAGE_MEDIA)
			.build()
	}
}
