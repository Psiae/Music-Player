package com.flammky.musicplayer.playbackcontrol.domain.usecase

import com.flammky.musicplayer.playbackcontrol.data.PlaybackInfo
import com.flammky.musicplayer.playbackcontrol.domain.provider.PlaybackInfoProvider
import kotlinx.coroutines.flow.Flow

internal class ObservePlaybackInfo(
	private val provider: PlaybackInfoProvider
) {
	suspend fun observe(): Flow<PlaybackInfo> = provider.observe()
}
