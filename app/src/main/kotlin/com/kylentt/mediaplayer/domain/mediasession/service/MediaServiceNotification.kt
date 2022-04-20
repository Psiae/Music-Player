package com.kylentt.mediaplayer.domain.mediasession.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.media3.session.MediaController
import androidx.media3.session.MediaNotification
import com.kylentt.mediaplayer.app.AppDispatchers
import com.kylentt.mediaplayer.app.AppScope
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.helper.VersionHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

class MediaServiceNotification(
  private val dispatchers: AppDispatchers,
  private val protoRepo: ProtoRepository,
  private val service: MediaService,
  private val scope: AppScope
) : MediaNotification.Provider {

  val manager = service
    .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  init {
    if (VersionHelper.hasOreo()) createNotificationChannel()
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createNotificationChannel() {
    manager.createNotificationChannel(
      NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        NOTIFICATION_NAME,
        NotificationManager.IMPORTANCE_LOW
      )
    )
  }

  override fun createNotification(
    mediaController: MediaController,
    actionFactory: MediaNotification.ActionFactory,
    onNotificationChangedCallback: MediaNotification.Provider.Callback
  ): MediaNotification {
    notificationCallback = onNotificationChangedCallback
    TODO("Not yet implemented")
  }

  override fun handleCustomAction(
    mediaController: MediaController,
    action: String,
    extras: Bundle
  ) {
    TODO("Not yet implemented")
  }

  private var notificationCallbackJob = Job().job
  private var notificationCallback = MediaNotification.Provider.Callback {}
  private fun launchNotificationCallback() {
    notificationCallbackJob.cancel()
    notificationCallbackJob = service.serviceScope.launch {

    }
  }

  companion object {
    const val NOTIFICATION_CHANNEL_ID = "Media Service Channel"
    const val NOTIFICATION_NAME = "Media Service Notification"
    const val NOTIFICATION_ID = 301
  }
}
