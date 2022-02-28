package com.kylentt.mediaplayer.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaDataSource
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.kylentt.mediaplayer.core.util.removeSuffix
import com.kylentt.mediaplayer.data.source.local.LocalSource
import com.kylentt.mediaplayer.data.source.local.LocalSourceImpl
import com.kylentt.mediaplayer.domain.model.Song
import com.kylentt.mediaplayer.domain.model.getDisplayTitle
import jp.wasabeef.transformers.coil.CropSquareTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.lang.Exception


// Local Repository for available Song

class SongRepositoryImpl(
    private val source: LocalSourceImpl,
    private val context: Context,
    private val coil: ImageLoader
) : SongRepository {

    var songList = listOf<Song>()

    override suspend fun getSongs(): Flow<List<Song>> {
        return flow {
            emit(songList)
            source.fetchSong().collect {
            songList = it
            emit(it)
        } }
    }

    fun fetchSongs(): Flow<List<Song>> {
        return flow { source.fetchSong().collect { songList = it ; emit(it)} }
    }

    suspend fun fetchSongFromDocs(uri: Uri) = withContext(Dispatchers.Default) {
        var toReturn: Triple<String, Long, Long>? = null
        context.applicationContext.contentResolver.query(uri,
            null, null, null, null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val byteIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            val lastIndex = cursor.getColumnIndex(
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )
            while(cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                val byte = cursor.getLong(byteIndex)
                val last = cursor.getLong(lastIndex)
                Timber.d("MainActivity intent handler $name $byte $last ")
                toReturn = Triple(name, byte, last.removeSuffix("000"))
            }
        }
        if (toReturn == null) Toast.makeText(context, "Unsupported", Toast.LENGTH_LONG).show()
        toReturn
    }

    suspend fun fetchMetaFromUri(uri: Uri) = withContext(Dispatchers.Default) {
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
            item
        } catch (e : Exception) {
            if (e is IllegalArgumentException) withContext(Dispatchers.Main) {
                e.printStackTrace()
                Toast.makeText(context, "Unsupported", Toast.LENGTH_LONG).show()
            } ; null
        }
    }

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
}