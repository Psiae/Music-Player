package com.kylentt.mediaplayer.data.repository

import android.content.Context
import com.kylentt.mediaplayer.app.AppScope
import com.kylentt.mediaplayer.data.source.local.MediaStoreSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepoModule {

  @Provides
  @Singleton
  fun provideProtoRepository(
    @ApplicationContext context: Context,
    coroutineScope: AppScope
  ) = ProtoRepositoryImpl(context, coroutineScope)

  @Provides
  @Singleton
  fun provideMediaRepository(
    @ApplicationContext context: Context,
    coroutineScope: AppScope,
    mediaStoreSource: MediaStoreSource
  ) = MediaRepositoryImpl(context, mediaStoreSource)


}
