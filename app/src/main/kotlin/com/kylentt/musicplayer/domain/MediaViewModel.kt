package com.kylentt.musicplayer.domain

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kylentt.musicplayer.domain.mediasession.MediaSessionManager
import com.kylentt.musicplayer.domain.mediasession.service.MediaService
import com.kylentt.musicplayer.domain.mediasession.service.MediaServiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class MediaViewModel @Inject constructor(
    private val sessionManager: MediaSessionManager
) : ViewModel() {

    // more explicit
    val serviceState = mutableStateOf<MediaServiceState>(MediaServiceState.UNIT)
    val showSplashScreen
        get() = when(serviceState.value) {
            MediaServiceState.CONNECTED, is MediaServiceState.ERROR -> false
            else -> true
        }

    fun connectService() { sessionManager.connectService() }

    suspend fun collectServiceState(): Unit = sessionManager.serviceState.collect { state ->
       when(state) {
           is MediaServiceState.ERROR -> { /* TODO */ }
           else -> serviceState.value = state
       }
    }

    init {
        viewModelScope.launch {
            collectServiceState()
        }
    }



}