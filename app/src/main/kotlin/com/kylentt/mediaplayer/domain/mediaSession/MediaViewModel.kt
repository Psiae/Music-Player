package com.kylentt.mediaplayer.domain.mediaSession

import androidx.lifecycle.ViewModel
import com.kylentt.musicplayer.domain.mediasession.MediaSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val sessionManager: MediaSessionManager
) : ViewModel() {

    val serviceState = sessionManager.serviceState

    fun connectService() {
        sessionManager.connectService()
    }

}