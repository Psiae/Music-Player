package com.kylentt.mediaplayer.domain.presenter

import androidx.annotation.FloatRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.kylentt.mediaplayer.data.repository.SongRepositoryImpl
import com.kylentt.mediaplayer.domain.model.toMediaItems
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ControllerViewModel @Inject constructor(
    private val connector: ServiceConnectorImpl,
    private val repository: SongRepositoryImpl
) : ViewModel() {

    /** Connector State */
    val mediaItems = connector.mediaItems
    val mediaItem = connector.mediaItem
    val isPlaying = connector.isPlaying
    val playerIndex = connector.playerIndex
    val position = connector.position
    val duration = connector.duration

    /** Controller Command */
    fun play(f: Boolean) = connector.controller(f) { it.play() }
    fun stop(f: Boolean) = connector.controller(f) { it.stop() }
    fun pause(f: Boolean) = connector.controller(f) { it.pause() }
    fun prepare(f: Boolean) = connector.controller(f) { it.prepare() }
    fun skipToNext(f: Boolean) = connector.controller(f) { it.seekToNext() }
    fun skipToPrev(f: Boolean) = connector.controller(f) { it.seekToPrevious() }
    fun getItemAt(f: Boolean, i: Int) = connector.controller(f) { it.getMediaItemAt(i) }

    private val mainUpdater = CoroutineScope( Dispatchers.Main + Job() )
    private val ioUpdater = CoroutineScope( Dispatchers.IO + Job() )

    init {
        viewModelScope.launch {
            repository.getSongs().collect { songs ->
                if (!connector.isServiceConnected()) {
                    connector.controller(true) {
                        with(it) {
                            songs.forEach { song ->
                                Timber.d(" size : ${songs.size}")
                                Timber.d(" title : ${song.title}")
                            }
                            setMediaItems(songs.toMediaItems())
                            prepare()
                        }
                    }
                }
            }

            connector.connectService()
            viewModelScope.launch {
                connector.positionEmitter().collect {
                    Timber.d("positionEmitter isValid $it")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (mainUpdater.isActive) mainUpdater.cancel()
        if (ioUpdater.isActive) ioUpdater.cancel()
    }
}