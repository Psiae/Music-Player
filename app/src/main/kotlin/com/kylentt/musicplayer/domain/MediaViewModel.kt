package com.kylentt.musicplayer.domain

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.kylentt.mediaplayer.core.util.handler.IntentHandler
import com.kylentt.musicplayer.data.repository.ProtoRepository
import com.kylentt.musicplayer.domain.mediasession.MediaIntentHandler
import com.kylentt.musicplayer.domain.mediasession.MediaSessionManager
import com.kylentt.musicplayer.domain.mediasession.service.MediaServiceState
import com.kylentt.musicplayer.ui.musicactivity.IntentWrapper
import com.kylentt.musicplayer.ui.preferences.AppSettings
import com.kylentt.musicplayer.ui.preferences.AppState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class MediaViewModel @Inject constructor(
    private val intentHandler: MediaIntentHandler,
    private val savedStateHandle: SavedStateHandle,
    private val sessionManager: MediaSessionManager,
    private val protoRepository: ProtoRepository
) : ViewModel() {

    val appState = protoRepository.appState
    val appSettings = protoRepository.appSettings

    suspend fun writeAppState(data: suspend (AppState) -> AppState) = protoRepository.writeToState { data(it) }
    suspend fun writeAppSettings(data: suspend (AppSettings) -> AppSettings) = protoRepository.writeToSettings { data(it) }

    val itemBitmap = mutableStateOf<ItemBitmap>(ItemBitmap.EMPTY)
    val navStartIndex = mutableStateOf<Int>(0)
    val whenGranted = mutableStateListOf< () -> Unit >()

    // more explicit
    val serviceState = mutableStateOf<MediaServiceState>(MediaServiceState.UNIT)
    val shouldHandleIntent: Boolean?
        get() {
            return savedStateHandle.get<Boolean>("CONNECTED")?.not()
        }
    val showSplashScreen
        get() = when(serviceState.value) {
            MediaServiceState.CONNECTED, is MediaServiceState.ERROR -> {
                appState.value == AppState.Defaults.INVALID
            }
            else -> true
        }

    fun connectService() {
        sessionManager.connectService()
        savedStateHandle.set("CONNECTED", true)
    }

    suspend fun handleIntent(wrapped: IntentWrapper) {
        intentHandler.handleIntent(wrapped)
    }

    private suspend fun collectServiceState(): Unit = sessionManager.serviceState.collect { state ->
       when(state) {
           is MediaServiceState.ERROR -> { /* TODO */ }
           else -> serviceState.value = state
       }
    }

    private suspend fun collectItemBitmap(): Unit = sessionManager.bitmapState.collect { bitmap ->
        val item = sessionManager.itemState.first()
        val ibm = itemBitmap.value
        itemBitmap.value = ibm.copy(bitmap = bitmap, item = item, fade = ibm.item != item)
    }

    init {
        viewModelScope.launch {
            collectServiceState()
        }
        viewModelScope.launch {
            collectItemBitmap()
        }
    }
}

data class ItemBitmap(
    val bitmap: Bitmap?,
    val item: MediaItem,
    val fade: Boolean = true
) {
    companion object {
        val EMPTY = ItemBitmap(null, MediaItem.EMPTY)
    }
}