package com.flammky.musicplayer.main.ext

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
import com.flammky.android.content.intent.isActionView
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.android.medialib.temp.MediaLibrary
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.common.kotlin.coroutines.AutoCancelJob
import com.flammky.musicplayer.base.media.mediaconnection.MediaConnectionRepository
import com.flammky.musicplayer.media.mediaconnection.playback.PlaybackConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


class MediaIntentHandler constructor(
	private val presenter: Presenter
) {

	private val s = MediaLibrary.API.sessions.manager.findSessionById("DEBUG")!!

	private var jobSlot: Job by AutoCancelJob()

	fun isMediaIntent(intent: Intent): Boolean {
		if (!intent.isActionView()) {
			return false
		}

		val data = intent.data
			?: return false

		val type = intent.type
			?: return false

		@Suppress("SimplifyBooleanWithConstants")
		val schemeSupported = data.scheme == ContentResolver.SCHEME_CONTENT
			/* || data.scheme == ContentResolver.SCHEME_FILE */

		return schemeSupported && type.startsWith("audio/")
	}

	fun handleMediaIntent(intent: Intent) {
		val clone = intent.clone() as Intent
		if (!isMediaIntent(clone)) return
		jobSlot = presenter.coroutineScope.launch(presenter.coroutineDispatchers.io) {
			val data = clone.data!!
			val id = data.toString()
			if (!validateAudioURI(data)) {
				return@launch
			}
			ensureActive()
			s.mediaController.play(
				item = MediaItem.Builder()
					.setMediaId(id)
					.setUri(data)
					.setRequestMetadata(RequestMetadata.Builder().setMediaUri(data).build())
					.build()
			)
			launch(presenter.coroutineDispatchers.io) {
				val metadata = fillMetadata(data)
				presenter.sharedRepository.provideMetadata(id, metadata)
			}
			launch(presenter.coroutineDispatchers.io) {
				val artworkProvider = presenter.artworkProvider
				val req = ArtworkProvider.Request.Builder(id, Bitmap::class.java)
					.setUri(data)
					.setStoreMemoryCacheAllowed(true)
					.setMemoryCacheAllowed(false)
					.setDiskCacheAllowed(false)
					.build()
				val result = artworkProvider.request(req).await()
				if (result.isSuccessful()) presenter.sharedRepository.provideArtwork(id, result.get())
			}
		}
	}

	private suspend fun validateAudioURI(uri: Uri): Boolean {
		// check if we can open the uri
		runCatching {
			presenter.androidContext.contentResolver.openInputStream(uri)!!.close()
		}.exceptionOrNull()?.let { ex ->
			coroutineContext.ensureActive()
			when (ex) {
				is FileNotFoundException -> presenter.showIntentRequestErrorMessage(
					message = "Requested File is not found"
				)
				is SecurityException -> presenter.showIntentRequestErrorMessage(
					message = "Requested File could not be read (no provider permission)"
				)
				is NullPointerException -> presenter.showIntentRequestErrorMessage(
					message = "Requested File could not be read (inaccessible provider)"
				)
				else -> presenter.showIntentRequestErrorMessage(message = "Unexpected Error Occurred")
			}
			return false
		}

		return true
	}

	private suspend fun fillMetadata(uri: Uri): MediaMetadata {
		presenter.mediaStore.audio.queryByUri(uri)?.let { from ->
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
					setDataSource(presenter.androidContext, uri)
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

	interface Presenter {
		val androidContext: Context
		val artworkProvider: ArtworkProvider
		val coroutineDispatchers: AndroidCoroutineDispatchers
		val coroutineScope: CoroutineScope
		val playbackConnection: PlaybackConnection
		val sharedRepository: MediaConnectionRepository
		val mediaStore: MediaStoreProvider

		fun showIntentRequestErrorMessage(message: String)
	}
}
