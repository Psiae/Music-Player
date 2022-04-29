package com.kylentt.disposed.disposed.ui.mainactivity.disposed.compose.screen.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class HomeViewModel @Inject constructor(
  private val manager: MediaSessionManager
) : ViewModel() {

  val playerItemState = mutableStateOf<MediaItem>(MediaItem.EMPTY)
  val playerPlaybackState = mutableStateOf<@Player.State Int>(Player.STATE_IDLE)

  init {
    viewModelScope.launch {
      manager.playbackState.collect {
          playerPlaybackState.value = it.playerState
          playerItemState.value = it.currentMediaItem
      }
    }
  }

}
