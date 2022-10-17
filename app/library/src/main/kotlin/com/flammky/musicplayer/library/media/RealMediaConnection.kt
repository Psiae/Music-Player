package com.flammky.musicplayer.library.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.core.MediaLibrary
import com.flammky.musicplayer.base.media.MediaConnectionDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class RealMediaConnection(
	private val context: Context,
	private val delegate: MediaConnectionDelegate,
	private val dispatcher: AndroidCoroutineDispatchers,
	private val mediaLibrary: MediaLibrary
) : MediaConnection {
	private val coroutineScope = CoroutineScope(SupervisorJob())

	override fun play(id: String, uri: Uri) {
		coroutineScope.launch(dispatcher.io) {
			delegate.play(createMediaItem(id, uri))
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
			setMetadata(fillMetadata(uri))
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
