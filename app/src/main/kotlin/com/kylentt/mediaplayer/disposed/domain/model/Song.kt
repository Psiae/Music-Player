package com.kylentt.mediaplayer.disposed.domain.model

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import timber.log.Timber

/** DON'T forget to change the Constant */
data class Song(
    val album: String = "", // Album Name, usually FolderName if there's no Embedded Album Name
    val albumId: String = "", // Album Id that can be Appended to get ImageUri from MediaStore
    val artist: String = "", // Artist Name, simply empty if not found
    val artistId: String = "", // ArtistId, Idk what for
    val byteSize: Long = 0L, // ByteSize that usually given by content Provider for Intent Handling
    val duration: Long = 0L, // Duration of the Song
    val data: String = "", // Data Path for Android (Q <=)
    val fileName: String = "", // Name of the file
    val fileParent: String = "", // Name of folder of the file
    val fileParentId: Long = 0L, // Id of folder of the file
    val albumImage: String = "", // Album Image, will make embeds option for user
    val lastModified: Long = 0L, // LastModified given to compare to Document Provided lastModified
    val mediaId: String = "", // ID from Audio Columns
    val mediaUri: String = "", // Appended EXTERNAL_CONTENT_URI with its ID above
    val title: String = "", // Embedded Title, without ext e.g : .mp3 / fileName have that one
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
