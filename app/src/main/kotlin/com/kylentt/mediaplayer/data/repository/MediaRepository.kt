package com.kylentt.mediaplayer.data.repository

import android.content.Context
import com.kylentt.mediaplayer.data.source.local.MediaStoreSong
import com.kylentt.mediaplayer.data.source.local.MediaStoreSource
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Repository Containing Media related data.
 * @property [getMediaStoreSong] @returns [Flow] of List<[MediaStoreSong]>
 * @see [MediaStoreSource]
 * @see [MediaRepositoryImpl]
 * @author Kylentt
 * @since 2022/04/30
 */

interface MediaRepository {
  suspend fun getMediaStoreSong(): Flow<List<MediaStoreSong>>
  suspend fun getMediaStoreSongById(id: String): MediaStoreSong?
  suspend fun getMediaStoreSongById(ids: List<String>): List<MediaStoreSong?>
}

@Singleton
class MediaRepositoryImpl(
  private val context: Context,
  private val mediaStore: MediaStoreSource
) : MediaRepository {

  override suspend fun getMediaStoreSong(): Flow<List<MediaStoreSong>> {
    return mediaStore.getMediaStoreSong().map { songs -> songs.filter { it.duration > 0L } }
  }

  override suspend fun getMediaStoreSongById(id: String): MediaStoreSong? {
    return getMediaStoreSong().first().find { it.songMediaId == id }
  }

	override suspend fun getMediaStoreSongById(ids: List<String>): List<MediaStoreSong?> {
		val toReturn = mutableListOf<MediaStoreSong>()
		val list = getMediaStoreSong().first()
		ids.forEach { id ->
			list.firstOrNull { song -> id == song.mediaId }?.let { toReturn.add(it) }
		}
		coroutineContext.ensureActive()
		return toReturn
	}
}
