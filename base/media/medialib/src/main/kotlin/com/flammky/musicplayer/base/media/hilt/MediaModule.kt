package com.flammky.musicplayer.base.media.hilt

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.musicplayer.base.media.r.*
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
	fun provideMediaConnectionRepository(
		@ApplicationContext context: Context,
		dispatchers: AndroidCoroutineDispatchers
	): MediaConnectionRepository {
		return RealMediaConnectionRepository.provide(context, dispatchers)
	}

	@Provides
	@Singleton
	fun provideMediaConnectionDelegate(
		mediaStoreProvider: MediaStoreProvider,
		repository: MediaConnectionRepository,
	): MediaConnectionDelegate = RealMediaConnectionDelegate(
		mediaStore = mediaStoreProvider,
		repository = repository,
		playback = RealMediaConnectionPlayback(),
	)
}
