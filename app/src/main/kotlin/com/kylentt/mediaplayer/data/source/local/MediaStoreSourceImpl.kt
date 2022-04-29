package com.kylentt.mediaplayer.data.source.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.kylentt.mediaplayer.app.AppDispatchers
import com.kylentt.mediaplayer.helper.VersionHelper
import com.kylentt.disposed.musicplayer.core.helper.MediaItemUtil
import com.kylentt.disposed.musicplayer.data.entity.SongEntity
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

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
) : SongEntity {

  override fun toString(): String {
    return "MediaStoreSong" +
      "\ntitle: $title" +
      "\nalbum: $album" +
      "\nartist: $artist" +
      "\nduration: $duration" +
      "\ndata: $data" +
      "\nmediaId: $mediaId" +
      "\nmediaUri: $mediaUri" +
      "\nextra: $albumArtUri, $albumId, $artistId, $byteSize, $fileName, $fileBucket, " +
      "$fileBuckedId, $lastModified"
  }

  override fun getAlbumName(): String = this.album
  override fun getArtistName(): String = this.artist
  override fun getDisplayTitle(): String = this.title
  override fun getUri(): Uri = mediaUri

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
          .setIsPlayable(duration > 0)
          .build()
      ).build()
  }
}

interface MediaStoreSource {
  suspend fun getMediaStoreSong(): Flow<List<MediaStoreSong>>
}

@Singleton
class MediaStoreSourceImpl(
  private val context: Context,
  private val dispatchers: AppDispatchers
) : MediaStoreSource {

  override suspend fun getMediaStoreSong(): Flow<List<MediaStoreSong>> =
    flow { emit(queryAudioColumn()) }
      .flowOn(dispatchers.io)

  private suspend fun queryAudioColumn(): List<MediaStoreSong> =
    withContext(coroutineContext) {
      val songList = mutableListOf<MediaStoreSong>()
      try {

        val songFolderName = if (VersionHelper.hasQ()) {
          MediaStore.Audio.AudioColumns.BUCKET_DISPLAY_NAME
        } else {
          MediaStore.Audio.AudioColumns.DATA
        }

        val songFolderId = if (VersionHelper.hasQ()) {
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
        val nCursor = context.contentResolver.query(
          MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
          projector, selector, null, selectOrder
        )

        nCursor?.use { cursor ->

          while (cursor.moveToNext()) {
            ensureActive()

            val songId =
              cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID))
            val album =
              cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM))
            val albumId =
              cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID))
            val artist =
              cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST))
            val artistId =
              cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST_ID))
            val byteSize =
              cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE))

            val d = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA)
            val data = if (d > -1) cursor.getString(d) else "DEPRECATED"

            val dateModified =
              cursor
                .getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_MODIFIED))
            val duration =
              cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION))
            val fileName =
              cursor
                .getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME))
            val title =
              cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE))
            val folder =
              cursor.getString(cursor.getColumnIndexOrThrow(songFolderName))
            val folderId =
              cursor.getString(cursor.getColumnIndexOrThrow(songFolderId))

            val albumUri = ContentUris
              .withAppendedId(Uri.parse(MEDIASTORE_ALBUM_ART_PATH), albumId)
            val songUri = ContentUris
              .withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId.toLong()).toString()

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
      } catch (e: Exception) {
        Timber.e(e)
      }
      songList.forEachIndexed { index, mediaStoreSong ->
        Timber.d("MediaStoreSource Queried: \n$mediaStoreSong\ntotal:$index")
      }
      return@withContext run {
        ensureActive()
        songList
      }
  }

  companion object {
    const val MEDIASTORE_ALBUM_ART_PATH = "content://media/external/audio/albumart"
  }
}
