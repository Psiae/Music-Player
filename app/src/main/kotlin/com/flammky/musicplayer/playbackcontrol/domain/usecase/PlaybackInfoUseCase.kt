package com.flammky.musicplayer.playbackcontrol.domain.usecase

import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackInfo
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackProperties
import com.flammky.musicplayer.media.mediaconnection.playback.PlaylistInfo
import com.flammky.musicplayer.media.mediaconnection.playback.PositionInfo
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

interface PlaybackInfoUseCase {
	suspend fun getInfoAsync(): Deferred<PlaybackInfo>
	fun observeCurrent(): Flow<PlaybackInfo>

	fun observeProperties(): Flow<PlaybackProperties>
	fun observePlaylist(): Flow<PlaylistInfo>
	fun observePosition(): Flow<PositionInfo>
}
