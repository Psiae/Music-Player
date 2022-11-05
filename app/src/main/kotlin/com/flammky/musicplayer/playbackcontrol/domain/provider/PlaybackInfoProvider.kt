package com.flammky.musicplayer.playbackcontrol.domain.provider

import com.flammky.musicplayer.playbackcontrol.data.PlaybackInfo
import com.google.firebase.inject.Deferred
import kotlinx.coroutines.flow.Flow

internal interface PlaybackInfoProvider {
	suspend fun getAsync(): Deferred<PlaybackInfo>
	suspend fun observe(): Flow<PlaybackInfo>
}
