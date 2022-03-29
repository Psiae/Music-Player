package com.kylentt.mediaplayer.di

import android.content.Context
import coil.Coil
import coil.ImageLoader
import com.kylentt.mediaplayer.core.util.handler.CoilHandler
import com.kylentt.mediaplayer.core.util.handler.IntentHandler
import com.kylentt.mediaplayer.core.util.handler.MediaItemHandler
import com.kylentt.mediaplayer.disposed.data.repository.SongRepositoryImpl
import com.kylentt.mediaplayer.disposed.data.source.local.LocalSourceImpl
import com.kylentt.mediaplayer.disposed.domain.presenter.ServiceConnectorImpl
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
    fun provideServiceConnector(
        @ApplicationContext context: Context,
        coil: ImageLoader
    ) = ServiceConnectorImpl(context, coil)

    @Singleton
    @Provides
    fun provideLocalSource(
        @ApplicationContext context: Context,
        coil: ImageLoader
    ) = LocalSourceImpl(context, coil)

    @Singleton
    @Provides
    fun provideSongRepository(
        @ApplicationContext context: Context,
        source: LocalSourceImpl
    ) = SongRepositoryImpl(source, context)

}

@Module
@InstallIn(SingletonComponent::class)
object SingletonProviderModule { /* TODO() */}

@Module
@InstallIn(SingletonComponent::class)
object SingletonComponentModule {

    @Singleton
    @Provides
    fun provideCoil(
        @ApplicationContext context: Context
    ) = Coil.imageLoader(context = context)

}

@Module
@InstallIn(SingletonComponent::class)
object SingletonHandlerModule {

    @Singleton
    @Provides
    fun provideCoilHandler(
        @ApplicationContext context: Context,
        coil: ImageLoader
    ) = CoilHandler(context, coil)

    @Singleton
    @Provides
    fun provideIntentHandler(
        @ApplicationContext context: Context
    ) = IntentHandler(context)

    @Singleton
    @Provides
    fun provideMediaItemHandler(
        @ApplicationContext context: Context,
    ) = MediaItemHandler(context)

}
