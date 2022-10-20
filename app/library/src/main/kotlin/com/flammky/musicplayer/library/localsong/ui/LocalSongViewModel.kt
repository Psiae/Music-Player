package com.flammky.musicplayer.library.localsong.ui

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.MediaLib
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.MediaLibrary
import com.flammky.musicplayer.library.localsong.data.LocalSongModel
import com.flammky.musicplayer.library.localsong.data.LocalSongRepository
import com.flammky.musicplayer.library.media.MediaConnection
import com.flammky.musicplayer.library.util.rewrite
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class LocalSongViewModel @Inject constructor(
	private val dispatcher: AndroidCoroutineDispatchers,
	private val mediaConnection: MediaConnection,
	private val repository: LocalSongRepository,
) : ViewModel() {

	private val contentObserver = MediaStoreProvider.ContentObserver { id: String, _, _ ->
		scheduleRefresh(id)
	}

	private val mediaController = MediaLibrary.API.sessions.manager.findSessionById("DEBUG")!!.mediaController

	private val _refreshing = mutableStateOf(false)
	val refreshing = _refreshing.derive()

	private val _listState = mutableStateOf<List<LocalSongModel>>(emptyList())
	val listState = _listState.derive()

	// we could instead expose flow from repository and do scheduling there
	private val scheduledRefresh = mutableListOf<Any?>()

	init {
		MediaLib.singleton.mediaProviders.mediaStore.audio.observe(contentObserver)
		refresh()
	}

	override fun onCleared() {
		MediaLib.singleton.mediaProviders.mediaStore.audio.removeObserver(contentObserver)
	}

	// we should rescan
	fun refresh() {
		scheduleRefresh()
	}

	private fun scheduleRefresh(id: String? = null) {
		fun refreshing(): Boolean {
			return _refreshing.value
		}
		fun markRefreshing(refreshing: Boolean) {
			_refreshing.value = refreshing
		}
		viewModelScope.launch {
			scheduledRefresh.add(id)
			if (!refreshing()) {
				markRefreshing(true)
				doScheduledRefresh(id)
				markRefreshing(false)
			}
		}
	}

	private suspend fun doScheduledRefresh(id: String? = null) {
		withContext(dispatcher.mainImmediate) {
			if (id == null) {
				while (scheduledRefresh.isNotEmpty()) {
					val size = scheduledRefresh.size
					_listState.overwrite(repository.requestUpdateAsync().await())
					scheduledRefresh.drop(size).let {
						scheduledRefresh.clear()
						scheduledRefresh.addAll(it)
					}
				}
			} else {
				_listState.rewrite { old ->
					repository.getModelAsync(id).await()
						// there's an update
						?.let { get -> old.map { if (it.id == id) get else it } }
						// deleted
						?: old.filter { it.id != id }
				}
				scheduledRefresh.filter { it != id }.let {
					scheduledRefresh.clear()
					scheduledRefresh.addAll(it)
				}
			}
		}
	}

	fun play(model: LocalSongModel) {
		mediaConnection.play(model.id, model.mediaItem.mediaUri)
	}

	suspend fun collectArtwork(model: LocalSongModel): Flow<Bitmap?> {
		return repository.collectArtwork(model)
	}

	// is explicit write like this better ?
	@Suppress("NOTHING_TO_INLINE")
	private inline fun <T> MutableState<T>.overwrite(value: T) {
		this.value = value
	}

	private fun <T> State<T>.derive(calculation: (T) -> T = { it }): State<T> {
		return derivedStateOf { calculation(value) }
	}
}


