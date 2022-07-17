package com.kylentt.musicplayer.domain.musiclib.core.media3

import android.content.Context
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaItemHelper
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
