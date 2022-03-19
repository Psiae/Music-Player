package com.kylentt.mediaplayer.domain.mediaSession

import android.content.Context
import com.kylentt.mediaplayer.core.exoplayer.util.ExoUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Singleton
    @Provides
    fun provideMediaSessionManager(
        @ApplicationContext context: Context
    ) = MediaSessionManager(context)

}

@Module
@InstallIn(ServiceComponent::class)
object MediaServiceModule {

    @ServiceScoped
    @Provides
    fun provideExoPlayer(
        @ApplicationContext context: Context
    ) = with(ExoUtil) { newExo(context, defaultAttr()) }

}