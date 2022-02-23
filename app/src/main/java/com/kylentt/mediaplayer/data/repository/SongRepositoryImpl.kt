package com.kylentt.mediaplayer.data.repository

import com.kylentt.mediaplayer.data.source.local.LocalSource
import com.kylentt.mediaplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow


// Local Repository for available Song

class SongRepositoryImpl(
    private val source: LocalSource
) : SongRepository {

    override suspend fun getSongs(): Flow<List<Song>> {
        return source.fetchSong()
    }
}