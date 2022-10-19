package com.flammky.musicplayer.ui.playbackbox

import android.os.Looper
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.Contract
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.base.media.MediaConnectionDelegate
import com.flammky.musicplayer.domain.media.MediaConnection
import com.flammky.musicplayer.domain.media.MediaConnection.PlaybackInfo.Companion.actuallyUnset
import com.flammky.musicplayer.domain.media.MediaConnection.PlaybackInfo.Companion.isUnset
import com.flammky.musicplayer.ui.playbackbox.PlaybackBoxPositions.Companion.asPlaybackBoxPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

	fun play() = mediaConnectionDelegate.play()
	fun pause() = mediaConnectionDelegate.pause()

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

	fun observeMetadata(id: String): Flow<MediaMetadata?> {
		return mediaConnectionDelegate.repository.observeMetadata(id)
	}

	fun observeArtwork(id: String): Flow<Any?> {
		return mediaConnectionDelegate.repository.observeArtwork(id)
	}

	// should we pair artwork and metadata together ?
	fun observeCurrentArtwork(): Flow<Any?> {
		return callbackFlow {

			var observeArtworkJob: Job? = null
			var rememberId: String = ""

			send(null)

			val observeStreamJob = launch(dispatchers.main) {
				mediaConnection.playback.observePlaylistStream().safeCollect {
					when (it.reason) {
						-1 -> {
							send(null)
							rememberId = ""
							observeArtworkJob?.cancel()
						}
						0 -> {
							send(null)
							rememberId = it.list[it.currentIndex]
							observeArtworkJob = launch { observeArtwork(rememberId).safeCollect { art -> send(art) } }
						}
						1 -> {

						}
					}
				}
			}
			awaitClose {
				observeArtworkJob?.cancel()
				observeStreamJob.cancel()
			}
		}
	}

	// should we pair artwork and metadata together ?
	fun observeCurrentMetadata(): Flow<MediaMetadata?> {
		return callbackFlow {

			var observeMetadataJob: Job? = null
			var rememberId: String

			send(null)

			val observeStreamJob = launch(dispatchers.main) {
				mediaConnection.playback.observePlaylistStream().safeCollect {
					when (it.reason) {
						-1 -> {
							send(null)
							rememberId = ""
							observeMetadataJob?.cancel()
						}
						0 -> {
							send(null)
							rememberId = it.list[it.currentIndex]
							observeMetadataJob = launch { observeMetadata(rememberId).safeCollect { send(it) } }
						}
						1 -> {

						}
					}
				}
			}
			awaitClose {
				observeMetadataJob?.cancel()
				observeStreamJob.cancel()
			}
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
data class PlaybackBoxModel(
	val id: String = "",
	val playing: Boolean = false,
	val playWhenReady: Boolean = false,
) {

	companion object  {
		val UNSET = PlaybackBoxModel()

		fun MediaConnection.PlaybackInfo.asPlaybackBoxModel(): PlaybackBoxModel {
			return PlaybackBoxModel(
				id = this.id,
				playing = this.playing
			)
		}
 	}
}
