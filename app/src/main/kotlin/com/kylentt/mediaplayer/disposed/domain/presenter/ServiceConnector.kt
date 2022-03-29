package com.kylentt.mediaplayer.disposed.domain.presenter

import androidx.media3.session.MediaController

interface MusicServiceConnector {

    fun isServiceConnected(): Boolean

    fun connectService(onConnected: (MediaController) -> Unit = {} )

}