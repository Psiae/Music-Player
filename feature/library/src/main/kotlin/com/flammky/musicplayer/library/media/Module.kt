package com.flammky.musicplayer.library.media

import android.content.Context
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.MediaLib
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.r.MediaConnectionDelegate
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@dagger.Module
@dagger.hilt.InstallIn(SingletonComponent::class)
internal object Module {

	@Provides
	@Singleton
	fun provideMediaConnection(
		artworkProvider: ArtworkProvider,
		@ApplicationContext context: Context,
		delegate: MediaConnectionDelegate,
		dispatchers: AndroidCoroutineDispatchers,
		playbackConnection: PlaybackConnection,
	): MediaConnection {
		return RealMediaConnection(
			artworkProvider,
			context,
			dispatchers,
			MediaLib.singleton(context),
			delegate.repository,
			playbackConnection
		)
	}
}
