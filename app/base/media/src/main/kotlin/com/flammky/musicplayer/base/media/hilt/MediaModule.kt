package com.flammky.musicplayer.base.media.hilt

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.MediaLib
import com.flammky.musicplayer.base.media.mediaconnection.MediaConnectionDelegate
import com.flammky.musicplayer.base.media.mediaconnection.RealMediaConnectionDelegate
import com.flammky.musicplayer.base.media.mediaconnection.RealMediaConnectionPlayback
import com.flammky.musicplayer.base.media.mediaconnection.RealMediaConnectionRepository
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
	fun provideMediaConnectionDelegate(
		@ApplicationContext context: Context,
		dispatchers: AndroidCoroutineDispatchers
	): MediaConnectionDelegate = RealMediaConnectionDelegate(
		mediaStore = MediaLib.singleton(context).mediaProviders.mediaStore,
		repository = RealMediaConnectionRepository.provide(context, dispatchers),
		playback = RealMediaConnectionPlayback(),
	)
}
