package com.kylentt.musicplayer.domain.mediasession.service

import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.kylentt.musicplayer.domain.mediasession.MediaSessionManager
import kotlinx.coroutines.flow.MutableStateFlow

sealed class MediaServiceState {
    object STATE_UNIT : MediaServiceState()
    object STATE_DISCONNECTED : MediaServiceState()
    object STATE_CONNECTING : MediaServiceState()
    object STATE_CONNECTED : MediaServiceState()
    data class STATE_ERROR(val msg: String, val e: Exception) : MediaServiceState()
}

class MediaServiceConnector(
    private val manager: MediaSessionManager,
    private val context: Context
) {

    val serviceState = MutableStateFlow<MediaServiceState>(MediaServiceState.STATE_UNIT)

    lateinit var sessionToken: SessionToken
    lateinit var mediaController: MediaController


    fun connectService() {

    }

}