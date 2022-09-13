package com.kylentt.musicplayer.ui.main.compose.screens.library.old

import com.flammky.android.medialib.temp.api.player.MediaController
import com.kylentt.musicplayer.domain.musiclib.core.MusicLibrary
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

		override suspend fun play(model: LibraryViewModelOld.LocalSongModel) {
			if (!player.connected) player.connectService().await()
			player.play(model.mediaItem)
		}
	}
}
