package com.flammky.musicplayer.library.dump.localmedia.data

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Immutable
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

interface LocalSongRepository {
	suspend fun getModelsAsync(): Deferred<List<LocalSongModel>>
	suspend fun getModelAsync(id: String): Deferred<LocalSongModel?>
	suspend fun collectArtwork(model: LocalSongModel): Flow<Bitmap?>
	suspend fun collectArtwork(id: String): Flow<Bitmap?>

	suspend fun collectMetadata(id: String): Flow<AudioMetadata?>

	@Deprecated(message = "don't use", level = DeprecationLevel.ERROR)
	suspend fun requestUpdateAndGetAsync(): Deferred<List<LocalSongModel>>
	@Deprecated(message = "don't use", level = DeprecationLevel.ERROR)
	suspend fun requestUpdateAsync(): Deferred<List<Uri>>

	fun observeAvailable(): Flow<AvailabilityState>
	fun refreshMetadata(id: String): Job
	fun refreshMetadata(id: String, uri: Uri): Job
	fun refreshMetadata(model: LocalSongModel): Job
	fun refreshArtwork(id: String): Job
	fun refreshArtwork(id: String, uri: Uri): Job
	fun refreshArtwork(model: LocalSongModel): Job

	suspend fun requestMetadata(id: String): Result<AudioMetadata?>

	suspend fun requestArtwork(id: String): Result<Bitmap?>

	fun cachedMetadata(id: String): AudioMetadata?

	fun cachedArtwork(id: String): Bitmap?


	@Immutable
	data class AvailabilityState(
		val loading: Boolean = false,
		val list: ImmutableList<LocalSongModel> = persistentListOf()
	)
}
