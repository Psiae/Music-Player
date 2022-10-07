package com.flammky.musicplayer.library.localsong.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.flammky.musicplayer.library.localsong.data.LocalSongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class LocalSongViewModel @Inject constructor(
	private val repository: LocalSongRepository
) : ViewModel() {

	private val _refreshing = mutableStateOf(false)
	val refreshing: State<Boolean> = _refreshing.derive()

	val listState = mutableStateOf<List<LocalSongModel>>(emptyList())


	private fun <T> State<T>.derive(calculation: (T) -> T = { it }): State<T> {
		return derivedStateOf { calculation(value) }
	}
}


