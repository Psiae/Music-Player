package com.kylentt.musicplayer.ui.main.compose.screens.library

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kylentt.musicplayer.common.android.bitmap.bitmapfactory.BitmapSampler
import com.kylentt.musicplayer.common.coroutines.CoroutineDispatchers
import com.kylentt.musicplayer.common.media.audio.AudioFile
import com.kylentt.musicplayer.core.app.AppDelegate
import com.kylentt.musicplayer.domain.musiclib.core.MusicLibrary
import com.kylentt.musicplayer.domain.musiclib.source.mediastore.MediaStoreProvider
import com.kylentt.musicplayer.domain.musiclib.entity.AudioEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
	@ApplicationContext val context: Context,
	private val dispatchers: CoroutineDispatchers,
	private val mediaStoreSource: MediaStoreProvider
) : ViewModel() {
	private val player
		get() = MusicLibrary.localAgent.session.player

	private var mediaStoreSongs: List<AudioEntity> = emptyList()

	private var mRefreshing = mutableStateOf(false)

	private val cacheManager = AppDelegate.cacheManager

	private val _localSongModels = mutableStateOf<List<LocalSongModel>>(emptyList(), neverEqualPolicy())

	val refreshing: State<Boolean> get() = mRefreshing
	val localSongs: State<List<LocalSongModel>> get() = _localSongModels


	fun playSong(local: LocalSongModel) {
		mediaStoreSongs.find { it.uid == local.id }?.let {
			player.play(it.mediaItem)
		}
	}

	fun validateLocalSongs(): Unit {
		viewModelScope.launch(dispatchers.io) {


			val get = mediaStoreSource
				.queryAudioEntity(true, true)
				.filter { it.fileInfo.metadata.playable }

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
		maybeSongs: List<AudioEntity>? = null
	) = withContext(dispatchers.io) {

		val songs = (maybeSongs ?: mediaStoreSource.queryAudioEntity(true, true))
			.filter { it.fileInfo.metadata.playable }

		withContext(dispatchers.main) {
			mediaStoreSongs = songs
			_localSongModels.value = songs.map { LocalSongModel(it.uid, it.fileInfo.metadata.title) }
		}

		val toRemove = cacheManager.retrieveAllImageCacheFile(Bitmap::class,"LibraryViewModel").toMutableList()

		val deff = async {

			songs.forEach { song ->

				launch {

					// Todo: Constants
					val cached = cacheManager.retrieveImageCacheFile(song.uid + song.fileInfo.dateModified, "LibraryViewModel")

					if (cached != null) {

						toRemove.remove(cached)
						song.mediaItem.mediaMetadata.extras?.putString("cachedArtwork", cached.absolutePath)

						_localSongModels.value.forEach { if (it.id == song.uid) it.updateArtwork(cached) }

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
							song.mediaItem.mediaMetadata.extras?.putString("cachedArtwork", reg.absolutePath)
							it
						}
					}

					_localSongModels.value.forEach { if (it.id == song.uid) it.updateArtwork(embed) }
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

		private val mState = mutableStateOf<Any?>(null)

		val artState: State<Any?>
			get() = mState

		val isArtLoaded
			get() = artState.value !== null

		fun updateArtwork(art: Any?) {
			mState.value = art ?: NO_ART
		}
	}
}
