package com.flammky.musicplayer.playbackcontrol.domain.provider

import com.flammky.musicplayer.playbackcontrol.domain.model.PlaybackInfo
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

internal interface PlaybackInfoProvider {
	suspend fun getAsync(): Deferred<PlaybackInfo>
	suspend fun observe(): Flow<PlaybackInfo>
}
