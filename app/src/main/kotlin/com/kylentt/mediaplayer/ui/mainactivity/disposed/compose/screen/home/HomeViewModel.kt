package com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.screen.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.kylentt.musicplayer.domain.mediasession.MediaSessionManager
import com.kylentt.musicplayer.domain.mediasession.service.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val manager: MediaSessionManager
) : ViewModel() {

    val playerItemState = mutableStateOf<MediaItem>(MediaItem.EMPTY)
    val playerPlaybackState = mutableStateOf<PlaybackState>(PlaybackState.UNIT)

    init {
        viewModelScope.launch {
            manager.itemState.collect { playerItemState.value = it }
        }
        viewModelScope.launch {
            manager.playbackState.collect { playerPlaybackState.value = it }
        }
    }

}