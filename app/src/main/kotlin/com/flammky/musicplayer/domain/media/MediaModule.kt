package com.flammky.musicplayer.domain.media

import com.flammky.musicplayer.base.media.mediaconnection.MediaConnectionDelegate
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@dagger.Module
@dagger.hilt.InstallIn(SingletonComponent::class)
object MediaModule {

	@Provides
	@Singleton
	fun provideMediaConnection(delegate: MediaConnectionDelegate): MediaConnection {
		return RealMediaConnection(delegate)
	}

}
