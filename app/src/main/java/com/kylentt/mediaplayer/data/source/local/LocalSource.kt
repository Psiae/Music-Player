package com.kylentt.mediaplayer.data.source.local

import com.kylentt.mediaplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow


// Local Source or Device Storage
// TODO: Make Downloadable Remote Source
interface LocalSource {

    suspend fun fetchSong(): Flow<List<Song>>
}