package com.kylentt.mediaplayer.domain.mediasession.service.connector

import android.content.Context
import com.kylentt.mediaplayer.app.AppDispatchers
import com.kylentt.mediaplayer.app.AppScope
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionManager
import com.kylentt.mediaplayer.helper.Preconditions.verifyMainThread
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

class MediaServiceConnector(
  val appScope: AppScope,
  val baseContext: Context,
  val coilHelper: CoilHelper,
  val dispatchers: AppDispatchers,
  val itemHelper: MediaItemHelper,
  val protoRepo: ProtoRepository,
  sessionManager: MediaSessionManager
) {
  val serviceController = MediaServiceController(this)

  val itemBitmap = serviceController.itemBitmapSF.asStateFlow()
  val playbackState = serviceController.playbackStateSF.asStateFlow()
  val serviceState = serviceController.serviceStateSF.asStateFlow()

  fun connectService() {
    verifyMainThread()
    serviceController.connectController { Timber.d("Controller Connected") }
  }

  fun commandController(command: ControllerCommand) {
    verifyMainThread()
    serviceController.commandController(command)
  }
}


