package dev.dexsr.klio.library.user.playlist

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.dexsr.klio.core.AndroidUiFoundation
import dev.dexsr.klio.core.MainDispatcher
import dev.dexsr.klio.core.isOnUiLooper
import dev.dexsr.klio.library.playback.PlaylistPlaybackControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class PlaybackDetailScreenPlaybackState(
	private val pc: PlaylistPlaybackControl
) : RememberObserver {

	private var playbackInfoObserverCount = 0
	private var playbackInfoObserver: Job? = null


	private var _coroutineScope: CoroutineScope? = null
	private val coroutineScope
		get() = requireNotNull(_coroutineScope) {
			"coroutineScope wasn't initialized by remember"
		}

	var isPlayable by mutableStateOf<Boolean?>(null)
		private set
	var isPausable by mutableStateOf<Boolean?>(null)
		private set

	var isActionPause by mutableStateOf<Boolean?>(null)
		private set

	// this field is only filled if this playlist is playing
	var currentlyPlayingTrack by mutableStateOf<String?>(null)
		private set



	override fun onAbandoned() {
	}

	override fun onForgotten() {
		coroutineScope.cancel()
	}

	override fun onRemembered() {
		_coroutineScope = CoroutineScope(SupervisorJob())
	}

	fun play() {
		pc.playPlaylist()
	}

	fun pause() {
		pc.pausePlaylist()
	}

	fun playPlaylistFromTrack(id: String) {
		pc.playPlaylistFromTrack(id)
	}

	fun subscribePlaybackAsFlow(): Flow<Nothing> {
		return flow {
			check(AndroidUiFoundation.isOnUiLooper()) {
				"Composable should subscribe on UI dispatcher"
			}
			if (playbackInfoObserverCount++ == 0) {
				startPlaybackInfoObserver()
			}
			try {
				awaitCancellation()
			} finally { if (playbackInfoObserverCount-- == 1) stopPlaybackInfoObserver() }
		}
	}

	private fun startPlaybackInfoObserver() {
		playbackInfoObserver = coroutineScope.launch(AndroidUiFoundation.MainDispatcher.immediate) {
			pc.observePlaybackInfoAsFlow()
				.collect { info ->
					// fixme: no info yet
					isPlayable = true
					isPausable = info.canPause
					isPlayable = info.canPlay
					isActionPause = info.playing
				}
		}
	}

	private fun stopPlaybackInfoObserver() {
		playbackInfoObserver?.cancel()
		isPlayable = null
		isPausable = null
		isActionPause = null
		currentlyPlayingTrack = null
	}
}
