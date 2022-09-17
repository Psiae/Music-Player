package com.flammky.android.medialib.temp.media3.internal.mediacontroller

import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.flammky.android.medialib.temp.player.LibraryPlayer
import com.flammky.android.medialib.temp.player.LibraryPlayer.PlaybackState.Companion.asPlaybackState
import com.flammky.android.medialib.temp.player.PlayerContext
import com.flammky.android.medialib.temp.player.PlayerContextInfo
import com.flammky.android.medialib.temp.player.component.VolumeManager
import com.flammky.android.medialib.temp.player.event.LibraryPlayerEventListener
import com.flammky.android.medialib.temp.player.event.LibraryPlayerEventListener.Companion.asPlayerListener
import com.flammky.android.medialib.temp.player.options.FallbackInfo
import com.flammky.android.medialib.temp.player.playback.RepeatMode
import com.flammky.android.medialib.temp.player.playback.RepeatMode.Companion.asRepeatMode
import com.flammky.android.medialib.temp.util.addListener
import com.flammky.common.kotlin.comparable.clamp
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import kotlin.math.min

class MediaControllerWrapper internal constructor(
	private val playerContext: PlayerContext,
	private val wrapped: WrappedMediaController = WrappedMediaController(playerContext)
) : LibraryPlayer by wrapped {

	fun connect(
		onError: () -> Unit,
		onConnected: () -> Unit
	) = wrapped.connectMediaController(onConnected = onConnected, onError = onError)

	fun isConnected(): Boolean = wrapped.isStateConnected()

	internal class WrappedMediaController(private val playerContext: PlayerContext) : LibraryPlayer {
		private val handler = Handler(playerContext.looper)

		private var future: ListenableFuture<MediaController>? = null
		private var _mediaController: MediaController? = null

		private var state: WrappedState = WrappedState.NOTHING

		private val executor: Executor = Executor { runnable -> runnable.run() }

		private val listeners: MutableList<Pair<LibraryPlayerEventListener, Player.Listener>> =
			mutableListOf()

		private val mediaController: MediaController
			get() = _mediaController!!

		private val fallbackInfo: FallbackInfo
			get() = playerContext.fallbackInfo

		fun connectMediaController(
			connectionHint: Bundle = Bundle(),
			onConnected: () -> Unit = {},
			onError: () -> Unit = {}
		) {
			if (isStateConnected() && mediaController.isConnected) {
				return onConnected()
			}

			if (isStateConnecting()) {
				return future!!.addListener(executor) { if (it.isCancelled) onError() else onConnected() }
			}

			state = WrappedState.CONNECTING

			val componentName = ComponentName(playerContext.android, playerContext.libraryContext.serviceClass)
			val sessionToken = SessionToken(playerContext.android, componentName)

			try {
				MediaController.Builder(playerContext.android, sessionToken)
					.setApplicationLooper(playerContext.looper)
					.setConnectionHints(connectionHint)
					.buildAsync()
					.addListener(executor) {
						if (it.isCancelled) {
							future = null
							state = WrappedState.ERROR(IllegalStateException())
						} else {
							_mediaController = it.get()
							listeners.forEach { mediaController.addListener(it.second) }
							state = WrappedState.CONNECTED
							onConnected()
						}
					}
			} catch (e: Exception) {
				future = null
				state = WrappedState.ERROR(e)
				onError()
			}
		}

		fun isStateConnected(): Boolean = state == WrappedState.CONNECTED
		fun isStateConnecting(): Boolean = state == WrappedState.CONNECTING

		private fun <R> maybeControllerValue(defaultValue: R, get: (MediaController) -> R): R {
			return if (!isStateConnected()) defaultValue else get(mediaController)
		}

		override val availableCommands: Player.Commands
			get() = maybeControllerValue(fallbackInfo.noAvailableCommands) { it.availableCommands }

		override val playbackParameters: PlaybackParameters
			get() = maybeControllerValue(fallbackInfo.noPlaybackParameters) { it.playbackParameters }

		override val playWhenReady: Boolean
			get() = maybeControllerValue(false) { it.playWhenReady }

		override val playbackState: LibraryPlayer.PlaybackState
			get() = maybeControllerValue(fallbackInfo.noPlaybackState) { it.playbackState.asPlaybackState }

		override val repeatMode: RepeatMode
			get() = maybeControllerValue(fallbackInfo.noRepeatMode) { it.repeatMode.asRepeatMode }

		override val isLoading: Boolean
			get() = maybeControllerValue(false) { it.isLoading}

		override val isPlaying: Boolean
			get() = maybeControllerValue(false) { it.isPlaying }

		override val currentPeriod: Timeline.Period?
			get() = maybeControllerValue(null) {
				it.currentPeriodIndex.takeIf { i -> i > 0 }
					?.let { i -> it.currentTimeline.getPeriod(i, Timeline.Period()) }
			}

		override val currentPeriodIndex: Int
			get() = maybeControllerValue(fallbackInfo.noIndex) { it.currentPeriodIndex }

		override val timeLine: Timeline
			get() = maybeControllerValue(fallbackInfo.noTimeline) { it.currentTimeline }

		override val mediaItemCount: Int
			get() = maybeControllerValue(fallbackInfo.noIndex) { it.mediaItemCount }

		override val currentMediaItem: MediaItem?
			get() = maybeControllerValue(fallbackInfo.noMediaItem) { it.currentMediaItem }

		override val currentMediaItemIndex: Int
			get() = maybeControllerValue(fallbackInfo.noIndex) { it.currentMediaItemIndex }

		override val nextMediaItemIndex: Int
			get() = maybeControllerValue(fallbackInfo.noIndex) { it.nextMediaItemIndex }

		override val previousMediaItemIndex: Int
			get() = maybeControllerValue(fallbackInfo.noIndex) { it.previousMediaItemIndex }

		override val positionMs: Long
			get() = maybeControllerValue(fallbackInfo.noPositionMs) { it.currentPosition }

		override val bufferedPositionMs: Long
			get() = maybeControllerValue(fallbackInfo.noPositionMs) { it.bufferedPosition }

		override val bufferedDurationMs: Long
			get() = maybeControllerValue(fallbackInfo.noDurationMs) { it.totalBufferedDuration }

		override val durationMs: Long
			get() = maybeControllerValue(fallbackInfo.noDurationMs) { it.duration }

		override val contextInfo: PlayerContextInfo
			get() = playerContext.run {
				PlayerContextInfo(
					audioAttributes = audioAttributes,
					handleAudioReroute = handleAudioReroute.first,
					fallbackInfo = fallbackInfo,
					playbackControlInfo = playbackControlInfo
				)
			}


		override val volumeManager: VolumeManager = object : VolumeManager() {

			override var internalVolume: Float
				get() = maybeControllerValue(fallbackInfo.noIndex.toFloat()) { it.volume }
				set(value) { if (isStateConnected()) mediaController.volume = value.clamp(0f, 1f)  }

			override var deviceVolume: Int
				get() = maybeControllerValue(fallbackInfo.noIndex) { it.deviceVolume }
				set(value) { if (isStateConnected()) mediaController.deviceVolume = value.clamp(0, 1)  }

			override var deviceMuted: Boolean
				get() = maybeControllerValue(false) { it.isDeviceMuted }
				set(value) { if (isStateConnected()) mediaController.isDeviceMuted = value }
		}

		override val released: Boolean
			get() = maybeControllerValue(false) { !it.isConnected }

		override val seekable: Boolean
			get() = maybeControllerValue(false) { it.isCurrentMediaItemSeekable }

		override fun seekToDefaultPosition() {
			if (isStateConnected()) mediaController.seekToDefaultPosition()
		}

		override fun seekToDefaultPosition(index: Int) {
			if (isStateConnected()) mediaController.seekToDefaultPosition(index)
		}

		override fun seekToPosition(position: Long) {
			if (isStateConnected()) mediaController.seekTo(position)
		}

		override fun seekToMediaItem(index: Int, startPosition: Long) {
			if (isStateConnected()) mediaController.seekTo(index, startPosition)
		}

		override fun seekToPrevious() {
			if (isStateConnected()) mediaController.seekToPrevious()
		}

		override fun seekToNext() {
			if (isStateConnected()) mediaController.seekToNext()
		}

		override fun seekToPreviousMediaItem() {
			if (isStateConnected()) mediaController.seekToPreviousMediaItem()
		}

		override fun seekToNextMediaItem() {
			if (isStateConnected()) mediaController.seekToPreviousMediaItem()
		}

		override fun prepare() {
			if (isStateConnected()) mediaController.prepare()
		}

		override fun play() {
			if (isStateConnected()) mediaController.play()
		}

		override fun play(item: MediaItem) {
			if (isStateConnected()) {
				if (Looper.myLooper() != playerContext.looper) {
					handler.postAtFrontOfQueue { play(item) }
					return
				}
				val mc = mediaController
				if (mc.currentMediaItem?.mediaId != item.mediaId) mc.setMediaItem(item)
				if (mc.playbackState == Player.STATE_ENDED) mc.seekToDefaultPosition()
				mc.prepare()
				mc.play()
			}
		}

		override fun pause() {
			if (isStateConnected()) mediaController.pause()
		}

		override fun stop() {
			if (isStateConnected()) mediaController.stop()
		}

		override fun removeMediaItem(index: Int) {
			if (isStateConnected()) {
				mediaController.removeMediaItem(index)
			}
		}

		override fun removeMediaItem(item: MediaItem) {
			if (isStateConnected()) {
				removeMediaItems(listOf(item))
			}
		}

		override fun removeMediaItems(items: List<MediaItem>) {
			if (isStateConnected()) {
				val currentItems = getAllMediaItems()
				items.forEach {
					val index = currentItems.indexOf(it)
					if (index != -1) removeMediaItem(index)
				}
			}
		}

		override fun setMediaItems(items: List<MediaItem>) {
			if (isStateConnected()) mediaController.setMediaItems(items)
		}

		override fun addListener(listener: LibraryPlayerEventListener) {
			val m = listener.asPlayerListener(this)
			listeners.add(listener to m)
			if (isStateConnected()) mediaController.addListener(m)
		}

		override fun removeListener(listener: LibraryPlayerEventListener) {
			val get = listeners.filter { it.first === listener }
			listeners.removeAll { it.first === listener }

			if (isStateConnected()) {
				get.forEach { mediaController.removeListener(it.second) }
			}
		}

		override fun release() {
			if (isStateConnected()) mediaController.pause()
		}

		override fun getMediaItemAt(index: Int): MediaItem {
			if (index < 0 || !isStateConnected()) throw IndexOutOfBoundsException()
			return mediaController.getMediaItemAt(index)
		}

		override fun getAllMediaItems(limit: Int): List<MediaItem> {
			val holder: MutableList<MediaItem> = mutableListOf()

			if (isStateConnected()) {
				repeat(min(limit, mediaItemCount)) { i ->
					holder.add(mediaController.getMediaItemAt(i))
				}
			}

			return holder
		}
	}

	private sealed class WrappedState {
		object NOTHING : WrappedState()
		object CONNECTING : WrappedState()
		object CONNECTED : WrappedState()
		data class ERROR(val ex: Exception) : WrappedState()
	}
}
