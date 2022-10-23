package com.flammky.musicplayer.ui.playbackbox

import android.os.Looper
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.Contract
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.base.media.mediaconnection.MediaConnectionDelegate
import com.flammky.musicplayer.domain.media.MediaConnection
import com.flammky.musicplayer.domain.media.MediaConnection.PlaybackInfo.Companion.actuallyUnset
import com.flammky.musicplayer.domain.media.MediaConnection.PlaybackInfo.Companion.isUnset
import com.flammky.musicplayer.ui.playbackbox.PlaybackBoxPositions.Companion.asPlaybackBoxPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration

@HiltViewModel
class PlaybackBoxViewModel @Inject constructor(
	private val dispatchers: AndroidCoroutineDispatchers,
	private val mediaConnection: MediaConnection,
	private val mediaConnectionDelegate: MediaConnectionDelegate
	// I think we should consider injecting observable instead ?
) : ViewModel() {

	val playlistStreamFlow = mediaConnection.playback.observePlaylistStream()
		.stateIn(viewModelScope, SharingStarted.Lazily, MediaConnection.Playback.TracksStream())

	val positionStreamFlow = mediaConnection.playback.observePositionStream()
		.stateIn(viewModelScope, SharingStarted.Lazily, MediaConnection.Playback.PositionStream())
		.map { it.asPlaybackBoxPosition }

	val metadataFlow = callbackFlow {
		val job = viewModelScope.launch {
			var combinedJob: Job? = null
			playlistStreamFlow.safeCollect({combinedJob?.cancel()}) { update ->
				when (update.reason) {
					-1 -> {
						// UNSET
						send(null)
						combinedJob?.cancel()
					}
					0 -> {
						// TRANSITION
						send(null)
						combinedJob?.cancel()
						combinedJob = launch {
							val artFlow = observeArtwork(update.list[update.currentIndex])
							val metadataFlow = observeMetadata(update.list[update.currentIndex])
							combine(artFlow, metadataFlow) { art, metadata ->
								PlaybackBoxMetadata(
									artwork = art,
									title = metadata?.title ?: "",
									subtitle = if (metadata is AudioMetadata) metadata.albumArtistName
										?: metadata.artistName
										?: ""
									else ""
								)
							}.safeCollect { boxMetadata -> send(boxMetadata) }
						}
					}
					1 -> {
						// PLAYLIST CHANGE
					}
				}
			}
		}
		awaitClose { job.cancel() }
	}.stateIn(viewModelScope, SharingStarted.Lazily, PlaybackBoxMetadata())

	val artworkFlow = metadataFlow
		.map { it?.artwork }
		.stateIn(viewModelScope, SharingStarted.Lazily, PlaybackBoxMetadata())

	fun play() = mediaConnectionDelegate.play()
	fun pause() = mediaConnectionDelegate.pause()

	fun seekIndex(index: Int) = mediaConnection.playback.seekIndex(index)

	fun observeModel(): Flow<PlaybackBoxModel> {


		Timber.d("observeModel")
		return callbackFlow {

			var remember = PlaybackBoxModel.UNSET

			suspend fun newPlaybackInfo(info: MediaConnection.PlaybackInfo) {

				check(Looper.myLooper() == Looper.getMainLooper())
				if (info.isUnset) {
					remember = PlaybackBoxModel()
					return send(remember)
				}
				if (info.actuallyUnset) {
					remember = PlaybackBoxModel.UNSET
					return send(remember)
				}
				remember = PlaybackBoxModel(
					id = info.id,
					playing = info.playing,
					playWhenReady = info.playWhenReady,
				)
				send(remember)
			}

			val infoJob = viewModelScope.launch {
				mediaConnection.playback.observeInfo().safeCollect(collect = ::newPlaybackInfo)
			}

			awaitClose {
				infoJob.cancel()
			}
		}
	}

	suspend fun observeMetadata(id: String): Flow<MediaMetadata?> {
		return mediaConnectionDelegate.repository.observeMetadata(id)
	}

	suspend fun observeArtwork(id: String): Flow<Any?> {
		return mediaConnectionDelegate.repository.observeArtwork(id)
	}

	suspend fun observeBoxMetadata(id: String): Flow<PlaybackBoxMetadata> {
		return combine(observeArtwork(id), observeMetadata(id)) { a: Any?, b: MediaMetadata? ->
			val subtitle = (b as? AudioMetadata)?.let {
				it.albumArtistName ?: it.artistName
			}
			PlaybackBoxMetadata(a, b?.title ?: "", subtitle ?: "")
		}
	}

	/**
	 * Observe The Playback Positions
	 */
	fun observePositions(): Flow<PlaybackBoxPositions> {
		return callbackFlow {
			val job = launch(dispatchers.io) {
				mediaConnection.playback.observePositionStream().safeCollect { stream ->
					send(stream.asPlaybackBoxPosition)
				}
			}
			awaitClose { job.cancel() }
		}
	}
}

@Immutable
data class PlaybackBoxPositions(
	val bufferedPosition: Duration = Contract.POSITION_UNSET,
	val position: Duration = Contract.POSITION_UNSET,
	val duration: Duration = Contract.DURATION_UNSET,
) {
	companion object {
		val MediaConnection.Playback.PositionStream.asPlaybackBoxPosition
			get() = PlaybackBoxPositions(
				bufferedPosition = bufferedPosition,
				position = position,
				duration = duration
			)
	}
}

@Immutable
data class PlaybackBoxMetadata(
	val artwork: Any? = ART_UNSET,
	val title: String = "",
	val subtitle: String = "",
) {
	companion object {
		val ART_UNSET = Any()
	}
}


@Immutable
data class PlaybackBoxModel(
	val id: String = "",
	val playing: Boolean = false,
	val playWhenReady: Boolean = false,
) {

	companion object  {
		val UNSET = PlaybackBoxModel()
 	}
}
