package com.flammky.musicplayer.ui.main.compose.screens.library.old

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.app.AppDelegate
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider.ContentObserver.Flag.Companion.isDelete
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.kotlin.common.sync.sync
import com.flammky.musicplayer.common.android.concurrent.ConcurrencyHelper.checkMainThread
import com.flammky.musicplayer.domain.musiclib.media3.mediaitem.MediaItemPropertyHelper.mediaUri
import com.flammky.musicplayer.library.localsong.data.LocalSongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LibraryViewModelOld @Inject constructor(
	@ApplicationContext val context: Context,
	private val dispatchers: AndroidCoroutineDispatchers,
	/*private val oldMediaStore: MediaStoreProvider,*/
	private val mediaStoreProvider: MediaStoreProvider,
	private val artworkProvider: ArtworkProvider,
	private val sessionInteractor: SessionInteractor,
	private val localSongRepo: LocalSongRepository
) : ViewModel() {

	private val _refreshing = mutableStateOf(false)

	private val cacheManager = AppDelegate.cacheManager

	private val _localSongModels = mutableStateOf<List<LocalSongModel>>(emptyList())
		get() {
			checkMainThread {
				"State variables should only be accessed on Main Context"
			}
			return field
		}

	val localSongs: State<List<LocalSongModel>> = _localSongModels.derive()
	val refreshing: State<Boolean> = _refreshing.derive()

	private var refreshAvailable = true
	private var refreshJob: Job = Job()

	@Deprecated("Todo: make it listen internally instead")
	private val onContentChangedListener = MediaStoreProvider.ContentObserver { id, uri, flag ->
		viewModelScope.launch {
			if (flag.isDelete) {
				val list = ArrayList(_localSongModels.value)
				list.removeAll { it.id == id }
				_localSongModels.value = list.toList()

				val toRemove = mutableListOf<androidx.media3.common.MediaItem>()
				val items = sessionInteractor.getAllMediaItems()

				items.find { it.mediaUri == uri }?.let { toRemove.add(it) }

				// maybe `notifyUnplayableMedia sound kind of nicer`
				sessionInteractor.removeMediaItems(toRemove)
			} else {
				scheduleRefresh()
			}
		}
	}

	private val scheduledRefresh = mutableListOf<Any>()

	init {
		mediaStoreProvider.audio.observe(onContentChangedListener)
		refreshSongList()
	}

	fun playSong(local: LocalSongModel) = viewModelScope.launch { sessionInteractor.play(local) }

	fun validateLocalSongs(): Unit {
	}

	suspend fun observeArtwork(model: LocalSongModel): Flow<Bitmap?> {
		return withContext(dispatchers.io) { localSongRepo.collectArtwork(model.id) }
	}

	fun scheduleRefresh() {
		viewModelScope.launch {
			scheduledRefresh.add(Any())
			if (refreshAvailable) {
				_refreshing.value = true
				while (scheduledRefresh.isNotEmpty()) {
					val size = scheduledRefresh.size
					refreshJob = refreshSongList().also { it.join() }
					scheduledRefresh.drop(size).let {
						scheduledRefresh.clear()
						scheduledRefresh.addAll(it)
					}
					Timber.d("LibraryViewModel scheduleRefresh: refreshed $size request at once")
				}
				_refreshing.value = false
			} else {
				Timber.d("LibraryViewModel scheduleRefresh: scheduled for next refresh")
			}
		}
	}

	private fun refreshSongList() = viewModelScope.launch {
		if (refreshAvailable) {
			refreshAvailable = false
			internalRefreshLocalSongs()
			refreshAvailable = true
		}
	}

	private suspend fun internalRefreshLocalSongs() {
		withContext(dispatchers.io) {

			val locals = localSongRepo.getModelsAsync().await()

			if (!isActive) return@withContext

			val models = locals.map {
				val metadata = it.mediaItem.metadata as AudioMetadata
				val displayName = metadata.title
				LocalSongModel(it.id, displayName ?: "", it.mediaItem)
			}

			withContext(dispatchers.mainImmediate) {
				_localSongModels.sync { value = models }
			}
		}
	}

	private fun <T> State<T>.derive(): State<T> {
		return derivedStateOf { value }
	}
	private fun <T> State<T>.derive(calculation: (T) -> T): State<T> {
		return derivedStateOf { calculation(value) }
	}

	override fun onCleared() {
		mediaStoreProvider.audio.removeObserver(onContentChangedListener)
	}

	data class LocalSongModel(
		val id: String,
		val displayName: String,
		val mediaItem: MediaItem
	) {
		private object NO_ART

		private val _ArtState = mutableStateOf<Any?>(null)
		private val mLoadingState = mutableStateOf(false)

		val artState: State<Any?>
			get() = _ArtState

		val isArtLoaded
			get() = artState.value !== null

		val noArt: Boolean
			get() = artState.value === NO_ART

		fun updateArtwork(art: Any?) {
			val get = _ArtState.value
			if (art is Bitmap && get is Bitmap && art.sameAs(get)) {
				return
			}
			Timber.d("LocalSongModel $displayName update art: $art")
			_ArtState.value = art ?: NO_ART
		}
	}

	interface SessionInteractor {
		suspend fun getAllMediaItems(): List<androidx.media3.common.MediaItem>
		suspend fun removeMediaItem(item: androidx.media3.common.MediaItem)
		suspend fun removeMediaItems(items: List<androidx.media3.common.MediaItem>)
		suspend fun pause()
		suspend fun play(model: LocalSongModel)
	}
}
