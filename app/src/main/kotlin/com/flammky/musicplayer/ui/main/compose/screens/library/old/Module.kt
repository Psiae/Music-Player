package com.flammky.musicplayer.ui.main.compose.screens.library.old

import androidx.media3.common.MediaItem
import com.flammky.android.medialib.temp.api.player.MediaController
import com.flammky.musicplayer.domain.musiclib.core.MusicLibrary
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.guava.await
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {

	@Provides
	@Singleton
	fun provideInteractor(): LibraryViewModelOld.SessionInteractor = SessionInteractor()

	private class SessionInteractor : LibraryViewModelOld.SessionInteractor {
		private val player: MediaController
			get() = MusicLibrary.api.localAgent.session.player

		override suspend fun getAllMediaItems(): List<MediaItem> {
			return player.joinSuspend { getAllMediaItems() }
		}

		override suspend fun removeMediaItem(item: MediaItem) {
			player.joinSuspend { removeMediaItem(item) }
		}

		override suspend fun removeMediaItems(items: List<MediaItem>) {
			player.joinSuspend { removeMediaItems(items) }
		}

		override suspend fun pause() {
			player.joinSuspend { pause() }
		}

		override suspend fun play(model: LibraryViewModelOld.LocalSongModel) {
			player.joinSuspend {
				if (!player.connected) player.connectService().await()
				player.play(model.mediaItem)
			}
		}
	}
}
