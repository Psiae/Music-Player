package com.flammky.musicplayer.domain.musiclib.player.exoplayer.stream

import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.DefaultAllocator
import java.util.concurrent.TimeUnit

class LibLoadControl : LoadControl {

	// TODO, create adaptive implementation
	private val todoImpl = object : DefaultLoadControl(
		DefaultAllocator(true, 64 * 1024),
		/* minBufferMs = */ TimeUnit.MINUTES.toMillis(5).toInt(),
		/* maxBufferMs = */ TimeUnit.MINUTES.toMillis(5).toInt(),
		/* bufferForPlaybackMs = */
		DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
		/* bufferForPlaybackAfterRebufferMs = */
		DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
		/* targetBufferBytes = */ C.LENGTH_UNSET,
		/* prioritizeTimeOverSizeThresholds = */ false,
		/* backBufferDurationMs = */
		DefaultLoadControl.DEFAULT_BACK_BUFFER_DURATION_MS,
		/* retainBackBufferFromKeyframe = */
		DefaultLoadControl.DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME
	) {

	}

	override fun onPrepared() = todoImpl.onPrepared()

	override fun onTracksSelected(
		renderers: Array<out Renderer>,
		trackGroups: TrackGroupArray,
		trackSelections: Array<out ExoTrackSelection>
	) = todoImpl.onTracksSelected(renderers, trackGroups, trackSelections)

	override fun onStopped() = todoImpl.onStopped()

	override fun onReleased() = todoImpl.onReleased()

	override fun getAllocator(): Allocator = todoImpl.allocator

	override fun getBackBufferDurationUs(): Long = todoImpl.backBufferDurationUs

	override fun retainBackBufferFromKeyframe(): Boolean = todoImpl.retainBackBufferFromKeyframe()

	override fun shouldContinueLoading(
		playbackPositionUs: Long,
		bufferedDurationUs: Long,
		playbackSpeed: Float
	): Boolean = todoImpl.shouldContinueLoading(playbackPositionUs, bufferedDurationUs, playbackSpeed)

	override fun shouldStartPlayback(
		bufferedDurationUs: Long,
		playbackSpeed: Float,
		rebuffering: Boolean,
		targetLiveOffsetUs: Long
	): Boolean = todoImpl.shouldStartPlayback(bufferedDurationUs, playbackSpeed, rebuffering, targetLiveOffsetUs)
}
