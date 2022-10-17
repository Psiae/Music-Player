package com.flammky.musicplayer.library.localsong.data

import android.graphics.Bitmap
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

interface LocalSongRepository {
	suspend fun getModelsAsync(): Deferred<List<LocalSongModel>>
	suspend fun getModelAsync(id: String): Deferred<LocalSongModel?>
	suspend fun collectArtwork(model: LocalSongModel): Flow<Bitmap?>
	suspend fun collectArtwork(id: String): Flow<Bitmap?>

	suspend fun requestUpdateAsync(): Deferred<List<LocalSongModel>>
}
