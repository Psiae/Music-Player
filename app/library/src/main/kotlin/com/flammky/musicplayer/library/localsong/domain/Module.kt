package com.flammky.musicplayer.library.localsong.domain

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.MediaLib
import com.flammky.musicplayer.library.localsong.data.LocalSongRepository
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
	): ObservableLocalSongs = RealObservableLocalSongs(
		repository = repository,
		mediaStore = MediaLib.singleton(context).mediaProviders.mediaStore,
		dispatchers = dispatchers
	)

}
