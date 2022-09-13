package com.kylentt.mediaplayer.domain.musiclib

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.kylentt.mediaplayer.helper.external.MediaIntentHandler
import com.kylentt.mediaplayer.helper.external.MediaIntentHandlerImpl
import com.flammky.common.kotlin.coroutines.AndroidCoroutineDispatchers
import com.kylentt.musicplayer.domain.musiclib.player.exoplayer.ExoPlayerFactory
import com.kylentt.musicplayer.domain.musiclib.service.MusicLibraryService
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
		@ApplicationContext context: Context,
		androidCoroutineDispatchers: AndroidCoroutineDispatchers,
		mediaStoreProvider: com.flammky.android.medialib.temp.api.provider.mediastore.MediaStoreProvider
	): MediaIntentHandler {
		return MediaIntentHandlerImpl(context, androidCoroutineDispatchers, mediaStoreProvider)
	}

	@Provides
	@Singleton
	fun provideTestImageProvider(@ApplicationContext context: Context): com.flammky.android.medialib.temp.image.ImageProvider {
		val lru = com.flammky.android.medialib.temp.MediaLibrary.construct(context, MusicLibraryService::class.java).imageRepository.sharedBitmapLru
		return com.flammky.android.medialib.temp.image.internal.TestImageProvider(lru)
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
