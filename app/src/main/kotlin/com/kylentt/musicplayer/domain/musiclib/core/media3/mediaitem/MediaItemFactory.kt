package com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
import androidx.media3.common.MediaMetadata
import com.kylentt.musicplayer.common.extenstions.setPrefix
import com.kylentt.musicplayer.domain.musiclib.core.media3.mediaitem.MediaItemPropertyHelper.mediaUri
import timber.log.Timber
import wseemann.media.FFmpegMediaMetadataRetriever

object MediaItemFactory {

	private const val ART_URI_PREFIX = "ART"

	val EMPTY
		get() = MediaItem.EMPTY

	fun newBuilder(): MediaItem.Builder = MediaItem.Builder()

	fun fromMetaData(context: Context, uri: Uri): MediaItem {
		val mtr = MediaMetadataRetriever()
		return try {
			mtr.setDataSource(context, uri)
			val artist = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "<Unknown>"
			val album = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "<Unknown>"
			val title = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
				?: if (uri.scheme == "content") {
					context.contentResolver.query(uri, null, null, null, null)
						?.use { cursor ->
							cursor.moveToFirst()
							cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
						}
						?: "Unknown"
				} else {
					"Unknown"
				}

			val mediaId = "${uri.authority}" + "_" + MediaItem.fromUri(uri).hashCode().toString()

			val metadata = MediaMetadata.Builder()
				.setArtist(artist)
				.setAlbumTitle(album)
				.setDisplayTitle(title)
				.setTitle(title)
				.build()

			val reqMetadata = RequestMetadata.Builder()
				.setMediaUri(uri)
				.build()

			MediaItem.Builder()
				.setUri(uri)
				.setMediaId(mediaId)
				.setMediaMetadata(metadata)
				.setRequestMetadata(reqMetadata)
				.build()

		}	catch (e: Exception) {
			Timber.w("Failed To Build MediaItem from Uri: $uri \n${e}")
			EMPTY
		} finally {
			mtr.release()
		}
	}

	fun fillInLocalConfig(item: MediaItem, itemUri: Uri) = fillInLocalConfig(item, itemUri.toString())

	fun fillInLocalConfig(item: MediaItem, itemUri: String): MediaItem = with(newBuilder()) {
		setMediaMetadata(item.mediaMetadata)
		setRequestMetadata(item.requestMetadata)
		setMediaId(item.mediaId)
		setUri(itemUri)
		build()
	}

	fun getEmbeddedImage(context: Context, item: MediaItem): ByteArray? {
		return getEmbeddedImage(context, getUri(item) ?: return null)
	}

	fun getEmbeddedImage(context: Context, uri: Uri): ByteArray? {
		val mtr = FFmpegMediaMetadataRetriever()
		return try {
			mtr.setDataSource(context.applicationContext, uri)
			mtr.embeddedPicture
		} catch (e: Exception) {
			Timber.w("Failed to get embeddedPicture Metadata from $uri\n${e}")
			null
		} finally {
			mtr.release()
		}
	}

	fun getUri(item: MediaItem): Uri? = item.mediaUri

	fun hideArtUri(uri: Uri): Uri = hideArtUri(uri.toString()).toUri()
	fun showArtUri(uri: Uri): Uri = showArtUri(uri.toString()).toUri()

	fun hideArtUri(uri: String): String = uri.setPrefix(ART_URI_PREFIX)
	fun showArtUri(uri: String): String = uri.removePrefix(ART_URI_PREFIX)

	fun MediaItem?.orEmpty() = this ?: EMPTY
}

