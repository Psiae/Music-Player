package com.kylentt.mediaplayer.domain.model

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import timber.log.Timber

data class Song(
    val album: String = "",
    val albumId: String = "",
    val artist: String = "",
    val artistId: String = "",
    val byteSize: Long = 0L, // Intent Handler
    val duration: Long = 0L,
    val data: String = "",
    val fileName: String = "",
    val imageUri: String = "",
    val lastModified: Long = 0L, // Intent Handler
    val mediaId: String = "", // preferably Long
    val mediaUri: String = "",
    val title: String = "",
)

fun List<Song>.toMediaItems(): List<MediaItem> {
    return map { it.toMediaItem() }
}

// MediaItem for ExoPlayer Queue Management, will use this for UI for now

fun Song.toMediaItem(): MediaItem {
    Timber.d("toMediaItem $lastModified")
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(Uri.parse(mediaUri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setArtist(artist)
                .setAlbumArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(("ART$imageUri").toUri())
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

// because of localConfig issue
inline fun MediaItem.rebuild(): MediaItem {
    Timber.d("rebuild ${this.mediaMetadata.conductor}")
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(this.mediaMetadata.mediaUri)
        .setMediaMetadata(mediaMetadata)
        .build()
}

inline val MediaItem.getDateModified: Long?
    get() = mediaMetadata.conductor.toString().toLong()

inline val MediaItem.fileName: CharSequence?
    get() = mediaMetadata.title

inline val MediaItem.byteSize: Long?
    get() = mediaMetadata.compilation.toString().toLong()

inline val MediaItem.getArtist: CharSequence?
    get() = mediaMetadata.artist

inline val MediaItem.getSubtitle: CharSequence?
    get() = mediaMetadata.subtitle

inline val MediaItem.getDisplayTitle: CharSequence?
    get() = mediaMetadata.displayTitle

inline val MediaItem.artUri: Uri
    get() = mediaMetadata.artworkUri.toString().removePrefix("ART").toUri()
