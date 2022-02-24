package com.kylentt.mediaplayer.domain.presenter

import android.content.Context
import android.media.session.PlaybackState
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import com.kylentt.mediaplayer.data.repository.SongRepository
import com.kylentt.mediaplayer.data.repository.SongRepositoryImpl
import com.kylentt.mediaplayer.domain.model.Song
import com.kylentt.mediaplayer.domain.model.toMediaItem
import com.kylentt.mediaplayer.domain.model.toMediaItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ControllerViewModel @Inject constructor(
    private val repository: SongRepositoryImpl,
    private val connector: ServiceConnectorImpl
) : ViewModel() {

    /** Connector State */
    val isPlaying = connector.isPlaying
    val playerIndex = connector.playerIndex
    val mediaItem = connector.mediaItem
    val mediaItems = connector.mediaItems

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getSongs().collect { songs ->
                connector.songList = songs
                withContext(Dispatchers.Main) {
                    if (!connector.isServiceConnected()) {
                        connector.connectService()
                    }
                }
                delay(5000)
                withContext(Dispatchers.Main) {
                    justPrepare(songs)
                }
            }
        }
    }

    fun justPrepare(list: List<Song>) {
        connector.mediaController.addMediaItems(list.toMediaItems())
        connector.mediaController.prepare()
        connector.mediaController.playWhenReady = true
    }
}