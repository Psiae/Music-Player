package com.kylentt.mediaplayer.helper.media

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kylentt.mediaplayer.helper.external.providers.ContentProvidersHelper
import timber.log.Timber
import java.io.File
import javax.inject.Singleton

@Singleton
class MediaItemHelper(
  private val context: Context
) {

  fun buildFromMetadata(uri: Uri): MediaItem {
    return try {
      val mtr = MediaMetadataRetriever()
      val artist = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "<Unknown>"
      val album = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "<Unknown>"
      val title = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        ?: if (uri.scheme == "content") {
          context.contentResolver.query(uri, null, null, null, null)
            ?.use { cursor ->
              cursor.moveToFirst()
              cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        } else {
          File(uri.toString()).name
        }

      MediaItem.Builder()
        .setUri(uri)
        .setMediaId(MediaItem.fromUri(uri).hashCode().toString())
        .setMediaMetadata(
          MediaMetadata.Builder()
            .setMediaUri(uri)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setDisplayTitle(title)
            .build()
        ).build()
    } catch (e: Exception) {
      Timber.e(e)
      MediaItem.EMPTY
    }
  }

  fun rebuildMediaItem(item: MediaItem): MediaItem {
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
    val mtr = MediaMetadataRetriever()
    return try {
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
}
