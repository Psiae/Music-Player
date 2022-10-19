package com.flammky.musicplayer.base.media.hilt

import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.musicplayer.base.media.MediaConnectionDelegate
import com.flammky.musicplayer.base.media.RealMediaConnectionDelegate
import com.flammky.musicplayer.base.media.RealMediaConnectionPlayback
import com.flammky.musicplayer.base.media.RealMediaConnectionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

	@Provides
	@Singleton
	fun provideMediaConnectionDelegate(
		dispatchers: AndroidCoroutineDispatchers
	): MediaConnectionDelegate = RealMediaConnectionDelegate(
		repository = RealMediaConnectionRepository(dispatchers),
		playback = RealMediaConnectionPlayback()
	)
}
