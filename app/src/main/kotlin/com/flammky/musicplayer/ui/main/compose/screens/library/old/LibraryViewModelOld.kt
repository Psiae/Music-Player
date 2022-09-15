package com.flammky.musicplayer.ui.main.compose.screens.library.old

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.flammky.android.app.AppDelegate
import com.flammky.android.medialib.temp.MediaLibrary
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.MediaStoreAudioEntity
import com.flammky.android.common.kotlin.coroutines.AndroidCoroutineDispatchers
import com.flammky.common.media.audio.AudioFile
import com.flammky.musicplayer.common.android.bitmap.bitmapfactory.BitmapSampler
import com.flammky.android.medialib.temp.api.provider.mediastore.MediaStoreProvider
import com.flammky.musicplayer.domain.musiclib.media3.mediaitem.MediaItemPropertyHelper.mediaUri
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@HiltViewModel
class LibraryViewModelOld @Inject constructor(
	@ApplicationContext val context: Context,
	private val dispatchers: AndroidCoroutineDispatchers,
	private val mediaStore: MediaStoreProvider,
	private val artworkProvider: ArtworkProvider,
	private val sessionInteractor: SessionInteractor
) : ViewModel() {

	private var mRefreshing = mutableStateOf(false)

	private val cacheManager = AppDelegate.cacheManager

	private val _localSongModels = mutableStateListOf<LocalSongModel>()

	val localSongs: SnapshotStateList<LocalSongModel> get() = _localSongModels

	val refreshing: State<Boolean> get() = mRefreshing

	private val onContentChangeListener = MediaStoreProvider.OnContentChangedListener() { uris, flag ->
		if (flag == MediaStoreProvider.OnContentChangedListener.Flags.DELETE) {
			viewModelScope.launch(dispatchers.main) {
				val toRemove = mutableListOf<MediaItem>()
				val items = sessionInteractor.getAllMediaItems()

				uris.forEach { uri ->
					items.find { it.mediaUri == uri }?.let { toRemove.add(it) }
				}

				sessionInteractor.removeMediaItems(toRemove)
				sessionInteractor.pause()
			}
		}
		requestRefresh()
	}

	init {
		mediaStore.audio.registerOnContentChanged(onContentChangeListener)
	}

	fun playSong(local: LocalSongModel) = viewModelScope.launch { sessionInteractor.play(local) }

	fun validateLocalSongs(): Unit {

		viewModelScope.launch(dispatchers.io) {

			val get = mediaStore.audio.query()
				.filter { it.metadataInfo.durationMs > 0 }

			val local = _localSongModels

			if (get.size != local.size) {
				internalRefreshLocalSongs(get)
				return@launch
			}

			get.forEach { song ->
				local.find { it.id == song.uid }
					?: run {
						internalRefreshLocalSongs( get)
						return@launch
					}
			}
		}
	}

	private var refreshAvailable = true

	fun requestRefresh() = viewModelScope.launch(dispatchers.mainImmediate) {
		if (refreshAvailable) {
			refreshAvailable = false

			mRefreshing.value = true

			val took = measureTimeMillis { internalRefreshLocalSongs() }
			if (took < 500) delay(500 - took)

			mRefreshing.value = false

			refreshAvailable = true
		}
	}

	private suspend fun internalRefreshLocalSongs(
		maybeSongs: List<MediaStoreAudioEntity>? = null
	) {
		withContext(dispatchers.io) {

			val songs = (maybeSongs ?: mediaStore.audio.query()).filter {
				it.metadataInfo.durationMs > 0
			}

			val factory = mediaStore.audio.mediaItemFactory

			val models = songs.map {
				LocalSongModel(it.uid, it.metadataInfo.title, factory.createMediaItem(it))
			}

			_localSongModels.removeAll { true }
			_localSongModels.addAll(models)

			val api = MediaLibrary.API
			val lru = api.imageRepository.sharedBitmapLru

			val allCachedFile = cacheManager.retrieveAllImageCacheFile(Bitmap::class,"LibraryViewModel")

			val toAwait = mutableListOf<Deferred<Triple<File?, String?, Pair<String, Bitmap>?>>>()

			songs.forEachIndexed { index, song ->

				val deferred: Deferred<Triple<File?, String?, Pair<String, Bitmap>?>> = viewModelScope.async(dispatchers.io) {

					val usedFile: File?
					val lruIdToRemove: String?
					var lruElementToPut: Pair<String, Bitmap>? = null

					val id = song.uid

					// Todo: Constants
					val cachedFile = cacheManager.retrieveImageCacheFile(song.uid + song.fileInfo.dateModified, "LibraryViewModel")

					if (cachedFile != null) {

						usedFile = cachedFile

						val req = ArtworkProvider.Request
							.Builder(id, Bitmap::class.java)
							.setMinimumHeight(250)
							.setMinimumWidth(250)
							.setMemoryCacheAllowed(true)
							.setDiskCacheAllowed(true)
							.build()

						val provided: Bitmap? = artworkProvider.request(req).await().get()

						val cachedLru = id to provided

						if (cachedLru.second != null) {
							models[index].updateArtwork(cachedLru.second)
						} else {
							BitmapFactory.decodeFile(cachedFile.absolutePath)?.let {
								models[index].updateArtwork(it)
								lruElementToPut = id to it
							}
						}
						return@async Triple(usedFile, null, lruElementToPut)
					}

					lruIdToRemove = id

					val embed: Any? = AudioFile.Builder(
						context,
						song.uri
					).build().run { file?.delete()
						val data = imageData

						if (data == null || data.isEmpty()) {
							return@run null
						}

						BitmapSampler.ByteArray.toSampledBitmap(data, 0, data.size, 2000000)?.let {
							val reg = cacheManager.registerImageToCache(it, song.uid + song.fileInfo.dateModified, "LibraryViewModel")
							models[index].mediaItem.mediaMetadata.extras?.putString("cachedArtwork", reg.absolutePath)
							lruElementToPut = id to it
							it
						}
					}
					models[index].updateArtwork(embed)
					Triple(null, lruIdToRemove, lruElementToPut)
				}
				toAwait.add(deferred)
			}

			val result = toAwait.map { it.await() }

			Timber.d("LibraryViewModel InternalRefresh Awaited ${result.size} of ${songs.size}")

			result.mapNotNull { it.second }.forEach { lru.remove(it) }
			result.mapNotNull { it.third }.forEach { lru.put(it.first, it.second) }

			allCachedFile
				.filter { it !in result.map { r -> r.first } }
				.forEach {
					if (it.exists()) {
						Timber.d("Deleting $it from cache")
						it.delete()
					}
				}
		}
	}

	override fun onCleared() {
		mediaStore.audio.unregisterOnContentChanged(onContentChangeListener)
	}

	data class LocalSongModel(
		val id: String,
		val displayName: String,
		val mediaItem: MediaItem
	) {
		private object NO_ART

		private val mLoadedState = mutableStateOf<Any?>(null)
		private val mLoadingState = mutableStateOf(false)

		val artState: State<Any?>
			get() = mLoadedState

		val isArtLoaded
			get() = artState.value !== null

		val noArt: Boolean
			get() = artState.value === NO_ART

		fun updateArtwork(art: Any?) {
			Timber.d("LocalSongModel $displayName update art: $art")
			mLoadedState.value = art ?: NO_ART
		}
	}

	interface SessionInteractor {
		val currentMediaItem: MediaItem?

		fun getAllMediaItems(): List<MediaItem>
		fun removeMediaItem(item: MediaItem)
		fun removeMediaItems(items: List<MediaItem>)
		fun pause()
		suspend fun play(model: LocalSongModel)
	}
}
