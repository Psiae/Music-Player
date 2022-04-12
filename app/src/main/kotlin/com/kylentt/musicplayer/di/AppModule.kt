package com.kylentt.musicplayer.di

import android.content.Context
import coil.Coil
import coil.ImageLoader
import com.kylentt.mediaplayer.core.util.handler.CoilHandler
import com.kylentt.musicplayer.app.util.AppScope
import com.kylentt.musicplayer.data.repository.MediaRepository
import com.kylentt.musicplayer.data.repository.ProtoRepository
import com.kylentt.musicplayer.data.source.local.MediaStoreSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Singleton
  @Provides
  fun provideCoilInstance(@ApplicationContext context: Context) = Coil.imageLoader(context)

  @Singleton
  @Provides
  fun provideCoilHandler(@ApplicationContext context: Context, coil: ImageLoader) =
    CoilHandler(context, coil)

  @Singleton
  @Provides
  fun provideMediaStoreSource(@ApplicationContext context: Context) = MediaStoreSource(context)

  @Singleton
  @Provides
  fun provideMediaRepository(@ApplicationContext context: Context, ms: MediaStoreSource) =
    MediaRepository(context, ms)

  @Singleton
  @Provides
  fun provideProtoRepository(@ApplicationContext context: Context, scope: AppScope) =
    ProtoRepository(context, scope)

  @Singleton
  @Provides
  fun provideAppScope() = AppScope(
    defaultScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  )
}
