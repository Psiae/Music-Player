package com.kylentt.mediaplayer.domain.presenter

import androidx.media3.session.MediaController

interface ServiceConnector {

    fun isServiceConnected(): Boolean

    fun connectService(): Boolean

}