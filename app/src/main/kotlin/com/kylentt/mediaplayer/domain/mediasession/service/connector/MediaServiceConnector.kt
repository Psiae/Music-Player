package com.kylentt.mediaplayer.domain.mediasession.service.connector

import android.content.Context
import androidx.annotation.MainThread
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.core.coroutines.AppScope
import com.kylentt.mediaplayer.data.repository.ProtoRepository
import com.kylentt.mediaplayer.domain.mediasession.MediaSessionManager
import com.kylentt.mediaplayer.domain.mediasession.service.MediaService
import com.kylentt.mediaplayer.helper.image.CoilHelper
import com.kylentt.mediaplayer.helper.media.MediaItemHelper
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Connector between [MediaSessionManager] and [MediaServiceController]
 * @author Kylentt
 * @since 2022/04/30
 * @see [MediaService]
 */

class MediaServiceConnector(
  @JvmField val appScope: AppScope,
  @JvmField val baseContext: Context,
  @JvmField val coilHelper: CoilHelper,
  @JvmField val dispatchers: AppDispatchers,
  @JvmField val itemHelper: MediaItemHelper,
  @JvmField val protoRepo: ProtoRepository,
  @JvmField val sessionManager: MediaSessionManager
) {

  private val serviceController = MediaServiceController(this)

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


