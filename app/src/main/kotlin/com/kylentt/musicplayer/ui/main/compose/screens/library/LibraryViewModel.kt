package com.kylentt.musicplayer.ui.main.compose.screens.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kylentt.mediaplayer.data.source.local.MediaStoreSong
import com.kylentt.mediaplayer.data.source.local.MediaStoreSource
import com.kylentt.musicplayer.common.android.bitmap.bitmapfactory.BitmapSampler
import com.kylentt.musicplayer.common.coroutines.CoroutineDispatchers
import com.kylentt.musicplayer.common.media.audio.AudioFile
import com.kylentt.musicplayer.core.app.AppDelegate
import com.kylentt.musicplayer.domain.musiclib.MusicLibrary
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
	@ApplicationContext val context: Context,
	private val dispatchers: CoroutineDispatchers,
	private val mediaStoreSource: MediaStoreSource
) : ViewModel() {
	private val player
		get() = MusicLibrary.localAgent.session.player

	private var mediaStoreSongs: List<MediaStoreSong> = emptyList()

	private var mRefreshing = mutableStateOf(false)

	private val cacheManager = AppDelegate.cacheManager

	private val _localSongModels = mutableStateOf<List<LocalSongModel>>(emptyList(), neverEqualPolicy())

	val refreshing: State<Boolean> get() = mRefreshing
	val localSongs: State<List<LocalSongModel>> get() = _localSongModels


	fun playSong(local: LocalSongModel) {
		mediaStoreSongs.find { it.songMediaId == local.id }?.let {
			player.play(it.mediaItem)
		}
	}

	fun validateLocalSongs(): Unit {
		viewModelScope.launch(dispatchers.io) {
			val get = mediaStoreSource.getMediaStoreSong()
			val local = _localSongModels.value

			if (get.size != local.size) {
				internalRefreshLocalSongs(false, get)
				return@launch
			}

			get.forEach { song ->
				local.find { it.id == song.songMediaId }
					?: run {
						internalRefreshLocalSongs(false, get)
						return@launch
					}
			}
		}
	}

	fun requestRefresh() = viewModelScope.launch(dispatchers.io) {
		mRefreshing.value = true
		internalRefreshLocalSongs(true)
		mRefreshing.value = false
	}

	private suspend fun internalRefreshLocalSongs(
		isRequest: Boolean,
		maybeSongs: List<MediaStoreSong>? = null
	) = withContext(dispatchers.io) {

		val songs = maybeSongs ?: mediaStoreSource.getMediaStoreSong()

		withContext(dispatchers.main) {
			mediaStoreSongs = songs
			_localSongModels.value = songs.map { LocalSongModel(it.songMediaId, it.title) }
		}

		val toRemove = cacheManager.retrieveAllImageCacheFile(Bitmap::class,"LibraryViewModel").toMutableList()

		val deff = async {

			songs.forEach { song ->

				launch {

					// Todo: Constants
					val cached = cacheManager.retrieveImageCacheFile(song.songMediaId + song.lastModified, "LibraryViewModel")

					if (cached != null) {

						toRemove.remove(cached)
						song.mediaItem.mediaMetadata.extras?.putString("cachedArtwork", cached.absolutePath)

						withContext(dispatchers.main) {
							_localSongModels.value.forEach { if (it.id == song.songMediaId) it.updateArtwork(cached) }
						}

						return@launch
					}


					val embed: Any? = AudioFile.Builder(
						context,
						song.songMediaUri,
						cacheManager.startupCacheDir
					).build().run { file?.delete()
						val data = imageData

						if (data == null || data.isEmpty()) {
							return@run null
						}

						BitmapSampler.ByteArray.toSampledBitmap(data, 0, data.size, 2000000)?.let {
							val reg = cacheManager.registerImageToCache(it, song.songMediaId + song.lastModified, "LibraryViewModel")
							song.mediaItem.mediaMetadata.extras?.putString("cachedArtwork", reg.absolutePath)
							it
						}
					}

					withContext(dispatchers.main) {
						_localSongModels.value.forEach { if (it.id == song.songMediaId) it.updateArtwork(embed) }
					}
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
		object NO_ART

		private val mState = mutableStateOf<Any?>(NO_ART)

		val artState: State<Any?>
			get() = mState

		val isArtLoaded
			get() = artState.value !== NO_ART

		fun updateArtwork(art: Any?) {
			Timber.d("update artwork to ${art?.javaClass}")
			mState.value = art
		}
	}
}
