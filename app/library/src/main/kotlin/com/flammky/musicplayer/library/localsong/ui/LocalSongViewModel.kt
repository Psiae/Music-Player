package com.flammky.musicplayer.library.localsong.ui

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.library.localsong.data.LocalSongModel
import com.flammky.musicplayer.library.localsong.data.LocalSongRepository
import com.flammky.musicplayer.library.localsong.domain.ObservableLocalSongs
import com.flammky.musicplayer.library.media.MediaConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class LocalSongViewModel @Inject constructor(
	private val dispatcher: AndroidCoroutineDispatchers,
	private val mediaConnection: MediaConnection,
	private val repository: LocalSongRepository,
	private val observableLocalSongs: ObservableLocalSongs,
) : ViewModel() {

	private val _refreshing = mutableStateOf(false)
	val refreshing = _refreshing.derive()

	private val _listState = mutableStateOf<ImmutableList<LocalSongModel>>(persistentListOf())
	val listState = _listState.derive()

	private val _loaded = mutableStateOf(false)
	val loadedState = _loaded.derive()

	init {
		viewModelScope.launch {
			observableLocalSongs.refresh().join()
			_loaded.overwrite(true)
			observableLocalSongs.collectLocalSongs().first().let {
				_listState.overwrite(it)
				it.forEach { model -> observableLocalSongs.refreshMetadata(model) }
			}
			viewModelScope.launch {
				observableLocalSongs.collectLocalSongs().safeCollect {
					_listState.overwrite(it)
				}
			}
			viewModelScope.launch {
				observableLocalSongs.collectRefresh().safeCollect {
					_refreshing.overwrite(it)
				}
			}
		}
	}

	fun refresh() {
		viewModelScope.launch { observableLocalSongs.refresh() }
	}

	override fun onCleared() {
		// t
	}

	fun play(model: LocalSongModel) {
		mediaConnection.play(model.id, model.mediaItem.mediaUri)
	}

	suspend fun collectArtwork(model: LocalSongModel): Flow<Bitmap?> {
		return observableLocalSongs.observeArtwork(model.id).map { it as Bitmap? }
	}

	suspend fun collectMetadata(model: LocalSongModel): Flow<AudioMetadata?> {
		return repository.collectMetadata(model.id)
	}

	// is explicit write like this better ?
	@Suppress("NOTHING_TO_INLINE")
	private inline fun <T> MutableState<T>.overwrite(value: T) {
		this.value = value
	}

	private fun <T> State<T>.derive(): State<T> = derive { it }

	private fun <T> State<T>.derive(calculation: (T) -> T): State<T> {
		return derivedStateOf { calculation(value) }
	}
}


