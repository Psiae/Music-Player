package com.flammky.musicplayer.playbackcontrol.domain.usecase

import com.flammky.musicplayer.base.media.playback.RepeatMode
import com.flammky.musicplayer.base.media.playback.ShuffleMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface PlaybackUseCase {
	suspend fun getRepeatMode(): RepeatMode
	suspend fun observeRepeatModeChange(): Flow<RepeatMode>

	suspend fun getShuffleMode(): ShuffleMode
	suspend fun observeShuffleModeChange(): Flow<ShuffleMode>

	suspend fun getPlaybackProgress(): Duration
	suspend fun getPlaybackBufferedProgress(): Duration

	// should we have `id` for certain playlist ?
	suspend fun getPlayList(): ImmutableList<String>
	suspend fun observePlaylistChange(): ImmutableList<String>
}
