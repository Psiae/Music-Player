package com.kylentt.musicplayer.di

import android.content.Context
import com.kylentt.musicplayer.data.MediaRepository
import com.kylentt.musicplayer.data.source.local.MediaStoreSource
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
    fun provideMediaStoreSource(@ApplicationContext context: Context) = MediaStoreSource(context)

    @Singleton
    @Provides
    fun provideMediaRepository(@ApplicationContext context: Context, ms: MediaStoreSource) = MediaRepository(context, ms)
}