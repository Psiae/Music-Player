package com.flammky.musicplayer.library.localsong.data

import kotlinx.coroutines.Deferred

interface LocalSongRepository {
	suspend fun getEntitiesAsync(cache: Boolean): Deferred<List<LocalSongEntity>>
}
