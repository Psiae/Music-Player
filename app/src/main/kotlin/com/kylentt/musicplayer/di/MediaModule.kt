package com.kylentt.musicplayer.di

import android.content.Context
import com.kylentt.musicplayer.app.util.AppScope
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

}