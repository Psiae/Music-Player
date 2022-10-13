package com.flammky.musicplayer.base.media.hilt

import android.content.Context
import com.flammky.musicplayer.base.media.MediaConnection
import com.flammky.musicplayer.base.media.SingletonMediaConnection
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
	fun provideMediaConnection(
		@ApplicationContext context: Context
	): MediaConnection = SingletonMediaConnection.getOrCreate(context)
}
