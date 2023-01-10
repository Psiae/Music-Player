package com.flammky.musicplayer.playbackcontrol

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.r.MediaMetadataCacheRepository
import com.flammky.musicplayer.playbackcontrol.presentation.presenter.ExpectPlaybackControlPresenter
import com.flammky.musicplayer.playbackcontrol.presentation.presenter.PlaybackControlPresenter
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@dagger.Module
@dagger.hilt.InstallIn(SingletonComponent::class)
object Module {

	@Provides
	internal fun providePlaybackControlPresenter(
		@ApplicationContext context: Context,
		coroutineDispatchers: AndroidCoroutineDispatchers,
		playbackConnection: PlaybackConnection,
		authService: AuthService,
		metadataCacheRepository: MediaMetadataCacheRepository,
		artworkProvider: ArtworkProvider,
		mediaStoreProvider: MediaStoreProvider
	): PlaybackControlPresenter {
		return ExpectPlaybackControlPresenter(
			context,
			coroutineDispatchers,
			playbackConnection,
			authService,
			metadataCacheRepository,
			artworkProvider,
			mediaStoreProvider
		)
	}
}
