package com.kylentt.mediaplayer.domain.mediaSession

import androidx.lifecycle.ViewModel
import com.kylentt.mediaplayer.core.util.handler.IntentHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val mediaSessionManager: MediaSessionManager,
    private val intentHandler: IntentHandler
) : ViewModel() {

    fun connectSession() {
        mediaSessionManager
    }

}