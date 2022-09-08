package com.kylentt.mediaplayer.data.source

import android.content.Context
import com.kylentt.musicplayer.common.coroutines.AndroidCoroutineDispatchers
import com.kylentt.mediaplayer.data.source.local.MediaStoreSource
import com.kylentt.mediaplayer.data.source.local.MediaStoreSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module Providing DataSource
 * @author Kylentt
 * @since 2022/04/30
 * @see [MediaStoreSourceImpl]
 */

object SourceModule {

  @Module
  @InstallIn(SingletonComponent::class)
  object LocalSourceModule {

    @Provides
    @Singleton
    fun provideMediaStoreSource(
			@ApplicationContext context: Context,
			androidCoroutineDispatchers: AndroidCoroutineDispatchers
    ): MediaStoreSource = MediaStoreSourceImpl(context, androidCoroutineDispatchers)
  }
}
