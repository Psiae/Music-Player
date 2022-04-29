package com.kylentt.disposed.musicplayer.domain.mediasession.service

import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession

internal class MediaService : MediaLibraryService() {


  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
    TODO("Not yet implemented")
  }
}
