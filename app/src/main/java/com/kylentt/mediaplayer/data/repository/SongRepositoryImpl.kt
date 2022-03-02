package com.kylentt.mediaplayer.data.repository

import android.content.Context
import android.net.Uri
import coil.ImageLoader
import com.kylentt.mediaplayer.data.source.local.LocalSourceImpl
import com.kylentt.mediaplayer.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext


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

    suspend fun fetchMetaFromUri(uri: Uri) = withContext(Dispatchers.Default) { flow {
        source.fetchMetadataFromUri(uri).collect { emit(it) }
    } }

    // Get Data from this uri Column, for now its Display_Name, ByteSize & ( _data or lastModified )
    suspend fun fetchSongsFromDocs(uri : Uri) = withContext(Dispatchers.Default) {
        var list = listOf<Song>()
        source.fetchSong().collect { list = it }

        flow { // for now I will just do check then return without brute forcing it
            var toReturn: Pair<Song, List<Song>>? = null
            source.fetchSongFromDocument(uri).collect { _column ->
                _column?.let { column ->
                    val a = column.first
                    val b = column.second
                    val c = column.third

                    // check if it has _data
                    if (c.endsWith(a)) list.find { c == it.data }?.let {
                        toReturn = Pair(it, list)
                        return@collect
                    }
                    if (c.toLongOrNull() != null) list.find { c.contains(it.lastModified.toString()) && b == it.byteSize }?.let {
                        toReturn = Pair(it, list)
                        return@collect
                    }
                }
            }

            return@flow emit(toReturn)
        }
    }




}