package com.flammky.musicplayer.library.localsong

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.MediaLib
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.MediaLibrary
import com.flammky.android.medialib.temp.image.internal.TestArtworkProvider
import com.flammky.musicplayer.library.localsong.data.LocalSongRepository
import com.flammky.musicplayer.library.localsong.data.RealLocalSongRepository
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
	internal fun provideMediaStoreProvider(@ApplicationContext context: Context): MediaStoreProvider {
		return MediaLib.singleton(context).mediaProviders.mediaStore
	}

	@Singleton
	@Provides
	internal fun provideLocalSongRepository(
		@ApplicationContext context: Context,
		dispatchers: AndroidCoroutineDispatchers,
		mediaConnection: MediaConnection,
	): LocalSongRepository {
		val lru = MediaLibrary.API.imageRepository.sharedBitmapLru
		return RealLocalSongRepository(context, dispatchers, TestArtworkProvider(context, lru), mediaConnection)
	}

}
