package com.flammky.musicplayer.playbackcontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.musicplayer.base.coroutine.NonBlockingDispatcherPool
import com.flammky.musicplayer.common.android.concurrent.ConcurrencyHelper.checkMainThread
import com.flammky.musicplayer.domain.media.MediaConnection
import com.flammky.musicplayer.playbackcontrol.ui.controller.PlaybackController
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import com.flammky.musicplayer.ui.playbackcontrol.PlaybackControlPresenter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@HiltViewModel
internal class PlaybackControlViewModel @Inject constructor(
	private val mediaConnection: MediaConnection,
	private val presenter: PlaybackControlPresenter,
) : ViewModel(), PlaybackControlPresenter.ViewModel {

	init {
		presenter.initialize(viewModelScope.coroutineContext, this)
	}

	/**
	 * get the currently active sessionID,
	 * if this ViewModel is already cleared this function will return null regardless of current
	 */
	fun currentSessionID(): String? {
		return presenter.currentSessionID()
	}

	/**
	 * observe the currently active session ID as a flow,
	 * if this ViewModel is already cleared this will return an empty flow
	 * if this ViewModel is cleared during flow collection the flow will stop emitting
	 * null emission means that there is currently no active session
	 */
	fun observeCurrentSessionId(): Flow<String?> {
		return presenter.observeCurrentSessionId()
	}

	/**
	 * create a playback controller for the given [sessionID]
	 * @param sessionID the id of the session this controller should dispatch command onto
	 * @param coroutineContext the parent [CoroutineContext] of this controller.
	 *
	 * **
	 * provided dispatcher will be confined to a Single Parallelism via `limitedParallelism(1)`
	 * failure on confining attempt will be default to [NonBlockingDispatcherPool]
	 * **
	 *
	 * **
	 * providing a Job means that cancelling the said Job will also cancel all the Job within the
	 * controller, defaults to the ViewModel job
	 * **
	 */
	fun createController(
		sessionID: String,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): PlaybackController {
		return presenter.createController(
			sessionID = sessionID,
			coroutineContext = viewModelScope.coroutineContext + coroutineContext
		).also {
			Timber.d("PlaybackController for $sessionID created with coroutineContext: $coroutineContext")
		}
	}

	override fun onCleared() {
		presenter.dispose()
	}

	private val _metadataStateMap = mutableMapOf<String, StateFlow<PlaybackControlTrackMetadata>>()

	@OptIn(ExperimentalCoroutinesApi::class)
	val currentMetadataStateFlow = flow<PlaybackControlTrackMetadata> {
		var job: Job? = null
		presenter.observeCurrentSessionId()
			.transform { id ->
				job?.cancel()
				if (id == null) {
					emit(null)
					return@transform
				}
				val channel = Channel<PlaybackObserver?>(Channel.CONFLATED)
				job = viewModelScope.launch {
					val controller = presenter.createController(id, viewModelScope.coroutineContext)
					channel.send(controller.createPlaybackObserver())
					try {
						awaitCancellation()
					} finally {
						controller.dispose()
					}
				}
				emitAll(channel.consumeAsFlow())
			}.collect { observer ->
				observer?.createQueueCollector(EmptyCoroutineContext)
					?.let { collector ->
						collector.startCollect().join()
						collector.queueStateFlow
							.map { tracksInfo ->
								val id = tracksInfo.takeIf { it.currentIndex >= 0 && it.list.isNotEmpty() }
									?.let { safeTrackInfo -> safeTrackInfo.list[safeTrackInfo.currentIndex] }
									?: ""
								id.also {
									Timber.d("CurrentMetadataStateFlow sent$it, param: $tracksInfo")
								}
							}
							.distinctUntilChanged()
							.flatMapLatest(::observeMetadata)
							.collect(this)
					}
					?: emit(PlaybackControlTrackMetadata())
			}
	}.stateIn(viewModelScope, SharingStarted.Lazily, PlaybackControlTrackMetadata())

	// Inject as Dependency
	fun observeMetadata(id: String): StateFlow<PlaybackControlTrackMetadata> {
		checkMainThread()
		if (!_metadataStateMap.containsKey(id)) {
			_metadataStateMap[id] = createMetadataStateFlowForId(id)
		}
		return _metadataStateMap[id]!!
	}

	private fun createMetadataStateFlowForId(id: String): StateFlow<PlaybackControlTrackMetadata> {
		return flow {
			if (id == "") return@flow
			combine(
				flow = mediaConnection.repository.observeArtwork(id),
				flow2 = mediaConnection.repository.observeMetadata(id)
			) { art: Any?, metadata: MediaMetadata? ->
				val title = metadata?.title?.ifBlank { null }
					?: (metadata as? AudioFileMetadata)?.file
						?.let { fileMetadata ->
							fileMetadata.fileName?.ifBlank { null }
								?: (fileMetadata as? VirtualFileMetadata)?.uri?.toString()
						}
				val subtitle = (metadata as? AudioMetadata)
					?.let { it.albumArtistName ?: it.artistName }
				PlaybackControlTrackMetadata(id, art, title, subtitle)
			}.collect(this)
		}.stateIn(viewModelScope, SharingStarted.Lazily, PlaybackControlTrackMetadata(id))
	}
}

// TODO: Rewrite
@Immutable
data class PlaybackControlTrackMetadata(
	val id: String = "",
	val artwork: Any? = null,
	val title: String? = null,
	val subtitle: String? = null,
)
