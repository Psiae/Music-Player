package dev.dexsr.klio.library.user.playlist

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import dev.dexsr.klio.base.composeui.annotations.ComposeUiClass
import dev.dexsr.klio.core.AndroidUiFoundation
import dev.dexsr.klio.core.isOnUiLooper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Composable
fun rememberPlaylistDetailScreenState(
	playlistId: String
): PlaylistDetailScreenState {
	val flingBehavior = ScrollableDefaults.flingBehavior()
	return remember(playlistId) {
		PlaylistDetailScreenState(flingBehavior)
	}
}

@ComposeUiClass
class PlaylistDetailScreenState(
	flingBehavior: FlingBehavior
): RememberObserver {

	val scrollState = PlaylistDetailScreenScrollState(flingBehavior)

	var isPlayable by mutableStateOf<Boolean?>(null)
		private set
	var isPausable by mutableStateOf<Boolean?>(null)
		private set

	var isActionPause by mutableStateOf<Boolean?>(null)
		private set

	// this field is only filled if this playlist is playing
	var currentlyPlayingTrack by mutableStateOf<String?>(null)
		private set

	private var _coroutineScope: CoroutineScope? = null
	private val coroutineScope
		get() = requireNotNull(_coroutineScope) {
			"coroutineScope wasn't initialized by remember"
		}

	private var playbackInfoObserverCount = 0

	override fun onAbandoned() {
		scrollState.onAbandoned()
	}

	override fun onForgotten() {
		coroutineScope.cancel()
		scrollState.onForgotten()
	}

	override fun onRemembered() {
		_coroutineScope = CoroutineScope(SupervisorJob())
		scrollState.onRemembered()
	}

	fun play() {}

	fun pause() {}

	fun playPlaylistFromTrack(id: String) {}

	fun subscribePlaybackAsFlow(): Flow<Nothing> {
		return flow {
			check(AndroidUiFoundation.isOnUiLooper()) {
				"Composable should subscribe on UI dispatcher"
			}
			if (playbackInfoObserverCount++ == 1) {
				startPlaybackInfoObserver()
			}
			try {
			    awaitCancellation()
			} finally { if (playbackInfoObserverCount-- == 0) stopPlaybackInfoObserver() }
		}
	}

	private fun startPlaybackInfoObserver() {

	}

	private fun stopPlaybackInfoObserver() {
		isPlayable = null
		isPausable = null
		isActionPause = null
		currentlyPlayingTrack = null
	}
}
