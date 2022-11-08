package com.flammky.musicplayer.media.mediaconnection.playback

import kotlinx.coroutines.flow.Flow

interface PlaybackConnection {
	fun observePlayback(): Flow<PlaybackInfo>
}
