package dev.dexsr.klio.library.user.playlist

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import com.flammky.androidx.viewmodel.compose.activityViewModel
import com.flammky.musicplayer.library.dump.localmedia.ui.LocalSongViewModel
import dev.dexsr.klio.base.composeui.annotations.ComposeUiClass
import dev.dexsr.klio.library.playback.PlaylistPlaybackControl
import dev.dexsr.klio.library.playback.PlaylistPlaybackInfo
import kotlinx.coroutines.flow.Flow

@Composable
fun rememberPlaylistDetailScreenState(
	playlistId: String
): PlaylistDetailScreenState {
	val flingBehavior = ScrollableDefaults.flingBehavior()
	//@Deprecated("old impl")
	val vm = activityViewModel<LocalSongViewModel>()
	return remember(playlistId) {
		val pc = object : PlaylistPlaybackControl {
			override fun playPlaylist() {
				vm.play(0)
			}

			override fun pausePlaylist() {
				// no impl yet
			}

			override fun playPlaylistFromTrack(fromTrackId: String) {
				vm.play(fromTrackId)
			}

			override fun observePlaybackInfoAsFlow(): Flow<PlaylistPlaybackInfo> {
				return vm.observePlaylistPlaybackInfo()
			}
		}
		PlaylistDetailScreenState(flingBehavior, pc)
	}
}

@ComposeUiClass
class PlaylistDetailScreenState(
	flingBehavior: FlingBehavior,
	playbackControl: PlaylistPlaybackControl
): RememberObserver {

	val scrollState = PlaylistDetailScreenScrollState(flingBehavior)
	val playbackState = PlaybackDetailScreenPlaybackState(playbackControl)

	override fun onAbandoned() {
		scrollState.onAbandoned()
		playbackState.onAbandoned()
	}

	override fun onForgotten() {
		scrollState.onForgotten()
		playbackState.onForgotten()
	}

	override fun onRemembered() {
		scrollState.onRemembered()
		playbackState.onRemembered()
	}
}
