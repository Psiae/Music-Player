package com.kylentt.musicplayer.domain.mediasession.service

import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.kylentt.musicplayer.domain.mediasession.MediaSessionManager

internal class MediaService : MediaLibraryService() {


  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
    TODO("Not yet implemented")
  }
}
