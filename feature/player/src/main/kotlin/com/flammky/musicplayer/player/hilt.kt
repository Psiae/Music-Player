package com.flammky.musicplayer.player

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.media.MetadataProvider
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.r.MediaMetadataCacheRepository
import com.flammky.musicplayer.player.presentation.presenter.ExpectPlaybackControlPresenter
import com.flammky.musicplayer.player.presentation.presenter.PlaybackControlPresenter
import com.flammky.musicplayer.player.presentation.queue.QueuePresenter
import com.flammky.musicplayer.player.presentation.queue.r.ExpectQueuePresenter
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
		metadataProvider: MetadataProvider
	): PlaybackControlPresenter {
		return ExpectPlaybackControlPresenter(
			context,
			coroutineDispatchers,
			playbackConnection,
			authService,
			metadataCacheRepository,
			artworkProvider,
			metadataProvider
		)
	}

	@Provides
	internal fun provideQueuePresenter(
		authService: AuthService,
		playbackConnection: PlaybackConnection,
		artworkProvider: ArtworkProvider,
		metadataProvider: MetadataProvider,
		metadataCacheRepository: MediaMetadataCacheRepository
	): QueuePresenter {
		return ExpectQueuePresenter(
			authService,
			playbackConnection,
			artworkProvider,
			metadataProvider,
			metadataCacheRepository
		)
	}
}
