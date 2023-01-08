package com.flammky.musicplayer.playbackcontrol

import android.content.Context
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.playbackcontrol.ui.presenter.PlaybackControlPresenter
import com.flammky.musicplayer.playbackcontrol.ui.presenter.RealPlaybackControlPresenter
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@dagger.Module
@dagger.hilt.InstallIn(SingletonComponent::class)
object Module {

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
