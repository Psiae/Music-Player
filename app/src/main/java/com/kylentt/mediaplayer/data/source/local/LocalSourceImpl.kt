package com.kylentt.mediaplayer.data.source.local

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.kylentt.mediaplayer.core.util.Constants.ALBUM_ART_PATH
import com.kylentt.mediaplayer.core.util.VersionHelper
import com.kylentt.mediaplayer.core.util.removeSuffix
import com.kylentt.mediaplayer.domain.model.Song
import com.kylentt.mediaplayer.domain.model.getDisplayTitle
import jp.wasabeef.transformers.coil.CropSquareTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.Contract
import timber.log.Timber
import java.io.ByteArrayOutputStream

class LocalSourceImpl(
    private val context: Context,
    private val coil: ImageLoader
) : LocalSource {

    // simply query Audio Files from MediaStore
    override suspend fun fetchSong(): Flow<List<Song>> = flow { emit(queryDeviceSong()) }

    suspend fun fetchSongFromDocument(uri: Uri) = withContext(Dispatchers.Default) {
        flow {
            var toReturn: Triple<String, Long, String>? = null
            try {
                context.applicationContext.contentResolver.query(uri,
                    null, null, null, null
                )?.use { cursor ->
                    // OpenableColumns Should always be provided (so far)
                    val nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                    val byteIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)

                    // Identifier Columns, either the deprecated _data for <=Q or lastModified with above
                    // fortunately MediaStore still provide _data columns so either is Fine
                    var identifierIndex = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA)
                    if (identifierIndex == -1) identifierIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    cursor.moveToFirst()
                    val display_name = cursor.getString(nameIndex)
                    val byte_size = cursor.getLong(byteIndex)
                    val identifier = cursor.getString(identifierIndex)

                    Timber.d("IntentHandler LocalSource $display_name $byte_size $identifier")

                    toReturn = Triple(display_name, byte_size, identifier.removeSuffix("000"))
                }
                if (toReturn == null) Toast.makeText(context, "Unsupported", Toast.LENGTH_LONG).show()
                emit(toReturn)
            } catch (e : Exception) {
                Timber.d("IntentHandler LocalSource Error repo")
                e.printStackTrace()
                emit(toReturn)
            }
        }
    }

    suspend fun fetchMetadataFromUri(uri: Uri) = withContext(Dispatchers.Default) { flow {
        try {
            val mtr = MediaMetadataRetriever()
            mtr.setDataSource(context.applicationContext, uri)
            val artist = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val title = mtr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val pict = mtr.embeddedPicture
            val art = pict?.let {
                BitmapFactory.decodeByteArray(pict, 0, pict.size).squareWithCoil()
            }
            val bArr = art?.let {
                val stream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.toByteArray()
            }
            val item = MediaItem.Builder()
                .setUri(uri).setMediaMetadata(MediaMetadata.Builder()
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkData(bArr, MediaMetadata.PICTURE_TYPE_MEDIA)
                    .setTitle(title)
                    .setDisplayTitle(title)
                    .setSubtitle(artist ?: album)
                    .setMediaUri(uri)
                    .build()
                ).build()
            Timber.d("SongRepository Handled Uri to MediaItem ${item.getDisplayTitle}")
            emit(item)
        } catch (e : Exception) {
            if (e is IllegalArgumentException) withContext(Dispatchers.Main) {
                e.printStackTrace()
                Toast.makeText(context, "Unsupported", Toast.LENGTH_LONG).show()
            }
            emit(null)
        }
    } }

    private suspend fun Bitmap.squareWithCoil(): Bitmap? {
        val req = ImageRequest.Builder(context.applicationContext)
            .diskCachePolicy(CachePolicy.DISABLED)
            .transformations(CropSquareTransformation())
            .size(256)
            .scale(Scale.FILL)
            .data(this)
            .build()
        return ((coil.execute(req).drawable) as BitmapDrawable?)?.bitmap
    }

    private suspend fun queryDeviceSong() = withContext(Dispatchers.Default) {
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
                MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.DISPLAY_NAME,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns.DATE_MODIFIED,
                MediaStore.Audio.AudioColumns.SIZE,
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
                        byteSize = byteSize,
                        duration = duration,
                        data = data,
                        fileName = fileName,
                        fileParent = path,
                        fileParentId = pathId.toLong(),
                        imageUri = imageUri,
                        lastModified = dateModified,
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
        deviceSong.forEach {
            Timber.d("LocalSourceImpl QueryDeviceSong $it \n")
        }
        deviceSong
    }
}