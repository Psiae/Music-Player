package com.flammky.musicplayer.dump.mediaplayer.domain.musiclib

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.flammky.android.medialib.MediaLib
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.musicplayer.domain.media.MediaConnection
import com.flammky.musicplayer.domain.musiclib.player.exoplayer.ExoPlayerFactory
import com.flammky.musicplayer.domain.musiclib.service.MusicLibraryService
import com.flammky.musicplayer.dump.mediaplayer.helper.external.MediaIntentHandler
import com.flammky.musicplayer.dump.mediaplayer.helper.external.MediaIntentHandlerImpl
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
	fun provideMediaStoreProvider(@ApplicationContext context: Context): com.flammky.android.medialib.temp.api.provider.mediastore.MediaStoreProvider {
		return com.flammky.android.medialib.temp.MediaLibrary.construct(context, MusicLibraryService::class.java).providers.mediaStore
	}

	@Provides
	@Singleton
	fun provideImageRepository(@ApplicationContext context: Context): com.flammky.android.medialib.temp.image.ImageRepository {
		return com.flammky.android.medialib.temp.MediaLibrary.construct(context, MusicLibraryService::class.java).imageRepository
	}

	@Provides
	@Singleton
	fun provideMediaIntentHandler(
		artworkProvider: ArtworkProvider,
		@ApplicationContext context: Context,
		androidCoroutineDispatchers: com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers,
		mediaStoreProvider: com.flammky.android.medialib.temp.api.provider.mediastore.MediaStoreProvider,
		mediaConnection: MediaConnection,
	): MediaIntentHandler {
		return MediaIntentHandlerImpl(
			artworkProvider,
			context,
			androidCoroutineDispatchers,
			mediaStoreProvider,
			mediaConnection,
			MediaLib.singleton(context)
		)
	}

	@Provides
	@Singleton
	fun provideTestImageProvider(@ApplicationContext context: Context): com.flammky.android.medialib.temp.image.ArtworkProvider {
		val lru = com.flammky.android.medialib.temp.MediaLibrary.construct(context, MusicLibraryService::class.java).imageRepository.sharedBitmapLru
		return com.flammky.android.medialib.temp.image.internal.TestArtworkProvider(context, lru)
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
