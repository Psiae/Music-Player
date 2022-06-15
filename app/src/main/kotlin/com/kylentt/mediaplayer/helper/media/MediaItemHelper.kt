package com.kylentt.mediaplayer.helper.media

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kylentt.mediaplayer.helper.StringExtension.setPrefix
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class MediaItemHelper(
  private val context: Context
) {

  fun buildFromMetadata(uri: Uri): MediaItem {
    return try {
      val mtr = MediaMetadataRetriever()
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

      val mediaId = MediaItem.fromUri(uri).hashCode().toString()

      val metadata = MediaMetadata.Builder()
        .setMediaUri(uri)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setDisplayTitle(title)
        .build()

      MediaItem.Builder()
        .setUri(uri)
        .setMediaId(mediaId)
        .setMediaMetadata(metadata)
        .build()
    } catch (e: Exception) {
      Timber.e(e)
      MediaItem.EMPTY
    }
  }

	@Suppress("NOTHING_TO_INLINE")
	inline fun rebuildMediaItem(item: MediaItem): MediaItem {
    return MediaItem
      .Builder()
      .setMediaId(item.mediaId)
      .setUri(item.mediaMetadata.mediaUri)
      .setMediaMetadata(item.mediaMetadata)
      .build()
  }

  fun getEmbeddedPicture(item: MediaItem): ByteArray? {
    return item.mediaMetadata.mediaUri?.let { getEmbeddedPicture(it) }
  }

  fun getEmbeddedPicture(uri: Uri): ByteArray? {
    return try {
      val mtr = MediaMetadataRetriever()
      mtr.setDataSource(context, uri)
      mtr.embeddedPicture
    } catch (e: Exception) {
      Timber.e(e)
      null
    }
  }


  init {
    require(context is Application)
  }

  companion object {

		/**
		 * In case of [MediaItem] some devices might override [NotificationCompat.Builder.mLargeIcon]
		 * with [android.graphics.Bitmap] from [MediaMetadata.artworkUri] of the current [MediaItem]
		 * of the [androidx.media3.session.MediaSession] that is set to the [NotificationCompat].
		 *
		 * In e.g: Pixel Android 12 it will always try to maintain the aspect-ratio
		 * so it might came out bad, so the ArtworkUri is Hidden to be properly scaled as 1:1 later
		 */

    private const val ART_URI_PREFIX = "ART"

    @JvmStatic fun hideArtUri(uri: Uri): Uri = hideArtUri(uri.toString()).toUri()
    @JvmStatic fun showArtUri(uri: Uri): Uri = showArtUri(uri.toString()).toUri()

    @JvmStatic fun hideArtUri(uri: String): String = uri.setPrefix(ART_URI_PREFIX)
    @JvmStatic fun showArtUri(uri: String): String = uri.removePrefix(ART_URI_PREFIX)

		@JvmStatic fun MediaItem?.orEmpty() = this ?: MediaItem.EMPTY
  }
}
