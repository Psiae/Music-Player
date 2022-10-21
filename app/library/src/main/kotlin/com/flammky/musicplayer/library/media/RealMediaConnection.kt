package com.flammky.musicplayer.library.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.core.MediaLibrary
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.musicplayer.base.media.mediaconnection.MediaConnectionDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class RealMediaConnection(
	private val artworkProvider: ArtworkProvider,
	private val context: Context,
	private val delegate: MediaConnectionDelegate,
	private val dispatcher: AndroidCoroutineDispatchers,
	private val mediaLibrary: MediaLibrary
) : MediaConnection {
	private val coroutineScope = CoroutineScope(SupervisorJob())

	private val m = com.flammky.android.medialib.temp.MediaLibrary.API.sessions.manager.findSessionById("DEBUG")!!.mediaController

	override fun play(id: String, uri: Uri) {
		delegate.play(createMediaItem(id, uri))
		provideMetadata(id, uri)
		provideArtwork(id)
	}

	override val repository: MediaConnection.Repository = object : MediaConnection.Repository {

		override suspend fun getArtwork(id: String): Any? {
			return delegate.repository.getArtwork(id)
		}

		override fun observeArtwork(id: String): Flow<Any?> {
			return delegate.repository.observeArtwork(id)
		}

		override fun provideArtwork(id: String, artwork: Any?, silent: Boolean) {
			if (silent) {
				delegate.repository.silentProvideArtwork(id, artwork)
			} else {
				delegate.repository.provideArtwork(id, artwork)
			}
		}

		override fun evictArtwork(id: String, silent: Boolean) {
			if (silent) {
				delegate.repository.silentEvictArtwork(id)
			} else {
				delegate.repository.evictArtwork(id)
			}
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

	private fun provideMetadata(id: String, uri: Uri) {
		coroutineScope.launch(dispatcher.io) {
			val metadata = fillMetadata(uri)
			delegate.repository.provideMetadata(id, metadata)
		}
	}

	private fun provideArtwork(id: String) {
		coroutineScope.launch(dispatcher.io) {
			if (delegate.repository.getArtwork(id) == null) {
				val req = ArtworkProvider.Request.Builder(id, Bitmap::class.java)
					.setStoreMemoryCacheAllowed(true)
					.setDiskCacheAllowed(false)
					.build()
				val result = artworkProvider.request(req).await()
				if (result.isSuccessful()) delegate.repository.provideArtwork(id, result.get())
			}
		}
	}

	private fun fillMetadata(uri: Uri): MediaMetadata {
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
