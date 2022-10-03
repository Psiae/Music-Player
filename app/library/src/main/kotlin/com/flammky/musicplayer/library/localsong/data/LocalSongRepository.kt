package com.flammky.musicplayer.library.localsong.data

import kotlinx.coroutines.Deferred

internal interface LocalSongRepository {
	suspend fun getEntitiesAsync(cache: Boolean): Deferred<List<LocalSongEntity>>
}
