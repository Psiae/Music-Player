package com.kylentt.musicplayer.ui.main.compose.screens.library


import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.kylentt.musicplayer.domain.session.MediaSession
import com.flammky.android.medialib.temp.image.ImageProvider
import com.flammky.android.medialib.temp.api.provider.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.player.LibraryPlayer
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

class LibraryViewModel constructor(
	private val session: SessionInteractor,
	private val artworkProvider: ImageProvider,
	private val mediaStore: MediaStoreProvider
) : ViewModel() {

	private val _localSongs: SnapshotStateList<LocalSongModel> = mutableStateListOf()
	val localSongsState: SnapshotStateList<LocalSongModel> = _localSongs

	fun playSong(model: LocalSongModel) = viewModelScope.launch { session.play(model) }

	interface SessionInteractor {
		suspend fun play(model: LocalSongModel)
	}

	interface MediaProvider {
		suspend fun getLocalSongs(): Deferred<LocalSongModel>
	}
}
