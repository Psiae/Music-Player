package com.kylentt.mediaplayer.domain.mediasession.service

import android.content.Context
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.CONTENT_TYPE_MUSIC
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.exoplayer.ExoPlayer
import com.kylentt.mediaplayer.core.media3.ExoPlayerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

/**
 * Hilt Module Providing [MusicLibraryServiceModule] dependencies
 */

@Module
@InstallIn(ServiceComponent::class)
object MusicLibraryServiceModule {

	@ServiceScoped
	@Provides
	fun provideExoplayer(@ApplicationContext context: Context): ExoPlayer {
		return ExoPlayerFactory.createMusicExoPlayer(
			context, handleAudioFocus = true, handleAudioReroute = true
		)
	}
}
