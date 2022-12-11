package com.flammky.musicplayer.ui.playbackcontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.medialib.common.Contract
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.player.Player
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.common.android.concurrent.ConcurrencyHelper.checkMainThread
import com.flammky.musicplayer.domain.media.MediaConnection
import com.flammky.musicplayer.playbackcontrol.ui.controller.PlaybackController
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackObserver
import com.flammky.musicplayer.ui.playbackcontrol.PlaybackDetailPositionStream.PositionChangeReason.Companion.asPlaybackDetails
import com.flammky.musicplayer.ui.playbackcontrol.PlaybackDetailPropertiesInfo.Companion.asPlaybackDetails
import com.flammky.musicplayer.ui.playbackcontrol.PlaybackDetailTracksInfo.Companion.asPlaybackDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import kotlin.time.Duration

@HiltViewModel
internal class PlaybackControlViewModel @Inject constructor(
	private val mediaConnection: MediaConnection,
	private val presenter: PlaybackControlPresenter,
) : ViewModel() {

	val playbackController: PlaybackController = presenter.createController(
		this,
		CoroutineScope(viewModelScope.coroutineContext.job)
	)

	val playbackObserver: PlaybackObserver = playbackController.createObserver()

	override fun onCleared() {
		presenter.dispose()
	}

	private val _metadataStateMap = mutableMapOf<String, StateFlow<PlaybackControlTrackMetadata>>()
	private val _trackStreamFlow = MutableStateFlow(MediaConnection.Playback.TracksInfo())

	private val _playbackPropertiesFlow = mediaConnection.playback.observePropertiesInfo()

	// Inject as Dependency instead
	val trackStreamStateFlow = _trackStreamFlow
		.map { it.asPlaybackDetails }
		.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackDetailTracksInfo())

	// Inject as Dependency instead
	val playbackPropertiesStateFlow = _playbackPropertiesFlow
		.map { it.asPlaybackDetails }
		.stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackDetailPropertiesInfo())

	@OptIn(ExperimentalCoroutinesApi::class)
	val currentMetadataStateFlow = _trackStreamFlow.mapLatest { tracksInfo ->
		val id = tracksInfo.takeIf { it.currentIndex >= 0 && it.list.isNotEmpty() }
			?.let { safeTrackInfo -> safeTrackInfo.list[safeTrackInfo.currentIndex] }
			?: ""
		Timber.d("PlaybackDetailViewModel currentMetadata observing $id, $tracksInfo")
		id
	}.distinctUntilChanged()
		.flatMapLatest { id -> observeMetadata(id) }
		.stateIn(viewModelScope, SharingStarted.Lazily, PlaybackControlTrackMetadata())

	init {
		viewModelScope.launch {
			mediaConnection.playback.observePlaylistStream().safeCollect { _trackStreamFlow.value = it }
		}
	}



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
			val combined = combine(
				flow = mediaConnection.repository.observeArtwork(id),
				flow2 = mediaConnection.repository.observeMetadata(id)
			) { art: Any?, metadata: MediaMetadata? ->
				val title = metadata?.title?.ifBlank { null }
					?: (metadata as? AudioFileMetadata)?.file?.let { fileMetadata ->
						fileMetadata.fileName?.ifBlank { null }
							?: (fileMetadata as? VirtualFileMetadata)?.uri?.toString()
					}
				val subtitle = (metadata as? AudioMetadata)
					?.let { it.albumArtistName ?: it.artistName }
				PlaybackControlTrackMetadata(id, art, title, subtitle)
			}
			emitAll(combined)
		}.stateIn(viewModelScope, SharingStarted.Lazily, PlaybackControlTrackMetadata(id))
	}

	fun playWhenReady() {
		mediaConnection.playback.playWhenReady()
	}
	fun pause() {
		mediaConnection.playback.pause()
	}
	fun seekNext() {
		mediaConnection.playback.seekNext()
	}

	fun seekPrevious() {
		mediaConnection.playback.seekPrevious()
	}
	fun seekPreviousMediaForPager() {
		mediaConnection.playback.seekPreviousMedia()
	}
	fun enableShuffleMode() {
		mediaConnection.playback.setShuffleMode(true)
	}
	fun disableShuffleMode() {
		mediaConnection.playback.setShuffleMode(false)
	}
	fun enableRepeatMode() {
		mediaConnection.playback.setRepeatMode(Player.RepeatMode.ONE)
	}
	fun enableRepeatAllMode() {
		mediaConnection.playback.setRepeatMode(Player.RepeatMode.ALL)
	}
	fun disableRepeatMode() {
		mediaConnection.playback.setRepeatMode(Player.RepeatMode.OFF)
	}
}

@Immutable
data class PlaybackDetailPagerData(
	val previousItem: PlaybackDetailPagerItem? = null,
	val currentItem: PlaybackDetailPagerItem? = null,
	val nextItem: PlaybackDetailPagerItem? = null
) {

	sealed interface PagerEvent {
		object SEEK_NEXT : PagerEvent
		object SEEK_PREVIOUS : PagerEvent
	}

	companion object {
		inline val PlaybackDetailPagerData.currentIndex
			get() = when {
				nextItem != null && previousItem != null -> 1
				nextItem != null -> 0
				previousItem != null -> 1
				else -> -1
			}

		fun PlaybackDetailPagerData.idEquals(ids: List<String?>): Boolean {
			return previousItem?.key == ids.getOrNull(0) &&
				currentItem?.key == ids.getOrNull(1) &&
				nextItem?.key == ids.getOrNull(2)
		}

		fun PlaybackDetailPagerData.toImmutableList(): ImmutableList<PlaybackDetailPagerItem?> {
			return persistentListOf(previousItem, currentItem, nextItem)
		}

		fun PlaybackDetailPagerData.updateForId(
			id: String,
			item: PlaybackDetailPagerItem
		): PlaybackDetailPagerData {
			return when (id) {
				previousItem?.key -> copy(previousItem = item)
				currentItem?.key -> copy(currentItem = item)
				nextItem?.key -> copy(nextItem = item)
				else -> this
			}
		}

		fun List<PlaybackDetailPagerItem?>.toPagerData(): PlaybackDetailPagerData {
			return PlaybackDetailPagerData(
				getOrNull(0),
				getOrNull(1),
				getOrNull(2)
			)
		}

		fun PlaybackDetailPagerData.toNotNullList(): List<PlaybackDetailPagerItem> {
			val holder = mutableListOf<PlaybackDetailPagerItem>()
			if (previousItem != null) holder.add(previousItem)
			if (currentItem != null) holder.add(currentItem)
			if (nextItem != null) holder.add(nextItem)
			return holder
		}
	}
}

@Immutable
data class PlaybackDetailPagerItem(
	val key: String,
	val metadata: PlaybackControlTrackMetadata
)

@Immutable
data class PlaybackControlTrackMetadata(
	val id: String = "",
	val artwork: Any? = null,
	val title: String? = null,
	val subtitle: String? = null,
)

@Immutable
data class PlaybackDetailPropertiesInfo(
	val playWhenReady: Boolean = false,
	val playing: Boolean = false,
	// should be hasNext instead, it's our UI properties
	val hasNextMediaItem: Boolean = false,
	// should be hasPrevious instead, it's our UI properties
	val hasPreviousMediaItem: Boolean = false,
	val shuffleOn: Boolean = false,
	val repeatMode: Player.RepeatMode = Player.RepeatMode.OFF,
	val playerState: Player.State = Player.State.IDLE
	// later suppressionInfo
) {
	companion object {
		inline val MediaConnection.Playback.PropertiesInfo.asPlaybackDetails
			get() = PlaybackDetailPropertiesInfo(
				playWhenReady = playWhenReady,
				playing = playing,
				hasNextMediaItem = hasNextMediaItem,
				hasPreviousMediaItem = hasPreviousMediaItem,
				shuffleOn = shuffleEnabled,
				repeatMode = repeatMode,
				playerState = playerState
			)
	}
}

@Immutable
data class PlaybackDetailTracksInfo(
	val currentIndex: Int = Contract.INDEX_UNSET,
	val tracks: ImmutableList<String> = persistentListOf(),
) {
	companion object {
		inline val MediaConnection.Playback.TracksInfo.asPlaybackDetails
			get() = PlaybackDetailTracksInfo(
				currentIndex = currentIndex,
				tracks = list
			)
	}
}

@Immutable
data class PlaybackDetailPositionStream(
	val positionChangeReason: PositionChangeReason = PositionChangeReason.UNKNOWN,
	val position: Duration = Contract.POSITION_UNSET,
	val bufferedPosition: Duration = Contract.POSITION_UNSET,
	val duration: Duration = Contract.DURATION_INDEFINITE
) {
	companion object {
		inline val MediaConnection.Playback.PositionStream.asPlaybackDetails
			get() = PlaybackDetailPositionStream(
				positionChangeReason = positionChangeReason.asPlaybackDetails,
				position = position,
				bufferedPosition = bufferedPosition,
				duration = duration
			)
	}

	sealed interface PositionChangeReason {
		object AUTO : PositionChangeReason
		object USER_SEEK : PositionChangeReason
		object PERIODIC : PositionChangeReason
		object UNKNOWN : PositionChangeReason

		companion object {
			inline val MediaConnection.Playback.PositionStream.PositionChangeReason.asPlaybackDetails
				get() = when (this) {
					MediaConnection.Playback.PositionStream.PositionChangeReason.AUTO -> AUTO
					MediaConnection.Playback.PositionStream.PositionChangeReason.PERIODIC -> PERIODIC
					MediaConnection.Playback.PositionStream.PositionChangeReason.UNKN0WN -> UNKNOWN
					MediaConnection.Playback.PositionStream.PositionChangeReason.USER_SEEK -> USER_SEEK
				}
		}
	}
}
