package com.kylentt.mediaplayer.domain.presenter

import android.net.Uri
import android.widget.Toast
import androidx.annotation.FloatRange
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.kylentt.mediaplayer.data.repository.SongRepositoryImpl
import com.kylentt.mediaplayer.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.File
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

    // might add some more
    fun handlePlayIntent(name: String, byte: Long, lastModified: Long) = viewModelScope.launch {
        connector.connectService()
        handlePlayIntentFromRepo(name, byte, lastModified)
    }

    private suspend fun handlePlayIntentFromRepo(
        name: String,
        byte: Long,
        lastModified: Long
    ) = withContext(Dispatchers.Default) { repository.getSongs().collectLatest { list ->
        if (list.isEmpty()) return@collectLatest
        val song = list.find {
            lastModified.toString().contains(it.lastModified.toString()) && it.fileName == name
        } ?: list.find {
            it.fileName == name && it.byteSize == byte
        } ?: list.find {
            it.lastModified == lastModified
        } ?: list.find {
            it.fileName == name
        } ?: run { Timber.e("Idk what happen but it goes here for whatever reason") ; null }
        song?.let {
            withContext(Dispatchers.Main) {
                connector.controller(true) {
                    Timber.d("Controller Handle Intent by Connector")
                    it.setMediaItems(list.toMediaItems(), list.indexOf(song), 0)
                    it.prepare()
                    it.playWhenReady = true
                    Timber.d("Controller Handle Intent From Repo Handled")
                }
            }
        }
    } }

    private val mainUpdater = CoroutineScope( Dispatchers.Main + Job() )
    private val ioUpdater = CoroutineScope( Dispatchers.IO + Job() )

    init {

        connector.connectService()
        ioUpdater.launch {
            connector.positionEmitter().collect {
                Timber.d("positionEmitter isValid $it")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (mainUpdater.isActive) mainUpdater.cancel()
        if (ioUpdater.isActive) ioUpdater.cancel()
    }
}