package com.kylentt.mediaplayer.domain.mediasession.libraryservice

import android.content.Context
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

	const val handleAudioFocus = true
	const val handleAudioReroute = true

	@ServiceScoped
	@Provides
	fun provideExoplayer(@ApplicationContext context: Context): ExoPlayer {
		return ExoPlayerFactory.createMusicExoPlayer(
			context, handleAudioFocus = handleAudioFocus, handleAudioReroute = handleAudioReroute
		)
	}
}
