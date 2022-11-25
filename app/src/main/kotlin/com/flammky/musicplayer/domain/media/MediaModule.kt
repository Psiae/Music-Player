package com.flammky.musicplayer.domain.media

import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.musicplayer.base.media.mediaconnection.MediaConnectionDelegate
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.ui.playbackcontrol.PlaybackControlPresenter
import com.flammky.musicplayer.ui.playbackcontrol.RealPlaybackControlPresenter
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@dagger.Module
@dagger.hilt.InstallIn(SingletonComponent::class)
object MediaModule {

	@Provides
	@Singleton
	fun provideMediaConnection(delegate: MediaConnectionDelegate): MediaConnection {
		return RealMediaConnection(delegate)
	}

	@Provides
	@Singleton
	fun providePlaybackConnection(mediaConnection: MediaConnection): PlaybackConnection {
		return mediaConnection as PlaybackConnection
	}

	@Provides
	@Singleton
	fun providePlaybackControlPresenter(
		dispatchers: AndroidCoroutineDispatchers,
		playbackConnection: PlaybackConnection
	): PlaybackControlPresenter {
		return RealPlaybackControlPresenter(dispatchers, playbackConnection)
	}
}
