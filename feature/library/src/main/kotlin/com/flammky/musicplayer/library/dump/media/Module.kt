package com.flammky.musicplayer.library.dump.media

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.r.MediaMetadataCacheRepository
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@dagger.Module
@dagger.hilt.InstallIn(SingletonComponent::class)
internal object Module {

	@Provides
	@Singleton
	fun provideMediaConnection(
		artworkProvider: ArtworkProvider,
		@ApplicationContext context: Context,
		metadataCacheRepository: MediaMetadataCacheRepository,
		dispatchers: AndroidCoroutineDispatchers,
		playbackConnection: PlaybackConnection,
		mediaStoreProvider: MediaStoreProvider,
	): MediaConnection {
		return RealMediaConnection(
			artworkProvider,
			context,
			dispatchers,
			mediaStoreProvider,
			metadataCacheRepository,
			playbackConnection
		)
	}

	@Provides
	@Singleton
	fun provideMediaConnectionRepository(mc: MediaConnection) = mc.repository
}
