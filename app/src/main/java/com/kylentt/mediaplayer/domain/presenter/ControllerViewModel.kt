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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@HiltViewModel
class ControllerViewModel @Inject constructor(
    private val connector: ServiceConnectorImpl,
    private val repository: SongRepositoryImpl
) : ViewModel() {

    /** Connector State */
    val isPlaying = connector.isPlaying
    val playerIndex = connector.playerIndex
    val mediaItem = connector.mediaItem
    val mediaItems = connector.mediaItems
    val position = MutableStateFlow(-1L)
    val duration = MutableStateFlow(-1L)

    /** Controller Command */
    fun skipToNext() = connector.controller { it.seekToNext() }
    fun skipToPrev() = connector.controller { it.seekToPrevious() }

    fun seekTo(
        @FloatRange(from = 0.0, to = 1.0)
        f: Float
    ) = connector.controller { it.seekTo( (it.duration * f).toLong() ) }
    fun seekTo(l: Long) = connector.controller { it.seekTo(l) }
    fun seekToIndex(i: Int, p: Long = 0) = connector.controller { it.seekTo(i, p) }

    fun setMediaItems(list: List<MediaItem>)
            = connector.controller { it.setMediaItems(list) }

    fun setMediaItems(list: List<MediaItem>, bool: Boolean)
            = connector.controller { it.setMediaItems(list, bool) }

    fun setMediaItems(list: List<MediaItem>, i: Int, p: Long)
            = connector.controller { it.setMediaItems(list, i, p) }

    fun setMediaItem(m: MediaItem)
            = connector.controller { it.setMediaItem(m) }

    fun setMediaItem(m: MediaItem, bool: Boolean)
            = connector.controller { it.setMediaItem(m, bool) }

    fun setMediaItem(m: MediaItem, p: Long)
            = connector.controller { it.setMediaItem(m, p) }

    fun addMediaItems(list: List<MediaItem>)
            = connector.controller { it.addMediaItems(list) }

    fun addMediaItems(list: List<MediaItem>, i: Int)
            = connector.controller { it.addMediaItems(i, list) }
    
    fun addMediaItem(m: MediaItem)
            = connector.controller { it.addMediaItem(m) }
    
    fun addMediaItem(m: MediaItem, i: Int)
            = connector.controller { it.addMediaItem(i, m) }

    fun setRepeatMode(@Player.RepeatMode r: Int)
            = connector.controller { it.repeatMode = r }

    fun prepare() = connector.controller { it.prepare() }
    fun play() = connector.controller { it.play() }
    fun pause() = connector.controller { it.pause() }
    fun stop() = connector.controller { it.stop() }
    fun getItemAt(i: Int) = connector.controller { it.getMediaItemAt(i) }

    private val updater = CoroutineScope( Dispatchers.IO + Job())
    private suspend fun startUpdatePlaying() = withContext(Dispatchers.IO) {
        while (updater.isActive) {
            val pos = connector.getPos()
            val dur = connector.getDur()
            if (pos > -1 && pos <= dur) {
                position.value = pos
                duration.value = pos
            }
            delay(1000)
        }
    }

    init {
        viewModelScope.launch {
            repository.getSongs().collectLatest { songs ->
                connector.controller {
                    with(it) {
                        addMediaItems(songs.toMediaItems())
                    }
                }
            }
            connector.connectService()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (updater.isActive) updater.cancel()
    }
}