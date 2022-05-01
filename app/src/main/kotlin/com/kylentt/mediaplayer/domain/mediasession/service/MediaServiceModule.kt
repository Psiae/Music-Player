package com.kylentt.mediaplayer.domain.mediasession.service

import android.content.Context
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.CONTENT_TYPE_MUSIC
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

/**
 * Hilt Module Providing [MediaService] dependencies
 */

@Module
@InstallIn(ServiceComponent::class)
object MediaServiceModule {

  @ServiceScoped
  @Provides
  fun provideAudioAttributes(): AudioAttributes {
    return AudioAttributes
      .Builder()
      .setContentType(CONTENT_TYPE_MUSIC)
      .setUsage(USAGE_MEDIA)
      .build()
  }

  @ServiceScoped
  @Provides
  fun provideExoplayer(
    @ApplicationContext context: Context,
    audioAttr: AudioAttributes
  ): ExoPlayer {
    return ExoPlayer
      .Builder(context)
      .setAudioAttributes(audioAttr, true)
      .setHandleAudioBecomingNoisy(true)
      .setLooper(Looper.getMainLooper())
      .build()
  }

}
