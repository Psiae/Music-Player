package com.kylentt.mediaplayer.domain.service

import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession

// Service for UI to interact With ViewModel Controller

class MusicService : MediaLibraryService() {

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
    }
}