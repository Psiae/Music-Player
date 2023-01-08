package com.flammky.musicplayer.domain.musiclib.media3.mediaitem

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.flammky.common.kotlin.string.setPrefix
import com.flammky.common.media.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3.MP3File
import com.flammky.musicplayer.domain.musiclib.media3.mediaitem.MediaItemPropertyHelper.mediaUri
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object MediaItemFactory {

	private const val ART_URI_PREFIX = "ART"

	val EMPTY
		get() = MediaItem.EMPTY

	fun newBuilder(): MediaItem.Builder = MediaItem.Builder()

	fun fillInLocalConfig(item: MediaItem, itemUri: Uri) = fillInLocalConfig(item, itemUri.toString())

	fun fillInLocalConfig(item: MediaItem, itemUri: String): MediaItem = with(newBuilder()) {
		fillBundle(item.mediaMetadata)
		setMediaMetadata(item.mediaMetadata)
		setRequestMetadata(item.requestMetadata)
		setMediaId(item.mediaId)
		setUri(itemUri)
		build()
	}

	private fun MediaItem.Builder.fillBundle(metadata: MediaMetadata): MediaItem.Builder {
		val put =
			if (metadata.extras == null) {
				MediaMetadata.Builder()
					.populate(metadata)
					.setExtras(Bundle())
					.build()
			} else {
				metadata
			}
		setMediaMetadata(put)
		return this
	}

	fun getEmbeddedImage(context: Context, item: MediaItem): ByteArray? {
		return getEmbeddedImage(context, getUri(item) ?: return null)
	}

	fun getEmbeddedImage(context: Context, uri: Uri): ByteArray? {

		Timber.d("getEmbeddedImage for $uri")


		if (uri.scheme == ContentResolver.SCHEME_CONTENT) return run {
			val af = AudioFile.Builder(context, uri).build()
			af.file?.delete()
			val embed = af.imageData
			embed
		}.also {
			Timber.d("getEmbeddedImage for $uri returned ${it?.size}")
		}

		return try {
			val file = context.contentResolver.openInputStream(uri)
				?.use { iStream ->

					val cacheDir: File = context.externalCacheDir ?: return null
					File.createTempFile("embedTemp_", null, cacheDir).apply {
						iStream.writeToFile(this)
					}
				}
				?: return null

			val data = MP3File(file).iD3v2Tag?.firstArtwork?.binaryData
			file.delete()
			data
		} catch (e: Exception) {
			Timber.e("getEmbeddedImage exception: $e")
			null
		}.also {
			Timber.d("getEmbeddedImage for $uri returned $it")
		}
	}

	private fun InputStream.writeToFile(file: File, buffer: ByteArray = ByteArray(8192)) {
		use { iStream ->
			FileOutputStream(file).use { oStream ->
				while (true) {
					val byteCount = iStream.read(buffer)
					if (byteCount < 0) break
					oStream.write(buffer, 0, byteCount)
				}
				oStream.flush()
			}
		}
	}

	fun getUri(item: MediaItem): Uri? = item.mediaUri

	fun hideArtUri(uri: Uri): Uri = hideArtUri(uri.toString()).toUri()
	fun showArtUri(uri: Uri): Uri = showArtUri(uri.toString()).toUri()

	fun hideArtUri(uri: String): String = uri.setPrefix(ART_URI_PREFIX)
	fun showArtUri(uri: String): String = uri.removePrefix(ART_URI_PREFIX)

	fun MediaItem?.orEmpty() = this ?: EMPTY
}
