package com.flammky.musicplayer.library.dump.localmedia.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider.ContentObserver.Flag.Companion.isDelete
import com.flammky.android.medialib.providers.mediastore.base.audio.MediaStoreAudioEntity
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.android.medialib.temp.image.internal.TestArtworkProvider
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.base.media.MetadataProvider
import com.flammky.musicplayer.library.dump.media.MediaConnection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class RealLocalSongRepository(
	private val context: Context,
	private val dispatchers: AndroidCoroutineDispatchers,
	private val artworkProvider: ArtworkProvider,
	private val metadataProvider: MetadataProvider,
	private val mediaConnection: MediaConnection,
	private val mediaStoreProvider: MediaStoreProvider,
) : LocalSongRepository {

	private val audioProvider = mediaStoreProvider.audio

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

	@Deprecated("Might trigger ContentObserver on certain device")
	override suspend fun requestUpdateAsync(): Deferred<List<Uri>> =
		coroutineScope {
			async(dispatchers.io) {
				suspendCancellableCoroutine { cont ->
					audioProvider.rescan { cont.resume(it) }
				}
			}
		}

	@Deprecated("Might trigger ContentObserver on certain device")
	override suspend fun requestUpdateAndGetAsync(): Deferred<List<LocalSongModel>> =
		coroutineScope {
			async(dispatchers.io) {
				requestUpdateAsync().await()
				getModelsAsync().await()
			}
		}

	override fun refreshMetadata(model: LocalSongModel): Job {
		return ioScope.launch {
			val afm = model.metadata as? AudioFileMetadata
				?: return@launch
			(afm.file as? VirtualFileMetadata)?.uri
				?.let { uri -> refreshMetadata(model.id, uri).join() }
				?: refreshMetadata(model.id).join()
		}
	}

	override fun refreshMetadata(id: String): Job {
		return ioScope.launch {
			audioProvider.uriFromId(id)
				?.let { uri -> refreshMetadata(id, uri).join() }
		}
	}

	override fun refreshMetadata(id: String, uri: Uri): Job {
		return ioScope.launch {
			if (mediaConnection.repository.getMetadata(id) != null) {
				mediaConnection.repository.evictMetadata(id, true)
				metadataProvider.requestAsync(id).join()
			}
		}
	}

	override fun refreshArtwork(model: LocalSongModel): Job {
		return ioScope.launch {
			val afm = model.metadata as? AudioFileMetadata
				?: return@launch
			(afm.file as? VirtualFileMetadata)?.uri
				?.let { uri -> refreshArtwork(model.id, uri).join() }
				?: refreshArtwork(model.id, model.uri)
		}
	}

	override fun refreshArtwork(id: String): Job {
		return ioScope.launch {
			audioProvider.uriFromId(id)
				?.let { uri -> refreshArtwork(id, uri).join() }
		}
	}

	override fun refreshArtwork(id: String, uri: Uri): Job {
		return ioScope.launch {
			Timber.d("refresh $id $uri")
			if (mediaConnection.repository.getArtwork(id + "_raw") != null) {
				Timber.d("refresh evicting $id $uri")
				mediaConnection.repository.evictArtwork(id + "_raw", true)
				val artReq = ArtworkProvider.Request.Builder(id, Bitmap::class.java)
					.setDiskCacheAllowed(false)
					.setMemoryCacheAllowed(false)
					.setStoreDiskCacheAllowed(false)
					.setStoreMemoryCacheAllowed(true)
					.setUri(uri)
					.build()
				artworkProvider.request(artReq).await()
			}
		}
	}
	private fun toLocalSongModel(from: MediaStoreAudioEntity): LocalSongModel {
		val audioMetadata = AudioMetadata.build {
			val durationMs = from.metadata.durationMs
			setArtist(from.metadata.artist)
			setAlbumTitle(from.metadata.album)
			setTitle(from.metadata.title ?: from.file.fileName)
			setPlayable(if (durationMs != null && durationMs > 0) true else null)
			setDuration(durationMs?.milliseconds)
		}
		val fileMetadata = VirtualFileMetadata.build {
			setUri(from.uri)
			setScheme(from.uri.scheme)
			setAbsolutePath(from.file.absolutePath)
			setFileName(from.file.fileName)
			setDateAdded(from.file.dateAdded?.seconds)
			setLastModified(from.file.dateModified?.seconds)
			setSize(from.file.size)
		}

		val metadata = AudioFileMetadata(audioMetadata, fileMetadata, MediaMetadata.Extra())
		return LocalSongModel(from.uid, metadata.title ?: metadata.file.fileName, from.uri, metadata)
	}

	override suspend fun collectArtwork(model: LocalSongModel): Flow<Bitmap?> {
		return collectArtwork(model.id)
	}

	override suspend fun collectArtwork(id: String): Flow<Bitmap?> {
		return callbackFlow {
			val artId = id

			suspend fun requestBitmap(cache: Boolean, storeToCache: Boolean): Bitmap? {
				val req = ArtworkProvider.Request
					.Builder(artId, Bitmap::class.java)
					.setMinimumHeight(1)
					.setMinimumWidth(1)
					.setMemoryCacheAllowed(cache)
					.setDiskCacheAllowed(cache)
					.setStoreMemoryCacheAllowed(storeToCache)
					.setStoreDiskCacheAllowed(storeToCache)
					.build()
				return artworkProvider.request(req).await().get()
			}

			suspend fun removeCache() {
				(artworkProvider as? TestArtworkProvider)?.removeCacheForId(
					id = artId,
					mem = true,
					disk = true
				)
				mediaConnection.repository.evictArtwork(artId, silent = true)
			}

			val observer = MediaStoreProvider.ContentObserver { id, uri, flag ->
				if (id == artId) {
					ioScope.launch {
						if (flag.isDelete) {
							removeCache()
						} else {
							mediaConnection.repository.evictArtwork(id, true)
							requestBitmap(cache = false, storeToCache = true)
						}
					}
				}
			}

			val get = mediaConnection.repository.getArtwork(id)
			if (get == null) {
				requestBitmap(cache = true, storeToCache = true)
			}

			mediaConnection.repository.observeArtwork(id + "_raw").safeCollect {
				send(it as? Bitmap)
			}

			audioProvider.observe(observer)
			awaitClose {
				audioProvider.removeObserver(observer)
			}
		}
	}

	override suspend fun collectMetadata(id: String): Flow<AudioMetadata?> {
		return flow {
			metadataProvider.requestAsync(id)
			mediaConnection.repository.observeMetadata(id).map { it as? AudioMetadata }.collect(this)
		}
	}

	override fun observeAvailable(): Flow<LocalSongRepository.AvailabilityState> = callbackFlow {
		val scheduledRefresh = mutableListOf<Any?>()
		val mutex = Mutex()
		var remember = LocalSongRepository.AvailabilityState()

		suspend fun sendUpdate(
			loading: Boolean = false,
			list: ImmutableList<LocalSongModel> = persistentListOf()
		) {
			val remembered = mutex.withLock {
				LocalSongRepository.AvailabilityState(loading, list).also { remember = it }
			}
			send(remembered)
			Timber.d("ObserveAvailable sent $remembered")
		}

		suspend fun doScheduledRefresh(id: String? = null): ImmutableList<LocalSongModel> {
			if (id == null) {
				while (true) {
					val size = mutex.withLock { scheduledRefresh.size }
					if (size == 0) return mutex.withLock { remember.list }
					val get = getModelsAsync().await().toPersistentList()
					val loading = mutex.withLock { remember.loading }
					sendUpdate(loading, get)
					mutex.withLock {
						scheduledRefresh.drop(size).let {
							scheduledRefresh.clear()
							scheduledRefresh.addAll(it)
						}
						scheduledRefresh
					}.also {
						Timber.d("doSchedulerRefresh, refreshed $size at once")
					}
				}
			} else {
				return doScheduledRefresh(null)
				// TODO
			}
		}

		suspend fun scheduleRefresh(id: String? = null) {
			val remembered = mutex.withLock {
				scheduledRefresh.add(id)
				if (!remember.loading) remember else return
			}
			launch {
				sendUpdate(loading = true, remembered.list)
				sendUpdate(loading = false, doScheduledRefresh(id = id))
			}
		}

		val observer = MediaStoreProvider.ContentObserver { id, uri, flag ->
			Timber.d("ObserveCurrentAvailable $id, $uri, $flag")
			ioScope.launch { scheduleRefresh(id) }
		}

		withContext(dispatchers.io) {
			sendUpdate(loading = true)
			val current = mediaStoreProvider.audio.query()
				.map { toLocalSongModel(it) }
				.toPersistentList()
			sendUpdate(loading = false, list = current)
		}

		mediaStoreProvider.audio.observe(observer)
		awaitClose {
			mediaStoreProvider.audio.removeObserver(observer)
		}
	}
}
