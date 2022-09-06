package com.kylentt.musicplayer.ui.main.compose.screens.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.kylentt.musicplayer.common.android.bitmap.bitmapfactory.BitmapSampler
import com.kylentt.musicplayer.common.coroutines.CoroutineDispatchers
import com.kylentt.musicplayer.common.media.audio.AudioFile
import com.kylentt.musicplayer.core.app.AppDelegate
import com.kylentt.musicplayer.domain.musiclib.core.MusicLibrary
import com.kylentt.musicplayer.medialib.MediaLibrary
import com.kylentt.musicplayer.medialib.api.MediaLibraryAPI
import com.kylentt.musicplayer.medialib.api.provider.mediastore.MediaStoreProvider
import com.kylentt.musicplayer.medialib.internal.provider.mediastore.base.audio.MediaStoreAudioEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
	@ApplicationContext val context: Context,
	private val dispatchers: CoroutineDispatchers,
	private val mediaStoreSource: MediaStoreProvider
) : ViewModel() {

	private val player
		get() = MusicLibrary.api.localAgent.session.player

	private var mediaStoreSongs: List<MediaStoreAudioEntity> = emptyList()
	private var mediaItemList: List<MediaItem> = emptyList()

	private var mRefreshing = mutableStateOf(false)

	private val cacheManager = AppDelegate.cacheManager

	private val _localSongModels = mutableStateOf<List<LocalSongModel>>(emptyList(), neverEqualPolicy())

	val refreshing: State<Boolean> get() = mRefreshing
	val localSongs: State<List<LocalSongModel>> get() = _localSongModels


	fun playSong(local: LocalSongModel) {
		mediaItemList.find { it.mediaId == local.id }?.let {
			player.setMediaItems(listOf(it))
			player.prepare()
			player.play()
		}
	}

	fun validateLocalSongs(): Unit {

		viewModelScope.launch(dispatchers.io) {

			val get = mediaStoreSource
				.audioProvider
				.queryEntity()
				.filter { it.metadataInfo.durationMs > 0 }

			val local = _localSongModels.value

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

	fun requestRefresh() = viewModelScope.launch(dispatchers.io) {
		if (!mRefreshing.value) {
			mRefreshing.value = true
			internalRefreshLocalSongs()
			mRefreshing.value = false
		}
	}

	private suspend fun internalRefreshLocalSongs(
		maybeSongs: List<MediaStoreAudioEntity>? = null
	) = withContext(dispatchers.io) {

		val songs = (maybeSongs ?: mediaStoreSource.audioProvider.queryEntity()).filter {
			it.metadataInfo.durationMs > 0 }
		val mediaItems = songs.map {
			val factory = mediaStoreSource.audioProvider.mediaItemFactory
			factory.createMediaItem(it)
		}

		withContext(dispatchers.main) {
			mediaStoreSongs = songs
			mediaItemList = mediaItems
			_localSongModels.value = songs.map { LocalSongModel(it.uid, it.metadataInfo.title) }
		}

		val toRemove = cacheManager.retrieveAllImageCacheFile(Bitmap::class,"LibraryViewModel").toMutableList()

		val deff = async {

			songs.forEachIndexed { index, song ->

				viewModelScope.launch(dispatchers.io) {

					// Todo: Constants
					val cached = cacheManager.retrieveImageCacheFile(song.uid + song.fileInfo.dateModified, "LibraryViewModel")

					if (cached != null) {

						toRemove.remove(cached)
						mediaItems[index].mediaMetadata.extras?.putString("cachedArtwork", cached.absolutePath)

						BitmapFactory.decodeFile(cached.absolutePath)?.let {
							_localSongModels.value[index].updateArtwork(cached)
						}

						return@launch
					}


					val embed: Any? = AudioFile.Builder(
						context,
						song.uri
					).build().run { file?.delete()
						val data = imageData

						if (data == null || data.isEmpty()) {
							return@run null
						}

						BitmapSampler.ByteArray.toSampledBitmap(data, 0, data.size, 1000000)?.let {
							val reg = cacheManager.registerImageToCache(it, song.fileInfo.fileName + song.fileInfo.dateModified, "LibraryViewModel")
							mediaItems[index].mediaMetadata.extras?.putString("cachedArtwork", reg.absolutePath)
							it
						}
					}

					_localSongModels.value[index].updateArtwork(embed)
				}
			}
		}

		deff.await()
		toRemove.forEach { if (it.exists()) it.delete() }
	}

	data class LocalSongModel(
		val id: String,
		val displayName: String
	) {
		private object NO_ART

		private val mLoadedState = mutableStateOf<Any?>(null)
		private val mLoadingState = mutableStateOf(false)

		val artState: State<Any?>
			get() = mLoadedState

		val isArtLoaded
			get() = artState.value !== null
		fun updateArtwork(art: Any?) {
			mLoadedState.value = art ?: NO_ART
		}
	}
}
