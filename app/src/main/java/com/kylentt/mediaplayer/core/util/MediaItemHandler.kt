package com.kylentt.mediaplayer.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.PICTURE_TYPE_MEDIA
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.kylentt.mediaplayer.domain.model.Song
import jp.wasabeef.transformers.coil.CropSquareTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import javax.inject.Singleton

@Singleton
class MediaItemHandler(
    private val context: Context
) {
    private val mtr = MediaMetadataRetriever()

    fun getEmbeds(item: MediaItem): ByteArray? {
        mtr.setDataSource(context, item.getUri)
        return mtr.embeddedPicture
    }

    fun getEmbeds(uri: Uri): ByteArray? {
        mtr.setDataSource(context, uri)
        return mtr.embeddedPicture
    }

    suspend fun sRebuildMediaItem(list: List<MediaItem>) = withContext(Dispatchers.Default) {
        val toReturn = mutableListOf<MediaItem>()
        list.forEach {
            MediaItem.Builder()
                .setUri(it.getUri)
                .setMediaId(it.mediaId)
                .setMediaMetadata(it.mediaMetadata)
                .build()
        }
        toReturn
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
