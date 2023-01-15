package com.flammky.musicplayer.library.dump.localmedia.domain

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.musicplayer.library.dump.localmedia.data.LocalSongRepository
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@dagger.Module
@dagger.hilt.InstallIn(SingletonComponent::class)
object Module {


	@Provides
	@Singleton
	fun provideObservableLocalSongs(
		@ApplicationContext context: Context,
		dispatchers: AndroidCoroutineDispatchers,
		repository: LocalSongRepository,
		mediaStoreProvider: MediaStoreProvider
	): ObservableLocalSongs = RealObservableLocalSongs(
		repository = repository,
		mediaStore = mediaStoreProvider,
		dispatchers = dispatchers
	)
}
