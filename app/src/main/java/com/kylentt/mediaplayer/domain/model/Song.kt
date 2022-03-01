package com.kylentt.mediaplayer.domain.model

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kylentt.mediaplayer.core.util.Constants.SONG_BYTE
import com.kylentt.mediaplayer.core.util.Constants.SONG_DATA
import com.kylentt.mediaplayer.core.util.Constants.SONG_FILE_NAME
import com.kylentt.mediaplayer.core.util.Constants.SONG_FILE_PARENT
import com.kylentt.mediaplayer.core.util.Constants.SONG_FILE_PARENT_ID
import com.kylentt.mediaplayer.core.util.Constants.SONG_LAST_MODIFIED
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
    val imageUri: String = "", // Album Image, will make embeds option for user
    val lastModified: Long = 0L, // LastModified given to compare to Document Provided lastModified
    val mediaId: String = "", // ID from Audio Columns
    val mediaUri: String = "", // Appended EXTERNAL_CONTENT_URI with its ID above
    val title: String = "", // Embedded Title, without ext e.g : .mp3 / fileName have that one
)

// so far only either data + byteSize or lastModified + byteSize + fileName is provided
fun Song.findIdentifier(str: String): Pair<String, String>? {
    // TODO: Something maybe
    return null
}

// Using concatenating list.find{ (a1 == b1 && a2 == b2) || a3.contains(b3) } ?: list.find {another}
// Doesn't seems fun so i'll just use this for now, just curious, really,  don't scold me
// Might as well help me in this project :)
fun List<Song>.findIdentified(
    iden: String,
    str: Triple<String?, Pair<String, String>?, Triple<String, String, String>?>,
): Song? {

    val data = SONG_DATA
    val byte = SONG_BYTE
    val fileName = SONG_FILE_NAME
    val fileParent = SONG_FILE_PARENT
    val fileParentId = SONG_FILE_PARENT_ID
    val lastModified = SONG_LAST_MODIFIED

    // absolute single identifier
    val abs = str.first

    // pair identifier
    val pair1 = str.second?.first
    val pair2 = str.second?.second

    // Triple identifier
    val tri1 = str.third?.first
    val tri2 = str.third?.second
    val tri3 = str.third?.third

    // Maybe Quadruple and so on in the future?

    // so far data or path alone is enough, but deprecated so might change with fileName & parent file name
    // since its practically not possible to have same file name inside a folder unless those
    // vendors just make it possible for god only knows reason?
    if (iden == data) {
        Timber.d("IntentHandler try find with $data")
        return find { song -> abs == song.data }
    }

    if (iden == lastModified) {
        Timber.d("IntentHandler try find with $tri1 && $tri2 && $tri3")
        return find { song -> tri1?.contains(song.lastModified.toString()) == true && tri2 == song.byteSize.toString() && tri3 == song.fileName}
    }

    return null
}

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
