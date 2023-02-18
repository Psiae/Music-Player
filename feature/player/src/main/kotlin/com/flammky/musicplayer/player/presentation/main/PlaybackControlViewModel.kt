@file:OptIn(ExperimentalStdlibApi::class)

package com.flammky.musicplayer.player.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.flammky.musicplayer.player.presentation.presenter.PlaybackControlPresenter
import com.flammky.musicplayer.player.presentation.presenter.PlaybackObserver
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
	private val presenter: PlaybackControlPresenter,
) : ViewModel() {

	private val presenterDelegate = object : PlaybackControlPresenter.ViewModel {
		override val mainCoroutineContext: CoroutineContext = viewModelScope.coroutineContext
	}

	init {
		presenter.initialize(presenterDelegate)
	}

	/**
	 * the currently active user
	 */
	val currentAuth: User?
		get() = presenter.auth.currentUser

	/**
	 * observe the currently active User as a flow,
	 */
	fun observeCurrentAuth(): Flow<User?> = presenter.auth.observeCurrentUser()

	/**
	 * create a playback controller for the given [user]
	 * @param user the User this controller should dispatch command onto
	 * @param coroutineContext the parent [CoroutineContext] of this controller.
	 *
	 * **
	 * providing a Job means that cancelling the said Job will also cancel all the Job within the
	 * controller
	 * **
	 */
	fun createUserPlaybackController(
		user: User,
		coroutineContext: CoroutineContext = EmptyCoroutineContext
	): PlaybackController {
		return presenter.createUserPlaybackController(
			user = user,
			coroutineContext = coroutineContext
		)
	}

	override fun onCleared() {
		presenter.dispose()
	}

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
					val controller = presenter.createUserPlaybackController(user, viewModelScope.coroutineContext)
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
							.flatMapLatest(::observeSimpleMetadata)
							.collect(this)
					}
					?: emit(PlaybackControlTrackMetadata())
			}
	}.stateIn(viewModelScope, SharingStarted.Lazily, PlaybackControlTrackMetadata())

	fun observeMediaMetadata(id: String): Flow<MediaMetadata?> {
		return presenter.mediaRepo.observeMetadata(id)
	}

	fun observeMediaArtwork(id: String): Flow<Any?> {
		return presenter.mediaRepo.observeArtwork(id)
	}

	fun getCachedSimpleMetadata(id: String): PlaybackControlTrackMetadata {
		val metadata = presenter.mediaRepo.getCachedMetadata(id)
		return PlaybackControlTrackMetadata(
			id = id,
			artwork = presenter.mediaRepo.getCachedArtwork(id),
			title = metadata?.findTitle(),
			subtitle = metadata?.findSubtitle()
		)
	}
	fun observeSimpleMetadata(id: String): Flow<PlaybackControlTrackMetadata> {
		return combine(
			flow = presenter.mediaRepo.observeArtwork(id),
			flow2 = presenter.mediaRepo.observeMetadata(id)
		) { art: Any?, metadata: MediaMetadata? ->
			PlaybackControlTrackMetadata(id, art, metadata?.findTitle(), metadata?.findSubtitle())
		}
	}
	private fun MediaMetadata.findTitle(): String? = title?.ifBlank { null }
	private fun MediaMetadata.findSubtitle(): String? = (this as? AudioMetadata)
		?.let {
			it.albumArtistName ?: it.artistName
		}
		?: (this as? AudioFileMetadata)?.file
			?.let { fileMetadata ->
				fileMetadata.fileName?.ifBlank { null }
					?: (fileMetadata as? VirtualFileMetadata)?.uri?.toString()
			}
			?.ifBlank { null }
}

// TODO: Rewrite
@Immutable
data class PlaybackControlTrackMetadata(
	val id: String = "",
	val artwork: Any? = null,
	val title: String? = null,
	val subtitle: String? = null,
)
