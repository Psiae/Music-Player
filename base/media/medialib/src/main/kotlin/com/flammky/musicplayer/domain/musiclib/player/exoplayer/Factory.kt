package com.flammky.musicplayer.domain.musiclib.player.exoplayer

import android.content.Context
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.upstream.DefaultAllocator
import com.flammky.musicplayer.domain.musiclib.player.exoplayer.stream.LibLoadControl
import timber.log.Timber
import java.util.concurrent.TimeUnit

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
			setLoadControl(LibLoadControl())
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
