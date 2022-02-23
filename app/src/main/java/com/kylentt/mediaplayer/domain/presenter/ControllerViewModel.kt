package com.kylentt.mediaplayer.domain.presenter

import android.content.Context
import android.media.session.PlaybackState
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ControllerViewModel @Inject constructor(
    private val connector: ServiceConnectorImpl
) : ViewModel() {

    /** Connector State */
    val isPlaying = connector.isPlaying
    val playerIndex = connector.playerIndex
    val mediaItem = connector.mediaItem
    val mediaItems = connector.mediaItems

    init {
        if (!connector.isServiceConnected()) {
            connector.connectService()
        }
    }
}