package com.kylentt.musicplayer.di

import android.content.Context
import coil.Coil
import coil.ImageLoader
import com.kylentt.mediaplayer.app.AppDispatchers
import com.kylentt.mediaplayer.app.AppScope
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.data.repository.MediaRepositoryImpl
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.data.repository.ProtoRepositoryImpl
import com.kylentt.mediaplayer.data.source.local.MediaStoreSource
import com.kylentt.mediaplayer.data.source.local.MediaStoreSourceImpl
import com.kylentt.mediaplayer.disposed.core.util.handler.CoilHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Singleton
  @Provides
  fun provideCoilInstance(@ApplicationContext context: Context) =
    Coil.imageLoader(context)

  @Singleton
  @Provides
  fun provideCoilHandler(@ApplicationContext context: Context, coil: ImageLoader) =
    CoilHandler(context, coil)

  @Singleton
  @Provides
  fun provideMediaStoreSource(
    @ApplicationContext context: Context,
    dispatchers: AppDispatchers
  ): MediaStoreSource = MediaStoreSourceImpl(context, dispatchers)

  @Singleton
  @Provides
  fun provideMediaRepository(
    @ApplicationContext context: Context,
    ms: MediaStoreSource
  ): MediaRepository = MediaRepositoryImpl(context, ms)

  @Singleton
  @Provides
  fun provideProtoRepository(
    @ApplicationContext context: Context,
    scope: AppScope
  ): ProtoRepository = ProtoRepositoryImpl(context, scope)

  @Singleton
  @Provides
  fun provideAppDispatchers() =
    AppDispatchers.Default

  @Singleton
  @Provides
  fun provideAppScope(dispatchers: AppDispatchers) =
    AppScope.fromAppDispatchers(dispatchers)
}
