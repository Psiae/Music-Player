package com.flammky.musicplayer.base.media.r

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.musicplayer.base.media.MetadataProvider
import com.flammky.musicplayer.core.common.sync
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TestMetadataProvider(
	private val context: Context,
	private val coroutineDispatcher: AndroidCoroutineDispatchers,
	private val cacheRepository: MediaMetadataCacheRepository,
	private val mediaStoreProvider: MediaStoreProvider
) : MetadataProvider {

	@OptIn(ExperimentalCoroutinesApi::class)
	private val limitedIO = coroutineDispatcher.io.limitedParallelism(24)

	private val _lock = Any()
	private val coroutineScope = CoroutineScope(SupervisorJob())
	private val requestMap = mutableMapOf<String, Deferred<MediaMetadata?>>()

	override fun getCached(id: String): MediaMetadata? = cacheRepository.getMetadata(id)

	override fun requestAsync(id: String): Deferred<MediaMetadata?> {
		return getCached(id)
			?.let { CompletableDeferred(it) }
			?: requestMap.sync {
				get(id) ?: coroutineScope.async(limitedIO) {
					getCached(id)
						?: run {
							fillMetadata(id)
								?.also { cacheRepository.provideMetadata(id, it) }
								.also { requestMap.sync { remove(id) } }
						}
				}.also { job ->
					put(id, job)
				}
			}
	}

	private suspend fun fillMetadata(id: String): MediaMetadata? {
		val uri = mediaStoreProvider.audio.uriFromId(id) ?: id.toUri()
		if (!uri.toString().startsWith("content://")) {
			return null
		}
		return fillMetadata(uri)
	}

	private suspend fun fillMetadata(uri: Uri): MediaMetadata {
		mediaStoreProvider.audio.queryByUri(uri)?.let { from ->
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
