package com.flammky.android.medialib.temp.media3.internal.mediacontroller

import android.content.ComponentName
import android.os.*
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.flammky.android.medialib.common.mediaitem.RealMediaItem
import com.flammky.android.medialib.media3.Media3Item
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
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.locks.LockSupport
import kotlin.math.min
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class MediaControllerWrapper internal constructor(
	private val playerContext: PlayerContext,
	private val wrapped: WrappedMediaController = WrappedMediaController(playerContext)
) : ThreadLockedPlayer<MediaControllerWrapper> {

	private val handlerThread = object : HandlerThread("WrapperMediaController") {
		init { start() }
	}
	private val looperDispatcher = Handler(handlerThread.looper).asCoroutineDispatcher()

	private val looperImmediateScope = CoroutineScope(
		context = looperDispatcher.immediate + SupervisorJob()
	)

	override val publicLooper: Looper = handlerThread.looper

	override val availableCommands: Player.Commands
		get() = joinBlocking { wrapped.availableCommands }

	override val playbackParameters: PlaybackParameters
		get() = joinBlocking { wrapped.playbackParameters }

	override val playWhenReady: Boolean
		get() = joinBlocking { wrapped.playWhenReady }

	override val playbackState: LibraryPlayer.PlaybackState
		get() = joinBlocking { wrapped.playbackState }

	override val repeatMode: RepeatMode
		get() = joinBlocking { wrapped.repeatMode }

	override val isLoading: Boolean
		get() = joinBlocking { wrapped.isLoading }

	override val isPlaying: Boolean
		get() = joinBlocking { wrapped.isPlaying }

	override val currentPeriod: Timeline.Period?
		get() = joinBlocking { wrapped.currentPeriod }

	override val currentPeriodIndex: Int
		get() = joinBlocking { wrapped.currentPeriodIndex }

	override val timeLine: Timeline
		get() = joinBlocking { wrapped.timeLine }

	override val mediaItemCount: Int
		get() = joinBlocking { wrapped.mediaItemCount }

	override val currentMediaItem: MediaItem?
		get() = joinBlocking { wrapped.currentMediaItem }

	override val currentMediaItemIndex: Int
		get() = joinBlocking { wrapped.currentMediaItemIndex }

	override val nextMediaItemIndex: Int
		get() = joinBlocking { wrapped.nextMediaItemIndex }

	override val previousMediaItemIndex: Int
		get() = joinBlocking { wrapped.previousMediaItemIndex }

	override val positionMs: Long
		get() = joinBlocking { wrapped.positionMs }

	override val bufferedPositionMs: Long
		get() = joinBlocking { wrapped.bufferedPositionMs }

	override val bufferedDurationMs: Long
		get() = joinBlocking { wrapped.bufferedDurationMs }

	override val durationMs: Long
		get() = joinBlocking { wrapped.durationMs }

	override val contextInfo: PlayerContextInfo
		get() = joinBlocking { wrapped.contextInfo }

	override val volumeManager: VolumeManager
		get() = joinBlocking { wrapped.volumeManager }

	override val released: Boolean
		get() = joinBlocking { wrapped.released }

	override val seekable: Boolean
		get() = joinBlocking { wrapped.seekable }

	override fun seekToDefaultPosition() {
		this.post { wrapped.seekToDefaultPosition() }
	}

	override fun seekToDefaultPosition(index: Int) {
		this.post { wrapped.seekToDefaultPosition(index) }
	}

	override fun seekToPosition(position: Long) {
		this.post { wrapped.seekToPosition(position) }
	}

	override fun seekToMediaItem(index: Int, startPosition: Long) {
		this.post { wrapped.seekToMediaItem(index, startPosition) }
	}

	override fun seekToPrevious() {
		this.post { wrapped.seekToPrevious() }
	}

	override fun seekToNext() {
		this.post { wrapped.seekToNext() }
	}

	override fun seekToPreviousMediaItem() {
		this.post { wrapped.seekToPreviousMediaItem() }
	}

	override fun seekToNextMediaItem() {
		this.post { wrapped.seekToNextMediaItem() }
	}

	override fun removeMediaItem(item: MediaItem) {
		this.post { wrapped.removeMediaItem(item) }
	}

	override fun removeMediaItems(items: List<MediaItem>) {
		this.post { wrapped.removeMediaItems(items) }
	}

	override fun removeMediaItem(index: Int) {
		this.post { wrapped.removeMediaItem(index) }
	}

	override fun setMediaItems(items: List<com.flammky.android.medialib.common.mediaitem.MediaItem>) {
		this.post { wrapped.setMediaItems(items) }
	}

	override fun play() {
		this.post { wrapped.play() }
	}

	override fun play(item: MediaItem) {
		this.post { wrapped.play(item) }
	}

	override fun play(item: com.flammky.android.medialib.common.mediaitem.MediaItem) {
		this.post { wrapped.play(item) }
	}

	override fun pause() {
		this.post { wrapped.pause() }
	}

	override fun prepare() {
		this.post { wrapped.prepare() }
	}

	override fun stop() {
		this.post { wrapped.stop() }
	}

	override fun addListener(listener: LibraryPlayerEventListener) {
		this.post { wrapped.addListener(listener) }
	}

	override fun removeListener(listener: LibraryPlayerEventListener) {
		this.post { wrapped.removeListener(listener) }
	}

	override fun release() {
		this.post { wrapped.release() }
	}

	override fun getMediaItemAt(index: Int): MediaItem {
		return joinBlocking { wrapped.getMediaItemAt(index) }
	}

	override fun getAllMediaItems(limit: Int): List<MediaItem> {
		return joinBlocking { wrapped.getAllMediaItems(limit) }
	}

	fun connect(
		onError: () -> Unit,
		onConnected: () -> Unit
	): Unit {
		looperImmediateScope.launch {
			wrapped.connectMediaController(onError = onError, onConnected = onConnected)
		}
	}

	fun isConnected(): Boolean = joinBlocking { wrapped.isStateConnected() }

	override fun post(block: MediaControllerWrapper.() -> Unit) {
		if (joined()) {
			block()
		} else {
			looperImmediateScope.launch { block() }
		}
	}

	override fun <R> postListen(block: MediaControllerWrapper.() -> R, listener: (R) -> Unit) {
		if (joined()) {
			block()
		} else {
			looperImmediateScope.launch { listener(block()) }
		}
	}

	@OptIn(ExperimentalTime::class)
	override fun <R> joinBlockingSuspend(block: suspend MediaControllerWrapper.() -> R): R {
		return if (joined()) {
			measureTimedValue {
				var r: Any? = Unit
				looperImmediateScope.launch { r = block() }
				r as R
			}.apply {
				Timber.d("joinBlockingSuspend in Sync took ${duration.inWholeNanoseconds}ns")
			}.value
		} else {
			measureTimedValue {
				runBlocking(looperDispatcher.immediate) {
					block()
				}
			}.apply {
				Timber.d("joinBlockingSuspend runBlocking took ${duration.inWholeNanoseconds}ns")
			}.value
		}



		return measureTimedValue {
			var r: Any? = Unit
			looperImmediateScope.launch {
				delay(100)
				r = block()
			}
			r as R
		}.apply {
			Timber.d("joinBlockingSuspend in Sync took ${duration.inWholeNanoseconds}ns")
		}.value
	}

	@OptIn(ExperimentalTime::class)
	override suspend fun <R> joinSuspending(block: suspend MediaControllerWrapper.() -> R): R {
		return if (joined()) {
			measureTimedValue {
				block()
			}.apply {
				Timber.d("joinSuspend in Sync took ${duration.inWholeNanoseconds}ns")
			}.value
		} else {
			measureTimedValue {
				withContext(looperDispatcher.immediate) { block() }
			}.apply {
				Timber.d("joinSuspend withContext took ${duration.inWholeNanoseconds}ns")
			}.value
		}
	}

	@OptIn(ExperimentalTime::class)
	override fun <R> joinBlocking(block: MediaControllerWrapper.() -> R): R {
		return if (joined()) {
			measureTimedValue {
				block()
			}.apply {
				Timber.d("joinBlocking in Sync took ${duration.inWholeNanoseconds}ns")
			}.value
		} else {
			measureTimedValue {
				val hold = Any()
				val thread = Thread.currentThread()
				var result: Any? = hold

				postListen(block) {
					result = it
					LockSupport.unpark(thread)
				}

				while (result === hold) LockSupport.park(this)

				result as R

				// should we just use runBlocking instead?
				/*runBlocking(publicHandler.asCoroutineDispatcher()) { block() }*/
			}.apply {
				Timber.d("joinBlocking runBlocking took ${duration.inWholeNanoseconds}ns")
			}.value
		}
	}

	/**
	 * check if we are already inside internal looper, to avoid deadlock on call to runBlocking
	 */
	private fun joined(): Boolean = Looper.myLooper() == publicLooper
	internal class WrappedMediaController(private val playerContext: PlayerContext) : LibraryPlayer {

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
					.setApplicationLooper(Looper.myLooper()!!)
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

		override fun play(item: com.flammky.android.medialib.common.mediaitem.MediaItem) {
			if (isStateConnected()) {
				val internal = (item as RealMediaItem).internalItem as Media3Item
				play(internal.item)
			}
		}

		override fun play(item: MediaItem) {
			if (isStateConnected()) {
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
			if (isStateConnected() && items.isNotEmpty()) {
				val currentItems = getAllMediaItems()
				items.forEach {
					val index = currentItems.indexOf(it)
					if (index != -1) removeMediaItem(index)
				}
			}
		}

		override fun setMediaItems(items: List<com.flammky.android.medialib.common.mediaitem.MediaItem>) {
			if (isStateConnected()) mediaController.setMediaItems(
				items.map { ((it as RealMediaItem).internalItem as Media3Item).item }
			)
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
