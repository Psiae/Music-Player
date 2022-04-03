package com.kylentt.mediaplayer.disposed.domain.presenter

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.kylentt.mediaplayer.core.util.handler.CoilHandler
import com.kylentt.mediaplayer.core.util.handler.MediaItemHandler
import com.kylentt.mediaplayer.disposed.data.repository.SongRepositoryImpl
import com.kylentt.musicplayer.data.repository.MediaRepository
import com.kylentt.musicplayer.data.repository.ProtoRepository
import com.kylentt.musicplayer.domain.mediasession.MediaSessionManager
import com.kylentt.musicplayer.domain.mediasession.service.ControllerCommand
import com.kylentt.musicplayer.ui.musicactivity.IntentWrapper
import com.kylentt.musicplayer.ui.preferences.AppSettings
import com.kylentt.musicplayer.ui.preferences.AppState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

data class ItemBitmap(
    val item: MediaItem,
    val bm: Bitmap?,
    val fade: Boolean
)

@HiltViewModel
internal class ControllerViewModel @Inject constructor(
    private val connector: ServiceConnectorImpl,
    private val manager: MediaSessionManager,
    private val repository: SongRepositoryImpl,
    private val mediaRepo: MediaRepository,
    private val coilHandler: CoilHandler,
    private val itemHandler: MediaItemHandler,
    private val protoRepo: ProtoRepository
) : ViewModel() {

    /** Connector State */
    val serviceState = manager.serviceState
    fun checkServiceState() = connector.checkServiceState()
    fun connectService(onConnected: () -> Unit = {}) {
        manager.connectService()
        onConnected()
    }

    val appSetting = mutableStateOf(AppSettings())
    val appState = mutableStateOf(AppState())


    private fun startCollectSettings() = viewModelScope.launch {
        protoRepo.collectSettings().collect {
            appSetting.value = it
        }
    }

    private fun startCollectState() = viewModelScope.launch {
        protoRepo.collectState().collect {
            appState.value = it
        }
    }



    suspend fun collectAppState() = protoRepo.collectState()
    suspend fun collectAppSettings() = protoRepo.collectSettings()

    suspend fun writeAppState(data: (current: AppState) -> AppState) = protoRepo.writeToState { data(it) }
    suspend fun writeAppSettings(data: () -> AppSettings) = protoRepo.writeToSettings { data() }

    var startIndex = 0

    val itemBitmap = mutableStateOf(ItemBitmap(MediaItem.EMPTY, null, true))
    val playerCurrentMediaItem = manager.itemState
    val playerBitmap = manager.bitmapState

    val playerPlaybackState = manager.playbackState
    val playerCurrentPlaystate = mutableStateOf("Unit")

    val playerCurrentBitmap = manager.bitmapState

    private suspend fun collectItemBitmap() {
        playerBitmap.collect { bm ->
            val item = playerCurrentMediaItem.value
            itemBitmap.value = ItemBitmap(item, bm, item != itemBitmap.value.item)
        }
    }

    val position = connector.position
    val duration = connector.duration

    /*private suspend fun collectPlayerState() {
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
    }*/

    /*private suspend fun collectPlayerItem() {
        manager.itemState.collect {
            playerCurrentMediaItem.value = it
        }
    }*/

    /*private suspend fun updateBitmap(item: MediaItem) = withContext(Dispatchers.IO) {
        manager.bitmapState.collect {  }
    }*/

    init {
        Timber.d("ControllerViewModel initialized ${this.hashCode()}")
        startCollectSettings()
        startCollectState()
        viewModelScope.launch { collectItemBitmap() }
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