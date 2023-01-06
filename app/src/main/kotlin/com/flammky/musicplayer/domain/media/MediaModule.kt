package com.flammky.musicplayer.domain.media

import android.content.Context
import android.os.HandlerThread
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.mediaconnection.playback.real.RealPlaybackConnection
import com.flammky.musicplayer.base.media.r.MediaConnectionDelegate
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackControlPresenter
import com.flammky.musicplayer.playbackcontrol.ui.presenter.RealPlaybackControlPresenter
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
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
		@ApplicationContext context: Context,
		authService: AuthService
	): PlaybackConnection {
		return RealPlaybackConnection(
			context = context,
			authService = authService,
			looper = object : HandlerThread("PlaybackConnection") {
				init { start() }
			}.looper,
		)
	}

	@Provides
	internal fun providePlaybackControlPresenter(
		@ApplicationContext context: Context,
		playbackConnection: PlaybackConnection
	): PlaybackControlPresenter {
		return RealPlaybackControlPresenter(
			context,
			playbackConnection
		)
	}
}
