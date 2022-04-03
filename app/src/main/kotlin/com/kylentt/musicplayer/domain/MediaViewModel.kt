package com.kylentt.musicplayer.domain

import androidx.annotation.IntRange
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kylentt.musicplayer.data.repository.ProtoRepository
import com.kylentt.musicplayer.domain.mediasession.MediaSessionManager
import com.kylentt.musicplayer.domain.mediasession.service.MediaServiceState
import com.kylentt.musicplayer.ui.preferences.AppState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class MediaViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val sessionManager: MediaSessionManager,
    private val protoRepository: ProtoRepository
) : ViewModel() {

    val appState = protoRepository.appState
    val appSettings = protoRepository.appSettings

    var openState = AppState.Defaults.invalidState

    var navigationStartIndex: Int = 0

    // more explicit
    val serviceState = mutableStateOf<MediaServiceState>(MediaServiceState.UNIT)
    val shouldHandleIntent: Boolean?
        get() {
            return savedStateHandle.get<Boolean>("CONNECT")?.not()
        }
    val showSplashScreen
        get() = when(val state = serviceState.value) {
            MediaServiceState.CONNECTED, is MediaServiceState.ERROR -> {
                appState.value == AppState.Defaults.invalidState
            }
            else -> { true }
        }

    fun connectService() {
        sessionManager.connectService()
        savedStateHandle.set("CONNECTED", true)
    }

    private suspend fun collectServiceState(): Unit = sessionManager.serviceState.collect { state ->
       when(state) {
           is MediaServiceState.ERROR -> { /* TODO */ }
           else -> serviceState.value = state
       }
    }


    suspend fun writeAppState(data: suspend (AppState) -> AppState) = protoRepository.writeToState { data(it) }

    init {
        viewModelScope.launch {
            collectServiceState()
        }
    }



}