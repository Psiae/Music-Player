package com.kylentt.mediaplayer.domain.mediasession.service.connector

import android.content.Context
import androidx.annotation.MainThread
import com.kylentt.mediaplayer.app.coroutines.AppDispatchers
import com.kylentt.mediaplayer.app.coroutines.AppScope
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionManager
import com.kylentt.mediaplayer.helper.Preconditions.verifyMainThread
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/** Connector between MediaSessionManager and MediaServiceController */

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

  @MainThread
  fun connectService() {
    serviceController.connectController { Timber.d("Controller Connected") }
  }

  @MainThread
  fun commandController(command: ControllerCommand) {
    serviceController.commandController(command)
  }
}


