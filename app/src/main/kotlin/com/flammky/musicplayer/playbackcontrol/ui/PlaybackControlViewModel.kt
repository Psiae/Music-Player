package com.flammky.musicplayer.playbackcontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.coroutine.NonBlockingDispatcherPool
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.common.android.concurrent.ConcurrencyHelper.checkMainThread
import com.flammky.musicplayer.domain.media.MediaConnection
import com.flammky.musicplayer.playbackcontrol.ui.controller.PlaybackController
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackControlPresenter
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
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
	// should probably handled by presenter
	private val authService: AuthService,
	private val mediaConnection: MediaConnection,
	private val presenter: PlaybackControlPresenter,
) : ViewModel(), PlaybackControlPresenter.ViewModel {

	init {
		presenter.initialize(viewModelScope.coroutineContext, this)
	}

	/**
	 * get the currently active user
	 */
	fun currentAuth(): User? {
		return authService.currentUser
	}

	/**
	 * observe the currently active User as a flow,
	 */
	fun observeCurrentAuth(): Flow<User?> {
		return authService.observeCurrentUser()
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
		user: User,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): PlaybackController {
		return presenter.createController(
			user = user,
			coroutineContext = viewModelScope.coroutineContext + coroutineContext
		).also {
			Timber.d("PlaybackController for $user created with coroutineContext: $coroutineContext")
		}
	}

	override fun onCleared() {
		presenter.dispose()
	}

	private val _metadataStateMap = mutableMapOf<String, StateFlow<PlaybackControlTrackMetadata>>()

	@OptIn(ExperimentalCoroutinesApi::class)
	val currentMetadataStateFlow = flow<PlaybackControlTrackMetadata> {
		var job: Job? = null
		observeCurrentAuth()
			.transform { user ->
				job?.cancel()
				if (user == null) {
					emit(null)
					return@transform
				}
				val channel = Channel<PlaybackObserver?>(Channel.CONFLATED)
				job = viewModelScope.launch {
					val controller = presenter.createController(user, viewModelScope.coroutineContext)
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
