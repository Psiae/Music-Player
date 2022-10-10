package com.flammky.musicplayer.library.localsong.ui

import android.graphics.Bitmap
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.musicplayer.library.localsong.data.LocalSongModel
import com.flammky.musicplayer.library.localsong.data.LocalSongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
internal class LocalSongViewModel @Inject constructor(
	private val dispatcher: AndroidCoroutineDispatchers,
	private val repository: LocalSongRepository
) : ViewModel() {

	private val _refreshing = mutableStateOf(false)
	val refreshing = _refreshing.derive()

	private val _listState = mutableStateOf<List<LocalSongModel>>(emptyList())
	val listState = _listState.derive()

	suspend fun collectArtwork(item: LocalSongModel): Flow<Bitmap?> {
		return repository.collectArtwork(item)
	}

	private fun <T> State<T>.derive(calculation: (T) -> T = { it }): State<T> {
		return derivedStateOf { calculation(value) }
	}
}


