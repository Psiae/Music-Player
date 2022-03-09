package com.kylentt.mediaplayer.data.repository

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import coil.ImageLoader
import com.kylentt.mediaplayer.data.source.local.LocalSourceImpl
import com.kylentt.mediaplayer.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber


// Local Repository for available Song

class SongRepositoryImpl(
    private val localSource: LocalSourceImpl,
    private val context: Context,
    private val coil: ImageLoader
) : SongRepository {

    private var songList = listOf<Song>()

    // Just make ALl of them Flow idc just like it

    override suspend fun getLocalSongs(): Flow<List<Song>> = flow {
        emit(songList)
        localSource.fetchSong().collect {
            songList = it
            emit(it)
        }
    }

    override suspend fun fetchLocalSongs(): Flow<List<Song>> = flow {
        localSource.fetchSong().collect {
            emit(it)
        }
    }

    suspend fun fetchMetaFromUri(uri: Uri) = withContext(Dispatchers.IO) { flow {
        localSource.fetchMetadataFromUri(uri).collect { emit(it) }
    } }

    // Get Data from this uri Column, for now its Display_Name, ByteSize & ( _data or lastModified )
    suspend fun fetchSongsFromDocs(uri : Uri) = withContext(Dispatchers.IO) {
        var list = listOf<Song>()
        localSource.fetchSong().collect { list = it }

        flow { // for now I will just do check then return without brute forcing it
            var toReturn: Pair<Song, List<Song>>? = null
            localSource.fetchSongFromDocument(uri).collect { _column ->
                _column?.let { column ->
                    val a = column.first
                    val b = column.second
                    val c = column.third

                    // check if it has _data
                    if (c.endsWith(a)) list.find { c == it.data }?.let {
                        Timber.d("IntentHandler FetchFromDocs ${it.data} found")
                        toReturn = Pair(it, list)
                        return@collect
                    }
                    if (c.toLongOrNull() != null) list.find { c.contains(it.lastModified.toString()) && b == it.byteSize }?.let {
                        Timber.d("IntentHandler FetchFromDocs ${it.lastModified} found")
                        toReturn = Pair(it, list)
                        return@collect
                    }

                    // 1 Minute Difference So I guess its fine?
                    if (c.toLongOrNull() != null) list.find { c.contains(it.lastModified.toString().take(8)) && b == it.byteSize }?.let {
                        Timber.d("IntentHandler FetchFromDocs first 8 of ${it.lastModified} found")
                        toReturn = Pair(it, list)
                        return@collect
                    }

                    Timber.d("IntentHandler FetchFromDocs nothing found $_column")
                }
            }

            return@flow emit(toReturn)
        }
    }




}