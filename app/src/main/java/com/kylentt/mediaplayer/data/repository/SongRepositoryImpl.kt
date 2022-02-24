package com.kylentt.mediaplayer.data.repository

import com.kylentt.mediaplayer.data.source.local.LocalSource
import com.kylentt.mediaplayer.data.source.local.LocalSourceImpl
import com.kylentt.mediaplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import timber.log.Timber


// Local Repository for available Song

class SongRepositoryImpl(
    private val source: LocalSourceImpl
) : SongRepository {

    var songList = listOf<Song>()

    override suspend fun getSongs(): Flow<List<Song>> {
        return flow { source.fetchSong().collect {
            songList = it
            emit(it)
        } }
    }
}