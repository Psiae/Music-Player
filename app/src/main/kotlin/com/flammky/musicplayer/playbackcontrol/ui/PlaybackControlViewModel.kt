package com.flammky.musicplayer.playbackcontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.musicplayer.playbackcontrol.domain.usecase.ObservePlaybackInfo
import kotlinx.coroutines.launch

internal class PlaybackControlViewModel(
	observePlaybackInfo: ObservePlaybackInfo
) : ViewModel() {




	init {
		viewModelScope.launch {
			observePlaybackInfo.observe()
		}
	}
}
