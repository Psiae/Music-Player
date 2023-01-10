package com.flammky.musicplayer.dump.mediaplayer.domain.musiclib

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.musicplayer.base.media.MetadataProvider
import com.flammky.musicplayer.base.media.r.MediaMetadataCacheRepository
import com.flammky.musicplayer.base.media.r.TestMetadataProvider
import com.flammky.musicplayer.domain.musiclib.player.exoplayer.ExoPlayerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MusicLibraryModule {

	@Provides
	@Singleton
	fun provideTestArtworkProvider(
		@ApplicationContext context: Context,
		repository: MediaMetadataCacheRepository
	): com.flammky.android.medialib.temp.image.ArtworkProvider {
		return com.flammky.android.medialib.temp.image.internal.TestArtworkProvider(
			context,
			repository
		)
	}

	@Provides
	@Singleton
	fun provideTestMetadataProvider(
		@ApplicationContext context: Context,
		coroutineDispatchers: AndroidCoroutineDispatchers,
		cacheRepository: MediaMetadataCacheRepository,
		mediaStoreProvider: MediaStoreProvider
	): MetadataProvider {
		return TestMetadataProvider(context, coroutineDispatchers, cacheRepository, mediaStoreProvider)
	}
}

@Module
@InstallIn(ServiceComponent::class)
object MusicLibraryServiceModule {
	const val handleSelfAudioReroute = true
	const val handleSelfAudioFocus = true

	@Provides
	@Named("MusicExoPlayer")
	fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer {
		return ExoPlayerFactory
			.createMusicExoPlayer(context, handleSelfAudioFocus, handleSelfAudioReroute)
	}
}
