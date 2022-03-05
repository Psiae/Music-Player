package com.kylentt.mediaplayer.data.repository

import com.kylentt.mediaplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

// Repository that store Songs to be shared with ViewModel & Service

interface SongRepository {

    suspend fun getLocalSongs(): Flow<List<Song>> // fetch available on variable then query
    suspend fun fetchLocalSongs(): Flow<List<Song>>  // directly fetch from query

}