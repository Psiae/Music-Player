package com.flammky.android.medialib.temp.player

import com.flammky.android.medialib.common.mediaitem.MediaItem
import kotlinx.coroutines.Deferred

interface AsyncPlayer {
    val currentMediaItem: Deferred<MediaItem>
    val mediaItems: Deferred<List<MediaItem>>

    suspend fun play(item: MediaItem)
    suspend fun pause()
    suspend fun stop()
    suspend fun prepare()
}