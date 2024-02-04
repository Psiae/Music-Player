package dev.dexsr.klio.library.playback

import kotlinx.coroutines.flow.Flow

interface PlaylistPlaybackControl {

	fun playPlaylist() {

	}

	fun pausePlaylist() {

	}

	fun playPlaylistFromTrack(fromTrackId: String) {

	}

	fun observePlaybackInfoAsFlow(): Flow<PlaylistPlaybackInfo>
}
