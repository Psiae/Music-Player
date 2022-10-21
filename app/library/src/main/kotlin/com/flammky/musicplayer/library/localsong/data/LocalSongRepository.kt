package com.flammky.musicplayer.library.localsong.data

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Immutable
import com.flammky.android.medialib.common.mediaitem.MediaItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

interface LocalSongRepository {
	suspend fun getModelsAsync(): Deferred<List<LocalSongModel>>
	suspend fun getModelAsync(id: String): Deferred<LocalSongModel?>
	suspend fun collectArtwork(model: LocalSongModel): Flow<Bitmap?>
	suspend fun collectArtwork(id: String): Flow<Bitmap?>

	suspend fun requestUpdateAndGetAsync(): Deferred<List<LocalSongModel>>
	suspend fun requestUpdateAsync(): Deferred<List<Uri>>

	fun observeAvailable(): Flow<AvailabilityState>

	fun buildMediaItem(build: MediaItem.Builder.() -> Unit): MediaItem

	@Immutable
	data class AvailabilityState(
		val loading: Boolean = false,
		val list: ImmutableList<LocalSongModel> = persistentListOf()
	)
}
