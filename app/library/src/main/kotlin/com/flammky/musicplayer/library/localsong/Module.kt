package com.flammky.musicplayer.library.localsong

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.musicplayer.library.localsong.data.LocalSongRepository
import com.flammky.musicplayer.library.localsong.data.RealLocalSongRepository
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
		dispatchers: AndroidCoroutineDispatchers
	): LocalSongRepository = RealLocalSongRepository(context, dispatchers)
}
