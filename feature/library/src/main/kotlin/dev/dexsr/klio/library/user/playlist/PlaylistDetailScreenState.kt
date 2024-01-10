package dev.dexsr.klio.library.user.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import dev.dexsr.klio.library.compose.ComposeStable

@Composable
fun rememberPlaylistDetailScreenState(
	playlistId: String
): PlaylistDetailScreenState {
	return remember(playlistId) {
		PlaylistDetailScreenState()
	}.apply {
		DisposableEffect(key1 = this, effect = {
			init()
			onDispose(this@apply::dispose)
		})
	}
}

@ComposeStable
class PlaylistDetailScreenState() {

	fun init() {}

	fun dispose() {}
}
