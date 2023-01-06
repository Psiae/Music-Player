package com.flammky.musicplayer.domain.musiclib.media3

import android.content.Context
import com.flammky.musicplayer.domain.musiclib.media3.mediaitem.MediaItemHelper
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
	fun provideMediaItemHelper(@ApplicationContext context: Context) = MediaItemHelper(context)
}
