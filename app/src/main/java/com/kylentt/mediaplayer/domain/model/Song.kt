package com.kylentt.mediaplayer.domain.model

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SongEntity(
    val album: String = "",
    val albumId: String = "",
    val artist: String = "",
    val artistId: String = "",
    val duration: Long = 0L,
    val fileName: String = "",
    val imageUri: String = "",
    val mediaId: String = "", // preferably Long
    val mediaUri: String = "",
    val title: String = "",
    @PrimaryKey val id: Int? = null
)

data class Song(
    val album: String = "",
    val albumId: String = "",
    val artist: String = "",
    val artistId: String = "",
    val duration: Long = 0L,
    val fileName: String = "",
    val imageUri: String = "",
    val mediaId: String = "", // preferably Long
    val mediaUri: String = "",
    val title: String = "",
)

fun List<Song>.toSongEntities(): List<SongEntity> {
    return map { it.toSongEntity() }
}
fun List<SongEntity>.toSongs(): List<Song> {
    return map { it.toSong() }
}
fun SongEntity.toSong(): Song = Song(
    album, albumId, artist, artistId, duration, fileName, imageUri, mediaId, mediaUri, title
)
fun Song.toSongEntity(): SongEntity = SongEntity(
    album, albumId, artist, artistId, duration, imageUri, fileName,  mediaId, mediaUri, title
)
fun List<Song>.toMediaItems(): List<MediaItem> {
    return map { it.toMediaItem() }
}

// MediaItem for ExoPlayer Queue Management, will use this for UI for now

fun Song.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(Uri.parse(mediaUri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setArtist(artist)
                .setAlbumArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(Uri.parse(imageUri))
                .setDisplayTitle(title)
                .setMediaUri(Uri.parse(mediaUri))
                .setSubtitle(artist.ifEmpty { album })
                .setTitle(title)
                .setIsPlayable(true)
                .build())
        .build()
}

// because of localConfig issue
inline fun MediaItem.rebuild(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(this.mediaMetadata.mediaUri)
        .setMediaMetadata(mediaMetadata)
        .build()
}
