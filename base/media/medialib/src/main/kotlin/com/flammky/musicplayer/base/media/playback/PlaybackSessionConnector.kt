package com.flammky.musicplayer.base.media.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.flammky.musicplayer.core.common.atomic
import com.flammky.musicplayer.core.common.sync
import com.flammky.musicplayer.core.common.syncApply
import com.flammky.musicplayer.domain.musiclib.service.MusicLibraryService
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.guava.await
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * a class that represents a playback session
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackSessionConnector internal constructor(
	val id: String,
	private val context: Context,
	private val looper: Looper
) {

	private val observersMap = mutableMapOf<Any, Observer>()

	private val supervisorScope = CoroutineScope(SupervisorJob())
	private val looperDispatcher = Handler(looper).asCoroutineDispatcher()

	private val _lock = Any()

	private var _deferredController: Deferred<MediaController>? = null

	//
	// Considering These are UI info, should they be conflated ?
	// I think they should conflate themselves as necessary
	//
	private val _playWhenReadySharedFlow =
		MutableSharedFlow<Boolean>(1, 64)
	private val _repeatModeSharedFlow =
		MutableSharedFlow<RepeatMode>(1, 64)
	private val _shuffleModeSharedFlow =
		MutableSharedFlow<ShuffleMode>(1, 64)
	private val _queueSharedFlow =
		MutableSharedFlow<OldPlaybackQueue>(1, 64)
	private val _isPlayingSharedFlow =
		MutableSharedFlow<Boolean>(1, 64)
	private val _playbackSpeedSharedFlow =
		MutableSharedFlow<Float>(1, 64)
	private val _discontinuitySharedFlow =
		MutableSharedFlow<PlaybackEvent.PositionDiscontinuity>(1, 64)
	private val _durationSharedFlow =
		MutableSharedFlow<Duration>(1, 64)
	private val _propertiesSharedFlow =
		MutableSharedFlow<PlaybackProperties>(1, 64)

	@Volatile
	private var observerListenerRegistered = false

	@Volatile
	private var shouldRegisterObserverListener = false

	// in Looper
	// if we decided that it should conflated we should queue the dispatch normally
	private val observerListener = object : Player.Listener {

		// test
		private val _mod = atomic(0)

		override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
			val mod = _mod.incrementAndGet()
			supervisorScope.launch(looperDispatcher.immediate) {
				check(mod == _mod.get())
				_playWhenReadySharedFlow.emit(playWhenReady)
				_propertiesSharedFlow.emit(controller.getPlaybackProperties())
			}
		}

		override fun onIsPlayingChanged(isPlaying: Boolean) {
			val mod = _mod.incrementAndGet()
			supervisorScope.launch(looperDispatcher.immediate) {
				check(mod == _mod.get())
				_isPlayingSharedFlow.emit(isPlaying)
			}
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			val mod = _mod.incrementAndGet()
			supervisorScope.launch(looperDispatcher.immediate) {
				check(mod == _mod.get())
				_propertiesSharedFlow.emit(controller.getPlaybackProperties())
			}
		}

		override fun onTimelineChanged(timeline: Timeline, reason: Int) {
			val mod = _mod.incrementAndGet()
			supervisorScope.launch(looperDispatcher.immediate) {
				check(mod == _mod.get())
				when (reason) {
					Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED -> {
						_queueSharedFlow.emit(controller.getQueue())
					}
					Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE -> {
						_durationSharedFlow.emit(controller.getDuration())
					}
					else -> error("unknown TimelineChangeReason=$reason")
				}
			}
		}

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			val mod = _mod.incrementAndGet()
			supervisorScope.launch(looperDispatcher.immediate) {
				check(mod == _mod.get())
				_queueSharedFlow.emit(controller.getQueue())
				// if `duration` is already set then `onTimelineChanged(Timeline, 1)` won't be called
				_durationSharedFlow.emit(controller.getDuration())
				_propertiesSharedFlow.emit(controller.getPlaybackProperties())
			}
		}

		override fun onIsLoadingChanged(isLoading: Boolean) {
			val mod = _mod.incrementAndGet()
			supervisorScope.launch(looperDispatcher.immediate) {
				check(mod == _mod.get())
				_propertiesSharedFlow.emit(controller.getPlaybackProperties())
			}
		}

		override fun onPositionDiscontinuity(
			oldPosition: Player.PositionInfo,
			newPosition: Player.PositionInfo,
			reason: Int
		) {
			val mod = _mod.incrementAndGet()
			supervisorScope.launch(looperDispatcher.immediate) {
				check(mod == _mod.get())
				_discontinuitySharedFlow.emit(PlaybackEvent.PositionDiscontinuity(
					oldPosition = oldPosition.positionMs.milliseconds,
					newPosition = newPosition.positionMs.milliseconds,
					reason = if (reason == Player.DISCONTINUITY_REASON_SEEK) {
						PositionDiscontinuityReason.USER_SEEK
					} else {
						PositionDiscontinuityReason.UNKNOWN
					}
				))
			}
		}

		override fun onRepeatModeChanged(repeatMode: Int) {
			val mod = _mod.incrementAndGet()
			supervisorScope.launch(looperDispatcher.immediate) {
				check(mod == _mod.get())
				_repeatModeSharedFlow.emit(controller.getRepeatMode())
			}
		}

		override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
			val mod = _mod.incrementAndGet()
			supervisorScope.launch(looperDispatcher.immediate) {
				check(mod == _mod.get())
				_shuffleModeSharedFlow.emit(controller.getShuffleMode())
			}
		}
	}

	// TODO: More descriptive error
	suspend fun connect(): Result<Boolean> {

		val def: Deferred<MediaController> = sync(_lock) {

			if (_deferredController == null) {
				val componentName = ComponentName(context, MusicLibraryService::class.java)
				val sessionToken = SessionToken(context, componentName)
				_deferredController = supervisorScope.async(looperDispatcher.immediate) {
					MediaController.Builder(context, sessionToken)
						.setApplicationLooper(Looper.myLooper()!!)
						.setConnectionHints(Bundle().apply { putString("auth", id) })
						.buildAsync()
						.await()
						.syncApply(_lock) {
							if (shouldRegisterObserverListener) {
								addListener(observerListener)
								observerListenerRegistered = true
							}
						}
				}
			}

			_deferredController!!
		}

		return runCatching { def.await().isConnected }
	}


	val controller = object : PlaybackController {
		override val looper: Looper = this@PlaybackSessionConnector.looper

		override suspend fun getPlaybackSpeed(): Float {
			return runCatching {
				_deferredController?.await()?.playbackParameters?.speed
					?: 1F
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				1F
			}
		}

		override suspend fun getDuration(): Duration {
			return runCatching {
				_deferredController?.await()?.duration?.milliseconds?.takeIf { !it.isNegative() }
					?: PlaybackConstants.DURATION_UNSET
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				PlaybackConstants.DURATION_UNSET
			}
		}

		override suspend fun getBufferedPosition(): Duration {
			return runCatching {
				_deferredController?.await()?.bufferedPosition?.milliseconds?.takeIf { !it.isNegative() }
					?: PlaybackConstants.POSITION_UNSET
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				PlaybackConstants.POSITION_UNSET
			}
		}

		override suspend fun getPosition(): Duration {
			return runCatching {
				_deferredController?.await()?.currentPosition?.milliseconds?.takeIf { !it.isNegative() }
					?: PlaybackConstants.POSITION_UNSET
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				PlaybackConstants.POSITION_UNSET
			}
		}

		override suspend fun getShuffleMode(): ShuffleMode {
			return runCatching {
				if (_deferredController?.await()?.shuffleModeEnabled == true) {
					ShuffleMode.ON
				} else {
					ShuffleMode.OFF
				}
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				ShuffleMode.OFF
			}
		}

		override suspend fun getRepeatMode(): RepeatMode {
			return runCatching {
				when(_deferredController?.await()?.repeatMode
					?: Player.REPEAT_MODE_OFF
				) {
					Player.REPEAT_MODE_OFF -> RepeatMode.OFF
					Player.REPEAT_MODE_ONE -> RepeatMode.ONE
					Player.REPEAT_MODE_ALL -> RepeatMode.ALL
					else -> error("not exhaustive when statement")
				}
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				RepeatMode.OFF
			}
		}

		override suspend fun getQueue(): OldPlaybackQueue {
			if (Looper.myLooper() != looper) {
				return withLooperContext { getQueue() }
			}
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						val mtb = mutableListOf<MediaItem>()
							.apply {
								for (i in 0 until controller.mediaItemCount) {
									add(controller.getMediaItemAt(i))
								}
							}
						val list = mtb.map { it.mediaId }.toPersistentList()
						OldPlaybackQueue(
							list = list,
							currentIndex = if (list.isEmpty()) {
								return@let null
							} else
								controller.currentMediaItemIndex
						)
					}
					?: PlaybackConstants.QUEUE_UNSET
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				PlaybackConstants.QUEUE_UNSET
			}
		}

		override suspend fun isPlayWhenReady(): Boolean {
			return runCatching {
				_deferredController?.await()?.playWhenReady ?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun isPlaying(): Boolean {
			return runCatching {
				_deferredController?.await()?.isPlaying ?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun setPlayWhenReady(playWhenReady: Boolean): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						controller.playWhenReady = playWhenReady
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun setQueue(queue: OldPlaybackQueue): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						val state = controller.playbackState
						controller.stop()
						controller.clearMediaItems()
						controller.setMediaItems(
							queue.list.map { MediaItem.Builder().setMediaId(it).build() },
							queue.currentIndex,
							0L
						)
						if (state != androidx.media3.common.Player.STATE_IDLE) {
							controller.prepare()
						}
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun setRepeatMode(mode: RepeatMode): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						controller.repeatMode = when (mode) {
							RepeatMode.OFF -> Player.REPEAT_MODE_OFF
							RepeatMode.ONE -> Player.REPEAT_MODE_ONE
							RepeatMode.ALL -> Player.REPEAT_MODE_ALL
						}
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun setShuffleMode(mode: ShuffleMode): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						controller.shuffleModeEnabled = when (mode) {
							ShuffleMode.ON -> true
							ShuffleMode.OFF -> false
						}
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun toggleRepeatMode(): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						controller.repeatMode = when (controller.repeatMode) {
							Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
							Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
							Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
							else -> error("")
						}
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun toggleShuffleMode(): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						controller.shuffleModeEnabled = !controller.shuffleModeEnabled
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun seekPosition(progress: Duration): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						controller.seekTo(progress.inWholeMilliseconds)
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun seekPosition(
			expectId: String,
			expectDuration: Duration,
			percent: Float
		): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						if (controller.currentMediaItem?.mediaId != expectId) {
							return false
						}
						if (controller.duration != expectDuration.inWholeMilliseconds) {
							return false
						}
						controller.seekTo(((percent / 100) * expectDuration.inWholeMilliseconds).toLong())
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun seekIndex(index: Int, startPosition: Duration): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						controller.seekTo(index, startPosition.inWholeMilliseconds)
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun play(): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						if (controller.playbackState == androidx.media3.common.Player.STATE_IDLE) {
							controller.prepare()
						}
						if (controller.playbackState == androidx.media3.common.Player.STATE_ENDED) {
							controller.seekToDefaultPosition()
						}
						controller.play()
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun seekNext(): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						if (!controller.hasNextMediaItem()) {
							return@runCatching false
						}
						controller.seekToNextMediaItem()
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun seekPrevious(): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						controller.seekToPrevious()
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun seekPreviousMediaItem(): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						if (!controller.hasPreviousMediaItem()) {
							return@runCatching false
						}
						controller.seekToPreviousMediaItem()
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun getPlaybackProperties(): PlaybackProperties {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						PlaybackProperties(
							playbackState = when (controller.playbackState) {
								Player.STATE_READY -> PlaybackProperties.PlaybackState.READY
								Player.STATE_BUFFERING -> PlaybackProperties.PlaybackState.BUFFERING
								Player.STATE_ENDED -> PlaybackProperties.PlaybackState.ENDED
								Player.STATE_IDLE -> PlaybackProperties.PlaybackState.IDLE
								else -> error("MediaController PlaybackState unexpected: ${controller.playbackState}")
							},
							playWhenReady = controller.playWhenReady,
							canPlayWhenReady = !controller.playWhenReady,
							canPlay = controller.mediaItemCount > 0,
							playing = controller.isPlaying,
							loading = controller.isLoading,
							speed = controller.playbackParameters.speed,
							hasNextMediaItem = controller.hasNextMediaItem(),
							canSeekNext = controller.hasNextMediaItem(),
							hasPreviousMediaItem = controller.hasPreviousMediaItem(),
							canSeekPrevious = true,
							shuffleMode = if (controller.shuffleModeEnabled) ShuffleMode.ON else ShuffleMode.OFF,
							canShuffleOn = controller.isCommandAvailable(Player.COMMAND_SET_SHUFFLE_MODE),
							canShuffleOff = controller.isCommandAvailable(Player.COMMAND_SET_SHUFFLE_MODE),
							repeatMode = when (controller.repeatMode) {
								Player.REPEAT_MODE_OFF -> RepeatMode.OFF
								Player.REPEAT_MODE_ONE -> RepeatMode.ONE
								Player.REPEAT_MODE_ALL -> RepeatMode.ALL
								else -> error("not exhaustive when statement")
							},
							canRepeatOne = true,
							canRepeatAll = true,
							canRepeatOff = true,
							playbackSuppression = listOf(controller.playbackSuppressionReason)
								.filter { it != 0 }
								.toPersistentList()
						)
					}
					?: PlaybackProperties.UNSET
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				PlaybackProperties.UNSET
			}
		}

		override suspend fun requestMoveAsync(
			from: Int,
			expectFromId: String,
			to: Int,
			expectToId: String
		): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						val count = controller.mediaItemCount
						if (count <= from || count <= to) return@let false
						val atFrom = controller.getMediaItemAt(from)
						val atTo = controller.getMediaItemAt(to)
						if (atFrom.mediaId != expectFromId || atTo.mediaId != expectToId) return@let false
						controller.moveMediaItem(from, to)
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}

		override suspend fun requestSeekIndexAsync(
			from: Int,
			expectFromId: String,
			to: Int,
			expectToId: String
		): Boolean {
			return runCatching {
				_deferredController?.await()
					?.let { controller ->
						val count = controller.mediaItemCount
						if (count <= from || count <= to) return@let false
						val atFrom = controller.getMediaItemAt(from)
						val atTo = controller.getMediaItemAt(to)
						if (atFrom.mediaId != expectFromId || atTo.mediaId != expectToId) return@let false
						controller.seekTo(to, 0L)
						true
					}
					?: false
			}.getOrElse { ex ->
				if (ex !is CancellationException) throw ex
				false
			}
		}


		override fun acquireObserver(owner: Any): PlaybackController.Observer {
			return sync(_lock) {
				if (observersMap.isEmpty()) {
					initializeObserverListener()
				}
				observersMap.getOrPut(owner) {
					Observer()
				}
			}
		}

		override fun releaseObserver(owner: Any) {
			return sync(_lock) {
				observersMap.remove(owner)?.let {
					it.dispose()
					if (observersMap.isEmpty()) {
						removeObserverListener()
					}
				}
			}
		}

		override fun hasObserver(owner: Any): Boolean {
			return sync(_lock) {
				observersMap.contains(owner)
			}
		}

		override fun inLooper(): Boolean = Looper.myLooper() == looper

		override suspend fun <R> withLooperContext(block: suspend PlaybackController.() -> R): R {
			return if (Looper.myLooper() == looper) {
				block()
			} else {
				withContext(looperDispatcher) { block() }
			}
		}
	}

	// guarded by _lock
	private fun initializeObserverListener() {
		if (observerListenerRegistered) {
			// should not happen tho
			return
		}
		shouldRegisterObserverListener = true

		supervisorScope.launch(looperDispatcher.immediate) {
			sync(_lock) {
				if (!observerListenerRegistered && shouldRegisterObserverListener &&
					_deferredController?.isCompleted == true && _deferredController?.isCancelled == false
				) {
					_deferredController!!.getCompleted()
						.apply {
							addListener(observerListener)
						}
				}
			}
			_playWhenReadySharedFlow.emit(controller.isPlayWhenReady())
			_repeatModeSharedFlow.emit(controller.getRepeatMode())
			_shuffleModeSharedFlow.emit(controller.getShuffleMode())
			_queueSharedFlow.emit(controller.getQueue())
			_isPlayingSharedFlow.emit(controller.isPlaying())
			_playbackSpeedSharedFlow.emit(controller.getPlaybackSpeed())
			_durationSharedFlow.emit(controller.getDuration())
			_propertiesSharedFlow.emit(controller.getPlaybackProperties())
		}
	}

	// guarded by _lock
	private fun removeObserverListener() {
		shouldRegisterObserverListener = false
		if (!observerListenerRegistered) {
			// should not happen tho
			return
		}

		supervisorScope.launch(looperDispatcher) {
			sync(_lock) {
				if (observerListenerRegistered && !shouldRegisterObserverListener &&
					_deferredController?.isCompleted == true && _deferredController?.isCancelled == false
				) {
					_deferredController!!.getCompleted().removeListener(observerListener)
				}
			}
		}
	}

	private inner class Observer : PlaybackController.Observer {

		override fun observeRepeatMode(): Flow<RepeatMode> =
			_repeatModeSharedFlow.asSharedFlow()

		override fun observeShuffleMode(): Flow<ShuffleMode> =
			_shuffleModeSharedFlow.asSharedFlow()

		override fun observeQueue(): Flow<OldPlaybackQueue> =
			_queueSharedFlow.asSharedFlow()

		override fun observeIsPlaying(): Flow<Boolean> =
			_isPlayingSharedFlow.asSharedFlow()

		override fun observePlaybackSpeed(): Flow<Float> =
			_playbackSpeedSharedFlow.asSharedFlow()

		override fun observePositionDiscontinuityEvent(): Flow<PlaybackEvent.PositionDiscontinuity> =
			_discontinuitySharedFlow.asSharedFlow()

		override fun observeDuration(): Flow<Duration> =
			_durationSharedFlow.asSharedFlow()

		override fun observePlaybackProperties(): Flow<PlaybackProperties> =
			_propertiesSharedFlow.asSharedFlow()

		fun dispose() {
		}
	}
}
