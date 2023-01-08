package com.flammky.musicplayer.base.media.hilt

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.context.AndroidContext
import com.flammky.android.medialib.context.internal.RealLibraryContext
import com.flammky.android.medialib.providers.ProvidersContext
import com.flammky.android.medialib.providers.mediastore.MediaStoreContext
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.providers.mediastore.internal.RealMediaStoreProvider
import com.flammky.musicplayer.base.Playback
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.mediaconnection.playback.real.RealPlaybackConnection
import com.flammky.musicplayer.base.media.r.MediaContentWatcher
import com.flammky.musicplayer.base.media.r.MediaMetadataCacheRepository
import com.flammky.musicplayer.base.media.r.RealMediaMetadataCacheRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

	@Provides
	@Singleton
	fun providePlaybackConnection(
		@ApplicationContext context: Context,
		authService: AuthService
	): PlaybackConnection {
		return RealPlaybackConnection(
			context = context,
			authService = authService,
			looper = Playback.LOOPER,
		)
	}

	@Provides
	@Singleton
	fun provideMediaStore(@ApplicationContext context: Context): MediaStoreProvider {
		return RealMediaStoreProvider(MediaStoreContext(ProvidersContext(RealLibraryContext(
			AndroidContext(context)
		))))
	}

	@Provides
	@Singleton
	fun provideMediaMetadataCache(
		@ApplicationContext context: Context,
		dispatchers: AndroidCoroutineDispatchers
	): MediaMetadataCacheRepository {
		return RealMediaMetadataCacheRepository.provide(context, dispatchers)
	}

	@Provides
	@Singleton
	fun provideMediaContentWatcher(
		authService: AuthService,
		playbackConnection: PlaybackConnection,
		mediaStoreProvider: MediaStoreProvider,
	): MediaContentWatcher {
		return MediaContentWatcher(authService, playbackConnection, mediaStoreProvider)
	}
}
