package com.kylentt.mediaplayer.data.repository

import android.content.Context
import com.flammky.android.app.coroutines.AppScope
import com.kylentt.mediaplayer.data.source.local.MediaStoreSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module providing Repositories
 * @author Kylentt
 * @since 2022/04/30
 * @see [ProtoRepositoryImpl]
 * @see [MediaRepositoryImpl]
 */

@Module
@InstallIn(SingletonComponent::class)
object RepoModule {

  @Provides
  @Singleton
  fun provideProtoRepository(
    @ApplicationContext context: Context,
    coroutineScope: AppScope
  ): ProtoRepository = ProtoRepositoryImpl(context, coroutineScope)

  @Provides
  @Singleton
  fun provideMediaRepository(
      @ApplicationContext context: Context,
      coroutineScope: AppScope,
      mediaStoreSource: MediaStoreSource
  ): MediaRepository = MediaRepositoryImpl(context, mediaStoreSource)
}
