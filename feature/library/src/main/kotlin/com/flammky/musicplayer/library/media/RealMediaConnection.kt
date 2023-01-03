package com.flammky.musicplayer.library.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.core.MediaLibrary
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.musicplayer.base.coroutine.NonBlockingDispatcherPool
import com.flammky.musicplayer.base.media.r.MediaConnectionDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class RealMediaConnection(
	private val artworkProvider: ArtworkProvider,
	private val context: Context,
	private val delegate: MediaConnectionDelegate,
	private val dispatcher: AndroidCoroutineDispatchers,
	private val mediaLibrary: MediaLibrary
) : MediaConnection {
	private val coroutineScope = CoroutineScope(SupervisorJob())
	private val sDispatcher = NonBlockingDispatcherPool.get(1)

	override fun play(id: String, uri: Uri) {
		delegate.play(createMediaItem(id, uri))
		maybeProvideMetadata(id, uri)
		maybeProvideArtwork(id)
	}

	override fun play(queue: List<Pair<String, Uri>>, index: Int) {
		Timber.d(
			"""
				RMC play:
				queue=$queue
				index=$index
			"""
		)
		if (index !in queue.indices) return
		coroutineScope.launch(sDispatcher) {
			val mappedQueue = queue.map { createMediaItem(it.first, it.second) }
			delegate.play(mappedQueue, index)
			mappedQueue.forEach {
				val id = it.mediaId
				val uri = it.mediaUri
				maybeProvideMetadata(id, uri)
				maybeProvideArtwork(id)
			}
		}
	}

	override val repository: MediaConnection.Repository = object : MediaConnection.Repository {

		override suspend fun getArtwork(id: String): Any? {
			return delegate.repository.getArtwork(id)
		}

		// we should localize
		override suspend fun observeArtwork(id: String): Flow<Any?> {
			return delegate.repository.observeArtwork(id)
		}

		override suspend fun provideArtwork(id: String, artwork: Any?, silent: Boolean) {
			if (silent) {
				delegate.repository.silentProvideArtwork(id, artwork)
			} else {
				delegate.repository.provideArtwork(id, artwork)
			}
		}

		override suspend fun evictArtwork(id: String, silent: Boolean) {
			if (silent) {
				delegate.repository.silentEvictArtwork(id)
			} else {
				delegate.repository.evictArtwork(id)
			}
		}

		override suspend fun observeMetadata(id: String): Flow<MediaMetadata?> {
			return delegate.repository.observeMetadata(id)
		}

		override suspend fun provideMetadata(id: String, metadata: MediaMetadata) {
			return delegate.repository.provideMetadata(id, metadata)
		}
	}

	/**
	 * We should fill these metadata inside our service instead, or not at all and use singleton ?,
	 * I think the latter is better
	 */
	private fun createMediaItem(id: String, uri: Uri): MediaItem {
		return MediaItem.build(mediaLibrary.context) {
			setMediaId(id)
			setMediaUri(uri)
			setExtra(MediaItem.Extra())
			setMetadata(MediaMetadata.build { setExtra(MediaMetadata.Extra()) })
		}
	}

	private fun maybeProvideMetadata(id: String, uri: Uri) {
		coroutineScope.launch(dispatcher.io) {
			if (delegate.repository.getMetadata(id) == null) {
				val metadata = fillMetadata(uri)
				delegate.repository.provideMetadata(id, metadata)
			}
		}
	}

	private fun maybeProvideArtwork(id: String) {
		coroutineScope.launch(dispatcher.io) {
			if (delegate.repository.getArtwork(id) == null) {
				val req = ArtworkProvider.Request.Builder(id, Bitmap::class.java)
					.setStoreMemoryCacheAllowed(true)
					.setMemoryCacheAllowed(false)
					.setDiskCacheAllowed(false)
					.build()
				val result = artworkProvider.request(req).await()
				if (result.isSuccessful()) delegate.repository.provideArtwork(id, result.get())
			}
		}
	}

	private suspend fun fillMetadata(uri: Uri): MediaMetadata {
		mediaLibrary.mediaProviders.mediaStore.audio.queryByUri(uri)?.let { from ->
			val audioMetadata = fillAudioMetadata(uri)
			val fileMetadata = VirtualFileMetadata.build {
				setUri(from.uri)
				setScheme(from.uri.scheme)
				setAbsolutePath(from.file.absolutePath)
				setFileName(from.file.fileName)
				setDateAdded(from.file.dateAdded?.seconds)
				setLastModified(from.file.dateModified?.seconds)
				setSize(from.file.size)
			}
			return AudioFileMetadata(audioMetadata, fileMetadata)
		}

		return fillAudioMetadata(uri)
	}

	private fun fillAudioMetadata(uri: Uri): AudioMetadata {
		return AudioMetadata.build {
			try {
				MediaMetadataRetriever().applyUse {
					setDataSource(context, uri)
					setArtist(extractArtist())
					setAlbumArtist(extractAlbumArtist())
					setAlbumTitle(extractAlbum())
					setBitrate(extractBitrate())
					setDuration(extractDuration()?.milliseconds)
					setTitle(extractTitle())
					setPlayable(duration != null)
					setExtra(MediaMetadata.Extra())
				}
			} catch (_: Exception) {}
		}
	}

	private fun MediaMetadataRetriever.applyUse(apply: MediaMetadataRetriever.() -> Unit) {
		try {
			apply(this)
		} finally {
			release()
		}
	}

	private fun MediaMetadataRetriever.extractArtist(): String? {
		return tryOrNull { extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) }
	}

	private fun MediaMetadataRetriever.extractAlbumArtist(): String? {
		return tryOrNull { extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST) }
	}

	private fun MediaMetadataRetriever.extractAlbum(): String? {
		return tryOrNull { extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) }
	}

	private fun MediaMetadataRetriever.extractBitrate(): Long? {
		return tryOrNull { extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) }?.toLong()
	}

	private fun MediaMetadataRetriever.extractDuration(): Long? {
		return tryOrNull { extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) }?.toLong()
	}

	private fun MediaMetadataRetriever.extractTitle(): String? {
		return tryOrNull { extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) }
	}

	private inline fun <R> tryOrNull(block: () -> R): R? {
		return try {
			block()
		} catch (e: Exception) { null }
	}
}
