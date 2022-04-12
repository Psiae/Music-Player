package com.kylentt.musicplayer.data.source.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kylentt.mediaplayer.core.util.helper.VersionHelper
import com.kylentt.musicplayer.core.helper.MediaItemUtil
import com.kylentt.musicplayer.data.ProviderConstants
import com.kylentt.musicplayer.data.entity.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.lang.Exception

data class MediaStoreSong(
  val album: String,
  val albumArtUri: Uri,
  val albumId: Long,
  val artist: String,
  val artistId: Long,
  val byteSize: Long,
  val duration: Long,
  val data: String?,
  val fileName: String,
  val fileBucket: String,
  val fileBuckedId: String,
  val lastModified: Long,
  val mediaId: String,
  val mediaUri: Uri,
  val title: String
) : Song {

  override fun getAlbumName(): String {
    return album
  }

  override fun getArtistName(): String {
    return artist
  }

  override fun getDisplayTitle(): String {
    return title
  }

  override fun getUri(): Uri {
    return mediaUri
  }

  override fun toMediaItem(): MediaItem {
    return MediaItem.Builder()
      .setMediaId(mediaId)
      .setUri(mediaUri)
      .setMediaMetadata(
        MediaMetadata.Builder()
          .setArtist(getArtistName())
          .setAlbumArtist(getArtistName())
          .setAlbumTitle(getAlbumName())
          .setArtworkUri(MediaItemUtil.hideArtUri(albumArtUri))
          .setDisplayTitle(getDisplayTitle())
          .setDescription(fileName)
          .setMediaUri(mediaUri)
          .setSubtitle(artist)
          .setIsPlayable(true)
          .build()
      ).build()
  }
}

interface AsMediaItem {
  fun toMediaItem(): MediaItem
}

class MediaStoreSource(
  private val context: Context
) : LocalSource {
  suspend fun getMediaStoreSong() = flow { emit(queryAudioColumn()) }.flowOn(Dispatchers.IO)

  private suspend fun queryAudioColumn(): List<MediaStoreSong> {

    val songList = mutableListOf<MediaStoreSong>()

    try {

      val songFolderName = if (VersionHelper.isQ()) {
        MediaStore.Audio.AudioColumns.BUCKET_DISPLAY_NAME
      } else {
        MediaStore.Audio.AudioColumns.DATA
      }

      val songFolderId = if (VersionHelper.isQ()) {
        MediaStore.Audio.AudioColumns.BUCKET_ID
      } else {
        MediaStore.Audio.AudioColumns.DATA
      }

      val projector = arrayOf(
        MediaStore.Audio.AudioColumns._ID,
        MediaStore.Audio.AudioColumns.ALBUM,
        MediaStore.Audio.AudioColumns.ALBUM_ID,
        MediaStore.Audio.AudioColumns.ARTIST,
        MediaStore.Audio.AudioColumns.ARTIST_ID,
        MediaStore.Audio.AudioColumns.DISPLAY_NAME,
        MediaStore.Audio.AudioColumns.DURATION,
        MediaStore.Audio.AudioColumns.DATA,
        MediaStore.Audio.AudioColumns.DATE_MODIFIED,
        MediaStore.Audio.AudioColumns.SIZE,
        MediaStore.Audio.AudioColumns.TITLE,
        songFolderName,
        songFolderId,
      )

      val selector = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
      val selectOrder = MediaStore.Audio.Media.TITLE
      val cursor = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projector, selector, null, selectOrder
      )
      cursor?.use {

        while (cursor.moveToNext()) {

          val songId = cursor.getString(
            cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
          )
          val album = cursor.getString(
            cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM)
          )
          val albumId = cursor.getLong(
            cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID)
          )
          val artist = cursor.getString(
            cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST)
          )
          val artistId = cursor.getLong(
            cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST_ID)
          )
          val byteSize = cursor.getLong(
            cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE)
          )
          val d = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA)

          val data = if (d > -1) cursor.getString(d) else "DEPRECATED"

          val dateModified = cursor.getLong(
            cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_MODIFIED)
          )

          val duration = cursor.getLong(
            cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
          )

          val fileName = cursor.getString(
            cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
          )
          val title = cursor.getString(
            cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
          )
          val folder = cursor.getString(
            cursor.getColumnIndexOrThrow(songFolderName)
          )
          val folderId = cursor.getString(
            cursor.getColumnIndexOrThrow(songFolderId)
          )

          val albumUri = ContentUris.withAppendedId(
            Uri.parse(ProviderConstants.MEDIASTORE_ALBUM_ART_PATH),
            albumId
          )
          val songUri =
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId.toLong())
              .toString()

          if (duration != 0L) {
            songList.add(
              MediaStoreSong(
                album = album,
                albumArtUri = albumUri,
                albumId = albumId,
                artist = artist,
                artistId = artistId,
                byteSize = byteSize,
                duration = duration,
                data = data,
                fileName = fileName,
                fileBucket = folder,
                fileBuckedId = folderId,
                lastModified = dateModified,
                mediaId = songId,
                mediaUri = songUri.toUri(),
                title = title
              )
            )
          }
        }
      }
    } catch (e: Exception) {
      Timber.e(e)
    }
    songList.forEach {
      Timber.d("MediaStoreSource Queried $it \n")
    }
    return songList
  }
}
