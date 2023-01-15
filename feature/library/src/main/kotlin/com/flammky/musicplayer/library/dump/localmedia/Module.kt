package com.flammky.musicplayer.library.dump.localmedia

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.musicplayer.base.media.MetadataProvider
import com.flammky.musicplayer.library.dump.localmedia.data.LocalSongRepository
import com.flammky.musicplayer.library.dump.localmedia.data.RealLocalSongRepository
import com.flammky.musicplayer.library.dump.media.MediaConnection
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
		metadataProvider: MetadataProvider
	): LocalSongRepository {
		return RealLocalSongRepository(context, dispatchers, testArtworkProvider, metadataProvider, mediaConnection, mediaStoreProvider)
	}
}
