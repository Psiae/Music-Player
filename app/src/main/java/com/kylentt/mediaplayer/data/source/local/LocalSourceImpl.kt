package com.kylentt.mediaplayer.data.source.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.kylentt.mediaplayer.core.util.Constants.ALBUM_ART_PATH
import com.kylentt.mediaplayer.core.util.VersionHelper
import com.kylentt.mediaplayer.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber

class LocalSourceImpl(
    private val context: Context
) : LocalSource {

    override suspend fun fetchSong(): Flow<List<Song>> = flow { emit(queryDeviceSong()) }

    private suspend fun queryDeviceSong() = withContext(Dispatchers.IO) {
        val deviceSong = mutableListOf<Song>()
        try {
            // Folder Name
            val songPath =
                if (VersionHelper.isQ()) MediaStore.Audio.AudioColumns.BUCKET_DISPLAY_NAME
                else MediaStore.Audio.AudioColumns.DATA
            val songPathId =
                if (VersionHelper.isQ()) MediaStore.Audio.AudioColumns.BUCKET_ID
                else MediaStore.Audio.AudioColumns.DATA

            val projector = arrayOf(
                MediaStore.Audio.AudioColumns._ID,
                MediaStore.Audio.AudioColumns.ALBUM,
                MediaStore.Audio.AudioColumns.ALBUM_ID,
                MediaStore.Audio.AudioColumns.ARTIST,
                MediaStore.Audio.AudioColumns.ARTIST_ID,
                MediaStore.Audio.AudioColumns.DISPLAY_NAME,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns.TITLE,
                songPath,
                songPathId
            )
            val selector ="${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val selectOrder =  MediaStore.Audio.Media.DEFAULT_SORT_ORDER
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projector, selector, null, selectOrder
            )

            cursor?.use {
                while (cursor.moveToNext()) {
                    val songId = cursor.getLong(
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
                    val fileName = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
                    )
                    val duration = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
                    )
                    val title = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)
                    )
                    val path = cursor.getString(
                        cursor.getColumnIndexOrThrow(songPath)
                    )
                    val pathId = cursor.getString(
                        cursor.getColumnIndexOrThrow(songPathId)
                    )
                    val imageUri = ContentUris.withAppendedId(
                        Uri.parse(ALBUM_ART_PATH), albumId
                    ).toString()
                    val songUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId
                    ).toString()

                    Song(
                        album = album,
                        albumId = albumId.toString(),
                        artist = artist,
                        artistId = artistId.toString(),
                        duration = duration,
                        fileName = fileName,
                        imageUri = imageUri,
                        mediaId = songId.toString(),
                        mediaUri = songUri,
                        title = title
                    ).also {
                        if (it.duration != 0L) deviceSong.add(it)
                        // TODO: handle Corrupt Song
                    }
                }
            }
        } catch (e : Exception) {
            Timber.e(e)
        }
        deviceSong
    }
}