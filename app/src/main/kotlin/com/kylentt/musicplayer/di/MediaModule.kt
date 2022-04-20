package com.kylentt.musicplayer.di

import android.content.Context
import com.kylentt.mediaplayer.app.AppScope
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.data.repository.MediaRepositoryImpl
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.data.repository.ProtoRepositoryImpl
import com.kylentt.musicplayer.domain.mediasession.MediaIntentHandler
import com.kylentt.musicplayer.domain.mediasession.MediaSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

  @Singleton
  @Provides
  internal fun provideSessionManager(
    @ApplicationContext context: Context,
    scope: AppScope
  ) = MediaSessionManager.build(context, scope)

  @Singleton
  @Provides
  internal fun provideMediaIntentHandler(
    @ApplicationContext context: Context,
    manager: MediaSessionManager,
    mediaRepo: MediaRepository,
    proto: ProtoRepository
  ) = MediaIntentHandler(context, manager, mediaRepo, proto)

}
