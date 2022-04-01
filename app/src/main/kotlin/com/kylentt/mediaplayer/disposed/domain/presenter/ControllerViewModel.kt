package com.kylentt.mediaplayer.disposed.domain.presenter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.kylentt.mediaplayer.core.util.handler.CoilHandler
import com.kylentt.mediaplayer.core.util.handler.MediaItemHandler
import com.kylentt.mediaplayer.core.util.handler.getDisplayTitle
import com.kylentt.mediaplayer.disposed.data.repository.SongRepositoryImpl
import com.kylentt.musicplayer.core.helper.MediaItemUtil
import com.kylentt.musicplayer.data.MediaRepository
import com.kylentt.musicplayer.domain.mediasession.MediaSessionManager
import com.kylentt.musicplayer.domain.mediasession.service.ControllerCommand
import com.kylentt.musicplayer.domain.mediasession.service.PlaybackState
import com.kylentt.musicplayer.ui.musicactivity.IntentWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@HiltViewModel
internal class ControllerViewModel @Inject constructor(
    private val connector: ServiceConnectorImpl,
    private val manager: MediaSessionManager,
    private val repository: SongRepositoryImpl,
    private val mediaRepo: MediaRepository,
    private val coilHandler: CoilHandler,
    private val itemHandler: MediaItemHandler
) : ViewModel() {

    /** Connector State */
    val serviceState = manager.serviceState
    fun checkServiceState() = connector.checkServiceState()
    fun connectService(onConnected: () -> Unit = {}) {
        manager.connectService()
        onConnected()
    }

    val playerPlaybackState = mutableStateOf<PlaybackState>(PlaybackState.UNIT, policy = neverEqualPolicy())
    val playerCurrentPlaystate = mutableStateOf("Unit")
    val playerCurrentMediaItem = mutableStateOf(MediaItem.EMPTY, policy = structuralEqualityPolicy())
    val playerCurrentBitmap = mutableStateOf<Bitmap?>(null, policy = structuralEqualityPolicy())

    val position = connector.position
    val duration = connector.duration

    private suspend fun collectPlayerState() {
        manager.playbackState.collect { state ->
            when (state) {
                is PlaybackState.BUFFERING -> { playerPlaybackState.value = state }
                is PlaybackState.ENDED -> { playerPlaybackState.value = state }
                is PlaybackState.IDLE -> { playerPlaybackState.value = state }
                is PlaybackState.READY -> { playerPlaybackState.value = state }
                PlaybackState.UNIT -> {
                    if (playerPlaybackState.value !is PlaybackState.UNIT) Timber.wtf("playerPlaybackState is changed to Unit")
                    else Timber.d("PlayerState UNIT")
                }
            }
        }
    }

    private suspend fun collectPlayerItem() {
        manager.itemState.collect {
            playerCurrentMediaItem.value = it
            updateBitmap(it)
        }
    }

    private suspend fun updateBitmap(item: MediaItem) = withContext(Dispatchers.IO) {
        val bm = run {
            val barr = itemHandler.getEmbeds(item) ?: item.mediaMetadata.artworkUri?.let { itemHandler.getEmbeds(MediaItemUtil.showArtUri(it)) }
            barr?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        }
        withContext(Dispatchers.Main) {
            playerCurrentBitmap.value = bm
        }
    }

    init {
        Timber.d("ControllerViewModel initialized ${this.hashCode()}")
        viewModelScope.launch {
            collectPlayerItem()
        }
        viewModelScope.launch {
            collectPlayerState()
        }
    }

    /** Intent Handler */

    // Intent From Document Provider like FileManager
    suspend fun handleDocsIntent(intent: IntentWrapper, onHandled: () -> Unit) = withContext(Dispatchers.Main) {
        val uri = intent.data ?: run {
            Timber.e("Intent with null Data sent to ViewModel")
            return@withContext
        }
        repository.fetchSongsFromDocs(uri).collect {
            Timber.d("IntentHandler ViewModel DocsIntent ${it?.first} ${it?.second?.size}")
            it?.let {
                val commands = mutableListOf<ControllerCommand>()
                val mediaList = mutableListOf<MediaItem>()
                it.second.forEach { mediaList.add(it.toMediaItem()) }
                commands.add(ControllerCommand.Stop)
                commands.add(ControllerCommand.SetMediaItems(mediaList, it.second.indexOf(it.first)))
                commands.add(ControllerCommand.Prepare)
                commands.add(ControllerCommand.SetPlayWhenReady(true))
                manager.sendCommand(ControllerCommand.WithFade(ControllerCommand.MultiCommand(commands)))
                onHandled()
            } ?: handleItemIntent(intent, onHandled)
        }
    }

    suspend fun handleItemIntent(intent: IntentWrapper, onHandled: () -> Unit) = withContext(Dispatchers.Main) {
        val uri = intent.data ?: run {
            Timber.e("Intent with null Data sent to ViewModel")
            return@withContext
        }
        Timber.d("IntentHandler ItemIntent $uri")
        repository.fetchMetaFromUri(uri).collect { _item ->
            _item?.let { item ->
                val commands = mutableListOf<ControllerCommand>()
                val mediaList = mutableListOf<MediaItem>()
                mediaList.add(item)
                commands.add(ControllerCommand.Stop)
                commands.add(ControllerCommand.SetMediaItems(mediaList))
                commands.add(ControllerCommand.Prepare)
                commands.add(ControllerCommand.SetPlayWhenReady(true))
                manager.sendCommand(ControllerCommand.WithFade(ControllerCommand.MultiCommand(commands)))
                onHandled()
            }
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