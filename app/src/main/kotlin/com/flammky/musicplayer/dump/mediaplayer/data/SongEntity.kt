package com.flammky.musicplayer.dump.mediaplayer.data

import android.net.Uri
import androidx.media3.common.MediaItem

/**
 * Interface of Song
 * @see [com.flammky.dump.data.source.local.MediaStoreSong]
 * @author Kylentt
 * @since 2022/04/30
 */

interface SongEntity {
  val mediaIdPrefix: String

  val mediaItem: MediaItem
  val albumName: String
	val albumArtist: String
  val artistName: String
  val displayTitle: String
	val songFileName: String
  val songMediaId: String
  val songMediaArtworkUri: Uri
  val songMediaUri: Uri

  fun equalMediaItem(item: MediaItem): Boolean
}

data class Artist(val name: String)
data class Album(val title: String)

data class AlbumArtists(val album: Album, val artists: List<Artist>)
data class ArtistAlbums(val artist: Artist, val albums: List<Album>)
