package com.kylentt.musicplayer.data.repository

import android.content.Context
import com.kylentt.musicplayer.data.source.local.MediaStoreSource
import kotlinx.coroutines.flow.flow

class MediaRepository(
  private val context: Context,
  private val mediaStore: MediaStoreSource
) {

  suspend fun getMediaStoreSong() = flow {
    emit(mediaStore.getMediaStoreSong())
  }


}
