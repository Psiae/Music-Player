package com.kylentt.mediaplayer.di

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.*
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MediaSourceFactory
import coil.Coil
import coil.ImageLoader
import com.kylentt.mediaplayer.core.util.CoilHandler
import com.kylentt.mediaplayer.core.util.MediaItemHandler
import com.kylentt.mediaplayer.data.repository.SongRepositoryImpl
import com.kylentt.mediaplayer.data.source.local.LocalSourceImpl
import com.kylentt.mediaplayer.domain.presenter.ServiceConnectorImpl
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
        source: LocalSourceImpl,
        @ApplicationContext context: Context,
        coil: ImageLoader
    ) = SongRepositoryImpl(source, context, coil)

    @Singleton
    @Provides
    fun provideCoil(
        @ApplicationContext context: Context
    ) = Coil.imageLoader(context = context)

    @Singleton
    @Provides
    fun provideMTR() = MediaMetadataRetriever()

    @Singleton
    @Provides
    fun provideMediaItemHandler(
        @ApplicationContext context: Context,
    ) = MediaItemHandler(context)

    @Singleton
    @Provides
    fun provideCoilHandler(
        @ApplicationContext context: Context,
        coil: ImageLoader
    ) = CoilHandler(context, coil)

}

@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

    // Won't be Singleton in case it released for some reason

    @ServiceScoped
    @Provides
    fun provideAudioAttr() = AudioAttributes.Builder()
        .setContentType(CONTENT_TYPE_MUSIC)
        .setUsage(USAGE_MEDIA)
        .build()

    @ServiceScoped
    @Provides
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        attr: AudioAttributes
    ) = ExoPlayer.Builder(context)
        .setAudioAttributes(attr, true)
        .setHandleAudioBecomingNoisy(true)
        .build()
}
