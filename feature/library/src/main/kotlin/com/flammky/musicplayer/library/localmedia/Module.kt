package com.flammky.musicplayer.library.localmedia

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.musicplayer.library.localmedia.data.LocalSongRepository
import com.flammky.musicplayer.library.localmedia.data.RealLocalSongRepository
import com.flammky.musicplayer.library.media.MediaConnection
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@dagger.Module
@dagger.hilt.InstallIn(SingletonComponent::class)
internal object Module {

	@Singleton
	@Provides
	internal fun provideLocalSongRepository(
		@ApplicationContext context: Context,
		dispatchers: AndroidCoroutineDispatchers,
		mediaConnection: MediaConnection,
		testArtworkProvider: ArtworkProvider,
		mediaStoreProvider: MediaStoreProvider,
	): LocalSongRepository {
		return RealLocalSongRepository(context, dispatchers, testArtworkProvider, mediaConnection, mediaStoreProvider)
	}
}
