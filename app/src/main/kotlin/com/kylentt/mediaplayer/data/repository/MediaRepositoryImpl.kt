package com.kylentt.mediaplayer.data.repository

import android.content.Context
import com.kylentt.mediaplayer.data.source.local.MediaStoreSong
import com.kylentt.mediaplayer.data.source.local.MediaStoreSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

interface MediaRepository {
  suspend fun getMediaStoreSong(): Flow<List<MediaStoreSong>>
}

@Singleton
class MediaRepositoryImpl(
  private val context: Context,
  private val mediaStore: MediaStoreSource
) : MediaRepository {

  override suspend fun getMediaStoreSong() = mediaStore.getMediaStoreSong()
    .map { songs -> songs.filter { it.duration > 0L } }
}
