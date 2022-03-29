package com.kylentt.musicplayer.domain.mediasession.service

import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession

class MediaService : MediaLibraryService() {

    override fun onCreate() {
        super.onCreate()

    }


    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        TODO("Not yet implemented")
    }
}