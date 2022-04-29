package com.kylentt.disposed.musicplayer.core.helper

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaItemHelper @Inject constructor(
  @ApplicationContext private val context: Context
) {

  private val mtr = MediaMetadataRetriever()

  fun getEmbeddedPicture(item: MediaItem): ByteArray? = try {
    mtr.setDataSource(context, item.mediaMetadata.mediaUri)
    mtr.embeddedPicture
  } catch (e: Exception) {
    Timber.e(e)
    null
  }

  fun getEmbeddedPicture(uri: Uri): ByteArray? = try {
    mtr.setDataSource(context, uri)
    mtr.embeddedPicture
  } catch (e: Exception) {
    Timber.e(e)
    null
  }

  fun List<MediaItem>.rebuild() = with(MediaItemUtil) { rebuild() }
  fun MediaItem.rebuild() = with(MediaItemUtil) { rebuild() }

}

object MediaItemDefaults {
  const val artPrefix = "ART$"
}

object MediaItemUtil {

  // make it so that the notification at some device mediaSession doesn't override the art
  fun hideArtUri(uri: Uri) = hideArtUri(uri.toString()).toUri()
  fun showArtUri(uri: Uri) = showArtUri(uri.toString()).toUri()
  fun hideArtUri(str: String) = MediaItemDefaults.artPrefix + str
  fun showArtUri(str: String) = str.removePrefix(MediaItemDefaults.artPrefix)

  fun List<MediaItem>.rebuild() = map { it.rebuild() }

  fun MediaItem.rebuild() = MediaItem.Builder()
    .setMediaId(this.mediaId)
    .setUri(this.mediaMetadata.mediaUri)
    .setMediaMetadata(this.mediaMetadata)
    .build()

  inline val MediaItem.getArtist
    get() = this.mediaMetadata.artist

  inline val MediaItem.getAlbumArtist
    get() = this.mediaMetadata.albumArtist

  inline val MediaItem.getAlbum
    get() = this.mediaMetadata.albumTitle

  inline val MediaItem.getDisplayTitle
    get() = this.mediaMetadata.displayTitle
}
