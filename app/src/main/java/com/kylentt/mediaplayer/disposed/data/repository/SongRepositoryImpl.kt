package com.kylentt.mediaplayer.disposed.data.repository

import android.content.Context
import android.net.Uri
import com.kylentt.mediaplayer.disposed.data.source.local.LocalSourceImpl
import com.kylentt.mediaplayer.disposed.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber


// Local Repository for available Song

class SongRepositoryImpl(
    private val localSource: LocalSourceImpl,
    private val context: Context
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

    suspend fun fetchMetaFromUri(uri: Uri) =  flow {
        localSource.fetchMetadataFromUri(uri).collect { emit(it) }
    }

    // Get Data from this uri Column, for now its Display_Name, ByteSize & ( _data or lastModified )
    suspend fun fetchSongsFromDocs(uri : Uri) = flow {
        var toReturn: Pair<Song, List<Song>>? = null

        localSource.fetchSongFromDocument(uri).collect column@ { _column ->
            Timber.d("IntentHandler fetchSongFromDocument $_column")

            _column?.let { column ->
                Timber.d("IntentHandler fetching Song $column")
                localSource.fetchSong().collect { list ->
                    Timber.d("IntentHandler fetchSong Collected")
                    val a = column.first
                    val b = column.second
                    // TODO wrap it
                    val c = column.third

                    // check if it has _data
                    if (c.endsWith(a)) list.find { c == it.data }?.let {
                        Timber.d("IntentHandler FetchFromDocs ${it.data} found")
                        toReturn = Pair(it, list)
                        return@collect
                    }

                    if (c.toLongOrNull() != null) list.find { c.contains(it.lastModified.toString()) && b == it.byteSize && a == it.fileName }?.let {
                        Timber.d("IntentHandler FetchFromDocs ${it.lastModified} found")
                        toReturn = Pair(it, list)
                        return@collect
                    }

                    // 1 Minute Difference So I guess its fine?
                    if (c.toLongOrNull() != null) list.find { c.contains(it.lastModified.toString().take(8)) && b == it.byteSize && a == it.fileName}?.let {
                        Timber.d("IntentHandler FetchFromDocs first 8 of ${it.lastModified} found")
                        toReturn = Pair(it, list)
                        return@collect
                    }

                    Timber.d("IntentHandler FetchFromDocs nothing found $_column ${list.find { it.fileName == a }}")
                }
            }
        }
        emit(toReturn)
    }.flowOn(Dispatchers.IO)
}

/** Problem Notes :
 * There's chances MediaStore show different filename than document provider. usually MediaStore identified it as copies*/