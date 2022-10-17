package com.flammky.musicplayer.base.media.hilt

import com.flammky.musicplayer.base.media.MediaConnectionDelegate
import com.flammky.musicplayer.base.media.RealMediaConnectionDelegate
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
	fun provideMediaConnectionDelegate(): MediaConnectionDelegate = RealMediaConnectionDelegate()
}
