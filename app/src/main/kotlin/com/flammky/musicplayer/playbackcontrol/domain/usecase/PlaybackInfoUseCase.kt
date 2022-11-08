package com.flammky.musicplayer.playbackcontrol.domain.usecase

import com.flammky.musicplayer.playbackcontrol.domain.model.PlaybackInfo
import com.flammky.musicplayer.playbackcontrol.domain.provider.PlaybackInfoProvider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

internal class PlaybackInfoUseCase (
	private val infoProvider: PlaybackInfoProvider
) {
	suspend fun getInfoAsync(): Deferred<PlaybackInfo> = infoProvider.getAsync()
	suspend fun observe(): Flow<PlaybackInfo> = infoProvider.observe()
}
