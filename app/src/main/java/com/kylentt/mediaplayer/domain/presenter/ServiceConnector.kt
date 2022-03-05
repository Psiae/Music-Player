package com.kylentt.mediaplayer.domain.presenter

import androidx.media3.session.MediaController

interface MusicServiceConnector {

    fun isServiceConnected(): Boolean

    fun connectService(onConnected: (MediaController) -> Unit = {} )

}