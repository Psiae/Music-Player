package com.flammky.musicplayer.dump.mediaplayer.domain.musiclib

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.flammky.musicplayer.base.media.r.MediaMetadataCacheRepository
import com.flammky.musicplayer.domain.musiclib.player.exoplayer.ExoPlayerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MusicLibraryModule {

	@Provides
	@Singleton
	fun provideTestArtworkProvider(
		@ApplicationContext context: Context,
		repository: MediaMetadataCacheRepository
	): com.flammky.android.medialib.temp.image.ArtworkProvider {
		return com.flammky.android.medialib.temp.image.internal.TestArtworkProvider(
			context,
			repository
		)
	}
}

@Module
@InstallIn(ServiceComponent::class)
object MusicLibraryServiceModule {
	const val handleSelfAudioReroute = true
	const val handleSelfAudioFocus = true

	@Provides
	@Named("MusicExoPlayer")
	fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer {
		return ExoPlayerFactory
			.createMusicExoPlayer(context, handleSelfAudioFocus, handleSelfAudioReroute)
	}
}
