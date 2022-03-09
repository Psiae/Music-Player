package com.kylentt.mediaplayer.domain.presenter

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import com.kylentt.mediaplayer.core.util.MediaItemHandler
import com.kylentt.mediaplayer.data.repository.SongRepositoryImpl
import com.kylentt.mediaplayer.domain.model.toMediaItems
import com.kylentt.mediaplayer.domain.presenter.util.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ControllerViewModel @Inject constructor(
    private val connector: ServiceConnectorImpl,
    private val repository: SongRepositoryImpl,
    private val itemHandler: MediaItemHandler
) : ViewModel() {

    /** Connector State */
    val serviceState = connector.serviceState
    fun checkServiceState() = connector.checkServiceState()
    fun connectService(onConnected: (MediaController) -> Unit) {
        connector.connectService(onConnected = onConnected)
    }

    val playerPlaybackState = mutableStateOf<State.PlayerState.PlayerPlaybackState>(State.PlayerState.PlayerPlaybackState.Unit)
    val playerCurrentPlaystate = mutableStateOf("Unit")
    val playerCurrentMediaItem = mutableStateOf(MediaItem.EMPTY)

    val position = connector.position
    val duration = connector.duration

    private suspend fun collectPlayerItemState() {
        connector.playerItemState.collect {
            when (it) {
                is State.PlayerState.PlayerItemState.CurrentlyPlaying -> {
                    playerCurrentMediaItem.value = it.CurrentMediaItem
                }
            }
        }
    }

    // TODO : some processing
    private suspend fun collectPlayerState() {
        connector.playerPlaybackState.collect {
            when (it) {
                is State.PlayerState.PlayerPlaybackState.Idle -> {
                    playerPlaybackState.value = it
                    playerCurrentPlaystate.value = "Idle"
                }
                is State.PlayerState.PlayerPlaybackState.Buffering -> {
                    playerPlaybackState.value = it
                    playerCurrentPlaystate.value = "Buffering"
                }
                is State.PlayerState.PlayerPlaybackState.Ready -> {
                    playerPlaybackState.value = it
                    playerCurrentPlaystate.value = "Ready"
                }
                is State.PlayerState.PlayerPlaybackState.Ended -> {
                    playerPlaybackState.value = it
                    playerCurrentPlaystate.value = "Ended"
                }
                is State.PlayerState.PlayerPlaybackState.Error -> {
                    playerPlaybackState.value = it
                    playerCurrentPlaystate.value = "Error"
                }
                else -> { Unit }
            }
        }
    }

    init {
        viewModelScope.launch {
            collectPlayerState()
        }
        viewModelScope.launch {
            collectPlayerItemState()
        }
    }

    /** Intent Handler */

    // Intent From Document Provider like FileManager
    suspend fun handleDocsIntent(uri: Uri) = withContext(Dispatchers.Main) {
        repository.fetchSongsFromDocs(uri).collect {
            Timber.d("IntentHandler ViewModel DocsIntent ${it?.first} ${it?.second?.size}")
            it?.let {
                connector.readyWithFade { controller ->
                    controller.stop()
                    controller.setMediaItems(it.second.toMediaItems(), it.second.indexOf(it.first), 0)
                    controller.prepare()
                    controller.playWhenReady = true
                }
            } ?: handleItemIntent(uri)
        }
    }

    suspend fun handleItemIntent(uri: Uri) = withContext(Dispatchers.Main) {
        Timber.d("IntentHandler ItemIntent $uri")
        repository.fetchMetaFromUri(uri).collect { _item ->
            _item?.let { item -> connector.readyWithFade {
                it.setMediaItems(listOf(item),0,0)
                it.prepare()
                it.playWhenReady = true
                Timber.d("Controller IntentHandler handling ${item.mediaMetadata.title} handled")
            } }
        }
    }

    private val mainUpdater = CoroutineScope( Dispatchers.Main + Job() )
    private val ioUpdater = CoroutineScope( Dispatchers.IO + Job() )

    override fun onCleared() {
        super.onCleared()
        if (mainUpdater.isActive) mainUpdater.cancel()
        if (ioUpdater.isActive) ioUpdater.cancel()
    }
}