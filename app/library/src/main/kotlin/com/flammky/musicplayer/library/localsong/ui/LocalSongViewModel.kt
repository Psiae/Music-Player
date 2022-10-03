package com.flammky.musicplayer.library.localsong.ui

import androidx.lifecycle.ViewModel
import com.flammky.musicplayer.library.localsong.data.LocalSongRepository
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
internal class LocalSongViewModel(
	private val repository: LocalSongRepository
) : ViewModel() {

}
