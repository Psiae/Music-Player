package com.kylentt.mediaplayer.data.source

import android.content.Context
import com.kylentt.mediaplayer.app.coroutines.AppDispatchers
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
      coroutineDispatchers: AppDispatchers
    ) = MediaStoreSourceImpl(context, coroutineDispatchers)
  }
}
