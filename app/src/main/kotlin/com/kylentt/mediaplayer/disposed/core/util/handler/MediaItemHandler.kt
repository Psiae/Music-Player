package com.kylentt.mediaplayer.disposed.core.util.handler

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class MediaItemHandler(
  private val context: Context
) {
  private var mtr = MediaMetadataRetriever()

  fun getEmbeds(item: MediaItem): ByteArray? {
    synchronized(this) {
      return try {
        mtr.setDataSource(context, item.getUri)
        mtr.embeddedPicture
      } catch (e: Exception) {
        Timber.e(e)
        null
      }
    }
  }

  fun getEmbeds(uri: Uri): ByteArray? {
    return try {
      mtr.setDataSource(context, uri)
      mtr.embeddedPicture
    } catch (e: Exception) {
      when (e) {
        is IllegalArgumentException -> {}
        is RuntimeException -> {}
        else -> {}
      }
      Timber.e(e)
      null
    }
  }

  suspend fun sRebuildMediaItem(list: List<MediaItem>) = withContext(Dispatchers.IO) {
    list.map { rebuildMediaItem(it) }
  }

  fun rebuildMediaItems(list: List<MediaItem>): List<MediaItem> {
    return list.map { rebuildMediaItem(it) }
  }

  fun rebuildMediaItem(it: MediaItem) = MediaItem.Builder()
    .setUri(it.getUri)
    .setMediaId(it.mediaId)
    .setMediaMetadata(it.mediaMetadata)
    .build()

}

inline val MediaItem.getArtUri: Uri
  get() = mediaMetadata.artworkUri.toString().removePrefix("ART").toUri()

inline val MediaItem.getDisplayTitle: CharSequence?
  get() = mediaMetadata.displayTitle

inline val MediaItem.getSubtitle: CharSequence?
  get() = mediaMetadata.subtitle

inline val MediaItem.getUri
  get() = this.mediaMetadata.mediaUri
