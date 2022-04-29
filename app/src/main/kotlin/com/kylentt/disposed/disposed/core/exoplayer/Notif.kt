package com.kylentt.disposed.disposed.core.exoplayer

import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.MediaNotification
import timber.log.Timber

class NotifProvider(
  private val notif: ((controller: MediaController, callback: MediaNotification.Provider.Callback) -> MediaNotification)
) : MediaNotification.Provider {

  // Handle Notification myself
  override fun createNotification(
    mediaController: MediaController,
    actionFactory: MediaNotification.ActionFactory,
    onNotificationChangedCallback: MediaNotification.Provider.Callback,
  ): MediaNotification {
    Timber.d("NotificationProvider createNotification")
    return notif(mediaController, onNotificationChangedCallback)
  }

  // This method wasn't called for some reason
  override fun handleCustomAction(
    mediaController: MediaController,
    action: String,
    extras: Bundle,
  ) = Unit
}
