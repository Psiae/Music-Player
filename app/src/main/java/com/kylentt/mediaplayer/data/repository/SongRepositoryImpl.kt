package com.kylentt.mediaplayer.data.repository

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
import com.kylentt.mediaplayer.core.util.removeSuffix
import com.kylentt.mediaplayer.data.source.local.LocalSourceImpl
import com.kylentt.mediaplayer.domain.model.Song
import com.kylentt.mediaplayer.domain.model.getDisplayTitle
import jp.wasabeef.transformers.coil.CropSquareTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream


// Local Repository for available Song

class SongRepositoryImpl(
    private val source: LocalSourceImpl,
    private val context: Context,
    private val coil: ImageLoader
) : SongRepository {

    private var songList = listOf<Song>()

    // Just make ALl of them Flow idc just like it

    override suspend fun getSongs(): Flow<List<Song>> = flow { emit(songList)
        source.fetchSong().collect { songList = it
            emit(it)
        }
    }

    override suspend fun fetchSongs(): Flow<List<Song>> = flow {
        source.fetchSong().collect { songList = it ; emit(it)}
    }

    suspend fun fetchSongFromDocs(uri : Uri) = withContext(Dispatchers.Default) { flow {
        source.fetchSongFromDocument(uri).collect { emit(it) }
    } }

    suspend fun fetchMetaFromUri(uri: Uri) = withContext(Dispatchers.Default) { flow {
        source.fetchMetadataFromUri(uri).collect { emit(it) }
    } }
}