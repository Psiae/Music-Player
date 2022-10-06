package com.flammky.musicplayer.library.localsong.ui

import androidx.lifecycle.ViewModel
import com.flammky.musicplayer.library.localsong.data.LocalSongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class LocalSongViewModel @Inject constructor(
	private val repository: LocalSongRepository
) : ViewModel() {

}
