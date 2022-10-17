package com.flammky.musicplayer.library.localsong.data

import android.content.Context
import android.graphics.Bitmap
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.MediaLib
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.MediaItem.Companion.buildMediaItem
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioEntity
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.android.medialib.temp.image.internal.TestArtworkProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

class RealLocalSongRepository(
	private val context: Context,
	private val dispatchers: AndroidCoroutineDispatchers,
	private val artworkProvider: ArtworkProvider
) : LocalSongRepository {

	private val mediaLib = MediaLib.singleton(context)
	private val audioProvider = mediaLib.mediaProviders.mediaStore.audio

	private val ioScope = CoroutineScope(dispatchers.io)

	override suspend fun getModelsAsync(): Deferred<List<LocalSongModel>> =
		coroutineScope {
			async(dispatchers.io) {
				audioProvider.query().map { toLocalSongModel(it) }
			}
		}

	override suspend fun getModelAsync(id: String): Deferred<LocalSongModel?> =
		coroutineScope {
			async(dispatchers.io) {
				audioProvider.queryById(id)?.let { toLocalSongModel(it) }
			}
		}

	override suspend fun requestUpdateAsync(): Deferred<List<LocalSongModel>> =
		coroutineScope {
			async(dispatchers.io) {
				suspendCancellableCoroutine { cont ->
					audioProvider.rescan { cont.resume(it) }
				}
				getModelsAsync().await()
			}
		}

	private fun toLocalSongModel(from: MediaStoreAudioEntity): LocalSongModel {
		val metadata = AudioMetadata.build {
			val durationMs = from.metadata.durationMs
			setArtist(from.metadata.artist)
			setAlbumTitle(from.metadata.album)
			setTitle(from.metadata.title ?: from.file.fileName)
			setPlayable(if (durationMs != null && durationMs > 0) true else null)
			setDuration(durationMs?.milliseconds)
		}

		val mediaItem = mediaLib.context.buildMediaItem {
			setMediaId(from.uid)
			setMediaUri(from.uri)
			setExtra(MediaItem.Extra())
			setMetadata(metadata)
		}

		return MediaStoreLocalSongModel(from, mediaItem)
	}

	class MediaStoreLocalSongModel(
		val mediaStore: MediaStoreAudioEntity,
		mediaItem: MediaItem
	) : LocalSongModel(
		mediaStore.uid,
		mediaStore.metadata.title ?: mediaStore.metadata.title,
		mediaItem
	) {

	}

	override suspend fun collectArtwork(model: LocalSongModel): Flow<Bitmap?> {
		return collectArtwork(model.id)
	}

	override suspend fun collectArtwork(id: String): Flow<Bitmap?> {
		return callbackFlow {
			val artId = id

			suspend fun sendBitmap(cache: Boolean, storeToCache: Boolean) {
				val req = ArtworkProvider.Request
					.Builder(artId, Bitmap::class.java)
					.setMinimumHeight(1)
					.setMinimumWidth(1)
					.setMemoryCacheAllowed(cache)
					.setDiskCacheAllowed(cache)
					.setStoreMemoryCacheAllowed(storeToCache)
					.setStoreDiskCacheAllowed(storeToCache)
					.build()
				send(artworkProvider.request(req).await().get())
			}

			sendBitmap(cache = true, storeToCache = true)

			suspend fun removeCache() {
				(artworkProvider as? TestArtworkProvider)?.removeCacheForId(
					id = artId,
					mem = true,
					disk = true
				)
			}

			val observer = MediaStoreProvider.ContentObserver { id, uri, flag ->
				if (id == artId) {
					ioScope.launch {
						removeCache()
						sendBitmap(cache = false, storeToCache = false)
					}
				}
			}

			audioProvider.observe(observer)
			awaitClose {
				audioProvider.removeObserver(observer)
			}
		}
	}
}
