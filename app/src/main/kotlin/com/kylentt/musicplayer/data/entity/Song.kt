package com.kylentt.musicplayer.data.entity

import android.net.Uri
import androidx.media3.common.MediaItem
import com.kylentt.musicplayer.data.source.local.AsMediaItem

interface Song : AsMediaItem {

    fun getAlbumName(): String
    fun getArtistName(): String
    fun getDisplayTitle(): String
    fun getUri(): Uri

}

object SongDefaults {
    const val mediaItemBuilderKey = "Builder"
}

data class Artist(val name: String)
data class Album(val title: String)

data class AlbumArtists(val album: Album, val artists: List<Artist>)
data class ArtistAlbums(val artist: Artist, val album: List<Album>)