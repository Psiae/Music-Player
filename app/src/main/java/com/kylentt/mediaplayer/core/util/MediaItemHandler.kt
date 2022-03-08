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

class MediaItemHandler(
    private val context: Context,
    private val coil: ImageLoader
) {
    val mtr = MediaMetadataRetriever()

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

    suspend fun songsToItemWithEmbed(list: List<Song>): List<MediaItem> {
        return list.map { it.toMediaItemWithEmbed() }
    }



    suspend fun Song.toMediaItemWithEmbed(): MediaItem {
        Timber.d("toMediaItem $lastModified")
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(mediaUri.toUri())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setArtist(artist)
                    .setAlbumArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkData(getSquaredEmbed(mediaUri.toUri()), PICTURE_TYPE_MEDIA)
                    .setArtworkUri(("ART$albumImage").toUri())
                    .setDisplayTitle(title)
                    .setDescription(fileName)
                    .setMediaUri(mediaUri.toUri())
                    .setSubtitle(artist.ifEmpty { album })
                    .setCompilation(byteSize.toString())
                    .setConductor(lastModified.toString())
                    .setIsPlayable(true)
                    .build())
            .build()
    }

    suspend fun getSquaredEmbed(uri: Uri) = withContext(Dispatchers.Default) {
        mtr.setDataSource(context, uri)
        mtr.embeddedPicture?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size).squareWithCoil()?.let {
                val stream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.toByteArray()
            }
        }
    }

    suspend fun Bitmap.squareWithCoil(): Bitmap? {
        val req = ImageRequest.Builder(context.applicationContext)
            .transformations(CropSquareTransformation())
            .size(512)
            .scale(Scale.FILL)
            .data(this)
            .build()
        return ((coil.execute(req).drawable) as BitmapDrawable?)?.bitmap
    }



    fun rebuildMediaItem(list: List<MediaItem>): List<MediaItem> {
        val toReturn = mutableListOf<MediaItem>()
        list.forEach {
            MediaItem.Builder()
                .setUri(it.getUri)
                .setMediaId(it.mediaId)
                .setMediaMetadata(it.mediaMetadata)
                .build()
        }
        return toReturn
    }
}
inline val MediaItem.getUri
    get() = this.mediaMetadata.mediaUri
