package com.flammky.musicplayer.library.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.musicplayer.base.Playback
import com.flammky.musicplayer.base.media.MediaConstants
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.playback.PlaybackQueue
import com.flammky.musicplayer.base.media.r.MediaMetadataCacheRepository
import com.flammky.musicplayer.base.user.User
import kotlinx.collections.immutable.toPersistentList
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
	private val dispatcher: AndroidCoroutineDispatchers,
	private val mediaStore: MediaStoreProvider,
	private val mediaRepo: MediaMetadataCacheRepository,
	private val playbackConnection: PlaybackConnection
) : MediaConnection {
	private val coroutineScope = CoroutineScope(SupervisorJob())

	override fun play(
		user: User,
		queue: List<Pair<String, Uri>>,
		index: Int
	) {
		Timber.d(
			"""
				RMC play:
				queue=$queue
				index=$index
			"""
		)
		if (index !in queue.indices) return
		coroutineScope.launch(Playback.DISPATCHER) {
			playbackConnection.requestUserSessionAsync(user).await().controller.withLooperContext {
				val mappedQueue = queue.map { it.first }.toPersistentList()
				setQueue(PlaybackQueue(mappedQueue, index))
				play()
			}
			queue.forEach {
				maybeProvideMetadata(it.first, it.second)
				maybeProvideArtwork(it.first, it.second)
			}
		}
	}

	override val repository: MediaConnection.Repository = object : MediaConnection.Repository {

		override suspend fun getArtwork(id: String): Any? {
			return mediaRepo.getArtwork(id)
		}

		// we should localize
		override suspend fun observeArtwork(id: String): Flow<Any?> {
			return mediaRepo.observeArtwork(id)
		}

		override suspend fun provideArtwork(id: String, artwork: Any?, silent: Boolean) {
			if (silent) {
				mediaRepo.silentProvideArtwork(id, artwork ?: MediaConstants.NO_ARTWORK)
			} else {
				mediaRepo.provideArtwork(id, artwork ?: MediaConstants.NO_ARTWORK)
			}
		}

		override suspend fun evictArtwork(id: String, silent: Boolean) {
			if (silent) {
				mediaRepo.silentEvictArtwork(id)
			} else {
				mediaRepo.evictArtwork(id)
			}
		}

		override suspend fun observeMetadata(id: String): Flow<MediaMetadata?> {
			return mediaRepo.observeMetadata(id)
		}

		override suspend fun provideMetadata(id: String, metadata: MediaMetadata) {
			return mediaRepo.provideMetadata(id, metadata)
		}
	}

	private fun maybeProvideMetadata(id: String, uri: Uri) {
		coroutineScope.launch(dispatcher.io) {
			if (mediaRepo.getMetadata(id) == null) {
				val metadata = fillMetadata(uri)
				mediaRepo.provideMetadata(id, metadata)
			}
		}
	}

	private fun maybeProvideArtwork(id: String, uri: Uri) {
		coroutineScope.launch(dispatcher.io) {
			if (mediaRepo.getArtwork(id) == null) {
				val req = ArtworkProvider.Request.Builder(id, Bitmap::class.java)
					.setStoreMemoryCacheAllowed(true)
					.setMemoryCacheAllowed(false)
					.setDiskCacheAllowed(false)
					.build()
				val result = artworkProvider.request(req).await()
				if (result.isSuccessful()) mediaRepo.provideArtwork(id, result.get() ?: MediaConstants.NO_ARTWORK)
			}
		}
	}

	private suspend fun fillMetadata(uri: Uri): MediaMetadata {
		mediaStore.audio.queryByUri(uri)?.let { from ->
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
