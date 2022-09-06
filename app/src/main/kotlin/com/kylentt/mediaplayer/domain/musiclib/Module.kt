package com.kylentt.mediaplayer.domain.musiclib

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.kylentt.mediaplayer.helper.external.MediaIntentHandler
import com.kylentt.mediaplayer.helper.external.MediaIntentHandlerImpl
import com.kylentt.musicplayer.common.coroutines.CoroutineDispatchers
import com.kylentt.musicplayer.domain.musiclib.player.exoplayer.ExoPlayerFactory
import com.kylentt.musicplayer.medialib.MediaLibrary
import com.kylentt.musicplayer.medialib.api.provider.mediastore.MediaStoreProvider
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
	fun provideMediaStoreProvider(@ApplicationContext context: Context): MediaStoreProvider {
		return MediaLibrary.construct(context).providers.mediaStore
	}

	@Provides
	@Singleton
	fun provideMediaIntentHandler(
		@ApplicationContext context: Context,
		coroutineDispatchers: CoroutineDispatchers,
		mediaStoreProvider: MediaStoreProvider
	): MediaIntentHandler {
		return MediaIntentHandlerImpl(context, coroutineDispatchers, mediaStoreProvider)
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
