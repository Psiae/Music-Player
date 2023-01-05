package com.flammky.musicplayer.domain.media

import com.flammky.musicplayer.base.media.r.MediaConnectionDelegate
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.mediaconnection.playback.real.RealPlaybackConnection
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackControlPresenter
import com.flammky.musicplayer.playbackcontrol.ui.presenter.RealPlaybackControlPresenter
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
	fun providePlaybackConnection(
		delegate: MediaConnectionDelegate,
		media: MediaConnection
	): PlaybackConnection {
		val looper = delegate.playback.publicLooper
		return RealPlaybackConnection(looper).also {
			it.setCurrentSession((media as RealMediaConnection).playbackSession)
		}
	}

	@Provides
	internal fun providePlaybackControlPresenter(
		playbackConnection: PlaybackConnection
	): PlaybackControlPresenter {
		return RealPlaybackControlPresenter(playbackConnection)
	}
}
