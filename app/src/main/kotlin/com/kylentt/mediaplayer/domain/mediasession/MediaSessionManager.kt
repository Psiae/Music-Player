package com.kylentt.mediaplayer.domain.mediasession

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.media3.session.MediaLibraryService
import com.kylentt.mediaplayer.app.AppDispatchers
import com.kylentt.mediaplayer.app.AppScope
import com.kylentt.mediaplayer.data.repository.MediaRepository
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.domain.mediasession.service.connector.ControllerCommand
import com.kylentt.mediaplayer.domain.mediasession.service.connector.MediaServiceConnector
import com.kylentt.mediaplayer.helper.Preconditions.verifyMainThread
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import com.kylentt.mediaplayer.helper.external.MediaIntentHandlerImpl
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper
import java.lang.Exception
import javax.inject.Singleton

@Singleton
class MediaSessionManager(
  appScope: AppScope,
  baseContext: Context,
  coilHelper: CoilHelper,
  dispatchers: AppDispatchers,
  itemHelper: MediaItemHelper,
  mediaRepo: MediaRepository,
  protoRepo: ProtoRepository
) {

  val context = baseContext

  private val serviceConnector by lazy {
    MediaServiceConnector(
      appScope = appScope,
      baseContext = baseContext,
      coilHelper = coilHelper,
      dispatchers = dispatchers,
      itemHelper = itemHelper,
      protoRepo = protoRepo,
      sessionManager = this
    )
  }

  private val intentHandler by lazy {
    MediaIntentHandlerImpl(
      context = baseContext,
      dispatcher = dispatchers,
      itemHelper = itemHelper,
      mediaRepo = mediaRepo,
      protoRepo = protoRepo,
      sessionManager = this
    )
  }

  val itemBitmap = serviceConnector.itemBitmap
  val playbackState = serviceConnector.playbackState
  val serviceState = serviceConnector.serviceState

  @MainThread
  fun connectService() {
    verifyMainThread()
    serviceConnector.connectService()
  }

  @MainThread
  fun sendControllerCommand(command: ControllerCommand) {
    verifyMainThread()
    serviceConnector.commandController(command)
  }

  suspend fun handleMediaIntent(intent: IntentWrapper) {
    try {
      if (!intent.shouldHandleIntent) return
      intentHandler.handleMediaIntent(intent)
    } catch (e: Exception) {
      Toast
        .makeText(context, "Unable To Play Media", Toast.LENGTH_LONG)
        .show()
    }
  }


  init {
    require(baseContext is Application)
  }
}
